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
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.utilities.ResourceMgr;
import chs.utility.gfx.IViewInvalidationEnum;

import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class DynSimOffAction extends ControllerActionRT
{

	Model m_model;

	/**
	 * Creates a new instance of SimulateAction
	 */
	public DynSimOffAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
		setUndoableAction(false);
	}

	public String getActionUIClass()
	{
		return DynSimOffActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(DynSimOffAction.class, "DynSimOffAction.String.statusbarText");
	}

	public boolean isEnabled()
	{
		//return true ;

		return AnalysisServices.isActionSupportedInMUMode(m_model.getDesign()) &&
				(((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices())
						.isDynamicSimulationEnabled(m_model.getDesign())) && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{

		// get the design's uid and the subsystem ref.
		String uid = getUID();
		String subsystem = getSubsystem();

		// stop the simulation
		LogicAnalysisServices.getAnalysisServices()
				.setDynamicSimulationMode(uid, subsystem, LogicAnalysisServices.DYN_SIM_OFF, null);

		// remove the tool tip provider

		//((LogicAnalysisServices)LogicAnalysisServices.getAnalysisServices( )).setTooltipsEnabledForDesign( m_model.getDesign( ), false ) ;
		disableToolTips();

		// ensure all coloring is removed.
		IDynamicGfxService dgs = m_model.getDynamicGfxService();
		dgs.removeAllTransientGfx();

		// Re-set initial pre-selection
		getController().getSelectMgr().removeSelectSet();

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}

		safelyUpdateUI();

		// return and wait for the callback
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}

	protected String getUID()
	{
		if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return LogicAnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			return m_model.getDesign().getUID().toString();
		}
	}

	private void safelyUpdateUI()
	{
		if (CAFUtils.getInstance() != null && getController() != null && getController().getCaplet() != null) {
			CAFUtils.getInstance().tickleUI(getController().getCaplet().getFIB());
		}
	}

	protected String getSubsystem()
	{
		if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return "" + LogicAnalysisServices.getCurrentAnalysisNetlistScope().getProjectId();
		}
		else {
			return m_model.getDesign().getAnalysisSubsystem();
		}
	}

	protected void disableToolTips()
	{
		if (LogicAnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).setTooltipsEnabledForScope(
					LogicAnalysisServices.getCurrentAnalysisNetlistScope(), false);
		}
		else {
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).setTooltipsEnabledForDesign(
					m_model.getDesign().getUID(), false);
		}
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}