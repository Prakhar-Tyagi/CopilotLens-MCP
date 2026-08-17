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

import chs.caf.CAFUtils;
import chs.caf.ICAFProjectMgr;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.CapletLifecycleContext;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.capitalmanager.appserver.ILockInfo;
import chs.caplets.IDesignLockStrategy;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.EditDiagramDialog;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IIncLoadable;
import chs.common.IProperty;
import chs.common.IUID;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.IUserAccessNotifier;
import chs.utility.UserAccessNotifier;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.persist.LockableHelper;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EditDiagramDelegate extends EditLifecycleDelegate
{

	public EditDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, FunctionalPermissionEnum editDesignPermission)
	{
		super(lifeCycleListener, resourceClass, editDesignPermission);
	}

	public boolean editDiagram(@NotNull IProject project, ISchemDiagram diagram, List<?> context)
	{
		if (diagram.isDeleted()) {
			mLifeCycleListener.diagramDeleted(null, diagram);
			CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
			return false;
		}
		Consumer<IUserAccessNotifier> domainErrorMsgDisplayer = notifier -> {
			ResourceBasedMessageContent messageContent = new ResourceBasedMessageContent(BaseLifecycle.class,
					"BaseLifecycle.diagram.cannotEditDiagram");
			messageContent.setMessageParameters(notifier.getMessage());
			Message.show(PromptSeverity.ERROR, messageContent);
		};
		IUserAccessNotifier notifier = new UserAccessNotifier();
		if (!isAllowedToEdit(diagram.getDesign(), notifier, domainErrorMsgDisplayer)) {
			return false;
		}
		// PDVC-2196
		if (!isAllowedToOpen(diagram.getDesign())) {
			return false;
		}
		ICAFProjectMgr pm = getFIB().getProjectMgr();
		assert pm != null;

		ILogicDesign design = diagram.getDesign();
		IIncLoadable incLoadableDesign = CommonUtils.cast(design, IIncLoadable.class);
		boolean designWasSkeleton = incLoadableDesign != null && incLoadableDesign.isSkeleton();

		IIncLoadable incLoadableDiagram = CommonUtils.cast(diagram, IIncLoadable.class);
		boolean diagramWasSkeleton = incLoadableDiagram != null && incLoadableDiagram.isSkeleton();

		boolean designAlreadyLocked = IDesignLockStrategy.isLocked(design);
		boolean diagramAlreadyLocked = IDesignLockStrategy.isLocked(diagram);

		Model model = getModel(diagram.getDesign());
		boolean changed = false;
		boolean status = true;

		Profiler editDiagramProfiler = ProfilingService.NULL_PROFILER; // IN-2507
		try {
			boolean readonly = false;
			if (!diagramAlreadyLocked) {
				if (!processDesignLocking(project, context, design, diagram)) {
					return false;
				}
				readonly = !IDesignLockStrategy.isLocked(diagram);
			}

			if (!IDesignLockStrategy.isLocked(diagram) &&
					(diagram.isDeleted() || !doesPersistedDiagramExist(diagram))) {
				mLifeCycleListener.diagramDeleted(model, diagram);
				design.refresh();
				CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
				return false;
			}

			Profiler profiler = startProfiling();
			String title = ResourceMgr.getString(getResourceClass(), "Lifecycle.EditDiagram.Dialog.title");

			// Make sure diagram is loaded into memory - unfortunately we have to load the whole diagram
			// as the Diagram properties can require the border to be updated, also we don't have a mechanism
			// to flush the properties only
			diagram.getBackground(); // TODO creddy: Why is diagram loaded before the dialog is invoked?

			EditDiagramDialog dialog =
					getEditDiagramDialog(title, diagram, !readonly && design.areAttributesEditable());
			stopAndLogProfiler(profiler, "Open Edit Diagram dialog :");
			dialog.setVisible(true);
			editDiagramProfiler = startProfiling();

			if (dialog.isCancelled()) {
				return false;
			}

			// set all the diagram attributes per the dialog
			Set<IProperty> deletedPropSet = new LinkedHashSet<IProperty>(diagram.getNumProperties());
			changed = dialog.setChangedInfo(deletedPropSet);

			if (changed) {

				// Update the diagram border properties
				diagram.updateBorderProperties();

				// Save changes only if the diagram has no model, i.e no views (clients), saving will be done via model
				if (model == null) {
					saveDesignAndWaitUntilComplete(project, design, null, true);
				}
				else {
					//dts0100684717 - For an opened diagram, we shouldn't immediately update the usages as we have not updated the diagram name in DB.
					//Save will take care of updating the usages as per the new diagram name for opened diagrams.
					Collection<IUID> emptyList = Collections.emptyList();
					model.notifyModelChange(new ModelChangeEvent(model, emptyList));
					model.setModified(true);
				}

				((ICapletLifecycle) mLifeCycleListener).setWindowTitle(context, diagram);
				List<?> changeContext = new ArrayList<Object>(context);
				changeContext.remove(diagram);
				pm.projectEdited(project, changeContext);
				project.getCrossReferenceMonitor().generateCrossReferencesForDesign(design);
				DiagramAuditTrialHelper.getInstance().postStoredEvents();
			}
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex,
					ResourceMgr.getString(getResourceClass(), "Lifecycle.message.editFailed"));
		}
		finally {
			if (!changed || model == null) {
				if (incLoadableDesign != null && designWasSkeleton) {
					design.unloadChildren();
				}
				else if (!diagram.isDeleted()) {
					if (incLoadableDiagram != null && diagramWasSkeleton) {
						diagram.unloadChildren();
					}
				}
			}

			if (!changed) {
				status = false;
				if (!diagramAlreadyLocked) {
					tryReleasingLockFromDiagram(diagram);
				}
				if (!designAlreadyLocked) {
					tryReleasingLockFromDesign(design);
				}
			}
			else {
				if (model == null) {
					if (!diagramAlreadyLocked) {
						tryReleasingLockFromDiagram(diagram);
					}
					if (!designAlreadyLocked) {
						tryReleasingLockFromDesign(design);
					}
				}
			}
		}

		CAFUtils.getInstance().tickleUI(getFIB());
		stopAndLogProfiler(editDiagramProfiler, "Edit Diagram :");

		return status;
	}

	private void tryReleasingLockFromDesign(ILogicDesign design)
	{
		if (IDesignLockStrategy.isLocked(design)) {
			((BaseLifecycle) mLifeCycleListener).relinquishLockWithSaveTaskSanity(design.getProject(), design);
		}
	}

	private void tryReleasingLockFromDiagram(ISchemDiagram diagram)
	{
		if (!diagram.isDeleted()) {
			if (IDesignLockStrategy.isLocked(diagram)) {
				((BaseLifecycle) mLifeCycleListener).relinquishLockWithSaveTaskSanity(diagram.getProject(), diagram);
			}
		}
	}

	protected boolean doesPersistedDiagramExist(@NotNull IBaseDiagram diagram)
	{
		return LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), diagram.getObjType(),
				diagram.getUID().getString());
	}

	private boolean isAllowedToOpen(@NotNull ILogicDesign design)
	{
		// We cannot allow the design to be refreshed if it is open as the functional source of an integrator design
		// that is open in MU mode.
		// If the design in memory is out of date, we cannot allow the edit diagram action to continue as it will force
		// a refresh of the design, causing potential crashes and corruptions in the integrator design.

		CapletLifecycleContext.OpenResult openResult = CapletLifecycleHelper.getOpenDesignResult(design);

		if (openResult.isMultiUserModeResult()) {
			getStatusReporter().showInformationMessage(mMainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotOpenDiagram"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotOpen.associated"));
			return false;
		}
		return true;
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (!isMultiUserMode) {
			Model model = getModel(design);

			boolean didLock = IDesignLockStrategy.getLockStrategy(design).acquireLock(diagram);
			if (IDesignLockStrategy.isLocked(diagram)) {
				return true;
			}

			if (diagram.isDeleted() || !doesPersistedDiagramExist(diagram)) {
				mLifeCycleListener.diagramDeleted(model, diagram);
				design.refresh();
				CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
				return false;
			}

			if (checkDesignExists(project, design)) {
				// If the reason is because of domain problems, trump the current behavior and provide a custom
				// message.
				return shouldAllowReadOnly(project, context, design, diagram, didLock);
			}
			return false;
		}
		else {
			return mStatusHandler.showYesNoDialog(DesignCapletLifecycleHelper.class,
					"DesignCapletLifecycleHelper.msghdr.cannotEditDiagramProp",
					"DesignCapletLifecycleHelper.msg.cannotEditDiagramProp") == JOptionPane.YES_OPTION;
		}
	}

	protected boolean askOpenReadOnly(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull ILockInfo designLockInfo)
	{
		return askOpenReadOnlyForDiagramActions(design, diagram, designLockInfo);
	}

	@NotNull protected EditDiagramDialog getEditDiagramDialog(String title, @NotNull ISchemDiagram diagram, boolean editable)
	{
		return new EditDiagramDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), title, diagram, editable);
	}

	@NotNull @Override protected String getResourceKeyRoot(@Nullable IProject project)
	{
		return appendEnvToResourceKeyRoot("BaseLifecycleDelegate.editdiagram.lockfailed", project);
	}

	@NotNull @Override protected String getResourceKeyWhenUserNamesAreMissing()
	{
		return "BaseLifecycleDelegate.editDiagram.lockFailed.multiuser.diagramLockByUnknownUser";
	}
}
