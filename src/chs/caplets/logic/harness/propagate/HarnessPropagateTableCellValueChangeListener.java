/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.common.ITableCellValueChangeListener;

/**
 * cell value change listener
 */
public class HarnessPropagateTableCellValueChangeListener
		implements ITableCellValueChangeListener<IHarnessPropagateStatusMessage>
{

	private Table<IHarnessPropagateStatusMessage> table;

	HarnessPropagateTableCellValueChangeListener(Table<IHarnessPropagateStatusMessage> table)
	{
		this.table = table;
	}

	@Override
	public void cellValueChanged(IHarnessPropagateStatusMessage sourceItem,
			ColumnInformation<IHarnessPropagateStatusMessage> sourceColumnInfo,
			Object oldValue, Object newValue)
	{
		if (HarnessPropagateColumn.Propagate.getName().equals(sourceColumnInfo.getName())) {
			table.refreshData(new HarnessPropagateSelectionChange(sourceItem, (Boolean) newValue).getRefreshedRows());
		}
	}
}
