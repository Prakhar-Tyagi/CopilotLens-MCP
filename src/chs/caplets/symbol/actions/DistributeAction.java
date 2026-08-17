/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.cmd.AbstractGfxObjectCmd;
import chs.common.IObjectFilter;

public class DistributeAction extends chs.caf.caplet.helpers.graphics.DistributeAction
{

	/**
	 * @param controller
	 * @param type
	 */
	public DistributeAction(ICapletController controller, String type)
	{
		super(controller, type);
	}

	protected IObjectFilter createFilter()
	{
		return new AbstractGfxObjectCmd.SymbolFilter();
	}
}
