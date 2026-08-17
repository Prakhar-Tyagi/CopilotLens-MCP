/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2022 Siemens
 */
package chs.caplets.logic.actions.ui;

import chs.cof.changepolicy.IChangePolicyMgr;
import chs.utilities.ui.BasicUIFactory;
import chs.common.IObjectFilter;
import chs.common.ValueTypeEnum;
import chs.ctf.editui.AttributePropertyFactory;
import chs.ctf.editui.helpers.PropertyTable;
import chs.ctf.editui.helpers.TableBuilder;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.CommonUtils;
import chs.utilities.KeySeparatedStringBuilder;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.HorizontalValue;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyTypeValue;
import chs.utilities.ui.tree.AllNodeFilterableTree;
import chs.utilities.ui.tree.BasicDisplayNameTreeSearchFilter;
import chs.utilities.ui.tree.RightClickExpandCollapseMouseListener;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author chandras on 09-03-2018.
 */
public class FacetConflictResolutionPanel<T> extends JPanel implements IFacetConflictResolutionListener<T>
{

	private static final String NAME_COLUMN =
			ResourceMgr.getString(FacetConflictResolutionPanel.class, "FacetConflictResolutionPanel.column.name");
	private static final String FROM_COLUMN =
			ResourceMgr.getString(FacetConflictResolutionPanel.class, "FacetConflictResolutionPanel.column.from");
	private static final String TO_COLUMN =
			ResourceMgr.getString(FacetConflictResolutionPanel.class, "FacetConflictResolutionPanel.column.to");
	private static final String RESULT_COLUMN =
			ResourceMgr.getString(FacetConflictResolutionPanel.class, "FacetConflictResolutionPanel.column.result");

	private static final int ROW_HEIGHT = 25;
	private static final double BROWSER_SPLIT = 0.3;
	private static final double CONFLICT_SPLIT = 0.55;
	private static final int BUTTON_MAX_NAME_LENGTH = 40;

	@NotNull private final JPanel m_objectBrowser;
	@NotNull private final JPanel m_attributeConflicts;
	@NotNull private final JPanel m_propertyConflicts;
	@Nullable protected ConflictTree m_tree;
	@NotNull private final JSplitPane m_conflictSplit;
	@NotNull private final JLabel m_guidanceLabel;
	@NotNull private final JLabel m_statusLabel;
	@NotNull private final FacetSelectResultFrom m_resultChooser;
	@NotNull private final IFacetConflictResolutionModel m_model;
	@NotNull private final Map<IFacetConflictInfo, IProperty> m_facetPanels = new HashMap<>();

	@NotNull private final StatusGuideHandler m_sourceButtonGuide;
	@NotNull private final StatusGuideHandler m_targetButtonGuide;
	@NotNull private final StatusGuideHandler m_resultButtonGuide;
	@NotNull private final StatusGuideHandler m_treePaneGuide;

	@Nullable private final Window m_owner;

