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

import chs.caplets.logic.actions.shared.utils.TableUtils;
import chs.cof.logical.cable.ILogicObject;
import com.mentor.capital.javafx.table.SelectionPreferences;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author rmahato
 */
public abstract class BaseDetailPane extends TitledPane
{

	protected Table<DetailsTableInfo> table;
	protected String tableID;
	private final BooleanProperty m_tableClosingProperty = new SimpleBooleanProperty(false);

	protected BaseDetailPane(@NotNull String title, @NotNull String tableID)
	{
		this.tableID = tableID;
		setText(title);
	}

	public void updateContent(@Nullable ILogicObject selectedObject)
	{
		table = createTable(selectedObject);
		VBox vBox = new VBox(table);
		final int tableMinHeight = 120;
		vBox.setMinHeight(tableMinHeight);
		setContent(vBox);
	}

	@NotNull protected Table<DetailsTableInfo> createTable(@Nullable ILogicObject selectedObject)
	{
		Table<DetailsTableInfo> newTable = new Table<DetailsTableInfo>(tableID,
				new TableModel<DetailsTableInfo>(new TableDataStorage<>()),
				TableUtils::setAlphaNumComparator);
		DetailsTableInfo tableInfo = createTableInfo(selectedObject);
		newTable.addColumns(tableInfo.getColumns());
		newTable.addData(tableInfo.getTableData(selectedObject));
		newTable.setCellStateHandler(new DetailsTableCellStateHandler());
		newTable.sort(0);

		newTable.getTableView().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		setSelectionPrefrences(newTable);
		m_tableClosingProperty.unbind();
		m_tableClosingProperty.bind(newTable.tableClosingProperty());
		return newTable;
	}

	@Nullable public Table<DetailsTableInfo> getTable()
	{
		return table;
	}

	private void setSelectionPrefrences(Table<DetailsTableInfo> newTable)
	{
		SelectionPreferences selectionPreferences = new SelectionPreferences();
		selectionPreferences.setMultipleSelectionEnabled(false);
		newTable.setSelectionPreferences(selectionPreferences);
	}

	@NotNull protected abstract DetailsTableInfo createTableInfo(@Nullable ILogicObject selectedObject);

	@NotNull public BooleanProperty tableClosingProperty()
	{
		return m_tableClosingProperty;
	}
}