/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.AddDeviceFromICDWithInfo;
import chs.cof.icd.IICD;
import chs.cof.parts.ILibraryObject;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy: ICD is present <b>and</b> its library device exists in the parts library.
 * <p>
 * Validates that the ICD library device's part number matches the incoming
 * device info's part number and that valid (non-obsolete) library objects
 * exist for that part number before dispatching.
 * </p>
 */
public class ICDWithLibraryDeviceStrategy extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return context.getIcd() != null && context.getIcdLibraryDevice() != null;
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		IICD icd = context.getIcd();
		ILibraryObject libraryDevice = context.getIcdLibraryDevice();
		assert icd != null && libraryDevice != null; // guaranteed by canHandle

		AddDeviceFromICDWithInfo action =
				new AddDeviceFromICDWithInfo(context.getActiveCapletController(), icd, context.getLogicDesign());
		action.setDeviceInfo(context.getDeviceInfo(),
				isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), libraryDevice.getPartNumber()));
		dispatchAction(context, action);
	}
}

