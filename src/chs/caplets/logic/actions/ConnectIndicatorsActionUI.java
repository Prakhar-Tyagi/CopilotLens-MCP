package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture,
		Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign})
public class ConnectIndicatorsActionUI extends ActionUI
{

	public ConnectIndicatorsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(ConnectIndicatorsActionUI.class, "ConnectIndicatorsActionUI.logic.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(ConnectIndicatorsActionUI.class,
				"ConnectIndicatorsActionUI.logic.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(ConnectIndicatorsActionUI.class,
				"ConnectIndicatorsActionUI.logic.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
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

	public String getActionClass()
	{
		return ConnectIndicatorsAction.class.getName();
	}
}
