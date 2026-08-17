package chs.caplets.logic.actions;

import org.jetbrains.annotations.Nullable;

public interface IEditDeviceConnectorDetails
{

	@Nullable String getDeviceConnectorPinName();

	void setDeviceConnectorPinName(@Nullable String deviceConnectorPinName);

	@Nullable String getDeviceConnectorPartNumber();

	void setDeviceConnectorPartNumber(@Nullable String deviceConnectorPartNumber);

	@Nullable String getDeviceConnectorName();

	void setDeviceConnectorName(@Nullable String deviceConnectorName);

	@Nullable String getInitialDeviceConnectorPinName();

	@Nullable String getInitialDeviceConnectorPartNumber();

	@Nullable String getInitialDeviceConnectorName();

	boolean isDeviceConnectorNameUpdated();

	boolean isCavityUpdated();

	boolean isConnectorPartNumberUpdated();
}