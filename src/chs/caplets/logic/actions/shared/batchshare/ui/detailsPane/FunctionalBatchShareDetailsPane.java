/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.caplets.logic.actions.shared.batchshare.ui.IBatchShareRow;
import com.mentor.capital.javafx.table.Table;
import org.jetbrains.annotations.NotNull;

/**
 * Functional batch share details pane
 */
public class FunctionalBatchShareDetailsPane extends AbstractShareDetailsPane<IBatchShareRow>
{

	public FunctionalBatchShareDetailsPane(@NotNull
	Table<IBatchShareRow> mainTable)
	{
		super(mainTable);
	}

	@NotNull @Override protected AbstractInstanceDetailPane createInstanceDetailPane()
	{
		return new FunctionalInstanceDetailsPane(null);
	}

	@Override @NotNull protected String getExpandedTextKey()
	{
		return "BatchShareDetailsPane.expanded.text";
	}

	@Override @NotNull protected String getCollapsedTextKey()
	{
		return "BatchShareDetailsPane.collapsed.text";
	}
}
