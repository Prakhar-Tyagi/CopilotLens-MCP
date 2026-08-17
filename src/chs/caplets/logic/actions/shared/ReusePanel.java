/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.OutputWindowWrapper;
import chs.caplets.logic.actions.shared.helper.PinReuseHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPin;
import chs.common.IDesignDescriptor;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.images.CHSImageLoader;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.BasicUIFactory;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.ReusePanelRemoveAllButton;
import chs.utilities.ui.ReusePanelRemoveButton;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 24, 2005 Time: 1:12:11 PM
 */
public class ReusePanel extends JPanel
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull protected final PinReuseHandler mHandler;

	//UI Stuff
	private JList<IPinProxy> tiedListUI;
	private JList<IPinProxy> reusableListUI;
	private JButton addButton;
	private JButton removeButton;
	private JButton addAllButton;
	private JButton removeAllButton;
	public static final int SPACE = 10;

	private class SharedPinlistChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSharedPinlistChange();
		}
	}

	private class PinChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onPinChange();
		}

		private void onPinChange()
		{
			determineButtonEnablement();
		}
	}

	public ReusePanel(EditSharedPinListModel emodel, ILogicDesign theDesign)
	{
		mHandler = new PinReuseHandler(emodel, theDesign);
		// Change listeners
		mHandler.addPinChangeListener(new PinChangeListener());
		mHandler.addSharedChangeListener(new SharedPinlistChangeListener());
		tiedListUI = new JList<>(mHandler.getTiedList());
		initTiedListUI();
		reusableListUI = new JList<>(mHandler.getReusableList());
		initReusableListUI();

		addButton = BasicUIFactory.getInstance()
				.createSiemensCustomJButton(ResourceMgr.getString(ReusePanel.class, "ReusePanel.add.text"));
		addAllButton = BasicUIFactory.getInstance()
				.createSiemensCustomJButton(ResourceMgr.getString(ReusePanel.class, "ReusePanel.addall.text"));
		removeButton =
				new ReusePanelRemoveButton(ResourceMgr.getString(ReusePanel.class, "ReusePanel.remove.text"),
						reusableListUI,
						getRemoveTooltip(), getRemoveDisabledTooltip());
		removeAllButton =
				new ReusePanelRemoveAllButton(ResourceMgr.getString(ReusePanel.class, "ReusePanel.removeall.text"),
						reusableListUI, getRemoveAllTooltip(), getRemoveAllDisabledTooltip());
		initializeButtons();
		//
		// Put buttons in middle.
		//
		JPanel buttonPanel = createButtonPanel();

		buttonPanel.setLayout(new GridLayout(4, 1, 5, 5));

		addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(addButton);
		addAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(addAllButton);
		removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(removeButton);
		removeAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(removeAllButton);

		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		buttonBox.add(Box.createVerticalGlue());
		buttonBox.add(buttonPanel);
		buttonBox.add(Box.createVerticalGlue());

		final JPanel nonReusablePanel = new JPanel(new BorderLayout());
		nonReusablePanel.add(
				new JLabel(ResourceMgr.getStringForLabel(ReusePanel.class, "ReusePanel.nonreusablepins.text")),
				BorderLayout.NORTH);
		nonReusablePanel.add(new JScrollPane(tiedListUI), BorderLayout.CENTER);
		nonReusablePanel.add(Box.createHorizontalStrut(0), BorderLayout.WEST);
		JPanel reusablePanel = new JPanel(new BorderLayout());
		reusablePanel.add(new JLabel(ResourceMgr.getStringForLabel(ReusePanel.class, getReusablePinsText())),
				BorderLayout.NORTH);
		reusablePanel.add(new JScrollPane(reusableListUI), BorderLayout.CENTER);
		reusablePanel.add(Box.createHorizontalStrut(0), BorderLayout.EAST);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel mainGrid = new JPanel(new GridLayout(1, 3, 0, 0));
		mainGrid.add(nonReusablePanel);
		mainGrid.add(buttonBox);
		mainGrid.add(reusablePanel);

		Box mainBox = new Box(BoxLayout.X_AXIS);
		mainBox.add(Box.createHorizontalStrut(SPACE));
		mainBox.add(mainGrid);
		mainBox.add(Box.createHorizontalStrut(SPACE));

		add(Box.createVerticalStrut(SPACE));
		add(mainBox);
		add(Box.createVerticalStrut(SPACE));
	}

	private void initializeButtons()
	{
		initAddButton();
		initAddAllButton();
		initRemoveButton();
		initRemoveAllButton();
	}

	@NotNull private JPanel createButtonPanel()
	{
		return new JPanel()
		{
			public Dimension getMaximumSize()
			{
				return super.getPreferredSize();
			}
		};
	}

	private void initRemoveAllButton()
	{
		removeAllButton.setName("ReusePanel.removeAllButton");
		removeAllButton.setHorizontalTextPosition(SwingConstants.TRAILING);
		removeAllButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_removeall.gif"));
		removeAllButton.setDisabledIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_removeall_inactive.gif"));
		removeAllButton.setToolTipText(ResourceMgr.getString(ReusePanel.class, getRemoveAllTooltip()));
		removeAllButton.setMnemonic(ResourceMgr.getMnemonic(ReusePanel.class, "ReusePanel.removeall.mnemonic"));
		removeAllButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mHandler.removeAllFromReusable();
				determineButtonEnablement();
			}
		});
	}

	private void initRemoveButton()
	{
		removeButton.setName("ReusePanel.removeButton");
		removeButton.setHorizontalTextPosition(SwingConstants.TRAILING);
		removeButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_remove.gif"));
		removeButton.setDisabledIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_remove_inactive.gif"));
		removeButton.setToolTipText(ResourceMgr.getString(ReusePanel.class, getRemoveTooltip()));
		removeButton.setMnemonic(ResourceMgr.getMnemonic(ReusePanel.class, "ReusePanel.remove.mnemonic"));
		removeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				List<IPinProxy> selectedProxies = new ArrayList<IPinProxy>(reusableListUI.getSelectedValuesList());
				mHandler.removeFromReusable(selectedProxies);
				determineButtonEnablement();
			}
		});
	}

	private void initAddAllButton()
	{
		addAllButton.setName("ReusePanel.addAllButton");
		addAllButton.setHorizontalTextPosition(SwingConstants.LEADING);
		addAllButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_addall.gif"));
		addAllButton.setDisabledIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_addall_inactive.gif"));
		addAllButton.setToolTipText(ResourceMgr.getString(ReusePanel.class, getAddAllTooltip()));
		addAllButton.setMnemonic(ResourceMgr.getMnemonic(ReusePanel.class, "ReusePanel.addall.mnemonic"));
		addAllButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mHandler.makeAllPinsReusable();
				determineButtonEnablement();
			}
		});
	}

	private void initAddButton()
	{
		addButton.setName("ReusePanel.addButton");
		addButton.setHorizontalTextPosition(SwingConstants.LEADING);
		addButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_add.gif"));
		addButton.setDisabledIcon(CHSImageLoader.loadImageIcon("chs/images/general/ico_add_inactive.gif"));
		addButton.setToolTipText(ResourceMgr.getString(ReusePanel.class, getAddTooltip()));
		addButton.setMnemonic(ResourceMgr.getMnemonic(ReusePanel.class, "ReusePanel.add.mnemonic"));
		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final List<IPinProxy> s1 = tiedListUI.getSelectedValuesList();
				mHandler.makePinsReusable(s1);
				determineButtonEnablement();
			}
		});
	}

	private void initReusableListUI()
	{
		DefaultListSelectionModel reusableSelectionModel = new DefaultListSelectionModel();
		reusableSelectionModel.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				determineButtonEnablement();
			}
		});
		reusableListUI.setSelectionModel(reusableSelectionModel);
		reusableListUI.setName("ReusePanel.reusableList");
		reusableListUI.setCellRenderer(new PinProxyListCellRenderer());
	}

	private void initTiedListUI()
	{
		DefaultListSelectionModel tiedSelectionModel = new DefaultListSelectionModel();
		tiedSelectionModel.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				determineButtonEnablement();
			}
		});
		tiedListUI.setSelectionModel(tiedSelectionModel);
		tiedListUI.setName("ReusePanel.tiedList");
		tiedListUI.setCellRenderer(new PinProxyListCellRenderer());
	}

	private OutputWindowWrapper getOutputWindow()
	{
		return new OutputWindowWrapper(CAFUtils.getInstance().getOutputWindow());
	}

	private void displayNonReusablePinsErrors(int countNonReusablePin, @Nullable ISharedPin spin)
	{
		if (countNonReusablePin == 1) {
			assert spin != null;
			Set<IUID> designUIDs = spin.getUsedDesigns();
			IDesignDescriptor usedDesign =
					UIDMgr.getDesignDescriptor(designUIDs.iterator().next());
			assert usedDesign != null;
			String usedDesignName = usedDesign.getFullName();
			ResourceBasedMessageContent content =
					new ResourceBasedMessageContent(ReusePanel.class, getAddPinReservationError());
			content.setContext(ReusePanel.class, getAddPinReservationErrorContext());
			content.setMessage(ReusePanel.class, getAddPinReservationErrorContextMessage());
			content.setImplications(ReusePanel.class, getAddPinReservationErrorImplication(),
					usedDesignName);
			content.setGuidance(ReusePanel.class, getAddPinReservationErrorGuaidance());
			Message.show(PromptSeverity.ERROR, content);
		}
		else {
			ResourceBasedMessageContent content =
					new ResourceBasedMessageContent(ReusePanel.class, getAddAllPinReservationError());
			content.setContext(ReusePanel.class, getAddAllPinReservationErrorContext());
			content.setMessage(ReusePanel.class, getAddAllPinReservationErrorMessage());
			content.setImplications(ReusePanel.class, getAddAllPinReservationErrorImplication());
			content.setGuidance(ReusePanel.class, getAddAllPinReservationErrorGiaidance());
			Message.show(PromptSeverity.ERROR, content);
		}
	}

	private void determineButtonEnablement()
	{
		addButton.setEnabled(allowAdd());
		removeButton.setEnabled(allowRemove());
		addAllButton.setEnabled(allowAddAll());
		removeAllButton.setEnabled(allowRemoveAll());
	}

	private boolean allowAdd()
	{
		return mHandler.allowAdd(tiedListUI.getSelectedValuesList());
	}

	private boolean allowRemove()
	{
		//preventive fix for dts0100668713 - [CH] java.lang.IndexOutOfBoundsException: Index: 0, Size: 0 at
		if (reusableListUI.getModel().getSize() == 0) {
			return false;
		}
		return mHandler.allowRemove(reusableListUI.getSelectedValuesList());
	}

	public boolean allowAdd(@NotNull ISharedPin spin)
	{
		return mHandler.allowAdd(spin);
	}

	private boolean allowAddAll()
	{
		return mHandler.allowAddAll();
	}

	private boolean allowRemoveAll()
	{
		return mHandler.allowRemoveAll();
	}

	private void onSharedPinlistChange()
	{
		determineButtonEnablement();
	}

	public void init()
	{
		mHandler.init();
		onSharedPinlistChange();
	}

	private class PinProxyListCellRenderer extends DefaultListCellRenderer
	{

		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			Object valueSub = value;
			IPinProxy pp = null;
			if (value instanceof IPinProxy) {
				valueSub = ((IReadOnlyNamedObject) value).getName();
				pp = (IPinProxy) value;
			}
			Component c = super.getListCellRendererComponent(list, valueSub, index, isSelected, cellHasFocus);
			if (pp != null) {
				final boolean alreadyReusable = mHandler.isAlreadyReusable(pp);
				if (alreadyReusable && !mHandler.allowRemove(pp.getSharedPin())) {
					c.setForeground(CHSColors.getWarningForegroundColor());
				}
				else if (!alreadyReusable && pp.getSharedPin() != null &&
						!allowAdd(pp.getSharedPin())) {
					c.setForeground(CHSColors.getWarningForegroundColor());
				}
			}
			return c;
		}
	}

	@NotNull protected String getAddAllPinReservationErrorGiaidance()
	{
		return "ReusePanel.addAll.pinReservation.error.guidance";
	}

	@NotNull protected String getAddAllPinReservationErrorImplication()
	{
		return "ReusePanel.addAll.pinReservation.error.implication";
	}

	@NotNull protected String getAddAllPinReservationErrorMessage()
	{
		return "ReusePanel.addAll.pinReservation.error.message";
	}

	@NotNull protected String getAddAllPinReservationErrorContext()
	{
		return "ReusePanel.addAll.pinReservation.error.context";
	}

	@NotNull protected String getAddAllPinReservationError()
	{
		return "ReusePanel.addAll.pinReservation.error";
	}

	@NotNull protected String getAddPinReservationErrorGuaidance()
	{
		return "ReusePanel.add.pinReservation.error.guidance";
	}

	@NotNull protected String getAddPinReservationErrorImplication()
	{
		return "ReusePanel.add.pinReservation.error.implications";
	}

	@NotNull protected String getAddPinReservationErrorContextMessage()
	{
		return "ReusePanel.add.pinReservation.error.message";
	}

	@NotNull protected String getAddPinReservationErrorContext()
	{
		return "ReusePanel.add.pinReservation.error.context";
	}

	@NotNull protected String getAddPinReservationError()
	{
		return "ReusePanel.add.pinReservation.error";
	}

	@NotNull protected String getAddTooltip()
	{
		return "ReusePanel.add.tooltip";
	}

	@NotNull protected String getAddAllTooltip()
	{
		return "ReusePanel.addall.tooltip";
	}

	@NotNull protected String getRemoveTooltip()
	{
		return "ReusePanel.remove.tooltip";
	}

	@NotNull protected String getRemoveDisabledTooltip()
	{
		return "ReusePanel.remove.disabled.tooltip";
	}

	@NotNull protected String getRemoveAllTooltip()
	{
		return "ReusePanel.removeall.tooltip";
	}

	@NotNull protected String getRemoveAllDisabledTooltip()
	{
		return "ReusePanel.removeall.disabled.tooltip";
	}

	@NotNull protected String getReusablePinsText()
	{
		return "ReusePanel.reusablepins.text";
	}

	public void showAsReadOnly()
	{
		tiedListUI.setEnabled(false);
		reusableListUI.setEnabled(false);
		addAllButton.setEnabled(false);
		removeAllButton.setEnabled(false);
	}
}
