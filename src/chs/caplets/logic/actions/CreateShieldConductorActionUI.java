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
import java.awt.event.KeyEvent;

/**
 * Description of the Class
 *
 * @author gregc
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_SHIELD_CONDUCTOR_ACTION",
		label = "Shield",
		tooltip = "Add Shield Termination(S)",
		icon = "ico_shield_termination_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateShieldConductorActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateConductorActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateShieldConductorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_active.gif");
		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(CreateShieldConductorActionUI.class, "CreateShieldConductorActionUI.mnemonic");
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME,
				ResourceMgr.getString(CreateShieldConductorActionUI.class, "CreateShieldConductorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateShieldConductorActionUI.class,
				"CreateShieldConductorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateShieldConductorActionUI.class,
				"CreateShieldConductorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateShieldConductorAction.class.getName();
	}
}

