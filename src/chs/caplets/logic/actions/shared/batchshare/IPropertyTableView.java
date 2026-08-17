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

/**
 * Interface to determine if table view supports adding property columns.
 */
public interface IPropertyTableView
{
	/**
	 * Adds a column to display the specified property.
	 *
	 * @param propertyName the name of the property to add as a column
	 */
	void addPropertyColumn(@NotNull String propertyName);
}