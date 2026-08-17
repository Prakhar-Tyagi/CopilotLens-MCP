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

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_SAVE_ASSEMBLY_CONNECTIVITY_TO_LIBRARY_ACTION",
		label = "Save Design into Assembly...",
		tooltip = "Save Design content into Library Assembly Part",
		icon = "ico_save_assembly_connectivity_to_library_active",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class SaveAssemblyConnectivityToLibraryActionUI extends ActionUI
{

	public SaveAssemblyConnectivityToLibraryActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(SaveAssemblyConnectivityToLibraryActionUI.class,
				"SaveAssemblyConnectivityToLibraryActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(SaveAssemblyConnectivityToLibraryActionUI.class,
						"SaveAssemblyConnectivityToLibraryActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(SaveAssemblyConnectivityToLibraryActionUI.class,
				"SaveAssemblyConnectivityToLibraryActionUI.longDesc.decl"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(SaveAssemblyConnectivityToLibraryActionUI.class,
						"SaveAssemblyConnectivityToLibraryActionUI.mnemonic.decl"));

		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}

	public String getActionClass()
	{
		return SaveAssemblyConnectivityToLibraryAction.class.getName();
	}
}
