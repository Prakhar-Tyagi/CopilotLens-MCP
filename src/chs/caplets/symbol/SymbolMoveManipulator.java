/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.helpers.MoveManipulator;
import chs.caplets.logic.MoveConductorDecorations;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;

import java.util.List;


public class SymbolMoveManipulator extends MoveManipulator
{

	public SymbolMoveManipulator(IDynamicGfxService dynamics, IGfxModel model)
	{
		super(dynamics, model);
	}

	@Override protected void moveDecorations(List<IDynamicGfx> selectedDynamicGraphics)
	{
		MoveConductorDecorations.move(selectedDynamicGraphics);
	}
}
