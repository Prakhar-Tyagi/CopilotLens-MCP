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
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.utilities.ListMap;
import chs.utilities.StringUtils;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DeleteDissociatedCableObjectsRule extends AbstractLayoutDesignSyncRule
{

	private static final String DeleteLogPrefix = "Layout sync : delete object";

	public DeleteDissociatedCableObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "DeleteDissociatedCableObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignSyncStateManager syncChangeHolder = getSync().getSyncStateManager();
		final List<IUIDObject> toBeDeleted =
				UIDUtils.convertToNonDeletedUIDObjects(syncChangeHolder.getObjectUIDsToBeDeleted());
		final ListMap<String, String> deletedObjectsToReport = new ListMap<>();
		for (IUIDObject iuidObject : toBeDeleted) {
			recordDeletedObjectToReport(iuidObject, deletedObjectsToReport);
		}
		DeleteHelper.getInstance().delete(design, toBeDeleted, DeleteLogPrefix, true);
		reportObjectDeleted(reporter, deletedObjectsToReport, design);
		return true;
	}

	private void reportObjectDeleted(@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter,
			@NotNull ListMap<String, String> recorder, @NotNull ILayoutLogicDesign design)
	{
		for (Map.Entry<String, List<String>> newObjectsEntry : recorder.entrySet()) {
			final String type = newObjectsEntry.getKey();
			final List<String> values = newObjectsEntry.getValue();
			Collections.sort(values);
			final String deletedObjectNames = StringUtils.concatenate(values, StringUtils.COMMA_SPACE);
			final String designName = design.getFullName();
			reporter.reportSyncChangesMade("DeleteDissociatedCableObjectsRule.dissociatedObject",
					StringUtils.toLowerCase(type), designName, deletedObjectNames);
		}
	}

	private void recordDeletedObjectToReport(@NotNull IUIDObject toBeDeletedObject,
			@NotNull ListMap<String, String> recorder)
	{
		final String type = COFTypeEnum.getDisplayableTypeName(toBeDeletedObject);
		final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(toBeDeletedObject);
		final String dissociatedObjName =
				logicObject != null ? logicObject.getName() : toBeDeletedObject.getUID().getString();
		recorder.add(type, dissociatedObjName);
	}
}
