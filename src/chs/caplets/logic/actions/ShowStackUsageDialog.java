package chs.caplets.logic.actions;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IDesignContainer;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUIDObject;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.CHSColors;
import chs.utility.ui.IconUtils;
import chs.utility.ui.table.TableSorterModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;

/**
 * Created by IntelliJ IDEA. User: creddy Date: May 18, 2011 Time: 4:53:09 PM To change this template use File |
 * Settings | File Templates.
 */
public class ShowStackUsageDialog extends CAFOkCancelDialog
{

	private List<StackedObjectUsageData> m_stackedObjectUsageDatas = null;
	private List<StackedObjectUsageData> m_interfacedBlockData = null;
	@Nullable private IDiagramObject m_stackedObject = null;

	private static final int DIALOG_SIZE = 200;
	//	private static final int PREF_HEIGHT = 300;
	private static final Dimension JCOMP_PREF_DIM = new Dimension(2 * DIALOG_SIZE, DIALOG_SIZE);

	public ShowStackUsageDialog(Frame frame, List<StackedObjectUsageData> stackedObjectUsageData,
			@Nullable IDiagramObject stackedObject,
			List<StackedObjectUsageData> interfacedBlockData)
	{
		super(frame, getTheTitle(stackedObject), true);
		m_stackedObjectUsageDatas = stackedObjectUsageData;
		m_interfacedBlockData = interfacedBlockData;
		m_stackedObject = stackedObject;
		setName("ShowStackUsageDialog");
		rememberSize(false);
		addComponents();
		hookupComponents();
	}

	public void showData()
	{
		pack();
		setVisible(true);
	}

	@Nullable public IDiagramObject getStackedObject()
	{
		return m_stackedObject;
	}

	private void addComponents()
	{
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setName("ShowStackUsagesTabbedPane");
		tabbedPane.setToolTipText("");
		tabbedPane.setPreferredSize(JCOMP_PREF_DIM);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		ViewUsagesPanel usagesPanel;
		String tabName;
		char tabMnemonic;
		String designType = getDesignType(m_stackedObject);

		if (m_stackedObject instanceof ISchemStackPin) {
			tabName = ResourceMgr.getString(ShowStackUsageDialog.class, "ShowStackUsageDialog.stackpin.text");
			tabMnemonic = ResourceMgr.getMnemonic(ShowStackUsageDialog.class, "ShowStackUsageDialog.stackpin.mnemonic");
			usagesPanel = new ViewUsagesPanel(m_stackedObjectUsageDatas, m_stackedObject, PanelType.STACK_PIN,
					designType);
			addTab(tabbedPane, usagesPanel, tabName, tabMnemonic/*, tabToolTip*/);
		}
		else {
			if ((m_stackedObjectUsageDatas.isEmpty() && m_interfacedBlockData.isEmpty()) ||
					!m_stackedObjectUsageDatas.isEmpty()) {
				tabName = ResourceMgr.getString(ShowStackUsageDialog.class, "ShowStackUsageDialog."+designType+"conductors.text");
				tabMnemonic =
						ResourceMgr.getMnemonic(ShowStackUsageDialog.class, "ShowStackUsageDialog.conductors.mnemonic");
				usagesPanel = new ViewUsagesPanel(m_stackedObjectUsageDatas, m_stackedObject, PanelType.CONDUCTOR, designType);
				addTab(tabbedPane, usagesPanel, tabName, tabMnemonic/*, tabToolTip*/);
			}

			if (!m_interfacedBlockData.isEmpty()) {
				tabName =
						ResourceMgr.getString(ShowStackUsageDialog.class, "ShowStackUsageDialog."+designType+"blockconductors.text");
				tabMnemonic = ResourceMgr
						.getMnemonic(ShowStackUsageDialog.class, "ShowStackUsageDialog.blockconductors.mnemonic");
				usagesPanel =
						new ViewUsagesPanel(m_interfacedBlockData, m_stackedObject, PanelType.BLOCK_CONDUCTOR,
								designType);
				addTab(tabbedPane, usagesPanel, tabName, tabMnemonic/*, tabToolTip*/);
			}
		}
	}
	private static String getDesignType(@Nullable IDiagramObject stackedObject)
	{
		String designContainer =  DesignType.LOGIC.getName();
		if (stackedObject == null) {
			return designContainer;
		}

		if (stackedObject instanceof ISchemStackPin) {
			IPinList pl = (IPinList) stackedObject.getParent();
			ISchemDiagram diagram = null;
			if (pl != null) {
				diagram = pl.getDiagram();
			}
			if(diagram != null){
				IDesignContainer design = diagram.getDesign();
				if(design != null) {
					designContainer =  design.getDesignType().getName();
				}
			}
		}
		else if(stackedObject instanceof IConnectivityRef){
			designContainer = Objects.requireNonNull(
					((IConnectivityRef) stackedObject).getConnectivity().getDesignContainer()).getDesignType().getName();
		}

		return designContainer;
	}

