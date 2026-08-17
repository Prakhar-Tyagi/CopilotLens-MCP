package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsConnector;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemStackPin;

class ObjectsToConnectForHighway extends SchemObjectsToConnect<IHighwaySchematic, ISchemStackPin, IConductor>
{

	@Override protected void addConnectivityPin(ISchemStackPin pin)
	{
		m_connectivityPins.addAll(pin.getAllConnectivity());
	}

	@Override protected boolean connectSegmentContainers(IHighwaySchematic schem1, IHighwaySchematic schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectHighways(schem1, schem2);
	}

	@Override protected boolean connectSegmentContainerAndPin(IHighwaySchematic schem1, ISchemStackPin schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectHighwayAndStackPin(schem1, schem2);
	}

	@Override protected boolean connectSegmentContainersOfDifferentTypes(IHighwaySchematic schem1, IConductor schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectSchemConductorAndHighway(schem2, schem1);
	}

	@Override protected boolean connectSegmentContainerBetweenPinAndOtherSCType(ISchemStackPin pin1, IConductor schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectStackPinAndConductor(pin1, schem2);
	}


}
