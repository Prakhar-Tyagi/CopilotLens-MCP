package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.AbstractDnDHelper;
import chs.caplets.logic.actions.EditDeviceConnectorTableRow;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.cell.CapitalTableCell;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class EditDeviceConnectorDnDHelper extends AbstractDnDHelper<EditDeviceConnectorTableRow>
{

	private BiConsumer<Popup, Boolean> popupDisplay;
	private List<Integer> rowsSelected;

	EditDeviceConnectorDnDHelper(Table<EditDeviceConnectorTableRow> table, BiConsumer<Popup, Boolean> popupDisplay)
	{
		this.table = table;
		this.popupDisplay = popupDisplay;
	}

	@Override
	protected void doWhatEverIsRequired(
			CapitalTableCell<EditDeviceConnectorTableRow> targetCell,
			int sourceRowIndex, TableColumn<?, ?> sourceTableColumn,
			Object source)
	{

		if (EditDeviceConnectorColumns.DEVICECONNECTORPIN.equalsName(targetCell.getColumn())) {

			//noinspection unchecked
			TableView<EditDeviceConnectorTableRow> tableView =
					(TableView<EditDeviceConnectorTableRow>) table.getCenter();

			int targetIndex = targetCell.getIndex();

			ObservableList<EditDeviceConnectorTableRow> tableViewItems = tableView.getItems();
			int nextElementGap = 1;
			List<Integer> orderOfSourceForSwap = new ArrayList<>(rowsSelected);
			if (targetIndex > rowsSelected.get(0)) {
				CollectionUtils.reverse(orderOfSourceForSwap);
				nextElementGap = -1;
				targetIndex += rowsSelected.size() - 1;
			}

			List<CellSelection<EditDeviceConnectorTableRow>> cellSelections = new ArrayList<>();
			for (Integer dragRow : orderOfSourceForSwap) {
				EditDeviceConnectorTableRow sourceRowItem = tableViewItems.get(dragRow);

				EditDeviceConnectorTableRow targetRowItem = tableViewItems.get(targetIndex);
				int requiredTargetIndex = targetIndex;
				cellSelections
						.add(new CellSelection<EditDeviceConnectorTableRow>(targetRowItem, -1, 2, null, null, null));
				Integer[] columnIndex = new Integer[]{0};
				tableView.getColumns().forEach(aTableCol -> {
					//noinspection unchecked
					ColumnInformation<EditDeviceConnectorTableRow> aCol =
							(ColumnInformation<EditDeviceConnectorTableRow>) aTableCol.getUserData();
					if (!EditDeviceConnectorColumns.DEVICEPIN.equalsName(aCol.getName())) {
						Object value = aCol.getReadFunction().apply(targetRowItem);
						String originalTargetVal = value == null ? null : value.toString();
						value = aCol.getReadFunction().apply(sourceRowItem);
						String origSourceStringVal = value == null ? null : value.toString();
						table.setValue(requiredTargetIndex, columnIndex[0], origSourceStringVal, true);
						table.setValue(dragRow, columnIndex[0], originalTargetVal, true);
					}
					columnIndex[0] += 1;
				});
				targetIndex += nextElementGap;
			}

			table.select(new Selection<>(cellSelections));
		}
	}

	@Override protected void handleItemsSelectedForDrag(Collection<EditDeviceConnectorTableRow> itemsSelectedForDrag)
	{

	}

	@Override public void setOnDragDetected(@NotNull CapitalTableCell<EditDeviceConnectorTableRow> capitalTableCell,
			MouseEvent event)
	{
		List<CellSelection<EditDeviceConnectorTableRow>> selectedCells = table.getSelection().getSelectedCells();

		boolean areCellsSelectedNotOfcavitNameCol = selectedCells.stream()
				.filter(aCell -> !EditDeviceConnectorColumns.DEVICECONNECTORPIN
						.equalsName(aCell.getColumnInformation())).findFirst().isPresent();
		if (areCellsSelectedNotOfcavitNameCol) {
			return;
		}

		rowsSelected = new ArrayList<>();
		selectedCells.stream().forEach(aCell -> rowsSelected.add(aCell.getRowIndex()));
		Collections.sort(rowsSelected, (x, y) -> Integer.compare(x, y));
		EditDeviceConnDnDValidateDnDBetweenCells editDeviceConnDnDValidateDnDBetweenCells =
				new EditDeviceConnDnDValidateDnDBetweenCells(rowsSelected, table);
		EditDeviceConnDragBlockFinder reqblockFinder = new EditDeviceConnDragBlockFinder(rowsSelected);
		EditDeviceConnPopupHandler popupHandler = new EditDeviceConnPopupHandler(popupDisplay);
		reset(capitalTableCell, table, editDeviceConnDnDValidateDnDBetweenCells,
				reqblockFinder,
				popupHandler);
		super.setOnDragDetected(capitalTableCell, event);
	}

	@Override protected boolean canExtendSelections()
	{
		return false;
	}

	private static class EditDeviceConnDnDValidateDnDBetweenCells
			extends AbstractDnDHelper.ValidateDnDBetweenCells<EditDeviceConnectorTableRow>
	{

		protected Table<EditDeviceConnectorTableRow> table;
		private List<Integer> rowsSelected;

		EditDeviceConnDnDValidateDnDBetweenCells(List<Integer> rowsSelected,
				Table<EditDeviceConnectorTableRow> givenTable)
		{
			this.rowsSelected = rowsSelected;
			table = givenTable;
		}

		@Nullable @Override protected String findIfCellIsAcceptable(
				@Nullable ColumnInformation<EditDeviceConnectorTableRow> columnInformation,
				@Nullable EditDeviceConnectorTableRow item)
		{
			if (item == null || StringUtils.isBlank(item.getDeviceConnectorPinName()) ||
					StringUtils.isBlank(item.getDeviceConnectorName())) {
				return ResourceMgr.getString(EditDeviceConnectorDnDHelper.class,
						"EditDeviceConnectorAction.dndfailure.incorrectsource");
			}

			return null;
		}

		@Nullable @Override
		protected String areCompatibleForDragAndDrop(EditDeviceConnectorTableRow givensourceCellItem,
				EditDeviceConnectorTableRow givenhoverCellItem,
				CapitalTableCell<EditDeviceConnectorTableRow> givenHoverCell)
		{
			//noinspection unchecked
			TableView<EditDeviceConnectorTableRow> tableView =
					(TableView<EditDeviceConnectorTableRow>) table.getCenter();
			if (givenHoverCell.getIndex() + rowsSelected.size() > tableView.getItems().size()) {

				return ResourceMgr.getString(EditDeviceConnectorDnDHelper.class,
						"EditDeviceConnectorAction.dndfailure.outofrange");
			}
			return null;
		}
	}

	protected static class EditDeviceConnPopupHandler extends AbstractDnDHelper.PopupHandler
	{

		@Override protected void showSwap(double x, double y, TableCell<?, ?> givenCurrentCell)
		{
			show(x, y,
					ResourceMgr.getString(EditDeviceConnectorDnDHelper.class,
							"EditDeviceConnectorAction.dnd.swapconnections"),
					(CapitalTableCell<?>) givenCurrentCell);
		}

		@Override protected void showMove(double x, double y, TableCell<?, ?> givenCurrentCell)
		{

		}

		EditDeviceConnPopupHandler(BiConsumer<Popup, Boolean> popupDisplay)
		{
			super(popupDisplay);
		}
	}

	private static class EditDeviceConnDragBlockFinder
			extends AbstractDnDHelper.DragBlockFinder<EditDeviceConnectorTableRow>
	{

		private List<Integer> selectedRows;

		EditDeviceConnDragBlockFinder(List<Integer> selectedRows)
		{
			this.selectedRows = selectedRows;
		}

		@Override protected int getEndBlockIndex(CapitalTableCell<EditDeviceConnectorTableRow> cell, DragOrDrop val)
		{
			if (val.equals(DragOrDrop.DRAG)) {
				return selectedRows.get(selectedRows.size() - 1);
			}
			else {
				return cell.getIndex() + selectedRows.size() - 1;
			}
		}

		@Override protected int getStartBlockIndex(CapitalTableCell<EditDeviceConnectorTableRow> cell, DragOrDrop val)
		{
			if (val.equals(DragOrDrop.DRAG)) {
				return cell.getIndex() - (cell.getIndex() - selectedRows.get(0));
			}
			else {
				return cell.getIndex();
			}
		}
	}
}
