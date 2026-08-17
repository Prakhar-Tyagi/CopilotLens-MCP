/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2026 Siemens
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

/*
 *  Description of the Class
 *
 *@author     Darin Jackson
 *@created    August 1, 2001
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SHARED_JACK_CONNECTOR_ACTION",
		label = "Add Shared Jack",
		tooltip = "Add Shared Jack...",
		icon = "ico_shared_connector_jack_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSharedJackConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddSharedJackConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_connector_jack_active.gif");
		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(AddSharedJackConnectorActionUI.class, "AddSharedJackConnectorActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(AddSharedJackConnectorActionUI.class,
				"AddSharedJackConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddSharedJackConnectorActionUI.class,
				"AddSharedJackConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSharedJackConnectorActionUI.class,
				"AddSharedJackConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddSharedJackConnectorAction.class.getName();
	}
}
