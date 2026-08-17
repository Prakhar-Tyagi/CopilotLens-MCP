/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.common.IUID;
import chs.common.attr.IAttributeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base interface representing a row of data in batch share/unshare table operations.
 * <p>
 * This interface defines the contract for data objects displayed as rows in share and unshare
 * table views. Each row represents a shareable object or an unshareable redundant object with associated data,
 * selection state, and access to properties and attributes.
 */
public interface IShareRow
{

	/**
	 * Returns the name of the underlying object.
	 *
	 * @return the object name
	 */
	@NotNull String getName();

	/**
	 * Checks whether this row is currently selected.
	 *
	 * @return true if selected, false otherwise
	 */
	boolean isSelected();

	/**
	 * Sets the selection state of this row.
	 *
	 * @param selected true to select, false to deselect
	 */
	void setSelected(boolean selected);

	/**
	 * Returns the name of the design containing this object.
	 *
	 * @return the design name
	 */
	@NotNull String getDesignName();

	/**
	 * Returns the unique identifier of the object.
	 *
	 * @return the object UID
	 */
	@NotNull IUID getObjectUID();

	/**
	 * Returns the unique identifier of the design containing this object.
	 *
	 * @return the design UID
	 */
	@NotNull IUID getDesignUID();

	/**
	 * Checks whether this row is valid for sharing/unsharing operations.
	 *
	 * @return true if valid, false otherwise
	 */
	boolean isValid();

	/**
	 * Returns the value of the specified attribute.
	 *
	 * @param attribute the attribute type to retrieve
	 * @return the attribute value, or null if not found
	 */
	@Nullable String getAttributeValue(@NotNull IAttributeType attribute);

	/**
	 * Returns the type of the shareable object.
	 *
	 * @return the object type
	 */
	@NotNull ShareableEntityTypeEnum getObjectType();

	/**
	 * Returns the design abstraction of the underlying object
	 *
	 * @return the design abstraction
	 */
	@NotNull String getDesignAbstraction();
}

