/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(includeIn = {Application.CapitalCapture, Application.CapitalArchitect})
public class AssignDDTTypesActionUI extends ActionUI
{

	public AssignDDTTypesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AssignDDTTypesAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(AssignDDTTypesActionUI.class,
				"AssignDDTTypesActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, getValue(NAME));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AssignDDTTypesActionUI.class, "AssignDDTTypesActionUI.putValue.action.text_1"));

		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(AssignDDTTypesActionUI.class, "AssignDDTTypesActionUI.mnemonic"));
	}
}
 
