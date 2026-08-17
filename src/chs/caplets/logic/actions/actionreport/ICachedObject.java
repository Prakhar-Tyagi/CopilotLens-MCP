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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * snapshot object
 */
public interface ICachedObject
{

	@NotNull Map<String, String> getAttributes();

	@NotNull Map<String, String> getProperties();

	@Nullable String getDesignUID();

	boolean isSharedObject();

	void setIsSharedObject(boolean value);

	@NotNull Collection<ICachedObject> getChildren();

	@Nullable ICachedObject getParent();

	void addChild(ICachedObject child);

	@NotNull String getObjectTypeDisplayName();

	@NotNull IObjectInfo getObjectInfo();
}
