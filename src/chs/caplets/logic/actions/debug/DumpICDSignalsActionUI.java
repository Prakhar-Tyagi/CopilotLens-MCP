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
public class DumpICDSignalsActionUI extends ActionUI
{

	public DumpICDSignalsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = "Dump ICD Signals";
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif");

		putValue(NAME, name);
		String shortDesc = "Dump ICD Signals";
		putValue(SHORT_DESCRIPTION, shortDesc);
		String longDesc = "Examine ICD Signals for debugging purposes";
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
	}

	public String getActionClass()
	{
		return DumpICDSignalsAction.class.getName();
	}
}
