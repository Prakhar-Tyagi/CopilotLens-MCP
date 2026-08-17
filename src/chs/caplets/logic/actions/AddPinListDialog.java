/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caplets.logic.BasicAddPinListDialog;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utility.ui.PinSelectionAbstractPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog to add a schematic instance of an existing connectivity pinlist to a Logic diagram.
 * <p>
 * The instance may be added either as a parameterized instance or a symbolled instance.
 */
public class AddPinListDialog extends PinListPlaceOptionsDialog implements IPinInfoProvider
{

	@Nullable private IPinList pinlist;
	@Nullable private ISymbolDef symDef;
	private boolean success = false;
	private ConnectivityCommonPinSelectionPanel psp;
	private static final int MinimumPanelWidth = 550;
	private static final int MinimumPanelHeight = 300;
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);
	private boolean displayAutoGenerateOption;

	/**
	 * Construct the Add ... dialog ready for display via a call to selectPinList.
	 *
	 * @param frame Parent frame for the dialog
	 * @param pinlist Connectivity pinlist for which an instance will be added.  Pass null to display only a symbol
	 * tree
	 * @param symDef Fully loaded symbol for the pinlist, if available.  Pass null if symbol not set or missing.
	 * @param params  Pin Placement options
	 */
	public AddPinListDialog(@Nullable Frame frame, @Nullable IPinList pinlist, @Nullable ISymbolDef symDef,
			@NotNull IPlacementOptionParams params)
	{
		this(frame, pinlist, symDef, false, params);
	}

	public AddPinListDialog(@Nullable Frame frame, @NotNull ISymbolDef symDef,
			@NotNull IPlacementOptionParams params)
	{
		super(frame, getTitle(null), true, true);
		pinlist = null;
		displayAutoGenerateOption = false;
		m_autoGenerate = false;
		this.symDef = symDef;
		initAddPinListDialog(params);
		if (params.isAsGroupOptionEnabled()){
			m_placeAsGroupOption.addPropertyChangeListener(evt -> psp.setAllowTreeMixedSelection(!(boolean) evt.getNewValue()));
		}
	}

	public AddPinListDialog(@Nullable Frame frame, @Nullable IPinList pinlist, @Nullable ISymbolDef symDef,
			boolean displayAutoGenerateOption, @NotNull IPlacementOptionParams params)
	{
		super(frame, getTitle(pinlist), true);
		assert pinlist != null || symDef != null; // not expecting both null - empty dialog!
		this.pinlist = pinlist;
		this.displayAutoGenerateOption = displayAutoGenerateOption;
		m_autoGenerate = !(pinlist instanceof IFunction || pinlist instanceof IGenericInlineConnector);
		this.symDef = symDef;
		initAddPinListDialog(params);
	}

	public void setAllowTreeMixedSelection(boolean allowMixedSelection)
	{
		psp.setAllowTreeMixedSelection(allowMixedSelection);
	}

	private void initAddPinListDialog(@NotNull IPlacementOptionParams params)
	{
		setName("AddPinListDialog");
		rememberSize(true);
		setMinimumSize(MinimumPanelSize);
		addComponents(params);
		hookupButtons();
	}

	public boolean selectPinList()
	{
		pack();
		getOkButton().setText(ResourceMgr.getString(AddPinListDialog.class, "AddPinListDialog.OKButton.text"));
		setVisible(true);
		return success;
	}

	/**
	 * Get the list of pins that were selected in the dialog
	 *
	 * @return A possibly empty list of pins, sorted as they appear selected in the dialog
	 */
	public List<IPinProxy> getPins()
	{
		return psp.getPins();
	}

	/**
	 * Was the autogenerate option checked in the dialog
	 *
	 * @return The setting of the autogenerate option from the dialog
	 */
	public boolean getAutogenerate()
	{
		// SNAG demo - decided to remove the autogenerate checkbox from this dialog
		// keep this method and the client code in case we decide to put it back
		return m_autoGenerate;
	}

	/**
	 * Get the symbol specified in the dialog, or null
	 *
	 * @return The specified symbol or null
	 */

	@Nullable public ISymbolDef getSymbol()
	{

		return psp.getSymbol();
	}

	/**
	 * Get the block (member of composite symbol) specified in the dialog, or null
	 *
	 * @return The specified block or null
	 */
	@Nullable public IBlock getBlock()
	{
		return psp.getBlock();
	}

	/**
	 * Get the blocks (members of composite symbol) selected in the dialog, or null
	 *
	 * @return The specified blocks or null
	 */
	@Nullable public IBlock[] getBlocks()
	{
		return psp.getBlocks();
	}

	/**
	 * Overridden here to set a name for automation
	 *
	 * @param name The name - ignored here
	 */
	@Override public void setName(String name)
	{
		super.setName("AddPinListDialog");
	}

	private void hookupButtons()
	{
		getOkButton().addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						success = true;
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
						success = false;
						setVisible(false);
						savePrefs();
						dispose();
					}
				}
		);
	}

	private void addComponents(@NotNull IPlacementOptionParams params)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JPanel(), BorderLayout.EAST);
		panel.add(new JPanel(), BorderLayout.WEST);
		panel.add(new JPanel(), BorderLayout.NORTH);
		psp = getConnectivityPinSelectionPanel();
		panel.add(psp, BorderLayout.CENTER);
		m_optionsPanel = new JPanel();
		m_optionsPanel.setLayout(new BorderLayout());
		m_optionsPanel.add(new JPanel(), BorderLayout.WEST);
		initOptionsPropertyGroup();

		buildTypeSpecificOptions(PinListTypeEnum.from_connectivity(pinlist), params);
		panel.add(m_optionsPanel, BorderLayout.SOUTH);
		getContentPane().add(panel, BorderLayout.CENTER);

		getRootPane().setDefaultButton(getOkButton());

		psp.addSelectSymbolChangeListener(createSymbolSelectionListener());
		resetOptionsIfPanelchanged(psp.isPinPanelSelected());
	}

	@NotNull private ItemListener createSymbolSelectionListener()
	{
		return new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				JRadioButton btn = (JRadioButton) e.getSource();
				boolean pinPanelSelected = !btn.isSelected();
				resetOptionsIfPanelchanged(pinPanelSelected);
			}
		};
	}

	private void resetOptionsIfPanelchanged(boolean pinPanelSelected)
	{
		if (canAutogeneratePins()) {
			resetOptionIfPanelChanged(m_autoGenerateOption, pinPanelSelected);
		}
		if (m_placeAsGroupOption != null) {
			resetOptionIfPanelChanged(m_placeAsGroupOption, pinPanelSelected);
		}
		if (individualOption != null) {
			resetOptionIfPanelChanged(individualOption, pinPanelSelected);
		}
	}

	private void resetOptionIfPanelChanged(IBooleanProperty option, boolean pinPanelSelected)
	{
		option.setEnabled(pinPanelSelected);
		if (pinPanelSelected) {
			loadOptionPref(option);
		}
		else {
			option.setValue(false);
		}
	}

	protected boolean canAutogeneratePins()
	{
		return displayAutoGenerateOption;
	}

	private ConnectivityCommonPinSelectionPanel getConnectivityPinSelectionPanel()
	{
		return new ConnectivityCommonPinSelectionPanel(pinlist, symDef, this, new Consumer<List<?>>()
		{
			@Override public void accept(List<?> objects)
			{
				placeAsStackButtonStatusUpdate(objects);
			}
		}, getEscapeListener());
//		if (pinlist instanceof IFunction) {
//
//			return new ConnectivityPortSelectionPanel(pinlist);
//		}
//		return new ConnectivityPinSelectionPanel(pinlist, symDef, this);
	}

	protected void createAsReferenceCheckBox()
	{
		m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
		m_referenceOption.setName("chkReference");
		m_referenceOption.setMnemonic(ResourceMgr.getMnemonic(AddMultiSymbolledPinListDialog.class,
				"AddMultiSymbolledPinListDialog.referenceCheckBox.mnemonic"));
		m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
		m_referenceOption.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				m_reference = (boolean) evt.getNewValue();
				psp.setReference(m_reference);
				psp.reset();
			}
		});
