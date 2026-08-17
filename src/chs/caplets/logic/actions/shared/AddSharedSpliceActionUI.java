/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: May 3, 2004 Time: 8:16:52 PM
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_SHARED_SPLICE_ACTION",
		label = "Add Shared Splice",
		tooltip = "Add Shared Splice...",
		icon = "ico_shared_splice_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddSharedSpliceActionUI extends ActionUI
{

	public AddSharedSpliceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		putValue(MNEMONIC_KEY, new Integer(
				ResourceMgr.getMnemonic(AddSharedSpliceActionUI.class, "AddSharedSpliceActionUI.mnemonic.decl")));
		putValue(NAME,
				ResourceMgr.getStringForMenu(AddSharedSpliceActionUI.class, "AddSharedSpliceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AddSharedSpliceActionUI.class, "AddSharedSpliceActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedSpliceActionUI.class, "AddSharedSpliceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_splice_active.gif"));
	}

	public String getActionClass()
	{
		return AddSharedSpliceAction.class.getName();
	}
}