	@SuppressWarnings("OverlyLongMethod")
	public FacetConflictResolutionPanel(@Nullable Window owner, @NotNull IFacetConflictResolutionModel model)
	{
		setName("Facet.Conflict.Resolution.Panel");
		m_owner = owner;
		m_model = model;
		m_attributeConflicts = new JPanel();
		m_attributeConflicts.setName("Facet.Conflict.Resolution.Attributes");
		m_attributeConflicts.setLayout(new BorderLayout());

		m_propertyConflicts = new JPanel();
		m_propertyConflicts.setName("Facet.Conflict.Resolution.Properties");
		m_propertyConflicts.setLayout(new BorderLayout());

		m_objectBrowser = new JPanel();
		m_objectBrowser.setName("Facet.Conflict.Resolution.Browser");
		m_objectBrowser.setLayout(new BorderLayout());

		//initialize the status update handlers before creating panels.
		//these will be added as mouse listeners to the panels.
		m_sourceButtonGuide = new StatusGuideHandler(() -> ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.guidance.srcButton", getSourceObjectName(false)));
		m_targetButtonGuide = new StatusGuideHandler(() -> ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.guidance.targetButton", getTargetObjectName(false)));
		m_resultButtonGuide = new StatusGuideHandler(() -> ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.guidance.resultButton"));
		m_treePaneGuide = new StatusGuideHandler(() -> ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.guidance.treePane"));

		addNoConflictLabel(m_objectBrowser);

		buildAttributesConflictPanel();

		buildPropertiesConflictPanel();

		JPanel objectBrowserWithInset = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = getDefaultGBC();
		gbc.insets = new Insets(3, 3, 3, 3);
		objectBrowserWithInset.add(m_objectBrowser, gbc);

		JPanel propertyConflictsWithInset = new JPanel(new GridBagLayout());
		gbc = getDefaultGBC();
		gbc.insets = new Insets(3, 3, 3, 3);
		propertyConflictsWithInset.add(m_propertyConflicts, gbc);

		JPanel attributeConflictsWithInset = new JPanel(new GridBagLayout());
		gbc = getDefaultGBC();
		gbc.insets = new Insets(3, 3, 3, 3);
		attributeConflictsWithInset.add(m_attributeConflicts, gbc);

		m_conflictSplit =
				new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributeConflictsWithInset, propertyConflictsWithInset);
		m_conflictSplit.setName("Facet.Conflict.Resolution.Conflict");

		m_guidanceLabel = new JLabel(StringUtils.SPACE, SwingConstants.CENTER);
		m_guidanceLabel.setName("Facet.Conflict.Resolution.Guidance");

		m_resultChooser = new FacetSelectResultFrom(this::selectResultFromForTree);
		JPanel leftPane = postfixByResultSelectionOption(objectBrowserWithInset, m_resultChooser);

		JPanel rightPane = new JPanel(new GridBagLayout());
		rightPane.setName("Facet.Conflict.Resolution.RightPane");
		gbc = getDefaultGBC();
		rightPane.add(m_guidanceLabel, gbc);

		gbc = getDefaultGBC();
		gbc.gridy = 1;
		rightPane.add(m_conflictSplit, gbc);

		JSplitPane browserSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
		setLayout(new GridBagLayout());

		gbc = getDefaultGBC();
		add(browserSplit, gbc);

		gbc = getDefaultGBC();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		m_statusLabel = new JLabel(StringUtils.SPACE);
		add(m_statusLabel, gbc);

		//we will not have status panel. the tooltips are considered sufficient.
		m_statusLabel.setVisible(false);

		addComponentListener(new ComponentAdapter()
		{
			@Override public void componentResized(ComponentEvent e)
			{
				super.componentResized(e);
				//do this only once at intiatialization.
				browserSplit.setDividerLocation(BROWSER_SPLIT);
				int dividerLocation = new Double(browserSplit.getHeight() * CONFLICT_SPLIT).intValue();
				m_conflictSplit.setDividerLocation(dividerLocation);
				removeComponentListener(this);
			}
		});
	}

	private GridBagConstraints getDefaultGBC()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		return gbc;
	}

	private void selectResultFromForNode(@NotNull ValueOption option)
	{
		if (!m_facetPanels.isEmpty()) {
			for (Map.Entry<IFacetConflictInfo, IProperty> entry : m_facetPanels.entrySet()) {
				propagateUserChoice(entry.getKey(), entry.getValue(), option);
			}
			reDrawWindow();
		}
	}

	private void selectResultFromForTree(@NotNull ValueOption option)
	{
		Set<IFacetConflictNode> selectedNodes = getSelectedNodes();
		Set<IFacetConflictNode> topNodes = m_model.getTopNodes();
		for (IFacetConflictNode facetConflictNode : topNodes) {
			propagateUserChoice(facetConflictNode, option, selectedNodes::contains);
		}
		selectResultFromForNode(option);
		reDrawWindow();
	}

	private void propagateUserChoice(@NotNull IFacetConflictNode facetConflictNode, @NotNull ValueOption option,
			@NotNull IObjectFilter<IFacetConflictNode> filter)
	{
		for (IFacetConflictNode conflictNode : m_model.getChildNodes(facetConflictNode)) {
			propagateUserChoice(conflictNode, option, filter);
		}

		if (filter.accept(facetConflictNode)) {
			for (IFacetConflictInfo facetConflictInfo : facetConflictNode.getConflicts()) {
				facetConflictInfo.setUserChoice(option);
			}
		}
	}

	private String getSourceObjectName(boolean trimmed)
	{
		return buildObjectName(IFacetConflictNode::getSourceName, trimmed);
	}

	private String getTargetObjectName(boolean trimmed)
	{
		return buildObjectName(IFacetConflictNode::getTargetName, trimmed);
	}

	private String buildObjectName(@NotNull Function<IFacetConflictNode, String> mapper, boolean trimmed)
	{
		KeySeparatedStringBuilder name = new KeySeparatedStringBuilder(":");
		Set<IFacetConflictNode> topNodes = m_model.getTopNodes();
		int objCount = topNodes.size();
		if (objCount > 0) {
			int limit = BUTTON_MAX_NAME_LENGTH / objCount;
			for (IFacetConflictNode topNode : topNodes) {
				String val = mapper.apply(topNode);
				name.append(trimmed ? limitValue(val, limit) : val);
			}
		}
		return name.toString();
	}

	private String limitValue(@NotNull String val, int limit)
	{
		if (val.length() > limit) {
			String append = "...";
			return val.substring(0, Math.max(limit - append.length(), 0)) + append;
		}
		return val;
	}

	@NotNull private JPanel postfixByResultSelectionOption(@NotNull JPanel candidate,
			@NotNull FacetSelectResultFrom selectResultFrom)
	{
		JPanel result = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		result.add(selectResultFrom, gbc);

		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		result.add(candidate, gbc);
		return result;
	}

	private void buildPropertiesConflictPanel()
	{
		addNoConflictLabel(m_propertyConflicts);
		m_propertyConflicts.setBorder(BorderFactory.createTitledBorder(
				ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
						"FacetConflictResolutionPanel.properties")));
	}

	private void buildAttributesConflictPanel()
	{
		addNoConflictLabel(m_attributeConflicts);
		m_attributeConflicts.setBorder(BorderFactory.createTitledBorder(
				ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
						"FacetConflictResolutionPanel.attributes")));
	}

