/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared.utils;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.utilities.stream.StreamUtils;
import chs.utility.ui.IOutputWindowPane;
import com.mentor.capital.javafx.table.Table;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * Output window tab for showing messages in FX table
 */
public abstract class StatusMessageTableWindow<T> extends JPanel implements IOutputWindowPane
{

	private Table<T> m_table;

	protected StatusMessageTableWindow(@NotNull LayoutManager mgr, @NotNull StatusMessageTableModel<T> tableModel)
	{
		setLayout(mgr);
		Platform.runLater(() -> {
			m_table = new Table<>(tableModel.getTablePrefID(), tableModel, TableUtils::setAlphaNumComparator);
			m_table.addColumns(tableModel.getColumns());
			m_table.getTableView().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
			m_table.getTableView().getColumns().stream().forEach(column -> column.reorderableProperty().set(false));
		});
	}

	@NotNull protected JPanel createTablePanel(@NotNull String panelName)
	{
		JPanel tablePanel = new JPanel(new BorderLayout());
		final JFXPanel fxPanel = new JFXPanel();
		fxPanel.setName(panelName);
		tablePanel.add(fxPanel);
		Platform.runLater(() -> {
			Scene scene = new Scene(m_table);
			JFXFlatUIUtils.getInstance().setFlatUIFor(scene);
			fxPanel.setScene(scene);
		});
		return tablePanel;
	}

	protected void addAsOutputWindowTab(@NotNull String title, boolean setActive)
	{
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		outputWindow.addComponentPane(title, this, false);
		if(setActive){
			outputWindow.setActivePaneForced(title);
		}
	}

	public void addData(@NotNull Collection<T> messages)
	{
		Platform.runLater(() -> m_table.addData(messages));
	}

	public void removeData(@NotNull Collection<T> messages)
	{
		Platform.runLater(() -> m_table.removeData(messages));
	}

	@Override public boolean propagateKeyActions(@NotNull KeyEvent evt)
	{
		return m_table == null || m_table.columns().map(aCol -> aCol.getTableColumn()).filter(StreamUtils::notNull)
				.map(TableColumnBase::getContextMenu).filter(StreamUtils::notNull)
				.filter(aContext -> aContext.isShowing()).findFirst().isEmpty();
	}

	@Nullable public Table<T> getTable()
	{
		return m_table;
	}
}