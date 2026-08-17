/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caplets.logic.actions.CSVSaveAction;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableWindow;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.util.Collection;

/**
 * Output window tab for displaying batch share status messages in a table including CSV export functionality.
 * Provides a toolbar with export actions and manages the display of batch share operation results.
 */
public class BatchShareTableWindow extends StatusMessageTableWindow<IBatchShareStatusMessage>
{
	/** The table model managing the batch share status messages. */
	@NotNull private final BatchShareStatusMessageTableModel m_tableModel;
	/** The unique title for this window, used to create unique component names. */
	@NotNull private final String m_windowTitle;

	/**
	 * Creates a batch share table window with a toolbar and adds it as an output window tab.
	 *
	 * @param title the title of the output window tab
	 * @param tableModel the table model containing batch share status messages
	 */
	public BatchShareTableWindow(@NotNull String title,
			@NotNull BatchShareStatusMessageTableModel tableModel)
	{
		super(new BorderLayout(), tableModel);
		m_tableModel = tableModel;
		m_windowTitle = title;
		add(createToolBar(), BorderLayout.NORTH);
		add(createTablePanel("BatchShareTableWindowFXPanel"), BorderLayout.CENTER);
		addAsOutputWindowTab(title, true);
	}

	/**
	 * Creates a toolbar with CSV export button.
	 *
	 * @return the configured toolbar
	 */
	@NotNull private JToolBar createToolBar()
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		//push the button to the right
		toolBar.add(Box.createHorizontalGlue());
		CSVSaveAction csvSaveAction = new CSVSaveAction(m_tableModel);
		JButton csvButton = new JButton(csvSaveAction);
		csvButton.setFocusable(false);
		csvButton.setName(m_windowTitle);
		csvButton.getAccessibleContext().setAccessibleName(
				ResourceMgr.getString(chs.caf.helpers.ui.common.statusmessage.CSVSaveAction.class,
						"CSVSaveAction.saveToCSV"));
		toolBar.add(csvButton);

		return toolBar;
	}

	/**
	 * Adds batch share status messages to the table.
	 *
	 * @param messages the messages to add
	 */
	@Override
	public void addData(@NotNull Collection<IBatchShareStatusMessage> messages)
	{
		m_tableModel.addItems(messages);
		super.addData(messages);
	}

	/**
	 * Removes batch share status messages from the table.
	 *
	 * @param messages the messages to remove
	 */
	@Override
	public void removeData(@NotNull Collection<IBatchShareStatusMessage> messages)
	{
		m_tableModel.removeItems(messages);
		super.removeData(messages);
	}
}