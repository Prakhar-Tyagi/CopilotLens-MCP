/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddInterconnectShieldActionUI extends ActionUI
{

	public AddInterconnectShieldActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddInterconnectShieldAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(AddInterconnectShieldActionUI.class, "AddInterconnectShieldActionUI.name.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddInterconnectShieldActionUI.class,
				"AddInterconnectShieldActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddInterconnectShieldActionUI.class,
				"AddInterconnectShieldActionUI.longDesc.text"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr.getMnemonic(AddInterconnectShieldActionUI.class,
				"AddInterconnectShieldActionUI.mnemonic.text")));
	}
}
