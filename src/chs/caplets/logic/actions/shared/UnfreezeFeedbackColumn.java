/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Fixed set of columns in Unfreeze output window
 */
public enum UnfreezeFeedbackColumn
{
	Severity("Severity"),
	Message("Message"),
	Object("Object");

	private String name;

	@NotNull public String getName()
	{
		return name;
	}

	@NotNull public String getDisplayName()
	{
		return ResourceMgr.getString(UnfreezeStatusMessageTableModel.class,
				"UnfreezeColumn." + name + ".title");
	}

	UnfreezeFeedbackColumn(String name)
	{
		this.name = name;
	}
}
