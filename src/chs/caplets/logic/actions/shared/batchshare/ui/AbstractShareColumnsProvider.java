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
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ColumnTypeInfo;
import com.mentor.capital.javafx.table.IColumnTypeInfo;
import com.mentor.capital.javafx.table.cell.BooleanColumnType;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Abstract base class for providing column definitions for batch share/unshare table views.
 * <p>
 * This class manages the creation and registration of table columns used in batch share/unshare.
 *
 * @param <T> the type of row data, must extend {@link IShareRow}
 */
public abstract class AbstractShareColumnsProvider<T extends IShareRow>
{

	public static final String DESIGN_ABSTRACTION_COLUMN_NAME = "designAbstraction";
	public static final String SELECTION_COLUMN_NAME = "Selection";
	public static final String NAME_COLUMN_NAME = "Name";
	public static final String DESIGN_COLUMN_NAME = "Design";
	protected final CustomColumnResolver m_columnResolver;

	// Map to store column names to ColumnInformation suppliers
	protected final Map<String, Supplier<ColumnInformation<T>>> m_columnCreators = new HashMap<>();

	protected AbstractShareColumnsProvider(IBatchShareFilterObjectType[] filterObjectTypes)
	{
		m_columnResolver = new CustomColumnResolver(filterObjectTypes);
		registerAllColumns();
	}

	/**
	 * Registers all column creators, including common and specific ones.
	 */
	private void registerAllColumns()
	{
		registerCommonColumnCreators();
		registerSpecificColumnCreators();
	}

	/**
	 * Registers the creators for common columns between batch share/unshare
	 */
	private void registerCommonColumnCreators()
	{
		registerColumn(SELECTION_COLUMN_NAME, this::createSelectionColumn);
		registerColumn(NAME_COLUMN_NAME, this::createNameColumn);
		registerColumn(DESIGN_COLUMN_NAME, this::createDesignColumn);
		registerColumn(DESIGN_ABSTRACTION_COLUMN_NAME, this::createDesignAbstractionColumn);
	}

	/**
	 * Abstract method for subclasses to register their specific column creators
	 */
	protected abstract void registerSpecificColumnCreators();

	@NotNull protected ColumnInformation<T> createSelectionColumn()
	{
		return new BatchColumnInfo<>(
				ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.selection.text"),
				SELECTION_COLUMN_NAME, IShareRow::isSelected, (row, obj) -> row.setSelected((boolean) obj),
				new BatchSelectionColumnType(new ColumnTypeInfo(true)));
	}

	@NotNull protected ColumnInformation<T> createNameColumn()
	{
		return new BatchColumnInfo<>(
				ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.objectname.text"),
				NAME_COLUMN_NAME, IShareRow::getName);
	}

	@NotNull protected ColumnInformation<T> createDesignColumn()
	{
		return new BatchColumnInfo<>(
				ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.design.text"),
				DESIGN_COLUMN_NAME, IShareRow::getDesignName);
	}

	@NotNull protected ColumnInformation<T> createDesignAbstractionColumn()
	{
		return new BatchColumnInfo<>(
				ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.designAbstraction.text"),
				DESIGN_ABSTRACTION_COLUMN_NAME, IShareRow::getDesignAbstraction);
	}

	/**
	 * Retrieves a ColumnInformation object by its name.
	 * It first checks registered column creators, then delegates to the custom column resolver
	 *
	 * @param name The name of the column.
	 * @return The ColumnInformation object, or null if not found.
	 */
	@Nullable public ColumnInformation<T> getColumnInfo(String name)
	{
		Supplier<ColumnInformation<T>> creator = m_columnCreators.get(name);
		if (creator != null) {
			return creator.get();
		}

		return resolveCustomColumn(name);
	}

	/**
	 * Resolves custom columns (attributes/properties).
	 *
	 * @param name The column name to resolve
	 * @return The ColumnInformation, or null if not found
	 */
	@Nullable protected abstract ColumnInformation<T> resolveCustomColumn(String name);

	/**
	 * Registers a column creator for the specified column name.
	 *
	 * @param name    The name of the column (must match enum getName())
	 * @param creator A supplier that creates the ColumnInformation when needed
	 */
	protected void registerColumn(String name, Supplier<ColumnInformation<T>> creator)
	{
		m_columnCreators.put(name, creator);
	}

	/**
	 * Returns the list of default columns for the specific batch share/unshare operation.
	 */
	@NotNull public abstract List<ColumnInformation<T>> getDefaultColumns();

	protected static class BatchSelectionColumnType extends BooleanColumnType
	{

		public BatchSelectionColumnType(IColumnTypeInfo dataManipulator)
		{
			super(dataManipulator);
		}

		@Override @NotNull public IControlCreator getControlCreator()
		{
			return new BatchShareBooleanCellControlCreator();
		}
	}
}

