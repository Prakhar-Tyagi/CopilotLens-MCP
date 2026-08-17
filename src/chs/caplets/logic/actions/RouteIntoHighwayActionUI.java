/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.SingleLineHelper;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

/**
 * @author: Balaraju Kadukuntla
 * @Date: Mar 18, 2010 2:28:54 PM
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ROUTE_INTO_HIGHWAY_ACTION",
		label = "Route into Highway",
		tooltip = "Route into Highway(Ctrl+H)",
		icon = "ico_route_into_highway_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class RouteIntoHighwayActionUI extends ActionUI implements ISelectListener
{

	public RouteIntoHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.CTRL_MASK);
		Integer iMnemonic = new Integer(KeyEvent.VK_H);
		putValue(NAME, ResourceMgr.getString(RouteIntoHighwayActionUI.class, "RouteIntoHighwayActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(RouteIntoHighwayActionUI.class, "RouteIntoHighwayActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(RouteIntoHighwayActionUI.class, "RouteIntoHighwayActionUI.longDesc.decl"));

		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return RouteIntoHighwayAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		// only enabled if a conductor(s) and a highway are selected
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			SelectSet presel = activeSM.getPreSelections();
			int numberOfHighwaysSelected = 0;
			boolean conductorSelected = false;
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext();) {
				Selection sel = iter.getNext();
				ILogicObject logicObj = null;
				IUIDObject selectedObject = sel.getObject();
				if (selectedObject instanceof IConductor) {
					logicObj = ((IConnectivityRef) selectedObject).getConnectivity();
				}
				if (selectedObject instanceof chs.cof.logical.cable.IConductor) {
					logicObj = (ILogicObject) selectedObject;
				}
				if (IConductor.class.isAssignableFrom(sel.getSelectionClass()) &&
						logicObj instanceof IHighwayConductor) {
					conductorSelected = true;
				}
				else if (IHighwaySchematic.class.isAssignableFrom(sel.getSelectionClass())
						&& !SingleLineHelper.isSingleLineSchematic((IHighwaySchematic) selectedObject)) {
					numberOfHighwaysSelected++;
				}
			}
			setEnabled(conductorSelected && numberOfHighwaysSelected == 1);
		}
	}
}
