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
public class ViewFailedComponentsActionUI extends ActionUI
{

	public ViewFailedComponentsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name =
				ResourceMgr.getString(ViewFailedComponentsActionUI.class, "ViewFailedComponentsActionUI.String.name");
		String shortDesc =
				ResourceMgr.getString(ViewFailedComponentsActionUI.class, "ViewFailedComponentsActionUI.String.name");
		String longDesc =
				ResourceMgr.getString(ViewFailedComponentsActionUI.class, "ViewFailedComponentsActionUI.String.name");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_F);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/as_failure_indicator.gif");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(SMALL_ICON, icon);
	}

	//public void updateUI() {
	//}

	public String getActionClass()
	{
		return ViewFailedComponentsAction.class.getName();
	}
}
