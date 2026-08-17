/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.caf.CAFUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.utilities.ResourceMgr;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Class to report messages while using ICDs in Logic
 */

public abstract class ICDReporter implements IICDReporter
{

	@Override public void reportSingleEndedMessage(@NotNull IPinList pinList)
	{
		ILogicDesign design = Objects.requireNonNull(pinList.getLogicDesign());
		String msg = ResourceMgr.getString(ICDReporter.class,
				"SingleEndedConductor.Text", HTMLHelper.link(design, pinList));
		CAFUtils.getInstance().getOutputWindow().sendMessage(msg, getTabName(), true);
	}

	@NotNull protected abstract String getTabName();
}
