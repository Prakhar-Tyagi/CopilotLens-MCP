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
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class SymbolCreateSharedActionUI extends ActionUI
{

	public SymbolCreateSharedActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_S);

		putValue(NAME, ResourceMgr.getString(SymbolCreateSharedActionUI.class, "SymbolCreateSharedActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(SymbolCreateSharedActionUI.class, "SymbolCreateSharedActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(SymbolCreateSharedActionUI.class, "SymbolCreateSharedActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return SymbolCreateSharedAction.class.getName();
	}
}
