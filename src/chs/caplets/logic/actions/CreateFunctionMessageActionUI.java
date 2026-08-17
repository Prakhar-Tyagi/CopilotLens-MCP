package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class CreateFunctionMessageActionUI extends ActionUI
{

	public CreateFunctionMessageActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = IconUtils.getMessageActiveIcon();
//		Integer iMnemonic = new Integer(KeyEvent.VK_M, 0);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_M, 0);

		putValue(NAME, ResourceMgr
				.getString(CreateFunctionMessageActionUI.class, "CreateFunctionMessageActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionMessageActionUI.class,
						"CreateFunctionMessageActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionMessageActionUI.class,
						"CreateFunctionMessageActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
//		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getMessageInActiveIcon();
	}

	public String getActionClass()
	{
		return CreateFunctionMessageAction.class.getName();
	}
}
