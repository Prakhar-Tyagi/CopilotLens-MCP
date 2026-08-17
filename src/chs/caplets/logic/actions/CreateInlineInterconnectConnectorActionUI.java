/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Makes CreateInlineInterconnectConnectorAction available to toolbars and menus.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class CreateInlineInterconnectConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreatePlugConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreateInlineInterconnectConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_inline_interconnect_active.gif");
		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(CreateInlineConnectorActionUI.class, "CreateInlineInterconnectConnectorActionUI.mnemonic");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateInlineConnectorActionUI.class,
				"CreateInlineInterconnectConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(CreateInlineConnectorActionUI.class,
				"CreateInlineInterconnectConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateInlineConnectorActionUI.class,
				"CreateInlineInterconnectConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_inline_inactive.gif");
	}

	public String getActionClass()
	{
		return CreateInlineInterconnectConnectorAction.class.getName();
	}
}
