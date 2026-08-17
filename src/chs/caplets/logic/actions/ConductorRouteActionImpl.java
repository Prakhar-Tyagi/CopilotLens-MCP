/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2010-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IWindowMgr;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.AbstractConductorRouteActionImpl;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.IndicatorRefresher;
import chs.caplets.logic.SchemObjectHandler;
import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.table.IDynamicTableData;
import chs.cof.drawplus.table.IPluginTableData;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinContentInformation;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IBasePreferencesKeys;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IPreferenceMgr;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.IUIDProvider;
import chs.common.Side;
import chs.common.graph.data.OGDLayoutStylingContextHolder;
import chs.common.graph.data.OGDPreferenceContext;
import chs.common.preferencesets.IPreferenceSet;
import chs.utilities.BuildInfo;
import chs.utilities.CollectionUtils;
import chs.utilities.Environment;
import chs.utilities.ListMap;
import chs.utilities.ListSet;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.CompareUIDComparator;
import chs.utility.DiagramHelper;
import chs.utility.Placement;
import chs.utility.PortHelper;
import chs.utility.ProjectHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.ISchematicGenerationParams;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.MulticoreUtils;
import chs.utility.task.InterruptableTaskHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.IProgressCancelledHandler;
import chs.utility.ui.progress.ProgressCancelledException;
import chs.utility.ui.progress.ProgressGroup;
import chs.utility.ui.progress.ProgressTaskClient;
import chs.view.assist.HighwayConductorSchemCreator;
import chs.view.connectivity.IPinObject;
import chs.view.connectivity.edges.ConnectivityEdgeHelper;
import chs.view.connectivity.edges.ConnectivityEdgeUtilities;
import chs.view.connectivity.edges.ConnectivityVirtualVertex;
import chs.view.connectivity.edges.IConnectivityEdge;
import chs.view.connectivity.edges.IConnectivityEdgeResult;
import chs.view.connectivity.edges.IConnectivityVertex;
import chs.view.connectivity.edges.INetTermination;
import chs.view.connectivity.edges.IOrderedPinPair;
import chs.view.connectivity.edges.IShieldTermination;
import chs.view.memory.IConductorTraversalOutput;
import chs.view.memory.SchemTraversal;
import chs.view.route.ConductorRouteActionCostStrategy;
import chs.view.route.ConductorRouteOutput;
import chs.view.route.EdgeOrder;
import chs.view.route.IJointRouteEnd;
import chs.view.route.IModifiableCostStrategy;
import chs.view.route.IModifiableJointRouteEnd;
import chs.view.route.IPrototype;
import chs.view.route.IRoutable;
import chs.view.route.IRouteEnd;
import chs.view.route.IRouteIO;
import chs.view.route.IRouteOutput;
import chs.view.route.NoRouteOutput;
import chs.view.route.RoutableUtils;
import chs.view.route.SegmentsRouteOutput;
import chs.view.route.blockage.BlockageUtils;
import chs.view.route.blockage.IRouteContext;
import chs.view.route.blockage.IRoutePathConstraints;
import chs.view.route.blockage.RoutePathConstaintSatisfier;
import chs.view.schem.ISchematicCreationContext;
import chs.view.schem.logic.ConnectivityEdgeHandler;
import chs.view.utils.AutoRoutePathDeterminer;
import chs.view.utils.ConductorRouteActionHelper;
import chs.view.utils.DiagramFlowStyle;
import chs.view.utils.DiagramGenerationUtilities;
import chs.view.utils.GeneratorUtils;
import chs.view.utils.ShieldBodyHookupSelector;
import chs.view.utils.ViewsInternalConnectivityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ConductorRouteActionImpl extends AbstractConductorRouteActionImpl
{

	@NotNull private static ListMap<Object, IPin> m_schemPinFromConductor = new ListMap<Object, IPin>();
	@NotNull private Set<IPinList> m_pinLists = new LinkedHashSet<IPinList>();
	@NotNull private Set<? super IAbstractSchemPin> m_pins = new LinkedHashSet<>();
	@NotNull private Set<IShieldBody> m_shieldBodies = new LinkedHashSet<IShieldBody>();
	@NotNull private Set<ILogicSegmentContainer> m_conductors = new LinkedHashSet<>();
	@NotNull private Set<ILogicSegmentContainer> m_conductorsMustRoute = new LinkedHashSet<>();
	@NotNull private Set<ILogicSegment> m_segments = new LinkedHashSet<>();

	private static final int WIRE_SPACING = 1;
	private static final DiagramFlowStyle DIAGRAM_FLOW_STYLE = DiagramFlowStyle.DEFAULT;

	private static boolean s_init = false;
	private static List<IConnectivityEdge> edgeResultSet = new ListSet<IConnectivityEdge>();

	@NotNull private static ICAFUtilityProvider m_defaultCAFUtilityProvider = new DefaultCAFUtilityProvider();
	@NotNull private static ICAFUtilityProvider m_closedDiagramsCAFUtilityProvider =
			new ClosedDiagramsCAFUtilityProvider();

	@NotNull private static final String TASK_ID = "Auto Route";
	private static final int MONITOR_INTERVAL = 200;

	private static final int FIFTY_THOUSAND = 50000;
	private static final int CONDUCTOR_SIZE_LIMIT = 10;

	@NotNull private static final String ENABLE_AUTO_ROUTING = "EnableAutoRouting";
	@NotNull private static final String ENABLE_TRAVERSE_ROUTING = "EnableTraverseRouting";
	@NotNull private static final String ENABLE_FULL_SIGNAL_ROUTING = "EnableFullSignalRouting";
	private static final String ENABLE_THREE_PHASE_ROUTING = "EnableThreePhaseRouting";

	@Override protected void initializeAutoRouteFromPreferences()
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		setRoutingOptions(preferences.getBoolean(ENABLE_AUTO_ROUTING, false),
				preferences.getBoolean(ENABLE_TRAVERSE_ROUTING, false),
				preferences.getBoolean(ENABLE_FULL_SIGNAL_ROUTING, false),
				preferences.getBoolean(ENABLE_THREE_PHASE_ROUTING, false));
	}

	public static void init()
	{
		if (!s_init) {
			ConductorRouteAction.setInstance(new ConductorRouteActionImpl());
			s_init = true;
		}
	}

	public void addPinListForRoute(@NotNull IPinList pinList)
	{
		m_pinLists.add(pinList);
	}

	public void addPinListsForRoute(@NotNull Collection<IPinList> pinLists)
	{
		m_pinLists.addAll(pinLists);
	}

	@Override public boolean isDiagramActive(ISchemDiagram diagram)
	{
		return diagram.isEditable() && CAFUtils.getInstance().getOpenDiagrams().contains(diagram);
	}

	@Override public <T extends IAbstractSchemPin> void addPinForRoute(@NotNull T pin)
	{
		m_pins.add(pin);
	}

	@Override public <T extends IAbstractSchemPin> void addPinsForRoute(@NotNull Collection<T> pins)
	{
		m_pins.addAll(pins);
	}

	public void addShieldBodyForRoute(@NotNull IShieldBody shieldBody)
	{
		m_shieldBodies.add(shieldBody);
	}

	public void addShieldBodiesForRoute(@NotNull Collection<IShieldBody> shieldBodies)
	{
		m_shieldBodies.addAll(shieldBodies);
	}

	@Override public <T extends ILogicSegmentContainer> void addConductorForRoute(@NotNull T conductor)
	{
		m_conductors.add(conductor);
	}

	@Override public <T extends ILogicSegmentContainer> void addConductorsForRoute(@NotNull Collection<T> conductors)
	{
		addConductorsForRoute(conductors, false);
	}

	@Override public <T extends ILogicSegmentContainer> void addConductorsForRoute(@NotNull Collection<T> conductors,
			boolean mustRoute)
	{
		if (mustRoute) {
			m_conductorsMustRoute.addAll(conductors);
		}
		else {
			m_conductors.addAll(conductors);
		}
	}

	@Override public <T extends ILogicSegment> void addSegmentForRoute(@NotNull T segment)
	{
		m_segments.add(segment);
	}

	private boolean isEmpty()
	{
		return m_pinLists.isEmpty() && m_pins.isEmpty() && m_shieldBodies.isEmpty() && m_conductors.isEmpty() &&
				m_conductorsMustRoute.isEmpty() && m_segments.isEmpty();
	}

	public void clear()
	{
		m_pinLists.clear();
		m_pins.clear();
		m_shieldBodies.clear();
		m_conductors.clear();
		m_conductorsMustRoute.clear();
		m_segments.clear();
		m_schemPinFromConductor.clear();
	}

	public void processAction()
	{
		try {
			doProcessAction();
		}
		finally {
			// clear the local collections
			clear();
		}
	}

	private void doProcessAction()
	{
		Pair<Set<? extends ILogicSegmentContainer>, Set<? extends ILogicSegment>> routables = collectRoutables();
		Set<? extends ILogicSegmentContainer> conductors = routables.getFirst();
		Set<? extends ILogicSegment> segments = routables.getSecond();

		if (conductors.isEmpty() && segments.isEmpty()) {
			// clear the local collections
			clear();

			return;
		}

		if (showProgressBar()) {
			ConductorRouteActionRunnable arRunnable = new ConductorRouteActionRunnable(this);
			ProgressTaskClient ptc =
					new ProgressTaskClient(arRunnable.getProgress(), arRunnable, new IProgressCancelledHandler()
					{
						public void progressCancelled(IProgress progress)
						{
							// do nothing
						}
					});

			@Nullable Frame dialogFrame = getDefaultCAFUtilityProvider().getDialogFrame();
			if (dialogFrame == null) {
				// Is the from an unit test and allowing the progress bar to appear ?
				assert false : "diagram frame can not be null";
				return;
			}
			final InterruptableTaskHelper ith = InterruptableTaskHelper.instanceReset();

			ith.setButtonString(ResourceMgr.getString(AutoRouteAction.class, "AutoRouteAction.stop.button.text"));
			ith.executeTask(ptc, dialogFrame, TASK_ID, AutoRouteAction.class, "title.text", "header.text", "descr.text",
					null, MONITOR_INTERVAL, false, true);
		}
		else {
			@Nullable Container container = getDefaultCAFUtilityProvider().getContainer();
			@Nullable Cursor oldCursor = container != null ? container.getCursor() : null;

			try {

				if (oldCursor != null) {
					container.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}

				doProcessAction(null, getDefaultCAFUtilityProvider());
			}
			catch (Exception e) {
				Environment.getExceptionDisplay().displayException(e, false);
			}
			finally {
				if (oldCursor != null) {
					container.setCursor(oldCursor);
				}
			}
		}
	}

	public void processAction(boolean noProgressBar, boolean containsClosedDiagrams)
	{
		doProcessAction(null, containsClosedDiagrams ? getClosedDiagramsCAFUtilityProvider() :
				getDefaultCAFUtilityProvider());
	}

	@NotNull public IModifiableCostStrategy getCostStrategy()
	{

		IModifiableCostStrategy costStrategy = m_defaultCAFUtilityProvider.getCostStrategy();
		assert costStrategy != null;

		return costStrategy;
	}

	public boolean isDefaultProjectPreferences()
	{
		IProjectPreferenceMgr preferenceMgr = CAFUtils.getInstance().getCurrentProjectPreferences();
		if (preferenceMgr != null) {
			int minWireSpacing = preferenceMgr.getConductorSpacing();
			DiagramFlowStyle diagramFlow = DiagramFlowStyle.valueOf(preferenceMgr.getDiagramFlowStyle());
			int minWireLength = preferenceMgr.getMinConductorLength();
			return (minWireLength == IBasePreferencesKeys.MinWireLengthDefault &&
					minWireSpacing == IBasePreferencesKeys.MinWireLengthDefault &&
					diagramFlow.toString().contentEquals(IBasePreferencesKeys.DiagramFlowStyleDefault));
		}
		return false;
	}

	private void doProcessAction(@Nullable IProgress progress, @NotNull ICAFUtilityProvider cafUtilityProvider)
	{
		boolean isDefaultAutoRoutePreference = isDefaultProjectPreferences();
		Collection<ILogicSegmentContainer> processedNetConductors = new ArrayList<ILogicSegmentContainer>();
		Collection<ILogicSegmentContainer> processedSignalConductors = new ArrayList<ILogicSegmentContainer>();
		ICapletView activeView = CAFUtils.getInstance().getActiveCapletView();
		@Nullable SelectSet preSelections = cafUtilityProvider.getPreSelections();
		Collection<ILogicSegmentContainer> preSelectedConductors = new HashSet<>();
		ListMap<ILogicSegmentContainer, ILogicSegment> origSegmentsMap = new ListMap<>();
		boolean shouldUseNewCAVAL = ConnectivityEdgeHelper.useNewCAVAL();
		try {

			ConnectivityEdgeHelper.setUseNewCAVAL(true);
			if (isEmpty()) {
				return;
			}

			if (activeView != null) {
				activeView.lock();
			}

			Pair<Set<? extends ILogicSegmentContainer>, Set<? extends ILogicSegment>> routables = collectRoutables();
			Set<? extends ILogicSegmentContainer> conductors = routables.getFirst();
			Set<? extends ILogicSegment> segments = routables.getSecond();

			if (conductors.isEmpty() && segments.isEmpty()) {

				// clear the local collections
				clear();
				return;
			}

			int conductorsSize = conductors.size();
			int segmentsSize = segments.size();

			if (preSelections != null) {
				for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
					IUIDObject obj = iter.getNext();
					if (obj instanceof ILogicSegment) {
						ILogicSegmentContainer segmentOwner =
								RoutableUtils.getCommonRouteObjectUtils().getSegmentOwner((ILogicSegment) obj);
						if (segmentOwner != null) {
							preSelectedConductors.add(segmentOwner);
						}
					}
					else if (obj instanceof ILogicSegmentContainer) {
						preSelectedConductors.add((ILogicSegmentContainer) obj);
					}
				}
			}

			boolean enableTraverseRouting = isEnableTraverseRouting();
			boolean segmentsNotExist = segments.isEmpty();
			boolean hasHighwayObjects = hasHighwayObjects(conductors, segments);

			Set<IConductor> conductorsNotRouted = new LinkedHashSet<>();

			boolean isSignalRoute = false;
			boolean isRemainingConductorsEmpty = true;
			boolean enableFullSignalRouting = isEnableFullSignalRouting();

			if (!hasHighwayObjects && (enableTraverseRouting || enableFullSignalRouting) && segmentsNotExist) {
				Set<IConductor> schemConductorsNoHighways = CollectionUtils.getObjects(conductors, IConductor.class);
				isSignalRoute =
						doProcessForSignalRoute(progress, cafUtilityProvider, schemConductorsNoHighways, conductorsSize,
								segmentsSize, origSegmentsMap, preSelections, preSelectedConductors,
								conductorsNotRouted, processedSignalConductors, enableFullSignalRouting,
								isDefaultAutoRoutePreference, processedNetConductors);
				isRemainingConductorsEmpty = conductorsNotRouted.isEmpty();
			}

			if (!isSignalRoute || !isRemainingConductorsEmpty) {
				Map<IConductor, IHighwaySchematic> highwayConductorsMap = filterOutHighwayConductors(conductors);
				if (!isRemainingConductorsEmpty) {
					conductors = conductorsNotRouted;
				}
				conductors.removeAll(processedNetConductors);

				doProcessForNetRoute(progress, cafUtilityProvider, conductors, segments, conductorsSize, segmentsSize,
						origSegmentsMap, preSelections, preSelectedConductors, isRemainingConductorsEmpty,
						processedNetConductors, isDefaultAutoRoutePreference, Collections.emptySet());

				Set<IConductor> highwayConductors = new LinkedHashSet<>();
				for (Map.Entry<IConductor, IHighwaySchematic> entry : highwayConductorsMap.entrySet()) {
					IConductor highwayConductor = entry.getKey();
					IHighwaySchematic highwaySchematic = entry.getValue();
					IUIDObjectCollection<IPin> pins = highwayConductor.getPins();
					IPin pin = pins.isEmpty() ? null : pins.iterator().next();
					IJoint joint =
							HighwayConductorSchemCreator
									.createOrGetJoint(Collections.singletonList(highwaySchematic), pin);
					if (joint != null) {
						for (IConnected segment : highwayConductor.getSegments()) {
							if (isHighwayJoint(segment.getStartJoint())) {
								((ILogicSegment) segment).setStartNode(joint);
								highwayConductors.add(highwayConductor);
								break;
							}
							if (isHighwayJoint(segment.getEndJoint())) {
								((ILogicSegment) segment).setEndNode(joint);
								highwayConductors.add(highwayConductor);
								break;
							}
						}
					}
				}

				doProcessForNetRoute(null, cafUtilityProvider, highwayConductors, Collections.emptySet(),
						highwayConductors.size(), 0, origSegmentsMap, preSelections, preSelectedConductors,
						isRemainingConductorsEmpty, processedNetConductors, true, Collections.emptySet());

				cleanUpDanglingHighwaySegments(highwayConductorsMap.values());
			}
		}
		finally {

			ConnectivityEdgeHelper.setUseNewCAVAL(shouldUseNewCAVAL);

			// remove segments of processedNet conductors only.
			for (ILogicSegmentContainer conductor : processedNetConductors) {
				List<ILogicSegment> logicSegments = origSegmentsMap.get(conductor);
				List<IBaseSegment> newSegments = new ArrayList<>(conductor.getSegmentsOfType(IBaseSegment.class));
				Collections.sort(newSegments, SegmentHelper.getSegmentComparator());
				if (!newSegments.isEmpty()) {
					//LOGIC-14114:Do not remove associated graphic of wire after fetch/auto-route
					for (ILogicSegment segment : logicSegments) {
						LogicUtils.shiftAssociatedGraphics(segment, newSegments.get(0));
					}
				}
				for (ILogicSegment segment : logicSegments) {
					deleteSegment(segment);
				}
			}
			// update the preselections for conductors created in signal routing
			@Nullable ICapletModel model = cafUtilityProvider.getActiveCapletModel();
			if (model instanceof ILogicModel) {
				ISchemDiagram diagram = ((ILogicModel) model).getDiagram();
				if (preSelections != null && diagram != null) {
					SelectSet newSelections = new SelectSet();
					for (ILogicSegmentContainer routedConductor : processedSignalConductors) {
						if (DiagramHelper.getDiagram(routedConductor) == diagram) {
							for (ISegment aNewSegment : routedConductor.getSegmentsOfType(ISegment.class)) {
								newSelections.add(new Selection(aNewSegment), false);
							}
						}
					}
					//VIEWS-5342 decision not to process signal selections for now. But, we have to check later with proper requirements.
					// preSelections.add(newSelections);
					// in case of signal routing, clear the segments and conductors routed
					if (!processedSignalConductors.isEmpty()) {
						for (ILogicSegmentContainer cond : preSelectedConductors) {
							if (processedNetConductors.contains(cond)) {
								preSelections.remove(cond.getUID(), false);
							}
						}
					}
					preSelections.pruneOrphanedSelections();
				}
				else {
					assert false;
				}
			}

			// clear the local collections
			clear();

			if (activeView != null) {
				activeView.unlock();
			}
		}
	}

	private boolean isHighwayJoint(IJoint startJoint)
	{
		if (startJoint != null) {
			IDiagramObjectIterator associations = startJoint.getAssociations();
			Set<IHighwaySegment> highwaySegments = startJoint.getAssociations(IHighwaySegment.class);
			return associations.getSize() == 1 || !highwaySegments.isEmpty();
		}
		return false;
	}

	private Map<IConductor, IHighwaySchematic> filterOutHighwayConductors(
			Set<? extends ILogicSegmentContainer> segmentContainers)
	{
		Set<IHighwaySchematic> highwaySchems = CollectionUtils.getObjects(segmentContainers, IHighwaySchematic.class);
		Map<IConductor, IHighwaySchematic> highwayConductors = new LinkedHashMap<>();
		for (IHighwaySchematic highwaySchem : highwaySchems) {
			for (IConductor highwayCondcutor : highwaySchem.getConductors()) {
//				if (segmentContainers.contains(highwayCondcutor)) {
				highwayConductors.put(highwayCondcutor, highwaySchem);
//				}
			}
		}

		segmentContainers.removeAll(highwayConductors.keySet());

		return highwayConductors;
	}

	private boolean hasHighwayObjects(Set<? extends ILogicSegmentContainer> conductors,
			Set<? extends ILogicSegment> segments)
	{
		boolean highwayExist = !CollectionUtils.getObjects(conductors, IHighwaySchematic.class).isEmpty() ||
				!CollectionUtils.getObjects(segments, IHighwaySegment.class).isEmpty();
		if (highwayExist) {
			return true;
		}
		for (ILogicSegmentContainer conductor : conductors) {
			for (IConnected segment : conductor.getSegments()) {
				if (isConnectedToHighwaySegment(segment.getStartJoint()) ||
						isConnectedToHighwaySegment(segment.getEndJoint())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isConnectedToHighwaySegment(@Nullable IJoint joint)
	{
		return joint != null && !joint.getAssociations(IHighwaySegment.class).isEmpty();
	}

	private boolean doProcessForSignalRoute(IProgress progress, ICAFUtilityProvider cafUtilityProvider,
			Set<IConductor> conductors,
			int conductorsSize, int segmentsSize, ListMap<ILogicSegmentContainer, ILogicSegment> origSegmentsMap,
			@Nullable SelectSet preSelections, Collection<ILogicSegmentContainer> preSelectedConductors,
			@NotNull Set<IConductor> remainingConductors, Collection<ILogicSegmentContainer> processedSignalConductors,
			boolean enableFullSignalRouting, boolean isDefaultAutoRoutePreference,
			Collection<ILogicSegmentContainer> processedNetConductors)
	{
		ConductorRouteActionHelper.IndexGenerator idxGen = new ConductorRouteActionHelper.IndexGenerator();

		ListMap<ISchemDiagram, IConductor> diagramToConductors = getDiagramToObjectsMap(conductors);
		List<List<IConnectivityEdge>> connectivityEdgeList = new ArrayList<>();
		for (Map.Entry<ISchemDiagram, List<IConductor>> entry : diagramToConductors.entrySet()) {
			ISchemDiagram diagram = entry.getKey();
			List<IConductor> diagramConds = entry.getValue();
			Set<IConductor> internalConnectivityConductors = new LinkedHashSet<>();
			for (IConductor conductor : diagramConds) {
				if(isValidInternalConductor(conductor.getConnectivity(), diagram)){
					internalConnectivityConductors.add(conductor);
				}
			}
			Collection<IDiagramObject> additionalObstaclesToIgnore =
					ConductorRouteActionHelper.collectObstaclesToIgnore(conductors, new HashSet<>(), origSegmentsMap,
							true);

			doProcessForNetRoute(progress, cafUtilityProvider, internalConnectivityConductors, new HashSet<>(),
					conductorsSize, segmentsSize, origSegmentsMap, preSelections, preSelectedConductors,
					true, processedNetConductors, true, additionalObstaclesToIgnore);
			diagramConds.removeAll(internalConnectivityConductors);

			edgeResultSet.clear();
			ListMap<IAbstractPin, IPin> pinReps = new ListMap<>();
			Set<IUIDObject> originalSchematics = new HashSet<IUIDObject>();
			Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges = new HashMap<>();
			Pair<List<IRouteIO>, ShieldBodyHookupSelector> pair =
					orderConductorsAsCEs(diagram, diagramConds, pinReps, originalSchematics, remainingConductors,
							ignoredEdges, enableFullSignalRouting, connectivityEdgeList);

			List<ISegment> segments = CollectionUtils.getObjectList(originalSchematics, ISegment.class);
			for (ISegment segment : segments) {
				IConductor conductor = segment.getConductor();
				originalSchematics.add(conductor);
			}

			List<IRouteIO> routes = pair.getFirst();
			ShieldBodyHookupSelector shieldBodyHookupSelector = pair.getSecond();

			if (!containsSignal(routes) && !enableFullSignalRouting) {
				remainingConductors.addAll(diagramConds);
				return false;
			}

			Collection<IDiagramObject> obstaclesToIgnore = ConductorRouteActionHelper
					.collectObstaclesToIgnore(routes, origSegmentsMap, diagram, originalSchematics);
			if (!enableFullSignalRouting) {
				doSignalRouteWithoutAddedPath(cafUtilityProvider, processedSignalConductors, diagram, pinReps,
						originalSchematics, ignoredEdges, routes, shieldBodyHookupSelector, obstaclesToIgnore, isDefaultAutoRoutePreference);
			}
			else {
				doSignalRouteUsingAddedPath(cafUtilityProvider, processedSignalConductors, diagram,
						pinReps, originalSchematics, routes, obstaclesToIgnore, enableFullSignalRouting,
						isDefaultAutoRoutePreference, connectivityEdgeList);
			}
		}
		return true;
	}

	private void doSignalRouteWithoutAddedPath(ICAFUtilityProvider cafUtilityProvider,
			Collection<ILogicSegmentContainer> processedSignalConductors, ISchemDiagram diagram,
			ListMap<IAbstractPin, IPin> pinReps, Set<IUIDObject> originalSchematics,
			Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges, List<IRouteIO> routes,
			ShieldBodyHookupSelector shieldBodyHookupSelector, Collection<IDiagramObject> obstaclesToIgnore,
			boolean isDefaultAutoRoutePreference)
	{
		for (IConductor conductor : CollectionUtils.getObjects(obstaclesToIgnore, IConductor.class)) {
			obstaclesToIgnore.addAll(conductor.getSegments());
		}

		if (!ignoredEdges.isEmpty()) {
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(ResourceMgr
					.getString(ConductorRouteActionImpl.class,
							"ConductorRouteActionImpl.IgnoredConductors.Message"));
			for (Set<chs.cof.logical.cable.IConductor> ignoredConductors : ignoredEdges.values()) {
				for (chs.cof.logical.cable.IConductor conductor : ignoredConductors) {
					CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(conductor.getName());
				}
			}
		}

		routeCEsIncludingShields(
				obstaclesToIgnore,
				diagram,
				cafUtilityProvider,
				routes,
				shieldBodyHookupSelector, pinReps, originalSchematics, processedSignalConductors, isDefaultAutoRoutePreference);
	}

	private void doSignalRouteUsingAddedPath(ICAFUtilityProvider cafUtilityProvider,
			Collection<ILogicSegmentContainer> processedSignalConductors, ISchemDiagram diagram,
			ListMap<IAbstractPin, IPin> pinReps,
			Set<IUIDObject> originalSchematics, List<IRouteIO> routes, Collection<IDiagramObject> obstaclesToIgnore,
			boolean enableFullSignalRouting, boolean isDefaultAutoRoutePreference,
			List<List<IConnectivityEdge>> connectivityEdgeList)
	{
		for (IConductor conductor : CollectionUtils.getObjects(obstaclesToIgnore, IConductor.class)) {
			obstaclesToIgnore.addAll(conductor.getSegments());
			processedSignalConductors.add(conductor);
		}
		Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap = new HashMap<>();
		deleteSchematics(routes, diagram, originalSchematics, inlineConnectorToWidthMap);
		Set<chs.cof.logical.cable.IConductor> conductorsRouted = new HashSet<>();
		List<IRouteIO> orderedShieldRoutes = ConductorRouteActionHelper.extractShieldRoutesFromCEs(routes);
		for (IRouteIO routeIO : routes) {
			if (!orderedShieldRoutes.contains(routeIO)) {
				IConnectivityEdge cEdge = (IConnectivityEdge) routeIO.getRouteInput().getUserObject();
				conductorsRouted.addAll(ConnectivityEdgeUtilities.getContentConductors(cEdge));
				chs.cof.logical.cable.IConductor startConductor = cEdge.getStartConductor();
				if (startConductor != null) {
					conductorsRouted.add(startConductor);
				}
				chs.cof.logical.cable.IConductor endConductor = cEdge.getEndConductor();
				if (endConductor != null) {
					conductorsRouted.add(endConductor);
				}
			}
		}
		AutoRoutePathDeterminer
				.layoutUsingAddedPath(diagram, new HashSet<>(), obstaclesToIgnore, edgeResultSet, originalSchematics,
						!enableFullSignalRouting, isDefaultAutoRoutePreference, connectivityEdgeList,m_schemPinFromConductor);
		//Routing ShieldEdges
		inlineConnectorToWidthMap.clear();
		refreshMulticoreIndicatorsForCEs(routes, cafUtilityProvider, diagram);
		for (chs.cof.logical.cable.IConductor conductor : conductorsRouted) {
			for (IDiagramObject sCond : diagram.getRepresentations(conductor.getUID())) {
				if (sCond instanceof IConductor) {
					PortHelper.updatePortGfx((ICompoundObject) sCond, diagram.getGrid().getGridSpacing());
					processedSignalConductors.add((IConductor) sCond);
				}
			}
		}
		Collection<IConnectivityEdge> shieldEdges = new LinkedHashSet<IConnectivityEdge>();
		for (IRouteIO shieldRoute : orderedShieldRoutes) {
			Object userObject = shieldRoute.getRouteInput().getUserObject();
			if (userObject instanceof IConnectivityEdge) {
				shieldEdges.add((IConnectivityEdge) userObject);
			}
		}
		ShieldBodyHookupSelector shieldBodyHookupSelector =
				new ShieldBodyHookupSelector(shieldEdges, diagram, Collections.emptySet(), true, null);
		SchemObjectHandler schemObjectHandler = new SchemObjectHandler();
		schemObjectHandler.populateOldSchemAttributeMap(originalSchematics);
		ListMap<IAbstractPin, IPin> updatedPinReps = getPinRepsAfterAddedPath(diagram, pinReps, orderedShieldRoutes);
		routeCEs(orderedShieldRoutes, obstaclesToIgnore, diagram, shieldBodyHookupSelector, updatedPinReps,
				inlineConnectorToWidthMap, processedSignalConductors, schemObjectHandler, isDefaultAutoRoutePreference, true);
	}

	@NotNull private ListMap<IAbstractPin, IPin> getPinRepsAfterAddedPath(ISchemDiagram diagram,
			ListMap<IAbstractPin, IPin> pinReps, List<IRouteIO> routes)
	{
		ListMap<IAbstractPin, IPin> updatedPinReps = new ListMap<>();
		updatedPinReps.addAll(pinReps);
		Set<IAbstractPin> pins = new HashSet<>();
		for (IRouteIO route : routes) {
			IConnectivityEdge cEdge = (IConnectivityEdge) route.getRouteInput().getUserObject();
			IAbstractPin edgePin = cEdge.getPin1();
			if (edgePin != null) {
				pins.add(edgePin);
			}
			edgePin = cEdge.getPin2();
			if (edgePin != null) {
				pins.add(edgePin);
			}
		}
		diagram.getPinLists().stream()
				.flatMap(schemPinList -> schemPinList.getPins().stream())
				.filter(pin -> pins.contains(pin.getConnectivity()))
				.forEach(pin -> updatedPinReps.add(pin.getConnectivity(), pin));

		return updatedPinReps;
	}

	private boolean containsSignal(@NotNull List<IRouteIO> routes)
	{
		for (IRouteIO routeIO : routes) {
			IRoutable routable = routeIO.getRouteInput();
			Object userObject = routable.getUserObject();
			if (userObject instanceof IConnectivityEdge) {
				IConnectivityEdge cEdge = (IConnectivityEdge) userObject;
				List<Object> content = CollectionUtils.createList(cEdge.getContent());
				if (!(content.size() == 1 && content.get(0) instanceof chs.cof.logical.cable.IConductor)) {
					return true;
				}
			}
		}
		return false;
	}

	private void doProcessForNetRoute(IProgress progress, ICAFUtilityProvider cafUtilityProvider,
			Set<? extends ILogicSegmentContainer> conductors,
			Set<? extends ILogicSegment> segments, int conductorsSize, int segmentsSize,
			ListMap<ILogicSegmentContainer, ILogicSegment> origSegmentsMap,
			@Nullable SelectSet preSelections, Collection<ILogicSegmentContainer> preSelectedConductors,
			boolean isRemainingConductorsEmpty,
			Collection<ILogicSegmentContainer> processedConductors, boolean isDefaultAutoRoutePreference,
			Collection<IDiagramObject> additionalObstaclestoIgnore)
	{
		Collection<IDiagramObject> obstaclesToIgnore =
				ConductorRouteActionHelper.collectObstaclesToIgnore(conductors, segments, origSegmentsMap, false);
		obstaclesToIgnore.addAll(additionalObstaclestoIgnore);

		ConductorRouteActionHelper.IndexGenerator idxGen = new ConductorRouteActionHelper.IndexGenerator();
		ListMap<ISchemDiagram, ? extends ILogicSegment> diagramToSegments = getDiagramToObjectsMap(segments);
		for (Map.Entry<ISchemDiagram, ? extends List<? extends ILogicSegment>> entry : diagramToSegments
				.entrySet()) {
			ISchemDiagram diagram = entry.getKey();
			List<? extends ILogicSegment> diagramSegs = entry.getValue();
			// order segments
			List<IRouteIO> orderedRoutes = EdgeOrder.orderSegments(diagramSegs, diagram);
			routeSegments(orderedRoutes, progress, idxGen, conductorsSize, segmentsSize, obstaclesToIgnore,
					cafUtilityProvider);
		}

		ListMap<ISchemDiagram, ? extends ILogicSegmentContainer> diagramToConductors =
				getDiagramToObjectsMap(conductors);
		for (Map.Entry<ISchemDiagram, ? extends List<? extends ILogicSegmentContainer>> entry : diagramToConductors
				.entrySet()) {
			ISchemDiagram diagram = entry.getKey();
			List<? extends ILogicSegmentContainer> diagramConds = entry.getValue();

			routeConductorsIncludingShields(progress, conductorsSize, segmentsSize,
					obstaclesToIgnore,
					m_defaultCAFUtilityProvider.getCostStrategy(),
					processedConductors, idxGen,
					diagram,
					diagramConds,
					cafUtilityProvider, conductors,
					isDefaultAutoRoutePreference);
		}
	}

	private void deleteSegment(ILogicSegment segment)
	{
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		if (cdh.goingToCreate(segment)) {
			cdh.removeCreationObject(segment);
			if (segment instanceof IHighwaySegment) {
				IHighwaySegment highwaySegment = (IHighwaySegment) segment;
				if (SingleLineHelper.isSingleLineSegment(highwaySegment)) {
					cdh.removeCreationObject(highwaySegment.createOrGetSingleLineDefaultTableData());
				}
				else {
					cdh.removeCreationObject(highwaySegment.createOrGetHighwayDefaultTableData());
				}
				for (IPluginTableData pluginTableData : highwaySegment.getPluginTables()) {
					cdh.removeCreationObject(pluginTableData);
				}
				for (IDynamicTableData dynamicTableData : highwaySegment.getAllDynamicTables()) {
					cdh.removeCreationObject(dynamicTableData);
				}
			}
		}
		segment.delete();
	}

	private void cleanUpDanglingHighwaySegments(Collection<IHighwaySchematic> highwaySchematics)
	{
		for (IHighwaySchematic highwaySchematic : highwaySchematics) {

			int segmentsSize = highwaySchematic.getSegments().size();
			for (int i = 0; i < segmentsSize; i++) {
				if (!cleanUpDanglingHighwaySegment(highwaySchematic)) {
					break;
				}
			}
		}
	}

	private boolean cleanUpDanglingHighwaySegment(IHighwaySchematic highwaySchematic)
	{
		for (IHighwaySegment highwaySegment : highwaySchematic.getSegmentsOfType(IHighwaySegment.class)) {
			boolean canDeleteAtStart = checkForDeleteSegment(highwaySchematic, highwaySegment,
					highwaySegment.getStartJoint());
			boolean canDeleteAtEnd = checkForDeleteSegment(highwaySchematic, highwaySegment,
					highwaySegment.getEndJoint());
			if (canDeleteAtStart && canDeleteAtEnd) {
				cleanUpAndDeleteSegment(highwaySchematic, highwaySegment);
				return true;
			}
		}
		return false;
	}

	private boolean checkForDeleteSegment(IHighwaySchematic highwaySchematic, IHighwaySegment highwaySegment,
			IJoint startJoint)
	{
		if (startJoint == null) {
			return true;
		}

		if (startJoint.getNumAssociations() == 1) {
			IDiagramObject anAssociation = startJoint.getAssociations().next();
			if (anAssociation == highwaySegment) {
				return true;
			}
		}

		return false;
	}

	private void cleanUpAndDeleteSegment(IHighwaySchematic highwaySchematic, IHighwaySegment highwaySegment)
	{
		ConductorRouteActionHelper.removeConductorSegment(highwaySchematic, highwaySegment);
		deleteSegment(highwaySegment);
	}

	@NotNull private <T extends ICompoundObject> ListMap<ISchemDiagram, T> getDiagramToObjectsMap(
			@NotNull Collection<T> conductors)
	{
		ListMap<ISchemDiagram, T> diagramToObjs = new ListMap<ISchemDiagram, T>();
		for (T obj : conductors) {
			if (obj instanceof IDiagramObject) {
				ISchemDiagram diagram = DiagramHelper.getDiagram((IDiagramObject) obj);
				diagramToObjs.add(diagram, obj);
			}
		}
		return diagramToObjs;
	}

	private void routeSegments(@NotNull List<IRouteIO> orderedRoutes, @Nullable IProgress progress,
			@NotNull ConductorRouteActionHelper.IndexGenerator idxGen, int conductorsSize, int segmentsSize,
			@NotNull Collection<IDiagramObject> obstaclesToIgnore, @NotNull ICAFUtilityProvider cafUtilityProvider)
	{
		ConductorRouteActionHelper.CompositeStyleApplier
				styleApplier = new ConductorRouteActionHelper.CompositeStyleApplier();
		Set<ILogicObject> ignoredSegments = new LinkedHashSet<>();
		// route segments

		for (IRouteIO routeIO : orderedRoutes) {
			if (progress != null && progress.isCancelled()) {
				throw new ProgressCancelledException("Progress Cancelled while processing conductors");
			}

			IRoutable routable = routeIO.getRouteInput();
			Object userObject = routable.getUserObject();
			if (!(userObject instanceof ILogicSegment)) {
				continue;
			}

			ILogicSegment segment = (ILogicSegment) userObject;

			ILogicSegmentContainer segmentOwner =
					RoutableUtils.getCommonRouteObjectUtils().getSegmentOwner(segment);

			ConductorRouteActionHelper
					.printConductorDebugInfo(segmentOwner, idxGen.next(), conductorsSize + segmentsSize);

			routeSegment(routeIO, obstaclesToIgnore, cafUtilityProvider, ignoredSegments);
			if (segmentOwner != null) {
				styleApplier.addObject(segmentOwner);
			}
		}

		styleApplier.applyStyle();
		if (!ignoredSegments.isEmpty()) {
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
					ResourceMgr.getString(ConductorRouteActionImpl.class,
							"ConductorRouteActionImpl.IgnoredConductors.Message"));
			for (ILogicObject object : ignoredSegments) {
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(object.getName());
			}
		}
	}

	/**
	 * Gathers the input for routing. If a conductor has to be routed and if it is part of a multicore, the multicore
	 * shield should also be routed.
	 *
	 * @return Pair<Set<IConductor>, Set<ISegment>>
	 */
	private Pair<Set<? extends ILogicSegmentContainer>, Set<? extends ILogicSegment>> collectRoutables()
	{
		Collection<? extends IAbstractSchemPin> pins = collectAllPins();
		Set<? super ILogicSegmentContainer> conductors = new LinkedHashSet<>();
		conductors.addAll(collectAllConductors(pins));
		Set<ILogicSegment> segments = getFilteredSegments();
		Set<? super ILogicSegmentContainer> allConductors = new LinkedHashSet<>(conductors);
		for (ILogicSegment segment : segments) {
			ILogicSegmentContainer segmentOwner = RoutableUtils.getCommonRouteObjectUtils().getSegmentOwner(segment);
			if (segmentOwner != null) {
				allConductors.add(segmentOwner);
			}
		}
		// Conductors might have added to CreationDeletionHelper to delete
		Set<ILogicSegmentContainer> conductorsToRoute =
				CollectionUtils.getObjects(getNonDeletableObjects(allConductors), ILogicSegmentContainer.class);
		// Collect the multicores of all the conductors and segments, from the multicores collect all the connected shields.
		Set<IShieldBody> shieldBodies = new LinkedHashSet<IShieldBody>();
		for (ILogicSegmentContainer conductor : conductorsToRoute) {
			ILogicObject logicObject =
					RoutableUtils.getCommonRouteObjectUtils().getSegmentOwnerConnectivity(conductor);
			if (logicObject instanceof IShieldConductor) {
				continue;
			}
			@Nullable IMulticore multicore = RoutableUtils.getCommonRouteObjectUtils().getMulticore(conductor);

			List<IMulticore> multicores = new ArrayList<>();
			while (multicore != null) {
				multicores.add(multicore);
				multicore = multicore.getParent();
			}

			for (IMulticore aMulticore : multicores) {
				@Nullable chs.cof.logical.cable.IShieldBody shieldBody =
						aMulticore != null ? aMulticore.getShieldBody() : null;
				if (shieldBody != null && conductor instanceof IDiagramObject) {
					ISchemDiagram diagram = DiagramHelper.getDiagram((IDiagramObject) conductor);
					for (IDiagramObjectIterator itr = diagram.getRepresentations(shieldBody.getUID()); itr.hasNext(); ) {
						IDiagramObject next = itr.getNext();
						if (next instanceof IShieldBody) {
							shieldBodies.add((IShieldBody) next);
						}
					}
				}
			}
		}

		conductors.addAll(getConductorsFromShiledBodies(shieldBodies));
		deleteSegmentsIfCondcutorDoesExist(conductors, segments);
		return new Pair<Set<? extends ILogicSegmentContainer>, Set<? extends ILogicSegment>>(
				CollectionUtils.getObjects(conductors, ILogicSegmentContainer.class),
				CollectionUtils.getObjects(segments, ILogicSegment.class));
	}

	private void deleteSegmentsIfCondcutorDoesExist(@NotNull Set<? super ILogicSegmentContainer> conductors,
			@NotNull Set<ILogicSegment> segments)
	{
		Iterator<ILogicSegment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			ILogicSegment segment = iterator.next();
			IDiagramObject parent = segment.getParent();
			if (parent instanceof ILogicSegmentContainer) {
				ILogicSegmentContainer conductor = (ILogicSegmentContainer) parent;
				if (conductors.contains(conductor)) {
					iterator.remove();
				}
			}
		}
	}

	@NotNull private Collection<? extends IAbstractSchemPin> collectAllPins()
	{
		// FEAT00013786: Stack pins are connected only to Highways and autorouting not supported for highways
		// so no need to iterate over stack pins.
		Collection<? super IAbstractSchemPin> pins = new LinkedHashSet<>(getFilteredPins());

		for (IPinList pinList : getFilteredPinLists()) {
			pins.addAll(pinList.getAllPins());
		}
		return CollectionUtils.getObjectList(pins, IAbstractSchemPin.class);
	}

	@NotNull private Set<? extends ILogicSegmentContainer> collectAllConductors(
			@NotNull Collection<? extends IAbstractSchemPin> pins)
	{
		Set<? super ILogicSegmentContainer> conductors = getAllFilteredConductors();
		for (IAbstractSchemPin schemPin : pins) {
			conductors.addAll(RoutableUtils.getCommonRouteObjectUtils().getSegmentContainers(schemPin));
		}
		return CollectionUtils.getObjects(conductors, ILogicSegmentContainer.class);
	}

	@NotNull private Set<IPinList> getFilteredPinLists()
	{
		return isEnableAutoRouting() ? getNonDeletableObjects(m_pinLists) : Collections.<IPinList>emptySet();
	}

	@NotNull private Set<? super IAbstractSchemPin> getFilteredPins()
	{
		return isEnableAutoRouting() ? getNonDeletableObjects(m_pins) : Collections.emptySet();
	}

	@NotNull private Set<IShieldBody> getFilteredShieldBodies()
	{
		return isEnableAutoRouting() ? getNonDeletableObjects(m_shieldBodies) : Collections.<IShieldBody>emptySet();
	}

	@NotNull private Set<? super ILogicSegmentContainer> getAllFilteredConductors()
	{
		Set<? super ILogicSegmentContainer> conductors = new LinkedHashSet<>(getFilteredConductorsMustRoute());
		conductors.addAll(getConductorsFromShiledBodies());
		conductors.addAll(getFilteredConductors());
		return conductors;
	}

	@NotNull private Set<? extends ILogicSegmentContainer> getFilteredConductors()
	{
		Set<? extends ILogicSegmentContainer> conductors =
				isEnableAutoRouting() ? getNonDeletableObjects(m_conductors) : Collections.emptySet();
		return conductors;
	}

	public <T> LinkedHashSet<T> getNonDeletableObjects(Collection<T> uidObjects)
	{
		LinkedHashSet<T> filteredSet = new LinkedHashSet<T>(uidObjects.size());
		filteredSet.addAll(CollectionUtils.getFilteredCollection(uidObjects, new IObjectFilter<T>()
		{
			public boolean accept(T obj)
			{
				if (obj instanceof IUIDObject) {
					return !CreationDeletionHelper.getTheCreationHelper().goingToDelete((IUIDObject) obj);
				}
				return false;
			}
		}));
		return filteredSet;
	}

//	private static Pair<IJoint, IJoint> getClosestJoints(ISchemDiagram diagram, IConnectivityVertex startVertex,
//			IConnectivityVertex endVertex,
//			Object first, Object last, ShieldBodyHookupSelector shieldBodyHookupSelector, IConnectivityEdge cEdge,
//			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap)
//	{
//		Pair<IJoint, IJoint> pair = new Pair<IJoint, IJoint>(null, null);
//		//if it is a cEdge like shield-splice-wire-inline-wire-splice-shield
//		IJoint joint1 = getClosestJoint(diagram, startVertex, first, shieldBodyHookupSelector, cEdge,
//				conductorToJunctionMap, null, null);
//
//		IJoint joint2 = getClosestJoint(diagram, endVertex, last, shieldBodyHookupSelector, cEdge,
//				conductorToJunctionMap, joint1, null);
//
//		//In case of cEdge being from sheild termination to shield termination via inline pin pair,
//		// below step necessary
//		IJoint nearestJoint =
//				getClosestJoint(diagram, startVertex, first, shieldBodyHookupSelector, cEdge,
//						conductorToJunctionMap, joint2, joint1);
//		if (joint1 == null || (nearestJoint != null && !joint1.equals(nearestJoint))) {
//			joint1 = nearestJoint;
//		}
//		return new Pair<IJoint, IJoint>(joint1, joint2);
//	}
//
//	@Nullable
//	private static IJoint getClosestJoint(ISchemDiagram diagram, IConnectivityVertex vertex, Object adjacentConductor,
//			ShieldBodyHookupSelector shieldBodyHookupSelector, IConnectivityEdge cEdge,
//			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap, @Nullable IJoint fromJoint,
//			@Nullable IJoint thisJoint)
//	{
//		IAbstractPin pin1 = getPin(vertex, adjacentConductor);
//		if (pin1 == null) {
//			if (vertex.isVirtual() && vertex instanceof ConnectivityVirtualVertex) {
//				IShieldBodyHookup hookup = shieldBodyHookupSelector.getShieldBodyHookup(cEdge);
//				if (hookup == null) {
//					if (adjacentConductor instanceof IShieldTermination) {
//						IShieldConductor shield = ((IShieldTermination) adjacentConductor).getShield();
//						if (shield != null) {
//							IShieldBodyHookup nearestHookup =
//									ShieldBodyHookupSelector.findNearestHookup(diagram, shield, fromJoint);
//							if (nearestHookup != null) {
//								IJoint node = DiagramGenerationUtilities.getNode(nearestHookup);
//								if (thisJoint == null || !node.equals(thisJoint)) {
//									conductorToJunctionMap.put(shield, node);
//									return node;
//								}
//								else {
//									return thisJoint;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}

	@NotNull private Set<ILogicSegment> getFilteredSegments()
	{
		if (isEnableAutoRouting()) {
			Set<ILogicSegment> routableSegments = new LinkedHashSet<>(m_segments.size());
			for (ILogicSegment seg : new LinkedHashSet<ILogicSegment>(getNonDeletableObjects(m_segments))) {
				ILogicSegmentContainer segmentOwner = RoutableUtils.getCommonRouteObjectUtils().getSegmentOwner(seg);
				if (segmentOwner != null &&
						!CreationDeletionHelper.getTheCreationHelper().goingToDelete(segmentOwner)) {
					routableSegments.add(seg);
				}
			}
			return routableSegments;
		}
		return Collections.emptySet();
	}

	@NotNull private Set<? extends ILogicSegmentContainer> getFilteredConductorsMustRoute()
	{
		return getNonDeletableObjects(m_conductorsMustRoute);
	}

	@NotNull private Set<? extends ILogicSegmentContainer> getConductorsFromShiledBodies()
	{
		return getConductorsFromShiledBodies(getFilteredShieldBodies());
	}

	private Set<? extends ILogicSegmentContainer> getConductorsFromShiledBodies(@NotNull Set<IShieldBody> shieldBodies)
	{
		Set<ILogicSegmentContainer> connectedShields = new LinkedHashSet<>(shieldBodies.size());
		for (IShieldBody shieldBody : shieldBodies) {
			connectedShields.addAll(shieldBody.getAllDirectlyConnectedShields());
		}
		return getNonDeletableObjects(connectedShields);
	}

	private boolean routeSegment(@NotNull IRouteIO routeIO,
			@NotNull Collection<IDiagramObject> obstaclesToIgnore, @NotNull ICAFUtilityProvider cafUtilityProvider,
			Set<ILogicObject> ignoredSegments)
	{
		IRoutable routable = routeIO.getRouteInput();
		ILogicSegment segment = (ILogicSegment) routable.getUserObject();

		IConductor schemConductor = segment instanceof ISegment ? ((ISegment) segment).getConductor() : null;
		chs.cof.logical.cable.IConductor cableConductor =
				schemConductor != null ? schemConductor.getConnectivity() : null;

		ISchemDiagram diagram = DiagramHelper.getDiagram(segment);
		boolean shouldRouteInsideDev = ConductorRouteActionHelper.shouldRouteInsideDevice(cableConductor, diagram);
		@Nullable IInsertConductorSegmentModifier conductorSegmentModifier =
				getConductorSegmentModifier(diagram, cafUtilityProvider);
		if (conductorSegmentModifier == null) {
			// Is this from unit test ?
			return false;
		}

		List<ILocation> endLocations = new ArrayList<ILocation>(2);
		RoutableUtils.getEndLocations(segment, endLocations);
		int gridSpacing = diagram.getGrid().getGridSpacing();
		for (ILocation location : endLocations) {
			if (GeneratorUtils.isOffGridPin(location, gridSpacing)) {
				ignoredSegments.add(segment.getConnectivityRefOwner().getConnectivity());
				return false;
			}
		}

		boolean considerPreferredFirstSegmentLength = true;
		if (segment.getConnectivityRefOwner() != null &&
				segment.getConnectivityRefOwner().getConnectivity() instanceof IShieldConductor) {
			considerPreferredFirstSegmentLength = false;
		}

		IPrototype prototype = routeIO.getPrototype();
		int wireSpacing = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getMinimumWireSpacingEnsured() : WIRE_SPACING;
		DiagramFlowStyle diagramFlowStyle = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getDiagramFlowStyle() : DiagramFlowStyle.DEFAULT;
		List<List<ILocation>> pointsList =
				BlockageUtils.routeConductor(diagram, endLocations, considerPreferredFirstSegmentLength,
						obstaclesToIgnore, routable.isLoop(), ConductorRouteActionHelper.ROUTE_GRAPH_SIZE_TYPE,
						prototype, m_defaultCAFUtilityProvider.getCostStrategy(), routable.getExtents(), wireSpacing,
						diagramFlowStyle, shouldRouteInsideDev
				);

		if (pointsList.size() != 1) {
			ILogicObject logicObject =
					RoutableUtils.getCommonRouteObjectUtils().getSegmentOwnerConnectivity(segment);
			assert logicObject != null;
			assert false : "points size must be one for re-route of segment of conductor " +
					logicObject.getName();
			return false;
		}

		List<ILocation> points = pointsList.get(0);

		List<ILocation> segmentEndLocations = new ArrayList<ILocation>(2);
		RoutableUtils.getEndLocations(segment, segmentEndLocations, false);
		if (!order(segmentEndLocations, points)) {
			return false;
		}

		points.remove(points.size() - 1);
		points.remove(0);
		List<ILogicSegment> segments = points.isEmpty() ? Collections.emptyList() :
				conductorSegmentModifier.modifySegment(points, segment);

		routeIO.setRouteOutput(new SegmentsRouteOutput(segments));

		return true;
	}

	private boolean order(@NotNull List<ILocation> refLocations, @NotNull List<ILocation> points)
	{
		ILocation refFirst = refLocations.get(0);
		ILocation refLast = refLocations.get(1);

		return order(refFirst, refLast, points);
	}

	private boolean order(@NotNull ILocation refFirst, @NotNull ILocation refLast, @NotNull List<ILocation> points)
	{
		ILocation first = points.get(0);
		ILocation last = points.get(points.size() - 1);

		boolean forwardOrder = first.equals(refFirst) && last.equals(refLast);
		boolean reverseOrder = first.equals(refLast) && last.equals(refFirst);

		if (forwardOrder == reverseOrder) {
			assert false : "Error in routing output.\n Start point : " + toStringLocation(refFirst) + "\n End point: " +
					toStringLocation(refLast) + "\n Routing output : \n" + toStringLocation(points);
			return false;
		}

		if (reverseOrder) {
			Collections.reverse(points);
		}

		return true;
	}

	public boolean runInLast()
	{
		return false;
	}

	protected void updatePreferences()
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		// flow styling preferences
		preferences.putBoolean(ENABLE_AUTO_ROUTING, isEnableNetRouting());
		preferences.putBoolean(ENABLE_TRAVERSE_ROUTING, isEnableTraverseRouting());
		preferences.putBoolean(ENABLE_FULL_SIGNAL_ROUTING, isEnableFullSignalRouting());
		preferences.putBoolean(ENABLE_THREE_PHASE_ROUTING, isThreePhaseRouting());

		if (BuildInfo.getBuildInfo().areDeveloperOrQAExtensionsEnabled()) {
			System.out.println("Traverse routing is " + (isEnableTraverseRouting() ? "enabled" : "disabled"));
			System.out.println("Auto routing is " + (isEnableAutoRouting() ? "enabled" : "disabled"));
		}
	}

	@NotNull private static String toStringLocation(@NotNull ILocation location)
	{
		StringBuilder str = new StringBuilder();
		str.append('(');
		str.append(location.getX());
		str.append(',');
		str.append(location.getY());
		str.append(')');

		return str.toString();
	}

	@NotNull private static String toStringLocation(@NotNull List<? extends ILocation> locations)
	{
		StringBuilder str = new StringBuilder();
		str.append('[');
		boolean isFirstLocation = true;
		for (ILocation location : locations) {
			if (!isFirstLocation) {
				str.append(", ");
			}
			str.append(toStringLocation(location));
			isFirstLocation = false;
		}
		str.append(']');

		return str.toString();
	}

	/**
	 * Used to create new grip points(so new segments) for a given segment
	 *
	 * @param diagram schematic diagram
	 * @param cafUtilityProvider caf utility provider
	 *
	 * @return conductor segment modifier
	 */
	@Nullable private IInsertConductorSegmentModifier getConductorSegmentModifier(@NotNull ISchemDiagram diagram,
			@NotNull ICAFUtilityProvider cafUtilityProvider)
	{
		@Nullable ICapletController controller = cafUtilityProvider.getController(diagram);

		InsertConductorSegmentModifier conductorSegmentModifier =
				controller != null ? new InsertConductorSegmentModifier(controller) : null;
		if (conductorSegmentModifier == null) {
			// Is this from unit test
			return null;
		}

		return conductorSegmentModifier;
	}

	private boolean showProgressBar()
	{
		if (Environment.isUnitTest()) {
			return false;
		}
		Pair<Set<? extends ILogicSegmentContainer>, Set<? extends ILogicSegment>> routables = collectRoutables();
		Set<? extends ILogicSegmentContainer> conductors = routables.getFirst();
		Set<? extends ILogicSegment> segments = routables.getSecond();

		int routablesSize = 0;

		for (ILogicSegmentContainer conductor : conductors) {
			ISchemDiagram diagram = DiagramHelper.getDiagram(conductor);
			IExtent extent = diagram.getExtent();
			routablesSize += getExtentFactor(extent, diagram.getGrid().getGridSpacing());
			if (routablesSize > CONDUCTOR_SIZE_LIMIT) {
				return true;
			}
		}

		for (ILogicSegment segment : segments) {
			ISchemDiagram diagram = DiagramHelper.getDiagram(segment);
			if (diagram != null) {
				IExtent extent = diagram.getExtent();
				routablesSize += getExtentFactor(extent, diagram.getGrid().getGridSpacing());
			}
			else {
				// this is for unit tests only
				routablesSize++;
			}
			if (routablesSize > CONDUCTOR_SIZE_LIMIT) {
				return true;
			}
		}

		return routablesSize > CONDUCTOR_SIZE_LIMIT;
	}

	private int getExtentFactor(@NotNull IExtent extent, int gridSpacing)
	{
		// CID 1112392: Cast to long to avoid overflow
		long area = (long)(extent.getWidth() / gridSpacing) * (extent.getHeight() / gridSpacing);
		return area <= FIFTY_THOUSAND ? 1 : 2;
	}

	public static void setDefaultCAFUtilityProvider(@NotNull ICAFUtilityProvider defaultCAFUtilityProvider)
	{
		m_defaultCAFUtilityProvider = defaultCAFUtilityProvider;
	}

	private static ICAFUtilityProvider getDefaultCAFUtilityProvider()
	{
		return m_defaultCAFUtilityProvider;
	}

	public static void setClosedDiagramsCAFUtilityProvider(
			@NotNull ICAFUtilityProvider closedDiagramsCAFUtilityProvider)
	{
		m_closedDiagramsCAFUtilityProvider = closedDiagramsCAFUtilityProvider;
	}

	private ICAFUtilityProvider getClosedDiagramsCAFUtilityProvider()
	{
		return m_closedDiagramsCAFUtilityProvider;
	}

	public static void 	routeConductorsIncludingShields(IProgress progress, int conductorsSize, int segmentsSize,
			Collection<IDiagramObject> obstaclesToIgnore,
			IModifiableCostStrategy costStrategy,
			Collection<ILogicSegmentContainer> processedConductors,
			ConductorRouteActionHelper.IndexGenerator idxGen,
			ISchemDiagram diagram, List<? extends ILogicSegmentContainer> diagramConds,
			ICAFUtilityProvider cafUtilityProvider,
			Set<? extends ILogicSegmentContainer> conductors,
			boolean isDefaultAutoRoutePreference)
	{
		// order conductors, split shields to route after multicore indicator is refreshed
		List<IRouteIO> orderedRoutes = EdgeOrder.orderConductors(diagramConds, diagram);
		List<IRouteIO> orderedShieldRoutes = ConductorRouteActionHelper.extractShieldRoutes(orderedRoutes);
		List<ILogicSegmentContainer> offGridCondcutors = new ArrayList<>();

		int wireSpacing = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getMinimumWireSpacingEnsured() : WIRE_SPACING;
		DiagramFlowStyle diagramFlowStyle = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getDiagramFlowStyle() : DiagramFlowStyle.DEFAULT;

		//Reorder routes to first route the internal connectivity
		List<IRouteIO> newlyOrderedRoutes = orderRoutesbasedonInternalConnectivity(orderedRoutes, diagram);

		// route all conductors except shields
		ConductorRouteActionHelper.routeConductors(newlyOrderedRoutes, progress, idxGen, conductorsSize, segmentsSize,
				obstaclesToIgnore,
				costStrategy,
				processedConductors, wireSpacing, diagramFlowStyle, offGridCondcutors, isDefaultAutoRoutePreference,
				false);

		// refresh multicore indicators of the affected conductors
		refreshMulticoreIndicators(newlyOrderedRoutes, cafUtilityProvider, diagram);
		ensureMulticoreIndicatorsAreExist(newlyOrderedRoutes, diagram);
		ensureShieldsAreConnected(orderedShieldRoutes, diagram);

		// route shields
		ListMap<ILogicSegmentContainer, ILogicSegment> origSegmentsMap = new ListMap<>();
		Set<ILogicSegment> segments = new LinkedHashSet<>();
		Set<ILogicSegmentContainer> remainingConductors = new LinkedHashSet<>();
		for(ILogicSegmentContainer conductor : conductors){
			if(!processedConductors.contains(conductor)){
				remainingConductors.add(conductor);
			}
		}
		Collection<IDiagramObject> obstaclesToIgnoreForShields =
				ConductorRouteActionHelper.collectObstaclesToIgnore(remainingConductors, segments, origSegmentsMap,
						false);

		ConductorRouteActionHelper.routeConductors(orderedShieldRoutes, progress, idxGen, conductorsSize, segmentsSize,
				obstaclesToIgnoreForShields,
				costStrategy,
				processedConductors, wireSpacing, diagramFlowStyle, offGridCondcutors, isDefaultAutoRoutePreference,
				true);
		if (!offGridCondcutors.isEmpty()) {
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
					ResourceMgr.getString(ConductorRouteActionImpl.class,
							"ConductorRouteActionImpl.IgnoredConductors.Message"));
			for (ILogicSegmentContainer conductor : offGridCondcutors) {
				if (conductor instanceof IConductor) {
					CAFUtils.getInstance().getOutputWindow()
							.sendApplicationMessage(((IConductor) conductor).getConnectivity().getName());
				}
			}
		}
	}

	@NotNull private static List<IRouteIO> orderRoutesbasedonInternalConnectivity(List<IRouteIO> orderedRoutes,
			ISchemDiagram diagram)
	{

		List<IRouteIO> routeIOs = new ArrayList<>(orderedRoutes.size());
		for (IRouteIO routeIO : orderedRoutes) {
			IRoutable routable = routeIO.getRouteInput();
			Object routableUserObject = routable.getUserObject();
			if (!(routableUserObject instanceof ILogicSegmentContainer)) {
				continue;
			}

			ILogicSegmentContainer conductor = (ILogicSegmentContainer) routable.getUserObject();
			if (conductor instanceof IConductor) {
				chs.cof.logical.cable.IConductor cableConductor = ((IConductor) conductor).getConnectivity();
				boolean isValidInternalConductor = isValidInternalConductor(cableConductor, diagram);
				if (isValidInternalConductor) {
					routeIOs.add(routeIO);
				}
			}
		}
		for (IRouteIO routeIO : orderedRoutes) {
			if(!routeIOs.contains(routeIO)) {
				routeIOs.add(routeIO);
			}
		}
		return routeIOs;
	}

	private static boolean isValidInternalConductor(chs.cof.logical.cable.IConductor cableConductor,
			ISchemDiagram diagram)
	{
		boolean isADeviceToDeviceMatedNetConductor =
				diagram != null && ConductorRouteActionHelper.isAValidDeviceToDeviceConductor(cableConductor);
		if (isADeviceToDeviceMatedNetConductor) {
			return true;
		}
		boolean shouldRouteInsideDev = ConductorRouteActionHelper.shouldRouteInsideDevice(cableConductor, diagram);
		boolean isValidJCConnection = false;
		if (!shouldRouteInsideDev) {
			isValidJCConnection = ViewsInternalConnectivityUtils
					.isAValidJCConnection(new HashMap<>(), new ArrayList<>(), cableConductor);
		}
		return (shouldRouteInsideDev || isValidJCConnection);
	}

	private static void routeCEsIncludingShields(Collection<IDiagramObject> obstaclesToIgnore,
			ISchemDiagram diagram,
			ICAFUtilityProvider cafUtilityProvider,
			List<IRouteIO> orderedRoutes,
			ShieldBodyHookupSelector defaultShieldBodyHookupSelector, ListMap<IAbstractPin, IPin> pinReps,
			Set<IUIDObject> originalSchematics, Collection<ILogicSegmentContainer> processedSignalConductors,
			boolean isDefaultAutoRoutePreference)
	{
		SchemObjectHandler schemObjectHandler = new SchemObjectHandler();
		schemObjectHandler.populateOldSchemAttributeMap(originalSchematics);
		List<IRouteIO> orderedShieldRoutes = ConductorRouteActionHelper.extractShieldRoutesFromCEs(orderedRoutes);

		Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap = new HashMap<>();
		deleteSchematics(orderedRoutes, diagram, originalSchematics, inlineConnectorToWidthMap);
		// route all conductors except shields
		routeCEs(orderedRoutes, obstaclesToIgnore, diagram, defaultShieldBodyHookupSelector, pinReps,
				inlineConnectorToWidthMap, processedSignalConductors, schemObjectHandler, isDefaultAutoRoutePreference, false);
		inlineConnectorToWidthMap.clear();
		//deleting shieldSchems before routing
		deleteSchematics(orderedShieldRoutes, diagram, originalSchematics, inlineConnectorToWidthMap);

		// refresh multicore indicators of the affected conductors
		refreshMulticoreIndicatorsForCEs(orderedRoutes, cafUtilityProvider, diagram);

		// route shields
		ListMap<ILogicSegmentContainer, ILogicSegment> origSegmentsMap = new ListMap<>();
		Collection<IDiagramObject> obstaclesToIgnoreForShields = ConductorRouteActionHelper
				.collectObstaclesToIgnore(orderedShieldRoutes, origSegmentsMap, diagram, originalSchematics);
		for (IConductor conductor : CollectionUtils.getObjects(obstaclesToIgnore, IConductor.class)) {
			obstaclesToIgnoreForShields.addAll(conductor.getSegments());
		}

		Collection<IConnectivityEdge> shieldEdges = new LinkedHashSet<IConnectivityEdge>();
		for (IRouteIO shieldRoute : orderedShieldRoutes) {
			Object userObject = shieldRoute.getRouteInput().getUserObject();
			if (userObject instanceof IConnectivityEdge) {
				shieldEdges.add((IConnectivityEdge) userObject);
			}
		}
		ShieldBodyHookupSelector shieldBodyHookupSelector =
				new ShieldBodyHookupSelector(shieldEdges, diagram, Collections.<IShieldBodyHookup>emptySet(), true,
						null);
		routeCEs(orderedShieldRoutes, obstaclesToIgnoreForShields, diagram, shieldBodyHookupSelector, pinReps,
				inlineConnectorToWidthMap, processedSignalConductors, schemObjectHandler, isDefaultAutoRoutePreference, true);
	}

	private static void deleteSchematics(List<IRouteIO> orderedRoutes,
			ISchemDiagram diagram, Set<IUIDObject> originalSchematics,
			Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap)
	{
		for (IRouteIO orderedRoute : orderedRoutes) {
			IConnectivityEdge cEdge = (IConnectivityEdge) orderedRoute.getRouteInput().getUserObject();
			List<Object> contentList = CollectionUtils.createList(cEdge.getContent());
			if (contentList.isEmpty()) {
				continue;
			}

			List<Object> orderedList = new ArrayList<Object>(contentList);
			Object last = contentList.get(contentList.size() - 1);
			if (orderedList.size() != 1 && last instanceof IShieldTermination) {
				//We are doing this to first process shield termination.
				Collections.reverse(orderedList);
			}

			for (Object next : orderedList) {
				if (next instanceof INetTermination) {
					next = ((INetTermination) next).getNet();
				}
				deleteRepsRecur(diagram, next, null, originalSchematics, inlineConnectorToWidthMap);

				if (next instanceof IShieldTermination) {
					IShieldConductor shield = ((IShieldTermination) next).getShield();
					IAbstractPin termination = ((IShieldTermination) next).getTermination();
					deleteRepsRecur(diagram, shield, termination, originalSchematics, inlineConnectorToWidthMap);
				}
				else if (next instanceof IPinObject) {
					List<IAbstractPin> pins = ((IPinObject) next).getPins();
					for (IAbstractPin pin : pins) {
						deleteRepsRecur(diagram, pin, null, originalSchematics, inlineConnectorToWidthMap);
					}
				}
			}
		}
	}

	private static void routeCEs(List<IRouteIO> orderedRoutes, Collection<IDiagramObject> obstaclesToIgnore,
			@NotNull ISchemDiagram diagram,
			ShieldBodyHookupSelector shieldBodyHookupSelector,
			ListMap<IAbstractPin, IPin> pinReps,
			Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap,
			Collection<ILogicSegmentContainer> processedSignalConductors,
			SchemObjectHandler schemObjectHandler,
			boolean isDefaultAutoRoutePreference, boolean isShieldEdge)
	{

		int wireSpacing = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getMinimumWireSpacingEnsured() : WIRE_SPACING;
		DiagramFlowStyle diagramFlowStyle = m_defaultCAFUtilityProvider.getCostStrategy() != null ?
				m_defaultCAFUtilityProvider.getCostStrategy().getDiagramFlowStyle() : DiagramFlowStyle.DEFAULT;
		OGDPreferenceContext ogdContext =
				OGDPreferenceContext.getClone(OGDLayoutStylingContextHolder.layoutStylingContext);
		ogdContext.setConnectorWidth(1);
		int minWireLength = 5;
		ogdContext.setMinimumSegmentLength(minWireLength);
		if(!(isDefaultAutoRoutePreference || isShieldEdge)) {
			IProjectPreferenceMgr preferenceMgr = ProjectHelper.getProjectPreferences(diagram.getProject());
			if (preferenceMgr != null) {
				wireSpacing = preferenceMgr.getConductorSpacing();
				minWireLength = preferenceMgr.getMinConductorLength();
				diagramFlowStyle = DiagramFlowStyle.valueOf(preferenceMgr.getDiagramFlowStyle());
			}
			ogdContext.setMinimumSegmentLength(minWireLength);
		}

		boolean hasWiringInDesign = ViewsInternalConnectivityUtils.hasWiringInDesign(diagram.getDesign());
		for (IRouteIO orderedRoute : orderedRoutes) {
			IConnectivityEdge cEdge = (IConnectivityEdge) orderedRoute.getRouteInput().getUserObject();
			chs.cof.logical.cable.IConductor conductor2 = cEdge.getConductor();
			boolean shouldRouteInsideDev =
					hasWiringInDesign && ViewsInternalConnectivityUtils.isAValidInternalConnectivityNet(conductor2);
			List<Object> contentList = CollectionUtils.createList(cEdge.getContent());
			if (contentList.isEmpty()) {
				continue;
			}

			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap =
					new LinkedHashMap<chs.cof.logical.cable.IConductor, IJoint>();
			IConnectivityVertex startVertex = cEdge.getStartVertex();
			Object first = contentList.get(0);
			IJoint joint1 =
					getJoint(diagram, startVertex, first, shieldBodyHookupSelector, cEdge, conductorToJunctionMap,
							pinReps);

			IConnectivityVertex endVertex = cEdge.getEndVertex();
			Object last = contentList.get(contentList.size() - 1);
			IJoint joint2 = getJoint(diagram, endVertex, last, shieldBodyHookupSelector, cEdge, conductorToJunctionMap,
					pinReps);

			//Blow code added and removed at the very first time, as finding two hookups/virtual vertex locations
			// is an issue which can give pleasant output. It is more visible if indicators are split.
			// Commented test LogicAutoRouteTest.testRouteSignalInlineShieldConnectedToOffGridHookups refers to this
//			if (joint1 == null && joint2 == null) {
//				Pair<IJoint, IJoint> closestJoints =
//						getClosestJoints(diagram, startVertex, endVertex, first, last, shieldBodyHookupSelector, cEdge,
//								conductorToJunctionMap);
//				joint1 = closestJoints.getFirst();
//				joint2 = closestJoints.getSecond();
//			}
			boolean isValidJCConnection = false;
			Map<IAbstractPin, chs.cof.logical.cable.IConductor> groundPinToConductorMap = new HashMap<>();
			if (!shouldRouteInsideDev) {
				List<IAbstractPin> devicePins = new ArrayList<>();
				isValidJCConnection = ViewsInternalConnectivityUtils
						.isAValidJCConnection(groundPinToConductorMap, devicePins, conductor2);
			}
			if (isValidJCConnection) {
				List<ILocation> endLocations = new ArrayList<>();
				endLocations.add(joint1);
				endLocations.add(joint2);
				ViewsInternalConnectivityUtils.createSchemConductor(groundPinToConductorMap, diagram,
						endLocations, obstaclesToIgnore, orderedRoute,m_defaultCAFUtilityProvider.getCostStrategy(),
						conductor2, null, new ArrayList<>());
			}
			else {

				int length = Math.max(ConnectivityEdgeUtilities.getLength(cEdge, false, ogdContext), minWireLength) *
						diagram.getGrid().getGridSpacing();
				List<ILocation> points;
				if (joint1 != null && joint2 != null) {
					boolean considerPreferredFirstSegmentLength = true;
					if (cEdge.getStartConductor() instanceof IShieldConductor
							&& cEdge.getStartConductor() == cEdge.getEndConductor()) {
						considerPreferredFirstSegmentLength = false;
					}
					if (isDefaultAutoRoutePreference || isShieldEdge) {
						points = BlockageUtils
								.routeConductor(diagram, joint1, joint2, considerPreferredFirstSegmentLength,
										obstaclesToIgnore, Collections.emptySet(), IRouteContext.RouteGraphSize.MINIMUM,
										orderedRoute.getPrototype(),
										wireSpacing, diagramFlowStyle, shouldRouteInsideDev, false, null, false);
					}
					else {
						boolean islengthSatisfied = ConductorRouteActionHelper
								.checkIfRequiredLengthSatisfied(joint1, joint2, diagramFlowStyle, length);
						if (islengthSatisfied) {
							points = BlockageUtils
									.routeConductor(diagram, joint1, joint2, considerPreferredFirstSegmentLength,
											obstaclesToIgnore, Collections.emptySet(),
											IRouteContext.RouteGraphSize.MINIMUM,
											orderedRoute.getPrototype(),
											wireSpacing, diagramFlowStyle, shouldRouteInsideDev, false, null, false);
						}
						else {
							IPin firstPin = joint1.getAssociations(IPin.class).iterator().next();
							Side side = ExtentHelper.getSide(firstPin.getParent(), firstPin.getAbsLocation());
							IRoutePathConstraints constraints =
									new RoutePathConstaintSatisfier(diagramFlowStyle, length,
											ogdContext.getMinimumSegmentLength(), diagram.gridSpacing(),
											side);
							constraints.setSingledEndedRoute(true);
							points = BlockageUtils.doRouteConductor(diagram, RoutableUtils.createRouteEnd(joint1),
									RoutableUtils.createRouteEnd(joint2), length, considerPreferredFirstSegmentLength,
									obstaclesToIgnore, Collections.emptySet(), false,
									IRouteContext.RouteGraphSize.MINIMUM,
									orderedRoute.getPrototype(),
									null, null, wireSpacing, diagramFlowStyle,
									constraints, false, shouldRouteInsideDev, false, null, false);
						}
					}
				}
				else if ((joint1 != null) != (joint2 != null)) {
					// @todo - Set the desired widths in OGDPreferenceContext

					IJoint joint = joint1 != null ? joint1 : joint2;
					if (isDefaultAutoRoutePreference) {
						points = BlockageUtils
								.routeConductor(diagram, joint, length, obstaclesToIgnore,
										IRouteContext.RouteGraphSize.MINIMUM,
										orderedRoute.getPrototype(),
										wireSpacing, diagramFlowStyle, shouldRouteInsideDev);
					}
					else {
						IPin firstPin = joint.getAssociations(IPin.class).iterator().next();
						Side side = ExtentHelper.getSide(firstPin.getParent(), firstPin.getAbsLocation());
						IRoutePathConstraints constraints =
								new RoutePathConstaintSatisfier(diagramFlowStyle, length,
										ogdContext.getMinimumSegmentLength(), diagram.gridSpacing(),
										side);

						points = BlockageUtils.routeConductor(diagram, joint, true, obstaclesToIgnore,
								IRouteContext.RouteGraphSize.MINIMUM, orderedRoute.getPrototype(), wireSpacing,
								constraints, shouldRouteInsideDev, false);
					}
					if (joint1 == null) {
						//We need to reverse here because, auto route path start from 'joint' always and goes to predefined
						// length. In case joint1 is null, we need to reverse the order as in the later parts of code
						// PinPositionFinder.determineWirePoints assumes points in such a way it follows the connectivity
						// edge contents
						Collections.reverse(points);
					}
				}
				else {
					if (isShieldEdge) {
						//Need to create such shield edges where one edge end(shield) is connected to hookup other
						// end is hanging i.e not connected to any pin
						points = getLocationsForZeroTerminationShieldEdges(obstaclesToIgnore, diagram, wireSpacing,
								diagramFlowStyle, orderedRoute, cEdge, shouldRouteInsideDev, conductorToJunctionMap,
								length);
						if (points.isEmpty()) {
							continue;
						}
					}
					else {
						continue;
					}
				}

			ISchematicCreationContext ctx = getSchematicCreationContext(diagram, pinReps, inlineConnectorToWidthMap, schemObjectHandler);
			ConnectivityEdgeHandler.createEdgeSchematic(points, cEdge, ctx, conductorToJunctionMap, null, false, false);
			Set<chs.cof.logical.cable.IConductor> conductors =
					ConnectivityEdgeUtilities.getContentConductors(cEdge);
			chs.cof.logical.cable.IConductor startConductor = cEdge.getStartConductor();
			if (startConductor != null) {
				conductors.add(startConductor);
			}
			chs.cof.logical.cable.IConductor endConductor = cEdge.getEndConductor();
			if (endConductor != null) {
				conductors.add(endConductor);
			}

				for (chs.cof.logical.cable.IConductor conductor : conductors) {
					for (IDiagramObject sCond : ctx.getDiagram().getRepresentations(conductor.getUID())) {
						if (sCond instanceof IConductor) {
							PortHelper.updatePortGfx((ICompoundObject) sCond, diagram.getGrid().getGridSpacing());
							processedSignalConductors.add((IConductor) sCond);
						}
					}
				}
			}
			IRouteOutput output = NoRouteOutput.NO_ROUTE_OUTPUT;
			if (contentList.size() == 1) {
				Object contentItem = contentList.get(0);
				chs.cof.logical.cable.IConductor conductor = null;
				if (contentItem instanceof chs.cof.logical.cable.IConductor) {
					conductor = (chs.cof.logical.cable.IConductor) contentItem;
				}
				else if (contentItem instanceof IShieldTermination) {
					conductor = ((IShieldTermination) contentItem).getShield();
				}

				if (conductor != null) {
					IUIDObjectCollection reps =
							diagram.getRepresentationsCollection(conductor.getUID());
					Object rep = reps.iterator().hasNext() ? reps.iterator().next() : null;
					if (rep instanceof IConductor) {
						output = new ConductorRouteOutput((IConductor) rep);
					}
				}
			}
			orderedRoute.setRouteOutput(output);
		}
	}

	@NotNull private static List<ILocation> getLocationsForZeroTerminationShieldEdges(
			Collection<IDiagramObject> obstaclesToIgnore, ISchemDiagram diagram,
			int wireSpacing, DiagramFlowStyle diagramFlowStyle, IRouteIO orderedRoute, IConnectivityEdge cEdge,
			boolean shouldRouteInsideDev, Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap,
			int length)
	{
		IShieldConductor shieldConductor = null;
		if (cEdge.getStartConductor() instanceof IShieldConductor) {
			shieldConductor = (IShieldConductor) cEdge.getStartConductor();
		}
		else if (cEdge.getEndConductor() instanceof IShieldConductor) {
			shieldConductor = (IShieldConductor) cEdge.getEndConductor();
		}
		List<ILocation> points = new ArrayList<>();
		if (shieldConductor != null) {
			IShieldBodyHookup hookup = getShieldHookUpToUse(diagram, shieldConductor);
			if (hookup != null) {
				IJoint node = DiagramGenerationUtilities.getNode(hookup);
				conductorToJunctionMap.put(shieldConductor, node);
				points = BlockageUtils
						.routeConductor(diagram, node, length, obstaclesToIgnore,
								IRouteContext.RouteGraphSize.MINIMUM,
								orderedRoute.getPrototype(),
								wireSpacing, diagramFlowStyle, shouldRouteInsideDev);
				Collections.reverse(points);
			}
		}
		return points;
	}

	@Nullable
	private static IShieldBodyHookup getShieldHookUpToUse(ISchemDiagram diagram, IShieldConductor shieldConductor)
	{
		IMulticore multicore = shieldConductor.getMulticore();
		if (multicore != null && multicore.getShieldBody() != null) {
			chs.cof.logical.cable.IShieldBody shieldBody = multicore.getShieldBody();
			IDiagramObjectIterator shieldBodyRepresentations =
					diagram.getRepresentations(shieldBody.getUID());
			List<IShieldBodyHookup> shieldBodyHookups = new ArrayList<>();
			for (IDiagramObject shieldBodyRep : shieldBodyRepresentations) {
				if (shieldBodyRep instanceof IShieldBody) {
					shieldBodyHookups.addAll(
							((IShieldBody) shieldBodyRep).getShieldBodyHookups());
				}
			}
			if (!shieldBodyHookups.isEmpty()) {
				IShieldBodyHookup hookup = identifyShieldHookupToUseForZeroTerminationShield(shieldBodyHookups);
				return hookup;
			}
		}
		return null;
	}

	@Nullable private static IShieldBodyHookup identifyShieldHookupToUseForZeroTerminationShield(
			List<IShieldBodyHookup> shieldBodyHookups)
	{
		List<IShieldBodyHookup> usedHookups = shieldBodyHookups.stream()
				.filter(hookup -> !hookup.getShieldConductors().isEmpty())
				.collect(Collectors.toList());
		List<IShieldBodyHookup> hookupsToUse = new ArrayList<>();
		hookupsToUse.addAll(shieldBodyHookups);
		hookupsToUse.removeAll(usedHookups);
		IShieldBodyHookup hookUp = hookupsToUse.stream()
				.sorted(CompareUIDComparator.COMPARATOR)
				.findFirst()
				.orElse(null);
		if (hookUp != null) {
			return hookUp;
		}
		return shieldBodyHookups.stream()
				.sorted(CompareUIDComparator.COMPARATOR)
				.findFirst()
				.orElse(null);
	}

	@Nullable
	private static IJoint getJoint(ISchemDiagram diagram, IConnectivityVertex vertex, Object adjacentConductor,
			ShieldBodyHookupSelector shieldBodyHookupSelector, IConnectivityEdge cEdge,
			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap,
			ListMap<IAbstractPin, IPin> pinReps)
	{
		IAbstractPin pin1 = getPin(vertex, adjacentConductor);
		if (pin1 != null) {
			IPin rep1 = null;
			List<IPin> recordedEndSchemPins = pinReps.pull(pin1);
			@Nullable List<IPin> schematicPinsFromConductor = getSchematicPinFromConductorObject(adjacentConductor);
			if (recordedEndSchemPins != null) {
				for (IPin pin : schematicPinsFromConductor) {
					if (recordedEndSchemPins.contains(pin)) {
						rep1 = pin;
					}
				}
			}
			if (rep1 == null) {
				IDiagramObjectIterator reps1 = diagram.getRepresentations(pin1.getUID());
				rep1 = (IPin) (reps1.hasNext() ? reps1.next() : null);
			}
			return rep1 != null ? DiagramGenerationUtilities.getNode(rep1) : null;
		}
		else {
			if (vertex.isVirtual() && vertex instanceof ConnectivityVirtualVertex) {
				IShieldBodyHookup hookup = shieldBodyHookupSelector.getShieldBodyHookup(cEdge);
				if (hookup != null) {
					IJoint node = DiagramGenerationUtilities.getNode(hookup);
					if (adjacentConductor instanceof IShieldTermination) {
						IShieldConductor shield = ((IShieldTermination) adjacentConductor).getShield();
						if (shield != null) {
							conductorToJunctionMap.put(shield, node);
						}
					}
					return node;
				}
				else {
					return null;
				}
			}
			return null;
		}
	}

