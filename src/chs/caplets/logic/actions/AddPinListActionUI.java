/*
 * Copyright 2006 Mentor Graphics Corporation
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
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign})
public class AddPinListActionUI extends ActionUI
{

	public AddPinListActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		// TODO jacobt FEAT13040 : probably need resources for each pinlist type, rather than these
		putValue(NAME, ResourceMgr.getString(AddPinListActionUI.class, "AddPinListActionUI.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddPinListActionUI.class, "AddPinListActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddPinListActionUI.class, "AddPinListActionUI.longDesc.text"));
	}

	public String getActionClass()
	{
		return AddPinListAction.class.getName();
	}
}
