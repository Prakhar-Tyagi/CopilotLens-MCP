/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed;

import chs.caf.caplet.ICapletController;
import chs.caplets.logic.actions.AddLibraryPartWithSymbolAction;
import chs.cof.logical.cable.IDevice;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents an action to add a library part with a symbol and additional device information.
 * It extends the `AddLibraryPartWithSymbolAction` class and provides additional functionality
 * to handle device-specific information during the action execution.
 */
public class AddLibraryPartWithSymbolAndInfoAction extends AddLibraryPartWithSymbolAction
{
	@NotNull private CreateDeviceInfo m_deviceInfo;

	/**
	 * Construct the action.
	 *
	 * @param controller           The controller
	 * @param part                 The library part.  Must have a symbol.
	 */
	public AddLibraryPartWithSymbolAndInfoAction(@NotNull ICapletController controller,
			@NotNull ILibraryPartSelection part)
	{
		super(controller, part);
	}

	/**
	 * Setter to "feed" the action with DeviceInfo
	 * Sets the device information for this action.
	 * This method provides the required inputs for device creation.
	 *
	 * @param deviceInfo The CreateDeviceInfo object containing details for device creation.
	 */
	public void setDeviceInfo(@NotNull CreateDeviceInfo deviceInfo)
	{
		m_deviceInfo = deviceInfo;
	}

	/**
	 * Override addInstance to initialize the device with custom device info immediately
	 * after it's created, before any library part updates occur.
	 */
	@Override
	protected boolean addInstance()
	{
		// Call parent to create and add the device
		boolean result = super.addInstance();

		// Initialize with custom device info
		IDevice device = null;
		if (m_pinlist != null) {
			device = (IDevice) m_pinlist.getConnectivity();
		}
		m_deviceInfo.initialize(device,false);

		return result;
	}

	@Override public boolean isValid()
	{
		return true;
	}
}