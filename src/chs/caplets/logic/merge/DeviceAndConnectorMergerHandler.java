package chs.caplets.logic.merge;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;

class DeviceAndConnectorMergerHandler
{

	// added private constructor as all methods are static
	private DeviceAndConnectorMergerHandler()
	{
	}

	static void mergeConnectors(IBaseDevice sourceDevice, IBaseDevice targetDevice)
	{
		for (IConnector connector : sourceDevice.getConnectors()) {
			sourceDevice.removeConnector((IHarnessPlugConnector) connector);
			if (deviceAndConnectorAreConnected(targetDevice, connector)) {
				targetDevice.addConnector((IHarnessPlugConnector) connector);
			}
		}
	}

	private static boolean deviceAndConnectorAreConnected(IBaseDevice device, IConnector connector)
	{
		for (IAbstractPin devicePin : device.getPins()) {
			if (devicePin.isConnected(connector)) {
				return true;
			}
		}
		return false;
	}
	
}