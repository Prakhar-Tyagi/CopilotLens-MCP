/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caplets.logic.actions.CreateNoPinInlineConnectorAction;
import chs.caplets.logic.actions.CreateNoPinInlineConnectorActionUI;

import java.awt.event.ActionEvent;

/**
 * Extension of regular "InsertConnectorAction" to allow it be used for creation of inline using points and direction
 * provided.
 */
public class InsertInlineConnectorAction extends CreateNoPinInlineConnectorAction
		implements IInsertInlineMouseAndCursorHandler
{

	private boolean enabled = false;
	private String plugName = "";
	private String jackName = "";

	public InsertInlineConnectorAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected boolean postActionChanges()
	{
		InsertInlineResult.ResultInlineConnector resultConnector = getResultConnector();
		resultConnector.getLogicalJack().setName(jackName);
		resultConnector.getLogicalPlug().setName(plugName);
		return super.postActionChanges();
	}

	public void setPlugName(String plugName)
	{
		this.plugName = plugName;
	}

	public void setJackName(String jackName)
	{
		this.jackName = jackName;
	}

	/**
	 * QPE-15936 GM save issue - User cannot save modifications after continuing on validation failures
	 * <p>
	 * This replaces the direct <code>terminate(successful);</code> call.
	 * <p>
	 * Change made to ensure the transaction boundary is closed at the end of an action.
	 *
	 * @param successful - was the action successful?
	 */
	@Override protected boolean doTerminate(boolean successful)
	{
		boolean success = super.doTerminate(successful);
		enabled = false;
		return success;
	}

	@Override public boolean isEnabled()
	{
		return enabled;
	}

	protected void enable()
	{
		enabled = true;
	}

	public static class UI extends CreateNoPinInlineConnectorActionUI
	{

		private ICapletController capletController;

		public UI(ICapletController capletController)
		{
			super(capletController.getCaplet());
			this.capletController = capletController;
		}

		public String getActionClass()
		{
			return InsertInlineConnectorAction.class.getName();
		}

		public IAction getAction()
		{
			return capletController.getAction(getActionName());
		}

		public void actionPerformed(ActionEvent e)
		{
			IAction action = getAction();
			IActionMgr actionMgr = capletController.getActionMgr();
			if (action != null && actionMgr != null) {
				actionMgr.actionPerformed(action, e);
			}
		}
	}
}
