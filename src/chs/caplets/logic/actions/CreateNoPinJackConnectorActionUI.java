/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author Matt Boyd
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_NO_PIN_JACK_CONNECTOR_ACTION",
		label = "Add Jack",
		tooltip = "Create a new jack...",
		icon = "ico_connector_jack_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateNoPinJackConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreatePlugConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreateNoPinJackConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		// NOTE: we now use the With pins icons for the no pins actions because only the no pins variants are shown in the UI
		// this may change if we revert to having both variants available
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_connector_jack_active.gif");

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(CreateNoPinJackConnectorActionUI.class,
				"CreateNoPinJackConnectorActionUI.mnemonic"));
		putValue(NAME, ResourceMgr.getString(CreateNoPinJackConnectorActionUI.class,
				"CreateNoPinJackConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateNoPinJackConnectorActionUI.class,
				"CreateNoPinJackConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateNoPinJackConnectorActionUI.class,
				"CreateNoPinJackConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_connector_jack_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateNoPinJackConnectorAction.class.getName();
	}
}
