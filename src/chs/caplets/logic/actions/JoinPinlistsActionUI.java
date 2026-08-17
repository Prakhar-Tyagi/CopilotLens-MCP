/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
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
import javax.swing.KeyStroke;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.SvcDoc,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_JOIN_PINLISTS_ACTION",
		label = "Join",
		tooltip = "Join two instances of the same device or connector(J)",
		icon = "ico_transparent",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class JoinPinlistsActionUI extends ActionUI
{

	public JoinPinlistsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_J);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, 0);

		putValue(NAME, ResourceMgr.getString(JoinPinlistsActionUI.class, "JoinPinlistsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(JoinPinlistsActionUI.class, "JoinPinlistsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(JoinPinlistsActionUI.class, "JoinPinlistsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return JoinPinlistsAction.class.getName();
	}
}
