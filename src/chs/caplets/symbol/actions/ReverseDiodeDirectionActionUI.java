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
public class ReverseDiodeDirectionActionUI extends ActionUI
{

	public ReverseDiodeDirectionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(NAME,
				ResourceMgr.getString(ReverseDiodeDirectionActionUI.class, "ReverseDiodeDirectionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ReverseDiodeDirectionActionUI.class,
						"ReverseDiodeDirectionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ReverseDiodeDirectionActionUI.class,
						"ReverseDiodeDirectionActionUI.longDesc.decl"));
	}

	public String getActionClass()
	{
		return ReverseDiodeDirectionAction.class.getName();
	}
}
