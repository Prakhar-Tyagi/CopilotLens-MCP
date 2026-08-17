/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caplets.logic.actions.immersed.CreateDeviceFromLibraryPartWithInfoAction;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryObject;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy: No ICD, but the part number resolves to a valid {@link ILibraryDevice}.
 * <p>
 * Creates a device from the library part, optionally with a symbol.
 * </p>
 */
public class LibraryPartDeviceStrategy extends AbstractDeviceCreationStrategy
{

	@Override
	public boolean canHandle(@NotNull DeviceCreationContext context)
	{
		return context.getLibraryObject() instanceof ILibraryDevice;
	}

	@Override
	public void execute(@NotNull DeviceCreationContext context)
	{
		ILibraryObject libraryObject = context.getLibraryObject();
		assert libraryObject != null; // guaranteed by canHandle

		if (isPartNumberMismatch(context.getDeviceInfo().getPartNumber(), libraryObject.getPartNumber())) {
			return;
		}

		CreateDeviceFromLibraryPartWithInfoAction action =
				new CreateDeviceFromLibraryPartWithInfoAction(context.getActiveCapletController(), libraryObject,
						context.getLibraryGraphics());
		action.setDeviceInfo(context.getDeviceInfo());

		dispatchAction(context, action);
	}
}

