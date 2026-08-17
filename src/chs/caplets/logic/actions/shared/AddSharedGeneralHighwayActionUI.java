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
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Class that represents action user interface for AddSharedGeneralHighwayAction
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class AddSharedGeneralHighwayActionUI extends AddSharedHighwayActionUI
{

	public AddSharedGeneralHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddSharedGeneralHighwayAction.class.getName();
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(AddSharedGeneralHighwayActionUI.class,
						"AddSharedGeneralHighwayActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(AddSharedGeneralHighwayActionUI.class, "AddSharedGeneralHighwayActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedGeneralHighwayActionUI.class, "AddSharedGeneralHighwayActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedGeneralHighwayActionUI.class, "AddSharedGeneralHighwayActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_add_shared_active.gif"));
	}
}
