package chs.caplets.symbol;

import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.symbol.ISymbolDef;
import chs.utility.logic.ISymbolModel;
import chs.utility.symbol.AbstractSymbolPinTranstypingObserver;

/**
 * Created by IntelliJ IDEA. User: momostafa Date: Aug 6, 2009 Time: 4:37:35 AM
 */
public class DevicePinTranstypingObserver extends AbstractSymbolPinTranstypingObserver
{

	public DevicePinTranstypingObserver(ISymbolModel model)
	{
		super(model);
	}

	/**
	 * will replicate the connectivity and schematic of a device pin as an internal pin
	 *
	 * @param schemPin schem device pin to replicate
	 * @param cableOwner parent device of the internal pin
	 *
	 * @return replicated schematic internal pin associated with a internal pin
	 */
	protected IInternalSchemPin replicatePin(IGenericSchemPin schemPin, IPinList cableOwner)
	{
		IInternalSchemPin internalSchemPin = m_replicator.replicateDevicePinAsInternal((IPin) schemPin, cableOwner);
		return internalSchemPin;
	}

	protected void addPinToSymbolDef(ISymbolDef symDef, IGenericPin cablePin)
	{
		symDef.addInternalPin((IInternalPin) cablePin);
	}
}
