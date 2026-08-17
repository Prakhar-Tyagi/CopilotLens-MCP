/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.batchshare.ui.AbstractBatchShareDialog;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareDialog;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareParams;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Batch share action executor
 */
public class BatchShareActionExecutor extends AbstractBatchShareActionExecutor
{

	public BatchShareActionExecutor(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		super(project, designs, entitiesShareCriteria);
	}

	@Override @NotNull protected AbstractBatchShareDialog getBatchShareDialog(@NotNull BatchShareParams batchShareParams)
	{
		return new BatchShareDialog(CAFUtils.getInstance().getDialogFrame(),
				ResourceMgr.getString(BatchShareActionExecutor.class, "BatchShareActionExecutor.Dialog.title"),
				batchShareParams);
	}

	@Override protected boolean doShareUnplacedObjects()
	{
		return false;
	}
}
