/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caplets.logic.actions.shared.SharePinListActionHelper;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;

/**
 * Created by IntelliJ IDEA. User: hebae Date: Sep 11, 2005 Time: 11:07:42 AM To change this template use File |
 * Settings | File Templates.
 */
public class SymbolCreateSharedSpliceAction extends SymbolCreateSharedAction
{

	public SymbolCreateSharedSpliceAction(ICapletController controller)
	{
		super(controller);
	}

	protected SymbolCreateSharedSpliceAction(ICapletController controller, SharePinListActionHelper actionHelper)
	{
		super(controller, actionHelper);
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		boolean enable = false;

		ISymbolDef symbolDef = getActiveSymbolDef();
		if ((symbolDef != null) && (symbolDef.getSymbolType().equals(SymbolTypeEnum.SPLICE))) {
			enable = true;
		}

		return enable && isModeEnabled();
	}

	@Override protected boolean checkCache()
	{
		return false;
	}

	public String getActionUIClass()
	{
		return SymbolCreateSharedSpliceActionUI.class.getName();
	}
}