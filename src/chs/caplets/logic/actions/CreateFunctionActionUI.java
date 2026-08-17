/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class CreateFunctionActionUI extends ActionUI
{

	public CreateFunctionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = IconUtils.getFunctionCompActiveIcon();
		Integer iMnemonic = new Integer(KeyEvent.VK_F);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateFunctionActionUI.class, "CreateFunctionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionActionUI.class, "CreateFunctionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateFunctionActionUI.class, "CreateFunctionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return IconUtils.getFunctionCompInActiveIcon();
	}

	public String getActionClass()
	{
		return CreateFunctionAction.class.getName();
	}
}

