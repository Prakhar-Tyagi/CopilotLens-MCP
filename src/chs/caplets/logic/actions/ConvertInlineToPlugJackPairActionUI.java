package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class ConvertInlineToPlugJackPairActionUI extends ActionUI
{

	public ConvertInlineToPlugJackPairActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ConvertInlineToPlugJackPairActionUI.class,
				"ConvertInlineToPlugJackPairActionUI.action.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ConvertInlineToPlugJackPairActionUI.class,
						"ConvertInlineToPlugJackPairActionUI.action.shortdescription"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ConvertInlineToPlugJackPairActionUI.class,
						"ConvertInlineToPlugJackPairActionUI.action.longdescription"));
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled();
	}

	@Override public String getActionClass()
	{
		return ConvertInlineToPlugJackPairAction.class.getName();
	}
}
