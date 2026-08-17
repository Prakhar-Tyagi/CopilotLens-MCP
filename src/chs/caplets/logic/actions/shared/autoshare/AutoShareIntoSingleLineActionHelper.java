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
import chs.cof.logical.shared.ISharedObject;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible to support auto-share into behavior for Single Line
 */
public class AutoShareIntoSingleLineActionHelper extends AutoShareIntoHighwayActionHelper
{

	public AutoShareIntoSingleLineActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion, boolean isNewlyCreatedObject)
	{
		super(design, diagram, reporter, isBulkPromotion, isNewlyCreatedObject);
	}

	@Override protected boolean shouldAcceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		return SingleLineHelper.isSharedSingleLine(sharedObject);
	}
}
