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

import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.cell.BooleanCellControlCreator;
import org.jetbrains.annotations.Nullable;

/**
 * To display checkbox state as selected and un-selected in table filter
 */
public class BatchShareBooleanCellControlCreator extends BooleanCellControlCreator
{

	@Nullable @Override public String stringify(@Nullable Object item)
	{
		Boolean value = CommonUtils.cast(item, Boolean.class);
		if (value == null) {
			return super.stringify(item);
		}

		if (value) {
			return ResourceMgr.getString(this, "BatchShareBooleanCellControlCreator.selected.text");
		}
		return ResourceMgr.getString(this, "BatchShareBooleanCellControlCreator.unselected.text");
	}
}
