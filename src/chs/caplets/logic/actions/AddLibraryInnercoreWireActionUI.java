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
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 3, 2004 Time: 2:58:28 PM To change this template use Options |
 * File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class AddLibraryInnercoreWireActionUI extends ActionUI
{

	public AddLibraryInnercoreWireActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddLibraryInnercoreWireAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddLibraryInnercoreWireActionUI.class,
				"AddLibraryInnercoreWireActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreWireActionUI.class,
				"AddLibraryInnercoreWireActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddLibraryInnercoreWireActionUI.class,
				"AddLibraryInnercoreWireActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_wire_active.gif"));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_W));
	}
}
