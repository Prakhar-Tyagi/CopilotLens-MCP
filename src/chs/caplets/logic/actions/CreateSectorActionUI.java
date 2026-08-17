package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign, Application.ArtisanFunction}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SECTOR_ACTION",
		label = "Add Sector",
		tooltip = "Add Sector",
		icon = "ico_sector_active",
		buttonStyle = "MEDIUM_IMAGE")
public class CreateSectorActionUI extends ActionUI
{

	public CreateSectorActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = IconUtils.getSectorActiveIcon();
//		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_F);
//
//		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateSectorActionUI.class, "CreateSectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateSectorActionUI.class, "CreateSectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateSectorActionUI.class, "CreateSectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return CreateSectorAction.class.getName();
	}
}
