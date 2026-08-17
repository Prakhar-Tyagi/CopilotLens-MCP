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

/**
 * Description of the Class
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
		Application.CapitalArchitect, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_SET_NON_PIN_REFERENCE_ACTION",
		label = "Make Non-Reference Pin",
		tooltip = "Make Non-Reference Pin",
		icon = "ico_pin_non_reference_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class SetPinNonReferenceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public SetPinNonReferenceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		String name = ResourceMgr.getString(SetPinNonReferenceActionUI.class, "SetPinNonReferenceActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(SetPinNonReferenceActionUI.class,
				"SetPinNonReferenceActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(SetPinNonReferenceActionUI.class,
				"SetPinNonReferenceActionUI.longDesc.decl");

		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(ResourceMgr.getMnemonic(SetPinNonReferenceActionUI.class,
				"SetPinNonReferenceActionUI.action.mnemonic"));

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return SetPinNonReferenceAction.class.getName();
	}
}

