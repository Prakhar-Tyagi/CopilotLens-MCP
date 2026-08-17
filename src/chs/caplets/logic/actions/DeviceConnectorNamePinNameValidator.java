package chs.caplets.logic.actions;

import chs.common.ISystemPreferenceMgr;
import chs.system.CHSSystemMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeviceConnectorNamePinNameValidator
{

	private Map<String, DeviceConnectorPinDetails> connectorNameDetails;

	private Map<IFootprintDevicePinKey, String> devicePinNamesWithNoDCName = new HashMap<>();
	private Map<IFootprintDevicePinKey, String> devicePinNamesWithNoCavities = new HashMap<>();

	private NameCaseConsistencyChecker nameCaseConsistencyChecker;
	private Set<String> connectorNamesWithDuplicates = new LinkedHashSet<>();

	private Collection<IGenericTableCell<?>> cellsWithErrors = new LinkedHashSet<>();

	public DeviceConnectorNamePinNameValidator(Collection<EditDeviceConnectorTableRow> allRows)
	{
		connectorNameDetails = new HashMap<>();
		nameCaseConsistencyChecker = new NameCaseConsistencyChecker(false);
		allRows.forEach(aRow -> {
			String connectorName = aRow.getDeviceConnectorName();
			if (!StringUtils.isBlank(connectorName)) {
				DeviceConnectorPinDetails dcDetails = connectorNameDetails
						.computeIfAbsent(aRow.getDeviceConnectorName(),
								(aName) -> new DeviceConnectorPinDetails());

				nameCaseConsistencyChecker.add(connectorName);

				if (!StringUtils.isBlank(aRow.getDeviceConnectorPinName())) {
					dcDetails.addEditDeviceConnectorTableRow(aRow.getDeviceConnectorPinName());
				}
			}
		});
	}

	@SuppressWarnings("unused")
	public boolean validateConnectorNameChange(@Nullable String oldDCValue, @Nullable String newDCValue,
			@NotNull EditDeviceConnectorTableRow rowItem)
	{
		String pinName = rowItem.getDeviceConnectorPinName();
		DeviceConnectorPinDetails
				newDCDetails = !StringUtils.isBlank(newDCValue) ? connectorNameDetails
				.computeIfAbsent(newDCValue, (dcName) -> new DeviceConnectorPinDetails()) : null;
		DeviceConnectorPinDetails
				oldDCDetails = !StringUtils.isBlank(oldDCValue) ? connectorNameDetails
				.computeIfAbsent(oldDCValue, (dcName) -> new DeviceConnectorPinDetails()) : null;

		if (newDCDetails == null && oldDCDetails == null) {
			return connectorNamesWithDuplicates.isEmpty();
		}

		boolean newDuplicateFound = false;
		boolean oldDuplicateRemoved = false;
		if (oldDCDetails == null) {

			newDuplicateFound = newDCDetails.addEditDeviceConnectorTableRow(pinName);
		}
		else if (newDCDetails == null) {

			oldDuplicateRemoved = oldDCDetails.removeEditDeviceConnectorTableRow(pinName);
		}
		else {

			oldDuplicateRemoved = oldDCDetails.removeEditDeviceConnectorTableRow(pinName);

			newDuplicateFound = newDCDetails.addEditDeviceConnectorTableRow(pinName);
		}
		if (oldDuplicateRemoved) {
			connectorNamesWithDuplicates.remove(oldDCValue);
		}
		if (newDuplicateFound) {
			connectorNamesWithDuplicates.add(newDCValue);
		}
		return connectorNamesWithDuplicates.isEmpty();
	}

	public void handleConnectorNameChange(@Nullable String oldDCValue, @Nullable String newDCValue)
	{
		if (!StringUtils.isBlank(oldDCValue)) {
			nameCaseConsistencyChecker.remove(oldDCValue);
		}
		if (!StringUtils.isBlank(newDCValue)) {
			nameCaseConsistencyChecker.add(newDCValue);
		}
	}

	public boolean validateDCPinChange(@Nullable String oldValue, @Nullable String newValue,
			@NotNull EditDeviceConnectorTableRow rowItem)
	{
		String connectorName = rowItem.getDeviceConnectorName();
		if (StringUtils.isBlank(oldValue) && StringUtils.isBlank(newValue)) {
			return connectorNamesWithDuplicates.isEmpty();
		}
		boolean newDuplicateFound = false;
		boolean oldDuplicateRemoved = false;

		DeviceConnectorPinDetails dcPartNumberDetails = connectorNameDetails
				.computeIfAbsent(connectorName, (dcName) -> new DeviceConnectorPinDetails());

		if (StringUtils.isBlank(newValue)) {

			oldDuplicateRemoved = dcPartNumberDetails.removeEditDeviceConnectorTableRow(oldValue);
		}
		else if (StringUtils.isBlank(oldValue)) {
			newDuplicateFound = dcPartNumberDetails.addEditDeviceConnectorTableRow(newValue);
		}
		else {

			oldDuplicateRemoved = dcPartNumberDetails.removeEditDeviceConnectorTableRow(oldValue);
			newDuplicateFound = dcPartNumberDetails.addEditDeviceConnectorTableRow(newValue);
		}
		if (oldDuplicateRemoved && !newDuplicateFound) {
			connectorNamesWithDuplicates.remove(connectorName);
		}
		else if (newDuplicateFound) {
			connectorNamesWithDuplicates.add(connectorName);
		}

		return connectorNamesWithDuplicates.isEmpty();
	}

	@Nullable public String getDuplicatePinError(@Nullable String dcName, @Nullable String dcPinName)
	{

		DeviceConnectorPinDetails dcPinDetail = connectorNameDetails.get(dcName);
		if (dcPinDetail != null) {
			return dcPinDetail.getDuplicatePinError(dcPinName);
		}
		return null;
	}

	@Nullable private String getNoNameAssigned()
	{
		if (!devicePinNamesWithNoDCName.isEmpty()) {

			Set<IFootprintDevicePinKey> devicePinKeys = devicePinNamesWithNoDCName.keySet();
			SortedList<String> names =
					new SortedList<>(devicePinKeys.size(), AlphaNumComparator.getCaseSensitiveComparator());
			devicePinKeys.forEach((p) -> names.add(p.getName()));

			return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
					"EditDeviceConnectorAction.okdisabled.connectornamemissing", names.toString());
		}
		return null;
	}

	@Nullable private String getNoCavityAssigned()
	{
		if (!devicePinNamesWithNoCavities.isEmpty()) {

			Set<IFootprintDevicePinKey> devicePinKeys = devicePinNamesWithNoCavities.keySet();
			SortedList<String> names =
					new SortedList<>(devicePinKeys.size(), AlphaNumComparator.getCaseSensitiveComparator());
			devicePinKeys.forEach((p) -> names.add(p.getName()));

			return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
					"EditDeviceConnectorAction.okdisabled.cavitynamesmissing", names.toString());
		}

		return null;
	}

	@Nullable public String getErrorInCurrentTableState()
	{
		String error = getDuplicateError();
		if (error == null) {
			error = getNoNameAssigned();
		}
		if (error == null) {
			error = getNoCavityAssigned();
		}
		if (error == null) {
			error = nameCaseConsistencyChecker.getCaseInconsistentConnectorNamesError();
		}
		return error;
	}

	public void handleCavityAndConnectorNameChange(EditDeviceConnectorTableRow updatedItem)
	{
		IFootprintDevicePinKey devicePin = updatedItem.getDevicePin();
		if (!StringUtils.isBlank(updatedItem.getDeviceConnectorPinName()) &&
				StringUtils.isBlank(updatedItem.getDeviceConnectorName())) {
			addDevicePinWithNoDCName(devicePin, updatedItem.getDeviceConnectorPinName());
		}
		else {
			removeDevicePinAfterDCAssignation(devicePin);
		}
		if (StringUtils.isBlank(updatedItem.getDeviceConnectorPinName()) &&
				!StringUtils.isBlank(updatedItem.getDeviceConnectorName())) {
			addDevicePinWithNoCavityName(devicePin, updatedItem.getDeviceConnectorName());
		}
		else {
			removeDevicePinAfterCavityAssignation(devicePin);
		}
	}

	private void addDevicePinWithNoDCName(@NotNull IFootprintDevicePinKey devicePin, String dcPinName)
	{
		devicePinNamesWithNoDCName.put(devicePin, dcPinName);
	}

	private void addDevicePinWithNoCavityName(@NotNull IFootprintDevicePinKey devicePin, String deviceConnector)
	{
		devicePinNamesWithNoCavities.put(devicePin, deviceConnector);
	}

	private void removeDevicePinAfterDCAssignation(@NotNull IFootprintDevicePinKey devicePin)
	{
		devicePinNamesWithNoDCName.remove(devicePin);
	}

	private void removeDevicePinAfterCavityAssignation(@NotNull IFootprintDevicePinKey devicePin)
	{
		devicePinNamesWithNoCavities.remove(devicePin);
	}

	public void addCellWithError(IGenericTableCell<?> cell)
	{
		cellsWithErrors.add(cell);
	}

	public void removeCellWithError(IGenericTableCell<?> cell)
	{
		cellsWithErrors.remove(cell);
	}

	@Nullable private String getDuplicateError()
	{
		if (!connectorNamesWithDuplicates.isEmpty()) {
			return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
					"EditDeviceConnectorAction.okdisabled.connectorswithduplicatepins", connectorNamesWithDuplicates);
		}
		return null;
	}

	public void refreshCellsWithErrors()
	{
		Platform.runLater(() -> {
			Collection<IGenericTableCell<?>> currentCellsWithError = new HashSet<>(cellsWithErrors);
			currentCellsWithError.forEach(aCell -> aCell.updateCellValue(aCell.getValue(), false));
		});
	}

	@Nullable public String getCaseInconsistentConnectorName(String dcName)
	{
		return nameCaseConsistencyChecker.getCaseInconsistentConnectorNamesError(dcName);
	}

	private static class IgnoreCaseNameHandler
	{

		private Map<String, Integer> particularNameVariantCount = new HashMap<>(2);

		boolean add(String nameVariant)
		{
			Integer count = particularNameVariantCount.computeIfAbsent(nameVariant, (name) -> 0);
			count++;
			particularNameVariantCount.put(nameVariant, count);
			return particularNameVariantCount.size() > 1;
		}

		boolean remove(String nameVariant)
		{
			Integer count = particularNameVariantCount.get(nameVariant);
			if (count != null) {
				count--;
				if (count == 0) {
					particularNameVariantCount.remove(nameVariant);
				}
				else {
					particularNameVariantCount.put(nameVariant, count);
				}
			}
			return particularNameVariantCount.size() > 1;
		}
	}

	private static class NameCaseConsistencyChecker
	{

		private boolean allowMixedCaseDuplication;

		NameCaseConsistencyChecker(boolean allowMixedCaseDuplication)
		{
			this.allowMixedCaseDuplication = allowMixedCaseDuplication;
		}
		private Collection<String> duplicateConnectorNames = new HashSet<>(2);

		private Map<String, IgnoreCaseNameHandler> ignoreCaseNameHandlerMap = new HashMap<>();

		private String getId(String connectorName)
		{
			return allowMixedCaseDuplication ? connectorName :connectorName.toLowerCase(Locale.getDefault());
		}

		boolean add(String connectorName)
		{
			String name = getId(connectorName);
			IgnoreCaseNameHandler ignoreCaseNameHandler =
					ignoreCaseNameHandlerMap.computeIfAbsent(name, (val) -> new IgnoreCaseNameHandler());
			boolean isDuplicate = ignoreCaseNameHandler.add(connectorName);
			if (isDuplicate) {
				duplicateConnectorNames.add(name);
			}
			return !duplicateConnectorNames.isEmpty();
		}

		boolean remove(String connectorName)
		{
			String name = getId(connectorName);
			IgnoreCaseNameHandler ignoreCaseNameHandler = ignoreCaseNameHandlerMap.get(name);
			if (ignoreCaseNameHandler != null) {
				boolean isDuplicate = ignoreCaseNameHandler.remove(connectorName);
				if (!isDuplicate) {
					duplicateConnectorNames.remove(name);
				}
			}
			return !duplicateConnectorNames.isEmpty();
		}

		@Nullable public String getCaseInconsistentConnectorNamesError(String connectorName)
		{

			if (duplicateConnectorNames.contains(getId(connectorName))) {

				return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
						"EditDeviceConnectorAction.cellerror.samenamecasediff");
			}
			return null;
		}

		boolean hasCaseInconsistentNameError()
		{
			return !duplicateConnectorNames.isEmpty();
		}

		@Nullable public String getCaseInconsistentConnectorNamesError()
		{
			if (hasCaseInconsistentNameError()) {
				return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
						"EditDeviceConnectorAction.okdisabled.samenamecasediff", duplicateConnectorNames);
			}
			return null;
		}
	}

	private static class DeviceConnectorPinDetails
	{

		private Map<String, Integer> pinCount = new HashMap<>();
		private Collection<String> pinsWithDuplicates = new LinkedHashSet<>();

		private NameCaseConsistencyChecker cavityNameCaseConsistencyChecker;

		DeviceConnectorPinDetails(){
			ISystemPreferenceMgr preferences =
					(ISystemPreferenceMgr) CHSSystemMgr.getCHSSystem().getSystemData().getPreferences();
			cavityNameCaseConsistencyChecker = new NameCaseConsistencyChecker(preferences.isMixedCasePinNamesAllowed());
		}

		public boolean addEditDeviceConnectorTableRow(@Nullable String cavityName)
		{
			if (!StringUtils.isBlank(cavityName)) {
				cavityNameCaseConsistencyChecker.add(cavityName);
				Integer count = pinCount.computeIfAbsent(cavityName, (pinName) -> 0);
				count++;
				pinCount.put(cavityName, count);
				if (count > 1) {
					pinsWithDuplicates.add(cavityName);
				}
			}
			return !pinsWithDuplicates.isEmpty() || cavityNameCaseConsistencyChecker.hasCaseInconsistentNameError();
		}

		public boolean removeEditDeviceConnectorTableRow(@Nullable String cavityName)
		{
			if (!StringUtils.isBlank(cavityName)) {
				cavityNameCaseConsistencyChecker.remove(cavityName);
				Integer count = pinCount.get(cavityName);
				count--;
				if (count <= 1) {
					pinsWithDuplicates.remove(cavityName);
				}
				if (count < 1) {
					pinCount.remove(cavityName);
				}
				else {
					pinCount.put(cavityName, count);
				}
			}

			return pinsWithDuplicates.isEmpty() && !cavityNameCaseConsistencyChecker.hasCaseInconsistentNameError();
		}

		@Nullable public String getDuplicatePinError(@Nullable String dcPinName)
		{
			if (!StringUtils.isBlank(dcPinName)) {
				String onlyCaseDifferingCavityName =
						cavityNameCaseConsistencyChecker.getCaseInconsistentConnectorNamesError(dcPinName);
				if (onlyCaseDifferingCavityName != null) {
					return onlyCaseDifferingCavityName;
				}
				if (pinsWithDuplicates.contains(dcPinName)) {

					return ResourceMgr.getString(DeviceConnectorNamePinNameValidator.class,
							"EditDeviceConnectorAction.cellerror.duplicatecavity");
				}
			}
			return null;
		}
	}
}
