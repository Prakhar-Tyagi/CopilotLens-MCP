/*
 * Copyright 2010-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.ui;

import chs.caplets.logic.merge.Mergeable;
import chs.caplets.logic.merge.Merger;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicObjectIterator;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.project.IOptionExpression;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.filter.IMatcher;
import chs.utilities.ui.filter.MatchType;
import chs.utilities.ui.filter.PersistedFilterMatchManager;
import chs.utilities.ui.table.TableUtils;
import chs.utility.ui.table.ITableFilter;
import chs.utility.ui.table.TableFilterModel;
import chs.utility.ui.table.TableSorterModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class MergeIntoDialog extends CAFOkCancelDialog
{

	private ILogicObject m_sourceObject;
	private ILogicObject m_selectedLogicObject = null;
	private IFacetConflictResolutionModel m_conflictResolution = null;
	private static final int DIALOG_WIDTH = 800;
	private static final int DIALOG_HEIGHT = 550;
	private static final int DIALOG_MIN_WIDTH = 800;
	private static final int DIALOG_MIN_HEIGHT = 550;
	private static final int RESOLVE_TAB_IDX = 1;

	public MergeIntoDialog(Frame owner, String title, ILogicObject sourceObject)
	{
		super(owner, title, true);
		m_sourceObject = sourceObject;
		initDialog();
	}

	private void initDialog()
	{
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setName("MergeIntoTabbedPane");
		MergeIntoPanel mainPanel = new MergeIntoPanel(m_sourceObject);
		MergeIntoFacetConflictResolutionController conflictResolutionController =
				new MergeIntoFacetConflictResolutionController(m_sourceObject);
		FacetConflictResolutionPanel<ILogicObject> conflictResolutionPanel =
				new FacetConflictResolutionPanel<ILogicObject>(this, conflictResolutionController);
		conflictResolutionController.register(conflictResolutionPanel);
		m_conflictResolution = conflictResolutionController;
		mainPanel.setSelectionListener(new LogicObjectSelectionListener()
		{

			public void selectionChanged(@Nullable Object selection)
			{
				if (selection != null && selection instanceof ILogicObject) {
					getOkButton().setEnabled(true);
					m_selectedLogicObject = (ILogicObject) selection;
				}
				else {
					getOkButton().setEnabled(false);
					m_selectedLogicObject = null;
				}
				conflictResolutionController.targetChanged(m_selectedLogicObject);
				setupResolveTab(tabbedPane, !conflictResolutionController.getTopNodes().isEmpty());
			}
		});
		String selectTitle = ResourceMgr.getString(MergeIntoDialog.class, "MergeIntoPanel.tab.select");
		String resolveTitle = ResourceMgr.getString(MergeIntoDialog.class, "MergeIntoPanel.tab.resolve");
		tabbedPane.addTab(selectTitle, mainPanel);
		tabbedPane.addTab(resolveTitle, conflictResolutionPanel);
		setupResolveTab(tabbedPane, !conflictResolutionController.getTopNodes().isEmpty());

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		getOkButton().setEnabled(false);
		getOkButton().addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				setCancelled(false);
				setVisible(false);
				dispose();
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setCancelled(true);
				setVisible(false);
				dispose();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
	}

	private void setupResolveTab(JTabbedPane tabbedPane, boolean enabled)
	{
		tabbedPane.setEnabledAt(RESOLVE_TAB_IDX, enabled);
		tabbedPane.setToolTipTextAt(RESOLVE_TAB_IDX, ResourceMgr.getString(MergeIntoPanel.class,
				enabled ? "MergeIntoPanel.tab.resolve.tooltip.enable" : "MergeIntoPanel.tab.resolve.tooltip.disable"));
	}

	@Override protected void init()
	{
		super.init();
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
	}

	public ILogicObject getSelectedLogicObject()
	{
		return m_selectedLogicObject;
	}

	@Nullable public IFacetConflictResolutionModel getConflictResolution()
	{
		return m_conflictResolution;
	}

	private static class MergeIntoPanel extends JPanel
	{

		private LogicObjectSelectionListener m_selectionListener = null;

		MergeIntoPanel(ILogicObject sourceObject)
		{
			setName("mergeinto.mainpanel");
			initPanel(sourceObject);
		}

		private void initPanel(ILogicObject sourceObject)
		{
			GridBagLayout bagLayout = new GridBagLayout();
			setLayout(bagLayout);

			JLabel filterLabel =
					new JLabel(
							ResourceMgr.getStringForLabel(MergeIntoDialog.class, "MergeIntoPanel.filter.label.text"));

			// cdixon - we can use the FilterComponent here in place of this JTextField
			final JTextField filterText = new JTextField();
			filterText.setName("mergeinto.filter.text");

			JScrollPane scrollPane;
			final TableFilterModel filteredAndSortedTableModel = createTableModel(sourceObject);
			if (filteredAndSortedTableModel.getRowCount() == 0) {
				scrollPane = createNoObjectLabel(sourceObject);
			}
			else {
				scrollPane = createTable(filterText, filteredAndSortedTableModel);
			}

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(3, 3, 3, 3);
			gbc.gridx = 0;
			gbc.gridy = 0;
			add(filterLabel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.BOTH;
			add(filterText, gbc);

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			add(scrollPane, gbc);
		}

		private JScrollPane createNoObjectLabel(ILogicObject sourceObject)
		{
			JLabel noObjectsLabel =
					new JLabel(ResourceMgr.getString(MergeIntoDialog.class, "MergeIntoPanel.noobjects.text",
							COFTypeEnum.from_object(sourceObject).toString()));
			noObjectsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			noObjectsLabel.setFont(noObjectsLabel.getFont().deriveFont(Font.ITALIC));
			return new JScrollPane(noObjectsLabel);
		}

		private JScrollPane createTable(final JTextField filterText, final TableFilterModel tableModel)
		{
			final JTable objectsTable = new JTable(tableModel)
			{
				@Override public boolean isCellEditable(int row, int column)
				{
					return false;
				}
			};
			objectsTable.setName("mergeinto.list");
			((TableSorterModel) tableModel.getModel()).setupTableHeader(objectsTable);
			((TableSorterModel) tableModel.getModel()).sortByColumn(0);
			objectsTable.setDefaultRenderer(objectsTable.getColumnClass(0), new DefaultTableCellRenderer()
			{

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column)
				{
					super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					setEnabled(true);

					Mergeable possibility = ((LogicObjectProxy) value).getMergePossibility();
					if (possibility != null && possibility != Mergeable.Possible) {
						setEnabled(false);
					}
					if (possibility == Mergeable.Possible) {
						setToolTipText(getTooltipForMergeableTarget(((LogicObjectProxy) value).getLogicObject()));
					}
					else {
						setToolTipText(possibility == null ? "" : possibility.getReason());
					}
					//noinspection ReturnOfThis
					return this;
				}

				@Nullable private String getTooltipForMergeableTarget(ILogicObject logicObject)
				{
					StringBuilder builder = new StringBuilder();

					// Show diagram names where the logic object is, in the tooltip
					ILogicDesign logicDesign = (ILogicDesign) logicObject.getDesignContainer();
					List<String> diagramNames = new ArrayList<String>(2);
					for (IDesignSharedUsage designSharedUsage : logicDesign.getDesignWideUsageMgr()
							.getUsages(logicObject)) {
						if (!diagramNames.contains(designSharedUsage.getDiagramName())) {
							if (diagramNames.size() == 2) {
								diagramNames.add("...");
								break;
							}
							else {
								diagramNames.add(designSharedUsage.getDiagramName());
							}
						}
					}

					String diagramNamesString = StringUtils.convertIteratorToString(diagramNames.iterator(), ", ");
					if (!StringUtils.isBlank(diagramNamesString)) {
						builder.append(createRow(ResourceMgr.getStringForLabel(MergeIntoDialog.class,
								"MergeIntoPanel.tooltip.diagrams", diagramNames.size() > 1 ? "s" : ""),
								diagramNamesString));
					}

					// Then show the object's short description if it has got one
					String shortDesc = logicObject.getShortDescription();
					builder.append(StringUtils.isEmpty(shortDesc) ? "" : createRow(
							ResourceMgr.getStringForLabel(MergeIntoDialog.class,
									"MergeIntoPanel.tooltip.shortdescription"),
							shortDesc));

					// Show the Option Expression as well, if there's one
					IOptionExpression optionExpression = logicObject.getOptionExpression();
					builder.append(optionExpression == null || StringUtils.isEmpty(optionExpression.getExpression()) ?
							"" : createRow(
							ResourceMgr.getStringForLabel(MergeIntoDialog.class,
									"MergeIntoPanel.tooltip.optionexpression"),
							optionExpression.getExpression()));

					//Wrap the built table rows in a Table HTML
					if (builder.length() > 0) {
						builder.insert(0, "<html><table border=0>");
						builder.append("</html></html>");
						return builder.toString();
					}
					else {
						return null;
					}
				}

				private String createRow(String label, String value)
				{
					StringBuilder builder = new StringBuilder();
					builder.append("<tr>");
					builder.append("<td>").append("<b>").append(label).append("</b>").append("</td>");
					builder.append("<td>").append(value).append("</td>");
					builder.append("</tr>");
					return builder.toString();
				}
			});
			JScrollPane scrollPane = new JScrollPane(objectsTable);

			objectsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			objectsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{

				public void valueChanged(ListSelectionEvent e)
				{
					if (e.getValueIsAdjusting()) {
						return;
					}
					int selectedRow = objectsTable.getSelectedRow();

					if (selectedRow > -1) {
						LogicObjectProxy objectProxy = (LogicObjectProxy) tableModel.getValueAt(selectedRow, 0);
						Mergeable mergeable = objectProxy.getMergePossibility();
						if (mergeable != Mergeable.Possible) {
							m_selectionListener.selectionChanged(null);
						}
						else {
							m_selectionListener.selectionChanged(objectProxy.getLogicObject());
						}
					}
					else {
						m_selectionListener.selectionChanged(null);
					}
				}
			});

			filterText.getDocument().addDocumentListener(new DocumentListener()
			{
				public void insertUpdate(DocumentEvent e)
				{
					changedUpdate(e);
				}

				public void removeUpdate(DocumentEvent e)
				{
					changedUpdate(e);
				}

				public void changedUpdate(DocumentEvent e)
				{
					int selectedIndex = objectsTable.getSelectedRow();
					tableModel.filter(new ITableFilter()
					{

						public boolean accept(TableModel tableModel, int row)
						{
							String text = filterText.getText();
							filterText.setForeground(Color.BLACK);
							if (text != null && !text.isEmpty()) {

								// cdixon - maintains REGEX but could be made to use preferred match...
								IMatcher matcher = PersistedFilterMatchManager.getInstance().buildMatcher(text,
										MatchType.REGEX);

								if (matcher != null) {
									LogicObjectProxy objectProxy = (LogicObjectProxy) tableModel.getValueAt(row, 0);
									return matcher.isMatch(objectProxy.toString());
								}

								filterText.setForeground(Color.RED);
								return true;
							}
							else {
								return true;
							}
						}
					});

					if (selectedIndex >= 0 && selectedIndex < objectsTable.getRowCount()) {
						objectsTable.getSelectionModel().setSelectionInterval(0, selectedIndex);
					}
				}
			});

			filterText.addKeyListener(new KeyListener()
			{

				public void keyTyped(KeyEvent e)
				{
				}

				public void keyPressed(KeyEvent e)
				{
					int count = objectsTable.getModel().getRowCount();
					if (count == 0) {
						return;
					}
					ListSelectionModel selectionModel = objectsTable.getSelectionModel();
					int selectionIndex = selectionModel.getMinSelectionIndex();
					if (selectionIndex < 0) {
						selectionIndex = -1;
					}
					if (e.getKeyCode() == KeyEvent.VK_UP && selectionIndex > 0) {

						selectionModel.setSelectionInterval(selectionIndex - 1, selectionIndex - 1);
					}
					else if (e.getKeyCode() == KeyEvent.VK_DOWN && selectionIndex < count - 1) {
						selectionModel.setSelectionInterval(selectionIndex + 1, selectionIndex + 1);
					}
				}

				public void keyReleased(KeyEvent e)
				{
				}
			});

			TableUtils.setVisibleRowCount(objectsTable, 10);
			return scrollPane;
		}

		private TableFilterModel createTableModel(ILogicObject sourceObject)
		{
			assert sourceObject != null : "Null source object";

			ILogicDesign designContainer = (ILogicDesign) sourceObject.getDesignContainer();
			assert designContainer != null : "Null design";

			IConnectivity connectivity = designContainer.getConnectivity();
			assert connectivity != null : "Null connectivity";

			String objectTypeString = COFTypeEnum.from_object(sourceObject).toString();
			if (sourceObject instanceof IGenericInlineConnector) {
				objectTypeString = COFTypeEnum.Inline.toString();
			}
			String headerString =
					ResourceMgr.getString(MergeIntoDialog.class, "MergeIntoPanel.table.header.text", objectTypeString);
			DefaultTableModel tableModel = new DefaultTableModel(new Object[]{headerString}, 0);

			ILogicObjectIterator allObjects = connectivity.getObjects();
			while (allObjects.hasNext()) {
				ILogicObject object = allObjects.getNext();
				if (shouldCheckForMergePossibility(sourceObject, object)) {
					Mergeable possibility = Merger.areMergeable(sourceObject, object);
					tableModel.addRow(new Object[]{new LogicObjectProxy(object, possibility)});
				}
			}

			MergeDialogTableSorterModel sortedTableModel =
					new MergeDialogTableSorterModel(tableModel);
			TableFilterModel filteredAndSortedTableModel = new TableFilterModel(sortedTableModel);
			return filteredAndSortedTableModel;
		}

		private boolean shouldCheckForMergePossibility(@NotNull ILogicObject sourceObject,
				@NotNull ILogicObject otherObject)
		{
			if (otherObject.getClass() == sourceObject.getClass() && otherObject != sourceObject) {
				boolean isSourceObjectARingTerminal =
						(sourceObject instanceof IConnector && ((IConnector) sourceObject).isRingTerminal());
				boolean isOtherObjectARingTerminal =
						(otherObject instanceof IConnector && ((IConnector) otherObject).isRingTerminal());

				//Return false if only one of the objects is a ring terminal
				return (isOtherObjectARingTerminal == isSourceObjectARingTerminal);
			}
			return false;
		}

		public void setSelectionListener(LogicObjectSelectionListener logicObjectSelectionListener)
		{
			m_selectionListener = logicObjectSelectionListener;
		}

		private static class LogicObjectProxy
		{

			private ILogicObject m_logicObject;
			private Mergeable m_status;

			LogicObjectProxy(ILogicObject logicObject, Mergeable status)
			{
				m_logicObject = logicObject;
				m_status = status;
			}

			@Override
			public String toString()
			{
				StringBuilder displayname = new StringBuilder();
				if (m_logicObject instanceof IGenericInlineConnector) {
					IGenericInlineConnector matedConnector =
							((IGenericInlineConnector) m_logicObject).getMatedInlines().iterator().next();
					if (((IConnector) m_logicObject).isPlug()) {
						displayname.append(m_logicObject.getName());
						displayname.append("::");
						displayname.append(matedConnector.getName());
					}
					else {
						displayname.append(matedConnector.getName());
						displayname.append("::");
						displayname.append(m_logicObject.getName());
					}
				}
				else {
					displayname = new StringBuilder(m_logicObject.getName());
				}
				return displayname.toString();
			}

			public Mergeable getMergePossibility()
			{
				return m_status;
			}

			public ILogicObject getLogicObject()
			{
				return m_logicObject;
			}
		}
	}

	private interface LogicObjectSelectionListener
	{

		void selectionChanged(@Nullable Object selection);
	}

	private static class MergeDialogTableSorterModel extends TableSorterModel
	{

		private static final AlphaNumComparator<String> m_alphaNumComparator =
				new AlphaNumComparator<String>(true, true);

		private MergeDialogTableSorterModel(TableModel model)
		{
			super(model);
		}

		@Override protected int compareAlphaNumeric(String s1, String s2)
		{
			return m_alphaNumComparator.compare(s1, s2);
		}
	}
}