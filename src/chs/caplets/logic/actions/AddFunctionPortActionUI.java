package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class AddFunctionPortActionUI extends ActionUI
{

	public AddFunctionPortActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.FUNCTIONPIN_ACTIVE_ICON);

		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_P, 0);

		putValue(NAME, ResourceMgr.getString(AddFunctionPortActionUI.class, "AddPortActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddFunctionPortActionUI.class, "AddPortActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddFunctionPortActionUI.class, "AddPortActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.FUNCTIONPIN_INACTIVE_ICON);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddFunctionPortAction.class.getName();
	}
}
