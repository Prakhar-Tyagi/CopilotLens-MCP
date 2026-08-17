package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.harness.AbstractCreateRingTerminalActionUI;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_RING_TERMINAL_ACTION",
		label = "Add Ring Terminal",
		tooltip = "Add Ring Terminal(Ctrl+ Shift + T)",
		icon = "ico_ring_terminal_active",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class CreateRingTerminalActionUI extends AbstractCreateRingTerminalActionUI
{
	public CreateRingTerminalActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		super.setupUI();

		putValue(NAME, ResourceMgr.getString(CreateRingTerminalActionUI.class, "CreateRingTerminalActionUI.name.decl"));

		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateRingTerminalActionUI.class, "CreateRingTerminalActionUI.shortDesc.decl"));

		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateRingTerminalActionUI.class, "CreateRingTerminalActionUI.longDesc.decl"));

		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(CreateRingTerminalActionUI.class, "CreateRingTerminalActionUI.mnemonic.text");
		putValue(MNEMONIC_KEY, iMnemonic);

	}

	public String getActionClass()
	{
		return CreateRingTerminalAction.class.getName();
	}

}
