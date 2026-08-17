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
public class ResetActionUI extends ActionUI
{

	protected static final Icon activeIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/as_simulation_reset_active.png");

	public ResetActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getString(ResetActionUI.class, "ResetActionUI.String.name");
		String shortDesc = ResourceMgr.getString(ResetActionUI.class, "ResetActionUI.String.shortDesc");
		String longDesc = ResourceMgr.getString(ResetActionUI.class, "ResetActionUI.String.longDesc");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_R);

		//putValue(NAME, name);

		putValue(SMALL_ICON, activeIcon);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return ResetAction.class.getName();
	}
}
