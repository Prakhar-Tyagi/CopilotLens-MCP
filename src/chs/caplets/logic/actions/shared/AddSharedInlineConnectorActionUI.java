/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SHARED_INLINE_CONNECTOR_ACTION",
		label = "Add Shared Inline",
		tooltip = "Add Shared Inline...",
		icon = "ico_shared_inline_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSharedInlineConnectorActionUI extends ActionUI
{

	public AddSharedInlineConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddSharedInlineConnectorAction.class.getName();
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_inline_active.gif");
		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(AddSharedInlineConnectorActionUI.class, "AddSharedInlineConnectorActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(AddSharedInlineConnectorActionUI.class,
				"AddSharedInlineConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddSharedInlineConnectorActionUI.class,
				"AddSharedInlineConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSharedInlineConnectorActionUI.class,
				"AddSharedInlineConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}
}
