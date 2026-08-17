/*
 * Copyright 2003-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.analysis.ui.AnalysisAttachmentPanel;
import chs.caf.CAFUtils;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.caplets.logic.actions.shared.helper.EditSharedPinlistHandler;
import chs.caplets.logic.actions.shared.helper.IEditSharedPinlistAdapter;
import chs.caplets.shared.ForeignDesignChangesHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAnalysableSymbolAssociatable;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFunction;
import chs.cof.logical.shared.ISharedModularConnector;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.security.IDomain;
import chs.cof.symbol.ISymbolDef;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.ctf.caf.ui.SharedDomainPanel;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.editui.PartAssignmentFailureReason;
import chs.ctf.editui.shared.SharedModularConnectorClient;
import chs.ctf.editui.shared.SharedModularTabDialog;
import chs.ctf.ui.AssociateSymbolHandler;
import chs.utilities.BuildInfo;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.IMessageContent;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.helpers.revisioning.ValidationObject;
import chs.utility.ui.SharedPinListSymbolInstance;
import com.mentor.lookandfeel.InstallOldUIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"MethodOnlyUsedFromInnerClass"}) public class EditSharedPinlistDialog extends CAFOkCancelDialog
		implements ChangeListener, IPinListShareContextProvider, IEditSharedPinlistAdapter
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull private final EditSharedPinlistHandler mHandler;
	private SharedDomainPanel sharedDomainPanel = null;

	@Nullable private EditSharedPinlistSymbolPanel editSharedPinlistSymbolPanel = null;
	@Nullable private SelectSharedPanel selectSharedPanel = null;
	@Nullable protected ReusePanel reusePanel = null;
	@Nullable protected MapPanel mapperPanel = null;
	@Nullable private SharedModularTabDialog modularTab = null;
	@Nullable private JPanel m_modularPanel;
	/**
	 * This field holds the Analysis simulation model attachment panel.
	 */
	@Nullable private AnalysisAttachmentPanel attachmentPanel;
	private Component mapperTab;
	private Component reuseTab;
	private Component attachmentTab;
	private JTabbedPane tabHolder;
	private JPanel boxHolder;
	private JCheckBox styleToggleCB;
	private JPanel centerDisplayPanel;
	private static final int AttachmentPanelHeight = 30;
	private static final int PreferredPanelWidth = 600;
	private static final int PreferredPanelHeight = 220;
	private static final int MinimumPanelWidth = 616;
	private static final int MinimumPanelHeight = 314;
	private static final int TabPanelWidth = 650;
	private static final int TabPanelHeight = 270;
	static final Dimension PreferredPanelSize = new Dimension(PreferredPanelWidth, PreferredPanelHeight);
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);
	private Frame mOwner;
	private JLabel noteOnSharedChange;

	public EditSharedPinlistDialog(Frame owner, @Nullable String inTitle, ISharedPinList spl, Model m)
	{
		this(owner, inTitle, spl, null, null, m, false);
	}

	public EditSharedPinlistDialog(Frame owner, @Nullable String inTitle, @Nullable final ISharedPinList spl,
			@Nullable chs.cof.logical.cable.IPinList cpl, @Nullable IPinList pl, Model lModel,
			boolean fromSymbol)
	{
		super(owner, inTitle, true);
		rememberSize(false);
		mOwner = owner;
		setMinimumSize(MinimumPanelSize);

		final EditSharedPinListModel esplModel = new EditSharedPinListModel(pl, cpl, spl);
		esplModel.addChangeListener(this);
		final ILogicDesign logicDesign = lModel.getDesign();
		mHandler = createHandler(esplModel, logicDesign, cpl);
		mHandler.initializeComponents(spl, cpl, pl, fromSymbol);

		if (mHandler.isSplice(spl, pl)) {
			selectSharedPanel.setPreferredSize(PreferredPanelSize);
			getContentPane().add(selectSharedPanel, BorderLayout.CENTER);
		}
		else {
			// The tabbed pane will hold all the pieces.
			centerDisplayPanel = new JPanel(new BorderLayout());
			tabHolder = new JTabbedPane(SwingConstants.TOP);
			tabHolder.setPreferredSize(new Dimension(TabPanelWidth, TabPanelHeight));
			tabHolder.setName("EditSharedPinListTabbedPane");
			tabHolder.setPreferredSize(PreferredPanelSize);

			setUpTabbedStyle();
			if (!mHandler.isModularConnectorWithAtLeastOneFilledPosition(cpl)) {
				styleToggleCB = new JCheckBox(ResourceMgr.getString(EditSharedPinlistDialog.class,
						"EditSharedPinlistDialog.TabbedStyle.text"), true);
				styleToggleCB.setName("TabbedStyleCB");
				styleToggleCB.addItemListener(new ItemListener()
				{
					public void itemStateChanged(ItemEvent e)
					{
						if (e.getStateChange() == ItemEvent.SELECTED) {
							setUpTabbedStyle();
						}
						else {
							setUpBoxStyle();
						}
					}
				});
			}

			boxHolder = new JPanel();
			boxHolder.setLayout(new GridBagLayout());
			centerDisplayPanel.add(tabHolder, BorderLayout.CENTER);
			JPanel bottomPanel = new JPanel();
			bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
			bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			if (sharedDomainPanel != null) {
				bottomPanel.add(sharedDomainPanel);
			}
			bottomPanel.add(createEditSharedDetailsInfo());
			centerDisplayPanel.add(bottomPanel, BorderLayout.SOUTH);
			getContentPane().add(centerDisplayPanel, BorderLayout.CENTER);
			if (BuildInfo.getBuildInfo().areQAExtensionsEnabled()
					|| BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
				if (styleToggleCB != null) {
					getContentPane().add(styleToggleCB, BorderLayout.NORTH);
				}
			}
		}

		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Set<IDomain> sharedDomains = sharedDomainPanel != null ? sharedDomainPanel.getSharedDomains() : null;
				boolean complete =
						mHandler.onCompletion((pinListMgr) -> LogicActionMessageHelper.warnLocked(pinListMgr),
								EditSharedPinlistDialog.this::duplicateNameConfirmation, sharedDomains);
				if (complete) {
					setVisible(false);
					dispose();
				}
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mHandler.onCancel();
				setVisible(false);
				dispose();
				setCancelled(true);
			}
		});

		initPanels();
		pack();
	}

	@NotNull
	protected EditSharedPinlistHandler createHandler(@NotNull EditSharedPinListModel esplModel,
			ILogicDesign logicDesign, @Nullable chs.cof.logical.cable.IPinList cpl)
	{
		return new EditSharedPinlistHandler(esplModel, logicDesign, cpl, this);
	}

	protected boolean duplicateNameConfirmation(@NotNull IStringProperty nameProp)
	{
		//display dialog to confirm duplicate names
		return MessageHelper.showDoNotShowThisMessageAgainYesNoDialogue(
				CAFUtils.getInstance().getWindowMgr().getDialogFrame(), getClass().getName(),
				"",
				ResourceMgr.getString(MessageHelper.class,
						"MessageHelper.DuplicateName.warning.message", nameProp.getValue()));
	}

	private void initPanels()
	{
		if (mapperPanel != null) {
			mapperPanel.init();
		}
		if (reusePanel != null) {
			reusePanel.init();
		}
		if (selectSharedPanel != null) {
			selectSharedPanel.init();
		}
	}

	public void initModularComponent(@NotNull ISharedPinList spl)
	{
		modularTab = new SharedModularTabDialog(spl.getManager());
		m_modularPanel = modularTab.getPanel((ISharedConnector) spl);
		SharedModularConnectorClient client = modularTab.getClient();

		Consumer<ILogicDesign> saveHandler = ForeignDesignChangesHandler.createdSaveHandler();

		client.setSaveDesignHandler(saveHandler);

		client.createBoundsForDesignChanges(
				(design) -> new ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges(
						CAFUtils.getInstance().getControllerForDesign(design)));

		modularTab.getClient().addChangeListener(this);
	}

	public void initAnalysisComponent(@NotNull ISharedPinList spl, @NotNull ILogicDesign design)
	{
		if (spl instanceof ISharedFunction) {
			return;
		}
		attachmentPanel = new AnalysisAttachmentPanel(mOwner, false, spl,
				spl instanceof IAnalysableSymbolAssociatable ?
						new AssociateSymbolHandler((IAnalysableSymbolAssociatable) spl) : null);
		if (design.getProject() != null) {
			attachmentPanel.setSubsystemId(design.getProject().getAnalysisMgr().getSubsystemId());
		}
		attachmentPanel.setParentWindow(this);
		attachmentPanel.addChangeListener(this);
	}

	public void initReuseComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		reusePanel = new ReusePanel(esplModel, design);
	}

	public boolean hasEditSymbolComponent()
	{
		return editSharedPinlistSymbolPanel != null;
	}

	public boolean hasSelectSharedComponent()
	{
		return selectSharedPanel != null;
	}

	@Override public boolean hasMapperComponent()
	{
		return mapperPanel != null;
	}

	@Override public boolean hasReuseComponent()
	{
		return reusePanel != null;
	}

	@Override public boolean hasAnalysisComponent()
	{
		return attachmentPanel != null;
	}

	@Override public boolean isModularClientModified()
	{
		return modularTab != null && modularTab.getClient().isModified();
	}

	@NotNull @Override public ValidationObject getModularErrors()
	{
		return modularTab != null ? modularTab.getClient().validateModel() : new ValidationObject();
	}

	public void initEditSymbolComponent(@NotNull EditSharedPinListModel esplModel)
	{
		editSharedPinlistSymbolPanel = createEditSharedPinlistSymbolPanel(esplModel);
		if (!editSharedPinlistSymbolPanel.isUseful()) {
			editSharedPinlistSymbolPanel = null;
		}
	}

	@NotNull
	protected EditSharedPinlistSymbolPanel createEditSharedPinlistSymbolPanel(@NotNull EditSharedPinListModel esplModel)
	{
		return new EditSharedPinlistSymbolPanel(esplModel);
	}

	@Override public void updateAnalysisPinMap(@Nullable Map<ISharedPin, String> sharedPinAndTransientNameMap)
	{
		if (attachmentPanel != null) {
			attachmentPanel.setSharedPinAndTransientNameMap(sharedPinAndTransientNameMap);
		}
	}

	@Override public boolean hasAnalysisComponentChanged()
	{
		return attachmentPanel != null && attachmentPanel.hasChanged();
	}

	public void initSelectSharedComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			boolean fromSymbol)
	{
		selectSharedPanel = createSelectSharedPanel(esplModel, design, fromSymbol);
		final IStringProperty nameProperty = selectSharedPanel.getNameProperty();
		if (fromSymbol && nameProperty != null) {
			nameProperty.setValue(esplModel.getSymbolDef().getName());
		}
	}

	@NotNull protected SelectSharedPanel createSelectSharedPanel(
			@NotNull EditSharedPinListModel esplModel,
			@NotNull ILogicDesign design, boolean fromSymbol)
	{
		return new SelectSharedPanel(esplModel, fromSymbol, design);
	}

	@Override public boolean isBackshellCompatible(@NotNull ISharedPinList spl)
	{
		return selectSharedPanel != null && selectSharedPanel.isBackshellCompatible(spl);
	}

	@Override public boolean hasSharedDomainChanged()
	{
		return getSharedDomainPanel() != null && getSharedDomainPanel().hasChanged();
	}

	private JPanel createEditSharedDetailsInfo()
	{

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		noteOnSharedChange =
				new JLabel(ResourceMgr.getString(EditSharedPinlistDialog.class,
						"EditSharedPinlistDialog.dialog.undodisablenote"),
						SwingConstants.LEFT);
		noteOnSharedChange.setName("undodisablenote");
		panel.add(noteOnSharedChange, BorderLayout.NORTH);
		return panel;
	}

	public void initMapperComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		mapperPanel = new MapPanel(esplModel, design);
	}

	public void initSharedDomainComponent(@NotNull ISharedPinList spl)
	{
		sharedDomainPanel = new SharedDomainPanel(spl, spl.getProject());
		sharedDomainPanel.addChangeListener(this);
		if (spl instanceof ISharedConnector && ((ISharedModularConnector) spl).isModularChild()) {
			sharedDomainPanel.getDomainProperty().setEnabled(false);
		}
	}

	public boolean saveModularChanges()
	{
		if (modularTab != null) {
//			CommandHelper commandHelper = new CAFCommandHelper();
//			commandHelper.endAndCommitTransaction();
			PartAssignmentFailureReason errorInPreparingForCommit = modularTab.getClient().canCommitChanges();
			if (errorInPreparingForCommit != null) {
				IMessageContent messageContent =
						PartAssignmentFailureReason.createDefaultMessageContent(errorInPreparingForCommit,
								ResourceMgr.getString(EditSharedPinlistDialog.class,
										"EditSharedPinlistDialog.EditSharedPinlistAction"));
				Message.show(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), PromptSeverity.ERROR,
						messageContent);

				return false;
			}
			modularTab.getClient().commitChanges();
			return true;
		}
		return true;
	}

	public void cleanupOnDispose()
	{
		if (selectSharedPanel != null) {
			selectSharedPanel.dispose();
		}
		// See dts0100793661 and dts0100754523
		removeActionListeners(getOkButton());
		removeActionListeners(getCancelButton());
		removeActionListeners(getHelpButton());
	}

	public void windowClosing(WindowEvent e)
	{
		super.windowClosing(e);
	}

	public void setVisible(boolean show)
	{
		if (show) {
			mHandler.refreshNameMgr();
		}
		super.setVisible(show);
	}

	private void setUpTabbedStyle()
	{
		assert (centerDisplayPanel != null) : "Unexpected sate of UI!!!";
		if (boxHolder != null) {
			centerDisplayPanel.remove(boxHolder);
			boxHolder.removeAll();
		}
		if (editSharedPinlistSymbolPanel != null) {
			tabHolder.add(ResourceMgr.getString(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.removesymbol.text"), editSharedPinlistSymbolPanel);
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1, ResourceMgr.getMnemonic(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.removesymbol.mnemonic"));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1, ResourceMgr.getString(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.removesymbol.tooltip"));
			editSharedPinlistSymbolPanel.setBorder(null);
		}
		if (selectSharedPanel != null) {
			// get the type name
			String typeName = mHandler.getObjectTypeDisplayName();

			tabHolder.add(ResourceMgr.getString(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.selectshared.text", typeName), selectSharedPanel);
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1, ResourceMgr.getMnemonic(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.selectshared.mnemonic"));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1, ResourceMgr.getString(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.selectshared.tooltip"));
			selectSharedPanel.setBorder(null);
		}
		if (mapperPanel != null) {
			mapperTab = tabHolder.add(getMapperTitle(), mapperPanel);
			tabHolder.setForegroundAt(tabHolder.indexOfComponent(mapperTab),
					mHandler.hasDuplicatePinNames() ? Color.RED : tabHolder.getForeground());
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1,
					ResourceMgr.getMnemonic(EditSharedPinlistDialog.class, getMapPinsMnemonics()));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1,
					ResourceMgr.getString(EditSharedPinlistDialog.class, getMapPinsTooltip()));
			mapperPanel.setBorder(null);
			//dts0100686308: We are able to remove unused pins from shared pinlist even when library part is assigned to it
			// repeated checking in/out of tabbed style box enables the "Pins" tab. Below is th fix for that.
			tabHolder.setEnabledAt(tabHolder.indexOfComponent(mapperTab), mapperPanel.isMapperEnabled());
		}
		if (reusePanel != null) {
			reuseTab = tabHolder.add(
					ResourceMgr.getString(EditSharedPinlistDialog.class, getReusePinsText()),
					reusePanel);
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1, ResourceMgr.getMnemonic(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.reusepins.mnemonic"));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1,
					ResourceMgr.getString(EditSharedPinlistDialog.class, getReusePinsTooltip()));
			reusePanel.setBorder(null);
		}
		if (attachmentPanel != null) {
			attachmentTab = tabHolder.add(ResourceMgr.getString(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.attach.text"), attachmentPanel);
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1, ResourceMgr.getMnemonic(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.attach.mnemonic"));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1, ResourceMgr.getString(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.attach.tooltip"));
			attachmentPanel.setBorder(null);
		}
		if (modularTab != null) {
			tabHolder.add(ResourceMgr.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.modular.text"),
					m_modularPanel);
			tabHolder.setMnemonicAt(tabHolder.getTabCount() - 1, ResourceMgr.getMnemonic(EditSharedPinlistDialog.class,
					"EditSharedPinlistDialog.modular.mnemonic"));
			tabHolder.setToolTipTextAt(tabHolder.getTabCount() - 1,
					ResourceMgr.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.modular.tooltip"));
		}
		centerDisplayPanel.add(tabHolder, BorderLayout.CENTER);
		pack();
	}

	@NotNull protected String getReusePinsTooltip()
	{
		return "EditSharedPinlistDialog.reusepins.tooltip";
	}

	@NotNull protected String getReusePinsText()
	{
		return "EditSharedPinlistDialog.reusepins.text";
	}

	@NotNull protected String getMapPinsTooltip()
	{
		return "EditSharedPinlistDialog.mappins.tooltip";
	}

	@NotNull private String getMapPinsMnemonics()
	{
		return "EditSharedPinlistDialog.mappins.mnemonic";
	}

	private void setUpBoxStyle()
	{
		assert (centerDisplayPanel != null) : "Unexpected sate of UI!!!";
		centerDisplayPanel.remove(tabHolder);
		tabHolder.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		int gridy = 0;
		int preferredPanelHeight = 0;
		if (editSharedPinlistSymbolPanel != null) {
			c.gridy = gridy++;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(editSharedPinlistSymbolPanel, c);
			editSharedPinlistSymbolPanel.setBorder(BorderFactory.createTitledBorder(ResourceMgr.getString(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.removesymbol.text")));
		}
		if (selectSharedPanel != null) {
			c.gridy = gridy++;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(selectSharedPanel, c);
			selectSharedPanel.setBorder(BorderFactory.createTitledBorder(ResourceMgr.getString(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.selectshared.text",
					mHandler.getObjectTypeDisplayName())));
		}
		if (mapperPanel != null) {
			c.gridy = gridy++;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(mapperPanel, c);
			TitledBorder titleBorder = BorderFactory.createTitledBorder(getMapperTitle());
			mapperPanel.setBorder(titleBorder);
			if (titleBorder != null && mHandler.hasDuplicatePinNames()) {
				titleBorder.setTitleColor(Color.RED);
			}
		}
		if (reusePanel != null) {
			c.gridy = gridy++;
			c.weighty = 0.9;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(reusePanel, c);
			reusePanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(EditSharedPinlistDialog.class, getReusePinsText())));
		}
		if (m_modularPanel != null) {
			c.gridy = gridy++;
			c.weighty = 0.9;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(m_modularPanel, c);
			m_modularPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.modular.text")));
		}
		if (attachmentPanel != null) {
			c.gridy = gridy++;
			c.weighty = 0.01;
			preferredPanelHeight += AttachmentPanelHeight;
			boxHolder.add(attachmentPanel, c);
			attachmentPanel.setBorder(BorderFactory.createTitledBorder(
					ResourceMgr.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.attach.text")));
		}
		boxHolder.setPreferredSize(new Dimension(PreferredPanelSize.width, preferredPanelHeight));
		centerDisplayPanel.add(boxHolder, BorderLayout.CENTER);
		pack();
	}

	@Nullable public ISharedPinList getSharedPinList()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedPinList();
		}
	}

	@Nullable public String getSharedPinListName()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedPinListName();
		}
	}

	@Nullable public String getSharedPinListRevision()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedPinListRevision();
		}
	}

	@Nullable public String getSharedObjectMateName()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedPinListMateName();
		}
	}

	@Nullable public String getSharedObjectMateRevision()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedPinListMateRevision();
		}
	}

	@Nullable public Map<IAbstractPin, IPinProxy> getInstanceToSharedMap()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getConnectivityToSharedMap();
		}
	}

	@Nullable public Map<IPinProxy, IAbstractPin> getSharedToInstanceMap()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSharedToConnectivityMap();
		}
	}

	@Nullable public Set<IPinProxy> getReusablePins()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return new HashSet<IPinProxy>(mHandler.getReusableProxies());
		}
	}

	@Nullable public ISharedPin getConnectedPinToMakeReuable(ISharedPin spin)
	{
		return mHandler.getConnectedPinToMakeReuable(spin);
	}

	public Collection<ISharedPin> getConnectedPinsToMakeReusableValues()
	{
		return mHandler.getConnectedPinsToMakeReusableValues();
	}

	public Map<ISharedPin, ISharedPin> getConnectedPinsToMakeReusable()
	{
		return mHandler.getConnectedPinsToMakeReusable();
	}

	@Nullable public List<IPinProxy> getPlugMapInfo()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return new ArrayList<IPinProxy>(mHandler.getProxies());
		}
	}

	@Nullable public ISymbolDef getSymbolDef()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSymbolDef();
		}
	}

	public Collection<ISymbolDef> getSymbolDefsToAdd()
	{
		if (isCancelled()) {
			return Collections.emptyList();
		}
		else {
			return mHandler.getSymbolDefsForAddition();
		}
	}

	@Nullable public String getAnalysisModel()
	{
		if (isCancelled()) {
			return null;
		}
		return mHandler.getAnalysisModel();
	}

	@Nullable public String getAnalysisFunctionRealiser()
	{
		if (isCancelled()) {
			return null;
		}
		return mHandler.getAnalysisFunctionRealiser();
	}

	@Nullable public String getOverriddenAnalysisInterfaces()
	{
		if (isCancelled()) {
			return null;
		}
		return mHandler.getOverriddenAnalysisInterfaces();
	}

	@Nullable public String getOverriddenAnalysisFailureModes()
	{
		if (isCancelled()) {
			return null;
		}
		return mHandler.getOverriddenAnalysisFailureModes();
	}

	@Nullable public Collection<SharedPinListSymbolInstance> getSymbolInstancesForDeletion()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getSymbolInstancesForDeletion();
		}
	}

	public void stateChanged(ChangeEvent e)
	{
		final EditSharedPinlistHandler.Status status = mHandler.evaluateStatus();

		// If any data on any tab has been changed, set ok enabled.
		getOkButton().setEnabled(status.isOkEnabled());
		getOkButton().setToolTipText(status.getOkStatusMessage());

		if (styleToggleCB != null && styleToggleCB.isSelected()) {
			if (mapperPanel != null) {
				tabHolder.setEnabledAt(tabHolder.indexOfComponent(mapperTab), status.isMapperEnabled());
				tabHolder.setTitleAt(tabHolder.indexOfComponent(mapperTab), getMapperTitle());
				tabHolder.setForegroundAt(tabHolder.indexOfComponent(mapperTab),
						status.hasDuplicatePins() ? Color.RED : tabHolder.getForeground());
			}
			if (reusePanel != null) {
				tabHolder.setEnabledAt(tabHolder.indexOfComponent(reuseTab), status.isReuseEnabled());
			}
			if (attachmentPanel != null) {
				tabHolder.setEnabledAt(tabHolder.indexOfComponent(attachmentTab), true);
			}
		}
		else {
			if (mapperPanel != null) {
				mapperPanel.setEnabled(status.isMapperEnabled());
				TitledBorder tb = (TitledBorder) mapperPanel.getBorder();
				if (tb != null) {
					tb.setTitle(getMapperTitle());
					tb.setTitleColor(
							status.hasDuplicatePins() ? Color.RED : UIManager.getColor("TitledBorder.titleColor"));
				}
				mapperPanel.repaint();
			}
			if (reusePanel != null) {
				reusePanel.setEnabled(status.isReuseEnabled());
			}
			if (attachmentPanel != null) {
				attachmentPanel.setEnabled(true);
			}
		}
	}

	public boolean reusablePinErrors()
	{
		return mHandler.reusablePinErrors();
	}

	private String getMapperTitle()
	{
		// Can mapper panel do any mapping?
		if (mHandler.getCablePinlist() != null) {
			return ResourceMgr.getString(EditSharedPinlistDialog.class, getMapPinsText());
		}
		else {
			return ResourceMgr.getString(EditSharedPinlistDialog.class, getAddPinsText());
		}
	}

	@NotNull protected String getAddPinsText()
	{
		return "EditSharedPinlistDialog.addpins.text";
	}

	@NotNull protected String getMapPinsText()
	{
		return "EditSharedPinlistDialog.mappins.text";
	}

	public boolean isSharedNameGenerated()
	{
		return mHandler.isSharedPinListNameGenerated();
	}

	public boolean isSharedMateNameGenerated()
	{
		return mHandler.isSharedPinListMateNameGenerated();
	}

	/**
	 * This method applies the attachment panel changes. It is called when the EditSharedPinListAction terminates.
	 *
	 * @param newPins, the list of newly created pins.
	 */
	public void applyAttachmentPanelChanges(List<String[]> newPins)
	{
		if (attachmentPanel != null) {
			attachmentPanel.setNewlyCreatedPinList(newPins);
			attachmentPanel.applyChanges();
		}
	}

	public Map<ISymbolDef, ReverseMap<IAbstractPin, IPinProxy>> getSymbolDefsToPinProxyMap()
	{
		return mHandler.getSymbolDefsToPinProxyMap();
	}

	public void showAnalysisTab()
	{
		if (styleToggleCB != null && styleToggleCB.isSelected()) {
			tabHolder.setSelectedIndex(tabHolder.indexOfTab(ResourceMgr.getString(
					EditSharedPinlistDialog.class, "EditSharedPinlistDialog.attach.text")));
		}
	}

	public boolean preserveInternalConnectivity()
	{
		return mHandler.preserveInternalConnectivity();
	}

	@Nullable public Map<IConnector, String> getModularConnectorToSharedNamesMap()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getModularConnectorToSharedNamesMap();
		}
	}

	@Nullable public Map<IConnector, Boolean> getModularConnectorToSharedNameGeneratedMap()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return mHandler.getModularConnectorToSharedNameGeneratedMap();
		}
	}

	@Override public boolean canMakePinsReserved()
	{
		return true;
	}

	@Nullable public Set<IDomain> getSharedDomains()
	{
		if (!isCancelled()) {
			if (getSharedDomainPanel() != null) {
				return getSharedDomainPanel().getSharedDomains();
			}
		}
		return null;
	}

	@Nullable private SharedDomainPanel getSharedDomainPanel()
	{
		if (sharedDomainPanel != null) {
			return sharedDomainPanel;
		}
		if (selectSharedPanel != null) {
			return selectSharedPanel.getSharedDomainPanel();
		}
		return null;
	}

	public void showAsReadonly()
	{
		super.showAsReadonly();
		if (editSharedPinlistSymbolPanel != null) {
			editSharedPinlistSymbolPanel.showAsReadOnly();
		}
		if (reusePanel != null) {
			reusePanel.showAsReadOnly();
		}
		if (mapperPanel != null) {
			mapperPanel.showAsReadOnly();
		}
		if (attachmentPanel != null) {
			attachmentPanel.showAsReadOnly();
		}

		if (m_modularPanel != null) {
			PropertyPanel propertyPanel = CommonUtils.cast(m_modularPanel, PropertyPanel.class);
			if (propertyPanel != null) {
				IPropertyGroup propertyGroup = propertyPanel.getPropertyGroup();
				if (propertyGroup != null) {
					propertyGroup.setEditable(false);
				}
			}
		}
		SharedDomainPanel domainPanel = getSharedDomainPanel();
		if (domainPanel != null) {
			domainPanel.showAsReadOnly();
		}
		if (noteOnSharedChange != null) {
			noteOnSharedChange.setText(ResourceMgr
					.getString(EditSharedPinlistDialog.class, "EditSharedPinlistDialog.dialog.readonlynote"));
		}
	}
}
