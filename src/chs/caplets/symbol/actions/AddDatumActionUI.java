/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.XSCSymbol,
				Application.CapitalEssentialsSymbolDesigner, Application.SEElectricalSymbol})
public class AddDatumActionUI extends ActionUI implements ISelectListener
{

	public AddDatumActionUI(ICaplet caplet)
	{
		super(caplet);
		setupUI();
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{

		//Integer iMnemonic = (int) ResourceMgr
		//		.getMnemonic(AddDatumActionUI.class, "AddDatumActionUI.mnemonic." + getActionUIInstanceName());

		putValue(NAME, ResourceMgr.getStringForMenu(AddDatumActionUI.class,
				"AddDatumActionUI.name.decl.REDDatum"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddDatumActionUI.class,
				"AddDatumActionUI.shortDesc.decl.REDDatum"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddDatumActionUI.class,
				"AddDatumActionUI.longDesc.decl.REDDatum"));
		//putValue(MNEMONIC_KEY, iMnemonic);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
		putValue(SMALL_ICON, icon);
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddDatumAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
