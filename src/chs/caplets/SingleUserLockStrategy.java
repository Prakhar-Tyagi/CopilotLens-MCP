/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */

package chs.caplets;

import chs.cog.ICOGLockable;
import chs.cog.ICOGManagedLockable;
import chs.common.IUIDObject;

public class SingleUserLockStrategy implements IDesignLockStrategy
{

	@Override public boolean acquireLock(IUIDObject uidObject)
	{
		if (uidObject instanceof ICOGLockable) {
			return ((ICOGLockable) uidObject).lock();
		}

		if (uidObject instanceof ICOGManagedLockable) {
			ICOGLockable parent = ((ICOGManagedLockable) uidObject).getRootLockableParent();
			if (parent == null) {
				return false;
			}

			boolean status = parent.lock();
			if (status) {
				status = ((ICOGManagedLockable) uidObject).lock();
			}
			return status;
		}
		return false;
	}
}
