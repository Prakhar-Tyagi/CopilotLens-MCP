package chs.caplets.logic.actions;

import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryObject;
import chs.cofUtils.parts.PartNumberHelper;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import com.mentor.capital.javafx.table.cell.ITableCell;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class DeviceConnectorNamePartNumberValidator
{

	private Map<String, DeviceConnectorNamePartNumberDetails> connectorNameDetails;

	private Collection<IGenericTableCell<?>> cellsWithNameRelatedError = new HashSet<>();
	private Collection<IGenericTableCell<?>> cellsWithCavityRelatedErrors = new HashSet<>();
	private Set<String> connectorNamesWithDuplicates = new HashSet<>();
	private Set<IFootprintDevicePinKey> dcUnassignedDevicePinNames = new HashSet<>();
	private Collection<IFootprintDevicePinKey> incorrectCavities = new HashSet<>();

	public DeviceConnectorNamePartNumberValidator(Collection<EditDeviceConnectorTableRow> allRows)
	{
		connectorNameDetails = new HashMap<>();

		allRows.forEach(aRow -> {
			if (!StringUtils.isBlank(aRow.getDeviceConnectorName())) {
				DeviceConnectorNamePartNumberDetails dcDetails = connectorNameDetails
						.computeIfAbsent(aRow.getDeviceConnectorName(),
								(aName) -> new DeviceConnectorNamePartNumberDetails());

				String partNumber =
						StringUtils.isBlank(aRow.getDeviceConnectorPartNumber()) ? StringUtils.CTRL_CHAR_SEPARATOR :
								aRow.getDeviceConnectorPartNumber();

				dcDetails.addEditDeviceConnectorTableRow(partNumber);
			}
		});
	}

	public boolean validateConnectorNameChange(@Nullable String oldDCValue, @Nullable String newDCValue,
			@Nullable String givenPartNumber)
	{

		DeviceConnectorNamePartNumberDetails
				newDCDetails = !StringUtils.isBlank(newDCValue) ? connectorNameDetails
				.computeIfAbsent(newDCValue, (dcName) -> new DeviceConnectorNamePartNumberDetails()) : null;
		DeviceConnectorNamePartNumberDetails
				oldDCDetails = !StringUtils.isBlank(oldDCValue) ? connectorNameDetails
				.computeIfAbsent(oldDCValue, (dcName) -> new DeviceConnectorNamePartNumberDetails()) : null;

		if (newDCDetails == null && oldDCDetails == null) {
			return connectorNamesWithDuplicates.isEmpty();
		}

		String partNumber = StringUtils.isBlank(givenPartNumber) ? StringUtils.CTRL_CHAR_SEPARATOR : givenPartNumber;
		boolean newDuplicateFound = false;
		boolean oldDuplicateRemoved = false;
		if (oldDCDetails == null) {

			newDuplicateFound = newDCDetails.addEditDeviceConnectorTableRow(partNumber);
		}
		else if (newDCDetails == null) {

			oldDuplicateRemoved = oldDCDetails.removeEditDeviceConnectorTableRow(partNumber);
		}
		else {

			oldDuplicateRemoved = oldDCDetails.removeEditDeviceConnectorTableRow(partNumber);

			newDuplicateFound = newDCDetails.addEditDeviceConnectorTableRow(partNumber);
		}
		if (oldDuplicateRemoved) {
			connectorNamesWithDuplicates.remove(oldDCValue);
		}
		if (newDuplicateFound) {
			connectorNamesWithDuplicates.add(newDCValue);
		}
		return connectorNamesWithDuplicates.isEmpty();
	}

	public boolean validateConnectorPartnumberChange(String connectorName, @Nullable String givenOldValue,
			@Nullable String givenNewValue)
	{
		if (StringUtils.isBlank(givenOldValue) && StringUtils.isBlank(givenNewValue)) {
			return connectorNamesWithDuplicates.isEmpty();
		}
		String newValue = StringUtils.isBlank(givenNewValue) ? StringUtils.CTRL_CHAR_SEPARATOR : givenNewValue;
		String oldValue = StringUtils.isBlank(givenOldValue) ? StringUtils.CTRL_CHAR_SEPARATOR : givenOldValue;

		DeviceConnectorNamePartNumberDetails dcPartNumberDetails = connectorNameDetails
				.computeIfAbsent(connectorName, (dcName) -> new DeviceConnectorNamePartNumberDetails());

		boolean oldDuplicateRemoved = dcPartNumberDetails.removeEditDeviceConnectorTableRow(oldValue);
		boolean newDuplicateFound = dcPartNumberDetails.addEditDeviceConnectorTableRow(newValue);

		if (oldDuplicateRemoved) {
			connectorNamesWithDuplicates.remove(connectorName);
		}

		if (newDuplicateFound) {
			connectorNamesWithDuplicates.add(connectorName);
		}
		return connectorNamesWithDuplicates.isEmpty();
	}

	@Nullable public String checkDuplicate(@Nullable String dcName)
	{
		DeviceConnectorNamePartNumberDetails dcDetails = connectorNameDetails.get(dcName);
		if (dcDetails != null) {
			Collection<String> partNumbers = dcDetails.getPartNumbers();
			if (partNumbers.size() > 1) {

				StringJoiner stringify = new StringJoiner(",", "[", "]");
				partNumbers.forEach(aPartNumber -> {
					if (StringUtils.CTRL_CHAR_SEPARATOR.equals(aPartNumber)) {
						stringify.add("<>");
					}
					else {
						stringify.add(aPartNumber);
					}
				});
				return ResourceMgr.getString(DeviceConnectorNamePartNumberValidator.class,
						"EditDeviceConnectorAction.okdisabled.multiplepartnumbers", stringify, dcName);
			}
		}
		return null;
	}

	public void addCellsWithNameRelatedErrors(IGenericTableCell<?> cell)
	{
		cellsWithNameRelatedError.add(cell);
	}

	public void removeCellsWithNameRelatedErrors(IGenericTableCell<?> cell)
	{
		cellsWithNameRelatedError.remove(cell);
	}

	public void addCellsWithCavityRelatedErrors(IGenericTableCell<?> cell)
	{
		cellsWithCavityRelatedErrors.add(cell);
	}

	public void removeCellsWithCavityRelatedErrors(IGenericTableCell<?> cell)
	{
		cellsWithCavityRelatedErrors.remove(cell);
	}

	public void refreshCellsWithErrors()
	{
		Platform.runLater(() -> {
			Collection<IGenericTableCell<?>> currentCellsWithError = new HashSet<>(cellsWithNameRelatedError);
			currentCellsWithError.addAll(cellsWithCavityRelatedErrors);
			currentCellsWithError.forEach(aCell -> aCell.updateCellValue(aCell.getValue(), false));
		});
	}

	@Nullable private String getDuplicateError()
	{
		if (!connectorNamesWithDuplicates.isEmpty()) {
			return ResourceMgr.getString(DeviceConnectorNamePartNumberValidator.class,
					"EditDeviceConnectorAction.okdisabled.duplicateconnectornames",
					connectorNamesWithDuplicates.toString());
		}
		return null;
	}

	@Nullable private String getNoNameAssigned()
	{
		if (!dcUnassignedDevicePinNames.isEmpty()) {

			SortedList<String> names = new SortedList<>(dcUnassignedDevicePinNames.size(),
					AlphaNumComparator.getCaseSensitiveComparator());
			dcUnassignedDevicePinNames.forEach((p) -> names.add(p.getName()));

			return ResourceMgr.getString(DeviceConnectorNamePartNumberValidator.class,
					"EditDeviceConnectorAction.okdisabled.connectornamemissing", names.toString());
		}
		return null;
	}

	public void handlePartNumberAndConnectorNameChange(@NotNull EditDeviceConnectorTableRow sourceItem)
	{
		if (!StringUtils.isBlank(sourceItem.getDeviceConnectorPartNumber()) &&
				StringUtils.isBlank(sourceItem.getDeviceConnectorName())) {
			dcUnassignedDevicePinNames.add(sourceItem.getDevicePin());
		}
		else {
			dcUnassignedDevicePinNames.remove(sourceItem.getDevicePin());
		}
	}

	public void handlePartNumberAndCavityNameChange(@NotNull EditDeviceConnectorTableRow sourceItem)
	{
		if (getPartNumberAndCavityMismatch(sourceItem) != null) {
			incorrectCavities.add(sourceItem.getDevicePin());
		}
		else {
			incorrectCavities.remove(sourceItem.getDevicePin());
		}
	}

	@Nullable public String getErrorInCurrentTableState()
	{
		String error = getDuplicateError();
		if (error == null) {
			error = getNoNameAssigned();
		}
		if (error == null) {
			error = getMismatchedCavities();
		}
		return error;
	}

	@Nullable public String getMismatchedCavities()
	{
		if (!incorrectCavities.isEmpty()) {
			SortedList<String> names = new SortedList<>(incorrectCavities.size(),
					AlphaNumComparator.getCaseSensitiveComparator());
			incorrectCavities.forEach((p) -> names.add(p.getName()));
			return ResourceMgr.getString(DeviceConnectorNamePartNumberValidator.class,
					"EditDeviceConnectorAction.okdisabled.incorrectcavities", names.toString());
		}
		return null;
	}

	private static class DeviceConnectorNamePartNumberDetails
	{

		private Map<String, Integer> partNumberReferenceCount;

		DeviceConnectorNamePartNumberDetails()
		{
			partNumberReferenceCount = new HashMap<>();
		}

		boolean addEditDeviceConnectorTableRow(@Nullable String partNumber)
		{
			Integer count = partNumberReferenceCount.computeIfAbsent(partNumber,
					(aPartNumber) -> (0));
			count++;
			partNumberReferenceCount.put(partNumber, count);
			return partNumberReferenceCount.size() > 1;
		}

		boolean removeEditDeviceConnectorTableRow(@Nullable String oldpartNumber)
		{
			Integer count = partNumberReferenceCount.get(oldpartNumber);
			count = count != null ? count - 1 : 0;
			if (count <= 0) {
				partNumberReferenceCount.remove(oldpartNumber);
			}
			else {
				partNumberReferenceCount.put(oldpartNumber, count);
			}
			return partNumberReferenceCount.size() <= 1;
		}

		public Collection<String> getPartNumbers()
		{
			return partNumberReferenceCount.keySet();
		}
	}

	private Map<String, Collection<String>> cavityNamesCache = new HashMap<>();

	@Nullable public String getPartNumberAndCavityMismatch(@NotNull EditDeviceConnectorTableRow rowItem)
	{
		String partNumber = rowItem.getDeviceConnectorPartNumber();
		String cavity = rowItem.getDeviceConnectorPinName();
		if (!StringUtils.isBlank(cavity) && !StringUtils.isBlank(partNumber)) {
			if (!isCavityAppropriteForPN(partNumber, cavity)) {
				return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
						"EditDeviceConnectorAction.cellerror.cavitynotpresentinpartnumber", cavity, partNumber);
			}
		}
		return null;
	}

	private boolean isCavityAppropriteForPN(String partNumber, String cavity)
	{
		Collection<String> cavitiesinConnector = cavityNamesCache.computeIfAbsent(partNumber, aPartNumber -> {

			Collection<String> cavityNames = new HashSet<>();
			ILibraryObject libraryConnector = PartNumberHelper.getLibraryPartFromCombinedPartNumber(partNumber);
			if (libraryConnector instanceof ILibraryCavityContainer) {
				((ILibraryCavityContainer) libraryConnector).getCavities()
						.forEach(aCavity -> cavityNames.add(aCavity.getName()));
			}
			return cavityNames;
		});
		return cavitiesinConnector.contains(cavity);
	}
}
