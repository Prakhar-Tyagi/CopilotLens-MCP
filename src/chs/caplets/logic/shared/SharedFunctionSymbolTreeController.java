/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.shared;

import chs.images.CHSImages;

public class SharedFunctionSymbolTreeController extends SharedDeviceSymbolTreeController
{
	@Override public String getInactiveSymbolIconPath()
	{
		return CHSImages.SHARED_FUNCTION_INACTIVE_ICON;
	}

	@Override public String getActiveSymbolIconPath()
	{
		return CHSImages.SHARED_FUNCTION_ACTIVE_ICON;
	}
}
