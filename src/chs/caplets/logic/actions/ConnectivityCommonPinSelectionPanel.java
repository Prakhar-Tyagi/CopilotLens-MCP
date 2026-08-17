/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.project.IProject;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.system.FactoryMgr;
import chs.utility.ui.PlacePinLibrarySelection;
import chs.utility.ui.OkCancelDialog;
import chs.utility.ui.PinSelectionAbstractPanel;
import chs.utility.ui.PinSelectionCapabilities;
import chs.utility.ui.PinSelectionConfigurationParams;
import chs.utility.ui.PinSelectionParams;
import chs.utility.ui.PinSelectionResetParams;
import chs.utility.ui.PinSelectionSelectedSymbolTreeCapability;
import chs.utility.ui.PinSelectionSymbolTreeAndPinTableCardPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConnectivityCommonPinSelectionPanel extends PinSelectionAbstractPanel
{

	protected PinSelectionSelectedSymbolTreeCapability symbolTreeCapability = null;
	protected PinSelectionSymbolTreeAndPinTableCardPanel pinSelectionSymbolTreeAndPinTableCardPanel;

	/**
	 * Construct the panel in either pins or symbol mode.
	 * <p>
	 * If the symbol is not null then the panel shows the symbol hierarchy and a preview of the symbol graphic,
	 * otherwise it shows a table of pins available for placement on the diagram.
	 *
	 * @param pinlist The connectivity pinlist
	 * @param symDef The symbol, if required
	 * @param dialog - parent ok-cancel dialog.
	 */

	public ConnectivityCommonPinSelectionPanel(IPinList pinlist, @Nullable ISymbolDef symDef,
			@Nullable OkCancelDialog dialog, Consumer<List<?>> selectionHandler, @Nullable ActionListener escapeListner)
	{

		this(pinlist, symDef, dialog, selectionHandler, escapeListner, Collections.emptyList());
	}

	public ConnectivityCommonPinSelectionPanel(IPinList pinlist, @Nullable ISymbolDef symDef,
			@Nullable OkCancelDialog dialog, Consumer<List<?>> selectionHandler, @Nullable ActionListener escapeListner,
			Collection<PinSelectionCapabilities> ignoreCapabilities)
	{

		pinSelectionParams =
				new PinSelectionParams(pinlist, FactoryMgr.getDrawFactory(), FactoryMgr.getCommonFactory());

		initializeAllComponents(dialog, selectionHandler, () -> {

			if (symDef != null) {
				createCapabilitiesForSymbol(symDef);
			}
			if (pinlist != null) {
				createCabilitiesForPinlist(pinlist, ignoreCapabilities);
			}
		}, escapeListner);
	}

	/**
	 * Constructs the panel Un-stacking pins from stacked pin
	 *
	 * @param stackedPin Stacked pin from which pins to be un-stacked
	 */
	public ConnectivityCommonPinSelectionPanel(ISchemStackPin stackedPin, @Nullable OkCancelDialog dialog,
			Consumer<List<?>> selectionHandler, ActionListener escapeListner)
	{

		pinSelectionParams =
				new PinSelectionParams(stackedPin, FactoryMgr.getDrawFactory(),
						FactoryMgr.getCommonFactory());

		initializeAllComponents(dialog, selectionHandler, () -> {
			createCabilitiesForPinlist(pinSelectionParams.getPinList(),
					Collections.singleton(PinSelectionCapabilities.SymbolTreeCapability));
		}, escapeListner);
	}

	public ConnectivityCommonPinSelectionPanel(PlacePinLibrarySelection libararyCavityContainer,
			@Nullable OkCancelDialog dialog,
			Consumer<List<?>> selectionHandler, ActionListener escapeListner, IProject project, ILogicDesign design)
	{

		pinSelectionParams = new PinSelectionParams(FactoryMgr.getDrawFactory(), FactoryMgr.getCommonFactory(),
				libararyCavityContainer, design);

		initializeAllComponents(dialog, selectionHandler, () -> {
			createTableCapabilities(Arrays.asList(getTableTypeCapability()),
					PinSelectionConfigurationParams.PinType.PIN);
		}, escapeListner);
	}

	@NotNull protected PinSelectionCapabilities getTableTypeCapability()
	{
		return PinSelectionCapabilities.ConfigurableTableViewCapability;
	}

	private void initializeAllComponents(@Nullable OkCancelDialog dialog, Consumer<List<?>> givenSelectionHandler,
			Runnable createCapabilities, @Nullable ActionListener escapeListner)
	{
		selectionHandler = givenSelectionHandler;
		m_dialog = dialog;
		createCapabilities.run();
		addComponents(escapeListner, getRequiredPinType(pinSelectionParams.getPinList()));
		specificInitialize();
	}

	private void createCabilitiesForPinlist(@NotNull IPinList pinList,
			Collection<PinSelectionCapabilities> ignoreCapabilities)
	{
		PinSelectionConfigurationParams pinSelectionNameAndTitleParams = new PinSelectionConfigurationParams();
		pinSelectionNameAndTitleParams.setSymbolTreeName("symbolTree");

		Collection<PinSelectionCapabilities> requiredCapabilities =
				getRequiredCapabilities(pinList);
		requiredCapabilities.removeAll(ignoreCapabilities);
		createTableCapabilities(requiredCapabilities, getRequiredPinType(pinList));
		if (requiredCapabilities.contains(PinSelectionCapabilities.SymbolTreeCapability)) {
			symbolTreeCapability = new PinSelectionSelectedSymbolTreeCapability(pinSelectionParams, null,
					pinSelectionNameAndTitleParams);
		}
	}

	private void createCapabilitiesForSymbol(@NotNull ISymbolDef symbolDef)
	{
		PinSelectionConfigurationParams pinSelectionNameAndTitleParams = new PinSelectionConfigurationParams();
		pinSelectionNameAndTitleParams.setSymbolTreeName("symbolTree");
		symbolTreeCapability = new PinSelectionSelectedSymbolTreeCapability(pinSelectionParams, symbolDef,
				pinSelectionNameAndTitleParams);
	}

	public void setAllowTreeMixedSelection(boolean allowMixedSelection)
	{
		if (symbolTreeCapability != null) {
			symbolTreeCapability.setAllowTreeMixedSelection(allowMixedSelection);
		}
	}

	protected void addComponents(@Nullable ActionListener escapeListener,
			@NotNull PinSelectionConfigurationParams.PinType pinType)
	{

		IPinList pinlist = pinSelectionParams.getPinList();
		setLayout(new BorderLayout());

		JSplitPane pinTableAndSymbolView = createTablePinPanel(escapeListener, pinlist instanceof IConnector);
		JPanel symbolTreePane = createSymbolTreePane();

		if (pinTableAndSymbolView != null && symbolTreePane != null) {

			pinSelectionSymbolTreeAndPinTableCardPanel = new PinSelectionSymbolTreeAndPinTableCardPanel(pinType);
			PinSelectionSymbolTreeAndPinTableCardPanel.CardComponent pinTableComponent =
					new PinSelectionSymbolTreeAndPinTableCardPanel.CardComponent(pinTableAndSymbolView,
							getPinTableCapability(),
							new Supplier<Boolean>()
							{
								@Override public Boolean get()
								{
									return pinlist != null;
								}
							});
			PinSelectionSymbolTreeAndPinTableCardPanel.CardComponent symbolTreeComponent =
					new PinSelectionSymbolTreeAndPinTableCardPanel.CardComponent(
							symbolTreePane, symbolTreeCapability, new Supplier<Boolean>()
					{
						@Override public Boolean get()
						{
							return !symbolTreeCapability.getSelectedSymbolInstances().isEmpty();
						}
					});
			JPanel panel = pinSelectionSymbolTreeAndPinTableCardPanel
					.createSymbolTreeAndPinTableCapabilities(m_dialog, pinTableComponent, symbolTreeComponent);
			add(panel);
		}
		else if (pinTableAndSymbolView != null) {
			add(pinTableAndSymbolView);
		}
		else if (symbolTreePane != null) {
			add(symbolTreePane);
		}
	}

	private void specificInitialize()
	{
		super.init(symbolTreeCapability);
		if (symbolTreeCapability != null && pinSelectionSymbolTreeAndPinTableCardPanel != null) {
			pinSelectionSymbolTreeAndPinTableCardPanel.doSymbolSelection(true);
		}
	}

	@Nullable private JPanel createSymbolTreePane()
	{
		if (symbolTreeCapability != null) {
			return symbolTreeCapability.createSymbolTreeAndViewCapability(new Consumer<TreePath[]>()
			{
				@Override public void accept(TreePath[] treePaths)
				{
					if (m_dialog != null) {
						m_dialog.getOkButton().setEnabled(treePaths != null && treePaths.length > 0);
					}
				}
			});
		}
		return null;
	}

	public void reset()
	{

		PinSelectionResetParams pinSelectionResetParams = new PinSelectionResetParams(false, null);
		super.reset(pinSelectionResetParams, symbolTreeCapability);
	}

	public boolean isReference()
	{
		return pinSelectionParams.isReference();
	}

	public void setReference(boolean isReference)
	{
		pinSelectionParams.setReference(isReference);
	}

	@Nullable public IBlock[] getBlocks()
	{
		if (symbolTreeCapability != null) {
			return symbolTreeCapability.getBlocks();
		}
		return null;
	}

	@Nullable public IBlock getBlock()
	{
		if (symbolTreeCapability != null) {
			symbolTreeCapability.getBlock();
		}
		return null;
	}

	@Nullable ISymbolDef getSymbol()
	{
		if (symbolTreeCapability != null) {
			if (symbolTreeCapability.isEnabled()) {
				return symbolTreeCapability.getSymbol();
			}
		}
		return null;
	}

	public boolean isPinPanelSelected()
	{
		if (pinSelectionSymbolTreeAndPinTableCardPanel != null) {
			return pinSelectionSymbolTreeAndPinTableCardPanel.isPinTableSelected();
		}

		return getPinTableCapability() != null;
	}

	public void addSelectSymbolChangeListener(ItemListener itemListener)
	{
		if (pinSelectionSymbolTreeAndPinTableCardPanel != null) {
			pinSelectionSymbolTreeAndPinTableCardPanel.addSelectSymbolChangeListener(itemListener);
		}
	}

	private PinSelectionConfigurationParams.PinType getRequiredPinType(IPinList pinList)
	{
		if (pinList instanceof IFunction) {
			return PinSelectionConfigurationParams.PinType.PORT;
		}
		else {
			return PinSelectionConfigurationParams.PinType.PIN;
		}
	}
}
