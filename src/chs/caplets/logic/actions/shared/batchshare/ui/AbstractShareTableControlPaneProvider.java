/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.tabulareditor.IFilterableObjectType;
import chs.caplets.logic.actions.shared.batchshare.IPropertyTableView;
import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.cof.project.IProject;
import chs.common.IAttributePropertyProvider;
import chs.common.IPreferenceMgr;
import chs.common.attr.IAttributeType;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.system.ISystemData;
import chs.utilities.ResourceMgr;
import chs.utility.attr.AttributeHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Abstract base class for providing control pane UI components in batch share/unshare table dialogs.
 * <p>
 * This class manages the construction and behavior of a control pane positioned above the main table
 * in share/unshare dialogs. The control pane provides filtering capabilities and dynamic column
 * addition features, allowing users to customize their view of shareable/unshareable objects.
 *
 * @param <R> the row data type, must extend {@link IShareRow}
 * @param <T> the table view type, must extend {@link IShareTableView}
 */
public abstract class AbstractShareTableControlPaneProvider<R extends IShareRow, T extends IShareTableView<R>>
{

	public static final int BUTTON_SIZE = 30;
	public static final int BUTTON_WIDTH = 32;
	private final String key = "objectfilter";
	protected Pane mHoldingPane;
	@Nullable protected Dialog parentDialog;
	protected T tablePane;
	protected MenuButton addButton;
	protected IBatchShareFilterObjectType[] batchShareFilterObjectTypes;
	private FilteredList<IBatchShareFilterObjectType> currentSelectedObjectTypesList;
	private BatchShareObjectFilterProvider batchShareObjectFilterProvider;

	protected AbstractShareTableControlPaneProvider(@Nullable Dialog parentDialog, T tablePane,
			IBatchShareFilterObjectType[] batchShareFilterObjectTypes)
	{
		mHoldingPane = getHoldingPane();
		this.parentDialog = parentDialog;
		this.tablePane = tablePane;
		this.batchShareFilterObjectTypes = batchShareFilterObjectTypes;
		constructObjectTypeList();
		constructObjectFilter();
	}

	protected void constructObjectTypeList()
	{
		ObservableList<IBatchShareFilterObjectType> list = FXCollections.observableArrayList();
		list.addAll(batchShareFilterObjectTypes);
		currentSelectedObjectTypesList = new FilteredList<IBatchShareFilterObjectType>(list, t -> true);
	}

	@NotNull public Pane getControlPane()
	{
		return mHoldingPane;
	}

	protected abstract void constructContent();

	protected void constructRightSpacer()
	{
		Pane rightSpacer = new Pane();
		HBox.setHgrow(rightSpacer, Priority.ALWAYS);
		mHoldingPane.getChildren().add(rightSpacer);
	}

	protected void constructObjectFilterPane()
	{
		mHoldingPane.getChildren().add(batchShareObjectFilterProvider.constructToggleIconPane());
		batchShareObjectFilterProvider.clearAndReSelectElements(getSavedObjectSelectionFromPreferences());
	}

	protected void constructObjectFilter()
	{
		batchShareObjectFilterProvider =
				new BatchShareObjectFilterProvider(IFilterableObjectType.ObjectClass.Logic,
						this::applyObjectFilterChangesToTable,
						null, batchShareFilterObjectTypes);
	}

	protected void constructAddButton()
	{
		addButton = new MenuButton();
		addButton.setId("AddPropertyButton");
		updateGraphicForButton(addButton, "add");
		addButton.setTooltip(new Tooltip(getResourceString("addbutton.tooltip")));
		List<MenuItem> addButtonMenuItems = getAddButtonMenuItems();
		addButton.getItems().addAll(addButtonMenuItems);
		addButton.setMaxSize(BUTTON_WIDTH, BUTTON_SIZE);
		addButton.setMinSize(BUTTON_WIDTH, BUTTON_SIZE);
		mHoldingPane.getChildren().add(addButton);
	}


	@NotNull protected abstract List<MenuItem> getAddButtonMenuItems();

