/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IUpdateableAction;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.UnassignPortedConductorCmd;
import chs.cog.ICOGLockable;
import chs.common.IUIDObject;
import chs.common.IUpdateable;
import chs.common.RefreshStatusEnum;
import chs.ctf.ui.form.shareddeletion.DeleteSharedObjectCmd;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.IObjectInUseService;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.LockInfoDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;

public class LogicSharedObjectDeleteAction extends ControllerActionRT
{

	private ISpecialSelectMgr specialSelectMgr = null;
	private CHSCommand cmd = null;

	public LogicSharedObjectDeleteAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							boolean shouldDisplay = ActionRT.isDesignUnderConcurrentEdit() || isEnabled();
							if (!shouldDisplay) {
								Action ui = getActionUI();
								if (ui == null) {
									return false;
								}
								((IUpdateableAction) ui).updateUI();
							}
							return shouldDisplay;
						}

						public String getName()
						{
							return (String) getAction().getValue(Action.NAME);
						}
					}
			);
		}
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum success = IActionEnum.eCompleted;
		// If this action has been access by a selection in the shared object browser.
		IUIDObject uidObject = getOperand();
		if (uidObject instanceof ISharedObject) {
			ISharedObject sharedObject = (ISharedObject) uidObject;
			if (uidObject instanceof ISharedConductor) {
				ISharedConductor sharedCond = (ISharedConductor) uidObject;
				ISharedMulticore sharedMC = sharedCond.getMulticore();

				if (sharedMC != null) {
					if (sharedCond.isShield() && sharedMC.isUsed(IObjectInUseService.OBJECT_IN_USE)) {
						// If the conductor is a shield and the multicore has *ANY* uses we can not delete it because
						// it would leave hookups in designs.  Overbraids can never have thier shields deleted and this
						// is handled in the isEnabled function.
						MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
								ResourceMgr.getString(DeleteSharedObjectCmd.class,
										"LogicSharedObjectDeleteAction.title"),
								ResourceMgr.getString(DeleteSharedObjectCmd.class,
										"LogicSharedObjectDeleteAction.cannotDelete",
										ResourceMgr.getString(this,
												"LogicSharedObjectDeleteAction.cannotDeleteUsedShield")));
						return IActionEnum.eCanceled;
					}
				}
			}

			//dts0100547401: shared object should be the lastest version to be able to delete it.
			//dts0100879363 NPE when we try to delete innercore of shared multicore from shared tab when multicore itself is deleted in another session
			if (!(sharedObject instanceof ICOGLockable))/* && needsRefresh(sharedObject))*/ {
				RefreshStatusEnum refreshStatus = ((IUpdateable) sharedObject).refresh();
				if (refreshStatus.equals(RefreshStatusEnum.eRefreshed) ||
						refreshStatus.equals(RefreshStatusEnum.eObjectDoesNotExist)) {
					String mesg = refreshStatus.equals(RefreshStatusEnum.eRefreshed) ?
							"LogicSharedObjectDeleteAction.wasUpdated" : "LogicSharedObjectDeleteAction.wasDeleted";
					String deleteReason = ResourceMgr.getString(this, mesg,
							sharedObject.getObjectTypeForDisplay() + " " + sharedObject.getName());

					ResourceBasedMessageContent content =
							new ResourceBasedMessageContent(this, "LogicSharedObjectDeleteAction.cannotDelete");
					content.setContextParameters(sharedObject.getObjectTypeForDisplay());
					content.setMessageParameters(sharedObject.getObjectTypeForDisplay());
					content.setImplicationsParameters(deleteReason);
					Message.show(PromptSeverity.ERROR, content);

					Model model = (Model) getController().getCapletModel();
					IProject project = model.getDesign().getProject();
					ISharedConductorMgr scmgr = project.getSharedConductorMgr();
					scmgr.refresh();
					ISharedPinListMgr splmgr = project.getSharedPinListMgr();
					splmgr.refresh();
					SharedObjectMgr.fireChangeEventForManagers(scmgr, splmgr);
					return IActionEnum.eCanceled;
				}
			}
			DeleteSharedObjectCmd command = new DeleteSharedObjectCmd(sharedObject);
			cmd = command;
			if (!cmd.doExecuteAllowed()) {
				command.ShowCannotDeleteMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
				success = IActionEnum.eCanceled;
			}
			else if (!(sharedObject instanceof ISharedPinList)) {
				// Only prompt here if it's NOT as pinlist (i.e. a conductor or multicore) because the delete for
				// pinlists prompts itself.
				boolean answer = command.ConfirmDelete(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
				if (!answer) {
					// User cancelled.
					success = IActionEnum.eCanceled;
				}
			}
			setUndoableAction(false);
		}
		else if (uidObject instanceof IConductor) {
			IConductor conductor = (IConductor) uidObject;
			cmd = new UnassignPortedConductorCmd(new CAFCommandHelper(), conductor.getUID());
			if (!cmd.doExecuteAllowed()) {
				MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						ResourceMgr.getString(this, "LogicSharedObjectDeleteAction.port.title"),
						ResourceMgr.getString(this, "LogicSharedObjectDeleteAction.port.usedOnAnotherDiagram"));
				success = IActionEnum.eCanceled;
			}
			setUndoableAction(true);
		}
		return success;
	}

	private boolean needsRefresh(ISharedObject sharedObject)
	{
		ISharedLockableUpdateableObject sharedLockableUpdateableObject
				= ReferenceHelper.reduceToSharedUpdateableObject(sharedObject);
		if (sharedLockableUpdateableObject != null) {
			return sharedLockableUpdateableObject.needsRefresh();
		}
		return false;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean success = successful;
		if (success) {
			//refresh mgrs
			Model model = (Model) getController().getCapletModel();
			IProject project = model.getDesign().getProject();
			ISharedConductorMgr scmgr = project.getSharedConductorMgr();
			scmgr.refresh();

			if (!scmgr.lock()) {
				LockInfoDialog.showLockInfoDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						scmgr, UtilsHelper.getCHSSystem().getUserSession());
				return true;
			}
			scmgr.unlock();

			ISharedPinListMgr splmgr = project.getSharedPinListMgr();
			splmgr.refresh();
			if (!splmgr.lock()) {
				LockInfoDialog.showLockInfoDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						splmgr, UtilsHelper.getCHSSystem().getUserSession());
				return true;
			}
			splmgr.unlock();

			success = cmd.execute();
			if (!success) {
				if (cmd instanceof DeleteSharedObjectCmd) {
					((DeleteSharedObjectCmd) cmd)
							.ShowCannotDeleteMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
				}
				else {
					MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
							ResourceMgr.getString(this, "LogicSharedObjectDeleteAction.port.title"),
							ResourceMgr.getString(this, "LogicSharedObjectDeleteAction.port.cannotDelete"));
				}
			}

			SharedObjectMgr.fireChangeEventForManagers(scmgr, splmgr);
		}
		return success;
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	public boolean isEnabled()
	{
		return DeleteSharedObjectCmd.userHasDeleteUnusedSharedObjectsPermission() && getOperand() != null &&
				super.isEnabled();
	}

	public String getActionUIClass()
	{
		return LogicSharedObjectDeleteActionUI.class.getName();
	}

	@Nullable private IUIDObject getOperand()
	{
		IUIDObject uidObject = null;
		if (specialSelectMgr.getSelectedObjects().getSize() == 1) {
			String menuString = ResourceMgr.getString(LogicSharedObjectDeleteActionUI.class,
					"LogicSharedObjectDeleteActionUI.name.decl");
			uidObject = specialSelectMgr.getSelectedObjects().getNext();
			if (uidObject instanceof ISharedMessageSignal) {
				uidObject = null;
			}
			else if (uidObject instanceof IConductor) {
				menuString = ResourceMgr.getString(LogicSharedObjectDeleteActionUI.class,
						"LogicSharedObjectDeleteActionUI.name.port.decl");
			}
			else if (uidObject instanceof ISharedMulticore sharedMulticore) {
				// Can only delete from the top level
				if (sharedMulticore.getParent() != null ||
						SingleLineHelper.isMulticorePartOfAnySingleLine(sharedMulticore)) {
					uidObject = null;
				}
			}
			else if (uidObject instanceof ISharedConnector) {
				// Can only delete from the top level
				ISharedConnector connector = (ISharedConnector) uidObject;
				if (connector.getOccupiedPosition() != null) {
					uidObject = null;
				}
			}
			else if (uidObject instanceof ISharedConductor) {
				ISharedConductor sharedCond = (ISharedConductor) uidObject;
				ISharedMulticore sharedMC = sharedCond.getMulticore();

				// Not allowed to ever delete the shield of an overbraid because it can never be replaced.  Same as
				// CProject.
				if (sharedMC != null && sharedMC instanceof ISharedOverbraid) {
					uidObject = null;
				}
			}
			else if (!(uidObject instanceof ISharedObject)) {
				uidObject = null;
			}
			// Make sure the text is correct
			Action ui = getActionUI();
			if (ui != null) {
				ui.putValue(Action.NAME, menuString);
			}
		}
		return uidObject;
	}
}
