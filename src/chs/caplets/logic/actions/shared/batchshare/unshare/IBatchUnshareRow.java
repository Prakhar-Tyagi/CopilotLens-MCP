/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.common.IUID;
import chs.utilities.AlphaNumComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface representing a row of data in the batch unshare table dialog.
 * <p>
 * This interface extends {@link IShareRow} to provide specialized data access for redundant shared objects
 * that are candidates for unsharing in batch operations. Each row represents a redundant
 * shared object that can be converted back to a non-shared instance.
 */
public interface IBatchUnshareRow extends IShareRow, Comparable<IBatchUnshareRow>
{

	/**
	 * Gets the revision of the redundant shared object.
	 *
	 * @return the revision, or null if not available
	 */
	@Nullable String getRevision();

	/**
	 * Returns the value of the specified attribute.
	 *
	 * @param attributeName the attribute name
	 * @return the attribute value, or null if not found
	 */
	@Nullable String getAttributeValue(@NotNull String attributeName);

	/**
	 * Returns the unique identifier of the shared object.
	 *
	 * @return the shared object UID
	 */
	@NotNull IUID getSharedObjectUID();

	/**
	 * Gets the comma-separated build list names containing the design of the redundant shared object.
	 *
	 * @return comma-separated build list names, or null if not available
	 */
	@Nullable String getBuildList();

	/**
	 * Compares this row to another for sorting purposes.
	 * Comparison is performed first by object type name, then by name, then by design name.
	 *
	 * @param o the row to compare to
	 * @return a negative integer, zero, or a positive integer as this row is less than, equal to, or greater than the specified row
	 */
	@Override default int compareTo(@NotNull IBatchUnshareRow o)
	{
		int result = AlphaNumComparator.compare(getObjectType().name(), o.getObjectType().name(), false);
		if (result == 0) {
			result = AlphaNumComparator.compare(getName(), o.getName(), false);
		}
		if (result == 0) {
			result = AlphaNumComparator.compare(getDesignName(), o.getDesignName(), false);
		}
		return result;
	}
}

