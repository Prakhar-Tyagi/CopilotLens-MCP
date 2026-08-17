/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.PinListAddPinHelper;
import chs.cof.draw.IGrid;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPin;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LibraryObjectInfoCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Optional;
import java.util.Queue;

/**
 * Responsible for creating and placing schem pins on inline halves for shield terminations.
 */
public class ShieldTerminationPinPlacer
{

	/**
	 * Determines the pin creation strategy and places schem pins on both inline
	 * halves accordingly.
	 *
	 * @param sharedPins shared pin queue from the shared inline handler, empty if non-shared
	 * @param jackParams termination params for the jack side
	 * @param plugParams termination params for the plug side
	 * @return the explicit mate cable pin if created (SHARED_CABLE_JACK / SHARED_CABLE_PLUG),
	 * empty otherwise
	 */
	@NotNull
	Optional<IAbstractPin> placePins(@NotNull Optional<Queue<ISharedPin>> sharedPins,
			@NotNull InlineHalfShieldTerminationParams jackParams,
			@NotNull InlineHalfShieldTerminationParams plugParams)
	{
		IPinList schemJack = jackParams.getSchemInlineHalf();
		IPinList schemPlug = plugParams.getSchemInlineHalf();
		ISchemDiagram diagram = schemJack.getDiagram();

		chs.cof.logical.cable.IPinList jack = schemJack.getConnectivity();
		chs.cof.logical.cable.IPinList plug = schemPlug.getConnectivity();
		IAbstractPin jackPin = jackParams.getInlineHalfPin();
		IAbstractPin plugPin = plugParams.getInlineHalfPin();

		Point jackPos = jackParams.getInlineHalfPinRelativePosition();
		Point plugPos = plugParams.getInlineHalfPinRelativePosition();

		ShieldTerminationPinType jackType = jackParams.getPinType();
		ShieldTerminationPinType plugType = plugParams.getPinType();

		switch (determineStrategy(sharedPins, jackType, plugType)) {

			case SHARED_EXPLICIT:
				if (sharedPins.isPresent()) {
					setSharedPins(sharedPins.get(), plugPin, jackPin);
					createSchemPin(schemJack, jack, diagram, jackPos, jackPin, jackParams);
					createSchemPin(schemPlug, plug, diagram, plugPos, plugPin, plugParams);
				}
				return Optional.empty();

			case SHARED_CABLE_JACK:
				return placeSharedPins(sharedPins.get(), diagram,
						schemJack, jack, jackPin, jackPos, jackParams,
						schemPlug, plug, plugPin, plugPos, plugParams,
						true);

			case SHARED_CABLE_PLUG:
				return placeSharedPins(sharedPins.get(), diagram,
						schemJack, jack, jackPin, jackPos, jackParams,
						schemPlug, plug, plugPin, plugPos, plugParams,
						false);

			case NON_SHARED_EXPLICIT:
				createSchemPin(schemJack, jack, diagram, jackPos, jackPin, jackParams);
				createSchemPin(schemPlug, plug, diagram, plugPos, plugPin, plugParams);
				return Optional.empty();

			case NON_SHARED_AUTO:
			default:
				createSchemPin(schemJack, jack, diagram, jackPos, jackPin, jackParams);
				return Optional.empty();
		}
	}

