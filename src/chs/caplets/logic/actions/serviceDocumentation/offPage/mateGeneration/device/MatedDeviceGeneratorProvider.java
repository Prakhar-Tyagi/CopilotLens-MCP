/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.device;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListSchematicsGenerator;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.MatedPinListGeneratorProvider;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;

public class MatedDeviceGeneratorProvider extends MatedPinListGeneratorProvider
{

	public MatedDeviceGeneratorProvider(IMessageCollectorAndReporter messageReporter,
			ISchemDiagram activeDiagram)
	{
		super(messageReporter, activeDiagram);
	}

	@NotNull @Override protected IMatedPinListSchematicsGenerator getSchematicsGenerator()
	{
		return new DeviceSchematicsGenerator();
	}
}
