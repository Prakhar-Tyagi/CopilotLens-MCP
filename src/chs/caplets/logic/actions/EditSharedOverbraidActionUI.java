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
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_EDIT_SHARED_OVERBRAID_ACTION",
		label = "Add Shared Overbraid",
		tooltip = "Create Shared Overbraid(V)",
		icon = "ico_shared_overbraid_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class EditSharedOverbraidActionUI extends ActionUI
{

	public EditSharedOverbraidActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr.getMnemonic(EditSharedOverbraidActionUI.class,
				"EditSharedOverbraidActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getStringForMenu(EditSharedOverbraidActionUI.class,
				"EditSharedOverbraidActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(EditSharedOverbraidActionUI.class, "EditSharedOverbraidActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(EditSharedOverbraidActionUI.class, "EditSharedOverbraidActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_overbraid_active.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				ResourceMgr.getString(CreateOverbraidActionUI.class, "EditSharedOverbraidActionUI.accelerator.decl")));
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_overbraid_inactive.gif");
	}

	public String getActionClass()
	{
		return EditSharedOverbraidAction.class.getName();
	}
}
