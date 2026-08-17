/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
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

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_RIBBON_CREATE_WIRE_ACTION",
		label = "Add Wire",
		tooltip = "Add Wire(W)",
		icon = "ico_wire_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateWireActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateWireActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateWireActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_W);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, 0);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateWireActionUI.class, "CreateWireActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateWireActionUI.class, "CreateWireActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateWireActionUI.class, "CreateWireActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateWireAction.class.getName();
	}
}

