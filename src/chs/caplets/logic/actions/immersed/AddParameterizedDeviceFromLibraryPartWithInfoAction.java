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
import chs.caplets.logic.actions.AddParameterizedDeviceFromLibraryPartAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * This class extends the AddParameterizedDeviceFromLibraryPartAction class to provide
 * additional functionality for creating a parameterized device with specific information.
 * It allows setting device attributes and properties based on a DeviceInfo object.
 */
public class AddParameterizedDeviceFromLibraryPartWithInfoAction extends AddParameterizedDeviceFromLibraryPartAction
{

	@NotNull protected CreateDeviceInfo m_deviceInfo;

	/**
	 * Construct the action.
	 *
	 * @param controller controller
	 * @param part       The library part
	 */
	public AddParameterizedDeviceFromLibraryPartWithInfoAction(ICapletController controller,
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

	@NotNull
	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IPinList schemDevice = (IPinList) super.createParamObject(p1, p2);
		IDevice device = (IDevice) schemDevice.getConnectivity();
		m_deviceInfo.initialize(device, true);
		m_deviceInfo.setProperties(schemDevice, true);
		return schemDevice;
	}

	@Override public boolean isValid()
	{
		return true;
	}
}