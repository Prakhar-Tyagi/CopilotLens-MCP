/*
 * Copyright 2019 Mentor Graphics Corporation
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

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class CreateOtherComponentWithoutPartAndSymbolActionUI extends AbstractCreateOtherComponentActionUI
{

	public CreateOtherComponentWithoutPartAndSymbolActionUI(ICaplet caplet)
	{
		super(caplet, CreateOtherComponentWithoutPartAndSymbolAction.class.getName());
	}
}
