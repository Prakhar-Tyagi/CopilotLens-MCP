/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IEquivalenceSet;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedUsageDesign;
import chs.cof.logical.shared.ISharedUsageDesignIterator;
import chs.cof.logical.shared.ISharedUsageDiagram;
import chs.cof.logical.shared.ISharedUsageDiagramIterator;
import chs.cof.logical.shared.ISharedUsageDiagramObject;
import chs.cof.logical.shared.ISharedUsageDiagramObjectIterator;
import chs.cof.logical.shared.ISharedUsageInfo;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import chs.common.IDesignDescriptor;
import chs.common.IDesignMgr;
import chs.common.IUID;
import chs.common.UIDConvertIterator;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ListMap;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.SortedTreeNode;
import chs.utility.helpers.PortedConductorRepsHelper;
import chs.utility.helpers.revisioning.RevisionHelper;
import chs.utility.ui.IconUtils;

import javax.swing.Icon;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Matt Boyd
 */
public class EditPortTreeModel implements TreeModel
{

	public static final String WIRE_TYPE =
			ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.WiresNode.text");
	public static final String NET_TYPE =
			ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.NetsNode.text");
	public static final String SHIELD_TYPE =
			ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.ShieldsNode.text");

	private static final Comparator m_textComparator = new TextTreeNodeComparator();

	private DefaultTreeModel m_delegate = null;

	private EditPortTreeNode m_root = null;
	private ProjectTreeNode m_projectNode = null;
	private ConductorContainerTreeNode m_designNode = null;

	private Icon m_wireIcon = null;
	private Icon m_netIcon = null;
	private Icon m_shieldIcon = null;
	private Icon m_diagramIcon = null;
	private Icon m_designIcon = null;
	private Icon m_latestDesignIcon = null;
	private Icon m_projectIcon = null;
	private Icon m_folderIcon = null;
	private Icon m_designAbstractionIcon = null;

	/**
	 * Maps {@link IUID uids} of shared conductors to their {@link EditPortTreeNode} nodes.
	 */
	private Map m_nodeMap;
	private Map m_equivNodeMap;

	public EditPortTreeModel(IProject project, IDesign design)
	{
		m_diagramIcon = CHSImageLoader.loadImageIcon(CHSImages.DIAGRAM_ICON_ENABLED);
		m_designIcon = CHSImageLoader.loadImageIcon(CHSImages.DESIGN_ICON_ENABLED);
		m_latestDesignIcon = CHSImageLoader.loadImageIcon(CHSImages.LATEST_DESIGN_ICON_ENABLED);
		m_projectIcon = CHSImageLoader.loadImageIcon(CHSImages.PROJECT_ICON_ENABLED);
		m_folderIcon = CHSImageLoader.loadImageIcon(CHSImages.FOLDER_ICON_ENABLED);
		m_designAbstractionIcon = CHSImageLoader.loadImageIcon(CHSImages.DESIGN_ABSTRACTION_ICON_ENABLED);
		m_nodeMap = new HashMap();
		m_equivNodeMap = new HashMap();

		buildTreeModel(project, (ILogicDesign) design);
	}

