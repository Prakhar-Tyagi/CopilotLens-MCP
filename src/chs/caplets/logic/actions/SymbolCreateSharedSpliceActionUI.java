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
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: hebae Date: Sep 11, 2005 Time: 11:00:18 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class SymbolCreateSharedSpliceActionUI extends SymbolCreateSharedActionUI
{

	public SymbolCreateSharedSpliceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_S);

		putValue(NAME, ResourceMgr.getString(SymbolCreateSharedSpliceActionUI.class,
				"SymbolCreateSharedSpliceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(SymbolCreateSharedSpliceActionUI.class,
				"SymbolCreateSharedSpliceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(SymbolCreateSharedSpliceActionUI.class,
				"SymbolCreateSharedSpliceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return SymbolCreateSharedSpliceAction.class.getName();
	}
}
