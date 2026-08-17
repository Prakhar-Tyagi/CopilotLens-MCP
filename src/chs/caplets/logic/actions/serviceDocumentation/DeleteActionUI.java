/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;

@ApplicationSpecification(
		includeIn = {Application.SvcDoc})
public class DeleteActionUI extends chs.caplets.shared.actions.DeleteActionUI
{

	/**
	 * Constructor for the DeleteActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public DeleteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

/*	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(DeleteHydraActionUI.class, "DeleteHydraActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(DeleteHydraActionUI.class, "DeleteHydraActionUI.shortDesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(DeleteHydraActionUI.class, "DeleteHydraActionUI.longDesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_D));
		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}*/

	@Override public String getActionClass()
	{
		return PublisherDeleteAction.class.getName();
	}
}
