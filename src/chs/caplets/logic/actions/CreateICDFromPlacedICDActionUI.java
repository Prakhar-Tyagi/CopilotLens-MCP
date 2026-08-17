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
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class CreateICDFromPlacedICDActionUI extends ActionUI
{

	public CreateICDFromPlacedICDActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(CreateICDFromPlacedICDActionUI.class,
				"CreateICDFromPlacedICDActionUI.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(CreateICDFromPlacedICDActionUI.class,
						"CreateICDFromPlacedICDActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateICDFromPlacedICDActionUI.class,
						"CreateICDFromPlacedICDActionUI.longDesc.text"));
	}

	@Override public String getActionClass()
	{
		return CreateICDFromPlacedICDAction.class.getName();
	}
}
