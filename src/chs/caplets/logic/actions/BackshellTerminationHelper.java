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
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedPinList;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for setting up backshell terminations for inline connectors.
 */
public class BackshellTerminationHelper
{

	private final IBackshellProvider mBackshellProvider;
	private final IBackshellTerminationProvider mBackshellTerminationProvider;

	public BackshellTerminationHelper()
	{
		BackshellFactory backshellFactory = new BackshellFactory();
		mBackshellProvider = backshellFactory;
		mBackshellTerminationProvider = backshellFactory;
	}

	/**
	 * Adds backshell termination for the given connector.
	 * This method:
	 * 1. Gets or creates backshell for the connector
	 * 2. Creates backshell termination
	 *
	 * @param connector the connector to create backshell termination for
	 * @return the created backshell termination
	 */

	@NotNull
	public IBackshellTermination addBackshellTermination(@NotNull IConnector connector)
	{
		ISharedPinList sharedConnector = connector.getSharedPinList();
		if (sharedConnector != null) {
			lockSharedPinList(sharedConnector);
		}
		ISharedBackshell sharedBackshell = null;
		try {
			IBackshell backshell = mBackshellProvider.getOrCreateBackshell(connector);

			sharedBackshell =mBackshellProvider.getSharedBackshell(backshell);

			if (sharedBackshell != null) {
				lockSharedPinList(sharedBackshell);
			}

			return mBackshellTerminationProvider.getOrCreateBackshellTermination(backshell);
		}
		finally {
			if (sharedBackshell != null) {
				unlockSharedPinList(sharedBackshell);
			}

			if (sharedConnector != null) {
				unlockSharedPinList(sharedConnector);
			}
		}
	}

	private void lockSharedPinList(@NotNull ISharedPinList sharedPinList)
	{
		sharedPinList.lock();
	}

	private void unlockSharedPinList(@NotNull ISharedPinList sharedPinList)
	{
		sharedPinList.save();
		sharedPinList.unlock();
	}
}