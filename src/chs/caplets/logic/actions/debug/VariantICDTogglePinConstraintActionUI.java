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
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * @author pbhawsar on 17-02-2017
 */

@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner})
public class VariantICDTogglePinConstraintActionUI extends ActionUI
{

	private static final String enabledName = "Enable Pin constraint for variant ICDs";
	private static final String disabledName = "Disable Pin constraint for variant ICDs";

	public VariantICDTogglePinConstraintActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, "Toggle Pin constraint for variant ICDs");
		String shortDesc = enabledName;
		putValue(SHORT_DESCRIPTION, shortDesc);
		String longDesc = enabledName;
		putValue(LONG_DESCRIPTION, longDesc);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_debug.gif");
		putValue(SMALL_ICON, icon);
	}

	@Override public String getActionClass()
	{
		return VariantICDTogglePinConstraintAction.class.getName();
	}

	@Override public void updateUI()
	{
		if (ICDUtils.shouldVariantICDPinMatch()) {
			putValue(SHORT_DESCRIPTION, disabledName);
			putValue(LONG_DESCRIPTION, disabledName);
		}
		else {
			putValue(SHORT_DESCRIPTION, enabledName);
			putValue(LONG_DESCRIPTION, enabledName);
		}
		super.updateUI();
	}
}
