package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.BaseDisassociateConnectorActionUI;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 8 Apr, 2013
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class DisassociateConnectorActionUI extends BaseDisassociateConnectorActionUI
{

	public String getActionClass()
	{
		return DisassociateConnectorAction.class.getName();
	}

	public DisassociateConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}

		return super.isEnabled();
	}
}
