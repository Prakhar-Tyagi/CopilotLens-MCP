package chs.caplets.logic.shared;

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

public class SharedDeviceSymbolTreeController extends AbstractSymbolTreeController
{

	public String getInactiveSymbolIconPath()
	{
		return "chs/images/app/ico_shared_device_inactive.gif";
	}

	public String getActiveSymbolIconPath()
	{
		return "chs/images/app/ico_shared_device_active.gif";
	}

	public String getInactiveBlockIconPath()
	{
		return "chs/images/app/ico_sharedpinlist_inactive.gif";
	}

	public String getActiveBlockIconPath()
	{
		return "chs/images/app/ico_sharedpinlist.gif";
	}

	public boolean allowMultiSelection()
	{
		return true;
	}

	public boolean isSharedObject()
	{
		return true;
	}


}
