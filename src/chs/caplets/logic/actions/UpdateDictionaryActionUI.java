/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Action UI for UpdateDictionaryAction
 */
@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class UpdateDictionaryActionUI extends ActionUI
{

	public UpdateDictionaryActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(UpdateDictionaryActionUI.class, "UpdateDictionaryActionUI.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(UpdateDictionaryActionUI.class, "UpdateDictionaryActionUI.short"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(UpdateDictionaryActionUI.class, "UpdateDictionaryActionUI.longDesc"));
	}

	@Override @NotNull public String getActionClass()
	{
		return UpdateDictionaryAction.class.getName();
	}
}
