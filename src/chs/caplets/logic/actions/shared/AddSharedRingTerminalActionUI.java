/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
@ImmersedAction(actionId = "CAPITAL_ADD_SHARED_RING_TERMINAL_ACTION",
		label = "Add Shared Ring Terminal",
		tooltip = "Add Shared Ring Terminal...",
		icon = "ico_shared_ringterminal_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSharedRingTerminalActionUI extends ActionUI
{
	public AddSharedRingTerminalActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_ringterminal_active.gif");
//		Integer iMnemonic = (int) ResourceMgr
//				.getMnemonic(AddSharedPlugConnectorActionUI.class, "AddSharedRingTerminalActionUI.mnemonic");
//
//		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(AddSharedRingTerminalActionUI.class,
				"AddSharedRingTerminalActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddSharedRingTerminalActionUI.class,
				"AddSharedRingTerminalActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSharedRingTerminalActionUI.class,
				"AddSharedRingTerminalActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddSharedRingTerminalAction.class.getName();
	}
}