//	private static Pair<IJoint, IJoint> getClosestJoints(ISchemDiagram diagram, IConnectivityVertex startVertex,
//			IConnectivityVertex endVertex,
//			Object first, Object last, ShieldBodyHookupSelector shieldBodyHookupSelector, IConnectivityEdge cEdge,
//			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap)
//	{
//		Pair<IJoint, IJoint> pair = new Pair<IJoint, IJoint>(null, null);
//		//if it is a cEdge like shield-splice-wire-inline-wire-splice-shield
//		IJoint joint1 = getClosestJoint(diagram, startVertex, first, shieldBodyHookupSelector, cEdge,
//				conductorToJunctionMap, null, null);
//
//		IJoint joint2 = getClosestJoint(diagram, endVertex, last, shieldBodyHookupSelector, cEdge,
//				conductorToJunctionMap, joint1, null);
//
//		//In case of cEdge being from sheild termination to shield termination via inline pin pair,
//		// below step necessary
//		IJoint nearestJoint =
//				getClosestJoint(diagram, startVertex, first, shieldBodyHookupSelector, cEdge,
//						conductorToJunctionMap, joint2, joint1);
//		if (joint1 == null || (nearestJoint != null && !joint1.equals(nearestJoint))) {
//			joint1 = nearestJoint;
//		}
//		return new Pair<IJoint, IJoint>(joint1, joint2);
//	}
//
//	@Nullable
//	private static IJoint getClosestJoint(ISchemDiagram diagram, IConnectivityVertex vertex, Object adjacentConductor,
//			ShieldBodyHookupSelector shieldBodyHookupSelector, IConnectivityEdge cEdge,
//			Map<chs.cof.logical.cable.IConductor, IJoint> conductorToJunctionMap, @Nullable IJoint fromJoint,
//			@Nullable IJoint thisJoint)
//	{
//		IAbstractPin pin1 = getPin(vertex, adjacentConductor);
//		if (pin1 == null) {
//			if (vertex.isVirtual() && vertex instanceof ConnectivityVirtualVertex) {
//				IShieldBodyHookup hookup = shieldBodyHookupSelector.getShieldBodyHookup(cEdge);
//				if (hookup == null) {
//					if (adjacentConductor instanceof IShieldTermination) {
//						IShieldConductor shield = ((IShieldTermination) adjacentConductor).getShield();
//						if (shield != null) {
//							IShieldBodyHookup nearestHookup =
//									ShieldBodyHookupSelector.findNearestHookup(diagram, shield, fromJoint);
//							if (nearestHookup != null) {
//								IJoint node = DiagramGenerationUtilities.getNode(nearestHookup);
//								if (thisJoint == null || !node.equals(thisJoint)) {
//									conductorToJunctionMap.put(shield, node);
//									return node;
//								}
//								else {
//									return thisJoint;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return null;
//	}

	@Nullable private static IAbstractPin getPin(IConnectivityVertex endVertex, Object adjacentConductor)
	{
		IAbstractPin contentPin = endVertex.getContentPin();
		if (contentPin != null) {
			return contentPin;
		}
		Object conductor = adjacentConductor;
		if (conductor instanceof IShieldTermination) {
			conductor = ((IShieldTermination) conductor).getShield();
		}
		if (conductor instanceof chs.cof.logical.cable.IConductor) {
			Set<IAbstractPin> condPins =
					new HashSet<IAbstractPin>(((chs.cof.logical.cable.IConductor) conductor).getAllPins());
			IOrderedPinPair pinPair = endVertex.getContentOrderedPinPair();
			if (pinPair == null) {
				return null;
			}
			List<IAbstractPin> vertexPins = pinPair.getPins();
			condPins.retainAll(vertexPins);
			if (condPins.size() == 1) {
				return condPins.iterator().next();
			}
		}
		return null;
	}

	private static Pair<List<IRouteIO>, ShieldBodyHookupSelector> orderConductorsAsCEs(ISchemDiagram diagram,
			List<IConductor> conductors,
			ListMap<IAbstractPin, IPin> pinReps, Set<IUIDObject> originalSchematics,
			Set<IConductor> remainingConductors,
			Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges,
			boolean enableFullSignalRouting, List<List<IConnectivityEdge>> connectivityEdgeList)
	{
		List<IConnectivityEdge> connectivityEdges = new ListSet<IConnectivityEdge>();
		Set<IConnectivityEdge> shieldEdges = new LinkedHashSet<IConnectivityEdge>();
		for (IConductor conductor : conductors) {
			IEdgeFromSchematics edgeFromSchematics =
					new ConnectivityEdgeFromSchematics(conductor, ignoredEdges, diagram, enableFullSignalRouting);
			if (edgeFromSchematics.canRouteEdge()) {
				List<IConnectivityEdge> edges = Arrays.asList(((ConnectivityEdgeFromSchematics) edgeFromSchematics).resultSet.getEdges());
				if(enableFullSignalRouting && (doEdgesContainsCenterSplice(edges) || isAValidEarthJCConnection(diagram, conductor))){
					remainingConductors.add(conductor);
				}
				else {
					List<IConnectivityEdge> newEdgesAdded = new ArrayList<>();
					for (IConnectivityEdge connectivityEdge : edges) {
						assert connectivityEdge != null;
						originalSchematics.addAll(edgeFromSchematics.getIntermediateSchematics());
						// @todo - filter out the duplicate connectivity edges
						fixOrder(connectivityEdge);
						if (!connectivityEdges.contains(connectivityEdge)) {
							newEdgesAdded.add(connectivityEdge);
						}
						connectivityEdges.add(connectivityEdge);
						edgeResultSet.add(connectivityEdge);
						IConnectivityVertex startVertex = connectivityEdge.getStartVertex();
						IConnectivityVertex endVertex = connectivityEdge.getEndVertex();

						List<Object> contentList = CollectionUtils.createList(connectivityEdge.getContent());
						Object first = !contentList.isEmpty() ? contentList.get(0) : null;
						Object last = !contentList.isEmpty() ? contentList.get(contentList.size() - 1) : null;

						addToPinRepsMap(diagram, pinReps, startVertex, first, edgeFromSchematics);
						addToPinRepsMap(diagram, pinReps, endVertex, last, edgeFromSchematics);

						addToShieldEdges(shieldEdges, connectivityEdge, startVertex);
						addToShieldEdges(shieldEdges, connectivityEdge, endVertex);
					}
					if (enableFullSignalRouting && !newEdgesAdded.isEmpty()) {
						connectivityEdgeList.add(newEdgesAdded);
					}
				}
			}
			else {
				remainingConductors.add(conductor);
			}
		}

		ShieldBodyHookupSelector shieldBodyHookupSelector =
				new ShieldBodyHookupSelector(shieldEdges, diagram, Collections.<IShieldBodyHookup>emptySet(), true,
						null);
		List<IRouteIO> routeIOs = EdgeOrder.orderEdges(connectivityEdges, pinReps, shieldBodyHookupSelector, diagram);
		return new Pair<List<IRouteIO>, ShieldBodyHookupSelector>(routeIOs, shieldBodyHookupSelector);
	}

	private static boolean isAValidEarthJCConnection(ISchemDiagram diagram, IConductor conductor)
	{
		chs.cof.logical.cable.IConductor cableConductor = conductor.getConnectivity();
		boolean hasWiringInDesign = ViewsInternalConnectivityUtils.hasWiringInDesign(diagram.getDesign());
		boolean shouldRouteInsideDev = (hasWiringInDesign && ViewsInternalConnectivityUtils.isAValidInternalConnectivityNet(cableConductor));
		Map<IAbstractPin, chs.cof.logical.cable.IConductor> groundPinToConductorMap = new HashMap<>();
		List<IAbstractPin> devicePins = new ArrayList<>();
		boolean	isValidJCConnection = ViewsInternalConnectivityUtils
					.isAValidJCConnection(groundPinToConductorMap, devicePins, cableConductor);
		return shouldRouteInsideDev || isValidJCConnection ;
	}

	private static boolean doEdgesContainsCenterSplice(List<IConnectivityEdge> edges)
	{
		for(IConnectivityEdge edge : edges){
			IAbstractPin pin1 = edge.getPin1();
			if (isCenterSplicedPin(pin1)) {
				return true;
			}
			IAbstractPin pin2 = edge.getPin2();
			if(isCenterSplicedPin(pin2)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCenterSplicedPin(@Nullable IAbstractPin pin)
	{
		if(pin != null) {
			chs.cof.logical.cable.IPinList owner = pin.getOwner();
			if(owner instanceof ISplice){
				if(((ISplice) owner).getCenterStrippedWires().hasNext()){
					return true;
				}
			}
		}
		return false;
	}

	private static void fixOrder(@NotNull IConnectivityEdge connectivityEdge)
	{
		IAbstractPin pin1 = connectivityEdge.getPin1();
		IAbstractPin pin2 = connectivityEdge.getPin2();
		List<Object> content = CollectionUtils.createList(connectivityEdge.getContent());

		if (pin1 == null && pin2 != null && content.size() == 1 && content.get(0) instanceof INetTermination) {
			connectivityEdge.reverse();
		}
	}

	private static void addToShieldEdges(Set<IConnectivityEdge> shieldEdges, IConnectivityEdge connectivityEdge,
			IConnectivityVertex vertex)
	{
		if (vertex.isVirtual() && vertex instanceof ConnectivityVirtualVertex) {
			Object segment = ((ConnectivityVirtualVertex) vertex).getSegment();
			if (segment instanceof IShieldTermination) {
				shieldEdges.add(connectivityEdge);
			}
		}
	}

	private static void addToPinRepsMap(ISchemDiagram diagram, ListMap<IAbstractPin, IPin> pinReps,
			IConnectivityVertex vertex, Object adjacentConductor,
			IEdgeFromSchematics edgeFromSchematics)
	{
		IAbstractPin contentPin = getPin(vertex, adjacentConductor);
		if (contentPin != null) {
			@Nullable IPin schemPin = edgeFromSchematics.getSchemPin(contentPin);
			if (schemPin != null) {
				if (!pinReps.get(contentPin).contains(schemPin)) {
					pinReps.add(contentPin, schemPin);
				}
				m_schemPinFromConductor.add(adjacentConductor, schemPin);
			}
		}
	}

	@NotNull private static List<IPin> getSchematicPinFromConductorObject(Object conductor)
	{
		return m_schemPinFromConductor.get(conductor);
	}
	private static void deleteRepsRecur(ISchemDiagram diagram, Object next, @Nullable IAbstractPin termination,
			Set<IUIDObject> originalSchematics,
			@NotNull Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap)
	{
		if (next instanceof IUIDProvider) {
			IDiagramObjectIterator representations = diagram.getRepresentations(((IUIDProvider) next).getUID());
			for (IDiagramObject representation : representations) {
				if (canDeleteIfShieldConductorSchem(next, representation, termination)) {
					IDiagramObject parent = deleteSchem(representation, originalSchematics);
					if (parent instanceof IConnectivityRef) {
						ILogicObject connectivity = ((IConnectivityRef) parent).getConnectivity();
						if (connectivity instanceof ISplice || connectivity instanceof IConnector) {
							if (parent instanceof IPinList) {
								storeInlineConnectorsWidth(inlineConnectorToWidthMap, (IPinList) parent, connectivity);
							}
							deleteSchem(parent, originalSchematics);
						}
					}
				}
			}
		}
	}

	private static boolean canDeleteIfShieldConductorSchem(Object next, IDiagramObject representation,
			@Nullable IAbstractPin termination)
	{
		if (next instanceof IShieldConductor && termination != null) {
			if (representation instanceof IConductor) {
				IUIDObjectCollection<IPin> pins = ((IConductor) representation).getPins();
				Collection<IAbstractPin> connectivity = new HashSet<>();
				for (IPin pin : pins) {
					connectivity.add(pin.getConnectivity());
				}
				return connectivity.contains(termination);
			}
		}
		return true;
	}

	private static void storeInlineConnectorsWidth(
			@NotNull Map<IGenericInlineConnector, Integer> inlineConnectorToWidthMap,
			IPinList parent, ILogicObject connectivity)
	{
		if (connectivity instanceof IGenericInlineConnector) {
			if (inlineConnectorToWidthMap.get(connectivity) != null) {
				// if there are multiple representations keep schem with max width
				// this is to make sure for consistency
				if (inlineConnectorToWidthMap.get(connectivity) <
						parent.getReferenceWidth()) {
					inlineConnectorToWidthMap.put((IGenericInlineConnector) connectivity,
							parent.getReferenceWidth());
				}
			}
			else {
				inlineConnectorToWidthMap.put((IGenericInlineConnector) connectivity,
						parent.getReferenceWidth());
			}
		}
	}

	@Nullable
	private static IDiagramObject deleteSchem(IDiagramObject representation, Set<IUIDObject> originalSchematics)
	{
		if (!originalSchematics.contains(representation)) {
			return null;
		}
		IDiagramObject parent = representation.getParent();
		if (parent instanceof ICompoundObject) {
			((ICompoundObject) parent).removeObject(representation);
		}
		//dts0101229467 - Do some extra work for shield conductors re: daisy chains.
		// If there are no chains on either hookup on this shield body, nothing to do, the normal delete will
		// sort everything.
		if (isShieldConductorWithDiasyChain(representation)) {
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			cdh.addDeletionObject(representation);
		}
		else {
			representation.delete();
		}

		originalSchematics.remove(representation);
		return parent;
	}

	private static boolean isShieldConductorWithDiasyChain(IDiagramObject representation)
	{
		if (representation instanceof IConductor &&
				((IConnectivityRef) representation).getConnectivity() instanceof IShieldConductor) {
			IShieldBodyHookup hook = ((IConductor) representation).getHookup();
			if (hook != null) {
				return !(hook.getShieldChains().isEmpty() && hook.getOtherHookup() != null &&
						hook.getOtherHookup().getShieldChains().isEmpty());
			}
		}
		return false;
	}

	private static boolean collectOffGridConnectivityEdges(Set<IUIDObject> objects, IConnectivityEdge edge,
			Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges,
			ISchemDiagram diagram)
	{

		int gridSpacing = diagram.getGrid().getGridSpacing();

		for (IUIDObject object : objects) {
			if (object instanceof IPin) {
				IPin pin = (IPin) object;
				ILocation pinLocation = pin.getAbsLocation();
				if (pinLocation.getX() % gridSpacing != 0 || pinLocation.getY() % gridSpacing != 0) {
					//add to the set
					addOffGridEdge(edge, ignoredEdges);
					return false;
				}
			}
		}
		return true;
	}

	private static void addOffGridEdge(IConnectivityEdge edge,
			Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges)
	{
		Set<chs.cof.logical.cable.IConductor> conductors = new LinkedHashSet<>();
		List<Object> contentList = CollectionUtils.createList(edge.getContent());
		if (contentList.isEmpty()) {
			return;
		}
		for (Object object : contentList) {
			if (object instanceof chs.cof.logical.cable.IConductor) {
				chs.cof.logical.cable.IConductor conductor = (chs.cof.logical.cable.IConductor) object;
				conductors.add(conductor);
			}
		}
		ignoredEdges.put(edge, conductors);
	}


	private static ISchematicCreationContext getSchematicCreationContext(final ISchemDiagram diagram,
			final ListMap<IAbstractPin, IPin> pinReps,
			final Map<IGenericInlineConnector, Integer> inlineConnectorsWidthMap,
			SchemObjectHandler schemObjectHandler)
	{
		return new ISchematicCreationContext()
		{
			@Override public ISchemDiagram getDiagram()
			{
				return diagram;
			}

			@Override public ListMap<IAbstractPin, IPin> getPinReps()
			{
				return pinReps;
			}

			@Override public IPreferenceMgr getPreferenceMgr()
			{
				return diagram.getProject().getPreferences();
			}

			@Override public IPreferenceSet getPreferenceSet()
			{
				return diagram.getPreferenceSet();
			}

			@Override public ILogicDesign getDesign()
			{
				return diagram.getDesign();
			}

			@Override public IConductor getSchemConductor(chs.cof.logical.cable.IConductor conductor)
			{
				return null;
			}

			@Override public Set<IConductor> getSchemConductors(chs.cof.logical.cable.IConductor conductor)
			{
				return new HashSet<IConductor>();
			}

			@Override
			public void addConductorSchem(chs.cof.logical.cable.IConductor conductor, IConductor schemConductor)
			{
			}

			@Override
			public void removeConductorSchem(chs.cof.logical.cable.IConductor conductor, IConductor schemConductor)
			{
			}

			@NotNull @Override public OGDPreferenceContext getOGDPreferenceContext()
			{
				return new OGDPreferenceContext(true);
			}

			@Override public Map<IGenericInlineConnector, Integer> getInlineConnectorsWidthMap()
			{
				return inlineConnectorsWidthMap;
			}

			public void notify(IDiagramObject diagramObject)
			{
				schemObjectHandler.updateSchemObject(diagramObject);
			}
		};
	}

	private static void refreshMulticoreIndicatorsForCEs(@NotNull List<IRouteIO> orderedRoutes,
			@NotNull ICAFUtilityProvider cafUtilityProvider, @NotNull ISchemDiagram diagram)
	{
		List<IUID> conductorUIDs = new ArrayList<IUID>(orderedRoutes.size());
		for (IRouteIO routeIO : orderedRoutes) {
			Object userObject = routeIO.getRouteInput().getUserObject();
			if (userObject instanceof IConnectivityEdge) {
				IConnectivityEdge connectivityEdge = (IConnectivityEdge) userObject;
				Iterator<?> content = connectivityEdge.getContent();
				while (content.hasNext()) {
					Object next = content.next();
					if (next instanceof chs.cof.logical.cable.IConductor) {
						IUIDObjectCollection reps =
								diagram.getRepresentationsCollection(((IUIDProvider) next).getUID());
						for (Object rep : reps) {
							if (rep instanceof IUIDProvider && rep instanceof IConductor) {
								conductorUIDs.add(((IUIDProvider) rep).getUID());
							}
						}
					}
				}
			}
		}

		doRefreshIndicatorsOnDiagram(cafUtilityProvider, diagram, conductorUIDs);

		Set<IMulticore> multicores = new LinkedHashSet<>();
		for (IUID condUID : conductorUIDs) {
			IConductor conductor = IConductor.class.cast(condUID.getObject());
			assert conductor != null;
			if (conductor.getConnectivity().getMulticore() != null) {
				IMulticore multicore = conductor.getConnectivity().getMulticore();
				if (!diagram.getRepresentations(multicore.getShieldBody().getUID()).hasNext()) {
					multicores.add(multicore);
				}
			}
		}
		multicores = multicores.stream().map(mc -> mc.getRootMulticore())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		MulticoreUtils.generateMulticores(multicores, diagram, new ISchematicGenerationParams()
		{
			@Override public int getIndicatorOffset()
			{
				return 1;
			}

			@NotNull @Override public IProjectPreferenceMgr getProjectPreferenceMgr()
			{
				return diagram.getDesign().getProject().getPreferences();
			}

			@Nullable @Override public IPreferenceSet getPreferenceSet()
			{
				return diagram.getPreferenceSet();
			}
		});
	}

	private static void refreshMulticoreIndicators(@NotNull List<IRouteIO> orderedRoutes,
			@NotNull ICAFUtilityProvider cafUtilityProvider, @NotNull ISchemDiagram diagram)
	{
		List<IUID> conductorUIDs = new ArrayList<IUID>(orderedRoutes.size());
		for (IRouteIO routeIO : orderedRoutes) {
			Object userObject = routeIO.getRouteInput().getUserObject();
			if (userObject instanceof IConductor) {
				IConductor conductor = (IConductor) userObject;
				conductorUIDs.add(conductor.getUID());
			}
		}

		doRefreshIndicatorsOnDiagram(cafUtilityProvider, diagram, conductorUIDs);
	}

	private static void doRefreshIndicatorsOnDiagram(@NotNull ICAFUtilityProvider cafUtilityProvider,
			@NotNull ISchemDiagram diagram, List<IUID> conductorUIDs)
	{
		IndicatorRefresher indicatorRefresher = IndicatorRefresher.getIndicatorRefresher(diagram);
		@Nullable ICapletModel model = cafUtilityProvider.getModel(diagram);

		if (model == null) {
			// Is this from unit test
			indicatorRefresher.refreshIndicators(conductorUIDs, false);
			return;
		}

		indicatorRefresher.refreshIndicators(new ModelChangeEvent(model, conductorUIDs), true);
	}

	private static void ensureMulticoreIndicatorsAreExist(@NotNull List<IRouteIO> orderedRoutes,
			@NotNull ISchemDiagram diagram)
	{
		Generator gen = Generator.getGenerator();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		orderedRoutes.stream()
				.map(rIO -> rIO.getRouteInput().getUserObject())
				.filter(IConductor.class::isInstance)
				.map(uo -> ((IConductor) uo).getConnectivity())
				.filter(Objects::nonNull)
				.map(c -> c.getMulticore())
				.filter(Objects::nonNull)
				.distinct()
				.filter(mc -> diagram.getRepresentationsCollection(mc.getShieldBody().getUID()).isEmpty())
				.map(mc -> mc.getRootMulticore())
				.distinct()
				.forEach(rmc -> Placement.placeIndicators(gen, diagram, rmc, rmc.getShieldBody(), gp, false));
	}

	private static void ensureShieldsAreConnected(@NotNull List<IRouteIO> orderedShieldRoutes,
			@NotNull ISchemDiagram diagram)
	{
		orderedShieldRoutes.stream()
				.map(osr -> osr.getRouteInput())
				.filter(ri -> ri.getUserObject() instanceof IConductor)
				.filter(ri -> ((IConductor) ri.getUserObject()).getHookup() == null)
				.filter(ri -> ((IConductor) ri.getUserObject()).getConnectivity().getMulticore() != null)
				.forEach(ri -> findHookupAndConnect(ri, diagram));
	}

	private static void findHookupAndConnect(@NotNull IRoutable routable, @NotNull ISchemDiagram diagram)
	{
		IShieldBodyHookup nearestHookup = findHookup(routable, diagram);
		if (nearestHookup != null) {
			connectToHookup(nearestHookup, routable);
		}
	}

	@Nullable private static IShieldBodyHookup findHookup(@NotNull IRoutable routable, @NotNull ISchemDiagram diagram)
	{
		IConductor shieldSchem = (IConductor) routable.getUserObject();
		IMulticore multicore = shieldSchem.getConnectivity().getMulticore();
		assert multicore != null;

		IUIDObjectCollection<IPin> pins = shieldSchem.getPins();
		@Nullable IPin pin = !pins.isEmpty() ? pins.iterator().next() : null;
		@Nullable ILocation pinLocation = pin != null ? pin.getAbsLocation() : null;

		double minDistance = Double.MAX_VALUE;
		int minShieldConnectionsCount = Integer.MAX_VALUE;
		IShieldBodyHookup nearestHookup = null;

		for (Object schemShieldBody : diagram.getRepresentationsCollection(multicore.getShieldBody().getUID())) {
			if (schemShieldBody instanceof IShieldBody) {
				for (IShieldBodyHookup shieldBodyHookup : ((IShieldBody) schemShieldBody).getShieldBodyHookups()) {
					ILocation hookupLocation = shieldBodyHookup.getAbsLocation();
					double distance = pinLocation != null ? pinLocation.distance(hookupLocation) : Double.MAX_VALUE;
					int shieldConnectionsCount = shieldBodyHookup.getShieldConductors().size();
					if (shieldConnectionsCount < minShieldConnectionsCount ||
							(shieldConnectionsCount == minShieldConnectionsCount && distance < minDistance)) {
						minShieldConnectionsCount = shieldConnectionsCount;
						minDistance = distance;
						nearestHookup = shieldBodyHookup;
					}
				}
			}
		}

		return nearestHookup;
	}

	private static void connectToHookup(@NotNull IShieldBodyHookup nearestHookup, @NotNull IRoutable routable)
	{

		@Nullable IRouteEnd selectedRouteEnd = identifyRouteEndToBeUpdated(routable);
		if (selectedRouteEnd instanceof IModifiableJointRouteEnd) {
			IJoint nearestHookupJoint = DiagramGenerationUtilities.getNode(nearestHookup);
			((IModifiableJointRouteEnd) selectedRouteEnd).setUserObject(nearestHookupJoint);

			connectSchemConductorToHookup(nearestHookupJoint, routable);
		}
	}

	private static void connectSchemConductorToHookup(@NotNull IJoint nearestHookupJoint, @NotNull IRoutable routable)
	{
		IConductor shieldSchem = (IConductor) routable.getUserObject();
		List<IConnected> segments = new ArrayList<IConnected>(shieldSchem.getSegments());
		IConnected firstSegment = null;
		IConnected lastSegment = null;
		IJoint startJoint = null;
		IJoint lastJoint = null;

		if (segments.size() == 1) {
			firstSegment = segments.get(0);
			lastSegment = firstSegment;
			startJoint = firstSegment.getStartJoint();
			lastJoint = firstSegment.getEndJoint();
		}

		if (segments.size() > 1) {
			firstSegment = segments.get(0);
			lastSegment = segments.get(segments.size() - 1);

			IConnected secondSegment = segments.get(1);
			startJoint = getFirstSegmentCandidateJoint(firstSegment, secondSegment);

			IConnected lastButOneSegment = segments.get(segments.size() - 2);
			lastJoint = getFirstSegmentCandidateJoint(lastSegment, lastButOneSegment);

		}

		if (firstSegment != null) {
			IUIDObjectCollection<IPin> pins = shieldSchem.getPins();
			if (!connectToSegment(startJoint, nearestHookupJoint, firstSegment, pins)) {
				connectToSegment(lastJoint, nearestHookupJoint, lastSegment, pins);
			}
		}
	}

	@Nullable private static IRouteEnd identifyRouteEndToBeUpdated(@NotNull IRoutable routable)
	{
		List<IRouteEnd> routeEnds =
				routable.getRouteEnds().stream().filter(rE -> rE instanceof IJointRouteEnd)
						.collect(Collectors.toList());

		@Nullable IRouteEnd selectedRouteEnd =
				routeEnds.size() == 1 ? routeEnds.iterator().next() : null;

		selectedRouteEnd = (selectedRouteEnd != null) ? selectedRouteEnd :
				routeEnds.stream()
						.filter(rE -> rE.getAssociations(IPin.class).isEmpty())
						.findFirst()
						.orElse(null);

		IConductor shieldSchem = (IConductor) routable.getUserObject();
		IUIDObjectCollection<IPin> pins = shieldSchem.getPins();
		selectedRouteEnd = (selectedRouteEnd != null) ? selectedRouteEnd :
				routeEnds.stream()
						.filter(rE -> rE.getAssociations(IPin.class).stream()
								.noneMatch(p -> pins.contains(p)))
						.findFirst()
						.orElse(null);

		selectedRouteEnd = (selectedRouteEnd != null) ? selectedRouteEnd :
				routeEnds.stream().findFirst().orElse(null);

		return selectedRouteEnd;
	}

	@Nullable private static IJoint getFirstSegmentCandidateJoint(IConnected firstSegment, IConnected secondSegment)
	{
		IJoint start = firstSegment.getStartJoint();
		boolean startConnected = (start == secondSegment.getStartJoint() || start == secondSegment.getEndJoint());
		IJoint end = firstSegment.getEndJoint();
		return !startConnected? start : end;
	}

	private static boolean connectToSegment(@Nullable IJoint fromJoint, @NotNull IJoint toJoint,
			@NotNull IConnected segment, @NotNull IUIDObjectCollection<IPin> pins)
	{
		if (fromJoint != null) {
			Set<IPin> startPins = fromJoint.getAssociations(IPin.class);
			if (Collections.disjoint(pins, startPins)) {
				if (segment.getStartJoint() == fromJoint) {
					((ILogicSegment) segment).setStartNode(toJoint);
				}
				else if (segment.getEndJoint() == fromJoint) {
					((ILogicSegment) segment).setEndNode(toJoint);
				}
				return true;
			}
		}
		return false;
	}

	public static class ConductorRouteActionRunnable implements Runnable
	{

		private ProgressGroup m_progress = null;
		private ConductorRouteActionImpl m_conductorRouteAction;

		ConductorRouteActionRunnable(ConductorRouteActionImpl conductorRouteAction)
		{
			m_conductorRouteAction = conductorRouteAction;
			m_progress = new ProgressGroup("");
			m_progress.forceIndeterminate(true);
			m_progress.setRange(0);
		}

		public IProgress getProgress()
		{
			return m_progress;
		}

		public void run()
		{
			m_progress.start();
			m_progress.increment();

			try {
				m_conductorRouteAction.doProcessAction(m_progress, getDefaultCAFUtilityProvider());
			}
			catch (ProgressCancelledException e) {
				// do nothing
			}
			catch (Exception e) {
				Environment.getExceptionDisplay().displayException(e, false);
			}
			finally {
				m_progress.complete();
			}
		}
	}

	private abstract static class AbstractCAFUtilityProvider implements ICAFUtilityProvider
	{

		@Nullable private IModifiableCostStrategy m_costStrategy = null;

		@Nullable public Container getContainer()
		{
			ICapletView activeCapletView = CAFUtils.getInstance().getActiveCapletView();
			ICapletWindow capletWindow = activeCapletView != null ? activeCapletView.getWindow() : null;
			return capletWindow != null ? capletWindow.getContainer() : null;
		}

		@NotNull public Frame getDialogFrame()
		{
			return CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		}

		@Nullable public IModifiableCostStrategy getCostStrategy()
		{
			if (m_costStrategy == null) {
				m_costStrategy = new ConductorRouteActionCostStrategy();
			}

			return m_costStrategy;
		}
	}

	private static class DefaultCAFUtilityProvider extends AbstractCAFUtilityProvider
	{

		private DefaultCAFUtilityProvider()
		{
		}

		@NotNull private static ICapletController getActiveCapletController()
		{
			return CAFUtils.getInstance().getActiveCapletController();
		}

		@Nullable public ICapletController getController(@NotNull ISchemDiagram diagram)
		{
			ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
			if (cw == null) {
				assert false : "caplet view can not be null for diagram " + diagram.getName() + " : " +
						diagram.getDesignContainer().getFullName();
				return null;
			}
			return cw.getController();
		}

		@Nullable public ICapletModel getModel(@NotNull ISchemDiagram diagram)
		{
			ICapletController controller = getController(diagram);
			if (controller != null) {
				return controller.getCapletModel();
			}
			return null;
		}

		@NotNull public ICapletModel getActiveCapletModel()
		{
			return getActiveCapletController().getCapletModel();
		}

		@NotNull public SelectSet getPreSelections()
		{
			return getActiveCapletController().getSelectMgr().getPreSelections();
		}
	}

	private static class ClosedDiagramsCAFUtilityProvider extends AbstractCAFUtilityProvider
	{

		private ClosedDiagramsCAFUtilityProvider()
		{
		}

		@Nullable private static ICapletController getActiveCapletController()
		{
			return CAFUtils.getInstance().getActiveCapletController();
		}

		@Nullable public ICapletController getController(@NotNull ISchemDiagram diagram)
		{
			ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
			return cw != null ? cw.getController() : null;
		}

		@Nullable public ICapletModel getModel(@NotNull ISchemDiagram diagram)
		{
			ICapletController controller = getController(diagram);
			return controller != null ? controller.getCapletModel() : null;
		}

		@Nullable public ICapletModel getActiveCapletModel()
		{
			ICapletController activeCapletController = getActiveCapletController();
			return activeCapletController != null ? activeCapletController.getCapletModel() : null;
		}

		@Nullable public SelectSet getPreSelections()
		{
			ICapletController activeCapletController = getActiveCapletController();
			return activeCapletController != null ? activeCapletController.getSelectMgr().getPreSelections() : null;
		}
	}

	public static class UnitTestCAFUtilityProvider implements ICAFUtilityProvider
	{

		@Nullable public ICapletController getController(@NotNull ISchemDiagram diagram)
		{
			ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
			if (cw != null) {
				return cw.getController();
			}
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view instanceof IGfxView && ((IGfxView) view).getDiagram() == diagram) {
				return view.getController();
			}
			return null;
		}

		@Nullable public ICapletModel getModel(@NotNull ISchemDiagram diagram)
		{
			ICapletController controller = getController(diagram);
			return controller != null ? controller.getCapletModel() : null;
		}

		@Nullable public Container getContainer()
		{
			ICapletView activeCapletView = CAFUtils.getInstance().getActiveCapletView();
			ICapletWindow capletWindow = activeCapletView != null ? activeCapletView.getWindow() : null;
			return capletWindow != null ? capletWindow.getContainer() : null;
		}

		@Nullable public Frame getDialogFrame()
		{
			IWindowMgr windowMgr = CAFUtils.getInstance().getWindowMgr();
			return windowMgr != null ? windowMgr.getDialogFrame() : null;
		}

		@Nullable public ICapletModel getActiveCapletModel()
		{
			ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
			return capletController != null ? capletController.getCapletModel() : null;
		}

		@Nullable public SelectSet getPreSelections()
		{
			ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
			ISelectMgr selectMgr = capletController != null ? capletController.getSelectMgr() : null;
			return selectMgr != null ? selectMgr.getPreSelections() : null;
		}

		@Nullable public IModifiableCostStrategy getCostStrategy()
		{
			return null;
		}
	}

	private interface IEdgeFromSchematics
	{

		@Nullable IConnectivityEdge getConnectivityEdge();

		@Nullable IPin getSchemPin(@NotNull IAbstractPin endPin);

		@NotNull Set<IUIDObject> getIntermediateSchematics();

		@NotNull Set<IUIDObject> getEndSchematics();

		boolean canRouteEdge();
	}

	private static class ConnectivityEdgeFromSchematics implements IEdgeFromSchematics
	{

		@Nullable private IConductorTraversalOutput traversalOutput;
		@Nullable private IConnectivityEdge edge;
		@Nullable public IConnectivityEdgeResult resultSet;

		ConnectivityEdgeFromSchematics(@NotNull IConductor conductor,
				@NotNull Map<IConnectivityEdge, Set<chs.cof.logical.cable.IConductor>> ignoredEdges,
				ISchemDiagram diagram, boolean enableFullSignalRouting)
		{
			if (!enableFullSignalRouting) {
				traversalOutput = SchemTraversal.ContentExtractor.traverse(Collections.singleton(conductor));
			}
			else {
				traversalOutput = SchemTraversal.ContentExtractor
						.traverse(Collections.singleton(conductor), new SchemTraversal.TraversePolicy()
						{

							@Override protected boolean checkSplicePinSegmentsSize(@NotNull IPin schemPin)
							{
								return schemPin.getSegments().size() > 1;
							}

							@Override
							protected boolean getPinSize(@NotNull IPin schemPin, @NotNull IPinList attachedPinList,
									@NotNull IPin connectedPinSchem)
							{
								return attachedPinList.getPins().size() == 1 &&
										connectedPinSchem.getSegments().size() >= 1 &&
										schemPin.getSegments().size() >= 1;
							}

							@Override public boolean shouldTraverse(@NotNull IJoint joint)
							{
								return true;
							}

							@Override public boolean shouldTravesrDeviceConnectorPinPair()
							{
								return false;
							}

							@NotNull @Override
							public Collection<ILogicSegment> getSegmentToTraverse(@Nullable IJoint joint,
									@NotNull ILogicSegment segment)
							{
								Collection<ILogicSegment> segments = new ListSet<ILogicSegment>();
								if (joint == null) {
									return Collections.emptyList();
								}
								Set<ILogicSegment> segmentsAttachedToPin = joint.getAssociations(ILogicSegment.class);
								if (!segmentsAttachedToPin.isEmpty()) {
									//assert segmentsAttachedToPin.size() <= 1;
									for (ILogicSegment pinSegment : segmentsAttachedToPin) {
										if (pinSegment != segment) {
											segments.add(pinSegment);
										}
									}
								}
								return segments;
							}
						});
			}
			IPinContentInformation pinContent = traversalOutput.getPinContentInfo();
			IConnectivityEdgeResult connectivityEdges = ConnectivityEdgeHelper.createConnectivityEdges(pinContent);
			resultSet = connectivityEdges;
			if(enableFullSignalRouting) {
				edge = connectivityEdges.getNumEdges() < 1 ? null : connectivityEdges.getEdges()[0];
			}
			else  {
				edge = connectivityEdges.getNumEdges() != 1 ? null : connectivityEdges.getEdges()[0];
			}
			if (ignoredEdges.containsKey(edge)) {
				edge = null;
			}
			else if (edge != null) {
				Set<IUIDObject> allObjects = new LinkedHashSet<>();
				allObjects.addAll(getIntermediateSchematics());
				for (IUIDObject object : getEndSchematics()) {
					if (object instanceof IPin) {
						allObjects.add(object);
					}
				}
				boolean isValidEdge = collectOffGridConnectivityEdges(allObjects, edge, ignoredEdges, diagram);
				edge = isValidEdge ? edge : null;
			}
		}

		@Override public boolean canRouteEdge()
		{
			return edge != null && ConductorRouteActionHelper.isAutoRouteSupported(edge);
		}

		@Nullable @Override public IConnectivityEdge getConnectivityEdge()
		{
			return edge;
		}

		@Nullable @Override public IPin getSchemPin(@NotNull IAbstractPin endPin)
		{
			if (traversalOutput == null) {
				return null;
			}
			return traversalOutput.getEndSchematics().stream()
					.filter(IPin.class::isInstance)
					.map(IPin.class::cast)
					.filter(schemPin -> schemPin.getConnectivity() == endPin)
					.findFirst()
					.orElse(null);
		}

		@NotNull @Override public Set<IUIDObject> getIntermediateSchematics()
		{
			return traversalOutput != null ? traversalOutput.getIntermediateSchematics() : Collections.emptySet();
		}

		@NotNull @Override public Set<IUIDObject> getEndSchematics()

		{
			return traversalOutput != null ? traversalOutput.getEndSchematics() : Collections.emptySet();
		}
	}
}
