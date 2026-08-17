/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableWindow;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.impl.CapitalTableColumn;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Helper to create output window tab with fx table and add rows
 */
public abstract class FXStatusWindowAssistant<T>
{

	@NotNull protected final String m_tabName;
	@NotNull protected StatusMessageTableWindow<T> m_statusWindow;

	protected FXStatusWindowAssistant(@NotNull String tabName)
	{
		m_tabName = tabName;
	}

	@NotNull protected String getTabName()
	{
		return m_tabName;
	}

	public void removeStatusTab()
	{
		CAFUtils.getInstance().getOutputWindow().removePane(getTabName());
	}

	public void addStatusMessages(@NotNull Collection<T> messages)
	{
		if(m_statusWindow!= null)
		{
			m_statusWindow.addData(messages);
		}

	}
	protected void constructStatusWindow(String fixedColumnName)
	{
		m_statusWindow = getStatusWindow();
		Platform.runLater(() -> {
			Table<T> table = m_statusWindow.getTable();
			if (table != null) {
				table.columns().forEach(columnInfo -> {
					if (fixedColumnName.equals(columnInfo.getName())) {
						CapitalTableColumn<T> tableColumn = columnInfo.getTableColumn();
						if (tableColumn != null) {
							final int fixedSize = 100;
							tableColumn.setMinWidth(fixedSize);
							tableColumn.setMaxWidth(fixedSize);
						}
					}
				});
			}
		});
	}

	@NotNull protected abstract StatusMessageTableWindow<T> getStatusWindow();
}
