package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 16/07/12 Time: 11:49 To change this template use File | Settings |
 * File Templates.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class AssociateSymbolActionUI extends ActionUI
{

	public AssociateSymbolActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		String name = ResourceMgr.getStringForMenu(AssociateSymbolAction.class, "associatesymbol.action.name");
		String shortDescription =
				ResourceMgr.getStringForMenu(AssociateSymbolAction.class, "associatesymbol.action.shortdescription");
		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDescription);
	}

	@Override public String getActionClass()
	{
		return AssociateSymbolAction.class.getName();
	}
}
