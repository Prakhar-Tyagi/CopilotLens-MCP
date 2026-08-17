/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.AddSharedDeviceWithInfoAction;
import chs.cof.logical.shared.ISharedPinList;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy: The device has at least one associated shared pin list.
 */
public class SharedDeviceStrategy
		extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return !context.getSharedPinLists().isEmpty();
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		ISharedPinList pinList = context.getSharedPinLists().iterator().next();

		AddSharedDeviceWithInfoAction action =
				new AddSharedDeviceWithInfoAction(context.getActiveCapletController(),
						null, pinList);
		action.setDeviceInfo(context.getDeviceInfo(),
				isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), pinList.getPartNumber()));
		dispatchAction(context, action);
	}
}
