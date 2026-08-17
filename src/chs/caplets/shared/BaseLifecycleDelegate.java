/*
 * Copyright 2016-2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.caf.CAFProfilingKey;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.IWindowMgr;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.IntegrationSyncEvent;
import chs.caf.caplet.helpers.CapletLifecycleContext;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.ICustomLogicRegistry;
import chs.caf.caplet.helpers.IDefaultCustomLogicSupport;
import chs.caf.caplet.helpers.IStatusDelegator;
import chs.caf.caplet.helpers.IStatusHandler;
import chs.caf.caplet.helpers.LifecycleStatusHandler;
import chs.caf.caplet.helpers.NonIntrusiveCustomLogicRegistry;
import chs.caf.helpers.ui.std.UIManager;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.LockException;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cof.security.FunctionalPermissionMgr;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.common.IDesignContainer;
import chs.common.IUIDObject;
import chs.system.ICHSSystem;
import chs.utilities.CollectionUtils;
import chs.utilities.LifecycleUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.permission.PermissionHelper;
import chs.utility.DiagramApplicabilityCheck;
import chs.utility.DomainViolationMessageDisplay;
import chs.utility.helpers.LockHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.persist.DesignPersistenceUtils;
import chs.utility.persist.LockableHelper;
import chs.utility.task.ITask;
import chs.utility.task.TaskMgr;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseLifecycleDelegate implements IStatusDelegator, IDefaultCustomLogicSupport
{

	@NotNull protected final ILifeCycleChangeListener mLifeCycleListener;
	@NotNull protected final Frame mMainFrame;
	@NotNull private final Class<? extends DesignCapletLifecycleHelper> mResourceClass;
	@NotNull protected IStatusHandler mStatusHandler;
	@NotNull private ICustomLogicRegistry mCustomLogicRegistry;
	public List<Pair<Boolean, IBaseDiagram>> createNew(List<List<?>> contextList, int extra)
	{
		List<Pair<Boolean, IBaseDiagram>> extralist = new ArrayList<>();
		return extralist;
	}

	protected BaseLifecycleDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			@NotNull Class<? extends DesignCapletLifecycleHelper> resourceClass)
	{
		mLifeCycleListener = lifeCycleListener;
		mMainFrame = getDialogMainFrame();
		mResourceClass = resourceClass;
		mStatusHandler = new LifecycleStatusHandler();
		mCustomLogicRegistry = new NonIntrusiveCustomLogicRegistry();
	}

	protected Frame getDialogMainFrame()
	{
		return CAFUtils.getInstance().getWindowMgr().getDialogFrame();
	}

	@Override public <E extends Enum<E>, F extends Enum<F>> ICustomLogicRegistry<F> setCustomLogicSupport(
			@NotNull ICustomLogicRegistry<E> logicRegistry)
	{
		ICustomLogicRegistry<F> oldRegistry = (ICustomLogicRegistry<F>)mCustomLogicRegistry;
		mCustomLogicRegistry = logicRegistry;
		return oldRegistry;
	}

	@Override public <E extends Enum<E>> ICustomLogicRegistry<E> getCustomLogicSupport()
	{
		return mCustomLogicRegistry;
	}

	public IStatusHandler setStatusReporter(IStatusHandler statusHandler)
	{
		IStatusHandler oldHandler = mStatusHandler;
		mStatusHandler = statusHandler;
		return oldHandler;
	}

	public IStatusHandler getStatusReporter()
	{
		return mStatusHandler;
	}

	protected Class<? extends DesignCapletLifecycleHelper> getResourceClass()
	{
		return mResourceClass;
	}

	protected IFIB getFIB()
	{
		return CAFUtils.getInstance().getFIB();
	}

	@Nullable Model getModel(ILogicDesign logicDesign)
	{
		return ((BaseLifecycle) mLifeCycleListener).getModel(logicDesign);
	}

	@Nullable
	protected Model createModel(@NotNull IProject project, @NotNull ILogicDesign design, @NotNull ISchemDiagram diagram,
			@NotNull ICaplet mCaplet, @Nullable Model existingModel)
	{
		CAFUtils.getInstance().getOutputWindow()
				.sendDebugMessage("New Caplet Instance " + mCaplet.getName() + " created", true);
		//CAFUtils.getInstance().getOutputWindow().createCommentsTab("New Comments tab begin created",true,design.getFullName());

		// Make sure the connectivity and diagram are loaded into memory.
		Model model = existingModel;
		CAFUtils.getInstance().getScanningLock().obtainScanningLock();
		try {
			diagram.loadToMemory();
			//
			// Check the contents of the diagram/design
			//
			DiagramApplicabilityCheck.Info info = DiagramApplicabilityCheck.validate(diagram);
			//
			// not valid? - rewind and terminate the open.
			//
			if (info != null) {
				//
				// Remove the lock and display message to user.
				//
				design.unlock();
				//
				getStatusReporter().showErrorMessage(mMainFrame,
						ResourceMgr.getString(getResourceClass(), "BaseLifecycle.msghdr.cannotOpen"),
						ResourceMgr.getString(getResourceClass(), "BaseLifecycle.diagram.restrictedDataOnDiagram",
								info.getOffendingType()));
				return null;
			}

			model = mLifeCycleListener.createModel(project, design, diagram, existingModel);
		}
		finally {
			CAFUtils.getInstance().getScanningLock().releaseScanningLock();
		}
		return model;
	}

	/**
	 * @param project the owning project
	 * @param design the design to check
	 * @param diagram diagram to be locked
	 *
	 * @return boolean
	 */
	protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		boolean didLock = design.lock();
		if (design.isLocked()) {
			return true;
		}
		if (checkDesignExists(project, design)) {
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
							getStatusReporter().showErrorMessage(DesignCapletLifecycleHelper.class,
									"DesignCapletLifecycleHelper.diagram.cannotopendiagram.title",
									"DesignCapletLifecycleHelper.diagram.syncissue.title");
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
			return shouldAllowReadOnly(project, context, design, diagram, didLock);
		}
		return false;
	}

	protected boolean shouldAllowReadOnly(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram, boolean didLock)
	{
		String domainPermReason = checkDomainPerm(project, design);
		ILockInfo lockInfo = LockHelper.getLockInfo(design);

		boolean openReadOnly;
		if (domainPermReason == null && lockInfo != null) {
			openReadOnly = askOpenReadOnly(design, diagram, lockInfo);
		}
		else {
			DomainViolationMessageDisplay domainViolationMessageDisplay = new DomainViolationMessageDisplay();
			openReadOnly = domainViolationMessageDisplay.shouldOpenInReadOnlyMode(domainPermReason, design);
		}
		if (!openReadOnly) {
			setOpenResult(context, CapletLifecycleContext.OpenResult.USER_DECLINED);
		}
		if (openReadOnly && !didLock) {
			// We didn't lock the design earlier and so we didn't refresh it.  We now know we are going to open it
			// read only, so perform the refresh.
			if (isDesignDeleted(project, design)) {
				// Do we need to set the OpenResult here ??
				return false;
			}
			design.refresh();
		}
		return openReadOnly;
	}

	/**
	 * Is the project ot the design within the project deleted?
	 * <p>
	 * Should be getting the project from the design. Are there cases where the design project is not set?
	 * <p>
	 * @param project Project
	 * @param design Design that is owned by the project.
	 * @return true if either the project or the design is deleted.
	 */
	protected boolean isDesignDeleted(@NotNull IProject project, @NotNull ILogicDesign design)
	{
		return mLifeCycleListener.projectDeleted(project) || !checkDesignExists(project, design);
	}

	protected boolean askOpenReadOnly(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull ILockInfo designLockInfo)
	{
		boolean openReadOnly = getStatusReporter().isOpenReadOnly(getClass(), mMainFrame, getResourceKeyRoot(design.getProject()),
				CollectionUtils.createArray(getDesignLockUserNames(design, designLockInfo),
						designLockInfo.getTimeStamp()), null);
		return openReadOnly;
	}

	protected boolean askOpenReadOnlyForDiagramActions(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull ILockInfo designLockInfo)
	{
		boolean isDesignFullLocked = designLockInfo.getLockStatus() == ILockInfo.LockStatus.OTHER_USER_LOCKED;

		String[] implicationParams = null;
		String resourceKey = null;
		if (isDesignFullLocked) {
			implicationParams = CollectionUtils
					.createArray(getDesignLockUserNames(design, designLockInfo), designLockInfo.getTimeStamp());
			resourceKey = getResourceKeyRoot(design.getProject());
		}
		else if (designLockInfo.isWeakLocked() && diagram != null) {
			if (LogicConcurrencyHelper.isLogicInSingleUserMode(design.getProject())) {
				resourceKey = getResourceKeyRoot(design.getProject()) + ".diagramLock";
				implicationParams = CollectionUtils.createArray(getDesignLockUserNames(design, designLockInfo));
			}
			else {
				String userNames = getDiagramLockUserNames(diagram, designLockInfo);
				if (userNames != null) {
					resourceKey = getResourceKeyRoot(design.getProject()) + ".diagramLock";
					implicationParams = CollectionUtils.createArray(userNames);
				}
				else {
					resourceKey = getResourceKeyWhenUserNamesAreMissing();
					implicationParams = CollectionUtils.createArray(diagram.getName());
				}
			}
		}

		boolean openReadOnly = false;
		if (resourceKey != null && implicationParams != null) {
			openReadOnly =
					getStatusReporter().isOpenReadOnly(getClass(), mMainFrame, resourceKey, implicationParams, null);
		}
		return openReadOnly;
	}

	@NotNull protected String getResourceKeyWhenUserNamesAreMissing()
	{
		return "BaseLifecycleDelegate.openDiagram.lockFailed.multiuser.diagramLockByUnknownUser";
	}

	protected void showDesignLockedErrorMessage(@NotNull ILogicDesign design)
	{
		ILockInfo lockInfo = LockHelper.getLockInfo(design);
		if (lockInfo != null) {
			getStatusReporter().showErrorMessage(getClass(), getResourceKeyRoot(design.getProject()), CollectionUtils.createArray(
					getDesignLockUserNames(design, lockInfo), lockInfo.getTimeStamp()), null);
		}
	}

	@NotNull protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return "BaseLifecycleDelegate.editdesign.designweaklocked";
	}

	@NotNull protected String appendEnvToResourceKeyRoot(@NotNull String resourceKeyRoot, @Nullable IProject project)
	{
		if (LogicConcurrencyHelper.isLogicInMultiUserMode(project)) {
			return resourceKeyRoot + ".multiuser";
		}
		return resourceKeyRoot + ".singleuser";
	}

	@Nullable protected ILockInfo getLockInfo(@NotNull IUIDObject lockable)
	{
		ILockInfo lockInfo = null;
		try {
			lockInfo = CAFUtils.getInstance().getUserSession().getLockInfo(lockable.getUID().toString());
		}
		catch (UserSessionException e) {
			// write msg to log.
			System.out.println(e.aError);
			e.printStackTrace();
		}
		return lockInfo;
	}

	@NotNull protected String getDesignLockUserNames(@NotNull ILogicDesign design, @NotNull ILockInfo lockInfo)
	{
		List<ILockInfo> lockInfos = LockHelper.getWeakLockableLockInfo(design, lockInfo);
		return LockHelper.getLockUserNames(lockInfos);
	}

	@Nullable
	protected String getDiagramLockUserNames(@NotNull ISchemDiagram diagram, @NotNull ILockInfo designLockInfo)
	{
		ILockInfo lockInfo = designLockInfo;
		if (designLockInfo.isWeakLocked()) {
			lockInfo = getLockInfo(diagram);
			if (lockInfo != null && lockInfo.getLockStatus() == ILockInfo.LockStatus.NOT_LOCKED) {
				return null;
			}
		}
		return LockHelper.getLockUserNames(CollectionUtils.createListNoNulls(lockInfo));
	}

	protected boolean checkDesignExists(IProject project, @NotNull ILogicDesign design)
	{
		if (design.isNew() && !design.isDeleted()) {
			return true;
		}

		if (LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), design)) {
			return true;
		}
		mLifeCycleListener.designRemotelyDeleted(project, design);
		return false;
	}

	/**
	 * This checks to see if the reason that a plane couldn't be locked was because the user isn't a member of the right
	 * domain.
	 *
	 * @param project the owning project
	 * @param design Plane design that was attempted to be locked but it failed. //	 * @param readOnlyMsg Message
	 * presented to the user to query about opening a read-only container.
	 *
	 * @return Either the string for the domain permission error or null if this is not the case.
	 */
	@Nullable protected String checkDomainPerm(@NotNull IProject project, @NotNull IDesignContainer design)
	{
		LockException lockEx = design.getLockException();

		if (lockEx == null) {
			lockEx = project.getLockException();
			if (lockEx == null) {
				return null;
			}
		}

		String lockMsg = lockEx.getMessage();

		if (PermissionHelper.hasReadOnlyPermission(lockEx.aError) ||
				lockMsg.contains(PermissionHelper.DOMAIN_EXCEPTION_TAG)) {
			return PermissionHelper.getI18nReadonlyMessage(lockEx.aError);
		}
		// Unable to determin why the user cannot lock the design, present the default message that
		// was the current behavior.
		return null;
	}

	protected boolean haveReadOnlyModeOpen(@NotNull ILogicDesign design)
	{
		for (ISchemDiagram diag : design.getDiagrams()) {
			final Model model = getModel(design);
			if (model != null && !model.isEditable()) {
				if (model.containsDiagram(diag)) {
					return true;
				}
			}
		}
		return false;
	}

	protected void setOpenResult(@Nullable List<?> context, @NotNull CapletLifecycleContext.OpenResult openResult)
	{
		if (context != null) {
			CapletLifecycleContext lifecycleContext =
					LifecycleUtils.getContextObject(context, CapletLifecycleContext.class);
			if (lifecycleContext != null) {
				lifecycleContext.setOpenResult(openResult);
			}
		}
	}

	/**
	 * @param project the project
	 *
	 * @return true if any background save is running for the specified project
	 */
	protected boolean anyRunningSaveForProject(IProject project)
	{
		String taskId = DesignPersistenceUtils.getSaveTaskId(project);
		ITask projectSaveTask = TaskMgr.defaultTaskMgr().getRunningTask(taskId);
		return projectSaveTask != null;
	}

	protected Profiler startProfiling()
	{
		return ProfilingService.createAndStartProfiler(CAFProfilingKey.LIFECYCLE.getKeyName());
	}

	protected void stopAndLogProfiler(Profiler profiler, final String logKey)
	{
		ProfilingService.stopAndLogProfiler(profiler, logKey);
	}

	/**
	 * @param container the container
	 * @param restrictDesign avoid design
	 * @param restrictDiagram avoid diagram
	 * @param savealways always save
	 * @param runDRCs If true runs Design Rule Checks on all design objects
	 *
	 * @return ITask
	 */
	@Nullable
	protected ITask save(IUIDObject container, @Nullable IDesign restrictDesign,
			@Nullable ISchemDiagram restrictDiagram,
			SaveParameters saveParameters)
	{
		return ((ICapletLifecycle) mLifeCycleListener).save(container, restrictDesign,
				restrictDiagram != null ? Collections.singletonList(restrictDiagram) : null,
				saveParameters.getSaveAlways(), saveParameters.getRunDRCs());
	}

	// Calls designDeleted for side effects, ignores return value.
	protected void diagramRemotelyDeleted(IProject project, @Nullable List<?> context)
	{
		if (context != null) {
			getFIB().getProjectMgr().projectChildDeleted(project, context);
		}
		else {
			getFIB().getProjectMgr().projectChanged(project);
		}
		simulateWindowSwitching();      // nasty trigger for Integrator sync.
	}

	// To trigger Integrator sync action
	protected void simulateWindowSwitching()
	{
		IWindowMgr winMgr = CAFUtils.getInstance().getWindowMgr();
		if (winMgr instanceof UIManager) {
			((UIManager) winMgr).fireWindowChange(new IntegrationSyncEvent(winMgr));
		}
	}

	protected boolean checkPermission(@NotNull FunctionalPermissionEnum functionalPermissionEnum)
	{
		boolean hasPermission = hasPermission(functionalPermissionEnum);
		if (!hasPermission) {
			showFunctionalPermissionDeniedMessage(functionalPermissionEnum);
		}
		return hasPermission;
	}

	protected boolean hasPermission(FunctionalPermissionEnum permissionEnum)
	{
		ICHSSystem chsSystem = UtilsHelper.getCHSSystem();
		return chsSystem.getFunctionalPermissionMgr().hasPermission(permissionEnum);
	}

	/**
	 * Show a standard message for all design types used when a functional permission is denied.
	 * <p>
	 *
	 * @param functionalPermissionEnum Functional permission that is denied
	 */
	private void showFunctionalPermissionDeniedMessage(@NotNull FunctionalPermissionEnum functionalPermissionEnum)
	{
		final Frame dialogFrame = getDialogMainFrame();
		getStatusReporter().showPrivilegesMessage(dialogFrame,
				PermissionHelper.getInternationalisedName(FunctionalPermissionMgr.permissionEnumToString(
						functionalPermissionEnum)));
	}
}
