package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.NamePropertyValidator;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.ui.MCNode;
import chs.caplets.logic.actions.ui.MCNodeCreator;
import chs.caplets.logic.actions.ui.MCSharedMatcher;
import chs.caplets.logic.actions.ui.MCSharedNode;
import chs.caplets.logic.actions.ui.MCSharedNodeCreator;
import chs.caplets.logic.shared.LockedCableMulticoreFilter;
import chs.caplets.logic.shared.LockedSharedMulticoreFilter;
import chs.caplets.logic.shared.SharedAbstractableTree;
import chs.caplets.logic.shared.SharedMulticoreStructureMatchFilter;
import chs.caplets.shared.properties.PropertiesClient;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedOverbraidIterator;
import chs.cof.project.IProject;
import chs.cof.security.IDomain;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.ctf.caf.ui.SharedDomainPanel;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.AnimatedIcon;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.property.validator.AbstractPropertyValidator;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utilities.ui.tree.TreeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A JPanel clas which is used in MulticoreShareDialog
 */
public class MulticoreSharedPanel extends JPanel
{

	public static final int HSPACE = 10;
	public static final int VSPACE = 5;
	private final IProject m_project;
	private final SharedDomainPanel domainPanel;
	private JRadioButton exactRadioBtn;
	private ButtonGroup filterTypeButtonGroup;
	private JRadioButton containmentRadioBtn;
	private JRadioButton shareIntoRB;
	private JLabel shareIntoNotifLbl;
	private Runnable m_nameGenerated = null;
	protected PopulateSharedIntoListWorker shareIntoWorker;
	private final IDesign m_design;
	protected IStringProperty m_revisionProp;
	private IMulticore m_multicore;
	private JPanel treePanel;
	protected SharedAbstractableTree sharedMulticoreTree;
	private IPropertyGroup namingGroup;
	private JRadioButton createNewRadioBtn;
	protected IStringProperty m_nameProp;
	private SharedMulticoreModel m_sharedMulticoreModel;
	private List<ISharedMulticore> sharedMulticores;
	private Map<String, List<ISharedMulticore>> shareIntoMulticores;
	private LockedSharedMulticoreFilter m_sharedLockFilter;
	private LockedCableMulticoreFilter m_cableLockFilter;
	private SharedMulticoreStructureMatchFilter m_structureMatchFilter;
	private MulticoreSharedController m_multicoreSharedController;
	private static final String CONTAINMENT = "CONTAINMENT";
	private static final String EXACT = "EXACT";
	private boolean isStrictMatch = true;
	@Nullable private String previousSelectMulticoreName;

	public MulticoreSharedPanel(@NotNull SharedMulticoreModel sharedMulticoreModel, IDesign design,
			@NotNull MulticoreSharedController multicoreSharedController)
	{
		setName("MulticoreSharedPanel");
		m_sharedMulticoreModel = sharedMulticoreModel;
		m_multicoreSharedController = multicoreSharedController;
		m_multicore = sharedMulticoreModel.getMulticore();
		m_design = design;
		m_project = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();

		setFilters();
		createNameAndRevisionProperty(sharedMulticoreModel, (ILogicDesign) design);

		addValidatorsAndListeners();

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = newGridBagConstraints();
		Insets cmptInsets = new Insets(VSPACE, HSPACE, 0, HSPACE);
		gbc.insets = cmptInsets;
		ButtonGroup shareTypeButtonGroup = new ButtonGroup();
		ItemListener buttonListener = sharedRadioButtonListener();
		createNewRadioBtn = newRadioButton("createNewRadioBtn", "MulticoreSharedPanel.createNewRB.text", true,
				shareTypeButtonGroup, buttonListener);
		add(createNewRadioBtn, gbc);
		JPanel namePanel = getNamePanel();
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		add(namePanel, gbc);

		domainPanel = new SharedDomainPanel(null, m_project);
		gbc.gridy++;
		add(domainPanel, gbc);

		JPanel bottomPanel = new JPanel(new GridBagLayout());
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridy++;
		add(bottomPanel, gbc);

		JPanel btnPanel = getButtonPanel(shareTypeButtonGroup, buttonListener);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		gbc.gridy = 0;
		gbc.insets = cmptInsets;
		bottomPanel.add(btnPanel, gbc);

		gbc.gridy++;
		JPanel filterPanel = getFilterPanel();
		bottomPanel.add(filterPanel, gbc);
		// Add empty panel which will receive the tree and uses the space below for now
		treePanel = new JPanel(new BorderLayout());
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.gridy++;
		gbc.insets = new Insets(VSPACE, HSPACE, VSPACE, HSPACE);
		bottomPanel.add(treePanel, gbc);

		loadSharedMulticores();
		createNewRadioBtn.setSelected(true);
		setRequestFocusEnabled(true);
	}

