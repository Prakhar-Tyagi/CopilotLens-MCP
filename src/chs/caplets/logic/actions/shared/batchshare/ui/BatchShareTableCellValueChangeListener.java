/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.common.ITableCellValueChangeListener;
import javafx.scene.control.Label;

/**
 * cell value change listener
 */
public class BatchShareTableCellValueChangeListener implements ITableCellValueChangeListener<IBatchShareRow>
{

	private Table<IBatchShareRow> table;
	private Label objectsSelectedLabel;

	BatchShareTableCellValueChangeListener(Table<IBatchShareRow> table, Label objectsSelectedLabel)
	{
		this.table = table;
		this.objectsSelectedLabel = objectsSelectedLabel;
	}

	@Override
	public void cellValueChanged(IBatchShareRow sourceItem, ColumnInformation<IBatchShareRow> sourceColumnInfo,
			Object oldValue, Object newValue)
	{
		if (BatchShareColumn.SELECTION.getName().equals(sourceColumnInfo.getName())) {
			table.refreshData(new SelectionChange(sourceItem, (Boolean) newValue).refreshRows());
			int noOfObjectsSelected = table.getData().filtered(row -> row.isSelected()).size();
			objectsSelectedLabel.setText(ResourceMgr.getString(BatchShareDialog.class, "BatchShareDialog.objectsSelected.text", noOfObjectsSelected));
		}
		else if (BatchShareColumn.ACTION.getName().equals(sourceColumnInfo.getName())) {
			table.refreshData(new ActionChange(sourceItem, (Action) newValue).refreshRows());
		}
	}
}
