package chs.caplets.shared.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class GroupAttributeUpdateActionUI extends ActionUI
{

	protected GroupAttributeUpdateActionUI(@NotNull ICaplet caplet)
	{
		super(caplet, false);
	}

	@Override public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(GroupAttributeUpdateActionUI.class, "GroupAttributeUpdateActionUI.Update.Name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr
				.getString(GroupAttributeUpdateActionUI.class, "GroupAttributeUpdateActionUI.Update.ShortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(GroupAttributeUpdateActionUI.class, "GroupAttributeUpdateActionUI.Update.LongDesc"));
	}

	@Override public String getActionClass()
	{
		return GroupAttributeUpdateAction.class.getName();
	}
}
