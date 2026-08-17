/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.system.FactoryMgr;

/**
 * This class exists for typing only.
 */
public class AddSharedInterconnectDeviceAction extends AddSharedDeviceAction
{

	public AddSharedInterconnectDeviceAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
	}

	protected PinListTypeEnum getType()
	{
		return PinListTypeEnum.TypeInterconnectDevice;
	}

	public String getActionUIClass()
	{
		return AddSharedInterconnectDeviceActionUI.class.getName();
	}

	protected String getCtxCommand()
	{
		return "AddSharedInterconnectDevice";
	}

	protected IDevice createLogicDevice()
	{
		return FactoryMgr.getCableFactory().createInterconnectDevice(FactoryMgr.getCommonFactory().createUID());
	}
}
