/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.capture.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.DeleteAction;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Subclass of Logic Delete action to refresh styleset when a port from function is deleted.
 */

public class CaptureDeleteAction extends DeleteAction
{

	public CaptureDeleteAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return CaptureDeleteActionUI.class.getName();
	}

	@Override protected boolean editModel()
	{
		SelectSet selectionsToBeDeleted = getSelectionsToBeDeleted();
		Set<IUIDObject> objectsToBeProcessed = collectObjectsForPostProcessing(selectionsToBeDeleted);

		boolean completed = super.editModel();

		if (completed) {
			regenerateSchematics(objectsToBeProcessed);
		}
		return completed;
	}

	@NotNull private Set<IUIDObject> collectObjectsForPostProcessing(SelectSet toBeDeleted)
	{
		Set<IUIDObject> objectsTOBeRegenerated = new LinkedHashSet<>();
		SelectedUIDObjectIterator selectedUIDObjects = toBeDeleted.getSelectedUIDObjects();
		while (selectedUIDObjects.hasNext()) {
			IUIDObject object = selectedUIDObjects.getNext();
			if (object instanceof IPin) {
				IDiagramObject parent = ((IDiagramObject) object).getParent();
				if (parent != null) {
					objectsTOBeRegenerated.add(parent);
				}
			}
		}
		return objectsTOBeRegenerated;
	}

	private void regenerateSchematics(Set<IUIDObject> selectedObjects)
	{
		Generator generator = Generator.getGenerator();
		for (IUIDObject object : selectedObjects) {
			if (object instanceof IPinList && objectExistsOnDiagram((IDiagramObject) object)) {
				regenerateFunctionSchematics((IPinList) object, generator);
			}
		}
	}

	private boolean objectExistsOnDiagram(IDiagramObject diagramObject)
	{
		return DiagramHelper.getDiagram(diagramObject) != null;
	}

	private void regenerateFunctionSchematics(IPinList function, Generator generator)
	{
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(function);
		generator.generateDevice(function, gp);
	}
}
