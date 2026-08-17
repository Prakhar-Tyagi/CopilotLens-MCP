package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 8/1/15
 */

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner,Application.CapitalEssentialsDesign,Application.SEElectricalDesign})
public class PlaceAssemblyTreeActionUI extends ActionUI
{

	public PlaceAssemblyTreeActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_assembly_active.gif");

		putValue(NAME, ResourceMgr.getStringForMenu(CreateAssemblyActionUI.class, "PlaceAssemblyTreeActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateAssemblyActionUI.class, "PlaceAssemblyTreeActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateAssemblyActionUI.class, "PlaceAssemblyTreeActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_assembly_inactive.gif");
	}

	public String getActionClass()
	{
		return PlaceAssemblyTreeAction.class.getName();
	}
}

