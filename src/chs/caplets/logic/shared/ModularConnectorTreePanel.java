package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.SelectSharedPanel;
import chs.caplets.logic.actions.shared.helper.ModularConnectorHandler.IConnectorNode;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.table.ITreeTableModel;
import chs.utilities.ui.table.JTreeTable;
import chs.utilities.ui.table.TreeTableModelAdapter;
import com.mentor.lookandfeel.FlatXTableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 25 Mar, 2013
 */
public class ModularConnectorTreePanel extends JPanel
{

	//create & set model
	private ModularConnectorTreeTable m_treeTable;
	@NotNull private final ModularConnectorTreeModel m_treeModel;

	private MouseAdapter m_mouseAdapter = null;
	private static final int NAME_COL_WIDTH = 300;
	private static final int NAME_FIXED_WIDTH = 50;
	private static final int TREE_WIDTH = 450;
	private ModularTreeCellEditor m_modTreeEditor;

	public ModularConnectorTreePanel(@NotNull ModularConnectorTreeModel model)
	{
		m_treeModel = model;
	}

	public void initTree()
	{
		setLayout(new GridBagLayout());
		setPreferredSize(new Dimension(TREE_WIDTH, 100));
		setMinimumSize(new Dimension(TREE_WIDTH, 100));

		m_treeTable = new ModularConnectorTreeTable(m_treeModel);
		DefaultMutableTreeNode rootNode = m_treeModel.createRootNode();

		m_treeModel.setRoot(rootNode);
		m_treeTable.setTreeModel(m_treeModel);

		formatTableHeader();

		setupTree();
		setupTable();
		reloadTreeData();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(m_treeTable);
		scrollPane.getViewport().setBackground(Color.WHITE);

		GridBagConstraints GBC = new GridBagConstraints();
		GBC.gridx = 0;
		GBC.gridy = 0;
		GBC.weightx = 1.0;
		GBC.weighty = 1.0;
		GBC.anchor = GridBagConstraints.NORTH;
		GBC.fill = GridBagConstraints.BOTH;
		GBC.insets = new Insets(0, 0, 0, 0);

		add(scrollPane, GBC);
	}

	protected void reloadTreeData()
	{
		JTree tree = m_treeTable.getTree();
		DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) m_treeModel.getRoot();
		rootNode.removeAllChildren();
		m_treeModel.populateTree(rootNode);

		m_treeModel.reload(rootNode);

		tree.repaint();
		//make tree editable & expand all rows
		tree.setEditable(true);
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	public void formatTableHeader()
	{
		JTableHeader header = m_treeTable.getTableHeader();
		TableColumnModel columnModel = header.getColumnModel();

		TableColumn tableColumn = columnModel.getColumn(0);
		tableColumn.setPreferredWidth(NAME_COL_WIDTH);
		tableColumn = columnModel.getColumn(1);
		tableColumn.setHeaderRenderer(createDefaultTableHeaderRenderer(JLabel.CENTER));
		tableColumn.setWidth(NAME_FIXED_WIDTH);
		tableColumn.setPreferredWidth(NAME_FIXED_WIDTH);

		m_treeTable.getTableHeader().setReorderingAllowed(false);
		m_treeTable.setShowVerticalLines(true);
	}

