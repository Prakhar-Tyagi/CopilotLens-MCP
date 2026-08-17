/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.caplets.logic.actions.shared.utils.TableUtils;
import chs.common.attr.IAttributeType;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.ui.RemoveMenuItemProvider;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.SelectionPreferences;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract base class for Table views used in batch share and unshare operations.
 * <p>
 * This class provides the base implementation for displaying and managing tabular data
 * in share/unshare dialogs. It encapsulates common functionality for creating, configuring, and
 * interacting with Table instances that display rows of {@link IShareRow} data.
 *
 * @param <T> the row data type, must extend {@link IShareRow}
 */
public abstract class AbstractShareTableView<T extends IShareRow> implements IShareTableView<T>
{

	public static final String PROPERTY_PREFIX = "Property:";
	public static final String ATTRIBUTE_PREFIX = "Attribute:";

	protected Table<T> table;
	protected TableDataStorage<T> dataStorage;
	protected Set<String> mandatoryColumnNames;
	protected Label objectsSelectedLabel;

	protected AbstractShareTableView(@NotNull Label objectsSelectedLabel, @NotNull Set<String> mandatoryColumnNames)
	{
		this.objectsSelectedLabel = objectsSelectedLabel;
		this.mandatoryColumnNames = mandatoryColumnNames;
	}

	protected void createTable(@NotNull Collection<T> data, @NotNull String tableId,
			@NotNull Function<String, ColumnInformation<T>> columnInfoProvider,
			@NotNull List<ColumnInformation<T>> defaultColumns)
	{
		dataStorage = new TableDataStorage<>();
		table = new Table<T>(tableId, new TableModel<T>(dataStorage, columnInfoProvider),
				TableUtils::setAlphaNumComparator);
		addMandatoryColumns(defaultColumns);
		setSelectionParameters();
		setColumnsResizePolicy();
		addData(data.stream().sorted().collect(Collectors.toList()));
		table.setCommitOnFocusLoss(true);
		configureCellStateHandler();
		configureMenuItems();
		sortTable();
	}

	protected int findColumnIndex(String name)
	{
		List<String> colNames = table.columns().map(ColumnInformation::getName).collect(Collectors.toList());
		return colNames.indexOf(name);
	}

	protected void addData(Collection<T> data)
	{
		table.addData(data);
	}

	protected void setColumnsResizePolicy()
	{
		table.getTableView().setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
	}

	protected void addMandatoryColumns(List<ColumnInformation<T>> defaultColumns)
	{
		table.addColumns(defaultColumns).addMenuItemProvider(new RemoveMenuItemProvider<T>(table,
				columnInformation -> mandatoryColumnNames.contains(columnInformation.getName())));
	}

	protected void setSelectionParameters()
	{
		SelectionPreferences selectionPreferences = new SelectionPreferences();
		selectionPreferences.setCellSelectionEnabled(false);
		selectionPreferences.setMultipleSelectionEnabled(false);
		table.setSelectionPreferences(selectionPreferences);
	}

	@Override public void addDesignAbstractionColumn()
	{
		addColumn(ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.designAbstraction.text"),
				AbstractShareColumnsProvider.DESIGN_ABSTRACTION_COLUMN_NAME, t -> t.getDesignAbstraction());
	}

	@Override @NotNull public Table<T> getTablePane()
	{
		return table;
	}

	@Override public void filter(Predicate<T> predicate)
	{
		table.filter(predicate);
		table.scrollToFirstCell();
	}

	@Override public void applyEdits()
	{
		dataStorage.apply();
	}

	@Override public void addAttributeColumn(@NotNull IAttributeType attribute)
	{
		addColumn(attribute.getDisplayName(), ATTRIBUTE_PREFIX + attribute.getName(),
				(t) -> t.getAttributeValue(attribute));
	}

	protected void addColumn(String displayName, @NotNull String columnName, Function<T, String> supplier)
	{
		if (isAlreadyPresentInTheTable(columnName)) {
			return;
		}

		Platform.runLater(new Runnable()
		{
			@Override public void run()
			{
				List<ColumnInformation<T>> columnInformations = new ArrayList<>();
				columnInformations.add(createColumnInfo(displayName, columnName, (t) -> {
					return StringUtils.nonNull(supplier.apply(t));
				}));
				table.addColumns(columnInformations);
			}
		});
	}

	protected boolean isAlreadyPresentInTheTable(@NotNull String columnName)
	{
		return table.columns().filter(col -> columnName.equalsIgnoreCase(col.getName())).findAny().isPresent();
	}

	@NotNull protected BatchColumnInfo<T> createColumnInfo(String displayName, String columnName,
			Function<T, Object> readMethod)
	{
		return new BatchColumnInfo<>(displayName, columnName, readMethod);
	}

	protected abstract void configureCellStateHandler();

	protected abstract void configureMenuItems();

	protected abstract void sortTable();
}

