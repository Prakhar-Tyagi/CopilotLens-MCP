/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IExtent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementMarginControl implements IDevicePlacementMarginControl
{

	@NotNull private final ISchemDiagram m_diagram;
	@NotNull private final Map<IDevice, IDeviceMargin> m_deviceMargins = new HashMap<>();

	public DevicePlacementMarginControl(@NotNull ISchemDiagram diagram)
	{
		m_diagram = diagram;
	}

	@NotNull @Override public IExtent getMarginExtent(@NotNull IDevice device, @NotNull IExtent snappedExtent)
	{
		return constructDeviceMargin(device).getMarginExtent(snappedExtent);
	}

	@Override public int getMargin(@NotNull IDevice device, @NotNull DeviceMarginSide side)
	{
		return constructDeviceMargin(device).getMargin(side);
	}

	@NotNull private IDeviceMargin constructDeviceMargin(@NotNull IDevice device)
	{
		return m_deviceMargins.computeIfAbsent(device, d -> new DeviceMargin(device, m_diagram.getGrid()));
	}
}
