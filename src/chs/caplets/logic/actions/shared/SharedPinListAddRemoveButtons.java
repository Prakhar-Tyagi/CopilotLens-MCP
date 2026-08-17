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

import chs.caplets.logic.actions.shared.helper.AddRemovePinHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.naming.INameMgr;
import chs.common.IRootComponentService;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.ui.form.RenameDialog;
import chs.utilities.SwingServices;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.SharedPinListAddButton;
import chs.utilities.ui.SharedPinListRemoveButton;
import chs.utilities.ui.SharedPinListRenameButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 26, 2005 Time: 12:28:07 PM
 */
public class SharedPinListAddRemoveButtons extends JPanel
{

	public enum BUTTON_TYPE
	{
		ADD, RENAME, DELETE
	}

	// Delegate to perform model changes and other business logic.
	@NotNull protected AddRemovePinHandler mHandler;

	//UI Stuff
	private JButton m_addButton;
	private JButton m_removeButton;
	private JList<IPinProxy> m_proxyList;
	private JLabel m_prefixLabel;
	private JButton renameButton;
	private Consumer<BUTTON_TYPE> m_postActionListener = (b) -> {
	};

	public SharedPinListAddRemoveButtons(JList<IPinProxy> proxyList, ILogicDesign design, int orientation,
			EditSharedPinListModel esplModel, @NotNull Map<String, Integer> pinNameToCountMap)
	{
		mHandler = getAddRemovePinHandler(proxyList, design, esplModel, pinNameToCountMap);
		m_proxyList = proxyList;
		final JPanel buttonHolder = new JPanel()
		{
			public Dimension getMaximumSize()
			{
				return super.getPreferredSize();
			}
		};
		buttonHolder.setLayout(new GridLayout(3, 1, 5, 5));

		// Build the buttons
		buildAddButton();
		buildRemoveButton();
		buildRenameButton();

		// Add and format the buttons
		buttonHolder.add(m_addButton);
		buttonHolder.add(m_removeButton);
		buttonHolder.add(renameButton);
		if (orientation == SwingConstants.HORIZONTAL) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(Box.createHorizontalGlue());
			add(buttonHolder);
			add(Box.createHorizontalGlue());
		}
		else {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createVerticalGlue());
			add(buttonHolder);
			add(Box.createVerticalGlue());
		}
		m_proxyList.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				final List<IPinProxy> selected = m_proxyList.getSelectedValuesList();
				final StringBuilder toolTip = new StringBuilder();
				boolean removeOK = !selected.isEmpty() && mHandler.allowRemovePin(selected, toolTip);
				if (!removeOK) {
					m_removeButton.setToolTipText(toolTip.toString());
				}
				m_removeButton.setEnabled(removeOK);
				boolean allowRename = selected.size() == 1 && !mHandler.isFrozenSharedPinList();
				renameButton.setEnabled(allowRename);
			}
		});
	}

	public void setPostActionListener(@NotNull Consumer<BUTTON_TYPE> postActionListener)
	{
		m_postActionListener = postActionListener;
	}

	@NotNull protected AddRemovePinHandler getAddRemovePinHandler(JList<IPinProxy> proxyList, ILogicDesign design,
			EditSharedPinListModel esplModel, @NotNull Map<String, Integer> pinNameToCountMap)
	{
		return new AddRemovePinHandler(esplModel, design, pinNameToCountMap, proxyList.getModel());
	}

	private void buildRemoveButton()
	{
		m_removeButton = new SharedPinListRemoveButton(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.removeButton.text"), m_proxyList, mHandler, getRemoveButtonTooltip());

		m_removeButton.setEnabled(false);
		m_removeButton.setName("SPLAddRemove.removeButton");
		m_removeButton.setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				getRemoveButtonTooltip()));
		m_removeButton.setMnemonic(ResourceMgr.getMnemonic(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.removeButton.mnemonic"));
		m_removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_removeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final List<IPinProxy> selected = m_proxyList.getSelectedValuesList();
				for (IPinProxy ppp : selected) {
					mHandler.removeSharedPin(ppp);
				}
				m_postActionListener.accept(BUTTON_TYPE.DELETE);
				m_proxyList.repaint();
			}
		});
	}

	private void buildAddButton()
	{
		m_addButton = new SharedPinListAddButton(ResourceMgr.getStringForMenu(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.addButton.text"), mHandler);
		m_addButton.setName("SPLAddRemove.addButton");
		m_addButton.setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				getAddPinsButtonTooltip()));
		m_addButton.setMnemonic(ResourceMgr.getMnemonic(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.addButton.mnemonic"));
		m_addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JPanel holder = new JPanel();
				holder.setLayout(new GridLayout(3, 2));
				String prefix = getPrefixString();
				final JTextField prefixTF = new JTextField();
				prefixTF.setName("SPLAddRemove.prefix");
				prefixTF.setText(prefix);
				m_prefixLabel = new JLabel(ResourceMgr.getStringForLabel(SharedPinListAddRemoveButtons.class,
						getPinListPrefixText()));
				holder.add(m_prefixLabel);
				holder.add(prefixTF);

				int minsp = 0;
				final Set<String> proxyNames = mHandler.getProxyNames();
				for (String name : proxyNames) {
					if (name.startsWith(prefix)) {
						int n = CommonUtils.parseIndex(name.substring(prefix.length()));
						minsp = Math.max(minsp, n);
					}
				}
				minsp++;

				final JTextField startnum = new JTextField();
				startnum.setName("SPLAddRemove.startnum");
				holder.add(new JLabel(ResourceMgr.getStringForLabel(SharedPinListAddRemoveButtons.class,
						"SharedPinListAddRemoveButtons.startindex.text")));
				holder.add(startnum);
				startnum.setText(Integer.toString(minsp));

				final JTextField pincount = new JTextField();
				pincount.setName("SPLAddRemove.pincount");
				holder.add(new JLabel(ResourceMgr.getStringForLabel(SharedPinListAddRemoveButtons.class,
						getPinCountText())));
				holder.add(pincount);
				pincount.setText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
						"SharedPinListAddRemoveButtons.pincount.text"));

				final SimpleOkCancelDialog dialog =
						new SimpleOkCancelDialog(holder,
								(Dialog) SwingServices.getRoot(SharedPinListAddRemoveButtons.this),
								ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
										getDialogTitle()));

				dialog.setHelpID(getHelpIDName());

				KeyListener keyListener =
						new KeyAdapter()
						{
							public void keyReleased(KeyEvent e)
							{
								boolean valid = true;
								// First, see if the individual fields are valid.
								int pinCt = CommonUtils.parseCount(pincount.getText());
								if (pinCt < 1) {
									valid = false;
									if (!pincount.getText().isEmpty()) {
										pincount.setForeground(CHSColors.getErrorForegroundColor());
									}
								}

								int startIdx = CommonUtils.parseIndex(startnum.getText());
								if (startIdx < 0) {
									valid = false;
									if (!startnum.getText().trim().isEmpty()) {
										startnum.setForeground(CHSColors.getErrorForegroundColor());
									}
								}

								if (startnum.getText().trim().isEmpty() &&
										(pincount.getText().trim().isEmpty() || pinCt == 1)) {
									valid = true;
								}
								if (startnum.getText().trim().isEmpty() &&
										(pincount.getText().trim().isEmpty()) && (startIdx < 0)) {
									valid = false;
								}

								// If all fields are valid in themselves, make sure the combination of values won't cause
								// duplicate names
								if (valid) {
									final String prefixTxt = prefixTF.getText().trim();
									int i = 0;
									if (startIdx < 0 &&
											(proxyNames.contains(prefixTxt) || StringUtils.isEmpty(prefixTxt))) {
										valid = false;
									}
									else {
										while (valid && i < pinCt) {
											String pname =
													new StringBuilder().append(prefixTxt).append(startIdx + i)
															.toString();
											if (proxyNames.contains(pname)) {
												valid = false;
											}
											else {
												i++;
											}
										}
									}
									if (valid) {
										prefixTF.setForeground(CHSColors.getNormalForegroundColor());
										startnum.setForeground(CHSColors.getNormalForegroundColor());
										pincount.setForeground(CHSColors.getNormalForegroundColor());
									}
									else {
										// If something's not right, prefix and startnum definitely share some of the blame
										prefixTF.setForeground(CHSColors.getErrorForegroundColor());
										startnum.setForeground(CHSColors.getErrorForegroundColor());
										if (i > 0) {
											// If lowering the number of pins to create would eliminate the problem, blame
											// the pincount too.
											pincount.setForeground(CHSColors.getErrorForegroundColor());
										}
									}
								}
								else if (pincount.getText().trim().isEmpty()
										&& startnum.getText().trim().isEmpty()
										&& !prefixTF.getText().trim().isEmpty()
										&& !proxyNames.contains(prefixTF.getText().trim())) {
									prefixTF.setForeground(CHSColors.getNormalForegroundColor());
									pincount.setForeground(CHSColors.getNormalForegroundColor());
									valid = true;
								}
								dialog.getOkButton().setEnabled(valid);
								if ((pincount.getText().trim().isEmpty() || pinCt == 1)
										&& startnum.getText().trim().isEmpty()) {
									m_prefixLabel.setText(ResourceMgr.getStringForLabel(
											SharedPinListAddRemoveButtons.class,
											getPinNameLabelText()));
								}
								else {
									m_prefixLabel.setText(ResourceMgr.getStringForLabel(
											SharedPinListAddRemoveButtons.class,
											getPinListPrefixText()));
								}
							}
						};
				prefixTF.addKeyListener(keyListener);
				startnum.addKeyListener(keyListener);
				pincount.addKeyListener(keyListener);
				dialog.setVisible(true);

				if (!dialog.isCancelled()) {
					if (isAddSinglePin(startnum, pincount)) {
						String pname = prefixTF.getText().trim();
						mHandler.addPin(pname);
					}
					else {
						String prefixStr = prefixTF.getText();
						int pc = CommonUtils.parseIndex(pincount.getText());
						int sp = CommonUtils.parseIndex(startnum.getText());
						for (int i = 0; i < pc; i++) {
							String pname = new StringBuilder().append(prefixStr).append(sp + i).toString();
							mHandler.addPin(pname);
						}
					}
					m_postActionListener.accept(BUTTON_TYPE.ADD);
					m_proxyList.repaint();
				}
			}

			private boolean isAddSinglePin(@NotNull JTextField startnum, @NotNull JTextField pincount)
			{
				return (pincount.getText().trim().isEmpty() || CommonUtils.parseCount(pincount.getText()) == 1)
						&& startnum.getText().trim().isEmpty();
			}
		});
	}

	@NotNull protected String getHelpIDName()
	{
		return SharedPinListAddRemoveButtons.class.getName();
	}

	private void buildRenameButton()
	{
		renameButton = new SharedPinListRenameButton(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.renameButton.text"), m_proxyList, mHandler, getTooltipForRenameButton(),
				getTooltipForRenameButtonWhenNoObjectSelected(), getRemaneButtonTooltipForRenameForFrozenNotAllowed());
		renameButton.setEnabled(false);
		renameButton.setName("SPLAddRemove.renameButton");
		renameButton.setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				getTooltipForRenameButton()));
		renameButton.setMnemonic(ResourceMgr.getMnemonic(SharedPinListAddRemoveButtons.class,
				"SharedPinListAddRemoveButtons.renameButton.mnemonic"));
		renameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		renameButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final IPinProxy selected = m_proxyList.getSelectedValue();
				//dts0100642752-Software allows duplicate names to be added to a shared pin list
				final Set<String> proxyNames = mHandler.getProxyNames();
				// todo creddy: Why is rename pin enabled for libraried object?
				proxyNames.addAll(mHandler.getHiddenCavities());
				final String newName = displayRenameDialog(selected, proxyNames);
				if (newName != null) {
					mHandler.rename(selected, newName);
					m_postActionListener.accept(BUTTON_TYPE.RENAME);
					m_proxyList.repaint();
				}
			}
		});
	}

	@Nullable private String displayRenameDialog(final IPinProxy selected, final Set<String> proxyNames)
	{
		RenameDialog dialog = new RenameDialog(
				(Dialog) SwingServices.getRoot(this),
				selected.getName(), ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
				getRenamePinDialogTitle()), "",
				ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
						getPinNameLabelText()))
		{
			private Set<String> m_proxyNames = new HashSet<String>(proxyNames);

			// Return null if valid; errmsg otherwise.  Check name is not empty and not a duplicate
			@Nullable public String checkValidName(String newName, String oldname)
			{
				return mHandler.checkValidName(newName, m_proxyNames);
			}

			// Need to override this too as we don't mind if the name is the same as the original.
			@Nullable public String getNewName()
			{
				if (isCancelled()) {
					return null;
				}
				String nn = getNameProperty().getValue().trim();
				if (!nn.isEmpty()) {
					return nn;
				}
				else {
					return null;
				}
			}
		};
		dialog.getOkButton().setEnabled(true);
		dialog.setVisible(true);
		return dialog.getNewName();
	}

	public JButton getAddButton()
	{
		return m_addButton;
	}

	public JButton getRemoveButton()
	{
		return m_removeButton;
	}

	public JButton getRenameButton()
	{
		return renameButton;
	}

	protected String getPrefixString()
	{
		return mHandler.getProject().getNameMgr().getObjectPrefix(INameMgr.PIN).getString();
	}

	@NotNull protected String getPinCountText()
	{
		return "SharedPinListAddRemoveButtons.count.text";
	}

	@NotNull protected String getPinNameLabelText()
	{
		return "SharedPinListAddRemoveButtons.fullname.text";
	}

	@NotNull protected String getDialogTitle()
	{
		return "SharedPinListAddRemoveButtons.AddDialog.title";
	}

	@NotNull protected String getPinListPrefixText()
	{
		return "SharedPinListAddRemoveButtons.prefix.text";
	}

	@NotNull protected String getRemaneButtonTooltipForRenameForFrozenNotAllowed()
	{
		return "SharedPinListAddRemoveButtons.renameButton.notAllowedToRenameFrozen";
	}

	@NotNull protected String getTooltipForRenameButtonWhenNoObjectSelected()
	{
		return "SharedPinListAddRemoveButtons.renameButton.notOneSelected";
	}

	@NotNull protected String getTooltipForRenameButton()
	{
		return "SharedPinListAddRemoveButtons.renameButton.tooltip";
	}

	@NotNull protected String getRenamePinDialogTitle()
	{
		return "SharedPinListAddRemoveButtons.renameButton.dialogTitle";
	}

	@NotNull protected String getRemoveButtonTooltip()
	{
		return "SharedPinListAddRemoveButtons.removeButton.tooltip";
	}

	@NotNull protected String getAddPinsButtonTooltip()
	{
		return "SharedPinListAddRemoveButtons.addButton.tooltip";
	}
}
