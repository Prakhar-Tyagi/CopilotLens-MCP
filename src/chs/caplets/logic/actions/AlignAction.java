/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.cmd.AlignCmd;
import chs.common.IObjectFilter;

public class AlignAction extends chs.caf.caplet.helpers.graphics.AlignAction
{

	/**
	 * @param controller
	 * @param type
	 */
	public AlignAction(ICapletController controller, String type)
	{
		super(controller, type);
	}

	protected IObjectFilter createFilter()
	{
		return new AlignCmd.LogicFilter();
	}
}
