/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
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

/**
 * Description of the Class
 *
 * @author gregc
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_ADD_INSTANCE_ACTION",
		label = "Symbol",
		tooltip = "Add Symbol...",
		icon = "ico_instantiate_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddInstanceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddInstanceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_instantiate_active.gif");
		putValue(NAME, ResourceMgr.getString(AddInstanceActionUI.class, "AddInstanceActionUI.name.decl.capture"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddInstanceActionUI.class, "AddInstanceActionUI.shortDesc.decl.capture"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddInstanceActionUI.class, "AddInstanceActionUI.longDesc.decl.capture"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddInstanceActionUI.class, "AddInstanceActionUI.mnemonic.decl.capture")));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddInstanceAction.class.getName();
	}
}

