/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.helpers.DeletabilityChecker;
import chs.caplets.logic.helpers.IDeletabilityChecker;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static chs.utility.logic.LogicUtils.hasUsage;

/**
 * Delete Graphical Instance action - Delete the selected objects without deleting the connectivity
 */
public class UnplaceAction extends DeleteAction
{

	public UnplaceAction(ICapletController controller)
	{
		super(controller);
		deleteConnectivity = false;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selectSet)
	{
		Set<ILogicObject> objects = new HashSet<>();
		for (SelectedUIDObjectIterator iterator = selectSet.getSelectedUIDObjects(); iterator.hasNext(); ) {
			IUIDObject obj = iterator.getNext();
			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(obj);
			if (logicObject != null) {
				objects.add(logicObject);
			}
		}
		for (IUIDObject object : objects) {
			IDeletabilityChecker deletabilityChecker = new DeletabilityChecker();
			if (deletabilityChecker.canDelete(object, selectSet) && hasUsage((ILogicObject) object)) {
				container.add(new ActionEntry(getActionUI()));
				break;
			}
		}
	}

	@Override @NotNull public String getActionUIClass()
	{
		return UnplaceActionUI.class.getName();
	}
}
