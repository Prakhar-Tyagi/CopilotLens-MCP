/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.KeyStroke;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Feb 13, 2004 Time: 12:25:42 PM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CROSS_LINK_ACTION",
		label = "View Related Items",
		tooltip = "View Related Items(X)",
		icon = "cross_link",
		buttonStyle = "SMALL_IMAGE")
public class CrossLinkActionUI extends ActionUI
{

	public CrossLinkActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return CrossLinkAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(CrossLinkActionUI.class, "CrossLinkActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CrossLinkActionUI.class, "CrossLinkActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CrossLinkActionUI.class, "CrossLinkActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(CrossLinkActionUI.class, "CrossLinkActionUI.mnemonic")));
		char accelerator = ResourceMgr.getChar(CrossLinkActionUI.class, "CrossLinkActionUI.accelerator");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator, 0));
	}
}
