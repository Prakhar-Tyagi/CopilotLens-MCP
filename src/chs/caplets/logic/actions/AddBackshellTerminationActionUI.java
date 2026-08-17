/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2026 Siemens
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

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_BACKSHELL_TERMINATION_ACTION",
		label = "Add Backshell Termination",
		tooltip = "Add Backshell Termination(Ctrl+ B)",
		icon = "ico_backshell_term_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddBackshellTerminationActionUI extends ActionUI
{

	public static final String ACTIVE_ICON = "chs/images/app/ico_backshell_term_active.gif";
	public static final String INACTIVE_ICON = "chs/images/app/ico_backshell_term_inactive.gif";

	public AddBackshellTerminationActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		final Class<AddBackshellTerminationActionUI> actionUIClass = AddBackshellTerminationActionUI.class;

		putValue(NAME, ResourceMgr.getString(actionUIClass, "AddBackshellTerminationActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(actionUIClass, "AddBackshellTerminationActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(actionUIClass, "AddBackshellTerminationActionUI.longDesc.decl"));

		Integer iMnemonic = (int) ResourceMgr.getMnemonic(actionUIClass, "AddBackshellTerminationActionUI.mnemonic");
		KeyStroke accel = KeyStroke.getKeyStroke(iMnemonic, Event.CTRL_MASK); // CTRL + B

		putValue(SMALL_ICON, getActiveIcon());
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	private Icon getActiveIcon()
	{
		return CHSImageLoader.loadImageIcon(ACTIVE_ICON);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(INACTIVE_ICON);
	}

	@Override public String getActionClass()
	{
		return AddBackshellTerminationAction.class.getName();
	}
}
