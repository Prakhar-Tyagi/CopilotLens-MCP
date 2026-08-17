/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
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

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.CapitalCapture,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_CONNECT_BY_PIN_ACTION",
		label = "Connect By Pin",
		tooltip = "Connect By Pin(Shift+NumPad+)",
		icon = "ico_connect_by_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ConnectByPinActionUI extends ActionUI
{

	public ConnectByPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ConnectByPinActionUI.class, "ConnectByPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ConnectByPinActionUI.class, "ConnectByPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ConnectByPinActionUI.class, "ConnectByPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(ConnectByPinActionUI.class, "ConnectByPinActionUI.mnemonic.decl"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, Event.SHIFT_MASK));
	}

	public String getActionClass()
	{
		return ConnectByPinAction.class.getName();
	}
}
