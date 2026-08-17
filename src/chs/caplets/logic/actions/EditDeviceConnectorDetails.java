package chs.caplets.logic.actions;

import chs.utilities.IUpdateableDataUnit;
import chs.utilities.UpdateableDataUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditDeviceConnectorDetails implements IEditDeviceConnectorDetails
{

	@NotNull private IUpdateableDataUnit<String> m_deviceConnectorPinName;
	@NotNull private IUpdateableDataUnit<String> m_deviceConnectorPartNumber;
	@NotNull private IUpdateableDataUnit<String> m_deviceConnectorName;

	public EditDeviceConnectorDetails()
	{
		this(null, null, null);
	}

	public EditDeviceConnectorDetails(@Nullable String deviceConnectorPartNumber,
			@Nullable String deviceConnectorPinName, @Nullable String deviceConnectorName)
	{
		m_deviceConnectorName = new UpdateableDataUnit<String>(deviceConnectorName);
		m_deviceConnectorPartNumber = new UpdateableDataUnit<String>(deviceConnectorPartNumber);
		m_deviceConnectorPinName = new UpdateableDataUnit<String>(deviceConnectorPinName);
	}

	@Nullable public String getDeviceConnectorPinName()
	{
		return m_deviceConnectorPinName.getValue();
	}

	public void setDeviceConnectorPinName(@Nullable String deviceConnectorPinName)
	{
		m_deviceConnectorPinName = m_deviceConnectorPinName.getUpdated(deviceConnectorPinName);
	}

	@Nullable public String getDeviceConnectorPartNumber()
	{
		return m_deviceConnectorPartNumber.getValue();
	}

	public void setDeviceConnectorPartNumber(@Nullable String deviceConnectorPartNumber)
	{
		m_deviceConnectorPartNumber = m_deviceConnectorPartNumber.getUpdated(deviceConnectorPartNumber);
	}

	@Nullable public String getDeviceConnectorName()
	{
		return m_deviceConnectorName.getValue();
	}

	public void setDeviceConnectorName(@Nullable String deviceConnectorName)
	{
		m_deviceConnectorName = m_deviceConnectorName.getUpdated(deviceConnectorName);
	}

	@Nullable public String getInitialDeviceConnectorPinName()
	{
		return m_deviceConnectorPinName.getOriginal();
	}

	@Nullable public String getInitialDeviceConnectorPartNumber()
	{
		return m_deviceConnectorPartNumber.getOriginal();
	}

	@Nullable public String getInitialDeviceConnectorName()
	{
		return m_deviceConnectorName.getOriginal();
	}

	public boolean isDeviceConnectorNameUpdated()
	{
		return m_deviceConnectorName.isUpdated();
	}

	public boolean isCavityUpdated()
	{
		return m_deviceConnectorPinName.isUpdated();
	}

	public boolean isConnectorPartNumberUpdated()
	{
		return m_deviceConnectorPartNumber.isUpdated();
	}
}
