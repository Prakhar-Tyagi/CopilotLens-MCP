/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Fluid-specific UI for creating devices without ports.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign})
@ImmersedAction(actionId = "CAPITAL_FLUID_CREATE_NO_PIN_DEVICE_ACTION")
public class FluidCreateNoPinDeviceActionUI extends CreateNoPinDeviceActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidCreateNoPinDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(FluidCreateNoPinDeviceActionUI.class,
				"FluidCreateNoPinDeviceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(FluidCreateNoPinDeviceActionUI.class,
						"FluidCreateNoPinDeviceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FluidCreateNoPinDeviceActionUI.class,
						"FluidCreateNoPinDeviceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/fluid-device-small.png"));
	}
}
