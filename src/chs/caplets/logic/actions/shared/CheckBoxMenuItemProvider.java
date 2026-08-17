/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.strategy.ITableColumnMenuItemProvider;
import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Menu item for selecting and de selecting checkbox for all rows
 */
public abstract class CheckBoxMenuItemProvider<T> implements ITableColumnMenuItemProvider<T>
{

	public enum Action
	{
		SELECT_ALL(true, "SelectAllAction",
				ResourceMgr.getString(CheckBoxMenuItemProvider.class, "CheckBoxMenuItemProvider.menuitem.selectall")),
		CLEAR_ALL(false, "ClearAllAction",
				ResourceMgr.getString(CheckBoxMenuItemProvider.class, "CheckBoxMenuItemProvider.menuitem.clearall"));

		@NotNull public String getMenuItemDisplayName()
		{
			return menuItemDisplayName;
		}

		private boolean state;
		@NotNull private String m_id;
		private String menuItemDisplayName;

		Action(boolean state, @NotNull String id, String menuItemDisplayName)
		{
			this.state = state;
			m_id = id;
			this.menuItemDisplayName = menuItemDisplayName;
		}

		public boolean getState()
		{
			return state;
		}

		@NotNull public String getId()
		{
			return m_id;
		}
	}

	protected Table<T> table;
	private String columnName;
	private Action action;

	protected CheckBoxMenuItemProvider(Table<T> table, String columnName, Action action)
	{
		this.table = table;
		this.columnName = columnName;
		this.action = action;
	}

	@NotNull @Override
	public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
	{
		return columnName.equals(columnInformation.getName()) ? Collections.singletonList(getMenuItem()) :
				Collections.emptyList();
	}

	@NotNull private MenuItem getMenuItem()
	{
		MenuItem menuItem = new MenuItem(action.getMenuItemDisplayName());
		menuItem.setId(action.getId());
		menuItem.setOnAction(event -> setColumn(action.getState()));
		return menuItem;
	}

	protected void setColumn(Boolean value)
	{
		int colIndex = findColumnIndex(columnName);
		if (colIndex == -1) {
			return;
		}
		for (int row = 0; row < table.filteredData().count(); row++) {
			table.setValue(row, colIndex, value, false);
		}
	}

	private int findColumnIndex(String name)
	{
		List<String> colNames = table.columns().map(colInfo -> colInfo.getName()).collect(Collectors.toList());
		return colNames.indexOf(name);
	}
}
