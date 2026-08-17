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
import chs.cof.symbol.ISymbolDef;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class CreateLayoutComponentWithoutPartAndSymbolAction extends AbstractCreateOtherComponentAction
{

	protected CreateLayoutComponentWithoutPartAndSymbolAction(ICapletController controller, String sActionUIClass)
	{
		super(controller, sActionUIClass);
	}

	@Override protected List<ISymbolDef> acquireSelectedSymbols()
	{
		return Collections.emptyList();
	}

	@Nullable @Override protected ILibraryObject acquireSelectedLibraryPart()
	{
		return null;
	}

	@Override protected boolean shouldInitPhysicalDimensionAttributes()
	{
		return true;
	}
}
