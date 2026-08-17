/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Fixed set of columns in batch share dialog
 */
public enum BatchShareColumn
{
	SELECTION(AbstractShareColumnsProvider.SELECTION_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.selection.text")),
	OBJECT_NAME(AbstractShareColumnsProvider.NAME_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.objectname.text")),
	ACTION("Action", ResourceMgr.getString(BatchShareColumn.class, "BatchShareColumn.action.text")),
	DESIGN(AbstractShareColumnsProvider.DESIGN_COLUMN_NAME,
			ResourceMgr.getString(AbstractShareColumnsProvider.class, "CommonColumn.design.text")),
	MATCHED_BY("Matchedby", ResourceMgr.getString(BatchShareColumn.class, "BatchShareColumn.matchedBy.text"));

	private String name;
	private String displayName;

	@NotNull public String getName()
	{
		return name;
	}

	@NotNull public String getDisplayName()
	{
		return displayName;
	}

	BatchShareColumn(String name, String displayName)
	{
		this.name = name;
		this.displayName = displayName;
	}
}
