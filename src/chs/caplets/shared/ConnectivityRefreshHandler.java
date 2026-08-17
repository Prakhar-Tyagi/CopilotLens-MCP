package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.helpers.CHSUndoableEdit;
import chs.cof.draw.IColor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyActionInfo;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyEvent;
import chs.cofUtils.logical.concurrency.LogicConcurrencyController;
import chs.cofUtils.logical.concurrency.LogicConcurrencyEventType;
import chs.common.IDesignContainer;
import chs.common.ILockableLogicObjectRefHolder;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDObjectUtils;
import chs.system.UIDMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ConnectivityRefreshHandler extends AbstractDesignUpdateListener
{

	private Set<IUID> mModifiedLogicObjectUIDs;
	@NotNull private String mCurrentActionDescription = "";

	ConnectivityRefreshHandler(@NotNull ILogicDesign design, @NotNull ICapletController capletController)
	{
		super(design, capletController);
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		mModifiedLogicObjectUIDs = null;
		mController = capletController;
	}

	public void actionStarted(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		super.actionStarted(actionId);
		mCurrentActionDescription = getActionDescription(actionId);
		mModifiedLogicObjectUIDs = new HashSet<>();
	}

	private String getActionDescription(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		String actionDescription = actionId.getActionDescription();
		return StringUtils.isBlank(actionDescription) ? "" : HTMLHelper.bold(actionDescription + ": ");
	}

	public void actionEnded(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		super.actionEnded(actionId);
		mModifiedLogicObjectUIDs = null;
		mCurrentActionDescription = "";
	}

	@Override protected boolean shouldHandleUpdateFromRemoteOncePerAction()
	{
		return true;
	}

	@Override protected void handleDesignDataUpdateFromRemote()
	{
		IDesignContainer design = getDesign();
		String message = mCurrentActionDescription +
				HTMLHelper.color(IColor.GREEN, ResourceMgr.getString(BaseController.class,
						"LogicConcurrencyConnectivityRefresh.clearUndo.information",
						HTMLHelper.bold(design != null ? design.getFullName() : "Unknown")));
		LogicConcurrencyController.getInstance().getCAFView().getConcurrentEditReporter().report(message);
		mController.clearUndoQueue();
		CreationDeletionHelper.getTheCreationHelper().clear();
	}

	@SuppressWarnings("unused") protected void doActionEnded(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		if (getModifiedObjects() != null) {
			clearImpactedEdits();
			mModifiedLogicObjectUIDs.clear();
			mModifiedLogicObjectUIDs = null;
		}
	}

	private void clearImpactedEdits()
	{
		IDesignContainer design = getDesign();
		String designName = HTMLHelper.bold(design != null ? design.getFullName() : "Unknown");
		Pair<IUndoableContainer, Integer> containerToClear = getUndoContainerToClear();
		if (containerToClear == null) {
			return;
		}

		IUndoableContainer container = containerToClear.getFirst();
		Integer maxEditIndexToClear = containerToClear.getSecond();
		if (maxEditIndexToClear > -1) {
			List<UndoableEdit> edits = container.getUndoManager().getEdits();
			String message;
			if (maxEditIndexToClear < edits.size() - 1) {
				UndoableEdit lastClearedEdit = edits.get(maxEditIndexToClear);
				message = getResourceString(
						"LogicConcurrencyConnectivityRefresh.clearPartialUndo.information", designName,
						HTMLHelper.bold(lastClearedEdit.getPresentationName()));
			}
			else {
				message = getResourceString("LogicConcurrencyConnectivityRefresh.clearUndo.information",
						designName);
			}
			LogicConcurrencyController.getInstance().getCAFView().getConcurrentEditReporter()
					.report(mCurrentActionDescription + HTMLHelper.color(IColor.GREEN, message));
			container.getUndoManager().trimEdits(0, maxEditIndexToClear);
		}
	}

//	private void clearImpactedEdits(@NotNull ILogicConcurrencyActionInfo actionId,
//			@NotNull ILogicConnectivity logicConnectivity)
//	{
//		IDesignContainer design = logicConnectivity.getDesign();
//		String designName = HTMLHelper.bold(design != null ? design.getFullName() : "Unknown");
//		for (Map.Entry<IUndoableContainer, Integer> entry : getUndoContainersToClear().entrySet()) {
//			IUndoableContainer container = entry.getKey();
//			Integer maxEditIndexToClear = entry.getValue();
//			if (maxEditIndexToClear > -1) {
//				List<UndoableEdit> edits = container.getUndoManager().getEdits();
//				String message;
//				if (maxEditIndexToClear < edits.size() - 1) {
//					UndoableEdit lastClearedEdit = edits.get(maxEditIndexToClear);
//					message = getResourceString(
//							"LogicConcurrencyConnectivityRefresh.clearPartialUndo.information", designName,
//							HTMLHelper.bold(lastClearedEdit.getPresentationName()));
//				}
//				else {
//					message = getResourceString("LogicConcurrencyConnectivityRefresh.clearUndo.information",
//							designName);
//				}
//				LogicConcurrencyController.getInstance().getCAFView().getConcurrentEditReporter()
//						.report(getActionDescription(actionId) + HTMLHelper.color(IColor.GREEN, message));
//				container.getUndoManager().trimEdits(0, maxEditIndexToClear);
//			}
//		}
//	}

	private String getResourceString(String key, Object... args)
	{
		return ResourceMgr.getString(BaseController.class, key, args);
	}

	public void objectRemotelyModified(Collection<IUID> iuid)
	{
		if (mModifiedLogicObjectUIDs != null) {
			mModifiedLogicObjectUIDs.addAll(iuid);
		}
	}

	@Nullable private Pair<IUndoableContainer, Integer> getUndoContainerToClear()
	{
		IUndoableContainer undoableContainer = mController.getUndoableContainer();
		if (undoableContainer == null) {
			return null;
		}

		int maxEditIndex = -1;
		List<UndoableEdit> edits = undoableContainer.getUndoManager().getEdits();
		for (int editIndex = 0; editIndex < edits.size(); editIndex++) {
			UndoableEdit undoableEdit = edits.get(editIndex);
			if (shouldClearUndo(getModifiedObjects(), undoableEdit)) {
				maxEditIndex = editIndex;
			}
		}
		return new Pair<IUndoableContainer, Integer>(undoableContainer, maxEditIndex);
	}

//	private Map<IUndoableContainer, Integer> getUndoContainersToClear()
//	{
//		Map<IUndoableContainer, Integer> undoableContainers = getUndoableContainers();
//		if (undoableContainers.isEmpty()) {
//			return undoableContainers;
//		}
//
//		for (Map.Entry<IUndoableContainer, Integer> entry : undoableContainers.entrySet()) {
//			int maxEditIndex = -1;
//			IUndoableContainer container = entry.getKey();
//			List<UndoableEdit> edits = container.getUndoManager().getEdits();
//			for (int editIndex = 0; editIndex < edits.size(); editIndex++) {
//				UndoableEdit undoableEdit = edits.get(editIndex);
//				if (getUndoContainersToClear(getModifiedObjects(), undoableEdit)) {
//					maxEditIndex = editIndex;
//				}
//			}
//			entry.setValue(maxEditIndex);
//		}
//		return undoableContainers;
//	}

	private boolean shouldClearUndo(Collection<IUID> modifiedObjectUIDs, UndoableEdit undoableEdit)
	{
		boolean bClearUndo = false;
		if (undoableEdit instanceof CHSUndoableEdit) {
			CHSUndoableEdit edit = (CHSUndoableEdit) undoableEdit;

			Collection<IUIDObject> editedObjects = edit.getEditedObjects();
			Collection<IUIDObject> deletedObjects = edit.getDeletedObjects();
			Collection<IUIDObject> newObjects = new ArrayList<>();
			for (IUID newUID : edit.getNewUIDs()) {
				IUIDObject object = UIDMgr.getObject(newUID);
				if (object != null) {
					newObjects.add(object);
				}
			}
			if (shouldClearUndo(modifiedObjectUIDs, editedObjects) ||
					shouldClearUndo(modifiedObjectUIDs, deletedObjects) ||
					shouldClearUndo(modifiedObjectUIDs, newObjects)) {
				bClearUndo = true;
			}
		}
		return bClearUndo;
	}

	private boolean shouldClearUndo(Collection<IUID> modifiedObjectUIDs, Collection<IUIDObject> undoObjects)
	{
		for (IUIDObject object : undoObjects) {
			IUIDObject editedObj = object;
			ILockableLogicObjectRefHolder holder =
					UIDObjectUtils.reduceToNonDeletedObjectOfType(editedObj, ILockableLogicObjectRefHolder.class);
			IUID lockableLogicObjectUID = null;
			if (holder != null) {
				lockableLogicObjectUID = holder.getLockableLogicObjectUID();
			}
			if (lockableLogicObjectUID == null) {
				continue;
			}
			if (modifiedObjectUIDs.contains(lockableLogicObjectUID)) {
				return true;
			}
		}
		return false;
	}

//	@NotNull private Map<IUndoableContainer, Integer> getUndoableContainers()
//	{
//		ILogicConnectivity logicConnectivity = getConnectivity();
//		if (logicConnectivity == null) {
//			return Collections.emptyMap();
//		}
//		IDesignContainer design = logicConnectivity.getDesign();
//		if (design == null) {
//			return Collections.emptyMap();
//		}
//		return getUndoableContainersForDesign(design);
//	}
//
//	private Map<IUndoableContainer, Integer> getUndoableContainersForDesign(@NotNull final IDesignContainer design)
//	{
//		Collection<ICapletModel> capletModels = CAFUtils.getInstance().getModelsOfDesign(design);
//		Map<IUndoableContainer, Integer> undoableContainers = new HashMap<>();
//		for (ICapletModel capletModel : capletModels) {
//			undoableContainers.put(capletModel.getController().getUndoableContainer(), -1);
//		}
//		IUndoableContainer undoableContainer = mController.getUndoableContainer();
//		assert undoableContainer != null;
//		undoableContainers.put(undoableContainer, -1);
//
//		return undoableContainers;
//	}

	ICapletController getController()
	{
		return mController;
	}

	Collection<IUID> getModifiedObjects()
	{
		return mModifiedLogicObjectUIDs;
	}

	@Override protected boolean shouldNotify(@Nullable ILogicDesign context)
	{
		//listen only for the related design only.
		return isContextMatching(context) && super.shouldNotify(context);
	}

	@Override protected void doProcessEvent(@NotNull ILogicConcurrencyEvent event)
	{
		//do need to invoke parent method. otherwise it will not intercept design update from remote.
		super.doProcessEvent(event);
		LogicConcurrencyEventType eventType = event.getEventType();
		if (eventType == LogicConcurrencyEventType.LOGICOBJECT_REMOTELY_MODIFIED) {
			objectRemotelyModified(event.getObjects());
		}
	}

	@NotNull @Override public Collection<LogicConcurrencyEventType> getInterestedEvents()
	{
		List<LogicConcurrencyEventType> interestedEvents = new ArrayList<>(4);
		interestedEvents.addAll(super.getInterestedEvents());
		// Disable the logic object modified event
		// interestedEvents.add(LogicConcurrencyEventType.LOGICOBJECT_REMOTELY_MODIFIED);
		return Collections.unmodifiableCollection(interestedEvents);
	}
}
