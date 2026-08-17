/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.AddSharedICDWithInfoAction;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy: ICD is present and user preference indicates that shared ICD placement is preferred.
 */
public class SharedICDDeviceStrategy
		extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return context.getIcd() != null && context.getDoNotAutoShareICD();
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		assert context.getIcd() != null;
		AddSharedICDWithInfoAction action =
				new AddSharedICDWithInfoAction(context.getActiveCapletController(), context.getIcd(),
						context.getLogicDesign());
		action.setDeviceInfo(context.getDeviceInfo(),
				isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), context.getIcd().getPartNumber()));
		dispatchAction(context, action);
	}
}
