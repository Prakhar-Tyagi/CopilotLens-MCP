/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListGenerator;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.MatedPinListGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.device.MatedDeviceGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.jack.MatedJackGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.modular.MatedModularConnectorGeneratorProvider;
import chs.cof.drawplus.ICompoundDiagramObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.IUIDObjectCollection;
import chs.publisher.offPage.IDesignContentToBeCopied;
import chs.publisher.offPage.ISelectionForFetch;
import chs.utilities.ListMap;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generates mated connectors for pins in the selection in the current diagram This is based on the content that is
 * fetched from the fetch action
 */
class MatedPinListsGenerator
{

	private Set<IPin> pins;
	private ListMap<ICompoundDiagramObject, IDiagramObject> contentForWhichCopyIsNotNeeded;
	private IMessageCollectorAndReporter messageReporter;
	private ISchemDiagram activeDiagram;
	private IPinFilter pinFilter;
	private Set<InterestingPinsHolder> interestingPinsHolders;

	MatedPinListsGenerator(ISelectionForFetch selection, IMessageCollectorAndReporter messageReporter,
			ISchemDiagram activeDiagram, IPinFilter pinFilter)
	{
		pins = new HashSet<>();
		selection.getPins()
				.stream()
				.filter(IPin.class::isInstance)
				.map(IPin.class::cast)
				.filter(pin -> pin.getConnectivity().getSharedPin() != null)
				.forEach(pins::add);
		selection.getPinLists()
				.stream()
				.flatMap(p -> p.getPins().stream())
				.filter(pin -> pin.getConnectivity().getSharedPin() != null)
				.forEach(pins::add);
		Set<IPin> otherPins = pins
				.stream()
				.map(IPin::getParent)
				.filter(IPinList.class::isInstance)
				.map(IPinList.class::cast)
				.map(IPinList::getAllPins)
				.flatMap(IUIDObjectCollection::stream)
				.filter(IPin.class::isInstance)
				.map(IPin.class::cast)
				.filter(pin -> pin.getConnectivity().getSharedPin() != null)
				.filter(pinFilter::accept)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		pins.addAll(otherPins);
		contentForWhichCopyIsNotNeeded = new ListMap<>();
		this.messageReporter = messageReporter;
		this.activeDiagram = activeDiagram;
		this.pinFilter = pinFilter;
		setUpInterestingPinHolders();
	}

	private void setUpInterestingPinHolders()
	{
		Predicate<ILogicObject> isAModularConnector = this::isAModularConnector;
		Predicate<ILogicObject> isADevice = mate -> mate instanceof IDevice;
		Predicate<ILogicObject> isAJack = mate -> mate instanceof IJackConnector;
		InterestingPinsHolder devicesPinsHolder = new InterestingPinsHolder(isADevice,
				new MatedDeviceGeneratorProvider(messageReporter, activeDiagram));
		InterestingPinsHolder modularPinsHolder =
				new InterestingPinsHolder(isAModularConnector,
						new MatedModularConnectorGeneratorProvider(messageReporter, activeDiagram));
		InterestingPinsHolder jackPinsHolder =
				new InterestingPinsHolder(isAJack, new MatedJackGeneratorProvider(messageReporter, activeDiagram));
		InterestingPinsHolder otherPinsHolder =
				new InterestingPinsHolder(Predicate.not(isAModularConnector)
						.and(Predicate.not(isADevice))
						.and(Predicate.not(isAJack)),
						new MatedPinListGeneratorProvider(messageReporter, activeDiagram));
		interestingPinsHolders = new LinkedHashSet<>();
		interestingPinsHolders.add(devicesPinsHolder);
		interestingPinsHolders.add(modularPinsHolder);
		interestingPinsHolders.add(jackPinsHolder);
		interestingPinsHolders.add(otherPinsHolder);
	}

	@NotNull
	List<IDiagramObject> getContentForWhichCopyIsNotNeeded(IDesignContentToBeCopied designContentToBeCopied)
	{
		ObjectsToToCopyProvider objectsToToCopyProvider =
				new ObjectsToToCopyProvider(pins, designContentToBeCopied, pinFilter, interestingPinsHolders);
		ListMap<ICompoundDiagramObject, IDiagramObject> pinToDiagramObjectMap = new ListMap<>();
		objectsToToCopyProvider.populateDiagramObjectsMap(pinToDiagramObjectMap);
		contentForWhichCopyIsNotNeeded.addAll(pinToDiagramObjectMap);
		List<IDiagramObject> noCopyNeeded = pinToDiagramObjectMap
				.values()
				.stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
		return noCopyNeeded;
	}

	private boolean isAModularConnector(ILogicObject fetchObjectConnectivity)
	{
		return fetchObjectConnectivity instanceof IConnector &&
				(((IConnector) fetchObjectConnectivity).isModularParent() ||
						((IConnector) fetchObjectConnectivity).isModularChild());
	}

	void generateMatedPinLists()
	{
		interestingPinsHolders.forEach(this::generateMatedPinLists);
	}

	private void generateMatedPinLists(InterestingPinsHolder interestingPinsHolder)
	{
		IMatedPinListGeneratorProvider mateGeneratorProvider = interestingPinsHolder.getMatedPinListGeneratorProvider();
		Set<IPin> pinsOfInterest = interestingPinsHolder.getPinsOfInterest();
		generateMatedPinLists(pinsOfInterest, mateGeneratorProvider);
	}

