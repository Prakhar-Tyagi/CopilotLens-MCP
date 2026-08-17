/*
* Copyright 2017 Mentor Graphics Corporation
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
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * @author pbhawsar on 17-02-2017
 */

@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture})
public class BuildICDFromJsonActionUI extends ActionUI
{

	public BuildICDFromJsonActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		String name = "Build ICD(s) from JSON";
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif");

		putValue(NAME, name);
		String shortDesc = "Build ICD(s) from JSON";
		putValue(SHORT_DESCRIPTION, shortDesc);
		String longDesc = "Build ICD(s) from JSON";
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return BuildICDFromJsonAction.class.getName();
	}
}
