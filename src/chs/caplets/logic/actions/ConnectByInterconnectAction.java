/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IInterconnectConductor;
import org.jetbrains.annotations.Nullable;

public class ConnectByInterconnectAction extends ConnectAction
{

	public ConnectByInterconnectAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return ConnectByInterconnectActionUI.class.getName();
	}

	@Nullable protected Class<? extends IConductor> getConductorClass()
	{
		return IInterconnectConductor.class;
	}
}
