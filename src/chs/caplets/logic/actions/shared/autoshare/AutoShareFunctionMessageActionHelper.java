/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.SharedFunctionMessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of function message batch share action helper
 */
public class AutoShareFunctionMessageActionHelper extends AutoShareConductorActionHelper
{

	public AutoShareFunctionMessageActionHelper(@NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion)
	{
		super(design, diagram, reporter, isBulkPromotion);
		setShareHelper(new SharedFunctionMessageHelper());
	}
}
