/*
 * Copyright 2007-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.ctf.ui.ProjectModel;
import chs.ctf.ui.form.AbstractEntryDialog;
import chs.ctf.ui.form.shareddeletion.DeleteSharedObjectCmd;
import chs.ctf.ui.form.shareddeletion.FunctionalSharedDeletionModel;
import chs.ctf.ui.form.shareddeletion.SharedDeletionModel;

import java.awt.Frame;
import java.awt.event.ActionEvent;

/**
 * Created by jamesmw User: jamesmw Date: 06-Jul-2007 Time: 15:08:02
 */
public class LogicSharedObjectDeleteUnusedAction extends ControllerActionRT
{

	public LogicSharedObjectDeleteUnusedAction(ICapletController controller)
	{
		super(controller);
		setUndoableAction(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean handleTransactionsInAction()
	{
		return false;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean success = successful;
		if (success) {
			Model model = (Model) getController().getCapletModel();
			IProject project = model.getDesign().getProject();

			//refresh mgrs
			ISharedConductorMgr scmgr = project.getSharedConductorMgr();
			scmgr.refresh();
			ISharedPinListMgr splmgr = project.getSharedPinListMgr();
			splmgr.refresh();

			// Show the dialog - which does all the work.
			ProjectModel projectmodel = new ProjectModel(project);
			SharedDeletionModel sdm = getSharedDeletionModel(projectmodel);
			success = showSharedObjectDeleteDialog(sdm);

			SharedObjectMgr.fireChangeEventForManagers(splmgr, scmgr);
		}
		return success;
	}

	private boolean showSharedObjectDeleteDialog(SharedDeletionModel sdm)
	{
		Frame dialogFrame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		LogicDeleteUnusedSharedObjectDialog dialog;
		if (shouldShowFunctionalUi()) {
			dialog = new FunctionalDeleteUnusedSharedObjectDialog(dialogFrame, sdm);
		}
		else {
			dialog = new LogicDeleteUnusedSharedObjectDialog(dialogFrame, sdm);
		}

		return dialog.showDialog() == AbstractEntryDialog.OK;
	}

	private SharedDeletionModel getSharedDeletionModel(ProjectModel projectmodel)
	{
		if (shouldShowFunctionalUi()) {
			return new FunctionalSharedDeletionModel(projectmodel);
		}
		return new SharedDeletionModel(projectmodel, false);
	}

	private boolean shouldShowFunctionalUi()
	{
		Model model = (Model) getController().getCapletModel();
		return model != null && model.getDesign() instanceof IFunctionLogicDesign;
	}

	public boolean isEnabled()
	{
		return DeleteSharedObjectCmd.userHasDeleteUnusedSharedObjectsPermission() && super.isEnabled();
	}

	public String getActionUIClass()
	{
		return LogicSharedObjectDeleteUnusedActionUI.class.getName();
	}
}