/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.ai.designsummarizer.EditDesignDialogAiExtension;
import chs.bridges.TeamCenterReleaseLevelController;
import chs.bridges.webservices.exceptions.WebServiceClientException;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.helpers.CapletLifecycleContext;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.PropTextSyncDesignCleaner;
import chs.caf.helpers.ui.std.UIManager;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.UnfreezeMessageWrapper;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.project.effectivity.EffectivityModelUpdater;
import chs.cof.project.folder.IFolderMgr;
import chs.cof.project.folder.IPrivilegedFolderMgr;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IDesignAbstraction;
import chs.common.IProperty;
import chs.common.IReleaseLevel;
import chs.common.IUID;
import chs.common.IUpgradeableDesignContainer;
import chs.ctf.caf.ui.DesignEditDialog;
import chs.ctf.caf.ui.DesignInfoDialog;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.ctf.caf.utils.FreezeHelper;
import chs.ctf.caf.utils.batchUnfreeze.UnFreezeSharedObjectsDialog;
import chs.ctf.caf.utils.batchUnfreeze.UnFreezeSharedObjectsScope;
import chs.ctf.caf.utils.batchUnfreeze.UnfreezeHelper;
import chs.ctf.ui.effectivity.EffectivityHelper;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DomainChecker;
import chs.utility.IUserAccessFromPlugin;
import chs.utility.IUserAccessNotifier;
import chs.utility.PluggableDomainChecker;
import chs.utility.UserAccessNotifier;
import chs.utility.helpers.ReleaseLevelHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.persist.DesignPersistenceUtils;
import chs.utility.persist.ProjectStorageHelper;
import chs.utility.persist.ServerUpdateHelper;
import com.mentor.capital.javafx.DesignAbstractionInfo;
import com.mentor.capital.javafx.VisibilityManager;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EditDesignDelegate extends EditLifecycleDelegate
{

	protected static final boolean NOT_COPY = false;
	protected static final boolean NOT_REVISION = false;
	protected static final boolean NOT_EVALUATION = false;
	protected static final boolean NOT_NEW = false;

	public EditDesignDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, FunctionalPermissionEnum editDesignPermission)
	{
		super(lifeCycleListener, resourceClass, editDesignPermission);
	}

	protected DesignEditDialog getDesignEditDialog(IProject project, ILogicDesign design, boolean isDesignChanged,
			boolean readonly, String title, Frame mainFrame) throws NoReleaseLevelsException
	{
		return ((BaseLifecycle) mLifeCycleListener)
				.createEditDialog(mainFrame, title, project, design, isDesignChanged, false, false, readonly,
						new TeamCenterReleaseLevelController());
	}

	public boolean editDesign(@NotNull IProject project, ILogicDesign design, List<?> context)
	{
		UtilsHelper.refreshDesignsIfNeeded(design);
		Consumer<IUserAccessNotifier> domainErrorMsgDisplayer = notifierConsumer -> {
			ResourceBasedMessageContent messageContent =
					new ResourceBasedMessageContent(BaseLifecycle.class,
							"BaseLifecycle.diagram.cannotEditDesign");
			messageContent.setMessageParameters(notifierConsumer.getMessage());
			Message.show(PromptSeverity.ERROR, messageContent);
		};
		IUserAccessNotifier notifier = new UserAccessNotifier();
		if (!isAllowedToEdit(design, notifier, domainErrorMsgDisplayer)) {
			return false;
		}

		//PDVC-2496
		if (!isAllowedToOpen(design)) {
			return false;
		}

		// If design is modified we need to set the flag
		boolean isModelModified = false;
		Model model = getModel(design);
		if (model != null && model.isModified()) {
			isModelModified = true;
		}

		IFolderMgr folderMgr = null;
		boolean designAlreadyLocked = design.isLocked();
		boolean releaseLevelChanged = false;
		boolean bDesignChanged = false;
		EffectivityModelUpdater.EffectivityModelUpdateData effectivityUpdateData = null;

		// IN-2437
		// Add profiler for processing after the Edit Design dialog is closed
		// Declared here so that we can log it right at the end of the method
		Profiler closeDialogProfiler = ProfilingService.NULL_PROFILER;
		try {
			boolean readonly = false;
			if (!designAlreadyLocked) {
				if (!processDesignLocking(project, context, design, null)) {
					return false;
				}
				readonly = !design.isLocked();
			}
			updateDesignWindowTitle(design, false);

			Profiler openDialogProfiler = startProfiling();

			// Check to make sure that the FolderMgr is not locked
			// @todo - if read-only we should not need the foldermgr locked!
			folderMgr = project.getFolderMgr();
			// dts0100567666 Beta Tests - Manager design folder is locked when a another user tries to call design-Edit.
			// The Folder Manager will get locked in the DesignEditDialog if required
//			if (!lockFolderMgr(folderMgr)) {
//				return false;
//			}

			// refresh the optionmgr before we refresh the design.  The design may reference new options
			project.getOptionMgr().refresh();
			// dts0100361189 - disappearing flipper bug
			project.refreshDesignListAndFolderMgr();

			//if the dialog readonly is false but design accessibility plugin is present
			// the below code checks the design accessibility after the project refresh happening at line:160
			IUserAccessFromPlugin accessibilityPlugin = PluggableDomainChecker.getInstance().getAccessibilityPlugin(Set.of(design));
			if (!readonly && accessibilityPlugin != null) {
				readonly =! DomainChecker.domainableAllowedForUserWrite(design, new UserAccessNotifier());
			}

			DesignEditDialog dialog = getEditDesignDialog(project, design, isModelModified, readonly);
			EditDesignDialogAiExtension.extendDialog(dialog);

			stopAndLogProfiler(openDialogProfiler, "Open Edit Design dialog :");
			dialog.setVisible(true);

			String title = getEditDialogName();

			if (dialog.isCancelled() ||
					!freezeUnfreezeObjects(project, design, dialog, title)) {
				return false;
			}

			// start profiling of the OK case here
			// there can be more dialog interaction in FreezeHelper.performReleaseFreeze
			// so this is good enough for the common case (we don't need a profiler hook for cancel)
			closeDialogProfiler = startProfiling();

			// Get the values from the dialog

			// Save the design if it is modified and user changed to
			// design-non-modify-able release level
			releaseLevelChanged = dialog.isReleaseLevelChanged();
			IReleaseLevel level = dialog.getReleaseLevel();
			if ((releaseLevelChanged && !level.isDesignChangeAllowed()) ||
					((IUpgradeableDesignContainer) design).isUpgradePendingSave()) {
				doCompleteSave(project, design, model,
						releaseLevelChanged && !level.isDesignChangeAllowed(),
						((IUpgradeableDesignContainer) design).isUpgradePendingSave());
			}

			boolean nameChanged = dialog.isDesignNameChanged();

			if (nameChanged) {
				// Create a rename update
				String request = ProjectStorageHelper.getRenameUpdate(context, dialog.getDesignName());
				boolean updateSuccess = ServerUpdateHelper.updateServerData(request);
				if (!updateSuccess) {
					return false;
				}
			}

			// store old release level incase we need to send it to the web service
			IReleaseLevel oldLevel = design.getReleaseLevel();

			// Hacky fix to dts0100583278 - Editing the Design details in design tools, without opening the diagram,
			// shows inconsistency between design name & design folder names.
			// The problem is the flush() code (and other subsequent code within this transaction) will call
			// design.getDiagrams() - this will cause the diagrams to be loaded (if they are not loaded) which will
			// cause the design name to be reset to its original value.  So loading the diagrams ahead of the changes
			// to the cof design object ensures that we do not get this problem.
			design.getDiagrams();
			// set all the design attributes per the dialog
			Set<IProperty> deletedPropSet = new LinkedHashSet<IProperty>(design.getNumProperties());
			boolean changed = dialog.applyChanges(deletedPropSet);
			boolean applicableOptionsChanged = dialog.getApplicableOptionsEdited();

			boolean isIECAttributePropagated = dialog.getisIECAttributePropagated();
			if (isIECAttributePropagated) {
				saveDesignAndWaitUntilComplete(project, design, null, true);
			}

			effectivityUpdateData = dialog.getEffectivityUpdateData();
			final boolean doEffectivityChanges =
					EffectivityHelper.doEffectivityChanges(project.getEffectivityMgr(), design, effectivityUpdateData);
			// UXFWK-3761 fix - Setting up the flag based on effectivity changes
			CAFUtils.getInstance().getCAFProjectMgr().setDesignEffectivityChangesExists(doEffectivityChanges, project);
			bDesignChanged = changed || applicableOptionsChanged || releaseLevelChanged || nameChanged ||
					isIECAttributePropagated || doEffectivityChanges;
			if (bDesignChanged) {
				if (!deletedPropSet.isEmpty()) {
					ProjectStorageHelper.deleteProperties(design, deletedPropSet);
					// FEAT 15783.  If deleted properties and a diagram is open, clean the text.
					List<IBaseDiagram> openDiagrams = CAFUtils.getInstance().getLoadedDiagrams(design);
					if (!openDiagrams.isEmpty()) {
						PropTextSyncDesignCleaner cleaner = new PropTextSyncDesignCleaner();
						cleaner.synchronizeDeletedDesignProperties(model, openDiagrams, deletedPropSet);
					}
				}
				//
				//Regenerate all cross reference to update design name
				// - This could be optimized as the only designs that need updating are those that use objects that are
				// ALSO used on this design.
				//

				project.getCrossReferenceMonitor().generateCrossReferences();
				// Save design changes
				design.flush(false);
				// locking problem, do we need to flush the design mgr, we don't seem to have locked it?
				project.getDesignMgr().flush();
				((IPrivilegedFolderMgr) folderMgr).renameDesignFolderNode(design);
				folderMgr.flush();
				//dts0100752888: removed the 2 calls to projectEdited
				// It is anyway called by ProjectWindow.LifecycleEditAction.post

				if (model != null) {
					boolean bModelChanged = model.isModified();
					Collection<IUID> emptyList = Collections.emptyList();
					model.notifyModelChange(new ModelChangeEvent(model, emptyList));
					model.setModified(bModelChanged);
				}
			}

			// update the effectivity.
			bDesignChanged |= doEffectivityChanges;
			if (bDesignChanged) {
				// Update the diagram border properties
				updateBorderProperties(design);
			}
			if (releaseLevelChanged) {
				ReleaseLevelHelper.postReleaseLevelChangeAuditEventForDesign(design, oldLevel);
				// send the data to the web service
				sendDataToWebService(design, oldLevel);
			}
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex,
					ResourceMgr.getString(getResourceClass(), "Lifecycle.message.editFailed"));
		}
		finally { // Check is the folderMgr is locked, if it is unlock it
			if (folderMgr != null && folderMgr.isLocked()) {
				// Unlock the folderMgr
				DesignPersistenceUtils.unlockFolderMgr(folderMgr);
			}

			mLifeCycleListener.designModified(design, designAlreadyLocked, releaseLevelChanged);

			if (effectivityUpdateData != null) {
				EffectivityHelper.unlockObjects(effectivityUpdateData.getLockedObjects());
			}

			//(SP1202) dts0100791998 - Open diagram is requesting the user whether changes need to be saved,
			// despite the only change being the status against the Design details.
			updateDesignWindowTitle(design, bDesignChanged);
		}
		IDesignAbstraction designAbstraction = design.getDesignAbstraction();
		UIManager uiManager = (UIManager) CAFUtils.getInstance().getFIB().getUIMgr();
		if (uiManager != null) {
			ICapletController controllerForDesign = CAFUtils.getInstance().getControllerForDesign(design);
			if (controllerForDesign != null) {
				String designAbstractionName = designAbstraction != null ? designAbstraction.getName() : null;
				String designAbstractionType = designAbstraction != null ? designAbstraction.getType().getName() : null;
				DesignAbstractionInfo designAbstractionInfo =
						new DesignAbstractionInfo(designAbstractionName, designAbstractionType);
				String designType = uiManager.getDesignType(controllerForDesign.getCaplet());
				List<IBaseDiagram> openDiagrams = CAFUtils.getInstance().getOpenDiagrams();
				VisibilityManager.viewStateChanged(designType, true, designAbstractionInfo, null, openDiagrams.size());
				notifyStatusBar(uiManager);
			}
		}

		CAFUtils.getInstance().tickleUI(getFIB());
		stopAndLogProfiler(closeDialogProfiler, "Edit Design :");

		return true;
	}

	private void notifyStatusBar(@NotNull UIManager uiManager)
	{
		IDisplayContextListener statusBar = CommonUtils.cast(uiManager.getWindowMgr().getStatusBar(), IDisplayContextListener.class);
		if (statusBar != null) {
			statusBar.windowChanged(new WindowChangeEvent(uiManager, uiManager.getCurrentWindow(), uiManager.getCurrentWindow()));
		}
	}

	/**
	 * Tries to Freeze/Unfreeze shared object in design based on TargetRelease level behaviour.
	 *
	 * @param project - Current project.
	 * @param design  - Design being edited.
	 * @param dialog  - Design edit dialog.
	 * @param title   - Edit design dialog title.
	 * @return - Returns true if Successfully Freezed/Unfreezed else false.
	 */
	private boolean freezeUnfreezeObjects(@NotNull IProject project, @NotNull ILogicDesign design,
			@NotNull DesignEditDialog dialog, @NotNull String title)
	{
		IReleaseLevel targetReleaseLevel = dialog.getReleaseLevel();
		if (targetReleaseLevel.isFrozenSharedObjectsRequired()) {
			return FreezeHelper.performReleaseFreeze(project, design, dialog.getReleaseLevel(), mMainFrame, title);
		}
		else if (targetReleaseLevel.isUnFreezeOfSharedObjectRequired() && dialog.isReleaseLevelChanged()) {
			return unfreezeSharedObjects(design, mMainFrame);
		}
		else {
			return true;
		}
	}

	private boolean unfreezeSharedObjects(@NotNull ILogicDesign design, @NotNull Frame frame)
	{
		UnFreezeSharedObjectsScope unFreezeSharedObjectsScope =
				new UnFreezeSharedObjectsScope(design);
		UnFreezeSharedObjectsDialog sharedObjectsDialog =
				getUnFreezeSharedObjectsDialog(frame, unFreezeSharedObjectsScope);
		if (!sharedObjectsDialog.showDialog()) {
			return false;
		}
		UnfreezeHelper unFreezeHelper = getUnFreezeHelper(unFreezeSharedObjectsScope);
		unFreezeHelper.unfreezeSharedObjects();
		Set<UnfreezeMessageWrapper> unfreezeMessages = unFreezeHelper.getUnfreezeMessages();
		UnfreezeOutputTabHandler outputHandler;
		if(unfreezeMessages.isEmpty()){
			outputHandler = new UnfreezeOutputTabHandler(false);
		}
		else {
			outputHandler = new UnfreezeOutputTabHandler(true);
		}
		outputHandler.showMessages(unfreezeMessages, design);
		return true;
	}

	@NotNull protected UnFreezeSharedObjectsDialog getUnFreezeSharedObjectsDialog(@NotNull Frame frame,
			@NotNull UnFreezeSharedObjectsScope unFreezeSharedObjectsScope)
	{
		UnFreezeSharedObjectsDialog sharedObjectsDialog =
				new UnFreezeSharedObjectsDialog(frame, unFreezeSharedObjectsScope);
		return sharedObjectsDialog;
	}

	@NotNull protected UnfreezeHelper getUnFreezeHelper(@NotNull UnFreezeSharedObjectsScope unFreezeSharedObjectsScope)
	{
		return new UnfreezeHelper(unFreezeSharedObjectsScope);
	}

	private void updateDesignWindowTitle(@NotNull ILogicDesign design, boolean notifyModelChange)
	{
		Model model = getModel(design);
		boolean bModelChanged = false;
		if (model != null) {
			bModelChanged = model.isModified();
		}
		CAFUtils.getInstance().updateWindowTitlesForDesign(design, notifyModelChange);
		if (model != null) {
			model.setModified(bModelChanged);
		}
	}

	private void doCompleteSave(@NotNull IProject project, @NotNull ILogicDesign design, @Nullable Model model,
			boolean releaseChangeMakingDesignReadOnly, boolean upgradePendingSave)
	{
		boolean doCompleteDesignSave = false;
		boolean runDRCs = false;

		List<IBaseDiagram> restrictDiagrams = new ArrayList<>();
		if (releaseChangeMakingDesignReadOnly) {
			if (updateBorderPropertiesOnDiagrams(design, restrictDiagrams) ||
					(model != null && model.isModified())) {
				doCompleteDesignSave = true;
				runDRCs = true;
			}
		}

		if (upgradePendingSave) {
			design.getDiagrams().stream()
					.filter(diagram -> !restrictDiagrams.contains(diagram))
					.forEach(restrictDiagrams::add);
			doCompleteDesignSave = true;
		}

		if (doCompleteDesignSave) {
			saveDesignAndWaitUntilComplete(project, design, restrictDiagrams, runDRCs);
			if (model != null) {
				model.setModified(false);
				model.getController().getUndoableContainer().clear();
			}
		}
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (!isMultiUserMode) {
			if (((IPrivilegedCOGManagedLockableChildrenContainer) design).isWeakLocked()) {
				return mStatusHandler.isOpenReadOnly(getClass(), getResourceKeyRoot(design.getProject()) + ".selfWeakLocked");
			}
			return super.processDesignLocking(project, context, design, diagram);
		}
		else {
			return mStatusHandler.showYesNoDialog(DesignCapletLifecycleHelper.class,
					"DesignCapletLifecycleHelper.msghdr.cannotEditDesignProp",
					"DesignCapletLifecycleHelper.msg.cannotEditDesignProp") == JOptionPane.YES_OPTION;
		}
	}

	private DesignEditDialog getEditDesignDialog(IProject project, ILogicDesign design, boolean isDesignChanged,
			boolean readonly) throws NoReleaseLevelsException
	{
		String title = getEditDialogName();
		return getDesignEditDialog(project, design, isDesignChanged, readonly, title, mMainFrame);
	}

	private String getEditDialogName()
	{
		String title = ResourceMgr.getString(DesignInfoDialog.class, "DesignInfoDialog.dialog.title.text");
		return title;
	}

	private boolean updateBorderPropertiesOnDiagrams(ILogicDesign design, List<IBaseDiagram> updatedDiagrams)
	{
		boolean designChanged = false;
		for (ISchemDiagram diagram : design.getDiagrams()) {
			if (diagram.updateDiagramOnRelease()) {
				designChanged = true;
				updatedDiagrams.add(diagram);
			}
		}
		return designChanged;
	}

	private void updateBorderProperties(ILogicDesign design)
	{
		for (ISchemDiagram diagram : design.getDiagrams()) {
			if (diagram.isLoadedInMemory()) {
				diagram.updateBorderProperties();
			}
		}
	}

	protected void sendDataToWebService(ILogicDesign design, IReleaseLevel oldLevel) throws WebServiceClientException
	{
		FactoryMgr.getBridgesFactory().getWebServiceHookHelper().sendStatusReleaseNotification(design, oldLevel);
	}

	@Override @NotNull protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return appendEnvToResourceKeyRoot("BaseLifecycleDelegate.editdesign.lockfailed", project);
	}

	protected boolean isAllowedToOpen(@NotNull ILogicDesign design)
	{
		// We cannot allow the design to be refreshed if it is open as the functional source of an integrator design
		// that is open in MU mode.
		// If the design in memory is out of date, we cannot allow the edit diagram action to continue as it will force
		// a refresh of the design, causing potential crashes and corruptions in the integratior design.

		CapletLifecycleContext.OpenResult openResult = CapletLifecycleHelper.getOpenDesignResult(design);

		if (openResult.isMultiUserModeResult()) {
			ResourceBasedMessageContent messageContent =
					new ResourceBasedMessageContent(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.outOfSyncReferencer");
			messageContent.setImplicationsParameters(design.getFullName());
			messageContent.setGuidanceParameters(design.getFullName());
			Message.show(PromptSeverity.ERROR, messageContent);
			return false;
		}

		return true;
	}

}