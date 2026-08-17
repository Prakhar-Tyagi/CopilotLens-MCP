package chs.caplets.logic.updateSymbol;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolRef;
import chs.common.IReadOnlyNamedObject;
import chs.common.IReference;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utility.CavityProxy;
import chs.utility.SymbolUtils;
import chs.utility.helpers.InternalLinkHelper;
import chs.utility.logic.AddBlockCableInfoParams;
import chs.utility.logic.IAddBlockInfoParams;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceSymbolInformationUpdator extends SymbolInfomationUpdator<IDevice, IAbstractPin>
{

	private IDevice device;
	private IPinList schemPinList;

	public DeviceSymbolInformationUpdator(IPinList pinList, @Nullable ISymbolDef symbolDef)
	{
		super((IDevice) pinList.getConnectivity(), symbolDef);
		schemPinList = pinList;
		device = (IDevice) pinList.getConnectivity();
	}

	public void removeInvalidInternalLinks(ISymbolDef symbolDef)
	{
		Set<IUID> symbolPinUIDs = new HashSet<>();
		for (IGenericSchemPin schemPin : getSymbolPins(symbolDef)) {
			symbolPinUIDs.add(schemPin.getConnectivity().getUID());
		}
		for (IInternalPin internalPin : ((IDevice) symbolDef.getConnectivity()).getInternalPins()) {
			symbolPinUIDs.add(internalPin.getUID());
		}

		Set<IInternalPin> internalPinstoBeDeleted = getInternalPinsToBeDeleted(symbolPinUIDs);

		for (IInternalSchemPin schemInternalLink : schemPinList.getInternalPins()) {
			if (internalPinstoBeDeleted.contains(schemInternalLink.getConnectivity())) {
				schemInternalLink.delete();
			}
		}

		for (IInternalPin link : internalPinstoBeDeleted) {
			if (UIDMgr.getNonDeletedObject(link.getUID()) != null) {
				link.delete();
			}
		}
	}

	@NotNull private Set<IInternalPin> getInternalPinsToBeDeleted(Set<IUID> symbolPinUIDs)
	{
		Set<IInternalPin> internalPinsToRemove = new HashSet<>();
		for (IInternalPin pin : device.getInternalPins()) {
			if (!symbolPinUIDs.contains(pin.getReference())) {
				internalPinsToRemove.add(pin);
			}
		}

		ILogicDesign design = device.getLogicDesign();

		Set<IInternalPin> internalPinstoBeDeleted = new HashSet<>();
		if (design != null) {
			for (IInternalPin pin : internalPinsToRemove) {
				if (new InternalLinkHelper().isLastUsage(design, pin, t -> schemPinList == t)) {
					internalPinstoBeDeleted.add(pin);
				}
			}
		}
		return internalPinstoBeDeleted;
	}

	public void createAndUpdatePinsAndInternalPins(ISymbolDef symbolDef)
	{
		Map<IUID, String> symbolToLibraryCaivityMap = getSymbolToLibraryMapping();

		Set<IUID> symPinsConnectedtoLinks = getSymbolPinsConnectedToLinks(symbolDef);

		for (IGenericSchemPin schemPin : getSymbolPins(symbolDef)) {
			IGenericPin symbolPin = schemPin.getConnectivity();
			IGenericPin pin = getMatchingPin(schemPin.getConnectivity());
			if (pin != null) {
				setReference(symbolPin, pin);
			}

			if (pin == null && symPinsConnectedtoLinks.contains(symbolPin.getUID())) {
				createorUpdateInternalPin(symbolToLibraryCaivityMap, symbolPin);
			}
		}

		createInternalPins(symbolDef);
		removeInvalidInternalLinks(symbolDef);
	}

	private void createInternalPins(ISymbolDef symbolDef)
	{
		SymbolUtils.addInternalPinsToDevice(device,
				CollectionUtils.createList(((IDevice) symbolDef.getConnectivity()).getInternalPins()));
	}

	@NotNull private Set<IUID> getSymbolPinsConnectedToLinks(ISymbolDef symbolDef)
	{
		Set<IUID> symPinsConnectedtoLinks = new HashSet<>();
		for (IInternalLink link : ((IDevice) symbolDef.getConnectivity()).getInternalLinkCollection()) {
			IGenericPin startPin = link.getStartPin();
			if (startPin != null) {
				symPinsConnectedtoLinks.add(startPin.getUID());
			}
			IGenericPin endtPin = link.getEndPin();
			if (endtPin != null) {
				symPinsConnectedtoLinks.add(endtPin.getUID());
			}
		}
		return symPinsConnectedtoLinks;
	}

	private void createorUpdateInternalPin(Map<IUID, String> symbolToLibraryCaivityMap, IGenericPin symbolPin)
	{
		IInternalPin internalPin = getMatchingInternalPin(symbolPin);
		if (internalPin == null) {
			internalPin =
					FactoryMgr.getCablePropertiedFactory().createInternalPinForOwner(FactoryMgr.createUID(),
							device);
			device.addInternalPin(internalPin);
		}
		String cavityName = symbolToLibraryCaivityMap.get(symbolPin.getUID());
		if (cavityName != null) {
			internalPin.setName(cavityName);
		}
		internalPin.setReference(symbolPin.getUID());
	}

	private void setReference(IGenericPin symbolPin, IGenericPin pin)
	{
		IReference referencer = CommonUtils.cast(pin, IReference.class);
		if (referencer != null) {
			referencer.setReference(symbolPin.getUID());
		}
	}

	@NotNull private Map<IUID, String> getSymbolToLibraryMapping()
	{
		Map<IUID, String> symbolToLibraryCaivityMap = new HashMap<>();
		for (IReadOnlyNamedObject obj : symbolMap) {
			if (obj instanceof CavityProxy) {
				CavityProxy cavityProxy = (CavityProxy) obj;
				symbolToLibraryCaivityMap.put(cavityProxy.getPin().getConnectivity().getUID(), cavityProxy.getName());
			}
		}
		return symbolToLibraryCaivityMap;
	}

	public void updateBlockAssociation(IDevice pinOwner, ISymbolDef symbolDef)
	{
		ISymbolRef symbolRef = new SymbolRef(symbolDef.getUID(), symbolDef.getServerTimeModified());
		pinOwner.setSymbolRef(symbolRef);
		IAddBlockInfoParams blockInfoParams = new AddBlockCableInfoParams(schemPinList.getConnectivity());
		LogicUtils.addBlockInfo(symbolDef, blockInfoParams);
	}

	@Nullable public IGenericPin getMatchingPin(IGenericPin symbolPin)
	{
		Collection<IAbstractPin> devicePins = device.getPinCollection();
		return getMatching(symbolPin, devicePins);
	}

	@Nullable private IInternalPin getMatchingInternalPin(IGenericPin symbolPin)
	{
		Collection<IGenericPin> internalPins = new HashSet<>();
		for (IInternalPin pin : device.getInternalPins()) {
			internalPins.add(pin);
		}
		IGenericPin matching = getMatching(symbolPin, internalPins);
		return matching instanceof IInternalPin ? (IInternalPin) matching : null;
	}

	@Nullable private IGenericPin getMatching(IGenericPin symbolPin, Collection<? extends IGenericPin> devicePins)
	{
		for (IGenericPin pin : devicePins) {
			if (pin.getName().equals(symbolPin.getName())) {
				return pin;
			}
		}

		for (IReadOnlyNamedObject obj : symbolMap) {
			if (obj instanceof CavityProxy) {
				CavityProxy cavityProxy = (CavityProxy) obj;
				if (cavityProxy.getPin().getConnectivity().getName().equals(symbolPin.getName())) {
					for (IGenericPin pin : devicePins) {
						if (pin.getName().equals(obj.getName())) {
							return pin;
						}
					}
				}
			}
		}
		return null;
	}

	protected void addSymbolPinAssociation(IPin symbolPin, IGenericPin pin)
	{
		((IReference) pin).setReference(symbolPin.getUID());
	}

	protected ISymbolDefIterator getSymbols(IDevice spl)
	{
		return spl.getSymbols();
	}

	@Override void updateInternalLinks(ISymbolDef symbolDef)
	{
		SymbolUtils.updateInternalLinks(schemPinList, symbolDef);
	}

	protected void ensureSymbolAssociation(ISymbolDef symDef, boolean copyProperties)
	{
		updateBlockAssociation(pinlist, symDef);
		if (copyProperties) {
			copyProperties(symDef.getPinList(), pinlist);
		}
		addSymbolPinAssociations(symDef, copyProperties);
	}

	private Set<IPin> getAllSymbolPins(ISymbolDef sym)
	{
		Set<IPin> pinsConnectedToInternalLinks = new HashSet<>();
		for (IPin pin : SymbolUtils.collectAllSymbolPins(sym)) {
			IAbstractPin cablePin = pin.getConnectivity();
			if (cablePin != null) {
				pinsConnectedToInternalLinks.add(pin);
			}
		}
		return pinsConnectedToInternalLinks;
	}

	public void addSymbolPinAssociations(ISymbolDef sym, boolean copyProperties)
	{
		// we need to get the pin list from the symbol...
		IPinList symPinList = sym.getPinList();

		if (symPinList != null) {
			for (IPin symbolPin : getAllSymbolPins(sym)) {
				IGenericPin pin = getMatchingPin(symbolPin.getConnectivity());
				if (pin != null) {
					if (copyProperties) {
						copyProperties(symbolPin, pin);
					}
					addSymbolPinAssociation(symbolPin, pin);
				}
			}
		}
	}
}
