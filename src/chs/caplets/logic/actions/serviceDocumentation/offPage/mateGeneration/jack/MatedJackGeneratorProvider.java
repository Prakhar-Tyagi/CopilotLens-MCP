/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.jack;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListSchematicsGenerator;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.MatedPinListGeneratorProvider;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;

/**
 * This provides the schematics generator for the mated jack connector
 */
public class MatedJackGeneratorProvider extends MatedPinListGeneratorProvider
{

	public MatedJackGeneratorProvider(IMessageCollectorAndReporter messageReporter,
			ISchemDiagram activeDiagram)
	{
		super(messageReporter, activeDiagram);
	}

	@NotNull @Override protected IMatedPinListSchematicsGenerator getSchematicsGenerator()
	{
		return new MatedJackSchematicsGenerator();
	}
}
