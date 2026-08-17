/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.utils.StatusMessageTableWindow;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

/**
 * Output window tab for showing bulk unfreeze messages in FX table
 */
public class UnfreezeTableWindow extends StatusMessageTableWindow<IUnfreezeStatusMessage>
{
	public UnfreezeTableWindow(@NotNull String title,
			@NotNull UnfreezeStatusMessageTableModel tableModel, boolean setActive)
	{
		super(new BorderLayout(), tableModel);
		if(setActive)
		{
			add(createTablePanel("UnfreezeTableWindowFXPanel"));
			addAsOutputWindowTab(title, setActive);
		}
	}
}
