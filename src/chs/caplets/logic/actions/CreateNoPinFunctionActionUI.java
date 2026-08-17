package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.ArtisanFunction})
public class CreateNoPinFunctionActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateNoPinFunctionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = IconUtils.getFunctionCompActiveIcon();
		putValue(NAME,
				ResourceMgr.getString(CreateNoPinFunctionActionUI.class, "CreateNoPinFunctionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateNoPinFunctionActionUI.class, "CreateNoPinFunctionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateNoPinFunctionActionUI.class, "CreateNoPinFunctionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getFunctionCompInActiveIcon();
	}

	public String getActionClass()
	{
		return CreateNoPinFunctionAction.class.getName();
	}
}
