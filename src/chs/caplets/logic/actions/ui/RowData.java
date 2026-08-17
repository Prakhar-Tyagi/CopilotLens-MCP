/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2026 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.caplets.logic.actions.ManageConnectorDesignScope;
import chs.caplets.logic.actions.ManageConnectorPinSelections;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedConnectorPin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.ISharedPinUsage;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.ConductorWrapper;
import chs.ctf.caf.utils.IConductorProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.subsystem.logic.manageconnections.IPinProvider;
import chs.subsystem.logic.manageconnections.ISharedPinProvider;
import chs.subsystem.logic.manageconnections.ManageConnectionsServices;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.HybridSet;
import chs.utilities.Pair;
import chs.utility.logic.ManageConnectionsPinProvider;
import chs.utility.logic.PinUtils;
import chs.utility.logic.sharedpinconnection.ConductorProxy;
import chs.utility.logic.sharedpinconnection.SharedPinConnectionFinder;
import chs.utility.ui.PinConductorConnectionSortHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RowData
{

	private ManageConnectorsDialog mManageConnectorsDialog;
	private Collection<IWireConductor> mNewWireConnections = new HybridSet<>();
	private IPinList m_pinList;
	private ISharedPinList mSharedPinList;
	private ISharedPinReservationView sharedPinUsageCache;
	private ManageConnectorDesignScope mDesignsInScope;
	private Collection<IPinProxy> mSharedAndLibraryCavityPins = new LinkedHashSet<>();

	public RowData(ManageConnectorsDialog manageConnectorsDialog)
	{
		mManageConnectorsDialog = manageConnectorsDialog;
	}

	public RowData setNewWireConnections(Collection<IWireConductor> newWireConnections)
	{
		mNewWireConnections.addAll(newWireConnections);
		return this;
	}

	public RowData setPinList(IPinList pinList)
	{
		m_pinList = pinList;
		return this;
	}

	public RowData setSharedPinList(ISharedPinList sharedPinList)
	{
		mSharedPinList = sharedPinList;
		return this;
	}

	public RowData setDesignsInScope(ManageConnectorDesignScope designsInScope)
	{
		mDesignsInScope = designsInScope;
		return this;
	}

	@NotNull
	public Collection<ManageConnectorConnectionsInfo> createData(ManageConnectorPinSelections pinSelections)
	{

		List<ManageConnectorConnectionsInfo> allPinData = new ArrayList<>();
		Map<IDesignDescriptor, Collection<IPinProxy>> pinProxiesInContainer = new LinkedHashMap<>();

		Collection<String> processedPins = new HashSet<>();
		Collection<String> bsProcessedPins = new HashSet<>();
		if (m_pinList != null) {
			createDataForNonSharedPinList(pinSelections, allPinData, pinProxiesInContainer, processedPins, bsProcessedPins);
		}
		if (mSharedPinList != null) {
			createDataForSharedPinList(pinSelections, allPinData, pinProxiesInContainer, processedPins, bsProcessedPins);
		}
		pinSelections.setPins(pinProxiesInContainer, mSharedAndLibraryCavityPins);

		final SortHelperProvider sortHelperProvider =
				mManageConnectorsDialog.getSortHelperProvider();
		sortHelperProvider.resetData(allPinData);
		pinSelections.updateDataUsingSortHelper(sortHelperProvider);
		allPinData = allPinData.stream().sorted(new Comparator<ManageConnectorConnectionsInfo>()
		{
			@Override public int compare(ManageConnectorConnectionsInfo o1, ManageConnectorConnectionsInfo o2)
			{
				Comparable<?> o1comparable = sortHelperProvider.get().createComparableForPin(o1.getFirst());
				Comparable<?> o2Comparable = sortHelperProvider.get().createComparableForPin(o2.getFirst());
				return PinConductorConnectionSortHelper.getDefaultComparator().compare(o1comparable, o2Comparable);
			}
		}).collect(Collectors.toList());

		Collection<ManageConnectorConnectionsInfo> manageConnectorConnectionsInfos = new LinkedHashSet<>();
		manageConnectorConnectionsInfos.addAll(allPinData);

		return manageConnectorConnectionsInfos;
	}

	protected void createDataForNonSharedPinList(ManageConnectorPinSelections manageConnectorPinSelections,
			List<ManageConnectorConnectionsInfo> allPinData,
			Map<IDesignDescriptor, Collection<IPinProxy>> pinProxiesInContainer, Collection<String> processedPins,
			Collection<String> bsProcessedPins)
	{
		Collection<IPinProxy> pinProxiesInThisContainer = new LinkedHashSet<>();
		List<IAbstractPin> pins = ManageConnectionsServices.requireExtension(m_pinList, IPinProvider.class).getAllPins();

		for (IAbstractPin aPin : pins) {
			IPinProxy aPinProxy = new PinProxy(aPin);
			if (aPin.getSharedPin() != null) {
				aPinProxy.setSharedPin(aPin.getSharedPin());
			}
			pinProxiesInThisContainer.add(aPinProxy);
			addConnectionInfoForPin(aPin, allPinData, aPinProxy, manageConnectorPinSelections);
			if (aPin instanceof IDevicePin) {
				for (IAbstractPin connectorPin : aPin.getConnectedPins()) {
					addConnectionInfoForPin(connectorPin, allPinData, aPinProxy, manageConnectorPinSelections);
				}
			}
			if (aPin instanceof IBackshellTermination) {
				aPinProxy.setName(aPin.getOwner().getName() + ":"+ aPin.getName());
				bsProcessedPins.add(aPin.getName());
			} else {
				processedPins.add(aPin.getName());
			}
			pinProxiesInContainer.put(m_pinList.getLogicDesign(), pinProxiesInThisContainer);
		}

		if (mSharedPinList == null) {
			if (m_pinList.getLibraryObject() != null) {
				ILibraryBaseObject libraryBaseObject = m_pinList.getLibraryObject();
				if (libraryBaseObject instanceof ILibraryCavityContainer) {
					Set<String> excludedCavities = new HashSet<>(processedPins);
					if (m_pinList instanceof IConnector) {
						excludedCavities.addAll(((IConnector) m_pinList).getBlockedCavities());
					}
					((ILibraryCavityContainer) libraryBaseObject).getAllCavities().stream()
							.filter(cavity -> !processedPins.contains(cavity.getName()))
							.filter(cavity -> !excludedCavities.contains(cavity.getName()))
							.forEach(cavity -> addPinProxies(manageConnectorPinSelections, allPinData,
									new PinProxy(cavity, false)));
				}
			}
			if (m_pinList instanceof IConnector connector) {
				IBackshell backshell = connector.getBackshell();
				ILibraryBaseObject bsLibObject = null;
				if (backshell != null && backshell.getLibraryObject() != null) {
					bsLibObject = backshell.getLibraryObject();
				}
				if (bsLibObject != null && bsLibObject instanceof ILibraryCavityContainer bsObj) {
					bsObj.getAllCavities().stream()
							.filter(cavity -> !bsProcessedPins.contains(cavity.getName()))
							.forEach(cavity -> {
								PinProxy pinProxy = new PinProxy(cavity, false);
								if (cavity.getOwner() != null) {
									pinProxy.setName(backshell.getName() + ":" + cavity.getName());
								}
								addPinProxies(manageConnectorPinSelections, allPinData, pinProxy);
							});
				}
			}
		}
	}

	private void addPinProxies(ManageConnectorPinSelections manageConnectorPinSelections,
			List<ManageConnectorConnectionsInfo> allPinData, IPinProxy pinProxy)
	{
		ManageConnectorConnectionsInfo info = new ManageConnectorConnectionsInfo(
				new ManageConnectorConnectionsInfo.Connection(pinProxy, (IConductor) null),
				manageConnectorPinSelections,
				null)
				.setDisabledReason(
						ManageConnectorConnectionsInfo.DisableReason.LIBRARYCAVITYNOTUSED);
		allPinData.add(info);
		mSharedAndLibraryCavityPins.add(pinProxy);
	}

	private void addConnectionInfoForPin(@NotNull IAbstractPin aPin,
			@NotNull List<ManageConnectorConnectionsInfo> allPinData,
			IPinProxy aPinProxy, ManageConnectorPinSelections manageConnectorPinSelections)
	{
		for (IConductor aConductor : aPin.getConductors()) {
			ManageConnectorConnectionsInfo info = new ManageConnectorConnectionsInfo(
					new ManageConnectorConnectionsInfo.Connection(aPinProxy, aConductor),
					manageConnectorPinSelections,
					m_pinList.getLogicDesign());
			info.validatePinForReferenceUsage();
			setStateFlagsForInfo(info);
			allPinData.add(info);
		}
	}

	protected void createDataForSharedPinList(ManageConnectorPinSelections manageConnectorPinSelections,
			List<ManageConnectorConnectionsInfo> allPinData,
			Map<IDesignDescriptor, Collection<IPinProxy>> pinProxiesInContainer, Collection<String> processedPins,
			Collection<String> bsProcessedPins)
	{
		List<ISharedPin> sharedPins =
				ManageConnectionsServices.requireExtension(mSharedPinList, ISharedPinProvider.class).getAllSharedPins();
		Collection<IDesignDescriptor> editableDesigns = mDesignsInScope.getEditableDesigns();
		Collection<IUID> designUIDsInScope =
				mDesignsInScope.getDesignsInScope().stream().map(aDesign -> aDesign.getUID())
						.collect(Collectors.toSet());
		SharedPinConnectionFinder sharedPinConnectionFinder = createSharedConnectionFinder(designUIDsInScope);
		loadDSUMs(sharedPinConnectionFinder.getDesignsWherePinsAreConnected());
		IDesignDescriptor ignoreDesignForShared =
				m_pinList != null ? m_pinList.getLogicDesign() : null;

		for (ISharedPin aSharedPin : sharedPins) {
			Map<IDesignDescriptor, Collection<Pair<IPinProxy, IConductorProxy>>> connections =
					sharedPinConnectionFinder.getConnections(aSharedPin);
			if (!connections.isEmpty()) {
				if (aSharedPin instanceof ISharedBackshellTermination) {
					bsProcessedPins.add(aSharedPin.getName());
				} else {
					processedPins.add(aSharedPin.getName());
				}
			}
			for (IDesignDescriptor aDesignDescriptor : connections.keySet()) {
				if (ignoreDesignForShared != null &&
						ignoreDesignForShared.getUID().isEquiv(aDesignDescriptor.getUID()) ||
						!mDesignsInScope.isDesignInScope(aDesignDescriptor)) {
					continue;
				}
				Collection<IPinProxy> pinProxiesInThisContainer =
						getPinProxiesInThisContainer(pinProxiesInContainer, aDesignDescriptor);
				for (Pair<IPinProxy, IConductorProxy> connectionInContainer : connections
						.get(aDesignDescriptor)) {
					IPinProxy aPinProxyInThisContainer = connectionInContainer.getFirst();
					if (aSharedPin instanceof ISharedBackshellTermination) {
						aPinProxyInThisContainer.setName(aSharedPin.getOwner().getName() + ":" + aSharedPin.getName());
					}
					pinProxiesInThisContainer.add(aPinProxyInThisContainer);
					boolean shouldCreateConnectionsInfo = true;
					ManageConnectorConnectionsInfo.DisableReason disableReason =
							ManageConnectorConnectionsInfo.DisableReason.NONE;
					if (!mDesignsInScope.isDesignEditable(aDesignDescriptor)) {
						if (editableDesigns.size() == 1) {
							IDesignDescriptor loneDesignBeingEdited = editableDesigns.iterator().next();
							if (manageConnectorPinSelections
									.canUseThePinInDesign(aPinProxyInThisContainer, loneDesignBeingEdited)) {
								disableReason =
										ManageConnectorConnectionsInfo.DisableReason.INSTANCEOFSHAREDPINUSABLEINCURRENTDESIGN;
							}
							else {
								disableReason =
										ManageConnectorConnectionsInfo.DisableReason.INSTANCEOFSHAREDPINNOTUSABLEINCURRENTDESIGN;
							}
						}
						else {
							IDesignContainer design = aDesignDescriptor.getDesignContainer();
							if (design != null) {
								disableReason =
										ManageConnectorConnectionsInfo.DisableReason.DESIGNCANNOTBELOCKED;
								boolean isDesignEditable =
										(design.isEditable() &&
												!CAFUtils.getInstance().isDesignOpenReadOnly(design));
								if (!isDesignEditable) {
									disableReason =
											ManageConnectorConnectionsInfo.DisableReason.DESIGNCANNOTBEEDITED;
								}
							}
							else {
								shouldCreateConnectionsInfo = false;
							}
						}
					}
					if (shouldCreateConnectionsInfo) {
						ManageConnectorConnectionsInfo info = new ManageConnectorConnectionsInfo(
								new ManageConnectorConnectionsInfo.Connection(aPinProxyInThisContainer,
										connectionInContainer.getSecond()),
								manageConnectorPinSelections, aDesignDescriptor)
								.setDisabledReason(disableReason)
								.setSharedPinMateUID(
										sharedPinConnectionFinder.getSharedMatePinForUnloadedDesign(
												aDesignDescriptor.getUID(),
												aSharedPin.getUID()));
						setStateFlagsForInfo(info);
						allPinData.add(info);
						info.validatePinForReferenceUsage();
					}
				}
			}
		}

		Map<String, IPinProxy> sharedPinToPinProxyMap =
				sharedPinConnectionFinder.getSharedPinToUnconnectedPinProxyMap(mSharedPinList);
		manageConnectorPinSelections.setUnconnectedPinProxies(sharedPinToPinProxyMap.values());

		Set<IPinProxy> pinProxies =
				createUnconnectedPinProxies(manageConnectorPinSelections, allPinData, processedPins, bsProcessedPins,
						editableDesigns, sharedPinToPinProxyMap);

		//Populate design names for shared pins which are not connected to any conductor
		if (getSharedPinReservationView() != null && !pinProxies.isEmpty()) {
			PinUtils.populateSharedPinUsages(getSharedPinReservationView(), pinProxies,
					mDesignsInScope.getDesignsInScope());
		}
		// Populate design names for shared backshell pins which are not connected to any conductor
		populateSharedPinUsagesForUnConnectedBackshellPins(pinProxies);
	}

	private void populateSharedPinUsagesForUnConnectedBackshellPins(Set<IPinProxy> pinProxies)
	{
		Set<IUID> validDesignsInScope = mDesignsInScope.getDesignsInScope().stream()
				.map(designDescriptor -> designDescriptor.getUID())
				.collect(Collectors.toSet());
		IProjectSharedUsageView sharedUsageView = mSharedPinList.getProject().getSharedUsageView();
		for (IPinProxy pinProxy : pinProxies) {
			if (pinProxy.getSharedPin() != null && pinProxy.getSharedPin() instanceof ISharedBackshellTermination) {
				Collection<ISharedPinUsage> usages =
						sharedUsageView.getUsagesOf(ISharedPinUsage.class, pinProxy.getSharedPin(),
								validDesignsInScope);
				Set<IUID> designUIDs = usages.stream().map(usage -> usage.getDesignUID()).collect(Collectors.toSet());
				designUIDs.retainAll(validDesignsInScope);
				pinProxy.addAttribute(null, IAttributeTypes.DESIGN, PinUtils.getDesignInfoDisplayString(designUIDs));
			}
		}
	}

	private void loadDSUMs(@NotNull Set<IUID> designUIDs)
	{
		IProject project = mSharedPinList.getProject();
		if (project != null) {
			project.getSharedUsageView().loadDSUMs(designUIDs, null);
		}
	}

	@NotNull
	private Set<IPinProxy> createUnconnectedPinProxies(ManageConnectorPinSelections manageConnectorPinSelections,
			List<ManageConnectorConnectionsInfo> allPinData, Collection<String> processedPins, Collection<String> bsProcessedPins,
			Collection<IDesignDescriptor> editableDesigns, Map<String, IPinProxy> sharedPinToPinProxyMap)
	{
		Set<IPinProxy> pinProxies = new HashSet<>();
		for (ISharedPin aSharedPin : getRemainingSharedPins(processedPins, bsProcessedPins)) {
			IPinProxy pinProxy = sharedPinToPinProxyMap.get(aSharedPin.getUID().getString());
			if (pinProxy == null) {
				pinProxy = new PinProxy(aSharedPin);
			}
			if(aSharedPin instanceof ISharedBackshellTermination) {
				pinProxy.setName(aSharedPin.getOwner().getName() + ":" + aSharedPin.getName());
			}
			pinProxies.add(pinProxy);
			IDesignDescriptor loneDesignBeingEdited = null;
			if (editableDesigns.size() == 1) {
				loneDesignBeingEdited = editableDesigns.iterator().next();
			}
			ManageConnectorConnectionsInfo.DisableReason disableReason;
			if (manageConnectorPinSelections.canUseThePinInDesign(pinProxy, loneDesignBeingEdited)) {
				disableReason =
						ManageConnectorConnectionsInfo.DisableReason.UNPLACEDINSTANCEOFSHAREDPINUSABLEINCURRENTDESIGN;
			}
			else {
				disableReason =
						ManageConnectorConnectionsInfo.DisableReason.UNPLACEDINSTANCEOFSHAREDPINNOTUSABLEINCURRENTDESIGN;
			}
			ManageConnectorConnectionsInfo info = new ManageConnectorConnectionsInfo(
					new ManageConnectorConnectionsInfo.Connection(pinProxy, (IConductorProxy) null),
					manageConnectorPinSelections, null)
					.setDisabledReason(disableReason);
			mSharedAndLibraryCavityPins.add(pinProxy);
			setStateFlagsForInfo(info);
			allPinData.add(info);
		}
		return pinProxies;
	}

	@NotNull
	private List<ISharedPin> getRemainingSharedPins(Collection<String> processedPins, Collection<String> bsProcessedPins)
	{
		Predicate<ISharedPin> predicate = (pin) -> {
			if (pin instanceof ISharedBackshellTermination) {
				return !bsProcessedPins.contains(pin.getName());
			}
			if (!processedPins.contains(pin.getName())) {
				if (pin instanceof ISharedConnectorPin) {
					return !((ISharedConnectorPin) pin).isBlockedCavity();
				}
				return true;
			}
			return false;
		};
		return ManageConnectionsServices.requireExtension(mSharedPinList, ISharedPinProvider.class).getAllSharedPins()
				.stream()
				.filter(sPin -> predicate.test(sPin))
				.collect(Collectors.toList());
	}

	@NotNull
	private Collection<IPinProxy> getPinProxiesInThisContainer(
			Map<IDesignDescriptor, Collection<IPinProxy>> pinProxiesInContainer, IDesignDescriptor aDesignDescriptor)
	{
		Collection<IPinProxy> pinProxiesInThisContainer = pinProxiesInContainer.get(aDesignDescriptor);
		if (pinProxiesInThisContainer == null) {
			pinProxiesInThisContainer = new LinkedHashSet<>();
			pinProxiesInContainer.put(aDesignDescriptor, pinProxiesInThisContainer);
		}
		return pinProxiesInThisContainer;
	}

	@NotNull private SharedPinConnectionFinder createSharedConnectionFinder(Collection<IUID> designUIDsInScope)
	{
		Cursor prevCursor = mManageConnectorsDialog.getCursor();
		mManageConnectorsDialog.setEnabled(false);
		mManageConnectorsDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		SharedPinConnectionFinder sharedPinConnectionFinder;
		try {
			sharedPinConnectionFinder =
					new SharedPinConnectionFinder(mSharedPinList, null, designUIDsInScope,
							getSharedPinReservationView(), new ManageConnectionsPinProvider());
		}
		finally {
			mManageConnectorsDialog.setEnabled(true);
			mManageConnectorsDialog.setCursor(prevCursor);
		}
		return sharedPinConnectionFinder;
	}

	private void setStateFlagsForInfo(@NotNull ManageConnectorConnectionsInfo info)
	{
		if (isNewConnection(info.getSecond())) {
			info.setIsNewConnection();
		}

		info.setLibraried(PinProxyHelper.isPartAssigned(info.getFirst()));
	}

	private boolean isNewConnection(@Nullable IConductorProxy conductorProxy)
	{
		final IConductor conductor = getConductorFromProxy(conductorProxy);
		if (conductor != null) {
			return isNewConnection(conductor);
		}
		return false;
	}

	@Nullable
	private IConductor getConductorFromProxy(@Nullable IConductorProxy conductorProxy)
	{
		if (conductorProxy instanceof ConductorProxy) {
			return ((ConductorProxy) conductorProxy).getConductor();
		}
		if (conductorProxy instanceof ConductorWrapper) {
			return ((ConductorWrapper) conductorProxy).getCableConductor();
		}
		return null;
	}

	private boolean isNewConnection(@NotNull IConductor conductor)
	{
		if (mNewWireConnections.contains(conductor)) {
			return true;
		}
		for (IAbstractPin pin : conductor.getPins()) {
			if (pin instanceof IConnectorPin) {
				final IAbstractPin connectedPin = ((IConnectorPin) pin).getConnectedPinForConnectorPin();
				if (connectedPin != null &&
						CollectionUtils.containsAtLeastOne(connectedPin.getConductorsAsSet(), mNewWireConnections)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable public ISharedPinReservationView getSharedPinReservationView()
	{
		if (mSharedPinList != null && sharedPinUsageCache == null) {
			sharedPinUsageCache =
					FactoryMgr.getCommonFactory().constructSharedPinReservationView(mSharedPinList);
		}
		return sharedPinUsageCache;
	}
}