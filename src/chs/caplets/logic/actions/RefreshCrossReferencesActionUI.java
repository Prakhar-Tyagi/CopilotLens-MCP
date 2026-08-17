/*
 * Copyright 2003-2008 Mentor Graphics Corporation
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
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Oct 20, 2003 Time: 5:05:27 PM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class RefreshCrossReferencesActionUI extends ActionUI
{

	public RefreshCrossReferencesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_R);

		putValue(NAME, ResourceMgr.getString(RefreshCrossReferencesActionUI.class,
				"RefreshCrossReferencesActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(RefreshCrossReferencesActionUI.class,
				"RefreshCrossReferencesActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(RefreshCrossReferencesActionUI.class,
				"RefreshCrossReferencesActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return RefreshCrossReferencesAction.class.getName();
	}
}
