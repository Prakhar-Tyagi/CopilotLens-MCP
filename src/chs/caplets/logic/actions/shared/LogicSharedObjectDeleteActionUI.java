/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by jamesmw User: jamesmw Date: 27-Jun-2007 Time: 11:21:15
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.ArtisanFunction}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class LogicSharedObjectDeleteActionUI extends ActionUI implements ISharedObjectBrowserAction
{

	public LogicSharedObjectDeleteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(LogicSharedObjectDeleteActionUI.class,
						"LogicSharedObjectDeleteActionUI.mnemonic.decl")));
		putValue(NAME, ResourceMgr.getString(LogicSharedObjectDeleteActionUI.class,
				"LogicSharedObjectDeleteActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(LogicSharedObjectDeleteActionUI.class,
						"LogicSharedObjectDeleteActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(LogicSharedObjectDeleteActionUI.class,
						"LogicSharedObjectDeleteActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_delete_shared_active.gif"));
	}

	public String getActionClass()
	{
		return LogicSharedObjectDeleteAction.class.getName();
	}

	public boolean isEnabled()
	{
		IAction action = getAction();
		if (action != null) {
			if (ActionRT.isDesignUnderConcurrentEdit()) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
				return false;
			}
			return ISharedObjectBrowserAction.isTreeConstructionComplete() && action.isEnabled();
		}
		return false;
	}
}