/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cofUtils.parameterized.SymbolExtentForPinSide;

/**
 * AutoCloseable to register ICDSymbolExtentForPinSide
 */

public class SymbolPinSideCalculatorForICD implements AutoCloseable
{

	public SymbolPinSideCalculatorForICD()
	{
		SymbolExtentForPinSide.getInstance().registerICDSymbolExtentForPinSide();
	}

	@Override public void close()
	{
		SymbolExtentForPinSide.getInstance().registerDefaultSymbolExtentForPinSide();
	}
}
