/*
 * Copyright 2004-2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

// caf imports

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * * Called when the user attempts to cut some object(s) * * The method is undoable, becuase it makes changes to the
 * caplet model
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalArchitect, Application.CapitalEssentialsDesign, Application.ArtisanArchitect,
				Application.SEElectricalDesign})
public class SpiceSimulationModeActionUI extends ActionUI
{

	public SpiceSimulationModeActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name =
				ResourceMgr.getString(SpiceSimulationModeActionUI.class, "SpiceSimulationModeActionUI.String.name");
		String shortDesc = ResourceMgr
				.getString(SpiceSimulationModeActionUI.class, "SpiceSimulationModeActionUI.String.shortDesc");
		String longDesc =
				ResourceMgr.getString(SpiceSimulationModeActionUI.class, "SpiceSimulationModeActionUI.String.longDesc");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/dc.png");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
	}

	public void updateUI()
	{
		//System.err.println(" Update ui called " ) ;
		firePropertyChange("SimulationControlsPanel.selected", null, Boolean.valueOf(getActionIsSet()));
		super.updateUI();
	}

	public String getActionClass()
	{
		return SpiceSimulationModeAction.class.getName();
	}

	protected boolean getActionIsSet()
	{
		if (CAFUtils.getInstance().getActiveCapletController() != null) {
			if (CAFUtils.getInstance().getActiveCapletController().getAction(getActionClass()) != null) {

				SpiceSimulationModeAction action =
						(SpiceSimulationModeAction) CAFUtils.getInstance().getActiveCapletController()
								.getAction(getActionClass());
				return action.isSetSimulationMode();
			}
		}
		return false;
	}
}
