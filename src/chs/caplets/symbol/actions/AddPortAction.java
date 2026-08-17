package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.schem.IGenericSchemPin;

public class AddPortAction extends AddPinAction
{

	public AddPortAction(ICapletController controller)
	{
		super(controller);
	}

	protected IAddPinActionHelper createAddPinActionHelper()
	{
		return new AddPortActionHelper(m_model);
	}

	protected void connectToLink(IGenericSchemPin pin)
	{

	}
}
