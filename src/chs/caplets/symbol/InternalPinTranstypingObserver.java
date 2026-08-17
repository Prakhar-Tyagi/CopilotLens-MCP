package chs.caplets.symbol;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.symbol.ISymbolDef;
import chs.utility.logic.ISymbolModel;
import chs.utility.symbol.AbstractSymbolPinTranstypingObserver;

/**
 * Created by IntelliJ IDEA. User: momostafa Date: Aug 6, 2009 Time: 4:37:22 AM To change this template use File |
 * Settings | File Templates.
 */
public class InternalPinTranstypingObserver extends AbstractSymbolPinTranstypingObserver
{

	public InternalPinTranstypingObserver(ISymbolModel model)
	{
		super(model);
	}

	/**
	 * will replicate the connectivity and schematic of an internal pin to a device pin
	 *
	 * @param schemPin schem internal pin to replicate
	 * @param cableOwner parent device of the internal pin
	 *
	 * @return replicated schematic device pin associated with a cable device pin
	 */
	protected IPin replicatePin(IGenericSchemPin schemPin, IPinList cableOwner)
	{
		IPin deviceSchemPin = m_replicator.replicateInternalPinAsDevicePin((IInternalSchemPin) schemPin, cableOwner);
		return deviceSchemPin;
	}

	protected void addPinToSymbolDef(ISymbolDef symDef, IGenericPin cablePin)
	{
		symDef.addPin((IAbstractPin) cablePin);
	}
}
