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
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

/**
 * Class to manage cell states based on batch share conditions
 */
public class HarnessPropagateCellInfo
{

	@Nullable private ColumnInformation<IHarnessPropagateStatusMessage> column;
	private final IHarnessPropagateStatusMessage rowItem;
	private StringJoiner tooltip = new StringJoiner("\n");

	public HarnessPropagateCellInfo(@Nullable IHarnessPropagateStatusMessage rowItem,
			@Nullable ColumnInformation<IHarnessPropagateStatusMessage> column)
	{
		this.rowItem = rowItem;
		this.column = column;
		if (rowItem != null && column != null && "Message".equals(column.getName())) {
			tooltip.add(rowItem.getMessage());
		}
		if (rowItem != null && column != null && "Design".equals(column.getName())) {
			tooltip.add(rowItem.getDesignName());
		}
	}

	public boolean isEditable()
	{
		return rowItem != null && rowItem.isEditable();
	}

	@Nullable public String getTooltip()
	{
		return tooltip.toString();
	}
}
