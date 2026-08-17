/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
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
import java.awt.Event;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.CapitalArchitect,
		Application.CapitalCapture, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_ADD_DEVICE_FROM_LIBRARY_PART_ACTION",
		label = "Add Device From Library Part",
		tooltip = "Add a device from a library part(Ctrl+L)",
		icon = "ico_device_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddDeviceFromLibraryPartActionUI extends ActionUI
{

	public AddDeviceFromLibraryPartActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryPartActionUI.mnemonic.decl"));
		putValue(NAME, ResourceMgr.getStringForMenu(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryPartActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryPartActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddDeviceFromLibraryPartActionUI.class,
				"AddDeviceFromLibraryPartActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.CTRL_MASK));
	}

	public String getActionClass()
	{
		return AddDeviceFromLibraryPartAction.class.getName();
	}
}
