/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.AddParameterizedDeviceFromICDWithInfoAction;
import chs.cof.icd.IICD;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy: ICD is present but has <b>no</b> associated library device.
 * <p>
 * Creates a transient (parameterized) device from the ICD without symbol
 * or part information.
 * </p>
 */
public class ICDWithoutLibraryDeviceStrategy extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return context.getIcd() != null && context.getIcdLibraryDevice() == null;
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		IICD icd = context.getIcd();
		assert icd != null;

		AddParameterizedDeviceFromICDWithInfoAction action =
				new AddParameterizedDeviceFromICDWithInfoAction(context.getActiveCapletController(),
						icd, context.getLogicDesign());
		action.setDeviceInfo(context.getDeviceInfo(), isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), icd.getPartNumber()));
		dispatchAction(context, action);
	}
}

