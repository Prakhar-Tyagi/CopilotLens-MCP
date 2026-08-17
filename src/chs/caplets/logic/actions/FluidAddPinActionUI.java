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
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Fluid-specific UI for adding ports.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture,
		Application.CapitalArchitect, Application.CapitalEssentialsDesign, Application.ArtisanFunction,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_FLUID_ADD_PIN_ACTION")
public class FluidAddPinActionUI extends AddPinActionUI
{

	/**
	 * Constructor for the FluidAddPinActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public FluidAddPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddPinActionUI.class, "FluidAddPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AddPinActionUI.class, "FluidAddPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddPinActionUI.class, "FluidAddPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, getActiveIcon());
	}

	@Nullable @Override
	protected Icon getActiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/fluid-port-small.png");
	}
}
