/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Instantiate cable from home tab
 *
 * @created June 14, 2023
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_SINGLE_LINE_ACTION",
		label = "Single Line",
		tooltip = "Add Single Line(E)",
		icon = "single_line_icon",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateSingleLineActionUI extends CreateHighwayActionUI
{

	public CreateSingleLineActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.SINGLE_LINE_ICON);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0);

		putValue(NAME, ResourceMgr.getString(CreateConductorActionUI.class, "CreateSingleLineActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateSingleLineActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateConductorActionUI.class, "CreateSingleLineActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
	}

	public String getActionClass()
	{
		return CreateSingleLineAction.class.getName();
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.SINGLE_LINE_DISABLED_ICON);
	}
}
