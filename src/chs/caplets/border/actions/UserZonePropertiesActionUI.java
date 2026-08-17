/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		allowInDevExtensionsFor = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
				Application.CapitalEssentialsSymbolDesigner, Application.XSCSymbol, Application.SEElectricalSymbol},
		allowInQAExtensionsFor = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
				Application.CapitalEssentialsSymbolDesigner, Application.XSCSymbol, Application.SEElectricalSymbol})
public class UserZonePropertiesActionUI extends ActionUI
{

	public UserZonePropertiesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getStringForMenu(UserZonePropertiesActionUI.class,
				"UserZonePropertiesActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(UserZonePropertiesActionUI.class,
				"UserZonePropertiesActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(UserZonePropertiesActionUI.class,
				"UserZonePropertiesActionUI.longDesc.decl");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/javafx_ui/edit_user_zone_properties-small.png");
		Integer iMnemonic = (int) ResourceMgr.getMnemonic(UserZonePropertiesActionUI.class,
				"UserZonePropertiesActionUI.mnemonic");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return UserZonePropertiesAction.class.getName();
	}
}
