/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.PlaceInlineShieldTerminationEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.cof.logical.shared.ISharedPin;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ILocation;
import chs.common.preferencesets.IPreferenceSet;
import chs.utilities.ReverseMap;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.topology.utils.IInlineAssistFailureCollector;
import chs.utility.topology.utils.subsystem.messaging.MessagingServices;
import chs.view.assist.AbstractConnectionCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * This class is responsible for adding shield terminations to inline halves -
 * via shield directly, or via pigtail wires (based on shieldTerminationType).
 */
public class ShieldAdder
{

	private final ShieldTerminationPinPlacer mPinPlacer = new ShieldTerminationPinPlacer();

	public void processShieldConnections(@NotNull InsertInlineResult.ResultInlineConnector resultConnector,
			@NotNull NewConnectorData newConnectorData, @NotNull ISystemLogicDiagram systemLogicDiagram,
			@NotNull Optional<Queue<ISharedPin>> sharedPins)
	{
		Map<IShieldConductor, ShieldPositionData> shieldTerminations =
				newConnectorData.getExtent().getShieldTerminationInfos();
		if (shieldTerminations.isEmpty()) {
			return;
		}

		IPinList schemJack = resultConnector.getJack();
		IPinList schemPlug = resultConnector.getPlug();

		boolean isReversedPinSide = newConnectorData.getDirection().isReversedPinSide();
		Map<IShieldConductor, IShieldConductor> jackShieldToPlugShieldMap =
				getJackShieldToPlugShieldMapFromInline(schemJack, isReversedPinSide);

		Collection<IPin> createdSchemInlinePins =
				processInlineHalfPinsWithTerminations(sharedPins, shieldTerminations, jackShieldToPlugShieldMap,
						isReversedPinSide, schemJack, schemPlug);

		PreferenceSetHelper.applyStyleSet(createdSchemInlinePins, systemLogicDiagram, true);
	}

	@NotNull
	private Collection<IPin> processInlineHalfPinsWithTerminations(@NotNull Optional<Queue<ISharedPin>> sharedPins,
			@NotNull Map<IShieldConductor, ShieldPositionData> shieldTerminations,
			@NotNull Map<IShieldConductor, IShieldConductor> jackShieldToPlugShieldMap, boolean isReversedPinSide,
			@NotNull IPinList schemJack, @NotNull IPinList schemPlug)
	{
		Collection<IPin> createdSchemInlinePins = new HashSet<>();

		//always iterate through shieldTerminations
		for (IShieldConductor jackShield : shieldTerminations.keySet()) {
			IShieldConductor plugShield = jackShieldToPlugShieldMap.get(jackShield);
			if (plugShield == null) {
				continue;
			}
			if (isReversedPinSide) {
				IShieldConductor temp = jackShield;
				jackShield = plugShield;
				plugShield = temp;
			}
			ShieldPositionData placement =
					isReversedPinSide ? shieldTerminations.get(plugShield) : shieldTerminations.get(jackShield);

			if (placement == null || placement.getTerminations().size() < 2) {
				continue;
			}

			InlineHalfShieldTerminationParams jackShieldTerminationParams =
					new InlineHalfShieldTerminationParams(jackShield, placement.getTerminations().get(0), schemJack);
			InlineHalfShieldTerminationParams plugShieldTerminationParams =
					new InlineHalfShieldTerminationParams(plugShield, placement.getTerminations().get(1), schemPlug);

			ISchemDiagram diagram = schemJack.getDiagram();
			if (isHookupAlreadyConnectedToShield(diagram, jackShieldTerminationParams, plugShieldTerminationParams)) {
				MessagingServices.getService(IInlineAssistFailureCollector.class)
						.addMessage("InlineAssistFailureCollector.InsufficientSpace");
				continue;
			}

			createInlinePinsWithTerminations(createdSchemInlinePins, sharedPins, jackShieldTerminationParams,
					plugShieldTerminationParams);
		}
		return createdSchemInlinePins;
	}

	private void createInlinePinsWithTerminations(@NotNull Collection<IPin> createdSchemInlinePins,
			@NotNull Optional<Queue<ISharedPin>> sharedPins,
			@NotNull InlineHalfShieldTerminationParams jackShieldTerminationParams,
			@NotNull InlineHalfShieldTerminationParams plugShieldTerminationParams)
	{
		IPinList schemJack = jackShieldTerminationParams.getSchemInlineHalf();
		IPinList schemPlug = plugShieldTerminationParams.getSchemInlineHalf();
		IAbstractPin jackPin = createPinForInlineHalf(schemJack, jackShieldTerminationParams);
		IAbstractPin plugPin = createPinForInlineHalf(schemPlug, plugShieldTerminationParams);

		plugPin.connectIfPossible(jackPin);

		jackShieldTerminationParams.setInlineHalfPin(jackPin);
		plugShieldTerminationParams.setInlineHalfPin(plugPin);

		Optional<IAbstractPin> mateCablePin =
				mPinPlacer.placePins(sharedPins, jackShieldTerminationParams, plugShieldTerminationParams);

		regenerateInlineHalves(schemJack, schemPlug);

		IPin schemJackPin = getSchemPin(schemJack, jackPin);
		IPin schemPlugPin = getSchemPin(schemPlug, plugPin);
		if (schemJackPin == null || schemPlugPin == null) {
			return;
		}
		createdSchemInlinePins.add(schemJackPin);
		createdSchemInlinePins.add(schemPlugPin);
		jackShieldTerminationParams.setSchemInlineHalfPin(schemJackPin);
		plugShieldTerminationParams.setSchemInlineHalfPin(schemPlugPin);

		// If an explicit mate cable pin was created, then add its schem pin
		if (mateCablePin.isPresent()) {
			IPin schemMatePin = getSchemPin(schemPlug, mateCablePin.get());
			if (schemMatePin == null) {
				schemMatePin = getSchemPin(schemJack, mateCablePin.get());
			}
			if (schemMatePin != null) {
				createdSchemInlinePins.add(schemMatePin);
			}
		}

		processTermination(jackShieldTerminationParams);
		processTermination(plugShieldTerminationParams);
	}

