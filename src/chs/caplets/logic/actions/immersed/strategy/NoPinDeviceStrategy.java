/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.CreateDeviceWithInfoAction;
import org.jetbrains.annotations.NotNull;

/**
 * Fallback strategy: creates a device with no pins, no ICD, and no library part.
 * <p>
 * This strategy always applies and should be registered <b>last</b> in the chain.
 * </p>
 */
public class NoPinDeviceStrategy extends AbstractDeviceCreationStrategy
{
	/**
	 * Always returns {@code true} ? this is the catch-all fallback.
	 */
	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return true;
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		CreateDeviceWithInfoAction action =
				new CreateDeviceWithInfoAction(context.getActiveCapletController());
		action.setDeviceInfo(context.getDeviceInfo());

		dispatchAction(context, action);
	}
}

