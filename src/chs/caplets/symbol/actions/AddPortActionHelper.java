package chs.caplets.symbol.actions;

import chs.caplets.symbol.Model;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.utilities.ResourceMgr;

public class AddPortActionHelper implements IAddPinActionHelper
{

	protected Model m_model;

	public AddPortActionHelper(Model model)
	{
		m_model = model;
	}

	@Override
	public boolean isValidToPerformAction()
	{
		return isFunctionSymbol();
	}

	/**
	 * Return our matching ActionUI class
	 */
	@Override
	public String getActionUIClass()
	{
		return AddPortActionUI.class.getName();
	}

	@Override
	public boolean isEnabled()
	{
		return isFunctionSymbol();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AddPinActionUI.class, "AddPortActionHelper.placement.guidance");
	}

	protected boolean isFunctionSymbol()
	{
		if (m_model.getSymbolDef() instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) m_model.getSymbolDef();
			return symDef.getSymbolType() == SymbolTypeEnum.FUNCTION;
		}
		return false;
	}
}
