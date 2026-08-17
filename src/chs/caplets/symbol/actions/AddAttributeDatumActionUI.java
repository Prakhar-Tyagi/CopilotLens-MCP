/*
 * Copyright 2008 Mentor Graphics Corporation
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
 * Created by IntelliJ IDEA. User: amittal Date: Apr 3, 2008 Time: 11:10:06 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class AddAttributeDatumActionUI extends ActionUI
{

	public AddAttributeDatumActionUI(ICaplet caplet)
	{
		super(caplet);
		setupUI();
	}

	public void setupUI()
	{

		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");

		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(AddAttributeDatumActionUI.class, "AddAttributeDatumActionUI.mnemonic");

		putValue(NAME, ResourceMgr.getString(AddAttributeDatumActionUI.class,
				"AddAttributeDatumActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddAttributeDatumActionUI.class,
				"AddAttributeDatumActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddAttributeDatumActionUI.class,
				"AddAttributeDatumActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		//getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public String getActionClass()
	{
		return AddAttributeDatumAction.class
				.getName();  //To change body of implemented methods use File | Settings | File Templates.
	}
}


