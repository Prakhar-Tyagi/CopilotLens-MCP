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

import javax.swing.Icon;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class AddBlockInstanceActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddBlockInstanceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_instantiate_active.gif");

		putValue(NAME, ResourceMgr.getString(AddBlockInstanceActionUI.class, "AddBlockInstanceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddBlockInstanceActionUI.class, "AddBlockInstanceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddBlockInstanceActionUI.class, "AddBlockInstanceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(AddBlockInstanceActionUI.class, "AddBlockInstanceActionUI.mnemonic"));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return chs.caplets.symbol.actions.AddBlockInstanceAction.class.getName();
	}
}

