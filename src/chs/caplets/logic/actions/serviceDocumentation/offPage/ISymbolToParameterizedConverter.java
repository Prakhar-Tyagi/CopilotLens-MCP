/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.cof.logical.schem.IPinList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface to convert schematic objects into parameterized objects
 */
public interface ISymbolToParameterizedConverter
{

	/**
	 * @param pinListsToConvert schematic objects to be converted
	 * @return true if the conversion is successfully executed.
	 */
	default boolean convert(List<IPinList> pinListsToConvert)
	{
		return false;
	}

	@NotNull static ISymbolToParameterizedConverter getDefaultConverter()
	{
		return new ISymbolToParameterizedConverter()
		{
		};
	}
}
