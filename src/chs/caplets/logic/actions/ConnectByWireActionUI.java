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

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_CONNECT_BY_WIRE_ACTION",
		label = "Connect pins by Wire",
		tooltip = "Connect by Wire(Shift+W)",
		icon = "ico_wire_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ConnectByWireActionUI extends ActionUI
{

	public ConnectByWireActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ConnectByWireActionUI.class, "ConnectByWireActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ConnectByWireActionUI.class, "ConnectByWireActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ConnectByWireActionUI.class, "ConnectByWireActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(ConnectByWireActionUI.class, "ConnectByWireActionUI.mnemonic.decl"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.SHIFT_MASK));
	}

	public String getActionClass()
	{
		return ConnectByWireAction.class.getName();
	}
}
