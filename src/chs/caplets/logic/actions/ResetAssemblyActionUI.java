/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
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

/**
 * Created by IntelliJ IDEA. User: brangan Date: May 25, 2011 Time: 2:20:45 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RESET_ASSEMBLY_ACTION",
		label = "Update Assembly Supplements",
		tooltip = "Update additional content on connectors in assemblies(backshells, inserts)",
		icon = "ico_reset_assembly_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ResetAssemblyActionUI extends ActionUI
{

	public ResetAssemblyActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Integer mnemonicVal = (int) ResourceMgr.getMnemonic(this, "ResetAssemblyActionUI.logic.mnemonic.decl");
		putValue(MNEMONIC_KEY, Integer.valueOf(mnemonicVal));
		putValue(NAME, ResourceMgr.getString(ResetAssemblyActionUI.class, "ResetAssemblyActionUI.logic.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ResetAssemblyActionUI.class, "ResetAssemblyActionUI.logic.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ResetAssemblyActionUI.class, "ResetAssemblyActionUI.logic.longDesc.decl"));

		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(SMALL_ICON, icon);
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
		return ResetAssemblyAction.class.getName();
	}
}