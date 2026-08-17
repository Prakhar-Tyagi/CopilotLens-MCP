/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2015-2024 Siemens
 */
package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.TransientLibraryDevice;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IOptionedObject;
import chs.common.IDesignAbstraction;
import chs.common.IICDReferredObject;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.ValueTypeEnum;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.ICDSignalDetailsFinder;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDPinSignalAssociation;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.NameTemplateHelper;
import chs.utility.icd.placement.ICDPlacementServiceLocator;
import chs.utility.icd.placement.IUpdateFromICDOptionExpressionHandler;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ICDPlacementHelper
{
	private static Map<Map<String, IICDAssociatedSignal>,Pair<Map<String, IICDAssociatedSignal>, Map<IConductor, IICDAssociatedSignal>>>
			signalMapToUnmatchedConductorsMap = new HashMap<>();

	private ICDPlacementHelper()
	{
	}

	@NotNull private static IICDInterconnectStrategy determineInterconnectionStrategy(ISchemDiagram diagram)
	{
		return determineInterconnectionStrategy(diagram, new PlaceICDPersistenceHandler(diagram, false));
	}

	@NotNull private static IICDInterconnectStrategy determineInterconnectionStrategy(ISchemDiagram diagram,
			@NotNull PersistenceHandler persistenceHandler)
	{
		ILogicDesign design = diagram.getDesign();
		if (design != null) {
			if (ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(design)) {
				return IICDInterconnectStrategy.NULL_ICD_INTERCONNECT_STRATEGY;
			}
			IDesignAbstraction designAbstraction = design.getDesignAbstraction();
			if (designAbstraction != null && designAbstraction.getAllowAutoCreation()) {
				return new ICDInterconnectByWire(persistenceHandler);
			}
			return new ICDInterconnectByNet(persistenceHandler);
		}
		return IICDInterconnectStrategy.NULL_ICD_INTERCONNECT_STRATEGY;
	}

	public static void updateICDNameAndRouting(IPinList pinlist, ILibraryPartSelection librarySelection,
			ISchemDiagram diagram, boolean generateSingleEnded)
	{
		if (librarySelection instanceof IICDSelection) {
			IICDSelection icdSelection = (IICDSelection) librarySelection;
			String deviceName = icdSelection.getSelectedDeviceName();
			IDeviceICD icd = icdSelection.getICD();
			// if transient library object was used to instantiate the icd, then that library ref should
			// be removed from the device
			if (icdSelection.getSelectedObject() instanceof TransientLibraryDevice) {
				//noinspection ConstantConditions
				pinlist.getConnectivity().assignLibraryDetails(null);
			}
			if (icd != null) {
				PlaceICDPersistenceHandler handler = new PlaceICDPersistenceHandler(diagram, generateSingleEnded);
				updateICDNameRoutingAndProperties(pinlist, diagram, deviceName, icd, handler);
			}
		}
	}

	public static void updateICDNameRoutingAndProperties(IPinList pinlist, ISchemDiagram diagram, String deviceName,
			@NotNull IDeviceICD icd, PersistenceHandler persistenceHandler)
	{
		updateICDNameAndRouting(pinlist, icd, deviceName, diagram, persistenceHandler);
		//Overwriting option Expression on Logic Device
		chs.cof.logical.cable.IPinList connectivityObject = pinlist.getConnectivity();

		propagateOptionExpression(connectivityObject, icd);

		if (icd.getLibraryDevice() == null) {
			//if there's a part assigned, the connectivity type code is same as the part type code. This will be taken
			//care in part assignment/part update.
			connectivityObject.setTypeCode(StringUtils.nonNull(icd.getTypeCode()));
		}

		ILibraryDevice libraryDevice = icd.getLibraryDevice();
		// copy the properties assoicated with oti name template for device name
		Set<String> propertiesThatCannotBeOverriden = getAllPropertyNames(icd);
		propertiesThatCannotBeOverriden.addAll(getAllPropertyNames(libraryDevice));

		NameTemplateHelper
				.addPropertiesFromOTINameTemplate(FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr(),
						deviceName, connectivityObject, propertiesThatCannotBeOverriden);
		ICDUtils.copyPropertiesOntoPlacedObject(icd, libraryDevice, connectivityObject);
		//should report if IICDReferredObject.ICD_REF_PROP_NAME named property is coming from ICD also.
		ensureICDRefPropOnDevice(icd, connectivityObject);
		updatePinAttributesAndProperties(icd, libraryDevice, connectivityObject);
	}

	private static void updatePinAttributesAndProperties(IDeviceICD icd, @Nullable ILibraryDevice libraryDevice,
			chs.cof.logical.cable.IPinList device)
	{
		Map<String, ILibraryCavity> libraryPinNameToCavityMap = new HashMap<>();
		if (libraryDevice != null) {
			for (ILibraryCavity libraryCavity : libraryDevice.getAllCavities()) {
				libraryPinNameToCavityMap.put(libraryCavity.getName(), libraryCavity);
			}
		}

		Map<String, IAbstractPin> devicePinNameToAbstractPinMap = new HashMap<>();
		Map<String, ISharedPin> devicePinNameToSharedPinMap = new HashMap<>();
		ISharedPinList sharedDevice = device.getSharedPinList();
		if (sharedDevice != null) {
			for (ISharedPin sharedPin : sharedDevice.getPins()) {
				devicePinNameToSharedPinMap.put(sharedPin.getName(), sharedPin);
			}
		}
		else {
			for (IAbstractPin abstractPin : device.getPinCollection()) {
				devicePinNameToAbstractPinMap.put(abstractPin.getName(), abstractPin);
			}
		}

		for (IDeviceICDPinSignalAssociation iicdPinSignalAssociation : icd.getICDUsageDefinition()
				.getPinSignalAssociations()) {
			String pinName = iicdPinSignalAssociation.getPinName();
			ISharedPin sharedPin = devicePinNameToSharedPinMap.get(pinName);
			if (sharedPin != null) {
				propagateOptionExpression(sharedPin, iicdPinSignalAssociation);
				IPropertiedObject libraryPinObject =
						CommonUtils.cast(libraryPinNameToCavityMap.get(sharedPin.getName()), IPropertiedObject.class);
				ICDUtils.copyPropertiesOntoPlacedObject(iicdPinSignalAssociation, libraryPinObject, sharedPin);
			}
			IAbstractPin placedPin = devicePinNameToAbstractPinMap.get(pinName);
			if (placedPin != null) {
				propagateOptionExpression(placedPin, iicdPinSignalAssociation);
				IPropertiedObject libraryPinObject =
						CommonUtils.cast(libraryPinNameToCavityMap.get(placedPin.getName()), IPropertiedObject.class);
				ICDUtils.copyPropertiesOntoPlacedObject(iicdPinSignalAssociation, libraryPinObject, placedPin);
			}
		}
	}


	public static void propagateOptionExpression(
			ILogicObject destination,
			IOptionedObject source
			)
	{
		ICDPlacementServiceLocator
				.getInstace()
				.locateService(IUpdateFromICDOptionExpressionHandler.class)
				.update(destination, source);
	}

	private static void propagateOptionExpression(
			ISharedPin destination,
			IOptionedObject source
	)
	{
		ICDPlacementServiceLocator
				.getInstace()
				.locateService(IUpdateFromICDOptionExpressionHandler.class)
				.update(destination, source);
	}

	public static void updateICDNameAndRouting(IPinList pinlist, @NotNull IDeviceICD icd, String deviceName,
			ISchemDiagram diagram, @NotNull IObjectFilter<IPin> pinFilter, boolean generateSingleEnded)
	{
		updateICDNameAndRouting(pinlist, icd, deviceName, diagram, new PlaceICDPersistenceHandler(diagram, generateSingleEnded), pinFilter);
	}

	public static void updateICDNameAndRouting(IPinList pinlist, @NotNull IDeviceICD icd, String deviceName,
			ISchemDiagram diagram)
	{
		updateICDNameAndRouting(pinlist, icd, deviceName, diagram, CommonUtils.getNoFilter(), false);
	}

	public static void updateICDNameAndRouting(IPinList pinlist, @NotNull IDeviceICD icd, String deviceName,
			ISchemDiagram diagram, PersistenceHandler persistenceHandler)
	{
		updateICDNameAndRouting(pinlist, icd, deviceName, diagram, persistenceHandler, CommonUtils.getNoFilter());
	}

	public static void updateICDNameAndRouting(IPinList pinlist, IDeviceICD icd, String deviceName,
			ISchemDiagram diagram,
			PersistenceHandler persistenceHandler, @NotNull IObjectFilter<IPin> pinFilter)
	{
		ILogicDesign design = diagram.getDesign();
		if (design != null && ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(design)) {
			return;
		}
		updateByICDName(pinlist, icd, deviceName);
		updateICDRouting(pinlist, icd, diagram, persistenceHandler, pinFilter);
		//LOGIC-6017 update the pin attributes and properties on the cable device.
		chs.cof.logical.cable.IPinList connectivityObject = pinlist.getConnectivity();
		ILibraryDevice libraryDevice = CommonUtils.cast(connectivityObject.getLibraryObject(), ILibraryDevice.class);
		updatePinAttributesAndProperties(icd, libraryDevice, connectivityObject);
	}

	public static void updateByICDName(IPinList pinlist, @NotNull IDeviceICD icd, String deviceName)
	{
		chs.cof.logical.cable.IPinList pl = pinlist.getConnectivity();
		if (pl != null) {
			if (!StringUtils.isEmpty(deviceName)) {
				pl.setName(deviceName);
			}
			ensureICDRefPropOnDevice(icd, pl);
		}
	}

	public static void ensureICDRefPropOnDevice(@NotNull IDeviceICD icd, chs.cof.logical.cable.IPinList pl)
	{
		String icdRefPropName = IICDReferredObject.ICD_REF_PROP_NAME;
		IProperty property = pl.findPropertyByName(icdRefPropName);
		if (property != null) {
			pl.removeProperty(property);
		}
		IProperty prop = FactoryMgr.getCommonFactory()
				.constructProperty(icdRefPropName, ValueTypeEnum.TypeString, icd.getFullName(), false, pl);
		pl.addProperty(prop);
	}

	public static void updateByICDName(@NotNull IPinList createdDevice, ILibraryPartSelection librarySelection)
	{
		if (librarySelection instanceof IICDSelection) {
			IICDSelection iicdSelection = (IICDSelection) librarySelection;
			if (iicdSelection.getICD() != null) {
				updateByICDName(createdDevice, iicdSelection.getICD(), iicdSelection.getSelectedDeviceName());
			}
		}
	}

	public static void updateICDRouting(@NotNull IPinList pinlist, @NotNull IDeviceICD icd, ISchemDiagram diagram,
			boolean generateSingleEnded)
	{
		updateICDRouting(pinlist, icd, diagram, new PlaceICDPersistenceHandler(diagram, generateSingleEnded));
	}

	public static void updateICDRouting(@NotNull IPinList pinlist, @NotNull IDeviceICD icd, ISchemDiagram diagram,
			@NotNull PersistenceHandler persistenceHandler)
	{
		updateICDRouting(pinlist, icd, diagram, persistenceHandler, CommonUtils.getNoFilter());
	}

	public static void updateICDRouting(@NotNull IPinList pinlist, @NotNull IDeviceICD icd, ISchemDiagram diagram,
			@NotNull PersistenceHandler persistenceHandler, @NotNull IObjectFilter<IPin> pinFilter)
	{
		IICDInterconnectStrategy iicdInterconnectStrategy =
				determineInterconnectionStrategy(diagram, persistenceHandler);
		try {
			//The ICD signal group paths (multicore information) are normalized
			//to compute equivalence of the multicore information on its signals.
			//ASML states name of signal to be real information. And signal-group
			//path is a localized information and depicts only the hierarchy.
			//The information doesn't depict capital multicore information.
			//So we have some heuristics for deriving multicore information from
			//signal group paths. However, that information needs to be derived
			//using only the placed ICDs of a design. The applicable ICDs of a
			//design are not real reflection of to be used ICDs.
			//So the holder of that information will be invalidated. And this
			//equivalence will be re-computed based upon the placed ICDs.
			ILogicDesign logicDesign = diagram.getDesign();
			if (logicDesign != null) {
				logicDesign.getDesignICDContainer().resetEquivalentSignalGroupPaths();
			}
			iicdInterconnectStrategy.updateICDRouting(pinlist, icd, diagram, pinFilter);
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			populatePropertiesOnNonICDConductors(pinlist, icd, iicdInterconnectStrategy);
		}
		finally {
			iicdInterconnectStrategy.endRouting();
		}
	}

	private static void populatePropertiesOnNonICDConductors(@NotNull IPinList pinlist, @NotNull IDeviceICD icd,
			IICDInterconnectStrategy icdInterconnectStrategy)
	{
		if (pinlist.getConnectivity() instanceof IDevice) {
			IDevice device = CommonUtils.cast(pinlist.getConnectivity(), IDevice.class);
			if (device != null) {
				boolean isWiringAbstraction = icdInterconnectStrategy.isWiringAbstraction();
				Map<String, IConductor> conductorMap = icdInterconnectStrategy.getAllVistedConductors();
				final Set<IShieldConductor> newShieldsInMulticores =
						icdInterconnectStrategy.getNewShieldsInMulticores();
				Map<String, Map<String, IICDAssociatedSignal>> pinNameToSingalMap = formICDPinNameToSignalMap(icd);
				IAbstractPinIterator devicePins = device.getPins();
				Collection<IConductor> conductorsToPreLock =
						collectConductors(isWiringAbstraction, conductorMap, newShieldsInMulticores,
								devicePins, pinNameToSingalMap);
				preLockObjects(conductorsToPreLock, icd, device, isWiringAbstraction, conductorMap,
						newShieldsInMulticores);
			}
		}
	}

	@NotNull
	private static Collection<IConductor> collectConductors(boolean isWiringAbstraction,
															@NotNull Map<String, IConductor> conductorMap,
															@NotNull Set<IShieldConductor> newShieldsInMulticores,
															@NotNull IAbstractPinIterator devicePins,
															@NotNull Map<String, Map<String, IICDAssociatedSignal>> pinNameToSingalMap)
	{
		Collection<IConductor> objectsToLock = new HashSet<>();
		for (IAbstractPin directDevicePin : devicePins) {
			Map<String, IICDAssociatedSignal> signalMap = pinNameToSingalMap.get(directDevicePin.getName());
			if (signalMap == null || signalMap.isEmpty()) {
				continue;
			}
			Map<String, IICDAssociatedSignal> signalMapBeforeUpdate = new HashMap<>(signalMap);
			Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap =
					findUnvisitedConductorsForDevicePin(conductorMap, newShieldsInMulticores, isWiringAbstraction, directDevicePin,
							signalMap);
			if (unmatchedConductorsToICDSignalMap.isEmpty()) {
				continue;
			}

			signalMapToUnmatchedConductorsMap.put(signalMapBeforeUpdate, new Pair<>(signalMap, unmatchedConductorsToICDSignalMap));
			IConductor conductor = unmatchedConductorsToICDSignalMap.entrySet().iterator().next().getKey();
			IICDAssociatedSignal icdAssociatedSignal = unmatchedConductorsToICDSignalMap.get(conductor);
			if (icdAssociatedSignal == null) {
				continue;
			}
			objectsToLock.add(conductor);
		}
		return objectsToLock;
	}

	private static void preLockObjects(@NotNull Collection<IConductor> conductorsToPreLock, @NotNull IDeviceICD icd,
									   @NotNull IDevice device, boolean isWiringAbstraction,
									   @NotNull Map<String, IConductor> conductorMap,
									   @NotNull Set<IShieldConductor> newShieldsInMulticores)
	{

		Collection<ISharedObject> sharedObjectsToPreLock = collectObjectsToPreLock(conductorsToPreLock);
		BatchLockRefreshHelper.batchLockWithPromise(sharedObjectsToPreLock,
				() -> updatePropForUnVisitedConductorsIfNeeded(icd, device, isWiringAbstraction, conductorMap,
						newShieldsInMulticores));
	}

	@NotNull private static Collection<ISharedObject> collectObjectsToPreLock(@NotNull Collection<IConductor> conductors)
	{
		return conductors.stream()
				.filter(conductor -> conductor.isShared())
				.map(conductor -> conductor.getSharedConductor())
				.filter(sharedConductor -> sharedConductor != null)
				.collect(Collectors.toSet());
	}

	private static void updatePropForUnVisitedConductorsIfNeeded(@NotNull IDeviceICD icd,
																 @NotNull IDevice device, boolean isWiringAbstraction,
																 @NotNull Map<String, IConductor> conductorMap,
																 @NotNull Set<IShieldConductor> newShieldsInMulticores)
	{
		for (Map.Entry<Map<String, IICDAssociatedSignal>, Pair<Map<String, IICDAssociatedSignal>, Map<IConductor, IICDAssociatedSignal>>> entry : signalMapToUnmatchedConductorsMap.entrySet()) {
			Pair<Map<String, IICDAssociatedSignal>, Map<IConductor, IICDAssociatedSignal>> signalMapToUnmatchedConductors = entry.getValue();

			Map<String, IICDAssociatedSignal> signalMap = signalMapToUnmatchedConductors.getKey();
			Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap = signalMapToUnmatchedConductors.getValue();
			updatePropForUnvisitedConductors(isWiringAbstraction, signalMap, unmatchedConductorsToICDSignalMap);
		}

		// Update properties on conductors connected on backshell terminations
		final List<IDeviceICD> icdList = Collections.singletonList(icd);
		final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdBSTermToSignalMap =
				ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(icdList);
		ICDUtils.processMatchingBSTerminals(device, icdList, (icdBSTerm, logicBSTerm) -> {
			final Set<IICDAssociatedSignal> icdAssociatedSignals =
					icdBSTermToSignalMap.pullReadOnlySafeSet(icdBSTerm);
			final Map<String, IICDAssociatedSignal> signalMap = icdAssociatedSignals.stream()
					.collect(Collectors.toMap(IICDAssociatedSignal::getNetName, Function.identity()));
			Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap =
					findUnvisitedConductorsForDevicePin(conductorMap, newShieldsInMulticores, isWiringAbstraction,
							logicBSTerm, signalMap);
			updatePropForUnvisitedConductors(isWiringAbstraction, signalMap, unmatchedConductorsToICDSignalMap);
		});
	}

	private static void updatePropForUnvisitedConductors(boolean isWiringAbstraction,
														 @NotNull Map<String, IICDAssociatedSignal> signalMap,
														 @NotNull Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap)
	{
		updatePropForUnvisitedConductorsWithMatchingSignal(unmatchedConductorsToICDSignalMap);
		updatePropForUnvisitedConducorsWithNoICDAssociatedSignal(isWiringAbstraction, signalMap,
				unmatchedConductorsToICDSignalMap);
	}

	private static void updatePropForUnvisitedConducorsWithNoICDAssociatedSignal(boolean isWiringAbstraction,
			Map<String, IICDAssociatedSignal> signalMap,
			Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap)
	{
		if (signalMap.size() == 1 && unmatchedConductorsToICDSignalMap.size() == 1) {
			Map.Entry<IConductor, IICDAssociatedSignal> entry =
					unmatchedConductorsToICDSignalMap.entrySet().iterator().next();
			String signalName = ICDUtils.getAssociatedSignalNameForConductor(entry.getKey(), isWiringAbstraction);
			//Find out if this null check is necessary
			if (signalName == null) {
				ICDInterconnectStrategy
						.updateAttributesAndPropOnPlacedSignal(entry.getKey(), signalMap.values().iterator().next());
			}
		}
	}

	private static void updatePropForUnvisitedConductorsWithMatchingSignal(
			@NotNull Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap)
	{
		List<IConductor> toRemove = new ArrayList<IConductor>();
		for (Map.Entry<IConductor, IICDAssociatedSignal> entry : unmatchedConductorsToICDSignalMap.entrySet()) {
			IConductor conductor = entry.getKey();
			if (entry.getValue() != null) {
				ICDInterconnectStrategy.updateAttributesAndPropOnPlacedSignal(conductor, entry.getValue());
				toRemove.add(entry.getKey());
			}
		}

		for (IConductor conductor : toRemove) {
			unmatchedConductorsToICDSignalMap.remove(conductor);
		}
	}

	private static Map<IConductor, IICDAssociatedSignal> findUnvisitedConductorsForDevicePin(
			Map<String, IConductor> conductorMap, @NotNull Set<IShieldConductor> newShieldsInMulticores,
			boolean isWiringAbstraction, IAbstractPin directDevicePin,
			Map<String, IICDAssociatedSignal> signalMap)
	{
		Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap = new HashMap<>();
		for (IConductor iConductor : directDevicePin.getConductors()) {
			findIfConductorIsExtra(conductorMap, newShieldsInMulticores, isWiringAbstraction, signalMap,
					unmatchedConductorsToICDSignalMap,
					iConductor);
		}
		for (IAbstractPin connectedPin : directDevicePin.getConnectedPins()) {
			if (connectedPin instanceof IConnectorPin) {
				for (IConductor conductor : connectedPin.getConductors()) {
					findIfConductorIsExtra(conductorMap, newShieldsInMulticores, isWiringAbstraction, signalMap,
							unmatchedConductorsToICDSignalMap, conductor);
				}
			}
		}
		return unmatchedConductorsToICDSignalMap;
	}

	private static void findIfConductorIsExtra(Map<String, IConductor> visitedConductorMap,
			Set<IShieldConductor> newShieldsInMulticores, boolean isWiringAbstraction,
			Map<String, IICDAssociatedSignal> signalMap,
			Map<IConductor, IICDAssociatedSignal> unmatchedConductorsToICDSignalMap, IConductor conductor)
	{
		String placedSignalName = ICDUtils.getAssociatedSignalNameForConductor(conductor, isWiringAbstraction);

		if (visitedConductorMap.containsKey(conductor.getName()) || newShieldsInMulticores.contains(conductor)) {
			signalMap.remove(placedSignalName);
		}
		else {
			IICDAssociatedSignal iicdAssociatedSignal = null;
			if (placedSignalName != null) {
				iicdAssociatedSignal = signalMap.get(placedSignalName);
				signalMap.remove(placedSignalName);
			}
			unmatchedConductorsToICDSignalMap.put(conductor, iicdAssociatedSignal);
		}
	}

	public static Map<String, Map<String, IICDAssociatedSignal>> formICDPinNameToSignalMap(
			@NotNull IDeviceICD matchingICD)
	{
		Map<String, Map<String, IICDAssociatedSignal>> icdPinNameToSignalMap = new HashMap<>();
		for (IDeviceICDPinSignalAssociation pinSignalAsso : matchingICD.getICDUsageDefinition()
				.getPinSignalAssociations()) {
			Map<String, IICDAssociatedSignal> signalNameToSignalMap = new HashMap<>();
			for (IICDAssociatedSignal signal : pinSignalAsso.getICDAssociatedSignals()) {
				signalNameToSignalMap.put(signal.getNetName(), signal);
			}
			icdPinNameToSignalMap.put(pinSignalAsso.getPinName(), signalNameToSignalMap);
		}
		return icdPinNameToSignalMap;
	}

	public static List<IDynamicGfx> updateNetTraces(IPinList currentSchemDevice, IDeviceICD currentICD,
			ISchemDiagram diagram, @Nullable List<Pair<ILocation, String>> pinAbsLocationInfo)
	{
		return updateNetTraces(currentSchemDevice, currentICD, diagram, pinAbsLocationInfo, false);
	}

	public static List<IDynamicGfx> updateNetTraces(IPinList currentSchemPinlist, IDeviceICD currentICD,
			ISchemDiagram diagram, @Nullable List<Pair<ILocation, String>> pinAbsLocationInfo,
			boolean placingBackshellTerm)
	{
		return determineInterconnectionStrategy(diagram).updateNetTraces(currentSchemPinlist, currentICD, diagram,
				pinAbsLocationInfo, placingBackshellTerm);
	}

	@NotNull public static Set<String> getAllPropertyNames(@Nullable IPropertiedObject propertiedObj)
	{
		Set<String> libDevProps = new HashSet<>();
		if (propertiedObj != null) {
			for (IProperty prop : propertiedObj.getProperties()) {
				libDevProps.add(prop.getName().toLowerCase(Locale.ENGLISH));
			}
		}
		return libDevProps;
	}
}
