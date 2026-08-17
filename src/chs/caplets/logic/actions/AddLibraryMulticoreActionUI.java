/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 2, 2004 Time: 9:21:29 AM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_LIBRARY_MULTICORE_ACTION",
		label = "Add Multicore Library",
		tooltip = "Add Multicore Library",
		icon = "ico_multicore_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddLibraryMulticoreActionUI extends ActionUI
{

	public AddLibraryMulticoreActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddLibraryMulticoreAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(AddLibraryMulticoreActionUI.class,
				"AddLibraryMulticoreActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddLibraryMulticoreActionUI.class,
				"AddLibraryMulticoreActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddLibraryMulticoreActionUI.class,
				"AddLibraryMulticoreActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_multicore_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddLibraryMulticoreActionUI.class,
						"AddLibraryMulticoreActionUI.putValue.action.mnemonic")));
	}
}
