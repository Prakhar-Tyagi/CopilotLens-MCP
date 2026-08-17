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
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_OVERBRAID_ACTION",
		label = "Add Overbraid",
		tooltip = "Overbraid(O)",
		icon = "ico_overbraid_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateOverbraidActionUI extends ActionUI
{

	public CreateOverbraidActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(CreateOverbraidActionUI.class, "CreateOverbraidActionUI.mnemonic.decl")));
		putValue(NAME,
				ResourceMgr.getStringForMenu(CreateOverbraidActionUI.class, "CreateOverbraidActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateOverbraidActionUI.class, "CreateOverbraidActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateOverbraidActionUI.class, "CreateOverbraidActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_overbraid_active.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				ResourceMgr.getString(CreateOverbraidActionUI.class, "CreateOverbraidActionUI.accelerator.decl")));
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_overbraid_inactive.gif");
	}

	public String getActionClass()
	{
		return CreateOverbraidAction.class.getName();
	}
}
