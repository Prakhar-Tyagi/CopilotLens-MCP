/*
 * Copyright 2013 Mentor Graphics Corporation
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

@ApplicationSpecification(includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
		Application.CapitalEssentialsSymbolDesigner, Application.XSCSymbol, Application.SEElectricalSymbol})
public class EditUserDefinedZonesActionUI extends ActionUI
{

	public EditUserDefinedZonesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getString(EditUserDefinedZonesActionUI.class,
				"EditUserDefinedZonesActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(EditUserDefinedZonesActionUI.class,
				"EditUserDefinedZonesActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(EditUserDefinedZonesActionUI.class,
				"EditUserDefinedZonesActionUI.longDesc.decl");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/javafx_ui/zone-small.png");
		Integer iMnemonic = (int) ResourceMgr.getMnemonic(EditUserDefinedZonesActionUI.class,
				"EditUserDefinedZonesActionUI.mnemonic");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return EditUserDefinedZonesAction.class.getName();
	}
}
