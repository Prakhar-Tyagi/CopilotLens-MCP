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

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class PurgeActionUI extends DeleteActionUI
{

	public PurgeActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Overridden here so that this variant of the action is activated on Shift+Delete
	 */
	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(PurgeActionUI.class, "PurgeActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(PurgeActionUI.class, "PurgeActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(PurgeActionUI.class, "PurgeActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.SHIFT_MASK));
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(PurgeActionUI.class, "PurgeActionUI.mnemonic"));
	}

	@Override @NotNull public String getActionClass()
	{
		return PurgeAction.class.getName();
	}
}
