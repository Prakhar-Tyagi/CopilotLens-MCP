/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.CapitalArchitect, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_OVERBRAID_SHEATH_ACTION",
		label = "Quick Add Overbraid",
		tooltip = "Quick Add Overbraid(Shift+O)",
		icon = "ico_quick_add_overbraid_small",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateOverbraidSheathActionUI extends ActionUI
{

	public CreateOverbraidSheathActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK));
		putValue(SHORT_DESCRIPTION, ResourceMgr
				.getString(CreateMulticoreWithAccelAction.class, "CreateMulticoreWithAccelAction.Overbraid.Title"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(CreateMulticoreWithAccelAction.class, "CreateMulticoreWithAccelAction.Overbraid.Description"));
		putValue(NAME, ResourceMgr
				.getString(CreateMulticoreWithAccelAction.class, "CreateMulticoreWithAccelAction.Overbraid.Title"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/quick-add-overbraid-small.png"));
	}

	@Override public String getActionClass()
	{
		return CreateOverbraidwithAccelAction.class.getName();
	}
}
