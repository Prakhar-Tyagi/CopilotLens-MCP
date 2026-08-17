/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2026 Siemens
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
import chs.caf.caplet.selection.Selection;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.common.DesignUtils;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.util.Iterator;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner,Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_ADD_BACKSHELL_ACTION",
		label = "Add Backshell",
		tooltip = "Add Backshell",
		icon = "ico_backshell_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddBackshellActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddBackshellActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_backshell_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_B);

		putValue(NAME, ResourceMgr.getStringForMenu(AddBackshellActionUI.class, "AddBackshellActionUI.name.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
		setupNames(false);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddBackshellAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}

	public void updateUI()
	{
		//
		boolean hasBackshellAlready = false;
		int selcount = 0;

		ISelectMgr smgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (smgr != null) {
			// Get the list of selected objects from CAF Utils.
			for (Iterator iter = smgr.getCurrentSelections().getSelectedUIDObjects(); iter.hasNext(); ) {
				Selection sel = (Selection) iter.next();
				IPinList uidObj = DesignUtils.getLoadedObject(sel.getUID(), IPinList.class);
				if (uidObj != null) {
					chs.cof.logical.cable.IPinList plc = uidObj.getConnectivity();
					if (!(plc instanceof IConnector connector)) {
						continue;
					}
					if (connector.getBackshell() != null) {
						hasBackshellAlready = true;
					}
					selcount++;
				}
			}
			if (selcount != 1) {
				hasBackshellAlready = false;
			}
		}
		//
		// Sort out names..
		//
		setupNames(hasBackshellAlready);
		super.updateUI();
	}

	private void setupNames(boolean hasBackshellAlready)
	{
		if (hasBackshellAlready) {
			putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddBackshellActionUI.class,
					"AddBackshellActionUI.exist.shortDesc.decl"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(AddBackshellActionUI.class, "AddBackshellActionUI.exist.longDesc.decl"));
		}
		else {
			putValue(SHORT_DESCRIPTION,
					ResourceMgr.getStringForMenu(AddBackshellActionUI.class, "AddBackshellActionUI.shortDesc.decl"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(AddBackshellActionUI.class, "AddBackshellActionUI.longDesc.decl"));
		}
	}
}