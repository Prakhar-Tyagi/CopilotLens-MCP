/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.merge;

import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import org.jetbrains.annotations.NotNull;

public class BlockDevicePinlistMerger extends PinlistMerger
{

	public BlockDevicePinlistMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
	}

	@Override protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);
		resetSchematicsConnectivity(sourceSchemObject, targetlogicObject);
		IPinList schemDevice = (IPinList) sourceSchemObject;
		for (IPinList schemAttachedPinlist : schemDevice.getAttachedPinListObjects()) {

			detachUnconnectedConnector(schemAttachedPinlist, schemDevice);
		}
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{

		super.mergeChildrenConnectivity(sourceLogicObject, targetLogicObject);

		DeviceAndConnectorMergerHandler
				.mergeConnectors((IBaseDevice) sourceLogicObject, (IBaseDevice) targetLogicObject);
	}
}
