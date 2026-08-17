/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_RIBBON_GENERATE_HARNESS_CONNECTORS_ACTION",
		label = "Generate Connectors",
		tooltip = "Generate Harness Connectors",
		icon = "ico_generate_harness_connectors")
public class GenerateHarnessConnActionUI extends ActionUI
{

	public GenerateHarnessConnActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_generate_harness_connectors_active.gif");

		putValue(NAME,
				ResourceMgr.getString(GenerateHarnessConnActionUI.class, "GenerateHarnessConnActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(GenerateHarnessConnActionUI.class, "GenerateHarnessConnActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(GenerateHarnessConnActionUI.class, "GenerateHarnessConnActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, (int) ResourceMgr
				.getMnemonic(GenerateHarnessConnActionUI.class, "GenerateHarnessConnActionUI.mnemonic"));
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_generate_harness_connectors_inactive.gif");
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		return super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return GenerateHarnessConnAction.class.getName();
	}
}
