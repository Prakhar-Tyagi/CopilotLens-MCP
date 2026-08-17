/*
 * Copyright 2010-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.actions.AutoRouteSettingsActionUI;
import chs.view.route.AutoRouteSettingsDialog;

import javax.swing.WindowConstants;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AutoRouteSettingsAction extends ControllerActionRT
{

	public AutoRouteSettingsAction(ICapletController controller)
	{
		super(controller);
		setUndoableAction(true);
	}

	public String getActionUIClass()
	{
		return AutoRouteSettingsActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();

		final AutoRouteSettingsDialog dialog =
				new AutoRouteSettingsDialog(owner, ConductorRouteAction.getInstance().getCostStrategy());

		// Initialize the dialog
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		dialog.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				dialog.setVisible(true);
			}
		});
		dialog.getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dialog.setVisible(true);
			}
		});

		// Setup an action listener on the Cancel button to terminate without success
		dialog.getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dialog.setVisible(true);
			}
		});
		dialog.setVisible(true);

		return IActionEnum.eCompleted;
	}

	/**
	 * Description of the Method
	 *
	 * @param successful Description of Parameter
	 *
	 * @return Description of the Returned Value
	 */
	public boolean onTerminate(boolean successful)
	{
		return true;
	}
}
