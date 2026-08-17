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
import chs.caplets.logic.layout.ParameterizedPhysicalDimensionUpdater;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class UpdateSchemDevicePhysicalDimensionsRule extends AbstractLayoutDesignSyncRule
{

	public UpdateSchemDevicePhysicalDimensionsRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "UpdateSchemDevicePhysicalDimensionsRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			final ParameterizedPhysicalDimensionUpdater paramUpdater =
					ParameterizedPhysicalDimensionUpdater.getInstance();
			final Collection<IUID> objectUIDsToBeDeleted = getSync().getSyncStateManager().getObjectUIDsToBeDeleted();
			for (IDevice device : connectivity.getDevices()) {
				if (!objectUIDsToBeDeleted.contains(device.getUID())) {
					paramUpdater
							.updateParamPhysicalDimension(design, device, (msg) -> reportWarningMessage(reporter, msg));
				}
			}
		}
		return true;
	}

	private void reportWarningMessage(@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter,
			@NotNull String updaterMessage)
	{
		reporter.reportWarning("UpdateSchemDevicePhysicalDimensionsRule.failedToUpdateDimension", updaterMessage);
	}
}
