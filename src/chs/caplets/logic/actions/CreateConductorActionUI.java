/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
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
import javax.swing.KeyStroke;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.ArtisanFunction}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_CONDUCTOR_ACTION",
		label = "Add Net",
		tooltip = "Add Net(N)",
		icon = "ico_net_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateConductorActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateConductorActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateConductorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_net_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_N);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, 0);

		putValue(NAME, ResourceMgr.getString(CreateConductorActionUI.class, "CreateConductorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateConductorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateConductorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_net_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateConductorAction.class.getName();
	}
}