	protected void addNoConflictLabel(JPanel panel)
	{
		String noConflict = ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.noConflicts");
		JLabel view = new JLabel(noConflict, SwingConstants.CENTER);
		panel.add(new JScrollPane(view));
	}

	@NotNull private Set<IFacetConflictNode> getSelectedNodes()
	{
		if (m_tree != null) {
			TreePath[] selectionPaths = m_tree.getSelectionPaths();
			if (selectionPaths != null) {
				return doGetSelectedNodes(selectionPaths);
			}
		}
		return Collections.emptySet();
	}

	@NotNull private Set<IFacetConflictNode> doGetSelectedNodes(@NotNull TreePath[] selectionPaths)
	{
		Set<IFacetConflictNode> conflictNodeSet = new HashSet<>(selectionPaths.length);
		for (TreePath selectionPath : selectionPaths) {
			ConflictTreeObjectNode conflicNode =
					CommonUtils.cast(selectionPath.getLastPathComponent(), ConflictTreeObjectNode.class);
			if (conflicNode != null) {
				conflictNodeSet.add((IFacetConflictNode) conflicNode.getUserObject());
			}
		}
		return Collections.unmodifiableSet(conflictNodeSet);
	}

	@SuppressWarnings("unused")
	public void treeNodeSelected(@NotNull TreeSelectionEvent e)
	{
		clearFacetPanel();

		Set<IFacetConflictNode> selectedNodes = getSelectedNodes();
		IFacetConflictNode facetConflictNode = selectedNodes.size() == 1 ? selectedNodes.iterator().next() : null;

		if (facetConflictNode != null) {
			Set<IFacetConflictInfo> attributes = new LinkedHashSet<>();
			Set<IFacetConflictInfo> properties = new LinkedHashSet<>();
			for (IFacetConflictInfo entry : facetConflictNode.getConflicts()) {
				if (entry.isAttribute()) {
					attributes.add(entry);
				}
				else {
					properties.add(entry);
				}
			}
			buildFacetConflictsPanel(facetConflictNode, m_attributeConflicts, attributes, "conflictValues.attr");
			buildFacetConflictsPanel(facetConflictNode, m_propertyConflicts, properties, "conflictValues.prop");
		}

		treeNodeSelectionChanged(selectedNodes);

		reDrawWindow();
	}

