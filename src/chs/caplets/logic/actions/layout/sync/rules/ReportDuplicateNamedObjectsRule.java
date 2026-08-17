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
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.ctf.drc.DRCDuplicateNameCheckHelper;
import chs.utilities.ListMap;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

public class ReportDuplicateNamedObjectsRule extends AbstractLayoutDesignSyncRule
{

	public ReportDuplicateNamedObjectsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "ReportDuplicateNamedObjectsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final DRCDuplicateNameCheckHelper duplicateNameCheckHelper = new DRCDuplicateNameCheckHelper();
		final Collection<ILogicObject> interestingObjects = duplicateNameCheckHelper.getInterestingObjects(design);
		final ListMap<String, ILogicObject> seen = new ListMap<>();
		//Create Map
		final Collection<IUID> objectUIDsToBeDeleted = getSync().getSyncStateManager().getObjectUIDsToBeDeleted();
		for (ILogicObject logicObject : interestingObjects) {
			if (!objectUIDsToBeDeleted.contains(logicObject.getUID())) {
				String myKey = duplicateNameCheckHelper.createKey(logicObject, getProject().getPreferences());
				seen.add(myKey, logicObject);
			}
		}

		//Report Duplicates
		Collection<List<ILogicObject>> collectionDupLists = seen.values();
		for (List<ILogicObject> dupList : collectionDupLists) {
			if (dupList.size() > 1) {
				reportDuplicateNameError(duplicateNameCheckHelper, dupList, reporter, design);
			}
		}
		return true;
	}

	private void reportDuplicateNameError(@NotNull DRCDuplicateNameCheckHelper nameCheckHelper,
			@NotNull List<ILogicObject> dupList, @NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter,
			@NotNull ILayoutLogicDesign design)
	{
		//prepare to display list of duplicate objects
		String msg = nameCheckHelper.getObjString(dupList.size());
		final List<Object> objects = nameCheckHelper.generateObjList(dupList);
		Object[] objArray = new Object[objects.size()];
		for (int i = 0; i < objects.size(); i++) {
			final Object obj = objects.get(i);
			if (obj instanceof IReadOnlyNamedObject) {
				objArray[i] = HTMLHelper.link(design, (IReadOnlyNamedObject) obj);
			}
			else {
				objArray[i] = obj;
			}
		}
		final String formattedText = new MessageFormat(msg).format(objArray);
		reporter.reportWarning("ReportDuplicateNamedObjectsRule.duplicateNameMsg", formattedText);
	}
}
