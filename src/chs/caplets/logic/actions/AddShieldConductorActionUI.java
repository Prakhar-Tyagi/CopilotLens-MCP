/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Instantiate shield conductor from browser tab
 *
 * @author chandras on 22-10-2021.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class AddShieldConductorActionUI extends ActionUI
{

	/**
	 * Constructor for the AddShieldConductorActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddShieldConductorActionUI(ICaplet caplet)
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
				.getMnemonic(AddShieldConductorActionUI.class, "AddShieldConductorActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME,
				ResourceMgr.getString(AddShieldConductorActionUI.class, "AddShieldConductorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddShieldConductorActionUI.class,
				"AddShieldConductorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddShieldConductorActionUI.class,
				"AddShieldConductorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddShieldConductorAction.class.getName();
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_inactive.gif");
	}
}