	@NotNull
	private Optional<IAbstractPin> placeSharedPins(
			@NotNull Queue<ISharedPin> sharedPins,
			@NotNull ISchemDiagram diagram,
			@NotNull IPinList schemJack, @NotNull chs.cof.logical.cable.IPinList jack,
			@NotNull IAbstractPin jackPin, @NotNull Point jackPos,
			@NotNull InlineHalfShieldTerminationParams jackParams,
			@NotNull IPinList schemPlug, @NotNull chs.cof.logical.cable.IPinList plug,
			@NotNull IAbstractPin plugPin, @NotNull Point plugPos,
			@NotNull InlineHalfShieldTerminationParams plugParams,
			boolean cableIsOnJack)
	{
		IPinList cableSideSchemPl = cableIsOnJack ? schemJack : schemPlug;
		IPinList mateSideSchemPl = cableIsOnJack ? schemPlug : schemJack;
		chs.cof.logical.cable.IPinList cableSidePl = cableIsOnJack ? jack : plug;
		chs.cof.logical.cable.IPinList mateSidePl = cableIsOnJack ? plug : jack;
		IAbstractPin inlinePin = cableIsOnJack ? jackPin : plugPin;
		IAbstractPin nonInlinePin = cableIsOnJack ? plugPin : jackPin;
		Point inlinePinPosition = cableIsOnJack ? jackPos : plugPos;
		Point nonInlinePinPosition = cableIsOnJack ? plugPos : jackPos;
		InlineHalfShieldTerminationParams nonInlinePinParams = cableIsOnJack ? plugParams : jackParams;

		// Step 1: create mate cable pin, set shared pins, and connect mate pins
		InlineHalfShieldTerminationParams inlinePinParams = cableIsOnJack ? jackParams : plugParams;
		IAbstractPin matePin = inlinePinParams.getPinType().getFactory().createPin(mateSideSchemPl);
		if (cableIsOnJack) {
			setSharedPins(sharedPins, matePin, inlinePin);
		}
		else {
			setSharedPins(sharedPins, inlinePin, matePin);
		}
		matePin.connectIfPossible(inlinePin);

		// Step 2: compute mate pin position
		IPin anyMateSidePin = mateSideSchemPl.getPins().stream().findFirst().orElse(null);
		if (anyMateSidePin != null) {
			Point matePos = new Point(anyMateSidePin.getLocation().getX(), inlinePinPosition.y);

			// Step 3: place mate pin schem on the mate side via addPinOnly.
			new PinListAddPinHelper(mateSideSchemPl, false)
					.addPinOnly(diagram, matePos, mateSideSchemPl, mateSidePl, matePin,
							null, null, null, null, null);

			// Step 4: place cable pin schem on cable side via addPinOnly
			new PinListAddPinHelper(cableSideSchemPl, false)
					.addPinOnly(diagram, inlinePinPosition, cableSideSchemPl, cableSidePl, inlinePin,
							null, null, null, null, null);

			// Step 5: connect mate schem pin
			IPin cableSchemPin = getSchemPin(cableSideSchemPl, inlinePin);
			if (cableSchemPin != null) {
				ConnectionHelper chelper = new ConnectionHelper();
				if (chelper.examine(cableSchemPin, diagram)) {
					IGrid grid = diagram.getGrid();
					chelper.connectPin(cableSchemPin, grid, true, new LibraryObjectInfoCache());
				}
			}
		}

		// Step 6: create the non-inline pin at other side
		createSchemPin(mateSideSchemPl, mateSidePl, diagram, nonInlinePinPosition, nonInlinePin, nonInlinePinParams);

		return Optional.of(matePin);
	}

	private enum PinCreationStrategy
	{
		SHARED_EXPLICIT,
		SHARED_CABLE_JACK,
		SHARED_CABLE_PLUG,
		NON_SHARED_EXPLICIT,
		NON_SHARED_AUTO
	}

	@NotNull
	private PinCreationStrategy determineStrategy(
			@NotNull Optional<Queue<ISharedPin>> sharedPins,
			@NotNull ShieldTerminationPinType jackType,
			@NotNull ShieldTerminationPinType plugType)
	{
		if (sharedPins.isPresent()) {
			boolean jackIsCable = jackType.shouldSetSharedPins();
			boolean plugIsCable = plugType.shouldSetSharedPins();
			if (jackIsCable && plugIsCable) {
				return PinCreationStrategy.SHARED_EXPLICIT;
			}
			if (jackIsCable) {
				return PinCreationStrategy.SHARED_CABLE_JACK;
			}
			if (plugIsCable) {
				return PinCreationStrategy.SHARED_CABLE_PLUG;
			}
		}
		if (jackType.requiresExplicitMateCreation() || plugType.requiresExplicitMateCreation()) {
			return PinCreationStrategy.NON_SHARED_EXPLICIT;
		}
		return PinCreationStrategy.NON_SHARED_AUTO;
	}

	private void createSchemPin(@NotNull IPinList schemHalf,
			@NotNull chs.cof.logical.cable.IPinList half,
			@NotNull ISchemDiagram diagram, @NotNull Point pos,
			@NotNull IAbstractPin pin,
			@NotNull InlineHalfShieldTerminationParams shieldTerminationParams)
	{
		shieldTerminationParams.getPinType().getFactory().createSchemPin(schemHalf, half, diagram, pos, pin);
	}

	private void setSharedPins(@NotNull Queue<ISharedPin> sharedPins,
			@NotNull IAbstractPin plugPin, @NotNull IAbstractPin jackPin)
	{
		final ISharedPin sharedPlugPin = sharedPins.poll();
		assert sharedPlugPin != null;
		final ISharedPin sharedJackPin = sharedPlugPin.getMatePin();
		assert sharedJackPin != null;
		plugPin.setSharedPin(sharedPlugPin);
		jackPin.setSharedPin(sharedJackPin);
	}

	@Nullable
	private IPin getSchemPin(@NotNull IPinList schemPinList, @NotNull IAbstractPin pin)
	{
		return schemPinList.getPins().stream()
				.filter(schemPin -> pin.equals(schemPin.getConnectivity()))
				.findFirst()
				.orElse(null);
	}
}
