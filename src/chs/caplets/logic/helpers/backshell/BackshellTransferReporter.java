/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured report collecting all messages from a backshell termination migration.
 * Categorizes messages as success, warning (frozen/read-only), or failure (hard errors).
 */
public class BackshellTransferReporter
{
	public record Message(String device, String deviceConnector, String targetConnector, BackshellTransferResult backshellTransferResult) {}
	private final List<Message> messages = new ArrayList<>();
	private boolean sharedConnectorLockNeeded;

	public void addMessage(@NotNull String deviceName, String deviceConnectorName, String targetConnectorName,
			@NotNull BackshellTransferResult backshellTransferResult)
	{
		messages.add(new Message(deviceName, deviceConnectorName, targetConnectorName, backshellTransferResult));
	}

	public void clear()
	{
		messages.clear();
	}

	@NotNull public List<Message> getMessages()
	{
		return messages;
	}

	public boolean isSharedConnectorLockNeeded()
	{
		return sharedConnectorLockNeeded;
	}

	public void setSharedConnectorLockNeeded(boolean sharedConnectorLockNeeded)
	{
		this.sharedConnectorLockNeeded = sharedConnectorLockNeeded;
	}
}

