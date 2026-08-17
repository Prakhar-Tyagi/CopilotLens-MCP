/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.ILocation;
import chs.common.PreferenceContext;
import chs.common.Side;
import chs.common.preferencesets.IPreferenceSet;
import chs.utilities.ListMap;
import chs.view.schem.IPinProxy;
import chs.view.schem.PinProxy;
import chs.view.schem.logic.AbstractHarnessPlugConnectorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collections;

/**
 * Default schematic generator implementation for plug connector.
 * Uses AbstractHarnessPlugConnectorHandler#createConnectorSchematic for creating plug schematics
 */
public class SchematicsGenerator implements IMatedPinListSchematicsGenerator
{

	public SchematicsGenerator()
	{
	}

	@NotNull
	public IPinList createSchematics(chs.cof.logical.cable.IPinList replicatedPinList, IAbstractPin replicatedPin,
			IPin schematicDevicePin)
	{
//		assert replicatedPinList instanceof IHarnessPlugConnector;
		IAbstractPin devicePin = schematicDevicePin.getConnectivity();
		IPinList schematicDevice = (IPinList) schematicDevicePin.getParent();
		assert schematicDevice != null;
		ISchemDiagram diagram = schematicDevice.getDiagram();
		int gridSpacing = diagram.getGrid().getGridSpacing();
		PinProxy pinProxy =
				new PinProxy(devicePin, replicatedPin, Collections.emptyList(), Collections.emptyList());
		ILocation location = schematicDevicePin.getLocation();
		pinProxy.setLocation(devicePin, new Point(location.getX(), location.getY()));
		Point replicatedPinLocation = getReplicatedPinLocation(diagram, gridSpacing);
		PinSideCalculator pinsidecalculator = PinSideCalculator.createAbsolute(schematicDevice);
		Side side = pinsidecalculator.getSide(schematicDevicePin);

		pinProxy.setLocation(replicatedPin, replicatedPinLocation);

		pinProxy.setSchematicPin(schematicDevicePin);
		ILocation absLocation = schematicDevicePin.getAbsLocation(location.getX(), location.getY());
		IPreferenceSet preferenceSet = diagram.getPreferenceSet();
		IPinList connectorSchematic =
				createConnectorSchematic(replicatedPinList, side, replicatedPin, pinProxy,
						absLocation, preferenceSet, schematicDevice);
		connectorSchematic.markAsSupplementary();
		return connectorSchematic;
	}

	@NotNull protected Point getReplicatedPinLocation(ISchemDiagram diagram, int gridSpacing)
	{
		@SuppressWarnings("ConstantConditions") int generatedPinListWidth =
				diagram.getDesign().getProject().getPreferences()
						.getGeneratedConnectorWidth(PreferenceContext.LOGIC);
		Point replicatedPinLocation = new Point(gridSpacing * generatedPinListWidth, 0);
		return replicatedPinLocation;
	}

	@NotNull protected IPinList createConnectorSchematic(chs.cof.logical.cable.IPinList replicatedPinList, Side side,
			IAbstractPin replicatedPin, IPinProxy pinProxy, ILocation absLocation,
			@Nullable IPreferenceSet preferenceSet, IPinList schematicDevice)
	{
		ListMap<IAbstractPin, IPinProxy> connectorPinProxyMap = new ListMap<IAbstractPin, IPinProxy>();
		connectorPinProxyMap.add(replicatedPin, pinProxy);
		return AbstractHarnessPlugConnectorHandler
				.createConnectorSchematic(replicatedPinList, connectorPinProxyMap, absLocation,
						side, schematicDevice, schematicDevice.getDiagram(), preferenceSet);
	}

	@Override public void regenerateSchematics(@NotNull IPinList schematicConnector)
	{
//		schematicConnector.regenerateDiagramObject();
	}
}
