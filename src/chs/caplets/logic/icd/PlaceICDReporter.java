/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.utilities.AppInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Class to report messages while placing ICDs in Logic
 */
public class PlaceICDReporter extends ICDReporter
{

	@NotNull @Override protected String getTabName()
	{
		return AppInfo.getAppInfo().getApplicationTitle();
	}
}
