/*
 * Copyright 2010 Mentor Graphics Corporation
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
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})

public class ConvertToResistanceLinkActionUI extends ActionUI
{

	public ConvertToResistanceLinkActionUI(ICaplet caplet)
	{
		super(caplet);
		setupUI();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ConvertToResistanceLinkActionUI.class,
				"ConvertToResistanceLinkActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ConvertToResistanceLinkActionUI.class,
						"ConvertToResistanceLinkActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ConvertToResistanceLinkActionUI.class,
						"ConvertToResistanceLinkActionUI.longDesc.decl"));
	}

	public String getActionClass()
	{
		return ConvertToResistanceLinkAction.class.getName();
	}
}
