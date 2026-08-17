/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic;

import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Tooltips for move/swap operations in the manage connections dialog.
 */
public enum MoveSwapErrorCode
{
	NoError,
	PinsWithDifferentMates("ManageConnectorsAction.dialog.tooltip.DisableSwapMoveForPinsWithDifferentMates"),
	SwappedPinIsJumperType("ManageConnectorsAction.dialog.tooltip.PinIsJumperType.Swap"),
	MovedToPinIsJumperType("ManageConnectorsAction.dialog.tooltip.PinIsJumperType.Move"),
	SwapBackshellPin("ManageConnectorsAction.dialog.tooltip.BackshellPinType.Swap"),
	MoveBackshellPin("ManageConnectorsAction.dialog.tooltip.BackshellPinType.Move");

	@NotNull private String mResourceKey;

	MoveSwapErrorCode()
	{
		this(StringUtils.EMPTY_STRING);
	}

	MoveSwapErrorCode(@NotNull String key)
	{
		mResourceKey = key;
	}

	@NotNull public String getKey()
	{
		return mResourceKey;
	}

	public boolean isSuccess()
	{
		return StringUtils.isBlank(mResourceKey);
	}
}
