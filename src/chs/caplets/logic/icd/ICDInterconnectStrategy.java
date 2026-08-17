/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2015-2025 Siemens
 */
package chs.caplets.logic.icd;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.actions.HarnessAttributeUpdater;
import chs.caplets.logic.actions.SelectedPartUpdateHelper;
import chs.caplets.logic.actions.UpdateICDAction;
import chs.caplets.logic.merge.ConductorMerger;
import chs.cof.draw.IColor;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshell;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.logical.GeneralReportValidationHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.wdg.IGeneratedConductor;
import chs.cof.logical.footprint.IReportValidationHandler;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.ILibraryDevice;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.ICommonFactory;
import chs.common.ILocation;
import chs.common.ILockable;
import chs.common.ILockableDelegate;
import chs.common.IObjectFilter;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.Side;
import chs.common.UIDUtils;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.dataservices.SharedObjectUsageCache;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.system.DisableLanguageTranslation;
import chs.system.FactoryMgr;
import chs.system.ISystemObjectTypeInfoMgr;
import chs.system.UIDMgr;
import chs.utilities.CaseLessStringKey;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.LinkedTHashSet;
import chs.utilities.ListMap;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.ICDSignalDetailsFinder;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDSignalsContainer;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.ConductorWrapperForJoinSegments;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.helpers.LogicSegmentWrapperForJoinSegments;
import chs.utility.helpers.NameTemplateHelper;
import chs.utility.helpers.PinListConnectionHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.ShieldBodyHelper;
import chs.utility.logic.PinUtils;
import chs.utility.logic.SchemGraphUtils;
import chs.utility.stream.IPipelineStreamExecutable;
import chs.utility.stream.PipelineStream;
import chs.utility.stream.PipelineStreamInput;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.LockInfoDialog;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public abstract class ICDInterconnectStrategy implements IICDInterconnectStrategy
{

	private static final int MAX_SEGS_AT_JOINING_JOINT = 4;
	private static final int MAX_SEGS_AT_JOINING_SPLICE = 4;
	protected ICDMulticoreContext multicoreContext;
	protected PersistenceHandler mPersistenceHandler;
	@NotNull private SchemDeviceICDPinInfoCache m_SchemDeviceICDPinInfoCache;
	private Map<String, chs.cof.logical.cable.IConductor> allVisitedConductors = new HashMap<>();
	private Map<chs.cof.logical.cable.IConductor,IICDAssociatedSignal> conductorsToSignalMap = new HashMap<>();


	public Map<String, chs.cof.logical.cable.IConductor> getAllVistedConductors()
	{
		return allVisitedConductors;
	}

	@NotNull @Override public Set<IShieldConductor> getNewShieldsInMulticores()
	{
		return multicoreContext.getNewShieldsInMulticores();
	}

	@Override public void endRouting()
	{
		mPersistenceHandler.endRouting();
	}

	protected ICDInterconnectStrategy(@NotNull PersistenceHandler persistenceHandler)
	{
		m_SchemDeviceICDPinInfoCache = new SchemDeviceICDPinInfoCache();
		multicoreContext = new ICDMulticoreContext();
		mPersistenceHandler = persistenceHandler;
	}

	public void updateICDRouting(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram,
			@NotNull IObjectFilter<IPin> pinFilter)
	{
		PinListConnectionHelper.runUnderBoost(() -> {
			doUpdateICDRouting(currentSchemDevice, currentICD, diagram, pinFilter);
			return Void.TYPE;
		});
	}

	protected abstract void generateSingleEndedConductors(@NotNull ISchemDiagram diagram,
			@NotNull List<PlacingPinRouteInfo> placingPinInfo);

	private void doUpdateICDRouting(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram,
			@NotNull IObjectFilter<IPin> pinFilter)
	{
		IICDSignalSourceSchemPinlist currentSchemDevInfo =
				m_SchemDeviceICDPinInfoCache.getICDSignalSourceSchemPinlist(currentSchemDevice);

		DeviceToICDMatcher icdMatcher = new DeviceToICDMatcher(diagram);
		Map<IDevice, List<IDeviceICD>> placedDeviceToICDs = icdMatcher.getPlacedDeviceToICDsInPrecedence();
		List<PlacingPinRouteInfo> placingPinRouteInfo =
				ICDInterconnectPreprocessor.constructPlacingPinRouteInfo(currentSchemDevInfo, currentICD, pinFilter);
		if (!placedDeviceToICDs.isEmpty()) {

			multicoreContext.populateSignalConnectionCount(diagram.getDesign());

			for (Map.Entry<IDevice, List<IDeviceICD>> entry : placedDeviceToICDs.entrySet()) {
				IDevice placedDevice = entry.getKey();
				List<IDeviceICD> icdsAssociatedWithDevice = entry.getValue();
				routeBetweenDevices(placedDevice, icdsAssociatedWithDevice, diagram, placingPinRouteInfo);
			}
		}

		Collection<ISegment> disconnectedSegments = new HashSet<>();
		Collection<IConductor> disconnectedConductors = mPersistenceHandler
				.disconnectInvalidSignals(currentSchemDevInfo, currentICD, disconnectedSegments);

		// remove non shield disconnected conductors from multicores, so that multicore generation happens smoothly
		Set<chs.cof.logical.cable.IConductor> disconnectedCableConds =
				disconnectedConductors.stream().map(IConductor::getConnectivity)
						.filter(cond -> !(cond instanceof IShieldConductor))
						.collect(Collectors.toSet());
		mPersistenceHandler.collectEmptyMulticores(disconnectedCableConds);
		mPersistenceHandler.removeSignalsFromInvalidMulticores(currentSchemDevice, currentICD, multicoreContext,
				isWiringAbstraction());

		createDanglingConductors(diagram);

		if (shouldGenerateSingleEnded()) {
			generateSingleEndedConductors(diagram, placingPinRouteInfo);
		}

		generateMulticores(currentSchemDevice, currentICD, diagram);
		mPersistenceHandler
				.collectOrphanedConductors(disconnectedConductors, disconnectedSegments, isWiringAbstraction());
	}

	private boolean shouldGenerateSingleEnded()
	{
		return mPersistenceHandler.isGenerateSingleEnded();
	}

	private void generateMulticores(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram)
	{
		Collection<ISharedObject> sharedConductors = collectObjectsForPreProcessing(currentSchemDevice);
		SharedObjectUsageCache.collectSharedObjectUsages(sharedConductors);
		BatchLockRefreshHelper.batchLockWithPromise(sharedConductors,
				() -> performGenerateMulticoreOperation(currentSchemDevice, currentICD, diagram));
		unlockObjectsLockedUsingPromise(sharedConductors);
		SharedObjectUsageCache.clear();
	}

	@NotNull private Collection<ISharedObject> collectObjectsForPreProcessing(@NotNull IPinList currentSchemDevice)
	{
		Collection<ISharedObject> sharedObjects = new HashSet<>();
		for (Map.Entry<IICDAssociatedSignal, Set<IConductor>> condSignalMap : multicoreContext
				.getSignalConductorMap().entrySet()) {
			for (IConductor schemCond : condSignalMap.getValue()) {
				if (!shouldCreateMulticore(currentSchemDevice, schemCond)) {
					continue;
				}
				ISharedConductor sharedConductor = schemCond.getConnectivity().getSharedConductor();
				if (sharedConductor != null) {
					if (!sharedConductor.isLocked()) {
						sharedObjects.add(sharedConductor);
					}
				}
			}
		}
		return sharedObjects;
	}

	private void performGenerateMulticoreOperation(@NotNull IPinList currentSchemDevice, @NotNull IDeviceICD currentICD,
													@NotNull ISchemDiagram diagram)
	{
		Set<IMulticore> multicoresToBeProcessed = new HashSet<>();
		for (Map.Entry<IICDAssociatedSignal, Set<IConductor>> condSignalMap : multicoreContext
				.getSignalConductorMap().entrySet()) {
			IICDAssociatedSignal signal = condSignalMap.getKey();
			for (IConductor schemCond : condSignalMap.getValue()) {
				if (!shouldCreateMulticore(currentSchemDevice, schemCond)) {
					continue;
				}
				Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond = getConnectedPinLists(schemCond);
				List<ICDMulticoreAdapter> connectedMulticores =
						getConnectedMulticores(currentSchemDevice, schemCond, signal, connectedCablePlsToCond).stream()
								.map(ICDMulticoreAdapter::new)
								.collect(Collectors.toList());
				List<ICDMulticoreAdapter> designMulticores =
						getDesignMulticores(currentSchemDevice, schemCond, signal, connectedCablePlsToCond).stream()
								.map(ICDMulticoreAdapter::new)
								.collect(Collectors.toList());

				MulticoreBuilder builder =
						new MulticoreBuilder(signal, schemCond.getConnectivity(), connectedMulticores, designMulticores,
								mPersistenceHandler);
				Collection<IMulticore> createdMulticores = builder.createMulticore();

				if (createdMulticores != null) {
					multicoresToBeProcessed.addAll(createdMulticores);
				}
			}
		}

		//if a schem/connectivity is created and added in existing multicore
		//we need to process those multicores also for harness attr and shielding.
		for (IConductor schemCond : multicoreContext.getSignalConductorMap().itemSet()) {
			chs.cof.logical.cable.IConductor conductor = schemCond.getConnectivity();
			IMulticore multicore = conductor.getMulticore();
			if (multicore != null) {
				multicoresToBeProcessed.add(multicore);
			}
		}

		Set<IMulticore> rootMulticoreToProcess = multicoresToBeProcessed.stream()
				.map(IMulticore::getRootMulticore)
				.collect(Collectors.toSet());

		Set<IMulticore> mcsNeedingIndicatorCleanup = multicoreContext.getMCsOfReusedDanglingConductors();
		Set<IMulticore> multicoresForShielding = getMulticoresForShielding(rootMulticoreToProcess, mcsNeedingIndicatorCleanup);
		generateShields(currentSchemDevice, currentICD, diagram, multicoresForShielding);

		for (IMulticore multicoreToProcess : mcsNeedingIndicatorCleanup) {
			for (IMulticore mc : multicoreToProcess.getAllMulticoresInHierarchy()) {
				IndicatorCleaner cleaner = new IndicatorCleaner(mc, diagram);
				cleaner.cleanIndicators();
			}
		}

		Set<IMulticore> unsharedRootMulticores = new HashSet<>();
		Set<ISharedMulticore> sharedRootMulticores = new HashSet<>();
		rootMulticoreToProcess.forEach(rootMulticore -> {
			ISharedMulticore rootSharedMulticore = rootMulticore.getSharedMulticore();
			if (rootSharedMulticore != null) {
				sharedRootMulticores.add(rootSharedMulticore);
			}
			else {
				unsharedRootMulticores.add(rootMulticore);
			}
		});
		HarnessAttributeUpdater harnessAttributeUpdater = new HarnessAttributeUpdater();
		harnessAttributeUpdater.syncMulticores(unsharedRootMulticores, sharedRootMulticores);
	}

	private void unlockObjectsLockedUsingPromise(@NotNull Collection<ISharedObject> sharedConductors)
	{
		Collection<IUID> lockedSharedObjects = mPersistenceHandler.getLockTracker().getLockedSharedObjects();
		Set<IUID> sharedConductorsUIDs = UIDUtils.convertToUIDSet(sharedConductors);
		sharedConductorsUIDs.stream()
				.filter(sharedObjectUID -> !lockedSharedObjects.contains(sharedObjectUID))
				.map(sharedObjectUID -> UIDMgr.getObjectOfType(sharedObjectUID, ILockable.class))
				.filter(lockable -> lockable != null)
				.forEach(ILockable::unlock);
	}

	private boolean shouldCreateMulticore(IPinList currentSchemDevice, IConductor schemCond)
	{
		chs.cof.logical.cable.IConductor cableCond = schemCond.getConnectivity();
		if (cableCond.getMulticore() != null) {
			// todo: Instead of check here, should avoid registering these.
			return false;
		}
		ISharedConductor sharedConductor = cableCond.getSharedConductor();
		if (sharedConductor != null && sharedConductor.isFrozen()) {
			return false;
		}

		if (isCondConnectedToICD(currentSchemDevice, schemCond)) {
			return true;
		}

		for (IPinList pinList : currentSchemDevice.getAttachedPinListObjects()) {
			if (isCondConnectedToICD(pinList, schemCond)) {
				return true;
			}
		}
		return false;
	}

	private boolean isCondConnectedToICD(IPinList currentSchemDevice, IConductor schemCond)
	{
		boolean isCondConnectedToICD = false;
		for (IPin pin : currentSchemDevice.getPins()) {
			if (schemCond.getPins().contains(pin)) {
				isCondConnectedToICD = true;
			}
		}
		return isCondConnectedToICD;
	}

	private void generateShields(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram,
			Set<IMulticore> multicoresForShielding)
	{
		if (!multicoresForShielding.isEmpty()) {
			createShieldInMulticore(multicoresForShielding, diagram);
			connectPinsToHookupOfThisMulticore(multicoresForShielding, diagram);
		}
		// create a shield if any of the pins in currentSchemDevice is a shield pin and its multicore exists in design
		generateShieldOnPin(currentSchemDevice, currentICD, diagram);
	}

	@NotNull
	private Set<IMulticore> getMulticoresForShielding(@NotNull Set<IMulticore> multicoresToBeProcessed,
			@NotNull Set<IMulticore> mcsNeedingIndicatorCleanup)
	{
		Set<IMulticore> multicoresForShielding = new LinkedTHashSet<>();
		multicoresToBeProcessed.forEach(multicore -> {
			boolean addOnlyIfNoIndicator = !mcsNeedingIndicatorCleanup.contains(multicore);
			ShieldBuilder.placeIndicatorsOnAllDiagrams(multicore, addOnlyIfNoIndicator);
			multicore.getAllMulticoresInHierarchy().stream().forEach(childMulticore -> {
				// attempt to create a shield conductor only if there isn't any and if the multicore type allows a shield
				if (childMulticore.getShield() == null &&
						IndicatorHelper.isMulticoreShieldIndicator(childMulticore.getShieldBody())) {
					multicoresForShielding.add(childMulticore);
				}
			});
		});
		return multicoresForShielding;
	}

	private void generateShieldOnPin(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram)
	{
		List<IMulticore> candidateMulticores = getMulticoreCandidatesToProcessForShieldCreation(currentSchemDevice);
		ListMap<String, IICDAssociatedSignal> icdShieldSignalsAssociated =
				ICDSignalDetailsFinder.getICDShieldSignalsAssociated(Collections.singletonList(currentICD));
		currentSchemDevice.getPins().forEach(pin -> {
			List<IICDAssociatedSignal> iicdAssociatedShieldSignals = icdShieldSignalsAssociated.pullReadOnlySafeList(
					StringUtils.nonNull(ICDUtils.getICDMatchName(pin.getConnectivity())));
			iicdAssociatedShieldSignals.forEach(signal -> candidateMulticores.stream()
					.filter(childMulticore -> ShieldBuilder.isSignalAShieldForThisMulticore(signal, childMulticore))
					.forEach(childMulticore -> ShieldBuilder
							.doCreateShieldOnPin(diagram, Collections.singleton(pin), signal, childMulticore,
									multicoreContext, this, mPersistenceHandler.getLockTracker())));
		});
		createShieldsOnBackshellTerminations(candidateMulticores, diagram, currentSchemDevice,
				Collections.singletonList(currentICD));
	}

	@NotNull private List<IMulticore> getMulticoreCandidatesToProcessForShieldCreation(IPinList currentSchemDevice)
	{
		// the set of multicores we are interested in varies for wiring abstraction, we should consider only those multicores
		// that are connected to this schem pinlist
		IConnectivity connectivity = currentSchemDevice.getConnectivity().getConnectivity();
		return connectivity.getMulticores(false, false);
	}

	protected void createDanglingConductors(ISchemDiagram diagram)
	{
	}

	@NotNull public ListSet<IMulticore> getConnectedMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond)
	{
		ListSet<IMulticore> multicores =
				mPersistenceHandler.getConnectedMulticores(pinList, schemCond, signal, getConductorType());

		// Remove multicores connected to more than 2 pinlists (If required)
		return getMulticoresNotTerminatingAtMoreThanTwoPls(signal, multicores, connectedCablePlsToCond);
	}

	@NotNull public ListSet<IMulticore> getDesignMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond)
	{
		ListSet<IMulticore> multicores =
				mPersistenceHandler.getDesignMulticores(pinList, schemCond, signal, getConductorType());

		// Remove multicores connected to more than 2 pinlists (If required)
		return getMulticoresNotTerminatingAtMoreThanTwoPls(signal, multicores, connectedCablePlsToCond);
	}

	@NotNull protected Set<chs.cof.logical.cable.IPinList> getConnectedPinLists(IConductor schemCond)
	{
		Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond = new HashSet<>();
		for (IPin pin : schemCond.getPins()) {
			IPinList pinParent = getPinList(pin);
			if (pinParent != null) {
				connectedCablePlsToCond.add(pinParent.getConnectivity());
			}
		}
		return connectedCablePlsToCond;
	}

	@NotNull protected ListSet<IMulticore> getMulticoresNotTerminatingAtMoreThanTwoPls(IICDAssociatedSignal signal,
			ListSet<IMulticore> multicores,
			Set<chs.cof.logical.cable.IPinList> connectedCablePlsToCond)
	{
		ListSet<IMulticore> multicoresNotTerminatingAtMoreThanTwoPls = new ListSet<>();
		for (IMulticore multicore : multicores) {
			boolean doesSignalHasMoreThanTwoPinEnds =
					multicoreContext.getSignalConnectionCount(signal.getNetName()) > 2;
			Set<chs.cof.logical.cable.IPinList> connectedCablePLsToMC =
					getPinListsConnectedToMulticore(multicore, doesSignalHasMoreThanTwoPinEnds);
			connectedCablePLsToMC.addAll(connectedCablePlsToCond);
			if (connectedCablePLsToMC.size() <= 2) {
				multicoresNotTerminatingAtMoreThanTwoPls.add(multicore);
			}
		}
		return multicoresNotTerminatingAtMoreThanTwoPls;
	}

	@NotNull protected Set<chs.cof.logical.cable.IPinList> getPinListsConnectedToMulticore(IMulticore multicore,
			boolean overrideInnercoreCheck)
	{
		Set<chs.cof.logical.cable.IPinList> connectedCablePLsToMC = new HashSet<>();
		for (chs.cof.logical.cable.IConductor conductor : multicore.getRootMulticore().getAllConductorsInHierarchy()) {
			if (overrideInnercoreCheck || doesSignalTerminateAtMoreThanTwoPins(conductor)) {
				for (IAbstractPin abstractPin : conductor.getPins()) {
					connectedCablePLsToMC.add(getPinOwner(abstractPin));
				}
			}
		}
		return connectedCablePLsToMC;
	}

	@Nullable protected chs.cof.logical.cable.IPinList getPinOwner(@NotNull IAbstractPin pin)
	{
		return PinUtils.getRootOwnerPinList(pin);
	}

	protected boolean doesSignalTerminateAtMoreThanTwoPins(chs.cof.logical.cable.IConductor conductor)
	{
		IGeneratedConductor generatedConductor = CommonUtils.cast(conductor, IGeneratedConductor.class);
		String topoSignalName = null;
		if (generatedConductor != null) {
			topoSignalName = ICDUtils.getSourceICDSignal(generatedConductor);
		}
		int connectionCount = 0;
		if (topoSignalName != null && !topoSignalName.isEmpty()) {
			connectionCount = multicoreContext.getSignalConnectionCount(topoSignalName);
		}
		return connectionCount > 2;
	}

	@NotNull protected abstract String getConductorType();

	protected boolean isDanglingSegmentAvailableForJoining(ISegment segment)
	{
		return true;
	}

	private interface IInterconnectJoinCallback
	{

		void connect(ISegment segment, ILocation breakLoc, boolean isJoinAtEnd);

		void connect(IPin placedPin);
	}

	private void traverseAndJoinPinsBySignal(IAbstractPin placedPin, IPlacingPinController placingPinCtrl,
			ISchemDiagram diagram, IInterconnectJoinCallback callBack, IICDAssociatedSignal iicdAssociatedSignal)
	{
		List<IPin> placedPinsInPrecedence = getPlacedPinsInPrecedence(placedPin, placingPinCtrl, diagram);
		if (placedPinsInPrecedence.isEmpty()) {
			return;
		}
		if (!doTraverseAndJoinPinToSegment(placedPinsInPrecedence, placingPinCtrl, diagram, callBack,
				iicdAssociatedSignal)) {
			//nothing possible. create a new conductor between the placed/placing pins.
			IPin anyPlacedPin = placedPinsInPrecedence.get(0);
			if (placingPinCtrl.canConnect(anyPlacedPin)) {
				callBack.connect(anyPlacedPin);
			}
			else {
				placingPinCtrl.registerDanglingConnection(anyPlacedPin);
			}
		}
	}

	protected boolean isConductorValidToProcessForConnection(chs.cof.logical.cable.IConductor connectivity,
			String signalName)
	{
		return connectivity.getName().equals(signalName);
	}

	private boolean isSegmentValidToProcessForConnection(ISegment obj, String signalName)
	{
		chs.cof.logical.cable.IConductor connectivity = obj.getConductor().getConnectivity();
		boolean typeMatch = getCableConductorType().isAssignableFrom(connectivity.getClass());
		return typeMatch && (isConductorValidToProcessForConnection(connectivity, signalName) ||
				isSegmentPartOfADanglingConductor(obj));
	}

	private boolean isSegmentPartOfADanglingConductor(ISegment segment)
	{
		return segment.getConductor().getSegmentsOfType(ISegment.class).stream()
				.anyMatch(SegmentHelper::isOpenEndedSegment);
	}

	@SuppressWarnings("OverlyLongMethod")
	private boolean doTraverseAndJoinPinToSegment(List<IPin> placedPins, IPlacingPinController placingPinCtrl,
			ISchemDiagram diagram, IInterconnectJoinCallback callBack, final IICDAssociatedSignal iicdAssociatedSignal)
	{
		final Set<ISegment> segments = new HashSet<ISegment>();
		final Set<IPin> pins = new HashSet<>();
		SegmentHelper.ISegmentVisitController controller = new SegmentHelper.ISegmentVisitController()
		{
			@Override public boolean visit(ISegment obj)
			{
				if (isSegmentValidToProcessForConnection(obj, iicdAssociatedSignal.getNetName())) {
					return segments.add(obj);
				}
				return false;
			}

			@Override public void collect(IPin pin)
			{
				pins.add(pin);
			}

			@Override public Collection<IPin> getNextPinsToContinueTraversal(IPin pin)
			{
				return getShortCircuitedPinsToContinueTraversal(pin, pins);
			}

			@Override public IDiagramObjectIterator getAssociations(IJoint aJoint)
			{
				return aJoint.getAssociations();
			}
		};

		for (IPin placedPin : placedPins) {
			SegmentHelper.traverseNodesAccessibleFromNode(placedPin.getJoint(), controller);
		}

		if (!placingPinCtrl.proceedToJoin(pins)) {
			//the two pins are already connected.
			//Still go ahead and update properties and Option Expression
			//because these may have been changed in ICD
			for (ISegment segment : segments) {
				if (segment.getParent() != null) {
					chs.cof.logical.cable.IConductor placedConductor =
							((IConductor) segment.getParent()).getConnectivity();
					updateAttributesAndPropOnPlacedSignal(placedConductor, iicdAssociatedSignal);
					chs.cof.logical.cable.IConductor cableConductor =
							((IConductor) segment.getParent()).getConnectivity();
					allVisitedConductors.put(cableConductor.getName(), cableConductor);
				}
			}
			return true;
		}

		ILocation placingPinLoc = placingPinCtrl.getPlacingPinReferenceLocation();
		final double placingPinLocX = placingPinLoc.getX();
		final double placingPinLocY = placingPinLoc.getY();
		Comparator<SchemGraphUtils.ISchemGraphNode> graphNodeComparator =
				(o1, o2) -> compareLocation(o1, o2, placingPinLocX, placingPinLocY);

		Comparator<ILocation> jointComparator = (o1, o2) -> compareLocation(o1, o2, placingPinLocX, placingPinLocY);

		Map<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> vertices = new TreeMap<>(graphNodeComparator);
		//SchemEdge ensures that a joint is created if not present.
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		List<SchemGraphUtils.SchemEdge> edges = new ArrayList<>();
		for (ISegment seg : segments) {
			SchemGraphUtils.SchemEdge edge = new SchemGraphUtils.SchemEdge(seg, vertices);
			edges.add(edge);
		}
		edges.clear();

		//first try with dangling ends.
		for (Map.Entry<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> entry : vertices.entrySet()) {
			SchemGraphUtils.SchemVertex schemVertex = entry.getValue();
			if (schemVertex.isDanglingEnd()) {
				Set<ISegment> segs = schemVertex.getSegments();
				assert segs.size() == 1 : "Invalid state of a dangling end!!!";
				if (!segs.isEmpty()) {
					ISegment segment = segs.iterator().next();
					if (isDanglingSegmentAvailableForJoining(segment)) {
						if (placingPinCtrl.canConnect(segment)) {
							Point pt = schemVertex.getPoint();
							ILocation breakPoint = FactoryMgr.getCommonFactory().constructLocation(pt.x, pt.y);
							callBack.connect(segment, breakPoint, true);
							return true;
						}
					}
				}
			}
		}

		// before going further and trying with non-dangling ends, see if any of the traversed pins are available for connection
		Predicate<IConductor> signalNameMatch = cond -> iicdAssociatedSignal.getNetName()
				.equals(ICDUtils.getAssociatedSignalNameForConductor(cond.getConnectivity(), isWiringAbstraction()));
		Predicate<IPin> noSignalPinFilter = (pin) -> {
			long matchingSignalCountAtPin = pin.getConductors().stream().filter(signalNameMatch).count();
			if (isNonSymbolledSplicePin(pin)) {
				// for a splice pin, it becomes eligible to connect to, if it has got only one signal,
				// the one which we have used to traverse and reach here
				return matchingSignalCountAtPin == 1;
			}
			else {
				return matchingSignalCountAtPin == 0;
			}
		};

		Set<IPin> transformedPins = new HashSet<>(pins.size());
		for (IPin pin : pins) {
			transformedPins.add(checkConnectedConnectorPinIfSourceIsDevicePin(pin));
		}

		if (attemptConnectionAtPin(callBack, transformedPins, jointComparator, noSignalPinFilter, placingPinCtrl)) {
			return true;
		}

		//first try with non-dangling ends without pins.
		for (Map.Entry<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> entry : vertices.entrySet()) {
			SchemGraphUtils.SchemVertex schemVertex = entry.getValue();
			if (!schemVertex.isDanglingEnd() && schemVertex.canHaveSplice()) {
				Set<ISegment> segs = schemVertex.getSegments();
				if (!segs.isEmpty() && segs.size() < MAX_SEGS_AT_JOINING_JOINT) {
					ISegment segment = segs.iterator().next();
					if (placingPinCtrl.canSplit(segment)) {
						Point pt = schemVertex.getPoint();
						ILocation breakPoint = FactoryMgr.getCommonFactory().constructLocation(pt.x, pt.y);
						callBack.connect(segment, breakPoint, true);
						return true;
					}
				}
			}
		}

		//now try with an existing splice (non-symbolled)
		Predicate<IPin> splicePinFilter = ICDInterconnectStrategy::isNonSymbolledSplicePin;
		if (attemptConnectionAtPin(callBack, transformedPins, jointComparator, splicePinFilter, placingPinCtrl)) {
			return true;
		}

		Map<ILocation, ISegment> splitSegments = new TreeMap<>(jointComparator);
		for (ISegment seg : segments) {
			splitSegments.put(getMiddleBreakPoint(seg, diagram), seg);
		}
		//now try with splitting a segment.
		if (!splitSegments.isEmpty()) {
			Map.Entry<ILocation, ISegment> entry = splitSegments.entrySet().iterator().next();
			ISegment segment = entry.getValue();
			if (placingPinCtrl.canSplit(segment)) {
				callBack.connect(segment, entry.getKey(), false);
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("OverlyLongMethod")
	private boolean connectPinsTraversedFromPlacingAndPlacedPins(IPin placedPin, IPlacingPinController placingPinCtrl,
			ISchemDiagram diagram, IInterconnectJoinCallback callBack, final IICDAssociatedSignal iicdAssociatedSignal)
	{
		final Set<IPin> pins = new HashSet<>();
		pins.add(placedPin);
		final Set<ISegment> segments = new HashSet<ISegment>();
		SegmentHelper.ISegmentVisitController controller = new SegmentHelper.ISegmentVisitController()
		{
			@Override public boolean visit(ISegment obj)
			{
				// if a segment is part of a dangling conductor, use it with just type check
				// if a segment is not part of a dangling conductor, use it with type check and name matching checks
				// so that such segments can be used for splitting up while connecting multiple ICDs with same signal
				if (isSegmentValidToProcessForConnection(obj, iicdAssociatedSignal.getNetName())) {
					return segments.add(obj);
				}
				return false;
			}

			@Override public void collect(IPin pin)
			{
				pins.add(pin);
			}

			@Override public Collection<IPin> getNextPinsToContinueTraversal(IPin pin)
			{
				return Collections.emptyList();
			}

			@Override public IDiagramObjectIterator getAssociations(IJoint aJoint)
			{
				return aJoint.getAssociations();
			}
		};

		SegmentHelper.traverseNodesAccessibleFromNode(placedPin.getJoint(), controller);
		if (!placingPinCtrl.proceedToJoin(pins)) {
			//the two pins are already connected.
			//Still go ahead and update properties and Option Expression
			//because these may have been changed in ICD
			for (ISegment segment : segments) {
				if (segment.getParent() != null) {
					chs.cof.logical.cable.IConductor placedConductor =
							((IConductor) segment.getParent()).getConnectivity();
					updateAttributesAndPropOnPlacedSignal(placedConductor, iicdAssociatedSignal);
					chs.cof.logical.cable.IConductor cableConductor =
							((IConductor) segment.getParent()).getConnectivity();
					allVisitedConductors.put(cableConductor.getName(), cableConductor);
				}
			}
			return true;
		}

		ILocation placingPinLoc = placingPinCtrl.getPlacingPinReferenceLocation();
		final double placingPinLocX = placingPinLoc.getX();
		final double placingPinLocY = placingPinLoc.getY();
		Comparator<SchemGraphUtils.ISchemGraphNode> graphNodeComparator =
				(o1, o2) -> compareLocation(o1, o2, placingPinLocX, placingPinLocY);

		Comparator<ILocation> jointComparator = (o1, o2) -> compareLocation(o1, o2, placingPinLocX, placingPinLocY);

		Map<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> vertices = new TreeMap<>(graphNodeComparator);
		//SchemEdge ensures that a joint is created if not present.
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		List<SchemGraphUtils.SchemEdge> edges = new ArrayList<>();
		for (ISegment seg : segments) {
			SchemGraphUtils.SchemEdge edge = new SchemGraphUtils.SchemEdge(seg, vertices);
			edges.add(edge);
		}
		edges.clear();

		//first try with dangling ends.
		for (Map.Entry<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> entry : vertices.entrySet()) {
			SchemGraphUtils.SchemVertex schemVertex = entry.getValue();
			if (schemVertex.isDanglingEnd()) {
				Set<ISegment> segs = schemVertex.getSegments();
				assert segs.size() == 1 : "Invalid state of a dangling end!!!";
				if (!segs.isEmpty()) {
					ISegment segment = segs.iterator().next();
					if (isDanglingSegmentAvailableForJoining(segment)) {
						if (placingPinCtrl.canConnect(segment)) {
							Point pt = schemVertex.getPoint();
							ILocation breakPoint = FactoryMgr.getCommonFactory().constructLocation(pt.x, pt.y);
							callBack.connect(segment, breakPoint, true);
							IConductor conductor = segment.getConductor();
							addNewConductorToContext(iicdAssociatedSignal, conductor);
							if (!mPersistenceHandler.isUpdate()) {
								multicoreContext.registerReusedDanglingConductor(conductor);
							}
							return true;
						}
					}
				}
			}
		}

		// before going further and trying with non-dangling ends, see if any of the traversed pins are available for connection
		Predicate<IPin> noSignalPinFilter = (pin) -> {
			long matchingSignalCountAtPin = pin.getConductors().size();
			if (isNonSymbolledSplicePin(pin)) {
				return true;
			}
			else {
				return matchingSignalCountAtPin == 0;
			}
		};

		if (attemptConnectionAtPin(callBack, pins, jointComparator, noSignalPinFilter, placingPinCtrl)) {
			return true;
		}

		//first try with non-dangling ends without pins.
		for (Map.Entry<SchemGraphUtils.ISchemGraphNode, SchemGraphUtils.SchemVertex> entry : vertices.entrySet()) {
			SchemGraphUtils.SchemVertex schemVertex = entry.getValue();
			if (!schemVertex.isDanglingEnd() && schemVertex.canHaveSplice()) {
				Set<ISegment> segs = schemVertex.getSegments();
				if (!segs.isEmpty() && segs.size() < MAX_SEGS_AT_JOINING_JOINT) {
					ISegment segment = segs.iterator().next();
					if (placingPinCtrl.canSplit(segment)) {
						Point pt = schemVertex.getPoint();
						ILocation breakPoint = FactoryMgr.getCommonFactory().constructLocation(pt.x, pt.y);
						callBack.connect(segment, breakPoint, true);
						return true;
					}
				}
			}
		}

		//now try with an existing splice (non-symbolled)
		Predicate<IPin> splicePinFilter = ICDInterconnectStrategy::isNonSymbolledSplicePin;
		if (attemptConnectionAtPin(callBack, pins, jointComparator, splicePinFilter, placingPinCtrl)) {
			return true;
		}

		Map<ILocation, ISegment> splitSegments = new TreeMap<>(jointComparator);
		for (ISegment seg : segments) {
			splitSegments.put(getMiddleBreakPoint(seg, diagram), seg);
		}
		//now try with splitting a segment.
		if (!splitSegments.isEmpty()) {
			Map.Entry<ILocation, ISegment> entry = splitSegments.entrySet().iterator().next();
			ISegment segment = entry.getValue();
			if (placingPinCtrl.canSplit(segment)) {
				callBack.connect(segment, entry.getKey(), false);
				return true;
			}
		}
		//now try multi-term
		if (placingPinCtrl.canConnect(placedPin)) {
			callBack.connect(placedPin);
			return true;
		}
		return false;
	}

	@NotNull private Collection<IPin> getShortCircuitedPinsToContinueTraversal(IPin pin, final Set<IPin> pins)
	{
		Collection<IPin> nonVisitedMatedPins = CollectionUtils.getFilteredCollection(
				PinPlacementHelper.getConnectedSchemPins(pin), obj -> !(pins.contains(obj)));

		IPinList schemPinList = (IPinList) pin.getParent();
		if (schemPinList != null) {
			chs.cof.logical.cable.IPinList cablePinlist = schemPinList.getConnectivity();
			// if inline or plug/jack pair
			if (cablePinlist instanceof IConnector) {
				IPin matchingPin =
						ConnectionHelper.getMatchingPinForConnectorPin(pin, schemPinList, IConnector.class);
				if (matchingPin != null && !pins.contains(matchingPin)) {
					nonVisitedMatedPins.add(matchingPin);
				}
			}
			// if symbol with internal connectivity
			if (cablePinlist instanceof IDevice && ((IDevice) cablePinlist).hasInternalConnectivity()) {
				IGenericPin otherEnd = getOtherEndPinOfInternalLink(pin.getConnectivity());
				if (otherEnd != null) {
					Optional<IPin> pinOptional = schemPinList.getPins().stream()
							.filter(schemPin -> (schemPin.getConnectivity() == otherEnd))
							.findFirst();
					if (pinOptional.isPresent()) {
						IPin pinTraversedOnSymbol = pinOptional.get();
						if (!pins.contains(pinTraversedOnSymbol)) {
							nonVisitedMatedPins.add(pinTraversedOnSymbol);
						}
					}
				}
			}
			// if splice symbol with 2 pins - splice symbols with more than 2 pins are ignored as they lead to
			// multiple paths for traversal
			if (isSpliceSymbol(cablePinlist)) {
				if (cablePinlist.getPins().getSize() == 2) {
					Optional<IPin> unvisitedPin =
							schemPinList.getPins().stream().filter(splicePin -> splicePin != pin)
									.findAny();
					if (unvisitedPin.isPresent() && !pins.contains(unvisitedPin.get())) {
						nonVisitedMatedPins.add(unvisitedPin.get());
					}
				}
			}
		}

		for (IPin nonVisitedPin : nonVisitedMatedPins) {
			if (nonVisitedPin.getJoint() == null) {
				pins.add(nonVisitedPin);
			}
		}
		return nonVisitedMatedPins;
	}

	public static boolean isNonSymbolledSplicePin(IPin pin)
	{
		IPinList pinlist = getPinList(pin);
		if (pinlist == null) {
			return false;
		}
		ISymbolRef symbolRef = pinlist.getSymbolRef();
		return symbolRef == null && ConnectionHelper.isSplicePin(pin);
	}

	private boolean attemptConnectionAtPin(IInterconnectJoinCallback callBack, Set<IPin> pins,
			Comparator<ILocation> jointComparator, @NotNull Predicate<IPin> filter,
			IPlacingPinController placingPinCtrl)
	{
		//now try with an existing splice.
		Map<ILocation, IPin> spliceVertices = new TreeMap<>(jointComparator);
		for (IPin pin : pins) {
			if (filter.test(pin)) {
				IJoint joint = pin.getJoint();
				if (joint != null) {
					if (joint.getAssociations(ISegment.class).size() < MAX_SEGS_AT_JOINING_SPLICE) {
						spliceVertices.put(joint, pin);
					}
				}
				else {
					spliceVertices.put(pin.getLocation(), pin);
				}
			}
		}

		for (IPin splicePin : spliceVertices.values()) {
			IPinList splice = getPinList(splicePin);
			if (splice != null) {
				if (placingPinCtrl.canConnect(splicePin)) {
					callBack.connect(splicePin);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isSpliceSymbol(chs.cof.logical.cable.IPinList cablePinlist)
	{
		ISymbolRef symbolRef = cablePinlist.getSymbolRef();
		return symbolRef != null && (cablePinlist instanceof ISplice);
	}

	public static void updateAttributesAndPropOnPlacedSignal(chs.cof.logical.cable.IConductor placedConductor,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		if (placedConductor != null) {
			if (placedConductor.isShared()) {
				acquireLockAndUpdateAttribPropForSharedConductor(placedConductor, iicdAssociatedSignal);
			}
			else {
				updatePropAndAttribsForPlacedConductor(placedConductor, iicdAssociatedSignal);
			}
		}
	}

	private void collectSharedObjectsAndUpdateAttributesAndPropOnPlacedSignalForNonShared(
			@Nullable chs.cof.logical.cable.IConductor placedConductor,
			@NotNull IICDAssociatedSignal iicdAssociatedSignal)
	{
		if (placedConductor != null) {
			if (placedConductor.isShared()) {
				conductorsToSignalMap.put(placedConductor, iicdAssociatedSignal);
			} else {
				updatePropAndAttribsForPlacedConductor(placedConductor, iicdAssociatedSignal);
			}
		}
	}

	private static void acquireLockAndUpdateShortDescriptionForSharedConductor(
			chs.cof.logical.cable.IConductor connectivity, String signalName,
			ISystemObjectTypeInfoMgr objectTypeInfoMgr)
	{
		ISharedObject sharedConductor = connectivity.getSharedObject();
		if (sharedConductor != null) {
			boolean lockSuccess = false;
			try {
				lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedConductor, false);
				if (lockSuccess) {
					populateShortDescription(connectivity, signalName, objectTypeInfoMgr);
				}
				else {
					reportErrorMessage(sharedConductor);
				}
			}
			finally {
				if (lockSuccess) {
					LockUpdateHelper.flushAndUnlockSharedObject(sharedConductor);
				}
			}
		}
	}

	private static void acquireLockAndUpdateAttribPropForSharedConductor(chs.cof.logical.cable.IConductor connectivity,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		ISharedConductor sharedConductor = connectivity.getSharedConductor();
		if (sharedConductor != null) {
			boolean lockSuccess = false;
			boolean propertiesUpdated = false;
			try {
				if (!sharedConductor.isLocked()) {
					lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedConductor, false);
				}

				if (sharedConductor.isLocked()) {
					if (!sharedConductor.isFrozen()) {
						updatePropAndAttribsForPlacedConductor(connectivity, iicdAssociatedSignal);
						propertiesUpdated = true;
					}
					else {
						reportFrozenErrorMessage(sharedConductor);
					}
				}
				else {
					reportErrorMessage(sharedConductor);
				}
			}
			finally {
				if (lockSuccess) {
					if (propertiesUpdated) {
						LockUpdateHelper.flushAndUnlockSharedObject(sharedConductor);
					}
					else {
						LockUpdateHelper.unlock(sharedConductor);
					}
				}
				else if (sharedConductor.isLocked() && propertiesUpdated) {
					sharedConductor.flush();
				}
			}
		}
	}

	private static void reportErrorMessage(ISharedObject sharedConductor)
	{
		IReportValidationHandler handler = GeneralReportValidationHandler.getHandle(LogTabType.TAB_ICD);
		ILockable lockable = sharedConductor.getLockableUpdateableRoot();
		if (lockable instanceof ILockableDelegate) {
			lockable = ((ILockableDelegate) lockable).getLockableDelegate();
		}
		assert lockable != null;
		handler.report(PromptSeverity.ERROR, HTMLHelper.color(IColor.RED,
				ResourceMgr.getString(ICDInterconnectStrategy.class,
						"ICDInterconnectStrategy.sharedObjectLockError.message",
						LockInfoDialog.getCategoryAndNameForDisplay(lockable).getSecond())));
	}

	private static void reportFrozenErrorMessage(ISharedObject sharedConductor)
	{
		IReportValidationHandler handler = GeneralReportValidationHandler.getHandle(LogTabType.TAB_ICD);
		String message = ResourceMgr.getString(UpdateICDAction.class,
				"UpdateICDAction.output.isFrozen", HTMLHelper.link(sharedConductor));
		handler.report(PromptSeverity.ERROR, HTMLHelper.color(IColor.RED, message));
	}

	private static void updatePropAndAttribsForPlacedConductor(
			chs.cof.logical.cable.IConductor placedConductor,
			IICDAssociatedSignal iicdAssociatedSignal
	)
	{
		String signalName;

		String icdSignalName = iicdAssociatedSignal.getNetName();
		if (placedConductor instanceof IWireConductor) {
			signalName = ICDUtils.getSourceICDSignal(placedConductor);
			//If its wiring design and there is no source signal defined on wire than
			//update the source signal on the wire
			if (StringUtils.isBlank(signalName) || !signalName.equals(icdSignalName)) {
				ICDUtils.setSourceICDSignal(placedConductor, icdSignalName);
				signalName = icdSignalName;
			}
		}
		else {
			signalName = placedConductor.getName();
		}

		//Do not update if the signal is differant from icdSignal
		if (!StringUtils.isBlank(signalName) && signalName.equals(icdSignalName)) {
			//Update option expression
			ICDPlacementHelper.propagateOptionExpression(
					placedConductor,
					iicdAssociatedSignal
			);
			Set<String> propertiesThatCannotBeOverriden = new HashSet<>();
			IPropertiedObject libraryDevice = null;
			if (placedConductor.getLibraryObject() != null) {
				libraryDevice = (IPropertiedObject) placedConductor.getLibraryObject();
				propertiesThatCannotBeOverriden.addAll(ICDPlacementHelper.getAllPropertyNames(libraryDevice));
			}

			ICDUtils.copyPropertiesOntoPlacedObject(iicdAssociatedSignal, libraryDevice, placedConductor);
			//PropertyHelper.copyAllProperties(placedConductor, iicdAssociatedSignal);

			// copy the properties assoicated with oti name template for signal name
			propertiesThatCannotBeOverriden.addAll(ICDPlacementHelper.getAllPropertyNames(iicdAssociatedSignal));
			ISystemObjectTypeInfoMgr objectTypeInfoMgr =
					FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
			NameTemplateHelper
					.addPropertiesFromOTINameTemplate(
							objectTypeInfoMgr,
							signalName, placedConductor, propertiesThatCannotBeOverriden);
			populateShortDescription(placedConductor, signalName, objectTypeInfoMgr);
		}
	}

	private static void populateShortDescription(chs.cof.logical.cable.IConductor placedConductor, String signalName,
			ISystemObjectTypeInfoMgr objectTypeInfoMgr)
	{
		if (!StringUtils.isBlank(signalName) && !placedConductor.getType().equals(ISharedConductor.SHIELD_TYPE)) {
			IObjectTypeInfo objectTypeInfo = objectTypeInfoMgr.getByClass(INetConductor.class);
			if (objectTypeInfo != null) {
				INameTemplate nameTemplate = objectTypeInfo.getNameTemplateByName(signalName);
				if (nameTemplate != null) {
					try (DisableLanguageTranslation translation = new DisableLanguageTranslation()) {
						placedConductor.setShortDescription(nameTemplate.getShortDescription());
					}
				}
			}
		}
	}

	private int compareLocation(ILocation o1, ILocation o2, double refLocX, double refLocY)
	{
		return compareLocation(o1.getX(), o1.getY(), o2.getX(), o2.getY(), refLocX, refLocY);
	}

	private int compareLocation(int x1, int y1, int x2, int y2, double refLocX, double refLocY)
	{
		double dist1 = Point2D.distanceSq(x1, y1, refLocX, refLocY);
		double dist2 = Point2D.distanceSq(x2, y2, refLocX, refLocY);
		int compare = Double.compare(dist1, dist2);
		return (compare == 0) ? CommonUtils.comparePoints(x1, y1, x2, y2) : compare;
	}

	private int compareLocation(SchemGraphUtils.ISchemGraphNode o1, SchemGraphUtils.ISchemGraphNode o2, double refLocX,
			double refLocY)
	{
		return compareLocation(o1.getX(), o1.getY(), o2.getX(), o2.getY(), refLocX, refLocY);
	}

	private void routeBetweenDevices(IDevice placedDevice, List<IDeviceICD> icdsAssociatedWithDevice,
			final ISchemDiagram diagram, List<PlacingPinRouteInfo> placingPinRouteInfo)
	{
		SetMap<String, CaseLessStringKey> signalAssociations =
				ICDSignalDetailsFinder.getNetsAssociatedWithPin(icdsAssociatedWithDevice);
		IAbstractPinIterator pins = placedDevice.getPins();
		List<IAbstractPin> placedDevPinsToProcess = new ArrayList<>(pins.getSize());
		for (IAbstractPin placedDevPin : pins) {
			if (!areAllSchemPinsReference(placedDevPin, diagram)) {
				placedDevPinsToProcess.add(placedDevPin);
			}
		}

		final Map<IICDBackshell, IBackshell> icdToLogicBackshellMapForPlacedDev =
				ICDUtils.determineICDBackshellToLogicBackshellMapping(placedDevice, icdsAssociatedWithDevice);
		final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdTerminationInfoForPlacedDev =
				ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(icdsAssociatedWithDevice);
		final SetMap<IICDBackshellTermination, CaseLessStringKey> termSignalAssociations =
				ICDSignalDetailsFinder.getTermNetsAssociatedWithPin(icdsAssociatedWithDevice);
		for (PlacingPinRouteInfo pinRouteInfo : placingPinRouteInfo) {
			//try to connect with placed device pins
			for (IAbstractPin placedDevPin : placedDevPinsToProcess) {
				Set<CaseLessStringKey> netsAssociatedWithPin = signalAssociations
						.pullReadOnlySafeSet(StringUtils.nonNull(ICDUtils.getICDMatchName(placedDevPin)));
				routeBetweenPins(diagram, placedDevPin, pinRouteInfo.getPlacingDevPin(), pinRouteInfo.getPlacingPin(),
						netsAssociatedWithPin, pinRouteInfo.getAssociatedSignals());
			}
			//try to connect with placed backshell terminations
			for (Map.Entry<IICDBackshellTermination, Set<IICDAssociatedSignal>> entry :
					icdTerminationInfoForPlacedDev.entrySet()) {
				IICDBackshellTermination icdBSTerm = entry.getKey();
				final IBackshellTermination placedTerm =
						ICDUtils.determineMatchingBSTerm(icdBSTerm, icdToLogicBackshellMapForPlacedDev);
				if (placedTerm == null) {
					continue;
				}
				Set<CaseLessStringKey> netsAssociatedWithPlacedTerm =
						termSignalAssociations.pullReadOnlySafeSet(icdBSTerm);
				routeBetweenPins(diagram, placedTerm, pinRouteInfo.getPlacingDevPin(), pinRouteInfo.getPlacingPin(),
						netsAssociatedWithPlacedTerm, pinRouteInfo.getAssociatedSignals());
			}
		}

		// update attributes and properties for shared objects
		Collection<ISharedConductor> sharedConductors = collectObjectsToPreLock();
		BatchLockRefreshHelper.batchLockWithPromise(sharedConductors,
				()->updateAttributesPropsOnPlacedSignalForSharedObjects());
		conductorsToSignalMap.clear();
	}

	@NotNull
	private Collection<ISharedConductor> collectObjectsToPreLock()
	{
		Collection<ISharedConductor> sharedConductorsToPreLock = new HashSet<>();
		for (Map.Entry<chs.cof.logical.cable.IConductor, IICDAssociatedSignal> entry : conductorsToSignalMap.entrySet()) {
			chs.cof.logical.cable.IConductor conductor = entry.getKey();
			ISharedConductor sharedConductor = conductor.getSharedConductor();
			if (sharedConductor != null) {
				if (!sharedConductor.isLocked()) {
					sharedConductorsToPreLock.add(sharedConductor);
				}

			}
		}
		return sharedConductorsToPreLock;
	}

	private void updateAttributesPropsOnPlacedSignalForSharedObjects()
	{
		for(Map.Entry<chs.cof.logical.cable.IConductor,IICDAssociatedSignal> entry : conductorsToSignalMap.entrySet())
		{
			chs.cof.logical.cable.IConductor conductor = entry.getKey();
			IICDAssociatedSignal signal = entry.getValue();
			acquireLockAndUpdateAttribPropForSharedConductor(conductor,signal);
		}
	}

	private void routeBetweenPins(ISchemDiagram diagram, IAbstractPin placedLogicPin,
			IPin placingDevPin, IPin placingPin, Set<CaseLessStringKey> netsAssociatedWithPin,
			Collection<IICDAssociatedSignal> associatedSignals)
	{
		for (IICDAssociatedSignal signal : associatedSignals) {
			if (signal.isShieldWire()) {
				continue;
			}
			if (netsAssociatedWithPin.contains(CaseLessStringKey.toKey(signal.getNetName()))) {
				connectPinsWithSignal(diagram, placedLogicPin, placingDevPin, placingPin, signal, associatedSignals);
			}
		}
	}

	private void connectPinsWithSignal(ISchemDiagram diagram, IAbstractPin placedLogicPin, IPin placingDevPin,
			IPin placingPin, IICDAssociatedSignal signal, Collection<IICDAssociatedSignal> associatedSignals)
	{
		List<IICDAssociatedSignal> otherValidSignals = associatedSignals.stream()
				.filter(associatedSignal -> associatedSignal != signal)
				.collect(Collectors.toList());
		IPin sourcePin = checkConnectedConnectorPinIfSourceIsDevicePin(placingPin);
		IAbstractPin destinationPin = placedLogicPin;
		IPlacingPinController placingPinCtrl = getPlacingPinController(placingPin, placingDevPin, signal);
		List<IPin> placedPinsInPrecedence = getPlacedPinsInPrecedence(destinationPin, placingPinCtrl, diagram);
		if (placedPinsInPrecedence.isEmpty()) {
			return;
		}
		// should the above placed pins also be converted into harness connector pins

		Set<IPin> pinsTraversedFromPlacedPin = new HashSet<>();
		List<SignalPathTree> placedPinTrees = new ArrayList<>();
		for (IPin placedPin : placedPinsInPrecedence) {
			SignalPathTree tree = new SignalPathTreeHelper(placedPin, sourcePin,
					Collections.emptySet(), getCableConductorType()).buildSignalPathTree(
					placedPin, pinsTraversedFromPlacedPin, otherValidSignals);
			placedPinTrees.add(tree);

			// see if any of the leaf nodes is the pin of our interest
			Optional<SignalPathTree> desiredLeafNode =
					tree.getLeafNodes().stream()
							.filter(node -> node.getPin() != null)
							.filter(node -> node.getPin().equals(sourcePin))
							.findFirst();
			if (desiredLeafNode.isPresent()) {
				List<IPin> pinsOnTheDesiredPath = SignalPathTreeHelper.getPathToRoot(desiredLeafNode.get());
				updatePropertiesOnThePath(pinsOnTheDesiredPath, signal, diagram, otherValidSignals);
				return;
			}
		}

		// there exists no path between the pins, so build a tree from source pin too and then connect
		SignalPathTree nodeOnPlacedPinTree = getBestFitNode(placedPinTrees);
		List<IPin> path1 = SignalPathTreeHelper.getPathToRoot(nodeOnPlacedPinTree);
		// exclude pinlists traversed in the path (from side of placed pin) that we would using to connect, to avoid trying to connect between pins of an inline
		Set<IPinList> pinlistsToSkip = path1.stream()
				.map(pin -> getPinList(pin))
				.filter(pinList -> pinList.getConnectivity() != destinationPin.getOwner()) // remove placedICD
				.filter(pinList -> pinList.getConnectivity() !=
						sourcePin.getConnectivity().getOwner()) // remove placingICD
				.collect(Collectors.toSet());

		Set<IPin> pinsTraversedFromPlacingPin = new HashSet<>();
		SignalPathTree placingPinTree =
				new SignalPathTreeHelper(sourcePin, null, pinlistsToSkip, getCableConductorType())
						.buildSignalPathTree(sourcePin, pinsTraversedFromPlacingPin, otherValidSignals);

		// get leaf nodes from above trees
		SignalPathTree nodeOnPlacingPinTree = getBestFitNode(Collections.singletonList(placingPinTree));

		// establish connection now
		List<IPin> path2 = SignalPathTreeHelper.getPathToRoot(nodeOnPlacingPinTree);

		IPin placingPinToUse = nodeOnPlacingPinTree.getPin() != null ? nodeOnPlacingPinTree.getPin() :
				nodeOnPlacingPinTree.getParent().getPin();
		IInterconnectJoinCallback callBack = getInterconnectJoinCallback(placingPinToUse, diagram, signal);

		// update placingPinCtrl
		placingPinCtrl = getPlacingPinController(placingPinToUse, placingPinToUse, signal);

		if (!connectPinsTraversedFromPlacingAndPlacedPins(nodeOnPlacedPinTree.getPin(), placingPinCtrl, diagram,
				callBack,
				signal)) {
			// if all fails, connect placing pin and placed pin
			IPin anyPlacedPin = placedPinsInPrecedence.get(0);
			if (placingPinCtrl.canConnect(anyPlacedPin)) {
				callBack.connect(anyPlacedPin);
			}
			else {
				placingPinCtrl.registerDanglingConnection(anyPlacedPin);
			}
		}
		updatePropertiesOnThePath(path1, signal, diagram, otherValidSignals);
		updatePropertiesOnThePath(path2, signal, diagram, otherValidSignals);
		updatePropertiesOnThePath(Arrays.asList(placingPinToUse, nodeOnPlacedPinTree.getPin()), signal, diagram,
				otherValidSignals);
	}

	protected abstract void updateToNewConnectivity(IConductor conductor, IICDAssociatedSignal signal,
			ISchemDiagram diagram);

	public void updatePropertiesOnThePath(List<IPin> pinsOnPath, IICDAssociatedSignal signal, ISchemDiagram diagram,
			@Nullable List<IICDAssociatedSignal> otherValidSignalsToBeSkipped)
	{
		if (pinsOnPath.size() < 2) {
			return;
		}

		for (int i = 0; i < pinsOnPath.size() - 1; i++) {
			IPin firstPin = pinsOnPath.get(i);
			IPin secondPin = pinsOnPath.get(i + 1);

			Optional<IConductor> conductorOnPath = firstPin.getConductors().stream()
					.filter(conductor -> getCableConductorType()
							.isAssignableFrom(conductor.getConnectivity().getClass()))
					.filter(conductor -> conductor.getPins().contains(secondPin))
					.filter(conductor -> !SignalPathTreeHelper
							.isConductorReferringToOtherValidSignal(conductor, otherValidSignalsToBeSkipped))
					.findFirst();

			if (conductorOnPath.isPresent()) {
				IConductor schemConductor = conductorOnPath.get();
				collectSharedObjectsAndUpdateAttributesAndPropOnPlacedSignalForNonShared(schemConductor.getConnectivity(), signal);
				ConductorMerger.processCompositeDecorationTexts(schemConductor);
				// also correct the net name
				updateToNewConnectivity(schemConductor, signal, diagram);
			}
		}
	}

	private SignalPathTree getBestFitNode(List<SignalPathTree> rootNodes)
	{
		List<SignalPathTree> allLeafNodes = new ArrayList<>();
		for (SignalPathTree rootNode : rootNodes) {
			allLeafNodes.addAll(rootNode.getLeafNodes());
		}

		// there can't be empty leaf nodes, ensure that, fail gracefully in such case

		// 1st preference - dangling node of highest depth
		Optional<SignalPathTree> danglingNode = allLeafNodes.stream()
				.filter(node -> node.getPin() == null)
				.sorted((a, b) -> Integer.compare(b.getDepth(), a.getDepth()))
				.findFirst();

		if (danglingNode.isPresent()) {
			return danglingNode.get().getParent();
		}

		List<SignalPathTree> allNodes = new ArrayList<>();
		for (SignalPathTree rootNode : rootNodes) {
			allNodes.addAll(rootNode.getAllNodes());
		}
		// 2nd preference - open pin with no conductors on it and nothing can be traversed further from it
		Optional<SignalPathTree> noConductorNode = allNodes.stream()
				.filter(node -> node.getPin().getConductors().isEmpty())
				.filter(node -> PinPlacementHelper.getConnectedSchemPins(node.getPin()).isEmpty())
				.filter(node -> !node.getPin().isReference())
				.sorted((a, b) -> Integer.compare(b.getDepth(), a.getDepth()))
				.findFirst();

		if (noConductorNode.isPresent()) {
			return noConductorNode.get();
		}

		// if reference pin, parent should be returned ??
		SignalPathTree bestFitNode = allLeafNodes.stream()
				.sorted((a, b) -> Integer.compare(b.getDepth(), a.getDepth())).iterator().next();
		return bestFitNode.getPin().isReference() ? bestFitNode.getParent() : bestFitNode;
	}

	protected boolean hasPlacingPinAlreadyGotTheSignal(IPin placingPin, IICDAssociatedSignal signal,
			IAbstractPin placedPin)
	{
		return false;
	}

	private List<IPin> getPlacedPinsInPrecedence(IAbstractPin abstractPin, IPlacingPinController placingPinCtrl,
			ISchemDiagram diagram)
	{
		List<IPin> devPins = new ArrayList<>();
		List<IPin> connectorPins = new ArrayList<>();
		for (IDiagramObject diagramObject : diagram.getRepresentations(abstractPin.getUID())) {
			final IPin placedDevicePin = CommonUtils.cast(diagramObject, IPin.class);
			if (placedDevicePin == null) {
				continue;
			}
			final IPinList placedDevice = CommonUtils.cast(diagramObject.getParent(), IPinList.class);
			if (placedDevice == null) {
				continue;
			}
			IPin matchedPin = m_SchemDeviceICDPinInfoCache.getICDSignalSourceSchemPinlist(placedDevice)
					.getConnectedSchemHarnConnectorPin(placedDevicePin);
			if (matchedPin != null) {
				connectorPins.add(matchedPin);
			}
			else {
				devPins.add(placedDevicePin);
			}
		}

		ILocation placingPinLoc = placingPinCtrl.getPlacingPinReferenceLocation();
		final double placingPinLocX = placingPinLoc.getX();
		final double placingPinLocY = placingPinLoc.getY();
		Comparator<IPin> jointComparator =
				(o1, o2) -> compareLocation(o1.getAbsLocation(), o2.getAbsLocation(), placingPinLocX, placingPinLocY);
		Collections.sort(devPins, jointComparator);
		Collections.sort(connectorPins, jointComparator);
		List<IPin> placedPins = new ArrayList<>();
		//first connected device pins then un-connected pins
		placedPins.addAll(connectorPins);
		placedPins.addAll(devPins);
		return Collections.unmodifiableList(placedPins);
	}

//	private void doTraverseAndJoinPinToSegment(IAbstractPin placedPin, final IPin placingPin,
//			final IPin placingRefPin, final ISchemDiagram diagram, final IICDAssociatedSignal iicdAssociatedSignal)
//	{
//		IInterconnectJoinCallback callBack = getInterconnectJoinCallback(placingPin, diagram, iicdAssociatedSignal);
//
//		IPlacingPinController placingPinCtrl = getPlacingPinController(placingPin, placingRefPin, iicdAssociatedSignal);
//		traverseAndJoinPinsBySignal(placedPin, placingPinCtrl, diagram, callBack, iicdAssociatedSignal);
//	}

	@NotNull private IInterconnectJoinCallback getInterconnectJoinCallback(final IPin placingPin,
			final ISchemDiagram diagram, final IICDAssociatedSignal iicdAssociatedSignal)
	{
		return new IInterconnectJoinCallback()
		{
			@Override
			public void connect(ISegment segment, ILocation breakLoc, boolean isJoinAtEnd)
			{
				connectPinWithExistingSegment(placingPin, segment, breakLoc, iicdAssociatedSignal, diagram);
			}

			@Override
			public void connect(IPin placedPin)
			{
				if (!reuseCondAndConnectToPin(placedPin, placingPin, diagram, iicdAssociatedSignal)) {
					constructCondAndConnectToPin(placedPin, placingPin, diagram, iicdAssociatedSignal);
				}
			}
		};
	}

	private boolean reuseCondAndConnectToPin(IPin placedPin, IPin placingPin, ISchemDiagram diagram,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		// if placing pin has already got conductor of our type (logical/wiring match), use it
		Collection<IConductor> reusableConds = placingPin.getConductors().stream()
				.filter(cond -> getCableConductorType().isAssignableFrom(cond.getConnectivity().getClass()))
				.collect(Collectors.toList());

		for (IConductor cond : reusableConds) {
			for (ISegment segment : cond.getSegmentsOfType(ISegment.class)) {
				if (SegmentHelper.isOpenEndedSegment(segment)) {
					ILocation location = SegmentHelper.getAppropriatePointSnappedToGrid(segment);
					connectPinWithExistingSegment(placedPin, segment, location, iicdAssociatedSignal, diagram);
					//addNewConductorToContext(iicdAssociatedSignal, cond); // is this required here or should Update ICD consider all conductors for processing by default
					//or should the above call be placed in connectPinWithExistingSegment, but it would affect AddPin flow too, so should Update ICD take care of processing all
					//conductors instead of just those in multicore context
					return true;
				}
			}
		}
		return false;
	}

	public IPin getPinOtherEnd(IPin sourcePin, IICDAssociatedSignal iicdAssociatedSignal)
	{
		return getPinOtherEnd(sourcePin, iicdAssociatedSignal, null);
	}

	public IPin getPinOtherEnd(IPin sourcePin, IICDAssociatedSignal iicdAssociatedSignal,
			@Nullable IConductor conductorToExclude)
	{
		// from source pin, collect conductors of the desired signal and if any of such conductors terminates on
		// inline or plug-jack pair, get the mate of that
		// symbol, get the pin that's internally connected to it
		List<IConductor> matchingConductors = sourcePin.getConductors().stream()
				.filter(cond -> conductorToExclude == null || cond != conductorToExclude)
				.filter(cond -> iicdAssociatedSignal.getNetName().equals(ICDUtils
						.getAssociatedSignalNameForConductor(cond.getConnectivity(), isWiringAbstraction())))
				.collect(Collectors.toList());

		for (IConductor matchingConductor : matchingConductors) {
			for (IPin pin : matchingConductor.getPins()) {
				if (pin != sourcePin) {
					IPinList schemPinList = (IPinList) pin.getParent();
					if (schemPinList == null) {
						continue;
					}
					chs.cof.logical.cable.IPinList cablePinlist = schemPinList.getConnectivity();
					// if inline or plug/jack pair
					if (cablePinlist instanceof IConnector) {
						IPin matchingPin =
								ConnectionHelper.getMatchingPinForConnectorPin(pin, schemPinList, IConnector.class);
						if (matchingPin != null) {
							return getPinOtherEnd(matchingPin, iicdAssociatedSignal);
						}
					}
					// if symbol with internal connectivity
					if (cablePinlist instanceof IDevice && ((IDevice) cablePinlist).hasInternalConnectivity()) {
						IGenericPin otherEnd = getOtherEndPinOfInternalLink(pin.getConnectivity());
						if (otherEnd != null) {
							Optional<IPin> pinOptional = schemPinList.getPins().stream()
									.filter(schemPin -> (schemPin.getConnectivity() == otherEnd))
									.findFirst();
							if (pinOptional.isPresent()) {
								return getPinOtherEnd(pinOptional.get(), iicdAssociatedSignal);
							}
						}
					}
					// if splice with only one pin
					if (cablePinlist instanceof ISplice && (cablePinlist.getNumPins() == 1)) {
						IPin splicePin = schemPinList.getPins().iterator().next();
						if (splicePin.getConductors().size() == 2) {
							// we need to traverse further only if there are two conductors this splice pin
							// one through which we have come here and one through we need to move forward
							return getPinOtherEnd(splicePin, iicdAssociatedSignal, matchingConductor);
						}
						else {
							// can't traverse further because of multiple paths, so use this for connection
							return splicePin;
						}
					}
					// if splice symbol
					if (isSpliceSymbol(cablePinlist)) {
						if (cablePinlist.getPins().getSize() == 2) {
							Optional<IPin> unvisitedPin =
									schemPinList.getPins().stream().filter(splicePin -> splicePin != pin)
											.findAny();
							if (unvisitedPin.isPresent()) {
								return getPinOtherEnd(unvisitedPin.get(), iicdAssociatedSignal);
							}
						}
					}
				}
			}
		}
		return sourcePin;
	}

	@NotNull private IPin checkConnectedConnectorPinIfSourceIsDevicePin(@NotNull IPin sourcePin)
	{
		IPinList sourcePL = CommonUtils.cast(sourcePin.getParent(), IPinList.class);
		IDevice device = sourcePL != null ? CommonUtils.cast(sourcePL.getConnectivity(), IDevice.class) : null;
		IPin matchedDevConnectedHarnConnPin = null;
		if (device != null) {
			matchedDevConnectedHarnConnPin = m_SchemDeviceICDPinInfoCache.getICDSignalSourceSchemPinlist(sourcePL)
					.getConnectedSchemHarnConnectorPin(sourcePin);
		}
		return matchedDevConnectedHarnConnPin != null ? matchedDevConnectedHarnConnPin : sourcePin;
	}

	@Nullable private IGenericPin getOtherEndPinOfInternalLink(IGenericPin pin)
	{
		IGenericPin pinOfInterest = pin;
		List<IInternalLink> traversedInternalLinks = new LinkedList<>();
		Collection<IInternalLink> internalLinks = PinUtils.getInternalLinks(pinOfInterest);
		IPipelineStreamExecutable<IInternalLink, List<IInternalLink>> streamExecutable =
				PipelineStream.<IInternalLink>stream()
						.filter(link -> !traversedInternalLinks.contains(link))
						.collect(Collectors.toList());
		Collection<IInternalLink> untraversedInternalLinks =
				streamExecutable.execute(PipelineStreamInput.of(internalLinks));

		while (untraversedInternalLinks.size() == 1) {
			IInternalLink internalLink = untraversedInternalLinks.iterator().next();

			IGenericPin startPin = internalLink.getStartPin();
			IGenericPin endPin = internalLink.getEndPin();

			if (startPin.equals(pinOfInterest)) {
				pinOfInterest = endPin;
			}
			else if (endPin.equals(pinOfInterest)) {
				pinOfInterest = startPin;
			}
			if (pinOfInterest instanceof IAbstractPin) {
				return pinOfInterest;
			}
			else {
				// keep traversing further
				internalLinks = PinUtils.getInternalLinks(pinOfInterest);
				traversedInternalLinks.add(internalLink);
				untraversedInternalLinks =
						streamExecutable.execute(PipelineStreamInput.of(internalLinks));
			}
		}
		return null;
	}

	@NotNull IPlacingPinController getPlacingPinController(final IPin placingPin, final IPin placingRefPin,
			final IICDAssociatedSignal iicdAssociatedSignal)
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
				return true;
			}

			@Override public boolean canConnect(ISegment segment)
			{
				return true;
			}

			@Override public boolean canSplit(ISegment segment)
			{
				// do not allow splitting a segment that is connected to itself
				return !(segment.getConductor().getPins().contains(placingPin));
			}

			@Override public void registerDanglingConnection(IPin placedPin)
			{

			}
		};
	}

	protected abstract Class<? extends chs.cof.logical.cable.IConductor> getCableConductorType();

	protected abstract chs.cof.logical.cable.IConductor constructNewCableConductor(ISchemDiagram diagram,
			IICDAssociatedSignal associatedSignal);

	@NotNull
	protected Set<IConductor> constructCondAndConnectToPin(IPin placedPin, IPin placingPin, ISchemDiagram diagram,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		chs.cof.logical.cable.IConductor cableCond;
		// if either placedPin or placingPin is a splitting object create new conductor
		if (SignalPathTreeHelper.isPinOnASplittingObject(placedPin) ||
				SignalPathTreeHelper.isPinOnASplittingObject(placingPin)) {
			cableCond = constructNewCableConductor(diagram, iicdAssociatedSignal);
		}
		else {
			cableCond = getCableConductorToJoinWithPin(diagram, iicdAssociatedSignal);
		}
		if(cableCond == null) {
			return Collections.emptySet();
		}
		updateAttributesAndPropOnPlacedSignal(cableCond, iicdAssociatedSignal);
		IConductor net =
				constructSchemCondAndConnectToPin(placedPin, placingPin, diagram, cableCond);
		chs.cof.logical.cable.IConductor cableConductor = net.getConnectivity();
		allVisitedConductors.put(cableConductor.getName(), cableConductor);
		addNewConductorToContext(iicdAssociatedSignal, net);
		return Collections.singleton(net);
	}

	@NotNull
	protected IConductor constructSchemCondAndConnectToPin(IPin placedPin, IPin placingPin, ISchemDiagram diagram,
			chs.cof.logical.cable.IConductor cableCond)
	{
		IConductor net = getNewSchemConductor(diagram, placedPin.getAbsLocation(), placingPin.getAbsLocation(),
				cableCond);
		ISegment segment = (ISegment) net.getSegments().iterator().next();
		segment.connectPin(placedPin);
		segment.connectPin(placingPin);
		IPinList placedPL = getPinList(placedPin);
		IPinList placingPL = getPinList(placingPin);
		if (placedPL != null) {
			ConductorRouteAction.getInstance().addPinListForRoute(placedPL);
		}
		if (placingPL != null) {
			ConductorRouteAction.getInstance().addPinListForRoute(placingPL);
		}
		ConductorRouteAction.getInstance().addConductorForRoute(net);
		return net;
	}

	protected void createShieldInMulticore(@NotNull Set<IMulticore> multicoresForShielding, ISchemDiagram diagram)
	{
		// scan the ICD devices in ICD library applicable to this design and if any of their pins have got a signal of type shield
		// whose cable specification points to any of the multicore received as argument,
		// then create a shield conductor and add it to the multicore

		ILogicDesign design = diagram.getDesign();
		if (design == null) {
			return;
		}
		List<IDeviceICD> icds = new ArrayList<>();
		icds.addAll(diagram.getDesign().getDesignICDContainer().getApplicableICDsWithDesignAssociation());
		List<IICDAssociatedSignal> icdSignalsOfInterest = ICDSignalDetailsFinder.getICDSignalsOfInterest(icds);
		createShieldInConnectivityIfNeeded(multicoresForShielding, diagram, icdSignalsOfInterest);
	}

	private void createShieldInConnectivityIfNeeded(@NotNull Set<IMulticore> multicoresForShielding, @NotNull ISchemDiagram diagram,
													@NotNull List<IICDAssociatedSignal> icdSignalsOfInterest)
	{
		Map<IShieldConductor, Map<IICDAssociatedSignal, IMulticore>> multicoreToShieldSignalMap = new HashMap<>();
		for (IICDAssociatedSignal signal : icdSignalsOfInterest) {
			for (IMulticore multicore : multicoresForShielding) {
				if (ShieldBuilder.isSignalAShieldForThisMulticore(signal, multicore)) {
					ShieldBuilder.createShieldInConnectivity(multicore, signal, diagram, multicoreContext,
							mPersistenceHandler.getLockTracker(), multicoreToShieldSignalMap);
				}
			}
		}
		preLockObjects(diagram, multicoreToShieldSignalMap);
	}

	private void preLockObjects(@NotNull ISchemDiagram diagram,
								@NotNull Map<IShieldConductor, Map<IICDAssociatedSignal, IMulticore>> multicoreToShieldSignalMap)
	{
		Set<IShieldConductor> shieldConductors = multicoreToShieldSignalMap.keySet();
		Collection<ISharedObject> sharedObjectsToPreLock = collectObjectsToPreLock(shieldConductors);
		BatchLockRefreshHelper.batchLockWithPromise(sharedObjectsToPreLock,
				() -> updateAttributesAndPropOnPlacedSignal(diagram, multicoreToShieldSignalMap));
	}

	@NotNull
	private static Collection<ISharedObject> collectObjectsToPreLock(@NotNull Set<IShieldConductor> shieldConductors)
	{
		return shieldConductors.stream()
				.filter(shieldConductor -> shieldConductor.isShared())
				.map(shieldConductor -> shieldConductor.getSharedConductor())
				.filter(sharedConductor -> sharedConductor != null)
				.collect(Collectors.toSet());
	}

	private void updateAttributesAndPropOnPlacedSignal(
			@NotNull ISchemDiagram diagram,
			@NotNull Map<IShieldConductor, Map<IICDAssociatedSignal, IMulticore>> multicoreToShieldSignalMap)
	{
		for (Map.Entry<IShieldConductor, Map<IICDAssociatedSignal, IMulticore>> outerMap : multicoreToShieldSignalMap.entrySet()) {
			Map<IICDAssociatedSignal, IMulticore> signalToMulticoreMap = outerMap.getValue();
			IShieldConductor shieldConductor = outerMap.getKey();
			for (Map.Entry<IICDAssociatedSignal, IMulticore> innerMap : signalToMulticoreMap.entrySet()) {
				IICDAssociatedSignal signal = innerMap.getKey();
				IMulticore multicore = innerMap.getValue();
				updateAttributesAndPropOnPlacedSignal(shieldConductor, signal);
				assert diagram.getDesign() != null;
				ShieldBodyHelper.createShieldBodyHookups(diagram.getDesign(), multicore.getShieldBody());
			}
		}
	}

	protected void connectPinsToHookupOfThisMulticore(@NotNull Set<IMulticore> multicoresForShielding,
			ISchemDiagram diagram)
	{
		// scan the ICD devices in the current diagram and if any of their placed pins have got a signal of type shield
		// whose cable specification points to any of the multicore received as argument,
		// connect the pins to hookup by creating schem shield conductors

		DeviceToICDMatcher icdMatcher = new DeviceToICDMatcher(diagram);
		Map<IDevice, List<IDeviceICD>> placedDeviceToICDs = icdMatcher.getPlacedDeviceToICDsInPrecedence();
		if (placedDeviceToICDs.isEmpty()) {
			return;
		}

		for (Map.Entry<IDevice, List<IDeviceICD>> entry : placedDeviceToICDs.entrySet()) {
			IDevice placedDevice = entry.getKey();
			List<IDeviceICD> icdsAssociatedWithDevice = entry.getValue();
			if (icdsAssociatedWithDevice.isEmpty()) {
				continue;
			}
			ListMap<String, IICDAssociatedSignal> icdShieldSignalsAssociated =
					ICDSignalDetailsFinder.getICDShieldSignalsAssociated(icdsAssociatedWithDevice);
			for (IMulticore multicore : multicoresForShielding) {
				placedDevice.getPins().stream().forEach(pin -> icdShieldSignalsAssociated
						.pullReadOnlySafeList(StringUtils.nonNull(ICDUtils.getICDMatchName(pin))).stream()
						.filter(signal -> ShieldBuilder.isSignalAShieldForThisMulticore(signal, multicore))
						.forEach(signal -> ShieldBuilder.createSchemShieldConductors(pin, multicore, diagram,
								this, signal)));
			}
			if (icdsAssociatedWithDevice.size() == 1) {
				createShieldsOnBackshellTerminations(multicoresForShielding, diagram, placedDevice,
						icdsAssociatedWithDevice);
			}
		}
	}

	private void createShieldsOnBackshellTerminations(@NotNull Collection<IMulticore> multicoresForShielding,
			ISchemDiagram diagram, IDevice placedDevice, List<IDeviceICD> icdsAssociatedWithDevice)
	{
		createShieldsOnBackshellTerminations(multicoresForShielding, diagram, placedDevice, icdsAssociatedWithDevice,
				ICDInterconnectPreprocessor.getTerminationsToProcess(diagram, placedDevice));
	}

	private void createShieldsOnBackshellTerminations(@NotNull Collection<IMulticore> multicoresForShielding,
			ISchemDiagram diagram, IPinList placedDevice, List<IDeviceICD> icdsAssociatedWithDevice)
	{
		IDevice connectivity = CommonUtils.cast(placedDevice.getConnectivity(), IDevice.class);
		if (connectivity == null) {
			return;
		}
		createShieldsOnBackshellTerminations(multicoresForShielding, diagram, connectivity, icdsAssociatedWithDevice,
				ICDInterconnectPreprocessor.getTerminationsToProcess(placedDevice));
	}

	private void createShieldsOnBackshellTerminations(@NotNull Collection<IMulticore> multicoresForShielding,
			ISchemDiagram diagram, IDevice placedDevice, List<IDeviceICD> icdsAssociatedWithDevice,
			SetMap<IBackshellTermination, IPin> schemTermCandidates)
	{
		Map<IICDBackshell, IBackshell> icdToLogicBackshellMap =
				ICDUtils.determineICDBackshellToLogicBackshellMapping(placedDevice, icdsAssociatedWithDevice);

		final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdTerminationInfo =
				ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(icdsAssociatedWithDevice);
		for (Map.Entry<IICDBackshellTermination, Set<IICDAssociatedSignal>> terminationSetEntry : icdTerminationInfo
				.entrySet()) {
			final IICDBackshellTermination icdTerm = terminationSetEntry.getKey();
			final IBackshell backshell = icdToLogicBackshellMap.get(icdTerm.getBackshell());
			if (backshell == null) {
				continue;
			}
			final IBackshellTermination term = backshell.findBackshellTerminationByName(icdTerm.getName());
			if (term == null) {
				continue;
			}
			for (IICDAssociatedSignal signal : terminationSetEntry.getValue()) {
				for (IMulticore multicore : multicoresForShielding) {
					if (ShieldBuilder.isSignalAShieldForThisMulticore(signal, multicore)) {
						Set<IPin> schemPins = schemTermCandidates.pullReadOnlySafeSet(term);
						ShieldBuilder.doCreateShieldOnPin(diagram, schemPins, signal, multicore, multicoreContext,
								this, mPersistenceHandler.getLockTracker());
					}
				}
			}
		}
	}

	@Nullable public static String getICDPathFromRootMulticore(IMulticore multicore)
	{
		IProperty sourceProperty = multicore.findPropertyByName(ICDMulticore.SOURCE_CABLE_NAME);
		if (sourceProperty != null) {
			String icdPath = sourceProperty.getAsString();
			if (multicore.getParent() != null) {
				String icdPathForParent = getICDPathFromRootMulticore(multicore.getParent());
				if (icdPathForParent != null) {
					return icdPath + ICDUtils.MC_PATH_SEPARATOR + icdPathForParent;
				}
			}
			else {
				return icdPath;
			}
		}
		return null;
	}

	protected IConductor getNewSchemConductor(ISchemDiagram diagram, ILocation pt1, ILocation pt2,
			@Nullable chs.cof.logical.cable.IConductor cableCond)
	{
		List<Point> points = new ArrayList<Point>();
		points.add(GfxUtils.getPoint(pt1));
		points.add(GfxUtils.getPoint(pt2));
		CreateSchemConductorCmd cmd = new CreateSchemConductorCmd(diagram, points, getCableConductorType());
		cmd.setCableConductor(cableCond);
		cmd.execute();
		return cmd.getConductor();
	}

	protected void registerToDesign(ISchemDiagram diagram, chs.cof.logical.cable.IConductor cableCond)
	{
		ILogicDesign design = diagram.getDesign();
		assert design != null;
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		connectivity.addConductor(cableCond);
	}

	protected chs.cof.logical.cable.IConductor getCableConductorToJoinExistingConductor(ISchemDiagram diagram,
			IICDAssociatedSignal associatedSignal, IConductor existingNet)
	{
		return existingNet.getConnectivity();
	}

	protected chs.cof.logical.cable.IConductor getCableConductorToJoinWithPin(ISchemDiagram diagram,
			IICDAssociatedSignal associatedSignal)
	{
		return constructNewCableConductor(diagram, associatedSignal);
	}

	private ILocation getMiddleBreakPoint(ILogicSegment firstSegment, ISchemDiagram diagram)
	{
		ICommonFactory commFact = FactoryMgr.getCommonFactory();
		ILocation startPoint = firstSegment.getStartPoint();
		ILocation endPoint = firstSegment.getEndPoint();
		IGrid grid = diagram.getGrid();
		return commFact.constructLocation(grid.snap((startPoint.getX() + endPoint.getX()) / 2),
				grid.snap((startPoint.getY() + endPoint.getY()) / 2));
	}

	private void connectPinWithExistingSegment(IPin placingPin, ISegment firstSegment, ILocation breakPoint,
			IICDAssociatedSignal iicdAssociatedSignal, ISchemDiagram diagram)
	{
		IConductor existingNet = firstSegment.getConductor();
		String netName = iicdAssociatedSignal.getNetName();
		ILocation startPoint = firstSegment.getStartPoint();
		ILocation endPoint = firstSegment.getEndPoint();
		ILocation placingPinLoc = placingPin.getAbsLocation();
		ISegment newSegment = firstSegment;
		boolean isPlacingPinAtJoinEnd =
				(startPoint.equals(breakPoint) || endPoint.equals(breakPoint)) && placingPinLoc.equals(breakPoint);
		if (!isPlacingPinAtJoinEnd) {
			chs.cof.logical.cable.IConductor cableConductorToJoinExistingConductor =
					getCableConductorToJoinExistingConductor(diagram, iicdAssociatedSignal, existingNet);
			ISystemObjectTypeInfoMgr objectTypeInfoMgr =
					FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
			if (cableConductorToJoinExistingConductor != null) {
				if (cableConductorToJoinExistingConductor.isShared()) {
					acquireLockAndUpdateShortDescriptionForSharedConductor(cableConductorToJoinExistingConductor,
							netName,
							objectTypeInfoMgr);
				}
				else {
					populateShortDescription(cableConductorToJoinExistingConductor, netName, objectTypeInfoMgr);
				}
			}

			IConductor net = getNewSchemConductor(diagram, breakPoint, placingPinLoc,
					cableConductorToJoinExistingConductor);
			newSegment = (ISegment) net.getSegments().iterator().next();
			SegmentHelper.joinSegments(
					(ConductorWrapperForJoinSegments) LogicSegmentWrapperForJoinSegments.getHandler(newSegment),
					firstSegment, new Point(breakPoint.getX(), breakPoint.getY()), true);
		}
		else {
			updatePropAndAttribsForPlacedConductor(firstSegment.getConductor().getConnectivity(), iicdAssociatedSignal);
		}
		newSegment.connectPin(placingPin);
		if (newSegment != firstSegment) {
			addForRouting(placingPin, newSegment);
		}
	}

	protected void addForRouting(IPin placingPin, ISegment newSegment)
	{
		List<IJoint> joints = new ArrayList<>();
		IJoint startNode = newSegment.getStartNode();
		if (startNode != null) {
			joints.add(startNode);
		}
		IJoint endNode = newSegment.getEndNode();
		if (endNode != null) {
			joints.add(endNode);
		}
		for (IJoint node : joints) {
			for (IPin pin : node.getAssociations(IPin.class)) {
				if (pin.getConnectivity() instanceof ISplicePin) {
					IDiagramObject parent = getPinList(pin);
					if (parent != null) {
						ConductorRouteAction.getInstance().addPinListForRoute((IPinList) parent);
					}
				}
			}
		}

		IPinList placingPL = getPinList(placingPin);
		if (placingPL != null) {
			ConductorRouteAction.getInstance().addPinListForRoute(placingPL);
		}
		ConductorRouteAction.getInstance().addSegmentForRoute(newSegment);
		chs.cof.logical.cable.IConductor cableConductor = newSegment.getConductor().getConnectivity();
		allVisitedConductors.put(cableConductor.getName(), cableConductor);
	}

	protected void addNewConductorToContext(IICDAssociatedSignal associatedSignal, IConductor conductor)
	{
		multicoreContext.registerPotentialCable(associatedSignal, conductor);
	}

	@Nullable protected static IPinList getPinList(@NotNull IPin pin)
	{
		return CommonUtils.cast(pin.getParent(), IPinList.class);
	}

	@Nullable protected ILocation calculateDanglingLocation(IPin placingPin, int conductorLength)
	{
		try (SymbolPinSideCalculatorForICD calculator = new SymbolPinSideCalculatorForICD()) {
			ISchemDiagram diagram = DiagramHelper.getDiagram(placingPin);
			if (diagram == null) {
				return null;
			}
			int gridSpacing = diagram.getGrid().getGridSpacing();

			IPinList placingPL = getPinList(placingPin);
			if (placingPL == null) {
				return null;
			}
			PinSideCalculator pinSideCalculator = PinSideCalculator.createAbsolute(placingPL);
			Side pinSide = pinSideCalculator.getSide(placingPin);

			ILocation danglingLocation = placingPin.getAbsLocation();
			int requiredLength = conductorLength * gridSpacing;
			switch (pinSide) {
				case LEFT:
					danglingLocation.applyDelta(-requiredLength, 0);
					break;
				case RIGHT:
					danglingLocation.applyDelta(requiredLength, 0);
					break;
				case BOTTOM:
					danglingLocation.applyDelta(0, -requiredLength);
					break;
				case TOP:
					danglingLocation.applyDelta(0, requiredLength);
					break;
			}

			return danglingLocation;
		}
	}

	public List<IDynamicGfx> updateNetTraces(IPinList currentSchemPinlist, IDeviceICD currentICD, ISchemDiagram diagram,
			@Nullable List<Pair<ILocation, String>> pinAbsLocationInfo, boolean placingBackshellTerm)
	{
		return PinListConnectionHelper.runUnderBoost(() -> {
			return doUpdateNetTraces(currentSchemPinlist, currentICD, diagram, pinAbsLocationInfo,
					placingBackshellTerm);
		});
	}

	@NotNull private List<IDynamicGfx> doUpdateNetTraces(IPinList currentSchemPinlist, IDeviceICD currentICD,
			ISchemDiagram diagram, @Nullable List<Pair<ILocation, String>> pinAbsLocationInfo,
			boolean placingBackshellTerm)
	{
		IICDSignalSourceSchemPinlist currentSignalSourcePinlist = getICDSignalSourceSchemPinlist(currentSchemPinlist);

		if (currentSignalSourcePinlist == null) {
			return Collections.emptyList();
		}
		final IPinList currentSchemDevice = currentSignalSourcePinlist.getSchemDevice();

		if (currentSchemDevice == null) {
			return Collections.emptyList();
		}

		DeviceToICDMatcher icdMatcher = new DeviceToICDMatcher(diagram);
		Map<IDevice, List<IDeviceICD>> placedDeviceToICDs = icdMatcher.getPlacedDeviceToICDsInPrecedence();
		placedDeviceToICDs.remove(currentSchemDevice.getConnectivity());

		List<IDynamicGfx> dynamicGfxs = new ArrayList<IDynamicGfx>();

		if (!placedDeviceToICDs.isEmpty()) {
			IPlacingPinInfo placingPinInfo;
			if (placingBackshellTerm) {
				placingPinInfo = new GeneralPlacingPinInfo(pinAbsLocationInfo, null);
			}
			else {
				ILibraryDevice libraryDevice = currentICD.getLibraryDevice();
				Map<String, String> cavVsPinNames = new HashMap<>();
				if (libraryDevice != null) {
					List<IAbstractPin> pins =
							CollectionUtils.createList(currentSchemPinlist.getConnectivity().getPins());
					Map<IReadOnlyNamedObject, IPinProxy> libCavityVsConnectivityPins =
							SelectedPartUpdateHelper.mapPins(libraryDevice, pins);
					for (Map.Entry<IReadOnlyNamedObject, IPinProxy> entry : libCavityVsConnectivityPins.entrySet()) {
						cavVsPinNames.put(entry.getKey().getName(), entry.getValue().getName());
					}
				}
				placingPinInfo = new GeneralPlacingPinInfo(pinAbsLocationInfo, cavVsPinNames);
			}

			for (Map.Entry<IDevice, List<IDeviceICD>> entry : placedDeviceToICDs.entrySet()) {
				IDevice placedDevice = entry.getKey();
				if (ICDUtils.getICDMatchName(placedDevice).equalsIgnoreCase(currentICD.getRole())) {
					continue;
				}
				List<IDeviceICD> icdsAssociatedWithDevice = entry.getValue();

				addTraceBetweenDevices(placedDevice, icdsAssociatedWithDevice, currentSignalSourcePinlist, currentICD,
						dynamicGfxs, diagram, placingPinInfo);
			}
		}
		return dynamicGfxs;
	}

	private boolean areAllSchemPinsReference(IAbstractPin placedDevPin, ISchemDiagram diagram)
	{
		IDiagramObjectIterator representations = diagram.getRepresentations(placedDevPin.getUID());
		while (representations.hasNext()) {
			IDiagramObject diagramObject = representations.next();
			final IPin schemPin = CommonUtils.cast(diagramObject, IPin.class);
			if (schemPin != null && !schemPin.isReference()) {
				return false;
			}
		}
		return true;
	}

	private void addTraceBetweenDevices(IDevice placedDevice, List<IDeviceICD> icdsAssociatedWithDevice,
			IICDSignalSourceSchemPinlist currentICDSignalSourcePinlist, IDeviceICD currentICD,
			final List<IDynamicGfx> dynamicGfxs, ISchemDiagram diagram, IPlacingPinInfo placingPinInfo)
	{
		SetMap<String, CaseLessStringKey> signalAssociations =
				ICDSignalDetailsFinder.getNetsAssociatedWithPin(icdsAssociatedWithDevice);
		for (IAbstractPin placedDevPin : placedDevice.getPins()) {
			if (areAllSchemPinsReference(placedDevPin, diagram)) {
				continue;
			}
			Set<CaseLessStringKey> netsAssociatedWithPin = signalAssociations
					.pullReadOnlySafeSet(StringUtils.nonNull(ICDUtils.getICDMatchName(placedDevPin)));
			addTraceBetweenPins(currentICDSignalSourcePinlist, currentICD, dynamicGfxs, diagram, placedDevPin,
					netsAssociatedWithPin, placingPinInfo);
		}

		// add traces with placed device backshell terminations
		final SetMap<IICDBackshellTermination, CaseLessStringKey> termNetsAssociatedWithPin =
				ICDSignalDetailsFinder.getTermNetsAssociatedWithPin(icdsAssociatedWithDevice);
		ICDUtils.processMatchingBSTerminals(placedDevice, icdsAssociatedWithDevice, (icdBSTerm, logicBSTerm) -> {
			final Set<CaseLessStringKey> netsAssociatedWithterm =
					termNetsAssociatedWithPin.pullReadOnlySafeSet(icdBSTerm);
			addTraceBetweenPins(currentICDSignalSourcePinlist, currentICD, dynamicGfxs, diagram, logicBSTerm,
					netsAssociatedWithterm, placingPinInfo);
		});
	}

	private void addTraceBetweenPins(IICDSignalSourceSchemPinlist currentICDSignalSourcePinlist, IDeviceICD currentICD,
			final List<IDynamicGfx> dynamicGfxs, ISchemDiagram diagram, IAbstractPin placedDevPinOrTerm,
			Set<CaseLessStringKey> netsAssociatedWithPin, IPlacingPinInfo placingPinInfo)
	{
		for (IDeviceICDSignalsContainer pinSignalAsso : currentICDSignalSourcePinlist
				.getICDSignalContainers(currentICD)) {
			Set<ILocation> placingPinAbsLocations =
					placingPinInfo.getPlacingPinAbsoluteLocations(currentICDSignalSourcePinlist, pinSignalAsso);
			for (ILocation placingPinAbsLocation : placingPinAbsLocations) {
				for (IICDAssociatedSignal signal : pinSignalAsso.getICDAssociatedSignals()) {
					if (signal.isShieldWire()) {
						continue;
					}
					if (netsAssociatedWithPin.contains(CaseLessStringKey.toKey(signal.getNetName()))) {
						final ILocation placingPinLoc = placingPinAbsLocation;
						IInterconnectJoinCallback callBack = new IInterconnectJoinCallback()
						{
							@Override
							public void connect(ISegment segment, ILocation breakLoc, boolean isJoinAtEnd)
							{
								ICDDynamicGraphicsProvider.showTrace(placingPinLoc, breakLoc, dynamicGfxs);
								if (!isJoinAtEnd) {
									ICDDynamicGraphicsProvider.showSplitTrace(segment.getStartPoint(), segment.getEndPoint(), breakLoc,
											dynamicGfxs);
								}
							}

							@Override
							public void connect(IPin placedPin)
							{
								ICDDynamicGraphicsProvider.showTrace(placingPinLoc, placedPin.getAbsLocation(), dynamicGfxs);
							}
						};

						IPlacingPinController placingPinCtrl = new IPlacingPinController()
						{
							@Override public boolean proceedToJoin(Set<IPin> connectedPins)
							{
								return true;
							}

							@Override public ILocation getPlacingPinReferenceLocation()
							{
								return placingPinLoc;
							}

							@Override public boolean canConnect(IPin placedPin)
							{
								return true;
							}

							@Override public boolean canConnect(ISegment segment)
							{
								return true;
							}

							@Override public boolean canSplit(ISegment segment)
							{
								return true;
							}

							@Override public void registerDanglingConnection(IPin placedPin)
							{

							}
						};
						traverseAndJoinPinsBySignal(placedDevPinOrTerm, placingPinCtrl, diagram, callBack, signal);
					}
				}
			}
		}
	}

	public static String getMulticoreSource(IPropertiedObject multicore)
	{
		String icdSourceName = null;
		IProperty sourceProperty = multicore.findPropertyByName(ICDMulticore.SOURCE_CABLE_NAME);
		if (sourceProperty != null) {
			icdSourceName = sourceProperty.getAsString();
		}
		return icdSourceName != null ? icdSourceName : "";
	}

	@Nullable public IICDSignalSourceSchemPinlist getICDSignalSourceSchemPinlist(@NotNull IPinList schemPinlist)
	{
		return m_SchemDeviceICDPinInfoCache.getICDSignalSourceSchemPinlist(schemPinlist);
	}

	private static class SchemDeviceICDPinInfoCache
	{

		@NotNull private final Map<IPinList, IICDSignalSourceSchemPinlist> m_schemPinlistInfos = new HashMap<>();

		@NotNull public IICDSignalSourceSchemPinlist getICDSignalSourceSchemPinlist(@NotNull IPinList schemPinlist)
		{
			IICDSignalSourceSchemPinlist icdSingalSourceSchemPinlist = m_schemPinlistInfos.get(schemPinlist);
			if (icdSingalSourceSchemPinlist == null) {
				final chs.cof.logical.cable.IPinList cablePinList = schemPinlist.getConnectivity();
				if (cablePinList instanceof IDevice) {
					icdSingalSourceSchemPinlist = new SchemDeviceICDPinInfo(schemPinlist);
				}
				else if (cablePinList instanceof IConnector) {
					icdSingalSourceSchemPinlist = new ICDBackshellSignalSourceConnector(schemPinlist);
				}
				else {
					icdSingalSourceSchemPinlist = new NullICDSignalSourceSchemPinList(schemPinlist);
					assert false : "Unhandled schematic pinlist!!!";
				}
				m_schemPinlistInfos.put(schemPinlist, icdSingalSourceSchemPinlist);
			}
			return icdSingalSourceSchemPinlist;
		}
	}
}