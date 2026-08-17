/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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

@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class DumpSelectedSharedPinMatingActionUI extends ActionUI
{

	public DumpSelectedSharedPinMatingActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = "Dump Shared Matings";
		String shortDesc = "Dump Selected Shared pin list(s) matings";
		String longDesc = "Dump shared pins mated to selected shared pin list(s) pins";
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
	}

	public String getActionClass()
	{
		return DumpSelectedSharedPinMatingAction.class.getName();
	}
}
