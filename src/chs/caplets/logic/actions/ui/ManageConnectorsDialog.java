/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */
package chs.caplets.logic.actions.ui;

import chs.caf.helpers.GfxViewHelper;
import chs.caplets.logic.actions.ConductorConnectionChanger;
import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.caplets.logic.actions.ManageConnectorDesignScope;
import chs.caplets.logic.actions.ManageConnectorItemChangeListenerProvider;
import chs.caplets.logic.actions.ManageConnectorPinDuplicationFinder;
import chs.caplets.logic.actions.ManageConnectorPinSelections;
import chs.caplets.logic.actions.ManageConnectorPlugMapHighlighter;
import chs.caplets.logic.actions.ManageConnectorsAction;
import chs.caplets.logic.actions.PinListAddPinHelper;
import chs.cof.library.IFootprintable;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.ISymbolDef;
import chs.common.IDesignDescriptor;
import chs.common.IMultiSymbolledPinlist;
import chs.common.IProperty;
import chs.common.ISystemPreferenceMgr;
import chs.common.IUID;
import chs.common.PreferenceContext;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.ctf.caf.utils.IConductorProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.images.CHSImageLoader;
import chs.services.gfx.GfxView;
import chs.system.CHSSystemMgr;
import chs.system.FactoryMgr;
import chs.utilities.LinkedHybridSet;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.BasicUIFactory;
import chs.utility.SymbolUtils;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.PinUtils;
import chs.utility.ui.ConnectorSymbolViewModel;
import chs.utility.ui.ConnectorViewPanel;
import chs.utility.ui.FaceViewSymbol;
import chs.utility.ui.LogicPinTable;
import chs.utility.ui.OkCancelDialog;
import chs.utility.ui.PaneSplitter;
import chs.utility.ui.PinConductorConnectionSortHelper;
import chs.utility.ui.PinListFaceViewModel;
import chs.utility.ui.PinProxyToSymbolPinNameCache;
import chs.utility.ui.PinTableColumnChooser;
import chs.utility.ui.PinTableColumnProvider;
import chs.utility.ui.RemoveMenuItemProvider;
import chs.utility.ui.pintable.ColumnChooserObjectType;
import chs.utility.ui.pintable.ColumnCreationParams;
import chs.utility.ui.pintable.DeviceColumnInformationCreatorFactory;
import chs.utility.ui.pintable.IPinTableColumnInfo;
import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ITableSelectionListener;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.SelectionPreferences;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import com.mentor.capital.javafx.table.cell.CapitalTableCell;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import com.mentor.capital.javafx.table.common.TableFeature;
import com.mentor.capital.javafx.table.helpers.IgnoreEscapeKeyListener;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ManageConnectorsDialog extends SimpleOkCancelDialog
{

	public static final int DIALOG_WIDTH = 1000;
	public static final int DIALOG_HEIGHT = 400;
	public static final int ADD_BTN_WIDTH = 30;
	public static final int ADD_BTN_HEIGHT = 30;
	public static final String PIN_NAME = "Pin Name";
	public static final String WIRE_NAME = "Wire Name";
	public static final String MANAGE_CONNECTORS_TABLE_ID = "Manage Connectors";
	public static final String MANAGE_DEVICES_TABLE_ID = "Manage Devices";

	protected chs.cof.logical.cable.IPinList m_pinList;
	protected TableDataStorage<ManageConnectorConnectionsInfo> tableDataStorage;
	protected ManageConnectorItemChangeListenerProvider itemChangeListenerProvider;
	protected ConductorConnectionChanger mConductorConnectionChanger;
	protected Table<ManageConnectorConnectionsInfo> table;
	protected SortHelperProvider sortHelperProvider;
	protected ISharedPinList mSharedPinList;
	protected ManageConnectorDesignScope mDesignsInScope;
	private JCheckBox editSharedDetails;
	private WindowAdapter windowAdapter;
	private BiConsumer<Collection<String>, ManageConnectorPinDuplicationFinder.DuplicateReason> buttonStateHandler;
	protected List<String> mandatoryColumnNames =
			new ArrayList<>(Arrays.asList(
					PIN_NAME,
					WIRE_NAME,
					IPinTableColumnInfo.PINICON));

	private Function<ManageConnectorsDialog, RowData> mRowDataSupplier = dialog -> new RowData(dialog)
			.setPinList(m_pinList)
			.setDesignsInScope(mDesignsInScope)
			.setSharedPinList(mSharedPinList);
	protected Function<ColumnsToBeAdded.ColumnsToBeAddParameters, ColumnsToBeAdded> mColumnsToBeAddedSupplier =
			parameters -> new ColumnsToBeAdded(parameters);
	// Maps the pin proxies to the UIDS of the diagrams they are used on, used by the zoom mechanism to determine
	// whether or not a particular selection can be zoomed. Populated on construction so we do not query usages from
	// the JavaFX thread.
	private SetMap<IPinProxy, IUID> mPinDiagramUsages = new SetMap<>();
	protected ManageConnectorsAction.LockSharedObjects mLockHelper;
	private PinProxyToSymbolPinNameCache pinProxyToSymbolPinNameCache = new PinProxyToSymbolPinNameCache();

	public ManageConnectorsDialog(@Nullable Frame owner, @Nullable String title, boolean modal,
			chs.cof.logical.cable.IPinList givenPinList, ConductorConnectionChanger conductorConnectionChanger,
			ManageConnectorDesignScope designsInScope)
	{
		super(null, owner, title, modal);
		mConductorConnectionChanger = conductorConnectionChanger;
		m_pinList = givenPinList;
		mDesignsInScope = designsInScope;
	}

	public ManageConnectorsDialog(@Nullable Frame owner, @Nullable String title, boolean modal,
			ISharedPinList givenSharedConnector, ConductorConnectionChanger conductorConnectionChanger,
			ManageConnectorDesignScope designsInScops, ManageConnectorsAction.LockSharedObjects lockHelper)
	{
		super(null, owner, title, modal);
		mConductorConnectionChanger = conductorConnectionChanger;
		mSharedPinList = givenSharedConnector;
		mDesignsInScope = designsInScops;
		mLockHelper = lockHelper;
	}

	public ManageConnectorsDialog setRowDataSupplier(@NotNull Function<ManageConnectorsDialog, RowData> supplier)
	{
		mRowDataSupplier = supplier;
		return this;
	}

	public ManageConnectorsDialog setColumnsToBeAddedSupplier(
			@NotNull Function<ColumnsToBeAdded.ColumnsToBeAddParameters, ColumnsToBeAdded> supplier)
	{
		mColumnsToBeAddedSupplier = supplier;
		return this;
	}

	public ManageConnectorsDialog setSharedInline(@NotNull ISharedPinList sharedConnector)
	{
		m_pinList = null;
		mSharedPinList = sharedConnector;
		return this;
	}

	protected OkCancelDialog createDialog()
	{
		rememberSize(true);
		setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

		ManageConnectorPinSelections manageConnectorPinSelections =
				mSharedPinList == null ? new ManageConnectorPinSelections(m_pinList, mDesignsInScope) :
						new ManageConnectorPinSelections(mSharedPinList, mDesignsInScope);

		//noinspection ParameterNameDiffersFromOverriddenParameter
		buttonStateHandler = new BiConsumer<Collection<String>, ManageConnectorPinDuplicationFinder.DuplicateReason>()
		{
			@Override
			public void accept(Collection<String> s,
					ManageConnectorPinDuplicationFinder.DuplicateReason duplicateReason)
			{
				if (s.isEmpty() ||
						duplicateReason.equals(ManageConnectorPinDuplicationFinder.DuplicateReason.None)) {
					getOkButton().setEnabled(true);
					getOkButton().setToolTipText(null);
				}
				else {
					getOkButton().setEnabled(false);

					getOkButton().setToolTipText(duplicateReason.toString());
				}
			}
		};
		ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder =
				new ManageConnectorPinDuplicationFinder(manageConnectorPinSelections, buttonStateHandler);

		Collection<ManageConnectorConnectionsInfo> data = createData(manageConnectorPinSelections);
		new ManageConnectorsConnectionsData().populatePinDiagramUsages(data, mSharedPinList, mPinDiagramUsages);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		//panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		JPanel tablePanel = createTablePanel(manageConnectorPinSelections, manageConnectorPinDuplicationFinder, data);
		tablePanel.setMinimumSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));
		tablePanel.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));
		ConnectorViewPanel<String> plugMapPanel = createPlugMapPanel(data);

		if (plugMapPanel != null) {
			Platform.runLater(getPlugMapTask(plugMapPanel, manageConnectorPinSelections));
		}

		Platform.runLater(() -> table.addHandler(KeyEvent.KEY_PRESSED, e -> {
			tableKeyPressed(e, plugMapPanel, this);
		}));

		if (plugMapPanel != null) {
			plugMapPanel.setName("PlugmapPanel");
			plugMapPanel.setMinimumSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));
			plugMapPanel.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));

			addComponentListener(componentListener(plugMapPanel));

			final PaneSplitter paneSplitter = new PaneSplitter().setPreferenceClass(getClass());
			panel.add(paneSplitter.createSplitter(tablePanel, plugMapPanel));

			windowAdapter = paneSplitter.getWindowAdapter();
			addWindowListener(windowAdapter);
		}
		else {
			panel.add(tablePanel);
		}

		if (!mDesignsInScope.areAllEditsConsidered() && !mDesignsInScope.isReadonly()) {
			JPanel checkBoxPanel = createEditSharedDetailsCheckBox();
			panel.add(checkBoxPanel, BorderLayout.SOUTH);
		}
		if (mDesignsInScope.isReadonly()) {
			getOkButton().setEnabled(false);
			getOkButton().setToolTipText(ResourceMgr.getString(ManageConnectorsAction.class,
					"ManageConnectorsAction.dialog.okdisablewhennotpartofactiveBL"));
		}
		getContentPane().add(panel);
		getRootPane().setDefaultButton(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setNameExplicitly("ManageConnectorDialog");
		rememberSize(true);
		return this;
	}

	protected WindowAdapter getWindowClosingHandler()
	{
		return windowAdapter;
	}

	protected BiConsumer<Collection<String>, ManageConnectorPinDuplicationFinder.DuplicateReason> getButtonStateHandler()
	{
		return buttonStateHandler;
	}

	@Override protected void prepareToApplyEdits()
	{
		tableDataStorage.apply();
	}

	public Runnable getPlugMapTask(ConnectorViewPanel<String> plugMapPanel,
			ManageConnectorPinSelections manageConnectorPinSelections)
	{
		return new Runnable()
		{
			@Override public void run()
			{
				if (plugMapPanel != null) {
					table.addSelectionListener(getSelectionListener(plugMapPanel));
					Collection<String> connectedPins = new LinkedHashSet<>();
					for (IPinProxy aPin : manageConnectorPinSelections.getPins()) {
						if (!getConductors(aPin).isEmpty()) {
							connectedPins.add(aPin.getName());
						}
					}
					ManageConnectorPlugMapHighlighter manageConnectorPlugMapHighlighter =
							new ManageConnectorPlugMapHighlighter(manageConnectorPinSelections, connectedPins);
					plugMapPanel.setPinConnectionChangeInfoProvider(manageConnectorPlugMapHighlighter);
					itemChangeListenerProvider.addPlugmapHighlighter(manageConnectorPlugMapHighlighter);
				}
			}
		};
	}

	@NotNull public ITableSelectionListener<ManageConnectorConnectionsInfo> getSelectionListener(
			final ConnectorViewPanel<String> plugMapPanel)
	{
		return new ITableSelectionListener<ManageConnectorConnectionsInfo>()
		{
			@Override public void changed(Selection<ManageConnectorConnectionsInfo> selection, int lastSelectionIndex)
			{
				highlightPinOnPlugPanel(plugMapPanel, selection);
			}
		};
	}

	public void highlightPinOnPlugPanel(ConnectorViewPanel<String> connectorView,
			Selection<ManageConnectorConnectionsInfo> selection)
	{
		List<String> pinNames = new ArrayList<>();
		for (CellSelection<ManageConnectorConnectionsInfo> c : selection.getSelectedCells()) {
			ManageConnectorConnectionsInfo item = c.getSelectedItem();
			item.applyOnPin(pinProxy -> {
				pinNames.add(getPinNameToMatch(pinProxy, connectorView));
				return pinProxy.getName();
			});
		}
		if (connectorView != null) {

			connectorView.highlightPinsOnSymbolView(pinNames);
		}
	}

	@NotNull
	private String getPinNameToMatch(@NotNull IPinProxy pinProxy, @Nullable ConnectorViewPanel<String> connectorView)
	{
		if (connectorView != null && connectorView.getConnectorViewModel() != null) {
			ISymbolDef symbolDef = connectorView.getConnectorViewModel().getSymbolDef();
			if (symbolDef != null) {
				return pinProxyToSymbolPinNameCache.getName(symbolDef, pinProxy, pinProxy.getName());
			}
		}
		return pinProxy.getName();
	}

	public void tableKeyPressed(KeyEvent e, @Nullable ConnectorViewPanel<String> plugMapPanel, OkCancelDialog dialog)
	{
		if (KeyCode.Z.equals(e.getCode())) {
			zoomToSelectedOnSymbolView(plugMapPanel);
		}
		else if (KeyCode.ESCAPE.equals(e.getCode())) {
			dialog.getCancelButton().doClick();
		}
	}

	public void zoomToSelectedOnSymbolView(@Nullable ConnectorViewPanel<String> plugMapPanel)
	{
		if (plugMapPanel != null) {
			plugMapPanel.zoomToSelectedOnSymbolView();
		}
	}

	@Nullable public ConnectorViewPanel<String> createPlugMapPanel(
			Collection<ManageConnectorConnectionsInfo> data)
	{

		ConnectorViewPanel<String> connectorViewPanel =
				new ConnectorViewPanel<String>(FactoryMgr.getDrawFactory(), FactoryMgr.getCommonFactory(), false)
				{
					public void highlightPinInList(@Nullable String selectedPin, boolean appendSelection)
					{

						List<CellSelection<ManageConnectorConnectionsInfo>> selections = new ArrayList<>();
						for (ManageConnectorConnectionsInfo aData : data) {
							aData.applyOnPin(pinProxy -> {
										if (pinProxy.getName().equals(selectedPin)) {
											selections.add(new CellSelection<>(aData, -1, 1, null, null, null));
										}
										return pinProxy.getName();
									}
							);
						}
						Platform.runLater(() -> table.select(new Selection<>(selections)));
					}
				};
		Map<String, ISymbolDef> symbolDefs = null;

		final PreferenceContext context = PreferenceContext.determineContext(
				UtilsHelper.getCAFUtils().getActiveDesignContainer());
		if (mSharedPinList != null) {
			symbolDefs = mSharedPinList instanceof ISharedConnector ?
					SymbolUtils.getPlugMap(context, mSharedPinList) :
					SymbolUtils.getAllPlugMaps(mSharedPinList);
		}
		else if (m_pinList != null) {
			symbolDefs = m_pinList instanceof IMultiSymbolledPinlist ?
					SymbolUtils.getAllPlugMaps((IMultiSymbolledPinlist) m_pinList) :
					SymbolUtils.getPlugMapsForLibraryConnector(context, m_pinList.getLibraryObject());
		}

		if (symbolDefs != null && !symbolDefs.isEmpty()) {
			connectorViewPanel.constructSymbolView(createPinListSymbolViewModel(symbolDefs));
			return connectorViewPanel;
		}
		return null;
	}

	protected JPanel createTablePanel(ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data)
	{
		JPanel tablePanel = new JPanel(new BorderLayout());
		ColumnChooserObjectType.ensureDummyObjectsAreCreated();
		final JFXPanel fxPanel = new JFXPanel();
		fxPanel.setName("ManageConnectorTable");
		tablePanel.add(fxPanel, BorderLayout.CENTER);
		fxPanel.addKeyListener(new IgnoreEscapeKeyListener());
		doCreateTablePanel(fxPanel, tablePanel, manageConnectorPinSelections, manageConnectorPinDuplicationFinder,
				data);
		return tablePanel;
	}

	protected void doCreateTablePanel(JFXPanel fxPanel, JPanel tablePanel,
			ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data)
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				initFX(fxPanel, tablePanel, manageConnectorPinSelections, manageConnectorPinDuplicationFinder, data);
			}
		});
	}

	public boolean showDialog()
	{
		return showDialog(false);
	}

	public boolean showDialog(boolean waitForFX)
	{
		if (waitForFX) {
			// PDV-10688/PDV-11443
			// This needs to be done before any other work otherwise we see problems with the
			// dialog invoked from right-clicking the column headers, i.e. the buttons do not highlight
			// as the mouse moves over and the filter text field cannot gain focus.
			waitForFX();
		}

		initData();

		createDialog();

		setVisible(true);
		return !isCancelled();
	}

	protected void initData()
	{
		tableDataStorage = new TableDataStorage<>();
		ILibraryCavityContainer cavityContainer;
		String name;
		if (mSharedPinList != null) {
			cavityContainer = (ILibraryCavityContainer) mSharedPinList.getLibraryObject();
			name = mSharedPinList.getName();
		}
		else {
			cavityContainer = (ILibraryCavityContainer) m_pinList.getLibraryObject();
			name = m_pinList.getName();
		}
		sortHelperProvider = new SortHelperProvider(cavityContainer, name, mDesignsInScope.getDesignsInScope());
	}

	protected void initFX(JFXPanel fxPanel, JPanel topPanel,
			ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data)
	{
		// This method is invoked on the JavaFX thread
		Scene scene = createScene(topPanel, manageConnectorPinSelections, manageConnectorPinDuplicationFinder, data);
		PlatformImpl.runAndWait(() -> fxPanel.setScene(scene));
		fxPanel.requestFocusInWindow();
	}

	@NotNull private Scene createScene(@NotNull JPanel topPanel,
			ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data)
	{
		sortHelperProvider.resetData(data);
		PinListAddPinHelper.ManageConnectorDnDHelper dndHelper =
				new PinListAddPinHelper.ManageConnectorDnDHelper(sortHelperProvider);
		table = createTable(manageConnectorPinSelections, manageConnectorPinDuplicationFinder, data,
				sortHelperProvider, dndHelper);
		List<String> columnNames = table.columns().map(t -> t.getName()).collect(Collectors.toList());
		table.setComparatorForColumn(columnNames, PinConductorConnectionSortHelper.getDefaultComparator());

		Scene scene = new Scene(table);
		JFXFlatUIUtils.getInstance().setFlatUIFor(scene);
		Collection<ColumnInformation<ManageConnectorConnectionsInfo>> columnsWithDragAndDrop =
				table.columns().filter(col -> WIRE_NAME.equals(col.getName()) || PIN_NAME.equals(col.getName()))
						.collect(
								Collectors.toList());

		columnsWithDragAndDrop.forEach(col -> col.setPostCellCreation(dndHelper
				.addDragAndDropFunctionalityOnCell(table, (popup, show) -> {
					if (show) {
						popup.show(scene.getWindow());
					}
					else {
						popup.hide();
					}
				}, col)));

//		PinListAddPinHelper.ManageConnectorDnDHelper
//				.setDnDFunctionalityCell(table, scene, sortHelperProvider, PIN_NAME, WIRE_NAME);

		final JPanel buttonPanel = createButtonPanel(data);
		topPanel.add(buttonPanel, BorderLayout.NORTH);
		return (scene);
	}

	@NotNull private JPanel createButtonPanel(@NotNull Collection<ManageConnectorConnectionsInfo> data)
	{
		final JPanel buttonPanel = new JPanel(new GridBagLayout());
		addWarningPanel(buttonPanel);
		addButtons(data, buttonPanel);
		return buttonPanel;
	}

	private void addWarningPanel(JPanel buttonPanel)
	{
		GridBagConstraints gbc = initGBC(1.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 0);
		JPanel warningPanel = createWarningPanel();
		if (warningPanel != null) {
			buttonPanel.add(warningPanel, gbc);
		}
		else {
			// still consume the space
			JPanel gluePanel = new JPanel();
			gluePanel.setLayout(new BoxLayout(gluePanel, BoxLayout.X_AXIS));
			gluePanel.add(Box.createHorizontalGlue());
			buttonPanel.add(gluePanel, gbc);
		}
	}

	private void addButtons(@NotNull Collection<ManageConnectorConnectionsInfo> data,
			@NotNull JPanel buttonPanel)
	{
		GridBagConstraints gbc = initGBC(0, GridBagConstraints.NONE, GridBagConstraints.EAST, 1);
		JButton zoomButton = createZoomButton();
		buttonPanel.add(zoomButton, gbc);

		gbc = initGBC(0, GridBagConstraints.NONE, GridBagConstraints.EAST, 2);
		JButton addButton = createAddButton(data, sortHelperProvider);
		buttonPanel.add(addButton, gbc);
	}

	@NotNull private GridBagConstraints initGBC(double weightX, int fill, int anchor, int gridX)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.weightx = weightX;
		gbc.fill = fill;
		gbc.anchor = anchor;
		gbc.gridx = gridX;
		return gbc;
	}

	@Nullable protected JPanel createWarningPanel()
	{
		return null;
	}

	protected Function<String, ColumnInformation<ManageConnectorConnectionsInfo>> getColumnsCreator(
			ColumnsToBeAdded columnsToBeAdded)
	{
		return columnName -> {
			if (PIN_NAME.equals(columnName)) {
				return columnsToBeAdded.createManageConnectorColumnForPinName();
			}
			if (WIRE_NAME.equals(columnName)) {
				return columnsToBeAdded.createManageConnectorColumnForWire();
			}
			if (IPinTableColumnInfo.PINICON.equals(columnName)) {
				return columnsToBeAdded.createManageConnectorColumnForIcon();
			}
			return getPinTableColumnProvider(getColumnCreationParams())
					.generateInformation(columnName);
		};
	}

	private ColumnCreationParams getColumnCreationParams()
	{
		ColumnCreationParams columnCreationParams = ColumnCreationParams
				.getDefaultColumnCreationParams(isPinListDeviceType() ? ColumnChooserObjectType.Device :
						ColumnChooserObjectType.Connector, mSharedPinList != null);
		if (m_pinList instanceof IFootprintable) {
			ILibraryDeviceFootprint footprint = ((IFootprintable) m_pinList).getFootprint();
			columnCreationParams.setFootprint(footprint);
		}
		else if (mSharedPinList != null) {
			columnCreationParams.setFootprint(mSharedPinList.getFootprint());
		}
		return columnCreationParams;
	}

	private PinTableColumnProvider getPinTableColumnProvider(ColumnCreationParams columnCreationParams)
	{
		if (isPinListDeviceType()) {
			return new PinTableColumnProvider(
					new DeviceColumnInformationCreatorFactory(sortHelperProvider, columnCreationParams));
		}
		else {
			return new PinTableColumnProvider(sortHelperProvider, columnCreationParams);
		}
	}

	protected Table<ManageConnectorConnectionsInfo> createTable(
			ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data, Supplier<PinConductorConnectionSortHelper> sortHelper,
			PinListAddPinHelper.ManageConnectorDnDHelper dndHelper)
	{

		itemChangeListenerProvider =
				new ManageConnectorItemChangeListenerProvider(manageConnectorPinDuplicationFinder);
		tableDataStorage.addDataChangeListener(itemChangeListenerProvider);

		ColumnsToBeAdded columnsToBeAdded =
				mColumnsToBeAddedSupplier
						.apply(new ColumnsToBeAdded.ColumnsToBeAddParameters(itemChangeListenerProvider,
								manageConnectorPinSelections,
								mConductorConnectionChanger,
								manageConnectorPinDuplicationFinder, sortHelper, mSharedPinList, data));

		Table<ManageConnectorConnectionsInfo> pinConductorTable = createTable(columnsToBeAdded,
				this::columnCreatedHandler);

		pinConductorTable
				.setSelectionPreferences(new SelectionPreferences().setCellSelectionEnabled(true))
				.setFeatureEnabled(TableFeature.Copy, false)
				.setFeatureEnabled(TableFeature.Paste, false)
				.setFeatureEnabled(TableFeature.Delete, false)

				.getStylesheets()
				.add(LogicPinTable.class.getResource("logicpintablecss.css").toExternalForm().replace('\\', '/'));

		pinConductorTable
				.addData(data)
				.addColumns(columnsToBeAdded.getColumnsIfNotAlreadyAdded())
				// enable sorting on pin icon column
				.setColumnSortable(Collections.singleton(IPinTableColumnInfo.PINICON), true)
				.addMenuItemProvider(new RemoveMenuItemProvider<ManageConnectorConnectionsInfo>(pinConductorTable,
						columnInformation -> mandatoryColumnNames.contains(columnInformation.getName())))

				.setCellStateHandler(new ITableCellStateHandler<ManageConnectorConnectionsInfo>()
				{
					@Override
					public boolean isEditable(@NotNull ITableCellValueProvider<ManageConnectorConnectionsInfo> cell)
					{
						ManageConnectorConnectionsInfo item = cell.getRowItem();
						return item != null && item.isEditable();
					}

					@Override public void updateStyle(@NotNull ITableCell<ManageConnectorConnectionsInfo> tableCell,
							@NotNull Node styleable)
					{
						dndHelper.updateStyle((CapitalTableCell<ManageConnectorConnectionsInfo>) tableCell);
					}

					@Override public boolean isValid(@NotNull ITableCell<ManageConnectorConnectionsInfo> cell)
					{
						return true;
					}
				});

		columnsToBeAdded.registerListeners(pinConductorTable);

		return pinConductorTable;
	}

	private void columnCreatedHandler(TableColumnBase<?, ?> createdTableColumn)
	{
		ContextMenu contextMenu = createdTableColumn.getContextMenu();
		if (contextMenu != null) {
			ReadOnlyBooleanProperty showingProperty = contextMenu.showingProperty();
			if (showingProperty != null) {
				showingProperty.addListener(new OnShowingPropertyChangeListener(contextMenu));
			}
		}
	}

	@NotNull protected Table<ManageConnectorConnectionsInfo> createTable(ColumnsToBeAdded columnsToBeAdded,
			Consumer<TableColumnBase<?, ?>> columnCreationListener)
	{
		String tableId = mSharedPinList instanceof ISharedDevice || m_pinList instanceof IDevice ?
				MANAGE_DEVICES_TABLE_ID : MANAGE_CONNECTORS_TABLE_ID;
		return new Table<>(tableId,
				new TableModel<ManageConnectorConnectionsInfo>(tableDataStorage, getColumnsCreator(columnsToBeAdded)),
				columnCreationListener);
	}

	@Nullable private ConnectorSymbolViewModel<String> createPinListSymbolViewModel(
			@Nullable final Map<String, ISymbolDef> symbolDefs)
	{
		if (symbolDefs != null && !symbolDefs.isEmpty()) {
			if (mSharedPinList != null) {
				return new PinListFaceViewModel(FactoryMgr.getCommonFactory(),
						FaceViewSymbol.getFaceViewSymbols(symbolDefs), PinUtils.getPinsWithConductors(mSharedPinList),
						!ignorePinNameCase(), mSharedPinList.getPartNumber());
			}
			else {
				final ILogicDesign logicDesign = m_pinList.getLogicDesign();
				assert logicDesign != null;
				return new PinListFaceViewModel(FactoryMgr.getCommonFactory(),
						FaceViewSymbol.getFaceViewSymbols(symbolDefs),
						getPinsWithConductors(m_pinList), !ignorePinNameCase(),
						m_pinList.getPartNumber());
			}
		}
		return null;
	}

	private Set<String> getPinsWithConductors(chs.cof.logical.cable.IPinList pinList)
	{

		if (pinList.getSharedPinList() != null) {
			return PinUtils.getPinsWithConductors(pinList.getSharedPinList());
		}
		return PinUtils.getPinsWithConductors(pinList);
	}

	@NotNull public ComponentListener componentListener(ConnectorViewPanel<String> connectorViewPanel)
	{
		return new ComponentAdapter()
		{
			@Override public void componentShown(ComponentEvent e)
			{
				if (connectorViewPanel != null) {
					connectorViewPanel.markPinConnections();
				}
			}
		};
	}

	private boolean ignorePinNameCase()
	{
		ISystemPreferenceMgr preferences =
				(ISystemPreferenceMgr) CHSSystemMgr.getCHSSystem().getSystemData().getPreferences();
		return !preferences.isMixedCasePinNamesAllowed();
	}

	private void extractPinsAndWiresOfConnection(
			@NotNull Collection<ManageConnectorConnectionsInfo> manageConnectorConnectionsInfos,
			@NotNull List<IPinProxy> pinProxies, @NotNull List<IConductorProxy> conductors)
	{
		manageConnectorConnectionsInfos.stream()
				.forEach(manageConnectorConnectionsInfo -> manageConnectorConnectionsInfo.applyOnPin(
						pinProxy ->
						{
							pinProxies.add(pinProxy);
							return pinProxy.getName();
						}
				));
		manageConnectorConnectionsInfos.stream()
				.forEach(manageConnectorConnectionsInfo -> manageConnectorConnectionsInfo.applyOnWire(
						conductorProxy ->
						{
							if (conductorProxy != null) {
								conductors.add(conductorProxy);
								return conductorProxy.getValueOfAttribute(IAttributeTypes.NAME);
							}
							return "";
						}
				));
	}

	private JPanel createEditSharedDetailsCheckBox()
	{

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		editSharedDetails =
				new JCheckBox(getResourceString("ManageConnectorsAction.dialog.editSharedDetails"));
		editSharedDetails.setHorizontalAlignment(SwingConstants.LEFT);
		editSharedDetails.setToolTipText(getResourceString("ManageConnectorsAction.dialog.tooltip.editSharedDetails"));
		editSharedDetails.setName("EditSharedDetails");
		editSharedDetails.setMnemonic(java.awt.event.KeyEvent.VK_E);
		editSharedDetails.addChangeListener(new ChangeListener()
		{
			@Override public void stateChanged(ChangeEvent e)
			{
				if (editSharedDetails.isSelected() && !mDesignsInScope.isReadonly()) {
					mDesignsInScope.makeAllDesignsInScopeEditable();
					editSharedDetails.setEnabled(false);
					editSharedDetails.setToolTipText("");
					Platform.runLater(() -> {
						if (mLockHelper != null) {
							Set<String> uids = new HashSet<>();
							table.getData().stream().forEach(row -> {
								String uid = row.getConnectedPinOwnerId();
								if (!StringUtils.isBlank(uid)) {
									uids.add(uid);
								}
							});
							mLockHelper.lockAdditionalSharedPinLists(uids);
						}
						table.getData().stream().forEach(row -> row.setEditable());
						@SuppressWarnings("unchecked")
						TableView<ManageConnectorConnectionsInfo> tableView =
								(TableView<ManageConnectorConnectionsInfo>) table.getCenter();
						tableView.refresh();
					});
				}
			}
		});

		panel.add(editSharedDetails, BorderLayout.WEST);
		JLabel noteOnSharedChange =
				new JLabel(getResourceString("ManageConnectorsAction.dialog.undodisablenote"), SwingConstants.LEFT);
		panel.add(noteOnSharedChange, BorderLayout.WEST);
		return panel;
	}

	@NotNull
	protected JButton createAddButton(@NotNull Collection<ManageConnectorConnectionsInfo> manageConnectorConnectionsInfos,
			Supplier<PinConductorConnectionSortHelper> sortHelper)
	{
		JButton addButton = BasicUIFactory.getInstance().createTTButton();
		addButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/app/reportgenerator/add.png"));
		addButton.setName("addColumn");
		addButton.setToolTipText(getResourceString("ManageConnectorsAction.dialog.tooltip.AddColumn"));
		addButton.setSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));
		addButton.setPreferredSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));
		addButton.setMaximumSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));

		List<IPinProxy> pinProxies = new ArrayList<>();
		List<IConductorProxy> conductors = new ArrayList<>();
		extractPinsAndWiresOfConnection(manageConnectorConnectionsInfos, pinProxies, conductors);

		ColumnCreationParams columnCreationParams = getColumnCreationParams();
		if (m_pinList instanceof IFootprintable) {
			ILibraryDeviceFootprint footprint = ((IFootprintable) m_pinList).getFootprint();
			columnCreationParams.setFootprint(footprint);
		}
		PinTableColumnChooser<ManageConnectorConnectionsInfo> pinTableColumnChooser =
				getPinTableColumnChooser(pinProxies, conductors, sortHelper, columnCreationParams);
		// add a listener to Add button
		addButton.addMouseListener(new AddButtonMouseListener(pinTableColumnChooser));
		//Add esc handler for popup menu
		KeyStroke escKeystroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
		getRootPane().registerKeyboardAction(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (pinTableColumnChooser != null && pinTableColumnChooser.isPopUpMenuVisible()) {
					pinTableColumnChooser.hidePopUpMenu();
				}
				else {
					getCancelButton().doClick();
				}
			}
		}, escKeystroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

		return addButton;
	}

	protected PinTableColumnChooser<ManageConnectorConnectionsInfo> getPinTableColumnChooser(List<IPinProxy> pinProxies,
			List<IConductorProxy> conductors, Supplier<PinConductorConnectionSortHelper> sortHelper,
			ColumnCreationParams columnCreationParams)
	{
		return new PinTableColumnChooser<ManageConnectorConnectionsInfo>(table, getProperties(pinProxies), conductors,
				PinConductorConnectionSortHelper.getDefaultComparator(),
				getPinTableColumnProvider(columnCreationParams), columnCreationParams);
	}

	protected Supplier<Collection<IProperty>> getProperties(Collection<IPinProxy> pinProxies)
	{
		if (mSharedPinList == null) {
			if (m_pinList != null) {
				ILibraryBaseObject libraryObject = m_pinList.getLibraryObject();
				if (libraryObject instanceof ILibraryObject) {
					FactoryMgr.getCHSSystem().getPartsLibrary().getLibraryBatchLoader()
							.loadFully(Collections.singleton((ILibraryObject) libraryObject), false);
				}
			}
		}
		return () -> pinProxies.stream()
				.flatMap(aPinProxy -> aPinProxy.getAllProperties().stream())
				.collect(Collectors.toSet());
	}

	@NotNull private JButton createZoomButton()
	{
		JButton zoomButton = BasicUIFactory.getInstance().createTTButton();
		// todo pdv-10688 is this the correct image? Should it be placeholder for moment?
		zoomButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/javafx_ui/zoom-selection-small.png"));
		zoomButton.setName("ZoomSelected");
		zoomButton.setToolTipText(getResourceString("ManageConnectorsAction.dialog.tooltip.NoSelection"));
		zoomButton.setSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));
		zoomButton.setPreferredSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));
		zoomButton.setMaximumSize(new Dimension(ADD_BTN_WIDTH, ADD_BTN_HEIGHT));
		zoomButton.addActionListener(e -> zoomToPinList());
		zoomButton.setEnabled(false);

		table.addSelectionListener((selection, lastSelectionIndex) -> zoomSelectionHandler(zoomButton, selection));

		return zoomButton;
	}

	protected void zoomSelectionHandler(@NotNull JButton zoomButton,
			@NotNull Selection<ManageConnectorConnectionsInfo> selection)
	{
		final List<CellSelection<ManageConnectorConnectionsInfo>> cells = selection.getSelectedCells();
		if (cells.isEmpty()) {
			zoomButton.setEnabled(false);
			zoomButton.setToolTipText(getResourceString("ManageConnectorsAction.dialog.tooltip.NoSelection"));
		}
		else {
			final SelectionState state = selectedPinsOnSingleDiagram(cells);
			zoomButton.setEnabled(state == SelectionState.OK);
			String tooltip = null;
			switch (state) {
				case OK:
					tooltip = isPinListDeviceType() ?
							getResourceString("ManageConnectorsAction.dialog.tooltip.ZoomSelectedForDevice") :
							getResourceString("ManageConnectorsAction.dialog.tooltip.ZoomSelectedForConnector");
					break;
				case MultipleDiagrams:
					tooltip = getResourceString("ManageConnectorsAction.dialog.tooltip.MultipleDiagrams");
					break;
				case NotLoaded:
					tooltip = getResourceString("ManageConnectorsAction.dialog.tooltip.DesignNotLoaded");
					break;
			}
			zoomButton.setToolTipText(tooltip);
		}
	}

	private boolean isPinListDeviceType()
	{
		return mSharedPinList != null ? mSharedPinList.isDeviceType() : m_pinList instanceof IDevice;
	}

	private String getResourceString(@NotNull String key)
	{
		return ResourceMgr.getString(ManageConnectorsAction.class, key);
	}

	protected void zoomToPinList()
	{
		final Selection<ManageConnectorConnectionsInfo> selection = table.getSelection();
		final List<CellSelection<ManageConnectorConnectionsInfo>> cells = selection.getSelectedCells();
		Pair<ISchemDiagram, Collection<IPinList>> pinlistPerDiagram = gatherPinlistsToSelect(cells);
		if (pinlistPerDiagram != null) {
			selectAndZoomToPinlists(pinlistPerDiagram);
		}
	}

	private void selectAndZoomToPinlists(@NotNull Pair<ISchemDiagram, Collection<IPinList>> pinlistPerDiagram)
	{
		final GfxView gfxView = GfxViewHelper.openLogicDiagram(pinlistPerDiagram.getFirst());
		if (gfxView != null) {
			final ILogicDesign design = pinlistPerDiagram.getFirst().getDesign();
			assert design != null;
			// Having opened this diagram, we should make sure it doesn't get unlocked on closing the dialog
			mDesignsInScope.removeFromLockedInAction(design);
			boolean addToSelection = false;
			for (IPinList pinlistToSelect : pinlistPerDiagram.getSecond()) {
				GfxViewHelper.locateAndSelectObject(gfxView, pinlistToSelect, addToSelection, false, true, true);
				addToSelection = true;
			}
			GfxViewHelper.zoomSelected(gfxView, true);
		}
	}

	@Nullable
	protected Pair<ISchemDiagram, Collection<IPinList>> gatherPinlistsToSelect(
			@NotNull List<CellSelection<ManageConnectorConnectionsInfo>> cells)
	{
		if (cells.isEmpty()) {
			return null;
		}

		ISchemDiagram diagram = null;
		Set<IPinList> pinlists = new LinkedHybridSet<>();
		for (CellSelection<ManageConnectorConnectionsInfo> cell : cells) {
			final ManageConnectorConnectionsInfo info = cell.getSelectedItem();
			final IDesignDescriptor design = info.getDesign();
			if (design == null) {
				//design could be null for unplaced library cavity pins, unplaced shared pins.
				continue;
			}

			final ILogicDesign logicDesign = (ILogicDesign) design.getDesignContainer();
			if (logicDesign == null) {
				continue;
			}
			final IDesignWideUsageMgr designWideUsageMgr = logicDesign.getDesignWideUsageMgr();
			final IAbstractPin cablePin = PinProxyHelper.getCablePin(info.getFirst(), logicDesign, mSharedPinList);
			if (cablePin != null) {
				final List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(cablePin);
				for (IDesignSharedUsage usage : usages) {
					final ISchemDiagram usageDiagram = logicDesign.getDiagram(usage.getDiagramUID());
					if (usageDiagram != null) {
						usageDiagram.loadToMemory();
						IAbstractSchemPin pin = (IAbstractSchemPin) usage.getDiagramObject();
						if (pin != null) {
							IPinList parent = (IPinList) pin.getParent();
							if (parent != null) {
								if (diagram == null) {
									diagram = usage.getDiagram();
								}
								if (diagram == usage.getDiagram()) {
									pinlists.add(parent);
								}
								else {
									return null;
								}
							}
						}
					}
				}
			}
		}
		return diagram != null ? new Pair<>(diagram, pinlists) : null;
	}

	public enum SelectionState
	{
		OK, MultipleDiagrams, NotLoaded
	}

	/**
	 * Check that pins of all the selected cells are in the same diagram
	 *
	 * @param cells the currently selected tqable cells
	 * @return boolean
	 */
	protected SelectionState selectedPinsOnSingleDiagram(
			@NotNull List<CellSelection<ManageConnectorConnectionsInfo>> cells)
	{
		assert !cells.isEmpty();

		IUID diagramUID = null;
		boolean singleDiagram = false;
		for (CellSelection<ManageConnectorConnectionsInfo> cell : cells) {
			final ManageConnectorConnectionsInfo info = cell.getSelectedItem();
			final IDesignDescriptor design = info.getDesign();
			if (design == null) {
				//design could be null for unplaced library cavity pins, unplaced shared pins.
				continue;
			}

			final ILogicDesign logicDesign = (ILogicDesign) design.getDesignContainer();
			if (logicDesign == null) {
				continue;
			}
			Set<IUID> diagrams = getPinDiagramUsages(info.getOriginalPin());
			for (IUID usageDiagramUID : diagrams) {
				if (diagramUID == null) {
					diagramUID = usageDiagramUID;
					singleDiagram = true;
				}
				if (!diagramUID.equals(usageDiagramUID)) {
					return SelectionState.MultipleDiagrams;
				}
			}
		}
		return singleDiagram ? SelectionState.OK : SelectionState.NotLoaded;
	}

	/**
	 * Returns the UIDs of the diagrams the provided pin proxy is used on.
	 * <p>
	 *
	 * @param pin Pin to look up usages of
	 * @return the uids of diagrams where the pin is used.
	 */
	private Set<IUID> getPinDiagramUsages(IPinProxy pin)
	{
		return mPinDiagramUsages.getSet(pin);
	}

	public static void waitForFX()
	{
		try {
			//Check if we are on EDT already. If not there is no need to clear event loop explicitly.
			if (SwingUtilities.isEventDispatchThread()) {
				//Initially we have to clear EDT event queue, since there might be runnables that are locking JavaFX thread.
				//For example invocations of SwingUtils.invokeAndWait from JavaFX thread.
				EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
				SecondaryLoop secondaryLoop = eq.createSecondaryLoop();
				SwingUtilities.invokeLater(secondaryLoop::exit);
				//Enter secondary loop to processed all jobs submitted to EDT.
				secondaryLoop.enter();
			}

			//Check that we are not on the fx thread already.
			if (!Platform.isFxApplicationThread()) {
				//This ensures that any previously submitted jobs to JavaFX thread finished.
				CountDownLatch latch = new CountDownLatch(1);
				Platform.runLater(latch::countDown);
				//Wait no longer than 5 seconds to avoid possible deadlocking of this tread.
				latch.await(5, TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public SortHelperProvider getSortHelperProvider()
	{
		return sortHelperProvider;
	}

	protected static class AddButtonMouseListener extends MouseAdapter
	{

		@Nullable private PinTableColumnChooser<ManageConnectorConnectionsInfo> mPinTableColumnChooser;

		public AddButtonMouseListener(@Nullable
				PinTableColumnChooser<ManageConnectorConnectionsInfo> pinTableColumnChooser)
		{
			mPinTableColumnChooser = pinTableColumnChooser;
		}

		@Override public void mouseClicked(MouseEvent e)
		{
			if (mPinTableColumnChooser != null) {
				mPinTableColumnChooser.showPopupMenu(e.getX(), e.getY(), e.getComponent());
			}
		}
	}

	public Collection<ManageConnectorConnectionsInfo> createData(
			ManageConnectorPinSelections manageConnectorPinSelections)
	{
		RowData rowDataToBeAdded = mRowDataSupplier.apply(this);
		return rowDataToBeAdded.createData(manageConnectorPinSelections);
	}

	public Collection<IConductor> getConductors(IPinProxy pin)
	{
		IAbstractPin cPin = pin.getCablePin();
		Collection<IConductor> conductors = new LinkedHashSet<>();
		if (cPin != null) {
			conductors.addAll(cPin.getConductorsAsSet());
		}
		return conductors;
	}

	public void setConductorConnectionChangerForTest(ConductorConnectionChanger conductorConnectionChanger)
	{
		mConductorConnectionChanger = conductorConnectionChanger;
	}

	public boolean applyTableChangesForUnitTest()
	{
		return mConductorConnectionChanger.changeConnections();
	}

	@NotNull @Override public String getHelpID()
	{
		return ManageConnectorsAction.class.getName();
	}

	/**
	 * This class implements a listener for on showing property for given ContextMenu. The listener attempts to request
	 * a focus for owner window of the context menu. This is needed to allow correct operation of JavaFX components in
	 * case current focus is outside the JavaFX object as right clicking on the element (i.e. Column Header) does not
	 * set focus to JavaFX object. PDV-11443
	 */
	private static class OnShowingPropertyChangeListener implements javafx.beans.value.ChangeListener<Boolean>
	{

		private ContextMenu storedMenu;

		private OnShowingPropertyChangeListener(@NotNull ContextMenu targetMenu)
		{
			storedMenu = targetMenu;
		}

		@Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
		{
			Window ownerWindow = storedMenu.getOwnerWindow();
			if (ownerWindow != null) {
				ownerWindow.requestFocus();
			}
		}
	}
}