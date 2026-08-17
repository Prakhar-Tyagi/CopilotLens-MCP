/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.cof.drawplus.ICompoundDiagramObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectorBase;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.publisher.offPage.IDesignContentToBeCopied;
import chs.publisher.offPage.IDiagramContentToBeCopied;
import chs.servicedoc.schemConnectivity.ConnectedSchemPinsProvider;
import chs.utilities.ListMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ObjectsToToCopyProvider
{

	private Set<IPin> m_allPins;
	private Set<ISharedPin> m_allSharedPins;
	private IDesignContentToBeCopied m_designContentToBeCopied;
	private IPinFilter m_pinFilter;
	private Set<InterestingPinsHolder> m_interestingPinsHolders;

	ObjectsToToCopyProvider(Set<IPin> allPins, IDesignContentToBeCopied designContentToBeCopied,
			IPinFilter pinFilter,
			Set<InterestingPinsHolder> interestingPinsHolders)
	{
		m_allPins = allPins;
		m_allSharedPins = m_allPins
				.stream()
				.map(IPin::getConnectivity)
				.map(IAbstractPin::getSharedPin)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		m_designContentToBeCopied = designContentToBeCopied;
		m_pinFilter = pinFilter;
		m_interestingPinsHolders = interestingPinsHolders;
	}

	void populateDiagramObjectsMap(ListMap<ICompoundDiagramObject, IDiagramObject> pinToDiagramObjectMap)
	{
		ConnectedSchemPinsProvider connectedSchemPinsProvider = new ConnectedSchemPinsProvider();
		for (IPin pin : m_allPins) {
			IDiagramObject parent = pin.getParent();
//			if (parent instanceof IPinList && ((IPinList) parent).getParameterized() == null) {
//				continue;
//			}
			if (pin.isReference()) {
				continue;
			}
			Set<IPin> connectedSchemPins = connectedSchemPinsProvider.getConnectedSchemPins(pin);
			if (!connectedSchemPins.isEmpty()) {
				continue;
			}
			populateForPin(pinToDiagramObjectMap, pin, parent);
		}
	}

	private void populateForPin(ListMap<ICompoundDiagramObject, IDiagramObject> pinToDiagramObjectMap, IPin pin,
			@Nullable IDiagramObject parent)
	{
		for (IDiagramContentToBeCopied diagramContentToBeCopied : m_designContentToBeCopied
				.getDiagramContentToBeCopied()) {
			Set<IDiagramObject> diagramObjects = diagramContentToBeCopied.getDiagramObjects();
			for (IDiagramObject diagramObject : diagramObjects) {
				ILogicObject fetchObjectConnectivity = ((IConnectivityRef) diagramObject).getConnectivity();
				if (fetchObjectConnectivity instanceof chs.cof.logical.cable.IPinList &&
						diagramObject instanceof IPinList) {
					assert parent != null;
					IPinList fetchedDiagramObject = (IPinList) diagramObject;
					Collection<IPin> pins = fetchedDiagramObject.getPins();
					if (isSameDevice(pins, fetchObjectConnectivity, pin)) {
						pinToDiagramObjectMap.add(pin, diagramObject);
					}
					else if (isCorrectMate(fetchedDiagramObject, fetchObjectConnectivity, pin, pins)) {
						pinToDiagramObjectMap.add(pin, diagramObject);
						addInterestingPins(pin, fetchObjectConnectivity);
					}
				}
			}
		}
	}

	private void addInterestingPins(IPin pin, ILogicObject fetchObjectConnectivity)
	{
		m_interestingPinsHolders
				.stream()
				.filter(interestingPinsHolder -> interestingPinsHolder.getFilterOfInterest()
						.test(fetchObjectConnectivity))
				.forEach(interestingPinsHolder -> interestingPinsHolder.add(pin));
	}

	private boolean isCorrectMate(IPinList fetchedDiagramObject, ILogicObject fetchedObjectConnectivity,
			IPin activeDiagramSchematicPin, Collection<IPin> pins)
	{
		if (fetchedObjectConnectivity instanceof chs.cof.logical.cable.IPinList) {
			boolean isValidPinList = isValidPinList(fetchedObjectConnectivity);
			if (fetchedObjectConnectivity.getSharedObject() instanceof ISharedPinList || isValidPinList) {
				Set<IPin> connectedDevicePins = pins
						.stream()
						.map(ObjectsToToCopyProvider::getConnectedPins)
						.flatMap(Set::stream)
						.collect(Collectors.toSet());
				boolean anyDevicePinNotInCurrentSelection = isAnyPinListPinNotInCurrentSelection(connectedDevicePins);
				return isTheCorrectMate(fetchedDiagramObject, activeDiagramSchematicPin) &&
						!anyDevicePinNotInCurrentSelection;
			}
		}
		return false;
	}

	private boolean isSameDevice(Collection<IPin> fetchedDiagramObjectPins, ILogicObject fetchedObjectConnectivity,
			IPin activeDiagramSchematicPin)
	{
		IPinList pinList = (IPinList) activeDiagramSchematicPin.getParent();
		assert pinList != null;
		ISharedPinList sharedPinList = pinList.getConnectivity().getSharedPinList();
		boolean isValidSharedPinList =
				sharedPinList instanceof ISharedDevice || sharedPinList instanceof ISharedConnector;
		if (isValidSharedPinList && sharedPinList == fetchedObjectConnectivity.getSharedObject()) {
			boolean anyPinListPinNotInCurrentSelection = isAnyPinListPinNotInCurrentSelection(fetchedDiagramObjectPins);
			return !anyPinListPinNotInCurrentSelection;
		}
		return false;
	}

	private boolean isValidPinList(ILogicObject fetchedObjectConnectivity)
	{
//		return fetchedObjectConnectivity instanceof IDeviceOwned;
		return fetchedObjectConnectivity instanceof IConnectorBase || fetchedObjectConnectivity instanceof IDevice;
	}

	private boolean isAnyPinListPinNotInCurrentSelection(Collection<IPin> devicePins)
	{
		return devicePins
				.stream()
				.filter(m_pinFilter::accept)
				.map(IPin::getConnectivity)
				.map(IAbstractPin::getSharedPin)
				.filter(Objects::nonNull)
				.anyMatch(Predicate.not(m_allSharedPins::contains));
	}

	private boolean isTheCorrectMate(IPinList fetchedPinListSchematic, IPin devicePinSchematic)
	{
		IAbstractPin devicePinConnectivity = devicePinSchematic.getConnectivity();
		return isCorrectMate(fetchedPinListSchematic, devicePinConnectivity);
	}

	private boolean isCorrectMate(IPinList fetchedPinListSchematic, IAbstractPin devicePinConnectivity)
	{
		return getPinWithMate(fetchedPinListSchematic, devicePinConnectivity) != null;
	}

	@Nullable static IPin getPinWithMate(IPinList fetchedPinListSchematic, ILogicObject devicePinConnectivity)
	{
		for (IPin fetchedPlugSchematicPin : fetchedPinListSchematic.getPins()) {
			Set<IPin> connectedSchematicPins = getConnectedPins(fetchedPlugSchematicPin);
			for (IPin connectedSchematicPin : connectedSchematicPins) {
				IAbstractPin connectedPin = connectedSchematicPin.getConnectivity();
				if (devicePinConnectivity.getSharedObject() == connectedPin.getSharedPin()) {
					return fetchedPlugSchematicPin;
				}
			}
		}
		return null;
	}

	@NotNull private static Set<IPin> getConnectedPins(IPin fetchedPlugSchematicPin)
	{
		ConnectedSchemPinsProvider connectedSchemPinsProvider = new ConnectedSchemPinsProvider();
		Set<IPin> connectedSchematicPins =
				connectedSchemPinsProvider.getConnectedSchemPins(fetchedPlugSchematicPin);
		return connectedSchematicPins;
	}
}
