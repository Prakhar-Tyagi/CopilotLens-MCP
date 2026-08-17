/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.bridges;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.bridges.BridgeActionUI;
import chs.caf.caplet.ICaplet;

@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalHarnessDesigner,
				Application.CapitalHarnessDesignerModular, Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator})
public class BridgeOutFilterActionUI extends BridgeActionUI
{

	public BridgeOutFilterActionUI(ICaplet caplet)
	{
		super(caplet);
	}
}
