/*
 * Copyright 2004-2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.IAnalysisSimulationSessionController;
import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This action allows the interface property on a  component ( or the first found ) on a component with multiple
 * properties to be toggled through quickly.
 *
 * @author rharring
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class AnalysisInterfaceToggleAction extends ControllerActionRT implements IActionUI
{

	// //////////////// //
	// Resource strings //
	// //////////////// //

	protected final String noComponentSelectedString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.noComponentSelected");
	protected final String multipleComponentsSelectedString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.multipleComponentsSelected");
	protected final String examiningComponentString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.examiningComponentSelected");
	protected final String changingPropertyString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.changingProperty");
	protected final String changingPropertyToString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.changingPropertyTo");
	protected final String noPropertyString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.noPropertyToChange");
	protected final String notSimulatingString =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.notSimulating");
	protected final String statusBarText =
			ResourceMgr.getString(AnalysisInterfaceToggleAction.class,
					"AnalysisInterfaceToggleAction.String.statusBarText");

	HashMap<String, Object> values;
	ArrayList<ILogicObject> m_targetObjects;

	/**
	 * Creates a new instance of AnalysisPopupMenuBuilderAction
	 */
	public AnalysisInterfaceToggleAction(ICapletController c)
	{
		super(c);
		values = new HashMap<String, Object>();
		m_targetObjects = new ArrayList<ILogicObject>();
		setupUI();
	}

	public String getActionUIClass()
	{
		return AnalysisInterfaceToggleAction.class.getName();
	}

	public java.awt.Cursor getCursor()
	{
		return null;
	}

	public String getStatusbarText()
	{
		return statusBarText;
	}

	private Model getModel()
	{
		return (Model) getController().getCapletModel();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		//System.err.println("I've been pressed with Ctrl-Space onAct" ) ;
		// ok we want to
		//    a) get the selection set, to find the selected component
		// assuming we're simming....
		//    b) find the ComponentInformation pertinent to the selected component
		//    c) set the property
		//    d) update the simulation.....

		// obtain selection
		setSelectedLogicObjects();
		if (m_targetObjects.size() == 0) {
			LogicAnalysisServices.getAnalysisServices().getControlPanel().setMessage(noComponentSelectedString);
			return IActionEnum.eCanceled;
		}
		else if (m_targetObjects.size() > 1) {
			LogicAnalysisServices.getAnalysisServices().getControlPanel().setMessage(multipleComponentsSelectedString);
		}

		// first get the sim session for the given model
		final String uid = getModel().getDesign().getUID().toString();
		final IAnalysisSimulationSessionController session =
				LogicAnalysisServices.getAnalysisServices().getSimSession(uid);

		if (session != null) {
			// get the components name
			ILogicObject namedObject = m_targetObjects.get(0);
			final String name = namedObject.getName();
			LogicAnalysisServices.getAnalysisServices().getControlPanel().setMessage(examiningComponentString + name);

			// create a runnable to do the work whilst allowing the ui to update....
			Runnable r = new Runnable()
			{
				public void run()
				{
					// try and change the switch position, will return false if it can't
					// or there isn't a new switch position....
					String setting = session.incrementInterfacePosition(uid, name);

					// if we changed the switch position then lets resimulate,
					if (setting != null) {
						String propertyName = setting.substring(0, setting.indexOf(","));
						String value = setting.substring(setting.indexOf(",") + 1, setting.length());
						LogicAnalysisServices.getAnalysisServices().getControlPanel().addMessage(
								changingPropertyString + propertyName + " " + changingPropertyToString + value);

						// This may take some time so we thread it off to avoid user visible delays...

						((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices())
								.updateSimulation(getModel());
					}
					else {
						LogicAnalysisServices.getAnalysisServices().getControlPanel().addMessage(noPropertyString);
					}
				}
			};
			new Thread(r).start();
		}
		else {
			LogicAnalysisServices.getAnalysisServices().getControlPanel().addMessage(notSimulatingString);
		}

		return IActionEnum.eCanceled;
	}

	protected boolean onTerminate(boolean successful)
	{
		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
	}

	public String getActionClass()
	{
		return getClass().getName();
	}

	public String getActionUIName()
	{
		return getClass().getName();
	}

	@Nullable public String getActionUIInstanceName()
	{
		return null;
	}

	public String getToolTipText()
	{
		return null;
	}

	@NotNull public ICaplet getCaplet()
	{
		return getController().getCaplet();
	}

	public Object getValue(String key)
	{
		return values.get(key);
	}

	public void putValue(String key, Object value)
	{
		values.put(key, value);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
	}

	public void setEnabled(boolean b)
	{
	}

	public void setupUI()
	{
		//System.err.println( KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, java.awt.Event.CTRL_MASK ) ) ;
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, java.awt.Event.CTRL_MASK));
	}

	public void updateUI()
	{
	}

	// ///////////// //
	// Other methods //
	// ///////////// ///

	void setSelectedLogicObjects()
	{
		// First clear it out
		m_targetObjects.clear();
		Map<IPinList, chs.cof.logical.schem.IPinList> backMap = new HashMap<IPinList, chs.cof.logical.schem.IPinList>();

		// Get the pre selections to see if there is exactly on to work with
		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (selectMgr == null) {
			return;
		}

		SelectSet selections = selectMgr.getPreSelections();

		if (selections.getSelectCount() == 1) {
			// If we have exactly one object selected, then see if it is one
			// we care about and if so make sure it already has an analysis
			// model associated with it.
			IUIDObject uidObj = selections.getSelectedUIDObjects().getNext();

			if (uidObj instanceof IRepresentedObject) {
				IRepresentedObject repObj = (IRepresentedObject) uidObj;

				IUIDObject connObj = repObj.getRawConnectivity();
				if (connObj instanceof ILogicObject) {
					ILogicObject logicObj = (ILogicObject) connObj;

					if (logicObj instanceof IDevice ||
							(logicObj instanceof IConnector) ||
							(logicObj instanceof ISplice) ||
							(logicObj instanceof IWireConductor) ||
							(logicObj instanceof IShieldConductor)
							) {
						m_targetObjects.add(logicObj);
					}
				}
			}
		}
		else if (selections.getSelectCount() > 1) {
			// There are multiple objects selected.  See if they are all either conductors
			// or pin lists with the same symbol reference.  The first element into the selected
			// objects is what all other objects will compare against.
			for (SelectedUIDObjectIterator sit = selections.getSelectedUIDObjects(); sit.hasNext();) {
				IUIDObject obj = sit.getNext();
				if (obj instanceof chs.cof.logical.schem.IConductor) {
					chs.cof.logical.schem.IConductor schemConductor = (chs.cof.logical.schem.IConductor) obj;
					ILogicObject logicObject = (ILogicObject) schemConductor.getRawConnectivity();

					// We don't allow model attachment to Nets, so make sure we
					// don't have a net.
					if (logicObject instanceof INetConductor) {
						m_targetObjects.clear();
						return;
					}

					if (m_targetObjects.isEmpty()) {
						// This is the first one in so just add it
						m_targetObjects.add(logicObject);
					}
					else if (m_targetObjects.get(0) instanceof IConductor) {
						// Already one of these in, so put this one in
						m_targetObjects.add(logicObject);
					}
					else {
						// Not a match and there is already something in, so get out
						// since there is a mix of objects
						m_targetObjects.clear();
						return;
					}
				}
				else if (obj instanceof chs.cof.logical.schem.IPinList) {
					chs.cof.logical.schem.IPinList schemPinList = (chs.cof.logical.schem.IPinList) obj;
					IPinList pinList = (IPinList) schemPinList.getRawConnectivity();
					if (m_targetObjects.isEmpty()) {
						// If this pinList has a symbol then it has potential, but if not then
						// we don't support multiple attachment.
						if (schemPinList.getSymbolRef() != null) {
							// This is the first one in so just add it
							m_targetObjects.add(pinList);
							backMap.put(pinList, schemPinList);
						}
						else {
							m_targetObjects.clear();
							return;
						}
					}
					else if (m_targetObjects.get(0) instanceof IPinList) {
						// See if the pin lists have the same symbol
						IPinList firstPL = (IPinList) m_targetObjects.get(0);
						chs.cof.logical.schem.IPinList firstSchemPinList = backMap.get(firstPL);
						if (schemPinList.getSymbolRef() != null && (schemPinList.getSymbolRef().getSymbolUID() ==
								firstSchemPinList.getSymbolRef().getSymbolUID())) {
							// We have a match, so put it in
							m_targetObjects.add(pinList);
							backMap.put(pinList, schemPinList);
						}
						else {
							// Symbols don't match, so get out
							m_targetObjects.clear();
							return;
						}
					}
					else {
						// Not a match and there is already something in, so get out
						// since there is a mix of objects
						m_targetObjects.clear();
						return;
					}
				}
			}
		}
	}
}