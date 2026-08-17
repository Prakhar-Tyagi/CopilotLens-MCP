/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.symbol;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.helpers.GridScaleSettings;
import chs.cof.draw.IGrid;
import chs.cof.security.IDomain;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.SymbolLibraryTypeEnum;
import chs.ctf.caf.ui.DomainPanel;
import chs.ctf.caf.ui.DomainPanelManager;
import chs.ctf.ui.form.SymbolLibraryRenameDlg;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.IPromptContent;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utilities.ui.messaging.impl.DefaultQuestionPromptSeverityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class EditSymbolLibraryDialog extends SymbolLibraryRenameDlg
{

	@NotNull private SymbolLibraryTypeEnum mSymTypeEnum;
	@Nullable private DomainPanel mDomainPanel;
	@Nullable private GridScaleSettings mGridScaleSettings = null;
	private boolean mSaveSettings = false;

	public EditSymbolLibraryDialog(@NotNull Frame frame, @NotNull String title, @NotNull IAbstractLibrary library,
			@NotNull Class<? extends ICapletLifecycle> lifeCycleCls)
	{
		super(frame, library, title, ResourceMgr.getString(lifeCycleCls,
				"Lifecycle.Rename.Label.Text"),
				lifeCycleCls, library,
				CAFUtils.getInstance().getSymbolLibraryMgr());

		IGrid inputGrid = library.getGrid();
		if (inputGrid == null) {
			inputGrid = FactoryMgr.getDrawFactory().createGrid();
		}
		mSymTypeEnum = library.getType();
		if (shouldShowGridSetting()) {
			mGridScaleSettings = new GridScaleSettings(this, inputGrid.getRealMapping(), inputGrid,
					library.getSymbolScaleType(), library.getResizable(), null, true, false);
			addAdditionalPanel(mGridScaleSettings.getPanel());
		}

		mDomainPanel = DomainPanelManager.getInstance().constructDomainPanel(library, null);
		if (mDomainPanel != null) {
			String domainTitle = mDomainPanel.getDomainLabel().getText();
			JPanel domainPanel = new JPanel(new GridBagLayout());
			domainPanel.setBorder(BorderFactory.createTitledBorder(domainTitle));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(2, 2, 2, 2);
			domainPanel.add(mDomainPanel.getDomainComboBox(), gbc);
			domainPanel.setSize(domainPanel.getPreferredSize());
			addAdditionalPanel(domainPanel);
		}

		// Initialize the dialog
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (shouldShowGridSetting()) {
					if (mGridScaleSettings.isUpdateAllExistingSymbols() ||
							mGridScaleSettings.isUpdateResizeAllExistingSymbols()) {
						boolean ret = true;
						if (mGridScaleSettings.isUpdateAllExistingSymbols()) {
							ResourceBasedMessageContent messageContent = getResourceBasedMessageContent();
							Choice okProceed = new Choice(getClass(), "EditSymbolLibraryDialog.warn.choice.saveAndContinue");
							Choice cancel = new Choice(getClass(), "EditSymbolLibraryDialog.warn.choice.cancel");
							final Choice selectedChoice = showUpdateAllConfirmationMessage(messageContent, okProceed, cancel);

							if (selectedChoice == cancel) {
								ret = false;
							}
						}
						if (ret) {
							mSaveSettings = true;
							setVisible(false);
						}
					}
					else {
						mSaveSettings = true;
						setVisible(false);
					}
				}
				else {
					mSaveSettings = true;
					setVisible(false);
				}
			}
		});

		if(mGridScaleSettings!=null) {
			mGridScaleSettings.registerValidtyListener(getOkListener());
		}
		// Setup an action listener on the Cancel button to terminate without success

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mSaveSettings = false;
				setVisible(false);
			}
		});
	}

	@NotNull private ResourceBasedMessageContent getResourceBasedMessageContent()
	{
		ResourceBasedMessageContent messageContent = new ResourceBasedMessageContent(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.title");
		messageContent.setContext(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.warn.title");
		messageContent.setMessage(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.warn.message");
		messageContent.setImplications(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.warn.implications");
		messageContent.setGuidance(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.warn.guidance");
		return messageContent;
	}

	@NotNull protected Choice showUpdateAllConfirmationMessage(IPromptContent messageContent, Choice okProceed, Choice cancel){
		Choice selectedChoice = Question.show(new DefaultQuestionPromptSeverityProvider()
		{
			@Override @NotNull public PromptSeverity getSeverity()
			{
				return PromptSeverity.WARNING;
			}
		}, messageContent, okProceed, cancel);
		return selectedChoice;
	}

	public boolean shouldShowGridSetting()
	{
		return SymbolLibraryTypeEnum.SYMBOL.equals(mSymTypeEnum);
	}

	@Nullable public IDomain getDomain()
	{
		if (mDomainPanel != null) {
			return mDomainPanel.getDomain();
		}
		return null;
	}

	@Nullable public GridScaleSettings getGridSettings()
	{
		return mGridScaleSettings;
	}

	public boolean isSave()
	{
		return mSaveSettings;
	}

	@NotNull public String getHelpID()
	{
		String helpIdPrefix =
				"chs.caf.helpers.ui.common.SymbolLibraryBrowserTree$EditSymbolLibraryAction$EditSymbolLibraryDlg";
		return helpIdPrefix + "_Convert_To_Physical_Scale";
	}

	public boolean isDomainChanged()
	{
		if (mDomainPanel != null) {
			return mDomainPanel.isDomainChanged();
		}
		return false;
	}

	@Nullable public DomainPanel getDomainPanel()
	{
		return mDomainPanel;
	}
}
