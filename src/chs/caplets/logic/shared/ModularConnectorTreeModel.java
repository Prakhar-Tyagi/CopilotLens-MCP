/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2025 Siemens
 */
package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.helper.ModularConnectorHandler;
import chs.caplets.logic.actions.shared.helper.ModularConnectorHandler.IConnectorNode;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.table.ITreeTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

public class ModularConnectorTreeModel extends DefaultTreeModel implements ITreeTableModel
{

	// Delegate to perform model changes and other business logic, reused in Auto-share flow.
	@NotNull protected final ModularConnectorHandler mHandler;
	private static final int COLUMN_COUNT = 2;
	private String[] m_ColumnHeaders = new String[]{
			ResourceMgr.getString(ModularConnectorTreeModel.class, "ModularConnectorTreeModel.col.name"),
			ResourceMgr.getString(ModularConnectorTreeModel.class, "ModularConnectorTreeModel.col.generated")
	};
	private Class<?>[] m_aColumnClassTypes =
			{
					ITreeTableModel.class,
					JCheckBox.class
			};

	public ModularConnectorTreeModel(@Nullable TreeNode root, @NotNull EditSharedPinListModel esplModel, @NotNull
			ILogicDesign design, @NotNull IConnector connector)
	{
		super(root);
		mHandler = new ModularConnectorHandler(esplModel, design, connector, this::getConnectorNodesInTree);
		addModelListener();
	}

	@NotNull private Collection<IConnectorNode> getConnectorNodesInTree()
	{
		final Collection<IConnectorNode> nodesInTree = new ArrayList<>();
		Enumeration e = ((DefaultMutableTreeNode) getRoot()).breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode mutableTreenode = (DefaultMutableTreeNode) e.nextElement();
			nodesInTree.add((IConnectorNode) mutableTreenode.getUserObject());
		}

		return nodesInTree;
	}

	public void populateTree(DefaultMutableTreeNode node)
	{
		populateTree(node, mHandler.getRootConnector());
	}

	private void populateTree(@NotNull DefaultMutableTreeNode node, @NotNull IConnector connector)
	{
		for (IConnectorNode rNode : mHandler.createChildrenNodes(connector)) {
			ModularConnectorTreeNode childNode = new ModularConnectorTreeNode(rNode);
			insertNodeInto(childNode, node, node.getChildCount());
			populateTree(childNode, rNode.getConnector());
		}
	}

	/**
	 * When a node is edited, check if there is actually an edit. If so, 1. first check if the new name is valid &
	 * update node's valid status 2. update the model with this new name 3. now, update the valid status of rest of the
	 * tree nodes(other node's which were invalid earlier might now become valid etc) 4. Finally, set the tree's
	 * validity in the model
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
		DefaultMutableTreeNode mutabletreenode = (DefaultMutableTreeNode) path.getLastPathComponent();
		IConnectorNode node = (IConnectorNode) mutabletreenode.getUserObject();
		String oldName = node.toString().trim();
		String newName = ((String) newValue).trim();
		if (oldName.compareToIgnoreCase(newName) != 0) {
			mHandler.onConnectorNameChange(node, newName);
			nodeChanged(mutabletreenode);
		}
	}

	@Override public int getColumnCount()
	{
		return COLUMN_COUNT;
	}

	@Override public Object getColumnIdentifier(int column)
	{
		return m_ColumnHeaders[column];
	}

	@Override public Class getColumnClass(int column)
	{
		return m_aColumnClassTypes[column];
	}

	@Override public Object getValueAt(Object node, int column)
	{
		DefaultMutableTreeNode ctn = (DefaultMutableTreeNode) node;
		final IConnectorNode cNode = (IConnectorNode) ctn.getUserObject();
		if (column == 0) {
			return mHandler.getSharedName(cNode);
		}
		if (column == 1) {
			return mHandler.getSharedNameGenerated(cNode);
		}
		return null;
	}

	@Override public void setValueAt(Object aValue, Object node, int column)
	{
		if (node instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
			Object conn = treeNode.getUserObject();
			if (conn instanceof IConnectorNode) {
				final IConnectorNode cNode = (IConnectorNode) conn;
				if (column == 0) {
					String newName = ((String) aValue).trim();
					mHandler.setSharedName(cNode, newName);
				}
				else if (column == 1) {
					mHandler.setSharedNameGenerated(cNode, (Boolean) aValue);
				}
			}
			fireTreeNodesChanged(treeNode, getPathToRoot(treeNode), null, null);
		}
	}

	@Override public boolean isCellEditable(Object node, int column)
	{
		return true;
	}

	@Override public void renameNode(Object node, String name)
	{
	}

	@NotNull public DefaultMutableTreeNode createRootNode()
	{
		final IConnectorNode rNode = mHandler.createRootNode();
		ModularConnectorTreeNode rootNode = new ModularConnectorTreeNode(rNode);
		return rootNode;
	}

	protected void addModelListener()
	{
		addTreeModelListener(new TreeModelListener()
		{
			public void treeNodesChanged(TreeModelEvent e)
			{
			}

			public void treeNodesInserted(TreeModelEvent e)
			{
				Object[] objects =  e.getChildren();
				if (objects != null) {
					for (Object ob : objects) {
						if (ob instanceof DefaultMutableTreeNode) {
							DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) ob;
							Object conn = treeNode.getUserObject();
							if (conn instanceof IConnectorNode) {
								IConnectorNode rNode = (IConnectorNode) conn;
								mHandler.onConnectorNodeAdd(rNode);
							}
						}
					}
				}
			}

			public void treeNodesRemoved(TreeModelEvent e)
			{
			}

			public void treeStructureChanged(TreeModelEvent e)
			{
			}
		});
	}
}
