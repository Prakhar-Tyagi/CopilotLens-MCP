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

import chs.caf.caplet.ICapletController;
import chs.cof.symbol.ISymbolDef;

import java.util.Collections;
import java.util.List;

/**
 * @author chandras on 3-10-2019.
 */
public class CreateOtherComponentOnlyWithPartAction extends AbstractCreateOtherComponentAction
{

	public CreateOtherComponentOnlyWithPartAction(ICapletController controller)
	{
		super(controller, CreateOtherComponentOnlyWithPartActionUI.class.getName());
	}

	@Override public boolean isEnabled()
	{
		return acquireSelectedLibraryPart() != null && super.isEnabled();
	}

	@Override protected boolean shouldInitPhysicalDimensionAttributes()
	{
		return false;
	}

	@Override protected List<ISymbolDef> acquireSelectedSymbols()
	{
		//ignore the part associated library symbols
		return Collections.emptyList();
	}
}