package chs.caplets.logic.actions;

import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Hierarchy info of the multicore
 */
public class MulticoreHierarchyInfo extends AbstractLogicObjectHierarchyInfo
{

	private boolean isShared;

	private MulticoreHierarchyInfo()
	{

	}

	@Nullable @Override protected IUIDObject getParentObject(@Nullable IUIDObject child)
	{
		return CreateMulticoreActionHelper.getParentObject(child);
	}

	@Nullable @Override protected Collection<? extends IUIDObject> getChildObjects(@Nullable IUIDObject parent)
	{
		return CreateMulticoreActionHelper.getChildrenObjects(parent);
	}

	public MulticoreHierarchyInfo(@Nullable IUIDObject self)
	{
		super(self);
		isShared = isObjectShared(self);
	}

	private boolean isObjectShared(@Nullable IUIDObject object)
	{
		if (object instanceof ILogicObject) {
			return ((ILogicObject) object).isShared();
		}
		return false;
	}

	@NotNull public static MulticoreHierarchyInfo construct(@Nullable IUIDObject parent, IUIDObject... children)
	{
		MulticoreHierarchyInfo info = new MulticoreHierarchyInfo();
		copy(info, parent, children);
		return info;
	}

	public boolean compareSharedState(@Nullable MulticoreHierarchyInfo other)
	{
		if (other == null) {
			return false;
		}
		return isShared == other.isShared;
	}
}
