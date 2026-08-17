/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.action.immersed.ImmersedActions;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Oct 17, 2003 Time: 3:00:38 PM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedActions({
		@ImmersedAction(actionId = "CAPITAL_REMOVE_HOME_ACTION", instanceName = "NotHome",
				label = "Remove Home Condition", tooltip = "Remove Home Condition", icon = "ico_remove_home_active",
				buttonStyle = "SMALL_IMAGE_AND_TEXT"),
		@ImmersedAction(actionId = "CAPITAL_INDICATE_HOME_ACTION", instanceName = "Home",
				label = "Indicate Home Condition", tooltip = "Indicate Home Condition", icon = "ico_indicate_home_active",
				buttonStyle = "SMALL_IMAGE_AND_TEXT")
})
public class ToggleHomeActionUI extends ActionUI
{

	public static final String MARK_HOME = "Home";
	public static final String REMOVE_HOME = "NotHome";

	public ToggleHomeActionUI(ICaplet caplet, String instanceName)
	{
		super(caplet, instanceName);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		String name = ResourceMgr
				.getString(ToggleHomeActionUI.class, "ToggleHomeActionUI." + getActionUIInstanceName() + ".name.decl");
		String longDesc = ResourceMgr.getString(ToggleHomeActionUI.class,
				"ToggleHomeActionUI." + getActionUIInstanceName() + ".longDesc.decl");
		char mnemonic = ResourceMgr
				.getMnemonic(ToggleHomeActionUI.class, "ToggleHomeActionUI." + getActionUIInstanceName() + ".mnemonic");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, name);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(mnemonic));
	}

	public String getActionClass()
	{
		return ToggleHomeAction.class.getName();
	}
}
