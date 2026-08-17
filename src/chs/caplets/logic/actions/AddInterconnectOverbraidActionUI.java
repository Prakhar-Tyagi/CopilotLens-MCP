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
public class AddInterconnectOverbraidActionUI extends ActionUI
{

	public AddInterconnectOverbraidActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddInterconnectOverbraidAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(AddInterconnectOverbraidActionUI.class, "AddInterconnectOverbraidActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddInterconnectOverbraidActionUI.class,
				"AddInterconnectOverbraidActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddInterconnectOverbraidActionUI.class,
				"AddInterconnectOverbraidActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_overbraid_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr.getMnemonic(AddInterconnectWireActionUI.class,
				"AddInterconnectOverbraidActionUI.mnemonic")));
	}
}
