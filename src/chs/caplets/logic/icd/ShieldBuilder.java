package chs.caplets.logic.icd;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDNetCableElement;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicalConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedFactory;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibrarySingleWireCore;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IDesignAbstraction;
import chs.common.ILocation;
import chs.common.IUID;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.ICDUtils;
import chs.utility.Placement;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.ConductorWrapperForJoinSegments;
import chs.utility.helpers.LogicSegmentWrapperForJoinSegments;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.ShieldBodyHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.MulticoreUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ShieldBuilder
{

	private ShieldBuilder()
	{
	}

	public static void createShieldInConnectivity(
			@NotNull IMulticore multicore, @NotNull IICDAssociatedSignal signal, @NotNull ISchemDiagram diagram,
			@NotNull ICDMulticoreContext multicoreContext, @NotNull SharedDetailsLockHelper lockTracker,
			@NotNull Map<IShieldConductor, Map<IICDAssociatedSignal, IMulticore>> multicoreToSignalShieldMap)
	{
		IShieldConductor shieldConductor =
				collectShieldConductor(multicore, signal, diagram, multicoreContext, lockTracker);

		Map<IICDAssociatedSignal, IMulticore> signalToMulticoreMap = new HashMap<>();
		signalToMulticoreMap.put(signal, multicore);
		if (shieldConductor != null) {
			multicoreToSignalShieldMap.put(shieldConductor, signalToMulticoreMap);
		}
	}

	public static void createShieldInConnectivityAndLock(
			@NotNull IMulticore multicore, @NotNull IICDAssociatedSignal signal, @NotNull ISchemDiagram diagram,
			@NotNull ICDMulticoreContext multicoreContext, @NotNull SharedDetailsLockHelper lockTracker)
	{

		IShieldConductor shieldConductor =
				collectShieldConductor(multicore, signal, diagram, multicoreContext, lockTracker);

		if (shieldConductor == null) {
			return;
		}
		ICDInterconnectStrategy.updateAttributesAndPropOnPlacedSignal(shieldConductor, signal);
		assert diagram.getDesign() != null;
		ShieldBodyHelper.createShieldBodyHookups(diagram.getDesign(), multicore.getShieldBody());
	}

	@Nullable
	private static IShieldConductor collectShieldConductor(
			@NotNull IMulticore multicore, @NotNull IICDAssociatedSignal signal, @NotNull ISchemDiagram diagram,
			@NotNull ICDMulticoreContext multicoreContext, @NotNull SharedDetailsLockHelper lockTracker)
	{
		if (multicore.getShield() != null) {
			return null;
		}

		ISharedConductor sharedShield = null;
		if (multicore.isShared()) {
			sharedShield = createSharedShieldConductor(signal, diagram, multicore, lockTracker);
			if (sharedShield == null) {
				// log error message here
				String msg = ResourceMgr
						.getString(ICDInterconnectStrategy.class, "ICDInterconnectStrategy.SharedLockFailure.text");
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
				return null;
			}
		}

		IShieldConductor shieldConductor =
				FactoryMgr.getCablePropertiedFactory().createShieldConductor(FactoryMgr.createUID());
		ICDUtils.setSourceICDSignal(shieldConductor, signal.getNetName());
		shieldConductor.setName(signal.getNetName());
		multicore.getConnectivity().addConductor(shieldConductor);
		multicore.setShield(shieldConductor);

		ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
		if (sharedMulticore != null) {
			shieldConductor.setSharedConductor(sharedShield);
		}

		ILibraryBaseObject libraryObject = multicore.getLibraryObject();
		if (libraryObject != null) {
			ILibrarySingleWireCore shieldLibraryRef = SharedConductorHelper.getShieldLibraryRef(libraryObject);
			if (shieldLibraryRef != null) {
				shieldConductor.setLibraryRef(shieldLibraryRef.getUID());
				MulticoreLibraryHelper.addInnerCoreProperties(shieldConductor, shieldLibraryRef);
			}
		}

		multicoreContext.registerNewMulticoreShield(shieldConductor);
		return shieldConductor;
	}

	@Nullable
	private static ISharedConductor createSharedShieldConductor(IICDAssociatedSignal signal, ISchemDiagram diagram,
			IMulticore multicore, SharedDetailsLockHelper lockTracker)
	{
		ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
		if (sharedMulticore == null) {
			return null;
		}

		ISharedConductor shield = sharedMulticore.getShield();
		if (shield != null) {
			return shield;
		}
		// lock all required and create shared conductor
		ILogicDesign design = diagram.getDesign();
		if (design == null) {
			return null;
		}
		IProject project = design.getProject();
		if (project == null) {
			return null;
		}

		IUID sharedMulticoreUID = sharedMulticore.getUID();
		if (!lockTracker.lock(sharedMulticore, true, signal.getNetName())) {
			return null;
		}

		sharedMulticore = UIDMgr.getObjectOfType(sharedMulticoreUID, ISharedMulticore.class);
		assert sharedMulticore != null;

		shield = sharedMulticore.getShield(); // Again check after lock and refresh
		if (shield != null) {
			return shield;
		}

		ISharedFactory sharedFactory = UtilsHelper.getCHSUtils().getSharedFactory();
		ISharedConductor sharedShield =
				sharedFactory.createSharedConductor(FactoryMgr.getCommonFactory().createUID());
		sharedShield.setType(ISharedConductor.SHIELD_TYPE);
		sharedShield.setName(signal.getNetName());
		sharedShield.setRevision(sharedMulticore.getRevision());
		sharedShield.setDesignAbstraction(design.getDesignAbstraction());

		ISharedConductorMgr sharedConductorMgr = project.getSharedConductorMgr();
		sharedConductorMgr.addSharedConductor(sharedShield);
		sharedMulticore.setShield(sharedShield);
		sharedShield.flushNew(sharedConductorMgr.getObjType(), sharedConductorMgr);
		return sharedShield;
	}

	private static boolean connectifPossibleToOpenEndedShieldAtThisHookup(IShieldBodyHookup hookup, IPin schemPin)
	{
		// if there is an open-ended schem shield available at this hookup, use it to connect to the pin
		Collection<IConductor> shieldConductorsAtThisHookup = hookup.getShieldConductors();
		for (IConductor schemShield : shieldConductorsAtThisHookup) {
			for (ISegment segment : schemShield.getSegmentsOfType(ISegment.class)) {
				if (SegmentHelper.isOpenEndedSegment(segment)) {
					ILocation location = SegmentHelper.getAppropriatePointSnappedToGrid(segment);
					ISegment segmentToConnect = segment;
					if (!location.equals(schemPin.getAbsLocation())) {
						List<Point> points = new ArrayList<Point>();
						points.add(GfxUtils.getPoint(schemPin.getAbsLocation()));
						points.add(GfxUtils.getPoint(location));
						CreateSchemConductorCmd cmd = new CreateSchemConductorCmd(schemPin.getDiagram(), points,
								IShieldConductor.class);
						cmd.setCableConductor(schemShield.getConnectivity());
						cmd.execute();

						ISegment newSegment = (ISegment) cmd.getConductor().getSegments().iterator().next();
						SegmentHelper.joinSegments((ConductorWrapperForJoinSegments) LogicSegmentWrapperForJoinSegments
								.getHandler(newSegment), segment, new Point(location.getX(), location.getY()), true);
						segmentToConnect = newSegment;
					}

					segmentToConnect.connectPin(schemPin);
					ConductorRouteAction.getInstance().addSegmentForRoute(segmentToConnect);
					ConductorRouteAction.getInstance().addPinForRoute(schemPin);
					return true;
				}
			}
		}
		return false;
	}

	@Nullable private static IShieldBodyHookup getNearestShieldHookup(@NotNull IShieldConductor shield,
			@NotNull ILocation originatingSchemPinLoc, @NotNull ISchemDiagram diagram)
	{
		IMulticore multicore = shield.getMulticore();
		if (multicore == null) {
			return null;
		}
		Set<chs.cof.logical.schem.IShieldBody> schemShieldBodies = getSchemShieldBodies(multicore, diagram);

		if (schemShieldBodies.isEmpty()) {
			placeIndicators(diagram, multicore, true);
			schemShieldBodies = getSchemShieldBodies(multicore, diagram);
		}

		Pair<Double, IShieldBodyHookup> nearestHookup = new Pair<Double, IShieldBodyHookup>(Double.MAX_VALUE, null);
		for (chs.cof.logical.schem.IShieldBody shieldBody : schemShieldBodies) {
			for (IShieldBodyHookup shieldBodyHookup : shieldBody.getShieldBodyHookups()) {
				ILocation location = shieldBodyHookup.getAbsLocation();
				double distance = location.distance(originatingSchemPinLoc);
				if (nearestHookup.getFirst() > distance) {
					nearestHookup.set(distance, shieldBodyHookup);
				}
			}
		}
		return nearestHookup.getSecond();
	}

	@NotNull public static Set<chs.cof.logical.schem.IShieldBody> getSchemShieldBodies(IMulticore multicore,
			@NotNull ISchemDiagram diagram)
	{
		return CollectionUtils.getObjects(diagram.getRepresentations(multicore.getShieldBody().getUID()),
				chs.cof.logical.schem.IShieldBody.class);
	}

	public static void placeIndicatorsOnAllDiagrams(@Nullable IMulticore placingRootMC, boolean addOnlyIfNoIndicator)
	{
		ILogicDesign logicDesign = placingRootMC != null ? placingRootMC.getLogicDesign() : null;
		if (logicDesign != null) {
			Set<IUID> usageDiagramIds = new HashSet<>();
			logicDesign.getDesignWideUsageMgr().getMulticoreDiagrams(placingRootMC, usageDiagramIds);
			usageDiagramIds.forEach(d -> {
				ISchemDiagram diagram = logicDesign.getDiagram(d);
				if (diagram != null) {
					placeIndicators(diagram, placingRootMC, addOnlyIfNoIndicator);
				}
			});
		}
	}

	public static void placeIndicators(ISchemDiagram diagram, @Nullable IMulticore placingRootMC,
			boolean addOnlyIfNoIndicator)
	{
		Placement.runUnderCrossingsProcessingControl(() -> {
			doPlaceIndicators(diagram, placingRootMC, addOnlyIfNoIndicator);
			return Void.TYPE;
		}, Placement.ElaboratedCrossingsControl.SINGLE_PAIR, false);
	}

	private static void doPlaceIndicators(ISchemDiagram diagram, @Nullable IMulticore placingRootMC,
			boolean addOnlyIfNoIndicator)
	{
		IShieldBody sb = null;
		if (placingRootMC != null) {
			sb = placingRootMC.getShieldBody();
		}
		if (sb != null) {
			Generator gen = Generator.getGenerator();
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			//noinspection ConstantConditions
			Placement.placeIndicators(gen, diagram, placingRootMC, sb, gp, true, null, addOnlyIfNoIndicator, false);
			Set<chs.cof.logical.schem.IShieldBody> schemShieldBodies = getSchemShieldBodies(placingRootMC, diagram);

			if (schemShieldBodies.isEmpty()) {
				// dts0101359159 Data produced in Capital Device as ICD is not being used in Capital - start supporting
				// defintion of multiple signals between two pins of ICDs.
				// We failed to generate the indicators as the crossings are treated as invalid because of the presence of segments
				// corresponding to two different conductors - so force the indicator placement in these special cases
				//noinspection ConstantConditions
				Placement.placeIndicators(gen, diagram, placingRootMC, sb, gp, true, null, addOnlyIfNoIndicator, true);
			}
		}
	}

	public static void doCreateShieldOnPin(ISchemDiagram diagram, Collection<IPin> pins, IICDAssociatedSignal signal,
			IMulticore childMulticore, ICDMulticoreContext multicoreContext,
			ICDInterconnectStrategy icdInterconnectStrategy, SharedDetailsLockHelper lockTracker)
	{
		if (childMulticore.getShield() == null) {
			if (IndicatorHelper.isMulticoreShieldIndicator(childMulticore.getShieldBody())) {
				createShieldInConnectivityAndLock(childMulticore, signal, diagram, multicoreContext, lockTracker);
			}
			else {
				String msg = ResourceMgr.getString(ICDInterconnectStrategy.class,
						"ICDInterconnectStrategy.InvalidShieldConnection.text", signal.getNetName(),
						AppInfo.getAppInfo().getSuiteDisplayName());
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
			}
		}
		createSchemShieldConductors(pins, childMulticore, diagram, icdInterconnectStrategy, signal);
	}

	public static boolean isSignalAShieldForThisMulticore(IICDAssociatedSignal signal, @NotNull IMulticore multicore)
	{
		// return true if this signal is of type shield and its cable specification matches the argument multicore,
		if (!signal.isShieldWire()) {
			return false;
		}

		String logicMulticoreHierarchy = ICDInterconnectStrategy.getICDPathFromRootMulticore(multicore);
		if (logicMulticoreHierarchy == null) {
			return false;
		}
		ILogicDesign design = CommonUtils.cast(multicore.getConnectivity().getDesign(), ILogicDesign.class);
		if (design == null) {
			return false;
		}
		Set<List<IICDNetCableElement>> signalGroupPaths =
				design.getDesignICDContainer().getEquivalentSignalGroupPaths(signal);
		for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
			if (doesSignalGroupPathMatch(logicMulticoreHierarchy, signalGroupPath)) {
				return true;
			}
		}
		return false;
	}

	private static boolean doesSignalGroupPathMatch(@NotNull String logicMulticoreHierarchy,
			@NotNull List<IICDNetCableElement> cableHierarchy)
	{
		StringBuilder expectedMCNameWithHierarchy = new StringBuilder();
		cableHierarchy.forEach(pair -> {
			if (expectedMCNameWithHierarchy.length() > 0) {
				expectedMCNameWithHierarchy.append(ICDUtils.MC_PATH_SEPARATOR);
			}
			expectedMCNameWithHierarchy.append(pair.getOriginalName());
		});
		return expectedMCNameWithHierarchy.toString().equals(logicMulticoreHierarchy);
	}

	public static void createSchemShieldConductors(IAbstractPin pin, @NotNull IMulticore multicore,
			ISchemDiagram diagram, ICDInterconnectStrategy icdInterconnectStrategy,
			IICDAssociatedSignal signal)
	{
		Collection<IPin> iPins = CollectionUtils.filterByClass(diagram.getRepresentations(pin.getUID()), IPin.class);
		createSchemShieldConductors(iPins, multicore, diagram, icdInterconnectStrategy, signal);
	}

	private static void createSchemShieldConductors(Collection<IPin> schemPins, @NotNull IMulticore multicore,
			ISchemDiagram diagram, ICDInterconnectStrategy icdInterconnectStrategy,
			@NotNull IICDAssociatedSignal signal)
	{
		final IShieldConductor existingShield = multicore.getShield();
		if (existingShield == null) {
			return;
		}

		if (!existingShield.getName().equals(signal.getNetName())) {
			syncConductorWithSignal(existingShield, signal);
		}

		Comparator<IICDSignalSourceSchemPinlist> plComparator =
				(a, b) -> a.getSchemPinlist().getUID().compareTo(b.getSchemPinlist().getUID());
		Comparator<IPin> pinComparator = (a, b) -> a.getUID().compareTo(b.getUID());
		SetMap<IICDSignalSourceSchemPinlist, IPin> schemPinsToProcess =
				new SetMap<IICDSignalSourceSchemPinlist, IPin>(() -> new TreeMap<>(plComparator))
				{
					@Override protected Set<IPin> createSet()
					{
						return new TreeSet<>(pinComparator);
					}
				};

		for (IPin schemPin : schemPins) {
			IPinList schemPinlist = ICDInterconnectStrategy.getPinList(schemPin);
			if (schemPinlist == null) {
				continue;
			}

			IICDSignalSourceSchemPinlist icdSignalSourceSchemPinlist =
					icdInterconnectStrategy.getICDSignalSourceSchemPinlist(schemPinlist);

			if (icdSignalSourceSchemPinlist == null) {
				continue;
			}

			final IPinList schemDevice = icdSignalSourceSchemPinlist.getSchemDevice();
			if (schemDevice == null || !isPinlistConnectedToTheMulticore(schemDevice, multicore)) {
				continue;
			}

			schemPinsToProcess.add(icdSignalSourceSchemPinlist, schemPin);
		}

		for (Map.Entry<IICDSignalSourceSchemPinlist, Set<IPin>> entry : schemPinsToProcess.entrySet()) {
			IICDSignalSourceSchemPinlist icdSingalSourceSchemPinlist = entry.getKey();
			doCreateSchemShieldConductors(multicore, diagram, icdSingalSourceSchemPinlist, entry.getValue());
		}
	}

	public static void syncConductorWithSignal(@NotNull ILogicalConductor shield,
			@NotNull IICDAssociatedSignal icdAssociatedSignal)
	{
		if (shield.isShared()) {
			acquireLockAndSyncConductorWithSignal(shield, icdAssociatedSignal);
		}
		else {
			updateNameAndSourceICDSignalOnCond(shield, icdAssociatedSignal);
		}
	}

	private static void updateNameAndSourceICDSignalOnCond(@NotNull ILogicalConductor shield,
			@NotNull IICDAssociatedSignal icdAssociatedSignal)
	{
		final String netName = icdAssociatedSignal.getNetName();
		shield.setName(netName);
		ICDUtils.setSourceICDSignal(shield, netName);
	}

	private static void acquireLockAndSyncConductorWithSignal(ILogicalConductor shield,
			IICDAssociatedSignal icdAssociatedSignal)
	{
		ISharedConductor sharedConductor = shield.getSharedConductor();
		if (sharedConductor != null) {
			boolean lockSuccess = false;
			try {
				if (!sharedConductor.isLocked()) {
					lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedConductor);
				}
				if (sharedConductor.isLocked()) {
					updateNameAndSourceICDSignalOnCond(shield, icdAssociatedSignal);
				}
			}
			finally {
				if (lockSuccess) {
					LockUpdateHelper.flushAndUnlockSharedObject(sharedConductor);
				}
				else if (sharedConductor.isLocked()) {
					sharedConductor.flush();
				}
			}
		}
	}

	private static void doCreateSchemShieldConductors(@NotNull IMulticore multicore, ISchemDiagram diagram,
			IICDSignalSourceSchemPinlist icdSingalSourceSchemPinlist, Set<IPin> schemPins)
	{
		for (IPin schemPin : schemPins) {
			if (schemPin.isReference()) {
				continue;
			}
			IPin pinToHoldSchemShield = icdSingalSourceSchemPinlist.getEquivalentICDMatchingSignalPin(schemPin);
			if (pinToHoldSchemShield == null) {
				continue;
			}

			if (isPinSchematicallyConnectedToTheShieldConductor(pinToHoldSchemShield, multicore)) {
				continue;
			}

			IShieldBodyHookup hookup =
					getNearestShieldHookup(multicore.getShield(), pinToHoldSchemShield.getAbsLocation(), diagram);
			if (hookup != null) {
				if (connectifPossibleToOpenEndedShieldAtThisHookup(hookup, pinToHoldSchemShield)) {
					continue;
				}
				List<Point> points = new ArrayList<Point>();
				points.add(GfxUtils.getPoint(pinToHoldSchemShield.getAbsLocation()));
				points.add(GfxUtils.getPoint(hookup.getAbsLocation()));
				CreateSchemConductorCmd cmd = new CreateSchemConductorCmd(diagram, points,
						IShieldConductor.class);
				cmd.setCableConductor(multicore.getShield());
				cmd.execute();
				for (ISegment segment : cmd.getConductor().getSegmentsOfType(ISegment.class)) {
					segment.connectPin(pinToHoldSchemShield);
					ConductorHelper.connectShieldBodyHookup(segment, hookup,
							new Point(hookup.getAbsLocation().getX(), hookup.getAbsLocation().getY()));
					ConductorRouteAction.getInstance().addSegmentForRoute(segment);
				}
			}
			ConductorRouteAction.getInstance().addPinForRoute(pinToHoldSchemShield);
		}
	}

	private static boolean isPinSchematicallyConnectedToTheShieldConductor(IPin pin, IMulticore multicore)
	{
		Class<? extends chs.cof.logical.cable.IConductor> cableType = INetConductor.class;
		ILogicDesign design = multicore.getLogicDesign();
		if (design != null) {
			IDesignAbstraction designAbstraction = design.getDesignAbstraction();
			if (designAbstraction != null && designAbstraction.getAllowAutoCreation()) {
				cableType = IWireConductor.class;
			}
		}
		SignalPathTree tree =
				new SignalPathTreeHelper(null, Collections.emptySet(), cableType).buildSignalPathTree(pin,
						new HashSet<>(), null);
		List<SignalPathTree> leafNodes = tree.getLeafNodes().stream().filter(node -> node.getPin() == null)
				.collect(Collectors.toList());
		for (SignalPathTree leafNode : leafNodes) {
			IPin pinToCheck = leafNode.getParent().getPin();
			// if pinToCheck connects to hookup then return true
			boolean isConnected = pinToCheck.getConductors().stream()
					.anyMatch(cond -> cond.getConnectivity() instanceof IShieldConductor &&
							cond.getConnectivity() == multicore.getShield());
			if (isConnected) {
				return true;
			}
		}

		return pin.getConductors().stream()
				.anyMatch(cond -> cond.getConnectivity() instanceof IShieldConductor &&
						cond.getConnectivity() == multicore.getShield());
	}

	private static boolean isPinlistConnectedToTheMulticore(IPinList schemPinlist, IMulticore multicoreOfInterest)
	{
		Set<IPinList> connectedPLs = new LinkedHashSet<>();
		connectedPLs.add(schemPinlist);
		connectedPLs.addAll(SchemPinListHelper.getAttachedSchemPinLists(schemPinlist, IHarnessPlugConnector.class));
		for (IPinList connectedPL : connectedPLs) {
			for (IPin pin : connectedPL.getPins()) {
				for (IConductor conductor : pin.getConductors()) {
					IMulticore rootMulticore =
							MulticoreUtils.getRootMulticore(conductor.getConnectivity().getMulticore(), true);
					if (rootMulticore != null) {
						for (IMulticore multicore : rootMulticore.getAllMulticoresInHierarchy()) {
							if (multicore == multicoreOfInterest) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
