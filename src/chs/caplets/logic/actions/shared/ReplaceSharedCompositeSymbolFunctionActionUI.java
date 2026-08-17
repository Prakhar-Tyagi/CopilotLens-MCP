package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class ReplaceSharedCompositeSymbolFunctionActionUI extends ReplaceSharedCompositeSymbolActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public ReplaceSharedCompositeSymbolFunctionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolFunctionActionUI.mnemonic"));
		putValue(NAME, ResourceMgr.getStringForMenu(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolFunctionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolFunctionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolFunctionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return ReplaceSharedCompositeSymbolFunctionAction.class.getName();
	}
}
