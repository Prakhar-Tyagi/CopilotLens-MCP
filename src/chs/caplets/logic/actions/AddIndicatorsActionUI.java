/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 15, 2004 Time: 9:33:10 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.CapitalCapture,
		Application.CapitalArchitect, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_INDICATORS_ACTION",
		label = "Add Indicators",
		tooltip = "Add Indicators for multicore",
		icon = "ico_indicator_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddIndicatorsActionUI extends ActionUI
{

	public AddIndicatorsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddIndicatorsAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(AddIndicatorsActionUI.class, "AddIndicatorsActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddIndicatorsActionUI.class, "AddIndicatorsActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddIndicatorsActionUI.class, "AddIndicatorsActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_I));
	}
}
