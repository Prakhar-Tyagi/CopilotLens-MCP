/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caplets.logic.actions.shared.AddSharedICDAction;
import chs.caplets.logic.actions.shared.AddSharedICDActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class CreateICDFromSharedICDBrowserAction extends AbstractAction
{

	public CreateICDFromSharedICDBrowserAction()
	{
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddSharedICDActionUI.class, "AddSharedICDActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AddSharedICDActionUI.class, "AddSharedICDActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedICDActionUI.class, "AddSharedICDActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_device_active.gif"));
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		IAction action = getActiveCapletController().getAction(AddSharedICDAction.class);
		if (action != null) {
			IActionMgr actMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actMgr != null) {
				actMgr.actionPerformed(action, e);
			}
		}
	}

	protected ICapletController getActiveCapletController()
	{
		return CAFUtils.getInstance().getActiveCapletController();
	}

}