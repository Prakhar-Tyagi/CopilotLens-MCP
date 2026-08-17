/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ITabularEditorAppliedChangeListener;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.schem.IShieldBody;
import chs.common.IAttributePropertyProvider;
import chs.common.styles.IStyleableObject;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Logic Tabular Editor listener that applies style to the selected and edited diagram objects.
 */
public class LogicTabularEditorStyleRefresher implements ITabularEditorAppliedChangeListener
{
	@NotNull private final ICapletController controller;
	@NotNull private final Set<IAttributePropertyProvider> editedObjects;

	public LogicTabularEditorStyleRefresher(@NotNull ICapletController controller,
			@NotNull Set<IAttributePropertyProvider> editedObjects)
	{
		this.controller = controller;
		this.editedObjects = editedObjects;
	}

	@Override public void changesApplied()
	{
		SelectSet selections = controller.getSelectMgr().getPreSelections();
		Set<IStyleableObject> editedStyleableObjects = selections.getSelectedObjects(IStyleableObject.class).stream()
				.filter(obj -> hasBeenEdited(obj))
				.collect(Collectors.toSet());
		RegenerateGraphicsAction.getInstance().addObjectsForRegenrate(editedStyleableObjects, false);
	}

	private boolean hasBeenEdited(@NotNull IStyleableObject obj)
	{
		boolean isEdited = editedObjects.contains(obj.getAttributePropertyProvider());
		if (!isEdited) {
			if (obj instanceof IShieldBody schemShieldBody) {
				return editedObjects.contains(schemShieldBody.getConnectivity().getMulticore());
			}
		}
		return isEdited;
	}
}
