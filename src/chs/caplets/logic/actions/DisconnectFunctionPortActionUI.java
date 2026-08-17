package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class DisconnectFunctionPortActionUI extends AbstractDisconnectActionUI
{

	public DisconnectFunctionPortActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	protected String getLongDescription()
	{
		return ResourceMgr
				.getString(DisconnectFunctionPortActionUI.class, "DisconnectFunctionPortActionUI.longDesc.decl");
	}

	public String getActionClass()
	{
		return DisconnectFunctionPortAction.class.getName();
	}
}
