/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryDeviceFootprintConnectorDetail;
import chs.cof.parts.ILibraryDeviceFootprintPinMapping;
import chs.cof.parts.ILibraryObject;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.StripingTableCellRenderer;
import chs.utility.helpers.LibraryHelper;
import chs.utility.ui.table.TableSorterModel;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * A dialog for adding pins to a NON-SHARED pinlist for which a  LIBRARY PART has been specified
 */
public class AddPinDialog extends CAFOkCancelDialog
{

	private JTable pinTable;
	protected TableSorterModel sortedTableModel;
	private IPinList pinList;
	private ILibraryObject libraryObject;
	private ILibraryDeviceFootprint footprint;
	private boolean success;

	private static String[] headers = {
			ResourceMgr.getString(AddPinDialog.class, "AddPinDialog.table.headers.pin.text"),
			ResourceMgr.getString(AddPinDialog.class, "AddPinDialog.table.headers.conn.text"),
			ResourceMgr.getString(AddPinDialog.class, "AddPinDialog.table.headers.cav.text")
	};

	/**
	 * Constructor for the AddSharedPinListDialog object
	 *
	 * @param frame Description of the Parameter
	 * @param title Description of the Parameter
	 */
	public AddPinDialog(
			Frame frame,
			String title,
			IPinList pl,
			ILibraryObject libObj,
			ILibraryDeviceFootprint mapping
	)
	{
		super(frame, title, true);
		assert pl.getSharedPinList() == null : "This dialog should not be used for shared pinlists";

		pinList = pl;
		libraryObject = libObj;
		footprint = mapping;
		success = false;

		try {
			buildTable();
			hookupButtons();
			pack();
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, false);
		}
	}

	private void buildTable()
	{
		//
		// No footprint -> only display the cavity name
		//
		Object[] trimHeaders;
		if (footprint == null) {
			trimHeaders = new Object[]{headers[0]};
		}
		else {
			trimHeaders = headers;
		}
		if (pinList instanceof IConnector) {
			trimHeaders = new Object[]{
					ResourceMgr.getString(AddPinDialog.class, "AddPinDialog.table.headers.connectorpin.text")};
		}
		AddPinTableModel tModel = new AddPinTableModel(trimHeaders, 0);

		//
		// Create map of device pin  -> footprint.
		//
		Map<String, ILibraryDeviceFootprintPinMapping> fpMap =
				new HashMap<String, ILibraryDeviceFootprintPinMapping>();
		if (footprint != null) {
			for (ILibraryDeviceFootprintConnectorDetail connectorDetail : footprint.getLibraryDeviceFootprintConnectorDetails()) {
				for (ILibraryDeviceFootprintPinMapping pinMap : connectorDetail.getFootprintPinMappings()) {
					fpMap.put(pinMap.getPin().getName(), pinMap);
				}
			}
		}

		for (ILibraryCavity cavity : LibraryHelper.getCavities(libraryObject)) {
			String devicePinName = cavity.getName();
			ILibraryDeviceFootprintPinMapping pinMap = fpMap.get(devicePinName);

			// look for pin on device - we will want to disable it
			// (not allow it to be added) if it is already placed
			boolean usedAlready = false;
			for (IAbstractPinIterator pi = pinList.getPins(); pi.hasNext();) {
				IAbstractPin devPin = pi.getNext();
				if (devPin.getName().equals(devicePinName)) {
					usedAlready = true;
					break;
				}
			}

			if (usedAlready) {
				continue;
			}
			Object[] rowContents;
			if (footprint != null && pinMap != null) {
				rowContents = new Object[]{devicePinName, pinMap.getOwner().getConnectorName(), pinMap.getCavity().getName()};
			}
			else {
				rowContents = new Object[]{devicePinName};
			}
			tModel.addRow(rowContents);
		}

		sortedTableModel = new TableSorterModel(tModel);

		// if there are no pins
		if (sortedTableModel.getRowCount() == 0) {
			JLabel warning = new JLabel(ResourceMgr.getString(
					AddPinDialog.class, "AddPinDialog.noPins.wanring.text"));
			warning.setHorizontalAlignment(SwingConstants.CENTER);
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(new EmptyBorder(20, 20, 20, 20));
			p.add(warning, BorderLayout.CENTER);
			p.setPreferredSize(new Dimension(300, 60));
			getContentPane().add(p, BorderLayout.CENTER);
			rememberSize(false);
			return;
		}

		pinTable = new JTable(sortedTableModel);
		pinTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pinTable.setRowSelectionAllowed(true);
		pinTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && pinTable.getSelectedRowCount() != 0) {
					getOkButton().doClick();
				}
			}
		});
		pinTable.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiers() == 0) {
					getCancelButton().doClick();
				}
			}
		});

		// Need to use the more general InputMap/ActionMap method to override the Enter key action
		// as the JTable also acts ont he Enter key to move the selection down one row. Just adding
		// a KeyListener does not stop the JTable action.
		String ENTER_ACTION_KEY = "ENTER_ACTION_KEY";
		int noModifiers = 0;
		KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, noModifiers, false);
		InputMap inputMap = pinTable.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(enterKey, ENTER_ACTION_KEY);
		AbstractAction enterAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				getOkButton().doClick();
			}
		};
		pinTable.getActionMap().put(ENTER_ACTION_KEY, enterAction);

		pinTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				getOkButton().setEnabled(pinTable.getSelectedRowCount() > 0);
			}
		});
		sortedTableModel.setupTableHeader(pinTable);
		sortedTableModel.sortByColumn(0);

		pinTable.setName("AddPinTable");
		pinTable.setDefaultRenderer(Object.class, new AddPinCellRenderer());
		pinTable.setRowSelectionInterval(0, 0);

		JScrollPane sp = new JScrollPane(pinTable);
		int width = (footprint == null) ? 175 : 300;
		sp.setPreferredSize(new Dimension(width, 200));

		getContentPane().add(sp, BorderLayout.CENTER);
	}

	/**
	 * Brings up this dialog in a modal fashion to figure out what shared pinlist and sharedpin are being added to the
	 * diagram.
	 *
	 * @return Description of the Return Value
	 */
	public boolean selectPin()
	{
		setVisible(true);
		return success && (pinTable.getSelectedRow() >= 0);
	}

	public String getSelectedPin()
	{
		int index = pinTable.getSelectedRow();
		return (index >= 0)
				? (String) sortedTableModel.getValueAt(index, 0)
				: null;
	}

	private void hookupButtons()
	{
		if (sortedTableModel.getRowCount() > 0) {
			getOkButton().addActionListener(
					new ActionListener()
					{
						public void actionPerformed(ActionEvent evt)
						{
							success = true;
							setVisible(false);
							dispose();
						}
					}
			);
		}
		else {
			getOkButton().setEnabled(false);
		}

		getCancelButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						success = false;
						setVisible(false);
						dispose();
					}
				}
		);
	}

	private static class AddPinTableModel extends DefaultTableModel
	{

		AddPinTableModel(Object[] columnNames, int rowCount)
		{
			super(columnNames, rowCount);
		}

		public String getColumnName(int column)
		{

			Object id = columnIdentifiers.elementAt(column);
			return (String) id;
		}

		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	}

	private class AddPinCellRenderer extends StripingTableCellRenderer
	{

		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column
		)
		{
			super.getTableCellRendererComponent(table,
					value,
					isSelected,
					hasFocus,
					row,
					column);

//			setHorizontalAlignment(SwingConstants.CENTER);
			setFont(table.getFont());
			setText((String) value);

			return this;
		}
	}
}

