/*
 * Copyright 2005-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ActionUI;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class UpdateInstanceActionUI extends ActionUI
{

	public UpdateInstanceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * @see IActionUI#setupUI()
	 */
	public void setupUI()
	{
		setResources("name.decl", false, "name.decl", "longDesc.decl", "mnemonic", null,
				"chs/images/app/ico_update_instance_active.gif");
	}

	/**
	 * @see IActionUI#getActionClass()
	 */
	public String getActionClass()
	{
		return UpdateInstanceAction.class.getName();
	}
}


