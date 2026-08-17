/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.SvcDoc,
		Application.SvcDoc, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_ADD_WIRE_LIST_TABLE_ACTION",
		label = "Add Wire List Table",
		tooltip = "Add Wire List Table",
		icon = "add_wire_list_table",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class AddWireListTableActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddWireListTableActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_table_active.gif");
		putValue(NAME, ResourceMgr.getString(AddInstanceActionUI.class, "AddWireListTableActionUI.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddInstanceActionUI.class, "AddWireListTableActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddInstanceActionUI.class, "AddWireListTableActionUI.longDesc.text"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddInstanceActionUI.class, "AddWireListTableActionUI.mnemonic.text")));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddWireListTableAction.class.getName();
	}
}
