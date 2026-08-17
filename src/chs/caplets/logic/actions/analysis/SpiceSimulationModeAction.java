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
import chs.utilities.StringUtils;

import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class SpiceSimulationModeAction extends ControllerActionRT
{

	protected static final String modeChangedToSpiceString =
			ResourceMgr
					.getString(SpiceSimulationModeAction.class, "SpiceSimulationModeAction.String.modeChangedToSpice");
	private static final String ELECTRICAL_QUANTITATIVE_SPICE = "electrical.quantitative.spice";

	/**
	 * Creates a new instance of SimulateAction
	 */
	public SpiceSimulationModeAction(ICapletController c)
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
		return SpiceSimulationModeActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(SpiceSimulationModeAction.class, "SpiceSimulationModeAction.String.statusbarText");
	}

	public String getUid()
	{
		if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			//System.err.println("Returning the scope uid.");
			return LogicAnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			//System.err.println("Returning the model uid.");
			return getModel().getDesign().getUID().toString();
		}
	}

	protected IActionEnum onActivate(ActionEvent e)
	{

		LogicAnalysisServices.getAnalysisServices().getControlPanel().setMessage(modeChangedToSpiceString);
		//System.err.println("Simulate Action " + hashCode( ) + "  on " + m_model.getDesign( ).getUID( ).toString( ) ) ;
		String uid = getUid();
		//String subsystem = m_model.getDesign( ).getAnalysisSubsystem( ) ;

		LogicAnalysisServices.getAnalysisServices().setSimulationType(ELECTRICAL_QUANTITATIVE_SPICE);

		IAnalysisSimulationSessionController session = LogicAnalysisServices.getAnalysisServices().getSimSession(uid);
		if (session != null) {
			//session.beginSimulation( uid, subsystem ) ;
			// update hte netlist
			String netlist = LogicAnalysisServices.getAnalysisServices().getNetlist(uid);
			session.setSimulationType(uid, ELECTRICAL_QUANTITATIVE_SPICE, StringUtils.getBytes(netlist));
			if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
				((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
						LogicAnalysisServices.getCurrentAnalysisNetlistScope());
			}
			else {
				((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(getModel());
			}
		}

		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}

	public boolean isSetSimulationMode()
	{
		String uid = getUid();

		//System.err.println("In is set simulation mode SSME" ) ;
		String target = "";
		IAnalysisSimulationSessionController session = LogicAnalysisServices.getAnalysisServices().getSimSession(uid);
		if (session != null) {
			//System.err.println("SpiceSimModeAction : Sim session type is : " + session.getSimulationType( uid ) ) ;
			target = session.getSimulationType(uid);
		}
		else {
			String type = LogicAnalysisServices.getAnalysisServices().getSimulationType();
			if (type != null) {
				target = type;
			}
		}

		//System.err.println("Returning false" );
		boolean isSet = "electrical.quantitative.spice".equals(target);

		return isSet;
	}

	@Override protected boolean checkCache()
	{
		return false;
	}

	@Override public boolean isEnabled()
	{
		return AnalysisServices.isActionSupportedInMUMode(getModel().getDesign()) && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
	}
}
