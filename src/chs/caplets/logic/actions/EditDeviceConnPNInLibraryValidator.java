package chs.caplets.logic.actions;

import chs.cof.parts.ILibraryObject;
import chs.cofUtils.parts.PartNumberHelper;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditDeviceConnPNInLibraryValidator
{

	private Map<String, Integer> invalidPNCount = new LinkedHashMap<>();

	public EditDeviceConnPNInLibraryValidator(Collection<EditDeviceConnectorTableRow> allRows)
	{

		for (EditDeviceConnectorTableRow aRow : allRows) {
			createOrAddPNEntry(aRow.getDeviceConnectorPartNumber());
		}
	}

	public boolean validatePartnumberChange(@Nullable String oldValue, @Nullable String newValue)
	{

		if (StringUtils.isBlank(newValue)) {
			if (!StringUtils.isBlank(oldValue)) {
				invalidPNCount.remove(oldValue);
			}
			return !invalidPNCount.isEmpty();
		}

		createOrAddPNEntry(newValue);
		if (!StringUtils.isBlank(oldValue)) {
			Integer count = invalidPNCount.get(oldValue);
			if (count != null) {
				count--;
				if (count == 0) {
					invalidPNCount.remove(oldValue);
				}
				else {
					invalidPNCount.put(oldValue, count);
				}
			}
		}

		return !invalidPNCount.isEmpty();
	}

	private void createOrAddPNEntry(@Nullable String newValue)
	{
		if (StringUtils.isBlank(newValue)) {
			return;
		}
		ILibraryObject libraryObject = PartNumberHelper.getLibraryPartFromCombinedPartNumber(newValue);
		if (libraryObject == null) {
			Integer count = invalidPNCount.computeIfAbsent(newValue, pn -> 0);
			count++;
			invalidPNCount.put(newValue, count);
		}
	}

	@Nullable public String getErrorInCurrentState()
	{
		if (!invalidPNCount.isEmpty()) {
			return "Part numbers " + invalidPNCount.keySet() + " used for device connectors are not in library";
		}
		return null;
	}
}
