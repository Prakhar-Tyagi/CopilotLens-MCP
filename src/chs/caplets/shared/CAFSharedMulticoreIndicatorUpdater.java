/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-22 Siemens
 */
package chs.caplets.shared;

import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 08-10-2022.
 */
public class CAFSharedMulticoreIndicatorUpdater extends SharedMulticoreIndicatorUpdater
{

	public CAFSharedMulticoreIndicatorUpdater(@NotNull ILogicModel model)
	{
		super(model.getDesign());
	}
}
