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
import chs.analysis.IAnalysisSimulationSessionController;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class ResetAction extends ControllerActionRT
{

	/**
	 * Creates a new instance of ResetAction
	 */
	public ResetAction(ICapletController c)
	{
		super(c);
		setUndoableAction(false);
	}

	protected Model getModel()
	{
		return (Model) getController().getCapletModel();
	}

	public String getActionUIClass()
	{
		return ResetActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(ResetAction.class, "ResetAction.String.statusbarText");
	}

	public String getUid()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return AnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			return getModel().getDesign().getUID().toString();
		}
	}

	public boolean isEnabled()
	{
		String uid = getUid();
		return AnalysisServices.isActionSupportedInMUMode(getModel().getDesign()) &&
				LogicAnalysisServices.getAnalysisServices().getDynamicSimulationMode(uid) !=
						AnalysisServices.DYN_SIM_OFF && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
	}

	/**
	 * Should we cache the isenabled status? <NOTE> This will add listeners for each action
	 *
	 * @return true for now.  We should only do this for slow actions
	 */
	@Override protected boolean checkCache()
	{
		return false;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		String uid = getUid();
		//String subsystem = m_model.getDesign( ).getAnalysisSubsystem( ) ;

		IAnalysisSimulationSessionController session = LogicAnalysisServices.getAnalysisServices().getSimSession(uid);
		if (session != null) {

			try {
				//session.setAnalysisSimulationSessionClient( uid, this ) ;
				session.reset(uid);
				if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
					((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
							LogicAnalysisServices.getCurrentAnalysisNetlistScope(), true);
				}
				else {
					((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices())
							.updateSimulation(getModel(), true);
				}
				//session.setAnalysisSimulationSessionClient( uid, null ) ;
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/*IDynamicGfxService dgs = m_model.getDynamicGfxService();
				dgs.removeAllTransientGfx();

				// Re-set initial pre-selection
				getController().getSelectMgr().removeSelectSet();

				ICapletView view = CAFUtils.getInstance().getActiveCapletView();
				if (view != null)
				{
					view.invalidate(IViewInvalidationEnum.eFull);
				}

				LogicAnalysisServices.getAnalysisServices( ).setSimulationResultVariables( uid, null ) ;
				LogicAnalysisServices.getAnalysisServices( ).getControlPanel( ).setMessage( "Simulation Reset." ) ;*/
		// return and wait for the callback
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}
}
