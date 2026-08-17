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
public class CreateFunctionConductorActionUI extends ActionUI
{

	public CreateFunctionConductorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = IconUtils.getFunctionCondActiveIcon();
//		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_S);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);

		putValue(NAME, ResourceMgr
				.getString(CreateFunctionConductorActionUI.class, "CreateFunctionConductorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionConductorActionUI.class,
						"CreateFunctionConductorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionConductorActionUI.class,
						"CreateFunctionConductorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
//		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getFunctionCondInActiveIcon();
	}

	public String getActionClass()
	{
		return CreateFunctionConductorAction.class.getName();
	}
}