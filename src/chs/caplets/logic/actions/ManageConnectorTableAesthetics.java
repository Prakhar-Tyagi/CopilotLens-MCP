/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions;

import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.cell.AutoComboBoxCellBuilder;
import com.mentor.capital.javafx.table.cell.AutoCompleteCellControlCreator;
import com.mentor.capital.javafx.table.cell.ComboBoxCellBuilder;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.NonEditableControlCreator;
import com.mentor.capital.javafx.table.cell.TableColumnType;
import com.mentor.capital.javafx.table.helpers.EditControl;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManageConnectorTableAesthetics
{

	//	private static String notEditableStyle = "-fx-background-color:rgb(159,159,159)";
	private static TableColumnType nonEditableCellType = new NonEditableCellType();

	public static final String NON_EDITABLE_STYLE = "-fx-text-fill: rgb(159,159,159);-fx-font-weight: bold;";

	private static IControlCreator nonEditableCreator = new NonEditableControlCreator()
	{
		@Nullable @Override public Node createRenderer(IGenericTableCell<?> cell)
		{
			if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {
				ManageConnectorConnectionsInfo item = (ManageConnectorConnectionsInfo) cell.getRowItem();

				if (!item.isEditable()) {
					Label label = new Label();
					label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

					label.setStyle("-fx-text-fill: rgb(159,159,159)");
					return label;
				}
			}
			return super.createRenderer(cell);
		}

		@Override public void updateValue(Node control, IGenericTableCell<?> cell)
		{
			if (control instanceof Labeled && cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {

				ManageConnectorConnectionsInfo item = (ManageConnectorConnectionsInfo) cell.getRowItem();
				control.setStyle(null);
				if (!item.isEditable()) {

					control.setStyle(NON_EDITABLE_STYLE);
				}
				setDisabledToolTipText((Labeled) control, item);

				((Labeled) control).setText(stringify(cell.getValue()));
				return;
			}

			super.updateValue(control, cell);
		}
	};

	public static TableColumnType getNonEditableCellType()
	{
		return nonEditableCellType;
	}

	private static class NonEditableCellType implements TableColumnType
	{

		@Override public IControlCreator getControlCreator()
		{
			return nonEditableCreator;
		}

		@Nullable @Override public Object getDefaultValue()
		{
			return "";
		}
	}

	public static IControlCreator getControlCreatorForPinNameColumn(
			Function<IGenericTableCell<?>, Collection<?>> possibleValues,
			@Nullable ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			@Nullable ManageConnectorPinSelections manageConnectorPinSelections, @Nullable ISharedPinList sharedPinlist)
	{
		Map<IDesignDescriptor, ObservableList<Object>> comboBoxEntriesForADesign = new LinkedHashMap<>();
		return new AutoCompleteCellControlCreator(possibleValues)
		{
			@NotNull protected ComboBoxCellBuilder getComboBoxCellBuilder()
			{
				return new AutoComboBoxCellBuilder()
				{
					protected void commit(KeyEvent event, ITableCell<?> cell, ComboBox<Object> comboBox)
					{

						if (event.getCode() == KeyCode.ENTER) {
							if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {
								Collection<String> requiredPossibleValues =
										new HashSet<>(possibleValues.apply(cell)
												.stream().map(t -> t.toString()).collect(Collectors.toList()));
								String value = comboBox.getSelectionModel().getSelectedItem() != null ?
										comboBox.getSelectionModel().getSelectedItem().toString() : null;
								String cellValue = cell.getValue() != null ? cell.getValue().toString() : "";
								if (value != null && requiredPossibleValues.contains(value) &&
										!StringUtils.equals(value, cellValue)) {
									try (ManageConnectorReplaceAllCollector temp = new ManageConnectorReplaceAllCollector()) {

										super.commit(event, cell, comboBox);
										return;
									}
								}
							}
							cell.cancelEdit();
						}

						else {
							super.commit(event, cell, comboBox);
						}
					}

					public void updateValue(Node control, ITableCell<?> cell, @Nullable ObservableList<Object> items)
					{
						if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {
							ManageConnectorConnectionsInfo manageConnectorConnectionsInfo =
									(ManageConnectorConnectionsInfo) cell.getRowItem();

							IDesignDescriptor designDescriptor = manageConnectorConnectionsInfo.getDesign();
							if (designDescriptor != null && manageConnectorPinSelections != null &&
									sharedPinlist != null) {

								Collection<Comparable<?>> applicablePins =
										manageConnectorPinSelections
												.getNotationsForSharedPinsApplicableInDesign(designDescriptor);

								if (items != null) {
									items.retainAll(applicablePins);
								}
							}
						}
						super.updateValue(control, cell, items);
					}
				};
			}

			@NotNull @Override public EditControl createEditor(ITableCell<?> cell)
			{
				if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {

					if (((ManageConnectorConnectionsInfo) (cell.getRowItem())).isEditable()) {
						final EditControl editControl = super.createEditor(cell);
						final Node node = editControl.getNode();
						if (node instanceof ComboBox<?>) {
							((ComboBox<?>) node).getSelectionModel().selectedItemProperty().addListener(
									new ChangeListener<Object>()
									{

										@Override public void changed(ObservableValue<?> observable, Object oldValue,
												Object newValue)
										{
											String oldValueString = oldValue != null ? oldValue.toString() : "";
											String newValueString = newValue != null ? newValue.toString() : "";
											Collection<String> possibleStringValues =
													new HashSet<>(possibleValues.apply(cell)
															.stream().map(t -> t.toString())
															.collect(Collectors.toList()));
											if (possibleStringValues.contains(newValueString) &&
													!StringUtils.equals(oldValueString, newValueString)) {
												try (ManageConnectorReplaceAllCollector temp = new ManageConnectorReplaceAllCollector()) {

													if (newValue != null && !newValue.equals(cell.getValue())) {
														cell.commitEdit(newValue);
													}
												}
											}
										}
									});

							((ComboBoxBase<?>) node).setEditable(true);
							((ComboBox<?>) node).getEditor().setEditable(true);
							((Region) node).setMaxSize(Double.MAX_VALUE, ((Region) node).getMaxHeight());
						}
						return editControl;
					}
				}
				return new EditControl()
				{
				};
			}

			@Nullable @Override public Node createRenderer(IGenericTableCell<?> cell)
			{
				if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {

					Label label = new Label();
					label.getStyleClass().add("capital-table-label");
					label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

					return label;
				}

				return super.createRenderer(cell);
			}

			@Override public void updateValue(Node control, IGenericTableCell<?> cell)
			{
				if (control instanceof Labeled) {

					if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {
						ManageConnectorConnectionsInfo item = (ManageConnectorConnectionsInfo) cell.getRowItem();
						Collection<String> possibleStringValues =
								new HashSet<>(possibleValues.apply(cell)
										.stream().map(t -> t.toString()).collect(Collectors.toList()));
						if (cell.getValue() != null && !StringUtils.isBlank(cell.getValue().toString())) {
							control.setStyle(null);
						}

						if (!item.isEditable()) {

							control.setStyle(NON_EDITABLE_STYLE);
						}

						else if (cell.getValue() != null && possibleStringValues.contains(cell.getValue().toString()) &&
								manageConnectorPinDuplicationFinder != null &&
								manageConnectorPinDuplicationFinder.isDuplicate(item.getFirst(), item.getDesign())) {
							control.setStyle("-fx-text-fill: red");
						}

						setToolTipText((Labeled) control, item);

						((Labeled) control).setText(stringify(cell.getValue()));
					}
				}
				else {

					if (!StringUtils.isBlank(cell.getValue().toString())) {
						super.updateValue(control, cell);
					}
				}
			}

			@NotNull protected ObservableList<Object> getItems(ITableCell<?> cell)
			{
				if (cell.getRowItem() instanceof ManageConnectorConnectionsInfo) {
					ManageConnectorConnectionsInfo rowItem = (ManageConnectorConnectionsInfo) cell.getRowItem();
					IDesignDescriptor rowDesign = rowItem.getDesign();
					if (comboBoxEntriesForADesign.get(rowDesign) != null) {
						return comboBoxEntriesForADesign.get(rowDesign);
					}
					if (rowDesign != null && manageConnectorPinSelections != null &&
							sharedPinlist != null) {

						Collection<Comparable<?>> applicablePins =
								manageConnectorPinSelections
										.getNotationsForSharedPinsApplicableInDesign(rowDesign);
						Collection<?> pinsToBeDisplayed = possibleValues.apply(cell);
						Collection<?> requiredApplicablePins = pinsToBeDisplayed.stream()
								.filter(pinToBeDisplayed -> applicablePins.contains(pinToBeDisplayed)).collect(
										Collectors.toList());

						comboBoxEntriesForADesign
								.put(rowDesign, FXCollections.observableArrayList(requiredApplicablePins));
						return comboBoxEntriesForADesign.get(rowDesign);
					}
				}

				return super.getItems(cell);
			}
		};
	}

	private static void setDisabledToolTipText(Labeled control, ManageConnectorConnectionsInfo item)
	{
		if (!item.isEditable()) {
			setToolTipText(control, item);
		}
		else {
			control.setTooltip(null);
		}
	}

	private static void setToolTipText(Labeled control, ManageConnectorConnectionsInfo item)
	{
		Tooltip tooltip = control.getTooltip();
		String currentStatus = item.getCurrentStatus();
		if (tooltip != null) {
			if (StringUtils.isEmpty(currentStatus)) {
				control.setTooltip(null);
			}
			else {
				tooltip.setText(currentStatus);
			}
		}
		else if (!StringUtils.isEmpty(currentStatus)) {
			tooltip = new Tooltip(currentStatus);
			control.setTooltip(tooltip);
		}
	}
}

