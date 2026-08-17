/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * *  Functionality to swap a shared device
 *
 * Created: 12/23/2005
 * Author: andyw
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.SvcDoc},immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_REPLACE_SHARED_COMPOSITE_SYMBOL_ACTION",
		label = "Replace Shared Composite",
		tooltip = "Replace Shared Composite...",
		icon = "replace_shared_composite_symbol",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ReplaceSharedCompositeSymbolActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public ReplaceSharedCompositeSymbolActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolActionUI.mnemonic"));
		putValue(NAME, ResourceMgr.getStringForMenu(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(ReplaceSharedCompositeSymbolActionUI.class,
				"ReplaceSharedCompositeSymbolActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return ReplaceSharedCompositeSymbolAction.class.getName();
	}
}
