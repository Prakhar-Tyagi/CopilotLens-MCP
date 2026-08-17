/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Class that represents action user interface for AddSharedSingleLineAction
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddSharedSingleLineActionUI extends AddSharedHighwayActionUI
{

	public AddSharedSingleLineActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddSharedSingleLineActionUI.class, "AddSharedSingleLineActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSharedSingleLineActionUI.class, "AddSharedSingleLineActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSharedSingleLineActionUI.class, "AddSharedSingleLineActionUI.longDesc.decl"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_add_shared_active.gif"));
	}

	@NotNull @Override public String getActionClass()
	{
		return AddSharedSingleLineAction.class.getName();
	}
}
