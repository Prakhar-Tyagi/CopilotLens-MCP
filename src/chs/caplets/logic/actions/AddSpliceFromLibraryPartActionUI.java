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

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SPLICE_FROM_LIBRARY_PART_ACTION",
		label = "Add Splice From Library",
		tooltip = "Add a splice from a library",
		icon = "ico_splice_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSpliceFromLibraryPartActionUI
		extends ActionUI
{

	public AddSpliceFromLibraryPartActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(AddSpliceFromLibraryPartActionUI.class,
				"AddSpliceFromLibraryPartActionUI.mnemonic.decl"));
		putValue(NAME, ResourceMgr.getStringForMenu(AddSpliceFromLibraryPartActionUI.class,
				"AddSpliceFromLibraryPartActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddSpliceFromLibraryPartActionUI.class,
				"AddSpliceFromLibraryPartActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSpliceFromLibraryPartActionUI.class,
				"AddSpliceFromLibraryPartActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_splice_active.gif"));
	}

	public String getActionClass()
	{
		return AddSpliceFromLibraryPartAction.class.getName();
	}
}
