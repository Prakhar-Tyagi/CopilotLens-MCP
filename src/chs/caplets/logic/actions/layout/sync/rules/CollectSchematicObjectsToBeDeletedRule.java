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
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class CollectSchematicObjectsToBeDeletedRule extends AbstractLayoutDesignSyncRule
{

	public CollectSchematicObjectsToBeDeletedRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "CollectSchematicObjectsToBeDeletedRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		final Collection<IUID> objectUIDsToBeDeleted = syncChangeHolder.getObjectUIDsToBeDeleted();
		final IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
		for (IUID toBeDeleted : objectUIDsToBeDeleted) {
			final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(toBeDeleted);
			if (logicObject != null) {
				final List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(logicObject);
				syncChangeHolder.recordSchemsToBeDeleted(usages);
			}
		}
		return true;
	}
}
