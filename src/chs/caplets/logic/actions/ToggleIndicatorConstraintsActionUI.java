/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2026 Siemens
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

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.SvcDoc, Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_TOGGLE_INDICATOR_CONSTRAINTS_ACTION",
		label = "Toggle Indicator",
		tooltip = "Toggle Indicator",
		icon = "ico_toggle_indicator_constraints_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ToggleIndicatorConstraintsActionUI extends ActionUI
{

	private static final String ALLOW_NAME = ResourceMgr
			.getString(ToggleIndicatorConstraintsActionUI.class, "ToggleIndicatorConstraintsActionUI.Allow.name");
	private static final String ALLOW_LONGDESC = ResourceMgr
			.getString(ToggleIndicatorConstraintsActionUI.class, "ToggleIndicatorConstraintsActionUI.Allow.longDesc");
	private static final char ALLOW_MNEMONIC = ResourceMgr
			.getMnemonic(ToggleIndicatorConstraintsActionUI.class, "ToggleIndicatorConstraintsActionUI.Allow.mnemonic");

	public ToggleIndicatorConstraintsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, ALLOW_NAME);
		putValue(SHORT_DESCRIPTION, ALLOW_NAME);
		putValue(LONG_DESCRIPTION, ALLOW_LONGDESC);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(ALLOW_MNEMONIC));
	}

	public String getActionClass()
	{
		return ToggleIndicatorConstraintsAction.class.getName();
	}
}
