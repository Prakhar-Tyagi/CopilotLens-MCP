/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface to determine if row supports property retrieval.
 */
public interface IPropertyRow
{
	/**
	 * Returns the value of the specified property.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @return the property value, or null if not found
	 */
	@Nullable String getPropertyValue(@NotNull String propertyName);
}
