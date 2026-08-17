/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.merge;

import chs.cof.logical.cable.IDeviceConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Handles merging of device connectors including properties, attributes, library parts,
 * abstract pins, backshells, and mappings.
 * Delegates to DevicePinlistMerger for individual merge operations and uses BackshellMerger
 * for backshell-specific merging.
 */
public class DeviceConnectorPinlistMerger
{

	@NotNull private final DevicePinlistMerger m_devicePinlistMerger;

	public DeviceConnectorPinlistMerger(@NotNull DevicePinlistMerger devicePinlistMerger)
	{
		m_devicePinlistMerger = devicePinlistMerger;
	}

	/**
	 * Merges all aspects of device connectors including properties, attributes, library parts,
	 * abstract pins, backshells, and mappings.
	 * This method provides a centralized location for device-connector-specific merge logic.
	 *
	 * @param sourceDeviceConnector the device connector being merged from
	 * @param targetDeviceConnector the device connector being merged into
	 */
	public void merge(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull IDeviceConnector targetDeviceConnector)
	{
		m_devicePinlistMerger.mergeProperties(sourceDeviceConnector, targetDeviceConnector);
		m_devicePinlistMerger.mergeAttributes(sourceDeviceConnector, targetDeviceConnector);
		m_devicePinlistMerger.mergeLibraryPart(sourceDeviceConnector, targetDeviceConnector);
		m_devicePinlistMerger.mergeAbstractPins(sourceDeviceConnector, targetDeviceConnector);
		mergeBackshell(sourceDeviceConnector, targetDeviceConnector);
		m_devicePinlistMerger.addMapping(sourceDeviceConnector, targetDeviceConnector);
	}

	private void mergeBackshell(@NotNull IDeviceConnector sourceDeviceConnector,
			@NotNull IDeviceConnector targetDeviceConnector)
	{
		new BackshellMerger()
				.mergeBackshell(sourceDeviceConnector, targetDeviceConnector, m_devicePinlistMerger);
	}
}
