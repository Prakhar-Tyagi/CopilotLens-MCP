/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.modular;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.SchematicsGenerator;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.helpers.ConnectorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Schematic generator implementation for modular connectors
 * Once the modular hierarchy is shared into, regenerateSchematicConnector is called to generate the proper modular schematics
 */
class ModularConnectorSchematicsGenerator extends SchematicsGenerator
{

	@Override public void regenerateSchematics(@NotNull IPinList schematicConnector)
	{
		ISchemDiagram diagram = schematicConnector.getDiagram();
		ConnectorHelper
				.distributeAddPinArgsToPinLists(schematicConnector, diagram, new ArrayList<>(), (a, b) -> {
				}, (p) -> true);
	}
}
