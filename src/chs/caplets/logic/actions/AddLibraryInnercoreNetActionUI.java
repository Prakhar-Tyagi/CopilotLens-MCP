package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Jul 15, 2010 Time: 3:11:00 PM To change this template use File |
 * Settings | File Templates.
 */

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner,  Application.CapitalCapture, Application.CapitalArchitect})
public class AddLibraryInnercoreNetActionUI extends ActionUI
{

	public AddLibraryInnercoreNetActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddLibraryInnercoreNetAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddLibraryInnercoreNetActionUI.class,
				"AddLibraryInnercoreNetActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreNetActionUI.class,
				"AddLibraryInnercoreNetActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreNetActionUI.class,
				"AddLibraryInnercoreNetActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_N));
	}
}
