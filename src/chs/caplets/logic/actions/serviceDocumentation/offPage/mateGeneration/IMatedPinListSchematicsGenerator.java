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
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;

/**
 * interface provides a way to generate connector schematics, also provides a way to regenerate schematics
 */
public interface IMatedPinListSchematicsGenerator
{

	@NotNull chs.cof.logical.schem.IPinList createSchematics(IPinList replicatedPinList,
			IAbstractPin replicatedPin, IPin schematicDevicePin);

	void regenerateSchematics(@NotNull chs.cof.logical.schem.IPinList schematicConnector);
}