	private TreeModel buildTreeModel(IProject project, ILogicDesign design)
	{
		Comparator comparator = new OrderedTreeNodeComparator();
		m_root = new EditPortTreeNode(ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.RootNode.text"),
				m_folderIcon, false, true);
		m_delegate = new DefaultTreeModel(m_root);
		m_root.setComparator(comparator);

		m_projectNode = new ProjectTreeNode(project);

		ISharedConductorMgr condMgr = project.getSharedConductorMgr();
		// be smart about refreshing, it can be costly - but w DO need to make sure tyhe shared conductors are also refreshed.
		if (condMgr != null) {
			condMgr.refresh();
			for (ISharedConductorIterator sharedConds = condMgr.getSharedConductors(); sharedConds.hasNext(); ) {
				ISharedConductor sharedCond = sharedConds.getNext();
				// Adds the node to the proper parent
				// just default it to wire, seems as good as anything
				Icon icon = IconUtils.getIcon(sharedCond);

				SharedConductorTreeNode node =
						new SharedConductorTreeNode(sharedCond, icon, true, false, project, design);
				node.setComparator(m_textComparator);
				m_projectNode.addSharedConductorTreeNode(node);
			}
		}
		m_root.add(m_projectNode);
		// determine if this design is the latest, and display the correct icon...
		IDesignMgr designMgr = project.getDesignMgr();
		boolean bIsLatest = designMgr.isLatestRevision((IDesignDescriptor) design);
		m_designNode = new ConductorContainerTreeNode(design.getName(),
				((bIsLatest) ? m_latestDesignIcon : m_designIcon), 1, false);
		m_designNode.setComparator(comparator);

		final IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		// Adds the design ports to the tree model
		for (IConductorIterator conds = connectivity.getConductors(); conds.hasNext(); ) {
			IConductor cond = conds.getNext();
			ISharedConductor sharedCond = cond.getSharedConductor();
			if (sharedCond != null) {
				continue;
			}
			PortTreeNode node = new PortTreeNode(cond, IconUtils.getIcon(cond), false);
			node.setComparator(m_textComparator);
			buildPortNodeContents(node, design);
			m_designNode.addConductorTreeNode(node);
		}
		m_root.add(m_designNode);
		m_delegate.setAsksAllowsChildren(false);

		return m_delegate;
	}

	public void share(PortTreeNode node)
	{
		m_designNode.removeConductorTreeNode(node);
		node.setProjectLevel(true);
		m_projectNode.addConductorTreeNode(node);
		m_delegate.reload();
	}

	public void share(SharedConductorTreeNode node)
	{
		m_designNode.removeSharedConductorTreeNode(node);
		node.setProjectLevel(true);
		m_projectNode.addSharedConductorTreeNode(node);
		m_delegate.reload();
	}

	public void unshare(PortTreeNode node)
	{
		m_projectNode.removeConductorTreeNode(node);
		node.setProjectLevel(false);
		m_designNode.addConductorTreeNode(node);
		m_delegate.reload();
	}

	public void unshare(SharedConductorTreeNode node)
	{
		m_projectNode.removeSharedConductorTreeNode(node);
		node.setProjectLevel(false);
		m_designNode.addSharedConductorTreeNode(node);
		m_delegate.reload();
	}

	private void buildPortNodeContents(PortTreeNode node, ILogicDesign design)
	{
		ListMap portedCondsMap = PortedConductorRepsHelper.getPortedConductors(design);
		IConductor targetCond = node.getConductor();
		List<IUID> schemConds = new ArrayList<IUID>();
		node.setProjectLevel(false);
		for (Iterator diagrams = portedCondsMap.keySet().iterator(); diagrams.hasNext(); ) {
			ISchemDiagram diagram = (ISchemDiagram) diagrams.next();
			List portedConds = portedCondsMap.getList(diagram);
			schemConds.clear();
			for (Iterator conds = portedConds.iterator(); conds.hasNext(); ) {
				chs.cof.logical.schem.IConductor cond = (chs.cof.logical.schem.IConductor) conds.next();
				if (cond.getConnectivity() == targetCond) {
					schemConds.add(cond.getUID());
				}
			}

			if (!schemConds.isEmpty()) {
				DiagramTreeNode diagramNode =
						new DiagramTreeNode(diagram.getName(), diagram.getUID(), schemConds, false);
				node.add(diagramNode);
			}

			node.addDiagramConductors(diagram, schemConds);
		}
		node.setProjectLevel(true);
		EditPortTreeNode designNode = new EditPortTreeNode(design.getName(), m_designIcon, true);
		designNode.setComparator(m_textComparator);
		for (Iterator diagrams = portedCondsMap.keySet().iterator(); diagrams.hasNext(); ) {
			ISchemDiagram diagram = (ISchemDiagram) diagrams.next();
			List portedConds = portedCondsMap.getList(diagram);
			schemConds.clear();
			for (Iterator conds = portedConds.iterator(); conds.hasNext(); ) {
				chs.cof.logical.schem.IConductor cond = (chs.cof.logical.schem.IConductor) conds.next();
				if (cond.getConnectivity() == targetCond) {
					schemConds.add(cond.getUID());
				}
			}

			if (!schemConds.isEmpty()) {
				DiagramTreeNode diagramNode =
						new DiagramTreeNode(diagram.getName(), diagram.getUID(), schemConds, false);
				designNode.add(diagramNode);
			}
		}
		node.add(designNode);
		node.setProjectLevel(false);
	}

