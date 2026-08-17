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
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_NO_PIN_INLINE_CONNECTOR_ACTION",
		label = "Add Inline",
		tooltip = "Add Inline",
		icon = "ico_inline_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateNoPinInlineConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreatePlugConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreateNoPinInlineConnectorActionUI(ICaplet caplet)
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
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_inline_active.gif");

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(CreateNoPinInlineConnectorActionUI.class,
				"CreateNoPinInlineConnectorActionUI.mnemonic"));
		putValue(NAME, ResourceMgr.getString(CreateNoPinInlineConnectorActionUI.class,
				"CreateNoPinInlineConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateNoPinInlineConnectorActionUI.class,
				"CreateNoPinInlineConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateNoPinInlineConnectorActionUI.class,
				"CreateNoPinInlineConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_inline_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateNoPinInlineConnectorAction.class.getName();
	}
}
