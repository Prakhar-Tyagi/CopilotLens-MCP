package chs.caplets.logic.actions.serviceDocumentation.smartflows;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.SvcDoc})
public class ChangeFlowDirectionActionUI extends ActionUI
{

	public ChangeFlowDirectionActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif");
		putValue(NAME, ResourceMgr
				.getString(ChangeFlowDirectionActionUI.class, "ProcessFlowDirectionTogglerActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ChangeFlowDirectionActionUI.class,
						"ProcessFlowDirectionTogglerActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ChangeFlowDirectionActionUI.class,
						"ProcessFlowDirectionTogglerActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return ChangeFlowDirectionAction.class.getName();
	}
}
