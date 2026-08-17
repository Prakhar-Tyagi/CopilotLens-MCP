/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.rules;

import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.SetAttributesAndPropertiesByRuleAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caplets.logic.commands.LogicSetAttributesAndPropertiesByRuleCmd;
import chs.cof.logical.ILogicDesign;
import chs.cofUtils.cmd.SetAttributesAndPropertiesByRuleCmd;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.ILogicModel;

import java.awt.event.ActionEvent;

public class LogicSetAttributesAndPropertiesByRuleAction extends SetAttributesAndPropertiesByRuleAction
{

	public LogicSetAttributesAndPropertiesByRuleAction(ICapletController controller)
	{
		super(controller);
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	protected SetAttributesAndPropertiesByRuleCmd createCommand()
	{
		ILogicDesign design = (ILogicDesign) ((ILogicModel) getController().getCapletModel()).getDesign();
		return new LogicSetAttributesAndPropertiesByRuleCmd(new CAFCommandHelper(), design);
	}

	public String getActionUIClass()
	{
		return LogicSetAttributesAndPropertiesByRuleActionUI.class.getName();
	}

	/**
	 * Overridden here because we might need to ask a question after the command has been prepared.  This asking is
	 * currently not easily done from the command because it requires us to use a "Don't ask me this again" in the
	 * prompt.
	 *
	 * @see SetAttributesAndPropertiesByRuleAction#onActivate(ActionEvent)
	 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum activateResult = super.onActivate(e);
		if (activateResult == IActionEnum.eCompleted) {
			// coding error elsewhere if the command is not as below:
			LogicSetAttributesAndPropertiesByRuleCmd cmd = (LogicSetAttributesAndPropertiesByRuleCmd) getCommand();
			if (cmd.getSharedObjectChangesRequired()) {
				// prompt user before we make a mass of shared object changes, with a "Don't ask again" checkbox
				// NOTE: this is the reason we have a seperate prepare for this task, rather than just processing all in the complete
				String heading = ResourceMgr.getString(LogicSetAttributesAndPropertiesByRuleAction.class,
						"LogicSetAttributesAndPropertiesByRuleAction.prompt.heading.SharedPropertyChanges");
				String question = ResourceMgr.getString(LogicSetAttributesAndPropertiesByRuleAction.class,
						"LogicSetAttributesAndPropertiesByRuleAction.prompt.message.SharedPropertyChanges");

				// NOTE: ConfirmSaveDialog is not actually a prompt for saving,
				// it is a general prompt that is only shown based on a registry setting derived from the class name
				// (or any other key) that is passed to the constructor
				ConfirmSaveDialog confirmDlg = new ConfirmSaveDialog(getClass().getName(), heading, question);
				if (confirmDlg.userCanceled()) {
					activateResult = IActionEnum.eCanceled;
				}
			}
		}

		return activateResult;
	}

	/**
	 * Overridden here because we need to clear the Undo stack if changes were made to shared objects.
	 */
	protected void postCmdExecute()
	{
		// TODO jacobt FEAT3351 : this can be done in the command (CommandHelper has a method for it)
		// TODO jacobt FEAT3351 : we should probably not even clear the Undo stack here (currently do it to avoid a crash)
		LogicSetAttributesAndPropertiesByRuleCmd cmd = (LogicSetAttributesAndPropertiesByRuleCmd) getCommand();
		if (cmd.getSharedObjectChangesMade()) {
			getController().getUndoableContainer().endEdit();
			getController().getUndoableContainer().clear();
			CreationDeletionHelper.getTheCreationHelper().processImportedObjects();
		}
	}
}
