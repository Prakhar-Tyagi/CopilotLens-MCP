package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class CreateModularSchematicsActionUI extends ActionUI
{

	public CreateModularSchematicsActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(CreateModularSchematicsActionUI.class,
				"CreateModularSchematicsActionUI.action.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateModularSchematicsActionUI.class,
						"CreateModularSchematicsActionUI.action.shortdescription"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateModularSchematicsActionUI.class,
						"CreateModularSchematicsActionUI.action.longdescription"));
	}

	@Override public String getActionClass()
	{
		return CreateModularSchematicsAction.class.getName();
	}
}
