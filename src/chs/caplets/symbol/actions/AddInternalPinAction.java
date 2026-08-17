package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.symbol.ISymbolDef;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: momostafa Date: Jul 29, 2009 Time: 9:41:54 AM To change this template use File |
 * Settings | File Templates.
 */
public class AddInternalPinAction extends AddPinAction
{

	public AddInternalPinAction(ICapletController controller)
	{
		super(controller);
	}

	protected IGenericPin createCablePin()
	{
		return ((ISymbolDef) m_model.getSymbolDef()).createInternalPin();
	}

	protected IGenericSchemPin createSchemPin(IUID uid, IGenericPin cablePin)
	{
		return FactoryMgr.getSchemFactory().constructInternalPin(uid, (IInternalPin) cablePin,
				m_currValidPoint.x, m_currValidPoint.y);
	}

	public String getActionUIClass()
	{
		return AddInternalPinActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		//
		// In the symbol editor, we can only add internal pins to devices
		//
		if (m_model.getSymbolDef() instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();

			if (!(SymbolUtils.isDeviceSymbol(symDef))/* || SymbolUtils.isGroundSymbol(symDef))*/) {
				return false;
			}
		}

		return isModeEnabled();
	}

	@Override protected boolean isAValidLocationToAddPin(@NotNull String location)
	{
		final Set<IGenericSchemPin> existingPinsAtThisLocation = m_pinLocations.pull(location);
		return existingPinsAtThisLocation == null || existingPinsAtThisLocation.isEmpty();
	}
}
