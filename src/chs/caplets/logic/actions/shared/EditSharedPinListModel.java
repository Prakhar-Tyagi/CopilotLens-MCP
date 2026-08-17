/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.cof.library.IFootprintable;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IInterconnectConnector;
import chs.cof.logical.cable.IInterconnectDevice;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedConnectorPin;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedInterconnectDevice;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.IStampIterator;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.IReadOnlyNamedObject;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ReverseMap;
import chs.utilities.ui.SortedListModel;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.ValidityChangeEvent;
import chs.utility.IObjectInUseService;
import chs.utility.LibraryPinFacade;
import chs.utility.SymbolUtils;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.logic.PinUtils;
import chs.utility.ui.SharedPinListSymbolInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"MethodOnlyUsedFromInnerClass"}) public class EditSharedPinListModel
		implements IPropertyValidityListener
{

	@Nullable private IPinList schemPinlist;
	@Nullable private chs.cof.logical.cable.IPinList cablePinlist = null;
	private ISharedPinList sharedPinList; // also nullable (always has been) but causes too much noise in IJ
	private ISymbolDef symbolDef;
	private final boolean isSymbolDefEditable;
	private final boolean isShare;
	private String sharedPinListName;
	private String sharedPinListRevision;
	private boolean m_sharedPinListNameGenerated;
	private String sharedPinListMateName;
	private String sharedPinListMateRevision;
	private boolean m_sharedPinListMateNameGenerated;
	private SymbolModificationCollection<SharedPinListSymbolInstance> symbolInstancesForDeletion;
	private SymbolModificationCollection<ISymbolDef> symbolDefsForAddition;
	private ReverseMap<IAbstractPin, IPinProxy> connectivityToSharedMap = new ConnectivityToSharedMap();
	@NotNull private final ProxyList proxies;
	private ProxyList reusableProxies;
	private Collection<String> hiddenCavities;
	private final ChangeEvent mapChangeEvent;
	private final ChangeEvent removalEvent;
	private final ChangeEvent additionEvent;
	private List<ChangeListener> schemChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> sharedChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> nameChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> pinChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> mapChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> removalChangeListeners = new ArrayList<ChangeListener>();
	private List<ChangeListener> reuseChangeListeners = new ArrayList<ChangeListener>();
	private List<IPropertyValidityListener> propertyValidityChangeListeners =
			new ArrayList<IPropertyValidityListener>();

	private int maxPinsAllowed;

	private Map<ISymbolDef, ReverseMap<IAbstractPin, IPinProxy>> symbolDefsToPinProxyMap =
			new HashMap<ISymbolDef, ReverseMap<IAbstractPin, IPinProxy>>();
	private Map<ISharedPin, ISharedPin> connectedPinToMakeReusable = new HashMap<ISharedPin, ISharedPin>();
	private Map<IConnector, String> modularConnectorToSharedNameMap = new HashMap<IConnector, String>();
	private Map<IConnector, Boolean> modularConnectorToSharedNameGeneratedMap = new HashMap<IConnector, Boolean>();
	private boolean m_ModularConnectorTreeValid = true;

	@Nullable private SharedPinUsageInfo sharedPinUsageInfo;

	public EditSharedPinListModel(@Nullable IPinList iSchemPinlist,
			@Nullable chs.cof.logical.cable.IPinList iCablePinlist,
			@Nullable ISharedPinList iSharedPinList)
	{
		sharedPinList = iSharedPinList;
		boolean isSplice;
		if (sharedPinList == null) {
			schemPinlist = iSchemPinlist;
			cablePinlist = iCablePinlist;
			assert schemPinlist == null || cablePinlist == schemPinlist.getConnectivity();
			if (cablePinlist instanceof IDevice || cablePinlist instanceof ISplice ||
					cablePinlist instanceof IFunction) {
				symbolDef = SymbolUtils.getSymbolDef(cablePinlist);
				symbolDefsToPinProxyMap.put(getSymbolDef(), connectivityToSharedMap);
			}
			isShare = true;
			isSplice = schemPinlist != null && schemPinlist.getConnectivity() instanceof ISplice;
		}
		else {
			isShare = false;
			isSplice = PinListTypeEnum.TypeSplice.equals(sharedPinList.getType());
		}

		isSymbolDefEditable = (!isSplice && symbolDef == null &&
				!(iSharedPinList instanceof ISharedInterconnectDevice) &&
				!(iSharedPinList instanceof ISharedConnector &&
						(iSharedPinList).getType() == PinListTypeEnum.TypeInterconnectConnector));
		proxies = new ProxyList();
		reusableProxies = new ProxyList();
		hiddenCavities = new HashSet<>();
		sharedPinUsageInfo = null;
		symbolInstancesForDeletion = new SymbolModificationCollection<SharedPinListSymbolInstance>()
		{
			protected void fireEvent()
			{
				fireRemovalEvent(removalEvent);
			}
		};
		symbolDefsForAddition = new SymbolModificationCollection<ISymbolDef>()
		{
			protected void fireEvent()
			{
				fireRemovalEvent(additionEvent);
			}
		};
		ChangeEvent pinChangeEvent = new ChangeEvent(proxies);
		ChangeEvent reuseChangeEvent = new ChangeEvent(reusableProxies);
		mapChangeEvent = new ChangeEvent(connectivityToSharedMap);
		removalEvent = new ChangeEvent(symbolInstancesForDeletion);
		additionEvent = new ChangeEvent(symbolDefsForAddition);
		proxies.setChangeEvent(pinChangeEvent);
		reusableProxies.setChangeEvent(reuseChangeEvent);
		determineMaxPins();
		if (sharedPinList != null) {
			createProxies();

			// load the symbols PinMapping data
			assert sharedPinList != null;
			for (ISymbolDefIterator symIt = sharedPinList.getSymbols(); symIt.hasNext(); ) {
				ISymbolDef def = symIt.getNext();

				assert sharedPinList != null;
				if (sharedPinList.getSymbolInstancePinMapping(def, 0) != null) {
					ReverseMap<IAbstractPin, IPinProxy> defMap = new ConnectivityToSharedMap();
					assert sharedPinList != null;
					Map<ISharedPin, IPin> map = sharedPinList.getSymbolInstancePinMapping(def, 0);
					for (IPinProxy proxy : getProxies()) {
						if (map.containsKey(proxy.getSharedPin())) {
							defMap.put((map.get(proxy.getSharedPin())).getConnectivity(), proxy);
						}
					}
					symbolDefsToPinProxyMap.put(def, defMap);
				}
			}
		}
	}

	public Map<ISymbolDef, ReverseMap<IAbstractPin, IPinProxy>> getSymbolDefsToPinProxyMap()
	{
		return symbolDefsToPinProxyMap;
	}

	/**
	 * Access to schematic pinlist used in the share operation.
	 * <p/>
	 * Could be null when editing SPL or when connectivity pinlist is getting shared (e.g. multiple or unplaced)
	 *
	 * @return The possibly null schematic pinlist
	 */
	@Nullable public IPinList getSchemPinlist()
	{
		return schemPinlist;
	}

	/**
	 * Access to connectivity pinlist used in the share operation.
	 * <p/>
	 * Could be null when editing SPL
	 *
	 * @return The possibly null connectivity pinlist
	 */
	@Nullable public chs.cof.logical.cable.IPinList getCablePinlist()
	{
		return cablePinlist;
	}

	public ISharedPinList getSharedPinList()
	{
		return sharedPinList;
	}

	public void setSharedPinList(ISharedPinList spl)
	{
		if (!isShare) {
			throw new IllegalStateException("sharedPinList field is read-only");
		}
		if (sharedPinList != spl) {
			sharedPinList = spl;
			determineMaxPins();
			connectivityToSharedMap.clear();
			proxies.clear();
			hiddenCavities.clear();
			sharedPinUsageInfo = null;
			if (sharedPinList != null) {
				createProxies();
			}
			reusableProxies.clear();
			fireSharedChangeEvent(new ChangeEvent(this));
		}
	}

	private void createProxies()
	{
		if (sharedPinList != null) {
			sharedPinUsageInfo = new SharedPinUsageInfo(sharedPinList);
			// Add the proxies to be added to respective lists and then add/remove to list model in one go
			// This avoids performance overhead of ProxyList.fireChangeEvent being called for every add/remove
			List<IPinProxy> proxiesToBeAdded = new ArrayList<IPinProxy>();
			List<IPinProxy> proxiesReuse = new ArrayList<IPinProxy>();
			for (ISharedPinIterator spit = sharedPinList.getPins(); spit.hasNext(); ) {
				ISharedPin spin = spit.getNext();
				boolean isBlockedCavity = isBlockedCavity(spin);
				if (isBlockedCavity) {
					hiddenCavities.add(spin.getName());
				}
				else {
					PinProxy ppp = new PinProxy(spin);
					proxiesToBeAdded.add(ppp);
					if (spin.isReusable()) {
						proxiesReuse.add(ppp);
					}
				}
			}
			if (!proxiesToBeAdded.isEmpty()) {
				proxies.addAll(proxiesToBeAdded);
				proxiesToBeAdded.clear();
			}
			if (!proxiesReuse.isEmpty()) {
				reusableProxies.addAll(proxiesReuse);
				proxiesReuse.clear();
			}
		}
	}

	private boolean isBlockedCavity(ISharedPin spin)
	{
		boolean isBlockedCavity = false;
		ISharedConnectorPin sharedConnectorPin = CommonUtils.cast(spin, ISharedConnectorPin.class);
		if (sharedConnectorPin != null) {
			isBlockedCavity = sharedConnectorPin.isBlockedCavity();
		}
		return isBlockedCavity;
	}

	@Nullable public String getSharedPinListMateName()
	{
		if (sharedPinList instanceof ISharedConnector) {
			final ISharedConnector mate = ((ISharedConnector) sharedPinList).getMate();
			return mate != null ? mate.getName() : null;
		}
		else {
			return sharedPinListMateName;
		}
	}

	public void setSharedPinListMateName(String splMateName)
	{
		if (!isShare) {
			throw new IllegalStateException("sharedPinListMateName field is read-only");
		}
		sharedPinListMateName = splMateName;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	@Nullable public String getSharedPinListMateRevision()
	{
		if (getSharedPinList() instanceof ISharedConnector) {
			final ISharedConnector mate = ((ISharedConnector) getSharedPinList()).getMate();
			return mate != null ? mate.getName() : null;
		}
		else {
			return sharedPinListMateRevision;
		}
	}

	public boolean isSharedPinUsed(@NotNull ISharedPin spin)
	{
		return sharedPinUsageInfo != null ?
				sharedPinUsageInfo.isUsed(spin) : spin.isUsed(IObjectInUseService.OBJECT_IN_USE);
	}

	public void setSharedPinListMateRevision(String splRevision)
	{
		if (!isShare) {
			throw new IllegalStateException("sharedPinListMateRevision field is read-only");
		}
		sharedPinListMateRevision = splRevision;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public String getSharedPinListName()
	{
		return getSharedPinList() != null ? getSharedPinList().getName() : sharedPinListName;
	}

	public void setSharedPinListName(String splName)
	{
		if (!isShare) {
			throw new IllegalStateException("sharedPinListName field is read-only");
		}
		sharedPinListName = splName;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public String getSharedPinListRevision()
	{
		return getSharedPinList() != null ? getSharedPinList().getRevision() : sharedPinListRevision;
	}

	public void setSharedPinListRevision(String splRevision)
	{
		if (!isShare) {
			throw new IllegalStateException("sharedPinListRevision field is read-only");
		}
		sharedPinListRevision = splRevision;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public ISymbolDef getSymbolDef()
	{
		return symbolDef;
	}

	public void setSymbolDef(ISymbolDef symdef)
	{
		if (!isSymbolDefEditable) {
			throw new IllegalStateException("symbolDef field is read-only");
		}
		if (symbolDef != symdef) {
			symbolDef = symdef;
			if (symbolDef != null) {
				schemPinlist = symbolDef.getPinList();
				// dts0100541410  NPE after editing shared device and add symbol for it
				// Get the connectivity to be able to do the pin mapping
				if (schemPinlist != null) {
					cablePinlist = schemPinlist.getConnectivity();
				}
				if (symbolDefsToPinProxyMap.containsKey(symbolDef)) {
					connectivityToSharedMap = symbolDefsToPinProxyMap.get(symbolDef);
				}
				else {
					connectivityToSharedMap = new ConnectivityToSharedMap();
					symbolDefsToPinProxyMap.put(symbolDef, connectivityToSharedMap);
				}
			}
			else {
				schemPinlist = null;
			}
			determineMaxPins();
			fireSchemChangeEvent(new ChangeEvent(this));
		}
	}

	public Collection<SharedPinListSymbolInstance> getSymbolInstancesForDeletion()
	{
		return symbolInstancesForDeletion;
	}

	public Collection<ISymbolDef> getSymbolDefsForAddition()
	{
		return symbolDefsForAddition;
	}

	public SortedListModel<IPinProxy> getReusableProxies()
	{
		return reusableProxies;
	}

	@NotNull public SortedListModel<IPinProxy> getProxies()
	{
		return proxies;
	}

	public Collection<String> getHiddenCavities()
	{
		return hiddenCavities;
	}

	public ReverseMap<IAbstractPin, IPinProxy> getConnectivityToSharedMap()
	{
		if (getSymbolDef() != null) {
			return symbolDefsToPinProxyMap.get(getSymbolDef());
		}
		else {
			return connectivityToSharedMap;
		}
	}

	public Map<IPinProxy, IAbstractPin> getSharedToConnectivityMap()
	{
		if (getSymbolDef() != null) {
			return symbolDefsToPinProxyMap.get(getSymbolDef()).getReverseMap();
		}
		else {
			return connectivityToSharedMap.getReverseMap();
		}
	}

	public boolean isShare()
	{
		return isShare;
	}

	public boolean isSymbolDefEditable()
	{
		return isSymbolDefEditable;
	}

	public boolean allowAddPins()
	{
		return proxies.size() < maxPinsAllowed();
	}

	public boolean allowUnreusePins(ISharedPinList spl)
	{
		for (ISharedPin spin : spl.getPins()) {
			if (allowRemove(spin)) {
				return true;
			}
		}
		return false;
	}

	public boolean allowRemove(@Nullable ISharedPin spin)
	{
		return spin != null && spin.isReusable() && !isSharedPinUsed(spin);
	}

	public int maxPinsAllowed()
	{
		return maxPinsAllowed;
	}

	private void determineMaxPins()
	{
		maxPinsAllowed = Integer.MAX_VALUE; // Initialize to unlimited.
		if (sharedPinList != null) { // Share into or Edit
			if (sharedPinList.isFrozen()) {
				// Can't add pins to a frozen pinlist, so current number of pins is the maximum
				maxPinsAllowed = sharedPinList.getPins().getSize();
			}
			else if (sharedPinList.isPartAssigned()) {
				ILibraryObject libObj = (ILibraryObject) sharedPinList.getLibraryObject();
				if (libObj != null) {
					//
					// Library Objject - if it is an interconnect connector, limit to 1.
					// If it is a regular device, use the facade.
					//
					if (sharedPinList.getType() == PinListTypeEnum.TypeInterconnectConnector) {
						maxPinsAllowed = 1;
					}
					else if (sharedPinList.getType() == PinListTypeEnum.TypeInterconnectDevice) {
						LibraryPinFacade lpf = new LibraryPinFacade(libObj, sharedPinList.getFootprint(),
								IInterconnectDevice.class, true);
						maxPinsAllowed = lpf.getAllPinNames().size();
					}
					else {
						maxPinsAllowed = libObj.getNumCavities();
						ISharedConnector sharedConnector = CommonUtils.cast(sharedPinList, ISharedConnector.class);
						if (sharedConnector != null) {
							maxPinsAllowed -= sharedConnector.getBlockedCavities().size();
						}
					}
				}
				else {
					maxPinsAllowed = sharedPinList.getPins().getSize();
				}
				// If this is an inline, check the other side to ensure we're not adding too many pins.
				// This COULD happen due to a bug in library where you can map mis-matched library parts to the same
				// inline.
				determineMaxSharedInlinePins();
			}
			else if (sharedPinList.getType().equals(PinListTypeEnum.TypeRingTerminal)) {
				//For ring terminals, there can not be more than 1 pin
				maxPinsAllowed = 1;
			}
			else if (sharedPinList.getType().isPlug() || sharedPinList.getType().isJack()) {
				// Only one half of an inline may have a library part, so handle if the other side is selected.
				// We can't let them add pins to one half if the other half has reached max
				determineMaxSharedInlinePins();
			}
			else if (sharedPinList.getType().equals(PinListTypeEnum.TypeJack)
					|| sharedPinList.getType().equals(PinListTypeEnum.TypePlug)
					|| sharedPinList.getType().equals(PinListTypeEnum.TypeInlineJack)
					|| sharedPinList.getType().equals(PinListTypeEnum.TypeInlinePlug)
					|| sharedPinList.getType().equals(PinListTypeEnum.TypeSplice)) {
				// A shared connector or splice with a symbol must have exactly the same number of pins as its symbol.
				// Therefore set the maximum number of pins to the number of pins on the symbol.
				if (symbolDef != null) {
					// Adding a symbol. We know that if we got this far the symbol has as many pins as or more pins than
					// the shared pinlist.
					maxPinsAllowed = symbolDef.getNumPins();
				}
				else if (sharedPinList.hasSymbols() && symbolInstancesForDeletion.isEmpty()) {
					// The shared pinlist already has a symbol, and the user is not removing it. Therefore the shared
					// pinlist must already have the same number of pins as the symbol, and it cannot have any more.
					maxPinsAllowed = sharedPinList.getPins().getSize();
				}
			}
		}
		else if (schemPinlist != null) {
			chs.cof.logical.cable.IPinList cpl = schemPinlist.getConnectivity();
			if (cpl.isPartAssigned()) {
				ILibraryObject libObj = (ILibraryObject) cpl.getLibraryObject();
				if (libObj != null) {
					//
					// Library Object - if it is an interconnect connector, limit to 1.
					// If it is a regular device, use the facade.
					//
					if (cpl instanceof IInterconnectConnector) {
						maxPinsAllowed = 1;
					}
					else if (cpl instanceof IInterconnectDevice) {
						LibraryPinFacade lpf = new LibraryPinFacade(libObj, ((IFootprintable) cpl).getFootprint(),
								IInterconnectDevice.class, true);
						maxPinsAllowed = lpf.getAllPinNames().size();
					}
					else {
						maxPinsAllowed = libObj.getNumCavities();
					}
				}
				else {
					maxPinsAllowed = cpl.getNumPins();
				}
			}
			else if (IConnector.Statics.isRingTerminalTypeConnector(cpl)) {
				maxPinsAllowed = 1;
			}
			// Only one half of an inline may have a library part, so handle if the other side is selected.
			// We can't let them add pins to one half if the other half has reached max
			determineMaxInlinePins(cpl);
		}
	}

	public boolean pinlistTypeIsDevice()
	{
		if (sharedPinList != null) {
			return sharedPinList instanceof ISharedDevice;
		}
		else if (schemPinlist != null) {
			return schemPinlist.getConnectivity() instanceof IDevice;
		}
		return false;
	}

	public boolean pinlistTypeIsFunction()
	{
		if (sharedPinList != null) {
			return sharedPinList.isFunctionType();
		}
		else if (schemPinlist != null) {
			return schemPinlist.getConnectivity() instanceof IFunction;
		}
		return false;
	}

	public boolean canCurrentPinListHaveMultipleSymbols()
	{
		return pinlistTypeIsFunction() || pinlistTypeIsDevice();
	}

	private void determineMaxSharedInlinePins()
	{
		if (sharedPinList instanceof ISharedConnector) {
			ISharedConnector connector = (ISharedConnector) sharedPinList;
			if (connector.getType() != PinListTypeEnum.TypeInlineJack &&
					connector.getType() != PinListTypeEnum.TypeInlinePlug) {
				return;
			}
			for (Object obj : connector.getMates()) {
				ISharedConnector mate = (ISharedConnector) obj;
				if (mate.isPartAssigned()) {
					ILibraryObject libObj = (ILibraryObject) mate.getLibraryObject();
					if (libObj != null) {
						maxPinsAllowed = Math.min(maxPinsAllowed, libObj.getNumCavities());
					}
					else {
						maxPinsAllowed = Math.min(maxPinsAllowed, mate.getPins().getSize());
					}
				}
			}
		}
	}

	private void determineMaxInlinePins(chs.cof.logical.cable.IPinList pinlist)
	{
		if (pinlist instanceof IConnector) {
			IConnector connector = (IConnector) pinlist;
			if (!connector.isInline()) {
				return;
			}
			for (IConnector mate : connector.getMates()) {
				if (mate.isPartAssigned()) {
					ILibraryObject libObj = (ILibraryObject) mate.getLibraryObject();
					if (libObj != null) {
						maxPinsAllowed = Math.min(maxPinsAllowed, libObj.getNumCavities());
					}
				}
			}
		}
	}

	private boolean areSymbolsAvailable()
	{
		ISymbolLibraryMgr libraryMgr = FactoryMgr.getSystemFactory().getCHSSystem().getSymbolLibraryMgr();
		for (IAbstractLibrary lib : libraryMgr.getSymbolLibraries(libraryMgr.getAccessConfiguration())) {
			for (IStampIterator sitr = lib.getSymbols(); sitr.hasNext(); ) {
				IStamp symbol = sitr.getNext();
				if (acceptSymbol(symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isSharedPinListEditable()
	{
		return isSymbolDefEditable                           // Can symbol can be added or replaced?
				|| isShare                                 // If it's a share, there's mapping to do
				|| allowPinReuseManagement(); // Can we make any reusable pin unreusable?
	}

	public boolean allowPinReuseManagement()
	{
		if (sharedPinList == null) {
			return allowAddPins();
		}
		return allowAddPins()                            // Can pins be added?
				|| sharedPinList.getUnrestrictedPins().getSize() // Can we make any pins reusable?
				< sharedPinList.getPins().getSize()
				|| allowUnreusePins(sharedPinList); // Can we make any pins non-reusable?
	}

	public static boolean isSharedPinListEditable(ISharedPinList spl)
	{
		EditSharedPinListModel esplm = new EditSharedPinListModel(null, null, spl);
		return esplm.isSharedPinListEditable();
	}

	public boolean acceptSymbol(IStamp stamp)
	{
		if (!(stamp instanceof ISymbolDef)) {
			return false;
		}

		ISymbolDef symdef = (ISymbolDef) stamp;
		if (symdef.getSymbolType() != SymbolTypeEnum.DEVICE) {
			return false;
		}

		int numSymPins = symdef.getNumPins();
		int numSharedPins = 0;
		boolean isLibraryPart = false;
		boolean isConnector = false;

		if (getSharedPinList() != null) {
			ISharedPinList spl = getSharedPinList();
			numSharedPins = spl.getPins().getSize();
			isLibraryPart = spl.isPartAssigned();
			isConnector = !spl.getType().equals(PinListTypeEnum.TypeDevice);
		}
		else {
			if (cablePinlist != null) {
				numSharedPins = cablePinlist.getNumPins();
				assert cablePinlist != null;
				isLibraryPart = cablePinlist.isPartAssigned();
				isConnector = cablePinlist instanceof IConnector;
			}
			else {
				assert false;
			}
		}

		// Adding a symbol with more pins than the shared pinlist would make it necessary to add more
		// pins to the shared pinlist. But we can't add any more pins if the shared pinlist has a library part.
		if (isLibraryPart && numSymPins > numSharedPins) {
			return false;
		}

		// If shared connector, symbol is plugmap, and as such must account for all pins on shared connector.
		// Therefore it must have at least as many pins as the shared connector
		return !(isConnector && numSymPins < numSharedPins);
	}

	public void addChangeListener(ChangeListener listener)
	{
		addSchemChangeListener(listener);
		addSharedChangeListener(listener);
		addNameChangeListener(listener);
		addPinChangeListener(listener);
		addMapChangeListener(listener);
		addReuseChangeListener(listener);
		addRemovalListener(listener);
	}

	public void addSchemChangeListener(ChangeListener listener)
	{
		schemChangeListeners.add(listener);
	}

	private void fireSchemChangeEvent(ChangeEvent e)
	{
		for (ChangeListener schemChangeListener : schemChangeListeners) {
			schemChangeListener.stateChanged(e);
		}
	}

	public void addSharedChangeListener(ChangeListener listener)
	{
		sharedChangeListeners.add(listener);
	}

	private void fireSharedChangeEvent(ChangeEvent e)
	{
		for (ChangeListener sharedChangeListener : sharedChangeListeners) {
			sharedChangeListener.stateChanged(e);
		}
	}

	public void addNameChangeListener(ChangeListener listener)
	{
		nameChangeListeners.add(listener);
	}

	private void fireNameChangeEvent(ChangeEvent e)
	{
		for (ChangeListener nameChangeListener : nameChangeListeners) {
			nameChangeListener.stateChanged(e);
		}
	}

	public void addPinChangeListener(ChangeListener listener)
	{
		pinChangeListeners.add(listener);
	}

	private void firePinChangeEvent(ChangeEvent e)
	{
		for (ChangeListener pinChangeListener : pinChangeListeners) {
			pinChangeListener.stateChanged(e);
		}
	}

	public void addMapChangeListener(ChangeListener listener)
	{
		mapChangeListeners.add(listener);
	}

	private void fireMapChangeEvent(ChangeEvent e)
	{
		for (ChangeListener mapChangeListener : mapChangeListeners) {
			mapChangeListener.stateChanged(e);
		}
	}

	public void addReuseChangeListener(ChangeListener listener)
	{
		reuseChangeListeners.add(listener);
	}

	private void fireReuseChangeEvent(ChangeEvent e)
	{
		for (ChangeListener reuseChangeListener : reuseChangeListeners) {
			reuseChangeListener.stateChanged(e);
		}
	}

	public void addRemovalListener(ChangeListener listener)
	{
		removalChangeListeners.add(listener);
	}

	private void fireRemovalEvent(ChangeEvent e)
	{
		for (ChangeListener removalChangeListener : removalChangeListeners) {
			removalChangeListener.stateChanged(e);
		}
	}

	public void addPropertyValidityChangeListener(IPropertyValidityListener listener)
	{
		propertyValidityChangeListeners.add(listener);
	}

	private void fireValidityChangeEvent(ValidityChangeEvent e)
	{
		for (IPropertyValidityListener propertyValidityChangeListener : propertyValidityChangeListeners) {
			propertyValidityChangeListener.validityChanged(e);
		}
	}

	// ChangeListener implementation
	public void stateChanged(ChangeEvent e)
	{

	}

	public void mapSymbolDefToPinMaps()
	{
		symbolDefsToPinProxyMap.put(getSymbolDef(), getConnectivityToSharedMap());
	}

	public ReverseMap<IAbstractPin, IPinProxy> getConnectivityToSharedMap(ISymbolDef def)
	{
		return symbolDefsToPinProxyMap.get(def);
	}

	public void addConnectedPinToMakeReusable(ISharedPin spin, ISharedPin pin)
	{
		connectedPinToMakeReusable.put(spin, pin);
	}

	public ISharedPin getConnectedPinToMakeReuable(ISharedPin spin)
	{
		return connectedPinToMakeReusable.get(spin);
	}

	public Collection<ISharedPin> getConnectedPinsToMakeReusableValues()
	{
		return connectedPinToMakeReusable.values();
	}

	public Map<ISharedPin, ISharedPin> getConnectedPinsToMakeReusable()
	{
		return connectedPinToMakeReusable;
	}

	public Map<IConnector, String> getModularConnectorToSharedNamesMap()
	{
		return modularConnectorToSharedNameMap;
	}

	public void setModularConnectorToSharedNamesMap(Map<IConnector, String> connVsSharedNamesMap)
	{
		modularConnectorToSharedNameMap = new HashMap<IConnector, String>(connVsSharedNamesMap);
	}

	public void putModularConnectorToSharedNamesMap(IConnector connector, String sharedName)
	{
		modularConnectorToSharedNameMap.put(connector, sharedName);
		if (connector == getCablePinlist()) {
			setSharedPinListName(sharedName);
		}
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public Map<IConnector, Boolean> getModularConnectorToSharedNameGeneratedMap()
	{
		return modularConnectorToSharedNameGeneratedMap;
	}

	public void setModularConnectorToSharedNameGeneratedMap(Map<IConnector, Boolean> connVsNameGeneratedMap)
	{
		modularConnectorToSharedNameGeneratedMap = new HashMap<IConnector, Boolean>(connVsNameGeneratedMap);
	}

	public void putModularConnectorToSharedNameGeneratedMap(IConnector connector, Boolean nameGenerated)
	{
		modularConnectorToSharedNameGeneratedMap.put(connector, nameGenerated);
		if (connector == getCablePinlist()) {
			setSharedPinListNameGenerated(nameGenerated);
		}
	}

	public void setModularConnectorTreeValidity(boolean modularConnectorTreeValid)
	{
		m_ModularConnectorTreeValid = modularConnectorTreeValid;
		fireNameChangeEvent(new ChangeEvent(this));
	}

	public boolean getModularConnectorTreeValidity()
	{
		return m_ModularConnectorTreeValid;
	}

	private class ConnectivityToSharedMap extends ReverseMap<IAbstractPin, IPinProxy>
	{

		public void clear()
		{
			super.clear();
			fireMapChangeEvent(mapChangeEvent);
		}

		public IPinProxy put(IAbstractPin key, IPinProxy value)
		{
			IPinProxy oldObject = super.put(key, value);
			fireMapChangeEvent(mapChangeEvent);
			return oldObject;
		}

		public void putAll(Map<? extends IAbstractPin, ? extends IPinProxy> t)
		{
			super.putAll(t);
			fireMapChangeEvent(mapChangeEvent);
		}

		public IPinProxy remove(Object key)
		{
			// Can't change this as it's a java call
			//noinspection SuspiciousMethodCalls
			IPinProxy removedObject = super.remove(key);
			fireMapChangeEvent(mapChangeEvent);
			return removedObject;
		}
	}

	private abstract class SymbolModificationCollection<E> extends ArrayList<E>
	{

		protected SymbolModificationCollection()
		{
		}

		public boolean remove(Object o)
		{
			// Can't change this as it's a java call
			//noinspection SuspiciousMethodCalls
			boolean changed = super.remove(o);
			if (changed) {
				fireEvent();
			}
			return changed;
		}

		public boolean removeAll(Collection<?> c)
		{
			boolean changed = super.removeAll(c);
			if (changed) {
				fireEvent();
			}
			return changed;
		}

		public boolean retainAll(Collection<?> c)
		{
			boolean changed = super.retainAll(c);
			if (changed) {
				fireEvent();
			}
			return changed;
		}

		public void clear()
		{
			super.clear();
			fireEvent();
		}

		public void add(int index, E element)
		{
			super.add(index, element);
			fireEvent();
		}

		public boolean add(E e)
		{
			boolean changed = super.add(e);
			if (changed) {
				fireEvent();
			}
			return changed;
		}

		public boolean addAll(Collection<? extends E> c)
		{
			boolean changed = super.addAll(c);
			if (changed) {
				fireEvent();
			}
			return changed;
		}

		public boolean addAll(int index, Collection<? extends E> c)
		{
			return super.addAll(index, c);
		}

		public E remove(int index)
		{
			return super.remove(index);
		}

		protected void removeRange(int fromIndex, int toIndex)
		{
			super.removeRange(fromIndex, toIndex);
			fireEvent();
		}

		public E set(int index, E element)
		{
			return super.set(index, element);
		}

		public void trimToSize()
		{
			super.trimToSize();
			fireEvent();
		}

		protected abstract void fireEvent();
	}

	public class ProxyList extends SortedListModel<IPinProxy>
	{

		private ChangeEvent changeEvent;

		private ProxyList()
		{
			// No idea how to resolve this one as the static new inside NameObjectComparator cannot be typed
			super(new NamedObjectComparator<IPinProxy>(true, true, false)
			{
				@Override public int compare(IPinProxy o1, IPinProxy o2)
				{
					if (Environment.shouldSortSharePinsBasedOnDeviceConnectors()) {
						int devConnResult = PinUtils.comparePinGrouping(o1, o2,
								o -> PinUtils.determineDeviceConnector(o.getSharedPin()),
								IReadOnlyNamedObject::getName, m_alpha);
						if (devConnResult != 0) {
							return devConnResult;
						}
					}
					return super.compare(o1, o2);
				}
			});
		}

		public void setChangeEvent(ChangeEvent event)
		{
			changeEvent = event;
		}

		public void add(int index, IPinProxy element)
		{
			super.add(index, element);
			fireChangeEvent();
		}

		public boolean addAll(int index, Collection<? extends IPinProxy> c)
		{
			boolean changed = super.addAll(index, c);
			fireChangeEvent();
			return changed;
		}

		public IPinProxy remove(int index)
		{
			IPinProxy old = super.remove(index);
			fireChangeEvent();
			return old;
		}

		public IPinProxy set(int index, IPinProxy element)
		{
			IPinProxy old = super.set(index, element);
			fireChangeEvent();
			return old;
		}

		public boolean add(IPinProxy o)
		{
			boolean changed = super.add(o);
			fireChangeEvent();
			return changed;
		}

		protected boolean addWithoutNotify(IPinProxy o)
		{
			return super.add(o);
		}

		public boolean addAll(Collection<? extends IPinProxy> c)
		{
			for (IPinProxy obj : c) {
				addWithoutNotify(obj);
			}
			fireChangeEvent();
			return true;
		}

		public void clear()
		{
			super.clear();
			fireChangeEvent();
		}

		public boolean remove(Object o)
		{
			// Cannot fix this as Java takes the object
			//noinspection SuspiciousMethodCalls
			boolean changed = super.remove(o);
			fireChangeEvent();
			return changed;
		}

		protected boolean removeWithoutNotify(Object o)
		{
			return super.remove(o);
		}

		public boolean removeAll(Collection<?> c)
		{
			boolean changed = false;
			for (Object obj : c) {
				if (removeWithoutNotify(obj)) {
					changed = true;
				}
			}
			fireChangeEvent();
			return changed;
		}

		public boolean retainAll(Collection<?> c)
		{
			boolean changed = super.retainAll(c);
			fireChangeEvent();
			return changed;
		}

		public void fireChangeEvent()
		{
			if (changeEvent.getSource() == EditSharedPinListModel.this.getProxies()) {
				firePinChangeEvent(changeEvent);
			}
			else {
				fireReuseChangeEvent(changeEvent);
			}
		}
	}

	public boolean isSharedPinListNameGenerated()
	{
		return m_sharedPinListNameGenerated;
	}

	public void setSharedPinListNameGenerated(boolean generated)
	{
		m_sharedPinListNameGenerated = generated;
	}

	public boolean isSharedPinListMateNameGenerated()
	{
		return m_sharedPinListMateNameGenerated;
	}

	public void setSharedPinListMateNameGenerated(boolean generated)
	{
		m_sharedPinListMateNameGenerated = generated;
	}

	/**
	 * @return the number of connectivity pins from either the schem representations or the symbol representations. This
	 * count won't include backshell terminations on connectors, only the pins
	 */
	public int getPinCount()
	{
		// first check the symboldef just in case it is a composite. Only the symbol def will know about the
		// entire pin count
		ISymbolDef sDef = getSymbolDef();
		if (sDef != null) {
			return sDef.getNumPins();
		}
		// just get the schem pinlist, then ask its connectivity for the number of pins on it. This is valid
		// for all pin lists and won't return pins in the case of backshells.
		IPinList schPL = getSchemPinlist();
		if (schPL == null) {
			return 0;
		}
		chs.cof.logical.cable.IPinList connPL = schPL.getConnectivity();
		if (connPL == null) {
			return 0;
		}
		return connPL.getNumPins();
	}

	public void validityChanged(ValidityChangeEvent evt)
	{
		fireValidityChangeEvent(evt);
	}

	public void invalidReasonChanged(IProperty property)
	{
		// TODO Auto-generated method stub

	}
}
