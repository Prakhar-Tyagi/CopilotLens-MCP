/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.utilities.ResourceMgr;

/**
 * ActionUI class for Add General Highway action
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
				Application.ArtisanFunction, Application.SEElectricalDesign})
public class AddGeneralHighwayActionUI extends AddHighwayActionUI
{
	public AddGeneralHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddGeneralHighwayActionUI.class, "AddGeneralHighwayActionUI.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddGeneralHighwayActionUI.class, "AddGeneralHighwayActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddGeneralHighwayActionUI.class, "AddGeneralHighwayActionUI.longDesc.text"));
	}

	public String getActionClass()
	{
		return AddGeneralHighwayAction.class.getName();
	}
}
