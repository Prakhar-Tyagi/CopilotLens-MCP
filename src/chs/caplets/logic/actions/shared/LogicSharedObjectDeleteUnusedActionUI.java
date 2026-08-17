/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.ctf.ui.form.shareddeletion.DeleteSharedObjectCmd;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect, Application.ArtisanFunction})
public class LogicSharedObjectDeleteUnusedActionUI extends ActionUI implements ISharedObjectBrowserAction
{

	public LogicSharedObjectDeleteUnusedActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public boolean isEnabled()
	{
		return ISharedObjectBrowserAction.isTreeConstructionComplete() && DeleteSharedObjectCmd.userHasDeleteUnusedSharedObjectsPermission();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(LogicSharedObjectDeleteUnusedActionUI.class,
				"LogicSharedObjectDeleteUnusedActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(LogicSharedObjectDeleteUnusedActionUI.class,
						"LogicSharedObjectDeleteUnusedActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LogicSharedObjectDeleteUnusedActionUI.class,
						"LogicSharedObjectDeleteUnusedActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_delete_unused_shared_active.gif"));
	}

	public String getActionClass()
	{
		return LogicSharedObjectDeleteUnusedAction.class.getName();
	}
}
