/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_BLOCK_DEVICE_ACTION",
		label = "Block Device",
		tooltip = "Create Block Device",
		icon = "ico_block_device_without_design_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateBlockDeviceActionUI extends ActionUI
{

	public CreateBlockDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_K);
		putValue(MNEMONIC_KEY, iMnemonic);
		String nameProperty = "CreateBlockDeviceActionUI.name.";
		nameProperty += "decl";
		putValue(NAME, ResourceMgr.getString(CreateDeviceActionUI.class, nameProperty));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateDeviceActionUI.class, "CreateBlockDeviceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateDeviceActionUI.class, "CreateBlockDeviceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, IconUtils.getBlockDeviceWithoutDesignIcon(IconUtils.ACTIVE));
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getBlockDeviceWithoutDesignIcon(IconUtils.INACTIVE);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateBlockDeviceAction.class.getName();
	}
}

