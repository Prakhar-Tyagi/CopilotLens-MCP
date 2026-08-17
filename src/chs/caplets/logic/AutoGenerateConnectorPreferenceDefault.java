/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic;

import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

/**
 * This class returns default value for AutoGenerateConnector toggle
 */
public class AutoGenerateConnectorPreferenceDefault
{

	private AutoGenerateConnectorPreferenceDefault()
	{
	}

	/**
	 * @param design - design from which we will read Design Abstraction's "AllowAutoCreation" flag value.
	 * @return default value for AutoGenerateConnector from Design Abstraction.
	 */
	public static boolean getDefault(@NotNull ILogicDesign design)
	{
		if (design.getDesignAbstraction() != null) {
			return design.getDesignAbstraction().getAllowAutoCreation();
		}
		return true;
	}
}
