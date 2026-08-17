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
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * ActionUI class for Add Single Line action
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class AddSingleLineActionUI extends AddHighwayActionUI
{

	public AddSingleLineActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddSingleLineActionUI.class, "AddSingleLineActionUI.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddSingleLineActionUI.class, "AddSingleLineActionUI.shortDesc.text"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddSingleLineActionUI.class, "AddSingleLineActionUI.longDesc.text"));
	}

	@NotNull @Override public String getActionClass()
	{
		return AddSingleLineAction.class.getName();
	}
}