	protected TableCellRenderer createDefaultTableHeaderRenderer(int alignment)
	{
		DefaultTableCellRenderer label = new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				if (table != null) {
					JTableHeader header = table.getTableHeader();
					if (header != null) {
						setForeground(header.getForeground());
						setBackground(header.getBackground());
						setFont(header.getFont());
					}
				}
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));

				// Sets the column label text. Adds a leading and trailing space to
				// the text otherwise text is partially obscured by the drawn border
				setText((value == null) ? "" : " " + value.toString() + " ");

				return this;
			}
		};

		label.setHorizontalAlignment(alignment);
		return label;
	}

	protected void setupTree()
	{
		final JTree tree = m_treeTable.getTree();

		tree.setName("ModularConnectorTree");

		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);

		tree.addMouseListener(getMouseListener());
		ToolTipManager.sharedInstance().registerComponent(tree);
		//create and set renderer
		ModularTreeCellRenderer renderer = new ModularTreeCellRenderer();
		tree.setCellRenderer(renderer);
	}

	@Nullable private Icon getNodeIcon(Object value)
	{
		Icon icon = null;
		if (value instanceof ModularConnectorTreeNode) {
			ModularConnectorTreeNode mutableTreeNode = (ModularConnectorTreeNode) value;
			icon = mutableTreeNode.getIcon();
		}
		return icon;
	}

	private class ModularTreeCellEditor extends DefaultCellEditor
	{

		private ModularConnectorTreeModel m_nodeToEdit;
		private JPanel m_editorComponent;
		private JTextField m_textField;

		private ModularTreeCellEditor(JTextField textField, DefaultMutableTreeNode node,
				ModularConnectorTreeModel modConTreeModel)
		{
			super(textField);
			setName("connectorname");
			m_nodeToEdit = modConTreeModel;
			m_textField = textField;
			m_textField.addFocusListener(new FocusAdapter()
			{
				@Override public void focusLost(FocusEvent e)
				{
					if (m_modTreeEditor != null) {
						m_modTreeEditor.stopCellEditing();
					}
				}
			});
			int spacing = 42 + (18 * node.getLevel());

			m_editorComponent = new JPanel(new BorderLayout())
			{
				@Override public void setBounds(int x, int y, int width, int height)
				{
					int newX = x + spacing;
					int newWidth = width - spacing;
					super.setBounds(newX, y, newWidth, height);
				}
			};
			m_editorComponent.add(m_textField, BorderLayout.CENTER);

			setClickCountToStart(2);
		}

		@Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column)
		{
			m_textField.setText(value != null ? value.toString() : "");
			m_textField.setBorder(UIManager.getBorder("TextField.border"));
			SwingUtilities.invokeLater(() -> m_textField.requestFocusInWindow());

			return m_editorComponent;
		}

		@Override public Object getCellEditorValue()
		{
			return m_textField.getText();
		}

		@Override public boolean stopCellEditing()
		{
			if (m_nodeToEdit != null && m_textField != null) {
				TreePath selectionPath = m_treeTable.getTree().getSelectionPath();
				m_nodeToEdit.valueForPathChanged(selectionPath, m_textField.getText().trim());
			}
			m_modTreeEditor = null;
			if (m_treeTable != null && m_treeTable.getTree() != null) {
				m_treeTable.getTree().updateUI();
			}
			return super.stopCellEditing();
		}

		@Override public boolean isCellEditable(EventObject e)
		{
			if (e instanceof MouseEvent) {
				MouseEvent me = (MouseEvent) e;
				if (me.getClickCount() >= 2) {
					return true;
				}
			}
			return false;
		}
	}

	private class ModularTreeCellRenderer extends DefaultTreeCellRenderer
	{

		ModularTreeCellRenderer()
		{
			setOpenIcon(null);
			setClosedIcon(null);
			setLeafIcon(null);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus)
		{
			Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (value instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode mutableTreeNode = (DefaultMutableTreeNode) value;
				IConnectorNode treeNode = (IConnectorNode) mutableTreeNode.getUserObject();
				setToolTipText(treeNode.getMessage(null));
				if (!treeNode.isValid()) {
					comp.setForeground(CHSColors.getErrorForegroundColor());
					comp.setBackground(getBackground());
				}
				Icon icon = getNodeIcon(value);
				setOpenIcon(icon);
				setClosedIcon(icon);
				setLeafIcon(icon);
				setIcon(icon);
			}
			return comp;
		}
	}

	protected void setupTable()
	{
		m_treeTable.setName("ModularConnectorTreeTable");
		m_treeTable.addMouseListener(getMouseListener());
	}

	private MouseAdapter getMouseListener()
	{
		if (m_mouseAdapter == null) {
			m_mouseAdapter = new ModularConnectorTreePanelMouseAdapter();
		}
		return m_mouseAdapter;
	}

	private static class ModularConnectorTreePanelMouseAdapter extends MouseAdapter
	{

		public void mouseClicked(MouseEvent e)
		{
		}
	}

	@Nullable private DefaultMutableTreeNode getNode()
	{
		TreePath selection = m_treeTable.getTree().getSelectionPath();
		m_treeTable.getTree().scrollPathToVisible(selection);

		DefaultMutableTreeNode node = null;
		if (selection != null) {
			node = (DefaultMutableTreeNode) selection.getLastPathComponent();
		}
		return node;
	}

	public class ModularConnectorTreeTable extends JTreeTable
	{

		ModularConnectorTreeTable(ModularConnectorTreeModel treeTableModel)
		{
			super(treeTableModel);
			setName("ModularConnectorTable");
			addListeners();
		}

		@Override public boolean getRowSelectionAllowed()
		{
			return false;
		}

		private void addListeners()
		{
			addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent e)
				{
					DefaultMutableTreeNode selectedNode = getNode();

					if (selectedNode != null) {
						int nodeLevel = selectedNode.getLevel();
						int rowNumber = getSelectedRow();

						int xLeftBound = nodeLevel * 18 + 2;
						int xRightBound = nodeLevel * 18 + 16;
						int yTopBound = rowNumber * 20 + 4;
						int yBottomBound = rowNumber * 20 + 18;

						getTree().setSelectionPath(new TreePath(selectedNode.getPath()));

						if (e.getX() >= xLeftBound && e.getX() < xRightBound && e.getY() >= yTopBound &&
								e.getY() < yBottomBound) {
							SwingUtilities.invokeLater(() -> {
								if (selectedNode instanceof ModularConnectorTreeNode) {
									if (getTree().isExpanded(getTree().getSelectionPath())) {
										getTree().collapsePath(getTree().getSelectionPath());
									}
									else {
										getTree().expandPath(getTree().getSelectionPath());
									}
									if (e.getClickCount() >= 2 && m_modTreeEditor != null) {
										m_modTreeEditor.stopCellEditing();
									}
								}
							});
						}
					}
				}
			});

			addKeyListener(new KeyAdapter()
			{
				@Override public void keyPressed(KeyEvent e)
				{
					DefaultMutableTreeNode selectedNode = getNode();

					if (e.getKeyChar() == '*' || e.getKeyChar() == '/' || e.getKeyChar() == '+' || e.getKeyChar() == '-') {
						SwingUtilities.invokeLater(() -> {
							if (selectedNode instanceof ModularConnectorTreeNode) {
								if (e.getKeyChar() == '*' || e.getKeyChar() == '+') {
									getTree().expandPath(getTree().getSelectionPath());
								}
								else if (e.getKeyChar() == '/' || e.getKeyChar() == '-') {
									getTree().collapsePath(getTree().getSelectionPath());
								}
								getTree().setSelectionPath(new TreePath(selectedNode.getPath()));
							}
						});
					}
				}
			});
		}

		/*
		 We dont want tree column to be selected when some other column is selected.
		 This is problem in case of editable treeTable
		 */
		public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend)
		{
			if (columnIndex == 0) {
				super.changeSelection(rowIndex, columnIndex, toggle, extend);
			}
			else {
				super.clearSelection();
			}
		}

		public TableCellEditor getCellEditor(int row, int column)
		{
			if (row < 0 || column < 0) {
				return null;
			}

			m_modTreeEditor = null;
			TableCellEditor editor = super.getCellEditor(row, column);
			JTree tree = m_treeTable.getTree();
			TreePath selection = tree.getPathForRow(row);

			if (isCellEditable(row, column)) {
				switch (column) {
					case 0:
						if (selection != null) {
							final DefaultMutableTreeNode currentNode =
									(DefaultMutableTreeNode) selection.getLastPathComponent();
							if (currentNode != null) {
								tree.setSelectionPath(new TreePath(currentNode.getPath()));
							}
							if (currentNode != null) {
								if (column == 0) {
									removeEditor();
									final JTextField textField = new JTextField();
									textField.setName("TextField");
									textField.setText((String) m_treeModel.getValueAt(currentNode, 0));

									textField.addKeyListener(new KeyListener()
									{
										@Override public void keyTyped(KeyEvent e)
										{
										}

										public void keyReleased(KeyEvent e)
										{
										}

										@Override public void keyPressed(KeyEvent e)
										{
										}
									});
									m_modTreeEditor = new ModularTreeCellEditor(textField, currentNode, m_treeModel);
									return m_modTreeEditor;
								}
							}
						}

						return editor;

					case 1:
						JCheckBox checkBox = new ModularTableCheckboxCellRenderer();
						checkBox.setBackground(getSelectionBackground());
						return new DefaultCellEditor(checkBox)
						{
							@Override public boolean isCellEditable(EventObject anEvent)
							{
								return isSourceNameGenerated(row);
							}
						};
					default:
						return super.getCellEditor(row, column);
				}
			}
			return super.getCellEditor(row, column);
		}

		public TableCellRenderer getCellRenderer(int row, int column)
		{
			TableCellRenderer renderer;
			if (isCellEditable(row, column)) {

				switch (column) {
					case 0:
						renderer = super.getCellRenderer(row, column);
						break;
					case 1:
						renderer = new ModularTableCheckboxCellRenderer();
						break;
					default:
						renderer = super.getCellRenderer(row, column);
				}
			}
			else {
				renderer = super.getCellRenderer(row, column);
			}
			return renderer;
		}

		public boolean isCellEditable(int row, int column)
		{
			return true;
		}

		@Override protected TreeTableModelAdapter createTableModelAdapter(ITreeTableModel treeTableModel, JTree tree)
		{
			return new TreeTableModelAdapter(treeTableModel, tree)
			{
				@Override protected boolean isValidRowToFireEvent(int row)
				{
					boolean isValidFromSuper = super.isValidRowToFireEvent(row);
					return isValidFromSuper && row != -1;
				}
			};
		}

		public boolean isSourceNameGenerated(int row)
		{
			DefaultMutableTreeNode nodeForRow =
					CommonUtils.cast(m_treeTableModelAdapter.nodeForRow(row), DefaultMutableTreeNode.class);
			assert nodeForRow != null;
			IConnectorNode connectorNode = CommonUtils.cast(nodeForRow.getUserObject(), IConnectorNode.class);
			assert connectorNode != null;
			return connectorNode.isGeneratedName();
		}
	}

	private class ModularTableCheckboxCellRenderer extends JCheckBox implements TableCellRenderer
	{

		ModularTableCheckboxCellRenderer()
		{
			setHorizontalAlignment(CENTER);
			setName("namefixedcheckbox");
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
		{
			ModularTableCheckboxCellRenderer modularTableCheckboxCellRenderer = new ModularTableCheckboxCellRenderer();
			modularTableCheckboxCellRenderer.setBackground((isSelected) ? table.getSelectionBackground() :
					FlatXTableUtils.getBackgroundColorForTableRow(table, row));
			if (isSelected) {
				modularTableCheckboxCellRenderer.setForeground(table.getSelectionForeground());
			}
			else {
				modularTableCheckboxCellRenderer.setForeground(table.getForeground());
			}
			if (value instanceof Boolean) {
				modularTableCheckboxCellRenderer.setSelected((Boolean) value);
			}
			else {
				modularTableCheckboxCellRenderer.setSelected(false);
			}
			boolean sourceNameGenerated = m_treeTable.isSourceNameGenerated(row);
			modularTableCheckboxCellRenderer.setEnabled(sourceNameGenerated);
			modularTableCheckboxCellRenderer.setToolTipText(ResourceMgr.getString(SelectSharedPanel.class,
					sourceNameGenerated ? "SelectSharedPanel.generated.enabled.tooltip" :
							"SelectSharedPanel.generated.disabled.tooltip"));
			return modularTableCheckboxCellRenderer;
		}
	}
}