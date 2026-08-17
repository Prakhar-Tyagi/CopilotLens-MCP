/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.AbstractShareDetailsPane;
import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.FunctionalBatchShareDetailsPane;
import com.mentor.capital.javafx.table.Table;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;

/**
 * Functional batch share dialog
 */
public class FunctionalBatchShareDialog extends AbstractBatchShareDialog
{

	public static final String FUNCTIONAL_BATCH_SHARE_TABLE_ID = "functional_batch_share_table";

	public FunctionalBatchShareDialog(@Nullable Frame frame,
			@Nullable String title, IBatchShareParams params)
	{
		super(frame, title, params);
	}

	@NotNull @Override protected BatchShareTableView createBatchShareTableView(@NotNull Label objectSelectedLabel)
	{
		return new BatchShareTableView(params.getData(), objectSelectedLabel, FUNCTIONAL_BATCH_SHARE_TABLE_ID,
				FunctionalBatchShareFilterObjectType.values());
	}

	@NotNull @Override protected AbstractShareTableControlPaneProvider<IBatchShareRow, IShareTableView<IBatchShareRow>> createBatchShareTableControlPaneProvider()
	{
		return new FunctionalBatchShareTableControlPaneProvider(this, batchShareTableView,
				FunctionalBatchShareFilterObjectType.values());
	}

	@NotNull @Override
	protected AbstractShareDetailsPane<IBatchShareRow> createBatchShareDetailsPane(@NotNull Table<IBatchShareRow> tablePane)
	{
		return new FunctionalBatchShareDetailsPane(tablePane);
	}
}
