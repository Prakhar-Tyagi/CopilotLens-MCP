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

/**
 * @author chandras on 3-10-2019.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class CreateOtherComponentOnlyWithSymbolActionUI extends AbstractCreateOtherComponentActionUI
{

	public CreateOtherComponentOnlyWithSymbolActionUI(ICaplet caplet)
	{
		super(caplet, CreateOtherComponentOnlyWithSymbolAction.class.getName());
	}
}

