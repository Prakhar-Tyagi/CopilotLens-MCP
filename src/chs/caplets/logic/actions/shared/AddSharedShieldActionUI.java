/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions.shared;

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
public class AddSharedShieldActionUI extends ActionUI
{

	/**
	 * Constructor for the AddShieldConductorActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddSharedShieldActionUI(ICaplet caplet)
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
				.getMnemonic(AddSharedShieldActionUI.class, "AddSharedShieldActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(AddSharedShieldActionUI.class,
				"AddSharedShieldActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddSharedShieldActionUI.class,
				"AddSharedShieldActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSharedShieldActionUI.class,
				"AddSharedShieldActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddSharedShieldAction.class.getName();
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_inactive.gif");
	}
}
