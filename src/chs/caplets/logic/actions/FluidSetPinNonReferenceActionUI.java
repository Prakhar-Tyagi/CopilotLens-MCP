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
 * Fluid-specific UI for setting port non-references.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
		Application.CapitalArchitect, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_FLUID_SET_NON_PIN_REFERENCE_ACTION")
public class FluidSetPinNonReferenceActionUI extends SetPinNonReferenceActionUI
{

	/**
	 * Constructor for the FluidSetPinNonReferenceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidSetPinNonReferenceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getString(FluidSetPinNonReferenceActionUI.class, "FluidSetPinNonReferenceActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(FluidSetPinNonReferenceActionUI.class,
				"FluidSetPinNonReferenceActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(FluidSetPinNonReferenceActionUI.class,
				"FluidSetPinNonReferenceActionUI.longDesc.decl");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}
}
