/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.merge;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible for merging backshell data from a source connector to a target connector.
 * Used by both ConnectorPinlistMerger and DeviceConnectorPinlistMerger.
 */
public class BackshellMerger implements IBackshellMerger
{

	/**
	 * Merges the backshell from the source connector onto the target connector.
	 * If the source has a backshell and the target does not, the backshell is transferred.
	 * If both have backshells, abstract pins are merged via the provided callback.
	 *
	 * @param sourceConnector     the connector supplying the backshell; must not be null
	 * @param targetConnector     the connector receiving the backshell; must not be null
	 * @param bsTerminationMerger the merger to use for merging backshell terminations when both connectors have backshells
	 */
	@Override
	public void mergeBackshell(@NotNull IConnector sourceConnector,
			@NotNull IConnector targetConnector,
			@NotNull IBackshellTerminationMerger bsTerminationMerger)
	{
		IBackshell sourceBackshell = sourceConnector.getBackshell();
		IBackshell targetBackshell = targetConnector.getBackshell();

		if (sourceBackshell != null && targetBackshell == null) {
			sourceConnector.setBackshell(null);
			targetConnector.setBackshell(sourceBackshell);
		}
		else if (sourceBackshell != null && targetBackshell != null) {
			bsTerminationMerger.mergeBackshellTerminations(sourceBackshell, targetBackshell);
		}
	}

	@Override
	public void mergeBackshell(@NotNull IBackshell sourceBackshell,
			@NotNull IBackshell targetBackshell,
			@NotNull IBackshellTerminationMerger bsTerminationMerger)
	{
		bsTerminationMerger.mergeBackshellTerminations(sourceBackshell, targetBackshell);
		mergeBackshellTerminationSchematics(targetBackshell, bsTerminationMerger);
	}

	@Override
	public void mergeBackshellTerminationSchematics(@NotNull IBackshell targetBackshell,
			@NotNull IBackshellTerminationMerger bsTerminationMerger)
	{
		IDevice device = getDevice(targetBackshell);
		if (device != null) {
			bsTerminationMerger.processSchematicsFor(device, new BSTerminationSchematicProcessor(bsTerminationMerger));
		}
	}

	@Override
	public void mergeBackshellConnectivity(@NotNull IBackshell sourceBackshell, @NotNull IBackshell targetBackshell,
			@NotNull IBackshellTerminationMerger bsTerminationMerger)
	{
		bsTerminationMerger.mergeBackshellTerminations(sourceBackshell, targetBackshell);
	}

	@Nullable
	private IDevice getDevice(@NotNull IBackshell backshell)
	{
		if (backshell.getOwner() instanceof IDeviceConnector dsc) {
			return dsc.getOwner(IDevice.class);
		}
		return null;
	}

	@Override
	public void mergeBackshellTerminationsSchematicsInDiagram(@NotNull IBackshell targetBackshell,
			@NotNull IBackshellTerminationMerger bsTerminationMerger, @NotNull ISchemDiagram diagram)
	{
		IDevice device = getDevice(targetBackshell);
		if (device != null) {
			bsTerminationMerger.processSchematicsForDiagram(device,
					new BSTerminationSchematicProcessor(bsTerminationMerger), diagram);
		}
	}

	private static class BSTerminationSchematicProcessor implements ISchematicProcessor
	{

		@NotNull private final IBackshellTerminationMerger bsTerminationMerger;

		BSTerminationSchematicProcessor(@NotNull IBackshellTerminationMerger bsTerminationMerger)
		{
			this.bsTerminationMerger = bsTerminationMerger;
		}

		public void process(IConnectivityRef schemObject)
		{
			if (schemObject instanceof IPinList pinList) {
				for (IPin schemPin : pinList.getPins()) {
					IGenericPin mappedConnectivity =
							(IGenericPin) bsTerminationMerger.getMappedValue(schemPin.getConnectivity());
					if (mappedConnectivity != null) {
						schemPin.setConnectivity(mappedConnectivity);
					}
				}
				bsTerminationMerger.addProcessedSchematic(schemObject);
			}
		}
	}
}