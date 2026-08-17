/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.common.ITableCellValueChangeListener;
import javafx.scene.control.Label;

/**
 * Cell value change listener for batch unshare table
 */
public class BatchUnshareTableCellValueChangeListener implements ITableCellValueChangeListener<IBatchUnshareRow>
{

	private Table<IBatchUnshareRow> table;
	private Label objectsSelectedLabel;

	BatchUnshareTableCellValueChangeListener(Table<IBatchUnshareRow> table, Label objectsSelectedLabel)
	{
		this.table = table;
		this.objectsSelectedLabel = objectsSelectedLabel;
	}

	@Override
	public void cellValueChanged(IBatchUnshareRow sourceItem, ColumnInformation<IBatchUnshareRow> sourceColumnInfo,
			Object oldValue, Object newValue)
	{
		if (BatchUnshareColumn.SELECTION.getName().equals(sourceColumnInfo.getName())) {
			sourceItem.setSelected((Boolean) newValue);
			int noOfObjectsSelected = table.getData().filtered(IBatchUnshareRow::isSelected).size();
			objectsSelectedLabel.setText(
					ResourceMgr.getString(BatchUnshareDialog.class, "BatchUnshareDialog.objectsSelected.text",
							noOfObjectsSelected));
		}
	}
}

