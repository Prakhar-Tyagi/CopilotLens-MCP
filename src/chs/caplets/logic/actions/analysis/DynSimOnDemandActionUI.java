/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

// caf imports

import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * * This action starts the dynamic simulation process in * the backgound and adds tells the AnalysisServices instance *
 * to pay attention to all changes to the model.
 */
public class DynSimOnDemandActionUI extends AnalysisDrawerPersistentUI
{

	public DynSimOnDemandActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getString(DynSimOnDemandActionUI.class, "DynSimOnDemandActionUI.String.name");
		String shortDesc = ResourceMgr.getString(DynSimOnDemandActionUI.class, "DynSimOnDemandActionUI.String.name");
		String longDesc = ResourceMgr.getString(DynSimOnDemandActionUI.class, "DynSimOnDemandActionUI.String.name");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_R);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/as_simulation_ondemand_active.png");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(SMALL_ICON, icon);
		super.setupUI();
		//super.putValue( Drawer.DRAWER_PERSISTENT, new Boolean( true ) ) ;
	}

	public String getActionClass()
	{
		return DynSimOnDemandAction.class.getName();
	}
}