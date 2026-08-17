/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddParametrizedDeviceFromICDActionUI extends ActionUI
{

	public AddParametrizedDeviceFromICDActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(AddParametrizedDeviceFromICDActionUI.class,
				"AddParametrizedDeviceFromICDActionUI.mnemonic"));
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddParametrizedDeviceFromICDActionUI.class,
						"AddParametrizedDeviceFromICDActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
	}

	public String getActionClass()
	{
		return AddParametrizedDeviceFromICDAction.class.getName();
	}
}