/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalLogicDesigner,
				Application.ArtisanFunction})
public class FreezeSharedObjectsActionUI extends ActionUI implements ISharedObjectBrowserAction
{

	private static final String DEFAULT_NAME =
			ResourceMgr.getStringForMenu(FreezeSharedObjectsActionUI.class, "FreezeSharedObjectsActionUI.name.decl");
	private static final String DEFAULT_SHORTDESC = ResourceMgr
			.getStringForMenu(FreezeSharedObjectsActionUI.class, "FreezeSharedObjectsActionUI.shortDesc.decl");
	private static final String DEFAULT_LONGDESC =
			ResourceMgr.getString(FreezeSharedObjectsActionUI.class, "FreezeSharedObjectsActionUI.longDesc.decl");

	public FreezeSharedObjectsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_freeze_shared_active.gif");
		putValue(NAME, DEFAULT_NAME);
		putValue(SHORT_DESCRIPTION, DEFAULT_SHORTDESC);
		putValue(LONG_DESCRIPTION, DEFAULT_LONGDESC);
		putValue(SMALL_ICON, icon);
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
//			getAction().setDisabledReason(disabledReason);
			return false;
		}
		putValue(SHORT_DESCRIPTION, DEFAULT_SHORTDESC);
		if (getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}
		return ISharedObjectBrowserAction.isTreeConstructionComplete() && super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return FreezeSharedObjectsAction.class.getName();
	}
}
