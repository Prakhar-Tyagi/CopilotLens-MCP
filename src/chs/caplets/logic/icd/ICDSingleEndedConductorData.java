/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that holds the information about the pins and the single ended conductors generated against them
 */

public class ICDSingleEndedConductorData
{
	private Map<IPin, IConductor> singleEndedConductors;

	public ICDSingleEndedConductorData()
	{
		singleEndedConductors = new HashMap<>();
	}

	public void addData(IPin pin, IConductor conductor)
	{
		singleEndedConductors.put(pin, conductor);
	}

	@NotNull public Map<IPin, IConductor> getData()
	{
		return singleEndedConductors;
	}

}
