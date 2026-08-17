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
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SynchronizeDiagramsRule extends AbstractLayoutDesignSyncRule
{

	public SynchronizeDiagramsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "SynchronizeDiagramsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final Set<IUID> unusedUIDs = getSync().getSyncStateManager().getUnusedUIDs();
		getSync().runDiagramSync(unusedUIDs, (diagramObject -> reportUnplacedObjects(diagramObject, reporter)));
		for (IUID unusedUID : unusedUIDs) {
			logUnusedObject(unusedUID, design.getFullName());
		}
		return true;
	}

	private void reportUnplacedObjects(@NotNull IDiagramObject diagramObject,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ISchemDiagram diagram = DiagramHelper.getDiagram(diagramObject);
		if (diagram != null) {
			final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(diagramObject);
			if (logicObject != null) {
				reportUnplacedObjects(logicObject, diagram, reporter);
			}
		}
	}

	private void reportUnplacedObjects(@NotNull ILogicObject logicObject, @NotNull ISchemDiagram diagram,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final IDevice device = CommonUtils.cast(logicObject, IDevice.class);
		if (device != null) {
			reporter.reportWarning("SynchronizeDiagramsRule.unplacedDevice", device.getName(), diagram.getName());
		}
	}

	private void logUnusedObject(@NotNull IUID unusedUID, @NotNull String designName)
	{
		final IUIDObject nonDeletedObject = UIDMgr.getNonDeletedObject(unusedUID);
		if (nonDeletedObject != null) {
			logUnusedObject(nonDeletedObject, designName);
		}
	}

	private void logUnusedObject(@NotNull IUIDObject nonDeletedObject, @NotNull String designName)
	{
		String uid = nonDeletedObject.getUID().getString();
		final String name =
				nonDeletedObject instanceof IReadOnlyNamedObject ? ((IReadOnlyNamedObject) nonDeletedObject).getName() :
						StringUtils.EMPTY_STRING;
		final String type = nonDeletedObject.getClass().getSimpleName();
		System.out.println(
				"Layout sync : delete unused object from design <" + designName + ">: Deleted " + type + " " + uid +
						" " + name);
		assertKnownUnsedObjectChild(nonDeletedObject);
	}

	private void assertKnownUnsedObjectChild(@NotNull IUIDObject nonDeletedObject)
	{
		assert nonDeletedObject instanceof IDeviceConnector || nonDeletedObject instanceof IGenericPin ||
				nonDeletedObject instanceof IInternalLink || nonDeletedObject instanceof IBackshell :
				"SyncReconstructHierarchyProvider allows reusing child objects. " +
						"If these child objects are deleted in source design, they must be deleted here. " +
						"Otherwise they could be part of edit and get validated in endEdit" +
						"If a new type is reported, make sure that the schematic cleanup is done before deleting its connectivity here." +
						"And reset this connectivity object's relation with true (Remaining existing) objects in design." +
						"These objects by now will only have one way connection to true objects on layout design." +
						"Calling delete directly tries to reset two way connection e.g. devicePin <-> deviceConnPin";
	}
}
