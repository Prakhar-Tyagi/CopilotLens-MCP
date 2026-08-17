package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class ManageSignalsActionUI extends ActionUI
{

	public ManageSignalsActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(ManageSignalsAction.class, "ManageSignalsAction.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ManageSignalsAction.class, "ManageSignalsAction.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ManageSignalsAction.class, "ManageSignalsAction.longDesc.text"));
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.FUNCTION_COND_ACTIVE_ICON);
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK));
	}

	@Override public String getActionClass()
	{
		return ManageSignalsAction.class.getName();
	}
}
