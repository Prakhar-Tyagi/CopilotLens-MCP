/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.AbstractShareDetailsPane;
import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.AbstractInstanceDetailPane;
import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.InstanceDetailPane;
import com.mentor.capital.javafx.table.Table;
import org.jetbrains.annotations.NotNull;

/**
 * Details pane component for the batch unshare dialog that displays detailed information about selected object.
 * <p>
 * This pane extends {@link AbstractShareDetailsPane} to provide a specialized view for unshare operations.
 * It displays detailed information about the currently selected object in the batch unshare table,
 * allowing users to review object properties before confirming the unshare action.
 */
public class BatchUnshareDetailsPane extends AbstractShareDetailsPane<IBatchUnshareRow>
{

	public BatchUnshareDetailsPane(@NotNull Table<IBatchUnshareRow> mainTable)
	{
		super(mainTable);
	}

	@Override @NotNull protected AbstractInstanceDetailPane createInstanceDetailPane()
	{
		return new InstanceDetailPane(null);
	}

	@Override @NotNull protected String getExpandedTextKey()
	{
		return "BatchUnshareDetailsPane.expanded.text";
	}

	@Override @NotNull protected String getCollapsedTextKey()
	{
		return "BatchUnshareDetailsPane.collapsed.text";
	}
}

