/*
 * Copyright 2005-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.helper.PinMappingHandler;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedDeviceConnectorPin;
import chs.cof.logical.shared.ISharedDevicePin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.project.IOptionExpression;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.utils.IGenericPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.BasicUIFactory;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.SortedListModel;
import chs.utility.logic.StudPinUtils;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 24, 2005 Time: 1:13:27 PM
 */
public class MapPanel extends JPanel
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull protected final PinMappingHandler mHandler;

	// UI stuff
	// List of connectivity pins on the instance
	protected JList<IAbstractPin> fromList;
	// List of shared pin proxies
	protected JList<IPinProxy> toList;
	private JButton assocAll_button;
	private JButton unassoc_button;
	private JButton assoc_button;
	private JCheckBox showAllCB;
	private JLabel fromLabel;
	private JLabel toLabel;
	private JButton unassocAll_button;
	private Box mapComponentsHolder;
	protected JTable m_mapTable;
	private SharedPinListAddRemoveButtons addRemoveButtons;
	private JLabel deviceConnectorMismatch;

	public static final int SPACE = 10;

	private class MapTableModel extends AbstractTableModel
	{

		public int getColumnCount()
		{
			return 2;
		}

		public int getRowCount()
		{
			return fromList.getModel().getSize();
		}

		@Nullable public Object getValueAt(int rowIndex, int columnIndex)
		{
			IAbstractPin pin = fromList.getModel().getElementAt(rowIndex);
			if (columnIndex == 0) {
				return pin.getName();
			}
			else {
				return mHandler.getSharePinProxyName(pin);
			}
		}
	}

	private class SharedPinlistChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSharedPinlistChange();
		}
	}

	private class NameChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onNameChange();
		}
	}

	private class SchemPinlistChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSchemPinlistChange();
		}
	}

	private class ReuseChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			determineMappability();
			determineButtonEnablement();
			determineDeviceConnectorMappingValidity();
		}
	}

	private class SymbolDeletionListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			determineAddButtonState();
		}
	}

	private class MapChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onMapChange();
		}
	}

	public MapPanel(EditSharedPinListModel emodel, ILogicDesign des)
	{
		mHandler = createHandler(emodel, des);
		mHandler.addSharedChangeListener(new SharedPinlistChangeListener());
		mHandler.addNameChangeListener(new NameChangeListener());
		mHandler.addSchemChangeListener(new SchemPinlistChangeListener());
		mHandler.addReuseChangeListener(new ReuseChangeListener());
		mHandler.addMapChangeListener(new MapChangeListener());
		mHandler.addRemovalListener(new SymbolDeletionListener());

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		JPanel fromPanel = createFromPanel();

		JPanel toPanel = createToPanel();

		mainPanel.add(Box.createHorizontalStrut(SPACE));

		mapComponentsHolder = createMapComponentsHolder(fromPanel);
		mainPanel.add(mapComponentsHolder);

		mainPanel.add(toPanel);
		mainPanel.add(Box.createHorizontalStrut(SPACE));
		addRemoveButtons = getAddRemoveButtons(emodel, des);
		//adding action listener doesn't work. because this action listener
		//will be added later than the ones added in constructor and hence
		//being called before them. The action listeners are called in LIFO.
		addRemoveButtons.setPostActionListener((b) -> {
			determineButtonEnablement();
			if (b.equals(SharedPinListAddRemoveButtons.BUTTON_TYPE.ADD)) {
				determineAddButtonState();
			}
			determineDeviceConnectorMappingValidity();
		});
		mainPanel.add(addRemoveButtons);
		mainPanel.add(Box.createHorizontalStrut(SPACE));

		showAllCB = new JCheckBox(ResourceMgr.getString(MapPanel.class, getShowUnavailableText()), true);
		showAllCB.setName("MapPanel.showAllCB");
		showAllCB.setToolTipText(ResourceMgr.getString(MapPanel.class, getShowUnavailableTooltip()));
		showAllCB.setMnemonic(ResourceMgr.getMnemonic(MapPanel.class, "MapPanel.showUnavailable.mnemonic"));
		showAllCB.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				toList.repaint();
				determineButtonEnablement();
				determineDeviceConnectorMappingValidity();
			}
		});
		TableModel tableModel = new MapTableModel();

		m_mapTable = new JTable(tableModel);
		m_mapTable.setTableHeader(null);
		m_mapTable.setName("MapPanel.mapTable");

		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(SPACE, 0, SPACE, 0);
		gbc.weightx = 1.0;

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 0;
		gbc.weighty = 1.0;
		add(mainPanel, gbc);

		deviceConnectorMismatch = new JLabel(ResourceMgr.getString(MapPanel.class,
				"MapPanel.deviceConnectorMismatch.warning"));
		deviceConnectorMismatch.setIcon(IconUtils.getWarningIcon());
		deviceConnectorMismatch.setName("MapPanel.deviceConnectorMismatch");
		deviceConnectorMismatch.setVisible(false);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy = 1;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, SPACE, SPACE, 0);
		add(deviceConnectorMismatch, gbc);
	}

	@NotNull protected PinMappingHandler createHandler(EditSharedPinListModel emodel, ILogicDesign des)
	{
		return new PinMappingHandler(emodel, des);
	}

	private JPanel createToPanel()
	{
		JPanel toPanel = new JPanel();
		toPanel.setLayout(new BorderLayout());
		toLabel = new JLabel();
		toPanel.add(toLabel, BorderLayout.NORTH);

		toList = createShareToListComponent();
		JScrollPane toScrollPane = new JScrollPane(toList);
		toPanel.add(toScrollPane, BorderLayout.CENTER);
		return toPanel;
	}

	private JPanel createFromPanel()
	{
		JPanel fromPanel = new JPanel();
		fromPanel.setLayout(new BorderLayout());
		fromLabel = new JLabel();
		fromPanel.add(fromLabel, BorderLayout.NORTH);

		fromList = createShareFromListComponent();
		JScrollPane fromScrollPane = new JScrollPane(fromList);
		fromPanel.add(fromScrollPane, BorderLayout.CENTER);
		return fromPanel;
	}

	private JPanel createInnerMapButtonPanel()
	{
		JPanel innerMapButtonPanel = new JPanel()
		{
			public Dimension getMaximumSize()
			{
				return super.getPreferredSize();
			}
		};

		innerMapButtonPanel.setLayout(new GridLayout(4, 1, 5, 5));

		assoc_button = createAssociateButton();
		innerMapButtonPanel.add(assoc_button);

		assocAll_button = createAssociateAllButton();
		innerMapButtonPanel.add(assocAll_button);

		unassoc_button = createUnassociateButton();
		innerMapButtonPanel.add(unassoc_button);

		unassocAll_button = createUnassociateAllButton();
		innerMapButtonPanel.add(unassocAll_button);
		return innerMapButtonPanel;
	}

	private Box createMapComponentsHolder(Component fromPanel)
	{
		JPanel innerMapButtonPanel = createInnerMapButtonPanel();

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(fromPanel);
		box.add(Box.createHorizontalStrut(SPACE));
		JPanel outerMapButtonPanel = new JPanel();
		outerMapButtonPanel.setLayout(new BoxLayout(outerMapButtonPanel, BoxLayout.Y_AXIS));
		outerMapButtonPanel.add(Box.createVerticalGlue());
		outerMapButtonPanel.add(innerMapButtonPanel);
		outerMapButtonPanel.add(Box.createVerticalGlue());
		box.add(outerMapButtonPanel);
		box.add(Box.createHorizontalStrut(SPACE));

		return box;
	}

	private JButton createUnassociateAllButton()
	{
		JButton button = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "MapPanel.unassociateAll.text"));
		button.setName("MapPanel.unassocAllButton");
		button.setToolTipText(ResourceMgr.getString(MapPanel.class, getUnassociateAllTooltip()));
		button.setMnemonic(ResourceMgr.getMnemonic(MapPanel.class, "MapPanel.unassociateAll.mnemonic"));
		button.setEnabled(false); // Initially disabled
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mHandler.unassociateAll();
				fromList.repaint();
				toList.repaint();
				determineButtonEnablement();
				determineDeviceConnectorMappingValidity();
			}
		});
		button.setAlignmentX(Component.CENTER_ALIGNMENT);

		return button;
	}

	private JButton createUnassociateButton()
	{
		JButton button = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "MapPanel.unassociate.text"));
		button.setName("MapPanel.unassocButton");
		button.setToolTipText(ResourceMgr.getString(MapPanel.class, getUnassociateTooltip()));
		button.setMnemonic(ResourceMgr.getMnemonic(MapPanel.class, "MapPanel.unassociate.mnemonic"));
		button.setEnabled(false); // Initially disabled
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IAbstractPin pin = fromList.getSelectedValue();
				final IPinProxy proxy = toList.getSelectedValue();
				mHandler.unassociate(pin, proxy);
				fromList.repaint();
				toList.repaint();
				determineButtonEnablement();
				determineDeviceConnectorMappingValidity();
			}
		});
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		return button;
	}

	private JButton createAssociateAllButton()
	{
		JButton button = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "MapPanel.autoassociate.text"));
		button.setName("MapPanel.assocAllButton");
		button.setToolTipText(ResourceMgr.getString(MapPanel.class, getAutoAssociateTooltip()));
		button.setMnemonic(ResourceMgr.getMnemonic(MapPanel.class, "MapPanel.autoassociate.mnemonic"));
		button.setEnabled(mHandler.allowGenerateMapping());
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				associateAll();
				fromList.repaint();
				toList.repaint();
				// Clear the selection to force the add/delete buttons to (dis)able
				toList.getSelectionModel().clearSelection();
				fromList.getSelectionModel().clearSelection();
				determineButtonEnablement();
				determineDeviceConnectorMappingValidity();
			}
		});
		button.setAlignmentX(Component.CENTER_ALIGNMENT);

		return button;
	}

	private void associateAll()
	{
		mHandler.associateAll(this::showPinCreationConfirmation);
	}

	protected boolean showPinCreationConfirmation()
	{
		return MessageHelper.showOkCancelDialog(this,
				ResourceMgr.getString(MapPanel.class, getAutoAssociateWarningTitle()),
				ResourceMgr.getString(MapPanel.class, getAutoAssociateWarningHeader()),
				ResourceMgr.getString(MapPanel.class, "MapPanel.autoassociate.warningmsg"),
				JOptionPane.WARNING_MESSAGE);
	}

	private JButton createAssociateButton()
	{
		JButton button = BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MapPanel.class, "MapPanel.associate.text"));
		button.setName("MapPanel.assocButton");
		button.setToolTipText(ResourceMgr.getString(MapPanel.class, getAssociateTooltip()));
		button.setMnemonic(ResourceMgr.getMnemonic(MapPanel.class, "MapPanel.associate.mnemonic"));
		button.setEnabled(false); // Initially disabled
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IAbstractPin pin = fromList.getSelectedValue();
				IPinProxy proxy = toList.getSelectedValue();
				mHandler.associate(pin, proxy);
				fromList.repaint();
				toList.repaint();
				// Clear the selection to force the add/delete buttons to (dis)able
				toList.getSelectionModel().clearSelection();
				fromList.getSelectionModel().clearSelection();
				determineButtonEnablement();
				determineDeviceConnectorMappingValidity();
			}
		});
		button.setAlignmentX(Component.CENTER_ALIGNMENT);

		return button;
	}

	private JList<IPinProxy> createShareToListComponent()
	{
		JList<IPinProxy> list = new JList<>(mHandler.getToListModel());
		list.setName("MapPanel.shareToList");
		list.setCellRenderer(new ToPinListCellRenderer());
		list.setSelectionModel(new ToListSelectionModel());
		list.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				determineButtonEnablement();
			}
		});
		list.getModel().addListDataListener(new ListDataListener()
		{
			public void intervalRemoved(ListDataEvent e)
			{
				mHandler.removeIntervalFromMapping();
			}

			public void contentsChanged(ListDataEvent e)
			{
			}

			public void intervalAdded(ListDataEvent e)
			{
			}
		});

		return list;
	}

	private JList<IAbstractPin> createShareFromListComponent()
	{
		JList<IAbstractPin> list = new JList<>(mHandler.getFromListModel());
		list.setName("MapPanel.shareFromList");
		list.setCellRenderer(new FromPinListCellRenderer());
		list.setSelectionModel(new FromListSelectionModel());
		list.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				determineButtonEnablement();
			}
		});

		return list;
	}

	public void init()
	{
		mHandler.init();
		onSchemPinlistChange();
		onSharedPinlistChange();
	}

	protected void onMapChange()
	{
		m_mapTable.repaint();
	}

	private void onSchemPinlistChange()
	{
		IPinList cablePL = mHandler.getCablePinlist();
		if (cablePL == null) {
			// Hide fromList - nothing to map
			fromLabel.setText(ResourceMgr.getString(MapPanel.class, "MapPanel.nosymbol.text"));
			mapComponentsHolder.setVisible(false);
		}
		else {
			// Show fromList - nothing to map
			mapComponentsHolder.setVisible(true);
			updateFromLabel();
		}
		determineButtonEnablement();
		determineAddButtonState();
		determineDeviceConnectorMappingValidity();
	}

	private void onSharedPinlistChange()
	{
		determineMappability();
		onNameChange();
		determineButtonEnablement();
		determineAddButtonState();
		determineDeviceConnectorMappingValidity();
	}

	protected void determineAddButtonState()
	{
		addRemoveButtons.getAddButton().setEnabled(mHandler.allowAddPins());
	}

	private void onNameChange()
	{
		toLabel.setText(ResourceMgr.getString(MapPanel.class, "MapPanel.plugmap.text"));
	}

	private void determineMappability()
	{
		showAllCB.setEnabled(mHandler.canShowUnavailablePins());
	}

	private boolean allowAssociate(@NotNull StringBuilder toolTip)
	{
		final List<IPinProxy> selected = toList.getSelectedValuesList();
		if (selected.size() != 1) {
			return false;
		}

		//
		// Only allow Association if they are the same species (interconnect/non-interconnect)
		//
		IAbstractPin from = fromList.getSelectedValue();
		IPinProxy to = toList.getSelectedValue();
		return mHandler.allowAssociate(from, to, toolTip);
	}

	private boolean allowUnassociate()
	{
		final List<IPinProxy> selected = toList.getSelectedValuesList();
		if (selected.size() != 1) {
			return false;
		}

		IAbstractPin from = fromList.getSelectedValue();
		IPinProxy to = toList.getSelectedValue();
		return mHandler.allowUnassociate(from, to);
	}

	private void determineButtonEnablement()
	{
		final StringBuilder toolTip = new StringBuilder();
		boolean associateButtonState = allowAssociate(toolTip);

		assoc_button.setEnabled(associateButtonState);
		assoc_button.setToolTipText(toolTip.toString());
		assoc_button.setEnabled(associateButtonState);
		unassoc_button.setEnabled(allowUnassociate());
		assocAll_button.setEnabled(mHandler.allowGenerateMapping());
		unassocAll_button.setEnabled(mHandler.allowUnassociateAll());
	}

	private void determineDeviceConnectorMappingValidity()
	{
		mHandler.regenerateDeviceConnMapping();
		deviceConnectorMismatch.setVisible(mHandler.hasDeviceConnectorMismatches());
	}

	private class FromListSelectionModel extends DefaultListSelectionModel
	{

		public int getSelectionMode()
		{
			return SINGLE_SELECTION;
		}

		public void setSelectionInterval(int index0, int index1)
		{
			super.setSelectionInterval(index0, index1);
			if (index0 == index1) {
				IAbstractPin from = fromList.getSelectedValue();
				IPinProxy to = mHandler.getAssociatedProxy(from);
				if (to != null) {
					toList.setSelectedValue(to, true);
				}
				determineButtonEnablement();
			}
		}
	}

	private class ToListSelectionModel extends DefaultListSelectionModel
	{

		public int getSelectionMode()
		{
			return MULTIPLE_INTERVAL_SELECTION;
		}

		public void setSelectionInterval(int index0, int index1)
		{
			super.setSelectionInterval(index0, index1);
			if (index0 == index1) {
				IPinProxy to = toList.getSelectedValue();
				IAbstractPin from = mHandler.getAssociatedPin(to);
				if (from != null) {
					fromList.setSelectedValue(from, true);
				}
				determineButtonEnablement();
			}
		}

		public void addSelectionInterval(int index0, int index1)
		{
			super.addSelectionInterval(index0, index1);
			determineButtonEnablement();
		}
	}

	private abstract class PinListCellRenderer extends DefaultListCellRenderer
	{

		protected PinListCellRenderer()
		{
		}

		private void processDeviceConnectorInfo(@NotNull IAbstractPin pin,
				@NotNull BiConsumer<IDeviceConnPin, IDeviceConnector> consumer)
		{
			IDevicePin devicePin = CommonUtils.cast(pin, IDevicePin.class);
			IDeviceConnPin devConnPin = devicePin != null ?
					CommonUtils.cast(devicePin.getDeviceConnectorPin(), IDeviceConnPin.class) : null;
			if (devConnPin != null) {
				IDeviceConnector deviceConn = CommonUtils.cast(devConnPin.getOwner(), IDeviceConnector.class);
				if (deviceConn != null) {
					consumer.accept(devConnPin, deviceConn);
				}
			}
		}

		private void processSharedDeviceConnectorInfo(@NotNull ISharedPin pin,
				@NotNull BiConsumer<ISharedDeviceConnectorPin, ISharedDeviceConnector> consumer)
		{
			ISharedDevicePin devicePin = CommonUtils.cast(pin, ISharedDevicePin.class);
			ISharedDeviceConnectorPin devConnPin = devicePin != null ?
					CommonUtils.cast(devicePin.getMatePin(), ISharedDeviceConnectorPin.class) : null;
			if (devConnPin != null) {
				ISharedDeviceConnector owner = CommonUtils.cast(devConnPin.getOwner(), ISharedDeviceConnector.class);
				if (owner != null) {
					consumer.accept(devConnPin, owner);
				}
			}
		}

		@NotNull private String generatePartInfo(@NotNull ILibrariedObject deviceConn)
		{
			StringBuilder builder = new StringBuilder();
			String partNumber = deviceConn.getPartNumber();
			if (!StringUtils.isBlank(partNumber)) {
				builder.append(partNumber);
				String partRevision = deviceConn.getPartRevision();
				if (!StringUtils.isBlank(partRevision)) {
					builder.append(":").append(partRevision);
				}
			}
			String result = builder.toString();
			return StringUtils.isBlank(result) ? StringUtils.EMPTY_STRING :
					ResourceMgr.getString(MapPanel.class, "MapPanel.device.connector.mismatch.tooltip", result);
		}

		@NotNull protected String generateDeviceConnectorTooltipInfo(@NotNull IAbstractPin pin)
		{
			StringBuilder builder = new StringBuilder();
			processDeviceConnectorInfo(pin, (devConnPin, deviceConn) -> {
				builder.append(generatePartInfo(deviceConn));
			});
			return builder.toString();
		}

		@NotNull protected String generateDeviceConnectorTooltipInfo(@NotNull ISharedPin pin)
		{
			StringBuilder builder = new StringBuilder();
			processSharedDeviceConnectorInfo(pin, (devConnPin, deviceConn) -> {
				builder.append(generatePartInfo(deviceConn));
			});
			return builder.toString();
		}

		@NotNull private Object appendDeviceConnectorInfo(@NotNull Object currVal, @NotNull IAbstractPin pin)
		{
			StringBuilder builder = new StringBuilder();
			processDeviceConnectorInfo(pin, (devConnPin, deviceConn) -> {
				builder.append(" (").append(deviceConn.getName()).append(":").append(devConnPin.getName()).append(")");
			});
			return currVal + builder.toString();
		}

		@NotNull private Object appendSharedDeviceConnectorInfo(@NotNull Object currVal, @NotNull ISharedPin pin)
		{
			StringBuilder builder = new StringBuilder();
			processSharedDeviceConnectorInfo(pin, (devConnPin, deviceConn) -> {
				builder.append(" (").append(deviceConn.getName()).append(":").append(devConnPin.getName()).append(")");
			});
			return currVal + builder.toString();
		}

		protected final void decorateForDeviceConnectorInfo(JLabel label, String tooltip, boolean lossOfPartInfo)
		{
			if (lossOfPartInfo) {
				label.setIcon(IconUtils.getIconDecoratedWithWarningStatus(label.getIcon()));
			}
			if (!StringUtils.isBlank(tooltip)) {
				label.setToolTipText(tooltip);
			}
		}

		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			Object valueSub = value;
			if (value instanceof IAbstractPin) {
				valueSub = mHandler.makeBlockPinName((IAbstractPin) value);
				valueSub = appendDeviceConnectorInfo(valueSub, (IAbstractPin) value);
			}
			else if (value instanceof IPinProxy) {
				IPinProxy poxyProxy = (IPinProxy) value;
				valueSub = poxyProxy.getName();
				ISharedPin spin = poxyProxy.getSharedPin();
				if (spin != null) {
					IOptionExpression optExp = spin.getOptionExpression();
					if (optExp != null && !optExp.getExpression().isEmpty()) {
						valueSub = valueSub + " [" + optExp.getExpression() + ']';
					}
					valueSub = appendSharedDeviceConnectorInfo(valueSub, spin);
				}
			}
			return super.getListCellRendererComponent(list, valueSub, index, isSelected, cellHasFocus);
		}
	}

	private class FromPinListCellRenderer extends PinListCellRenderer
	{

		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			c.setToolTipText(null);
			if (mHandler.hasMapping(value)) {
				c.setFont(c.getFont().deriveFont(Font.PLAIN));
			}
			else {
				c.setFont(c.getFont().deriveFont(Font.BOLD));
			}
			c.setIcon(StudPinUtils.getActivePinIcon(value));
			IAbstractPin pin = (IAbstractPin) value;
			decorateForDeviceConnectorInfo(c, generateDeviceConnectorTooltipInfo(pin),
					mHandler.isDevConnMappingInvalid(pin));
			return c;
		}
	}

	private class ToPinListCellRenderer extends PinListCellRenderer
	{

		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			IPinProxy ppp = (IPinProxy) value;
			JLabel c = (JLabel) super.getListCellRendererComponent(list, ppp, index, isSelected, cellHasFocus);
			c.setToolTipText(null);
			Icon icon = null;
			if (mHandler.isUnmappableProxy(ppp)) {
				c.setForeground(CHSColors.getWarningForegroundColor());
				c.setVisible(showAllCB.isSelected());
				c.setBackground(CHSColors.getReadOnlyBackgroundColor());
				c.setToolTipText(ResourceMgr.getString(MapPanel.class, getUnAvailableTooltip()));
			}
			else {
				final ReverseMap<IAbstractPin, IPinProxy> mapping = mHandler.getConnectivityToSharedMap();
				final SortedListModel<IPinProxy> reusableProxies = mHandler.getReusableProxies();
				if (mapping.containsValue(value)) {
					if (reusableProxies.contains(ppp)) {
						c.setFont(c.getFont().deriveFont(Font.ITALIC));
					}
					else {
						c.setFont(c.getFont().deriveFont(Font.PLAIN));
					}
					ISharedPin sharedPin = ((IGenericPinProxy) value).getSharedPin();
					if (sharedPin == null ||
							(sharedPin instanceof ISharedDevicePin && !((ISharedDevicePin) sharedPin).isStud())) {
						icon = StudPinUtils.getActivePinIcon(mapping.getKey(value));
					}
				}
				else {
					if (reusableProxies.contains(ppp)) {
						c.setFont(c.getFont().deriveFont(Font.ITALIC | Font.BOLD));
					}
					else {
						c.setFont(c.getFont().deriveFont(Font.BOLD));
					}
				}
			}
			c.setIcon(icon != null ? icon : StudPinUtils.getActivePinIcon(value));
			Integer pinCount = mHandler.getPinNameToCountMap().get(ppp.getName());
			if (pinCount != null && pinCount > 1) {
				c.setForeground(Color.RED);
			}
			else {
				c.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			}
			ISharedPin sharedPin = ppp.getSharedPin();
			String tooltip = sharedPin != null ? generateDeviceConnectorTooltipInfo(sharedPin) :
					StringUtils.EMPTY_STRING;
			decorateForDeviceConnectorInfo(c, tooltip, mHandler.isDevConnMappingInvalid(ppp));
			return c;
		}
	}

	public boolean isMapperEnabled()
	{
		return mHandler.isMapperValid();
	}

	private void updateFromLabel()
	{
		final ISymbolDef symbolDef = mHandler.getSymbolDef();
		if (symbolDef == null) {
			// This is a share of a parameterized object. Find the pins to map from in the diagram schem object.
			fromLabel.setText(ResourceMgr.getString(MapPanel.class, "MapPanel.instance.text"));
		}
		else {
			fromLabel.setText(ResourceMgr.getString(MapPanel.class, "MapPanel.symbol.text", symbolDef.getName()));
		}
	}

	@NotNull
	protected SharedPinListAddRemoveButtons getAddRemoveButtons(EditSharedPinListModel emodel, ILogicDesign des)
	{
		return new SharedPinListAddRemoveButtons(toList, des, SwingConstants.HORIZONTAL, emodel,
				mHandler.getPinNameToCountMap());
	}

	@NotNull protected String getAssociateTooltip()
	{
		return "MapPanel.associate.tooltip";
	}

	@NotNull protected String getUnAvailableTooltip()
	{
		return "MapPanel.unavailable.tooltip";
	}

	@NotNull protected String getAutoAssociateWarningHeader()
	{
		return "MapPanel.autoassociate.warningheader";
	}

	@NotNull protected String getAutoAssociateWarningTitle()
	{
		return "MapPanel.autoassociate.warningtitle";
	}

	@NotNull protected String getShowUnavailableTooltip()
	{
		return "MapPanel.showUnavailable.tooltip";
	}

	@NotNull protected String getShowUnavailableText()
	{
		return "MapPanel.showUnavailable.text";
	}

	@NotNull protected String getUnassociateAllTooltip()
	{
		return "MapPanel.unassociateAll.tooltip";
	}

	@NotNull protected String getUnassociateTooltip()
	{
		return "MapPanel.unassociate.tooltip";
	}

	@NotNull protected String getAutoAssociateTooltip()
	{
		return "MapPanel.autoassociate.tooltip";
	}

	public void showAsReadOnly()
	{
		toList.setEnabled(false);
		addRemoveButtons.getAddButton().setEnabled(false);
		addRemoveButtons.getRemoveButton().setEnabled(false);
		addRemoveButtons.getRenameButton().setEnabled(false);
	}
}
