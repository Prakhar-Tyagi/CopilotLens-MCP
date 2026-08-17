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
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_DEVICE_ACTION")
public class CreateDeviceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_W);

		putValue(MNEMONIC_KEY, iMnemonic);
		String nameProperty = "CreateDeviceActionUI.name.";
		if (AppInfo.getAppInfo().isCapitalCapture()) {
			nameProperty += "Capture.";
		}
		nameProperty += "decl";
		putValue(NAME, ResourceMgr.getString(CreateDeviceActionUI.class, nameProperty));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateDeviceActionUI.class, "CreateDeviceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateDeviceActionUI.class, "CreateDeviceActionUI.longDesc.decl"));
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
		return CreateDeviceAction.class.getName();
	}
}

