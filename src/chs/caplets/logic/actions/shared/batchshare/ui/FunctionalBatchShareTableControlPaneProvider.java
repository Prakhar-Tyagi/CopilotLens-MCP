/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.StringUtils;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Functional batch share table control pane provider
 */
public class FunctionalBatchShareTableControlPaneProvider extends AbstractShareTableControlPaneProvider<IBatchShareRow, IShareTableView<IBatchShareRow>>
{

	private static final String KEY = "customfilter";
	private BatchShareCustomFilterProvider customFilterProvider;
	public static final String ADD_DIALOG_HELP_ID = "logic_action_batchshare_addcolumn_dialog";

	FunctionalBatchShareTableControlPaneProvider(@Nullable Dialog parentDialog, IShareTableView<IBatchShareRow> tablePane,
			IBatchShareFilterObjectType[] batchShareFilterObjectTypes)
	{
		super(parentDialog, tablePane, batchShareFilterObjectTypes);
		customFilterProvider = constructCustomFilterProvider();
		constructContent();
	}

	@Override protected void constructContent()
	{
		constructObjectFilterPane();
		constructRightSpacer();
		constructCustomFilterPane();
		constructAddButton();
	}

	private void constructCustomFilterPane()
	{
		Pane filterPane = customFilterProvider.constructFilterPane();
		mHoldingPane.getChildren().add(filterPane);
	}

	@NotNull @Override protected List<MenuItem> getAddButtonMenuItems()
	{
		List<MenuItem> menuItems = new ArrayList<>();
		menuItems.add(getAttributePropertyMenuItem());
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

	@Override protected boolean applyObjectFilterChangesToTable()
	{
		Predicate<IBatchShareRow> customFilterPredicate = customFilterProvider.getPredicate();
		Predicate<IBatchShareRow> combinedPredicate = getObjectFilterPredicate().and(customFilterPredicate);
		filterTable(combinedPredicate);
		return true;
	}

	@NotNull private BatchShareCustomFilterProvider constructCustomFilterProvider()
	{
		FunctionalBatchShareCustomFilterProvider customFilter =
				new FunctionalBatchShareCustomFilterProvider(this::applyObjectFilterChangesToTable);
		customFilter.clearAndResetFilterSelection(getSavedCustomFilterSelectionFromPreferences());
		return customFilter;
	}

	@NotNull private Set<String> getSavedCustomFilterSelectionFromPreferences()
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		String pref = preferences.get(KEY, "");
		return new HashSet<String>(Arrays.asList(pref.split(",")));
	}

	@Override public void saveObjectSelectionPreferences()
	{
		super.saveObjectSelectionPreferences();
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		String value = StringUtils.concatenate(customFilterProvider.getFilterIds(), ",");
		preferences.put(KEY, value);
	}

	@Override @NotNull protected String getResourcePrefix()
	{
		return "BatchShareTableControlPaneProvider.";
	}
}
