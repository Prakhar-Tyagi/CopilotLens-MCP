/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.cell.CapitalTableCell;
import com.mentor.capital.javafx.table.common.ITableCellDragSelectHandler;
import com.mentor.capital.javafx.table.helpers.copypaste.LocallySourcedTableContent;
import com.mentor.capital.javafx.table.impl.CapitalTableView;
import com.mentor.capital.javafx.table.impl.CapitalTableViewSkin;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public abstract class AbstractDnDHelper<T> implements ITableCellDragSelectHandler<T>

{

	public static final int POPUP_X = 20;
	public static final int POPUP_Y = 20;
	protected DragBlockFinder<T> blockFinder;
	private List<DragStyleSetter<T>> dragStyleSetterProvider;
	protected PopupHandler popup;
	protected Table<T> table;
	protected double headerRowHeight;
	@Nullable protected ScrollBar vBar;

	protected ValidateDnDBetweenCells<T> validateCellForDnD;

	public static final DataFormat SERIALIZED_MIME_TYPE = LocallySourcedTableContent.DATA_FORMAT;

	protected AbstractDnDHelper()
	{
		dragStyleSetterProvider = new ArrayList<>();
	}

	public void reset(CapitalTableCell<T> capitalTableCell, Table<T> givenTable,
			ValidateDnDBetweenCells<T> givenValidateCellForDnD, DragBlockFinder<T> givenBlockFinder,
			PopupHandler givenPopupHandler)
	{
		dragStyleSetterProvider = new ArrayList<>();
		popup = givenPopupHandler;
		table = givenTable;

		CapitalTableViewSkin<?> skin =
				(CapitalTableViewSkin<?>) (((CapitalTableView<?>) capitalTableCell.getTableView()).getSkin());

		if (skin != null) { //skin could be null for manage connector tests.
			headerRowHeight = ((Region) skin.queryAccessibleAttribute(AccessibleAttribute.HEADER)).getHeight();
			vBar = skin.getVerticalScrollBar();
		}
		else {
			headerRowHeight = 0;
			vBar = null;
		}

		validateCellForDnD = givenValidateCellForDnD;
		blockFinder = givenBlockFinder;
	}

	//source item need not be same as source cell.getRowItem as the cells are reused in scrolling and the original row item could be different.
	protected abstract void doWhatEverIsRequired(
			CapitalTableCell<T> targetCell,
			int sourceRowIndex, TableColumn<?, ?> sourceTableColumn,
			Object source);

	@Override public void updateStyle(@NotNull CapitalTableCell<T> capitalTableCell)
	{
		DragStyleSetter<T> dragStyleSetter =
				dragStyleSetterProvider.isEmpty() ? null : dragStyleSetterProvider.get(0);
		if (dragStyleSetter != null) {
			dragStyleSetter.updateSelected(capitalTableCell);
		}
	}

	@Override public void onMouseMoved(@NotNull CapitalTableCell<T> capitalTableCell, MouseEvent event)
	{

	}

	@Override public void onMouseExited(@NotNull CapitalTableCell<T> capitalTableCell)
	{

	}

	@Override public void setOnDragDetected(@NotNull CapitalTableCell<T> capitalTableCell, MouseEvent event)
	{
		popup.clear();

		String cellHoverDisability =
				validateCellForDnD.findIfCellIsAcceptable(capitalTableCell);

		if (cellHoverDisability == null) {
			Integer index = capitalTableCell.getIndex();
			DragStyleSetter<T>
					dragStyleSetter =
					new DragStyleSetter<T>(table, capitalTableCell.getTableView(), blockFinder, validateCellForDnD,
							canExtendSelections());
			dragStyleSetterProvider.add(dragStyleSetter);
			Collection<T> itemsSelectedForDrag =
					dragStyleSetter
							.setStyleOnDragStart(capitalTableCell);

			handleItemsSelectedForDrag(itemsSelectedForDrag);

			Dragboard db = capitalTableCell.startDragAndDrop(TransferMode.ANY);
			PinListAddPinHelper.DragboardWrapper dbWrapper = getDragboardWrapper(db);
			Map<DataFormat, Object> clipboardContent = new ClipboardContent();
			clipboardContent.put(SERIALIZED_MIME_TYPE, index);
			dbWrapper.setClipboardContent(clipboardContent);
			event.consume();
		}
	}

	protected abstract boolean canExtendSelections();

	protected abstract void handleItemsSelectedForDrag(Collection<T> itemsSelectedForDrag);

	@Override public void setOnMouseDragEntered(@NotNull CapitalTableCell<T> capitalTableCell, MouseDragEvent event)
	{

	}

	@Override public void setOnMouseDragReleased(MouseDragEvent event)
	{

	}

	@Override public void setOnMouseDragExited(@NotNull CapitalTableCell<T> capitalTableCell, MouseDragEvent event)
	{

	}

	@Override public void setOnDragDone(CapitalTableCell<T> capitalTableCell, DragEvent event)
	{
		DragStyleSetter<T> dragStyleSetter =
				dragStyleSetterProvider.isEmpty() ? null : dragStyleSetterProvider.get(0);
		if (dragStyleSetter != null) {
			dragStyleSetter.clearDragSelections();
		}
		dragStyleSetterProvider.clear();
	}

	@Override public void setOnDragDropped(CapitalTableCell<T> capitalTableCell, DragEvent event)
	{
		popup.clear();
		capitalTableCell.setCursor(Cursor.DEFAULT);
		Dragboard db = event.getDragboard();
		DragStyleSetter<T> dragStyleSetter =
				dragStyleSetterProvider.get(0);
		if (dragStyleSetter != null) {
			dragStyleSetter.clearDragSelections();
		}
		dragStyleSetterProvider.clear();

		if (getDragboardWrapper(db).hasContent(SERIALIZED_MIME_TYPE)) {
			try {
				int draggedIndex = (Integer) getDragboardWrapper(db).getContent(SERIALIZED_MIME_TYPE);
				//noinspection unchecked
				CapitalTableCell<T> targetCell = (CapitalTableCell<T>) event.getGestureTarget();
				//noinspection unchecked
				CapitalTableCell<T> sourceCell = (CapitalTableCell<T>) event.getGestureSource();
				T sourceItem = table.getData().get(draggedIndex);
				T targetItem = targetCell.getRowItem();
				String cellHoverDisability =
						validateCellForDnD.areCompatibleForDragAndDrop(sourceItem, targetItem, targetCell);

				if (cellHoverDisability != null) {
					return;
				}

				doWhatEverIsRequired(
						targetCell,
						draggedIndex,
						sourceCell.getTableColumn(),
						event.getSource());
			}
			finally {
				event.setDropCompleted(true);
				event.consume();
			}

			capitalTableCell.getTableView().refresh();
		}
	}

	@Override public void setOnDragOver(CapitalTableCell<T> capitalTableCell, DragEvent event)
	{
		Dragboard db = event.getDragboard();
		PinListAddPinHelper.DragboardWrapper dragboardWrapper = getDragboardWrapper(db);
		if (dragboardWrapper.hasContent(SERIALIZED_MIME_TYPE)) {
			adjustVScroll(event.getSceneY(), capitalTableCell.getHeight());
			if (capitalTableCell.getIndex() != (Integer) dragboardWrapper.getContent(SERIALIZED_MIME_TYPE)) {
				dragboardWrapper.acceptTransferModes(event);
				event.consume();
			}
		}
	}

	@Override public void setOnDragEntered(CapitalTableCell<T> capitalTableCell, DragEvent event)
	{
		Dragboard db = event.getDragboard();

		PinListAddPinHelper.DragboardWrapper dragboardWrapper = getDragboardWrapper(db);
		if (dragboardWrapper.hasContent(SERIALIZED_MIME_TYPE) && dragboardWrapper.isValidForCell(capitalTableCell)) {

			//noinspection unchecked
			CapitalTableCell<T> sourceCell = (CapitalTableCell<T>) event.getGestureSource();
			//noinspection unchecked
			CapitalTableCell<T> hoverCell = ((CapitalTableCell<T>) event.getSource());

			int draggedIndex = (Integer) dragboardWrapper.getContent(SERIALIZED_MIME_TYPE);
			T sourceRowItem = table.getData().get(draggedIndex);
			ColumnInformation<T> tableTargetColumn = hoverCell.getColumn();
			ColumnInformation<T> sourceColumn = sourceCell.getColumn();

			double xCoord = event.getScreenX();
			double yCoord = event.getScreenY();

			String cellHoverDisability =
					validateCellForDnD.areCompatibleForDragAndDrop(sourceRowItem, hoverCell.getRowItem(), hoverCell);

			if (sourceColumn == null || tableTargetColumn == null) {
				popup.show(xCoord, yCoord, "Source and destination columns not found for drag operation",
						hoverCell);
			}
			else if (cellHoverDisability == null) {
				if (StringUtils.equals(sourceColumn.getName(), tableTargetColumn.getName())) {
					popup.showSwap(xCoord, yCoord, hoverCell);
				}
				else {
					popup.showMove(xCoord, yCoord, hoverCell);
				}

				DragStyleSetter<T> dragStyleSetter =
						dragStyleSetterProvider.get(0);
				dragStyleSetter
						.setStyleOnDragHover(hoverCell);
			}
			else {

				popup.show(xCoord, yCoord, cellHoverDisability, hoverCell);
			}

			event.consume();
		}
	}

	@Override public void setOnDragExited(CapitalTableCell<T> capitalTableCell, DragEvent event)
	{
		Dragboard db = event.getDragboard();
		PinListAddPinHelper.DragboardWrapper dbWrapper = getDragboardWrapper(db);

		adjustVScroll(event.getSceneY(), capitalTableCell.getHeight());

		if (dbWrapper.isValidForCell(capitalTableCell)) {

			DragStyleSetter<?> dragStyleSetter =
					dragStyleSetterProvider.isEmpty() ? null :
							dragStyleSetterProvider.get(0);
			if (dragStyleSetter != null) {
				dragStyleSetter
						.setStyleOnDragExited();
			}

			event.consume();
		}
	}

	@Override public void changed(Selection<T> selection, int lastSelectionIndex)
	{

	}

	protected PinListAddPinHelper.DragboardWrapper getDragboardWrapper(Dragboard db)
	{
		return new PinListAddPinHelper.DragboardWrapper(db);
	}

	public abstract static class DragBlockFinder<T>
	{

		protected enum DragOrDrop
		{
			DRAG,
			DROP
		}

		protected abstract int getEndBlockIndex(CapitalTableCell<T> cell, DragOrDrop val);

		protected abstract int getStartBlockIndex(CapitalTableCell<T> cell, DragOrDrop val);
	}

	protected static class DragStyleSetter<T>
	{

		private int dragBlockEnd = -1;
		private int dragBlockStart = -1;
		private Map<CapitalTableCell<T>, String> cellsUpdated = new HashMap<>();
		private DragBlockFinder<T> blockFinder;
		private ValidateDnDBetweenCells<T> validateDnDBetweenCells;
		private Table<T> table;
		private TableView<T> tableView;
		private boolean canExtendedSelection;

		DragStyleSetter(Table<T> table, TableView<T> tableView, DragBlockFinder<T> blockFinder,
				ValidateDnDBetweenCells<T> validateDnDBetweenCells, boolean canExtendSelection)
		{
			this.table = table;
			this.tableView = tableView;
			this.blockFinder = blockFinder;
			this.validateDnDBetweenCells = validateDnDBetweenCells;
			canExtendedSelection = canExtendSelection;
		}

		Collection<T> setStyleOnDragStart(
				CapitalTableCell<T> cell)
		{

			List<CellSelection<T>> selectedCells = table.getSelection().getSelectedCells();
			Set<Integer> selectedRowIndices = new LinkedHashSet<>(selectedCells.size());
			for (CellSelection<T> cellSelection : selectedCells) {
				selectedRowIndices.add(cellSelection.getRowIndex());
			}

			cell.getTableView().getSelectionModel().clearSelection();

			dragBlockEnd = blockFinder.getEndBlockIndex(cell, DragBlockFinder.DragOrDrop.DRAG);

			dragBlockStart = blockFinder.getStartBlockIndex(cell, DragBlockFinder.DragOrDrop.DRAG);

			Collection<T> itemsSelectedForDrag = new ArrayList<>();
			for (int i = dragBlockStart; i < dragBlockEnd + 1; i++) {
				if (canExtendedSelection || selectedRowIndices.contains(i)) {

					itemsSelectedForDrag.add(table.getData().get(i));

					tableView.getSelectionModel().select(i, cell.getTableColumn());
				}
			}
			return itemsSelectedForDrag;
		}

		void setStyleOnDragExited()
		{

			for (CapitalTableCell<T> aCell : cellsUpdated.keySet()) {

				aCell.setStyle(cellsUpdated.get(aCell));
				aCell.getTableColumn();

				tableView.getSelectionModel().clearSelection(aCell.getIndex(), aCell.getTableColumn());
			}
			cellsUpdated.clear();
		}

		void setStyleOnDragHover(CapitalTableCell<T> cell)
		{

			int dragBlockHoverEnd = blockFinder.getEndBlockIndex(cell, DragBlockFinder.DragOrDrop.DROP);

			int dragBlockHoverStart = blockFinder.getStartBlockIndex(cell, DragBlockFinder.DragOrDrop.DROP);

			for (int i = dragBlockHoverStart; i < dragBlockHoverEnd + 1; i++) {

				tableView.getSelectionModel().select(i, cell.getTableColumn());
			}
		}

		boolean updateSelected(CapitalTableCell<T> cell)
		{
			if (dragBlockStart == -1 || dragBlockEnd == -1) {
				return false;
			}
			if (cell.getIndex() <= dragBlockEnd && cell.getIndex() >= dragBlockStart) {
				return false;
			}
			if (validateDnDBetweenCells.findIfCellIsAcceptable(cell) != null) {
				cellsUpdated.put(cell, cell.getStyle());
				cell.setStyle(null);
			}
			else {
				String stlye = cell.getStyle();
				if (!cellsUpdated.containsKey(cell)) {
					cellsUpdated.put(cell, cell.getStyle());
					stlye = (StringUtils.isEmpty(stlye) ? "-fx-background-color: gold;" :
							stlye + ";" + "-fx-background-color: gold;");
					cell.setStyle(stlye);
				}
			}
			return true;
		}

		void clearDragSelections()
		{
			for (CapitalTableCell<T> aCell : cellsUpdated.keySet()) {
				aCell.setStyle(cellsUpdated.get(aCell));
			}

			tableView.getSelectionModel().clearSelection();
		}
	}

	protected abstract static class ValidateDnDBetweenCells<T>
	{

		@Nullable
		protected abstract String findIfCellIsAcceptable(@Nullable ColumnInformation<T> columnInformation,
				@Nullable T item);

		@Nullable
		private String findIfCellIsAcceptable(CapitalTableCell<T> cell)
		{

			return findIfCellIsAcceptable(cell.getColumn(), cell.getRowItem());
		}

		protected abstract String areCompatibleForDragAndDrop(
				T givensourceCellItem, @Nullable T givenhoverCellItem, CapitalTableCell<T> givenHoverCell);
	}

	private void adjustVScroll(double currentY, double cellHeight)
	{

		if (vBar != null) {
			double value = vBar.getValue();
			if ((currentY < cellHeight + headerRowHeight) && value > 0) {

				vBar.decrement();
			}
			if ((vBar.getHeight() + headerRowHeight) - currentY < cellHeight) {

				vBar.increment();
			}
		}
	}

	public abstract static class PopupHandler
	{

		@Nullable protected CapitalTableCell<?> currentCell;
		protected final Popup popup = new Popup();
		protected Label popupLabel;
		protected BiConsumer<Popup, Boolean> popupDisplay;

		protected abstract void showSwap(double x, double y, TableCell<?, ?> givenCurrentCell);

		protected abstract void showMove(double x, double y, TableCell<?, ?> givenCurrentCell);

		protected PopupHandler(BiConsumer<Popup, Boolean> popupDisplay)
		{

			popupLabel = new Label();
			popupLabel.getStyleClass().add("tooltip");
			popup.setAutoHide(true);
			this.popupDisplay = popupDisplay;

			popup.getContent().addAll(popupLabel);
		}

		protected void show(double x, double y, String text,
				CapitalTableCell<?> givenCurrentCell)
		{

			if (currentCell != givenCurrentCell) {

				popup.setX(x + POPUP_X);
				popup.setY(y + POPUP_Y);

				popupLabel.setText(" " + text + " ");
				popupDisplay.accept(popup, true);

				currentCell = givenCurrentCell;
			}
		}

		void clear()
		{
			popupLabel.setText("");
			popup.hide();
			currentCell = null;
		}
	}
}




