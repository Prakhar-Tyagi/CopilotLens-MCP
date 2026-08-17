package chs.caplets.logic.updateSymbol;

import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.common.IReadOnlyNamedObject;
import chs.utility.CavityProxy;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.Nullable;

public class SharedPinListSymbolInfomationUpdator extends SymbolInfomationUpdator<ISharedPinList, ISharedPin>
{

	private ISharedPinList sharedPinList;
	private DeviceSymbolInformationUpdator deviceSymbolInfoUpdator;
	private IPinList schemPinlist;
	private boolean canUpdateConnectivity = false;

	public SharedPinListSymbolInfomationUpdator(ISharedPinList sharedPinList, IPinList pinList,
			@Nullable ISymbolDef symbolDef, boolean canUpdateConnectivity)
	{
		super(sharedPinList, symbolDef);
		this.sharedPinList = sharedPinList;
		deviceSymbolInfoUpdator = new DeviceSymbolInformationUpdator(pinList, symbolDef);
		schemPinlist = pinList;
		this.canUpdateConnectivity = canUpdateConnectivity;
	}

	@Nullable public ISharedPin getMatchingPin(IGenericPin symbolPin)
	{
		if (sharedPinList != null) {
			ISharedPinIterator pinIT = sharedPinList.getPins();
			while (pinIT.hasNext()) {
				ISharedPin pin = pinIT.next();
				if (pin.getName().equals(symbolPin.getName())) {
					return pin;
				}
			}

			for (IReadOnlyNamedObject obj : symbolMap) {
				if (obj instanceof CavityProxy) {
					CavityProxy cavityProxy = (CavityProxy) obj;
					if (cavityProxy.getPin().getConnectivity().getName()
							.equals(symbolPin.getName())) {
						pinIT = sharedPinList.getPins();
						while (pinIT.hasNext()) {
							ISharedPin pin = pinIT.next();
							if (pin.getName().equals(obj.getName())) {
								return pin;
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Override void updateInternalLinks(ISymbolDef symbolDef)
	{
		if (shouldUpdateConnectivity()) {
			SymbolUtils.updateInternalLinks(schemPinlist, symbolDef);
		}
	}

	private boolean shouldUpdateConnectivity()
	{
		return canUpdateConnectivity;
	}

	protected ISymbolDefIterator getSymbols(ISharedPinList spl)
	{
		return spl.getSymbols();
	}

	@Override void updateBlockAssociation(ISharedPinList pinOwner, ISymbolDef symbolDef)
	{
		if (shouldUpdateConnectivity()) {
			deviceSymbolInfoUpdator.updateBlockAssociation((IDevice) schemPinlist.getConnectivity(), symbolDef);
		}
	}

	protected void ensureSymbolAssociation(ISymbolDef symDef, boolean copyProperties)
	{
		//noinspection ConstantConditions
		SharedPinListHelper.addLibraryPartAssociatedSymbolToSPL(sharedPinList, symDef,
				(spl, sympin) -> getMatchingPin(sympin.getConnectivity()), copyProperties);
		updateBlockAssociation(sharedPinList, symDef);
	}

	@Override public void initializeSymbolInfomation(ILibraryObject libraryObject, ISymbolDef symbolDef)
	{
		super.initializeSymbolInfomation(libraryObject, symbolDef);
		if (shouldUpdateConnectivity()) {
			deviceSymbolInfoUpdator.initializeSymbolInfomation(libraryObject, symbolDef);
		}
	}

	@Override void createAndUpdatePinsAndInternalPins(ISymbolDef symbolDef)
	{
		if (shouldUpdateConnectivity()) {
			deviceSymbolInfoUpdator.createAndUpdatePinsAndInternalPins(symbolDef);
		}
	}
}
