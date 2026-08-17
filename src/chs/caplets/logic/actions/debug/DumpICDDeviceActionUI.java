/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.debug;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;

import javax.swing.Icon;

/**
 * @author chandras on 06-02-2017.
 */
@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture})
public class DumpICDDeviceActionUI extends ActionUI
{

	public DumpICDDeviceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = "Dump Selected ICD Device(s)";
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif");

		putValue(NAME, name);
		String shortDesc = "Dump Selected ICD Device(s)";
		putValue(SHORT_DESCRIPTION, shortDesc);
		String longDesc = "Examine selected ICD Device(s) for debugging purposes";
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
	}

	public String getActionClass()
	{
		return DumpICDDeviceAction.class.getName();
	}
}
