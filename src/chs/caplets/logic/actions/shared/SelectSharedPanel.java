/*
 * Copyright 2005-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.NamePropertyValidator;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.SharedPinListInfo;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.helper.ISelectSharedAdapter;
import chs.caplets.logic.actions.shared.helper.SelectSharedHandler;
import chs.caplets.logic.shared.AbstractLockedSharedObjectFilter;
import chs.caplets.logic.shared.LockedSharedObjectFilter;
import chs.caplets.logic.shared.ModularConnectorTreeModel;
import chs.caplets.logic.shared.ModularConnectorTreePanel;
import chs.caplets.logic.shared.SharedAbstractableTree;
import chs.caplets.shared.properties.PropertiesClient;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.ctf.caf.ui.SharedDomainPanel;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.AnimatedIcon;
import chs.utilities.ui.TextIcon;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 25, 2005 Time: 12:39:16 PM
 */
public class SelectSharedPanel extends JPanel
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull protected final SelectSharedHandler mHandler;
	@NotNull private final SelectSharedAdapter mAdapter;
	private final SharedDomainPanel domainPanel;
	@NotNull private Model m_model;

	public static final int HSPACE = 10;
	public static final int VSPACE = 5;
	// UI Stuff
	protected SharedAbstractableTree sharedPinListTree;
	private JRadioButton m_symbolCreateNewRB;
	private JRadioButton m_symbolShareIntoRB;
	private JPanel treePanel;
	private JLabel shareIntoNotifLbl;
	protected PopulateSharedIntoListWorker shareIntoWorker;

	public SelectSharedPanel(@NotNull EditSharedPinListModel emodel, boolean fromSymbol, ILogicDesign design)
	{
		setName("SelectSharedPanel");
		m_model = new Model(CAFUtils.getInstance().getActiveCapletController(), design);
		mAdapter = new SelectSharedAdapter(m_model);
		mHandler = createHandler(emodel, design, mAdapter, fromSymbol);

		// Property structure is in place
		// Create the panel
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = newGridBagConstraints();
		Insets cmptInsets = new Insets(VSPACE, HSPACE, 0, HSPACE);
		gbc.insets = cmptInsets;

		ButtonGroup symbolButtonGroup = new ButtonGroup();
		ActionListener buttonListener = newRadioButtonActionListener();
		m_symbolCreateNewRB = newRadioButton("createNewRadio", "SelectSharedPanel.createNewRB.text", true,
				symbolButtonGroup, buttonListener);
		if (!mHandler.isModularConnectorWithAtLeastOneFilledPosition()) {
			add(m_symbolCreateNewRB, gbc);
		}

		JPanel namePanel = getNamePanel(emodel);
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		add(namePanel, gbc);

		domainPanel =
				new SharedDomainPanel(emodel.getSharedPinList(),
						mHandler.getProject());
		if (emodel.getCablePinlist() != null && emodel.getCablePinlist().supportsDomain()) {
			gbc.gridy++;
			add(domainPanel, gbc);
		}


		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridy++;
		if (mHandler.isShareIntoAllowed()) {
			add(bottomPanel, gbc);
		}

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		m_symbolShareIntoRB = newRadioButton("shareIntoRadio", "SelectSharedPanel.shareIntoRB.text", false,
				symbolButtonGroup, buttonListener);
		m_symbolShareIntoRB.setEnabled(false);
		btnPanel.add(m_symbolShareIntoRB);

		shareIntoNotifLbl = newMessageLabel("shareIntoNotifLbl", "SelectSharedPanel.shareIntoNotifLbl.loading.text");
		animateLabel(shareIntoNotifLbl);
		btnPanel.add(shareIntoNotifLbl);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		gbc.gridy = 0;
		gbc.insets = cmptInsets;
		bottomPanel.add(btnPanel, gbc);

		// Add empty panel which will receive the tree and uses the space below for now
		treePanel = new JPanel(new BorderLayout());
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.gridy++;
		gbc.insets = new Insets(VSPACE, HSPACE, VSPACE, HSPACE);
		bottomPanel.add(treePanel, gbc);

		m_symbolCreateNewRB.setSelected(true);

		// Populate the list in separate thread
		shareIntoWorker = getShareIntoWorker();
		shareIntoWorker.execute();

		setRequestFocusEnabled(true);
	}

	@NotNull protected SelectSharedHandler createHandler(@NotNull EditSharedPinListModel emodel,
			ILogicDesign design, @NotNull ISelectSharedAdapter creator, boolean fromSymbol)
	{
		final IProject currentProject = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
		return new SelectSharedHandler(emodel, design, fromSymbol, creator, currentProject);
	}

	protected PopulateSharedIntoListWorker getShareIntoWorker()
	{
		return new PopulateSharedIntoListWorker();
	}

	private JPanel getNamePanel(@NotNull EditSharedPinListModel emodel)
	{
		JPanel namePanel;
		if (mHandler.isModularConnectorWithAtLeastOneFilledPosition()) {
			namePanel = new JPanel(new BorderLayout(5, 10));
//			ModularConnectorTree tree = new ModularConnectorTree((IConnector)pinlist, esplModel);
//			JScrollPane treeView = new JScrollPane(tree.getTree());
//			treeView.setPreferredSize(new Dimension(450, 100));
//			namePanel.add(treeView, BorderLayout.CENTER);
			ModularConnectorTreeModel model = new ModularConnectorTreeModel(null, emodel, mHandler.getDesign(),
					(IConnector) mHandler.getCablePinlist());
			ModularConnectorTreePanel panel =
					new ModularConnectorTreePanel(model);
			panel.initTree();
			namePanel.add(panel, BorderLayout.CENTER);

			namePanel.add(new PropertyPanel("NamePanel", mAdapter.getNameGroup()), BorderLayout.SOUTH);
		}
		else {
			namePanel = new PropertyPanel("NamePanel", mAdapter.getNameGroup());
		}
		return namePanel;
	}

	public static JLabel newMessageLabel(String name, String resourceTag)
	{
		JLabel label = new JLabel(name);
		label.setText(ResourceMgr.getString(SelectSharedPanel.class, resourceTag));

		// Change font to italics
		Font labelFont = label.getFont();
		Font newFont = labelFont.deriveFont(Font.ITALIC);
		label.setFont(newFont);
		//Use padding for Italic Font
		label.setBorder(new EmptyBorder(0, 3, 0, 3));
		// Change color to disabled
		JTextArea dummyText = new JTextArea();
		Color color = dummyText.getDisabledTextColor();
		label.setForeground(color);

		return label;
	}

	private static JRadioButton newRadioButton(String name, String resourceTag, boolean selected, ButtonGroup group,
			ActionListener listener)
	{
		JRadioButton radioBtn = new JRadioButton(ResourceMgr.getString(SelectSharedPanel.class, resourceTag, selected));
		radioBtn.setName(name);
		radioBtn.addActionListener(listener);
		group.add(radioBtn);

		return radioBtn;
	}

	public static void animateLabel(JLabel label)
	{
		label.setHorizontalTextPosition(SwingConstants.LEADING);
		AnimatedIcon icon = new AnimatedIcon(label);
		icon.setAlignmentX(AnimatedIcon.LEFT);
		icon.addIcon(new TextIcon(label, "."));
		icon.addIcon(new TextIcon(label, ".."));
		icon.addIcon(new TextIcon(label, "..."));
		icon.addIcon(new TextIcon(label, "...."));
		icon.addIcon(new TextIcon(label, "....."));
		label.setIcon(icon);
		icon.start();
	}

	private GridBagConstraints newGridBagConstraints()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;

		return gbc;
	}

	private ActionListener newRadioButtonActionListener()
	{
		return new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean createNew = e.getSource() == m_symbolCreateNewRB;
				mAdapter.getNameGroup().setEnabled(createNew);
				domainPanel.getDomainProperty().setEnabled(createNew);
				if (sharedPinListTree != null) {
					sharedPinListTree.setEnabled(!createNew);
					if (createNew) {
						sharedPinListTree.clearSelection();
						mHandler.onCreateNew();
					}
					else {
						sharedPinListTree.selectShareIntoCandidate(mHandler.getCablePinlist().getName());
						mHandler.preShareInto();
					}
				}
			}
		};
	}

	public void setSharedPinListOnModel(@Nullable ISharedPinList newSPL)
	{
		int numberOfUnLoadedDSUM = mHandler.getNumberOfUnloadedDSUM();

		if (numberOfUnLoadedDSUM > 0) {
			// removed simple progress indicator as it was causing array out of bounds exception
			// with two threads accessing the list of proxies - dts0101237548
			// instead changing the cursor type to show the user that processing is happening
			try {
				setCursor(new Cursor(Cursor.WAIT_CURSOR));
				mHandler.setSharedPinListOnModel(newSPL);
			}
			finally {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}
		else {
			mHandler.setSharedPinListOnModel(newSPL);
		}
	}

	@Nullable public SharedDomainPanel getSharedDomainPanel()
	{
		return domainPanel;
	}

	protected class PopulateSharedIntoListWorker extends SwingWorker<List<ISharedPinList>, Object>
	{

		private boolean isFinished = false;
		private boolean isForciblyTerminated = false;

		protected PopulateSharedIntoListWorker()
		{
		}

		private void forceTerminate()
		{
			//if the doInBackground is already finished but done is pending
			//then cancel will do nothing. so need to update a flag also.
			cancel(true);
			isForciblyTerminated = true;
		}

		@Override public List<ISharedPinList> doInBackground()
		{
			List<ISharedPinList> status = retrieveShareIntoPinLists();
			isFinished = true;
			return status;
		}

		public boolean isFinished()
		{
			return isFinished;
		}

		@Override protected void done()
		{
			try {
				//but now we don't want to update if either it was cancelled within
				//doInBackground or forceterminated during dispose of dialog.
				if (!(isCancelled() || isForciblyTerminated)) {
					updatePanelUI(get());
				}
			}
			catch (InterruptedException ignore) {
			}
			catch (ExecutionException ignore) {
			}
		}

		protected List<ISharedPinList> retrieveShareIntoPinLists()
		{
			List<ISharedPinList> shareIntoPinLists = new ArrayList<ISharedPinList>(10);

			if (isCancelled()) {
				return shareIntoPinLists;
			}
			final Map<IUID, String> usedRevisions = mHandler.mapRevisions();

			if (isCancelled()) {
				return shareIntoPinLists;
			}

			IProject proj = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
			final ISharedPinListMgr splmgr = proj.getSharedPinListMgr();

			Map<ISharedPinList, SharedPinListInfo> spls = getSharedPinListInfos(splmgr);

			if (isCancelled()) {
				return shareIntoPinLists;
			}

			for (ISharedPinList spl : ((ISharedFullyLoadedPinListMgr)splmgr)
					.getEditableSharedPinLists(PinListTypeEnum.from_connectivity(mHandler.getCablePinlist()))) {
				if (isCancelled()) {
					return shareIntoPinLists;
				}

				final SharedPinListInfo sharedPinListInfo = spls.get(spl);
				if (mHandler.isValidPinlistToShareInto(usedRevisions, sharedPinListInfo, spl)) {
					shareIntoPinLists.add(spl);
				}
			}

			return shareIntoPinLists;
		}

		private void stopLabelAnimation(JLabel label)
		{
			Icon icon = label.getIcon();
			if (icon instanceof AnimatedIcon) {
				((AnimatedIcon) icon).stop();
			}
			label.setIcon(null);
		}

		private TreeSelectionListener newTreeSelectionListener()
		{
			return new TreeSelectionListener()
			{
				public void valueChanged(TreeSelectionEvent e)
				{
					TreePath path = sharedPinListTree.getSelectionPath();
					DefaultMutableTreeNode sel =
							path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;

					ISharedPinList newSPL;
					if (sel != null && sel.getUserObject() instanceof ISharedPinList) {
						newSPL = (ISharedPinList) sel.getUserObject();
					}
					else {
						newSPL = null;
					}

					onShareIntoPinlistSelect(newSPL);
				}
			};
		}

		public void updatePanelUI(List<ISharedPinList> shareIntoPinLists)
		{
			if (shareIntoPinLists.isEmpty()) {
				stopLabelAnimation(shareIntoNotifLbl);
				shareIntoNotifLbl.setText(ResourceMgr.getString(SelectSharedPanel.class,
						"SelectSharedPanel.shareIntoNotifLbl.emptyList.text"));
			}
			else {
				// Remove the label from the bottom panel and make the radio button available
				shareIntoNotifLbl.setVisible(false);
				m_symbolShareIntoRB.setEnabled(true);

				// Add the tree
				treePanel.setToolTipText(
						ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedlist.tooltip"));
				sharedPinListTree = new SharedAbstractableTree(shareIntoPinLists, mHandler.getDesign());
				mAdapter.setSharedAbstractableTree(sharedPinListTree);
				for (IObjectUIFilterOption filter : mHandler.getFilters()) {
					sharedPinListTree.registerFilter(filter);
				}
				sharedPinListTree.setEnabled(false);
				sharedPinListTree.setName("SelectSharedPanel.SharedObjectList");
				sharedPinListTree.clearSelection();
				DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel();
				dtsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
				sharedPinListTree.setSelectionModel(dtsm);
				ToolTipManager.sharedInstance().registerComponent(sharedPinListTree);
				treePanel.add(new JScrollPane(sharedPinListTree), BorderLayout.CENTER);
				sharedPinListTree.addTreeSelectionListener(newTreeSelectionListener());
			}

			revalidate();
			repaint();
		}
	}

	public void onShareIntoPinlistSelect(@Nullable ISharedPinList newSPL)
	{
		mHandler.onShareIntoPinlist(newSPL, this::setSharedPinListOnModel);
	}

	public boolean isBackshellCompatible(ISharedPinList targetSharedPinlist)
	{
		return mHandler.isBackshellCompatible(targetSharedPinlist);
	}

	@NotNull
	protected Map<ISharedPinList, SharedPinListInfo> getSharedPinListInfos(ISharedPinListMgr mgr)
	{
		return mHandler.getSharedPinListInfos(mgr);
	}

	public void init()
	{
		mHandler.init();
	}

	@Nullable public IStringProperty getNameProperty()
	{
		return mHandler.getNameProperty();
	}

	/**
	 * Clean up the model
	 */
	public void dispose()
	{
		// cancel the worker in case still running
		if (shareIntoWorker != null) {
			shareIntoWorker.forceTerminate();
			waitForShareIntoWrokerToFinish(10, 100); /*1s*/
		}

		m_model.destroy();
	}

	protected void waitForShareIntoWrokerToFinish(int pollingCount, int pollingSleepTime)
	{
		if (shareIntoWorker != null) {
			for (int i = 0; i < pollingCount; ++i) {
				if (shareIntoWorker.isFinished()) {
					break;
				}
				try {
					//noinspection BusyWait
					Thread.sleep(pollingSleepTime);
				}
				catch (InterruptedException ignored) {
					break;
				}
			}
		}
	}

	private static class SelectSharedAdapter implements ISelectSharedAdapter
	{

		@NotNull private IPropertyGroup namingGroup;
		@NotNull private LockedSharedObjectFilter mLockSharedFilter;
		@NotNull private Model mModel;

		private SelectSharedAdapter(@NotNull Model model)
		{
			mModel = model;
			namingGroup = PropertyFactory.createPropertyGroup("Group");
			namingGroup.setBorder(BorderValue.NONE);
			mLockSharedFilter = new LockedSharedObjectFilter();
		}

		@NotNull public IStringProperty createNameProperty(boolean isInlineJack)
		{
			final IPropertyGroup nameGroup = createGroup("NameGroup");
			final IStringProperty nameProp =
					nameGroup.createStringProperty("SelectSharedPanel.SharedObjectNameTF", null, null);
			nameProp.setHorizontalWeight(1.0);
			nameProp.setHorizontalFill(true);
			if (isInlineJack) {
				nameGroup.setLabel(
						ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedreceptaclename.text"));
				nameProp.setLabel(
						ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedreceptaclename.text"));
				nameProp.setToolTipText(
						ResourceMgr.getString(SelectSharedPanel.class,
								"SelectSharedPanel.sharedreceptaclename.tooltip"));
			}
			else {
				nameGroup.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedname.text"));
				nameProp
						.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedname.text"));
				nameProp.setToolTipText(
						ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedname.tooltip"));
			}
			return nameProp;
		}

		@NotNull private IPropertyGroup createGroup(@NotNull String groupName)
		{
			final IPropertyGroup group = namingGroup.createPropertyGroup(groupName, GroupTypeValue.ROW);
			group.setLabel("Name: ");
			group.setBorder(BorderValue.NONE);
			group.setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
			return group;
		}

		@NotNull public IBooleanProperty createGeneratedProperty(boolean defaultGeneratedValue, boolean isEnabled)
		{
			IPropertyGroup nameGroup =
					CommonUtils.cast(namingGroup.getPropertyByName("NameGroup"), IPropertyGroup.class);
			if (nameGroup == null) {
				nameGroup = createGroup("NameGroup");
			}
			final IBooleanProperty generatedProp =
					nameGroup.createBooleanProperty("Generated", "Generated", defaultGeneratedValue);
			generatedProp.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.generated"));
			setupTooltipTextOnGeneratedCheckBox(generatedProp, isEnabled);
			return generatedProp;
		}

		@NotNull public IStringProperty createMateNameProperty()
		{
			IPropertyGroup nameMateGroup = createGroup("NameMateGroup");
			final IStringProperty nameMateProp =
					nameMateGroup.createStringProperty("SelectSharedPanel.SharedObjectMateNameTF", null, null);
			nameMateProp.setHorizontalWeight(1.0);
			nameMateProp.setHorizontalFill(true);
			nameMateGroup
					.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedplugname.text"));
			nameMateProp
					.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedplugname.text"));
			nameMateProp.setToolTipText(
					ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedplugname.tooltip"));
			return nameMateProp;
		}

		@NotNull
		public IBooleanProperty createMateGeneratedProperty(boolean defaultMateGeneratedvalue, boolean isEnabled)
		{
			IPropertyGroup nameMateGroup =
					CommonUtils.cast(namingGroup.getPropertyByName("NameMateGroup"), IPropertyGroup.class);
			if (nameMateGroup == null) {
				nameMateGroup = createGroup("NameMateGroup");
			}
			final IBooleanProperty generatedMateProp = nameMateGroup.createBooleanProperty("MateGenerated",
					"Generated", defaultMateGeneratedvalue);
			generatedMateProp.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.generated"));
			setupTooltipTextOnGeneratedCheckBox(generatedMateProp, isEnabled);
			return generatedMateProp;
		}

		private void setupTooltipTextOnGeneratedCheckBox(@NotNull IBooleanProperty generatedProp, boolean isEnabled)
		{
			generatedProp.setToolTipText(ResourceMgr.getString(SelectSharedPanel.class, isEnabled ?
					"SelectSharedPanel.generated.enabled.tooltip" : "SelectSharedPanel.generated.disabled.tooltip"));
		}

		@NotNull public IStringProperty createRevisionProperty()
		{
			final IStringProperty revisionProp =
					namingGroup.createStringProperty("SelectSharedPanel.sharedrevision.text", null, null);
			revisionProp
					.setLabel(ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedrevision.text"));
			revisionProp.setToolTipText(
					ResourceMgr.getString(SelectSharedPanel.class, "SelectSharedPanel.sharedrevision.tooltip"));
			revisionProp.setHorizontalWeight(1.0);
			revisionProp.setHorizontalFill(true);
			return revisionProp;
		}

		@NotNull @Override public AbstractLockedSharedObjectFilter getLockedSharedObjectFilter(boolean isBulkShare)
		{
			return mLockSharedFilter;
		}

		@NotNull public IPropertyGroup getNameGroup()
		{
			return namingGroup;
		}

		public void setSharedAbstractableTree(@NotNull SharedAbstractableTree sharedPinListTree)
		{
			mLockSharedFilter.setSharedAbstractableTree(sharedPinListTree);
		}

		@NotNull public IPropertyValidator createNamePropertyValidator(@Nullable IReadOnlyNamedObject namedObject,
				@Nullable INameMgr nameMgr)
		{
			final PropertiesClient propClient = new PropertiesClient(mModel);
			return new NamePropertyValidator(propClient, namedObject);
		}

		@Override public boolean shouldReportNameValidation()
		{
			return true;
		}
	}
}