	private void generateMatedPinLists(Set<IPin> pinListSchematicPins,
			IMatedPinListGeneratorProvider matedPinListGeneratorProvider)
	{
		Map<IPin, IPin> params = pinListSchematicPins
				.stream()
				.map(this::createPinPair)
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<IPin, IPin> sortedPins = matedPinListGeneratorProvider.getPinPairTransformer().apply(params);
		Map<IUID, Map<IPin, IPin>> sortedGroups = new LinkedHashMap<>();
		for (Map.Entry<IPin, IPin> entry : sortedPins.entrySet()) {
			IUID uid = getUID(entry);
			sortedGroups.putIfAbsent(uid, new LinkedHashMap<>());
			sortedGroups.get(uid).put(entry.getKey(), entry.getValue());
		}
		for (IUID iuid : sortedGroups.keySet()) {
			generateMatedPinList(matedPinListGeneratorProvider, sortedGroups.get(iuid));
		}
	}

	@Nullable private IUID getUID(Map.Entry<IPin, IPin> param)
	{
		chs.cof.logical.cable.IPinList owner = IMatedPinListGeneratorProvider.getOwner(param);
		if (owner.getSharedPinList() != null) {
			return owner.getSharedPinList().getUID();
		}
		return owner.getUID();
	}

	@Nullable private Map.Entry<IPin, IPin> createPinPair(IPin schematicPin)
	{
		List<IDiagramObject> mappedDiagramObjects =
				contentForWhichCopyIsNotNeeded.get(schematicPin);
		IPinList schematicDevice = (IPinList) schematicPin.getParent();
		assert schematicDevice != null;
		chs.cof.logical.cable.IPinList connectivity = schematicDevice.getConnectivity();
		boolean isValidPinList = isValidPinList(connectivity);
		if (isValidPinList && connectivity.getSharedPinList() != null) {
			ILogicObject cablePin =
					((IConnectivityRef) schematicPin).getConnectivity();
			Optional<IDiagramObject> fetchedDiagramObject = mappedDiagramObjects
					.stream()
					.filter(this::isValidMate)
					.findFirst();

			Optional<Map.Entry<IPin, IPin>> entry = mappedDiagramObjects
					.stream()
					.filter(this::isValidMate)
					.map(d -> (IPinList) d)
					.map(fetchedSchematicPinList -> ObjectsToToCopyProvider
							.getPinWithMate(fetchedSchematicPinList, cablePin))
					.filter(Objects::nonNull)
					.map(fetchSchematicPin -> Map.entry(fetchSchematicPin, schematicPin))
					.findFirst();
			if (entry.isPresent()) {
				return entry.get();
			}

//			if (fetchedDiagramObject.isPresent()) {
//				IPinList fetchedSchematicPinList = (IPinList) fetchedDiagramObject.get();
//				//@@TODO m:n support
//				IPin fetchSchematicPin = getMappedPin(cablePin, fetchedSchematicPinList);
//				if (fetchSchematicPin == null) {
//					return null;
//				}
//				return Map.entry(fetchSchematicPin, schematicPin);
//			}
		}
		return null;
	}

	private boolean isValidMate(IDiagramObject d)
	{
		ILogicObject connectivity = ((IConnectivityRef) d).getConnectivity();
		return connectivity instanceof IPlugConnector || connectivity instanceof IJackConnector ||
				connectivity instanceof IDevice;
	}

	private boolean isValidPinList(chs.cof.logical.cable.IPinList connectivity)
	{
//		return connectivity instanceof IDevice;
		return true;
	}

	private void generateMatedPinList(IMatedPinListGeneratorProvider matedPinListGeneratorProvider,
			Map<IPin, IPin> pinPairs)
	{
		IMatedPinListGenerator mateGenerator = matedPinListGeneratorProvider.getMatedPinListGenerator(pinPairs);
		mateGenerator.generateMatedPinLists();
	}

//	private boolean isExistingInstanceOnTheActiveDiagram(ILogicObject fetchedObjectConnectivity,
//			IPin activeDiagramSchematicPin)
//	{
//		IPinList pinList = (IPinList) activeDiagramSchematicPin.getParent();
//		assert pinList != null;
//		ISharedPinList sharedPinList = pinList.getConnectivity().getSharedPinList();
//		ISharedObject sharedObject = fetchedObjectConnectivity.getSharedObject();
//		if (sharedPinList instanceof ISharedDevice && sharedPinList == sharedObject) {
//			return true;
//		}
//		if (fetchedObjectConnectivity instanceof chs.cof.logical.cable.IPinList) {
//			if (sharedObject instanceof ISharedConnector || fetchedObjectConnectivity instanceof IDeviceOwned) {
//				IBaseDevice owner = ((IDeviceOwned) fetchedObjectConnectivity).getOwner();
//				if (owner != null) {
//					ISharedPinList sharedOwnerPinList = owner.getSharedPinList();
//					return sharedOwnerPinList != null && sharedOwnerPinList == sharedPinList;
//				}
//			}
//		}
//		return false;
//	}
}
