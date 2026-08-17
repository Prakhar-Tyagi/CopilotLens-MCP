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
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.system.UIDMgr;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class CleanupDeletedLayoutObjectRefRule extends AbstractLayoutDesignSyncRule
{

	public CleanupDeletedLayoutObjectRefRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "CleanupDeletedLayoutObjectRefRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		Set<IUID> deletedLayoutObjectUID = new HashSet<>();
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		for (IUID layoutObjectUID : layoutDesignMgr.getLayoutLogicObjectUIDs()) {
			if (UIDMgr.getNonDeletedObject(layoutObjectUID) == null) {
				deletedLayoutObjectUID.add(layoutObjectUID);
			}
		}
		for (IUID deletedObjUID : deletedLayoutObjectUID) {
			layoutDesignMgr.removeSourceObjectReference(deletedObjUID);
		}
		return true;
	}
}
