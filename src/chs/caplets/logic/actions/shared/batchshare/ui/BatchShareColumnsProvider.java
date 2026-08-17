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

import chs.common.attr.IAttributeType;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ColumnTypeInfo;
import com.mentor.capital.javafx.table.IColumnTypeInfo;
import com.mentor.capital.javafx.table.cell.EnumCellControlCreator;
import com.mentor.capital.javafx.table.cell.EnumColumnType;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Column provider for batch share dialog
 */

public class BatchShareColumnsProvider extends AbstractShareColumnsProvider<IBatchShareRow>
{

	public BatchShareColumnsProvider(IBatchShareFilterObjectType[] filterObjectTypes)
	{
		super(filterObjectTypes);
	}

	/**
	 * Registers the creators for columns specific to batch sharing.
	 */
	@Override protected void registerSpecificColumnCreators()
	{
		registerColumn(BatchShareColumn.ACTION.getName(), this::createActionColumn);
		registerColumn(BatchShareColumn.MATCHED_BY.getName(), this::createMatchedByColumn);
	}

	/**
	 * Resolves custom columns for batch share supporting attributes and properties.
	 */
	@Override @Nullable protected ColumnInformation<IBatchShareRow> resolveCustomColumn(String name)
	{
		Function<IAttributeType, ColumnInformation<IBatchShareRow>> attributeColumnCreator =
				attrType -> new BatchColumnInfo<>(attrType.getDisplayName(), name,
						row -> row.getAttributeValue(attrType));

		Function<String, ColumnInformation<IBatchShareRow>> propertyColumnCreator =
				propName -> new BatchColumnInfo<>(propName, name, row -> row.getPropertyValue(propName));

		return m_columnResolver.resolveCustomColumn(name, attributeColumnCreator, propertyColumnCreator);
	}

	@NotNull @Override public List<ColumnInformation<IBatchShareRow>> getDefaultColumns()
	{
		return Arrays.stream(BatchShareColumn.values()).map(item -> getColumnInfo(item.getName()))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	@NotNull protected ColumnInformation<IBatchShareRow> createMatchedByColumn()
	{
		return new BatchColumnInfo<>(BatchShareColumn.MATCHED_BY.getDisplayName(),
				BatchShareColumn.MATCHED_BY.getName(), IBatchShareRow::getMatchBy);
	}

	@NotNull protected ColumnInformation<IBatchShareRow> createActionColumn()
	{
		return new BatchColumnInfo<>(BatchShareColumn.ACTION.getDisplayName(), BatchShareColumn.ACTION.getName(),
				IBatchShareRow::getAction, (row, obj) -> row.setAction((Action) obj),
				new BatchShareEnumColumnType(new ColumnTypeInfo(Action.SHARE)));
	}

	private static class BatchShareEnumColumnType extends EnumColumnType
	{

		BatchShareEnumColumnType(IColumnTypeInfo dataManipulator)
		{
			super(dataManipulator);
		}

		@NotNull @Override public IControlCreator getControlCreator()
		{
			return new EnumCellControlCreator(new BatchShareComboboxCellBuilder());
		}
	}
}

