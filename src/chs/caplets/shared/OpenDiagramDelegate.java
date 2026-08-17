/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */
package chs.caplets.shared;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.scope.AnalysisNetlistScopeFactory;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.IWindowMgr;
import chs.caf.LifeCycleCacheUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.helpers.CapletLifecycleContext;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.DiagramStyleSetValidityChecker;
import chs.caf.caplet.helpers.OpenDiagramCustomLogicRegistry;
import chs.caf.caplet.helpers.graphics.FilterControlMgr;
import chs.caf.helpers.ui.common.ProjectAndSymbolTreeNodeIconProvider;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.IDesignLockStrategy;
import chs.caplets.logic.IndicatorRefresher;
import chs.caplets.logic.Model;
import chs.caplets.logic.View;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.draw.Grid;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.table.ITableData;
import chs.cof.drawplus.table.ITeamPlayNotesTableData;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IAnalysisBuildList;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.IBuildListIterator;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.cof.topology.IBaseTopologyDesign;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.ICHSIterator;
import chs.common.IGuard;
import chs.common.IIncLoadable;
import chs.common.RefreshStatusEnum;
import chs.common.preferencesets.IPreferenceSet;
import chs.common.preferencesets.IReadOnlyStyleSetInfoHolder;
import chs.common.validation.ValidationException;
import chs.common.validation.ValidationHelper;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.subsystem.immersedapp.IControllerSelectionSyncService;
import chs.subsystem.immersedapp.IImmersedViewService;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.utilities.LifecycleUtils;
import chs.utilities.NullableObjectWrapper;
import chs.utilities.ResourceMgr;
import chs.utilities.WrappingRuntimeException;
import chs.utility.DesignLockHelper;
import chs.utility.IUserAccessNotifier;
import chs.utility.UserAccessNotifier;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicDesignAssociationChecker;
import chs.utility.helpers.UtilsHelper;
import chs.utility.persist.promise.IPromise;
import chs.utility.persist.promise.PromiseFactory;
import chs.utility.persist.promise.ResponseSize;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.task.TaskMgr;
import chs.utility.ui.BuildListSelectorPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenDiagramDelegate extends BaseLifecycleDelegate
{

	protected ICaplet mCaplet;
	private String mDesignTagXML;
	private boolean mUpdateXRefOnReadOnly;
	private int mGridSpace;
	private boolean openAsReadOnly;

	public boolean isOpenAsReadOnly()
	{
		return openAsReadOnly;
	}

	public void setOpenAsReadOnly(boolean openAsReadOnly)
	{
		this.openAsReadOnly = openAsReadOnly;
	}

	public OpenDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener, ICaplet caplet,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, String designTagXML,
			boolean updateXRefOnReadOnly, int gridSpace)
	{
		super(lifeCycleListener, resourceClass);
		mCaplet = caplet;
		mDesignTagXML = designTagXML;
		mUpdateXRefOnReadOnly = updateXRefOnReadOnly;
		mGridSpace = gridSpace;
	}

	public boolean openExisting(List<?> context, boolean showAltBuildListDlg)
	{
		// Open a schematic diagram
		// The context should have a project a design and a diagram, and it
		// could also have a specific sheet to open.

		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		ILogicDesign design = DesignCapletLifecycleHelper.getContextDesignContainer(context, ILogicDesign.class);
		ISchemDiagram diagram = LifecycleUtils.getContextObject(context, ISchemDiagram.class);

		final String titleCannotOpenDiagram = "BaseLifecycle.diagram.cannotopendiagram.title";
		final Class<BaseLifecycle> resourceClass = BaseLifecycle.class;
		if (project == null) {
			getStatusReporter().showErrorMessage(resourceClass, titleCannotOpenDiagram,
					"BaseLifecycle.diagram.noProject.message");
			return false;
		}
		if (design == null) {
			getStatusReporter()
					.showErrorMessage(resourceClass, titleCannotOpenDiagram,
							"BaseLifecycle.diagram.noDesign.message");
			return false;
		}

		if (mLifeCycleListener.projectDeleted(project)) {
			return false;
		}

		if (!checkDesignExists(project, design)) {
			return false;
		}

		if (diagram == null) {
			// No sheet specified, so just open the first one.
			// We could put up a dialog here and let the user
			// pick which sheet to open if there are more than one.
			ICHSIterator<ISchemDiagram> diagramIter = design.getDiagrams();
			diagram = diagramIter.getNext();
			if (diagram == null) {
				getStatusReporter().showErrorMessage(resourceClass, titleCannotOpenDiagram,
						"BaseLifecycle.diagram.noDiagram.message");
				return false;
			}
		}

		// If there is a save operation is in progress for this project, we can allow to open only fully loaded diagrams
		boolean isSaveRunning = anyRunningSaveForProject(project);
		if (isSaveRunning && diagram instanceof IIncLoadable) {
			if (((IIncLoadable) diagram).isSkeleton()) {
				getStatusReporter().showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						ResourceMgr.getString(resourceClass, titleCannotOpenDiagram),
						ResourceMgr.getString(resourceClass,
								"BaseLifecycle.diagram.saveinProgress.message", diagram.getName()));
				return false;
			}
		}

		if (!isSaveRunning) {
			//PDVC-915 -- If Integrator Plane is open in MU mode and associated logic design has been modified
			// in other session, we should not refresh the logic design from Integrator plane.
			if (!new LogicDesignAssociationChecker(design).execute()) {
				design.refresh();
			}
			if (diagram.isDeleted()) {
				diagramRemotelyDeleted(project, context);
				return false;
			}
		}

		//FEAT00011755 -check if the design domain is available for this user
		IUserAccessNotifier notifier = new UserAccessNotifier();
		if (!DesignCapletLifecycleHelper.ValidateDesignAccess(design, notifier)) {
			return false;
		}
		// else generated or released designs can be opened read only even if the style set is not valid.

		// Create a new Model, then create a view on it.
		ISchemDiagram finalDiagram = diagram;
		List<IBaseDiagram> diagrams =
				CAFUtils.getInstance().getOpenDiagrams().stream().filter(d -> d.getUID() == finalDiagram.getUID())
						.collect(
								Collectors.toList());
		boolean isDiagramModelOpenedAlready = diagrams.contains(diagram);
		Model model = openDiagramWithValidationFailureSupport(context, showAltBuildListDlg, project, diagram);
		if (model == null) {
			CreationDeletionHelper.getTheCreationHelper().clearNewObjects();
			return false;
		}

		postOpenDiagram(context, design, diagram, model);
		if (!isDiagramModelOpenedAlready) {
			ImmersedAppServices.getService(IControllerSelectionSyncService.class)
					.register(model.getController());
			DiagramAuditTrialHelper.getInstance().postDiagramAuditTrail(diagram, AuditableEventType.DIAGRAM_OPENED);
		}
		return true;
	}

	private void postOpenDiagram(@Nullable List<?> context, ILogicDesign design, ISchemDiagram diagram, Model model)
	{
		// Update the border properties. updateBorderProperties() will check if the design is editable before
		// updating the properties
		boolean borderPropertiesUpdated = diagram.updateBorderProperties();
		if (model.isEditable()) {
			if (borderPropertiesUpdated) {
				// If the border properties have been updated, set the modified flag of the model.
				// This will prompt the user to save the design - dts0100779915.
				String message = ResourceMgr
						.getString(ICapletLifecycle.class, "Lifecycle.message.borderPropertiesUpdated",
								design.getFullName() + ":" + diagram.getName());
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
				model.setModified(true);
			}
			setOpenResultWhenUndefined(context, CapletLifecycleContext.OpenResult.SUCCEEDED);
			notifyManagerForBackup(design);
		}
		else {
			setOpenResultWhenUndefined(context, CapletLifecycleContext.OpenResult.READ_ONLY);
			//dts0100809185  SYSTEM TESTING - Icons are not grayed out when user open a diagram in read only mode in absence of style set
			tickleUI(diagram);
		}

		// FEAT3184 - Object Model Integrity
		// we should validate the diagram that was loaded. Validate usages as well
		ValidationHelper.validateAfterLoad(diagram, design.getSharedUsageMgr());
	}

	private void setOpenResultWhenUndefined(@Nullable List<?> context, CapletLifecycleContext.OpenResult succeeded)
	{
		CapletLifecycleContext lifecycleContext =
				context != null ? LifecycleUtils.getContextObject(context, CapletLifecycleContext.class) : null;
		if (lifecycleContext != null && lifecycleContext.getOpenResult() == null) {
			lifecycleContext.setOpenResult(succeeded);
		}
	}

	private void notifyManagerForBackup(ILogicDesign design)
	{
		IUserSession managerConnection = UtilsHelper.getCHSSystem().getData();
		// check that we aren't in local mode
		if (managerConnection != null) {
			if (!TaskMgr.defaultTaskMgr().hasRunningTasks()) {
				managerConnection.openedDiagram(mDesignTagXML, design.getUID().getString());
			}
		}
	}

	@Nullable private Model openDiagramWithValidationFailureSupport(List<?> context, boolean showAltBuildListDlg,
			IProject project, ISchemDiagram diagram)
	{
		try {
			return (Model) openDiagram(context, project, diagram, showAltBuildListDlg);
		}
		catch (ValidationException exception) {
			mLifeCycleListener.diagramValidationFailed(exception, diagram.getDesign());
		}
		return null;
	}

	/**
	 * Opens a diagram. Create a model and a view for the diagram.
	 *
	 * @param context Optional Lifecycle style 'context' for passing back reason for failure to clients, e.g the user
	 * declined to open a diagram read-only when prompted
	 * @param project the opening diagram project
	 * @param diagram the diagram to open
	 * @param showAltBuildListDlg If true may prompt the user to switch to a different active build list
	 *
	 * @return The new model.
	 */
	@Nullable
	public ICapletModel openDiagram(@Nullable List<?> context, @NotNull IProject project,
			@NotNull ISchemDiagram diagram, boolean showAltBuildListDlg)
	{

		ILogicDesign design = diagram.getDesign();
		boolean isDiagramFullyLoaded = diagram.isFullyLoaded();
		assert design != null;

		// ensure the design Mgr list of designs and folder mgr are both up-to-date
		refreshDesignMgrPrerequisites(project);
		if (project.refreshDesignListAndFolderMgr() == RefreshStatusEnum.eRefreshed) {
			getFIB().getProjectMgr().projectChanged(project);
		}

		Model model = createNewSchematicModel(context, project, design, diagram);
		if (model != null) {
			if (isDiagramFullyLoaded) {
				refreshTeamPlayNotesData(diagram);
			}
			if (!createView(model, project, design, diagram, showAltBuildListDlg)) {
				return null;
			}
		}
		return model;
	}

	private void refreshTeamPlayNotesData(@NotNull ISchemDiagram diagram)
	{
		final ITableData tableData = diagram.getTableData(ITeamPlayNotesTableData.class);
		if (tableData != null) {
			// If the diagram was already loaded (when copied/revised) at the time of opening the
			// diagram, the table data does not have the latest team play data as it is saved after
			// importing and saving the design (ASSETS-9496)
			tableData.fireTableDataChanged();
		}
	}

	@Nullable public Model createNewSchematicModel(@Nullable List<?> context, @NotNull IProject project,
			@NotNull ILogicDesign design, @NotNull ISchemDiagram diagram)
	{
		final boolean designIsEditable = design.isEditable();
		if (!designIsEditable) {
			// hook to decide if we need to continue opening diagram. Result.DONOTBYPASS will be returned by No Op.
			if (executeWithReturn(Result.DONOTBYPASS, OpenDiagramCustomLogicRegistry.ID.DECIDEOPEN_ONNONEDITABLEDIAGRAM)
					== Result.BYPASS) {
				// NOTE: we are using non I18N strings here because they are never shown to the user, however they
				// are recorded for the Inline Assist case so are useful for debugging.
				getStatusReporter().showInformationMessage(mMainFrame,
						"Diagram cannot be opened",
						"The diagram cannot be opened because the design is read only.");
				return null;
			}
		}
		//
		// If it's not a new design, we need to get a lock on behalf of the diagram.
		// Prompt user to open ready only, if existing locks exists. When we are deleting a diagram we dont want to be
		// warned about buildlist, as that only applies to designs.
		//
		//try to lock the design even if the diagram exists in model.
		if (!design.isLocked()) {
			ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
			if (cw == null) {
				//try this only if diagram is not already open.
				boolean designCanBeOpened;
				// dts0101275951 - Regression When a release level that does not allow design edits is assigned to a
				// Capital Logic design and the user opens the design it opens in read only but a lock gets put on that
				// design for that user account.
				// In the flow where read-only designs are not bypassing via executeWithReturn we will hit this code.
				// We do not want to get a lock on designs that are read-only for this user.
				if (designIsEditable) {
					designCanBeOpened = processDesignLocking(project, context, design, diagram);
				}
				else {
					// No permissions, domains or release levels can prevent a user opening a design read-only. So just
					// check that the design exists.
					designCanBeOpened = !isDesignDeleted(project, design);
				}

				// hook to decide if we need to continue opening diagram. Result.DONOTBYPASS will be returned by No Op.
				if (executeWithReturn(Result.DONOTBYPASS,
						OpenDiagramCustomLogicRegistry.ID.DECIDEOPEN_ONLOCKEDDIAGRAM)
						== Result.BYPASS) {

					// hook to record that we have a lock currently.
					execute(OpenDiagramCustomLogicRegistry.ID.RECORDDIAGRAMLOCKEDSTATUS, designCanBeOpened);
					return null;
				}

				if (!designCanBeOpened) {
					return null;
				}
			}
		}
		else {
			// hook to record who locked the diagram
			execute(OpenDiagramCustomLogicRegistry.ID.RECORDLOCKOWNER, diagram);
			// hook to record that we have a lock currently.
			execute(OpenDiagramCustomLogicRegistry.ID.RECORDDIAGRAMLOCKEDSTATUS, true);
		}

		Model model = mLifeCycleListener.findModel(design);
		if (model != null && model.containsDiagram(diagram)) {
			model.setCurrentDiagram(diagram);
			return model;
		}

		// If diagram is not loaded in memory then it should be an existing diagram ?
		if (diagram.isDeleted()) {
			diagramRemotelyDeleted(project, context);
			return null;
		}

		return createModel(project, design, diagram, mCaplet, model);
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		Boolean doOpenReadOnly = checkDesignLockingForTopoMU(project, context, design, diagram);
		if (doOpenReadOnly != null) {
			return doOpenReadOnly;
		}
		boolean wasDesignLocked = IDesignLockStrategy.isLocked(design);
		boolean didLock = false;
		if (!isOpenAsReadOnly()) {
			didLock = IDesignLockStrategy.getLockStrategy(design).acquireLock(diagram);
		}
		if (IDesignLockStrategy.isLocked(diagram)) {
			// hook to record who locked the diagram
			execute(OpenDiagramCustomLogicRegistry.ID.RECORDLOCKOWNER, diagram);
			return true;
		}
		if (checkDesignExists(project, design)) {

			if (!wasDesignLocked && IDesignLockStrategy.isLocked(design)) {
				IDesignLockStrategy.releaseLock(design);
			}

			IUserSession managerConnection = UtilsHelper.getCHSSystem().getData();
			// check that we aren't in local mode
			if (managerConnection != null) {
				try {
					if (managerConnection.objectModified(design.getObjType(), design.getUID().getString(),
							design.getTimeModified()) != 0) {
						//
						// Design is out of date - Better
						//
						boolean haveReadOnlyModelOpen = haveReadOnlyModeOpen(design);
						if (haveReadOnlyModelOpen) {
							String header = ResourceMgr
									.getString(DesignCapletLifecycleHelper.class,
											"DesignCapletLifecycleHelper.diagram.cannotopendiagram.title");
							String msg =
									ResourceMgr.getString(DesignCapletLifecycleHelper.class,
											"DesignCapletLifecycleHelper.diagram.syncissue.title");

							getStatusReporter().showErrorMessage(mMainFrame, header, msg);
							return false;
						}
					}
				}
				catch (UserSessionException use) {
					throw new WrappingRuntimeException(use);
				}
			}

			// If the reason is because of domain problems, trump the current bahavior and provide a custom
			// message.
			boolean openReadOnly = shouldAllowReadOnly(project, context, design, diagram, didLock);
			if (openReadOnly) {
				//This is to make sure that diagram should open in readonly mode when chosen to do so
				executeWithReturn(Result.DONOTBYPASS, OpenDiagramCustomLogicRegistry.ID.DECIDEOPEN_ONNONEDITABLEDIAGRAM,
						true);
			}
			return openReadOnly;
		}
		return false;
	}

	@Nullable protected Boolean checkDesignLockingForTopoMU(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		assert diagram != null;
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		NullableObjectWrapper<IBaseTopologyDesign> outOfSyncDesign = new NullableObjectWrapper<>();

		if (isMultiUserMode) {
			boolean designIsAssociated =
					DesignLockHelper.hasAssociatedTopoDesignInWeakLock(design, outOfSyncDesign);
			// If this design is associated with the plane open in MU mode then we can open it read-only, or not
			// at all if the design is out of sync
			if (designIsAssociated) {
				if (outOfSyncDesign.isPresent()) {
					getStatusReporter().showInformationMessage(mMainFrame,
							ResourceMgr.getString(DesignCapletLifecycleHelper.class,
									"DesignCapletLifecycleHelper.msghdr.cannotOpen"),
							ResourceMgr.getString(DesignCapletLifecycleHelper.class,
									"DesignCapletLifecycleHelper.msg.cannotOpen.associated"));
					setOpenResult(context, CapletLifecycleContext.OpenResult.MU_MODE_ASSOCIATED_MODIFIED);
					return false;
				}
			}
			else {
				// If the design is not associated, we can open it read-only, or not at all if the shared object
				// manages need refresh
				if (isSharedObjRefreshNeeded(project)) {
					getStatusReporter().showInformationMessage(mMainFrame,
							ResourceMgr.getString(DesignCapletLifecycleHelper.class,
									"DesignCapletLifecycleHelper.msghdr.cannotOpen"),
							ResourceMgr.getString(DesignCapletLifecycleHelper.class,
									"DesignCapletLifecycleHelper.msg.cannotOpen.shared"));
					setOpenResult(context, CapletLifecycleContext.OpenResult.MU_MODE_SHARED_OBJECTS_MODIFIED);
					return false;
				}
			}
			boolean doOpenReadOnly = getStatusReporter().showYesNoDialog(mMainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotOpenDiagram"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotOpenDiagram")) == JOptionPane
					.YES_OPTION;
			if (!doOpenReadOnly) {
				setOpenResult(context, CapletLifecycleContext.OpenResult.USER_DECLINED);
			}
			return doOpenReadOnly;
		}
		return null;
	}

	protected boolean askOpenReadOnly(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull ILockInfo designLockInfo)
	{
		return isOpenAsReadOnly() || askOpenReadOnlyForDiagramActions(design, diagram, designLockInfo);
	}

	@NotNull protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return appendEnvToResourceKeyRoot("BaseLifecycleDelegate.opendiagram.lockfailed", project);
	}

	public boolean isSharedObjRefreshNeeded(@NotNull IProject project)
	{
		return project.getSharedConductorMgr().needsRefresh() || project.getSharedPinListMgr().isUpdatedOnManager();
	}

	private void refreshDesignMgrPrerequisites(IProject project)
	{
		// refresh ReleaseLevel, Option and DesignAbstraction Mgrs before we refresh the designs. Design may reference these(dts0100801790)
		IPromise promise = PromiseFactory.createPromise();
		promise
				.requestRefreshOf(project.getReleaseLevelMgr(), ResponseSize.SMALL)
				.requestRefreshOf(project.getDesignAbstractionMgr(),ResponseSize.SMALL)
				.requestRefreshOf(project.getOptionMgr(), ResponseSize.SMALL)
				.issue()
				.thenApply(() -> {
					project.getReleaseLevelMgr().refresh();
					project.getDesignAbstractionMgr().refresh();
					project.getOptionMgr().refresh();
				});
	}

	private void tickleUI(ISchemDiagram diagram)
	{
		CAFUtils.getInstance().tickleUI(getFIB());
		ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
		if (cw != null) {
			String title = LogicCapletUtils.getDiagramTitle(diagram, true);
			cw.setTitle(title);
		}
	}

	protected boolean createView(@NotNull Model model, @NotNull IProject project, @NotNull ILogicDesign design,
			@NotNull ISchemDiagram diagram, boolean showAltBuildListDlg)
	{
		//
		// If the diagrams design is not in the active build list, prompt to switch over.
		//
		checkActiveBuildList(project, design, showAltBuildListDlg);
		model.getController().getCaplet().getLifecycle()
					.setDiagramTabIcon(ProjectAndSymbolTreeNodeIconProvider.getProjectIcon(diagram, project, null));
		ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(diagram);
		if (cw == null) {
			IFIB fib = getFIB();
			IWindowMgr windowMgr = fib.getWindowMgr();
			if (!ensureStyleSetIsValid(diagram, design, model)) {
				if (((IPrivilegedCOGManagedLockableChildrenContainer) design).isWeakLocked() && diagram.isLocked()) {
					diagram.unlock();
				}
				mLifeCycleListener.removeDiagramFromModel(diagram);
				return false;
			}

			cw = doOpenDiagramWindow(project, design, diagram, model, windowMgr, () -> {

				// add a listener for analysis ( model changes trigger dynamic
				// resimulation )
				if (CapitalAnalysisFactory.getAnalysisInterface() != null) {
					model.addModelChangeListener((IModelChangeListener) LogicAnalysisServices.getAnalysisServices());
				}
			});
			PreferenceSetHelper.logArchivedStyleSetWarning(diagram);
		}
		else {
			String newTitle = LogicCapletUtils.getDiagramTitle(diagram, !model.isEditable());
			cw.setTitle(newTitle);
			cw.activate();  // activeate window and bring it to the front
		}

		if (!design.isUnderConcurrentEdit()) {
			boolean bModelChanged = model.isModified();
			CAFUtils.getInstance().updateWindowTitlesForDesign(design, false);
			model.setModified(bModelChanged);
		}

		ICapletController controller = cw.getController();
		if (controller != null) {
			//Logic-16429: Create Analysis Toolbar at the time user clicks Analysis tab
			// to avoid performance degradation due to computing build lists in Analysis scope at the time we open diagram
			//To fix dts0100733491 - Shared Tab Icons in the browser tree are not always displayed
			((BaseController) controller).createSharedTabToolbar();
			((BaseController) controller).createDesignBrowserToolbar();
		}
		return true;
	}

	protected void checkActiveBuildList(@NotNull IProject project, @NotNull ILogicDesign design,
			boolean showAltBuildListDlg)
	{
		final IBuildListMgr blm = project.getBuildListMgr();
		if (blm != null && showAltBuildListDlg) {
			IBuildList active = blm.getActiveBuildList();
			if (active != null && !active.containsDesignUID(design.getUID())) {
				//
				// Not in active build list - prompt to switch to a BL that is OK.
				//
				List<IBuildList> buildLists = new ArrayList<IBuildList>();
				for (IBuildListIterator itr = blm.getBuildLists(); itr.hasNext(); ) {
					IBuildList bl = itr.next();
					if (bl.containsDesignUID(design.getUID())) {
						buildLists.add(bl);
					}
				}
				final BuildListSelectorPanel blPanel = new BuildListSelectorPanel(buildLists, blm.getActiveBuildList());
				String dialogTitle = ResourceMgr.getString(getResourceClass(), "Lifecycle.notInActiveBuildList.title");
				final CAFOkCancelDialog bldialog = getNotInActiveBuildListDialog(dialogTitle);

				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout(4, 4));

				JPanel labelPanel = new JPanel();
				labelPanel.setLayout(new BorderLayout());
				labelPanel.add(new JLabel(ResourceMgr.getString(getResourceClass(),
						"Lifecycle.notInActiveBuildList.description", design.getName()), SwingConstants.CENTER),
						BorderLayout.CENTER);
				panel.add(labelPanel, BorderLayout.NORTH);
				panel.add(blPanel, BorderLayout.CENTER);
				bldialog.getContentPane().add(panel, BorderLayout.CENTER);
				bldialog.pack();

				bldialog.getOkButton().addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						bldialog.setVisible(false);
						IBuildList sel = blPanel.getSelectedItem();
						blm.setActiveBuildList(sel);
						if (sel instanceof IAnalysisBuildList &&
								CapitalAnalysisFactory.getAnalysisInterface() != null) {
							AnalysisServices
									.setCurrentAnalysisNetlistScope(AnalysisNetlistScopeFactory.createScope(sel),
											blm.getProject().getUID().getString());
						}
						CAFUtils.getInstance().getCAFProjectMgr()
								.projectChanged(CAFUtils.getInstance().getCurrentProject());
					}
				});

				bldialog.getCancelButton().addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						bldialog.setVisible(false);
					}
				});
				bldialog.setVisible(true);
			}
		}
	}

	@NotNull protected CAFOkCancelDialog getNotInActiveBuildListDialog(final String dialogTitle)
	{
		return new NotInBuildListDialog(dialogTitle);
	}

	public static class NotInBuildListDialog extends CAFOkCancelDialog
	{

		public NotInBuildListDialog(String dialogTitle)
		{
			super(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), dialogTitle, true);
		}

		@NotNull @Override public String getHelpID()
		{
			return "chs.caf.cafmain.actions.SetAlternativeActiveBuildListAction"; // Not a class, but it's close...
		}
	}

	public void openDiagramOnMove(ISchemDiagram diagram)
	{
		ILogicDesign design = diagram.getDesign();
		if (design != null) {
			Model model = getModel(design);
			if (model != null) {
				model.addDiagram(diagram);
				diagram.setWithModel(true);
				IProject project = design.getProject();
				if (project != null) {
					setDrawGridSpacing(diagram, mGridSpace);
					// Create the window first and set the layout
					IWindowMgr windowMgr = getFIB().getWindowMgr();
					if (!ensureStyleSetIsValid(diagram, design, model)) {
						mLifeCycleListener.removeDiagramFromModel(diagram);
						return;
					}
					doOpenDiagramWindow(project, design, diagram, model, windowMgr, () -> {
					});
					postOpenDiagram(null, design, diagram, model);
				}
			}
		}
	}

	private void setDrawGridSpacing(ISchemDiagram diagram, int drawGridSpacing)
	{
		IGrid grid = diagram.getGrid();
		if (grid != null) {
			IGrid subGrid = grid.getSubGrid();
			if (subGrid == null) {
				subGrid = new Grid();
			}
			subGrid.setGridSpacing((grid.getGridSpacing() * drawGridSpacing) / 100);
		}
	}

	@NotNull private ICapletWindow doOpenDiagramWindow(IProject project, ILogicDesign design, ISchemDiagram diagram,
			Model model, IWindowMgr windowMgr, Runnable analysisActivity)
	{
		LifeCycleCacheUtils.addToCache(diagram);
		// Create the window first and set the layout
		ICapletWindow cw = windowMgr.createCapletWindow(mCaplet, model.getController());
		String title = LogicCapletUtils.getDiagramTitle(diagram, !model.isEditable());
		adjustModifiedFunction(cw, diagram);
		cw.setTitle(title);
		cw.getContainer().setLayout(new GridLayout(1, 1, 4, 4));

		// Create the view and tell it about the sheet
		View dv = new View(model, cw);
		dv.setDiagram(diagram);
		dv.setName(title);

		analysisActivity.run();

		if (model.isEditable() || mUpdateXRefOnReadOnly) {
			//
			// Regenerate the cross references for THIS design only.
			//
			project.getCrossReferenceMonitor().generateCrossReferencesForDesign(design);
		}

		FilterControlMgr.getInstance().filterDiagram(diagram);
		removeOrphanIndicators(diagram, design);
		// Set the new window as active
		cw.display();
		return cw;
	}

	/**
	 * QPE-16670 - ST181BashXSPEDSI5:unsaved changes by asterisk is shown on readonly diagram in MU mode
	 * <p>
	 * Adjust the modified function used to determine if the diagram is modified
	 *
	 * @param capletWindow - the caplet window
	 * @param diagram - the diagram within the window
	 */
	protected void adjustModifiedFunction(ICapletWindow capletWindow, ISchemDiagram diagram)
	{
		if (LogicConcurrencyHelper.isLogicInMultiUserMode(diagram.getProject())) {
			capletWindow.setCapletWindowModifiedFunction((t) -> {
				if (!t.isModelModified()) {
					return false;
				}
				return diagram.isLocked() && diagram.isModified();
			});
		}
	}

	private void removeOrphanIndicators(ISchemDiagram diagram, ILogicDesign design)
	{
		try {
			design.beginLocalEdit();
			// CARCH-1291 - changed the creation deletion helper guard
			try (IGuard ignored = CreationDeletionHelper.createDisableCreationDeletionHelperInThreadGuard()) {
				IndicatorRefresher.getIndicatorRefresher(diagram).removeIndicators();
			}
		}
		finally {
			design.endLocalEdit();
		}
	}

	protected boolean ensureStyleSetIsValid(ISchemDiagram diagram, ILogicDesign design, Model model)
	{
		if ((design.isLocked() || ((IPrivilegedCOGManagedLockableChildrenContainer) design).isWeakLocked()) &&
				design.isEditable()) {
			Map<IReadOnlyStyleSetInfoHolder, String> unAvailableStyleSets =
					new LinkedHashMap<IReadOnlyStyleSetInfoHolder, String>();
			// If the diagram is not already opened and we are openeing it now then we need to check if the style
			// set is valid
			DiagramStyleSetValidityChecker styleSetLicenseChecker =
					getDiagramStyleSetValidityChecker(design, diagram, unAvailableStyleSets);
			if (!styleSetLicenseChecker.isCanOpenDiagram()) {
				CAFUtils.getInstance().setDiagramNavCancelledIfInvokedFromHomePage(true);
				return false;
			}
			@Nullable IPreferenceSet substituteStyleSet = styleSetLicenseChecker.getSubstituteStyleSet();
			if (substituteStyleSet != null) {
				assert unAvailableStyleSets.keySet().contains(diagram);
				// Now we have the design locked, change the style set used by the diagram and apply that styling.
				diagram.setPreferenceSetName(substituteStyleSet.getName());
				// Don't automatically restyle the objects in the diagram, the user can use Apply Style if they wish.

				// The change is not logged as undoable so we must persist it now.
				((ICapletLifecycle) mLifeCycleListener).foregroundSave(design, false);

				model.ensureStyleSetValidity();
			}
		}
		return true;
	}

	@NotNull
	protected DiagramStyleSetValidityChecker getDiagramStyleSetValidityChecker(ILogicDesign design,
			ISchemDiagram diagram,
			Map<IReadOnlyStyleSetInfoHolder, String> unAvailableStyleSets)
	{
		return DiagramStyleSetValidityChecker
				.checkStyleSetValidity(design, diagram, unAvailableStyleSets, mMainFrame);
	}
}
