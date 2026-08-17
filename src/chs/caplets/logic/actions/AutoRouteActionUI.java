/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
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
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Description of the Class
 *
 * @author Steve Geisler
 * @created August 12, 2002
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_AUTO_ROUTE_ACTION",
		label = "Route",
		tooltip = "Route(Ctrl+R)",
		icon = "ico_route_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AutoRouteActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the AutoRouteActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AutoRouteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_route_active.gif");
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.Event.CTRL_MASK);
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_R);

		String name = ResourceMgr.getString(AutoRouteActionUI.class, "AutoRouteActionUI.name.decl");
		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, name);
		putValue(LONG_DESCRIPTION, getLongDescription(getCaplet()));
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
		putValue(MNEMONIC_KEY, iMnemonic);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	private String getLongDescription(ICaplet caplet)
	{
		if(caplet != null && caplet.isFunctionCaplet()){
			return ResourceMgr.getString(AutoRouteActionUI.class, "AutoRouteActionUI.signal.longDesc.decl");
		}
		return ResourceMgr.getString(AutoRouteActionUI.class, "AutoRouteActionUI.longDesc.decl");
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_route_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AutoRouteAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		// only enabled if a conductor(s) is selected
		boolean bEnable = false;
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			SelectSet presel = activeSM.getPreSelections();
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext();) {
				Selection sel = iter.getNext();
				if (IConductor.class.isAssignableFrom(sel.getSelectionClass())) {
					bEnable = true;
					break;
				}
			}
		}

		setEnabled(false);
	}
}
