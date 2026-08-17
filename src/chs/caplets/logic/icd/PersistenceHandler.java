package chs.caplets.logic.icd;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.DeleteHelper;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDNetCableElement;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.logic.MulticoreUtils;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class PersistenceHandler
{

	private SharedDetailsLockHelper mLockTracker;
	private Collection<IUIDObject> mOrphanedConductors;
	private Collection<ILogicObject> mReplacedConductors;
	protected ISchemDiagram mDiagram;
	private boolean mGenerateSingleEnded;
	private IICDReporter mReporter;

	protected PersistenceHandler(ISchemDiagram diagram, boolean generateSingleEnded, @NotNull IICDReporter reporter)
	{
		mLockTracker = new SharedDetailsLockHelper();
		mOrphanedConductors = new HashSet<>();
		mReplacedConductors = new HashSet<>();
		mDiagram = diagram;
		mGenerateSingleEnded = generateSingleEnded;
		mReporter = reporter;
	}

	@NotNull public SharedDetailsLockHelper getLockTracker()
	{
		return mLockTracker;
	}

	public boolean isGenerateSingleEnded()
	{
		return mGenerateSingleEnded;
	}

	public abstract void endRouting();

	protected void deleteOrphanedObjects()
	{
		collectOrphanedFromReplacedObjects();
		// DeleteHelper is not deleting when schem conductor is passed for deletion
		// so pass all the segments of the schem conductor to be deleted
		Collection<IUIDObject> objectsToDelete = new HashSet<>();
		for (IUIDObject orphanedConductor : mOrphanedConductors) {
			if (orphanedConductor instanceof IConductor) {
				Set<ISegment> segmentsToDelete =
						((ISegmentCollector) orphanedConductor).getSegmentsOfType(ISegment.class);
				objectsToDelete.addAll(Collections.unmodifiableSet(segmentsToDelete));
			}
			objectsToDelete.add(orphanedConductor);
		}
		if(!objectsToDelete.isEmpty()) {
			DeleteHelper.getInstance().delete(mDiagram, objectsToDelete, true);
		}
	}

	private void collectOrphanedFromReplacedObjects()
	{
		ILogicDesign design = mDiagram.getDesign();
		if (design != null) {
			IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
			mReplacedConductors.stream()
					.filter(logicObject -> !designWideUsageMgr.hasUsage(logicObject))
					.collect(Collectors.toCollection(() -> mOrphanedConductors));
		}
	}

	public abstract void endRoutingAll();

	public abstract boolean isUpdate();

	@NotNull
	public abstract Collection<IConductor> disconnectInvalidSignals(IICDSignalSourceSchemPinlist currentSchemDevice,
			IDeviceICD currentICD, Collection<ISegment> disconnectedSegments);

	public void collectOrphanedConductors(Collection<IConductor> disconnectedConductors,
			Collection<ISegment> disconnectedSegments, boolean isWiringAbstraction)
	{
		Collection<IUIDObject> objectsToDelete = new HashSet<>();
		if (!isWiringAbstraction) {
			Predicate<ISegment> shieldSegmentToBeRetained = (seg) -> seg.getConductor() != null
					&& seg.getConductor().getConnectivity() instanceof IShieldConductor
					&& seg.getConductor().getNumPins() == 1;
			Collection<ISegment> segmentsToBeRetained = disconnectedSegments.stream()
					.filter(shieldSegmentToBeRetained)
					.collect(Collectors.toList());
			// disconnected shield segments might have been reused on generate multicores, so retain those shield segments
			// if they are connected to a pin
			disconnectedSegments.removeAll(segmentsToBeRetained);
			objectsToDelete.addAll(disconnectedSegments);
		}
		for (IConductor conductor : disconnectedConductors) {
			boolean deleteEntirely = isWiringAbstraction || (conductor.getNumPins() < 2);
			boolean isShieldConductor = conductor.getConnectivity() instanceof IShieldConductor;
			boolean notAConnectedShield = !isShieldConductor || (conductor.getNumPins() == 0);
			// entire schem conductor has to be deleted only if it is either a wire or a net connected to just one pin
			// also disconnected shields might have been reused on generate multicores, so delete those disconnected shields only if they
			// are not connected to a pin
			if (deleteEntirely && notAConnectedShield) {
				objectsToDelete.add(conductor);
				objectsToDelete.addAll(conductor.getSegments());
			}
			else {
				ConductorRouteAction.getInstance().addConductorForRoute(conductor);
			}
		}
		mOrphanedConductors.addAll(objectsToDelete);
	}

	public abstract void removeSignalsFromInvalidMulticores(IPinList currentSchemDevice, IDeviceICD currentICD,
			ICDMulticoreContext multicoreContext, boolean isWiringAbstraction);

	@NotNull
	public abstract ListSet<IMulticore> getDesignMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType);

	@NotNull
	public abstract ListSet<IMulticore> getConnectedMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType);

	public abstract void collectEmptyMulticores(Collection<chs.cof.logical.cable.IConductor> conductorsToBeUpdated);

	@NotNull
	protected ListSet<IMulticore> getConnectedMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, boolean topOnlyMulticores, String conductorType)
	{
		// First get multicores on pinlists connected to the conductor
		ListSet<IPinList> connectedPLs = new ListSet<>();
		IPinList priorityPinListToGetMCs = pinList;
		for (IPin pin : schemCond.getPins()) {
			IPinList pinParent = CommonUtils.cast(pin.getParent(), IPinList.class);
			if (pinParent != null) {
				connectedPLs.add(pinParent);
				if (addAttachedPinLists(pinParent, pinList, connectedPLs)) {
					priorityPinListToGetMCs = pinParent;
				}
			}
		}

		if (connectedPLs.contains(priorityPinListToGetMCs)) {
			connectedPLs.remove(priorityPinListToGetMCs);
			connectedPLs.add(0, priorityPinListToGetMCs);
		}

		Set<IMulticore> connectingRootMulticores = new HashSet<>();
		for (IPinList connectedPL : connectedPLs) {
			for (IPin pin : connectedPL.getPins()) {
				for (IConductor conductor : pin.getConductors()) {
					IMulticore multicore =
							MulticoreUtils.getRootMulticore(conductor.getConnectivity().getMulticore(), true);
					if (multicore != null) {
						connectingRootMulticores.add(multicore);
					}
				}
			}
		}

		ListSet<IMulticore> multicores = new ListSet<>();
		final boolean isCondShared = schemCond.getConnectivity().isShared();
		for (IMulticore connectingRootMulticore : connectingRootMulticores) {
			if (!ICDUtils.doesMulticoreHaveSignal(connectingRootMulticore, signal, isCondShared, conductorType)) {
				if (topOnlyMulticores) {
					multicores.add(connectingRootMulticore);
				}
				else {
					multicores.addAll(connectingRootMulticore.getAllMulticoresInHierarchy());
				}
			}
		}

		return multicores;
	}

	@NotNull
	protected ListSet<IMulticore> getDesignMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, boolean topOnlyMulticores, String conductorType)
	{
		ListSet<IMulticore> multicores = new ListSet<>();
		Predicate<IMulticore> predicate = (mult) -> !ICDUtils
				.doesMulticoreHaveSignal(mult, signal, schemCond.getConnectivity().isShared(), conductorType);
		IConnectivity connectivity = pinList.getConnectivity().getConnectivity();
		connectivity.getMulticores(false, false).stream()
				.filter(multicore -> !topOnlyMulticores || multicore.getParent() == null ||
						multicore.getParent() instanceof IOverbraid)
//				.filter(mult -> !multicores.contains(mult))
				.filter(predicate)
				.sorted(getMulticoreComparator())
				.collect(Collectors.toCollection(() -> multicores));
		return multicores;
	}

	@NotNull
	private Comparator<IMulticore> getMulticoreComparator()
	{
		return (o1, o2) -> {
			if (!o1.isShared() && o2.isShared()) {
				return 1;
			}
			if (o1.isShared() && !o2.isShared()) {
				return -1;
			}
			return 0;
		};
	}

	private boolean addAttachedPinLists(IPinList pinParent, IPinList placingICDSchem, ListSet<IPinList> connectedPLs)
	{
		boolean isPinParentAttachedToPlacingICD = false;
		IHarnessPlugConnector connectedPinParent =
				CommonUtils.cast(pinParent.getConnectivity(), IHarnessPlugConnector.class);
		if (connectedPinParent != null) {
			IBaseDevice baseDevice = connectedPinParent.getOwner();
			if (baseDevice != null) {
				Collection<IPinList> attachedDevices = pinParent.getAttachedPinListObjects();
				// Ideally there should be only one device
				for (IPinList deviceSchem : attachedDevices) {
					for (IPinList deviceAttachedPL : deviceSchem.getAttachedPinListObjects()) {
						connectedPLs.add(deviceAttachedPL);
					}
					if (deviceSchem == placingICDSchem) {
						isPinParentAttachedToPlacingICD = true;
					}
				}
			}
		}
		return isPinParentAttachedToPlacingICD;
	}

	@NotNull public abstract Collection<ISharedMulticore> getSharedMulticores(IConnectivity connectivity,
			ISharedConductorMgr sharedConductorMgr, String condType);

	@NotNull public abstract Collection<ISharedMulticore> getSharedMulticores(@NotNull Collection<ISharedMulticore> sharedMulticores);

	@NotNull public Set<ISharedConductor> getSharedConductors(IConnectivity connectivity,
			ISharedConductorMgr sharedConductorMgr, String condType)
	{
		return sharedConductorMgr.getLogicalSharedConductors().stream()
				.filter(sharedConductor -> sharedConductor.getType().equalsIgnoreCase(condType))
				.filter(sharedConductor -> connectivity.findSharedConductor(sharedConductor) == null)
				.filter(sharedConductor -> {
					ISharedMulticore rootMulticore =
							MulticoreUtils.getRootMulticore(sharedConductor.getMulticore(), true);
					return rootMulticore == null || connectivity.findSharedMulticore(rootMulticore) == null;
				})
				.collect(Collectors.toSet());
	}

	@NotNull
	public ListSet<ISharedMulticore> getMatchingSharedMulticores(Collection<ISharedMulticore> sharedMulticores,
			IICDAssociatedSignal signal)
	{
		ListSet<ISharedMulticore> matchingMulticores = new ListSet<>();
		Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(signal);
		for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
			List<IICDNetCableElement> reversedPath = new ArrayList<>();
			for (IICDNetCableElement element : signalGroupPath) {
				reversedPath.add(0, element);
			}
			matchingMulticores.addAll(getSharedMulticoresMatchingHierarchy(sharedMulticores, reversedPath));
		}
		return matchingMulticores;
	}

	@NotNull private ListSet<ISharedMulticore> getSharedMulticoresMatchingHierarchy(
			@NotNull Collection<ISharedMulticore> sharedMulticores, @NotNull List<IICDNetCableElement> cableHierarchy)
	{
		ListSet<ISharedMulticore> reusableSharedParents = new ListSet<>();
		for (ISharedMulticore sharedMulticore : sharedMulticores) {
			ISharedMulticore reusableChildMulticore = null;
			Iterator<IICDNetCableElement> mcSourcePath = cableHierarchy.iterator();
			List<ISharedMulticore> multicoreList = Collections.singletonList(sharedMulticore);
			while (mcSourcePath.hasNext()) {
				IICDNetCableElement mcSourcePair = mcSourcePath.next();
				ISharedMulticore childMulticore = findMatchingMulticore(mcSourcePair, multicoreList);
				if (childMulticore == null) {
					break;
				}
				reusableChildMulticore = childMulticore;
				multicoreList = CollectionUtils.createList(reusableChildMulticore.getMulticores());
			}
			if (reusableChildMulticore != null) {
				reusableSharedParents.add(reusableChildMulticore);
			}
		}
		return reusableSharedParents;
	}

	@Nullable
	private ISharedMulticore findMatchingMulticore(@NotNull IICDNetCableElement mcSourcePair,
			@NotNull List<ISharedMulticore> multicores)
	{
		for (ISharedMulticore sharedMulticore : multicores) {
			String multicoreSource = ICDInterconnectStrategy.getMulticoreSource(sharedMulticore);
			if (mcSourcePair.getOriginalName().equals(multicoreSource) &&
					compareIndicatorTypes(sharedMulticore, mcSourcePair.getType())) {
				return sharedMulticore;
			}
		}
		return null;
	}

	protected boolean compareIndicatorTypes(ISharedMulticore multicore, String indicatorType)
	{
		return ICDUtils.determineIndicatorType(indicatorType).equals(ICDUtils.determineIndicatorType(multicore));
	}

	@NotNull public Set<ISharedMulticore> getMatchingSharedMulticores(Collection<ISharedMulticore> sharedMulticores,
			Pair<String, String> cableNameTypePair)
	{
		Set<ISharedMulticore> reusableSharedParents = new HashSet<>();
		sharedMulticores.forEach(sharedMulticore -> {
					String existingMCSourceName = getPropertyByName(sharedMulticore, ICDMulticore.SOURCE_CABLE_NAME);
					if (existingMCSourceName.equalsIgnoreCase(cableNameTypePair.getKey())) {
						reusableSharedParents.add(sharedMulticore);
					}
				}
		);
		return reusableSharedParents;
	}

	@NotNull private String getPropertyByName(@NotNull IPropertiedObject propertiedObject, String propertyName)
	{
		String propertyValue = null;
		IProperty property = propertiedObject.findPropertyByName(propertyName);
		if (property != null) {
			propertyValue = property.getAsString();
		}
		if (propertyValue == null) {
			return StringUtils.EMPTY_STRING;
		}
		return propertyValue;
	}

	protected boolean areAllConductorsOfMatchingType(ISharedMulticore multicore, String condType)
	{
		return multicore == null || multicore.getRootMulticore().getAllSharedConductorsInHierarchy(false).stream()
				.allMatch(sharedConductor -> sharedConductor.getType().equalsIgnoreCase(condType));
	}

	@NotNull public Set<ISharedConductor> getMatchingSharedConductors(Set<ISharedConductor> sharedConductors,
			chs.cof.logical.cable.IConductor conductor, IICDAssociatedSignal signal)
	{
		if (conductor.isShared()) {
			return Collections.emptySet();
		}
		final String conductorType = conductor.getType();
		boolean isWiring = conductorType.equalsIgnoreCase(ISharedConductor.WIRE_TYPE);
		return sharedConductors.stream()
				.filter(sharedConductor -> sharedConductor.getType().equalsIgnoreCase(conductorType))
				.filter(sharedConductor -> {
					final String signalName =
							ICDUtils.getAssociatedSignalNameForConductor(sharedConductor, isWiring);
					return signalName != null && signalName.equalsIgnoreCase(signal.getNetName());
				})
				.filter(sharedConductor -> areAllConductorsOfMatchingType(sharedConductor.getMulticore(),
						conductorType))
				.collect(Collectors.toSet());
	}

	public void collectReplacedConductors(ILogicObject conductor)
	{
		mReplacedConductors.add(conductor);
	}

	@NotNull protected Set<List<IICDNetCableElement>> getSignalGroupPaths(@NotNull IICDAssociatedSignal signal)
	{
		ILogicDesign design = mDiagram.getDesign();
		assert design != null;
		return design.getDesignICDContainer().getEquivalentSignalGroupPaths(signal);
	}

	@NotNull public IICDReporter getReporter()
	{
		return mReporter;
	}
}