	@NotNull private IAbstractPin createPinForInlineHalf(@NotNull IPinList schemInlineHalf,
			@NotNull InlineHalfShieldTerminationParams terminationParams)
	{
		ITerminationPinFactory factory = terminationParams.getPinType().getFactory();
		return factory.createPin(schemInlineHalf);
	}

	private boolean isHookupAlreadyConnectedToShield(@NotNull ISchemDiagram diagram,
			@NotNull InlineHalfShieldTerminationParams jackShieldTerminationParams,
			@NotNull InlineHalfShieldTerminationParams plugShieldTerminationParams)
	{
		IShieldConductor jackShield = jackShieldTerminationParams.getInlineHalfShield();
		ILocation jackPinAbsLocation = jackShieldTerminationParams.getInlineHalfPinAbsLocation();
		IShieldConductor plugShield = plugShieldTerminationParams.getInlineHalfShield();
		ILocation plugPinAbsPosition = plugShieldTerminationParams.getInlineHalfPinAbsLocation();

		IShieldBodyHookup jackSBHookup =
				AbstractConnectionCreator.getNearestShieldHookup(jackShield, jackPinAbsLocation, diagram);
		IShieldBodyHookup plugSBHookup =
				AbstractConnectionCreator.getNearestShieldHookup(plugShield, plugPinAbsPosition, diagram);

		if (jackSBHookup == null || plugSBHookup == null) {
			return true;
		}
		return !jackSBHookup.getShieldConductors().isEmpty() || !plugSBHookup.getShieldConductors().isEmpty();
	}

	private void processTermination(@NotNull InlineHalfShieldTerminationParams terminationParams)
	{
		PlaceInlineShieldTerminationEnum terminationType = terminationParams.getShieldTerminationType();
		getShieldConnector(terminationType).connectShield(terminationParams);
	}

	@NotNull private IShieldConnector getShieldConnector(@NotNull PlaceInlineShieldTerminationEnum terminationType)
	{
		if (terminationType == PlaceInlineShieldTerminationEnum.TERMINATE_VIA_SHIELD_TERMINATION) {
			return new ShieldConnector();
		}
		if (terminationType == PlaceInlineShieldTerminationEnum.TERMINATE_VIA_PIGTAIL_WIRE) {
			return new PigTailConnector();
		}
		return new NoOpShieldConnector();
	}

	private void regenerateInlineHalves(@NotNull IPinList schemJack, @NotNull IPinList schemPlug)
	{
		ISchemDiagram diagram = schemJack.getDiagram();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		GeneratorParameters gp = new GeneratorParameters(diagram.getGrid(), styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(schemJack, gp, Generator.NOREGENERATE_PROPERTIES, false);
		generator.generate(schemPlug, gp, Generator.NOREGENERATE_PROPERTIES, false);
	}

	@Nullable private IPin getSchemPin(@NotNull IPinList schemPinList, @NotNull IAbstractPin pin)
	{
		return schemPinList.getPins().stream().filter(schemPin -> pin.equals(schemPin.getConnectivity())).findFirst()
				.orElse(null);
	}

	@NotNull
	private Map<IShieldConductor, IShieldConductor> getJackShieldToPlugShieldMapFromInline(@NotNull IPinList schemJack,
			boolean isReversedPinSide)
	{
		ReverseMap<IShieldConductor, IShieldConductor> jackPlugShieldMap = new ReverseMap<>();
		Set<IMulticore> processedMulticores = new HashSet<>();
		for (IPin schemJackPin : schemJack.getPins()) {
			IAbstractPin jackPin = schemJackPin.getConnectivity();
			IAbstractPin matePlugPin = jackPin.getConnectedPins().iterator().next();

			IConductorIterator jackSideCondIter = jackPin.getConductors();
			IConductorIterator plugSideCondIter = matePlugPin.getConductors();

			// Ensure both sides have exactly one conductor (otherwise skip processing)
			if (jackSideCondIter.getSize() != 1 || plugSideCondIter.getSize() != 1) {
				continue;
			}

			IConductor jackSideConductor = jackSideCondIter.getNext();
			IConductor plugSideConductor = plugSideCondIter.getNext();

			IMulticore jackSideMulticore = jackSideConductor.getMulticore();
			IMulticore plugSideMulticore = plugSideConductor.getMulticore();

			// Traverse up the hierarchy of multicores, mapping shield conductors
			while (jackSideMulticore != null && plugSideMulticore != null) {
				if (!processedMulticores.add(jackSideMulticore)) {
					break; // Avoid re-processing the same multicore
				}

				IShieldConductor jackSideShieldConductor = jackSideMulticore.getShield();
				IShieldConductor plugSideShieldConductor = plugSideMulticore.getShield();

				if (jackSideShieldConductor != null && plugSideShieldConductor != null) {
					jackPlugShieldMap.put(jackSideShieldConductor, plugSideShieldConductor);
				}

				// Move to the parent multicore in the hierarchy
				jackSideMulticore = jackSideMulticore.getParent();
				plugSideMulticore = plugSideMulticore.getParent();
			}
		}

		return isReversedPinSide ? jackPlugShieldMap.getReverseMap() : jackPlugShieldMap;
	}
}
