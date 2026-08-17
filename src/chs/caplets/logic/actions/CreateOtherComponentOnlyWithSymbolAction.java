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
import chs.cof.parts.ILibraryObject;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 3-10-2019.
 */
public class CreateOtherComponentOnlyWithSymbolAction extends AbstractCreateOtherComponentAction
{

	public CreateOtherComponentOnlyWithSymbolAction(ICapletController controller)
	{
		super(controller, CreateOtherComponentOnlyWithSymbolActionUI.class.getName());
	}

	@Override public boolean isEnabled()
	{
		return !acquireSelectedSymbols().isEmpty() && super.isEnabled();
	}

	@Override protected boolean shouldInitPhysicalDimensionAttributes()
	{
		return true;
	}

	@Nullable @Override protected ILibraryObject acquireSelectedLibraryPart()
	{
		return null;
	}
}