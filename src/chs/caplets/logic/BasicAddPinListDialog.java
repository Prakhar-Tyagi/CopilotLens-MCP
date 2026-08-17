package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.cofUtils.logical.concurrency.LogicConcurrentEditReporter;
import chs.common.IMultiSymbolledPinlist;
import chs.common.IUID;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.ctf.caf.utils.IPinProxy;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.ui.ISymbolTreeController;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.PinSelectionUserOptions;
import chs.utility.ui.SymbolProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

public abstract class BasicAddPinListDialog extends PinListPlaceOptionsDialog
{

	protected ILogicDesign m_curDesign;
	protected boolean m_success;
	protected JPanel m_listPanel;
	protected JPanel m_pinListPanel;
	protected JScrollPane m_pinListScrollPane;
	protected boolean m_selectPinList;
	protected boolean m_symbolSelection;
	protected Boolean enablePinView = null;

	private static final int MinimumPanelWidth = 550;
	private static final int MinimumPanelHeight = 300;
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);

	protected BasicAddPinListDialog(@Nullable Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		m_selectPinList = true;
		m_autoGenerateOption = null;
		m_success = false;
		m_autoGenerate = false;
		m_reference = false;
		m_placeAsStack = false;
		setMinimumSize(MinimumPanelSize);
	}

	public abstract Consumer<List<?>> getSelectedPinsHandler();

	/**
	 * Returns a collection of IAbstractPin objects that represent the ISharedPin's to be created.
	 *
	 * @return The usedPins value
	 */
	public Collection<IPinProxy> getUsedPins()
	{
		return m_pinSelectionPanel.getPins();
	}

	public int getNumUsedPins()
	{
		return m_pinSelectionPanel.getNumUsedPins();
	}

	public void cleanUp()
	{
		m_pinSelectionPanel.cleanUp();
	}

	/**
	 * Returns a collection of IBlock objects that represent the Blocks to be created.
	 *
	 * @return The blocks list
	 */
	public SymbolProxy[] getSelectedSymbolInstances()
	{
		return m_pinSelectionPanel.getSelectedSymbolInstances();
	}

	public boolean hasAutogenerateCheckBox()
	{
		return m_autoGenerateOption != null;
	}

	protected void addComponents()
	{
		m_listPanel = new JPanel();
		m_listPanel.setLayout(new BorderLayout());
		if (m_pinListPanel != null) {
			m_listPanel.add(m_pinListPanel, BorderLayout.NORTH);
		}
		if (shouldCreateSelectionPanel()) {
			//@Todo(Logic Pinselectionpanel cleanup) remove the factories as arguments
			m_pinSelectionPanel = createPinSelectionPanel(getSymbolTreeController(), getSelectedPinsHandler());
			m_listPanel.add(m_pinSelectionPanel, BorderLayout.CENTER);

			if (canAutogeneratePins()) {
				m_pinSelectionPanel.addSelectSymbolChangeListener(e -> symbolSelectionChanged(e));
				m_pinSelectionPanel.addPinSelectionChangeListener(e -> {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						loadPrefs();
					}
					else {
						savePrefs();
					}
				});
			}
		}

		// Add options
		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new BorderLayout());

		//buildOptionsPanel();
		initOptionsPropertyGroup();
		JPanel pan = new JPanel();
		pan.setLayout(new BorderLayout());
		pan.add(new JPanel(), BorderLayout.WEST);
		m_optionsPanel.add(new JPanel(), BorderLayout.WEST);
		pan.add(m_optionsPanel, BorderLayout.SOUTH);
		m_listPanel.add(pan, BorderLayout.SOUTH);

		getContentPane().add(m_listPanel, BorderLayout.CENTER);
	}

	protected PinSelectionCommonPanel createPinSelectionPanel(ISymbolTreeController symbolTreeController,
			Consumer<List<?>> selectedPinsHandler)
	{
		return new PinSelectionCommonPanel(this, FactoryMgr.getDrawFactory(),
				CAFUtils.getInstance().getCommonFactory(),
				symbolTreeController, selectedPinsHandler,
				getEscapeListener());
	}

	protected ISymbolTreeController getSymbolTreeController()
	{
		return new DeviceSymbolTreeController();
	}

	public void setEnablePinView(boolean enable)
	{
		enablePinView = enable;
	}

	protected void symbolSelectionChanged(ItemEvent e)
	{

		JRadioButton btn = (JRadioButton) e.getSource();
		m_symbolSelection = btn.isSelected();
//		if(btn.isSelected())
//		{
//			savePrefs();
//		}
		//loadPrefs();
		resetAutoGenerateAndPlaceAsStack();
	}

	protected void resetAutoGenerateAndPlaceAsStack()
	{
		if (hasAutogenerateCheckBox()) {
			if (m_symbolSelection) {
				assert m_autoGenerateOption != null;
				m_autoGenerateOption.setEnabled(false);
				m_autoGenerateOption.setValue(false);
//				rootGroup.getBooleanPropertyByName(AUTOGENERATE_OPTION).setEnabled(false);
			}
			else {
				resetOption(m_autoGenerateOption, m_autoGenerate);
//				rootGroup.getBooleanPropertyByName(AUTOGENERATE_OPTION).setEnabled(canAddCurrentPinListToDesign());
			}
		}
		resetOption(m_placeAsStackOption, m_placeAsStack);
		if (!m_symbolSelection) {
			saveOptionPrefIfPresent(m_referenceOption);
			saveOptionPrefIfPresent(m_withConductorOption);
		}
		resetOption(m_placeAsGroupOption, m_placeAsGroup);
		if (individualOption != null) {
			resetOption(individualOption, individualOption.getValue());
		}
		if (m_loadSharedDetailsOption != null) {
			m_loadSharedDetailsOption.setEditable(!m_symbolSelection
					&& shouldOptionBeEnabled(m_loadSharedDetailsOption));
		}
	}

	protected boolean shouldOptionBeEnabled(IBooleanProperty property)
	{
		if (!canAddCurrentPinListToDesign()) {
			return false;
		}
		if (property == m_placeAsStackOption) {
			return !(m_symbolSelection || m_reference || isWithConductorOn());
		}
		if (property == m_placeAsGroupOption || property == individualOption) {
			return !m_symbolSelection;
		}
		return true;
	}

	protected boolean shouldCreateSelectionPanel()
	{
		return true;
	}

