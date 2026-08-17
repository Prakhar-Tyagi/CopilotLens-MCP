/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class LockedSharedConnectorInBackshellTransfer implements IGuard
{

	@Nullable private ISharedLockableUpdateableObject m_sharedLockableUpdateableObject;

	@Override public void close()
	{
		if (m_sharedLockableUpdateableObject != null) {
			flushAndUnlockSharedObjects(m_sharedLockableUpdateableObject);
		}
		m_sharedLockableUpdateableObject = null;
	}

	/**
	 * Attempts to lock a shared object for exclusive editing.
	 * On success, records the locked object in the report for later cleanup.
	 * On failure, records a lock failure in the report.
	 */
	public boolean attemptLock(@NotNull IDeviceConnector sourceDeviceconnector,
			@NotNull IConnector targetPlugConnector, @NotNull ISharedObject sharedObject,
			@NotNull BackshellTransferReporter reporter)
	{
		ISharedLockableUpdateableObject lockableRoot = sharedObject.getLockableUpdateableRoot();
		if (lockableRoot == null) {
			return true;
		}

		if (lockableRoot.isLocked()) {
			// Already locked — nothing more to do
			return true;
		}

		boolean locked = lockableRoot.lock();
		if (!locked) {
			String ownerName =
					sourceDeviceconnector.getOwner() != null ? sourceDeviceconnector.getOwner().getName() : "";
			reporter.addMessage(ownerName, sourceDeviceconnector.getName(),
					targetPlugConnector.getName(),
					BackshellTransferResult.LockFailed);
			return false;
		}
		m_sharedLockableUpdateableObject = lockableRoot;

		return true;
	}

	/**
	 * Unlocks all shared objects that were locked during validation.
	 * Should be called on migration completion or failure rollback.
	 */
	private void flushAndUnlockSharedObjects(ISharedLockableUpdateableObject lockable)
	{

		if (lockable instanceof ISharedConnector sharedConnector) {
			flushAndUnlock(sharedConnector);
			sharedConnector.getMates().forEach(mate -> flushAndUnlock(mate));
		}
		else {
			flushAndUnlock(lockable);
		}
	}

	private static void flushAndUnlock(ISharedLockableUpdateableObject lockable)
	{
		if (lockable.isModified()) {
			lockable.flush();
		}
		lockable.unlock();
	}
}
