/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 13, 2004 Time: 9:15:06 AM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect})
public class AddSharedNetActionUI extends ActionUI
{

	public AddSharedNetActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddSharedNetAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(AddSharedNetActionUI.class, "AddSharedNetActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(AddSharedNetActionUI.class, "AddSharedNetActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedNetActionUI.class, "AddSharedNetActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedNetActionUI.class, "AddSharedNetActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_add_shared_active.gif"));
	}
}
