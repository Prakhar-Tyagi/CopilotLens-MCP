package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;

public class DisconnectFunctionPortAction extends AbstractDisconnectAction
{

	public DisconnectFunctionPortAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return DisconnectFunctionPortActionUI.class.getName();
	}
}
