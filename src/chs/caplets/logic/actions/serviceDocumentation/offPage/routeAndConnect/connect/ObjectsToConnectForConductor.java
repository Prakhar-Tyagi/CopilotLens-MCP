package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsConnector;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;

class ObjectsToConnectForConductor extends SchemObjectsToConnect<IConductor, IPin, IHighwaySchematic>
{

	@Override protected void addConnectivityPin(IPin pin)
	{
		m_connectivityPins.add(pin.getConnectivity());
	}

	@Override protected boolean connectSegmentContainers(IConductor schem1, IConductor schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectSchemConductors(schem1, schem2);
	}

	@Override protected boolean connectSegmentContainerAndPin(IConductor schem1, IPin schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectSchemConductorAndPin(schem1, schem2);
	}

	@Override protected boolean connectSegmentContainersOfDifferentTypes(IConductor schem1, IHighwaySchematic schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectSchemConductorAndHighway(schem1, schem2);
	}

	@Override protected boolean connectSegmentContainerBetweenPinAndOtherSCType(IPin pin1, IHighwaySchematic schem2,
			ISchemObjectsConnector schemObjectsConnector)
	{
		return schemObjectsConnector.connectPinAndHighwaySchematic(pin1, schem2);
	}
}
