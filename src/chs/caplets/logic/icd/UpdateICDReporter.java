/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.caplets.logic.actions.UpdateICDActionHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Class to report messages while updating ICDs in Logic
 */
public class UpdateICDReporter extends ICDReporter
{

	@NotNull @Override protected String getTabName()
	{
		return UpdateICDActionHelper.getOutputTabName();
	}
}