//	public void setVisible(boolean enable)
//	{
//		if(enable==true)
//		{
//			if(m_pinSelectionPanel.isSymbolViewEnabled())
//			{
//				resetAutoGenerateAndPlaceAsStack();
//			}
//		}
//		super.setVisible(enable);
//	}

	@Override @NotNull protected String getAutoGenerationOptionComponentName()
	{
		return "SharedSymbolAutoGenerateOption";
	}

	protected void createAsStackOption()
	{
		m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
		m_placeAsStackOption.setName(AS_STACK_OPTION);
		m_placeAsStackOption.setMnemonic(
				ResourceMgr.getMnemonic(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsStack.mnemonic"));
		m_placeAsStackOption.addPropertyChangeListener(evt -> m_placeAsStack = (boolean) evt.getNewValue());
	}

	public boolean getAutoGenerate()
	{
		return canAutogeneratePins() && m_autoGenerate &&
				m_autoGenerateOption != null && m_autoGenerateOption.isEnabled();
	}

	public boolean getPlaceAsStack()
	{
		return m_placeAsStack;
	}

	public boolean getPlaceAsGroup()
	{
		return m_placeAsGroup;
	}

	@Override protected boolean canAutogeneratePins()
	{
		return true;
	}

	protected void hookupButtons()
	{
		getOkButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						onOk();
						m_success = true;
						setVisible(false);
						savePrefs();
						dispose();
					}
				}
		);

		getCancelButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						onCancel();
						m_success = false;
						setVisible(false);
						savePrefs();
						dispose();
					}
				}
		);
	}

	protected void onCancel()
	{
	}

	protected void onOk()
	{
	}

	protected boolean canAddCurrentPinListToDesign()
	{
		return true;
	}

	public boolean isSymbelSelectionSelected()
	{
		return m_symbolSelection;
	}

	public boolean selectPinList(IMultiSymbolledPinlist multiSymbolledPinlist, ISharedPinReservationView sharedPinView,
			@NotNull IPlacementOptionParams params)
	{
		PinSelectionUserOptions userOptions = createUserSelectionOption();
		userOptions.setSharedPinListChanged(true);

		m_pinSelectionPanel
				.reset(multiSymbolledPinlist, m_curDesign, sharedPinView, userOptions, Collections.emptyList());
		if (enablePinView != null && enablePinView) {
			m_pinSelectionPanel.enableSymbolSelection(true, false);
			m_pinSelectionPanel.doPinSelection();
		}

		buildTypeSpecificOptions(PinListTypeEnum.from_connectivity((IPinList) multiSymbolledPinlist), params);

		pack();
		if (m_symbolSelection) {
			disableOption(m_autoGenerateOption);
			disableOption(m_placeAsGroupOption);
			disableOption(m_placeAsStackOption);
			disableOption(individualOption);
			if (m_loadSharedDetailsOption != null) {
				m_loadSharedDetailsOption.setEditable(false);
			}
		}
		getOkButton()
				.setText(ResourceMgr.getString(BasicAddPinListDialog.class, "BasicAddPinListDialog.OKbutton.text"));
		getRootPane().setDefaultButton(getOkButton());
		setVisible(true);

		if (!lockLogicObjects(m_curDesign, multiSymbolledPinlist)) {
			m_success = false;
		}
		return m_success;
	}

	private boolean lockLogicObjects(ILogicDesign design, IMultiSymbolledPinlist multiSymbolledPinlist)
	{
		if (multiSymbolledPinlist instanceof IDevice) {
			return lockLogicObjects(design, Collections.singleton((IPinList) multiSymbolledPinlist));
		}
		if (multiSymbolledPinlist instanceof ISharedPinList) {
			return lockLogicObjects(design, (ISharedPinList) multiSymbolledPinlist);
		}
		return false;
	}

	protected boolean lockLogicObjects(ILogicDesign design, ISharedPinList sharedPinList)
	{
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		if (sharedPinList instanceof ISharedConnector) {
			Collection<ISharedPinList> sharedPinLists = new HashSet<>(1);
			ISharedConnector sharedToLock = (ISharedConnector) sharedPinList;
			while (sharedToLock != null) {
				sharedPinLists.add(sharedToLock);
				sharedToLock = sharedToLock.getParentConnector();
			}
			Map<ISharedPinList, IPinList> pinListMap = connectivity.findSharedPinList(sharedPinLists);
			return lockLogicObjects(design, pinListMap.values());
		}
		IPinList pinList = connectivity.findSharedPinList(sharedPinList);
		return pinList == null || lockLogicObjects(design, Collections.singleton(pinList));
	}

	protected boolean lockLogicObjects(ILogicDesign design, Collection<IPinList> pinLists)
	{
		Collection<IUID> lockFailedObjects = LogicObjectLockFinder.tryEdit(design, pinLists);
		if (!lockFailedObjects.isEmpty()) {
			String msg = ResourceMgr.getString(BasicAddPinListDialog.class, "BasicAddPinListDialog.lockFailure.text");
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(design, msg, lockFailedObjects, new LogicConcurrentEditReporter());
		}
		return LogicObjectLockFinder.areAllEditable(pinLists);
	}

	@Override protected ActionListener getEscapeListener()
	{
		return actionEvent -> getCancelButton().doClick();
	}

	public boolean getReference()
	{
		return m_reference;
	}

	@Override public boolean isSymbolSelected()
	{
		return m_symbolSelection;
	}
}
