/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.caplets.logic.actions.shared.batchshare.ui.IBatchShareRow;
import com.mentor.capital.javafx.table.Table;
import org.jetbrains.annotations.NotNull;

/**
 * @author rmahato
 */
public class BatchShareDetailsPane extends AbstractShareDetailsPane<IBatchShareRow>
{

	public BatchShareDetailsPane(@NotNull Table<IBatchShareRow> mainTable)
	{
		super(mainTable);
	}

	@Override @NotNull protected AbstractInstanceDetailPane createInstanceDetailPane()
	{
		return new InstanceDetailPane(null);
	}
	@Override @NotNull protected String getExpandedTextKey()
	{
		return "BatchShareDetailsPane.expanded.text";
	}

	@Override @NotNull protected String getCollapsedTextKey()
	{
		return "BatchShareDetailsPane.collapsed.text";
	}
}
