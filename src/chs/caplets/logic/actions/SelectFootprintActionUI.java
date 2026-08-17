package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.ImageIcon;

/**
 * @author chandras on 07-07-2017.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class SelectFootprintActionUI extends ActionUI
{

	public SelectFootprintActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		String name = ResourceMgr.getStringForMenu(SelectFootprintActionUI.class,
				"SelectFootprintActionUI.name.decl");
		String sdesc = ResourceMgr.getStringForMenu(SelectFootprintActionUI.class,
				"SelectFootprintActionUI.shortDesc.decl");
		String ldesc = ResourceMgr.getStringForMenu(SelectFootprintActionUI.class,
				"SelectFootprintActionUI.longDesc.decl");
		char mnemonic = ResourceMgr.getMnemonic(SelectFootprintActionUI.class,
				"SelectFootprintActionUI.mnemonic.decl");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, sdesc);
		putValue(LONG_DESCRIPTION, ldesc);
		putValue(MNEMONIC_KEY, (int) mnemonic);
		ImageIcon imageIcon = CHSImageLoader.loadImageIcon("chs/images/javafx_ui/footprint-small.png");
		putValue(SMALL_ICON, imageIcon);
	}

	@Override public String getActionClass()
	{
		return SelectFootprintAction.class.getName();
	}
}
