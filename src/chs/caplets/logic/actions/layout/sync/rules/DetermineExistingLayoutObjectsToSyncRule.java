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
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.ISourceObjectRefIterator;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DetermineExistingLayoutObjectsToSyncRule extends AbstractLayoutDesignSyncRule
{

	public DetermineExistingLayoutObjectsToSyncRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "DetermineExistingLayoutObjectsToSyncRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncStateManager = getSync().getSyncStateManager();
		Map<IUID, ILogicDesign> modifiedDesignsMap = new HashMap<>();
		for (ILogicDesign logicDesign : syncStateManager.getDesignsToSyncFrom()) {
			modifiedDesignsMap.put(logicDesign.getUID(), logicDesign);
		}
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		for (IUID layoutObjectUID : layoutDesignMgr.getLayoutLogicObjectUIDs()) {
			final ISourceObjectRefIterator sourceObjectRefs = layoutDesignMgr.getSourceObjectRefs(layoutObjectUID);
			for (ISourceObjectRef sourceObjectRef : sourceObjectRefs) {
				if (modifiedDesignsMap.containsKey(sourceObjectRef.getSourceDesignUID())) {
					syncStateManager.recordExistingSourceObjectRef(sourceObjectRef);
				}
			}
		}
		return true;
	}
}
