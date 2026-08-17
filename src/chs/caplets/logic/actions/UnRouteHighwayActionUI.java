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
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

/**
 * @author: Balaraju Kadukuntla
 * @Date: Mar 26, 2010 3:16:12 PM
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_UN_ROUTE_HIGHWAY_ACTION",
		label = "Un-Route...",
		tooltip = "Un-Route...(Ctrl+U)",
		icon = "ico_unroute_from_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class UnRouteHighwayActionUI extends ActionUI implements ISelectListener
{

	public UnRouteHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK);
		Integer iMnemonic = new Integer(KeyEvent.VK_U);
		putValue(NAME, ResourceMgr.getString(UnRouteHighwayActionUI.class, "UnRouteHighwayActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UnRouteHighwayActionUI.class, "UnRouteHighwayActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(UnRouteHighwayActionUI.class, "UnRouteHighwayActionUI.longDesc.decl"));

		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return UnRouteHighwayAction.class.getName();
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
				if (IConductor.class.isAssignableFrom(sel.getSelectionClass())) {
					conductorSelected = true;
				}
				else if (IHighwaySchematic.class.isAssignableFrom(sel.getSelectionClass())) {
					numberOfHighwaysSelected++;
				}
			}
			setEnabled(conductorSelected && numberOfHighwaysSelected == 1);
		}
	}
}
