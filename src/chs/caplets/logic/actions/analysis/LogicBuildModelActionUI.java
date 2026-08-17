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

import chs.caf.cafmain.actions.analysis.BuildModelActionUI;
import chs.caf.caplet.ICaplet;

/**
 * @author rharring
 */
public class LogicBuildModelActionUI extends BuildModelActionUI
{

	/**
	 * Creates a new instance of LogicBuildModelActionUI
	 */
	public LogicBuildModelActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return LogicBuildModelAction.class.getName();
	}
}
