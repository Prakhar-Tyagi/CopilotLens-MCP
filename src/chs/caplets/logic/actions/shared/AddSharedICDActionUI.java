/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner})
public class AddSharedICDActionUI extends ActionUI
{

	public AddSharedICDActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_device_active.gif");
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddSharedICDActionUI.class, "AddSharedICDActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AddSharedICDActionUI.class, "AddSharedICDActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedICDActionUI.class, "AddSharedICDActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return AddSharedICDAction.class.getName();
	}
}
