/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.common.attr.IAttributeType;
import com.mentor.capital.javafx.table.Table;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Base interface for share table view operations
 * @param <R> Row type
 */
public interface IShareTableView<R extends IShareRow>
{
	/**
	 * Filters the table rows based on the given predicate.
	 *
	 * @param predicate the filter condition to apply to rows
	 */
	void filter(Predicate<R> predicate);

	/**
	 * Adds a column to display the specified attribute.
	 *
	 * @param attribute the attribute type to add as a column
	 */
	void addAttributeColumn(@NotNull IAttributeType attribute);

	/**
	 * Adds a column to display the design abstraction.
	 */
	void addDesignAbstractionColumn();

	/**
	 * Returns the underlying table pane.
	 *
	 * @return the table pane
	 */
	@NotNull Table<R> getTablePane();

	/**
	 * Applies any pending edits to the table data.
	 */
	void applyEdits();
}

