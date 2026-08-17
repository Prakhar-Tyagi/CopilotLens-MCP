package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.caf.caplet.selection.SelectSet;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.SharedUsageDesignsIdentifier;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DeletableSelectionsProvider
{

	private final boolean m_checkForConnectivityChanges;
	private final IDeletableSelectionHelper m_deleteSelectionHelper;
	private final boolean m_logMessages;
	private final OutputStream m_streamForDebugging;
	private ConnectivityObjectProvider m_connectivityObjectProvider;

	public DeletableSelectionsProvider(boolean checkForConnectivityChanges,
			boolean logMessages, IDeletableSelectionHelper publisherDeleteHelper,
			@Nullable OutputStream streamForDebugging)
	{
		m_connectivityObjectProvider = new ConnectivityObjectProvider();
		m_checkForConnectivityChanges = checkForConnectivityChanges;
		m_deleteSelectionHelper = publisherDeleteHelper;
		m_logMessages = logMessages;
		m_streamForDebugging = streamForDebugging;
	}

	public Set<IUIDObject> getSelectionsToDelete(@NotNull Set<IUIDObject> originalSelection,
			SelectSet sset)
	{
		SelectionObjects selections = getSelections(originalSelection, sset);
		if (m_logMessages) {
			PublisherDeleteMessageLogger logger = new PublisherDeleteMessageLogger();
			logger.logMessages(selections);
		}
		serialize(selections);
		return selections.getDeletables();
	}

	private SelectionObjects getSelections(@NotNull Set<IUIDObject> originalSelection,
			SelectSet sset)
	{
		Set<IUIDObject> filteredSelections = getFilteredSelections(originalSelection);
		Map<ILogicObject, Set<IDiagramObject>> connectivityGroup = new HashMap<>();
		Map<IDiagramObject, Set<IUIDObject>> diagramObjectGroup = new HashMap<>();
		Set<IDiagramObject> probables = new HashSet<>();
		Set<IUIDObject> nonConnectivityDeletables = new HashSet<>();
		Map<IUIDObject, IDiagramObject> nonConnectivityDeletableToDiagramObjMap = new HashMap<>();
		SelectionObjects objs = new SelectionObjects();
		for (IUIDObject obj : filteredSelections) {
			IDiagramObject diagramObject = m_connectivityObjectProvider.getConnectivityRefDiagramObject(obj);
			if (diagramObject == null) {
				continue;
			}
			update(diagramObjectGroup, obj, diagramObject);
			boolean deletableAsPerLogic = m_deleteSelectionHelper.isDeletableAsPerLogic(obj, sset);
			if (!deletableAsPerLogic) {
				objs.addToNotDeletablesDueToOtherReasons(diagramObject);
			}
			boolean nonConnectivityDeletable =
					m_deleteSelectionHelper.isNonConnectivityDeletable(obj) && deletableAsPerLogic;
			if (nonConnectivityDeletable) {
				nonConnectivityDeletables.add(obj);
				nonConnectivityDeletableToDiagramObjMap.put(obj, diagramObject);
				continue;
			}
			boolean fetchedObject = m_deleteSelectionHelper.isSupplementaryObject(diagramObject);
			if (!fetchedObject) {
				objs.addToNotFetched(diagramObject);
			}
			boolean fetched = fetchedObject && deletableAsPerLogic;
			if (fetched) {
				probables.add(diagramObject);
			}
			update(connectivityGroup, diagramObject);
		}
		if (nonConnectivityDeletables.size() == filteredSelections.size()) {
			objs.addToDeletables(nonConnectivityDeletables);
			return objs;
		}
		Set<IDiagramObject> connectivityDeletables;
		if (m_checkForConnectivityChanges) {
			connectivityDeletables = probables
					.stream()
					.filter(Objects::nonNull)
					.filter(probable -> {
						return checkForConnectivityChanges(probable, connectivityGroup, objs);
					})
					.collect(Collectors.toSet());
			probables.removeAll(connectivityDeletables);
//			probables
//					.forEach(p -> objs.addToLastInstances(p));
		}
		else {
			connectivityDeletables = probables;
		}
		objs.addToDeletables(getAllDeletableObjects(connectivityDeletables, diagramObjectGroup));
		objs.addToDeletables(
				getNonConnectivityDeletableObjects(nonConnectivityDeletables, filteredSelections,
						nonConnectivityDeletableToDiagramObjMap));
		return objs;
	}

	private Set<IUIDObject> getFilteredSelections(Set<IUIDObject> originalSelections)
	{
		Set<IUIDObject> filteredSelections = new LinkedHashSet<>();
		for (IUIDObject selection : originalSelections) {
			if (IAbstractSchemPin.class.isInstance(selection)) {
				IDiagramObject pinParent = IAbstractSchemPin.class.cast(selection).getParent();
				if (!originalSelections.contains(pinParent)) {
					filteredSelections.add(selection);
				}
			}
			else {
				filteredSelections.add(selection);
			}
		}
		return filteredSelections;
	}

	private boolean checkForConnectivityChanges(@NotNull IDiagramObject probable,
			Map<ILogicObject, Set<IDiagramObject>> connectivityGroup, SelectionObjects objs)
	{
		Collection<ILogicObject> logicObjects = m_connectivityObjectProvider.getConnectivity(probable);
		Collection<IDesignContainer> designScope = getDesignScope(logicObjects);
		// if there are no logicObjects, then the object is not connected to anything
		for (ILogicObject connectivity : logicObjects) {
			Set<IDiagramObject> instances = connectivityGroup.get(connectivity);
			if (logicObjects.isEmpty() || instances == null) {
				return false;
			}
			boolean willNotChange = checkForConnectivityChange(probable, objs, connectivity, instances, designScope);
			if (!willNotChange) {
				return false;
			}
		}
		return true;
	}

	@NotNull private Set<IDesignContainer> getDesignScope(@NotNull Collection<ILogicObject> logicObjects)
	{
		if(logicObjects.isEmpty()) {
			return new HashSet<>();
		}
		IProject project = logicObjects.iterator().next().getProject();
		Collection<ILogicDesign> designs = SharedUsageDesignsIdentifier.determineDesigns(project, getSharedObjects(logicObjects));
		return CollectionUtils.getObjects(designs, IDesignContainer.class);
	}

	@NotNull private Collection<ISharedObject> getSharedObjects(@NotNull Collection<ILogicObject> logicObjects)
	{
		return logicObjects.stream()
				.filter(ILogicObject::isShared)
				.map(ILogicObject::getSharedObject)
				.collect(Collectors.toSet());
	}

	private boolean checkForConnectivityChange(@NotNull IDiagramObject probable,
			SelectionObjects objs, ILogicObject connectivity, Set<IDiagramObject> instances, @NotNull Collection<IDesignContainer> designScope)
	{
		ConnectivityChangeChecker connectivityChangeChecker =
				new ConnectivityChangeChecker(connectivity, instances, designScope);
		if (connectivityChangeChecker.containsAllInstances()) {
			objs.addToLastInstances(probable);
			return false;
		}
		if (connectivityChangeChecker.willConnectivityChangeWithDelete()) {
			objs.addToDeleteLeadsToConnectivityChange(probable);
			return false;
		}
		return true;
	}

	@NotNull private Set<IUIDObject> getNonConnectivityDeletableObjects(Set<IUIDObject> nonConnectivityDeletables,
			Set<IUIDObject> originalSelection,
			Map<IUIDObject, IDiagramObject> nonConnectivityDeletableToDiagramObjMap)
	{
		Set<IUIDObject> allDeletables = new HashSet<>();
		nonConnectivityDeletables.forEach(ncd -> {
			IDiagramObject diagramObject = nonConnectivityDeletableToDiagramObjMap.get(ncd);
			if (!originalSelection.contains(diagramObject)) {
				allDeletables.add(ncd);
			}
		});
		return allDeletables;
	}

	@NotNull private Set<IUIDObject> getAllDeletableObjects(Set<IDiagramObject> connectivityDeletables,
			Map<IDiagramObject, Set<IUIDObject>> diagramObjectGroup)
	{
		Set<IUIDObject> allDeletables = new HashSet<>();
		allDeletables.addAll(connectivityDeletables);
		connectivityDeletables.forEach(cd -> {
			Set<IUIDObject> relatedObjects = diagramObjectGroup.get(cd);
			if (relatedObjects != null) {
				allDeletables.addAll(relatedObjects);
			}
		});
		return allDeletables;
	}

	private void update(Map<IDiagramObject, Set<IUIDObject>> group, IUIDObject iuidObject,
			IDiagramObject diagramObject)
	{
		Set<IUIDObject> diagramObjects = group.get(diagramObject);
		if (diagramObjects == null) {
			diagramObjects = new HashSet<>();
		}
		diagramObjects.add(iuidObject);
		group.put(diagramObject, diagramObjects);
	}

	private void update(Map<ILogicObject, Set<IDiagramObject>> group, @NotNull IDiagramObject diagramObject)
	{
		Collection<ILogicObject> connectivities = m_connectivityObjectProvider.getConnectivity(diagramObject);
		if (connectivities.isEmpty()) {
			return;
		}
		for (ILogicObject connectivity : connectivities) {
			update(group, diagramObject, connectivity);
		}
	}

	private void update(Map<ILogicObject, Set<IDiagramObject>> group,
			@NotNull IDiagramObject diagramObject, ILogicObject connectivity)
	{
		Set<IDiagramObject> diagramObjects = group.get(connectivity);
		if (diagramObjects == null) {
			diagramObjects = new HashSet<>();
		}
		diagramObjects.add(diagramObject);
		group.put(connectivity, diagramObjects);
	}

	private void serialize(SelectionObjects selections)
	{
		DeletableSelectionsSerializer serializer = new DeletableSelectionsSerializer(selections, m_streamForDebugging,
				m_connectivityObjectProvider);
		serializer.serialize();
	}
}
