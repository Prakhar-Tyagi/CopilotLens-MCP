package chs.caplets.logic.actions;

import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.subsystem.logic.manageconnections.ISharedPinProvider;
import chs.subsystem.logic.manageconnections.ManageConnectionsServices;
import chs.utility.ui.PinConductorConnectionSortHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ManageConnectorPinSelections
{

	private IPinList pinlist;
	private Map<String, Map<String, IPinProxy>> pins;
	private Map<String, Map<String, IPinProxy>> newlyCreatedSharedPins = new HashMap<>();
	private ManageConnectorDesignScope logicDesignScope;
	private Map<IDesignDescriptor, Collection<ISharedPin>> availablePins;
	private ISharedPinList sharedPinList;
	private Map<String, ISharedPin> sharedPinsByName;
	private ManageConnectorApplicablePins manageConnectorApplicablePins;
	private Collection<IPinProxy> m_unconnectedSharedPinProxies;

	public ManageConnectorPinSelections(IPinList pinlist, ManageConnectorDesignScope designScope)
	{
		this.pinlist = pinlist;
		this.sharedPinList = pinlist.getSharedPinList();
		logicDesignScope = designScope;
		availablePins = new HashMap<>();
	}

	public ManageConnectorPinSelections(ISharedPinList pinlist, ManageConnectorDesignScope designScope)
	{
		logicDesignScope = designScope;
		this.sharedPinList = pinlist;
		availablePins = new HashMap<>();
		manageConnectorApplicablePins = new ManageConnectorApplicablePins(pinlist, designScope);
	}

	public boolean isSharedPinPresent(String pinName, IDesignDescriptor design)
	{
		if (pins == null) {
			return false;
		}
		IPinProxy pinProxy;

		Map<String, IPinProxy> pinproxiesInDesign = pins.get(design.getUID().getString());
		if (pinproxiesInDesign != null) {
			pinProxy = pinproxiesInDesign.get(pinName);
			if (pinProxy != null) {
				return true;
			}
		}
		return false;
	}

	@Nullable public IPinProxy getPinByName(String pinName, IDesignDescriptor design)
	{
		if (pins == null) {
			return null;
		}
		IPinProxy pinProxy;

		Map<String, IPinProxy> pinproxiesInDesign = pins.get(design.getUID().getString());
		if (pinproxiesInDesign != null) {
			pinProxy = pinproxiesInDesign.get(pinName);
			if (pinProxy != null) {
				return pinProxy;
			}
		}
		if (sharedPinList != null) {
			if (sharedPinsByName == null) {
				List<ISharedPin> sharedPins =
						ManageConnectionsServices.requireExtension(sharedPinList, ISharedPinProvider.class).getAllSharedPins();
				sharedPinsByName = new HashMap<>(sharedPins.size());
				sharedPins.forEach(sharedPin -> {
					String key = getSharedPinKey(sharedPin);
					sharedPinsByName.put(key, sharedPin);
				});
			}
			ISharedPin sPin = sharedPinsByName.get(pinName);
			Map<String, IPinProxy> sharedPinProxiesInContainer =
					newlyCreatedSharedPins.get(design.getUID().getString());
			if (sharedPinProxiesInContainer == null) {
				sharedPinProxiesInContainer = new HashMap<>();
				newlyCreatedSharedPins.put(design.getUID().getString(), sharedPinProxiesInContainer);
			}
			String pinKey = getSharedPinKey(sPin);

			IPinProxy sharedPinProxyInContainer = sharedPinProxiesInContainer.get(pinKey);
			if (sharedPinProxyInContainer == null) {
				sharedPinProxyInContainer = new PinProxy(sPin);
				if(sPin instanceof ISharedBackshellTermination){
					sharedPinProxyInContainer.setName(pinKey);
				}
			}
			sharedPinProxiesInContainer.put(pinKey, sharedPinProxyInContainer);
			return sharedPinProxyInContainer;
		}
		else {
			Map<String, IPinProxy> pinproxiesofShared = pins.get("other");
			if (pinproxiesofShared != null) {
				pinProxy = pinproxiesofShared.get(pinName);
				if (pinProxy != null) {
					return pinProxy;
				}
			}
		}

		return null;
	}

	@NotNull
	private String getSharedPinKey(ISharedPin sharedPin)
	{
		return sharedPin instanceof ISharedBackshellTermination
				? sharedPin.getOwner().getName() + ":" + sharedPin.getName()
				: sharedPin.getName();
	}

	public Collection<IPinProxy> getPins()
	{
		if (pins == null) {
			return Collections.emptyList();
		}
		Collection<IPinProxy> pinProxies = new ArrayList<>();
		for (Map<String, IPinProxy> pinProxiesInContainer : pins.values()) {
			pinProxies.addAll(pinProxiesInContainer.values());
		}
		return pinProxies;
	}

	public boolean canUseThePinInDesign(IPinProxy pin, @Nullable IDesignDescriptor designDescriptor)
	{

		ISharedPin sharedPin = pin.getSharedPin();
		if (sharedPin != null && designDescriptor != null && manageConnectorApplicablePins != null) {
			return manageConnectorApplicablePins.canUsePinInCurrentDesign(sharedPin, designDescriptor);
		}
		return true;
	}

	public Collection<Comparable<?>> getNotationsForSharedPinsApplicableInDesign(IDesignDescriptor designDescriptor)
	{
		return manageConnectorApplicablePins.getNotationsForSharedPinsApplicableInDesign(designDescriptor);
	}

	public Collection<String> getSharedPinsApplicableInDesign(IDesignDescriptor designDescriptor)
	{
		return manageConnectorApplicablePins.getPinSharedPinNamesApplicableInDesign(designDescriptor);
	}

	public List<Comparable<?>> getAllPossibleValues()
	{
		return manageConnectorApplicablePins.getAllPossibleValues();
	}

	public void setPins(Map<IDesignDescriptor, Collection<IPinProxy>> pinProxiesInContainers,
			Collection<IPinProxy> sharedAndLibraryCavityPins)
	{

		if (pins != null) {
			pins.clear();
		}
		else {
			pins = new LinkedHashMap<>();
		}
		for (IDesignDescriptor designDescriptor : pinProxiesInContainers.keySet()) {
			Collection<IPinProxy> connectionsInContainer =
					pinProxiesInContainers.get(designDescriptor);
			if (!connectionsInContainer.isEmpty()) {
				Collection<IPinProxy> pinProxiesInThisContainer = pinProxiesInContainers.get(designDescriptor);
				Map<String, IPinProxy> pinproxiesForGivenName = new LinkedHashMap<>();
				pinProxiesInThisContainer.stream()
						.forEach(aPinProxy -> pinproxiesForGivenName.put(aPinProxy.getName(), aPinProxy));
				pins.put(designDescriptor.getUID().getString(), pinproxiesForGivenName);
			}
		}
		Map<String, IPinProxy> sharedpinProxiesInContainer = new LinkedHashMap<>();
		pins.put("other", sharedpinProxiesInContainer);
		sharedAndLibraryCavityPins.stream()
				.forEach(aPinProxy -> sharedpinProxiesInContainer.put(aPinProxy.getName(), aPinProxy));
	}

	public void updateDataUsingSortHelper(Supplier<PinConductorConnectionSortHelper> sortHelperSupplier)
	{
		if (manageConnectorApplicablePins != null) {
			manageConnectorApplicablePins.updateDataUsingSortHelper(sortHelperSupplier);
		}
	}

	public boolean areMultipleDesignsEdited()
	{
		return logicDesignScope.areMultipleDesignsEdited();
	}

	public void setUnconnectedPinProxies(@NotNull Collection<IPinProxy> unconnectedSharedPinProxies)
	{
		m_unconnectedSharedPinProxies = unconnectedSharedPinProxies;
	}

	@NotNull public Collection<IPinProxy> getUnconnectedSharedPinProxies(){
		return m_unconnectedSharedPinProxies != null ? m_unconnectedSharedPinProxies :
				Collections.emptyList();
	}
}