	private void buildSharedConductorNodeContents(SharedConductorTreeNode node, IDesign activeDesign,
			ISharedUsageInfo usageInfo)
	{
		node.setProjectLevel(true);

		Set seenSCs = new HashSet();
		for (ISharedUsageDesignIterator designUses = usageInfo.getUsedDesigns(); designUses.hasNext(); ) {
			ISharedUsageDesign designUsage = designUses.getNext();

			EditPortTreeNode designNode = new EditPortTreeNode(designUsage.getName(), m_designIcon, true);
			designNode.setComparator(m_textComparator);

			boolean hasDiagrams = false;
			for (ISharedUsageDiagramIterator diagramUses = designUsage.getUsedDiagrams(); diagramUses.hasNext(); ) {
				ISharedUsageDiagram diagramUsage = diagramUses.getNext();

				List schemConds = new ArrayList();
				for (ISharedUsageDiagramObjectIterator diagObjs = diagramUsage.getUsedDiagramObjects();
						diagObjs.hasNext(); ) {
					ISharedUsageDiagramObject diagObj = diagObjs.getNext();

					schemConds.add(diagObj.getUID());
				}
				if (!schemConds.isEmpty()) {
					DiagramTreeNode diagramNode =
							new DiagramTreeNode(diagramUsage.getName(), diagramUsage.getUID(), schemConds, true);
					designNode.add(diagramNode);
					hasDiagrams = true;
				}
				node.addDiagramConductors(diagramUsage.getUID(), schemConds);
			}
			if (hasDiagrams) {
				node.add(designNode);
			}
			ISharedConductor sharedCond = (ISharedConductor) usageInfo.getSharedObject();
			ISharedConductorMgr sharedCondMgr = activeDesign.getProject().getSharedConductorMgr();
			if (sharedCond.isEquivalenceRepresentative()) {
				Icon icon = IconUtils.getIcon(sharedCond);
				EditPortTreeNode equivFolderNode = new EditPortTreeNode(
						ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.EquivFolder.text"),
						m_folderIcon, true);
				IEquivalenceSet equivSet = sharedCond.getEquivalenceSet();
				for (Iterator scs = equivSet.getSharedConductors().iterator(); scs.hasNext(); ) {
					ISharedConductor sc = (ISharedConductor) scs.next();
					if (sc.getUID().equals(sharedCond.getUID())) {
						continue;
					}
					if (seenSCs.contains(sc.getUID().getString())) {
						continue;
					}
					seenSCs.add(sc.getUID().getString());
					EditPortTreeNode equivNode = new EditPortTreeNode(sc.getName(), icon, true);
					equivFolderNode.add(equivNode);
					m_equivNodeMap.put(sc.getUID(), equivNode);
				}
				if (equivFolderNode.getChildCount() != 0) {
					node.add(equivFolderNode);
				}
			}
		}
		node.setProjectLevel(false);
		for (ISharedUsageDesignIterator designUses = usageInfo.getUsedDesigns(); designUses.hasNext(); ) {
			ISharedUsageDesign designUsage = designUses.getNext();
			if (activeDesign.getUID().isEquiv(designUsage.getUID())) {
				for (ISharedUsageDiagramIterator diagramUses = designUsage.getUsedDiagrams(); diagramUses.hasNext(); ) {
					ISharedUsageDiagram diagramUsage = diagramUses.getNext();

					List schemConds = new ArrayList();
					for (ISharedUsageDiagramObjectIterator diagObjs = diagramUsage.getUsedDiagramObjects();
							diagObjs.hasNext(); ) {
						ISharedUsageDiagramObject diagObj = diagObjs.getNext();

						schemConds.add(diagObj.getUID());
					}
					if (!schemConds.isEmpty()) {
						DiagramTreeNode diagramNode =
								new DiagramTreeNode(diagramUsage.getName(), diagramUsage.getUID(), schemConds, true);
						node.add(diagramNode);
					}
					node.addDiagramConductors(diagramUsage.getUID(), schemConds);
				}
			}
			else if (designUsage.countUsages() > 0) {
				node.setUsedInAnotherDesign(true);
			}
		}
		node.setProjectLevel(true);
	}

