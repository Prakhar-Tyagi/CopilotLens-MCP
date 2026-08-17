/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.AbstractShareDetailsPane;
import chs.caplets.logic.actions.shared.batchshare.ui.detailsPane.BatchShareDetailsPane;
import com.mentor.capital.javafx.table.Table;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;

/**
 * Batch share Dialog
 */
public class BatchShareDialog extends AbstractBatchShareDialog
{

	public static final String BATCH_SHARE_TABLE_ID = "batch_share_table";

	public BatchShareDialog(@Nullable Frame frame,
			@Nullable String title, IBatchShareParams params)
	{
		super(frame, title, params);
	}

	@Override @NotNull protected BatchShareTableView createBatchShareTableView(@NotNull Label objectSelectedLabel)
	{
		return new BatchShareTableView(params.getData(), objectSelectedLabel, BATCH_SHARE_TABLE_ID,
				BatchShareFilterObjectType.values());
	}

	@NotNull @Override protected AbstractShareTableControlPaneProvider<IBatchShareRow, IShareTableView<IBatchShareRow>> createBatchShareTableControlPaneProvider()
	{
		return new BatchShareTableControlPaneProvider(this, batchShareTableView, BatchShareFilterObjectType.values());
	}

	@Override @NotNull
	protected AbstractShareDetailsPane<IBatchShareRow> createBatchShareDetailsPane(@NotNull Table<IBatchShareRow> tablePane)
	{
		return new BatchShareDetailsPane(tablePane);
	}
}
