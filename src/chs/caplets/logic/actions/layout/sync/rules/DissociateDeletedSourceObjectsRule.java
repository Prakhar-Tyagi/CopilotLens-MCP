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

public class DissociateDeletedSourceObjectsRule extends AbstractLayoutDesignSyncRule
{

	public DissociateDeletedSourceObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "DissociateDeletedSourceObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();

		for (ISourceObjectRef sorToRemove : syncChangeHolder.getSourceObjectRefsToBeRemoved()) {
			layoutDesignMgr.removeSourceObjectReference(sorToRemove);
			final IUID referrerObjectUID = sorToRemove.getReferrerObjectUID();
			if (!layoutDesignMgr.hasSourceObjectReferences(referrerObjectUID)) {
				syncChangeHolder.recordObjectToBeDeleted(referrerObjectUID);
			}
		}
		return true;
	}
}
