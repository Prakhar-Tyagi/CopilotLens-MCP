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

import chs.cof.logical.cable.ILogicObject;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.menu.DefaultMenuItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author rmahato
 */
public abstract class DetailsTableInfo
{

	protected Map<String, String> coloumValueMap;

	@NotNull public Map<String, String> getColoumValueMap()
	{
		return coloumValueMap;
	}

	@NotNull abstract Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject);

	@NotNull public Collection<ColumnInformation<DetailsTableInfo>> getColumns()
	{
		Collection<ColumnInformation<DetailsTableInfo>> columns = new ArrayList<>();
		for (String key : coloumValueMap.keySet()) {
			ColumnInformation<DetailsTableInfo> column =
					new ColumnInformation<DetailsTableInfo>(key, key, object -> object.getColumnValue(key))
					{
						@Override public boolean displayDefaultMenuItem(DefaultMenuItem item)
						{
							return item != DefaultMenuItem.Select && item != DefaultMenuItem.Hide;
						}
					};
			columns.add(column);
		}
		return columns;
	}

	@NotNull public String getColumnValue(String columnName)
	{
		return coloumValueMap.get(columnName);
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof DetailsTableInfo) {
			DetailsTableInfo objInfo = (DetailsTableInfo) obj;
			Map<String, String> keyValueMap = getColoumValueMap();
			for (String key : keyValueMap.keySet()) {
				if (!getColumnValue(key).equals(objInfo.getColumnValue(key))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override public int hashCode()
	{
		StringBuilder result = new StringBuilder();
		for (String key : getColoumValueMap().keySet()) {
			result.append(key);
			result.append(":");
			result.append(getColumnValue(key));
		}
		return result.toString().hashCode();
	}

	public void addToolTip(ITableCell<DetailsTableInfo> tableCell)
	{
	}
}
