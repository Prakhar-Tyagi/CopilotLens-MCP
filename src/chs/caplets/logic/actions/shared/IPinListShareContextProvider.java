package chs.caplets.logic.actions.shared;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.security.IDomain;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPinListShareContextProvider
{

	@Nullable Map<IAbstractPin, IPinProxy> getInstanceToSharedMap();

	boolean preserveInternalConnectivity();

	@Nullable List<IPinProxy> getPlugMapInfo();

	@Nullable Set<IPinProxy> getReusablePins();

	@Nullable ISharedPinList getSharedPinList();

	@Nullable String getSharedObjectMateName();

	boolean isSharedMateNameGenerated();

	@Nullable String getSharedObjectMateRevision();

	@Nullable String getSharedPinListName();

	@Nullable String getSharedPinListRevision();

	boolean isSharedNameGenerated();

	@Nullable Map<IPinProxy, IAbstractPin> getSharedToInstanceMap();

	@Nullable String getAnalysisModel();

	@Nullable String getAnalysisFunctionRealiser();

	@Nullable String getOverriddenAnalysisInterfaces();

	@Nullable String getOverriddenAnalysisFailureModes();

	boolean reusablePinErrors();

	Map<ISharedPin, ISharedPin> getConnectedPinsToMakeReusable();

	@Nullable Map<IConnector, String> getModularConnectorToSharedNamesMap();

	@Nullable Map<IConnector, Boolean> getModularConnectorToSharedNameGeneratedMap();

	@Nullable ISymbolDef getSymbolDef();

	@Nullable default Set<IDomain> getSharedDomains(){
		return null;
	}
}
