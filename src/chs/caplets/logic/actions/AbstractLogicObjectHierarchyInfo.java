package chs.caplets.logic.actions;

import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractLogicObjectHierarchyInfo
{

	private IUID mParent;
	private Set<IUID> mChildren;

	protected AbstractLogicObjectHierarchyInfo()
	{
	}

	protected static void copy(AbstractLogicObjectHierarchyInfo info, @Nullable IUIDObject parent,
			IUIDObject... children)
	{
		info.mParent = parent != null ? parent.getUID() : null;
		info.mChildren = new HashSet<>(children.length);
		for (IUIDObject child : children) {
			info.mChildren.add(child.getUID());
		}
	}

	@Nullable protected abstract IUIDObject getParentObject(@Nullable IUIDObject child);

	@Nullable protected abstract Collection<? extends IUIDObject> getChildObjects(@Nullable IUIDObject parent);

	protected AbstractLogicObjectHierarchyInfo(@Nullable IUIDObject self)
	{
		mParent = getParent(self);
		mChildren = getChildren(self);
	}

	@Nullable public IUID getParent(@Nullable IUIDObject child)
	{
		IUIDObject obj = getParentObject(child);
		return obj != null ? obj.getUID() : null;
	}

	@Nullable public Set<IUID> getChildren(@Nullable IUIDObject parent)
	{
		Collection<? extends IUIDObject> childrenObjects = getChildObjects(parent);
		return childrenObjects != null ? UIDUtils.convertToUIDSet(childrenObjects) : null;
	}

	public boolean compare(AbstractLogicObjectHierarchyInfo other)
	{
		if (other == null) {
			return false;
		}
		if (mParent != other.mParent) {
			return false;
		}
		if (mChildren != null && other.mChildren != null && mChildren.equals(other.mChildren)) {
			return true;
		}
		return mChildren == null && other.mChildren == null;
	}
}
