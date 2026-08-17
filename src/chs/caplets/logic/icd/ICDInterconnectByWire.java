package chs.caplets.logic.icd;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDNetCableElement;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.wdg.IGeneratedConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.ILocation;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.view.utils.ConductorRouteActionHelper;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ICDInterconnectByWire extends ICDSingleEndedConnectStrategy
{

	ICDInterconnectByWire(@NotNull PersistenceHandler persistenceHandler)
	{
		super(persistenceHandler);
	}

	@NotNull @Override protected String getConductorType()
	{
		return ISharedConductor.WIRE_TYPE;
	}

	@Override protected Class<? extends IConductor> getCableConductorType()
	{
		return IWireConductor.class;
	}

	@Override
	protected IConductor constructNewCableConductor(ISchemDiagram diagram, IICDAssociatedSignal associatedSignal)
	{
		IWireConductor wire = FactoryMgr.getCablePropertiedFactory().createWireConductor(FactoryMgr.createUID());
		registerToDesign(diagram, wire);
		ICDUtils.setSourceICDSignal(wire, associatedSignal.getNetName());
		return wire;
	}

	protected IConductor getCableConductorToJoinExistingConductor(ISchemDiagram diagram,
			IICDAssociatedSignal associatedSignal,
			chs.cof.logical.schem.IConductor existingNet)
	{
		return constructNewCableConductor(diagram, associatedSignal);
	}

	protected boolean isDanglingSegmentAvailableForJoining(ISegment segment)
	{
		return segment.getConductor().getConnectivity().getNumPins() < 2;
	}

	protected boolean isConductorValidToProcessForConnection(IConductor connectivity, String signalName)
	{
		String associatedSignalName = ICDUtils.getAssociatedSignalNameForConductor(connectivity, isWiringAbstraction());
		return associatedSignalName != null && associatedSignalName.equals(signalName);
	}

	@Override protected boolean hasPlacingPinAlreadyGotTheSignal(IPin placingPin, IICDAssociatedSignal signal,
			@Nullable IAbstractPin placedPin)
	{
		if (StringUtils.isBlank(signal.getSignalGroupPath())) {
			return false;
		}
		// in case of wiring abstraction, single ended signals get created to avoid splice creation
		// in such cases, we should avoid connecting the placing pin already having the signal, as it just results in adding more single ended signals to the pin
		final Predicate<IConductor> condMatch = cond -> (signal.getNetName()
				.equalsIgnoreCase(ICDUtils.getAssociatedSignalNameForConductor(cond, isWiringAbstraction())));
		return placingPin.getConductors().stream()
				.filter(conductor -> conductor.getPins().size() < 2) // to check if it is a dangling conductor
				.map(chs.cof.logical.schem.IConductor::getConnectivity)
				.filter(cond -> cond instanceof IGeneratedConductor)
				.filter(cond -> cond.getMulticore() != null)
				.anyMatch(condMatch);
	}

	@NotNull @Override
	protected Set<chs.cof.logical.schem.IConductor> constructCondAndConnectToPin(IPin placedPin, IPin placingPin,
			ISchemDiagram diagram,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		Set<chs.cof.logical.schem.IConductor> newSchemConductors = new HashSet<>();
		Set<IConductor> conductors = getConnectedConductors(placingPin, placedPin);
		for (IConductor conductor : conductors) {
			newSchemConductors.add(constructSchemCondAndConnectToPin(placedPin, placingPin, diagram, conductor));
		}

		if (conductors.isEmpty()) {
			newSchemConductors
					.addAll(super.constructCondAndConnectToPin(placedPin, placingPin, diagram, iicdAssociatedSignal));
		}
		return newSchemConductors;
	}

	@Override protected void createDanglingConductors(ISchemDiagram diagram)
	{
		for (Map.Entry<IPin, Set<Pair<IPin, IICDAssociatedSignal>>> danglingPinEntry : multicoreContext
				.getDanglingConnections()
				.entrySet()) {
			IPin pin = danglingPinEntry.getKey();
			for (Pair<IPin, IICDAssociatedSignal> signalPair : danglingPinEntry.getValue()) {
				createDangling(diagram, pin, signalPair);
			}
		}
	}

	private void createDangling(ISchemDiagram diagram, IPin placingPin, Pair<IPin, IICDAssociatedSignal> signalPair)
	{
		if (hasPlacingPinAlreadyGotTheSignal(placingPin, signalPair.getValue(), null)) {
			return;
		}

		if (!placingPin.getConductors().isEmpty()) {
			return;
		}

		ILocation danglingLocation = calculateDanglingLocation(placingPin,
				ConductorRouteActionHelper.SINGLE_ENDED_CONDUCTOR_LENGTH);
		if (danglingLocation == null) {
			return;
		}

		IPin placedPin = signalPair.getKey();
		Set<IConductor> connectedWires = getConnectedConductors(placingPin, placedPin);

		for (IConductor connectedWire : connectedWires) {
			chs.cof.logical.schem.IConductor conductor =
					getNewSchemConductor(diagram, placedPin.getAbsLocation(), placingPin.getAbsLocation(),
							connectedWire);
			ISegment newSegment = (ISegment) conductor.getSegments().iterator().next();

			newSegment.connectPin(placingPin);
			addForRouting(placingPin, newSegment);

			newSegment.connectPin(placedPin);
			addForRouting(placedPin, newSegment);
		}

		if (connectedWires.isEmpty()) {
			IConductor connectedWire = constructNewCableConductor(diagram, signalPair.getValue());
			chs.cof.logical.schem.IConductor conductor =
					getNewSchemConductor(diagram, danglingLocation, placingPin.getAbsLocation(), connectedWire);
			ISegment newSegment = (ISegment) conductor.getSegments().iterator().next();

			newSegment.connectPin(placingPin);
			addForRouting(placingPin, newSegment);
			addNewConductorToContext(signalPair.getValue(), conductor);
		}
	}

	private Set<IConductor> getConnectedConductors(IPin placingPin, IPin placedPin)
	{
		IAbstractPin placingCablePin = placingPin.getConnectivity();
		IAbstractPin placedCablePin = placedPin.getConnectivity();

		Set<IConductor> placingPinConnectedConductors = placingPin.getConductors().stream()
				.map(chs.cof.logical.schem.IConductor::getConnectivity)
				.collect(Collectors.toSet());
		return placingCablePin.getConductorsOfType(IWireConductor.class).stream()
				.filter(wire -> !placingPinConnectedConductors.contains(wire))
				.filter(wire -> wire.getPins().stream().anyMatch(pin -> pin == placedCablePin))
				.collect(Collectors.toSet());
	}

	@NotNull @Override IPlacingPinController getPlacingPinController(IPin placingPin,
			IPin placingRefPin, IICDAssociatedSignal iicdAssociatedSignal)
	{
		return new IPlacingPinController()
		{
			@Override public boolean proceedToJoin(Set<IPin> connectedPins)
			{
				return !connectedPins.contains(placingPin);
			}

			@Override public ILocation getPlacingPinReferenceLocation()
			{
				return placingRefPin.getAbsLocation();
			}

			@Override public boolean canConnect(IPin placedPin)
			{
				String netName = iicdAssociatedSignal.getNetName();
				if (multicoreContext.getSignalConnectionCount(netName) <= 2) {
					return true;
				}

				if (multicoreContext.alreadyHasDanglingConnection(placedPin, iicdAssociatedSignal)) {
					return false;
				}

				if (multicoreContext.hasAlreadyVisitedConnection(placedPin, iicdAssociatedSignal)) {
					return false;
				}

				IPinList placedPinList = getPinList(placedPin);
				IPinList placingPinList = getPinList(placingPin);
				if (canReuseExistingConnectedMC(placedPinList, placingPinList)) {
					return true;
				}

				Set<IPinList> targetPinLists = CollectionUtils.createSetNoNulls(placedPinList, placingPinList);
				return !(doesMulticoreConnectToMultiplePLs(placedPinList, targetPinLists) ||
						doesMulticoreConnectToMultiplePLs(placingPinList, targetPinLists));
			}

			private boolean canReuseExistingConnectedMC(@Nullable IPinList placedPinList,
					@Nullable IPinList placingPinList)
			{
				Set<IMulticore> multicoresBetweenPinlists = new HashSet<>();
				if (placedPinList != null && placingPinList != null) {
					chs.cof.logical.cable.IPinList placedCablePL = placedPinList.getConnectivity();
					IDevice placedDevice = getOwnerDevice(placedCablePL);
					IDevice placingDevice = getOwnerDevice(placingPinList.getConnectivity());

					if (placingDevice != null && placedDevice != null) {
						Set<IDeviceICD> matchingPlacedICDs = ICDUtils.getMatchingICDs(placedDevice);
						Set<IDeviceICD> matchingPlacingICDs = ICDUtils.getMatchingICDs(placingDevice);
						if (!matchingPlacedICDs.isEmpty() && !matchingPlacingICDs.isEmpty()) {
							return false;
						}
					}
					ILogicDesign design = CommonUtils.cast(placedCablePL.getDesign(), ILogicDesign.class);
					if (design == null) {
						return false;
					}
					Set<List<IICDNetCableElement>> signalGroupPaths =
							design.getDesignICDContainer().getEquivalentSignalGroupPaths(iicdAssociatedSignal);
					for (IPin pin : placedPinList.getPins()) {
						for (chs.cof.logical.schem.IConductor conductor : pin.getConductors()) {
							IConductor connectedCond = conductor.getConnectivity();
							IMulticore connectedMulticore = connectedCond.getMulticore();
							if (CollectionUtils.containsAtLeastOne(conductor.getPins(), placingPinList.getPins())) {
								for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
									calculateMulticoresBetweenPinlists(multicoresBetweenPinlists, signalGroupPath,
											connectedMulticore);
								}
							}
						}
					}
				}

				return multicoresBetweenPinlists.size() == 1;
			}

			private void calculateMulticoresBetweenPinlists(@NotNull Set<IMulticore> multicoresBetweenPinlists,
					@NotNull List<IICDNetCableElement> cableHierarchy, @Nullable IMulticore mc)
			{
				IMulticore connectedMulticore = mc;
				while (connectedMulticore != null) {
					String multicoreSource = getMulticoreSource(connectedMulticore);
					if (!multicoreSource.isEmpty()) {
						for (IICDNetCableElement cableSourceNamePair : cableHierarchy) {
							if (multicoreSource.equalsIgnoreCase(cableSourceNamePair.getOriginalName())) {
								multicoresBetweenPinlists.add(connectedMulticore.getRootMulticore());
								break;
							}
						}
					}
					connectedMulticore = connectedMulticore.getParent();
				}
			}

			@Override public boolean canConnect(ISegment segment)
			{
				String netName = iicdAssociatedSignal.getNetName();
				if (multicoreContext.getSignalConnectionCount(netName) <= 2) {
					return true;
				}

				Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond = new HashSet<>();
				Set<IPinList> placedPinlists = new HashSet<>();
				for (IPin pin : segment.getConductor().getPins()) {
					multicoreContext.registerVisitedConnections(pin, iicdAssociatedSignal);
					connectedCablePlsToCond.add(getPinOwner(pin.getConnectivity()));
					placedPinlists.add(getPinList(pin));
				}

				IPinList placingPL = getPinList(placingPin);
				if (placingPL == null) {
					return false;
				}

				for (IPinList placedPL : placedPinlists) {
					if (placedPL.getConnectivity() != placingPL.getConnectivity()) {
						if (canReuseExistingConnectedMC(placedPL, placingPL)) {
							return true;
						}
					}
				}

				connectedCablePlsToCond.add(getPinOwner(placingPin.getConnectivity()));
				placedPinlists.add(placingPL);

				chs.cof.logical.schem.IConductor conductor = segment.getConductor();
				IConductor connectivity = conductor.getConnectivity();
				IMulticore multicore = connectivity.getMulticore();
				if (multicore != null) {
					Set<chs.cof.logical.cable.IPinList> connectedCablePLsToMC =
							getPinListsConnectedToMulticore(multicore, false);
					connectedCablePLsToMC.addAll(connectedCablePlsToCond);
					if (connectedCablePLsToMC.size() > 2) {
						return false;
					}
				}

				for (IPinList targetPinList : placedPinlists) {
					if (doesMulticoreConnectToMultiplePLs(targetPinList, placedPinlists)) {
						return false;
					}
				}

				return true;
			}

			@Override public boolean canSplit(ISegment segment)
			{
				// do not allow splitting a segment that is connected to itself
				if (segment.getConductor().getPins().contains(placingPin)) {
					return false;
				}
				// disallow splitting if segment is ending on an incorrect icd termination pin
				String signalName = ICDUtils.getAssociatedSignalNameForConductor(
						segment.getConductor().getConnectivity(), isWiringAbstraction()
				);
				if (signalName != null) {
					for (IPin pin : segment.getConductor().getPins()) {
						//IPin transformedPin = UpdateICDDisconnectConductorStrategy.transformToDevicePinIfRequired(pin);
						// transformation done in below call
						IDeviceICD icd = ICDUtils.getICDAssociatedToPin(pin);
						if (icd != null) {
							if (!ICDUtils.isSignalValidForThisPin(pin, icd, signalName)) {
								return false;
							}
						}
					}
				}
				for (IPin pin : segment.getConductor().getPins()) {
					multicoreContext.registerVisitedConnections(pin, iicdAssociatedSignal);
				}

				String signalGroupPath = iicdAssociatedSignal.getSignalGroupPath();
				if (signalGroupPath != null && !StringUtils.isBlank(signalGroupPath)) {
					return false;
				}

				chs.cof.logical.schem.IConductor conductor = segment.getConductor();
				IConductor connectivity = conductor.getConnectivity();
				return connectivity.getMulticore() == null;
			}

			@Override public void registerDanglingConnection(IPin placedPin)
			{
				multicoreContext.registerDanglingConnection(placingPin, placedPin, iicdAssociatedSignal);
			}

			private boolean doesMulticoreConnectToMultiplePLs(@Nullable IPinList ownerPinList,
					Set<IPinList> targetPinLists)
			{
				Set<chs.cof.logical.schem.IConductor> connectedConductors = getConnectedInnerCores(ownerPinList);
				return canHaveMultipleTerminations(connectedConductors, targetPinLists);
			}

			@NotNull
			private Set<chs.cof.logical.schem.IConductor> getConnectedInnerCores(@Nullable IPinList ownerPinList)
			{
				Set<chs.cof.logical.schem.IConductor> connectedConductors = new HashSet<>();
				if (ownerPinList != null) {
					ILogicDesign design =
							CommonUtils.cast(ownerPinList.getConnectivity().getDesign(), ILogicDesign.class);
					if (design == null) {
						return connectedConductors;
					}
					Set<List<IICDNetCableElement>> signalGroupPaths =
							design.getDesignICDContainer().getEquivalentSignalGroupPaths(iicdAssociatedSignal);
					for (IPin pin : ownerPinList.getPins()) {
						for (chs.cof.logical.schem.IConductor conductor : pin.getConductors()) {
							IConductor cableConductor = conductor.getConnectivity();
							IMulticore multicore = cableConductor.getMulticore();
							while (multicore != null) {
								String multicoreSource = getMulticoreSource(multicore);
								if (!multicoreSource.isEmpty()) {
									for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
										for (IICDNetCableElement element : signalGroupPath) {
											if (multicoreSource.equalsIgnoreCase(element.getOriginalName())) {
												connectedConductors.add(conductor);
											}
										}
									}
								}
								multicore = multicore.getParent();
							}
						}
					}
				}
				Set<chs.cof.logical.schem.IConductor> designWideInstances = new HashSet<>();
				for (chs.cof.logical.schem.IConductor conductor : connectedConductors) {
					ISchemDiagram diagram = DiagramHelper.getDiagram(conductor);
					assert diagram != null;
					IUID connectivityUID = conductor.getConnectivityUID();
					assert connectivityUID != null;
					for (IDiagramObject diagramObject : diagram.getRepresentations(connectivityUID)) {
						if (diagramObject instanceof chs.cof.logical.schem.IConductor) {
							designWideInstances.add((chs.cof.logical.schem.IConductor) diagramObject);
						}
					}
				}

				connectedConductors.addAll(designWideInstances);
				return connectedConductors;
			}

			private boolean canHaveMultipleTerminations(Set<chs.cof.logical.schem.IConductor> connectedConductors,
					Set<IPinList> targetPinLists)
			{
				Set<IPinList> connectedPinLists = new HashSet<>();
				for (chs.cof.logical.schem.IConductor connectedConductor : connectedConductors) {
					if (doesSignalTerminateAtMoreThanTwoPins(connectedConductor.getConnectivity())) {
						for (IPin pin : connectedConductor.getPins()) {
							connectedPinLists.add(getPinList(pin));
						}
					}
				}

				return connectedPinLists.size() > 2 ||
						(connectedPinLists.size() == 2 && !connectedPinLists.containsAll(targetPinLists));
			}
		};
	}

	@Nullable private IDevice getOwnerDevice(chs.cof.logical.cable.IPinList pinList)
	{
		IHarnessPlugConnector plugConnector = CommonUtils.cast(pinList, IHarnessPlugConnector.class);
		if (plugConnector != null) {
			return CommonUtils.cast(plugConnector.getOwner(), IDevice.class);
		}
		return CommonUtils.cast(pinList, IDevice.class);
	}

	@Override public boolean isWiringAbstraction()
	{
		return true;
	}

	@Override
	protected void updateToNewConnectivity(chs.cof.logical.schem.IConductor conductor, IICDAssociatedSignal signal,
			ISchemDiagram diagram)
	{

	}
}
