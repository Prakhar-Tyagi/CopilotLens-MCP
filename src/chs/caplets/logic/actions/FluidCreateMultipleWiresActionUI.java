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
 * Fluid-specific UI for creating multiple Lines.
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_FLUID_CREATE_MULTIPLE_WIRES_ACTION")
public class FluidCreateMultipleWiresActionUI extends CreateMultipleWiresActionUI
{

	/**
	 * Constructor for the FluidCreateMultipleWiresActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidCreateMultipleWiresActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(FluidCreateMultipleWiresActionUI.class,
				"FluidCreateMultipleWiresActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(FluidCreateMultipleWiresActionUI.class,
				"FluidCreateMultipleWiresActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(FluidCreateMultipleWiresActionUI.class,
				"FluidCreateMultipleWiresActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/fluid-connection-small.png"));
	}
}
