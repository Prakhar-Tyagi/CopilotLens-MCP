package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect;

import chs.caplets.logic.actions.LogicMultipointCreateAction;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageContentHelper;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect.SchemObjectsConnector;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect.SchemObjectsToConnectProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.EdgesConnecter;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.IRoutedSegmentEdgesProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.RoutedConductorSegmentEdges;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.RoutedHighwaySegmentEdges;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.publisher.offPage.ISelectionForFetch;
import chs.utilities.BuildInfo;
import chs.utilities.ResourceMgr;
import chs.utility.IMessageReporterWithContext;
import chs.utility.IMessageReporterWithContextListener;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.ui.progress.IProgress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RouteAndConnectFetchedObjectsCmd implements IMessageReporterWithContextListener
{

	@NotNull private final ISchemDiagram m_diagram;
	private final IProject m_project;
	private final List<ILogicDesign> m_designs;
	private SchemObjectsToConnectProvider schemObjectsToConnectProvider;
//	private IMessageReporterWithContext m_reporter;
	private IProgress m_progress;
	private ISelectionForFetch m_selection;

	public RouteAndConnectFetchedObjectsCmd(IProject project,
			List<ILogicDesign> logicDesigns, @NotNull ISchemDiagram diagram,
			ISelectionForFetch selection)
	{
		m_project = project;
		m_designs = logicDesigns;
		m_diagram = diagram;
		m_selection = selection;
	}

	public boolean routeAndConnect(Set<chs.cof.logical.schem.IConductor> conductorList,
			Set<IHighwaySchematic> highwaySchematics,
			Set<IAbstractSchemPin> fetchedPins)
	{
		Set<IAbstractSchemPin> pinsInSelection = m_selection.getAllPins();
		Set<chs.cof.logical.schem.IConductor> cToConnect = new LinkedHashSet<>(conductorList);
		Set<IHighwaySchematic> hToConnect = new LinkedHashSet<>(highwaySchematics);
		List<chs.cof.logical.schem.IConductor> conductorsInSelection = m_selection.getConductors();
		schemObjectsToConnectProvider =
				new SchemObjectsToConnectProvider(m_project, m_designs, m_diagram, pinsInSelection, fetchedPins,
						conductorsInSelection);
		List<chs.cof.logical.schem.IConductor> floatingConductorsInDiagram =
				m_diagram
						.getConductors()
						.stream()
						.filter(Predicate.not(FetchOffPageContentHelper::isCenterStrippedConductor).and(this::hasZeroPins))
						.filter(c -> c.getNumberOfInterfacedHighways() == 0)
						.collect(Collectors.toList());
		cToConnect.addAll(floatingConductorsInDiagram);
		Set<chs.cof.logical.schem.IConductor> schemWithNoConnectivity = cToConnect
				.stream()
				.filter(c -> c.getConnectivity() == null)
				.collect(Collectors.toSet());
		deleteObjects(schemWithNoConnectivity);
		Map<ILogicObject, Set<chs.cof.logical.schem.IConductor>> schemConductorsGroupedByConnectivity =
				groupByConnectivity(cToConnect);
		List<IHighwaySchematic> floatingHighwaysInDiagram =
				m_diagram
						.getHighways()
						.stream()
						.filter(c -> c.getConnectedStackPins().isEmpty())
						.collect(Collectors.toList());
		hToConnect.addAll(floatingHighwaysInDiagram);
		Map<ILogicObject, Set<IHighwaySchematic>> schemHighwaysGroupedByConnectivity =
				groupByConnectivity(hToConnect);
		getProgress()
				.ifPresent(p -> p.setRange(
						schemConductorsGroupedByConnectivity.size() + schemHighwaysGroupedByConnectivity.size()));
		connectSchemConductors(schemConductorsGroupedByConnectivity, hToConnect);
		connectHighways(schemHighwaysGroupedByConnectivity);
		updateCompositeText();
		m_diagram.refreshRepresentations();
		return true;
	}

	private boolean hasZeroPins(chs.cof.logical.schem.IConductor iConductor)
	{
		return iConductor.getNumPins() == 0;
	}

	private void connectSchemConductors(
			Map<ILogicObject, Set<chs.cof.logical.schem.IConductor>> schemConductorsGroupedByConnectivity,
			Set<IHighwaySchematic> highwaySchematics)
	{
		for (Map.Entry<ILogicObject, Set<chs.cof.logical.schem.IConductor>> entry : schemConductorsGroupedByConnectivity
				.entrySet()) {
			ILogicObject logicObject = entry.getKey();
//			if (logicObject instanceof IShieldConductor) {
//				continue;
//			}
			Set<chs.cof.logical.schem.IConductor> schemConductors = entry.getValue();
			if (logicObject instanceof IConductor) {
				IConductor conductorConn = (IConductor) logicObject;
				IRoutedSegmentEdgesProvider provider =
						new RoutedConductorSegmentEdges(m_diagram, conductorConn);
				ISchemObjectsConnector schemObjectsConnector =
						createSchemObjectsConnecter(provider);
				showProgressAndLog(logicObject);
				connectSchemConductors(schemObjectsConnector, schemConductors, highwaySchematics, conductorConn);
			}
		}
	}

	private void showProgressAndLog(ILogicObject logicObject)
	{
		String progressMsg = ResourceMgr.getString(RouteAndConnectFetchedObjectsCmd.class,
				"RouteAndConnectFetchedObjectsCmd.connecting.text", logicObject.getName());
		getProgress().ifPresent(p -> p.increment(progressMsg));
		logMsg("Connecting " + logicObject.getName() + "...");
	}

	public static void logMsg(String msg)
	{
		if (BuildInfo.getBuildInfo().areDeveloperOrQAExtensionsEnabled()) {
			System.out.println(msg);
		}
	}

	private void connectHighways(Map<ILogicObject, Set<IHighwaySchematic>> schemHighwaysGroupedByConnectivity)
	{
		for (Map.Entry<ILogicObject, Set<IHighwaySchematic>> entry : schemHighwaysGroupedByConnectivity.entrySet()) {
			ILogicObject logicObject = entry.getKey();
			Set<IHighwaySchematic> schemConductors = entry.getValue();
			if (logicObject instanceof IGeneralHighway) {
				IRoutedSegmentEdgesProvider provider =
						new RoutedHighwaySegmentEdges(m_diagram, (IGeneralHighway) logicObject);
				ISchemObjectsConnector schemObjectsConnector = createSchemObjectsConnecter(provider);
				showProgressAndLog(logicObject);
				connectHighways(schemObjectsConnector, schemConductors);
			}
		}
	}

	private <E extends IDiagramObject> Map<ILogicObject, Set<E>> groupByConnectivity(
			Set<E> conductorList)
	{
		return conductorList
				.stream()
				.collect(Collectors.groupingBy(this::getConnectivity, Collectors.toSet()));
	}

	@NotNull private ISchemObjectsConnector createSchemObjectsConnecter(IRoutedSegmentEdgesProvider provider)
	{
		EdgesConnecter edgesConnecter = new EdgesConnecter(provider);
		return new SchemObjectsConnector(edgesConnecter);
	}

	private Optional<IProgress> getProgress()
	{
		return Optional.ofNullable(m_progress);
	}

	private boolean connectSchemConductors(ISchemObjectsConnector schemObjectsConnector,
			Set<chs.cof.logical.schem.IConductor> schemConductors,
			Set<IHighwaySchematic> highwaySchematics, IConductor conductorConn)
	{
		ISchemObjectsToConnect schemObjects =
				schemObjectsToConnectProvider.getSchemObjectsToConnectForConductor(schemConductors, conductorConn);
		boolean connectSuccessfull = doRouteAndConnect(schemObjectsConnector, schemObjects);
		if (connectSuccessfull) {
			highwaySchematics.removeAll(schemObjects.getObjectsToIgnore());
			final Set<? extends IUIDObject> leftOverObjects = schemObjects.getLeftOverObjects();
			if (!leftOverObjects.isEmpty()) {
				final IUIDObject next = leftOverObjects.iterator().next();
				if (next instanceof chs.cof.logical.schem.IConductor) {
					final chs.cof.logical.schem.IConductor leftOverSchem = (chs.cof.logical.schem.IConductor) next;
					schemObjects = schemObjectsToConnectProvider.getSchemObjectsToConnectForConductor(
							Set.of(leftOverSchem), conductorConn);
					doRouteAndConnect(schemObjectsConnector, schemObjects);
				}
			}
		}
		return connectSuccessfull;
	}

	private boolean connectHighways(ISchemObjectsConnector schemObjectsConnector,
			Set<IHighwaySchematic> schemConductors)
	{
		ISchemObjectsToConnect schemObjects =
				schemObjectsToConnectProvider.getSchemObjectsToConnectForHighway(schemConductors);
		return doRouteAndConnect(schemObjectsConnector, schemObjects);
	}

	private boolean doRouteAndConnect(ISchemObjectsConnector schemObjectsConnector, ISchemObjectsToConnect schemObjects)
	{
		deleteObjects(schemObjects.getObjectsToDelete());
		schemObjects.addForAutoRoute();
		return schemObjects.connect(schemObjectsConnector);
	}

	void deleteObjects(Collection<? extends IUIDObject> toBeDeleted)
	{
		toBeDeleted.forEach(c -> {
			c.delete();
		});
	}

	@Nullable private ILogicObject getConnectivity(IDiagramObject diagramObject)
	{
		IConnectivityRef connectivityRef = getConnectivityRef(diagramObject);
		ILogicObject connectivity = null;
		if (connectivityRef != null) {
			connectivity = connectivityRef.getConnectivity();
		}
		return connectivity;
	}

	@Nullable private IConnectivityRef getConnectivityRef(IDiagramObject diagramObject)
	{
		IDiagramObject object = diagramObject;
		while (true) {
			if (object instanceof IConnectivityRef) {
				return (IConnectivityRef) object;
			}
			IDiagramObject parent = object.getParent();
			if (parent == null) {
				return null;
			}
			object = parent;
		}
	}

	private boolean updateCompositeText()
	{
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		Iterator<IUIDObject> itr = cdh.getNewObjectsToProcess();
		LogicMultipointCreateAction.updateCompositeTexts(itr);
		return true;
	}

	public void addProgress(IProgress progress)
	{
		m_progress = progress;
	}

	@Override public void setReporter(IMessageReporterWithContext reporter)
	{
//		m_reporter = reporter;
	}
}