	@NotNull protected MenuItem getAttributePropertyMenuItem()
	{
		MenuItem attributePropertyItem = new MenuItem(getResourceString("addbutton.menuItem1"));
		attributePropertyItem.setId("attributePropertyMenuItem");
		attributePropertyItem.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override public void handle(ActionEvent event)
			{
				onAddButtonPressed();
			}
		});
		return attributePropertyItem;
	}

	@NotNull protected MenuItem getAbstractionMenuItem()
	{
		MenuItem abstractionItem = new MenuItem(getResourceString("addbutton.menuItem2"));
		abstractionItem.setId("abstractionMenuItem");
		abstractionItem.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override public void handle(ActionEvent event)
			{
				addDesignAbstractionColumn();
			}
		});
		return abstractionItem;
	}

	private void addDesignAbstractionColumn()
	{
		tablePane.addDesignAbstractionColumn();
	}

	private void onAddButtonPressed()
	{
		SwingUtilities.invokeLater(() -> {
			currentSelectedObjectTypesList.setPredicate(batchShareObjectFilterProvider.getObjectTypePredicate());
			showAttributePropertyDialog();
		});
	}

	/**
	 * Shows the attribute/property selection dialog.
	 */
	protected void showAttributePropertyDialog()
	{
		AttributePropertySelectorDialog dialog =
				new AttributePropertySelectorDialog(parentDialog, getResourceString("attributeselector.title"), true,
						getAttributes(), getProperties(), getAddDialogHelpId());

		dialog.setVisible(true);
		if (!dialog.isCancelled()) {
			addColumnToTable(dialog.getSelection());
		}
	}

	@NotNull protected abstract String getAddDialogHelpId();


	private void addColumnToTable(@Nullable AttributePropertySelection selection)
	{
		if (selection == null) {
			return;
		}
		if (selection.getSelectedAttribute() != null) {
			addAttributeColumnToTable(selection.getSelectedAttribute());
		}
		else if (selection.getSelectedProperty() != null) {
			addPropertyColumnToTable(selection.getSelectedProperty());
		}
	}

	protected void addAttributeColumnToTable(@NotNull IAttributeType attribute)
	{
		tablePane.addAttributeColumn(attribute);
	}

	protected void addPropertyColumnToTable(@NotNull String propertyName)
	{
		if(tablePane instanceof IPropertyTableView pane)
		{
			pane.addPropertyColumn(propertyName);
		}
	}

	private void updateGraphicForButton(Labeled button, String path)
	{
		String fullPath = "chs/images/javafx_ui/" + path + "-small.png";
		button.setGraphic(new ImageView(CHSImageLoader.loadJFXImage(fullPath)));
	}

	@NotNull protected Pane getHoldingPane()
	{
		HBox hBox = new HBox();
		final int borderSpacing = 6;
		hBox.setPadding(new Insets(borderSpacing));
		return hBox;
	}

	protected boolean applyObjectFilterChangesToTable(Predicate<IAttributePropertyProvider> predicate)
	{
		currentSelectedObjectTypesList.setPredicate(batchShareObjectFilterProvider.getObjectTypePredicate());
		saveObjectSelectionPreferences();
		return applyObjectFilterChangesToTable();
	}

	protected boolean applyObjectFilterChangesToTable()
	{
		Predicate<R> pred = getObjectFilterPredicate();
		filterTable(pred);
		return true;
	}

	@NotNull protected Predicate<R> getObjectFilterPredicate()
	{
		Set<ShareableEntityTypeEnum> activeTypes = getActiveObjectFilterTypes();
		Predicate<R> pred =
				(o) -> activeTypes.stream().filter(cl -> cl.equals(o.getObjectType())).findAny().isPresent();
		return pred;
	}

	protected void filterTable(Predicate<R> predicate)
	{
		tablePane.filter(predicate);
	}

	@NotNull protected Set<ShareableEntityTypeEnum> getActiveObjectFilterTypes()
	{
		Set<ShareableEntityTypeEnum> activeTypes = EnumSet.noneOf(ShareableEntityTypeEnum.class);
		for (IBatchShareFilterObjectType type : currentSelectedObjectTypesList) {
			activeTypes.addAll(type.getRepresentedObjectTypes());
		}
		return activeTypes;
	}

	@NotNull protected Set<IAttributeType> getAttributes()
	{
		Set<String> fixedColumnNames = getFixedColumnNames();

		return currentSelectedObjectTypesList.stream()
				.flatMap(filterType -> filterType.getRepresentedObjectTypes().stream())
				.flatMap(type -> AttributeHelper.getAttributeTypes(type.getAttributeProviderClass()).values().stream()
						.filter(attr -> type.getAvailableAttributeNames().contains(attr.getName())))
				.filter(attr -> !fixedColumnNames.contains(attr.getName()))
				.collect(Collectors.toSet());
	}

	@NotNull protected abstract Set<String> getFixedColumnNames();

	@NotNull private Set<String> getProperties()
	{
		Set<String> properties = new HashSet<>();
		IPreferenceMgr preferenceMgr = getPreferences();
		if (preferenceMgr != null) {
			currentSelectedObjectTypesList.stream().forEach(filterType ->
			{
				for (ShareableEntityTypeEnum objectType : filterType.getRepresentedObjectTypes()) {
					properties.addAll(objectType.getOTIProperties(preferenceMgr));
				}
			});
		}
		return properties;
	}

	@Nullable private IPreferenceMgr getPreferences()
	{
		IProject currentProject = CAFUtils.getInstance().getCurrentProject();
		if (currentProject == null || currentProject.getObjectTypeInfoMgr() == null) {
			ISystemData systemData = FactoryMgr.getSystemFactory().getCHSSystem().getSystemData();
			return systemData.getPreferences();
		}
		return currentProject.getPreferences();
	}

	@NotNull private Set<String> getSavedObjectSelectionFromPreferences()
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		String pref = preferences.get(key, BatchShareFilterObjectType.DEVICE.getName());
		return new HashSet<String>(Arrays.asList(pref.split(",")));
	}

	public void saveObjectSelectionPreferences()
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		StringJoiner value = new StringJoiner(",");
		currentSelectedObjectTypesList.stream().forEach(obj -> value.add(obj.getName()));
		preferences.put(key, value.toString());
	}

	/**
	 * Retrieves a resource string for the given resource key.
	 * <p>
	 * This method constructs the full resource key
	 * by appending the subclass resource prefix (e.g.,"BatchShareTableControlPaneProvider.")
	 * to the given key.
	 * </p>
	 *
	 * @param resourceKey the resource key suffix (e.g., "attributeselector.title")
	 */
	@NotNull protected String getResourceString(@NotNull String resourceKey)
	{
		return ResourceMgr.getString(getClass(), getResourcePrefix() + resourceKey);
	}

	@NotNull protected abstract String getResourcePrefix();

}
