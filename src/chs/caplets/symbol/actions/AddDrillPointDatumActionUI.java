/*
 * Copyright 2013 Mentor Graphics Corporation
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

@ApplicationSpecification(includeIn = {Application.CapitalSymbolDesigner}) // formboard capability not in VeSys or Systems
public class AddDrillPointDatumActionUI extends ActionUI
{

	public AddDrillPointDatumActionUI(ICaplet caplet)
	{
		super(caplet);
		setupUI();
	}

	public void setupUI()
	{

		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");

		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(AddDrillPointDatumActionUI.class, "AddDrillPointDatumActionUI.mnemonic");

		putValue(NAME, ResourceMgr.getString(AddDrillPointDatumActionUI.class,
				"AddDrillPointDatumActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddDrillPointDatumActionUI.class,
				"AddDrillPointDatumActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddDrillPointDatumActionUI.class,
				"AddDrillPointDatumActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return AddDrillPointDatumAction.class.getName();
	}
}