	private void buildFacetConflictsPanel(@NotNull IFacetConflictNode node, @NotNull JPanel owner,
			@NotNull Set<IFacetConflictInfo> facets, @NotNull String tableName)
	{
		if (facets.isEmpty()) {
			addNoConflictLabel(owner);
		}
		else {
			PropertyTable conflictValues = getResolutionTable(node, facets, tableName);
			conflictValues.addMouseListener(m_resultButtonGuide);
			owner.add(conflictValues.buildComponent());
		}
	}

	@NotNull
	private PropertyTable getResolutionTable(@NotNull IFacetConflictNode node,
			@NotNull Set<IFacetConflictInfo> properties,
			@NotNull String tableName)
	{
		IPropertyGroup tableModelGrp = PropertyFactory.createPropertyGroup(tableName);
		tableModelGrp.setAttribute(IPropertyAttributes.INHERIT_VALIDITY, Boolean.TRUE);
		String[] columns = new String[]{NAME_COLUMN, FROM_COLUMN, TO_COLUMN, RESULT_COLUMN};
		tableModelGrp.setColumns(columns.length);
		tableModelGrp.setAttribute(IPropertyAttributes.GROUP_LABEL, columns);
		for (IFacetConflictInfo value : properties) {
			tableModelGrp.addProperty(getPropertyGroupForConflictFacet(node, value));
		}
		tableModelGrp.setAttribute(IPropertyAttributes.TABLE_SORT_COLUMN_INDEX, 0);
		TableBuilder tableBuilder = new TableBuilder();
		PropertyTable table = tableBuilder.buildTable(tableModelGrp, false);
		// Give editors focus when activated.
		table.setSurrendersFocusOnKeystroke(true);
		table.setRowHeight(ROW_HEIGHT);
		table.getTableHeader().setReorderingAllowed(false);
		return table;
	}

