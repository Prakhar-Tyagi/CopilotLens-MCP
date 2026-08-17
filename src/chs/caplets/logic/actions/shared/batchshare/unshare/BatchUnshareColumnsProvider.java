/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.ui.AbstractShareColumnsProvider;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchColumnInfo;
import chs.caplets.logic.actions.shared.batchshare.ui.IBatchShareFilterObjectType;
import chs.common.attr.IAttributeType;
import chs.common.attr.IAttributeTypes;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provider class for creating and managing column definitions in the batch unshare table.
 * <p>
 * This class is responsible for generating ColumnInformation objects that define the
 * structure, appearance, and behavior of columns displayed in the {@link BatchUnshareTableView}.
 * It supports both fixed mandatory columns (defined in {@link BatchUnshareColumn}) and dynamic
 * custom columns (attributes, properties, and design abstraction).
 */
public class BatchUnshareColumnsProvider extends AbstractShareColumnsProvider<IBatchUnshareRow>
{

	public BatchUnshareColumnsProvider(IBatchShareFilterObjectType[] filterObjectTypes)
	{
		super(filterObjectTypes);
	}

	/**
	 * Registers the creators for columns specific to batch unsharing.
	 */
	@Override protected void registerSpecificColumnCreators()
	{
		registerColumn(BatchUnshareColumn.REVISION.getName(), this::createRevisionColumn);
		registerColumn(BatchUnshareColumn.OPTION_EXPRESSION.getName(), this::createOptionExpressionColumn);
		registerColumn(BatchUnshareColumn.BUILD_LIST.getName(), this::createBuildListColumn);
	}


	/**
	 * Resolves custom columns for batch unshare supporting attributes.
	 */
	@Override @Nullable protected ColumnInformation<IBatchUnshareRow> resolveCustomColumn(String name)
	{
		Function<IAttributeType, ColumnInformation<IBatchUnshareRow>> attributeColumnCreator =
				attrType -> new BatchColumnInfo<>(attrType.getDisplayName(), name,
						row -> row.getAttributeValue(attrType));

		return m_columnResolver.resolveCustomColumn(name, attributeColumnCreator, null);
	}

	@NotNull @Override public List<ColumnInformation<IBatchUnshareRow>> getDefaultColumns()
	{
		return Arrays.stream(BatchUnshareColumn.values()).map(item -> getColumnInfo(item.getName()))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	@NotNull protected ColumnInformation<IBatchUnshareRow> createRevisionColumn()
	{
		return new BatchColumnInfo<>(BatchUnshareColumn.REVISION.getDisplayName(),
				BatchUnshareColumn.REVISION.getName(), IBatchUnshareRow::getRevision);
	}

	@NotNull protected ColumnInformation<IBatchUnshareRow> createOptionExpressionColumn()
	{
		return new BatchColumnInfo<>(BatchUnshareColumn.OPTION_EXPRESSION.getDisplayName(),
				BatchUnshareColumn.OPTION_EXPRESSION.getName(), row-> row.getAttributeValue(IAttributeTypes.OPTION_EXP));
	}

	@NotNull protected ColumnInformation<IBatchUnshareRow> createBuildListColumn()
	{
		return new BatchColumnInfo<>(BatchUnshareColumn.BUILD_LIST.getDisplayName(),
				BatchUnshareColumn.BUILD_LIST.getName(), IBatchUnshareRow::getBuildList);
	}
}

