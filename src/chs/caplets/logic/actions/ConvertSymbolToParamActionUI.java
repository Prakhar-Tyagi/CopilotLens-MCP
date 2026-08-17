/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.SvcDoc, Application.CapitalCapture, Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED
)
public class ConvertSymbolToParamActionUI extends ActionUI
{

	public ConvertSymbolToParamActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr
				.getString(ConvertSymbolToParamActionUI.class, "ConvertSymbolToParamAction.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr
						.getString(ConvertSymbolToParamActionUI.class, "ConvertSymbolToParamAction.sdesc.text"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(ConvertSymbolToParamActionUI.class, "ConvertSymbolToParamAction.ldesc.text"));
	}

	@Override public String getActionClass()
	{
		return ConvertSymbolToParamAction.class.getName();
	}
}
