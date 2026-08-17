package chs.caplets.logic.actions.ui;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caplets.shared.ForeignDesignChangesHandler;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.ILogicConnectivity;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedUsage;
import chs.common.*;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.AlphaNumComparator;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.SharedFunctionMessageHelper;
import chs.utility.helpers.SharedFunctionMessageState;
import chs.utility.logic.FunctionMessagesDictionarySignalHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.ui.OkCancelDialog;
import chs.utility.ui.pintable.ColumnChooserObjectType;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.TableModel;
import com.mentor.capital.javafx.table.cell.ITableCell;
import com.mentor.capital.javafx.table.cell.ITableCellValueProvider;
import com.mentor.capital.javafx.table.cell.TableColumnType;
import com.mentor.capital.javafx.table.common.ComparableImage;
import com.mentor.capital.javafx.table.common.ITableCellStateHandler;
import com.mentor.capital.javafx.table.menu.DefaultMenuItem;
import com.mentor.capital.javafx.table.strategy.ITableColumnMenuItemProvider;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ManageSignalsDialog extends SimpleOkCancelDialog implements ITableCellStateHandler<SignalData>
{

	public static final int DIALOG_WIDTH = 600;
	public static final int DIALOG_HEIGHT = 500;
	public static final int ICON_COLUMN_WIDTH = 50;
	public static final int SIGNAL_NAME_COLUMN_WIDTH = 200;
	public static final String SIGNAL_NAME = "Name";
	public static final String DICTIONARY_SIGNAL_NAME = "Dictionary Name";
	public static final String SIGNAL_CHECKBOX = "Active";
	public static final String ICON = "Active Signal Icon";
	public static final String MANAGE_SIGNAL_TABLE_ID = "Manage Signals";
	private BufferedImage bi;
	private Icon signalIcon;
	private IFunctionMessage m_Message;
	private Collection<IFunctionConductor> orphanSignal;
	private Collection<SignalData> signalData;
	private TableDataStorage<SignalData> tableDataStorage;
	private FunctionMessagesDictionarySignalHelper dictionarySignalHelper;
	private AlphaNumComparator<String> alphaNumComparator;
	private Table<SignalData> table;
	private ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges m_undoHandler;

	public ManageSignalsDialog(@Nullable Frame owner, @Nullable String messageName, boolean modal,
			IFunctionMessage m_Message)
	{
		super(null, owner, getDialogTitle(m_Message), modal);
		this.m_Message = m_Message;
		dictionarySignalHelper = new FunctionMessagesDictionarySignalHelper(m_Message);
	}

	protected OkCancelDialog createDialog()
	{
		rememberSize(true);
		setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JPanel(), BorderLayout.EAST);
		panel.add(new JPanel(), BorderLayout.WEST);
		panel.add(new JPanel(), BorderLayout.NORTH);

		JPanel tablePanel = createTablePanel(signalData);

		tablePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
				BorderFactory.createLoweredBevelBorder()));

		panel.add(tablePanel, BorderLayout.CENTER);
		getOkButton().setText(ResourceMgr.getString(ManageSignalsDialog.class,
				"ManageSignalsDialog.apply.text"));
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		ActionListener actionListener = (e) -> setViewCancelled();

		panel.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		InputMap inputMap = panel.getInputMap();
		inputMap.put(stroke, actionListener);
		panel.setName("SignalTable");

		getContentPane().add(panel);
		getRootPane().setDefaultButton(null);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setNameExplicitly("ManageSignalDialog");
		return this;
	}

	protected JPanel createTablePanel(Collection<SignalData> data)
	{
		JPanel tablePanel = new JPanel(new BorderLayout());
		ColumnChooserObjectType.ensureDummyObjectsAreCreated();
		final JFXPanel fxPanel = new JFXPanel();
		fxPanel.setName(MANAGE_SIGNAL_TABLE_ID);

		tablePanel.add(fxPanel, BorderLayout.CENTER);
		doCreateTablePanel(fxPanel, data);
		return tablePanel;
	}

	protected void doCreateTablePanel(JFXPanel fxPanel, Collection<SignalData> data)
	{
		Platform.runLater(() -> initFX(fxPanel, data));
	}

	public boolean showDialog()
	{

		initData();

		createDialog();

		if (!Environment.isHeadless()) {
			setVisible(true);
		}
		return !isCancelled();
	}

	protected void initData()
	{
		tableDataStorage = new TableDataStorage<>();
		createIcon();
		createSignalCollection();
	}

	private void createIcon()
	{
		signalIcon = CHSImageLoader.loadImageIcon(CHSImages.FUNCTION_COND_ACTIVE_ICON);
		bi = new BufferedImage(
				signalIcon.getIconWidth(),
				signalIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		signalIcon.paintIcon(null, g, 0, 0);
		g.dispose();
	}

	private void createSignalCollection()
	{
		final Set<String> dictSignals = new HashSet<>(dictionarySignalHelper.getDictionarySignalsNameswithRev());
		orphanSignal = new ArrayList<>();
		alphaNumComparator = new AlphaNumComparator<>(true, false, true);
		List<SignalData> signalsList = m_Message.getActiveSignals()
				.stream()
				.map((item) -> getSignalItem(item, dictSignals))
				.collect(Collectors.toList());

		for (String item : dictSignals) {
			signalsList.add(new SignalData(null, "", item, false, true));
		}

		Comparator<SignalData> checkBoxComparator = ((o1, o2) -> Boolean.compare(o2.isActive(), o1.isActive()));
		Comparator<SignalData> instanceNameComparator =
				((o1, o2) -> alphaNumComparator.compare(o1.getInstanceName(), o2.getInstanceName()));
		Comparator<SignalData> dictionaryNameComparator =
				((o1, o2) -> alphaNumComparator.compare(o1.getDictionaryName(), o2.getDictionaryName()));
		signalsList
				.sort(checkBoxComparator.thenComparing(instanceNameComparator).thenComparing(dictionaryNameComparator));

		signalData = signalsList;
	}

	SignalData getSignalItem(IFunctionConductor item, Set<String> dictSignals)
	{
		IAttribute dictAttribute = item.getAttribute(IAttributeTypes.DICTIONARY_SIGNAL_NAME);
		String dictName =
				((dictAttribute != null && dictAttribute.getAsObject() != null) ? dictAttribute.getString() : "");
		IAttribute dictRevAttribute = item.getAttribute(IAttributeTypes.DICTIONARY_SIGNAL_REVISION);
		String dictRev =
				((dictRevAttribute != null && dictRevAttribute.getAsObject() != null) ? dictRevAttribute.getString() :
						"");
		String key = dictName + ":" + dictRev;
		if (dictSignals.contains(key)) {
			dictSignals.remove(key);
			return new SignalData(item, item.getName(), key, true, true);
		}
		orphanSignal.add(item);
		return new SignalData(item, item.getName(), "", false, false);
	}

	protected void initFX(JFXPanel fxPanel, Collection<SignalData> data)
	{
		Scene scene = createScene(data);
		PlatformImpl.runAndWait(() -> {
			fxPanel.setScene(scene);
		});
		fxPanel.requestFocusInWindow();
	}

	private Scene createScene(Collection<SignalData> data)
	{
		table = createTable();
		table.addData(data).addColumns(createColumns());

		addMenuforCheckboxColumn();

		table.setColumnSortable(Collections.singleton(ICON), false);
		table.setColumnPreferredWidth(Collections.singleton(ICON), ICON_COLUMN_WIDTH);
		table.setColumnPreferredWidth(Arrays.asList(SIGNAL_NAME, DICTIONARY_SIGNAL_NAME), SIGNAL_NAME_COLUMN_WIDTH);

		table.setComparatorForColumn(Arrays.asList(SIGNAL_NAME, DICTIONARY_SIGNAL_NAME),
				(o1, o2) -> alphaNumComparator.compare(o1.toString(), o2.toString()));

		String managedesigns = ResourceMgr.getStylesheet(ManageSignalsDialog.class, "managesignalstable.css");
		Scene scene = new Scene(table);
		JFXFlatUIUtils.getInstance().setFlatUIFor(scene);
		scene.getStylesheets().add(managedesigns);
		table.setCellStateHandler(this);

		return (scene);
	}

	private void addMenuforCheckboxColumn()
	{
		ToggleGroup togr = new ToggleGroup();
		Predicate<ColumnInformation<SignalData>> isCheckboxColumn =
				columnInformation -> SIGNAL_CHECKBOX.equals(columnInformation.getName());
		table.addMenuItemProvider(new ClearAll<SignalData>(isCheckboxColumn));
		table.addMenuItemProvider(new SelectAll<SignalData>(isCheckboxColumn));
		table.addMenuItemProvider(new Separator<SignalData>(isCheckboxColumn));
		table.addMenuItemProvider(new ShowChecked<SignalData>(table, isCheckboxColumn, togr));
		table.addMenuItemProvider(new ShowUnChecked<SignalData>(table, isCheckboxColumn, togr));
		table.addMenuItemProvider(new ShowAll<SignalData>(isCheckboxColumn, togr));
	}

	protected Collection<ColumnInformation<SignalData>> createColumns()
	{
		Collection<ColumnInformation<SignalData>> columns = new ArrayList<>();
		columns.add(createSignalIconColumn());
		columns.add(createCheckboxColumn());
		columns.add(createInstanceSignalNameColumn());
		columns.add(createDictionarySignalNameColumn());
		return columns;
	}

	protected ColumnInformation<SignalData> createSignalIconColumn()
	{
		return new ColumnInformation<SignalData>(StringUtils.BLANK,
				ICON,
				a ->
				{
					return new ComparableImage(SwingFXUtils.toFXImage(bi, null), signalIcon.toString());
				},
				null,
				TableColumnType.typeImage())
		{
			@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
			{
				return false;
			}
		};
	}

	protected ColumnInformation<SignalData> createCheckboxColumn()
	{
		return new ColumnInformation<SignalData>(ResourceMgr.getString(ManageSignalsDialog.class,
				"ManageSignalsDialog.checkbox.text"), SIGNAL_CHECKBOX, (signal) -> signal.isActive(),
				(signal, str) -> updateUserChanges(signal, str.toString()),
				TableColumnType.typeBoolean())
		{
			@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
			{
				return false;
			}
		};
	}

	private void updateUserChanges(SignalData signal, String str)
	{
		createUndoContainer();
		IFunctionConductor functionConductor = signal.getiFunctionConductor();
		if ("false".equals(str) && signal.isActive()
				&& functionConductor != null) {
			removeSignal(functionConductor);
		}
		else if ("true".equals(str) && !signal.isActive()) {
			addSignal(signal);
		}
	}

	private void createUndoContainer()
	{
		if (m_Message != null && m_Message.getSharedConductor() != null && m_Message.getDesign() != null &&
				m_undoHandler == null) {
			ICapletController sourceDesignController =
					getControllerForDesign(m_Message.getDesign());
			if(sourceDesignController != null){
				m_undoHandler =
						createUndoHandlerForDesign(sourceDesignController);
			}
		}
	}

	@NotNull protected ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges createUndoHandlerForDesign(
			ICapletController sourceDesignController)
	{
		return new ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges(sourceDesignController);
	}

	@Nullable protected ICapletController getControllerForDesign(IDesignContainer design)
	{
		return CAFUtils.getInstance().getControllerForDesign(design);
	}

	private void addSignal(SignalData signal)
	{
		IFunctionConductor newActiveSignal =
				dictionarySignalHelper.createSignalFromDictionaryForMessage(signal.getDictionaryName());
		if (m_Message.getSharedConductor() != null && newActiveSignal != null) {
			ISharedFunctionMessage sharedFunctionMessage = (ISharedFunctionMessage) m_Message.getSharedConductor();
			if(m_Message.getDesign() != null){
				SharedFunctionMessageHelper
						.createNewSharedMessageSignal(newActiveSignal, sharedFunctionMessage,
								m_Message.getLogicDesign());
			}
		}
	}

	private void removeSignal(IFunctionConductor functionConductor)
	{
		if (m_Message.getSharedConductor() != null) {
			ISharedFunctionMessage sharedMessage = (ISharedFunctionMessage) m_Message.getSharedConductor();
			ISharedMessageSignal sharedSignal =
					(ISharedMessageSignal) functionConductor.getSharedConductor();
			sharedMessage.removeSignal(sharedSignal);
		}
		m_Message.removeSignal(functionConductor);
	}

	protected ColumnInformation<SignalData> createInstanceSignalNameColumn()
	{
		return new ColumnInformation<SignalData>(ResourceMgr.getString(ManageSignalsDialog.class,
				"ManageSignalsDialog.signalName.text"), SIGNAL_NAME, (signal) -> signal.getInstanceName())
		{
			@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
			{
				return !defaultMenuItemKey.equals(DefaultMenuItem.Hide);
			}
		};
	}

	protected ColumnInformation<SignalData> createDictionarySignalNameColumn()
	{
		return new ColumnInformation<SignalData>(ResourceMgr.getString(ManageSignalsDialog.class,
				"ManageSignalsDialog.dictionarySignalName.text"), DICTIONARY_SIGNAL_NAME,
				(signal) -> signal.getDictionaryName())
		{
			@Override public boolean displayDefaultMenuItem(DefaultMenuItem defaultMenuItemKey)
			{
				return !defaultMenuItemKey.equals(DefaultMenuItem.Hide);
			}
		};
	}

	@NotNull protected Table<SignalData> createTable()
	{
		return new Table<>(MANAGE_SIGNAL_TABLE_ID,
				new TableModel<SignalData>(tableDataStorage));
	}

	protected static String getDialogTitle()
	{
		return ResourceMgr.getString(ManageSignalsDialog.class,
				"ManageSignalsDialog.title.text");
	}

	protected static String getDialogTitle(IFunctionMessage message)
	{
		ISharedConductor sharedConductor = message.getSharedConductor();
		return getDialogTitle() + " " + message.getName() + (sharedConductor == null ? "" :
				":" + sharedConductor.getRevision());
	}

	@NotNull public String getHelpID()
	{
		// Note: donot change helpID, if changed intimate the documentation team
		return "Manage_Signals_Dialog";
	}

	int getColumnNumber(@Nullable String s)
	{
		if (s == null) {
			return -1;
		}
		List<String> columnNames = table.columns().map((col) -> col.getName()).collect(Collectors.toList());
		int columnNumber = -1;
		for (int i = 0; i < columnNames.size(); i++) {
			if (s.equals(columnNames.get(i))) {
				columnNumber = i;
				break;
			}
		}
		return columnNumber;
	}

	void setColumn(boolean flag)
	{
		int columnNumber = getColumnNumber(SIGNAL_CHECKBOX);
		assert columnNumber != -1;
		long totalfilteredSignals = table.filteredData().count();
		for (int i = 0; i < totalfilteredSignals; i++) {
			table.setValue(i, columnNumber, flag, false);
		}
	}

	@Override public boolean isEditable(@NotNull ITableCellValueProvider<SignalData> cell)
	{
		SignalData item = cell.getRowItem();
		return item == null || item.isPresentInDictionary();
	}

	@Override public void updateStyle(@NotNull ITableCell<SignalData> tableCell, @NotNull Node styleable)
	{
	}

	@Override public boolean isValid(@NotNull ITableCell<SignalData> cell)
	{
		return true;
	}

	/* For Test Pupose */
	public Collection<SignalData> getSignalData()
	{
		return signalData;
	}

	/* For Test Pupose */
	public Table<SignalData> getTable()
	{
		return table;
	}

	public void apply()
	{
		tableDataStorage.apply();
		for (IFunctionConductor item : orphanSignal) {
			removeSignal(item);
		}
		final ISharedConductor sharedMessage = m_Message.getSharedConductor();
		if (sharedMessage != null) {
			fixUpMessageStructureInOtherDesigns(getLoadedDesigns());
			sharedMessage.flush();
			if (m_Message.getDesign() != null && m_Message.getLogicDesign() instanceof IFunctionLogicDesign) {
				saveDesign((IFunctionLogicDesign) m_Message.getLogicDesign());
				if (m_undoHandler != null) {
					m_undoHandler.close();
				}
			}
		}
	}

	@NotNull protected Collection<IUID> getLoadedDesigns()
	{
		return ((IPrivilegedDesignMgr)m_Message.getProject().getDesignMgr()).getLoadedDesigns();
	}

	public void cleanup()
	{
		if (m_undoHandler != null) {
			m_undoHandler.close();
		}
	}

	protected void fixUpMessageStructureInOtherDesigns(Collection<IUID> loadedDesigns)
	{
		Collection<ISharedUsage> sharedUsages = getProjectSharedUsages(loadedDesigns);

		for (ISharedUsage sharedUsage : sharedUsages) {
			IUID designUId = sharedUsage.getDesignUID();
			IUIDObject design = UIDUtils.getUIDObject(designUId);
			if (design instanceof IFunctionLogicDesign && ((ILockable) design).isLocked() &&
					design != m_Message.getLogicDesign()) {
				IFunctionLogicDesign functionalDesign = ((IFunctionLogicDesign) design);
				IFunctionMessage message = (IFunctionMessage) sharedUsage.getLogicObject();
				if (functionalDesign.getConnectivity() != null && message != null) {
					updateMessageStructureInForeignDesign(functionalDesign, message);
				}
			}
		}
	}

	private void updateMessageStructureInForeignDesign(IFunctionLogicDesign functionalDesign, IFunctionMessage message)
	{
		ICapletController sourceDesignController =
				getControllerForDesign(functionalDesign);
		if(sourceDesignController != null){
			ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges
					undoIdler =
					createUndoHandlerForDesign(sourceDesignController);
			fixUpMessageStructure(functionalDesign, message);
			saveDesign(functionalDesign);
			undoIdler.close();
		}
	}

	protected void fixUpMessageStructure(IFunctionLogicDesign functionalDesign, IFunctionMessage message)
	{
		if(functionalDesign.getConnectivity() instanceof ILogicConnectivity){
			SharedFunctionMessageHelper
					.fixUpFunctionMessageStructure((ILogicConnectivity) functionalDesign.getConnectivity(),
							message, new SharedFunctionMessageState());
		}
	}

	protected Collection<ISharedUsage> getProjectSharedUsages(Collection<IUID> loadedDesigns)
	{
		return LogicUtils
				.getProjectSharedUsages(Arrays.asList(m_Message.getSharedConductor()), m_Message.getProject(),
						loadedDesigns, false);
	}

	protected void saveDesign(IFunctionLogicDesign design)
	{
		Consumer<ILogicDesign> saveHandler = ForeignDesignChangesHandler.createdSaveHandler();
		saveHandler.accept(design);
	}

	public IFunctionMessage getM_Message()
	{
		return m_Message;
	}

	public void setDictionarySignalHelper(FunctionMessagesDictionarySignalHelper dictionarySignalHelper)
	{
		this.dictionarySignalHelper = dictionarySignalHelper;
	}

	private class SelectAll<T> implements ITableColumnMenuItemProvider<T>
	{

		private Predicate<ColumnInformation<T>> isCheckboxColumn;

		SelectAll(Predicate<ColumnInformation<T>> isCheckboxColumn)
		{
			this.isCheckboxColumn = isCheckboxColumn;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(selectAllMenuItem()) : Collections.emptyList();
		}

		private MenuItem selectAllMenuItem()
		{
			MenuItem menuItem = new MenuItem(ResourceMgr.getString(ManageSignalsDialog.class,
					"ManageSignalsDialog.selectAll.text"));
			menuItem.setOnAction(event -> setColumn(true));
			return menuItem;
		}
	}

	private class ClearAll<T> implements ITableColumnMenuItemProvider<T>
	{

		private Predicate<ColumnInformation<T>> isCheckboxColumn;

		ClearAll(Predicate<ColumnInformation<T>> isCheckboxColumn)
		{
			this.isCheckboxColumn = isCheckboxColumn;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(clearAllMenuItem()) : Collections.emptyList();
		}

		private MenuItem clearAllMenuItem()
		{
			MenuItem menuItem = new MenuItem(ResourceMgr.getString(ManageSignalsDialog.class,
					"ManageSignalsDialog.clearAll.text"));

			menuItem.setOnAction(event -> setColumn(false));
			return menuItem;
		}
	}

	private class ShowAll<T> implements ITableColumnMenuItemProvider<T>
	{

		private ToggleGroup tog;
		private Predicate<ColumnInformation<T>> isCheckboxColumn;

		ShowAll(Predicate<ColumnInformation<T>> isCheckboxColumn, ToggleGroup tg)
		{
			this.isCheckboxColumn = isCheckboxColumn;
			tog = tg;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(showAllMenuItem()) : Collections.emptyList();
		}

		private MenuItem showAllMenuItem()
		{
			RadioMenuItem menuItem = new RadioMenuItem(ResourceMgr.getString(ManageSignalsDialog.class,
					"ManageSignalsDialog.all.text"));
			menuItem.setToggleGroup(tog);
			menuItem.setOnAction(event -> table.filter(item -> true));
			menuItem.setSelected(true);
			return menuItem;
		}
	}

	private static class ShowChecked<T> implements ITableColumnMenuItemProvider<T>
	{

		private Predicate<ColumnInformation<T>> isCheckboxColumn;
		private Table<T> t;
		private ToggleGroup togr;

		ShowChecked(Table<T> tab, Predicate<ColumnInformation<T>> isCheckboxColumn, ToggleGroup tg)
		{
			togr = tg;
			this.isCheckboxColumn = isCheckboxColumn;
			t = tab;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(showCheckedMenuItem(columnInformation)) : Collections.emptyList();
		}

		private MenuItem showCheckedMenuItem(ColumnInformation<T> columnInformation)
		{
			RadioMenuItem menuItem = new RadioMenuItem(ResourceMgr.getString(ManageSignalsDialog.class,
					"ManageSignalsDialog.checked.text"));
			menuItem.setOnAction(
					event -> t.filter((item) -> "true".equals(t.getStringValueForCell(item, columnInformation))));
			menuItem.setToggleGroup(togr);
			return menuItem;
		}
	}

	private static class ShowUnChecked<T> implements ITableColumnMenuItemProvider<T>
	{

		private ToggleGroup tgr;
		private Table<T> t;
		private Predicate<ColumnInformation<T>> isCheckboxColumn;

		ShowUnChecked(Table<T> tab, Predicate<ColumnInformation<T>> isCheckboxColumn, ToggleGroup tg)
		{
			t = tab;
			this.isCheckboxColumn = isCheckboxColumn;
			tgr = tg;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(showUnCheckedMenuItem(columnInformation)) : Collections.emptyList();
		}

		private MenuItem showUnCheckedMenuItem(ColumnInformation<T> columnInformation)
		{
			RadioMenuItem menuItem = new RadioMenuItem(ResourceMgr.getString(ManageSignalsDialog.class,
					"ManageSignalsDialog.unchecked.text"));
			menuItem.setToggleGroup(tgr);
			menuItem.setOnAction(
					event -> t.filter(item -> "false".equals(t.getStringValueForCell(item, columnInformation))));
			return menuItem;
		}
	}

	private static class Separator<T> implements ITableColumnMenuItemProvider<T>
	{

		private Predicate<ColumnInformation<T>> isCheckboxColumn;

		Separator(Predicate<ColumnInformation<T>> isCheckboxColumn)
		{
			this.isCheckboxColumn = isCheckboxColumn;
		}

		@NotNull @Override public List<MenuItem> getMenuItemsFor(@NotNull ColumnInformation<T> columnInformation)
		{
			return isCheckboxColumn.test(columnInformation) ?
					Collections.singletonList(separatorMenuItem()) : Collections.emptyList();
		}

		private MenuItem separatorMenuItem()
		{
			return new SeparatorMenuItem();
		}
	}
}
