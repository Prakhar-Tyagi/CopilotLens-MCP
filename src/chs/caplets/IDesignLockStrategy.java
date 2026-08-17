package chs.caplets;

import chs.cof.logical.ILogicDesign;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.cog.ICOGLockable;
import chs.cog.ICOGManagedLockable;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

public interface IDesignLockStrategy
{

	@NotNull static IDesignLockStrategy getLockStrategy(@NotNull ILogicDesign design)
	{
		if (LogicConcurrencyHelper.isLogicInSingleUserMode(design.getProject())  || !design.isMultiUserSupported()) {
			return new SingleUserLockStrategy();
		}
		return new MultiUserLockStrategy();
	}

	boolean acquireLock(IUIDObject uidObject);

	static boolean releaseLock(IUIDObject uidObject)
	{
		if (uidObject instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
			IPrivilegedCOGManagedLockableChildrenContainer design =
					(IPrivilegedCOGManagedLockableChildrenContainer) uidObject;
			if (design.isWeakLocked()) {
				return design.releaseWeakLock();
			}
			if (design.isLocked()) {
				return design.unlock();
			}
		}
		else if (uidObject instanceof ICOGManagedLockable) {
			return ((ICOGManagedLockable) uidObject).unlock();
		}
		else if (uidObject instanceof ICOGLockable) {
			return ((ICOGLockable) uidObject).unlock();
		}
		return false;
	}

	static boolean isLocked(IUIDObject uidObject)
	{
		if (uidObject instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
			IPrivilegedCOGManagedLockableChildrenContainer design =
					(IPrivilegedCOGManagedLockableChildrenContainer) uidObject;
			return design.isLocked() || design.isWeakLocked();
		}
		if (uidObject instanceof ICOGManagedLockable) {
			return ((ICOGManagedLockable) uidObject).isLocked();
		}
		if (uidObject instanceof ICOGLockable) {
			return ((ICOGLockable) uidObject).isLocked();
		}
		return false;
	}
}