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
import chs.cof.logical.ISourceObjectRef;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

public class DetermineDissociatedSourceObjectRefRule extends AbstractLayoutDesignSyncRule
{

	public DetermineDissociatedSourceObjectRefRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "DetermineDissociatedSourceObjectRefRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		for (IUID layoutObjectUID : layoutDesignMgr.getLayoutLogicObjectUIDs()) {
			for (ISourceObjectRef sourceObjectRef : layoutDesignMgr.getSourceObjectRefs(layoutObjectUID)) {
				if (!layoutDesignMgr.doesRefferedSourceExist(sourceObjectRef)) {
					syncChangeHolder.recordSourceObjectRefToBeRemoved(sourceObjectRef);
				}
			}
		}
		return true;
	}
}
