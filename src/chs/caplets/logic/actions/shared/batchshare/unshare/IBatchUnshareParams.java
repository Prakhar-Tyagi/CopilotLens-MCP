/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Parameter interface providing data for the batch unshare dialog.
 * <p>
 * This interface defines the contract for supplying data to the {@link BatchUnshareDialog}
 * and for retrieving the user's selection.
 */
public interface IBatchUnshareParams
{
	/**
	 * Returns the collection of rows to be displayed in the batch unshare dialog.
	 *
	 * @return the collection of batch unshare rows
	 */
	@NotNull Collection<IBatchUnshareRow> getData();

	/**
	 * Retrieves the rows selected by the user from the dialog.
	 *
	 * @return the set of selected rows
	 */
	@NotNull Set<IBatchUnshareRow> retrieveRowsFromUserSelection();
}

