package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.capitalmanager.appserver.DefaultUserSessionListener;
import chs.capitalmanager.appserver.IUserSession;
import chs.caplets.logic.actions.GenerateFilteredDiagramDialog;
import chs.caplets.logic.actions.serviceDocumentation.offPage.FetchOffPageConnectivityAction;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemDiagramIterator;
import chs.cof.project.IProject;
import chs.cof.project.folder.IFolder;
import chs.cog.ICOGLockable;
import chs.common.IIncLoadable;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUpgradeableDesignContainer;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;
import chs.system.ICHSSystem;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.IPair;
import chs.utilities.IXMLTags;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DesignUpgradeHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.DesignHelper;
import chs.utility.task.ITask;
import chs.utility.task.ITaskFinishedListener;
import chs.utility.ui.HTMLHelper;
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
import java.util.Set;
import java.util.stream.Collectors;

public class CreateFilteredDiagramDelegate extends CreateNewDelegate
{

	protected Set<ILogicDesign> m_designsToUnlock = new HashSet<>();

	protected List<Boolean> generationStatusList = new ArrayList<Boolean>();

	public CreateFilteredDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass,
			@NotNull ICaplet caplet, String designXMLTag, boolean updateXrefOnReadOnly, int drawGridSpacing)
	{
		super(lifeCycleListener, resourceClass, caplet, designXMLTag, updateXrefOnReadOnly, drawGridSpacing);
	}

	public Pair<Boolean, IBaseDiagram> createNew(List<?> context)
	{
		return new Pair<>(true, null);
	}

	@NotNull protected Set<IUID> doBatchLock(Set<ILogicDesign> toBeLocked)
	{
		return UtilsHelper.getPersistenceSession().batchLock(toBeLocked);
	}

	protected boolean batchLockDesigns(List<ILogicDesign> logicDesigns)
	{
		Set<ILogicDesign> toBeLocked = logicDesigns
				.stream()
				//not already locked and not the current design
				.filter(obj -> !obj.isLocked())
				.collect(Collectors.toSet());
		Set<IUID> failedToLock = doBatchLock(toBeLocked);
		//only those which are locked by the action
		m_designsToUnlock = toBeLocked
				.stream()
				.filter(obj -> obj.isLocked())
				.collect(Collectors.toSet());
		if (!failedToLock.isEmpty()) {
			List<ILogicDesign> notAbleToLock =
					IUIDObject.Statics.getListOfType(failedToLock, ILogicDesign.class);
			String message = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					"FetchOffPageConnectivityAction.message.cannot.lock.designs");
			if (logicDesigns.size() == 1) {
				showDesignLockedErrorMessage(logicDesigns.get(0));
				logicDesigns.remove(logicDesigns.get(0));
				return false;
			}
			for (ILogicDesign design : notAbleToLock) {
				LogHelper.printMsg(HTMLHelper.color("red", design.getName() + "->" + message));
				logicDesigns.remove(design);
			}
			return false;
		}
		return true;
	}

	private void filterInAccessibleDesigns(@NotNull List<ILogicDesign> logicDesigns)
	{
		Set<IUID> accessibleDesigns = SharedObjectDomainAccessibliltyChecker
				.getLogicDesignsBasedOnSharedObjectsAccessibility(
						logicDesigns.stream().map(logicDesign -> logicDesign.getUID()).collect(
								Collectors.toSet()));
		Set<ILogicDesign> inAccessibleDesigns =
				logicDesigns.stream().filter(logicDesign -> !accessibleDesigns.contains(logicDesign.getUID())).collect(
						Collectors.toSet());
		if (!inAccessibleDesigns.isEmpty()) {
			String message = ResourceMgr.getString(BaseLifecycleDelegate.class,
					"BaseLifecycleDelegate.autoviewnewdiagram.sharedobject.inAccessibleDesigns");
			if (logicDesigns.size() == 1) {
				showInAccessibleDesignErrorMessage(logicDesigns.get(0));
				logicDesigns.remove(logicDesigns.get(0));
				return;
			}
			for (ILogicDesign design : inAccessibleDesigns) {
				LogHelper.printMsg(HTMLHelper.color("red", design.getName() + "->" + message));
				logicDesigns.remove(design);
			}
		}
	}

	private void showInAccessibleDesignErrorMessage(@NotNull ILogicDesign design)
	{
		ResourceBasedMessageContent content = new ResourceBasedMessageContent(BaseLifecycleDelegate.class,
				"BaseLifecycleDelegate.autoviewnewdiagram.sharedobject.domainAccessError");
		content.setMessageParameters(design.getName());
		Message.show(PromptSeverity.ERROR, content);
	}

	public void logCavalGenerationMsg(String msg)
	{
		LogHelper.printMsg(HTMLHelper.color("red", msg));
	}

	public List<Pair<Boolean, IBaseDiagram>> createNewWithMultipleDesigns(List<List<?>> contextList)
	{
		List<Pair<Boolean, IBaseDiagram>> failureResult = new ArrayList<>();
		failureResult.add(FAILURE_RESULT);
		if (contextList == null || contextList.isEmpty()) {
			return failureResult;
		}
		IProject project = getProjectFromContext(contextList.get(0));//or  project as a param

		if (mLifeCycleListener.projectDeleted(project)) {
			return failureResult;
		}
		// Check for running save
		boolean anyRunningTask = anyRunningSaveForProject(project);
		if (anyRunningTask) { // The prev save is still not completed
			return failureResult; // Cancel the create new
		}

		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (isMultiUserMode) {
			getStatusReporter().showInformationMessage(mMainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotCreate"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotCreate"));
			return failureResult;
		}

		// None of the changes involved in adding a new or new filtered diagram are undoable.
		// This includes changes to temporary UID objects in the dialogs
		// Must ensure we reset this in a finally method just down there somewhere  V
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		boolean designAlreadyLocked = false;
		boolean discardDesign = false;
		List<Boolean> designsAlreadyLockedList = new ArrayList<>();
		List<Boolean> discardDesignList = new ArrayList<>();

		List<Pair<Boolean, IBaseDiagram>> listOfPairs = new ArrayList<>();
		List<ILogicDesign> designList = new ArrayList<>();
		ISchemDiagram diagram = null;
		try {
			List<IFolder> folderList = new ArrayList<>();
			boolean success;
			for (List<?> context : contextList) {
				ILogicDesign design = getDesignFromContext(context);

				// this is the created/generated diagram, will be used to determin the diagram to open
				diagram = null;
				success = false;

				// Creating a diagram in an existing design?
				// Check to see if we have permission to edit this design.
				if (!isEditAllowed(design,
						ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDiagram.text"))) {
					if (contextList.size() == 1) {
						return failureResult;
					}
					logCavalGenerationMsg(ResourceMgr
							.getString(getResourceClass(), "Lifecycle.CannotEdit.Message.text", design.getName()) +
							", " + ResourceMgr.getString(getResourceClass(), "Lifecycle.CannotGenerateDiagrams.text"));

					continue;
				}
				// Do we have a lock on this design locked, or can we one?
				designAlreadyLocked = design.isLocked();
				designsAlreadyLockedList.add(designAlreadyLocked);

				// Get the last context

				//Filtered Diagram
				if (mLifeCycleListener.projectDeleted(project)) {
					return failureResult;
				}
				if (!checkDesignExists(project, design)) {
					// Design was deleted while user was in dialog!
					String message = design.getName() + "-> " +
							ResourceMgr.getString(getResourceClass(), "Lifecycle.DesignNotExists.text") + ", " +
							ResourceMgr.getString(getResourceClass(), "Lifecycle.CannotGenerateDiagrams.text");
					LogHelper.printMsg(HTMLHelper.color("red", message));
					continue;
				}

				if (((ICapletLifecycle) mLifeCycleListener).isModified(design)) {
					// Design has been modified and need to be saved.
					MessageHelper.showInformationMessage(mMainFrame,
							ResourceMgr.getString(getResourceClass(), "Lifecycle.MessageHelper.SaveDesign.text"),
							ResourceMgr
									.getString(getResourceClass(),
											"Lifecycle.MessageHelper.SaveDesignMessage.text"));
					return failureResult;
				}

				DesignHelper.DESIGN_CONTAINS_UNSUPPORTED unsupportedObjType =
						DesignHelper.hasUnsupportedObjectsForGeneration(design, DesignHelper.DESIGN_CONTAINS_UNSUPPORTED.STACKPINS);
				if (unsupportedObjType != DesignHelper.DESIGN_CONTAINS_UNSUPPORTED.NONE) {
					if (contextList.size() == 1) {
						ResourceBasedMessageContent content = new ResourceBasedMessageContent(getResourceClass(),
								"BaseLifecycle.msg.cannotGenerateDiagrams");
						Message.show(PromptSeverity.ERROR, content);
						return failureResult;
					}
					else {
						logCavalGenerationMsg(design.getName() + "->" + ResourceMgr.getString(getResourceClass(),
								"BaseLifecycle.msg.cannotGenerateDiagrams.implications"));
						continue;
					}
				}
				designList.add(design);
				Object lastElem = context.get(context.size() - 1);
				if (lastElem instanceof IFolder) { // Folder is the parent
					IFolder folder = (IFolder) lastElem;
					folderList.add(folder);
				}
			}

			batchLockDesigns(designList);
			if (!designList.isEmpty()) {
				filterInAccessibleDesigns(designList);
			}
			if (designList.isEmpty()) {
				return failureResult;
			}

			List<ISchemDiagram> modifiedDiagrams = new ArrayList<ISchemDiagram>();
			List<ISchemDiagram> generatedDiagrams = new ArrayList<ISchemDiagram>();
			List<ISchemDiagram> regeneratedDiagrams = new ArrayList<ISchemDiagram>();

			boolean filter =
					mDesignTagXML.equals(IXMLTags.LOGICALDESIGN) || mDesignTagXML.equals(IXMLTags.FUNCTIONDESIGN);
			IPair<Boolean, Boolean> result =
					createDiagramByFilterWithMultipleDesigns(project, designList, folderList,
							filter,
							generatedDiagrams, regeneratedDiagrams, modifiedDiagrams, false);

			generationStatusList = GenerateFilteredDiagramDialog.getGenerationStatusList();
			Iterator<Boolean> generationStatusIter = generationStatusList.iterator();
			boolean allDesignsFailed = true;

			for (ILogicDesign design : designList) {
				success = result.getFirst() != null ? result.getFirst() : false;
				discardDesign = result.getSecond() != null ? result.getSecond() : true;
				if (!GenerateFilteredDiagramDialog.isSingleDesign()) {
					success = generationStatusIter.hasNext() ? generationStatusIter.next() : true;
					discardDesign = (!success);
				}
				discardDesignList.add(discardDesign);
				if (!success) {
					continue;
				}

				allDesignsFailed = false;
				Collection<IUID> diagrams = new ArrayList<IUID>();
				List<String> storedObjects = new ArrayList<>();
				IAuditTrailLogger auditTrailLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
				if (new DesignUpgradeHelper().isDesignHavingOlderDTDVersion((IUpgradeableDesignContainer) design)) {
					ISchemDiagramIterator designDiagrams = design.getDiagrams();
					while (designDiagrams.hasNext()) {
						diagrams.add(designDiagrams.getNext().getUID());
					}
				}
				else {
					String diagramType;
					for (ISchemDiagram dia : generatedDiagrams) {
						((IIncLoadable) dia).setSkeletonizable(true);
						if (!diagrams.contains(dia.getUID())) {
							diagrams.add(dia.getUID());
							DiagramAuditTrialHelper.getInstance().storeDiagramAuditTrail(dia, AuditableEventType.DIAGRAM_CREATED, null);
						}
					}
					for (ISchemDiagram dia : regeneratedDiagrams) {
						((IIncLoadable) dia).setSkeletonizable(true);
						if (!diagrams.contains(dia.getUID())) {
							diagrams.add(dia.getUID());
							DiagramAuditTrialHelper.getInstance().storeDiagramAuditTrail(dia, AuditableEventType.DIAGRAM_CREATED, null);
						}
					}
				}

				Map<IUID, Collection<IUID>> saveMap = new HashMap<IUID, Collection<IUID>>();
				saveMap.put(design.getUID(), diagrams);

				// dts0100438476 - ConcurrentModificationException - Generate Filtered Diagram - with Multicore - twice
				// No validation is triggered here. The validation has already been done after the diagrams have been generated.
				// TODO This needs investigating again as I have added a validation call 2 calls down the 'save' chain
				// in DesignCapletLifeCycleHelper.saveDesign()
				new ModelChangeNotifierAtCompletionOfSaveTask().saveDesign(design, project, saveMap);
				DiagramAuditTrialHelper.getInstance().postStoredEvents();
				// clean the undoable containers
				ICapletModel model = getModel(design);
				if (model != null) {
					model.getController().getUndoableContainer().clear();
				}
			}
			if (allDesignsFailed) {
				return failureResult;
			}

			if (!GenerateFilteredDiagramDialog.isRunInBackground()) {

				//Open the Generated Diagram
				diagram = getDiagramToDisplay(generatedDiagrams);

				if (diagram == null) {
					diagram = getDiagramToDisplay(regeneratedDiagrams);
				}
			}
			listOfPairs.add(new Pair<Boolean, IBaseDiagram>(true, diagram));
		}

		finally {
			// If we failed to create a diagram, and we locked the design just
			// to create a diagram, unlock it.
			DiagramAuditTrialHelper.getInstance().discardStoredEvents();
			Iterator<Boolean> discardDesignIter = discardDesignList.iterator();
			Iterator<Boolean> designAlreadyLockedIter = designsAlreadyLockedList.iterator();
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			for (ILogicDesign design : designList) {
				discardDesign = discardDesignIter.hasNext() ? discardDesignIter.next() : true;
				designAlreadyLocked = designAlreadyLockedIter.next();
				boolean isDesignDiagramToBeOpened = (diagram != null && diagram.getDesign() == design);
				if (!designAlreadyLocked && design.isLocked() && !isDesignDiagramToBeOpened) {
					design.unlock();
					((IIncLoadable) design).setSkeleton(true);
				}

				// If we locked the folder manager, unlock it.
				if (discardDesign) {
					((ICapletLifecycle) mLifeCycleListener).discard(design);
					((BaseLifecycle) mLifeCycleListener).closeDesign(project, design, false);

					design.unloadDiagramsFully();
					design.getDiagrams(); //load the diagram collection again with the original set of diagrams
				}
			}
			CAFUtils.getInstance().clearTempUndoableContainer();
		}

		// Tell the project manager that we have edited the project
		getFIB().getProjectMgr().projectEdited(project, contextList.get(0));
		return listOfPairs;
	}

	private class ModelChangeNotifierAtCompletionOfSaveTask extends DefaultUserSessionListener
			implements ITaskFinishedListener
	{

		@Nullable private ITask m_candidateTask;
		@Nullable private ILogicDesign m_design;

		private ModelChangeNotifierAtCompletionOfSaveTask()
		{
		}

		@Override public void taskFinished(ITask task, boolean success, Object result, Throwable throwable)
		{
			//since the tasks are being created for each design and will be running in separate threads
			//we could end up in listeners being notified for unfinished tasks. we need to ignore them.
			//candidateTask null means the task got completed before being set on this listener.
			if (m_candidateTask != null && !m_candidateTask.equals(task)) {
				return;
			}
			((ICapletLifecycle) mLifeCycleListener).removeSaveCompleteListener(this);
			ICHSSystem chsSystem = FactoryMgr.getCHSSystem();
			IBoundaryTransactionMarshaller marshaller = chsSystem.getBoundaryTransactionMarshaller();
			IUserSession userSession = chsSystem.getUserSession();
			//we will further postpone this to user session listener when the transaction is exited. In a nutshell
			//we have to notify the model change event only after the design/diagram has reached to the database.
			//otherwise manager receives constraint violation exception related to transient usages.
			// LOGIC-14982 - resulting in automation failures.
			if (userSession != null && marshaller.isWithinBoundary()) {
				userSession.addListener(this);
			}
			else {
				notifyModelChangeEvent(success);
			}
		}

		@Override public void exitedTransaction(boolean success)
		{
			super.exitedTransaction(success);
			ICHSSystem chsSystem = FactoryMgr.getCHSSystem();
			IUserSession userSession = chsSystem.getUserSession();
			IBoundaryTransactionMarshaller marshaller = chsSystem.getBoundaryTransactionMarshaller();
			if (userSession != null && !marshaller.isWithinBoundary()) {
				userSession.removeListener(this);
				notifyModelChangeEvent(success);
			}
		}

		private void notifyModelChangeEvent(boolean success)
		{
			ICapletModel model = m_design != null ? getModel(m_design) : null;
			if (model != null && !model.isDestroyed() && success) {
				boolean isModified = model.isModified();
				try {
					model.notifyModelChange(new ModelChangeEvent(model, Collections.emptyList()));
				}
				finally {
					model.setModified(isModified);
				}
			}
		}

		public void saveDesign(@NotNull ILogicDesign design, IProject project,
				Map<IUID, Collection<IUID>> saveMap)
		{
			//hold the design rather than model. because the save will happen in
			//another thread and model could be destroyed when the save task is
			//finished and would cause issue during notification of model change event.
			m_design = design;
			BaseLifecycle baseLifecycle = (BaseLifecycle) mLifeCycleListener;
			baseLifecycle.addSaveCompleteListener(this);
			ITask saveTask = null;
			try {
				saveTask = baseLifecycle.save(project, saveMap, true);
			}
			finally {
				if (saveTask != null) {
					m_candidateTask = saveTask;
				}
				else {
					baseLifecycle.removeSaveCompleteListener(this);
				}
			}
		}
	}

	@Nullable
	private ISchemDiagram getDiagramToDisplay(List<ISchemDiagram> diagrams)
	{
		if (diagrams.isEmpty()) {
			return null;
		}

		Iterator<ISchemDiagram> iter = diagrams.iterator();
		ISchemDiagram diagramToDisplay = iter.next();
		while (iter.hasNext()) {
			ISchemDiagram currentDiagram = iter.next();
			if (diagramToDisplay.getDesign().getName().compareTo(currentDiagram.getDesign().getName()) >= 0) {
				if (diagramToDisplay.getName().compareTo(currentDiagram.getName()) >= 0) {
					diagramToDisplay = currentDiagram;
				}
			}
		}

		return diagramToDisplay;
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (isMultiUserMode) {
			MessageHelper.showInformationMessage(mMainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotCreate"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotCreate"));
		}
		else {
			// Handle the possibility that it has been deleted.
			if (checkDesignExists(project, design)) {
				if (new LockUpdateHelper((ICOGLockable) design, false).lockAndRefresh()) {
					return true;
				}
				else {
					showDesignLockedErrorMessage(design);
				}
			}
		}
		return false;
	}

	@Override @NotNull protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return appendEnvToResourceKeyRoot("BaseLifecycleDelegate.autoviewnewdiagram.lockfailed", project);
	}

	/**
	 * Construct the appropriate title for the dialog used for new design, diagram or design+diagram
	 *
	 * @return The title of the dialog
	 */
	protected String getNewDialogTitle()
	{
		// app name is deliberately not i18n - see dts0100394488
		return ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDiagramDialog.Title", mCaplet.getDesignType());
	}
}
