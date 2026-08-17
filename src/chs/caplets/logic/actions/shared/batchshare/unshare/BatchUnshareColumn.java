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
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration defining the mandatory columns for the batch unshare table view.
 * <p>
 * This enum represents the fixed, non-removable columns that appear in the {@link BatchUnshareTableView}.
 * Each enum constant defines a column's internal name, localized display name, and a factory function
 * for creating the corresponding ColumnInformation object.
 */
public enum BatchUnshareColumn
{
	SELECTION(AbstractShareColumnsProvider.SELECTION_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.selection.text")),
	OBJECT_NAME(AbstractShareColumnsProvider.NAME_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.objectname.text")),
	REVISION("Revision", ResourceMgr.getString(BatchUnshareColumn.class, "BatchUnshareColumn.revision.text")),
	OPTION_EXPRESSION("OptionExpression",
			ResourceMgr.getString(BatchUnshareColumn.class, "BatchUnshareColumn.optionexpression.text")),
	DESIGN_NAME(AbstractShareColumnsProvider.DESIGN_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.design.text")),
	BUILD_LIST("BuildList", ResourceMgr.getString(BatchUnshareColumn.class, "BatchUnshareColumn.buildlist.text"));

	private final String m_name;
	private final String m_displayName;

	BatchUnshareColumn(String name, String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}

	@NotNull public String getName()
	{
		return m_name;
	}

	@NotNull public String getDisplayName()
	{
		return m_displayName;
	}
}

