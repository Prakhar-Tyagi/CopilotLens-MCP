/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.CheckBoxMenuItemProvider;
import chs.caplets.logic.actions.shared.batchshare.ui.AbstractShareTableView;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareFilterObjectType;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Table view component for displaying and managing redundant shared objects in batch unshare operations.
 * <p>
 * This class extends {@link AbstractShareTableView} to provide a specialized table interface for viewing,
 * selecting, and managing objects that are candidates for unsharing. The table displays multiple columns
 * of information about each object and provides interactive controls for bulk selection and deselection.
 */
public class BatchUnshareTableView extends AbstractShareTableView<IBatchUnshareRow>
{

	public BatchUnshareTableView(@NotNull Collection<IBatchUnshareRow> data, @NotNull Label objectsSelectedLabel,
			@NotNull String tableId)
	{
		super(objectsSelectedLabel, createMandatoryColumnNames());
		BatchUnshareColumnsProvider columnsProvider =
				new BatchUnshareColumnsProvider(BatchShareFilterObjectType.getUnshareRedundantTypes());
		createTable(data, tableId, name -> columnsProvider.getColumnInfo(name), columnsProvider.getDefaultColumns());
	}

	@NotNull private static Set<String> createMandatoryColumnNames()
	{
		return Arrays.stream(BatchUnshareColumn.values()).map(BatchUnshareColumn::getName).collect(Collectors.toSet());
	}

	@Override protected void configureCellStateHandler()
	{
		table.setCellStateHandler(new BatchUnshareTableCellStateHandler());
	}

	@Override protected void configureMenuItems()
	{
		table.addMenuItemProvider(
				new CheckBoxMenuItemProvider<IBatchUnshareRow>(table, BatchUnshareColumn.SELECTION.getName(),
						CheckBoxMenuItemProvider.Action.SELECT_ALL)
				{
				});
		table.addMenuItemProvider(
				new CheckBoxMenuItemProvider<IBatchUnshareRow>(table, BatchUnshareColumn.SELECTION.getName(),
						CheckBoxMenuItemProvider.Action.CLEAR_ALL)
				{
				});
		table.addValueChangeListener(new BatchUnshareTableCellValueChangeListener(table, objectsSelectedLabel));
	}

	@Override protected void sortTable()
	{
		table.sort(findColumnIndex(BatchUnshareColumn.OBJECT_NAME.getName()));
	}
}
