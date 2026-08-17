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
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.CapitalArchitect, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_COAXIAL_SHIELD_MULTICORE_ACTION",
		label = "Quick Add Shielded Multicore",
		tooltip = "Quick Add Shielded Multicore(Shift+H)",
		icon = "ico_quick_add_shield_multicore_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateCoaxialShieldMulticoreActionUI extends ActionUI
{

	public CreateCoaxialShieldMulticoreActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
				"CreateMulticoreWithAccelAction.Shielded.Title"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
				"CreateMulticoreWithAccelAction.Shielded.Description"));
		putValue(NAME,  ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
				"CreateMulticoreWithAccelAction.Shielded.Title"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/quick-add-shield-small.png"));
	}

	@Override public String getActionClass()
	{
		return CreateCoaxialShieldMulticoreAction.class.getName();
	}
}
