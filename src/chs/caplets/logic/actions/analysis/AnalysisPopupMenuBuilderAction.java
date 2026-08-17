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
import chs.caf.ActionCheckBox;
import chs.caf.ActionContainer;
import chs.caf.ActionSeparator;
import chs.caf.ICtxMenuProvider;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.analysis.AttachSVModelActionUI;
import chs.caf.cafmain.actions.analysis.EditModelActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.AssociateSymbolActionUI;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.caplets.logic.analysis.ui.AnalysisPropertiesDialog;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;

import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is a HACK to enable the correct building of the analysis popup menus.
 * <p/>
 * This single actions uses the caplet and model to get a session and populate the menus correctly.
 * <p/>
 * This ensures actions are not split across two submenus and that the created actions for setting failures / properties
 * are correctly implemented.
 *
 * @author rharring
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
				Application.SEElectricalDesign})
public class AnalysisPopupMenuBuilderAction extends ControllerActionRT implements ICtxMenuProvider
{

	public static final String analysisMenu =
			ResourceMgr.getString(AnalysisPopupMenuBuilderAction.class,
					"AnalysisPopupMenuBuilderAction.String.analysisMenu");
	public static final String toolsMenu =
			ResourceMgr
					.getString(AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.toolsMenu");
	public static final String generalMenu =
			ResourceMgr.getString(AnalysisPopupMenuBuilderAction.class,
					"AnalysisPopupMenuBuilderAction.String.generalMenu");
	public static final String simMenu =
			ResourceMgr
					.getString(AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.simMenu");
	public static final String menuSep = ",";
	public static final String results =
			ResourceMgr
					.getString(AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.results");
	public static final String componentMenu =
			ResourceMgr.getString(AnalysisPopupMenuBuilderAction.class,
					"AnalysisPopupMenuBuilderAction.String.componentMenu");
	public static final String subsystemMenu =
			ResourceMgr.getString(AnalysisPopupMenuBuilderAction.class,
					"AnalysisPopupMenuBuilderAction.String.subsystemMenu");

	//static EditModelAction editModelAction ;

	Model m_model;

	/**
	 * Creates a new instance of AnalysisPopupMenuBuilderAction
	 */
	public AnalysisPopupMenuBuilderAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
		//LogicAnalysisAttachmentTargetProvider provider =
		//   new LogicAnalysisAttachmentTargetProvider( m_model ) ;
		//editModelAction = new EditModelAction( getCaplet( ).getFIB( ), provider ) ;
	}

//	public boolean isValid() {
//		return true;
//	}

	//

	protected ICaplet getCaplet()
	{
		return getController().getCaplet();
	}

	protected String getUid()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			return AnalysisServices.getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			return m_model.getDesign().getUID().getString();
		}
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		//System.err.println("*****\nI've been asked to populate the pop up. " + hashCode( ) + "\n****" );

		// if nothing's selected we want a standard analysis menu setup
		String prefix = "";
		ILogicObject selectedObject = getSelectedObject(selections);
		String uid = getUid(); //m_model.getDesign( ).getUID( ).getString( ) ;

		if (selectedObject != null) {

			if (LogicAnalysisServices.getAnalysisServices().getDynamicSimulationMode(uid) != LogicAnalysisServices
					.DYN_SIM_OFF) {
				buildSimulationMenu(container, prefix, selectedObject);
			}

			buildComponentMenu(container, prefix, false);
		}
	}

//	public void buildToolsMenu(ActionContainer container, String prefix)
//	{
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						LauncherAction.class.getName()),
//				analysisMenu);
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						ComponentModelBuilderAction.class.getName()),
//				analysisMenu + menuSep + prefix + toolsMenu);
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						FMEAEditorAction.class.getName()),
//				analysisMenu + menuSep + prefix + toolsMenu);
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						FunctionBuilderAction.class.getName()),
//				analysisMenu + menuSep + prefix + toolsMenu);
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						ObjectManagerAction.class.getName()),
//				analysisMenu + menuSep + prefix + toolsMenu);
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getFIB().getAppActionMgr().getAction(
//						QSchemeBuilderAction.class.getName()),
//				analysisMenu + menuSep + prefix + toolsMenu);
//	}

	public void buildComponentMenu(ActionContainer container, String prefix, boolean includeComponentTag)
	{
		String path = analysisMenu + menuSep + prefix;
		if (includeComponentTag) {
			path += componentMenu;
		}

		LogicAnalysisServices.addActionToMenu(container,
				getCaplet().getActionUI(LogicAttachModelActionUI.class.getName()),
				path);

		//LogicAnalysisServices.addActionToMenu( container,
		//                                  editModelAction,
		//                                  analysisMenu + menuSep + prefix + componentMenu ) ;

		LogicAnalysisServices.addActionToMenu(container,
				getCaplet().getActionUI(LogicBuildModelActionUI.class.getName()),
				path);
		LogicAnalysisServices.addActionToMenu(container,
				getCaplet().getActionUI(EditModelActionUI.class.getName()),
				path);
		LogicAnalysisServices.addActionToMenu(container,
				getCaplet().getActionUI(AssociateSymbolActionUI.class.getName()),
				path);
        LogicAnalysisServices.addActionToMenu(container,
                getCaplet().getActionUI(AttachSVModelActionUI.class.getName()),
                path);
	}

//	public void buildSubsystemMenu(ActionContainer container, String prefix)
//	{
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemEditorActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemSimulatorActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemFMEAActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemSCAActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemStressActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//
//		LogicAnalysisServices.addActionToMenu(container,
//				getCaplet().getActionUI(SubsystemImportActionUI.class.getName()),
//				analysisMenu + menuSep + prefix + subsystemMenu);
//	}

	public void buildSimulationMenu(ActionContainer container, String prefix, ILogicObject obj)
	{
		String designUID = getUid(); // m_model.getDesign( ).getUID( ).toString( ) ;
		//System.err.println("Design uid = " + designUID ) ;
		String componentRef = obj.getName();
		//System.err.println("Compref : "+ componentRef ) ;

		IAnalysisSimulationSessionController session =
				LogicAnalysisServices.getAnalysisServices().getSimSession(designUID);
		if (session != null) {
			DefaultMutableTreeNode propertyRoot = session.getComponentInputProperties(designUID, componentRef);
			if (propertyRoot != null) {
				createPropertyActionTree(container, prefix, propertyRoot, designUID, componentRef, session);
			}
			DefaultMutableTreeNode failureRoot = session.getComponentFailures(designUID, componentRef);
			if (failureRoot != null) {
				createFailureActionTree(container, prefix, failureRoot, designUID, componentRef, session);
			}

			// try and get the properties from the last simulation.
			Map<String,Map<String,String>> designResults = LogicAnalysisServices.getAnalysisServices().getSimulationResultVariables(designUID);
			if (designResults != null) {
				Map<String,String> componentResults =  designResults.get(componentRef);
				if (componentResults != null) {
					ShowComponentPropertiesAction action =
							new ShowComponentPropertiesAction(designUID, componentRef, componentResults);
					LogicAnalysisServices.addActionToMenu(container, action, analysisMenu + menuSep + prefix, -1);
				}
			}

			LogicAnalysisServices
					.addActionEntryToMenu(container, new ActionSeparator(), analysisMenu + menuSep + prefix, -1);
		}
	}

	protected void createPropertyActionTree(ActionContainer container,
			String prefix,
			DefaultMutableTreeNode root,
			String uid,
			String componentName,
			IAnalysisSimulationSessionController session)
	{

		if (root == null || root.getChildCount() == 0) {
			return;
		}

		String propertyContainerName = (String) root.getUserObject();
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			String data = (String) child.getUserObject();
			int pos = data.indexOf(menuSep);
			String propertyName = data.substring(0, pos); // get the name
			pos++; // increment beyond the menu sep we've found
			int nextPos = data.indexOf(menuSep, pos);
			String value = data.substring(pos, nextPos); // get the value
			nextPos++; // increment beyond the menu sep we've found
			boolean isSet =
					Boolean.valueOf(data.substring(nextPos, data.lastIndexOf(menuSep))); // get the setting

			ActionCheckBox box = new ActionCheckBox(
					new InputPropertyAction(propertyName, value, componentName, session, uid), isSet);
			LogicAnalysisServices.addActionEntryToMenu(container, box,
					analysisMenu + menuSep + prefix + propertyContainerName + menuSep + propertyName, -1);
		}
	}

	protected void createFailureActionTree(ActionContainer container,
			String prefix,
			DefaultMutableTreeNode root,
			String uid,
			String componentName,
			IAnalysisSimulationSessionController session)
	{
		if (root == null || root.getChildCount() == 0) {
			return;
		}

		String failureContainerName = (String) root.getUserObject();

		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			String data = (String) child.getUserObject();
			String failureName = data.substring(0, data.indexOf(menuSep));
			boolean setting = Boolean.valueOf(data.substring(data.indexOf(menuSep) + 1, data.length())).booleanValue();
			ActionCheckBox box = new ActionCheckBox(
					new FailureModeAction(failureName, componentName, session, uid), setting);
			LogicAnalysisServices
					.addActionEntryToMenu(container, box, analysisMenu + menuSep + prefix + failureContainerName, -1);
		}
	}

	public ILogicObject getSelectedObject(SelectSet selections)
	{

		IUIDObject uidObj = null;

		// HSDP100009677, if a wire is selected segments are selected as well as a conductor and
		// hence the count > 1. Only the Conductor is a represented object so ...
		if (selections.getSelectCount() > 1) {
			SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();
			ArrayList representedObjects = new ArrayList();
			while (iter.hasNext()) {
				IUIDObject select = iter.getNext();
				if (select instanceof IRepresentedObject) {
					representedObjects.add(select);
				}
			}

			if (representedObjects.size() == 1) {
				uidObj = (IUIDObject) representedObjects.get(0);
			}
		}

		// if we still have too many selections we don't have a target.
		if (selections.getSelectCount() != 1 && uidObj == null) {
			return null;
		}

		// If we have exactly one object selected, then see if it is one
		// we care about and if so make sure it already has an analysis
		// model associated with it.
		if (uidObj == null) { // if we haven't already selected an object...
			uidObj = selections.getSelectedUIDObjects().getNext();
		}

		if (uidObj instanceof IRepresentedObject) {
			IRepresentedObject repObj = (IRepresentedObject) uidObj;

			IUIDObject connObj = repObj.getRawConnectivity();
			if (connObj instanceof ILogicObject) {
				ILogicObject logicObj = (ILogicObject) connObj;

				if (logicObj instanceof IDevice ||
						(logicObj instanceof IConnector) ||
						(logicObj instanceof ISplice) ||
						(logicObj instanceof IWireConductor) ||
						(logicObj instanceof IShieldConductor)) {
					return logicObj;
				}
			}
		}

		return null;
	}

	public String getActionUIClass()
	{
		return AnalysisPopupMenuBuilderActionUI.class.getName();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eActivated;
	}

	protected boolean onTerminate(boolean successful)
	{
		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
	}

	public void updateUI()
	{
		String uid = getUid();
		// ensure the correct analysis control panel is visible
		if (LogicAnalysisServices.getAnalysisServices().getDynamicSimulationMode(uid) == AnalysisServices.DYN_SIM_OFF) {
			AnalysisServices.getAnalysisServices().setControlPanelMode(AnalysisServices.OUTPUT_WINDOW_TEXT);
		}
		else {
			AnalysisServices.getAnalysisServices().setControlPanelMode(AnalysisServices.OUTPUT_WINDOW_SIM);
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	// ///////////// //
	// Inner actions //
	// ///////////// //

	@ApplicationSpecification(
			includeIn = {Application.CapitalCapture, Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
					Application.SEElectricalDesign})
	class FailureModeAction extends AbstractAction
	{

		String failureName, componentName;
		IAnalysisSimulationSessionController session;
		String uid;

		public FailureModeAction(String failureName, String componentName, IAnalysisSimulationSessionController session,
				String uid)
		{
            this.failureName = failureName;
			this.componentName = componentName;
			this.session = session;
			this.uid = uid;

			putValue(NAME, failureName);
			//putValue( SHORT_DESCRIPTION,
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.change")  + failureName +
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.failureModeAction.failureSetting")  ) ;
		}

		public void actionPerformed(ActionEvent ae)
		{
			boolean failed = ((ActionCheckBox) ae.getSource()).getState();
			session.setFailure(uid, componentName, failureName, failed);
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(m_model);
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalCapture, Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
					Application.SEElectricalDesign})
	class InputPropertyAction extends AbstractAction
	{

		String valueName, propertyName, componentName;
		IAnalysisSimulationSessionController session;
		String uid;

		public InputPropertyAction(String propertyName, String valueName, String componentName,
				IAnalysisSimulationSessionController session, String uid)
		{
            this.propertyName = propertyName;
			this.valueName = valueName;
			this.componentName = componentName;
			this.session = session;
			this.uid = uid;

			putValue(NAME, valueName);
			//putValue( SHORT_DESCRIPTION,
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.change") + propertyName +
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.inputPropertyAction.propertySetting") ) ;
		}

		public void actionPerformed(ActionEvent ae)
		{
			boolean failed = ((ActionCheckBox) ae.getSource()).getState();
			session.setProperty(uid, componentName, propertyName, valueName);
			((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(m_model);
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.CapitalCapture, Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
					Application.SEElectricalDesign})
	class ShowComponentPropertiesAction extends AbstractAction
	{

		String componentName;
		Map<String,String> componentVariables;
		String uid;

		public ShowComponentPropertiesAction(String uid, String componentName, Map<String,String> variables)
		{
            this.componentName = componentName;
            componentVariables = variables;
			putValue(NAME, results);
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AnalysisPopupMenuBuilderAction.class,
					"AnalysisPopupMenuBuilderAction.String.propertiesAction.desc"));
		}

		public void actionPerformed(ActionEvent e)
		{
			//System.err.println( componentVariables ) ;
			Iterator<String> i = componentVariables.keySet().iterator();
			List<List<String>> tableData = new ArrayList<List<String>>();
			while (i.hasNext()) {
				String key = i.next();
				String data = componentVariables.get(key);
				List<String> row = new ArrayList<String>(); // this is fed to JTable, need to be sure we can change to
				                                           // JList -- todo rharring, sram
				row.add(key);
				row.add(data);
				tableData.add(row);
			}
			AnalysisPropertiesDialog dialog = new AnalysisPropertiesDialog(uid, tableData, componentName);
			dialog.setVisible(true);
		}
	}

	/* class PropertiesDialog extends CAFOkCancelDialog {
		  public PropertiesDialog( Vector data, String component ) {
			 super( CAFUtils.getInstance().getWindowMgr( ).getDialogFrame(),
					ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.propertiesAction.dialogTitle") + component , true ) ;
			 //setTitle( "Properties for " + component + "..." ) ;
			 createGui( data ) ;
			 addActionListeners( ) ;
			 pack( ) ;
			 //setModal( true ) ;
		  }

		  protected void createGui( Vector data ) {
			 Vector columns = new Vector( ) ;
			 columns.addLogicElement( ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.propertiesAction.propertyColumn") ) ;
			 columns.addLogicElement( ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.propertiesAction.valueColumn") ) ;

			 JTable table = new JTable( data, columns ) {
				public boolean isCellEditable( int row, int col ) {
				   return false ;
				}
			 };

			 JScrollPane pane = new JScrollPane( table ) ;
			 pane.getViewport().setBackground( table.getBackground( ) ) ;
			 getContentPane( ).add( pane, BorderLayout.CENTER ) ;

		  }

		  protected void addActionListeners( ) {
			 ActionListener al = new ActionListener( ) {
				public void actionPerformed( ActionEvent ae ) {
				   setVisible( false ) ;
				}
			 }  ;

			 // don't really want any differences so add same listener to both.
			 // An ok buttoned dialog would be better.......
			 getOkButton( ).addActionListener( al ) ;
			 getCancelButton( ).addActionListener( al ) ;
		  }

	   }*/
}
