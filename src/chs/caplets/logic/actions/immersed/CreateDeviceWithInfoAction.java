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
import chs.caplets.logic.actions.CreateNoPinDeviceAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * This class represents an action for creating a device with specific information.
 * It extends the CreateNoPinDeviceAction class to inherit its functionality
 * and adds additional behavior for handling device-specific details.
 */
public class CreateDeviceWithInfoAction extends CreateNoPinDeviceAction
{

	@NotNull private CreateDeviceInfo deviceInfo;

	public CreateDeviceWithInfoAction(@NotNull ICapletController controller)
	{
		super(controller);
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
		this.deviceInfo = deviceInfo;
	}

	@Override @NotNull protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IPinList schemDevice = (IPinList) super.createParamObject(p1, p2);
		IDevice device = (IDevice) schemDevice.getConnectivity();

		deviceInfo.initialize(device, true);
		return schemDevice;
	}

	@Override protected boolean shouldAddPins()
	{
		return false;
	}

	@Override public boolean isValid()
	{
		return true;
	}
}