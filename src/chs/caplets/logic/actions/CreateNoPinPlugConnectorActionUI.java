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
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author Matt Boyd
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_NO_PIN_PLUG_CONNECTOR_ACTION",
		label = "Add Plug",
		tooltip = "Add plug connector without pins",
		icon = "ico_plug_connector_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateNoPinPlugConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreatePlugConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreateNoPinPlugConnectorActionUI(ICaplet caplet)
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
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.PLUG_CONNECTOR_ICON_ENABLED);

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(CreateNoPinPlugConnectorActionUI.class,
				"CreateNoPinPlugConnectorActionUI.mnemonic"));
		putValue(NAME, ResourceMgr.getString(CreateNoPinPlugConnectorActionUI.class,
				"CreateNoPinPlugConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateNoPinPlugConnectorActionUI.class,
				"CreateNoPinPlugConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateNoPinPlugConnectorActionUI.class,
				"CreateNoPinPlugConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.PLUG_CONNECTOR_ICON_DISABLED);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateNoPinPlugConnectorAction.class.getName();
	}
}
