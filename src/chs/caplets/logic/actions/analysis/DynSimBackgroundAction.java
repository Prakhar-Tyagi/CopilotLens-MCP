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
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.ICapitalAnalysis;
import chs.analysis.scope.AnalysisNetlistScopeFactory;
import chs.ans.ANSServices;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.analysis.AbstractAnalysisControllerActionRT;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.buildlist.ILogicAnalysisBuildList;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

/**
 * @author rharring
 */
public class DynSimBackgroundAction extends AbstractAnalysisControllerActionRT
{

	Model m_model;

	protected static boolean startingSimulation;
	protected ICapitalAnalysis m_capitalAnalysis;

	/**
	 * Creates a new instance of SimulateAction
	 */
	public DynSimBackgroundAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
		startingSimulation = false;
		m_capitalAnalysis = CapitalAnalysisFactory.getAnalysisInterface();
		setUndoableAction(false);
	}

	public String getActionUIClass()
	{
		return DynSimBackgroundActionUI.class.getName();
	}

	public Cursor getCursor()
	{
		return CAFUtils.getInstance().loadCursor(Cursor.WAIT_CURSOR);
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(DynSimBackgroundAction.class, "DynSimBackgroundAction.String.statusbarText");
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		//if another analysis opereation is in progress, alert the user with a message
		if (AnalysisServices.getAnalysisServices().isAnalysisOperationActive()) {
			// display error message
			AnalysisServices.getAnalysisServices().raiseErrorAnalysisOperationStarted();

			// return to prevent later crashes...
			return IActionEnum.eCanceled;
		}
		if(!AnalysisServices.isRelayServerConnectionAvailable()){
			return IActionEnum.eCanceled;
		}
		if (startingSimulation) {
			// display error message
			MessageHelper.showInformationMessage(CAFUtils.getInstance().getDialogFrame(),
					ResourceMgr.getString(DynSimBackgroundAction.class, "DynSimBackgroundAction.alreadyStarted.title"),
					ResourceMgr.getString(DynSimBackgroundAction.class,
							"DynSimBackgroundAction.alreadyStarted.message"));
			// return to prevent later crashes...
			return IActionEnum.eCompleted;
		}

		String simType = "electrical.qualitative";
		/*BackgroundSimulationSelectionDialog dialog = new BackgroundSimulationSelectionDialog( ) ;
			  dialog.setVisible( true ) ;
			  if ( ! dialog.isCancelled( ) ) {
				 simType = dialog.getSelectedSimulationType() ;
			  } else {
				 return IActionEnum.eCanceled ;
			  }*/

		final String uid = getUID();
		if(m_capitalAnalysis != null) {
			m_capitalAnalysis.setAnalysisClient(uid, this);
		}
		final String subsystem = getSubsystem();
		final ICapletController ctrl = getController();
		final String simulationType = LogicAnalysisServices.getAnalysisServices().getSimulationType();
		Runnable r = new Runnable()
		{
			public void run()
			{
				// we're starting a simulation now!
				startingSimulation = true;

				ANSServices.begin();
				try {
					// if there is no scope set it to this scope.
					boolean startSimulation = true;
					if (AnalysisServices.getCurrentAnalysisNetlistScope() == null) {
						AnalysisServices.changeScope(m_model.getDesign(),
								CAFUtils.getInstance().getCurrentProject().getUID().toString());
						CAFUtils.getInstance().getActiveCapletController().addAuditTab();
					}
					else if (!AnalysisServices.getCurrentAnalysisNetlistScope().isInScope(m_model.getDesign())) {
						startSimulation = false;
						MessageHelper.showInformationMessage(CAFUtils.getInstance().getDialogFrame(),
								ResourceMgr.getString(DynSimBackgroundAction.class,
										"DynSimBackgroundAction.notInScope.title"),
								ResourceMgr.getString(DynSimBackgroundAction.class,
										"DynSimBackgroundAction.notInScope.message"));
					}
					if (startSimulation) {
						// install tooltips
						//((LogicAnalysisServices)LogicAnalysisServices.getAnalysisServices( )).setTooltipsEnabledForDesign( m_model.getDesign( ), true ) ;
						enableToolTips();

						// set the dynamic simulation mode
						LogicAnalysisServices.getAnalysisServices().setDynamicSimulationMode(uid, subsystem,
								AnalysisServices.DYN_SIM_BACKGROUND, simulationType);

						// do the simulation
						//((LogicAnalysisServices)LogicAnalysisServices.getAnalysisServices( )).updateSimulation( m_model ) ;
						updateSimulation(uid);
					}
				}
				finally {
					// we're no longer starting a simulation
					startingSimulation = false;
					ANSServices.end();

					//reset the flag as the current opeation completed and another analysis operation can be performed
					AnalysisServices.getAnalysisServices().setIsAnalysisOperationActive(false);
				}

				//Having the Controller missing means that we the Diagram was closed from which simulation was started.
				if (m_model.getController() == null) {
					boolean keepSimulation = false;
					IAnalysisNetlistScope netlistScope = AnalysisServices.getCurrentAnalysisNetlistScope();
					if(netlistScope != null) {
						IUIDObject scopedObject = netlistScope.getScopedObject();
						//If we have a build list scoped we might want to keep the simulation
						//as some of diagrams under scope still might be open.
						if (scopedObject instanceof ILogicAnalysisBuildList) {
							ILogicAnalysisBuildList buildList = (ILogicAnalysisBuildList) scopedObject;
							//If any of scoped designs has at least one diagram open we must keep the simulation
							keepSimulation = buildList.getDesigns().stream()
									.anyMatch(this::designHasAtLeastOneDiagramOpen);
						}

						//If there is no reason to keep simulation(i.e. a diagram from same scope is open) we must stop it.
						if (!keepSimulation) {
							LogicAnalysisServices.getAnalysisServices().setDynamicSimulationMode(uid, subsystem,
									AnalysisServices.DYN_SIM_OFF, simulationType);
						}
					}
				}
				else {
					// refresh the ui
					safelyUpdateUI();
				}
			}

			private boolean designHasAtLeastOneDiagramOpen(ILogicDesign iLogicDesign)
			{
				return iLogicDesign.getDiagrams().stream()
						.anyMatch(ISchemDiagram::isFullyLoaded);
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

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}

	protected String getUID()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return AnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
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

	protected void updateSimulation(String uid)
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
					AnalysisServices.getCurrentAnalysisNetlistScope());
		}
		else {
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
					m_model, false, uid);
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