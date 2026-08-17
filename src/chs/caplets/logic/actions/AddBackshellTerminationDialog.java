/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddBackshellTerminationDialog extends BaseBackshellDialog
{

	private static final int PREFERRED_DIALOG_WIDTH = 250;
	private static final int PREFERRED_DIALOG_HEIGHT = 250;

	public AddBackshellTerminationDialog(Frame frame, String title, boolean amodal, IAddBackshellController controller)
	{
		super(frame, title, amodal, controller);

		jbInit();
		addListeners();
		pack();
	}

	private void jbInit()
	{
		setMinimumSize(new Dimension(PREFERRED_DIALOG_WIDTH, PREFERRED_DIALOG_HEIGHT));
		setPreferredSize(new Dimension(PREFERRED_DIALOG_WIDTH, PREFERRED_DIALOG_HEIGHT));

		JPanel terminationsPanel = new JPanel();
		terminationsPanel.setLayout(new BorderLayout(4, 4));
		terminationsPanel.add(getBackshellTerminationsScrollPane(), BorderLayout.CENTER);

		getContentPane().add(terminationsPanel);
	}

	@NotNull private JScrollPane getBackshellTerminationsScrollPane()
	{
		populateExistingBackshellTerminations();

		final JScrollPane scrollPane = prepareTerminationScrollPane("AddBackshellTerminationDialog.terminationList");

		selectFirstTermination();

		return scrollPane;
	}

	private void selectFirstTermination()
	{
		m_termList.setSelectedIndex(0);
	}

	@NotNull protected ActionListener getCancelActionListener()
	{
		return new AddBackshellTerminationCancelActionListener();
	}

	@NotNull protected ActionListener getOKActionListener()
	{
		return new AddBackshellTerminationOKActionListener(controller);
	}

	private class AddBackshellTerminationOKActionListener implements ActionListener
	{

		private final IAddBackshellController controller;

		private AddBackshellTerminationOKActionListener(IAddBackshellController controller)
		{
			this.controller = controller;
		}

		@Override public void actionPerformed(ActionEvent e)
		{
			controller.selectedBackshellTerminations(getSelectedBackshellTerminations());

			setCancelled(false);
			setVisible(false);
		}
	}

	private class AddBackshellTerminationCancelActionListener implements ActionListener
	{

		private AddBackshellTerminationCancelActionListener()
		{
		}

		@Override public void actionPerformed(ActionEvent e)
		{
			setCancelled(true);
			setVisible(false);
		}
	}
}
