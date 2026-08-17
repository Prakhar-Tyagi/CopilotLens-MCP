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

import chs.caplets.logic.actions.shared.CheckBoxMenuItemProvider;
import chs.caplets.logic.actions.shared.batchshare.IPropertyTableView;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class BatchShareTableView extends AbstractShareTableView<IBatchShareRow> implements IPropertyTableView
{

	public static final String BATCH_SHARE_TABLE_ID = "batch_share_table";

	public BatchShareTableView(@NotNull Collection<IBatchShareRow> data, @NotNull Label objectsSelectedLabel,
			@NotNull String tableId, @NotNull IBatchShareFilterObjectType[] filterObjectTypes)
	{
		super(objectsSelectedLabel, createMandatoryColumnNames());
		BatchShareColumnsProvider batchShareColumnsProvider = new BatchShareColumnsProvider(filterObjectTypes);
		createTable(data, tableId,
				name -> batchShareColumnsProvider.getColumnInfo(name),
				batchShareColumnsProvider.getDefaultColumns());
	}

	@Override public void addPropertyColumn(@NotNull String propertyName)
	{
		addColumn(propertyName, PROPERTY_PREFIX + propertyName, (t) -> t.getPropertyValue(propertyName));
	}

	@NotNull private static Set<String> createMandatoryColumnNames()
	{
		return Arrays.stream(BatchShareColumn.values()).map(BatchShareColumn::getName).collect(Collectors.toSet());
	}

	@Override protected void configureCellStateHandler()
	{
		table.setCellStateHandler(new BatchShareTableCellStateHandler());
	}

	@Override protected void configureMenuItems()
	{
		table.addMenuItemProvider(new BatchShareCheckBoxMenuItemProvider(table, BatchShareColumn.SELECTION.getName(),
				CheckBoxMenuItemProvider.Action.SELECT_ALL));
		table.addMenuItemProvider(new BatchShareCheckBoxMenuItemProvider(table, BatchShareColumn.SELECTION.getName(),
				CheckBoxMenuItemProvider.Action.CLEAR_ALL));
		table.addValueChangeListener(new BatchShareTableCellValueChangeListener(table, objectsSelectedLabel));
	}

	@Override protected void sortTable()
	{
		table.sort(findColumnIndex(BatchShareColumn.MATCHED_BY.getName()));
	}
}