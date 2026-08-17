package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.services.dynamicgfx.IDynamicGfx;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IICDInterconnectStrategy
{

	void updateICDRouting(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram,
			@NotNull IObjectFilter<IPin> pinFilter);

	List<IDynamicGfx> updateNetTraces(IPinList currentSchemPinlist, IDeviceICD currentICD, ISchemDiagram diagram,
			@Nullable List<Pair<ILocation, String>> pinAbsLocationInfo, boolean placingBackshellTerm);

	Map<String, IConductor> getAllVistedConductors();

	@NotNull Set<IShieldConductor> getNewShieldsInMulticores();

	IICDInterconnectStrategy NULL_ICD_INTERCONNECT_STRATEGY = new IICDInterconnectStrategy()
	{
		@Override
		public void updateICDRouting(IPinList currentSchemDevice, IDeviceICD currentICD, ISchemDiagram diagram,
				@NotNull IObjectFilter<IPin> pinFilter)
		{

		}

		@Override
		public List<IDynamicGfx> updateNetTraces(IPinList currentSchemPinlist, IDeviceICD currentICD,
				ISchemDiagram diagram,
				@Nullable List<Pair<ILocation, String>> pinAbsLocationInfo, boolean placingBackshellTerm)
		{
			return Collections.emptyList();
		}

		@Override public Map<String, IConductor> getAllVistedConductors()
		{
			return Collections.emptyMap();
		}

		@NotNull @Override public Set<IShieldConductor> getNewShieldsInMulticores()
		{
			return Collections.emptySet();
		}

		@Override public boolean isWiringAbstraction()
		{
			return false;
		}

		@Override public void endRouting()
		{

		}
	};

	boolean isWiringAbstraction();

	void endRouting();
}