/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.CapitalCapture,
		Application.CapitalArchitect, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_UPDATE_PART_ACTION",
		label = "Library Part",
		tooltip = "Update Library Part",
		icon = "update_part",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class UpdatePartActionUI extends ActionUI
{

	public UpdatePartActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(UpdatePartActionUI.class, "UpdatePartActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UpdatePartActionUI.class, "UpdatePartActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(UpdatePartActionUI.class, "UpdatePartActionUI.longDesc.decl"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(UpdatePartActionUI.class, "UpdatePartActionUI.mnemonic.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}

	public String getActionClass()
	{
		return UpdatePartAction.class.getName();
	}
}
