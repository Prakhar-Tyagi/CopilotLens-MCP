/*
 * Copyright 2018 Mentor Graphics Corporation
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
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class CreateChamferedNetInstanceActionUI extends CreateMultipleNetsActionUI
{

	public CreateChamferedNetInstanceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif");
		putValue(NAME, ResourceMgr.getString(CreateChamferedNetInstanceActionUI.class,
				"CreateChamferedNetInstanceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateChamferedNetInstanceActionUI.class,
				"CreateChamferedNetInstanceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateChamferedNetInstanceActionUI.class,
				"CreateChamferedNetInstanceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public String getActionClass()
	{
		return CreateChamferedNetInstanceAction.class.getName();
	}
}