	private void addTab(JTabbedPane tabbedPane, ViewUsagesPanel usagesPanel, String tabName,
			char tabMnemonic/*, String tabToolTip*/)
	{
		tabbedPane.add(tabName, usagesPanel);
		tabbedPane.setMnemonicAt(tabbedPane.getTabCount() - 1, tabMnemonic);
//		tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, tabToolTip);
		usagesPanel.setBorder(null);
	}

	@Override protected boolean keepCancelButton()
	{
		return false;
	}

	private void hookupComponents()
	{
		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		});
	}

	private static String getTheTitle(@Nullable IDiagramObject stackedObject)
	{
		StringBuilder stackedObjName = new StringBuilder();
		if (stackedObject instanceof ISchemStackPin) {
			IPinList pl = (IPinList) stackedObject.getParent();
			assert pl != null;
			stackedObjName.append(pl.getConnectivity().getName());
			stackedObjName.append(" {");
			stackedObjName.append(((IReadOnlyNamedObject) stackedObject).getName());
			stackedObjName.append("}");
		}
		else {
			IHighway highway = null;
			if (stackedObject != null) {
				highway = (IHighway) ((IConnectivityRef) stackedObject).getConnectivity();
				stackedObjName.append(highway.getName());
			}
		}

		return ResourceMgr.getString(ShowStackUsageDialog.class, "ShowStackUsageDialog.stackpin.title",
				stackedObjName.toString());
	}

	private enum PanelType
	{
		STACK_PIN,
		CONDUCTOR,
		BLOCK_CONDUCTOR
	}

	public static class ViewUsagesPanel extends JPanel
	{

		private static final int PIN_STATE_COLUMN = 0;
		private static final int PIN_STATE_COLUMN_WIDTH = 20;

		private DefaultTableModel usageTableModel;
		private JTable usageTable;
		private List<StackedObjectUsageData> m_usageData = null;
		@Nullable private IDiagramObject m_stackedObject = null;
		private PanelType m_panelType;
		private String m_designType;

		public ViewUsagesPanel(List<StackedObjectUsageData> usageData, @Nullable IDiagramObject stackedObject,
				PanelType panelType, String designType)

		{
			m_usageData = usageData;
			m_stackedObject = stackedObject;
			m_panelType = panelType;
			m_designType = designType;

			addComponents();
			init();
		}

		private void addComponents()
		{
			setLayout(new BorderLayout());
			add(stackPinPanel());
		}

		private JPanel stackPinPanel()
		{
			Object[] colNames;
			String usageTableName = "list_stackedpins_usages";
			if (m_panelType == PanelType.STACK_PIN) {
				colNames = new Object[]{
						"", // empty column header - icon will be set by renderer later
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel."+m_designType+"usagestable.pin"),
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel."+m_designType+"usagestable.conductor"),
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel.usagestable.target")};
			}
			else if (m_panelType == PanelType.CONDUCTOR) {
				colNames = new Object[]{
						"", // empty column header - icon will be set by renderer later
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel."+m_designType+"usagestable.conductor"),
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel."+m_designType+"usagestable.end1")};
				usageTableName = "list_conductor_usages";
			}
			else /*if (m_panelType == PanelType.BLOCK_CONDUCTOR)*/ {
				colNames = new Object[]{
						"", // empty column header - icon will be set by renderer later
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel."+m_designType+"usagestable.conductor"),
						ResourceMgr.getString(ViewUsagesPanel.class, "ViewUsagesPanel.usagestable.connectedblocks")};
				usageTableName = "list_blockconductor_usages";
			}

			usageTableModel = new DefaultTableModel(colNames, 0);
			usageTable = new JTable()
			{
				public boolean isCellEditable(int row, int column)
				{
					return false;
				}
			};
			usageTable.setName(usageTableName);
			usageTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			usageTable.getSelectionModel().addListSelectionListener(
					new ListSelectionListener()
					{
						public void valueChanged(ListSelectionEvent e)
						{
							//highlightPinsOnSymbolView();
						}
					}
			);
			JScrollPane listScrollPane = new JScrollPane(usageTable);
			listScrollPane.setPreferredSize(JCOMP_PREF_DIM);
			listScrollPane.setMinimumSize(JCOMP_PREF_DIM);

			JPanel pinlistPanel = new JPanel(new BorderLayout());
			pinlistPanel.add(listScrollPane, BorderLayout.CENTER);
			pinlistPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
			return pinlistPanel;
		}

		private void init()
		{
			initUsagePanel();
		}

		private void initUsagePanel()
		{
			assert usageTableModel.getRowCount() == 0;

			if (m_usageData == null) {
				return;
			}

			for (StackedObjectUsageData usage : m_usageData) {
				if (m_stackedObject instanceof ISchemStackPin) {
					usageTableModel.addRow(new Object[]{usage.getIcon(), usage.getSource(), usage.getCarrier(),
							usage.getTarget()});
				}
				else {
					usageTableModel.addRow(new Object[]{usage.getIcon(), usage.getCarrier(), usage.getSource()});
				}
			}
			TableSorterModel sortedModel = new TableSorterModel(usageTableModel);
			usageTable.setModel(sortedModel);
			sortedModel.setupTableHeader(usageTable);
//			sortedModel.sortByColumn(1);

			TableColumn pinStateColumn = usageTable.getTableHeader().getColumnModel().getColumn(PIN_STATE_COLUMN);
			pinStateColumn.setMaxWidth(PIN_STATE_COLUMN_WIDTH);

			pinStateColumn.setCellRenderer(new DefaultTableCellRenderer()
			{
				// implements javax.swing.table.TableCellRenderer
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus,
						int row, int column)
				{
					if (value instanceof Icon) {
						setIcon((Icon) value);
						setText(null);
						setHorizontalAlignment(CENTER);
					}
					if (isSelected) {
						setForeground(table.getSelectionForeground());
						setBackground(table.getSelectionBackground());
					}
					else {
						setForeground(table.getForeground());
						if (row % 2 != 0) {
							setBackground(CHSColors.getStripeBackgroundColor());
						}
						else {
							setBackground(UIManager.getColor("Table.background"));
						}
					}
					setEnabled(table.isEnabled());
					setOpaque(true);
					//noinspection ReturnOfThis
					return this;
				}
			});
		}
	}

	public static class StackedObjectUsageData
	{

		private IUIDObject m_sourceObject;
		private String m_source;
		private String m_carrier;
		private String m_target;
		private int m_sortOrder = 0;

		public StackedObjectUsageData(IUIDObject sourceObj, String source, String target, String carrier, int sortOrder)
		{
			m_sourceObject = sourceObj;
			m_carrier = carrier;
			m_source = source;
			m_target = target;
			m_sortOrder = sortOrder;
		}

		public String getCarrier()
		{
			return m_carrier;
		}

		public String getSource()
		{
			return m_source;
		}

		public IUIDObject getSourceObject()
		{
			return m_sourceObject;
		}

		public String getTarget()
		{
			return m_target;
		}

		public int getSortOrder()
		{
			return m_sortOrder;
		}

		@Nullable
		private Icon getIcon()
		{
			return IconUtils.getIcon(m_sourceObject);
		}
	}

	public void windowClosing(WindowEvent e)
	{
		storeGeometry();
		setVisible(false);
	}
}
