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
public class SimulateAction extends AbstractAnalysisControllerActionRT
{

	protected final String simulatingString =
			ResourceMgr.getString(SimulateAction.class, "SimulateAction.String.simulating");
	protected boolean isSimulating;

	/**
	 * Creates a new instance of SimulateAction
	 */
	public SimulateAction(ICapletController c)
	{
		super(c);
		isSimulating = false;
		setUndoableAction(false);
	}

	protected Model getModel()
	{
		return (Model) getController().getCapletModel();
	}

	public String getActionUIClass()
	{
		return SimulateActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(SimulateAction.class, "SimulateAction.String.statusbarText");
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
				LogicAnalysisServices.getAnalysisServices().getDynamicSimulationMode(
						uid) ==
						AnalysisServices.DYN_SIM_DEMAND && !isSimulating && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
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

		LogicAnalysisServices.getAnalysisServices().getControlPanel().setMessage(simulatingString);

		final String uid = getUid();
		//String subsystem = m_model.getDesign( ).getAnalysisSubsystem( ) ;

		Runnable r = new Runnable()
		{
			public void run()
			{
				IAnalysisSimulationSessionController session =
						LogicAnalysisServices.getAnalysisServices().getSimSession(uid);
				if (session != null) {
					isSimulating = true;
					CAFUtils.getInstance().tickleUI(getController().getCaplet().getFIB());

					try {
						if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
							((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
									AnalysisServices.getCurrentAnalysisNetlistScope(), true);
						}
						else {
							((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices())
									.updateSimulation(getModel(), true, uid);
						}
						LogicAnalysisServices.getAnalysisServices().setSimulationPropertiesModified(false);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					finally {
						isSimulating = false;
						//reset the flag as the current opeation completed and another analysis operation can be performed
						AnalysisServices.getAnalysisServices().setIsAnalysisOperationActive(false);
					}
				}
				ICapletController controller = getController();
				if (controller != null) {
					CAFUtils.getInstance().tickleUI(controller.getCaplet().getFIB());
				}
			}
		};

		// we need to perform the simulation off the event thread...
		Thread t = new Thread(r);
		//set the flag to prevent invoking another analysis operation when an operation is already in progress
		AnalysisServices.getAnalysisServices().setIsAnalysisOperationActive(true);
		t.start();

		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
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

	/*  public void showLicenseError(Exception e) {
	   }

	  public void updateColoring(String uid, byte[] coloring) {
		  System.err.println("Update coloring for " + uid ) ;
		   String s = new String( coloring ) ;
		   System.err.println( s ) ;
				   // Clean-up
		   IDynamicGfxService dgs = m_model.getDynamicGfxService();
		   dgs.removeAllTransientGfx();



		   try {
			  LogicAnalysisColoringImporter importer = new LogicAnalysisColoringImporter( m_model ) ;
			  importer.importStream(new ByteArrayInputStream(coloring));
			  AnalysisFunctionImporter functionImporter = new AnalysisFunctionImporter( ) ;
			  functionImporter.importStream( new ByteArrayInputStream( coloring ) ) ;
			  Vector functions = functionImporter.getFunctionList( ) ;
			  LogicAnalysisServices.getAnalysisServices( ).getControlPanel( ).showFunctions( functions ) ;
		   } catch ( Exception e ) {
			  e.printStackTrace( ) ;
		   }

		   // Refresh view
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view != null) {
				view.invalidate(IViewInvalidationEnum.eTransient);
			}



	   }

	   public void startingSimulation() {
	   }

	   public void stoppingSimulation() {
	   }
	   */
}
