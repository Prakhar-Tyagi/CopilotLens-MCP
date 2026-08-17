/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.AddPinListActionWithInfo;
import chs.cof.logical.cable.IPinList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy: The request contains a list of placed device pins (e.g. from a previous placement step), or an ICD with such a list.
 */
public class ExistingDeviceStrategy extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return (getPlacedDevicePinList(context) != null && context.getSharedPinLists().isEmpty());
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		IPinList placedDevicePinList = getPlacedDevicePinList(context);
		assert placedDevicePinList != null;

		AddPinListActionWithInfo addPinListActionWithInfo =
				new AddPinListActionWithInfo(context.getActiveCapletController(), placedDevicePinList);
		addPinListActionWithInfo.setDeviceInfo(context.getDeviceInfo(),
				isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), placedDevicePinList.getPartNumber()));
		dispatchAction(context, addPinListActionWithInfo);
	}

	@Nullable
	private static IPinList getPlacedDevicePinList(@NotNull DeviceCreationContext context)
	{
		return context.getPlacedDevicePinList();
	}
}
