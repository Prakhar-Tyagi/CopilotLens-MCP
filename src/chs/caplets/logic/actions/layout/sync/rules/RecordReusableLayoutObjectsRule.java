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
import chs.caplets.logic.actions.layout.sync.SyncReconstructHierarchyProvider;
import chs.cof.logical.ILayoutLogicDesign;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.system.UIDMgr;
import chs.utility.IUIDObjectHierarchyProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class RecordReusableLayoutObjectsRule extends AbstractLayoutDesignSyncRule
{

	public RecordReusableLayoutObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "RecordReusableLayoutObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		final Collection<IUID> existingLayoutObjects = syncChangeHolder.getExistingLayoutObjects();
		final SyncReconstructHierarchyProvider hierarchyProvider = getSync().getReconstrutHierarchyProvider();
		for (IUID layoutObjUID : existingLayoutObjects) {
			final IUIDObject uidObject = UIDMgr.getNonDeletedObject(layoutObjUID);
			if (uidObject != null) {
				recordHierarchy(uidObject, hierarchyProvider);
			}
		}
		return true;
	}

	private void recordHierarchy(@NotNull IUIDObject uidObject, @NotNull IUIDObjectHierarchyProvider hierarchyProvider)
	{
		final List<IUID> allChildrenInHierarchy = hierarchyProvider.getAllChildrenInHierarchy(uidObject);
		for (IUID childUID : allChildrenInHierarchy) {
			recordAsReusable(childUID);
		}
		recordAsReusable(uidObject.getUID());
	}

	private void recordAsReusable(@NotNull IUID objectToReuse)
	{
		getSync().getSyncStateManager().recordUIDToBeReused(objectToReuse);
	}
}
