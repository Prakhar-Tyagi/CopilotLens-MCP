/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.device;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.SchematicsGenerator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ILocation;
import chs.common.Side;
import chs.common.preferencesets.IPreferenceSet;
import chs.utility.helpers.ConnectionHelper;
import chs.view.schem.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;

public class DeviceSchematicsGenerator extends SchematicsGenerator
{

	@NotNull protected Point getReplicatedPinLocation(ISchemDiagram diagram, int gridSpacing)
	{
		Point replicatedPinLocation = new Point(0, 0);
		return replicatedPinLocation;
	}

	@NotNull @Override
	protected IPinList createConnectorSchematic(chs.cof.logical.cable.IPinList replicatedPinList, Side side,
			IAbstractPin replicatedPin, IPinProxy pinProxy, ILocation absLocation,
			@Nullable IPreferenceSet preferenceSet, IPinList schematicDevice)
	{
		final IPinList connectorSchematic =
				super.createConnectorSchematic(replicatedPinList, side, replicatedPin, pinProxy, absLocation,
						preferenceSet, schematicDevice);
		connectDevicePins(replicatedPin, pinProxy);
		return connectorSchematic;
	}

	private void connectDevicePins(IAbstractPin replicatedPin, IPinProxy pinProxy)
	{
		final IAbstractPin d1Pin = pinProxy.getPin();
		final IPin schemPin1 = pinProxy.getSchemPin(d1Pin);
		final IPin schemPin2 = pinProxy.getSchemPin(replicatedPin);
		if (schemPin1 != null && schemPin2 != null) {
			ConnectionHelper.connectDevicePins(schemPin1, schemPin2);
		}
	}
}
