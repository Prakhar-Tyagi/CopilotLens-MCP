/*
 * Copyright 2004-2008 Mentor Graphics Corporation
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

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 4, 2004 Time: 4:33:08 PM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class AddLibraryInnercoreShieldActionUI extends ActionUI
{

	public AddLibraryInnercoreShieldActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddLibraryInnercoreShieldAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddLibraryInnercoreShieldActionUI.class,
				"AddLibraryInnercoreShieldActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreShieldActionUI.class,
				"AddLibraryInnercoreShieldActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreShieldActionUI.class,
				"AddLibraryInnercoreShieldActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shield_termination_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_S));
	}
}