	class UIDNamePair
	{

		public IUID uid;
		public String name;

		public UIDNamePair(IUID u, String n)
		{
			uid = u;
			name = n;
		}
	}

	/**
	 * @param l
	 */
	public void addTreeModelListener(TreeModelListener l)
	{
		m_delegate.addTreeModelListener(l);
	}

	/**
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj)
	{
		return m_delegate.equals(obj);
	}

	/**
	 * @param parent
	 * @param index
	 * @return
	 */
	public Object getChild(Object parent, int index)
	{
		return m_delegate.getChild(parent, index);
	}

	/**
	 * @param parent
	 * @return
	 */
	public int getChildCount(Object parent)
	{
		return m_delegate.getChildCount(parent);
	}

	/**
	 * @param parent
	 * @param child
	 * @return
	 */
	public int getIndexOfChild(Object parent, Object child)
	{
		return m_delegate.getIndexOfChild(parent, child);
	}

	/**
	 * @return
	 */
	public Object getRoot()
	{
		return m_delegate.getRoot();
	}

	/**
	 * @see Object#hashCode()
	 */
	public int hashCode()
	{
		return m_delegate.hashCode();
	}

	/**
	 * @param node
	 * @return
	 */
	public boolean isLeaf(Object node)
	{
		return m_delegate.isLeaf(node);
	}

