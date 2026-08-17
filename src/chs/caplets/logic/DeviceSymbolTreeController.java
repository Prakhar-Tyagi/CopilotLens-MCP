package chs.caplets.logic;

/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

import chs.utility.ui.AbstractSymbolTreeController;

public class DeviceSymbolTreeController extends AbstractSymbolTreeController
{

	public String getInactiveSymbolIconPath()
	{
		return "chs/images/app/ico_device_inactive.gif";
	}

	public String getActiveSymbolIconPath()
	{
		return "chs/images/app/ico_device_active.gif";
	}

	public String getInactiveBlockIconPath()
	{
		return "chs/images/app/ico_pinlist_inactive.gif";
	}

	public String getActiveBlockIconPath()
	{
		return "chs/images/app/ico_pinlist.gif";
	}

	public boolean allowMultiSelection()
	{
		return false;
	}

	public boolean isSharedObject()
	{
		return false;
	}
}
