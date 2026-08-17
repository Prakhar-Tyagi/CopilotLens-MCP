package chs.caplets;

import chs.cog.ICOGManagedLockable;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUIDObject;

public class MultiUserLockStrategy implements IDesignLockStrategy
{

	@Override public boolean acquireLock(IUIDObject uidObject)
	{
		if (uidObject instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
			return ((IPrivilegedCOGManagedLockableChildrenContainer) uidObject).acquireWeakLock();
		}
		if (uidObject instanceof ICOGManagedLockable) {
			return ((ICOGManagedLockable) uidObject).lock();
		}
		return false;
	}
}
