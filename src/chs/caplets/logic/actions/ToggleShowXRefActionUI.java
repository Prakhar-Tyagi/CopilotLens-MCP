/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
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
 * Created by IntelliJ IDEA. User: lstamper Date: Jun 25, 2004 Time: 2:01:00 PM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode=ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedActions({
		@ImmersedAction(actionId = "CAPITAL_HIDE_XREF_ACTION", instanceName = "Hide",
				label = "Hide Cross-reference Text", tooltip = "Hide Cross-reference Text", icon = "ico_hide_xref_active",
				buttonStyle = "SMALL_IMAGE_AND_TEXT"),
		@ImmersedAction(actionId = "CAPITAL_SHOW_XREF_ACTION", instanceName = "Show",
				label = "Show Cross-reference Text", tooltip = "Show Cross-reference Text", icon = "ico_show_xref_active",
				buttonStyle = "SMALL_IMAGE_AND_TEXT")
})
public class ToggleShowXRefActionUI extends ActionUI
{

	public static final String SHOW_XREF = "Show";
	public static final String HIDE_XREF = "Hide";

	public ToggleShowXRefActionUI(ICaplet caplet, String instanceName)
	{
		super(caplet, instanceName);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		String name = ResourceMgr.getString(ToggleShowXRefActionUI.class,
				"ToggleShowXRefActionUI." + getActionUIInstanceName() + ".name");
		String longDesc = ResourceMgr.getString(ToggleShowXRefActionUI.class,
				"ToggleShowXRefActionUI." + getActionUIInstanceName() + ".longDesc");
		char mnemonic = ResourceMgr.getMnemonic(ToggleShowXRefActionUI.class,
				"ToggleShowXRefActionUI." + getActionUIInstanceName() + ".mnemonic");
		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, name);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(mnemonic));
	}

	public String getActionClass()
	{
		return ToggleShowXRefAction.class.getName();
	}
}
