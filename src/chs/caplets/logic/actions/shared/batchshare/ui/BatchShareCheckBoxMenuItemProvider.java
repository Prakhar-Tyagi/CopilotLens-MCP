/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.CheckBoxMenuItemProvider;
import com.mentor.capital.javafx.table.Table;

/**
 * Menu item for selecting and de selecting checkbox for all rows
 */
public class BatchShareCheckBoxMenuItemProvider extends CheckBoxMenuItemProvider<IBatchShareRow>
{

	BatchShareCheckBoxMenuItemProvider(Table<IBatchShareRow> table, String columnName, Action action)
	{
		super(table, columnName, action);
	}

	@Override protected void setColumn(Boolean value)
	{
		table.performBulkEdit(() -> {
			table.filteredData().forEach(row -> {
				new SelectionChange(row, value).refreshRows();
			});
			table.refreshAllData();
		});
	}
}
