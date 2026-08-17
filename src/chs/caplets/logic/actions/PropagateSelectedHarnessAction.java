/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import org.jetbrains.annotations.NotNull;

/**
 * Action to track Propagate Selected button on Propagate Harness table
 */
public class PropagateSelectedHarnessAction extends PropagateHarnessAction
{

	public PropagateSelectedHarnessAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return PropagateSelectedHarnessActionUI.class.getName();
	}
}
