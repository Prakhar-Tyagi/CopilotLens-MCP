/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
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

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign })
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_ASSEMBLY_ACTION",
		label = "Edit Assemblies",
		tooltip = "Edit Assemblies...",
		icon = "ico_assembly_active",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class CreateAssemblyActionUI extends ActionUI
{

	public CreateAssemblyActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_assembly_active.gif");

		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(CreateAssemblyActionUI.class, "CreateAssemblyActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getStringForMenu(CreateAssemblyActionUI.class, "CreateAssemblyActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateAssemblyActionUI.class, "CreateAssemblyActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateAssemblyActionUI.class, "CreateAssemblyActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_assembly_inactive.gif");
	}

	public String getActionClass()
	{
		return CreateAssemblyAction.class.getName();
	}
}
