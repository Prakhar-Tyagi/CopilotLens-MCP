/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.IAnalysisSimulationSessionController;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author rharring
 */
public class ViewFailedComponentsAction extends ControllerActionRT
{

	Model m_model;

	/**
	 * Creates a new instance of SimulateAction
	 */
	public ViewFailedComponentsAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
	}

	public String getActionUIClass()
	{
		return ViewFailedComponentsActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr
				.getString(ViewFailedComponentsAction.class, "ViewFailedComponentsAction.String.statusbarText");
	}

	public boolean isEnabled()
	{
		//boolean b = AnalysisServices.getAnalysisServices( ).getDynamicSimulationMode( ) == AnalysisServices.DYN_SIM_DEMAND ;
		//System.err.println( "SimulateAction is Enabled " + b ) ;
		//return b ;
		return AnalysisServices.isActionSupportedInMUMode(m_model.getDesign()) &&
				LogicAnalysisServices.getAnalysisServices().isDynamicSimulationEnabled() && super.isEnabled();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{

		String uid = m_model.getDesign().getUID().toString();

		IAnalysisSimulationSessionController session = LogicAnalysisServices.getAnalysisServices().getSimSession(uid);
		if (session != null && session.isRunning(uid)) {
			//session.beginSimulation( uid, subsystem ) ;
			// update hte netlist
			JPanel panel = new JPanel();
			Component c = session.createFailureList(uid);
			FailureDialog dialog = new FailureDialog(c);
			dialog.setVisible(true);
		}
		else {
			MessageHelper.showInformationMessage(getController().getCaplet().getFIB().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(ViewFailedComponentsAction.class,
							"ViewFailedComponentsAction.String.notSimulatingDesign.title"),
					ResourceMgr.getString(ViewFailedComponentsAction.class,
							"ViewFailedComponentsAction.String.notSimulatingDesign.message"));
		}

		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}

	// ///////////// //
	// Inner classes //
	// ///////////// //

	class FailureDialog extends CAFOkCancelDialog
	{

		public FailureDialog(Component c)
		{
			super(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(ViewFailedComponentsAction.class,
							"ViewFailedComponentsAction.String.failureDialogTitle"),
					true);
			//setTitle( "Applied Failure list..." ) ;
			getContentPane().add(c, BorderLayout.CENTER);
			addActionListeners();
			pack();
			//setModal( true ) ;

		}

		protected void addActionListeners()
		{
			ActionListener al = new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					setVisible(false);
				}
			};

			// don't really want any differences so add same listener to both.
			// An ok buttoned dialog would be better.......
			getOkButton().addActionListener(al);
			getCancelButton().addActionListener(al);
		}
	}
}