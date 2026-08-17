/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.analysis.SubsystemStressActionUI;
import chs.caf.caplet.ICaplet;

/**
 * @author rharring
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalArchitect, Application.CapitalEssentialsDesign})
public class LogicStressActionUI extends SubsystemStressActionUI
{

	/**
	 * Creates a new instance of LogicBuildModelActionUI
	 */
	public LogicStressActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return LogicStressAction.class.getName();
	}
}
