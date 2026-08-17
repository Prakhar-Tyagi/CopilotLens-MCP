/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.unshare;

import chs.caplets.logic.actions.shared.batchshare.ui.AbstractShareTableControlPaneProvider;
import chs.caplets.logic.actions.shared.batchshare.ui.AttributePropertySelectorDialog;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareFilterObjectType;
import chs.caplets.logic.actions.shared.batchshare.ui.IShareTableView;
import chs.common.attr.IAttributeType;
import javafx.geometry.Insets;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provider for the control pane UI in the batch unshare table dialog.
 * <p>
 * This class extends {@link AbstractShareTableControlPaneProvider} to provide a specialized
 * control pane for batch unshare redundant objects operations. The control pane provides users
 * with filtering and column customization capabilities for the unshare table view.
 */
public class BatchUnshareTableControlPaneProvider
		extends AbstractShareTableControlPaneProvider<IBatchUnshareRow, IShareTableView<IBatchUnshareRow>>
{

	public static final String ADD_DIALOG_HELP_ID = "logic_action_batchunshare_addcolumn_dialog";

	public BatchUnshareTableControlPaneProvider(@Nullable Dialog parentDialog,
			@NotNull IShareTableView<IBatchUnshareRow> tableView)
	{
		super(parentDialog, tableView, BatchShareFilterObjectType.getUnshareRedundantTypes());
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
		return Arrays.stream(BatchUnshareColumn.values()).map(BatchUnshareColumn::getName).collect(Collectors.toSet());
	}

	@Override @NotNull protected String getAddDialogHelpId()
	{
		return ADD_DIALOG_HELP_ID;
	}

	@Override @NotNull protected String getResourcePrefix()
	{
		return "BatchUnshareTableControlPaneProvider.";
	}


	@Override @NotNull protected Pane getHoldingPane()
	{
		HBox hBox = new HBox();
		final int borderSpacing = 8;
		hBox.setPadding(new Insets(borderSpacing));
		return hBox;
	}

	@Override protected void showAttributePropertyDialog()
	{
		AttributePropertySelectorDialog dialog =
				new AttributePropertySelectorDialog(parentDialog, getResourceString("attributeselector.title"), true,
						getAttributes(), getAddDialogHelpId());
		dialog.setVisible(true);
		if (!dialog.isCancelled()) {
			IAttributeType selectedAttribute = dialog.getSelectedAttribute();
			if (selectedAttribute != null) {
				addAttributeColumnToTable(selectedAttribute);
			}
		}
	}
}

