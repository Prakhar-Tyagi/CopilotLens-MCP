/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 5, 2004 Time: 12:42:08 PM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect, Application.CapitalCapture}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_SHARED_CONDUCTOR_GROUP_ACTION",
		label = "Add Shared Multicore",
		tooltip = "Create Shared Multicore",
		icon = "ico_shared_multicore_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateSharedConductorGroupActionUI extends ActionUI
{

	public CreateSharedConductorGroupActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_multicore_active.gif");
		Integer iMnemonic = new Integer(ResourceMgr.getMnemonic(CreateSharedConductorGroupActionUI.class,
				"CreateSharedConductorGroupActionUI.mnemonic"));

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(CreateSharedConductorGroupActionUI.class,
				"CreateSharedConductorGroupActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateSharedConductorGroupActionUI.class,
				"CreateSharedConductorGroupActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateSharedConductorGroupActionUI.class,
				"CreateSharedConductorGroupActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_multicore_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateSharedConductorGroupAction.class.getName();
	}
}
