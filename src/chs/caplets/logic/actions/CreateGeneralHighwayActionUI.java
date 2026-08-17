/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Instantiate general highway from home tab
 *
 * @created June 7, 2024
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.ArtisanFunction, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_GENERAL_HIGHWAY_ACTION",
		label = "Highway",
		tooltip = "Add Highway(G)",
		icon = "ico_highway_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateGeneralHighwayActionUI extends CreateHighwayActionUI
{

	public CreateGeneralHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.HIGHWAY_ICON);
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_G);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, 0);

		putValue(NAME, ResourceMgr.getString(CreateConductorActionUI.class, "CreateGeneralHighwayActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateGeneralHighwayActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateGeneralHighwayActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	public String getActionClass()
	{
		return CreateGeneralHighwayAction.class.getName();
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.HIGHWAY_DISABLED_ICON);
	}
}
