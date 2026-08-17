/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caplets.logic.actions.shared.FXStatusWindowAssistant;
import org.jetbrains.annotations.NotNull;

/**
 * Helper to create output window tab for Batch Share action with fx table and add rows
 */
public class BatchShareStatusWindowAssistant extends FXStatusWindowAssistant<IBatchShareStatusMessage>
{

	public BatchShareStatusWindowAssistant(@NotNull String tabName, String fixedColumnName)
	{
		super(tabName);
		removeStatusTab();
		constructStatusWindow(fixedColumnName);
	}

	@Override @NotNull protected BatchShareTableWindow getStatusWindow()
	{
		return new BatchShareTableWindow(getTabName(), new BatchShareStatusMessageTableModel());
	}
}