/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManageConnectorReplaceAllCollector implements AutoCloseable
{

	protected Collection<ManageConnectorConnectionsInfo> items = new ArrayList<>();

	protected Table<ManageConnectorConnectionsInfo> table;

	public static ManageConnectorReplaceAllCollector replaceAllCollector = null;

	public ManageConnectorReplaceAllCollector()
	{
		replaceAllCollector = this;
	}

	public void addItems(Collection<ManageConnectorConnectionsInfo> items, Table<ManageConnectorConnectionsInfo> table)
	{
		this.table = table;
		this.items = items;
	}

	@Override public void close()
	{

		List<CellSelection<ManageConnectorConnectionsInfo>> selections = new ArrayList<>();

		if (table != null && items != null && !items.isEmpty()) {
			for (ManageConnectorConnectionsInfo anItem : items) {
				table.replaceAll(anItem, anItem);
				selections.add(new CellSelection<ManageConnectorConnectionsInfo>(anItem, -1, 1, null, null, null));
				selections.add(new CellSelection<ManageConnectorConnectionsInfo>(anItem, -1, 2, null, null, null));
			}
			table.select(new Selection<>(selections));
		}
		replaceAllCollector = null;
	}
}
