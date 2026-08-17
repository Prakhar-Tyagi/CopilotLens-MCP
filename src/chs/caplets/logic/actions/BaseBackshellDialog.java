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

import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.parts.ILibraryCavity;
import chs.common.INamedPropertiedObject;
import chs.common.IReadOnlyNamedObject;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.ctf.caf.utils.IPinProxy;
import chs.images.CHSImageLoader;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.SortedListModel;
import chs.utility.helpers.NamedObjectComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseBackshellDialog extends CAFOkCancelDialog
{

	private static final Icon backshellTerminationIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/ico_backshell_term_active.gif");
	private static final Icon sharedbackshellTerminationIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/ico_shared_backshell_term_active.gif");
	@NotNull protected final IAddBackshellController controller;
	protected SortedListModel<Object> m_terms =
			new SortedListModel<>(NamedObjectComparator.caseInsensitiveComparator());
	protected JList<Object> m_termList;
	protected Set<String> m_existingTerminations = new HashSet<String>();

	protected BaseBackshellDialog(@Nullable Frame frame, String title, boolean amodal,
			@NotNull IAddBackshellController controller)
	{
		super(frame, title, amodal);
		this.controller = controller;
	}

	protected void addListeners()
	{
		// Setup an action listener on the Ok button to terminate with success
		getOkButton().addActionListener(getOKActionListener());

		// Setup an action listener on the Cancel button to terminate without success
		getCancelButton().addActionListener(getCancelActionListener());
	}

	@NotNull protected JScrollPane prepareTerminationScrollPane(String name)
	{
		m_termList = new JList<>(m_terms);
		m_termList.setSelectionModel(new DefaultListSelectionModel());
		m_termList.setName(name);
		m_termList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		m_termList.setCellRenderer(new BackshellTerminationsListCellRenderer());
		return new JScrollPane(m_termList);
	}

	protected abstract ActionListener getCancelActionListener();

	protected abstract ActionListener getOKActionListener();

	public List<IPinProxy> getSelectedBackshellTerminations()
	{
		List<IPinProxy> resList = new ArrayList<>();
		ListSelectionModel lsm = m_termList.getSelectionModel();
		ListModel<Object> lm = m_termList.getModel();
		for (int i = 0; i < lm.getSize(); i++) {
			if (lsm.isSelectedIndex(i)) {
				IPinProxy pinProxy = controller.createPinProxy(lm.getElementAt(i));
				resList.add(pinProxy);
			}
		}
		return resList;
	}

	protected void populateExistingBackshellTerminations()
	{
		// Track and add existing backshell termination names to selection list
		addExistingBackshellTerminations();

		// Also, track and add library terminations as well
		addLibraryTerminations();
	}

	private void addExistingBackshellTerminations()
	{
		for (INamedPropertiedObject term : controller.getExistingBackshellTerminations()) {
			m_terms.addSorted(term);
			m_existingTerminations.add(term.getName());
		}
	}

	private void addLibraryTerminations()
	{
		for (ILibraryCavity cavity : controller.getLibraryTerminations()) {
			if (!m_existingTerminations.contains(cavity.getName())) {
				m_terms.addSorted(cavity.getName());
				m_existingTerminations.add(cavity.getName());
			}
		}
	}

	protected static class BackshellTerminationsListCellRenderer extends DefaultListCellRenderer
	{

		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
		{
			// Render termination name
			Component comp = super.getListCellRendererComponent(list, getName(value), index, isSelected, cellHasFocus);

			//Render relevant icon based on termination type
			((JLabel) comp).setIcon(getBackshellTerminationIconToShow(value));

			//Render striped background color for alternate rows
			if (!isSelected && !cellHasFocus) {
				if (index % 2 != 0) {
					setBackground(CHSColors.getStripeBackgroundColor());
				}
			}
			return comp;
		}

		public String getName(Object value)
		{
			String textValue;
			if (value instanceof IReadOnlyNamedObject) {
				textValue = ((IReadOnlyNamedObject) value).getName();
			}
			else {
				textValue = value.toString();
			}
			return textValue;
		}

		public Icon getBackshellTerminationIconToShow(Object value)
		{
			Icon iconToShow = backshellTerminationIcon;
			if (value instanceof ISharedBackshellTermination) {
				iconToShow = sharedbackshellTerminationIcon;
			}
			else if (value instanceof IBackshellTermination && ((IGenericPin) value).getSharedPin() != null) {
				iconToShow = sharedbackshellTerminationIcon;
			}
			return iconToShow;
		}

	}
}
