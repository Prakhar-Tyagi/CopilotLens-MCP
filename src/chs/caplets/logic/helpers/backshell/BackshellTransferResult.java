/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Categorises the outcome of a backshell termination migration step and encapsulates
 * the logic for building the corresponding user-visible message string.
 */
public enum BackshellTransferResult
{
	Success("BackshellTransferMessageHelper.transfer.success"),
	SharedDeviceFrozen("BackshellTransferMessageHelper.transfer.frozenDevice"),
	SharedConnectorNotEditable("BackshellTransferMessageHelper.transfer.nonEditableSharedConnector"),
	LockFailed("BackshellTransferMessageHelper.transfer.lockedSharedConnector"),
	LibraryBackshellMisMatch("BackshellTransferMessageHelper.transfer.invalidPart"),
	SharedConnectorFrozen("BackshellTransferMessageHelper.transfer.frozenConnector"),
	RingTerminalConnector("BackshellTransferMessageHelper.transfer.ringTerminalConnector");


	private final String m_resourceKey;

	BackshellTransferResult(@NotNull String resourceKey)
	{
		m_resourceKey = resourceKey;
	}

	/**
	 * Builds the localised user message for this outcome.
	 *
	 * @param deviceName      Name of the device that owns the device-side connector
	 * @param dscName         Name of the device-side connector (DSC)
	 * @param harnessConnName Name of the mated harness connector
	 * @return the formatted message string, or {@code null} when no message should be shown
	 */
	@Nullable public String buildMessage(@NotNull String deviceName, @NotNull String dscName,
			@NotNull String harnessConnName)
	{
		return ResourceMgr.getString(BackshellTransferResult.class, m_resourceKey, deviceName, dscName, harnessConnName);
	}
}
