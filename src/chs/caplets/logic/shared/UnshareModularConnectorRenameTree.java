package chs.caplets.logic.shared;

import chs.utilities.ui.tree.BaseJTree;
import chs.utilities.ui.CHSColors;
import chs.utilities.ResourceMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CollectionUtils;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ILogicObject;
import chs.utility.ui.IconUtils;
import chs.common.IChangeListener;
import chs.common.IViewNotifier;
import chs.common.IUID;
import chs.system.UIDMgr;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.ToolTipManager;
import java.util.Map;
import java.util.EventObject;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.awt.Component;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 26 Apr, 2013. Tree used in Unshare Modualar Connector dialog.
 */

public class UnshareModularConnectorRenameTree implements IViewNotifier
{

	private BaseJTree tree = null;
	private Map<IConnector, String> m_ObjVsNameMap = null;
	private boolean m_bValidTree = true;
	private Collection<IChangeListener> m_listeners;

	public UnshareModularConnectorRenameTree(IConnector connector)
	{
		m_ObjVsNameMap = new HashMap<IConnector, String>();
		m_listeners = new ArrayList<IChangeListener>();
		tree = new BaseJTree();
		tree.setName("UnshareModularConnectorRenameTree");
		ToolTipManager.sharedInstance().registerComponent(tree);

		//create and set renderer
		ModularTreeCellRenderer renderer = new ModularTreeCellRenderer();
		tree.setCellRenderer(renderer);

		//create & set the editor
		ModularTreeCellEditor cellEditor = new ModularTreeCellEditor(tree, renderer);
		tree.setCellEditor(cellEditor);
		tree.setInvokesStopCellEditing(true);

		//create rootNode & populate tree
		final ConnectorNode rootNode = new ConnectorNode(connector);
		populateTree(rootNode);

		//create & set model
		final ModularConnectorTreeModel model = new ModularConnectorTreeModel(rootNode);
		tree.setModel(model);

		//make tree editable & expand all rows
		tree.setEditable(true);
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	public Map<IConnector, String> getObjNameMap()
	{
		return m_ObjVsNameMap;
	}

	public boolean isTreeValid()
	{
		return m_bValidTree;
	}

	/**
	 * Given a node, this will recursively build the tree [iterating all child connectors in hierarchy] At the same time,
	 * set the node's validity based on existing shared object names Also, populate the model's connector vs name map.
	 *
	 * @param node - input root node
	 */
	private void populateTree(ConnectorNode node)
	{
		IConnector connector = node.getConnector();
		m_ObjVsNameMap.put(connector, connector.getName());
		node.validateInExistingDesignAndUpdateNodeValidity();
		for (IConnector childConnector : getChildren(connector)) {
			ConnectorNode childNode = new ConnectorNode(childConnector);
			node.add(childNode);
			populateTree(childNode);
		}
	}

	@Override public void registerListener(IChangeListener l, String message)
	{
		m_listeners.add(l);
	}

	@Override public boolean removeListener(IChangeListener l)
	{
		return m_listeners.remove(l);
	}

	@Override public void removeAllListeners()
	{
		m_listeners.clear();
	}

	@Override public void notifyListeners(String message)
	{
		for (IChangeListener l : m_listeners) {
			l.changeNotify(this, null, null);
		}
	}

	private class ConnectorNode extends DefaultMutableTreeNode
	{

		private boolean m_valid = true;
		private String m_tooltip = "";

		ConnectorNode(IConnector connector)
		{
			super(connector);
		}

		public IConnector getConnector()
		{
			return (IConnector) getUserObject();
		}

		@Override public String toString()
		{
			String displayName = "";
			if (getUserObject() instanceof IConnector) {
				displayName = m_ObjVsNameMap.get(getConnector());
			}
			return displayName;
		}

		public void validateInExistingDesignAndUpdateNodeValidity()
		{
			if (doesNonSharedObjExistsWithSameName(toString(), getConnector())) {
				m_valid = false;
				setTooltip(ResourceMgr.getString(UnshareModularConnectorRenameTree.class,
						"UnshareModularConnectorRenameTree.nameused.text"));
				setModularConnectorTreeValidity(false);
			}
		}

		public void validate(String newName)
		{
			boolean valid = validateName(newName);
			if (m_valid != valid) {
				m_valid = valid;
				//tell the model that this node is changed. It will ask the renderer to repaint this node
				((DefaultTreeModel) tree.getModel()).nodeChanged(this);
			}
		}

		/**
		 * checks if new name provided is valid or not. Empty name is not valid Name which matches with any other connector in
		 * this hierarchy is not valid Name which matched with any shared object of this type is not valid Rest all cases is
		 * valid name
		 *
		 * @param newName - the newName whose validity has to be checked
		 *
		 * @return true if name is valid
		 */
		private boolean validateName(String newName)
		{
			StringBuffer errMsg = new StringBuffer();
			if (!isNameValid(newName, errMsg)) {
				setTooltip(errMsg.toString());
				return false;
			}
			IConnector currentNodeConnector = (IConnector) getUserObject();
			for (Map.Entry<IConnector, String> entry : m_ObjVsNameMap.entrySet()) {
				IConnector key = entry.getKey();
				if (key != currentNodeConnector) {
					String val = entry.getValue();
					if (newName.compareToIgnoreCase(val) == 0) {
						errMsg.append(ResourceMgr.getString(UnshareModularConnectorRenameTree.class,
								"UnshareModularConnectorRenameTree.nameused.text"));
						setTooltip(errMsg.toString());
						return false;
					}
				}
			}
			if (doesNonSharedObjExistsWithSameName(newName, currentNodeConnector)) {
				errMsg.append(ResourceMgr.getString(UnshareModularConnectorRenameTree.class,
						"UnshareModularConnectorRenameTree.nameused.text"));
				setTooltip(errMsg.toString());
				return false;
			}
			setTooltip("");
			return true;
		}

		public boolean isValid()
		{
			return m_valid;
		}

		public void update(String newName)
		{
			m_ObjVsNameMap.put(getConnector(), newName);
		}

		public void setTooltip(String tooltip)
		{
			m_tooltip = tooltip;
		}

		public String getTooltip()
		{
			return m_tooltip;
		}

		private boolean doesNonSharedObjExistsWithSameName(String name, IPinList pinlist)
		{
			int nDuplicateNamedObjects = 0;
			Set<IUID> nsObjIDs =
					CollectionUtils.createSet(pinlist.getNameMgr().getNameSpace(pinlist).getObjectSet(name));
			for (IUID uid : nsObjIDs) {
				ILogicObject nObj = (ILogicObject) UIDMgr.getObject(uid);
				if (!m_ObjVsNameMap.containsKey(nObj)) {
					nDuplicateNamedObjects++;
				}
			}
			return nDuplicateNamedObjects > 0;
		}

		private boolean isNameValid(String name, StringBuffer errmsg)
		{
			boolean ok = true;

			if (name == null || "".equals(name.trim())) {
				if (errmsg.length() != 0) {
					errmsg.append('\n');
				}
				errmsg.append(ResourceMgr.getString(UnshareModularConnectorRenameTree.class,
						"UnshareModularConnectorRenameTree.InvalidName.text"));
				ok = false;
			}
			else if (name.length() > CHSConstants.MAX_NAME_LENGTH) {
				errmsg.append(ResourceMgr.getString(UnshareModularConnectorRenameTree.class,
						"UnshareModularConnectorRenameTree.nametoolong.text",
						String.valueOf(CHSConstants.MAX_NAME_LENGTH)));
				ok = false;
			}

			return ok;
		}
	}

	private void setModularConnectorTreeValidity(boolean b)
	{
		m_bValidTree = b;
		notifyListeners(null);
	}

	@Nullable private Icon getNodeIcon(Object value)
	{
		Icon icon = null;
		if (value instanceof ConnectorNode) {
			ConnectorNode treeNode = (ConnectorNode) value;
			Icon m_rootIcon = IconUtils.getIcon(treeNode.getConnector(), IconUtils.ACTIVE);
			Icon m_childIcon = IconUtils.getFilledPositionIcon(IconUtils.NEITHER);
			icon = treeNode.isRoot() ? m_rootIcon : m_childIcon;
		}
		return icon;
	}

	private class ModularTreeCellRenderer extends DefaultTreeCellRenderer
	{

		/**
		 * overridden to change the text color based on its validity
		 */
		@Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
				boolean expanded,
				boolean leaf, int row, boolean hasFocus)
		{
			Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
					hasFocus);
			if (value instanceof ConnectorNode && comp instanceof JLabel) {
				JLabel label = (JLabel) comp;
				ConnectorNode treeNode = (ConnectorNode) value;
				setToolTipText(treeNode.getTooltip());
				if (!treeNode.isValid()) {
					label.setForeground(CHSColors.getErrorForegroundColor());
				}
				Icon icon = getNodeIcon(value);
				setOpenIcon(icon);
				setClosedIcon(icon);
				setLeafIcon(icon);
				setIcon(icon);
			}
			return comp;
		}
	}

	private class ModularConnectorTreeModel extends DefaultTreeModel
	{

		ModularConnectorTreeModel(TreeNode root)
		{
			super(root);
		}

		/**
		 * When a node is edited, check if there is actually an edit. If so, 1. first check if the new name is valid & update
		 * node's valid status 2. update the model with this new name 3. now, update the valid status of rest of the tree
		 * nodes(other node's which were invalid earlier might now become valid etc) 4. Finally, set the tree's validity in
		 * the model
		 *
		 * @param path - path to the node that is edited
		 * @param newValue - value entered in the node
		 */
		@Override
		public void valueForPathChanged(TreePath path, Object newValue)
		{
			if (path == null) {
				return;
			}
			ConnectorNode node = (ConnectorNode) path.getLastPathComponent();
			String oldName = node.toString();
			String newName = (String) newValue;
			if (oldName.compareToIgnoreCase(newName) != 0) {
				//update the model with new name
				node.update(newName);
				//check if the new name is valid & update node's validity
				node.validate(newName);
				//this inturn may change the validity of other nodes. Hence, validate & update the remaining tree nodes
				//If they change their validity status, nodeChangedEvent will trigger & the node is repainted
				validateTree((DefaultMutableTreeNode) getRoot(), node);
				//Now update the model if tree is valid
				setModularConnectorTreeValidity(getTreeValidity((DefaultMutableTreeNode) getRoot()));
			}
		}

		/**
		 * Get all the nodes & if all are valid, return true;  Else return false if atleast one node is invalid
		 *
		 * @param rootNode - tree's root
		 *
		 * @return - true if all nodes are valid; false otherwise
		 */
		private boolean getTreeValidity(DefaultMutableTreeNode rootNode)
		{
			Enumeration e = rootNode.breadthFirstEnumeration();
			while (e.hasMoreElements()) {
				ConnectorNode node = (ConnectorNode) e.nextElement();
				if (!node.isValid()) {
					return false;
				}
			}
			return true;
		}

		private void validateTree(DefaultMutableTreeNode rootNode, ConnectorNode connectorNode)
		{
			Enumeration e = rootNode.breadthFirstEnumeration();
			while (e.hasMoreElements()) {
				ConnectorNode node = (ConnectorNode) e.nextElement();
				if (connectorNode != node) {
					node.validate(node.toString());
				}
			}
		}
	}

	private class ModularTreeCellEditor extends DefaultTreeCellEditor
	{

		ModularTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer)
		{
			super(tree, renderer);
		}

		/**
		 * Override this to get the correct object name
		 */
		@SuppressWarnings({"ParameterHidesMemberVariable"})
		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
				boolean leaf, int row)
		{
			Component comp = super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
			editingIcon = getNodeIcon(value);
			return comp;
		}

		/**
		 * Override this to allow double-click to edit.
		 *
		 * @param event the event being studied
		 */
		protected boolean canEditImmediately(EventObject event)
		{
			return true;
		}
	}

	private List<IConnector> getChildren(IConnector connector)
	{
		List<IConnector> childern = new ArrayList<IConnector>();
		for (IInternalPosition position : connector.getPositions()) {
			for (IInternalPositionedObject obj : position.getAssociatedObjects()) {
				if (obj instanceof IConnector) {
					childern.add((IConnector) obj);
				}
			}
		}
		return childern;
	}

	public BaseJTree getTree()
	{
		return tree;
	}
}

