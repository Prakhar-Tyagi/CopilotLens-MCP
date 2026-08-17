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
import chs.images.CHSImageLoader;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_NO_PIN_DEVICE_ACTION",
	label = "Add Device Without Pins",
	tooltip = "Add Device Without Pins",
	icon = "ico_device_active",
	buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateNoPinDeviceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateNoPinDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		// NOTE: we now use the With pins icons for the no pins actions because only the no pins variants are shown in the UI
		// this may change if we revert to having both variants available
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif");
		Integer iMnemonic =
				(int) ResourceMgr.getMnemonic(CreateNoPinDeviceActionUI.class, "CreateNoPinDeviceActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		String nameProperty = "CreateNoPinDeviceActionUI.name.";
		if (AppInfo.getAppInfo().isCapitalCapture()) {
			nameProperty += "Capture.";
		}
		nameProperty += "decl";
		putValue(NAME, ResourceMgr.getString(CreateNoPinDeviceActionUI.class, nameProperty));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateNoPinDeviceActionUI.class, "CreateNoPinDeviceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateNoPinDeviceActionUI.class, "CreateNoPinDeviceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_device_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateNoPinDeviceAction.class.getName();
	}
}

