/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * Shared infrastructure for all device creation strategies.
 * <p>
 * Provides a helper to dispatch an action through the active caplet controller's
 * action manager and a reusable part-number validation helper, eliminating
 * duplicated boilerplate across concrete strategies.
 * </p>
 */
public abstract class AbstractDeviceCreationStrategy implements IDeviceCreationStrategy
{

	private static final int IMMERSED_ACTION_EVENT_ID = 10020;

	/**
	 * Checks whether the requested part number mismatches the existing one
	 * and, if so, shows an error dialog.
	 *
	 * @param requestedPartNumber the part number from the device info
	 * @param existingPartNumber  the part number already associated with the
	 *                            existing object (pin list, shared pin list,
	 *                            or library device)
	 * @return {@code true} if the part numbers do <b>not</b> match (caller
	 *         should abort); {@code false} if they match or validation is not
	 *         needed
	 */
	protected boolean isPartNumberMismatch(@NotNull String requestedPartNumber,
			@Nullable String existingPartNumber)
	{
		if (!Objects.equals(requestedPartNumber, existingPartNumber)) {
			if (existingPartNumber == null && requestedPartNumber.isEmpty()) {
				return false; // both are effectively "no part number", so treat as match
			}
			String message = ResourceMgr.getString(AbstractDeviceCreationStrategy.class,
					"AbstractDeviceCreationStrategy.Error.PartMatchError");
			LogHelper.appMsgSafe(message);
			return true;
		}
		return false;
	}

	/**
	 * Dispatches the given action through the caplet controller's action manager.
	 *
	 * @param context the device creation context (provides FIB and caplet controller)
	 * @param action  the action to perform
	 */
	protected void dispatchAction(@NotNull DeviceCreationContext context, @NotNull IAction action)
	{
		ICapletController controller = context.getActiveCapletController();
		ActionEvent event = new ActionEvent(context.getFib().getApplication(), IMMERSED_ACTION_EVENT_ID, "");
		controller.getActionMgr().actionPerformed(action, event);
	}
}

