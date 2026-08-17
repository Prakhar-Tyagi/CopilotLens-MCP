/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.helpers.tabulareditor.TablePane;
import chs.caf.caplet.helpers.tabulareditor.TabularEditor;
import chs.caf.caplet.helpers.tabulareditor.TabularSelection;
import chs.utilities.ui.tabulareditor.PropertyColumnProvider;
import org.jetbrains.annotations.NotNull;

public class LogicTablePane extends TablePane
{

	public LogicTablePane(@NotNull TabularSelection selection, TabularEditor owner,
			boolean enableAttributesTable)
	{
		super(selection, owner, enableAttributesTable, true, false);
	}

	@Override @NotNull protected PropertyColumnProvider getPropertyColumnProvider()
	{
		return new LogicPropertyColumnProvider();
	}
}