	@SuppressWarnings("OverlyLongMethod")
	@NotNull private IProperty getPropertyGroupForConflictFacet(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info)
	{
		IPropertyGroup pg = PropertyFactory.createPropertyGroup(info.getName()); //for uniquenes.

		ValueTypeEnum sourceType = info.getSourceType();
		PropertyTypeValue valType = AttributePropertyFactory.getPropertyType(sourceType);
		IProperty sourceCol = PropertyFactory.createProperty("conflict.row.source", valType);
		sourceCol.setLabel(info.getDisplayName());
		String sourceValue = info.getSourceValue();
		sourceCol.setObject(sourceValue);
		sourceCol.setToolTipText(getFacetValueTooltip(sourceType, sourceValue));
		sourceCol.setEditable(false);
		sourceCol.setInheritEnabled(false);
		pg.addProperty(sourceCol);

		ValueTypeEnum targetType = info.getTargetType();
		valType = AttributePropertyFactory.getPropertyType(targetType);
		IProperty destCol = PropertyFactory.createProperty("conflict.row.dest", valType);
		String targetValue = info.getTargetValue();
		destCol.setObject(targetValue);
		destCol.setToolTipText(getFacetValueTooltip(targetType, targetValue));
		destCol.setEditable(false);
		destCol.setInheritEnabled(false);
		pg.addProperty(destCol);

		String result = info.getResult();
		ValueTypeEnum resultType = info.getResultType();
		valType = AttributePropertyFactory.getPropertyType(resultType);

		final IProperty resultCol;
		if (PropertyTypeValue.BOOLEAN.equals(valType)) {
			resultCol = PropertyFactory.createProperty("conflict.row.result", valType);
			resultCol.setObject(Boolean.valueOf(result));
			resultCol.setToolTipText(getResultTooltip(resultType, result));
			resultCol.addPropertyChangeListener((p) -> {
				//do nothing if no change otherwise might endup in infinite loop?
				if (!p.getOldValue().toString().equals(p.getNewValue().toString())) {
					info.setResult(p.getNewValue().toString());
					ValueTypeEnum newResultType = info.getResultType();
					String newResult = info.getResult();
					resultCol.setToolTipText(getResultTooltip(newResultType, newResult));
					setUserChoiceForRelatedItems(node, info, info.getUserChoice());
				}
			});
		}
		else {
			resultCol = PropertyFactory.createActionProperty("conflict.row.result");
			resultCol.setToolTipText(getResultTooltip(resultType, result));
			resultCol.setAttribute(IPropertyAttributes.BUTTON_BORDER, true);
			resultCol.setAttribute(IPropertyAttributes.UPDATE_BUTTON_LABEL, true);
			resultCol.setBorder(BorderValue.SIMPLE);
			resultCol.setLabel(result);
			resultCol.setActionListener((a) -> {
				info.setUserChoice(info.getUserChoice().toggle());
				//label must be changed before firing the property change event.
				ValueTypeEnum newResultType = info.getResultType();
				String newResult = info.getResult();
				resultCol.setLabel(newResult);
				resultCol.setToolTipText(getResultTooltip(newResultType, newResult));
				resultCol.touchProperty();
				setUserChoiceForRelatedItems(node, info, info.getUserChoice());
			});
		}
		resultCol.setHorizontalJustification(HorizontalValue.CENTER);
		pg.addProperty(resultCol);
		m_facetPanels.put(info, resultCol);
		resultCol.setMouseListener(m_resultButtonGuide);
		if (hasChangePolicyViolation(node, info)) {
			resultCol.setEnabled(false);
			resultCol.setInheritEnabled(false);
			resultCol.setToolTipText(ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.column.changepolicyviolation.tooltip"));
		}
		return pg;
	}

	private String getResultTooltip(@NotNull ValueTypeEnum resultType, String result)
	{
		return ResourceMgr.getString(FacetConflictResolutionPanel.class,
				"FacetConflictResolutionPanel.column.result.tooltip", getValueTooltipString(resultType, result));
	}

	private String getFacetValueTooltip(@NotNull ValueTypeEnum valType, String val)
	{
		return "<html>" + getValueTooltipString(valType, val) + "</html>";
	}

	protected final String getValueTooltipString(@NotNull ValueTypeEnum valType, String val)
	{
		String toolTipVal = val;
		if (StringUtils.isBlank(val)) {
			if (ValueTypeEnum.TypeDouble.equals(valType)) {
				toolTipVal = "0.0";
			}
			else if (ValueTypeEnum.TypeInteger.equals(valType)) {
				toolTipVal = "0";
			}
		}
		return HTMLHelper.bold(valType.getDisplayName() + ": ") + StringUtils.quote(toolTipVal);
	}

	private void setUserChoiceForRelatedItems(@NotNull IFacetConflictNode node, @NotNull IFacetConflictInfo info,
			@NotNull ValueOption option)
	{
		for (IFacetConflictInfo relatedFacet : m_model.getRelatedFacets(node, info)) {
			IProperty panel = m_facetPanels.get(relatedFacet);
			if (panel != null) {
				propagateUserChoice(relatedFacet, panel, option);
			}
		}
		reDrawWindow();
	}

	private void propagateUserChoice(@NotNull IFacetConflictInfo relatedFacet, @NotNull IProperty panel,
			@NotNull ValueOption option)
	{
		relatedFacet.setUserChoice(option);
		String result = relatedFacet.getResult();
		if (ValueTypeEnum.TypeBoolean.equals(relatedFacet.getResultType())) {
			panel.setObject(Boolean.valueOf(result));
			panel.setToolTipText(getResultTooltip(relatedFacet.getResultType(), result));
		}
		else {
			panel.setLabel(result);
			panel.setToolTipText(getResultTooltip(relatedFacet.getResultType(), result));
			panel.touchProperty();
		}
	}

	private boolean hasChangePolicyViolation(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info)
	{
		if (info.isAttribute()) {
			return !allowAttributeChange(node, info);
		}
		else {
			return !allowPropertyChange(node, info);
		}
	}

	protected boolean allowPropertyChange(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info)
	{
		return IChangePolicyMgr.Statics.allowsPropertyChange(node.getNodeObject(), info.getName());
	}

	protected boolean allowAttributeChange(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info)
	{
		return IChangePolicyMgr.Statics.allowsAttributeChange(node.getNodeObject(), info.getName());
	}

	private class FacetSelectResultFrom extends JPanel
	{

		private final JButton srcButton;
		private final JButton targetButton;

		private FacetSelectResultFrom(@NotNull Consumer<ValueOption> actionListener)
		{
			setLayout(new GridBagLayout());
			JPanel container = new JPanel(new GridBagLayout());
			String border = ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.chooseresult.label");
			TitledBorder titledBorder = BorderFactory.createTitledBorder(border);
			setBorder(titledBorder);
			setToolTipText(ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.chooseresult.tooltip"));

			srcButton = BasicUIFactory.getInstance().createSiemensCustomJButton(getSourceLabel(""));
			String name = "selectResultFromForNode";
			srcButton.setName(name + ".source");

			srcButton.setToolTipText(getSourceTooltip(""));

			GridBagConstraints gbc = getDefaultGridBagConstraints();

			JPanel srcPanel = new JPanel(new BorderLayout());
			String srcBorder = ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.chooseresult.source");
			srcPanel.setBorder(BorderFactory.createTitledBorder(srcBorder));
			srcPanel.add(srcButton);
			container.add(srcPanel, gbc);

			targetButton = BasicUIFactory.getInstance().createSiemensCustomJButton(getTargetLabel(""));
			targetButton.setName(name + ".target");

			targetButton.setToolTipText(getTargetTooltip(""));

			srcButton.setMnemonic(KeyEvent.VK_S);
			targetButton.setMnemonic(KeyEvent.VK_T);

			gbc = getDefaultGridBagConstraints();
			gbc.gridy = 1;
			JPanel targetPanel = new JPanel(new BorderLayout());
			targetPanel.add(targetButton);
			String targetBorder = ResourceMgr.getStringForLabel(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.chooseresult.target");
			targetPanel.setBorder(BorderFactory.createTitledBorder(targetBorder));
			container.add(targetPanel, gbc);

			markBold(targetButton);

			srcButton.addActionListener((a) -> {
				markBold(srcButton);
				markNonBold(targetButton);
				actionListener.accept(ValueOption.Source);
			});

			targetButton.addActionListener((a) -> {
				markBold(targetButton);
				markNonBold(srcButton);
				actionListener.accept(ValueOption.Target);
			});

			gbc = getDefaultGridBagConstraints();
			add(container, gbc);

			srcButton.addMouseListener(m_sourceButtonGuide);
			targetButton.addMouseListener(m_targetButtonGuide);
		}

		@NotNull private GridBagConstraints getDefaultGridBagConstraints()
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			return gbc;
		}

		private void markBold(@NotNull JButton button)
		{
			//LOGIC-8148:The highlight of button is confusing and conflicting UX.
			button.setFont(button.getFont().deriveFont(Font.PLAIN));
		}

		private void markNonBold(@NotNull JButton button)
		{
			button.setFont(button.getFont().deriveFont(Font.PLAIN));
		}

		private void updateChooserButtons()
		{
			srcButton.setText(getSourceLabel(getSourceObjectName(true)));
			targetButton.setText(getTargetLabel(getTargetObjectName(true)));

			srcButton.setToolTipText(getSourceTooltip(getSourceObjectName(false)));
			targetButton.setToolTipText(getTargetTooltip(getTargetObjectName(false)));
		}

		private void selectionChanged(@NotNull Set<IFacetConflictNode> selectedNodes)
		{
			boolean enabled = !selectedNodes.isEmpty();
			srcButton.setEnabled(enabled);
			targetButton.setEnabled(enabled);

			if (enabled) {
				Optional<IFacetConflictNode> violations =
						selectedNodes.stream().filter(node -> hasChangePolicyViolations(node)).findFirst();
				if (violations.isPresent()) {
					srcButton.setEnabled(false);
					srcButton.setToolTipText(ResourceMgr.getString(FacetConflictResolutionPanel.class,
							"FacetConflictResolutionPanel.source.changepolicyviolation.tooltip"));
				}
			}
		}

		private boolean hasChangePolicyViolations(@NotNull IFacetConflictNode node)
		{
			for (IFacetConflictInfo info : node.getConflicts()) {
				if (hasChangePolicyViolation(node, info)) {
					return true;
				}
			}
			return false;
		}

		private String getTargetLabel(@NotNull String targetName)
		{
			return ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.choose.target.label", targetName);
		}

		private String getSourceLabel(@NotNull String sourceName)
		{
			return ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.choose.source.label", sourceName);
		}

		private String getTargetTooltip(@NotNull String targetName)
		{
			return ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.choose.target.tooltip", targetName);
		}

		private String getSourceTooltip(@NotNull String sourceName)
		{
			return ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.choose.source.tooltip", sourceName);
		}
	}

	private void reDrawWindow()
	{
		if (m_owner != null) {
			m_owner.validate();
			m_owner.repaint();
		}
	}

	private void clearAll()
	{
		clearBrowser();
		clearFacetPanel();
	}

	private void clearBrowser()
	{
		m_objectBrowser.removeAll();
		m_tree = null;
	}

	private void clearFacetPanel()
	{
		m_attributeConflicts.removeAll();
		m_propertyConflicts.removeAll();
		m_facetPanels.clear();
	}

	@Override public void targetChanged(@Nullable T target)
	{
		clearAll();

		Set<IFacetConflictNode> topNodes = m_model.getTopNodes();
		if (target == null || topNodes.isEmpty()) {
			addNoConflictLabel(m_objectBrowser);
			addNoConflictLabel(m_attributeConflicts);
			addNoConflictLabel(m_propertyConflicts);
			reDrawWindow();
			return;
		}

		ConflictTreeFolderNode rootNode = new ConflictTreeFolderNode();
		ConflictTree conflictTree = new ConflictTree(rootNode);
		m_tree = conflictTree;
		conflictTree.addMouseListener(m_treePaneGuide);
		m_objectBrowser.add(conflictTree.buildContentPanel(null));

		conflictTree.addTreeSelectionListener(this::treeNodeSelected);
		for (IFacetConflictNode facetConflictNode : topNodes) {
			buildTree(rootNode, facetConflictNode);
		}
		if (rootNode.getChildCount() > 0) {
			TreePath rootPath = new TreePath(rootNode);
			conflictTree.expandPath(rootPath);
			//There will be no selection initially in the tree and
			//thus buttons will be disabled and user would feel to
			//click on the tree node and button will be enabled and
			//thus he will be tempted to use those buttons.
			//TreeNode selectedNode = rootNode.getChildAt(0);
			//conflictTree.setSelectionPath(new TreePath(new TreeNode[]{rootNode, selectedNode}));
		}

		Set<IFacetConflictNode> selectedNodes = getSelectedNodes();
		treeNodeSelectionChanged(selectedNodes);

		m_resultChooser.updateChooserButtons();

		reDrawWindow();
	}

	private void treeNodeSelectionChanged(@NotNull Set<IFacetConflictNode> selectedNodes)
	{
		int sectionCount = selectedNodes.size();
		boolean enableGuidance = sectionCount != 1;
		m_guidanceLabel.setVisible(enableGuidance);
		m_conflictSplit.setVisible(!enableGuidance);
		if (sectionCount < 1) {
			m_guidanceLabel.setText(ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.guidance.noSelection"));
		}
		else if (sectionCount == 1) {
			m_guidanceLabel.setText(StringUtils.SPACE);
		}
		else {
			m_guidanceLabel.setText(ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.guidance.multipleSelection"));
		}
		m_resultChooser.selectionChanged(selectedNodes);
	}

	private void buildTree(@NotNull ConflictTreeNode rootNode, @NotNull IFacetConflictNode facetConflictNode)
	{
		ConflictTreeObjectNode newChild = new ConflictTreeObjectNode(facetConflictNode);
		rootNode.add(newChild);
		for (IFacetConflictNode conflictNode : m_model.getChildNodes(facetConflictNode)) {
			buildTree(newChild, conflictNode);
		}
	}

	protected static class ConflictTreeFolderNode extends ConflictTreeNode
	{

		private static final Icon FolderIcon = CHSImageLoader.loadImageIcon(CHSImages.FOLDER_ICON_ENABLED);

		public ConflictTreeFolderNode()
		{
			super(ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.tree.rootnode.name"));
		}

		@Nullable public Icon getIcon()
		{
			return FolderIcon;
		}
	}

	private static class ConflictTreeObjectNode extends ConflictTreeNode
	{

		private ConflictTreeObjectNode(@NotNull IFacetConflictNode facetConflictNode)
		{
			super(facetConflictNode);
		}

		@Nullable public Icon getIcon()
		{
			return IconUtils.getIcon(((IFacetConflictNode) getUserObject()).getNodeObject());
		}

		public String toString()
		{
			return ((IFacetConflictNode) getUserObject()).getTargetName();
		}

		@Nullable public String getTooltipText()
		{
			return ResourceMgr.getString(FacetConflictResolutionPanel.class,
					"FacetConflictResolutionPanel.tree.node.tooltip",
					((IFacetConflictNode) getUserObject()).getSourceName());
		}
	}

	private abstract static class ConflictTreeNode extends DefaultMutableTreeNode
	{

		private ConflictTreeNode(Object userObject)
		{
			super(userObject);
		}

		@Nullable public abstract Icon getIcon();

		@Nullable public String getTooltipText()
		{
			return null;
		}
	}

	protected static class ConflictTree extends AllNodeFilterableTree
	{

		public ConflictTree(@NotNull ConflictTreeFolderNode rootNode)
		{
			super(rootNode);
			setName("Facet.Conflict.Resolution.Tree");
			setCellRenderer(new ConflictTreeCellRenderer());
			setSearchFilter(new BasicDisplayNameTreeSearchFilter());
			setShowsRootHandles(true);
			expandRow(0);
			setRootVisible(false);
			addMouseListener(new RightClickExpandCollapseMouseListener(this));
			ToolTipManager.sharedInstance().registerComponent(this);
		}
	}

	private static class ConflictTreeCellRenderer extends DefaultTreeCellRenderer
	{

		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocusT)
		{
			ConflictTreeNode node = (ConflictTreeNode) value;
			Icon openStateIcon = getOpenIcon();
			Icon closedStateIcon = getClosedIcon();
			Icon nodeIcon = node.getIcon();
			if (nodeIcon == null) {
				nodeIcon = expanded ? openStateIcon : closedStateIcon;
			}
			setLeafIcon(nodeIcon);
			setOpenIcon(nodeIcon);
			setClosedIcon(nodeIcon);
			setToolTipText(node.getTooltipText());
			return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocusT);
		}
	}

	private class StatusGuideHandler extends MouseAdapter
	{

		@NotNull private final Supplier<String> m_status;

		private StatusGuideHandler(@NotNull Supplier<String> status)
		{
			m_status = status;
		}

		@Override public void mouseEntered(MouseEvent e)
		{
			m_statusLabel.setText(m_status.get());
			super.mouseExited(e);
		}

		@Override public void mouseExited(MouseEvent e)
		{
			m_statusLabel.setText(StringUtils.SPACE);
			super.mouseMoved(e);
		}
	}
}
