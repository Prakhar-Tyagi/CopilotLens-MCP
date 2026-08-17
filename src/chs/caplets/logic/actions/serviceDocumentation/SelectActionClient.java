/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.HydraMoveManipulator;
import chs.caf.caplet.helpers.HydraStretchManipulator;

@SuppressWarnings("ClassNameSameAsAncestorName") public class SelectActionClient
		extends chs.caplets.shared.actions.SelectActionClient
{

	public SelectActionClient(IAction action, ICapletController controller)
	{
		super(action, controller);
	}

	protected void createManipulators(IGfxModel model)
	{
		m_stretchManip = new HydraStretchManipulator(model.getDynamicGfxService(), model);
		m_moveManip = new HydraMoveManipulator(model.getDynamicGfxService(), model);
	}
}