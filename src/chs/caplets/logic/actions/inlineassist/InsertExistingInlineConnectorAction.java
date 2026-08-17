/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caf.caplet.ICapletController;
import chs.caplets.logic.actions.PlaceInlineConnectorAction;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.utility.ConductorSplitter;
import chs.utility.InlineConductorSplitter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class InsertExistingInlineConnectorAction extends PlaceInlineConnectorAction implements
		IInsertInlineMouseAndCursorHandler
{

	public InsertExistingInlineConnectorAction(ICapletController controller,
			IGenericInlineConnector inlineHalf,
			List<IAbstractPin> pins, boolean autogenerate, boolean reference)
	{
		super(controller, inlineHalf, pins, autogenerate, reference, false,false, Collections.emptyList());
	}

	@NotNull @Override protected ConductorSplitter getSplitter()
	{
		return new InlineConductorSplitter()
		{
			@Override protected boolean hasPartAssigned()
			{
				return false;
			}
		};
	}
}
