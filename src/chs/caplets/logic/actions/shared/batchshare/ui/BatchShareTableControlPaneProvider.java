/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class BatchShareTableControlPaneProvider
		extends AbstractShareTableControlPaneProvider<IBatchShareRow, IShareTableView<IBatchShareRow>>
{
	public static final String ADD_DIALOG_HELP_ID = "logic_action_batchshare_addcolumn_dialog";

	BatchShareTableControlPaneProvider(@Nullable Dialog parentDialog, IShareTableView<IBatchShareRow> tablePane,
			IBatchShareFilterObjectType[] batchShareFilterObjectTypes)
	{
		super(parentDialog, tablePane, batchShareFilterObjectTypes);
		constructContent();
	}

	@Override protected void constructContent()
	{
		constructObjectFilterPane();
		constructRightSpacer();
		constructAddButton();
	}

	@Override @NotNull protected List<MenuItem> getAddButtonMenuItems()
	{
		List<MenuItem> menuItems = new ArrayList<>();
		menuItems.add(getAttributePropertyMenuItem());
		menuItems.add(getAbstractionMenuItem());
		return menuItems;
	}

	@Override @NotNull protected Set<String> getFixedColumnNames()
	{
		return Arrays.stream(BatchShareColumn.values()).map(BatchShareColumn::getName).collect(Collectors.toSet());
	}

	@Override @NotNull protected String getAddDialogHelpId()
	{
		return ADD_DIALOG_HELP_ID;
	}

	@Override @NotNull protected String getResourcePrefix()
	{
		return "BatchShareTableControlPaneProvider.";
	}
}
