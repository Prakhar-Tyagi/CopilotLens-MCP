/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caplets.logic.actions.ui.EditDeviceConnectorDialog;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.user.IPrivilegedUserFootprintConnector;
import chs.cof.logical.footprint.user.UserDeviceFootprintFactory;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedDevicePin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cofUtils.parts.PartNumberHelper;
import chs.common.IReadOnlyNamedObject;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EditDeviceConnectorParams
{

	@NotNull private final IDevice device;
	@Nullable private final ISharedDevice sharedDevice;
	@Nullable private Set<EditDeviceConnectorTableRow> editDeviceConnectorTableRows = null;
	@NotNull protected final EditDeviceConnectorDataModel m_dataModel = new EditDeviceConnectorDataModel();

	public EditDeviceConnectorParams(@NotNull IDevice device)
	{
		this.device = device;
		sharedDevice = CommonUtils.cast(device.getSharedPinList(), ISharedDevice.class);
	}

	@NotNull public Collection<EditDeviceConnectorTableRow> getData()
	{
		if (editDeviceConnectorTableRows == null) {
			buildData();
		}
		assert editDeviceConnectorTableRows != null;
		return Collections.unmodifiableCollection(editDeviceConnectorTableRows);
	}

	@Nullable private String getCombinedPartNumber(@Nullable ILibrariedObject libObj)
	{
		return libObj != null ?
				PartNumberHelper.getCombinedPartNumber(libObj.getPartNumber(), libObj.getPartRevision()) : null;
	}

	private void buildData()
	{
		List<EditDeviceConnectorTableRow> allPinData = createEditDeviceConnectorTableRows();
		Collections.sort(allPinData, (o1, o2) -> o1.compareTo(o2));
		editDeviceConnectorTableRows = new LinkedHashSet<>();
		editDeviceConnectorTableRows.addAll(allPinData);
	}

	@NotNull
	protected List<EditDeviceConnectorTableRow> createEditDeviceConnectorTableRows()
	{
		List<EditDeviceConnectorTableRow> allPinData = new ArrayList<>();
		if (sharedDevice != null) {
			for (ISharedDevicePin devPin : CollectionUtils
					.filterByClass(sharedDevice.getPins(), ISharedDevicePin.class)) {
				if (devPin.isStud()) {
					continue;
				}
				ISharedPin devConnPin = devPin.getMatePin();
				ISharedPinList devConn = devConnPin != null ? devConnPin.getOwner() : null;
				buildPinData(devPin, devConnPin, devConn, devConn, allPinData);
			}
		}
		else {
			for (IDevicePin devPin : CollectionUtils.filterByClass(device.getPins(), IDevicePin.class)) {
				if (devPin.isStud()) {
					continue;
				}
				IDeviceConnPin devConnPin = devPin.getDeviceConnectorPin();
				IPinList devConn = devConnPin != null ? devConnPin.getOwner() : null;
				buildPinData(devPin, devConnPin, devConn, devConn, allPinData);
			}
		}
		return allPinData;
	}

	private void buildPinData(
			@NotNull IReadOnlyNamedObject devPin,
			@Nullable IReadOnlyNamedObject devConnPin,
			@Nullable IReadOnlyNamedObject devConn,
			@Nullable ILibrariedObject devConnLib,
			@NotNull List<EditDeviceConnectorTableRow> allPinData)
	{
		String combinedPartNumber = getCombinedPartNumber(devConnLib);
		FootprintDevicePinKey devicePinKey = new FootprintDevicePinKey(devPin.getName());
		String deviceConnectorPinName = devConnPin != null ? devConnPin.getName() : null;
		String deviceConnectorName = devConn != null ? devConn.getName() : null;
		buildPinData(allPinData, combinedPartNumber, deviceConnectorPinName, deviceConnectorName, devicePinKey);
	}

	protected void buildPinData(
			@NotNull List<EditDeviceConnectorTableRow> allPinData,
			@Nullable String combinedPartNumber,
			@Nullable String deviceConnectorPinName,
			@Nullable String deviceConnectorName,
			@NotNull FootprintDevicePinKey devicePinKey)
	{
		EditDeviceConnectorDetails details =
				new EditDeviceConnectorDetails(combinedPartNumber, deviceConnectorPinName, deviceConnectorName);
		m_dataModel.add(devicePinKey, details);
		allPinData.add(new EditDeviceConnectorTableRow(devicePinKey, m_dataModel));
	}

	@NotNull public IUserDeviceFootprint generateFootprint()
	{
		Collection<EditDeviceConnectorTableRow> deviceConnectorTableRows = getData();
		List<EditDeviceConnectorTableRow> validRows = getValidDeviceConnectorTableRows(deviceConnectorTableRows);
		//never return null from here. otherwise the device will treat
		//the existing device connectors as part of the footprint. which
		//will not clear the device connector if all need to be removed.
		UserDeviceFootprintFactory factory = new UserDeviceFootprintFactory();
		for (EditDeviceConnectorTableRow tableRow : validRows) {
			String pinName = tableRow.getDevicePinName();
			String connectorName = tableRow.getDeviceConnectorName();
			String cavityName = tableRow.getDeviceConnectorPinName();
			assert cavityName != null;
			assert connectorName != null;
			factory.addFootprintMapping(pinName, cavityName, connectorName);
			IPrivilegedUserFootprintConnector connector = factory.constructConnector(connectorName);
			List<String> tokens = StringUtils.splitStringintoList(tableRow.getDeviceConnectorPartNumber(),
					String.valueOf(PartNumberHelper.getPartRevisionSeparator()));
			if (!tokens.isEmpty()) {
				connector.setPartNumber(tokens.get(0));
				if (tokens.size() > 1) {
					connector.setPartRevision(tokens.get(1));
				}
			}
		}
		return factory.generateFootprint(sharedDevice != null ? sharedDevice : device);
	}

	@NotNull
	private List<EditDeviceConnectorTableRow> getValidDeviceConnectorTableRows(
			@NotNull Collection<EditDeviceConnectorTableRow> deviceConnectorTableRows)
	{
		List<EditDeviceConnectorTableRow> validRows = new ArrayList<>(deviceConnectorTableRows.size());
		for (EditDeviceConnectorTableRow tableRow : deviceConnectorTableRows) {
			String pinName = tableRow.getDevicePinName();
			String connectorName = tableRow.getDeviceConnectorName();
			String cavityName = tableRow.getDeviceConnectorPinName();
			if (arePinAndConnectorAndCavityNamesValid(pinName, connectorName, cavityName)) {
				validRows.add(tableRow);
			}
		}
		return validRows;
	}

	private boolean arePinAndConnectorAndCavityNamesValid(@Nullable String pinName, @Nullable String connectorName,
			@Nullable String cavityName)
	{
		return !StringUtils.isBlank(pinName) && !StringUtils.isBlank(connectorName) && !StringUtils.isBlank(cavityName);
	}

	public String getDialogTitle()
	{
		return ResourceMgr
				.getString(EditDeviceConnectorDialog.class, "EditDeviceConnectorDialog.title.text", device.getName());
	}
}
