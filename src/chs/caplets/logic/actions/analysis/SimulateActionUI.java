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
public class SimulateActionUI extends ActionUI
{

	protected final String name =
			ResourceMgr.getString(SimulateActionUI.class, "SimulateActionUI.String.name");
	;

	protected static final Icon activeIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/as_simulation_simulate_active.png");
	protected static final Icon inactiveIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/as_simulation_simulate_inactive.png");

	protected boolean indicateModified;

	public SimulateActionUI(ICaplet caplet)
	{
		super(caplet);
		indicateModified = false;
	}

	public void setupUI()
	{

		String shortDesc = name;
		String longDesc = ResourceMgr.getString(SimulateActionUI.class, "SimulateActionUI.String.longDesc");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_S);

		//putValue(NAME, name);
		putValue(SMALL_ICON, activeIcon);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public void updateUI()
	{
		updateNameValue();
		super.updateUI();
	}

	/**
	 * This method updates the name of the action, suffixing with an asterisk if it has been modified since the last
	 * simulation
	 */
	protected void updateNameValue()
	{
		if (indicateModified) {
			//putValue(NAME, name + " *");
			putValue(SMALL_ICON, activeIcon);
		}
		else {
			//putValue(NAME, name);
			putValue(SMALL_ICON, inactiveIcon);
		}
	}

	/**
	 * This method sets whether the UI should reflect the fact that the simulation properties have been modified
	 *
	 * @param modified, true if they have been modified.
	 */
	public void setIndicateModified(boolean modified)
	{
		indicateModified = modified;
		CAFUtils.getInstance().tickleUI(getCaplet().getFIB());
	}

	public String getActionClass()
	{
		return SimulateAction.class.getName();
	}
}
