/*
 * Copyright 2014-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.ctf.caf.utils.IBlockPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IPinSourceClient;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.sftable.AlphaNumericTableRowSorter;
import chs.utility.ui.sftable.SortableFilterableTable;
import chs.utility.ui.table.OrderedSelectionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 12/27/13 Time: 4:10 PM To change this template use File | Settings |
 * File Templates.
 */
public class BlockPinSelectionPanel extends JPanel implements IPinSourceClient
{

	private SortableFilterableTable m_table;
	private DefaultTableModel m_pinTableModel;
	private static final int PIN_LIST_TYPE_COLUMN = 0;
	private static final int PIN_LIST_NAME_COLUMN = 1;
	private static final int PIN_NAME_COLUMN = 2;
	private static final int PREF_WIDTH = 123;
	private static final int PREF_HEIGHT = 150;

	private static final Dimension JCOMP_PREF_DIM = new Dimension(2 * PREF_WIDTH, PREF_HEIGHT);
	private IAddBlockPinActionModel m_model = null;
	private boolean m_forPlacingBlock = false;
	//	private boolean m_autoGenerate = false;
//	private boolean m_placeAsStack = false;
//	private JCheckBox m_placeAsStackOption;
	private JDialog m_dialog = null;

	public BlockPinSelectionPanel(JDialog dialog, IAddBlockPinActionModel model, boolean forPlacingBlock)

	{
		m_model = model;
		m_forPlacingBlock = forPlacingBlock;
		m_dialog = dialog;
		initUI();
		initTableData();
	}

	private void initUI()
	{
		createPinTable();
		setUpPanel();
	}

	private void setUpPanel()
	{
		setLayout(new BorderLayout());

		JPanel pinlistPanel = new JPanel(new BorderLayout());
		pinlistPanel.add(
				new JLabel(ResourceMgr.getString(BlockPinSelectionPanel.class,
						"BlockPinSelectionPanel.selectPins.text")), BorderLayout.NORTH);

		JScrollPane listScrollPane = new JScrollPane(m_table);
		listScrollPane.setPreferredSize(JCOMP_PREF_DIM);
		listScrollPane.setMinimumSize(JCOMP_PREF_DIM);
		pinlistPanel.add(listScrollPane, BorderLayout.CENTER);

		//showCheckBoxes(pinlistPanel);

		add(pinlistPanel);
	}

	private void createPinTable()
	{
		// setup the columns
		Object[] colNames = new Object[]
				{
						ResourceMgr.getString(BlockPinSelectionPanel.class,
								"BlockPinSelectionPanel.pintable.PinListType"),
						ResourceMgr.getString(BlockPinSelectionPanel.class,
								"BlockPinSelectionPanel.pintable.PinListName"),
						ResourceMgr.getString(BlockPinSelectionPanel.class,
								"BlockPinSelectionPanel.pintable.PinName")
				};

		m_pinTableModel = new DefaultTableModel(colNames, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};

		m_table = new BlockPinSelectionTable(m_pinTableModel);
		m_table.setSortKeys(getSortKeys());
		m_table.setDefaultRenderer(Object.class, new BlockPinNameRenderer(false));
		m_table.setSelectionModel(new OrderedSelectionModel(m_table, PIN_NAME_COLUMN));
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				m_dialog.validate();
			}
		});

		m_table.setName("list_avail_pins");
		m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	private List<RowSorter.SortKey> getSortKeys()
	{
		List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(PIN_LIST_TYPE_COLUMN, SortOrder.ASCENDING));
		sortKeys.add(new RowSorter.SortKey(PIN_LIST_NAME_COLUMN, SortOrder.ASCENDING));
		sortKeys.add(new RowSorter.SortKey(PIN_NAME_COLUMN, SortOrder.ASCENDING));
		return sortKeys;
	}

	protected void initTableData()
	{
		Map<String, IPinProxy> pinProxyMap = m_model.getPinProxyMap();

		m_pinTableModel.getDataVector().removeAllElements();
		m_pinTableModel.fireTableDataChanged();

		for (IPinProxy pinProxy : pinProxyMap.values()) {
			IBlockPinProxy blkPinProxy = (IBlockPinProxy) pinProxy;
			m_pinTableModel.addRow(new Object[]{blkPinProxy.getPinListTypeDisplayString(),
					blkPinProxy.getAssociatedObject(), blkPinProxy});
		}
	}

	public JTable getTable()
	{
		return m_table;
	}

	/**
	 * Return the list of pins currently selected in the panel
	 *
	 * @return The list of pins, sorted case-insensitive by name
	 */
	public List<IPinProxy> getPins()
	{
		List<IPinProxy> pins = new ArrayList<IPinProxy>();
		ListSelectionModel selectionModel = m_table.getSelectionModel();
		if (selectionModel instanceof OrderedSelectionModel) {
			((OrderedSelectionModel) selectionModel).getSelectedOrder().stream()
					.forEach(obj -> pins.add((IPinProxy) obj));
		}
		if (pins.size() != m_table.getSelectedRowCount()) {
			pins.clear();
			for (int rowIdx : m_table.getSelectedRows()) {
				int actualIndex = m_table.getSortedRowIndex(rowIdx);
				Object o = m_table.getModel().getValueAt(actualIndex, PIN_NAME_COLUMN);
				if (o instanceof IPinProxy) {
					pins.add((IPinProxy) o);
				}
			}
		}
		return pins;
	}

	@Override
	public boolean isValidPin(int row, StringBuffer invalidReason)
	{
		int modelRow = m_table.convertRowIndexToModel(row);
		Object valueAt = m_table.getModel().getValueAt(modelRow, PIN_NAME_COLUMN);
		if (valueAt != null) {
			assert valueAt instanceof IBlockPinProxy;
			IBlockPinProxy blkPin = (IBlockPinProxy) valueAt;
			invalidReason.append(blkPin.getInvalidityReason());
			return blkPin.isValid();
		}

		return true;
	}

	private static class BlockPinSelectionTable extends SortableFilterableTable
	{

		private BlockPinSelectionTable(TableModel tableModel)
		{
			super(tableModel);
		}

		@Nullable
		@Override
		protected ComponentListener getComponentListener()
		{
			return new SFTableComponentListener()
			{
				@Override
				protected void updateScrollPaneSettings()
				{
					JScrollPane enclosingScrollPane = getEnclosingScrollPane();
					if (enclosingScrollPane != null) {
						enclosingScrollPane
								.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
					}
				}
			};
		}

		@NotNull
		@Override
		protected TableRowSorter<TableModel> createSorter(TableModel tableModel)
		{
			return new AlphaNumericTableRowSorter(tableModel);
		}
	}

	private static class BlockPinNameRenderer extends PinSelectionCommonPanel.PinNameRenderer
			implements ListCellRenderer
	{

		BlockPinNameRenderer(boolean bDisplayPinIcon)
		{
			super(bDisplayPinIcon);
		}

		public Component getListCellRendererComponent(
				JList list,
				Object value, // value to display
				int index, // cell index
				boolean isSelected, // is the cell selected
				boolean cellHasFocus)    // the list and the cell have the focus
		{
			Font ft = list.getFont();
			configureComponent(value, ft);

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			setEnabled(list.isEnabled());
			setOpaque(true);
			return this;
		}
	}
}

