package chs.caplets.logic.actions;

import chs.utilities.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class EditDeviceNameLengthValidator
{

	public static final int NAME_LENGTH = 255;
	private Map<String, Integer> invalidNameLength = new LinkedHashMap<>();

	public boolean validateNameLength(@Nullable String oldValue, @Nullable String newValue)
	{

		removeInvalidNameLength(oldValue);

		createOrAddinvalidName(newValue);

		return !invalidNameLength.isEmpty();
	}

	private void removeInvalidNameLength(@Nullable String oldValue)
	{
		if (!StringUtils.isBlank(oldValue)) {
			Integer count = invalidNameLength.get(oldValue);
			if (count != null) {
				count--;
				if (count == 0) {
					invalidNameLength.remove(oldValue);
				}
				else {
					invalidNameLength.put(oldValue, count);
				}
			}
		}
	}

	@Nullable public String getErrorInCurrentState()
	{
		if (!invalidNameLength.isEmpty()) {
			return "Connector/pin Name too long for some connectors";
		}
		return null;
	}

	private void createOrAddinvalidName(@Nullable String newValue)
	{
		if (!StringUtils.isBlank(newValue) && newValue.length() > NAME_LENGTH) {
			Integer count = invalidNameLength.computeIfAbsent(newValue, pn -> 0);
			count++;
			invalidNameLength.put(newValue, count);
		}
	}
}
