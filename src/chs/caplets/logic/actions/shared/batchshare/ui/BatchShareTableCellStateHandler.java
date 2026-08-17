/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;

/**
 * Handler to control the cell state based on conditions on the row element
 */
public class BatchShareTableCellStateHandler implements ITableCellStateHandler<IBatchShareRow>
{

	@Override public boolean isEditable(@NotNull ITableCellValueProvider<IBatchShareRow> cell)
	{
		return new CellInfo(cell.getRowItem(), cell.getColumn()).isEditable();
	}

	@Override public void updateStyle(@NotNull ITableCell<IBatchShareRow> tableCell, @NotNull Node styleable)
	{
		if (tableCell.getRowItem() == null) {
			return;
		}
		applySelectColumnCSS(tableCell);
		computeTooltip(tableCell);
	}

	private void computeTooltip(@NotNull ITableCell<IBatchShareRow> tableCell)
	{
		Labeled cell = CommonUtils.cast(tableCell, Labeled.class);
		if (cell != null) {
			cell.setTooltip(null);
			CellInfo cellInfo = new CellInfo(tableCell.getRowItem(), tableCell.getColumn());
			String tooltipMessage = cellInfo.getTooltip();
			if (tableCell.getRowItem() != null && !cellInfo.isEditable() && !StringUtils.isBlank(tooltipMessage)) {
				cell.tooltipProperty().set(new Tooltip(tooltipMessage));
			}
		}
	}

	private void applySelectColumnCSS(@NotNull ITableCell<IBatchShareRow> tableCell)
	{
		((Styleable) tableCell).getStyleClass().remove("select-table-cell");
		ColumnInformation<IBatchShareRow> column = tableCell.getColumn();
		if (column == null) {
			return;
		}
		if (BatchShareColumn.SELECTION.getName().equals(column.getName())) {
			((Styleable) tableCell).getStyleClass().add("select-table-cell");
		}
	}

	@Override public boolean isValid(@NotNull ITableCell<IBatchShareRow> cell)
	{
		return true;
	}
}
