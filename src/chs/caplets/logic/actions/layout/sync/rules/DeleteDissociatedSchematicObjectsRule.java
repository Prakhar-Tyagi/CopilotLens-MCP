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

import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.caplets.logic.actions.layout.sync.ILayoutDesignSyncStateManager;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.drawplus.ISegmentContainer;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeleteDissociatedSchematicObjectsRule extends AbstractLayoutDesignSyncRule
{

	public DeleteDissociatedSchematicObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "DeleteDissociatedSchematicObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		for (IUID diagramUID : syncChangeHolder.getDiagramsToProcessForSchemDeletion()) {
			final ISchemDiagram diagram = design.getDiagram(diagramUID);
			if (diagram != null) {
				diagram.loadToMemory();
				final List<IUIDObject> schemsForDeletion =
						UIDUtils.convertToUIDObject(syncChangeHolder.getSchemsForDeletion(diagramUID));
				DeleteHelper.getInstance().delete(diagram, expandedSetToDelete(schemsForDeletion), true);
			}
		}
		return true;
	}

	@NotNull private Collection<IUIDObject> expandedSetToDelete(@NotNull Collection<IUIDObject> toExpand)
	{
		Set<IUIDObject> expandedSet = new HashSet<>(toExpand);
		for (IUIDObject iuidObject : toExpand) {
			if (iuidObject instanceof ISegmentContainer) {
				expandedSet.addAll(((ISegmentCollector) iuidObject).getSegments());
			}
		}
		return expandedSet;
	}
}
