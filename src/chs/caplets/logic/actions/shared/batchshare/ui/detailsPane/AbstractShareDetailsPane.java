/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.common.DesignUtils;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.ITableSelectionListener;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.Table;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for expandable details pane in batch share/unshare dialogs.
 * <p>
 * This class extends TitledPane to provide a collapsible panel that displays detailed
 * information about object selected in the main share/unshare table.
 *
 * @param <T> the row data type, must extend {@link IShareRow}
 */
public abstract class AbstractShareDetailsPane<T extends IShareRow> extends TitledPane
{

	protected Table<T> mainTable;
	protected List<BaseDetailPane> childrenPanes;
	private T previouslySelectedRow = null;
	private boolean collapsed = true;

	protected AbstractShareDetailsPane(@NotNull Table<T> mainTable)
	{
		this.mainTable = mainTable;
		setId("DetailsPane");
		initializeContent();
		addTableSelectionListener();

		updateChildPanesWhenCurrentPaneExpands();
		ChangeListener<Boolean> expansionListener = (observable, oldValue, newValue) -> setText(newValue ?
				ResourceMgr.getString(getClass(), getExpandedTextKey()) :
				ResourceMgr.getString(getClass(), getCollapsedTextKey()));
		expandedProperty().addListener(expansionListener);
		setExpanded(false);
	}

	protected void updateChildPanesWhenCurrentPaneExpands()
	{
		expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
			if (isNowExpanded) {
				collapsed = false;
				updateChildrenPanes();
			}
			else {
				collapsed = true;
			}
		});
	}

	protected void addTableSelectionListener()
	{
		mainTable.addSelectionListener(new ITableSelectionListener<T>()
		{
			@Override
			public void changed(Selection<T> selection, int lastSelectionIndex)
			{
				if (!collapsed) {
					updateChildrenPanes();
				}
			}
		});
	}

	protected void updateChildrenPanes()
	{
		T selectedRow = getSelectedRow();
		if (selectedRow == null || isSameObjectSelected(selectedRow)) {
			return;
		}

		ILogicDesign design = getLogicDesign(selectedRow);

		//We need to load design before fetching child diagram usages.
		Task<Void> loadDesignTask = getLoadDesignTask(design);
		updateDetailsOnCompletionOfTask(loadDesignTask);
	}

	private boolean isSameObjectSelected(@NotNull T selectedRow)
	{
		boolean result = selectedRow.equals(previouslySelectedRow);
		previouslySelectedRow = selectedRow;
		return result;
	}

	private void updateDetailsOnCompletionOfTask(@NotNull Task<Void> loadDesignTask)
	{
		loadDesignTask.setOnSucceeded(e -> {
			updateDetailPanes();
			//Update cursor to normal when Diagram loading completed
			setSceneCursor(Cursor.DEFAULT);
		});
		loadDesignTask.setOnFailed(e -> setSceneCursor(Cursor.DEFAULT));

		setSceneCursor(Cursor.WAIT);
		new Thread(loadDesignTask).start();
	}

	@NotNull protected Task<Void> getLoadDesignTask(@NotNull ILogicDesign design)
	{
		return new Task<Void>()
		{
			@Override @Nullable public Void call()
			{
				if (!design.isLoadedInMemory()) {
					design.loadToMemory();
				}
				return null;
			}
		};
	}

	@NotNull private ILogicDesign getLogicDesign(@NotNull T selectedRow)
	{
		ILogicDesign design = DesignUtils.getDesign(selectedRow.getDesignUID(), ILogicDesign.class);
		assert design != null;
		return design;
	}

	private void updateDetailPanes()
	{
		ILogicObject currentSelection = getCurrentSelection();
		if (currentSelection != null) {
			for (BaseDetailPane pane : childrenPanes) {
				pane.updateContent(currentSelection);
			}
		}
	}

	@Nullable protected ILogicObject getCurrentSelection()
	{
		T selectedRow = getSelectedRow();
		if (selectedRow == null) {
			return null;
		}

		return (ILogicObject) selectedRow.getObjectUID().getObject();
	}

	protected void initializeContent()
	{
		createChildrenPanes();

		updateChildrenPanes();

		Accordion accordion = new Accordion();
		addChildrenPanesToAccordion(accordion);

		//Add the accordion to this Parent Details Pane
		setContent(accordion);
	}

	private void addChildrenPanesToAccordion(@NotNull Accordion accordion)
	{
		childrenPanes.stream().forEach(pane -> accordion.getPanes().add(pane));
		accordion.setExpandedPane(accordion.getPanes().get(0));
	}

	private void createChildrenPanes()
	{
		childrenPanes = new ArrayList<>();
		AbstractInstanceDetailPane instanceDetailPane = createInstanceDetailPane();
		ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> mainTable.toggleTableClosing();
		instanceDetailPane.tableClosingProperty().addListener(listener);
		childrenPanes.add(instanceDetailPane);

		AttributeDetailPane attributeDetailPane = new AttributeDetailPane(null);
		attributeDetailPane.tableClosingProperty().addListener(listener);
		childrenPanes.add(attributeDetailPane);

		PropertyDetailpane propertyDetailpane = new PropertyDetailpane(null);
		propertyDetailpane.tableClosingProperty().addListener(listener);
		childrenPanes.add(propertyDetailpane);
	}

	/**
	 * Constructs batch share table instance details pane
	 *
	 * @return batch share table instance details pane
	 */
	@NotNull protected abstract AbstractInstanceDetailPane createInstanceDetailPane();

	@Nullable private T getSelectedRow()
	{
		Selection<T> selection = mainTable.getSelection();
		List<T> selectedInfos = selection.getSelectedCells().stream()
				.map(CellSelection::getSelectedItem)
				.collect(Collectors.toList());

		if (selectedInfos.isEmpty()) {
			return null;
		}
		assert selectedInfos.size() == 1;
		return selectedInfos.iterator().next();
	}

	protected void setSceneCursor(Cursor wait)
	{
		getScene().getRoot().setCursor(wait);
	}

	@NotNull protected abstract String getExpandedTextKey();

	@NotNull protected abstract String getCollapsedTextKey();
}
