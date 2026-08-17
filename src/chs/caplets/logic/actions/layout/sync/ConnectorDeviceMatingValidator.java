/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.helpers.IPinDisconnectExecutor;
import chs.utility.helpers.PinDisconnectionExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConnectorDeviceMatingValidator implements ILayoutSyncObjectValidator
{

	@NotNull @Override
	public ILayoutSyncValidationResult validate(@NotNull ILogicObject logicObject)
	{
		if (logicObject instanceof IHarnessPlugConnector) {
			final IHarnessPlugConnector harnConnector = (IHarnessPlugConnector) logicObject;
			final IDevice owner = harnConnector.getOwner(IDevice.class);
			final SetMap<IConnectorPin, IDevicePin> connectedPinMap = getConnectedPinMap(harnConnector);
			for (IDevicePin devicePin : connectedPinMap.items()) {
				final IDevice matedPinOwner = CommonUtils.cast(devicePin.getOwner(), IDevice.class);
				if (owner != matedPinOwner) {
					return new ConnectorDeviceMatingValidationResult(harnConnector);
				}
			}
		}
		return ILayoutSyncValidationResult.SUCCESS;
	}

	@Override public boolean accepts(@NotNull ILogicObject logicObject)
	{
		return logicObject instanceof IHarnessPlugConnector;
	}

	@NotNull
	private static SetMap<IConnectorPin, IDevicePin> getConnectedPinMap(@NotNull IHarnessPlugConnector plugConnector)
	{
		final SetMap<IConnectorPin, IDevicePin> connectedPins = new SetMap<>();
		for (IConnectorPin pin : plugConnector.getConnectorPins()) {
			for (IAbstractPin connectedPin : pin.getConnectedPins()) {
				if (connectedPin instanceof IDevicePin) {
					connectedPins.add(pin, (IDevicePin) connectedPin);
				}
			}
		}
		return connectedPins;
	}

	private static class ConnectorDeviceMatingValidationResult implements ILayoutSyncValidationResult
	{

		@NotNull private final IHarnessPlugConnector mConn;

		private ConnectorDeviceMatingValidationResult(@NotNull IHarnessPlugConnector targetConn)
		{
			mConn = targetConn;
		}

		@NotNull @Override public Collection<String> getMessages()
		{
			final String type = COFTypeEnum.getDisplayableTypeName(mConn);
			return Arrays.asList(ResourceMgr.getString(ConnectorDeviceMatingValidationResult.class,
					"ConnectorDeviceMatingValidationResult.multipleDeviceOwner.failure", type, mConn.getName()));
		}

		@Override public void fixupValidationError()
		{
			IPinDisconnectExecutor disconnectExecutor = new PinDisconnectionExecutor();
			final Set<IDevice> deviceOwners = new HashSet<>();
			for (Map.Entry<IConnectorPin, Set<IDevicePin>> connectorPinSetEntry : getConnectedPinMap(mConn)
					.entrySet()) {
				IConnectorPin connectorPin = connectorPinSetEntry.getKey();
				for (IDevicePin devicePin : connectorPinSetEntry.getValue()) {
					disconnectExecutor.processPinMating(connectorPin, devicePin);
					final IDevice ownerDevice = CommonUtils.cast(devicePin.getOwner(), IDevice.class);
					if (ownerDevice != null) {
						deviceOwners.add(ownerDevice);
					}
				}
			}

			for (IDevice ownerDevice : deviceOwners) {
				disconnectExecutor.processDevicePlugMating(ownerDevice, mConn);
			}
		}
	}
}
