/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.DeviceConnectorNamePartNumberValidator;
import chs.caplets.logic.actions.DeviceConnectorNamePinNameValidator;
import chs.caplets.logic.actions.EditDeviceConnPNInLibraryValidator;
import chs.caplets.logic.actions.EditDeviceConnectorParams;
import chs.caplets.logic.actions.EditDeviceConnectorTableRow;
import chs.caplets.logic.actions.EditDeviceNameLengthValidator;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryConnector;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.parts.PartNumberHelper;
import chs.common.ISystemPreferenceMgr;
import chs.common.PreferenceContext;
import chs.common.attr.IAttributeTypes;
import chs.common.criteria.ICriteria;
import chs.common.criteria.ICriteriaFactory;
import chs.common.criteria.Restrictions;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.images.CHSImageLoader;
import chs.system.CHSSystemMgr;
import chs.system.FactoryMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.BasicUIFactory;
import chs.utility.SymbolUtils;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.ConnectorSymbolViewModel;
import chs.utility.ui.ConnectorViewPanel;
import chs.utility.ui.FaceViewSymbol;
import chs.utility.ui.OkCancelDialog;
import chs.utility.ui.PaneSplitter;
import chs.utility.ui.PinListFaceViewModel;
import chs.utility.ui.pintable.ColumnChooserObjectType;
import com.mentor.capital.javafx.table.CellSelection;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ITableSelectionListener;
import com.mentor.capital.javafx.table.Selection;
import com.mentor.capital.javafx.table.SelectionPreferences;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import com.mentor.capital.javafx.table.common.ITableCellValueChangeListener;
import com.mentor.capital.javafx.table.helpers.IgnoreEscapeKeyListener;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EditDeviceConnectorDialog extends SimpleOkCancelDialog
{

	private static final int DIALOG_WIDTH = 1000;
	private static final int DIALOG_HEIGHT = 400;
	private static final String EDITDEVICECONNECTOR_TABLE_ID = "EditDeviceConnectorTableId";

	private static final int EDC_BUTTON_WIDTH = 30;
	private static final int EDC_BUTTON_HEIGHT = 30;

	protected Table<EditDeviceConnectorTableRow> table;
	private TableDataStorage<EditDeviceConnectorTableRow> tableRowTableDataStorage;
	protected EditDeviceConnectorColumnsProvider columnsToBeAdded;

	private EditDeviceConnectorParams params;

	protected ConnectorViewPanel<String> connectorViewPanel;

	protected JButton editDCPartNumber;
	@Nullable protected EditDCPartnumberHandler editDCPartnumberHandler;

	private PaneSplitterParams paneSplitterParams;
	private PaneSplitter paneSplitter;
	protected DeviceConnectorNamePartNumberValidator dcNamePartNumberValidator;
	protected DeviceConnectorNamePinNameValidator dcNamePinNameValidator;
	protected EditDeviceConnPNInLibraryValidator invalidPartnumberHandler;
	protected EditDeviceNameLengthValidator nameLengthValidator;
	private Supplier<DeviceConnectorNamePartNumberValidator> deviceConnNamePartNumValidator =
			() -> dcNamePartNumberValidator;
	private Supplier<DeviceConnectorNamePinNameValidator> deviceConnNamePinNameValidator =
			() -> dcNamePinNameValidator;
	private CurrentSelection currentSelection;

	private Consumer<CurrentSelection> highlightSymbolViewer =
			(currentSel) -> highlighSymbolView(currentSel);

	private static class PaneSplitterParams
	{

		private int dividerLocation;

		void setParams(PaneSplitter paneSplitter)
		{
			dividerLocation = paneSplitter.getSplitPane().getDividerLocation();
		}

		void useParams(PaneSplitter givenPaneSplitter)
		{
			givenPaneSplitter.getSplitPane().setDividerLocation(DIALOG_WIDTH / 2);
			givenPaneSplitter.getSplitPane().setDividerLocation(dividerLocation);
		}
	}

	public EditDeviceConnectorDialog(@Nullable Frame owner,
			boolean modal, EditDeviceConnectorParams params)
	{
		super(null, owner, params.getDialogTitle(), modal);
		this.params = params;
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

		createDialog();

		setVisible(!Environment.isHeadless());
		return !isCancelled();
	}

	@Override protected void prepareToApplyEdits()
	{
		tableRowTableDataStorage.apply();
	}

	private void waitForFX()
	{
		ManageConnectorsDialog.waitForFX();
	}

	protected OkCancelDialog createDialog()
	{
		rememberSize(true);
		setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

		Collection<EditDeviceConnectorTableRow> data = params.getData();

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel createPNPanel = createDCPartnumebrLayout();
		connectorViewPanel = createPlugMapPanel(data);
		connectorViewPanel.setName("EditDeviceConnectorPlugmapPanel");
		connectorViewPanel.setMinimumSize(new Dimension(DIALOG_WIDTH / 4, DIALOG_HEIGHT / 2));
		connectorViewPanel.setPreferredSize(new Dimension(DIALOG_WIDTH / 4, DIALOG_HEIGHT / 2));
		paneSplitter = new PaneSplitter().setPreferenceClass(getClass());

		JPanel tablePanel = new JPanel(new BorderLayout());

		panel.add(paneSplitter.createSplitter(tablePanel, connectorViewPanel));
		tablePanel.add(createPNPanel, BorderLayout.NORTH);
		tablePanel.setMinimumSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));
		tablePanel.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2));

		populateTablePanel(data, tablePanel);

		WindowAdapter windowAdapter = paneSplitter.getWindowAdapter();
		addWindowListener(windowAdapter);

		getContentPane().add(panel);
		getRootPane().setDefaultButton(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setNameExplicitly("EditDeviceConnectorDialog");
		rememberSize(true);
		return this;
	}

	public ConnectorViewPanel<String> createPlugMapPanel(
			Collection<EditDeviceConnectorTableRow> data)
	{

		return
				new ConnectorViewPanel<String>(FactoryMgr.getDrawFactory(), FactoryMgr.getCommonFactory(), true)
				{
					public void highlightPinInList(@Nullable String selectedPin, boolean appendSelection)
					{

						//noinspection unchecked
						Pair<String, String> selectedId = (Pair<String, String>) getSelectedId();

						String viewPartNumber = selectedId != null ? selectedId.getSecond() : null;
						String connectorName = selectedId != null ? selectedId.getFirst() : null;
						Platform.runLater(() -> {
							List<CellSelection<EditDeviceConnectorTableRow>> selections = new ArrayList<>();
							if (appendSelection) {
								Selection<EditDeviceConnectorTableRow> tableSelections = table.getSelection();

								selections.addAll(tableSelections.getSelectedCells());
							}
							for (EditDeviceConnectorTableRow aData : data) {

								String dcPartNumber = aData.getDeviceConnectorPartNumber();
								String dcPinName = aData.getDeviceConnectorPinName();
								String dcName = aData.getDeviceConnectorName();

								if (dcPartNumber != null && dcPinName != null) {
									if (StringUtils.equals(dcPartNumber, viewPartNumber) &&
											StringUtils.equals(dcPinName, selectedPin) &&
											StringUtils.equals(dcName, connectorName)) {
										selections.add(new CellSelection<>(aData, -1, 2, null, null, null));
									}
								}
							}

							if (!selections.isEmpty()) {

								table.select(new Selection<>(selections));
							}
						});
					}
				};
	}

	protected JPanel createDCPartnumebrLayout()
	{

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		editDCPartNumber = BasicUIFactory.getInstance().createTTButton();
		editDCPartNumber.setEnabled(false);
		editDCPartNumber
				.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_editdeviceconnector_partnumber.png"));
		editDCPartNumber.setName("Edit Device Connector Partnumber");
		editDCPartNumber.setToolTipText(ResourceMgr
				.getString(EditDeviceConnectorDialog.class,
						"EditDeviceConnectorAction.PartnumberButton.disabledtooltip"));
		editDCPartNumber.setSize(new Dimension(EDC_BUTTON_WIDTH, EDC_BUTTON_HEIGHT));
		editDCPartNumber.setPreferredSize(new Dimension(EDC_BUTTON_WIDTH, EDC_BUTTON_HEIGHT));
		editDCPartNumber.setMaximumSize(new Dimension(EDC_BUTTON_WIDTH, EDC_BUTTON_HEIGHT));

		editDCPartNumber.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{

				if (editDCPartnumberHandler != null) {
					editDCPartnumberHandler.handleSelections(table);
				}
			}
		});

		buttonPanel.add(editDCPartNumber);

		JPanel childPanel = new JPanel();
		childPanel.setLayout(new BorderLayout());
		childPanel.add(buttonPanel, BorderLayout.EAST);
		return childPanel;
	}

	@NotNull protected ITableSelectionListener<EditDeviceConnectorTableRow> getSelectionListener()
	{
		return new ITableSelectionListener<EditDeviceConnectorTableRow>()
		{
			@Override public void changed(Selection<EditDeviceConnectorTableRow> selection, int lastSelectionIndex)
			{

				currentSelection = new CurrentSelection(selection);
				Map<Integer, EditDeviceConnectorTableRow> selectedRows = new HashMap<>(currentSelection.getRowMap());
				invokeInSwingThread(() -> {
					updatePartnumberLayout(selectedRows);
				});
				highlighSymbolView(currentSelection);
			}
		};
	}

	private void highlighSymbolView(CurrentSelection givenSelection)
	{
		if (connectorViewPanel != null) {

			if (givenSelection != null) {
				Collection<String> connectorNames = givenSelection.getConnectorNames();
				Collection<String> partNumbers = givenSelection.getPartNumbers();
				Pair<String, String> selectionId = null;
				if (connectorNames.size() == 1 && partNumbers.size() == 1) {
					selectionId =
							new Pair<String, String>(connectorNames.iterator().next(), partNumbers.iterator().next());
				}

				String dcPartNumber = selectionId != null ? selectionId.getSecond() : null;
				ILibraryObject libraryObject =
						dcPartNumber != null ? PartNumberHelper.getLibraryPartFromCombinedPartNumber(dcPartNumber) :
								null;
				final Collection<String> cavities = new HashSet<>(givenSelection.getCavities());
				rebuildConnectorViewAndHighlightPins(selectionId, libraryObject, cavities);
			}
			else {
				rebuildConnectorViewAndHighlightPins(null, null, null);
			}
		}
	}

	private void rebuildConnectorViewAndHighlightPins(@Nullable Pair<String, String> selectionId,
			@Nullable ILibraryObject libraryObject,
			@Nullable Collection<String> cavities)
	{
		invokeInSwingThread(() -> {
			reBuildConnectorView(libraryObject, selectionId);
			if (libraryObject != null && cavities != null) {
				connectorViewPanel.highlightPinsOnSymbolView(cavities);
			}
		});
	}

	protected static class CurrentSelection
	{

		private LinkedHashMap<Integer, EditDeviceConnectorTableRow> selectedRows;

		private Map<String, Integer> cavities;
		private Map<String, Integer> connectorNames;
		private Map<String, Integer> partNumbers;

		CurrentSelection(Selection<EditDeviceConnectorTableRow> selection)
		{
			selectedRows = new LinkedHashMap<>();
			selection.getSelectedCells()
					.forEach(aCell -> selectedRows.put(aCell.getRowIndex(), aCell.getSelectedItem()));
		}

		@NotNull private Map<String, Integer> getCavitiesMap()
		{
			if (cavities == null) {
				populateAllEntries();
			}
			return cavities;
		}

		@NotNull private Map<String, Integer> getConnectorNamesMap()
		{
			if (connectorNames == null) {
				populateAllEntries();
			}
			return connectorNames;
		}

		@NotNull private Map<String, Integer> getPartNumbersMap()
		{
			if (partNumbers == null) {
				populateAllEntries();
			}
			return partNumbers;
		}

		Collection<String> getCavities()
		{
			return getCavitiesMap().keySet();
		}

		Collection<String> getConnectorNames()
		{
			return getConnectorNamesMap().keySet();
		}

		Collection<String> getPartNumbers()
		{
			return getPartNumbersMap().keySet();
		}

		void updateConnectorName(@Nullable String oldValue, @Nullable String newValue)
		{
			updateRefCount(getConnectorNamesMap(), oldValue, newValue, false);
		}

		void updatePartnumber(@Nullable String oldValue, @Nullable String newValue)
		{
			updateRefCount(getPartNumbersMap(), oldValue, newValue, false);
		}

		void updateCavityName(@Nullable String oldValue, @Nullable String newValue)
		{
			updateRefCount(getCavitiesMap(), oldValue, newValue, true);
		}

		private void updateRefCount(@NotNull Map<String, Integer> givenMap, @Nullable String oldValue,
				@Nullable String newValue, boolean ignoreNull)
		{
			if (StringUtils.equals(oldValue, newValue)) {
				return;
			}
			if (ignoreNull && newValue == null) {
				return;
			}
			Integer count = givenMap.get(oldValue);
			if (count != null) {
				count--;
				if (count != 0) {
					givenMap.put(oldValue, count);
				}
				else {
					givenMap.remove(oldValue);
				}
			}
			count = givenMap.computeIfAbsent(newValue, (val) -> 0);
			count++;
			givenMap.put(newValue, count);
		}

		private void populateAllEntries()
		{
			connectorNames = new LinkedHashMap<>(selectedRows.size());
			partNumbers = new LinkedHashMap<>(selectedRows.size());
			cavities = new LinkedHashMap<>(selectedRows.size());
			for (Map.Entry<Integer, EditDeviceConnectorTableRow> namePn : selectedRows.entrySet()) {
				String connectorName = namePn.getValue().getDeviceConnectorName();
				Integer count = connectorNames.computeIfAbsent(connectorName, (name) -> 0);
				count++;
				connectorNames.put(connectorName, count);
				String partNumber = namePn.getValue().getDeviceConnectorPartNumber();
				count = partNumbers.computeIfAbsent(partNumber, (name) -> 0);
				count++;
				partNumbers.put(partNumber, count);
				String cavityName = namePn.getValue().getDeviceConnectorPinName();
				if (!StringUtils.isBlank(cavityName)) {
					count = cavities.computeIfAbsent(cavityName, (val) -> 0);
					count++;
					cavities.put(cavityName, count);
				}
			}
		}

		public Collection<EditDeviceConnectorTableRow> getRowItems()
		{
			return selectedRows.values();
		}

		public boolean isSymbolHighlightApplicable()
		{

			return connectorNames.size() == 1 && partNumbers.size() == 1 &&
					partNumbers.keySet().iterator().next() != null;
		}

		public Map<Integer, EditDeviceConnectorTableRow> getRowMap()
		{
			return selectedRows;
		}
	}

	private void updatePartnumberLayout(Map<Integer, EditDeviceConnectorTableRow> givenSelectedRows)
	{
		if (!givenSelectedRows.isEmpty()) {
			editDCPartnumberHandler = new EditDCPartnumberHandler(givenSelectedRows, this::getSelectedPart);
			if (editDCPartnumberHandler.areValidSelections()) {
				editDCPartNumber.setEnabled(true);
				editDCPartNumber.setToolTipText(ResourceMgr.getString(EditDeviceConnectorDialog.class,
						"EditDeviceConnectorAction.PartnumberButton.enabledtooltip"));
			}
			else {
				editDCPartnumberHandler = null;
				editDCPartNumber.setEnabled(false);
				editDCPartNumber.setToolTipText(ResourceMgr.getString(EditDeviceConnectorDialog.class,
						"EditDeviceConnectorAction.PartnumberButton.disimilarconnselected"));
			}
		}
		else {
			editDCPartnumberHandler = null;
			editDCPartNumber.setEnabled(false);
			editDCPartNumber.setToolTipText(ResourceMgr.getString(EditDeviceConnectorDialog.class,
					"EditDeviceConnectorAction.PartnumberButton.disabledtooltip"));
		}
	}

	protected void invokeInSwingThread(Runnable method)
	{
		SwingUtilities.invokeLater(() -> {
			method.run();
		});
	}

	private void reBuildConnectorView(@Nullable ILibraryObject libraryObject, @Nullable Pair<String, String> selectedId)
	{

		if (connectorViewPanel.isVisible()) {
			if (paneSplitterParams == null) {
				paneSplitterParams = new PaneSplitterParams();
			}
			paneSplitterParams.setParams(paneSplitter);
		}

		if (libraryObject == null || selectedId == null) {
			connectorViewPanel.constructSymbolView(null);
			return;
		}

		//noinspection unchecked
		Pair<String, String> modelSelectionId = (Pair<String, String>) connectorViewPanel.getSelectedId();
		String modelPN = null;
		String modelName = null;
		if (modelSelectionId != null) {
			modelPN = modelSelectionId.getSecond();
			modelName = modelSelectionId.getFirst();
		}

		if (!StringUtils.equals(modelName, selectedId.getFirst())) {
			if (modelSelectionId != null) {

				modelSelectionId.setFirst(selectedId.getFirst());
			}
		}

		if (StringUtils.equals(modelPN, libraryObject.getPartNumber())) {
			return;
		}
		final PreferenceContext context = PreferenceContext.determineContext(
				UtilsHelper.getCAFUtils().getActiveDesignContainer());
		Map<String, ISymbolDef> symbolDefs = SymbolUtils.getPlugMapsForLibraryConnector(context, libraryObject);

		if (symbolDefs != null && !symbolDefs.isEmpty()) {

			ConnectorSymbolViewModel<String> symbolViewModel =
					new PinListFaceViewModel(FactoryMgr.getCommonFactory(),
							FaceViewSymbol.getFaceViewSymbols(symbolDefs), Collections.emptySet(), !ignorePinNameCase(),
							selectedId);

			connectorViewPanel.constructSymbolView(symbolViewModel);
			if (paneSplitter != null) {
				if (paneSplitterParams != null) {
					paneSplitterParams.useParams(paneSplitter);
				}
				else {
					paneSplitter.getSplitPane().setDividerLocation(DIALOG_WIDTH / 2);
					paneSplitter.setDividerPosition();
				}
			}
		}
		else {
			connectorViewPanel.constructSymbolView(null);
		}
	}

	private boolean ignorePinNameCase()
	{
		ISystemPreferenceMgr preferences =
				(ISystemPreferenceMgr) CHSSystemMgr.getCHSSystem().getSystemData().getPreferences();
		return !preferences.isMixedCasePinNamesAllowed();
	}

	protected JPanel populateTablePanel(
			Collection<EditDeviceConnectorTableRow> data, JPanel tablePanel)
	{

		ColumnChooserObjectType.ensureDummyObjectsAreCreated();
		tableRowTableDataStorage = new TableDataStorage<>();
		final JFXPanel fxPanel = new JFXPanel();
		fxPanel.setName("EditDeviceConnectorTable");
		tablePanel.add(fxPanel, BorderLayout.CENTER);
		fxPanel.addKeyListener(new IgnoreEscapeKeyListener());
		Platform.runLater(() -> initFX(fxPanel, data));
		return tablePanel;
	}

	protected void initFX(JFXPanel fxPanel,
			Collection<EditDeviceConnectorTableRow> data)
	{

		dcNamePartNumberValidator = new DeviceConnectorNamePartNumberValidator(data);
		dcNamePinNameValidator = new DeviceConnectorNamePinNameValidator(data);
		columnsToBeAdded = new EditDeviceConnectorColumnsProvider(deviceConnNamePartNumValidator,
				deviceConnNamePinNameValidator);
		table = createTable(data);
		List<String> columnNames = table.columns().map(t -> t.getName()).collect(Collectors.toList());
		table.setComparatorForColumn(columnNames,AlphaNumComparator.getUniqueObjectAsUniqueComparator());
		table.addSelectionListener(getSelectionListener());
		table.addValueChangeListener(getCellValueChangeListener(data));
		table.addHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
			if (KeyCode.ESCAPE.equals(e.getCode())) {
				getCancelButton().doClick();
			}
		});
		Scene scene = new Scene(table);
		JFXFlatUIUtils.getInstance().setFlatUIFor(scene);
		table.setCellDragHandler(new EditDeviceConnectorDnDHelper(table, (popup, show) -> {
			if (show) {
				popup.show(scene.getWindow());
			}
			else {
				popup.hide();
			}
		}));
		table.setCommitOnFocusLoss(true);
		fxPanel.setScene(scene);
		fxPanel.requestFocusInWindow();
	}

	protected Table<EditDeviceConnectorTableRow> createTable(
			Collection<EditDeviceConnectorTableRow> data)
	{

		Table<EditDeviceConnectorTableRow> editDeviceConnectorTable = new Table<>(EDITDEVICECONNECTOR_TABLE_ID,
				new TableModel<EditDeviceConnectorTableRow>(tableRowTableDataStorage,
						columnName -> {
							return columnsToBeAdded.getColumnByName(columnName);
						}));

		editDeviceConnectorTable
				.setSelectionPreferences(new SelectionPreferences()
						.setCellSelectionEnabled(true)); //copy and paste features are enabled by default

		editDeviceConnectorTable
				.addData(data)
				.addColumns(columnsToBeAdded.getColumns())
				.setCellStateHandler(getCellStateHandler());

		return editDeviceConnectorTable;
	}

	protected ITableCellStateHandler<EditDeviceConnectorTableRow> getCellStateHandler()
	{
		return new ITableCellStateHandler<EditDeviceConnectorTableRow>()
		{
			@Override public boolean isEditable(@NotNull ITableCellValueProvider<EditDeviceConnectorTableRow> cell)
			{
				return true;
			}

			@Override
			public void updateStyle(@NotNull ITableCell<EditDeviceConnectorTableRow> tableCell, @NotNull Node styleable)
			{
				ColumnInformation<?> column = tableCell.getColumn();
				if (column != null) {
					EditDeviceConnectorColumnsProvider.EditDeviceConnectorUpdateStyle styler =
							columnsToBeAdded.getColumnStyler(column.getName());
					if (styler != null) {
						styler.updateStyle(tableCell, styleable);
					}
				}
			}

			@Override public boolean isValid(@NotNull ITableCell<EditDeviceConnectorTableRow> cell)
			{
				return true;
			}
		};
	}

	private ITableCellValueChangeListener<EditDeviceConnectorTableRow> getCellValueChangeListener(
			Collection<EditDeviceConnectorTableRow> givenData)
	{
		invalidPartnumberHandler = new EditDeviceConnPNInLibraryValidator(givenData);
		nameLengthValidator = new EditDeviceNameLengthValidator();
		return new ITableCellValueChangeListener<EditDeviceConnectorTableRow>()
		{

			@Override public void cellValueChanged(EditDeviceConnectorTableRow sourceItem,
					ColumnInformation<EditDeviceConnectorTableRow> sourceColumnInfo, Object oldValue, Object newValue)
			{
				String sourceColumnInfoName = sourceColumnInfo.getName();
				EditDeviceConnectorColumns columnUpdates =
						EditDeviceConnectorColumns.getColumnByName(sourceColumnInfoName);
				assert columnUpdates != null;
				columnUpdates.update(sourceItem, newValue);
				String oldValueString = oldValue != null ? oldValue.toString().trim() : null;
				String newValueString = newValue != null ? newValue.toString().trim() : null;
				if (!StringUtils.equals(oldValueString, newValueString)) {

					if (EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.equalsName(sourceColumnInfoName)) {

						if (!StringUtils.isBlank(sourceItem.getDeviceConnectorName())) {

							dcNamePartNumberValidator
									.validateConnectorPartnumberChange(sourceItem.getDeviceConnectorName(),
											oldValueString,
											newValueString);
						}
						invalidPartnumberHandler.validatePartnumberChange(oldValueString, newValueString);
						if (currentSelection.getRowItems().contains(sourceItem)) {
							currentSelection.updatePartnumber(oldValueString, newValueString);
							if (currentSelection.isSymbolHighlightApplicable()) {
								highlightSymbolViewer.accept(currentSelection);
							}
							else {
								highlightSymbolViewer.accept(null);
							}
						}
					}
					else if (EditDeviceConnectorColumns.DEVICECONNECTORNAME.equalsName(sourceColumnInfoName)) {

						dcNamePartNumberValidator
								.validateConnectorNameChange(oldValueString,
										newValueString, sourceItem.getDeviceConnectorPartNumber());
						if (!StringUtils.isBlank(sourceItem.getDeviceConnectorPinName())) {
							dcNamePinNameValidator.validateConnectorNameChange(oldValueString, newValueString,
									sourceItem);
						}
						dcNamePinNameValidator.handleConnectorNameChange(oldValueString, newValueString);

						nameLengthValidator.validateNameLength(oldValueString, newValueString);
						if (currentSelection.getRowItems().contains(sourceItem)) {

							currentSelection.updateConnectorName(oldValueString, newValueString);
							if (currentSelection.isSymbolHighlightApplicable()) {
								highlightSymbolViewer.accept(currentSelection);
							}
							else {
								highlightSymbolViewer.accept(null);
							}
						}
					}
					else if (EditDeviceConnectorColumns.DEVICECONNECTORPIN.equalsName(sourceColumnInfoName)) {

						if (!StringUtils.isBlank(sourceItem.getDeviceConnectorName())) {
							dcNamePinNameValidator.validateDCPinChange(oldValueString, newValueString, sourceItem);
						}
						if (currentSelection.getRowItems().contains(sourceItem)) {
							currentSelection.updateCavityName(oldValueString, newValueString);
						}
						final Collection<String> cavities = new HashSet<>(currentSelection.getCavities());
						invokeInSwingThread(() -> {
							if (connectorViewPanel.getConnectorViewModel() != null) {
								connectorViewPanel.highlightPinsOnSymbolView(cavities);
							}
						});

						nameLengthValidator.validateNameLength(oldValueString, newValueString);
					}
				}
				dcNamePartNumberValidator.refreshCellsWithErrors();
				dcNamePinNameValidator.refreshCellsWithErrors();

				updateOkButtonState(sourceItem);
			}

			private void updateOkButtonState(EditDeviceConnectorTableRow sourceItem)
			{
				dcNamePartNumberValidator.handlePartNumberAndConnectorNameChange(sourceItem);

				dcNamePartNumberValidator.handlePartNumberAndCavityNameChange(sourceItem);

				dcNamePinNameValidator.handleCavityAndConnectorNameChange(sourceItem);
				String error = dcNamePartNumberValidator.getErrorInCurrentTableState();
				if (error == null) {
					error = dcNamePinNameValidator.getErrorInCurrentTableState();
				}
				if (error == null) {
					error = invalidPartnumberHandler.getErrorInCurrentState();
				}
				if (error == null) {
					error = nameLengthValidator.getErrorInCurrentState();
				}
				final String errorFound = error;
				invokeInSwingThread(() -> {
					JButton okButton = getOkButton();
					if (errorFound != null) {
						okButton.setEnabled(false);
						okButton.setToolTipText(errorFound);
					}
					else {
						okButton.setEnabled(true);
						okButton.setToolTipText(null);
					}
				});
			}
		};
	}

	private static class EditDCPartnumberHandler
	{

		private final ReverseMap<Integer, EditDeviceConnectorTableRow> selectedRows;

		private final BiFunction<ICriteria<? extends ILibraryObject>, PartSelectionContext, ILibraryPartSelection>
				partSelector;

		EditDCPartnumberHandler(Map<Integer, EditDeviceConnectorTableRow> selectedRows,
				BiFunction<ICriteria<? extends ILibraryObject>, PartSelectionContext, ILibraryPartSelection> partSelector)
		{
			this.selectedRows = new ReverseMap<>(selectedRows);
			this.partSelector = partSelector;
		}

		boolean areValidSelections()
		{
			String firstDeviceName = null;
			for (EditDeviceConnectorTableRow aRow : selectedRows.values()) {
				String currentDeviceName = aRow.getDeviceConnectorName();
				if (currentDeviceName != null) {

					if (firstDeviceName == null) {
						firstDeviceName = currentDeviceName;
					}
					if (!StringUtils.equals(firstDeviceName, currentDeviceName)) {
						return false;
					}
				}
			}
			return true;
		}

		void handleSelections(@NotNull Table<EditDeviceConnectorTableRow> givenTable)
		{
			ILibraryPartSelection libraryPartSelection =
					getLibraryConnectorSelected(selectedRows.size());
			if (libraryPartSelection == null) {
				return;
			}
			ILibraryConnector libraryObjectSelected =
					CommonUtils.cast(libraryPartSelection.getSelectedObject(), ILibraryConnector.class);
			Platform.runLater(() -> {
				updateSelectedPartNumberOnTable(givenTable, libraryObjectSelected);
			});
		}

		private void updateSelectedPartNumberOnTable(@NotNull Table<EditDeviceConnectorTableRow> givenTable,
				@Nullable ILibraryConnector libraryObjectSelected)
		{
			if (libraryObjectSelected == null) {
				return;
			}

			String libraryPartNumber = PartNumberHelper.getCombinedPartNumber(libraryObjectSelected.getPartNumber()
					, libraryObjectSelected.getPartRevision());

			Integer[] columnIndex = new Integer[]{0};
			AlphaNumComparator<String> comparator = AlphaNumComparator.getUniqueObjectAsUniqueComparator();
			givenTable.columns().forEach(aCol -> {

				String colName = aCol.getName();
				if (EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.equalsName(colName)) {
					selectedRows.keySet().forEach(
							rowIndex -> givenTable.setValue(rowIndex, columnIndex[0], libraryPartNumber, true));
				}
				else if (EditDeviceConnectorColumns.DEVICECONNECTORPIN.equalsName(colName)) {

					Comparator<ILibraryCavity> cavityComparator =
							(o1, o2) -> {
								Integer sortOrder1 = o1.getSortOrder();
								Integer sortOrder2 = o2.getSortOrder();
								return sortOrder1.compareTo(sortOrder2);
							};

					Iterator<ILibraryCavity> sortedCavityIter = CollectionUtils.createSortedList(
							libraryObjectSelected.getCavities(), cavityComparator).iterator();

					Comparator<EditDeviceConnectorTableRow> rowComparator =
							(o1, o2) -> comparator.compare(o1.getDevicePinName(), o2.getDevicePinName());

					List<EditDeviceConnectorTableRow> sortedRowSelections =
							CollectionUtils.createSortedList(selectedRows.values(), rowComparator);

					sortedRowSelections.forEach(aRow -> givenTable
							.setValue(selectedRows.getReverseMap().get(aRow), columnIndex[0],
									sortedCavityIter.next().getName(), true));
				}
				columnIndex[0]++;
			});
		}

		@Nullable private ILibraryPartSelection getLibraryConnectorSelected(int pinCount)
		{

			ICriteriaFactory criteriaFactory = UtilsHelper.getCHSUtils().getCriteriaFactory();
			ICriteria<? extends ILibraryObject> criteria =
					criteriaFactory.createCriteria(ILibraryObject.GroupType.CONNECTOR.getLibraryObjectClass());
			criteria.setObjectLimit(-1);

			criteria.restriction(
					Restrictions.EQ(IAttributeTypes.GROUP_NAME, ILibraryObject.GroupType.CONNECTOR));
			criteria.restriction(Restrictions.GE(IAttributeTypes.NUM_CAVITIES, pinCount));

			PartSelectionContext partSelectionContext = new PartSelectionContext();

			partSelectionContext.setSelectionFilter(LibraryCriteriaHelper.getSelectionFilterForNoSymbols(
					null, null,
					LibraryCriteriaHelper
							.getCustomerDetailsFromScopes(CAFUtils.getInstance().getActiveDesignContainer(),
									CAFUtils.getInstance().getCurrentProject())
			));

			return partSelector.apply(criteria, partSelectionContext);
		}
	}

	@Nullable ILibraryPartSelection getSelectedPart(
			ICriteria<? extends ILibraryObject> criteria,
			PartSelectionContext partSelectionContext)
	{
		ILibraryPartSelector partSelector = Library.getInstance()
				.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
		return partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
				partSelectionContext, ConfigurationTypeEnum.LOGICAL,
				CAFUtils.getInstance().getActiveDesignContainer());
	}
}
