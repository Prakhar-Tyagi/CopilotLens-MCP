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

import chs.caf.cafmain.actions.analysis.AttachModelActionUI;
import chs.caf.caplet.ICaplet;

/**
 * @author rharring
 */
public class LogicAttachModelActionUI extends AttachModelActionUI
{

	/**
	 * Creates a new instance of LogicAttachModelActionUI
	 */
	public LogicAttachModelActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return LogicAttachModelAction.class.getName();
	}
}
