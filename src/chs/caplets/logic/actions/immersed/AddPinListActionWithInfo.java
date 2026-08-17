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
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.AddPinListAction;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

/**
 * An action to add a pin list with associated information
 */
public class AddPinListActionWithInfo extends AddPinListAction
{

	@NotNull private IPinList m_pinList;
	@NotNull private CreateDeviceInfo m_deviceInfo;
	private boolean m_partNumberMismatch;

	public AddPinListActionWithInfo(@NotNull ICapletController controller, @NotNull IPinList pinList)
	{
		super(controller);
		m_pinList = pinList;
	}

	@NotNull @Override protected IPinList getOperand()
	{
		return m_pinList;
	}

	public void setDeviceInfo(CreateDeviceInfo deviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = deviceInfo;
		m_partNumberMismatch = partNumberMismatch;
	}

	@Override protected IActionEnum activateAddWithoutSymbol(ActionEvent e, IPinList pinlist, List<IAbstractPin> pins,
			boolean autogenerate, boolean reference, boolean placeAsStack, boolean placeAsGroup,
			List<IPinProxy> pinProxies, boolean withConductor)
	{
		AddParameterizedPinListActionWithInfo addParameterizedPinListActionWithInfo =
				new AddParameterizedPinListActionWithInfo(getController(), pinlist, pins, autogenerate, reference,
						placeAsStack, placeAsGroup, pinProxies);
		addParameterizedPinListActionWithInfo.setDeviceInfo(m_deviceInfo, m_partNumberMismatch);
		subAction = addParameterizedPinListActionWithInfo;
		return addParameterizedPinListActionWithInfo.onActivate(e);
	}

	@Override
	protected IActionEnum activateAddWithSymbol(ActionEvent e, IPinList pinlist, ISymbolDef symDef, IBlock block,
			Map<IUID, ISymbolledPin> map, boolean reference, boolean withConductor)
	{
		AddSymbolledPinListActionWithInfo addSymbolledPinListActionWithInfo =
				new AddSymbolledPinListActionWithInfo(getController(), pinlist, symDef, block, map, reference);
		addSymbolledPinListActionWithInfo.setDeviceInfo(m_deviceInfo, m_partNumberMismatch);
		subAction = addSymbolledPinListActionWithInfo;
		return addSymbolledPinListActionWithInfo.onActivate(e);
	}
}
