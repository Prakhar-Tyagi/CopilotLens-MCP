package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.MapPanel;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInterconnectConnector;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.shared.CommonInSharedPinDBReservationAndDesignScope;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedDeviceConnectorPin;
import chs.cof.logical.shared.ISharedDevicePin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.ISymbolDef;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.system.FactoryMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Counter;
import chs.utilities.Environment;
import chs.utilities.ICounter;
import chs.utilities.ListMap;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.SortedListModel;
import chs.utility.ICDUtils;
import chs.utility.LibraryPinFacade;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class PinMappingHandler extends BaseSharePinlistHandler
{

	// Subset of toList content - mapping not allowed for these
	@NotNull private final Set<IPinProxy> unmappableProxies;
	@NotNull private final Map<IAbstractPin, String> pinBlockNameMap;
	@NotNull private final Map<String, Integer> pinNameToCountMap;
	@NotNull private final SortedListModel<IAbstractPin> fromListModel;
	@NotNull private final SortedListModel<IPinProxy> toListModel;
	private final boolean m_pinCreationAllowed;
	private boolean extendedPinMatch;
	private boolean mateCompatibilityCheck;
	private boolean m_pendingDeviceConnMapping = true;
	private ReverseMap<IAbstractPin, IPinProxy> invalidDeviceConnMapping = new ReverseMap<>();

	public void regenerateDeviceConnMapping()
	{
		m_pendingDeviceConnMapping = true;
		invalidDeviceConnMapping.clear();
	}

	public boolean isDevConnMappingInvalid(@NotNull IPinProxy ppp)
	{
		ensureDeviceConnectorMapping();
		return invalidDeviceConnMapping.containsValue(ppp);
	}

	public boolean isDevConnMappingInvalid(@NotNull IAbstractPin ppp)
	{
		ensureDeviceConnectorMapping();
		return invalidDeviceConnMapping.containsKey(ppp);
	}

	public boolean hasDeviceConnectorMismatches()
	{
		ensureDeviceConnectorMapping();
		return !invalidDeviceConnMapping.isEmpty();
	}

	private void deviceConnectorMapper(@NotNull IPinMappingDataLossInterception consumer)
	{
		Map<IAbstractPin, IPinProxy> connectivityToSharedMap = getConnectivityToSharedMap();
		//already shared device connectors would not be the case. however we
		//should try this transfer only for yet to shared device connectors only.
		for (Map.Entry<IAbstractPin, IPinProxy> entry : connectivityToSharedMap.entrySet()) {
			IDevicePin sourceDevPin = CommonUtils.cast(entry.getKey(), IDevicePin.class);
			IDeviceConnPin deviceConnPin = sourceDevPin != null ? sourceDevPin.getDeviceConnectorPin() : null;
			IDeviceConnector devConn = (deviceConnPin != null) ?
					CommonUtils.cast(deviceConnPin.getOwner(), IDeviceConnector.class) : null;
			if (devConn != null && devConn.getSharedPinList() == null) {
				ISharedPin sharedDevPin = entry.getValue().getSharedPin();
				ISharedDeviceConnectorPin sharedDevConnPin = sharedDevPin != null ?
						CommonUtils.cast(sharedDevPin.getMatePin(), ISharedDeviceConnectorPin.class) : null;
				ISharedDeviceConnector sharedDevConn = sharedDevConnPin != null ?
						CommonUtils.cast(sharedDevConnPin.getOwner(), ISharedDeviceConnector.class) : null;
				if (sharedDevConn != null) {
					consumer.accept(entry, deviceConnPin, devConn, sharedDevConnPin, sharedDevConn);
				}
			}
		}
	}

	private interface IPinMappingDataLossInterception
	{

		void accept(@NotNull Map.Entry<IAbstractPin, IPinProxy> entry,
				@NotNull IDeviceConnPin devConnPin, @NotNull IDeviceConnector devConn,
				@NotNull ISharedDeviceConnectorPin sharedDevConnPin,
				@NotNull ISharedDeviceConnector sharedDevConn);
	}

	private void ensureDeviceConnectorMapping()
	{
		//enable only for manual share into of devices.
		boolean isMismatchDetectionEnabled = isShare() && !isBulkShare() && getSharedPinList() instanceof ISharedDevice;
		if (isMismatchDetectionEnabled && m_pendingDeviceConnMapping) {
			SetMap<IDeviceConnector, ISharedDeviceConnector> candidatePinLists = SetMap.createShallowSetMap();
			SetMap<ISharedDeviceConnector, IDeviceConnector> candidateSharedPinLists = SetMap.createShallowSetMap();
			deviceConnectorMapper((entry, devConnPin, devConn, sharedDevConnPin, sharedDevConn) -> {
				candidatePinLists.add(devConn, sharedDevConn);
				candidateSharedPinLists.add(sharedDevConn, devConn);
			});

			deviceConnectorMapper((entry, devConnPin, devConn, sharedDevConnPin, sharedDevConn) -> {
				Set<ISharedDeviceConnector> mappedSDevConns = candidatePinLists.pullReadOnlySafeSet(devConn);
				Set<IDeviceConnector> mappedDevConns = candidateSharedPinLists.pullReadOnlySafeSet(sharedDevConn);
				if (mappedDevConns.size() == 1 && mappedSDevConns.size() == 1) {
					if (!isValidDevConnMapping(devConnPin, devConn, sharedDevConnPin, sharedDevConn)) {
						invalidDeviceConnMapping.put(entry.getKey(), entry.getValue());
					}
				}
				else {
					invalidDeviceConnMapping.put(entry.getKey(), entry.getValue());
				}
			});

			//the device pin mismatch should be checked irrespective of device connectors
			Map<IAbstractPin, IPinProxy> connectivityToSharedMap = getConnectivityToSharedMap();
			for (Map.Entry<IAbstractPin, IPinProxy> entry : connectivityToSharedMap.entrySet()) {
				if (!StringUtils.equalsTrimmed(entry.getKey().getName(), entry.getValue().getName())) {
					invalidDeviceConnMapping.put(entry.getKey(), entry.getValue());
				}
			}
		}
		m_pendingDeviceConnMapping = false;
	}

	private boolean isValidDevConnMapping(@NotNull IDeviceConnPin devConnPin, @NotNull IDeviceConnector devConn,
			@NotNull ISharedDeviceConnectorPin sharedDevConnPin, @NotNull ISharedDeviceConnector sharedDevConn)
	{
		return StringUtils.equalsTrimmed(devConnPin.getName(), sharedDevConnPin.getName()) &&
				StringUtils.equalsTrimmed(devConn.getName(), sharedDevConn.getName()) &&
				StringUtils.equalsTrimmed(devConn.getPartNumber(), sharedDevConn.getPartNumber()) &&
				StringUtils.equalsTrimmed(devConn.getPartRevision(), sharedDevConn.getPartRevision());
	}

	private enum AssociationResult
	{

		NO_ERROR, INCOMPATIBLE_MATED_CONNECTORS, CANNOT_CONVERT_LIBRARY_PINTYPE, CANNOT_CONVERT_FROZEN_PINTYPE
	}

	private static class ResultOfAssociationCheck
	{

		@NotNull private AssociationResult m_result = AssociationResult.NO_ERROR;

		protected void setResult(@NotNull AssociationResult result)
		{
			m_result = result;
		}

		@NotNull public AssociationResult getResult()
		{
			return m_result;
		}
	}

	private static final Map<AssociationResult, String> messageForAssociateAttempt =
			new EnumMap<AssociationResult, String>(AssociationResult.class);

	static {
		messageForAssociateAttempt.put(AssociationResult.NO_ERROR, "MapPanel.associate.tooltip");
		messageForAssociateAttempt.put(AssociationResult.INCOMPATIBLE_MATED_CONNECTORS,
				"MapPanel.associate.incompatiblematedconnectors");
		messageForAssociateAttempt.put(AssociationResult.CANNOT_CONVERT_LIBRARY_PINTYPE,
				"MapPanel.associate.cannotconvertlibrarypintype");
		messageForAssociateAttempt.put(AssociationResult.CANNOT_CONVERT_FROZEN_PINTYPE,
				"MapPanel.associate.cannoteditsharedfrozenpin");
	}

	private class SharedPinlistChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSharedPinlistChange();
		}
	}

	private class NameChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onNameChange();
		}
	}

	private class SchemPinlistChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSchemPinlistChange();
		}
	}

	private class ReuseChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onReuseChange();
		}

		private void onReuseChange()
		{
			determineMappability(null);
			updatePinCountMap();
		}
	}

	private class SymbolDeletionListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			updatePinCountMap();
		}
	}

	private class MapChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			updatePinCountMap();
		}
	}

	private class FromListModel extends SortedListModel<IAbstractPin>
	{

		private FromListModel()
		{
			super(new AlphaNumComparator<IAbstractPin>(true, true)
			{

				public int compare(IAbstractPin o1, IAbstractPin o2)
				{
					AlphaNumComparator<String> stringComparator = new AlphaNumComparator<String>(true, true);
					int blockResult = PinUtils.comparePinGrouping(o1, o2, pinBlockNameMap::get, Function.identity(),
							stringComparator);
					if (blockResult != 0) {
						return blockResult;
					}
					if (Environment.shouldSortSharePinsBasedOnDeviceConnectors()) {
						int devConnResult = PinUtils.comparePinGrouping(o1, o2, PinUtils::determineDeviceConnector,
								IReadOnlyNamedObject::getName, stringComparator);
						if (devConnResult != 0) {
							return devConnResult;
						}
					}
					// Either two top-level pins or two pins from the same block
					return stringComparator.compare(o1.getName(), o2.getName());
				}
			});
		}
	}

	public PinMappingHandler(@NotNull EditSharedPinListModel model,
			@NotNull ILogicDesign design)
	{
		this(model, design, null, true, true, true, false);
	}

	public PinMappingHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@Nullable IShareMessageContextReporter reporter, boolean extendedMatch, boolean mateCompCheck,
			boolean pinCreationsAllowed, boolean isBulkShare)
	{
		super(model, design, reporter, isBulkShare);
		extendedPinMatch = extendedMatch;
		mateCompatibilityCheck = mateCompCheck;
		m_pinCreationAllowed = pinCreationsAllowed;
		unmappableProxies = new HashSet<IPinProxy>();
		pinBlockNameMap = new HashMap<IAbstractPin, String>();
		pinNameToCountMap = new HashMap<String, Integer>();
		fromListModel = new FromListModel();
		toListModel = model.getProxies();
		addSharedChangeListener(new SharedPinlistChangeListener());
		addNameChangeListener(new NameChangeListener());
		addSchemChangeListener(new SchemPinlistChangeListener());
		addReuseChangeListener(new ReuseChangeListener());
		addMapChangeListener(new MapChangeListener());
		addRemovalListener(new SymbolDeletionListener());
	}

	public void init()
	{
		onSchemPinlistChange();
		onSharedPinlistChange();
	}

	@NotNull public SortedListModel<IAbstractPin> getFromListModel()
	{
		return fromListModel;
	}

	@NotNull public SortedListModel<IPinProxy> getToListModel()
	{
		return toListModel;
	}

	private void onSchemPinlistChange()
	{
		fromListModel.clear();
		IPinList cablePL = getCablePinlist();
		if (cablePL != null) {
			populateTheFromList(cablePL);
		}

		if (isShare() && getSharedPinList() == null && cablePL != null) {
			// This is a share, but not into an existing shared pinlist.
			// Start out with a set of proxies named after the pins on the pinlist to be shared,  plus
			// enough extra to fill out a library part definition if present.

			// Add the proxies to be added to respective lists and then add/remove to list model in one go
			// This avoids performance overhead of ProxyList.fireChangeEvent being called for every add/remove
			List<IPinProxy> proxiesToAdd = new ArrayList<IPinProxy>();
			for (IAbstractPinIterator pinIt = cablePL.getPins(); pinIt.hasNext(); ) {
				IAbstractPin pin = pinIt.getNext();

				IPinProxy proxy = findMatchingProxyByName(makeBlockPinName(pin), getProxies());
				if (proxy == null) {
					final PinProxy newProxy = createPinProxy(pin.getName());
					// This will store the name of the mated pin IFF this pin is an inline pin and has a connected pin
					// Needed during the share process dts0100401406
					newProxy.setPinMateName(pin);
					newProxy.setInterconnect(pin.isInterconnect());
					proxiesToAdd.add(newProxy);
				}
			}
			final ILibraryObject libraryObject = (ILibraryObject) cablePL.getLibraryObject();
			if (libraryObject != null) {
				createLibraryPinProxies(cablePL, proxiesToAdd, libraryObject);
			}
			else {
				IDeviceICD selectICD = ICDUtils.getMappedICD(CommonUtils.cast(cablePL, IDevice.class));
				if (selectICD != null && !cablePL.isShared() &&
						selectICD.getLibraryDevice() == null) {
					ILibraryDevice transientLibraryDevice = ICDUtils.createTransientLibraryDevice(selectICD);
					createLibraryPinProxies(cablePL, proxiesToAdd, transientLibraryDevice);
				}
			}

			if (!proxiesToAdd.isEmpty()) {
				//Avoid firing change event for every proxy that is added to model

				getProxies().addAll(proxiesToAdd);
				proxiesToAdd.clear();
			}
		}

		if (!pinlistTypeIsDevice()) {
			getConnectivityToSharedMap().clear();
		}
		if (getConnectivityToSharedMap().isEmpty()) {
			doAutoMap();
		}
		updatePinCountMap();
	}

	@NotNull protected PinProxy createPinProxy(String name)
	{
		return new PinProxy(name, true);
	}

	private boolean hasDuplicatePinNames()
	{
		return getPinNames().size() != fromListModel.getSize();
	}

	@NotNull private Set<String> getPinNames()
	{
		SortedListModel<IAbstractPin> pins = fromListModel;
		Set<String> pinNames = new HashSet<String>(pins.getSize());
		for (int i = 0; i < pins.getSize(); i++) {
			pinNames.add(((IReadOnlyNamedObject) pins.getElementAt(i)).getName());
		}
		return pinNames;
	}

	private void onSharedPinlistChange()
	{
		getConnectivityToSharedMap().clear();

		ISharedPinList sharedPinList = getSharedPinList();
		ISharedPinReservationView dbPinReservation = null;
		if (sharedPinList != null) {
			dbPinReservation = FactoryMgr.getCommonFactory().constructSharedPinReservationView(sharedPinList);
		}
		determineMappability(dbPinReservation);
		onNameChange();
		doAutoMap();
		updatePinCountMap();
	}

	private void onNameChange()
	{
		updatePinCountMap();
	}

	private void determineMappability(@Nullable ISharedPinReservationView dbPinReservation)
	{
		unmappableProxies.clear();

		// If this is a "Share Into", there may be some pre-existing shared pins that cannot be mapped to.
		if (isShare() && getSharedPinList() != null) {
			for (IPinProxy ppp : getProxies()) {
				if (!isMappable(ppp, getDesign(), dbPinReservation)) {
					unmappableProxies.add(ppp);
					IAbstractPin key = getConnectivityToSharedMap().getKey(ppp);
					if (key != null) {
						getConnectivityToSharedMap().remove(key);
					}
				}
			}
		}
	}

	public boolean allowGenerateMapping()
	{
		if (getCablePinlist() == null      // Nothing to map
				|| getConnectivityToSharedMap() == null       // Not ready to map
				|| !getConnectivityToSharedMap().isEmpty()    // Something is already mapped
				|| hasDuplicatePinNames()) { // Duplicate pin names - can happen for symbols
			return false;
		}
		return generateMapping(null, false, () -> true);
	}

	private void doAutoMap()
	{
		if (isShare() && getSharedPinList() != null) {
			// Normally we don't automatically generate a mapping in a 'Share Into', but we make an exception
			// for splices since there is only one possible mapping.
			if (getSharedPinList().getType() == PinListTypeEnum.TypeSplice) {
				//Assumption: the fromList has the same number of pins as the toList (this is enforced by how the
				//Share into list is filtered)
				for (int i = 0; i < fromListModel.getSize(); i++) {
					IPinProxy pinproxy = i < toListModel.getSize() ? toListModel.getElementAt(i) : null;
					getConnectivityToSharedMap().put(fromListModel.getElementAt(i), pinproxy);
				}
			}
		}
		else {
			if (allowGenerateMapping()) {
				doGenerateMapping(false, () -> true);
			}
		}
	}

	@Nullable
	private IPinProxy findMatchingProxyByName(@NotNull String name, @NotNull SortedListModel<IPinProxy> proxies)
	{
		for (IPinProxy proxy : proxies) {
			if (name.equalsIgnoreCase(proxy.getName())) {
				return proxy;
			}
		}
		return null;
	}

	public boolean doGenerateMapping(boolean bShowCreatePinWarning,
			@NotNull Supplier<Boolean> pinCreationConfirmation)
	{
		return generateMapping(getConnectivityToSharedMap(), bShowCreatePinWarning, pinCreationConfirmation);
	}

	/**
	 * @param outputMapping If it's getConnectivityToSharedMap(), mapping for output. Otherwise the call is just a
	 *                      dry-run to test for doability
	 * @return If passed getConnectivityToSharedMap(), true means that the mapping was successful, outputMapping is the
	 * generated mapping, and the model contains any proxies that needed to be generated. False means that the mapping
	 * failed and the output mapping is empty. If passed a map other than getConnectivityToSharedMap(), true means that
	 * the mapping will be successful if passed getConnectivityToSharedMap() and the same value in the automatic
	 * parameter, provided the state of the model does not change. False means that the repeat call will fail.
	 */

	private boolean generateMapping(@Nullable Map<IAbstractPin, IPinProxy> outputMapping, boolean bShowCreatePinWarning,
			@NotNull Supplier<Boolean> pinCreationConfirmation)
	{
		Map<IAbstractPin, IPinProxy> mapping;
		List<IPinProxy> proxies;
		if (outputMapping == null) {
			mapping = new HashMap<IAbstractPin, IPinProxy>();
			proxies = CollectionUtils.createList(getProxies().iterator());
		}
		else if (outputMapping == getConnectivityToSharedMap()) {
			outputMapping.clear();
			mapping = outputMapping;
			proxies = getProxies();
		}
		else {
			return false; // Method should only be called with getConnectivityToSharedMap() or null map
		}

		List<IAbstractPin> pins = new ArrayList<IAbstractPin>(fromListModel);
		//assuming proxies and pins are aleady sorted.
		//if not then need to sort them 1st to have consistent result.
		ListMap<String, IPinProxy> proxiesByName = new ListMap<>();
		ListMap<Integer, IPinProxy> proxiesByIndex = new ListMap<>();
		for (IPinProxy proxy : proxies) {
			String proxyName = proxy.getName();
			proxiesByName.add(proxyName, proxy);
			proxiesByIndex.add(CommonUtils.extractIndex(proxyName), proxy);
		}

		// Find the proxies with names identical to pins and other, mappable proxies.
		// Try to find a pin that matches each proxy by name or index
		// First, try for an exact match. If that fails, go through the pins and see if one matches this proxy by index
		// OK, we can't map to a proxy with the same name or index, but there is an existing differently-named proxy
		// that we can map to.
		//IF CPIN HAS ALL REFERENCE USAGES IN CURRENT DESIGN (DB & TRANSIENT), then we should be able to map it.
		//IT IS EQUIVALENT TO PLACING A NON-REUSABLE PIN AS REFERENCE IN CURRENT DESIGN

		Map<IPinProxy, Boolean> proxyMappability = new HashMap<>();
		Map<IAbstractPin, Boolean> onlyReferences = new HashMap<>();
		Set<IPinProxy> consumedProxies = new HashSet<>();
		//stage 1: Match by full name.
		for (Iterator<IAbstractPin> pinItr = pins.iterator(); pinItr.hasNext(); ) {
			IAbstractPin cpin = pinItr.next();
			boolean referenceUsageOnly = onlyReferences.computeIfAbsent(cpin, p -> areAllUsagesReference(p));
			boolean mapped = false;
			for (IPinProxy proxy : proxiesByName.pullReadOnlySafeList(cpin.getName())) {
				if (consumedProxies.contains(proxy)) {
					continue;
				}

				boolean mappable = referenceUsageOnly ||
						proxyMappability.computeIfAbsent(proxy, p -> isMappable(p, getDesign(), null));
				if (outputMapping == null && !mappable && !(extendedPinMatch || m_pinCreationAllowed)) {
					reportError(ResourceMgr
							.getString(MapPanel.class, "MapPanel.associate.cannotUseReservedPin"));
				}
				if (mappable && arePinAndProxyMappable(cpin, proxy)) {
					mapping.put(cpin, proxy);
					consumedProxies.add(proxy);
					pinItr.remove();
					mapped = true;
					break;
				}
			}
			if (!mapped && !extendedPinMatch && !m_pinCreationAllowed) {
				break;
			}
		}

		if (extendedPinMatch) {
			//stage 2: Match by postfix index name.
			for (Iterator<IAbstractPin> pinItr = pins.iterator(); pinItr.hasNext(); ) {
				IAbstractPin cpin = pinItr.next();
				int index = CommonUtils.extractIndex(cpin.getName());
				if (index < 0) {
					continue;
				}

				boolean referenceUsageOnly = onlyReferences.computeIfAbsent(cpin, p -> areAllUsagesReference(p));
				for (IPinProxy proxy : proxiesByIndex.pullReadOnlySafeList(index)) {
					if (consumedProxies.contains(proxy)) {
						continue;
					}

					boolean mappable = proxyMappability.computeIfAbsent(proxy, p -> isMappable(p, getDesign(), null));
					if ((referenceUsageOnly || mappable) && arePinAndProxyMappable(cpin, proxy)) {
						consumedProxies.add(proxy);
						mapping.put(cpin, proxy);
						pinItr.remove();
						break;
					}
				}
			}

			//stage 3: map with remaining proxies serially.
			List<IPinProxy> remainingProxies = new ArrayList<>(proxies.size());
			for (IPinProxy proxy : proxies) {
				if (!consumedProxies.contains(proxy)) {
					remainingProxies.add(proxy);
				}
			}

			for (Iterator<IAbstractPin> pinItr = pins.iterator(); pinItr.hasNext(); ) {
				IAbstractPin cpin = pinItr.next();
				boolean referenceUsageOnly = onlyReferences.computeIfAbsent(cpin, p -> areAllUsagesReference(p));
				for (Iterator<IPinProxy> remainingProxyItr = remainingProxies.iterator();
						remainingProxyItr.hasNext(); ) {
					IPinProxy proxy = remainingProxyItr.next();

					boolean mappable = proxyMappability.computeIfAbsent(proxy, p -> isMappable(p, getDesign(), null));
					if ((referenceUsageOnly || mappable) && arePinAndProxyMappable(cpin, proxy)) {
						mapping.put(cpin, proxy);
						pinItr.remove();
						remainingProxyItr.remove();
						break;
					}
				}
			}
		}

		//stage 4: create new proxies.
		// Anything left over must be a pin that matches an unmappable proxy by name. For these, generate proxies with
		// default indexed names if allowed.
		boolean bCreatedNewPin = false;
		List<IPinProxy> newProxies = new ArrayList<IPinProxy>();
		if (m_pinCreationAllowed) {
			int pinIndex = findHighIndex(proxies) + 1;
			String pinPrefix = getPinPrefix();
			for (Iterator<IAbstractPin> pinItr = pins.iterator(); pinItr.hasNext() && allowAddPins(); ) {
				IAbstractPin cpin = pinItr.next();
				String cpinName = cpin.getName();
				String newProxyName = cpinName;
				if (proxiesByName.contains(newProxyName)) {
					newProxyName = (pinPrefix + pinIndex);
					pinIndex++;
				}
				PinProxy newDefaultNamedProxy = createPinProxy(newProxyName);
				bCreatedNewPin = true;
				proxiesByName.add(newProxyName, newDefaultNamedProxy);

				if (bShowCreatePinWarning) {
					newProxies.add(newDefaultNamedProxy);
				}
				else {
					proxies.add(newDefaultNamedProxy);
				}
				mapping.put(cpin, newDefaultNamedProxy);
			}
		}

		if (mapping.size() == fromListModel.getSize()) {
			if (bShowCreatePinWarning && bCreatedNewPin) {
				boolean ignore = true;
				if (!pinCreationConfirmation.get()) {
					ignore = false;
				}
				if (!ignore) {
					// Do we need to delete the newProxies ?
					newProxies.clear();
					mapping.clear();
					return false;
				}
			}

			for (IPinProxy proxy : newProxies) {
				proxies.add(proxy);
			}
			return true;
		}
		else {
			mapping.clear();
			return false;
		}
	}

	private boolean areAllUsagesReference(@NotNull IAbstractPin devicePin)
	{
		//we  need to ensure DB usages are reference too.
		IDesignWideUsageMgr usageManager = getDesign().getDesignWideUsageMgr();
		List<IDesignSharedUsage> allUsages = usageManager.getUsages(devicePin);
		for (IDesignSharedUsage usage : allUsages) {
			if (usage.getLogicObjectUID().equals(devicePin.getUID())) {
				if (!usage.isReference()) {
					return false;
				}
			}
		}
		return !allUsages.isEmpty();
	}

	/**
	 * determine of the given pin and proxy(from ToList) are mappable.
	 *
	 * @param fromPin - pin from fromList
	 * @param to      - proxy from tolist
	 * @param result  - populate the result, so that it can be displayed as tooltip on 'associcate' button
	 * @return - true if mappable, in the following cases 1. always mappable in case of Inlines 2. In case of device &
	 * connector, mappable if the connected mates are mergeable 3. In case of shared device, if the mapping won't result
	 * in changing the type of library pin
	 */
	private boolean arePinAndProxyMappable(@NotNull IAbstractPin fromPin, @NotNull IPinProxy to,
			@NotNull ResultOfAssociationCheck result)
	{
		if (fromPin.getOwner() instanceof IGenericInlineConnector) {
			return true;
		}

		IAbstractPin toPin = to.getCablePin();
		ISharedPin sharedPin = to.getSharedPin();
		if (toPin == null && sharedPin != null) {
			toPin = getCablePinCorrespondingToSharedPin(sharedPin);
		}
		if (toPin != null && !areMatesMergeable(fromPin, toPin)) {
			result.setResult(AssociationResult.INCOMPATIBLE_MATED_CONNECTORS);
			return false;
		}
		if (sharedPin instanceof ISharedDevicePin && sharedPin.getOwner().getLibraryRef() != null) {
			if (!((ISharedDevicePin) sharedPin).isStud() && ((IDevicePin) fromPin).isStud()) {
				result.setResult(AssociationResult.CANNOT_CONVERT_LIBRARY_PINTYPE);
				return false;
			}
		}
		if (sharedPin instanceof ISharedDevicePin && sharedPin.getOwner().isFrozen()) {
			if (!((ISharedDevicePin) sharedPin).isStud() && ((IDevicePin) fromPin).isStud()) {
				result.setResult(AssociationResult.CANNOT_CONVERT_FROZEN_PINTYPE);
				return false;
			}
		}
		return true;
	}

	/**
	 * determines if the mates of the fromPin and toPin are mergeable
	 *
	 * @param fromPin - which is going to be shared-Into
	 * @param toPin   - to which a pin is going to be be shared into
	 * @return 1.False, In case of non-device pins (they won't support multiple mates) 2.False, in case from-pin &
	 * to-pin are connected to plug-plug "or" plug-RT "or" RT-plug 3.False, in case from-pin & to-pin are connected, but
	 * none of them is stud
	 */
	private boolean areMatesMergeable(@NotNull IAbstractPin fromPin, @NotNull IAbstractPin toPin)
	{
		Collection<IAbstractPin> connPinsOfFromPin = fromPin.getConnectedPins();
		Collection<IAbstractPin> connPinsOfToPin = toPin.getConnectedPins();
		if (connPinsOfFromPin.isEmpty() || connPinsOfToPin.isEmpty()) {
			return true;
		}

		if (fromPin instanceof IDevicePin && toPin instanceof IDevicePin) {
			//no pin other than a device pin can accomodate multiple mates
			IConnector fromPinOwner = (IConnector) connPinsOfFromPin.iterator().next().getOwner();
			IConnector toPinOwner = (IConnector) connPinsOfToPin.iterator().next().getOwner();
			if (fromPinOwner != null && toPinOwner != null) {
				return !mateCompatibilityCheck || isRingTermToStudConnection((IDevicePin) fromPin, (IDevicePin) toPin,
						fromPinOwner, toPinOwner);
			}
			return false;
		}

		return !mateCompatibilityCheck ||
				(connPinsOfFromPin.size() == connPinsOfToPin.size() && connPinsOfFromPin.containsAll(connPinsOfToPin));
	}

	private boolean isRingTermToStudConnection(@NotNull IDevicePin fromPin, @NotNull IDevicePin toPin,
			@NotNull IConnector fromPinOwner, @NotNull IConnector toPinOwner)
	{
		return fromPinOwner.isRingTerminal() && toPinOwner.isRingTerminal() &&
				(toPin.isStud() || fromPin.isStud());
	}

	public boolean arePinAndProxyMappable(@NotNull IAbstractPin fromPin, @NotNull IPinProxy to)
	{
		ResultOfAssociationCheck result = new ResultOfAssociationCheck();
		final boolean areMappable = arePinAndProxyMappable(fromPin, to, result);
		if (!areMappable) {
			reportError(getAssociationMessage(result.getResult()));
		}
		return areMappable;
	}

	@Nullable private IAbstractPin getCablePinCorrespondingToSharedPin(@NotNull ISharedPin sharedPin)
	{
		IConnectivity connectivity = getDesign().getConnectivity();
		if (connectivity != null) {
			IPinList cablePinList = connectivity.findSharedPinList(sharedPin.getOwner());
			if (cablePinList != null) {
				return cablePinList.findSharedPin(sharedPin);
			}
		}
		return null;
	}

	private int findHighIndex(@NotNull List<IPinProxy> proxies)
	{
		int highIndex = 0;
		String prefix = getPinPrefix();
		for (IPinProxy proxy : proxies) {
			String name = proxy.getName();
			if (name.startsWith(prefix)) {
				int n = CommonUtils.parseIndex(name.substring(prefix.length()));
				highIndex = Math.max(highIndex, n);
			}
		}
		return highIndex;
	}

	private boolean isMappable(@NotNull IPinProxy pp, @NotNull ILogicDesign design,
			@Nullable ISharedPinReservationView dbPinReservations)
	{
		if (!isShare()) {
			return true; // We're just mapping a symbol pin to the shared pin, can map to anything.
		}
		ISharedPin sharedPin = pp.getSharedPin();

		return (sharedPin != null && sharedPin.isReusable()) || isUsable(pp, design) ||
				isAnnexable(pp, design, dbPinReservations);
	}

	private boolean isUsable(@NotNull IPinProxy pp, @NotNull ILogicDesign design)
	{
		ISharedPin spin = pp.getSharedPin();
		if (getReusableProxies().contains(pp) || spin == null) {
			return true; // Shared pin has been made reusable, or is being created
		}
		else {
			return SharedPinHelper.isUsable(spin, design);
		}
	}

	private boolean isAnnexable(@NotNull IPinProxy pp, @NotNull ILogicDesign design,
			@Nullable ISharedPinReservationView dbPinReservations)
	{
		ISharedPin spin = pp.getSharedPin();
		if (spin == null) {
			return true; // Pin is being created
		}
		else {
			if (dbPinReservations != null) {

				CommonInSharedPinDBReservationAndDesignScope commonInSharedPinDBReservationAndDesignScope =
						SharedPinHelper.createSharedPinDBReservationScope(
								design,
								dbPinReservations.getSharedPinDBReservations(spin), spin);
				return SharedPinHelper.isAvailable(spin,  commonInSharedPinDBReservationAndDesignScope);
			}
			return SharedPinHelper.isAvailable(spin, design);
		}
	}

	private boolean allowAssociate(@NotNull ResultOfAssociationCheck result, @Nullable IAbstractPin from,
			@Nullable IPinProxy to)
	{
		//areAllUsagesReference(from) : IF CPIN HAS ALL REFERENCE USAGES IN CURRENT DESIGN, then we should be able to map it.
		//IT IS EQUIVALENT TO PLACING A NON-REUSABLE PIN AS REFERENCE IN CURRENT DESIGN
		return from != null && to != null && !(getConnectivityToSharedMap().get(from) == to) &&
				(!unmappableProxies.contains(to) || areAllUsagesReference(from)) &&
				from.isInterconnect() == to.isInterconnect() &&
				arePinAndProxyMappable(from, to, result);
	}

	public boolean allowUnassociate(@Nullable IAbstractPin from, @Nullable IPinProxy to)
	{
		return from != null && to != null && getConnectivityToSharedMap().get(from) == to;
	}

	@NotNull public String makeBlockPinName(@NotNull IAbstractPin pin)
	{
		String blkName = pinBlockNameMap.get(pin);
		if (blkName != null) {
			return blkName + ':' + pin.getName();
		}

		return pin.getName();
	}

	public boolean isMapperValid()
	{
		boolean mapperValid = true;
		IPinList cpl = getCablePinlist();
		ISharedPinList spl = getSharedPinList();
		if (spl == null
				&& (getSharedPinListName() == null || getSharedPinListRevision() == null
				|| ((cpl instanceof IInlineJackConnector || cpl instanceof IInlinePlugConnector)
				&& getSharedPinListMateName() == null))) {
			mapperValid = false;
		}
		if (mapperValid) {
			mapperValid = allowAddPins() || cpl != null;
		}
		return mapperValid;
	}

	private void populateTheFromList(@NotNull IPinList cablePL)
	{
		if (getSymbolDef() == null) {
			// This is a share of a parameterized object. Find the pins to map from in the diagram schem object.
			for (IAbstractPin cpin : cablePL.getPins()) {
				if (cpin instanceof IBackshellTermination) {
					continue;
				}
				fromListModel.add(cpin);
			}
		}
		else {
			// If this is a composite symbol that has multiple instances of a block, we need to artificially
			// index the block names so that the user can see some distinction between identically named pins on
			// different instances of a block.
			Map<IUID, IBlock> symPinBlockMap = new HashMap<IUID, IBlock>();
			ICounter<String> blockNameCounter = new Counter<String>();
			Map<IBlock, Integer> blockIndexMap = new HashMap<IBlock, Integer>();
			ISymbolDef sym = getSymbolDef();
			for (IBlockIterator bitr = sym.getBlocks(); bitr.hasNext(); ) {
				IBlock blk = bitr.getNext();
				blockNameCounter.increment(blk.getName());
				blockIndexMap.put(blk, blockNameCounter.getCount(blk.getName()));
				for (Object o : blk.getGfx().getObjects(IPin.class)) {
					IPin pin = (IPin) o;
					symPinBlockMap.put(pin.getConnectivity().getUID(), blk);
				}
			}
			List<IAbstractPin> pins = new ArrayList<IAbstractPin>();
			if (!isShare()) {
				// This is an edit of an existing shared pinlist, and the user is adding a symbol. Go through
				// The symbol itself to find the connectivity pins to map.
				for (IPin pin : sym.getGfx().getObjects(IPin.class)) {
					fromListModel.add(pin.getConnectivity());
				}
				// Add the blocks.
				for (IBlockIterator bitr = sym.getBlocks(); bitr.hasNext(); ) {
					IBlock blk = bitr.getNext();
					for (IPin pin : blk.getGfx().getObjects(IPin.class)) {
						pins.add(pin.getConnectivity());
						if (blockNameCounter.getCount(blk.getName()) > 1) {
							pinBlockNameMap
									.put(pin.getConnectivity(), blk.getName() + "[" + blockIndexMap.get(blk) + "]");
						}
						else {
							pinBlockNameMap.put(pin.getConnectivity(), blk.getName());
						}
					}
				}
			}
			else {
				// This is a share of a symbol instance. Go through the schem pinlist of the instance  to find
				// the connectivity pins to map.
				for (IAbstractPin cPin : cablePL.getPins()) {
					pins.add(cPin);
					IBlock blk = symPinBlockMap.get(cPin.getReference());
					if (blk != null) {
						if (blockNameCounter.getCount(blk.getName()) > 1) {
							pinBlockNameMap.put(cPin, blk.getName() + "[" + blockIndexMap.get(blk) + "]");
						}
						else {
							pinBlockNameMap.put(cPin, blk.getName());
						}
					}
				}
			}
			fromListModel.addAll(pins);
		}
	}

	private void createLibraryPinProxies(@NotNull IPinList cablePL, List<IPinProxy> proxiesToAdd,
			@NotNull ILibraryObject libraryObject)
	{
		// DR 402080 - don't want additional pins
		// TODO: Consider implications of this for InterconnectDevice. Missing pins mean the part has changed
		// TODO: since the device was bound to it. This algorithm does not fully sync the part to the library
		// TODO definition, would it be better not to add the missing pins at all?
		if (!(cablePL instanceof IInterconnectConnector)
				&& libraryObject.getNumCavities() > cablePL.getNumPins() && !isPinListUsesBlockSymbol()) {

			// The pinlist to be shared has a library part, but fewer pins than there are cavities on the part.
			// Generate enough additional proxies to complete the library definition. Use cavitiy names that are not
			// already taken by pins on the pinlist to be shared.

			//
			// Use the library pin Facade - this will squish device connectors into
			// a single pin - NOTE, ONLY use it for interconnects.
			//
			ILibraryDeviceFootprint lfp = null;
			if (cablePL instanceof IInterconnectDevice) {
				lfp = ((IInterconnectDevice) cablePL).getFootprint();
			}
			LibraryPinFacade facade = new LibraryPinFacade(libraryObject, lfp, cablePL.getClass(), true);
			for (String cavName : facade.getAllPinNames()) {
				if (cablePL.findPinByName(cavName) == null) {
					PinProxy proxy = createPinProxy(cavName);
					proxiesToAdd.add(proxy);
					//
					// If the cavity is a 'squished' one, then it is an interconnecxt pin.
					//
					if (facade.isDevConn(cavName)) {
						proxy.setInterconnect(true);
					}
				}
			}
		}
	}

	private boolean isPinListUsesBlockSymbol()
	{
		ISymbolDef sym = getSymbolDef();
		return (sym != null) && (sym.getNumBlocks() > 0);
	}

	@NotNull private String getAssociationMessage(@NotNull AssociationResult result)
	{
		String tooltip = messageForAssociateAttempt.get(result);
		if (tooltip == null) {
			tooltip = "PinMapper.associate.tooltip";
		}
		return ResourceMgr.getString(MapPanel.class, tooltip);
	}

	@NotNull public Map<String, Integer> getPinNameToCountMap()
	{
		return pinNameToCountMap;
	}

	@Nullable public String getSharePinProxyName(IAbstractPin pin)
	{
		PinProxy pp = (PinProxy) getConnectivityToSharedMap().get(pin);
		return pp == null ? null : pp.getName();
	}

	public void unassociateAll()
	{
		getConnectivityToSharedMap().clear();
	}

	public void unassociate(@Nullable IAbstractPin pin, @Nullable IPinProxy proxy)
	{
		getConnectivityToSharedMap().remove(pin);
		getConnectivityToSharedMap().remove(getConnectivityToSharedMap().getKey(proxy));
	}

	public void associateAll(@NotNull Supplier<Boolean> pinCreationConfirmation)
	{
		doGenerateMapping(true, pinCreationConfirmation);
		//dts0100839966: the associate all action may add new pin proxies so need to update the pincount map.
		updatePinCountMap();
	}

	public void associate(@Nullable IAbstractPin pin, @Nullable IPinProxy proxy)
	{
		unassociate(pin, proxy);
		getConnectivityToSharedMap().put(pin, proxy);
	}

	public void removeIntervalFromMapping()
	{
		for (ISymbolDef symbolDef : getModel().getSymbolDefsForAddition()) {
			ReverseMap<IAbstractPin, IPinProxy> connectivityToSharedMap =
					getModel().getConnectivityToSharedMap(symbolDef);
			for (Iterator<IAbstractPin> iterator = connectivityToSharedMap.keySet().iterator(); iterator.hasNext(); ) {
				IAbstractPin pin = iterator.next();
				PinProxy ppp = (PinProxy) connectivityToSharedMap.get(pin);
				if (!toListModel.contains(ppp)) {
					iterator.remove();
				}
			}
		}
	}

	public void updatePinCountMap()
	{
		pinNameToCountMap.clear();
		for (IPinProxy pp : getProxies()) {
			String pinName = pp.getName();
			Integer pinCount = pinNameToCountMap.get(pinName);
			if (pinCount == null) {
				pinNameToCountMap.put(pinName, 1);
			}
			else {
				pinNameToCountMap.put(pinName, pinCount + 1);
			}
		}
	}

	public boolean canShowUnavailablePins()
	{
		return !unmappableProxies.isEmpty();
	}

	@NotNull protected String getPinPrefix()
	{
		final IProject project = getDesign().getProject();
		return project != null ? project.getNameMgr().getObjectPrefix(INameMgr.PIN).getString() :
				StringUtils.EMPTY_STRING;
	}

	public boolean allowAssociate(@Nullable IAbstractPin from, @Nullable IPinProxy to, @NotNull StringBuilder message)
	{
		ResultOfAssociationCheck result = new ResultOfAssociationCheck();
		final boolean allowAssociate = allowAssociate(result, from, to);
		message.append(getAssociationMessage(result.getResult()));
		return allowAssociate;
	}

	@Nullable public IAbstractPin getAssociatedPin(@Nullable IPinProxy to)
	{
		return getConnectivityToSharedMap().getKey(to);
	}

	public boolean allowUnassociateAll()
	{
		return !getConnectivityToSharedMap().isEmpty();
	}

	@Nullable public IPinProxy getAssociatedProxy(@Nullable IAbstractPin pin)
	{
		return getConnectivityToSharedMap().get(pin);
	}

	public boolean isUnmappableProxy(@Nullable IPinProxy proxy)
	{
		return unmappableProxies.contains(proxy);
	}

	public boolean hasMapping(@Nullable Object value)
	{
		return getConnectivityToSharedMap().containsKey(value);
	}
}
