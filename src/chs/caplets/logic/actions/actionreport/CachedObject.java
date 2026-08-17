/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.cof.COFTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * snapshot object
 */
public class CachedObject implements ICachedObject
{

	private Map<String, String> mAttributes = new HashMap<>();
	private Map<String, String> mProperties = new HashMap<>();
	private IObjectInfo objectInfo;
	@Nullable private String designUID;
	private boolean isSharedObject = false;
	@Nullable private ICachedObject mParent;
	private Map<String, ICachedObject> mChildren;

	public CachedObject(@Nullable ICachedObject parent, @NotNull String name, @NotNull String uid,
			@Nullable String designUID, @NotNull COFTypeEnum objectType)
	{
		this.designUID = designUID;
		mChildren = new HashMap<>();
		mParent = parent;
		objectInfo = ObjectInfo.getObjectInfo(name, objectType, uid);
	}

	@NotNull @Override public Map<String, String> getAttributes()
	{
		return mAttributes;
	}

	@NotNull @Override public Map<String, String> getProperties()
	{
		return mProperties;
	}

	@Nullable @Override public String getDesignUID()
	{
		return designUID;
	}

	@Override public boolean isSharedObject()
	{
		return isSharedObject;
	}

	@Override public void setIsSharedObject(boolean value)
	{
		isSharedObject = value;
	}

	@NotNull @Override public Collection<ICachedObject> getChildren()
	{
		List<ICachedObject> children = new ArrayList<>(mChildren.values());
		Collections.sort(children, new Comparator<ICachedObject>()
		{
			@Override public int compare(ICachedObject o1, ICachedObject o2)
			{
				return o1.getObjectInfo().getName().compareTo(o2.getObjectInfo().getName());
			}
		});
		return children;
	}

	@Nullable @Override public ICachedObject getParent()
	{
		return mParent;
	}

	@Override public void addChild(ICachedObject child)
	{
		mChildren.put(child.getObjectInfo().getName(), child);
	}

	@Override @NotNull public String getObjectTypeDisplayName()
	{
		return objectInfo.getObjectType().toString();
	}

	@NotNull @Override public IObjectInfo getObjectInfo()
	{
		return objectInfo;
	}
}
