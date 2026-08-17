package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.utilities.CommonUtils;
import chs.utilities.LinkedSetMap;
import chs.utilities.ListSet;
import chs.utilities.SetMap;
import chs.utility.IDeviceICDBackshellSignalAssociation;
import chs.utility.IDeviceICDPinSignalAssociation;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ICDMulticoreContext
{

	@NotNull private SetMap<IICDAssociatedSignal, IConductor> mSignalConductorMap;
	@NotNull private SetMap<IPin, Pair<IPin, IICDAssociatedSignal>> mDeferredDanglingConnections;
	@NotNull private SetMap<IPin, IICDAssociatedSignal> mVisitedConnections;
	@NotNull private Map<String, Integer> mSignalConnectionCount;
	private Set<IShieldConductor> mNewShieldsInMulticores = new HashSet<>();
	@NotNull private Set<IConductor> mReusedDanglingConductors = new HashSet<>();

	public ICDMulticoreContext()
	{
		//need ordered list for signals
		mDeferredDanglingConnections = new LinkedSetMap<IPin, Pair<IPin, IICDAssociatedSignal>>(LinkedHashMap.class);
		mVisitedConnections = new SetMap<>();
		mSignalConnectionCount = new HashMap<>(10);

		Comparator<IICDAssociatedSignal> signalComparator = new Comparator<IICDAssociatedSignal>()
		{
			@Override public int compare(IICDAssociatedSignal o1, IICDAssociatedSignal o2)
			{
				return o1.getNetName().compareTo(o2.getNetName());
			}
		};
		Supplier<Map<IICDAssociatedSignal, Set<IConductor>>> mapSupplier =
				() -> new TreeMap<IICDAssociatedSignal, Set<IConductor>>(signalComparator);
		mSignalConductorMap = new SetMap<IICDAssociatedSignal, IConductor>(mapSupplier)
		{
			@Override protected Set<IConductor> createSet()
			{
				return new ListSet<>();
			}
		};
	}

	public void registerPotentialCable(@NotNull IICDAssociatedSignal associatedSignal,
			@NotNull IConductor schemConductor)
	{
		IShieldConductor shieldConductor = CommonUtils.cast(schemConductor.getConnectivity(), IShieldConductor.class);
		if (shieldConductor == null) { // Skip shields
			mSignalConductorMap.add(associatedSignal, schemConductor);
		}
	}

	public void registerDanglingConnection(IPin placingRefPin, IPin placedPin,
			IICDAssociatedSignal iicdAssociatedSignal)
	{
		mDeferredDanglingConnections
				.add(placingRefPin, new Pair<IPin, IICDAssociatedSignal>(placedPin, iicdAssociatedSignal));
	}

	public SetMap<IPin, Pair<IPin, IICDAssociatedSignal>> getDanglingConnections()
	{
		return mDeferredDanglingConnections;
	}

	public boolean alreadyHasDanglingConnection(IPin pin, IICDAssociatedSignal iicdAssociatedSignal)
	{
		Set<Pair<IPin, IICDAssociatedSignal>> targetConnections = mDeferredDanglingConnections.pullReadOnlySafeSet(pin);
		return targetConnections.stream().anyMatch(pair -> pair.getValue() == iicdAssociatedSignal);
	}

	public void registerVisitedConnections(IPin pin, IICDAssociatedSignal iicdAssociatedSignal)
	{
		mVisitedConnections.add(pin, iicdAssociatedSignal);
	}

	public boolean hasAlreadyVisitedConnection(IPin placedPin, IICDAssociatedSignal iicdAssociatedSignal)
	{
		return mVisitedConnections.pullReadOnlySafeSet(placedPin).contains(iicdAssociatedSignal);
	}

	public void registerNewMulticoreShield(@NotNull IShieldConductor shield)
	{
		mNewShieldsInMulticores.add(shield);
	}

	@NotNull public Set<IShieldConductor> getNewShieldsInMulticores()
	{
		return mNewShieldsInMulticores;
	}

	public void registerReusedDanglingConductor(@NotNull IConductor conductor)
	{
		mReusedDanglingConductors.add(conductor);
	}

	@NotNull public Set<IMulticore> getMCsOfReusedDanglingConductors()
	{
		return mReusedDanglingConductors.stream()
				.map(cond -> cond.getConnectivity().getRootMulticore())
				.filter(multicore -> multicore != null)
				.collect(Collectors.toSet());
	}

	@NotNull public SetMap<IICDAssociatedSignal, IConductor> getSignalConductorMap()
	{
		return mSignalConductorMap;
	}

	public void populateSignalConnectionCount(@Nullable ILogicDesign logicDesign)
	{
		if (logicDesign != null) {
			for (IDeviceICD iicd : logicDesign.getDesignICDContainer().getApplicableICDsWithDesignAssociation()) {
				for (IDeviceICDPinSignalAssociation pinSignalAssociation : iicd.getICDUsageDefinition()
						.getPinSignalAssociations()) {
					for (IICDAssociatedSignal associatedSignal : pinSignalAssociation.getICDAssociatedSignals()) {
						incrementSignalConnectionCount(associatedSignal);
					}
				}
				for (IDeviceICDBackshellSignalAssociation backshellSignalAssociation : iicd.getICDUsageDefinition()
						.getBackshellSignalAssociations()) {
					for (IICDAssociatedSignal associatedSignal : backshellSignalAssociation.getICDAssociatedSignals()) {
						incrementSignalConnectionCount(associatedSignal);
					}
				}
			}
		}
	}

	private void incrementSignalConnectionCount(IICDAssociatedSignal associatedSignal)
	{
		Integer connectionCount = mSignalConnectionCount.get(associatedSignal.getNetName());
		if (connectionCount == null) {
			connectionCount = 0;
		}
		connectionCount++;
		mSignalConnectionCount.put(associatedSignal.getNetName(), connectionCount);
	}

	public int getSignalConnectionCount(String netName)
	{
		Integer connectionCount = mSignalConnectionCount.get(netName);
		return connectionCount == null ? 0 : connectionCount;
	}
}
