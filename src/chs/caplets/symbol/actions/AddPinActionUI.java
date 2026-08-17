/*
 * Copyright 2002-2008 Mentor Graphics Corporation
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
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 * Description of the Class
 *
 * @author gregc
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class AddPinActionUI extends ActionUI implements ISelectListener
{

	public AddPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_active.gif");
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_P, 0);

		putValue(NAME, ResourceMgr.getString(AddPinActionUI.class, "AddPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddPinActionUI.class, "AddPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddPinActionUI.class, "AddPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, KeyEvent.VK_I);
		putValue(ACCELERATOR_KEY, accel);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_inactive.gif");
	}

	public String getActionClass()
	{
		return AddPinAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
