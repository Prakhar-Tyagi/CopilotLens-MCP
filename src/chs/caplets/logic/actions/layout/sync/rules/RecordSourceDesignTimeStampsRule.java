/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.caplets.logic.actions.layout.sync.ILayoutDesignSyncStateManager;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

public class RecordSourceDesignTimeStampsRule extends AbstractLayoutDesignSyncRule
{

	public RecordSourceDesignTimeStampsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "RecordSourceDesignTimeStampsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncStateManager = getSync().getSyncStateManager();
		for (ILogicDesign sourceDesign : syncStateManager.getDesignsToSyncFrom()) {
			if (sourceDesign != null) {
				syncStateManager.recordSourceDesignTimeStamp(sourceDesign.getUID(), sourceDesign.getTimeModified());
			}
		}
		return true;
	}
}
