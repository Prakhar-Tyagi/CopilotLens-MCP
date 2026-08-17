/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.topology.inlineassist.IDiagramConnectorDataCalculator;
import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.utility.topology.inlineconn.InlineShieldTerminationInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible for calculating the data for creating the connectors in the diagram.
 */
public class DiagramConnectorDataCalculator implements IDiagramConnectorDataCalculator
{

	@NotNull private final IConnectorGraphicsCalculator graphicsCalculator;
	@NotNull private final ShieldPinPositionCalculator shieldPinPositionCalculator;

	public DiagramConnectorDataCalculator(@NotNull InlineShieldTerminationInfo shieldTerminationInfo)
	{
		graphicsCalculator = createGraphicsCalculator();
		shieldPinPositionCalculator = createShieldPinPositionCalculator(shieldTerminationInfo);
	}

	@NotNull private LongestSegmentConnectorGraphicsCalculator createGraphicsCalculator()
	{
		return new LongestSegmentConnectorGraphicsCalculator();
	}

	@NotNull private ShieldPinPositionCalculator createShieldPinPositionCalculator(
			@NotNull InlineShieldTerminationInfo shieldTerminationInfo)
	{
		return new ShieldPinPositionCalculator(shieldTerminationInfo);
	}

	@Override @NotNull
	public IDiagramConnectorData getData(@NotNull ISystemLogicDiagram diagram, @NotNull List<IConductor> conductors,
			@NotNull Collection<IInlineAssistConductor> logicalConductors,
			@NotNull Set<IgnoredConductorInformation> ignoredConductors)
	{
		Collection<NewConnectorData> connectorData =
				graphicsCalculator.getNewConnectorsData(logicalConductors, conductors, ignoredConductors);
		connectorData.stream().forEach(connData -> shieldPinPositionCalculator.updateInlineExtent(connData, diagram));

		return new DiagramConnectorData(diagram, conductors, logicalConductors, ignoredConductors, connectorData);
	}
}
