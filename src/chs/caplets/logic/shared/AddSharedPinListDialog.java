/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.logic.shared;

import chs.caf.CAFUtils;
import chs.caf.helpers.CAFSharedUpdater;
import chs.caplets.logic.BasicAddPinListDialog;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import chs.common.IMultiSymbolledPinlist;
import chs.common.RefreshStatusEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.permission.PermissionEnum;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utility.SymbolUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.security.PermissionHelper;
import chs.utility.ui.ISymbolTreeController;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.PinSelectionConfigurationParams;
import chs.utility.ui.PinSelectionUserOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A dialog for shared pin usage in capital logic.
 * <p>
 * s@created October 3, 2002
 */
public class AddSharedPinListDialog extends BasicAddPinListDialog implements
		ISharedPinListInfoProvider
{

	private SharedAbstractableTree m_pinListView;
	private LockedSharedObjectFilter m_lockFilter = new LockedSharedObjectFilter();
//	// To select the proper PinList

	private PinListTypeEnum m_pltype;
	private ISharedPinList m_sharedPinList;
	private ISharedPinReservationView m_sharedPinReservationView;

	public static final String DevicePanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Device.text");
	public static final String FunctionPanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Function.text");
	public static final String PlugPanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Plug.text");
	public static final String JackPanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Jack.text");
	public static final String InlinePanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Inline.text");
	public static final String SplicePanelTitle =
			ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.Splice.text");
	public static final String InterconnectConnectorPanelTitle = ResourceMgr
			.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.InterconnectConnector.text");
	public static final String RingTerminalPanelTitle = ResourceMgr
			.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.TitledBorder.RingTerminal.text");

	public AddSharedPinListDialog(@Nullable Frame frame, String title, @Nullable PinListTypeEnum pltype)

	{
		super(frame, title, true);

		m_pltype = pltype;
		rememberSize(true);
		try {
			addComponents();
			hookupButtons();
			pack();
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, false);
		}
	}

	/**
	 * Constructor for the AddSharedPinListDialog object
	 *
	 * @param frame Description of the Parameter
	 * @param title Description of the Parameter
	 */
	public AddSharedPinListDialog(@Nullable Frame frame, String title)
	{
		this(frame, title, null);
	}

	public Consumer<List<?>> getSelectedPinsHandler()
	{

		return new Consumer<List<?>>()
		{
			@Override public void accept(List<?> objects)
			{
				//Not need to change place pin button status as place button enablement does not depend on selected pins.
				placeAsStackButtonStatusUpdate(objects);
			}
		};
	}

	@Override protected void addComponents()
	{
		m_pinListView = new SharedAbstractableTree(null);
		m_pinListView.registerFilter(m_lockFilter);
		m_pinListView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_pinListView.setName("list_curr_sharedObj");
		m_pinListView.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				TreePath path = m_pinListView.getSelectionPath();
				DefaultMutableTreeNode sel = path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;

				ISharedPinList newSPL = sel != null && sel.getUserObject() instanceof ISharedPinList ?
						(ISharedPinList) sel.getUserObject() : null;
				ISharedPinList oldSPL = m_sharedPinList;
				LibraryHelper.preLoadLibraryData(Collections.singleton(newSPL));
				if (newSPL != oldSPL) {
					if (oldSPL != null) {
						SharedPinListHelper.unlock(oldSPL);
					}

					if (newSPL != null) {
						newSPL = lockAndRefreshNewPinList(newSPL, path);
					}
					m_sharedPinList = newSPL;
				}

				if (oldSPL != m_sharedPinList && m_loadSharedDetailsOption != null) {
					m_loadSharedDetailsOption.setValue(false);
				}

				boolean canAdd = setupInitialStatusAfterSelection();
				updatePinSelectionPanel(canAdd, m_sharedPinList);
				applyApplicabiltiy(canAdd);
			}
		});

		m_pinListPanel = new JPanel();
		m_pinListPanel.setLayout(new BorderLayout());
		m_pinListScrollPane = new JScrollPane(m_pinListView);
		Dimension d = new Dimension(400, 150);
		m_pinListScrollPane.setPreferredSize(d);
		m_pinListScrollPane.setMinimumSize(d);
		m_pinListScrollPane.setMaximumSize(d);
		m_pinListPanel.add(m_pinListScrollPane, BorderLayout.CENTER);
		Border lineBorder = BorderFactory.createLineBorder(Color.black);
		m_pinListPanel.setBorder(BorderFactory.createTitledBorder(lineBorder));
		//m_pinListPanel.add(Box.createHorizontalStrut(100), BorderLayout.WEST);
		//m_pinListPanel.add(Box.createHorizontalStrut(100), BorderLayout.EAST);
		getRootPane().setDefaultButton(getOkButton());
		super.addComponents();
	}

	private void updatePinSelectionPanel(boolean canAdd, @Nullable ISharedPinList sharedPinList)
	{
		if (!canAdd && sharedPinList != null) {
			//ensure the change of icon and tooltip.
			m_lockFilter.setLockedOut(sharedPinList, true);
		}
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if (m_pinSelectionPanel != null) {
				ISharedPinReservationView pinview =
						FactoryMgr.getCommonFactory().constructSharedPinReservationView(sharedPinList);
				PinSelectionUserOptions userOptions = createUserSelectionOption();
				//ensure the pin/symbol content is cleared from panel if shared object is restricted.
				final IMultiSymbolledPinlist multiSymbolledPinlist = canAdd ? sharedPinList : null;
				if (!m_pinSelectionPanel.reset(multiSymbolledPinlist, m_curDesign, pinview, userOptions)) {
					m_success = false;
					setVisible(false);
				}
			}
		}
		finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	@Nullable private ISharedPinList lockAndRefreshNewPinList(@NotNull ISharedPinList newSPL, @NotNull TreePath path)
	{
		if (newSPL.getType() == PinListTypeEnum.TypeSplice &&
				!PermissionHelper.hasPermission(PermissionEnum.SHARED_OBJECTS)) {
			m_lockFilter.setLockedOut(newSPL, true);
			return null;
		}
		if (newSPL.getType() != PinListTypeEnum.TypeSplice && !lockSharedPinList(newSPL)) {
			m_lockFilter.setLockedOut(newSPL, true);
			return null;
		}
		RefreshStatusEnum rs = newSPL.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			LogicActionMessageHelper.warnDeleted(newSPL);
			DefaultTreeModel model = (DefaultTreeModel) m_pinListView.getModel();
			MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
			model.removeNodeFromParent(node);
			return null;
		}
		m_lockFilter.setLockedOut(newSPL, false);
		return newSPL;
	}

	@Override protected PinSelectionCommonPanel createPinSelectionPanel(ISymbolTreeController symbolTreeController,
			Consumer<List<?>> selectedPinsHandler)
	{
		if (m_pltype != null && m_pltype.isFunction()) {
			return new PinSelectionCommonPanel(this, FactoryMgr.getDrawFactory(),
					CAFUtils.getInstance().getCommonFactory(),
					symbolTreeController, selectedPinsHandler,
					getEscapeListener())
			{
				@NotNull @Override protected PinSelectionConfigurationParams.PinType getPinType()
				{
					return PinSelectionConfigurationParams.PinType.PORT;
				}
			};
		}
		return super.createPinSelectionPanel(symbolTreeController, selectedPinsHandler);
	}

	private boolean setupInitialStatusAfterSelection()
	{
		//must ensure it reflects the current status.
		//mask the enabled state from previous selection.
		boolean canAdd = canAddCurrentPinListToDesign();
		final JButton okButton = getOkButton();
		okButton.setEnabled(canAdd);
		return canAdd;
	}

	private void applyApplicabiltiy(boolean canAdd)
	{
		final JButton okButton = getOkButton();
		okButton.setEnabled(canAdd && okButton.isEnabled());

		if (canAdd || m_sharedPinList == null) {
			okButton.setToolTipText("");
		}
		else {
			okButton.setToolTipText(ResourceMgr.getString(AddSharedPinListDialog.class,
					"AddSharedPinListDialog.OKButton.Tooltip.text"));
		}

		if (m_pinSelectionPanel != null) {
			m_pinSelectionPanel.setEnabled(canAdd);
			if (m_autoGenerateOption != null) {
				m_autoGenerateOption.setEnabled(
						canAdd && canAutogeneratePins() && !m_pinSelectionPanel.isSymbolViewEnabled());
			}
		}

		if (m_loadSharedDetailsOption != null) {
			m_loadSharedDetailsOption.setEnabled(canAdd);
		}

		if (m_referenceOption != null) {
			m_referenceOption.setEnabled(canAdd && !getWithConductor());
		}

		if (m_withConductorOption != null) {
			m_withConductorOption.setEnabled(canAdd && !getReference());
		}

		if (canAdd) {
			if (SymbolUtils.compositeSymbolOutOfDate(m_sharedPinList)) {
				MessageHelper.showWarningMessage(m_listPanel,
						ResourceMgr.getString(AddSharedPinListDialog.class,
								"AddSharedPinListDialog.symbolOutOfDate.warning.heading"),
						ResourceMgr.getString(AddSharedPinListDialog.class,
								"AddSharedPinListDialog.symbolOutOfDate.warning.message"));
				if (m_pinSelectionPanel != null) {
					m_pinSelectionPanel.enableSymbolSelection(false, true);
				}
			}
		}
		resetAutoGenerateAndPlaceAsStack();
	}

	protected boolean lockSharedPinList(ISharedPinList newSPL)
	{
		return SharedPinListHelper.lockForExclusiveRead(newSPL);
	}

	@Override protected void createAsReferenceCheckBox()
	{
		m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
		m_referenceOption.setName("chkReference");
		m_referenceOption.setChoiceType(ChoiceTypeValue.CHECK_BOX);
		m_referenceOption.setMnemonic(ResourceMgr.getMnemonic(AddSharedPinListDialog.class,
				"AddSharedPinListDialog.referenceCheckBox.mnemonic"));
		m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
		m_referenceOption.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
//                setReferece(e.getStateChange());
				m_reference = (boolean) evt.getNewValue();
				if (m_sharedPinList == null) {
					return;
				}
				DefaultMutableTreeNode sel =
						(DefaultMutableTreeNode) m_pinListView.getSelectionPath().getLastPathComponent();
				if (sel != null) {
					m_sharedPinList = (ISharedPinList) sel.getUserObject();
				}
				else {
					m_sharedPinList = null;
				}
				if (m_pinSelectionPanel != null) {
					PinSelectionUserOptions userOptions = createUserSelectionOption();
					final boolean canAdd = canAddCurrentPinListToDesign();
					//ensure the pin/symbol content is cleared from panel if shared object is restricted.
					final IMultiSymbolledPinlist sharedPinlistForPSP = canAdd ? m_sharedPinList : null;
					if (!m_pinSelectionPanel.reset(sharedPinlistForPSP, m_curDesign,
							m_pinSelectionPanel.getSharedPinView(), userOptions)) {
						m_success = false;
						setVisible(false);
					}
				}
			}
		});
