/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;

public class DisconnectAction extends AbstractDisconnectAction
{

	public DisconnectAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return DisconnectActionUI.class.getName();
	}
}

