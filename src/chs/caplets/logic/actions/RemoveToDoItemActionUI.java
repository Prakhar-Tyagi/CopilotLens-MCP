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

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class RemoveToDoItemActionUI extends ActionUI
{

	public RemoveToDoItemActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return RemoveToDoItemAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(RemoveToDoItemActionUI.class, "RemoveToDoItemActionUI.action.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(RemoveToDoItemActionUI.class, "RemoveToDoItemActionUI.action.shortDesc"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(RemoveToDoItemActionUI.class, "RemoveToDoItemActionUI.action.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(RemoveToDoItemActionUI.class, "RemoveToDoItemActionUI.action.mnemonic")));
	}
}
