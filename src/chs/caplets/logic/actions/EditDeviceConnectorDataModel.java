package chs.caplets.logic.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EditDeviceConnectorDataModel
{

	private Map<IFootprintDevicePinKey, IEditDeviceConnectorDetails> m_data = new HashMap<>();

	public EditDeviceConnectorDataModel()
	{

	}

	public void add(@NotNull IFootprintDevicePinKey devicePinKey, @NotNull IEditDeviceConnectorDetails details)
	{
		m_data.put(devicePinKey, details);
	}

	@NotNull private IEditDeviceConnectorDetails getDeviceConnectorDetails(@NotNull IFootprintDevicePinKey devicePin)
	{
		return m_data.computeIfAbsent(devicePin, (o) -> new EditDeviceConnectorDetails());
	}

	public void setDeviceConnectorPartNumber(@NotNull IFootprintDevicePinKey devicePin,
			@Nullable String deviceConnectorPartNumber)
	{
		getDeviceConnectorDetails(devicePin).setDeviceConnectorPartNumber(deviceConnectorPartNumber);
	}

	public void setDeviceConnectorPinName(@NotNull IFootprintDevicePinKey devicePin, @Nullable String dcPinName)
	{
		getDeviceConnectorDetails(devicePin).setDeviceConnectorPinName(dcPinName);
	}

	public void setDeviceConnectorName(@NotNull IFootprintDevicePinKey devicePin, @Nullable String dcName)
	{
		getDeviceConnectorDetails(devicePin).setDeviceConnectorName(dcName);
	}

	@Nullable public String getDeviceConnectorPinName(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getDeviceConnectorPinName();
	}

	@Nullable public String getDeviceConnectorPartNumber(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getDeviceConnectorPartNumber();
	}

	@Nullable public String getDeviceConnectorName(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getDeviceConnectorName();
	}

	@Nullable public String getInitialDeviceConnectorPinName(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getInitialDeviceConnectorPinName();
	}

	@Nullable public String getInitialDeviceConnectorPartNumber(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getInitialDeviceConnectorPartNumber();
	}

	@Nullable public String getInitialDeviceConnectorName(@NotNull IFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).getInitialDeviceConnectorName();
	}

	public boolean isCavityUpdated(IPrivilegedFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).isCavityUpdated();
	}

	public boolean isConnectorNameUpdated(IPrivilegedFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).isDeviceConnectorNameUpdated();
	}

	public boolean isConnectorPartNumberUpdated(IPrivilegedFootprintDevicePinKey devicePin)
	{
		return getDeviceConnectorDetails(devicePin).isConnectorPartNumberUpdated();
	}
}
