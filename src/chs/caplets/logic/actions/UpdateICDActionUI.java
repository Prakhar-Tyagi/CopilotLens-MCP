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

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_UPDATE_ICD_ACTION",
		label = "Update From ICD",
		tooltip = "Update the device from ICD",
		icon = "ico_update_icd_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class UpdateICDActionUI extends ActionUI
{

	public UpdateICDActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.short"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(UpdateICDActionUI.class, "UpdateICDActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
	}

	@Override public String getActionClass()
	{
		return UpdateICDAction.class.getName();
	}
}
