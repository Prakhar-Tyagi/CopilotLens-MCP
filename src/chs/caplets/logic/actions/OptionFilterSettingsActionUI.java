/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionMode;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect, Application.CapitalCapture,
				Application.SvcDoc, Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_OPTION_FILTER_SETTINGS_ACTION",
		label = "Filter Design",
		tooltip = "Filter Design Content",
		icon = "ico_filter_design")
public class OptionFilterSettingsActionUI extends ActionUI
{

	public OptionFilterSettingsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return OptionFilterSettingsAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(OptionFilterSettingsAction.class, "OptionFilterSettingsActionUI.mnemonic")));
		putValue(NAME,
				ResourceMgr.getStringForMenu(OptionFilterSettingsAction.class, "OptionFilterSettingsActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(OptionFilterSettingsAction.class, "OptionFilterSettingsActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(OptionFilterSettingsAction.class, "OptionFilterSettingsActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_filter_general.gif"));

		putValue(ActionMode.CAPLET_MODE, ActionMode.TOPOLOGY_EDIT);
	}
}
