/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.utilities.ResourceMgr;
import chs.images.CHSImageLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.Event;

/**
 * Action UI class for UnplaceAction
 */
public class UnplaceActionUI extends DeleteActionUI
{

	public UnplaceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Overridden here so that this variant of the action is activated on Ctrl+Delete
	 */
	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(UnplaceActionUI.class, "UnplaceActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(UnplaceActionUI.class, "UnplaceActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(UnplaceActionUI.class, "UnplaceActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.CTRL_MASK));
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(UnplaceActionUI.class, "UnplaceActionUI.mnemonic"));
	}

	@Override @NotNull public String getActionClass()
	{
		return UnplaceAction.class.getName();
	}
}
