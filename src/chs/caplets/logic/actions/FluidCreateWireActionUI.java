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

import javax.swing.Icon;

/**
 * Fluid-specific UI for creating lines.
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_FLUID_CREATE_WIRE_ACTION")
public class FluidCreateWireActionUI extends CreateWireActionUI
{

	/**
	 * Constructor for the FluidCreateWireActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidCreateWireActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/fluid-connection-small.png");

		putValue(NAME, ResourceMgr.getString(CreateWireActionUI.class, "FluidCreateWireActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateWireActionUI.class, "FluidCreateWireActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateWireActionUI.class, "FluidCreateWireActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}
}
