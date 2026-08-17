package chs.caplets.logic.actions;

import chs.cof.logical.cable.IAssembly;
import chs.common.IAssembledObject;
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AssemblyHierarchyInfo extends AbstractLogicObjectHierarchyInfo
{

	private AssemblyHierarchyInfo()
	{

	}

	@Nullable @Override protected IUIDObject getParentObject(@Nullable IUIDObject child)
	{
		IAssembledObject assembledObject = CommonUtils.cast(child, IAssembledObject.class);
		return assembledObject != null ? assembledObject.getAssembly() : null;
	}

	@Nullable @Override protected Collection<? extends IUIDObject> getChildObjects(@Nullable IUIDObject parent)
	{
		IAssembly assemby = CommonUtils.cast(parent, IAssembly.class);
		return assemby != null ? assemby.getElements() : null;
	}

	public AssemblyHierarchyInfo(@Nullable IAssembledObject self)
	{
		super(self);
	}

	public static AssemblyHierarchyInfo construct(@Nullable IUIDObject parent, IUIDObject... children)
	{
		AssemblyHierarchyInfo info = new AssemblyHierarchyInfo();
		copy(info, parent, children);
		return info;
	}
}
