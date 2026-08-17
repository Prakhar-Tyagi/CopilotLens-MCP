package chs.caplets.symbol.actions;

import chs.caplets.symbol.Model;
import chs.cof.logical.cable.IPinList;
import chs.cof.symbol.IPSMStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.IPropertiedObject;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;

public class AddPinActionHelper implements IAddPinActionHelper
{

	protected Model m_model;
	private ISymbolDef symbolDef;

	public AddPinActionHelper(Model model)
	{
		m_model = model;
		IPSMStamp symbol = model.getSymbolDef();
		if (symbol instanceof ISymbolDef) {
			symbolDef = (ISymbolDef) symbol;
		}
	}

	@Override
	public boolean isValidToPerformAction()
	{
		boolean isValid = true;
		if (SymbolUtils.isBackshellSymbol(symbolDef)) {
			// Backshell may only have 1 pin.
			IPropertiedObject pobj = symbolDef.getPropertyHolder();
			if (pobj != null && (((IPinList) pobj).getNumPins() > 0)) {
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * Return our matching ActionUI class
	 */
	@Override
	public String getActionUIClass()
	{
		return AddPinActionUI.class.getName();
	}

	@Override
	public boolean isEnabled()
	{
		//
		// In the symbol editor, we can't add any pins to comments, and only one to a backshell
		//
		if (symbolDef != null) {
			if (SymbolUtils.isCommentSymbol(symbolDef)) {
				return false;
			}
			if (SymbolUtils.isBackshellSymbol(symbolDef)) {
				// Backshell may only have 1 pin.
				IPropertiedObject pobj = symbolDef.getPropertyHolder();
				return pobj != null && (((IPinList) pobj).getNumPins() == 0);
			}
			if (isFunctionSymbol()) {
				return false;
			}
		}
		return true;
	}

	protected boolean isFunctionSymbol()
	{
		return symbolDef.getSymbolType() == SymbolTypeEnum.FUNCTION;
	}

	@Override public String getStatusbarText()
	{
		return ResourceMgr.getString(AddPinActionUI.class, "AddPinActionHelper.placement.guidance");
	}
}
