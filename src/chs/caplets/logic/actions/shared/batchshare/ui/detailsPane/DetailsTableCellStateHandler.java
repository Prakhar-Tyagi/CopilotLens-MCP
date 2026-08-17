/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;

/**
 * @author rmahato
 */
public class DetailsTableCellStateHandler implements ITableCellStateHandler<DetailsTableInfo>
{


	@Override public boolean isEditable(@NotNull ITableCellValueProvider<DetailsTableInfo> cell)
	{
		return true;
	}

	@Override public void updateStyle(@NotNull ITableCell<DetailsTableInfo> tableCell, @NotNull Node styleable)
	{

		DetailsTableInfo tableInfo = tableCell.getRowItem();
		if(tableInfo != null){
			tableInfo.addToolTip(tableCell);
		}
	}

	@Override public boolean isValid(@NotNull ITableCell<DetailsTableInfo> cell)
	{
		return true;
	}
}
