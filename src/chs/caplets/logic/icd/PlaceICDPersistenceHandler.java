package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.utilities.ListSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class PlaceICDPersistenceHandler extends PersistenceHandler
{

	public PlaceICDPersistenceHandler(ISchemDiagram diagram, boolean generateSingleEnded)
	{
		super(diagram, generateSingleEnded, new PlaceICDReporter());
	}

	@Override public void endRouting()
	{
		deleteOrphanedObjects();
		getLockTracker().unlock();
	}

	@Override public void endRoutingAll()
	{
		// Do nothing
	}

	@Override public boolean isUpdate()
	{
		return false;
	}

	@Override @NotNull
	public Collection<IConductor> disconnectInvalidSignals(IICDSignalSourceSchemPinlist currentSchemDevice,
			IDeviceICD currentICD, Collection<ISegment> disconnectedSegments)
	{
		return Collections.emptySet();
	}

	@Override
	public void removeSignalsFromInvalidMulticores(IPinList currentSchemDevice, IDeviceICD currentICD,
			ICDMulticoreContext multicoreContext, boolean isWiringAbstraction)
	{
	}

	@NotNull @Override public ListSet<IMulticore> getConnectedMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType)
	{
		return getConnectedMulticores(pinList, schemCond, signal, false, conductorType);
	}

	@Override
	@NotNull
	public ListSet<IMulticore> getDesignMulticores(IPinList pinList, IConductor schemCond,
			IICDAssociatedSignal signal, String conductorType)
	{
		return getDesignMulticores(pinList, schemCond, signal, false, conductorType);
	}

	@Override public void collectEmptyMulticores(Collection<chs.cof.logical.cable.IConductor> conductorsToBeUpdated)
	{

	}

	@Override @NotNull public Collection<ISharedMulticore> getSharedMulticores(IConnectivity connectivity,
			ISharedConductorMgr sharedConductorMgr, String condType)
	{
		return sharedConductorMgr.getSharedMulticores().stream()
				.filter(sharedMulticore -> !sharedMulticore.isFrozen())
				.filter(sharedMulticore -> {
					ISharedMulticore rootMulticore = sharedMulticore.getRootMulticore();
					return connectivity.findSharedMulticore(rootMulticore) == null;
				})
				.filter(shareMulticore -> areAllConductorsOfMatchingType(shareMulticore, condType))
				.collect(Collectors.toList());
	}


	@Override
	@NotNull public Collection<ISharedMulticore> getSharedMulticores(@NotNull Collection<ISharedMulticore> sharedMulticores)
	{
		return sharedMulticores;
	}

}
