/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2011-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA. User: creddy Date: May 18, 2011 Time: 4:40:05 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_SHOW_STACK_USAGE_ACTION",
		label = "View content of highways and stacked pins",
		tooltip = "View content of highways and stacked pins",
		icon = "show_stack_usage",
		buttonStyle = "SMALL_IMAGE")
public class ShowStackUsageActionUI extends ActionUI
{

	public ShowStackUsageActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = KeyEvent.VK_U;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(ShowStackUsageActionUI.class, "ShowStackUsageActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ShowStackUsageActionUI.class, "ShowStackUsageActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ShowStackUsageActionUI.class, "ShowStackUsageActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return ShowStackUsageAction.class.getName();
	}
}
