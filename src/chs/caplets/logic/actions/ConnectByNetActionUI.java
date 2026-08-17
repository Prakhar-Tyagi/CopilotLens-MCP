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

@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalLogicDesigner},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_CONNECT_BY_NET_ACTION",
		label = "Connect pins by Net",
		tooltip = "Connect by Net(Shift+C)",
		icon = "ico_net_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ConnectByNetActionUI extends ActionUI
{

	public ConnectByNetActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ConnectByNetActionUI.class, "ConnectByNetActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ConnectByNetActionUI.class, "ConnectByNetActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ConnectByNetActionUI.class, "ConnectByNetActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(ConnectByNetActionUI.class, "ConnectByNetActionUI.mnemonic.decl"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.SHIFT_MASK));
	}

	public String getActionClass()
	{
		return ConnectByNetAction.class.getName();
	}
}