	/**
	 * @param l
	 */
	public void removeTreeModelListener(TreeModelListener l)
	{
		m_delegate.removeTreeModelListener(l);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString()
	{
		return m_delegate.toString();
	}

	/**
	 * @param path
	 * @param newValue
	 */
	public void valueForPathChanged(TreePath path, Object newValue)
	{
		m_delegate.valueForPathChanged(path, newValue);
	}

	public class EditPortTreeNode extends SortedTreeNode implements SimpleTreeNode
	{

		private String m_text = null;
		private Icon m_icon = null;
		private Icon m_disabledIcon = null;
		private boolean m_enabled = false;
		private boolean m_expanded = false;
		private boolean m_editable = false;
		private boolean m_projectLevel = false;
		private List m_projectLevelChildren;
		private List m_designLevelChildren;
		private boolean m_isLeaf = true;
		private boolean m_expState = false;

		public EditPortTreeNode(boolean projectLevel)
		{
			this(null, null, projectLevel);
		}

		public EditPortTreeNode(String text, boolean projectLevel)
		{
			this(text, null, projectLevel);
		}

		public EditPortTreeNode(String text, Icon icon, boolean projectLevel)
		{
			this(text, icon, projectLevel, true);
		}

		public EditPortTreeNode(String text, Icon icon, boolean projectLevel, boolean isLeaf)
		{
			m_text = text;
			m_icon = icon;
			m_projectLevel = projectLevel;
			m_isLeaf = isLeaf;
			m_projectLevelChildren = new ArrayList();
			m_designLevelChildren = new ArrayList();
		}

		private void init()
		{
			m_icon = m_folderIcon;
		}

		public String getText()
		{
			return m_text;
		}

		protected void setText(String text)
		{
			m_text = text;
		}

		public String getSuffixText()
		{
			return "";
		}

		public boolean isInvalid()
		{
			return false;
		}

		public Icon getIcon()
		{
			return m_icon;
		}

		protected void setIcon(Icon icon)
		{
			m_icon = icon;
		}

		public Icon getDisabledIcon()
		{
			return m_disabledIcon;
		}

		protected void setDisabledIcon(Icon icon)
		{
			m_disabledIcon = icon;
		}

		public String toString()
		{
			return getText();
		}

		public boolean isEditable()
		{
			return m_editable;
		}

		public void setEditable(boolean editable)
		{
			m_editable = editable;
		}

		public void setExpanded(boolean expanded)
		{
			m_expanded = expanded;
		}

		public void setEnabled(boolean enabled)
		{
			m_enabled = enabled;
		}

		public boolean isProjectLevel()
		{
			return m_projectLevel;
		}

		public void setProjectLevel(boolean projectLevel)
		{
			if (m_projectLevel != projectLevel) {
				m_projectLevel = projectLevel;
				// Switches between the sets of children
				removeAllChildren();
				if (m_projectLevel) {
					for (Iterator nodes = m_projectLevelChildren.iterator(); nodes.hasNext(); ) {
						MutableTreeNode node = (MutableTreeNode) nodes.next();
						super.add(node);
					}
				}
				else {
					for (Iterator nodes = m_designLevelChildren.iterator(); nodes.hasNext(); ) {
						MutableTreeNode node = (MutableTreeNode) nodes.next();
						super.add(node);
					}
				}
			}
		}

		/**
		 * @see DefaultMutableTreeNode#isLeaf()
		 */
		public boolean isLeaf()
		{
			if (!m_isLeaf) {
				return !m_isLeaf;
			}
			return super.isLeaf();
		}

		/**
		 * @return
		 */
		public boolean isExpState()
		{
			return m_expState;
		}

		/**
		 * @param b
		 */
		public void setExpState(boolean b)
		{
			m_expState = b;
		}

		/**
		 * @see SortedTreeNode#add(MutableTreeNode)
		 */
		public void add(MutableTreeNode node)
		{
			super.add(node);
			if (m_projectLevel) {
				m_projectLevelChildren.add(node);
			}
			else {
				m_designLevelChildren.add(node);
			}
		}

		/**
		 * @see DefaultMutableTreeNode#remove(MutableTreeNode)
		 */
		public void remove(MutableTreeNode node)
		{
			super.remove(node);
			if (m_projectLevel) {
				m_projectLevelChildren.remove(node);
			}
			else {
				m_designLevelChildren.remove(node);
			}
		}
	}

	public class OrderedTreeNode extends EditPortTreeNode
	{

		private String m_name = null;
		private int m_sortValue = 0;

		public OrderedTreeNode(String name, int sort, boolean projectLevel)
		{
			super(projectLevel);
			setText(name);
			setIcon(m_folderIcon);
			m_sortValue = sort;
		}

		public OrderedTreeNode(String name, Icon icon, int sort, boolean projectLevel)
		{
			super(name, icon, projectLevel);
			m_sortValue = sort;
		}

		public int getSortValue()
		{
			return m_sortValue;
		}

		public String toString()
		{
			return m_name;
		}

		/**
		 * Removes this node if its becomes empty.
		 */
		public void remove(int childIndex)
		{
			super.remove(childIndex);
		}
	}

	public static class OrderedTreeNodeComparator implements Comparator
	{

		public int compare(Object o1, Object o2)
		{
			OrderedTreeNode n1 = (OrderedTreeNode) o1;
			OrderedTreeNode n2 = (OrderedTreeNode) o2;
			int v1 = n1.getSortValue();
			int v2 = n2.getSortValue();
			return v1 - v2;
		}
	}

	public class PortTreeNode extends EditPortTreeNode
	{

		private IUID m_conductor = null;
		private ListMap m_diagramConductors = null;
		private String m_changedName = null;

		public PortTreeNode(IConductor conductor, Icon icon, boolean projectLevel)
		{
			super(projectLevel);
			m_conductor = conductor.getUID();
			m_diagramConductors = new ListMap();
			setUserObject(conductor);
			setIcon(icon);
			setEditable(true);
			m_nodeMap.put(conductor.getUID(), this);
		}

		public IConductor getConductor()
		{
			return (IConductor) UIDMgr.getObject(m_conductor);
		}

		public String getText()
		{
			if (m_changedName == null) {
				return getConductor().getName();
			}
			else {
				return m_changedName;
			}
		}

		public void addDiagramConductors(ISchemDiagram diagram, List conds)
		{
			m_diagramConductors.getList(diagram).addAll(conds);
		}

		public Iterator getDiagrams()
		{
			return m_diagramConductors.keySet().iterator();
		}

		public List getDiagramConductors(ISchemDiagram diagram)
		{
			return m_diagramConductors.getList(diagram);
		}

		public String toString()
		{
			return getConductor().getName();
		}

		public String getChangedName()
		{
			return m_changedName;
		}

		public void setChangedName(String string)
		{
			m_changedName = string;
		}

		public String getSuffixText()
		{
			if (isProjectLevel()) {
				// If this node has been shared, then fake the revision number.  We can be sure it will always be 1
				// at present, because we don't support share into, or provide a dialog to enter another number.
				return RevisionHelper.FULL_NAME_DELIMITER + '1';
			}
			else {
				return "";
			}
		}

		public Icon getIcon()
		{
			if (isProjectLevel()) {
				// If this node was shared within the dialog, then fake the icon type.  We are in disguise.. shhhh.
				return IconUtils.getSharedConductorIconForNonSharedConductor(UIDMgr.getObject(m_conductor).getClass(),
						IconUtils.ACTIVE);
			}
			else {
				return super.getIcon();
			}
		}
	}

	public class SharedConductorTreeNode extends EditPortTreeNode
	{

		private IUID m_conductor = null;
		private ListMap m_diagramConductors = null;
		private String m_changedName = null;
		/**
		 * Indicates that the shared conductor that this node represents is invalid.
		 */
		private boolean m_invalid = false;
		private boolean m_usedInAnotherDesign = false;
		private boolean m_loaded = false;
		private IProject m_project;
		private IDesign m_design;

		public SharedConductorTreeNode(ISharedConductor conductor, Icon icon, boolean projectLevel, boolean invalid,
				IProject project, IDesign design)
		{
			super(projectLevel);
			m_conductor = conductor.getUID();
			m_diagramConductors = new ListMap();
			setUserObject(conductor);
			setIcon(icon);
			setEditable(true);
			m_invalid = invalid;
			m_project = project;
			m_design = design;

			m_nodeMap.put(conductor.getUID(), this);
		}

		public void load()
		{
			if (!m_loaded) {
				m_loaded = true;

				ISharedUsageInfo info = m_project.getSharedUsageView().getSharedUsageInfo(getConductor());
				buildSharedConductorNodeContents(this, m_design, info);
			}
		}

		public boolean isLeaf()
		{
			if (!m_loaded) {
				return false;
			}
			return super.isLeaf();
		}

		public ISharedConductor getConductor()
		{
			return (ISharedConductor) UIDMgr.getObject(m_conductor);
		}

		public String getText()
		{
			if (m_changedName == null) {
				return getConductor().getName();
			}
			else {
				return m_changedName;
			}
		}

		/**
		 * @see EditPortTreeModel.EditPortTreeNode#getSuffixText()
		 */
		public String getSuffixText()
		{
			StringBuilder suffix = new StringBuilder(RevisionHelper.FULL_NAME_DELIMITER + getConductor().getRevision());
			IEquivalenceSet equivSet = getConductor().getEquivalenceSet();
			if (equivSet != null && !getConductor().isEquivalenceRepresentative()) {
				ISharedConductor rep = equivSet.getRepresentative();
				if (rep != null) {
					EditPortTreeNode node = (EditPortTreeNode) m_nodeMap.get(rep.getUID());
					suffix.append(" (");
					suffix.append(node.getText());
					suffix.append(RevisionHelper.FULL_NAME_DELIMITER);
					suffix.append(rep.getRevision());
					suffix.append(')');
				}
			}
			if (isInvalid() && isProjectLevel()) {
				suffix.append(" (");
				suffix.append(ResourceMgr.getString(EditPortTreeModel.class, "EditPortTreeModel.Suffix.Invalid.text"));
				suffix.append(')');
			}
			return suffix.toString();
		}

		public boolean isInvalid()
		{
			if (isProjectLevel()) {
				return m_invalid;
			}
			else {
				return true;
			}
		}

		public void addDiagramConductors(IUID diagramUID, List conds)
		{
			m_diagramConductors.getList(diagramUID).addAll(conds);
		}

		public Iterator getDiagrams()
		{
			return new UIDConvertIterator(m_diagramConductors.keySet());
		}

		public List getDiagramConductors(ISchemDiagram diagram)
		{
			return m_diagramConductors.getList(diagram.getUID());
		}

		public String toString()
		{
			return getText();
		}

		public String getChangedName()
		{
			return m_changedName;
		}

		public void setChangedName(String string)
		{
			m_changedName = string;
		}

		public boolean isUsedInAnotherDesign()
		{
			return m_usedInAnotherDesign;
		}

		public void setUsedInAnotherDesign(boolean usedInAnotherDesign)
		{
			m_usedInAnotherDesign = usedInAnotherDesign;
		}
	}

	public class ConductorContainerTreeNode extends OrderedTreeNode
	{

		private OrderedTreeNode m_wiresNode;
		private OrderedTreeNode m_netsNode;
		private OrderedTreeNode m_shieldsNode;

		public ConductorContainerTreeNode(String name, Icon icon, int sort, boolean projectLevel)
		{
			super(name, icon, sort, projectLevel);
			m_wiresNode = new OrderedTreeNode(WIRE_TYPE, 0, projectLevel);
			m_wiresNode.setComparator(m_textComparator);
			m_netsNode = new OrderedTreeNode(NET_TYPE, 1, projectLevel);
			m_netsNode.setComparator(m_textComparator);
			m_shieldsNode = new OrderedTreeNode(SHIELD_TYPE, 2, projectLevel);
			m_shieldsNode.setComparator(m_textComparator);
		}

		public OrderedTreeNode getNetsNode()
		{
			return m_netsNode;
		}

		public OrderedTreeNode getShieldsNode()
		{
			return m_shieldsNode;
		}

		public OrderedTreeNode getWiresNode()
		{
			return m_wiresNode;
		}

		public void addConductorTreeNode(PortTreeNode node)
		{
			if (node.getConductor() instanceof IWireConductor) {
				addConductorNode(getWiresNode(), node);
			}
			else if (node.getConductor() instanceof INetConductor) {
				addConductorNode(getNetsNode(), node);
			}
			else if (node.getConductor() instanceof IShieldConductor) {
				addConductorNode(getShieldsNode(), node);
			}
		}

		public void removeConductorTreeNode(PortTreeNode node)
		{
			if (node.getConductor() instanceof IWireConductor) {
				removeConductorNode(getWiresNode(), node);
			}
			else if (node.getConductor() instanceof INetConductor) {
				removeConductorNode(getNetsNode(), node);
			}
			else if (node.getConductor() instanceof IShieldConductor) {
				removeConductorNode(getShieldsNode(), node);
			}
		}

		public void addSharedConductorTreeNode(SharedConductorTreeNode node)
		{
			if (node.getConductor().isWire()) {
				addConductorNode(getWiresNode(), node);
			}
			else if (node.getConductor().isNet()) {
				addConductorNode(getNetsNode(), node);
			}
			else if (node.getConductor().isShield()) {
				addConductorNode(getShieldsNode(), node);
			}
		}

		public void removeSharedConductorTreeNode(SharedConductorTreeNode node)
		{
			if (node.getConductor().isWire()) {
				removeConductorNode(getWiresNode(), node);
			}
			else if (node.getConductor().isNet()) {
				removeConductorNode(getNetsNode(), node);
			}
			else if (node.getConductor().isShield()) {
				removeConductorNode(getShieldsNode(), node);
			}
		}

		private void addConductorNode(EditPortTreeNode condNode, EditPortTreeNode child)
		{
			if (condNode.getChildCount() == 0) {
				add(condNode);
			}
			condNode.add(child);
			sort();
		}

		private void removeConductorNode(EditPortTreeNode condNode, EditPortTreeNode child)
		{
			condNode.remove(child);
			if (condNode.getChildCount() == 0) {
				remove(condNode);
			}
		}
	}

	public static class TextTreeNodeComparator extends AlphaNumComparator
	{

		public TextTreeNodeComparator()
		{
			super(false);
		}

		public int compare(Object o1, Object o2)
		{
			EditPortTreeNode n1 = (EditPortTreeNode) o1;
			EditPortTreeNode n2 = (EditPortTreeNode) o2;
			String t1 = n1.getText();
			String t2 = n2.getText();
			return super.compare(t1, t2);
		}
	}

	public class DiagramTreeNode extends OrderedTreeNode
	{

		private IUID m_diagram = null;
		private List<IUID> m_conductors = null;

		public DiagramTreeNode(String name, IUID diagramUID, List<IUID> conds, boolean projectLevel)
		{
			super(name + " (" + conds.size() + ")", 0, projectLevel);
			m_diagram = diagramUID;
			m_conductors = new ArrayList<IUID>(conds);
			setText(name + " (" + conds.size() + ")");
			setIcon(m_diagramIcon);
		}

		/**
		 * Returns a list of UIDs to schem conductors.
		 *
		 * @return
		 */
		public List<IUID> getConductors()
		{
			return m_conductors;
		}
	}

	class ProjectTreeNode extends ConductorContainerTreeNode
	{

		private Map cctns = new HashMap();
		private Comparator comparator = new OrderedTreeNodeComparator();

		public ProjectTreeNode(IProject proj)
		{
			super(proj.getName(), m_projectIcon, 0, true);
			super.setComparator(comparator);
		}

		public void addConductorTreeNode(PortTreeNode node)
		{
			super.addConductorTreeNode(node);
		}

		public void removeConductorTreeNode(PortTreeNode node)
		{
			super.removeConductorTreeNode(node);
		}

		public void addSharedConductorTreeNode(SharedConductorTreeNode node)
		{
			ConductorContainerTreeNode otn = getTarget(node.getConductor());
			if (otn == this) {
				super.addSharedConductorTreeNode(node);
			}
			else {
				otn.addSharedConductorTreeNode(node);
			}
		}

		public void removeSharedConductorTreeNode(SharedConductorTreeNode node)
		{
			ConductorContainerTreeNode otn = getTarget(node.getConductor());
			if (otn == this) {
				super.removeSharedConductorTreeNode(node);
			}
			else {
				otn.removeSharedConductorTreeNode(node);
			}
		}

		private ConductorContainerTreeNode getTarget(ISharedConductor sc)
		{
			ConductorContainerTreeNode cctn;
			IDesignAbstraction dabs = sc.getDesignAbstraction();
			if (dabs == null) {
				cctn = this;
			}
			else {
				cctn = (ConductorContainerTreeNode) cctns.get(dabs);
				if (cctn == null) {
					String dabstr = "<None>";
					if (dabs != null) {
						dabstr = dabs.getName();
					}
					cctn = new ConductorContainerTreeNode(dabstr, m_designAbstractionIcon, 0, true);
					cctn.setComparator(comparator);
					cctns.put(dabs, cctn);
					add(cctn);
				}
			}
			return cctn;
		}
	}

	public interface SimpleTreeNode
	{

		public String getText();

		public String getSuffixText();

		public boolean isInvalid();

		public Icon getIcon();

		public Icon getDisabledIcon();

		public void setExpanded(boolean expanded);

		public void setEnabled(boolean enabled);

		public boolean isProjectLevel();
	}

	/**
	 * @param node
	 */
	public void removeNodeFromParent(MutableTreeNode node)
	{
		m_delegate.removeNodeFromParent(node);
	}

	/**
	 * Returns the node in the tree that represents the given conductor.
	 *
	 * @param condUID
	 * @return
	 */
	public EditPortTreeNode getEquivNode(IUID condUID)
	{
		return (EditPortTreeNode) m_equivNodeMap.get(condUID);
	}
}
