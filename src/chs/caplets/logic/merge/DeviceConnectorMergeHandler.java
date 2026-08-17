/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.merge;

import chs.caplets.logic.actions.actionreport.IActionChange;
import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import chs.caplets.logic.actions.actionreport.MergeActionChange;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.footprint.IUserFootprintConnector;
import chs.cof.logical.footprint.IUserFootprintMapping;
import chs.common.attr.IAttributeTypes;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.helpers.IFootprintMergeHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Device connector merge handler
 */
class DeviceConnectorMergeHandler implements IFootprintMergeHandler
{
	@NotNull private final Map<String, IDeviceConnector> sourceDevConns;
	@NotNull private final Map<String, IDeviceConnector> targetDevConns;
	@NotNull private final Map<IDeviceConnector, String> sourceDevConnsToMove;
	@NotNull private final Map<String, IDevicePin> sourceDevPins;
	@NotNull private final SetMap<IDeviceConnector, IDevicePin> sourceDevConnPinsToMove;
	@NotNull private final DevicePinlistMerger m_merger;
	@NotNull private final Map<IDeviceConnector, IMergeActionChange> deviceConnectorFeedbacks;

	DeviceConnectorMergeHandler(@NotNull IDevice sourceDevice, @NotNull IDevice targetDevice,
			@NotNull DevicePinlistMerger merger)
	{
		m_merger = merger;
		sourceDevConns = new HashMap<>();
		sourceDevice.getDeviceConnectors().forEach(dc -> sourceDevConns.put(dc.getName(), dc));
		targetDevConns = new HashMap<>();
		targetDevice.getDeviceConnectors().forEach(dc -> targetDevConns.put(dc.getName(), dc));
		sourceDevPins = new HashMap<>();
		CollectionUtils.filterByClass(sourceDevice.getPins(), IDevicePin.class)
				.forEach(dpin -> sourceDevPins.put(dpin.getName(), dpin));
		sourceDevConnsToMove = new HashMap<>();
		sourceDevice.getDeviceConnectors().forEach(sdc -> sourceDevConnsToMove.put(sdc, sdc.getName()));
		sourceDevConnPinsToMove = new SetMap<>();
		deviceConnectorFeedbacks = new HashMap<>();
	}

	@NotNull public Map<IDeviceConnector, String> getSourceDevConnsToMove()
	{
		return sourceDevConnsToMove;
	}

	@NotNull public SetMap<IDeviceConnector, IDevicePin> getSourceDevConnPinsToMove()
	{
		return sourceDevConnPinsToMove;
	}

	@NotNull public Map<IDeviceConnector, IMergeActionChange> getDeviceConnectorFeedbacks()
	{
		return deviceConnectorFeedbacks;
	}

	@Override public void exactMatch(@NotNull IUserFootprintConnector srcConnector,
			@NotNull IUserFootprintConnector tgtConnector)
	{
		mergeDeviceConnectors(srcConnector, tgtConnector);
	}

	@Override
	public void moveToTarget(@NotNull IUserFootprintMapping srcRow, @NotNull String connRename,
			@Nullable MergeFeedback feedback)
	{
		IDeviceConnector sourceDeviceConnector = sourceDevConns.get(srcRow.getConnector().getName());
		if (sourceDeviceConnector != null) {
			sourceDevConnsToMove.put(sourceDeviceConnector, connRename);
			IDevicePin devPin = sourceDevPins.get(srcRow.getPin().getName());
			if (devPin != null) {
				sourceDevConnPinsToMove.add(sourceDeviceConnector, devPin);
				if(!deviceConnectorFeedbacks.containsKey(sourceDeviceConnector) && feedback != null) {
					MergeActionChange mergeActionChange =
							determineChangeFeedback(sourceDeviceConnector, connRename, feedback);
					if(mergeActionChange != null) {
						deviceConnectorFeedbacks.put(sourceDeviceConnector, mergeActionChange);
					}
				}
			}
		}
	}

	@Nullable private MergeActionChange determineChangeFeedback(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull String connRename, @NotNull MergeFeedback feedback)
	{
		if (feedback == MergeFeedback.RENAMED_DUE_TO_PART_MISMATCH) {
			return getRenameFeedback(sourceDeviceConnector, connRename, ResourceMgr
					.getString(DeviceConnectorMergeHandler.class,
							"DeviceConnectorMergeHandler.renamedDueToPartMismatch.text"));
		}
		if (feedback == MergeFeedback.RENAMED_DUE_TO_DEVICE_PIN_MAPPED_TO_MULTIPLE_CONNECTORS) {
			return getRenameFeedback(sourceDeviceConnector, connRename, ResourceMgr
					.getString(DeviceConnectorMergeHandler.class,
							"DeviceConnectorMergeHandler.renamedDueToDevicePinMappedToMultipleConnectors.text"));
		}
		if (feedback == MergeFeedback.RENAMED_DUE_TO_CAVITY_MISMATCH) {
			return getRenameFeedback(sourceDeviceConnector, connRename, ResourceMgr
					.getString(DeviceConnectorMergeHandler.class,
							"DeviceConnectorMergeHandler.renamedDueToCavityMismatch.text"));
		}
		return null;
	}

	@NotNull private MergeActionChange getRenameFeedback(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull String connRename, @NotNull String reason)
	{
		StringBuilder sourceName = new StringBuilder();
		IBaseDevice owner = sourceDeviceConnector.getOwner();
		if (owner != null) {
			sourceName.append(owner.getName()).append(StringUtils.COLON);
		}
		sourceName.append(sourceDeviceConnector.getName());
		return new MergeActionChange(
				IAttributeTypes.NAME, sourceDeviceConnector.getName(), sourceDeviceConnector.getName(), connRename,
				IActionChange.ComparisonField.Attribute, sourceName.toString(), COFTypeEnum.DeviceConnector.toString(),
				reason);
	}

	@Override public void mergeWithTarget(@NotNull IUserFootprintMapping srcMapping,
			@NotNull IUserFootprintConnector targetConnector)
	{
		IDevicePin devPin = sourceDevPins.get(srcMapping.getPin().getName());
		IDeviceConnPin deviceConnPin = devPin == null ? null : devPin.getDeviceConnectorPin();
		if (devPin != null && deviceConnPin != null) {
			m_merger.addDeviceConnectorPinMapping(deviceConnPin, devPin);
		}
		if (sourceDevConnsToMove.containsKey(srcMapping.getConnector())) {
			mergeDeviceConnectors(srcMapping.getConnector(), targetConnector);
		}
	}

	private void mergeDeviceConnectors(@NotNull IUserFootprintConnector srcConnector,
			@NotNull IUserFootprintConnector tgtConnector)
	{
		IDeviceConnector sourceDeviceConnector = sourceDevConns.get(srcConnector.getName());
		IDeviceConnector targetDeviceConnector = targetDevConns.get(tgtConnector.getName());
		if (targetDeviceConnector != null && sourceDeviceConnector != null) {
			//merge these source and target device connectors.
			new DeviceConnectorPinlistMerger(m_merger).merge(sourceDeviceConnector, targetDeviceConnector);
			sourceDevConnsToMove.remove(sourceDeviceConnector);
		}
	}
}
