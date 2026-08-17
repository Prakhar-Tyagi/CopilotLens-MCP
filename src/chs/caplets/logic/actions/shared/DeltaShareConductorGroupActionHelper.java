/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.autoshare.AutoShareIntoConductorGroupActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Helper class for sharing into multicore in delta scenarios. it allows share for empty multicore.
 */
public class DeltaShareConductorGroupActionHelper extends AutoShareIntoConductorGroupActionHelper
{

	public DeltaShareConductorGroupActionHelper(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext messageReporter,
			@NotNull Map<ILogicObject, ISharedObject> multicoreHierarchyMap,
			boolean isBulkPromotion, boolean isNewlyCreatedSharedObj)
	{
		super(design, messageReporter, multicoreHierarchyMap, isBulkPromotion, isNewlyCreatedSharedObj);
	}

	@Override protected boolean attemptLockOnSourceMulticoreForShare(@NotNull IMulticore multicore,
			@NotNull ILogicDesign logicDesign, @NotNull String failureMsg)
	{
		return ShareConcurrencyHelper.attemptLockOnDeltaSourceMulticoreForShareInto(multicore, logicDesign, failureMsg);
	}
}
