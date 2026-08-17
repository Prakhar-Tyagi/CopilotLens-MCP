package chs.caplets.logic.actions.shared;


import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by nagamani on 23-02-2015.
 */
@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class AddSharedSignalActionUI extends ActionUI
{

	public AddSharedSignalActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddSharedSignalAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddSharedSignalActionUI.class, "AddSharedSignalActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(AddSharedSignalActionUI.class, "AddSharedSignalActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedSignalActionUI.class, "AddSharedSignalActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedSignalActionUI.class, "AddSharedSignalActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_signal_active.gif"));
	}
}
