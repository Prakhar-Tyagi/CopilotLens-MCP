/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionUI;
import chs.cof.logical.shared.ISharedObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by jamesmw User: jamesmw Date: 11-Jul-2007 Time: 14:52:52
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.ArtisanFunction}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class FreezeUnfreezeSharedObjectActionUI extends ActionUI
{

	public FreezeUnfreezeSharedObjectActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY,
				new Integer(ResourceMgr.getMnemonic(FreezeUnfreezeSharedObjectActionUI.class,
						"FreezeUnfreezeSharedObjectActionUI.mnemonic.freeze.decl")));
		putValue(NAME, ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
				"FreezeUnfreezeSharedObjectActionUI.name.freeze.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
						"FreezeUnfreezeSharedObjectActionUI.shortDesc.freeze.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
						"FreezeUnfreezeSharedObjectActionUI.longDesc.freeze.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_freeze_shared_active.gif"));
	}

	public void updateUI()
	{
		super.updateUI();
		IAction action = getAction();
		if (action != null) {
			ISharedObject sharedObject = (ISharedObject) ((FreezeUnfreezeSharedObjectAction) action).getOperand();
			if (sharedObject == null) {
				return;
			}

			String resourceString = "freeze";
			if (sharedObject.isFrozen()) {
				resourceString = "unfreeze";
			}

			putValue(MNEMONIC_KEY,
					new Integer(ResourceMgr.getMnemonic(FreezeUnfreezeSharedObjectActionUI.class,
							"FreezeUnfreezeSharedObjectActionUI.mnemonic." + resourceString + ".decl")));
			putValue(NAME, ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
					"FreezeUnfreezeSharedObjectActionUI.name." + resourceString + ".decl"));
			putValue(SHORT_DESCRIPTION,
					ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
							"FreezeUnfreezeSharedObjectActionUI.shortDesc." + resourceString + ".decl"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(FreezeUnfreezeSharedObjectActionUI.class,
							"FreezeUnfreezeSharedObjectActionUI.longDesc." + resourceString + ".decl"));
		}
	}

	public boolean isEnabled()
	{
//		if (ActionRT.isDesignUnderConcurrentEdit()) {
//			String disabledReason = ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode");
//			getAction().setDisabledReason(disabledReason);
//			return false;
//		}
		IAction action = getAction();
		if (action != null) {
			return action.isEnabled();
		}
		return false;
	}

	public String getActionClass()
	{
		return FreezeUnfreezeSharedObjectAction.class.getName();
	}
}
