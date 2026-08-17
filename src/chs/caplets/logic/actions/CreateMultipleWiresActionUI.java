/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_MULTIPLE_WIRES_ACTION",
		label = "Multi-connect by Wire",
		tooltip = "Multi-connect by Wire(Ctrl+Shift+W)",
		icon = "ico_wire_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateMultipleWiresActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateWireActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateMultipleWiresActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif");
		Integer iMnemonic = KeyEvent.VK_W;
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateMultipleWiresActionUI.class,
				"CreateMultipleWiresActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateMultipleWiresActionUI.class,
				"CreateMultipleWiresActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateMultipleWiresActionUI.class,
				"CreateMultipleWiresActionUI.longDesc.decl"));
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
		return CreateMultipleWiresAction.class.getName();
	}
}

