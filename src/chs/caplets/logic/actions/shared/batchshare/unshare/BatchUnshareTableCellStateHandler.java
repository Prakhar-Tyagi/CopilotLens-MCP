/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Handles cell state for batch unshare table
 */
public class BatchUnshareTableCellStateHandler implements ITableCellStateHandler<IBatchUnshareRow>
{

	@Override public boolean isEditable(@NotNull ITableCellValueProvider<IBatchUnshareRow> cell)
	{
		return cell.getColumn() != null &&
				BatchUnshareColumn.SELECTION.getName().equals(cell.getColumn().getName());
	}

	@Override public void updateStyle(@NotNull ITableCell<IBatchUnshareRow> tableCell, @NotNull Node styleable)
	{
	}

	@Override public boolean isValid(@NotNull ITableCell<IBatchUnshareRow> cell)
	{
		return true;
	}
}

