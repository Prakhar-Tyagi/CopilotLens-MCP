/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.KeyStroke;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.SEElectricalSymbol, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.CapitalSymbolForCapture})

public class ViewRelatedSymbolActionUI extends ActionUI
{

	public ViewRelatedSymbolActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return ViewRelatedSymbolAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ViewRelatedSymbolActionUI.class, "ViewRelatedSymbolActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ViewRelatedSymbolActionUI.class, "ViewRelatedSymbolActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ViewRelatedSymbolActionUI.class, "ViewRelatedSymbolActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr
				.getMnemonic(ViewRelatedSymbolActionUI.class, "ViewRelatedSymbolActionUI.mnemonic")));
		char accelerator =
				ResourceMgr.getChar(ViewRelatedSymbolActionUI.class, "ViewRelatedSymbolActionUI.accelerator");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator, 0));
	}
}
