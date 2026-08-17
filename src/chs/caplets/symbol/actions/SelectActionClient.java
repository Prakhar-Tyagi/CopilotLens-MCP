/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

// CAF imports

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IAction;
import chs.caplets.symbol.SymbolMoveManipulator;
import chs.caplets.symbol.SymbolStretchManipulator;

/**
 * Provide an implementation that knows how to return the correct manipulators, and the fact that this is the base
 * action.
 *
 * @author Glenn Reynholds
 */

public class SelectActionClient extends chs.caplets.shared.actions.SelectActionClient
{

	public SelectActionClient(IAction action, ICapletController controller)
	{
		super(action, controller);
	}

	@Override protected void createManipulators(IGfxModel model)
	{
		super.createManipulators(model);
		m_stretchManip = new SymbolStretchManipulator(model.getDynamicGfxService(), model);
		m_moveManip = new SymbolMoveManipulator(model.getDynamicGfxService(), model);
	}

	protected int previewSetHasManipulableObjects()
	{
		return (STRETCH_ALLOWED | MOVE_ALLOWED);
	}
}