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
 * Fluid-specific UI for moving ports..
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_FLUID_MOVE_PIN_ACTION")
public class FluidMovePinActionUI extends MovePinActionUI
{

	/**
	 * Constructor for the FluidMovePinActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidMovePinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(FluidMovePinActionUI.class, "FluidMovePinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(FluidMovePinActionUI.class, "FluidMovePinActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FluidMovePinActionUI.class, "FluidMovePinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}
}
