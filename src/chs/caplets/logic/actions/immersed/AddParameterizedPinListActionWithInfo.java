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
import chs.caplets.logic.actions.AddParameterizedPinListAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.ctf.caf.utils.IPinProxy;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.List;

/**
 * Extension of {@link AddParameterizedPinListAction} that also takes a {@link CreateDeviceInfo} to set additional
 * properties on the created pin list.
 */
public class AddParameterizedPinListActionWithInfo extends AddParameterizedPinListAction
{

	@NotNull protected CreateDeviceInfo m_deviceInfo;
	private boolean m_partNumberMismatch;

	public AddParameterizedPinListActionWithInfo(ICapletController controller,
			IPinList pinlist, List<IAbstractPin> pins,
			boolean autogenerate, boolean reference, boolean placeAsStack, boolean placeAsGroup,
			List<IPinProxy> pinProxies)
	{
		super(controller, pinlist, pins, autogenerate, reference, placeAsStack, placeAsGroup, pinProxies);
	}

	public void setDeviceInfo(CreateDeviceInfo mDeviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = mDeviceInfo;
		m_partNumberMismatch = partNumberMismatch;
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		chs.cof.logical.schem.IPinList schemDevice = (chs.cof.logical.schem.IPinList) super.createParamObject(p1, p2);
		m_deviceInfo.setProperties(schemDevice, m_partNumberMismatch);

		return schemDevice;
	}
}
