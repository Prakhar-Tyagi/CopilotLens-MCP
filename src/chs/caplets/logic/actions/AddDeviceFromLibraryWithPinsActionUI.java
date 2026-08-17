package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.CapitalArchitect,
		Application.CapitalCapture, Application.SEElectricalDesign})
public class AddDeviceFromLibraryWithPinsActionUI extends ActionUI
{

	public AddDeviceFromLibraryWithPinsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{

		putValue(NAME, ResourceMgr.getStringForMenu(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryWithPinsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryWithPinsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryWithPinsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
	}

	public String getActionClass()
	{
		return chs.caplets.logic.actions.AddDeviceFromLibraryWithPinsAction.class.getName();
	}
}