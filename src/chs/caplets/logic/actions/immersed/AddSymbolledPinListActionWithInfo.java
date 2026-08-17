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
import chs.caplets.logic.actions.AddSymbolledPinListAction;
import chs.cof.logical.cable.IPinList;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * An action to add a symbolled pin list to the logic design when sufficient information is available to support
 */
public class AddSymbolledPinListActionWithInfo
		extends AddSymbolledPinListAction
{
	@NotNull private CreateDeviceInfo m_deviceInfo;

	public AddSymbolledPinListActionWithInfo(ICapletController controller, IPinList pinlist, ISymbolDef symDef,
			IBlock block, Map<IUID, ISymbolledPin> symbolledPinMap, boolean reference)
	{
		super(controller, pinlist, symDef, block, symbolledPinMap, reference);
	}

	public void setDeviceInfo(@NotNull CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
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

		if (m_pinlist != null) {
			m_deviceInfo.setProperties(m_pinlist,false);
		}

		return result;
	}
}
