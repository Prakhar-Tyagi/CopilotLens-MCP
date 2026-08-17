/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.CapitalCapture,
		Application.CapitalArchitect, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_MULTICORE_ACTION",
		label = "Add Multicore",
		tooltip = "Generic Multicore(M)",
		icon = "ico_multicore_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateMulticoreActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateMulticoreActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		// PW - 04/16/03 - Defect#3499
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_multicore_active.gif");
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(CreateMulticoreActionUI.class, "CreateMulticoreActionUI.mnemonic.decl")));
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, 0);

		if (AppInfo.isCapitalCapture()) {
			putValue(NAME, ResourceMgr.getStringForMenu(CreateMulticoreActionUI.class,
					"CreateMulticoreActionUI.Capture.name.decl"));
		}
		else {
			putValue(NAME,
					ResourceMgr.getStringForMenu(CreateMulticoreActionUI.class, "CreateMulticoreActionUI.name.decl"));
		}
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateMulticoreActionUI.class, "CreateMulticoreActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateMulticoreActionUI.class, "CreateMulticoreActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_multicore_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateMulticoreAction.class.getName();
	}
}

