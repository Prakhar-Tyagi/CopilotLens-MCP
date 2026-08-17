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
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class FlattenBlockActionUI extends ActionUI
{

	public FlattenBlockActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(
				ResourceMgr.getMnemonic(FlattenBlockActionUI.class, "FlattenBlockActionUI.mnemonic"));

		putValue(NAME, ResourceMgr.getString(FlattenBlockActionUI.class, "FlattenBlockActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(FlattenBlockActionUI.class, "FlattenBlockActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FlattenBlockActionUI.class, "FlattenBlockActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return FlattenBlockAction.class.getName();
	}
}