//			m_optionsPanel.add(m_referenceOption, BorderLayout.CENTER);
//			setReferenceCheckBoxResources(
//					ResourceMgr.getString(AddMultiSymbolledPinListDialog.class,
//							"AddMultiSymbolledPinListDialog.referenceCheckBox.title"),
//					"chkReference",
//					ResourceMgr.getMnemonic(AddMultiSymbolledPinListDialog.class,
//							"AddMultiSymbolledPinListDialog.referenceCheckBox.mnemonic"));
	}

	@Override protected void createAsStackOption()
	{
		m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
		m_placeAsStackOption.setName(AS_STACK_OPTION);
		m_placeAsStackOption.setMnemonic(ResourceMgr
				.getMnemonic(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsStack.mnemonic"));
		m_placeAsStackOption.addPropertyChangeListener(evt -> m_placeAsStack = (boolean) evt.getNewValue());
//			JPanel pan = new JPanel();
//			pan.setLayout(new BorderLayout());
//			pan.add(m_placeAsStackOption, BorderLayout.CENTER);
//			pan.add(new JPanel(), BorderLayout.EAST);
//			m_optionsPanel.add(pan, BorderLayout.EAST);
	}

//	protected void setReferenceCheckBoxResources(String text, String name, char mnemonic)
//	{
//		m_referenceOption.setText(text);
//		m_referenceOption.setName(name);
//		m_referenceOption.setMnemonic(mnemonic);
//	}

	public boolean getReference()
	{
		return m_reference;
	}

	public boolean getPlaceAsStack()
	{
		return m_placeAsStack;
	}

	public boolean getPlaceAsGroup()
	{
		return m_placeAsGroup;
	}

	public static String getTitle(@Nullable IPinList pl)
	{
		String type;
		if (pl == null) {
			// currently only happens for selection of composite symbol from symbol tab
			type = "";
		}
		else if (pl instanceof IGenericInlineConnector) {
			type = COFTypeEnum.Inline.toString(); // "Inline" instead of "Plug" or "Jack"
		}
		else {
			type = COFTypeEnum.from_object(pl).toString(); // this is i18n
		}
		return ResourceMgr.getString(AddPinListDialog.class, "AddPinListDialog.title", type);
	}

	@NotNull @Override protected JRootPane createRootPane()
	{
		return new JRootPane();
	}

	@NotNull @Override public String getHelpID()
	{
		return AddMultiSymbolledPinListDialog.class.getName();
	}

	@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return psp;
	}
}
