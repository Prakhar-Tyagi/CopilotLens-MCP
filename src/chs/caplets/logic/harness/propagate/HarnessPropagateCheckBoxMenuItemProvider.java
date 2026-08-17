/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caplets.logic.actions.shared.CheckBoxMenuItemProvider;
import com.mentor.capital.javafx.table.Table;

/**
 * Menu item for selecting and de selecting checkbox for all rows
 */
public class HarnessPropagateCheckBoxMenuItemProvider extends CheckBoxMenuItemProvider<IHarnessPropagateStatusMessage>
{

	HarnessPropagateCheckBoxMenuItemProvider(Table<IHarnessPropagateStatusMessage> table, String columnName, Action action)
	{
		super(table, columnName, action);
	}
}