	private void createNameAndRevisionProperty(
			@NotNull SharedMulticoreModel sharedMulticoreModel, ILogicDesign design)
	{
		Model model = new Model(CAFUtils.getInstance().getActiveCapletController(), design);
		PropertiesClient propClient = new PropertiesClient(model);
		namingGroup = PropertyFactory.createPropertyGroup("Group");
		namingGroup.setBorder(BorderValue.NONE);
		final IPropertyGroup nameGroup = createNameGroup();
		m_nameProp = createNameProperty(nameGroup);
		IReadOnlyNamedObject namedObject = MulticoreSharedController.getNamedObject(m_multicore);
		if (namedObject != null) {
			m_nameProp.addValidator(new NamePropertyValidator(propClient, namedObject));
		}

		final boolean isEnabled = m_multicore.isGeneratedName();
		final IBooleanProperty generatedProp = createGeneratedProperty(sharedMulticoreModel, nameGroup, isEnabled);
		generatedProp.touch();
		m_nameGenerated = () -> generatedProp.setEnabled(m_multicore.isGeneratedName());
		m_nameGenerated.run();
		generatedProp.setToolTipText(ResourceMgr.getString(MulticoreSharedPanel.class, isEnabled ?
				"MulticoreSharedPanel.generated.enabled.tooltip" : "MulticoreSharedPanel.generated.disabled.tooltip"));

		m_revisionProp = createRevisionProperty();
		String revision = sharedMulticoreModel.getSharedMulticore() != null ?
				sharedMulticoreModel.getSharedMulticore().getRevision() : "1";
		m_revisionProp.setValue(revision);
	}

	@NotNull private JPanel getButtonPanel(ButtonGroup shareTypeButtonGroup, ItemListener buttonListener)
	{
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		shareIntoRB = newRadioButton("shareIntoRadio", "MulticoreSharedPanel.shareIntoRB.text", false,
				shareTypeButtonGroup, buttonListener);
		btnPanel.add(shareIntoRB);
		shareIntoNotifLbl = SelectSharedPanel
				.newMessageLabel("shareIntoNotifLbl", "MulticoreSharedPanel.shareIntoNotifLbl.loading.text");
		SelectSharedPanel.animateLabel(shareIntoNotifLbl);
		btnPanel.add(shareIntoNotifLbl);
		return btnPanel;
	}

	@NotNull private JPanel getFilterPanel()
	{
		JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		ItemListener filterTypeItemListener = filterTypeItemListener();
		JLabel matchLabel =
				new JLabel(ResourceMgr.getString(MulticoreSharedPanel.class,
						"MulticoreSharedPanel.matchLbl.text"),
						SwingConstants.LEFT);
		filterPanel.add(matchLabel);
		filterTypeButtonGroup = new ButtonGroup();
		exactRadioBtn =
				newRadioButton("exactRadioBtn", "MulticoreSharedPanel.exactRB.text", true, filterTypeButtonGroup,
						filterTypeItemListener);
		filterPanel.add(exactRadioBtn);
		containmentRadioBtn = newRadioButton("containmentRadioBtn", "MulticoreSharedPanel.containmentRB.text", false,
				filterTypeButtonGroup,
				filterTypeItemListener);
		filterPanel.add(containmentRadioBtn);
		exactRadioBtn.setEnabled(false);
		containmentRadioBtn.setEnabled(false);
		return filterPanel;
	}

	@NotNull private PropertyPanel getNamePanel()
	{
		return new PropertyPanel("NamePanel", namingGroup);
	}

