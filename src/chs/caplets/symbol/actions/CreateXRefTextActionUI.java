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

/**
 * ActionUI Skeleton Class
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class CreateXRefTextActionUI extends ActionUI
{

	public CreateXRefTextActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Create all UI elements for the action
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_xreftext_active.gif");

		putValue(NAME, ResourceMgr.getString(CreateXRefTextActionUI.class, "CreateXRefTextActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateXRefTextActionUI.class, "CreateXRefTextActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateXRefTextActionUI.class, "CreateXRefTextActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(CreateXRefTextActionUI.class, "CreateXRefTextActionUI.mnemonic"));
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_xreftext_inactive.gif");
	}

	/**
	 * Return our matching ActionRT class
	 */
	public String getActionClass()
	{
		return CreateXRefTextAction.class.getName();
	}
}
