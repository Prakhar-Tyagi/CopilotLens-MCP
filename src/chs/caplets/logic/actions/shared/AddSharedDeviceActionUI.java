/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/*
 *  Description of the Class
 *
 *@author     Darin Jackson
 *@created    August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SHARED_DEVICE_ACTION",
		label = "Add Shared Device",
		tooltip = "Add a shared device to the diagram",
		icon = "ico_shared_device_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSharedDeviceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddSharedDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_device_active.gif");
		Integer iMnemonic =
				(int) ResourceMgr.getMnemonic(AddSharedDeviceActionUI.class, "AddSharedDeviceActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddSharedDeviceActionUI.class, "AddSharedDeviceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AddSharedDeviceActionUI.class, "AddSharedDeviceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedDeviceActionUI.class, "AddSharedDeviceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddSharedDeviceAction.class.getName();
	}
}
