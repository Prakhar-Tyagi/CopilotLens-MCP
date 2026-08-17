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
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.cof.logical.ISourceDesignRefIterator;
import chs.cof.project.IProject;
import chs.common.IDesignMgr;
import chs.common.IUID;
import chs.common.sync.AbstractBaseSync;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

public class SyncMissingSourceDesignsRule extends AbstractLayoutDesignSyncRule
{

	public SyncMissingSourceDesignsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return AbstractBaseSync.CHECKING_ASSOCIATED_DESIGNS;
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		ISourceDesignRefIterator iter = layoutDesignMgr.getSystemLogicDesignRefs();
		final IProject project = design.getProject();
		assert project != null;
		final IDesignMgr designMgr = project.getDesignMgr();
		while (iter.hasNext()) {
			ISourceDesignRef ref = iter.next();
			ILogicDesign logicDesign = ref.getReferencedDesign();
			// If the design is missing (double check into the proj manager to see if it has completely gone)
			// then remove it and output a message.
			final IUID referencedObjectUID = ref.getDesignReference().getReferencedObjectUID();
			if (logicDesign == null && (referencedObjectUID == null ||
					designMgr.getLogicalDesign(referencedObjectUID) == null)) {
				layoutDesignMgr.removeSystemLogicDesignRef(ref);
				ref.delete();
				reporter.reportSyncChangesMade("SyncMissingSourceDesignsRule.deletedDesign",
						referencedObjectUID);
			}
		}
		return true;
	}
}
