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
 * Fluid-specific UI for connecting by port.
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.CapitalCapture, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_FLUID_CONNECT_BY_PIN_ACTION")
public class FluidConnectByPinActionUI extends ConnectByPinActionUI
{

	/**
	 * Constructor for the FluidConnectByPinActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidConnectByPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(FluidConnectByPinActionUI.class, "FluidConnectByPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(FluidConnectByPinActionUI.class, "FluidConnectByPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FluidConnectByPinActionUI.class, "FluidConnectByPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}
}
