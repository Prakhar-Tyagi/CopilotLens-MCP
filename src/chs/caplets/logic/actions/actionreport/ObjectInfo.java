/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.cof.COFTypeEnum;
import chs.common.INamedObject;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * object info - for mapping objects in comparison
 */
public class ObjectInfo implements IObjectInfo
{

	@NotNull private final String mName;
	@NotNull private final COFTypeEnum mType;
	@Nullable private final String mUID;

	private ObjectInfo(@NotNull String name, @NotNull COFTypeEnum type, @Nullable String uid)
	{
		mName = name;
		mType = type;
		mUID = uid;
	}

	@NotNull public static IObjectInfo getObjectInfo(@NotNull INamedObject object)
	{
		IUIDObject uidObject = CommonUtils.cast(object, IUIDObject.class);
		COFTypeEnum typeEnum = COFTypeEnum.Unknown;
		String uid = null;
		if (uidObject != null) {
			typeEnum = COFTypeEnum.from_object(uidObject);
			uid = uidObject.getUID().getString();
		}
		if(object instanceof IPinProxy){
			typeEnum = COFTypeEnum.Pin;
		}
		return new ObjectInfo(object.getName(), typeEnum, uid);
	}

	@NotNull public static IObjectInfo getObjectInfo(@NotNull String name, @NotNull COFTypeEnum type, @Nullable String uid)
	{
		return new ObjectInfo(name,type,uid);
	}

	@NotNull @Override public String getName()
	{
		return mName;
	}

	@NotNull @Override public COFTypeEnum getObjectType()
	{
		return mType;
	}

	@Nullable @Override public String getUID()
	{
		return mUID;
	}

	@Override public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ObjectInfo objectInfo = (ObjectInfo) obj;
		return mName.equals(objectInfo.mName) &&
				mType == objectInfo.mType &&
				Objects.equals(mUID, objectInfo.mUID);
	}

	@Override public int hashCode()
	{
		return Objects.hash(mName, mType, mUID);
	}
}
