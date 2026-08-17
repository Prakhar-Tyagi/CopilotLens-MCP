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
import chs.caf.caplet.helpers.StretchManipulator;
import chs.caplets.logic.MoveConductorDecorations;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * <p/> Stretch implementation of IManipulate. It handles basic grip-point movement.  This class only really manipulates
 * "ISmartPoint" objects. Indirectly this will affect IDynamicGfx objects. </p> <p> Ulimately the model is affected
 * through the abstract IDynamicGfxMediator interface. </p>
 */

public class SymbolStretchManipulator extends StretchManipulator
{

	/**
	 * Constructor for the CapitalSymbol StretchMainipulator object
	 *
	 * @param dynamics Description of Parameter
	 */
	public SymbolStretchManipulator(IDynamicGfxService dynamics, IGfxModel model)
	{
		super(dynamics, model);
	}

	@Override protected void moveDecorations(List<IDynamicGfx> selectedDynamicGraphics)
	{
		MoveConductorDecorations.move(selectedDynamicGraphics);
	}

	@NotNull public Collection<Integer> getGripRadiusCandidates()
	{
		return getViewSensitiveStretchGripRadiusCandidates();
	}
}

