/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for accessing logic object info
 */
public interface IObjectInfo extends Comparable<IObjectInfo>
{

	@NotNull String getUID();

	@NotNull String getDesignUID();

	@NotNull ShareableEntityTypeEnum getType();

	@Nullable String getAttributeValue(@NotNull String attributeXMLName);

	@Nullable String getPropertyValue(@NotNull String propertyName);

	@Nullable String getSharedUID();

	boolean isNonShared();

	@Nullable IConnectivityInfo getConnectivityInfo();

	/**
	 * Add custom/ extended attribute to an attribute list
	 *
	 * @param attributeName  custom attribute name
	 * @param attributeValue custom attribute value
	 */
	void addCustomAttribute(@NotNull String attributeName, @NotNull String attributeValue);
}
