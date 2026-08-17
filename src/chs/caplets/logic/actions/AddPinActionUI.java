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
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

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
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_ADD_PIN_ACTION",
		label = "Add Pin",
		tooltip = "Add Pin(P)",
		icon = "ico_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddPinActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_P);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0);

		putValue(NAME, ResourceMgr.getString(AddPinActionUI.class, "AddPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddPinActionUI.class, "AddPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddPinActionUI.class, "AddPinActionUI.longDesc.decl"));

		putValue(SMALL_ICON, getActiveIcon());
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	@Nullable
	protected Icon getActiveIcon()
	{
		ICaplet caplet = getCaplet();
		if (caplet.isFunctionCaplet()) {
			return CHSImageLoader.loadImageIcon(CHSImages.FUNCTIONPIN_ACTIVE_ICON);
		}
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_active.gif");
	}

	@Nullable
	public Icon getInactiveIcon()
	{
		if (getCaplet().isFunctionCaplet()) {
			return CHSImageLoader.loadImageIcon(CHSImages.FUNCTIONPIN_INACTIVE_ICON);
		}
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddPinAction.class.getName();
	}

	public void updateUI()
	{
		super.updateUI();
		putValue(SMALL_ICON, getActiveIcon());
	}


}

