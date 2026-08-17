/*
 * Copyright 2004-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.ICapitalAnalysis;
import chs.analysis.scope.AnalysisNetlistScopeFactory;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.analysis.AbstractAnalysisControllerActionRT;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class DynSimOnDemandAction extends AbstractAnalysisControllerActionRT
{

	Model m_model;
	protected ICapitalAnalysis m_capitalAnalysis;

	/**
	 * Creates a new instance of SimulateAction
	 */
	public DynSimOnDemandAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
		m_capitalAnalysis = CapitalAnalysisFactory.getAnalysisInterface();
		setUndoableAction(false);
	}

	public String getActionUIClass()
	{
		return DynSimOnDemandActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(DynSimOnDemandAction.class, "DynSimOnDemandAction.String.statusbarText");
	}

	public boolean isSelectedDynSimMode()
	{
		String uid = m_model.getDesign().getUID().toString();
		return LogicAnalysisServices.getAnalysisServices().getDynamicSimulationMode(uid) == LogicAnalysisServices
				.DYN_SIM_DEMAND;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		//if another analysis opereation is in progress, alert the user with a message
		if (AnalysisServices.getAnalysisServices().isAnalysisOperationActive()) {
			// display error message
			AnalysisServices.getAnalysisServices().raiseErrorAnalysisOperationStarted();
			return IActionEnum.eCanceled;
		}
		if(!AnalysisServices.isRelayServerConnectionAvailable()){
			return IActionEnum.eCanceled;
		}
		//System.err.println("Simulate Action " + hashCode( ) + "  on " + m_model.getDesign( ).getUID( ).toString( ) ) ;
		final String uid = getUID();
		if (m_capitalAnalysis != null) {
			m_capitalAnalysis.setAnalysisClient(uid, this);
		}
		final String subsystem = getSubsystem();
		final String simulationType = LogicAnalysisServices.getAnalysisServices().getSimulationType();
		Runnable r = new Runnable()
		{
			public void run()
			{
				// install tooltips
				enableToolTips();

				// set the dynamic simulation mode
				LogicAnalysisServices.getAnalysisServices()
						.setDynamicSimulationMode(uid, subsystem, AnalysisServices.DYN_SIM_DEMAND, simulationType);
				//reset the flag as the current opeation completed and another analysis operation can be performed
				AnalysisServices.getAnalysisServices().setIsAnalysisOperationActive(false);
				// update the ui
				safelyUpdateUI();
			}
		};
		Thread t = new Thread(r);
		//set the flag to prevent invoking another analysis operation when an operation is already in progress
		AnalysisServices.getAnalysisServices().setIsAnalysisOperationActive(true);
		t.start();
		// return and wait for the callback
		return IActionEnum.eCompleted;
	}

	private void safelyUpdateUI()
	{
		if (CAFUtils.getInstance() != null && getController() != null && getController().getCaplet() != null) {
			CAFUtils.getInstance().tickleUI(getController().getCaplet().getFIB());
		}
	}

	public boolean onTerminate(boolean successful)
	{
		return false;
	}

	protected String getUID()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return AnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			//return m_model.getDesign( ).getUID( ).toString( ) ;
			IAnalysisNetlistScope scope = AnalysisNetlistScopeFactory.createScope(m_model.getDesign());
			AnalysisServices
					.setCurrentAnalysisNetlistScope(scope, m_model.getDesign().getProject().getUID().toString());
			if (CAFUtils.getInstance().getActiveCapletController() != null) {
				CAFUtils.getInstance().getActiveCapletController().addAuditTab();
			}
			return scope != null ? scope.getUid() : ""; // Analysis code seems to treat empty string as null UID
		}
	}

	protected String getSubsystem()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return String.valueOf(AnalysisServices.getCurrentAnalysisNetlistScope().getProjectId());
		}
		else {
			return m_model.getDesign().getAnalysisSubsystem();
		}
	}

	protected void enableToolTips()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			LogicAnalysisServices.getAnalysisServices().setTooltipsEnabledForScope(
					AnalysisServices.getCurrentAnalysisNetlistScope(), true);
		}
		else {
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).setTooltipsEnabledForDesign(
					m_model.getDesign().getUID(), true);
		}
	}

	@Override public boolean isEnabled()
	{

		return AnalysisServices.isActionSupportedInMUMode(m_model.getDesign()) && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}