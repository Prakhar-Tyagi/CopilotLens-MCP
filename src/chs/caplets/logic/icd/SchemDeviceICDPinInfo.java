package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.common.ILocation;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDSignalsContainer;
import chs.utility.helpers.SchemPinListHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class SchemDeviceICDPinInfo implements IICDSignalSourceSchemPinlist
{

	@NotNull private final IPinList m_schemDev;
	@NotNull private final HashMap<String, IPin> m_icdPinMatch = new HashMap<>();
	@NotNull private final HashMap<IPin, IPin> m_devToPlugPinMap = new HashMap<>();

	public SchemDeviceICDPinInfo(@NotNull IPinList schemDev)
	{
		m_schemDev = schemDev;

		for (IPin pin : m_schemDev.getPins()) {
			if (!pin.isReference()) {
				m_icdPinMatch.put(StringUtils.nonNull(ICDUtils.getICDMatchName(pin.getConnectivity())), pin);
			}
		}

		BiConsumer<IPin, IPin> connectedPinReciever =
				(sourceSchemPin, connectedPin) -> m_devToPlugPinMap.put(sourceSchemPin, connectedPin);
		SchemPinListHelper.getConnectedSchemHarnConnectorPin(m_schemDev, connectedPinReciever);
	}

	@Nullable public IPin getSignalMatchingDevicePin(@Nullable String pinName)
	{
		return m_icdPinMatch.get(StringUtils.nonNull(pinName));
	}

	@Nullable public IPin getEquivalentICDMatchingSignalPin(@Nullable IPin pin)
	{
		if (pin != null) {
			IPin matchedPin = m_devToPlugPinMap.get(pin);
			return matchedPin != null ? matchedPin : pin;
		}
		return null;
	}

	@Nullable public IPin getConnectedSchemHarnConnectorPin(@Nullable IPin pin)
	{
		return pin != null ? m_devToPlugPinMap.get(pin) : null;
	}

	@Nullable public IDevice getCableDevice()
	{
		return CommonUtils.cast(m_schemDev.getConnectivity(), IDevice.class);
	}

	@NotNull @Override public IPinList getSchemPinlist()
	{
		return m_schemDev;
	}

	@NotNull @Override public IPinList getSchemDevice()
	{
		return m_schemDev;
	}

	@NotNull @Override public Collection<? extends IDeviceICDSignalsContainer> getICDSignalContainers(@NotNull IDeviceICD icd)
	{
		return icd.getICDUsageDefinition().getPinSignalAssociations();
	}

	@Nullable public ILocation getPinLocation(@Nullable String pinName)
	{
		IPin matchingDevicePin = getSignalMatchingDevicePin(pinName);
		matchingDevicePin = getEquivalentICDMatchingSignalPin(matchingDevicePin);
		if (matchingDevicePin != null) {
			return matchingDevicePin.getAbsLocation();
		}
		return null;
	}
}
