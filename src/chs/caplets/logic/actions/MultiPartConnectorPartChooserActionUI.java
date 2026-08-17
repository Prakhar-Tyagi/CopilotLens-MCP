package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * * Created by nagamani on 05-07-2017.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class MultiPartConnectorPartChooserActionUI extends ActionUI
{

	public MultiPartConnectorPartChooserActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Create all UI elements for the action
	 */
	public void setupUI()
	{
		String name =
				ResourceMgr.getStringForMenu(MultiPartConnectorPartChooserActionUI.class,
						"MultiPartConnectorPartChooserActionUI.String.decl");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_selection_active.gif");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, name); //use the name as short/long description
		putValue(LONG_DESCRIPTION, name);
		putValue(SMALL_ICON, icon);
	}

	/**
	 * Return our matching ActionRT class
	 */
	public String getActionClass()
	{
		return MultiPartConnectorPartChooserActionUI.class.getName();
	}
}
