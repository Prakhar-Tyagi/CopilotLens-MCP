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
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.shared.ISharedBackshell;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for creating and managing backshell terminations.
 */
public interface IBackshellTerminationProvider
{

	/**
	 * Creates or retrieves an existing backshell termination for the given backshell.
	 * Handles both shared and non-shared connectors.
	 *
	 * @param backshell the backshell to add termination to
	 * @return existing or newly created backshell termination, never null
	 */
	@NotNull
	IBackshellTermination getOrCreateBackshellTermination(@NotNull IBackshell backshell);

	/**
	 * Creates a backshell termination. Handles both shared and non-shared cases.
	 *
	 * @param backshell the backshell to add termination to
	 * @return newly created backshell termination
	 */
	@NotNull
	IBackshellTermination createBackshellTermination(@NotNull IBackshell backshell);

	/**
	 * Creates a shared backshell termination.
	 *
	 * @param sharedBackshell the shared backshell
	 * @param backshell       the instance backshell
	 * @return newly created instance backshell termination linked to shared termination
	 */
	@NotNull
	IBackshellTermination createSharedBackshellTermination(@NotNull ISharedBackshell sharedBackshell,
			@NotNull IBackshell backshell);

	/**
	 * Creates a non-shared backshell termination.
	 *
	 * @param backshell the backshell to add termination to
	 * @return newly created backshell termination
	 */
	@NotNull
	IBackshellTermination createNonSharedBackshellTermination(@NotNull IBackshell backshell);
}
