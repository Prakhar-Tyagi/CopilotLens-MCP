/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.shared.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import java.awt.event.KeyEvent;

/**
 * User: pwijaya Date: Oct 22, 2003 Time: 11:30:38 AM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
			Application.XSCSymbol, Application.SEElectricalSymbol})
public class ModifyZoneAreaActionUI extends ActionUI
{

	private static final String TITLE =
			ResourceMgr.getString(ModifyZoneAreaActionUI.class, "ModifyZoneAreaActionUI.Title");
	private static final String SHORT_DESC =
			ResourceMgr.getString(ModifyZoneAreaActionUI.class, "ModifyZoneAreaActionUI.ShortDesc");
	private static final String DESCRIPTION =
			ResourceMgr.getString(ModifyZoneAreaActionUI.class, "ModifyZoneAreaActionUI.Desc");

	public ModifyZoneAreaActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME, TITLE);
		putValue(SHORT_DESCRIPTION, SHORT_DESC);
		putValue(LONG_DESCRIPTION, DESCRIPTION);
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_Z));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return ModifyZoneAreaAction.class.getName();
	}
}
