/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(allowInQAExtensionsFor = {Application.CapitalLogicDesigner},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner})
public class RemoveDeviceConnectorsActionUI extends ActionUI
{

	public RemoveDeviceConnectorsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(RemoveDeviceConnectorsActionUI.class,
				"RemoveDeviceConnectorsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(RemoveDeviceConnectorsActionUI.class,
				"RemoveDeviceConnectorsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(RemoveDeviceConnectorsActionUI.class,
				"RemoveDeviceConnectorsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif"));
	}

	public String getActionClass()
	{
		return RemoveDeviceConnectorsAction.class.getName();
	}
}
