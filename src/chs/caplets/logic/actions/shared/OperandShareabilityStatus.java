package chs.caplets.logic.actions.shared;

import chs.cof.logical.cable.ILogicObject;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.logic.LogicObjectUtils;
import org.jetbrains.annotations.Nullable;

public enum OperandShareabilityStatus
{
	NonShareable,
	WrongSelection,
	ErroneousObject,
	AssembliedObject,
	EmptyMulticore,
	PartialPlacedMulticore("OperandShareabilityStatus.PartialPlacedMulticore.message"),
	Unplaced("OperandShareabilityStatus.Unplaced.message"),
	Undetermined,
	Shareable;

	@Nullable private String mResourceMsgKey;

	OperandShareabilityStatus()
	{
		mResourceMsgKey = null;
	}

	OperandShareabilityStatus(@Nullable String resourceKey)
	{
		mResourceMsgKey = resourceKey;
	}

	@Nullable public String getMessage(@Nullable ILogicObject logicObject)
	{
		if (mResourceMsgKey == null) {
			return null;
		}
		final String type = logicObject != null ? LogicObjectUtils.getLogicObjectType(logicObject) : null;
		if (type != null) {
			return ResourceMgr
					.getString(OperandShareabilityStatus.class, mResourceMsgKey, StringUtils.toLowerCase(type));
		}
		return ResourceMgr.getString(OperandShareabilityStatus.class, mResourceMsgKey);
	}
}
