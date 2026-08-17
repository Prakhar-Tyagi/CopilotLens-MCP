/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for creating and managing backshells.
 */
public interface IBackshellProvider
{

	/**
	 * Creates or retrieves an existing backshell for the given connector.
	 * Handles both shared and non-shared connectors.
	 *
	 * @param connector the connector to get/create backshell for
	 * @return existing or newly created backshell, never null
	 */
	@NotNull
	IBackshell getOrCreateBackshell(@NotNull IConnector connector);

	/**
	 * Creates a backshell with shared configuration.
	 * Creates both the shared backshell (if it doesn't exist) and the instance backshell.
	 *
	 * @param sharedConnector the shared connector
	 * @param connector       the instance connector
	 * @return newly created instance backshell linked to shared backshell
	 */
	@NotNull
	IBackshell createSharedBackshell(@NotNull ISharedConnector sharedConnector,
			@NotNull IConnector connector);

	/**
	 * Creates a non-shared backshell for the given connector.
	 *
	 * @param connector the connector to create backshell for
	 * @return newly created backshell
	 */
	@NotNull
	IBackshell createNonSharedBackshell(@NotNull IConnector connector);

	/**
	 * Gets the shared backshell from a backshell instance, if it exists.
	 *
	 * @param backshell the backshell instance
	 * @return the shared backshell, or null if the backshell is not shared
	 */
	@Nullable
	ISharedBackshell getSharedBackshell(@NotNull IBackshell backshell);
}
