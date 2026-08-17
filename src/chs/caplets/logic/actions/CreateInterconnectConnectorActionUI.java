/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author Matt Boyd
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class CreateInterconnectConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreateInterconnectConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreateInterconnectConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_connector_interconnect_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_C);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateInterconnectConnectorActionUI.class,
				"CreateInterconnectConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateInterconnectConnectorActionUI.class,
				"CreateInterconnectConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateInterconnectConnectorActionUI.class,
				"CreateInterconnectConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_connector_interconnect_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateInterconnectConnectorAction.class.getName();
	}
}
