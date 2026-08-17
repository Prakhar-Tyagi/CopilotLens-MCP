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

import java.util.Map;
import java.util.function.Function;

/**
 * provides mated connector generator
 */
public interface IMatedPinListGeneratorProvider
{

	/**
	 * @param pinPairs pairs of schematic connector pin to add on connector and schematic device pin mated to the pin
	 * @return generator which generates the schematics and other things necessary to generate schematic (replicate, share into)
	 */
	@NotNull IMatedPinListGenerator getMatedPinListGenerator(Map<IPin, IPin> pinPairs);

	/**
	 * @return transformer which trasnforms the pin pair map into another pin pair map which is needed for the implementation
	 */
	@NotNull default Function<Map<IPin, IPin>, Map<IPin, IPin>> getPinPairTransformer()
	{
		return Function.identity();
	}

	@NotNull static IPinList getOwner(Map.Entry<IPin, IPin> param)
	{
		IPin fetchedSchematicPin = param.getKey();
		IAbstractPin connectivity = fetchedSchematicPin.getConnectivity();
		IPinList owner = connectivity.getOwner();
		assert owner != null;
		return owner;
	}
}
