package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class CreateFunctionBlockActionUI extends ActionUI
{

	public CreateFunctionBlockActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = IconUtils.getFunctionBlockInActiveIcon();
		Integer iMnemonic = KeyEvent.VK_K;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateFunctionBlockActionUI.class, "CreateFunctionBlockActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionBlockActionUI.class, "CreateFunctionBlockActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionBlockActionUI.class, "CreateFunctionBlockActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getFunctionCompInActiveIcon();
	}

	public String getActionClass()
	{
		return CreateFunctionBlockAction.class.getName();
	}
}