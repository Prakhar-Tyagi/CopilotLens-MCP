package chs.caplets.logic.icd;

import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.DeleteHelper;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.icd.IICDNetCableElement;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.ShieldConductor;
import chs.cof.logical.cable.wdg.IGeneratedConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.common.IDesignAbstraction;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ListSet;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.ICDSignalDetailsFinder;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDPinSignalAssociation;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.NodeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UpdateICDPersistenceHandler extends PersistenceHandler
{

	private SetMap<IMulticore, IUID> emptyMulticores;
	private Collection<IUID> orphanedSharedInnercores;
//	private Collection<IUID> updatedSharedMCs;
	private Set<ISharedObject> frozenConductors;

	public UpdateICDPersistenceHandler(@NotNull ISchemDiagram diagram)
	{
		this(diagram, false);
	}
	public UpdateICDPersistenceHandler(@NotNull ISchemDiagram diagram, boolean generateSingleEnded)
	{
		super(diagram, generateSingleEnded, new UpdateICDReporter());
		emptyMulticores = SetMap.createShallowSetMap();
		orphanedSharedInnercores = new HashSet<>();
		frozenConductors = new HashSet<>();
//		updatedSharedMCs = new HashSet<>();
	}

	@Override public void endRouting()
	{
		// Do nothing
	}

	@Override public void endRoutingAll()
	{
		deleteOrphanedObjects();

		//IESCD-3664: compute the affected diagrams for indicator clean-up.
		//and load them before deleting the multi-cores.
		//We are cleaning up the orphaned indicators at load and sync of diagrams
		//now. so no need to load them before-hand to clean then up.
		Set<IMulticore> multicoresToDelete = new HashSet<>();
		for (Map.Entry<IMulticore, Set<IUID>> entry : emptyMulticores.entrySet()) {
			IMulticore multicore = entry.getKey();
			if (multicore.getAllConductorsInHierarchy().isEmpty()) {
				multicoresToDelete.add(multicore);
			}
		}

		DeleteHelper.getInstance().delete(mDiagram, multicoresToDelete, true);

		getLockTracker().unlock();
	}

	@Override public boolean isUpdate()
	{
		return true;
	}

	@Override
	@NotNull
	public Collection<IConductor> disconnectInvalidSignals(IICDSignalSourceSchemPinlist currentSchemDevice,
			IDeviceICD currentICD, Collection<ISegment> disconnectedSegments)
	{
		IPinList schemDevice = currentSchemDevice.getSchemDevice();
		if (schemDevice == null) {
			return Collections.emptyList();
		}
		SetMap<String, String> associatedSignalsMap = new SetMap<>();
		Map<String, Boolean> isSignalShieldTypeMap = new HashMap<>();
		for (IDeviceICDPinSignalAssociation pinSignalAssociation : currentICD.getICDUsageDefinition()
				.getPinSignalAssociations()) {
			for (IICDAssociatedSignal associatedSignal : pinSignalAssociation.getICDAssociatedSignals()) {
				associatedSignalsMap.add(pinSignalAssociation.getPinName(), associatedSignal.getNetName());
				isSignalShieldTypeMap.put(associatedSignal.getNetName(), associatedSignal.isShieldWire());
			}
		}
		Collection<IConductor> disconnectedConductors = new HashSet<>();

		// disconnect invalid signals on device/attached harness connector pins
		for (IPin pin : schemDevice.getPins()) {
			IAbstractPin cablePin = pin.getConnectivity();
			String pinName = cablePin.getName();
			Set<String> associatedSignals = associatedSignalsMap.pullReadOnlySafeSet(pinName);
			pin = currentSchemDevice.getEquivalentICDMatchingSignalPin(pin);
			if (pin == null) {
				continue;
			}
			cablePin = pin.getConnectivity();
			Collection<IConductor> conductorsToDisconnect = new HashSet<>();
			List<ISegment> segmentsToDelete = new ArrayList<>();
			calculateConductorsToDisconnect(currentICD, pin, associatedSignals, isSignalShieldTypeMap,
					conductorsToDisconnect,
					segmentsToDelete);

			disconnectedConductors.addAll(conductorsToDisconnect);
			disconnectedSegments.addAll(segmentsToDelete);
			// should similar thing be done for shield / backshell pins
			performNecessaryDisconnections(pin, cablePin, conductorsToDisconnect);
		}

		isSignalShieldTypeMap.clear();
		// disconnect invalid signals on backshell terminations
		SetMap<IBackshellTermination, IICDAssociatedSignal> logicBSTermAssociatedSignalsMap =
				getICDSignalsForLogicBackshellTerm(schemDevice, currentICD);

		for (IPin pin : getSchemBSTerminations(schemDevice)) {
			IBackshellTermination cablePin = CommonUtils.cast(pin.getConnectivity(), IBackshellTermination.class);
			if (cablePin != null && logicBSTermAssociatedSignalsMap.contains(cablePin)) {
				Set<String> associatedSignals = new HashSet<>();

				for (IICDAssociatedSignal signal : logicBSTermAssociatedSignalsMap.pullReadOnlySafeSet(cablePin)) {
					associatedSignals.add(signal.getNetName());
					isSignalShieldTypeMap.put(signal.getNetName(), signal.isShieldWire());
				}
				Collection<IConductor> conductorsToDisconnect = new HashSet<>();
				List<ISegment> segmentsToDelete = new ArrayList<>();
				calculateConductorsToDisconnect(currentICD, pin, associatedSignals, isSignalShieldTypeMap,
						conductorsToDisconnect,
						segmentsToDelete);
				disconnectedConductors.addAll(conductorsToDisconnect);
				performNecessaryDisconnections(pin, cablePin, conductorsToDisconnect);
			}
		}

		// we have decided to delete these instead of just disconnecting
		// also we have to delete those conductors that are valid but do not end on a correct ICD pin
		return disconnectedConductors;
	}

	@Override
	public void removeSignalsFromInvalidMulticores(IPinList currentSchemDevice, IDeviceICD currentICD,
			ICDMulticoreContext multicoreContext, boolean isWiringAbstraction)
	{
		SetMap<String, IICDAssociatedSignal> associatedSignalsMap = new SetMap<>();
		for (IDeviceICDPinSignalAssociation pinSignalAssociation : currentICD.getICDUsageDefinition()
				.getPinSignalAssociations()) {
			for (IICDAssociatedSignal associatedSignal : pinSignalAssociation.getICDAssociatedSignals()) {
				associatedSignalsMap.add(pinSignalAssociation.getPinName(), associatedSignal);
			}
		}

		List<IPin> schemPins = new ArrayList<>();
		schemPins.addAll(currentSchemDevice.getPins().stream().collect(Collectors.toList()));
		for (IPinList attachedObject : currentSchemDevice.getAttachedPinListObjects()) {
			schemPins.addAll(attachedObject.getPins().stream().collect(Collectors.toList()));
		}

		Collection<chs.cof.logical.cable.IConductor> conductorsToBeUpdated = new HashSet<>();
		for (IPin pin : schemPins) {
			String pinName = ICDUtils.transformToDevicePinIfRequired(pin).getConnectivity().getName();
			Set<IICDAssociatedSignal> associatedSignals = associatedSignalsMap.pullReadOnlySafeSet(pinName);
			for (IConductor conductor : pin.getConductors()) {
				IICDAssociatedSignal signalOfInterest = getICDSignal(conductor, associatedSignals, isWiringAbstraction);
				Boolean cableDefinitionMatch = true;
				if (signalOfInterest != null) {
					cableDefinitionMatch = doesCableDefinitionMatch(conductor, signalOfInterest);
				}
				ISharedConductor sharedConductor = conductor.getConnectivity().getSharedConductor();
				if (sharedConductor != null && sharedConductor.isFrozen()) {
					ISharedMulticore sharedMulticore = sharedConductor.getMulticore();
					if (!cableDefinitionMatch) {
						if (sharedMulticore != null && sharedMulticore.isFrozen()) {
							frozenConductors.add(sharedMulticore);
						}
						else {
							frozenConductors.add(sharedConductor);//collect frozen objects
						}
					}
					continue;
				}

				if (signalOfInterest != null && !cableDefinitionMatch) {
					multicoreContext.registerPotentialCable(signalOfInterest, conductor);
					conductorsToBeUpdated.add(conductor.getConnectivity());
				}
			}
		}
		collectEmptyMulticores(conductorsToBeUpdated);
	}

	@Nullable public Set<ISharedObject> getFrozenConductors()
	{
		return frozenConductors;
	}

	@NotNull @Override public ListSet<IMulticore> getConnectedMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType)
	{
		ListSet<IMulticore> multicores = getConnectedMulticores(pinList, schemCond, signal, true, conductorType);
		return getTopDownMatchingMulticores(signal, multicores);
	}

	@NotNull @Override public ListSet<IMulticore> getDesignMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType)
	{
		ListSet<IMulticore> multicores = getDesignMulticores(pinList, schemCond, signal, true, conductorType);
		return getTopDownMatchingMulticores(signal, multicores);
	}

	@NotNull
	private ListSet<IMulticore> getTopDownMatchingMulticores(IICDAssociatedSignal signal,
			Collection<IMulticore> multicores)
	{
		Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(signal);
		ListSet<IMulticore> matchingMCs = new ListSet<>();
		for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
			matchingMCs.addAll(getMulticoresMatchingHierarchy(multicores, signalGroupPath));
		}

		return matchingMCs;
	}

	@NotNull
	private ListSet<IMulticore> getMulticoresMatchingHierarchy(@NotNull Collection<IMulticore> multicores,
			@NotNull List<IICDNetCableElement> hierarchy)
	{
		List<IICDNetCableElement> cableHierarchy = new ArrayList<>();
		for (IICDNetCableElement stringPair : hierarchy) {
			cableHierarchy.add(0, stringPair);
		}

		ListSet<IMulticore> reusableMulticores = new ListSet<>();
		for (IMulticore multicore : multicores) {
			IMulticore reusableChildMulticore = null;
			Iterator<IICDNetCableElement> mcSourcePath = cableHierarchy.iterator();
			List<IMulticore> multicoreList = Collections.singletonList(multicore);
			while (mcSourcePath.hasNext()) {
				IICDNetCableElement mcSourcePair = mcSourcePath.next();
				IMulticore childMulticore = findMatchingMulticore(mcSourcePair, multicoreList);
				if (childMulticore == null) {
					break;
				}
				reusableChildMulticore = childMulticore;
				multicoreList = reusableChildMulticore.getMulticoresAsList();
			}
			if (reusableChildMulticore != null) {
				reusableMulticores.add(reusableChildMulticore);
			}
		}
		return reusableMulticores;
	}

	public void collectEmptyMulticores(Collection<chs.cof.logical.cable.IConductor> conductorsToBeUpdated)
	{
		Collection<IMulticore> updatedMulticores = new HashSet<>();
		SharedDetailsLockHelper lockTracker = getLockTracker();

		SetMap<IMulticore, IUID> parentMulticores = SetMap.createShallowSetMap();
		for (chs.cof.logical.cable.IConductor conductor : conductorsToBeUpdated) {
			IShieldConductor shieldConductor = CommonUtils.cast(conductor, IShieldConductor.class);
			if (shieldConductor == null) {
				IMulticore multicore = conductor.getMulticore();
				if (multicore != null) {
					parentMulticores.add(multicore, conductor.getUID());
					ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
					if (sharedMulticore != null) {
						IUID sharedMulticoreUID = sharedMulticore.getUID();
						if (lockTracker
								.lock(sharedMulticore, true, conductor.getName(), conductor.getSharedConductor())) {
							sharedMulticore = UIDMgr.getObjectOfType(sharedMulticoreUID, ISharedMulticore.class);
							assert sharedMulticore != null;
							try {
								ISharedConductor sharedConductor = conductor.getSharedConductor();
								orphanedSharedInnercores.add(sharedConductor.getUID());
//								updatedSharedMCs.add(sharedMulticoreUID);
								lockTracker.removeMulticoresFromDesigns(sharedConductor.getUID(),
										orphanedSharedInnercores);
								removeSharedConductorMember(sharedConductor, sharedMulticore);
								updatedMulticores.add(multicore.getRootMulticore());
								removeFromMulticore(conductor);
							}
							catch (UserSessionException e) {
								Environment.getExceptionDisplay().displayException(e, "UpdateICDAction failed");
							}
						}
					}
					else {
						removeFromMulticore(conductor);
						updatedMulticores.add(multicore.getRootMulticore());
					}
				}
			}
		}

//		for (IUID iuid : orphanedSharedInnercores) {
//		lockTracker.removeMulticoresFromDesigns(orphanedSharedInnercores, updatedSharedMCs);
//		}

		updatedMulticores.stream()
				.flatMap(rootmc -> rootmc.getAllMulticoresInHierarchy().stream())
				.filter(multicore -> multicore.getAllConductorsInHierarchy().isEmpty())
				.forEach(mc -> emptyMulticores.addAll(mc, parentMulticores.pullReadOnlySafeSet(mc)));
	}

	@Override @NotNull public Collection<ISharedMulticore> getSharedMulticores(IConnectivity connectivity,
			ISharedConductorMgr sharedConductorMgr, String condType)
	{
		return sharedConductorMgr.getSharedMulticores().stream()
				.filter(sharedMulticore -> !sharedMulticore.isFrozen())
				.filter(sharedMulticore -> {
					ISharedMulticore sharedMulticoreParent = sharedMulticore.getParent();
					if (sharedMulticoreParent != null && !(sharedMulticoreParent instanceof ISharedOverbraid)) {
						return false;
					}
					if (sharedMulticoreParent != null /*sharedMulticoreParent instanceof ISharedOverbraid*/) {
						return connectivity.findSharedMulticore(sharedMulticore) == null;
					}
					return connectivity.findSharedMulticore(sharedMulticore.getRootMulticore()) == null;
				})
				.filter(shareMulticore -> areAllConductorsOfMatchingType(shareMulticore, condType))
				.collect(Collectors.toList());
	}

	@Override @NotNull public Collection<ISharedMulticore> getSharedMulticores(@NotNull Collection<ISharedMulticore> sharedMulticores)
	{
		return sharedMulticores.stream()
				.filter(sharedMulticore -> {
					ISharedMulticore sharedMulticoreParent = sharedMulticore.getParent();
					return !(sharedMulticoreParent != null && !(sharedMulticoreParent instanceof ISharedOverbraid));
				})
				.collect(Collectors.toList());
	}

	private void removeSharedConductorMember(ISharedConductor childConductor, ISharedMulticore parentMulticore)
			throws UserSessionException
	{
		parentMulticore.removeConductor(childConductor);
		IUserSession userSession = FactoryMgr.getSystemFactory().getCHSSystem().getUserSession();
		userSession.deleteAssociation("sharedconductormember", new String[]
						{"ref", "sharedmulticore_id"}
				,
				new String[]
						{childConductor.getUID().getString(), parentMulticore.getUID().getString()});
	}

	private void removeFromMulticore(chs.cof.logical.cable.IConductor conductor)
	{
		IMulticore multicore = conductor.getMulticore();
		if (multicore != null) {
			multicore.removeConductor(conductor);
			if (multicore.isPartAssigned()) {
				conductor.assignLibraryPart(null);
			}
		}
	}

	@Nullable private IMulticore findMatchingMulticore(@NotNull IICDNetCableElement mcSourcePair,
			@NotNull List<IMulticore> multicores)
	{
		for (IMulticore multicore : multicores) {
			String multicoreSource = ICDInterconnectStrategy.getMulticoreSource(multicore);
			if (mcSourcePair.getOriginalName().equals(multicoreSource) &&
					compareIndicatorTypes(multicore, mcSourcePair.getType())) {
				return multicore;
			}
		}
		return null;
	}

	@Nullable private IICDAssociatedSignal getICDSignal(IConductor conductor, Set<IICDAssociatedSignal> signalSet,
			boolean isWiringAbstraction)
	{
		chs.cof.logical.cable.IConductor cableConductor = conductor.getConnectivity();
		String topoSignalName = ICDUtils.getAssociatedSignalNameForConductor(cableConductor, isWiringAbstraction);
		if (!StringUtils.isBlank(topoSignalName)) {
			Optional<IICDAssociatedSignal> signalOfInterest = signalSet.stream()
					.filter(signal -> signal.getNetName().equals(topoSignalName))
					.findFirst();
			if (signalOfInterest.isPresent()) {
				return signalOfInterest.get();
			}
		}
		return null;
	}

	private boolean doesCableDefinitionMatch(IConductor conductor, IICDAssociatedSignal signal)
	{
		IMulticore multicore = conductor.getConnectivity().getMulticore();
		String signalGroupPath = signal.getSignalGroupPath();
		if (multicore == null || ICDInterconnectStrategy.getMulticoreSource(multicore).isEmpty()) {
			return signalGroupPath == null || signalGroupPath.isEmpty();
		}

		if (signalGroupPath == null || signalGroupPath.isEmpty()) {
			return false;
		}

		// both multicore and signal group path exist, check if they match
		Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(signal);
		for (List<IICDNetCableElement> path : signalGroupPaths) {
			boolean hierarchyMatch = doesCableHierarchyMatch(multicore, path);
			if (hierarchyMatch) {
				return true;
			}
		}
		return false;
	}

	private boolean doesCableHierarchyMatch(IMulticore mc, @NotNull List<IICDNetCableElement> hierarchy)
	{
		IMulticore multicore = mc;
		for (IICDNetCableElement cableElement : hierarchy) {
			if (multicore == null || multicore instanceof IOverbraid) {
				// some hierarchy is missing on Logic side
				return false;
			}
			if (!cableElement.getOriginalName().equals(ICDInterconnectStrategy.getMulticoreSource(multicore))) {
				return false;
			}
			if (!compareIndicatorTypes(multicore, cableElement.getType())) {
				return false;
			}
			multicore = multicore.getParent();
		}

		// ensure there's no additional hierarchy on Logic side
		return multicore instanceof IOverbraid || multicore == null;
	}

	private boolean doesCableDefinitionMatch(ISharedConductor sharedConductor, IICDAssociatedSignal signal)
	{
		ISharedMulticore sharedMulticore = sharedConductor.getMulticore();
		String signalGroupPath = signal.getSignalGroupPath();
		if (sharedMulticore == null) {
			return signalGroupPath == null || signalGroupPath.isEmpty();
		}

		if (signalGroupPath == null || signalGroupPath.isEmpty()) {
			return false;
		}

		// both multicore and signal group path exist, check if they match
		Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(signal);
		for (List<IICDNetCableElement> path : signalGroupPaths) {
			boolean hierarchyMatch = doesCableHierarchyMatch(sharedMulticore, path);
			if (hierarchyMatch) {
				return true;
			}
		}
		return false;
	}

	private boolean doesCableHierarchyMatch(ISharedMulticore mc, @NotNull List<IICDNetCableElement> hierarchy)
	{
		ISharedMulticore sharedMulticore = mc;
		for (IICDNetCableElement pair : hierarchy) {
			if (sharedMulticore == null || sharedMulticore instanceof IOverbraid) {
				// some hierarchy is missing on Logic side
				return false;
			}
			if (!pair.getOriginalName().equals(ICDInterconnectStrategy.getMulticoreSource(sharedMulticore))) {
				return false;
			}
			if (!compareIndicatorTypes(sharedMulticore, pair.getType())) {
				return false;
			}
			sharedMulticore = sharedMulticore.getParent();
		}

		// ensure there's no additional hierarchy on Logic side
		return sharedMulticore instanceof ISharedOverbraid || sharedMulticore == null;
	}

	@Override @NotNull public Set<ISharedConductor> getMatchingSharedConductors(Set<ISharedConductor> sharedConductors,
			chs.cof.logical.cable.IConductor conductor, IICDAssociatedSignal signal)
	{
		return super.getMatchingSharedConductors(sharedConductors, conductor, signal).stream()
				.filter(sharedConductor -> doesCableDefinitionMatch(sharedConductor, signal))
				.collect(Collectors.toSet());
	}

	private boolean compareIndicatorTypes(IMulticore multicore, String indicatorType)
	{
		return ICDUtils.determineIndicatorType(indicatorType).equals(ICDUtils.determineIndicatorType(multicore));
	}

	private void performNecessaryDisconnections(IPin pin, IAbstractPin cablePin,
			Collection<IConductor> conductorsToDisconnect)
	{
		for (IConductor conductor : conductorsToDisconnect) {
			// disconnect from ICD pin
			NodeHelper.separateConductorAtNode(conductor, pin.getJoint(), FactoryMgr.getCommonFactory(),
					FactoryMgr.getSchemFactory());
			chs.cof.logical.cable.IConductor cableConductor = conductor.getConnectivity();
			if (!ConnectionHelper.hasMultipleConnections(cablePin, pin, cableConductor, conductor)) {
				cablePin.removeConductor(cableConductor);
				cableConductor.removePin(cablePin);
			}
			// LOGIC-5984 and LOGIC-5992 now disconnect from the other end too as this schem conductor is being passed to auto-route
			// and it in turn is creating new schem conductor and the disconnected conductor is not getting deleted
			if (conductor.getConnectivity() instanceof IWireConductor) {
				Optional<IPin> otherEndPin = conductor.getPins().stream().findFirst();
				if (otherEndPin.isPresent()) {
					NodeHelper.separateConductorAtNode(conductor, otherEndPin.get().getJoint(),
							FactoryMgr.getCommonFactory(), FactoryMgr.getSchemFactory());
					IAbstractPin otherEndCablePin = otherEndPin.get().getConnectivity();
					if (!ConnectionHelper
							.hasMultipleConnections(otherEndCablePin, otherEndPin.get(), cableConductor, conductor)) {
						otherEndCablePin.removeConductor(cableConductor);
						cableConductor.removePin(otherEndCablePin);
					}
				}
			}
		}
	}

	private boolean canDisconnectShieldConductor(@NotNull IGeneratedConductor cableConductor, String topoSignalName,
			@NotNull Map<String, Boolean> isSignalShieldTypeMap)
	{
		// shield conductor part of a multicore will ensure that one end of it is a hook-up
		// this check is essential as we do not want to disconnect those shield conductors which are part of connecting pins of ICDs through splices
		return cableConductor instanceof IShieldConductor && !isSignalShieldTypeMap.get(topoSignalName) &&
				(((chs.cof.logical.cable.IConductor) cableConductor).getMulticore() != null);
	}

	private void calculateConductorsToDisconnect(IDeviceICD currentICD, IPin pin, Set<String> associatedSignals,
			@NotNull Map<String, Boolean> isSignalShieldTypeMap, Collection<IConductor> conductorsToDisconnect,
			List<ISegment> segmentsToDelete)
	{
		for (IConductor conductor : pin.getConductors()) {
			IGeneratedConductor cableConductor =
					CommonUtils.cast(conductor.getConnectivity(), IGeneratedConductor.class);
			if (cableConductor != null) {
				String topoSignalName = ICDUtils.getSourceICDSignal(cableConductor);
				if (!StringUtils.isBlank(topoSignalName)) {
					// ICD driven signal but not valid on this pin
					if (!associatedSignals.contains(topoSignalName) ||
							typeDoesNotMatch(isSignalShieldTypeMap, cableConductor, topoSignalName) ||
							canDisconnectShieldConductor(cableConductor, topoSignalName, isSignalShieldTypeMap)) {
						buildDataForDisconnect(pin, conductorsToDisconnect, segmentsToDelete, conductor);
					}
					else {
						if (cableConductor instanceof ShieldConductor) {
							continue;
						}
						// ICD driven signal valid on this pin but ending on incorrect ICD termination
						List<IPin> startPins = conductor.getPins().stream()
								.filter(condPin -> condPin != pin)
								.collect(Collectors.toList());
						if (!startPins.isEmpty() &&
								!doesAnyOfThePinsLeadToAValidPath(startPins, pin, topoSignalName, currentICD)) {
							// none leads to a valid path
							buildDataForDisconnect(pin, conductorsToDisconnect, segmentsToDelete, conductor);
						}
					}
				}
			}
		}
	}

	private boolean typeDoesNotMatch(@NotNull Map<String, Boolean> isSignalShieldTypeMap,
			IGeneratedConductor cableConductor, String topoSignalName)
	{
		return (cableConductor instanceof INetConductor || cableConductor instanceof IWireConductor) &&
				isSignalShieldTypeMap.get(topoSignalName);
	}

	private boolean doesAnyOfThePinsLeadToAValidPath(List<IPin> startPins, IPin pin, String topoSignalName,
			IDeviceICD currentICD)
	{
		Class<? extends chs.cof.logical.cable.IConductor> cableType = INetConductor.class;
		IDesign design = mDiagram.getDesign();
		if (design != null) {
			IDesignAbstraction designAbstraction = design.getDesignAbstraction();
			if (designAbstraction != null && designAbstraction.getAllowAutoCreation()) {
				cableType = IWireConductor.class;
			}
		}
		for (IPin startPin : startPins) {
			if (SignalPathTreeHelper.isPinOnASplittingObject(startPin)) {
				// continue to check if the path ends on a valid ICD pin
				Set<IPin> pinsTraversed = new HashSet<>();
				pinsTraversed.add(pin);
				SignalPathTree tree = new SignalPathTreeHelper(null, Collections.emptySet(), cableType)
						.buildSignalPathTree(startPin, pinsTraversed, null);
				if (doesTreeContainAValidPath(currentICD, tree, topoSignalName)) {
					return true;
				}
			}
			else {
				// if ending on icd but incorrect pin, then clean it up
				IDeviceICD icd = getICDAssociatedToPin(startPin);
				if (icd == null) {
					return true; // ending on a device pin
				}
				if (ICDUtils.isSignalValidForThisPin(startPin, icd, topoSignalName)) {
					return true;
				}
			}
		}
		return false;
	}

	private void buildDataForDisconnect(IPin pin, Collection<IConductor> conductorsToDisconnect,
			List<ISegment> segmentsToDelete, IConductor conductor)
	{
		for (ISegment segment : conductor.getSegmentsOfType(ISegment.class)) {
			if ((segment.getStartJoint() != null && segment.getStartJoint().equals(pin.getJoint())) ||
					(segment.getEndJoint() != null && segment.getEndJoint().equals(pin.getJoint()))) {
				segmentsToDelete.add(segment);
			}
		}
		conductorsToDisconnect.add(conductor);
	}

	private boolean doesTreeContainAValidPath(IDeviceICD currentICD, SignalPathTree tree, String associatedSignal)
	{
		Predicate<IPin> noSignalPinFilter = (pin) -> {
			int noConductors = ICDInterconnectStrategy.isNonSymbolledSplicePin(pin) ? 1 : 0;
			return pin.getConductors().stream().count() == noConductors;
		};
		// get leaf nodes, if any of the leaf node is on a splitting object , return true
		// paths terminating on a splitting object are also treated as valid paths
		List<SignalPathTree> leafNodes = tree.getLeafNodes();
		for (SignalPathTree leafNode : leafNodes) {
			// for splice one conductor
			if ((leafNode.getPin() != null) && noSignalPinFilter.test(leafNode.getPin()) &&
					(SignalPathTreeHelper.isPinOnASplittingObject(leafNode.getPin()))) {
				return true;
			}
		}
		// if any of the nodes of this tree connects to an ICD with one of these signals then return true
		List<SignalPathTree> allNodes = tree.getAllNodes();
		for (SignalPathTree nodeInTree : allNodes) {
			if (nodeInTree.getPin() == null) {
				return true; // path ending as a dangling conductor
			}
			if (nodeInTree.getPin() != null &&
					isConnectedToAValidPin(currentICD, nodeInTree.getPin(), associatedSignal)) {
				return true;
			}
		}
		// no path leads to a correct ICD termination
		return false;
	}

	private boolean isConnectedToAValidPin(IDeviceICD currentICD, IPin pin, String associatedSignal)
	{
		// if atleast one pin connected to this pin is a valid pin then return true
		Set<IPin> connectedPins = getConnectedPins(pin);

		Set<IPin> nonICDnonSplittingObjectPins = connectedPins.stream()
				.filter(connectedPin -> !SignalPathTreeHelper.isPinOnASplittingObject(connectedPin))
				.filter(connectedPin -> getICDAssociatedToPin(connectedPin) == null)
				.collect(Collectors.toSet());

		if (!nonICDnonSplittingObjectPins.isEmpty()) {
			return true;
		}

		// use a predicate here instead of the long check
		Predicate<IPin> icdPinPredicate = pin1 -> {
			IDevice device = CommonUtils.cast(pin1.getConnectivity().getOwner(), IDevice.class);
			IDeviceICD icd = ICDUtils.getMappedICD(device);
			return icd != null && !icd.equals(currentICD);
		};
		Set<IPin> icdPins = connectedPins.stream()
				.filter(icdPinPredicate)
				.collect(Collectors.toSet());
		if (!icdPins.isEmpty()) {
			for (IPin icdPin : icdPins) {
				// get ICD signals associated with this pin
				// if the set of associated signals contains the input signal, return true
				if (isPinAnICDPinWithThisSignal(icdPin, associatedSignal)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	private static IDeviceICD getICDAssociatedToPin(IPin pin)
	{
		IPin transformedPin = transformToDevicePinIfRequired(pin);
		IPinList pinlist = ICDInterconnectStrategy.getPinList(transformedPin);
		if (pinlist == null) {
			return null;
		}
		IDevice device = CommonUtils.cast(pinlist.getConnectivity(), IDevice.class);
		if (device == null) {
			return null;
		}
		return ICDUtils.getMappedICD(device);
	}

	private boolean isPinAnICDPinWithThisSignal(IPin pin, String signalName)
	{
		// transform from connector pin to device pin
		//IPin transformedPin = transformToDevicePinIfRequired(pin);
		// transformation done in below call
		IDeviceICD icd = getICDAssociatedToPin(pin);
		return icd != null && ICDUtils.isSignalValidForThisPin(pin, icd, signalName);
	}

	@NotNull private Set<IPin> getConnectedPins(IPin pin)
	{
		Set<IPin> connectedPins = new HashSet<>();
		for (IConductor conductor : pin.getConductors()) {
			List<IPin> otherEnds = conductor.getPins().stream()
					.filter(pin1 -> pin1 != pin)
					.collect(Collectors.toList());
			if (!otherEnds.isEmpty()) {
				for (IPin otherEnd : otherEnds) {
					connectedPins.add(transformToDevicePinIfRequired(otherEnd));
				}
			}
		}
		return connectedPins;
	}

	private static IPin transformToDevicePinIfRequired(IPin pin)
	{
		IPinList pinParent = ICDInterconnectStrategy.getPinList(pin);
		IHarnessPlugConnector connectedPinParent = null;
		if (pinParent != null) {
			connectedPinParent = CommonUtils.cast(pinParent.getConnectivity(), IHarnessPlugConnector.class);
		}
		if (connectedPinParent != null) {
			IPin matchingDevicePin =
					ConnectionHelper.getMatchingPinForConnectorPin(pin, pinParent, IDevice.class);
			if (matchingDevicePin != null) {
				return matchingDevicePin;
			}
		}
		return pin;
	}

	@NotNull private SetMap<IBackshellTermination, IICDAssociatedSignal> getICDSignalsForLogicBackshellTerm(
			@NotNull IPinList pinList, IDeviceICD currentICD)
	{
		SetMap<IBackshellTermination, IICDAssociatedSignal> logicBSTermAssociatedSignalsMap = new SetMap<>();
		IDevice device = CommonUtils.cast(pinList.getConnectivity(), IDevice.class);
		if (device != null) {
			List<IDeviceICD> icdList = Collections.singletonList(currentICD);
			final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdTerminationInfo =
					ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(icdList);
			ICDUtils.processMatchingBSTerminals(device, icdList, (icdBSTerm, logicBSTerm) -> {
				final Set<IICDAssociatedSignal> icdAssociatedSignals =
						icdTerminationInfo.pullReadOnlySafeSet(icdBSTerm);
				logicBSTermAssociatedSignalsMap.addAll(logicBSTerm, icdAssociatedSignals);
			});
		}
		return logicBSTermAssociatedSignalsMap;
	}

	@NotNull private Collection<IPin> getSchemBSTerminations(IPinList pinList)
	{
		Collection<IPin> schemBSTs = new HashSet<>();
		Collection<IPinList> attachedPinLists = pinList.getAttachedPinListObjects();
		for (IPinList attachedPinlist : attachedPinLists) {
			IHarnessPlugConnector harnessConnector =
					CommonUtils.cast(attachedPinlist.getConnectivity(), IHarnessPlugConnector.class);
			if (harnessConnector != null) {
				schemBSTs.addAll(attachedPinlist.getPins().stream()
						.filter(pin -> pin.getConnectivity() instanceof IBackshellTermination)
						.collect(Collectors.toSet()));
			}
		}
		return schemBSTs;
	}

}