	@NotNull private IStringProperty createRevisionProperty()
	{
		IStringProperty revisionProp =
				namingGroup.createStringProperty("MulticoreSharedPanel.sharedrevision.text", null, null);
		revisionProp.setLabel(
				ResourceMgr.getStringForLabel(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedrevision.text"));
		revisionProp.setToolTipText(
				ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedrevision.tooltip"));
		revisionProp.setHorizontalWeight(1.0);
		revisionProp.setHorizontalFill(true);
		return revisionProp;
	}

	@NotNull private IBooleanProperty createGeneratedProperty(
			@NotNull SharedMulticoreModel sharedMulticoreModel, @NotNull IPropertyGroup nameGroup,
			boolean isEnabled)
	{
		final IBooleanProperty generatedProp =
				nameGroup.createBooleanProperty("Generated", "Generated", isEnabled);
		generatedProp.setLabel(ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.generated"));
		generatedProp.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				sharedMulticoreModel.setSharedMulticoreNameGenerated(generatedProp.getValue());
			}
		});
		return generatedProp;
	}

	private void loadSharedMulticores()
	{
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		sharedConductorMgr.refresh();
		sharedMulticores = new ArrayList<>();
		for (ISharedMulticoreIterator smIt = sharedConductorMgr.getEditableSharedMulticores(); smIt.hasNext(); ) {
			ISharedMulticore shdMulticore = smIt.getNext();
			if (shdMulticore.getRootMulticore() == shdMulticore) {
				sharedMulticores.add(shdMulticore);
			}
		}
		for (ISharedOverbraidIterator sbIt = sharedConductorMgr.getEditableSharedOverbraids(); sbIt.hasNext(); ) {
			// get the overbraids and add it to the list...
			ISharedOverbraid shdOverBrd = sbIt.getNext();
			if (shdOverBrd.getRootMulticore() == shdOverBrd) {
				sharedMulticores.add(shdOverBrd);
			}
		}
		shareIntoWorker = getShareIntoWorker();
		shareIntoWorker.execute();
	}

	@NotNull protected PopulateSharedIntoListWorker getShareIntoWorker()
	{
		return new PopulateSharedIntoListWorker();
	}

	@NotNull
	private static JRadioButton newRadioButton(String name, String resourceTag, boolean selected, ButtonGroup group,
			ItemListener listener)
	{
		JRadioButton radioBtn =
				new JRadioButton(ResourceMgr.getString(MulticoreSharedPanel.class, resourceTag, selected));
		radioBtn.setName(name);
		radioBtn.addItemListener(listener);
		group.add(radioBtn);
		return radioBtn;
	}

	private void addValidatorsAndListeners()
	{
		IPropertyValidator nameValidator = new AbstractPropertyValidator()
		{
			public boolean validate(IProperty property)
			{
				String valid = m_multicoreSharedController.validateName(property, m_multicore);
				if (valid != null) {
					setReason(valid);
				}
				// Valid if there was no error string
				return valid == null;
			}
		};
		m_nameProp.addValidator(nameValidator);
		m_nameProp.addValidityListener(m_sharedMulticoreModel);
		IPropertyValidator revisionValidator = new AbstractPropertyValidator()
		{
			public boolean validate(IProperty property)
			{
				String revision = m_revisionProp.getValue();
				@SuppressWarnings("MagicNumber") StringBuffer errmsg = new StringBuffer(32);
				boolean valid = validateRevision(revision, errmsg);

				//duplicate name
				if (!valid) {
					setReason(errmsg.toString());
				}
				return valid;
			}

			private boolean validateRevision(String revision, StringBuffer errmsg)
			{
				boolean ok = true;

				if (revision == null || revision.trim().isEmpty() ||
						revision.length() > CHSConstants.SHAREDOBJECT_REVISION_LENGTH) {
					if (errmsg.length() != 0) {
						errmsg.append('\n');
					}
					errmsg.append(
							ResourceMgr.getString(MulticoreSharedPanel.class,
									"MulticoreSharedPanel.InvalidRevision.text"));
					ok = false;
				}

				return ok;
			}
		};
		m_revisionProp.addValidator(revisionValidator);

		PropertyChangeListener nameRevisionListener = new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				IStringProperty p = (IStringProperty) evt.getSource();
				String value = StringUtils.trim(p.getValue());

				// if there is a validator and the validator determines value is not valid, then set the value to null
				if ((p.getValidator() != null) && (!p.getValidator().validate(p))) {
					value = null;
				}

				if (p == m_nameProp) {
					m_sharedMulticoreModel.setSharedMulticoreName(value);
				}
				else if (p == m_revisionProp) {
					m_sharedMulticoreModel.setSharedMulticoreRevision(value);
				}
			}
		};
		m_nameProp.addPropertyChangeListener(nameRevisionListener);
		m_revisionProp.addPropertyChangeListener(nameRevisionListener);
	}

	@NotNull private IStringProperty createNameProperty(IPropertyGroup nameGroup)
	{
		IStringProperty nameProp =
				nameGroup.createStringProperty("MulticoreSharedPanel.SharedObjectNameTF", null, null);
		nameProp.setHorizontalWeight(1.0);
		nameProp.setHorizontalFill(true);
		nameProp.setObject(m_multicore.getName());
		nameProp.setDefaultValueObject(m_multicore);
		nameProp.setLabel(ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedname.text"));
		nameProp.setToolTipText(
				ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedname.tooltip"));
		return nameProp;
	}

	@NotNull private IPropertyGroup createNameGroup()
	{
		final IPropertyGroup nameGroup = namingGroup.createPropertyGroup("NameGroup", GroupTypeValue.ROW);
		nameGroup.setLabel("Name: ");
		nameGroup.setBorder(BorderValue.NONE);
		nameGroup.setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
		nameGroup.setLabel(
				ResourceMgr.getStringForLabel(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedname.text"));
		return nameGroup;
	}

	private void setFilters()
	{
		m_cableLockFilter = new LockedCableMulticoreFilter(m_multicore);
		m_sharedLockFilter = new LockedSharedMulticoreFilter();
		m_sharedLockFilter.setDesign(m_design);
		m_structureMatchFilter = new SharedMulticoreStructureMatchFilter();
		m_structureMatchFilter.setLocalMulticore(m_multicore);
	}

	@NotNull private ItemListener filterTypeItemListener()
	{
		return new ItemListener()
		{

			@Override public void itemStateChanged(ItemEvent e)
			{
				boolean exactMatch = exactRadioBtn.isSelected();
				boolean containmentMatch = containmentRadioBtn.isSelected();
				if (sharedMulticoreTree != null && (exactMatch || containmentMatch)) {
					TreePath path = sharedMulticoreTree.getSelectionPath();
					DefaultMutableTreeNode sel =
							path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;
					if (sel != null && sel.getUserObject() instanceof ISharedMulticore) {
						ISharedMulticore previousSelectedMulticore = ((ISharedMulticore) sel.getUserObject());
						previousSelectMulticoreName = previousSelectedMulticore.getName();
					}
					sharedMulticoreTree.clearSelection();
					sharedMulticoreTree.clear();
					treePanel.setVisible(true);
					shareIntoNotifLbl.setVisible(false);
					m_sharedMulticoreModel.setSharedMulticoreName(null);
					if (exactMatch) {
						isStrictMatch = true;
						m_structureMatchFilter.setStrictMatch(true);
						populateSharedMulticores(EXACT);
					}
					else {
						isStrictMatch = false;
						m_structureMatchFilter.setStrictMatch(false);
						populateSharedMulticores(CONTAINMENT);
					}
					TreeUtils.expandAll(sharedMulticoreTree, true);
				}
			}
		};
	}

	public void init()
	{
		if (m_nameProp != null) {
			m_nameProp.touch();
		}
		if (m_revisionProp != null) {
			m_revisionProp.touch();
		}
	}

	protected class PopulateSharedIntoListWorker extends SwingWorker<Map<String, List<ISharedMulticore>>, Object>
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

		@NotNull @Override public Map<String, List<ISharedMulticore>> doInBackground()
		{
			Map<String, List<ISharedMulticore>> status = populateMatchedMulticores();
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

		private void stopLabelAnimation(@NotNull JLabel label)
		{
			Icon icon = label.getIcon();
			if (icon instanceof AnimatedIcon) {
				((AnimatedIcon) icon).stop();
			}
			label.setIcon(null);
		}

		public void updatePanelUI(Map<String, List<ISharedMulticore>> sharedMulticoreMap)
		{
			shareIntoMulticores = sharedMulticoreMap;
			stopLabelAnimation(shareIntoNotifLbl);
			List<ISharedMulticore> exactSharedMulticores = shareIntoMulticores.get(EXACT);
			List<ISharedMulticore> containmentSharedMulticores = shareIntoMulticores.get(CONTAINMENT);
			if ((exactSharedMulticores == null || exactSharedMulticores.isEmpty()) &&
					(containmentSharedMulticores == null || containmentSharedMulticores.isEmpty())) {
				shareIntoRB.setEnabled(false);
				shareIntoNotifLbl.setText(ResourceMgr.getString(MulticoreSharedPanel.class,
						"MulticoreSharedPanel.shareIntoNotifLbl.emptyList.text"));
			}
			else {
				if (exactSharedMulticores == null || exactSharedMulticores.isEmpty()) {
					sharedMulticoreTree = new SharedAbstractableTree(m_design);
					String noRecords = ResourceMgr.getString(MulticoreSharedPanel.class,
							"MulticoreSharedPanel.shareIntoNotifLbl.emptyExactList.text");
					sharedMulticoreTree.addElement(noRecords);
					DefaultTreeModel model = (DefaultTreeModel) sharedMulticoreTree.getModel();
					model.reload();
				}
				else {
					sharedMulticoreTree = new SharedAbstractableTree(exactSharedMulticores, m_design);
				}
				shareIntoRB.setEnabled(true);
				m_structureMatchFilter.setStrictMatch(true);
				shareIntoNotifLbl.setVisible(false);
				treePanel.setToolTipText(
						ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedlist.tooltip"));
				registerFilters();
				sharedMulticoreTree.setEnabled(false);
				sharedMulticoreTree.setName("MulticoreSharedPanel.SharedMulticoreList");
				sharedMulticoreTree.clearSelection();
				DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel();
				dtsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
				sharedMulticoreTree.setSelectionModel(dtsm);
				ToolTipManager.sharedInstance().registerComponent(sharedMulticoreTree);
				treePanel.add(new JScrollPane(sharedMulticoreTree), BorderLayout.CENTER);
				treePanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1, CHSColors.getBorderColor()));
				sharedMulticoreTree.addTreeSelectionListener(newTreeSelectionListener());
			}
			revalidate();
			repaint();
		}

		private void registerFilters()
		{
			m_sharedLockFilter.setSharedAbstractableTree(sharedMulticoreTree);
			sharedMulticoreTree.registerFilter(m_sharedLockFilter);
			sharedMulticoreTree.registerFilter(m_cableLockFilter);
			m_structureMatchFilter.setSharedAbstractableTree(sharedMulticoreTree);
			sharedMulticoreTree.registerFilter(m_structureMatchFilter);
		}

		@NotNull private TreeSelectionListener newTreeSelectionListener()
		{
			return new TreeSelectionListener()
			{
				public void valueChanged(TreeSelectionEvent e)
				{
					TreePath path = sharedMulticoreTree.getSelectionPath();
					DefaultMutableTreeNode sel =
							path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;
					ISharedMulticore newSharedMulticore;
					if (sel != null && sel.getUserObject() instanceof ISharedMulticore) {
						newSharedMulticore = ((ISharedMulticore) sel.getUserObject());
					}
					else {
						newSharedMulticore = null;
					}
					onShareIntoMulticoreSelect(newSharedMulticore);
				}
			};
		}

		@NotNull private Map<String, List<ISharedMulticore>> populateMatchedMulticores()
		{
			Map<String, List<ISharedMulticore>> filteredSharedMulticores = new HashMap<>();
			if (isCancelled()) {
				return filteredSharedMulticores;
			}
			if (sharedMulticores != null && !sharedMulticores.isEmpty()) {
				Map<IUID, String> usedRevisions = mapRevisions();
				if (isCancelled()) {
					return filteredSharedMulticores;
				}
				MCNode multicoreNode = Objects.requireNonNull(new MCNodeCreator(m_multicore).execute());
				List<ISharedMulticore> exactMatchedSharedMulticores = new ArrayList<>();
				List<ISharedMulticore> containmentMatchedSharedMulticores = new ArrayList<>();
				MCSharedMatcher mcSharedMatcher = new MCSharedMatcher();
				for (ISharedMulticore sharedMulticore : sharedMulticores) {
					if (isCancelled()) {
						return filteredSharedMulticores;
					}
					if (!isPartCompatible(sharedMulticore)) {
						continue;
					}
					String usedRevision = usedRevisions.get(sharedMulticore.getBaseId());
					if (usedRevision == null || usedRevision.equals(sharedMulticore.getRevision())) {
						MCSharedNode sharedMulticoreNode = Objects.requireNonNull(
								new MCSharedNodeCreator(sharedMulticore).execute());
						if (mcSharedMatcher.isMatched(true, multicoreNode, sharedMulticoreNode)) {
							exactMatchedSharedMulticores.add(sharedMulticore);
						}
						else if (mcSharedMatcher.isMatched(false, multicoreNode, sharedMulticoreNode)) {
							containmentMatchedSharedMulticores.add(sharedMulticore);
						}
					}
				}
				if (isCancelled()) {
					return filteredSharedMulticores;
				}

				if (!exactMatchedSharedMulticores.isEmpty()) {
					filteredSharedMulticores.put(EXACT, exactMatchedSharedMulticores);
					List<ISharedMulticore> containment = new ArrayList<>(exactMatchedSharedMulticores);
					filteredSharedMulticores.put(CONTAINMENT, containment);
				}
				if (isCancelled()) {
					return filteredSharedMulticores;
				}
				if (!containmentMatchedSharedMulticores.isEmpty()) {
					List<ISharedMulticore> containment = filteredSharedMulticores.get(CONTAINMENT);
					if (containment != null && !containment.isEmpty()) {
						containment.addAll(containmentMatchedSharedMulticores);
					}
					else {
						containment = new ArrayList<>(containmentMatchedSharedMulticores);
					}
					filteredSharedMulticores.put(CONTAINMENT, containment);
				}
			}
			return filteredSharedMulticores;
		}

		private boolean isPartCompatible(ISharedMulticore sharedMulticore)
		{
			if (m_multicore.isPartAssigned()) {
				if (!sharedMulticore.isPartAssigned()) {
					return false;
				}
				return m_multicore.getLibraryRef() == sharedMulticore.getLibraryRef();
			}
			return true;
		}

		@NotNull private Map<IUID, String> mapRevisions()
		{
			// Map all used revisions to their base id: only one revision of the family can
			// be used
			Map<IUID, String> usedRevisions = new HashMap<IUID, String>();
			IConnectivity connectivity = m_design.getConnectivity();
			if (connectivity != null) {
				for (IMulticoreIterator mcIt = connectivity.getMulticores(); mcIt.hasNext(); ) {
					IMulticore multicore = mcIt.getNext();
					ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
					if (sharedMulticore != null) {
						ISharedMulticore root = sharedMulticore.getRootMulticore();
						usedRevisions.put(root.getBaseId(), root.getRevision());
					}
				}
			}
			return usedRevisions;
		}
	}

	private void populateSharedMulticores(String filterType)
	{
		List<ISharedMulticore> filteredSharedMulticores = shareIntoMulticores.get(filterType);
		if (filteredSharedMulticores != null && !filteredSharedMulticores.isEmpty()) {
			sharedMulticoreTree.addElements(filteredSharedMulticores, false);
		}
		else {
			String noRecords = ResourceMgr.getString(MulticoreSharedPanel.class,
					"MulticoreSharedPanel.shareIntoNotifLbl.emptyExactList.text");
			sharedMulticoreTree.addElement(noRecords);
		}
		DefaultTreeModel model = (DefaultTreeModel) sharedMulticoreTree.getModel();
		model.reload();
		if (previousSelectMulticoreName != null) {
			sharedMulticoreTree.selectShareIntoCandidate(previousSelectMulticoreName);
		}
		sharedMulticoreTree.selectShareIntoCandidate(m_multicore.getName());
	}

	public void onShareIntoMulticoreSelect(@Nullable ISharedMulticore newSharedMulticore)
	{
		onShareIntoMulticore(newSharedMulticore, this::setSharedMulticoreOnModel);
	}

	public boolean onShareIntoMulticore(@Nullable ISharedMulticore newSharedMulticore,
			@NotNull Consumer<ISharedMulticore> newMulticoreSetter)
	{
		ISharedMulticore oldSharedMulticore = m_sharedMulticoreModel.getSharedMulticore();
		if (newSharedMulticore != oldSharedMulticore) {
			unlockSharedMulticore(oldSharedMulticore);
			final ISharedMulticore verifiedNewMulticore = verifyNewSharedMulticore(newSharedMulticore);
			newMulticoreSetter.accept(verifiedNewMulticore);
			return verifiedNewMulticore != null;
		}
		return false;
	}

	private void unlockSharedMulticore(@Nullable ISharedMulticore oldSharedMulticore)
	{
		if (oldSharedMulticore != null) {
			unlock(oldSharedMulticore);
		}
	}

	@Nullable private ISharedMulticore verifyNewSharedMulticore(@Nullable ISharedMulticore newSharedMulticore)
	{
		if (newSharedMulticore != null) {
			for (IObjectUIFilterOption objectFilter : getFilters()) {
				if (!objectFilter.selected(newSharedMulticore)) {
					return null;
				}
			}
		}
		return newSharedMulticore;
	}

	@NotNull public IObjectUIFilterOption[] getFilters()
	{
		// Order is important
		return new IObjectUIFilterOption[]{m_sharedLockFilter, m_cableLockFilter, m_structureMatchFilter};
	}

	private void unlock(@NotNull ISharedMulticore sharedMulticore)
	{
		sharedMulticore.unlock();
	}

	public void setSharedMulticoreOnModel(@Nullable ISharedMulticore newSharedMulticore)
	{
		m_multicoreSharedController.setStricMatch(isStrictMatch);
		m_sharedMulticoreModel.setSharedMulticore(newSharedMulticore);
	}

	@NotNull private GridBagConstraints newGridBagConstraints()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;

		return gbc;
	}

	@NotNull private ItemListener sharedRadioButtonListener()
	{
		return new ItemListener()
		{
			@Override public void itemStateChanged(ItemEvent e)
			{
				boolean createNew = createNewRadioBtn.getModel().isSelected();
				boolean shareInto = shareIntoRB.getModel().isSelected();
				namingGroup.setEnabled(createNew);
				domainPanel.getDomainProperty().setEnabled(createNew);
				if (sharedMulticoreTree != null && (createNew || shareInto)) {
					sharedMulticoreTree.setEnabled(!createNew);
					if (createNew) {
						sharedMulticoreTree.clearSelection();
						filterTypeButtonGroup.clearSelection();
						exactRadioBtn.setEnabled(false);
						containmentRadioBtn.setEnabled(false);
						if (m_sharedMulticoreModel.getSharedMulticore() != null) {
							unlock(m_sharedMulticoreModel.getSharedMulticore());
							m_sharedMulticoreModel.setSharedMulticore(null);
						}
						m_nameProp.touch();
						if (m_nameGenerated != null) {
							m_nameGenerated.run();
						}
					}
					else {
						exactRadioBtn.setEnabled(true);
						containmentRadioBtn.setEnabled(true);
						if (shareIntoMulticores.get(EXACT) != null) {
							filterTypeButtonGroup.setSelected(exactRadioBtn.getModel(), true);
						}
						else {
							filterTypeButtonGroup.setSelected(containmentRadioBtn.getModel(), true);
						}
						sharedMulticoreTree.selectShareIntoCandidate(m_multicore.getName());
						m_sharedMulticoreModel.setSharedMulticoreName(null);
					}
				}
			}
		};
	}

	public void dispose()
	{
		// cancel the worker in case still running
		if (shareIntoWorker != null) {
			shareIntoWorker.forceTerminate();
			waitForShareIntoWrokerToFinish(10, 100); /*1s*/
		}
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

	@Nullable public Set<IDomain> getSharedDomains()
	{
		return domainPanel.getSharedDomains();
	}
}
