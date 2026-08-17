package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.harness.AbstractCreateRingTerminalFromPartBrowserAction;
import chs.caplets.logic.actions.CreateRingTerminalAction;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreateRingTerminalFromPartBrowserAction extends AbstractCreateRingTerminalFromPartBrowserAction
{

	public CreateRingTerminalFromPartBrowserAction()
	{
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateRingTerminalFromPartBrowserAction.class,
				"CreateRingTerminalFromPartBrowserAction.longDesc.decl"));
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(CreateRingTerminalAction.class);
	}
}