//		m_optionsPanel.add(m_referenceOption, BorderLayout.CENTER);
//		setReferenceCheckBoxResources(
//                ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.referenceCheckBox.title"),
//                "chkReference",
//                ResourceMgr.getMnemonic(AddSharedPinListDialog.class,
//                        "AddSharedPinListDialog.referenceCheckBox.mnemonic"));
	}

	@Override public void createLoadSharedPinConnectionInfoCheckBox()
	{
		addLoadSharedPinConnectionInfo(
				t -> m_pinSelectionPanel.loadSharedPinConnectionInformationFromOtherDesigns(t));
	}

	public void windowClosing(WindowEvent e)
	{
		super.windowClosing(e);
		getCancelButton().doClick();
	}

	/**
	 * Brings up this dialog in a modal fashion to figure out what shared pinlist and sharedpin are being added to the
	 * diagram.
	 *
	 * @param design Description of the Parameter
	 * @param params params to configure building of options panel in dialog
	 * @return Description of the Return Value
	 */

	public boolean selectPinList(ILogicDesign design, @NotNull IPlacementOptionParams params)
	{
		return selectPinList(design, null, params);
	}

	public boolean selectPinList(ILogicDesign design, @Nullable PinListTypeEnum pltype,
			@NotNull IPlacementOptionParams params)
	{
		PinListTypeEnum pinListTypeEnum = pltype;

		if ((pinListTypeEnum == null && m_pltype == null)) {
			throw new IllegalArgumentException(
					"Pin List Type argument must be non-null if dialog was not defined with a type");
		}
		if (pinListTypeEnum != null && m_pltype != null && !pinListTypeEnum.equals(m_pltype)) {
			throw new IllegalArgumentException("Pin List Type argument must be null or match dialog definition");
		}

		if (pinListTypeEnum == null) {
			pinListTypeEnum = m_pltype;
		}

		if (m_pltype == null) {
			m_pltype = pinListTypeEnum;
		}

		String pinListPanelTitle = getPanelTitle(pinListTypeEnum);
		if (pinListPanelTitle != null) {
			((TitledBorder) m_pinListPanel.getBorder()).setTitle(pinListPanelTitle);
		}

		m_pinListView.clear();
		m_curDesign = design;
		m_selectPinList = true;
		buildTypeSpecificOptions(pinListTypeEnum, params);
		IProject project = design.getProject();
		assert project != null;
		CAFSharedUpdater csu =
				new CAFSharedUpdater(project, design, CAFUtils.getInstance().getWindowMgr());
		RefreshStatusEnum rs = csu.updateSharedPinListMgr();
		if (rs == RefreshStatusEnum.eObjectDoesNotExist) {
			return false;
		}
		List<ISharedPinList> sharedPinLists =
				CollectionUtils.createList(((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getAccessibleSharedPinlists(pinListTypeEnum));
		List<ISharedPinList> sharedPinListsToIgnore = new ArrayList<>();
		for (ISharedPinList obj : sharedPinLists) {
			if (obj instanceof ISharedConnector && ((ISharedConnector) obj).getOccupiedPosition() != null) {
				sharedPinListsToIgnore.add(obj);
			}
		}
		sharedPinLists.removeAll(sharedPinListsToIgnore);
		Collections.sort(sharedPinLists, new NamedObjectComparator<ISharedPinList>(false)
		{
			protected String getString(ISharedPinList object)
			{
				String s = super.getString(object);
				if (object != null) {
					IDesignAbstraction dabs = object.getDesignAbstraction();
					if (dabs != null) {
						// Put Abstractions at the end... [I know, I know]
						s = "ZZZ/" + dabs.getName() + "/" + s;
					}
				}
				return s;
			}
		});
		m_pinListView.addElements(sharedPinLists);
		ToolTipManager.sharedInstance().registerComponent(m_pinListView);

		if (!sharedPinLists.isEmpty()) {
			if (!m_selectPinList) {
				m_listPanel.add(m_pinListPanel, BorderLayout.NORTH);
				m_selectPinList = true;
			}
			m_pinListPanel.setEnabled(true);
			pack();
			if (m_pinSelectionPanel != null && m_pinSelectionPanel.isSymbolViewEnabled()) {
				m_autoGenerateOption.setEnabled(false);
				m_autoGenerateOption.setValue(false);
				if (m_placeAsStackOption != null) {
					m_placeAsStackOption.setEnabled(false);
					m_placeAsStackOption.setValue(false);
				}
			}
			setVisible(true);

			if (!lockLogicObjects(m_curDesign, m_sharedPinList)) {
				m_success = false;
			}
		}
		else {
			JOptionPane.showMessageDialog(this,
					ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.OptionPane_1.text"),
					ResourceMgr.getString(AddSharedPinListDialog.class, "AddSharedPinListDialog.OptionPane_2.text"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		return m_success;
	}

	@Nullable private String getPanelTitle(PinListTypeEnum pinListTypeEnum)
	{
		String pinListPanelTitle;
		switch (pinListTypeEnum.value()) {
			case PinListTypeEnum._TypeDevice:
				pinListPanelTitle = DevicePanelTitle;
				break;
			case PinListTypeEnum._TypeFunction:
				pinListPanelTitle = FunctionPanelTitle;
				break;
			case PinListTypeEnum._TypePlug:
				pinListPanelTitle = PlugPanelTitle;
				break;
			case PinListTypeEnum._TypeJack:
				pinListPanelTitle = JackPanelTitle;
				break;
			case PinListTypeEnum._TypeInlineJack:
				pinListPanelTitle = InlinePanelTitle;
				break;
			case PinListTypeEnum._TypeInlinePlug:
				pinListPanelTitle = InlinePanelTitle;
				break;
			case PinListTypeEnum._TypeSplice:
				pinListPanelTitle = SplicePanelTitle;
				break;
			case PinListTypeEnum._TypeInterconnectConnector:
				pinListPanelTitle = InterconnectConnectorPanelTitle;
				break;
			case PinListTypeEnum._TypeRingTerminal:
				pinListPanelTitle = RingTerminalPanelTitle;
				break;
			default:
				pinListPanelTitle = null;
		}
		return pinListPanelTitle;
	}

	public boolean selectPinList(ILogicDesign design, @Nullable ISharedPinList spl,
			ISharedPinReservationView sharedpinview, @NotNull IPlacementOptionParams params)
	{
		if (spl == null) {
			assert false;
			return false;
		}
		if (m_pltype != null && !m_pltype.equals(spl.getType())) {
			throw new IllegalArgumentException("Share Pin List argument type must agree with dialog type");
		}
		m_curDesign = design;
		m_sharedPinList = spl;

		// the SPL and it's mate must be locked until the action completes in case someone changes it in another session
		if (!lockAndRefreshSharedPinList(spl)) {
			return false;
		}

		if (!lockLogicObjects(m_curDesign, m_sharedPinList)) {
			return false;
		}
		// load pins if not yet loaded, to avoid loading during construction of the dialog
		// if not loaded now, load my be triggered by AWT or JavaFX threads of dialog and may lead to any concurrency issues
		m_sharedPinList.loadChildren();
		return selectPinList(m_sharedPinList, sharedpinview, params);
	}

	private boolean lockAndRefreshSharedPinList(ISharedPinList spl)
	{
		if (!SharedPinListHelper.lockForExclusiveRead(m_sharedPinList)) {
			LogicActionMessageHelper.warnLocked(m_sharedPinList);
			return false;
		}
		RefreshStatusEnum rs = spl.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			LogicActionMessageHelper.warnDeleted(m_sharedPinList);
			return false;
		}
		if (spl.getType().getMateType() != null && spl instanceof ISharedConnector) {
			Set<ISharedConnector> mates = ((ISharedConnector) spl).getMates();
			for (ISharedConnector mate : mates) {
				mate.refresh();
			}
		}
		return true;
	}

	@Override
	public boolean selectPinList(IMultiSymbolledPinlist multiSymbolledPinlist, ISharedPinReservationView sharedPinView,
			@NotNull IPlacementOptionParams params)
	{
		m_pinListView.addElement(m_sharedPinList);
		m_pinListView.setSelectionPath();
		PinSelectionUserOptions userOptions = createUserSelectionOption();
		userOptions.setSharedPinListChanged(true);

		boolean canAdd = setupInitialStatusAfterSelection();
		//ensure the pin/symbol content is cleared from panel if shared object is restricted.
		final IMultiSymbolledPinlist sharedPinlistForPSP = canAdd ? m_sharedPinList : null;
		if (!m_pinSelectionPanel.reset(sharedPinlistForPSP, m_curDesign, sharedPinView, userOptions,
				Collections.emptyList())) {
			return false;
		}

		if (m_selectPinList) {
			m_listPanel.remove(m_pinListPanel);
			m_selectPinList = false;
		}
		PinListTypeEnum plType = m_sharedPinList.getType();
		buildTypeSpecificOptions(plType, params);
		pack();

		applyApplicabiltiy(canAdd);

		if (m_pinSelectionPanel.isSymbolViewEnabled()) {
			disableOption(m_autoGenerateOption);
			disableOption(m_placeAsGroupOption);
			disableOption(m_placeAsStackOption);
			disableOption(individualOption);
			if (m_loadSharedDetailsOption != null) {
				m_loadSharedDetailsOption.setEditable(false);
			}
		}
		setVisible(true);

		return m_success;
	}

	/**
	 * Returns the IPinList that is to be represented.
	 *
	 * @return The pinList value
	 */
	public ISharedPinList getSharedPinList()
	{
		return m_sharedPinList;
	}

	@Override public boolean getWithConductor()
	{
		return m_withConductorOption != null && m_withConductorOption.getValue();
	}

	protected boolean canAddCurrentPinListToDesign()
	{
		//check for frozen/domain settings is necessary. because after refresh the conditions may change.
		final ISharedObjectAvailabilityReporter nullReporter = ISharedObjectAvailabilityReporter.NULL_REPORTER;
		return m_sharedPinList != null &&
				m_lockFilter.isSharedObjectAvailable(m_sharedPinList, m_curDesign, nullReporter);
	}

	@Override protected void onCancel()
	{
		super.onCancel();
		if (m_sharedPinList != null) {
			SharedPinListHelper.unlock(m_sharedPinList);
		}
	}

	@Override protected boolean shouldCreateSelectionPanel()
	{
		return !PinListTypeEnum.TypeSplice.equals(m_pltype);
	}

	@Override protected ISymbolTreeController getSymbolTreeController()
	{
		if (m_pltype != null && m_pltype.isFunction()) {
			return new SharedFunctionSymbolTreeController();
		}
		return new SharedDeviceSymbolTreeController();
	}

	@Override protected boolean canAutogeneratePins()
	{
		// the AddSharedJackConnectorAction does not support autogenerate pins - so late in the release we are just
		// going to disable the checkbox - see dts0100758123
		//this functionality is implemented now. - see dts0100840305
		return super.canAutogeneratePins();
	}

	@NotNull @Override protected JRootPane createRootPane()
	{
		return new JRootPane();
	}
}