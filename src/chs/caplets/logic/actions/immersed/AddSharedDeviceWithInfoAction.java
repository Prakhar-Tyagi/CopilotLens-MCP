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
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caplets.logic.actions.immersed.strategy.DeviceCreationContext;
import chs.caplets.logic.actions.shared.AddSharedDeviceAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.ValueTypeEnum;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.ui.SymbolProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.List;

/**
 * An action to add a shared device with associated information (e.g. pins).
 */
public class AddSharedDeviceWithInfoAction extends AddSharedDeviceAction
{

	@NotNull private ISharedPinList iSharedPinList;
	@NotNull private CreateDeviceInfo m_deviceInfo;
	private boolean m_partNumberMismatch;

	public AddSharedDeviceWithInfoAction(@NotNull ICapletController controller,
			@Nullable ISpecialSelectMgr sharedSelectMgr, @NotNull ISharedPinList sharedPinList)
	{
		super(controller, sharedSelectMgr);
		iSharedPinList = sharedPinList;
	}

	@Nullable @Override protected ISharedPinList getOperand()
	{
		return iSharedPinList;
	}

	@Override protected boolean isNonInteractiveObjectSelection(Object source)
	{
		return true;
	}

	public void setDeviceInfo(CreateDeviceInfo createDeviceInfo, boolean partNumberMismatch)
	{
		m_deviceInfo = createDeviceInfo;
		m_partNumberMismatch = partNumberMismatch;
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IPinList schemDevice = (IPinList) super.createParamObject(p1, p2);
		m_deviceInfo.setProperties(schemDevice, m_partNumberMismatch);

		return schemDevice;
	}

	@Nullable @Override
	protected chs.cof.logical.cable.IPinList generatePinList(List<Pair<IPinList, SymbolProxy>> symbolInstancesToUpdate,
			List<Pair<IPinList, SymbolProxy>> instancesToUpdate, CompositePinConnectivityFinder connectivityFinder)
	{
		chs.cof.logical.cable.IPinList iPinList =
				super.generatePinList(symbolInstancesToUpdate, instancesToUpdate, connectivityFinder);
		if (iPinList != null) {
			m_deviceInfo.setProperties(iPinList, m_partNumberMismatch);
		}
		return iPinList;
	}
}
