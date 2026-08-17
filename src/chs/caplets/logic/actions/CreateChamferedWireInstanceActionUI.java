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

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign})
public class CreateChamferedWireInstanceActionUI extends CreateMultipleWiresActionUI
{

	public CreateChamferedWireInstanceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif");
		putValue(NAME, ResourceMgr.getString(CreateChamferedWireInstanceActionUI.class,
				"CreateChamferedWireInstanceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateChamferedWireInstanceActionUI.class,
				"CreateChamferedWireInstanceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateChamferedWireInstanceActionUI.class,
				"CreateChamferedWireInstanceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public String getActionClass()
	{
		return CreateChamferedWireInstanceAction.class.getName();
	}
}

