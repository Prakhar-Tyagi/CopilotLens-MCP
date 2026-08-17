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

import java.util.Collection;

/**
 * Helper to create output window tab for bulk unfreeze
 */
public class UnfreezeStatusWindowAssistant extends FXStatusWindowAssistant<IUnfreezeStatusMessage>
{

	private final boolean setTabActive;

	public UnfreezeStatusWindowAssistant(@NotNull String tabName, String fixedColumnName, boolean setTabActive)
	{
		super(tabName);
		removeStatusTab();
		//setTabActive should be set before calling constructStatusWindow() function as this value is used inside it (inside getStatusWindow())
		this.setTabActive = setTabActive;
		constructStatusWindow(fixedColumnName);

	}

	@NotNull @Override protected StatusMessageTableWindow<IUnfreezeStatusMessage> getStatusWindow()
	{
		return new UnfreezeTableWindow(getTabName(), new UnfreezeStatusMessageTableModel(), setTabActive);
	}

	public void removeStatusMessage(@NotNull Collection<IUnfreezeStatusMessage> messages)
	{
		if (!messages.isEmpty()) {
			m_statusWindow.removeData(messages);
		}
	}
}
