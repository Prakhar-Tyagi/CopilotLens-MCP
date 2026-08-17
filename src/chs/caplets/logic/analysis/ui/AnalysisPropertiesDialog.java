/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.analysis.ui;

import chs.caf.CAFUtils;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.utilities.ui.BasicUIFactory;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author rharring
 */
public class AnalysisPropertiesDialog extends CAFOkCancelDialog
		implements MouseListener, TableCellRenderer, ListSelectionListener
{

	protected static final char watchChar = Character.toLowerCase(ResourceMgr.getChar(LogicAnalysisServices.class,
			"AnalysisPropertiesDialog.String.propertiesAction.watchSelected.mnemonic"));
	protected static final char stopChar = Character.toLowerCase(ResourceMgr.getChar(LogicAnalysisServices.class,
			"AnalysisPropertiesDialog.String.propertiesAction.stopWatching.mnemonic"));

	protected static DefaultTableCellRenderer renderComp = new DefaultTableCellRenderer();

	protected JTable table;
	protected String component;
	protected String uid;
	protected JButton watchButton;
	protected JButton stopButton;

	public AnalysisPropertiesDialog(String uid, List<List<String>> data, String component)
	{
		super(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(LogicAnalysisServices.class,
						"AnalysisPropertiesDialog.String.propertiesAction.dialogTitle") + " " + component, true);
		this.component = component;
		//setTitle( "Properties for " + component + "..." ) ;
		createGui(data);
		addActionListeners();
		pack();
		//setModal( true ) ;
	}

	private static class AnalysisPropertyTable extends JTable
	{

		@SuppressWarnings("UseOfObsoleteCollectionType") protected AnalysisPropertyTable(List<Vector<String>> rows, List<String> columns)
		{
			// TODO - revisit to remove Vectors...
			super(new DefaultTableModel(new Vector<Vector<String>>( rows ), new Vector<String>( columns )));
		}

		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	}

	protected void createGui(List<List<String>> data)
	{
		List<String> columns = new ArrayList<String>();
		columns.add(ResourceMgr.getString(LogicAnalysisServices.class,
				"AnalysisPropertiesDialog.String.propertiesAction.propertyColumn"));
		columns.add(ResourceMgr.getString(LogicAnalysisServices.class,
				"AnalysisPropertiesDialog.String.propertiesAction.valueColumn"));

		// Hmm, need tp convert to vector here...
		List<Vector<String>> convertedData = new ArrayList<Vector<String>>( );
		for ( List<String> list : data ) {
			Vector<String> v = new Vector<String>(list);
			convertedData.add( v );
		}

		table = new AnalysisPropertyTable(convertedData, columns);
		table.setAutoCreateRowSorter(true);

		table.setName("propertiesTable");
		table.addMouseListener(this);
		table.addKeyListener(this);
		table.getColumnModel().getColumn(0).setCellRenderer(this);
		table.getSelectionModel().addListSelectionListener(this);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		JScrollPane pane = new JScrollPane(table);
		pane.getViewport().setBackground(table.getBackground());
		mainPanel.add(pane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		watchButton = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(LogicAnalysisServices.class,
				"AnalysisPropertiesDialog.String.propertiesAction.watchSelected"));
		stopButton = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(LogicAnalysisServices.class,
				"AnalysisPropertiesDialog.String.propertiesAction.stopWatching"));
		watchButton.setMnemonic(watchChar);
		stopButton.setMnemonic(stopChar);
		watchButton.setEnabled(false);
		stopButton.setEnabled(false);
		panel.add(watchButton);
		panel.add(stopButton);
		mainPanel.add(panel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel, BorderLayout.CENTER);
	}

	protected String getProperty(int row)
	{
		return component + "." + table.getValueAt(row, 0);
	}

	protected String getValue(int row)
	{
		return (String) table.getValueAt(row, 1);
	}

	protected void addActionListeners()
	{
		ActionListener al = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (e.getSource() == watchButton) {
					addSelectedWatches();
				}
				else if (e.getSource() == stopButton) {
					stopSelectedWatches();
				}
				else {
					setVisible(false);
				}
			}
		};

		// don't really want any differences so add same listener to both.
		// An ok buttoned dialog would be better.......
		getOkButton().addActionListener(al);
		getCancelButton().addActionListener(al);
		watchButton.addActionListener(al);
		stopButton.addActionListener(al);
	}

	protected void addSelectedWatches()
	{
		alterWatches(true);
	}

	protected void alterWatches(boolean add)
	{
		int[] rows = table.getSelectedRows();
		if (rows.length > 0) {
			for (int row : rows) {
				LogicAnalysisServices.getAnalysisServices().monitorProperty(uid, getProperty(row), getValue(row), add);
			}
		}
		table.repaint(); // repaint the table.
		valueChanged(null); // fire blank event notification to update buttons.
	}

	protected void stopSelectedWatches()
	{
		alterWatches(false);
	}

	public void mouseClicked(MouseEvent mouseEvent)
	{
		if (mouseEvent.getClickCount() == 2) {
			int row = table.rowAtPoint(mouseEvent.getPoint());
			String property = getProperty(row);
			String value = getValue(row);
			LogicAnalysisServices.getAnalysisServices().monitorProperty(uid, property, value, watchButton.isEnabled());
			table.repaint();
			valueChanged(null);
		}
	}

	public void mouseEntered(MouseEvent mouseEvent)
	{
	}

	public void mouseExited(MouseEvent mouseEvent)
	{
	}

	public void mousePressed(MouseEvent mouseEvent)
	{
	}

	public void mouseReleased(MouseEvent mouseEvent)
	{
	}

	public void keyPressed(KeyEvent e)
	{
		final char keyChar = Character.toLowerCase(e.getKeyChar());
		if (keyChar == watchChar && watchButton.isEnabled()) {
			addSelectedWatches();
		}
		else if (keyChar == stopChar && stopButton.isEnabled()) {
			stopSelectedWatches();
		}
		else {
			super.keyPressed(e);
		}
	}

	public java.awt.Component getTableCellRendererComponent(JTable jTable, Object obj, boolean param, boolean param3,
			int param4, int param5)
	{
		DefaultTableCellRenderer rend =
				(DefaultTableCellRenderer) renderComp
						.getTableCellRendererComponent(jTable, obj, param, param, param4, param5);
		if (LogicAnalysisServices.getAnalysisServices().isBeingMonitored(uid, component + "." + obj)) {
			renderComp.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_zoomdynamic_active.gif"));
		}
		else {
			renderComp.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		}
		return renderComp;
	}

	public void valueChanged(ListSelectionEvent listSelectionEvent)
	{
		watchButton.setEnabled(false);
		stopButton.setEnabled(false);

		int[] rows = table.getSelectedRows();
		if (rows.length > 0) {
			for (int row : rows) {
				if (LogicAnalysisServices.getAnalysisServices().isBeingMonitored(uid, getProperty(row))) {
					stopButton.setEnabled(true);
				}
				else {
					watchButton.setEnabled(true);
				}
			}
		}
	}
}

