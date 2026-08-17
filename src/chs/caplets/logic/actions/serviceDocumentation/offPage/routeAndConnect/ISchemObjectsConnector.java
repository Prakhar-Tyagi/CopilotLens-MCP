package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect;

import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import org.jetbrains.annotations.Nullable;

public interface ISchemObjectsConnector
{

	boolean connectSchemConductors(IConductor schem1, IConductor schem2);

	boolean connectHighways(IHighwaySchematic schem1, IHighwaySchematic schem2);

	boolean connectSchemConductorAndPin(IConductor schem1, IAbstractSchemPin pin);

	boolean connectHighwayAndStackPin(IHighwaySchematic schem1, ISchemStackPin pin);

	boolean connectSchemPins(IAbstractSchemPin pin1, @Nullable IAbstractSchemPin pin2);

	boolean connectSchemConductorAndHighway(IConductor conductor, IHighwaySchematic highwaySchematic);

	boolean connectPinAndHighwaySchematic(IPin pin, IHighwaySchematic highwaySchematic);

	boolean connectStackPinAndConductor(ISchemStackPin stackPin, IConductor conductor);
}
