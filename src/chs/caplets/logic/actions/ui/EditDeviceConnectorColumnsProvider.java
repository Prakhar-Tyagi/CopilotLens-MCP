/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.DeviceConnectorNamePartNumberValidator;
import chs.caplets.logic.actions.DeviceConnectorNamePinNameValidator;
import chs.caplets.logic.actions.EditDeviceConnectorTableRow;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryObject;
import chs.cofUtils.parts.PartNumberHelper;
import chs.utilities.AlphaNumComparator;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ColumnTypeInfo;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.StringCellControlCreator;
import com.mentor.capital.javafx.table.cell.StringColumnType;
import com.mentor.capital.javafx.table.cell.TableAutoCompleteColumnType;
import com.mentor.capital.javafx.table.cell.TableColumnType;
import com.mentor.capital.javafx.table.helpers.EditControl;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import com.mentor.capital.javafx.table.menu.DefaultMenuItem;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EditDeviceConnectorColumnsProvider
{

	private Supplier<DeviceConnectorNamePartNumberValidator> dcNamePNvalidator;
	private Supplier<DeviceConnectorNamePinNameValidator> dcNamePinValidator;

	private Map<String, EditDeviceConnectorUpdateStyle> stylers = new HashMap<>();

	EditDeviceConnectorColumnsProvider(Supplier<DeviceConnectorNamePartNumberValidator> dcNamePNvalidator,
			Supplier<DeviceConnectorNamePinNameValidator> dcNamePinValidator)
	{
		this.dcNamePNvalidator = dcNamePNvalidator;
		this.dcNamePinValidator = dcNamePinValidator;
	}

	ColumnInformation<EditDeviceConnectorTableRow> getColumnByName(String name)
	{
		EditDeviceConnectorColumns column = EditDeviceConnectorColumns.getColumnByName(name);
		assert column != null;
		return column.getColumn(this);
	}

	@Nullable private String toString(@Nullable Object obj)
	{
		return obj != null ? obj.toString() : null;
	}

	Collection<ColumnInformation<EditDeviceConnectorTableRow>> getColumns()
	{
		Collection<ColumnInformation<EditDeviceConnectorTableRow>> columns = new ArrayList<>();
		columns.add(createDeviceConnectorNameColumn());
		columns.add(createDeviceConnectorPartNumberColumn());
		columns.add(createDeviceConnectorCavityNameColumn());
		columns.add(createDevicePinColumn());
		return columns;
	}

	@Nullable EditDeviceConnectorUpdateStyle getColumnStyler(String name)
	{
		return stylers.get(name);
	}

	protected ColumnInformation<EditDeviceConnectorTableRow> createDevicePinColumn()
	{
		Function<EditDeviceConnectorTableRow, Object> readMethod =
				editDeviceConnectorTableRow -> editDeviceConnectorTableRow.getDevicePinName();

		ColumnInformation<EditDeviceConnectorTableRow> devicePinNameColumn =
				new ColumnInformation<EditDeviceConnectorTableRow>(
						EditDeviceConnectorColumns.DEVICEPIN.getDisplayName(),
						EditDeviceConnectorColumns.DEVICEPIN.getName(),
						readMethod)
				{
					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}
				};

		return devicePinNameColumn;
	}

	private static void indicateChangeOrErrorOnCell(EditDeviceConnectorTableRow itemData, IGenericTableCell<?> cell,
			@Nullable String error,
			Control stylableNode)
	{
		ColumnInformation<?> column = cell.getColumn();
		assert column != null;
		String change = itemData.getChange(column.getName());

		if (!StringUtils.isBlank(error)) {
			String errorBorderColor = cell.isSelected() ? "-fx-border-color: #3296B9;" : "-fx-border-color: red;";
			if (cell.getValue() == null || StringUtils.isEmpty(cell.getValue().toString())) {
				if (change != null || itemData.isUpdated(cell.getColumn().getName())) {
					stylableNode.setStyle(errorBorderColor + " -fx-border-width: 1px;");
				}
			}
			else {
				stylableNode.setStyle(errorBorderColor + " -fx-font-style: italic; -fx-border-width: 1px;");
			}
		}
		else if (!StringUtils.isBlank(change)) {
			String accurateBorderColor = cell.isSelected() ? "-fx-border-color: #3296B9;" : "-fx-border-color: green;";
			stylableNode.setStyle(accurateBorderColor + " -fx-border-width: 1px;");
		}
		String tooltipToDisplay =
				!StringUtils.isEmpty(error) ? error : !StringUtils.isBlank(change) ? change : null;
		if (tooltipToDisplay != null) {

			Tooltip tooltip = stylableNode.getTooltip();
			if (tooltip == null) {
				tooltip = new Tooltip(tooltipToDisplay);
				stylableNode.setTooltip(tooltip);
			}
			else {
				tooltip.setText(tooltipToDisplay);
			}
		}
		else {

			stylableNode.setTooltip(null);
		}
	}

	@Nullable private static String handleDCCavityAndNameRelatedErrors(EditDeviceConnectorTableRow itemData,
			IGenericTableCell<?> cell,
			Supplier<DeviceConnectorNamePinNameValidator> validatorSupplier, @Nullable String errorAlreadyFound)
	{
		String error = itemData.getDCNameNotAssignedAndCavityNameAssigned();
		if (error == null) {
			error = itemData.getDCNameAssignedAndCavityNameNotAssigned();
		}
		if (error == null) {
			error = validatorSupplier.get()
					.getDuplicatePinError(
							itemData.getDeviceConnectorName(),
							itemData.getDeviceConnectorPinName());
		}
		if (error == null && itemData.getDeviceConnectorName() != null) {
			error = validatorSupplier.get().getCaseInconsistentConnectorName(itemData.getDeviceConnectorName());
		}
		if (error != null) {
			validatorSupplier.get().addCellWithError(cell);
		}
		else if (errorAlreadyFound == null) {
			validatorSupplier.get().removeCellWithError(cell);
		}
		if (errorAlreadyFound != null) {
			return errorAlreadyFound;
		}
		return error;
	}

	@Nullable private static String handleConnectorNameAndPNRelatedErrors(EditDeviceConnectorTableRow itemData,
			IGenericTableCell<?> cell,
			Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator, @Nullable String errorAlreadyFound)
	{

		String error = itemData.getDCNameNotAssignedAndPNAssigned();
		if (error == null) {
			error = partNumberValidator.get()
					.checkDuplicate(itemData.getDeviceConnectorName());
		}
		if (error != null) {
			partNumberValidator.get().addCellsWithNameRelatedErrors(cell);
		}
		else if (errorAlreadyFound == null) {
			partNumberValidator.get().removeCellsWithNameRelatedErrors(cell);
		}
		return error;
	}

	@Nullable private static String handleCavityNameAndPNRelatedErrors(EditDeviceConnectorTableRow itemData,
			IGenericTableCell<?> cell,
			Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator)
	{

		String error = partNumberValidator.get().getPartNumberAndCavityMismatch(itemData);

		if (error != null) {
			partNumberValidator.get().addCellsWithCavityRelatedErrors(cell);
		}
		else {
			partNumberValidator.get().removeCellsWithCavityRelatedErrors(cell);
		}
		return error;
	}

	public interface EditDeviceConnectorUpdateStyle
	{

		void updateStyle(@NotNull IGenericTableCell<?> tableCell, @NotNull Node styleable);
	}

	private static class DCNameStyler implements EditDeviceConnectorUpdateStyle
	{

		private Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator;
		private Supplier<DeviceConnectorNamePinNameValidator> pinNameValidator;

		DCNameStyler(Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator,
				Supplier<DeviceConnectorNamePinNameValidator> pinNameValidator)
		{
			this.pinNameValidator = pinNameValidator;
			this.partNumberValidator = partNumberValidator;
		}

		@Override public void updateStyle(@NotNull IGenericTableCell<?> tableCell, @NotNull Node styleable)
		{
			styleable.setStyle(null);
			Object itemData = tableCell.getRowItem();
			if (itemData instanceof EditDeviceConnectorTableRow) {
				String error = handleConnectorNameAndPNRelatedErrors((EditDeviceConnectorTableRow) itemData, tableCell,
						partNumberValidator, null);
				error = handleDCCavityAndNameRelatedErrors((EditDeviceConnectorTableRow) itemData, tableCell,
						pinNameValidator, error);
				if (error == null) {
					error = ((EditDeviceConnectorTableRow) itemData).getInvalidNameLength();
				}
				if (styleable instanceof Control) {
					indicateChangeOrErrorOnCell((EditDeviceConnectorTableRow) itemData, tableCell, error,
							(Control) styleable);
				}
			}
		}
	}

	private static class DCPNControlStyler implements EditDeviceConnectorUpdateStyle
	{

		private Supplier<DeviceConnectorNamePartNumberValidator> validatorSupplier;

		DCPNControlStyler(Supplier<DeviceConnectorNamePartNumberValidator> validatorSupplier)
		{
			this.validatorSupplier = validatorSupplier;
		}

		@Override public void updateStyle(@NotNull IGenericTableCell<?> tableCell, @NotNull Node styleable)
		{
			styleable.setStyle(null);
			Object itemData = tableCell.getRowItem();
			if (itemData instanceof EditDeviceConnectorTableRow) {
				String error = handleConnectorNameAndPNRelatedErrors((EditDeviceConnectorTableRow) itemData, tableCell,
						validatorSupplier, null);
				String otherError = handleCavityNameAndPNRelatedErrors((EditDeviceConnectorTableRow) itemData,
						tableCell,
						validatorSupplier);
				if (error == null) {
					error = otherError;
				}
				if (error == null) {
					error = ((EditDeviceConnectorTableRow) itemData).getInvalidPNError();
				}
				if (styleable instanceof Control) {
					indicateChangeOrErrorOnCell((EditDeviceConnectorTableRow) itemData, tableCell, error,
							(Control) styleable);
				}
			}
		}
	}

	private static class DCCavityNameStyler implements EditDeviceConnectorUpdateStyle
	{

		private Supplier<DeviceConnectorNamePinNameValidator> cavityNameValidator;

		private Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator;

		DCCavityNameStyler(
				Supplier<DeviceConnectorNamePinNameValidator> validatorSupplier,
				Supplier<DeviceConnectorNamePartNumberValidator> partNumberValidator)
		{

			cavityNameValidator = validatorSupplier;
			this.partNumberValidator = partNumberValidator;
		}

		@Override public void updateStyle(@NotNull IGenericTableCell<?> tableCell, @NotNull Node styleable)
		{
			styleable.setStyle(null);
			Object itemData = tableCell.getRowItem();
			if (itemData instanceof EditDeviceConnectorTableRow) {
				String error = handleDCCavityAndNameRelatedErrors((EditDeviceConnectorTableRow) itemData, tableCell,
						cavityNameValidator, null);

				String otherError = handleCavityNameAndPNRelatedErrors((EditDeviceConnectorTableRow) itemData,
						tableCell,
						partNumberValidator);

				if (error == null) { //do not move the handleCavity into the error == null condition
					error = otherError;
				}
				if (error == null) {
					error = ((EditDeviceConnectorTableRow) itemData).getInvalidCavityLength();
				}
				if (styleable instanceof Control) {
					indicateChangeOrErrorOnCell((EditDeviceConnectorTableRow) itemData, tableCell, error,
							(Control) styleable);
				}
			}
		}
	}

	protected ColumnInformation<EditDeviceConnectorTableRow> createDeviceConnectorNameColumn()
	{
		Function<EditDeviceConnectorTableRow, Object> readMethod =
				editDeviceConnectorTableRow -> editDeviceConnectorTableRow.getDeviceConnectorName();

		BiConsumer<EditDeviceConnectorTableRow, Object> writeMethod =
				(editDeviceConnectorTableRow, o) -> {
					editDeviceConnectorTableRow.setDeviceConnectorName(toString(o));
				};

		ColumnInformation<EditDeviceConnectorTableRow> deviceConnectorNameColumn =
				new ColumnInformation<EditDeviceConnectorTableRow>(
						EditDeviceConnectorColumns.DEVICECONNECTORNAME.getDisplayName(),
						EditDeviceConnectorColumns.DEVICECONNECTORNAME.getName(),
						readMethod,
						writeMethod,
						new StringColumnType(new ColumnTypeInfo(null)))
				{
					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}
				};

		stylers.put(deviceConnectorNameColumn.getName(), new DCNameStyler(dcNamePNvalidator, dcNamePinValidator));
		return deviceConnectorNameColumn;
	}

	protected ColumnInformation<EditDeviceConnectorTableRow> createDeviceConnectorPartNumberColumn()
	{
		Function<EditDeviceConnectorTableRow, Object> readMethod =
				editDeviceConnectorTableRow -> editDeviceConnectorTableRow.getDeviceConnectorPartNumber();

		BiConsumer<EditDeviceConnectorTableRow, Object> writeMethod =
				(editDeviceConnectorTableRow, o) -> {
					editDeviceConnectorTableRow.setDeviceConnectorPartNumber(toString(o));
				};

		TableColumnType tableColumnTypeForPN = new StringColumnType(
				null)
		{
			@Override public IControlCreator getControlCreator()
			{
				return new StringCellControlCreator()
				{
					@NotNull @Override public EditControl createEditor(ITableCell<?> cell)
					{
						final TextField textField = new TextField();
						textField.setEditable(false);

						return new EditControl()
						{
							@Override public Node getNode()
							{
								return textField;
							}
						};
					}
				};
			}
		};

		ColumnInformation<EditDeviceConnectorTableRow> deviceConnectorPNColumn =
				new ColumnInformation<EditDeviceConnectorTableRow>(
						EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.getDisplayName(),
						EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.getName(),
						readMethod,
						writeMethod,
						tableColumnTypeForPN)
				{
					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}
				};

		stylers.put(deviceConnectorPNColumn.getName(), new DCPNControlStyler(dcNamePNvalidator));
		return deviceConnectorPNColumn;
	}

	protected ColumnInformation<EditDeviceConnectorTableRow> createDeviceConnectorCavityNameColumn()
	{

		Function<EditDeviceConnectorTableRow, Object> readMethod =
				editDeviceConnectorTableRow -> editDeviceConnectorTableRow.getDeviceConnectorPinName();

		BiConsumer<EditDeviceConnectorTableRow, Object> writeMethod =
				(editDeviceConnectorTableRow, o) -> {
					editDeviceConnectorTableRow.setDeviceConnectorPinName(toString(o));
				};

		Function<IGenericTableCell<?>, Collection<?>> possibleValues = new Function<IGenericTableCell<?>, Collection<?>>()
		{
			@Override public Collection<?> apply(IGenericTableCell<?> t)
			{
				EditDeviceConnectorTableRow row = (EditDeviceConnectorTableRow) t.getRowItem();
				if (row != null) {
					String partNumber = row.getDeviceConnectorPartNumber();
					ILibraryObject libraryObject = PartNumberHelper.getLibraryPartFromCombinedPartNumber(partNumber);
					if (libraryObject instanceof ILibraryCavityContainer) {
						List<String> cavityNames = ((ILibraryCavityContainer) libraryObject).getCavities().stream()
								.map(aCavity -> aCavity.getName()).collect(
										Collectors.toList());
						Collections.sort(cavityNames, AlphaNumComparator.getUniqueObjectAsUniqueComparator());
						return cavityNames;
					}
				}
				return Collections.emptyList();
			}
		};

		TableColumnType tableColumnTypeForPinName = new TableAutoCompleteColumnType(
				null, possibleValues);

		ColumnInformation<EditDeviceConnectorTableRow> deviceConnectorCavityNameColumn =
				new ColumnInformation<EditDeviceConnectorTableRow>(
						EditDeviceConnectorColumns.DEVICECONNECTORPIN.getDisplayName(),
						EditDeviceConnectorColumns.DEVICECONNECTORPIN.getName(),
						readMethod,
						writeMethod,
						tableColumnTypeForPinName)
				{
					@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
					{
						return defaultMenuItemKey != DefaultMenuItem.Hide;
					}

					@Override public boolean isDraggable()
					{
						return true;
					}
				};

		stylers.put(deviceConnectorCavityNameColumn.getName(),
				new DCCavityNameStyler(dcNamePinValidator, dcNamePNvalidator));
		return deviceConnectorCavityNameColumn;
	}
}
