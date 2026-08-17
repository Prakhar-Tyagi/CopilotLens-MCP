package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class AddSharedMessageActionUI extends ActionUI
{

	public AddSharedMessageActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddSharedMessageAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddSharedMessageActionUI.class, "AddSharedMessageActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(AddSharedMessageActionUI.class, "AddSharedMessageActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedMessageActionUI.class, "AddSharedMessageActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedMessageActionUI.class, "AddSharedMessageActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon(CHSImages.SHARED_FUNCTION_MESSAGE_ACTIVE));
	}
}
