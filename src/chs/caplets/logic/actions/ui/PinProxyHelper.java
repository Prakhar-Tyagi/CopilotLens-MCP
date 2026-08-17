/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2017-2026 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.ctf.caf.utils.IPinProxy;
import chs.subsystem.logic.manageconnections.IPinProvider;
import chs.subsystem.logic.manageconnections.ManageConnectionsServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PinProxyHelper
{

	private PinProxyHelper()
	{
	}

	public static boolean isPartAssigned(@NotNull IPinProxy pinProxy)
	{
		ISharedPin sharedPin = pinProxy.getSharedPin();
		if (sharedPin != null) {
			return sharedPin.isPartAssigned();
		}
		IAbstractPin abstractPin = pinProxy.getCablePin();
		if (abstractPin != null) {
			return abstractPin.isPartAssigned();
		}
		return false;
	}

	@Nullable
	public static IAbstractPin getCablePin(@NotNull IPinProxy pinProxy, @NotNull ILogicDesign logicDesign,
			@Nullable ISharedPinList sharedPinList)
	{
		IAbstractPin cablePin = pinProxy.getCablePin();
		if (cablePin == null) {
			ISharedPin sharedPin = pinProxy.getSharedPin();
			if (sharedPin != null && sharedPinList != null) {
				IPinList currentPinList = getCablePinList(sharedPinList, logicDesign);
				if (currentPinList != null) {
					cablePin = getCablePinFor(sharedPin, currentPinList);
				}
			}
		}
		return cablePin;
	}

	@Nullable
	private static IPinList getCablePinList(@NotNull ISharedPinList sharedPinList,
			ILogicDesign logicDesign)
	{
		IConnectivity connectivity = logicDesign.getLoadedConnectivity();
		//do not load the connectivity if it is not already loaded.
		if (connectivity != null) {
			return connectivity.findSharedPinList(sharedPinList);
		}
		return null;
	}

	@Nullable private static IAbstractPin getCablePinFor(ISharedPin sharedPin, IPinList currentPinList)
	{
		IAbstractPin cablePin = null;
		Optional<IAbstractPin> pinProvider =
				ManageConnectionsServices.requireExtension(currentPinList, IPinProvider.class).getAllPins().stream()
						.filter(pin -> pin.getSharedPin() == sharedPin).findFirst();
		if (pinProvider.isPresent()) {
			cablePin = pinProvider.get();
		}
		return cablePin;
	}

}
