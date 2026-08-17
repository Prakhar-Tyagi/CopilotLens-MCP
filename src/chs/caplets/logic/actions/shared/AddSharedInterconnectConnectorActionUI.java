/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/*
 *  Description of the Class
 *
 *@author     Darin Jackson
 *@created    August 1, 2001
 */

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddSharedInterconnectConnectorActionUI extends ActionUI
{

	public AddSharedInterconnectConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_add_shared_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_C);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(AddSharedInterconnectConnectorActionUI.class,
				"AddSharedInterconnectConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(AddSharedInterconnectConnectorActionUI.class,
				"AddSharedInterconnectConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddSharedInterconnectConnectorActionUI.class,
				"AddSharedInterconnectConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddSharedInterconnectConnectorAction.class.getName();
	}
}
