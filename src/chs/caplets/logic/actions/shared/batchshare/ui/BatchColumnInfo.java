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
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.cell.TableColumnType;
import com.mentor.capital.javafx.table.menu.DefaultMenuItem;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Column information class to provide column behavior for batch share/unshare table views
 *
 * @param <T> the type of row data, must extend {@link IShareRow}
 */
public class BatchColumnInfo<T extends IShareRow> extends ColumnInformation<T>
{

	public BatchColumnInfo(String title, String name, Function<T, Object> readMethod,
			@Nullable BiConsumer<T, Object> writeMethod, @Nullable TableColumnType columnType)
	{
		super(title, name, readMethod, writeMethod, columnType);
	}

	public BatchColumnInfo(String title, String name, Function<T, Object> readMethod)
	{
		super(title, name, readMethod);
	}

	@Override public boolean displayDefaultMenuItem(DefaultMenuItem item)
	{
		return item != DefaultMenuItem.Select && item != DefaultMenuItem.Hide;
	}
}
