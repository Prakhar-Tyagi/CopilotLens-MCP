/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.caplets.logic.actions.layout.sync.ILayoutDesignSyncStateManager;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UpdateSourceDesignTimeStampsRule extends AbstractLayoutDesignSyncRule
{

	public UpdateSourceDesignTimeStampsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "UpdateSourceDesignTimeStampsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		for (ISourceDesignRef systemLogicDesignRef : layoutDesignMgr.getSystemLogicDesignRefs()) {
			final Long timeStamp =
					syncChangeHolder.getSourceDesignTimeStamp(systemLogicDesignRef.getReferencedDesignUID());
			if (timeStamp != null) {
				systemLogicDesignRef.setModificationTimeStampData(
						Objects.requireNonNull(systemLogicDesignRef.getReferencedDesign()).getDesignModificationTimeStampData());
			}
		}
		reporter.reportSyncChangesMade("UpdateSourceDesignTimeStampsRule.completedSync", design.getFullName());
		return true;
	}
}
