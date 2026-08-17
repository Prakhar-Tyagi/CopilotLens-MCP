/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.BatchDevicePlacementAction;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author chandras on 28-10-2019.
 */
public abstract class LogicDesignBrowserClient extends BrowserClientHelper
{

	protected LogicDesignBrowserClient(ICapletController controller)
	{
		super(controller);
	}

	public void handleMouseDragged(@NotNull Point diagramDropPoint, @NotNull List<IBrowserTreeNode> selectedNodes,
			@NotNull JTree tree)
	{
		final IActionMgr actionMgr = getController().getActionMgr();
		final ISelectMgr selectMgr = getController().getSelectMgr();
		final IAction action = actionMgr.findAction(BatchDevicePlacementAction.class.getName());
		final IAction activeAction = actionMgr.getActiveAction();
		if (action != null && activeAction != action) {
			boolean updatedSelectSet = false;
			Set<IBrowserTreeNode> candidateTreeNodesForCollapse = new HashSet<>();
			if (!action.isEnabled() && selectedNodes.size() == 1) {
				final IBrowserTreeNode folder = selectedNodes.get(0);
				List<IDevice> devicesToProcess = new ArrayList<>();
				collectDevicesToProcess(tree, folder, candidateTreeNodesForCollapse, devicesToProcess);
				if (!devicesToProcess.isEmpty()) {
					final SelectSet selectSet = new SelectSet();
					//add the selection set after setting the select set.
					//otherwise notification would be sent.
					selectSet.add(devicesToProcess, false);
					selectMgr.setSelectSet(selectSet);
					updatedSelectSet = true;
				}
			}
			if (action.isEnabled()) {
				ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "mousedrag");
				actionMgr.actionPerformed(action, ae);
			}
			else if (updatedSelectSet) {
				selectMgr.removeSelectSet();
				candidateTreeNodesForCollapse.forEach(n -> tree.collapsePath(new TreePath(n)));
			}
			else if (selectedNodes.size() == 1) {
				activateObject(selectedNodes.get(0).getUID());
			}
		}
	}

	private void collectDevicesToProcess(
			@NotNull JTree tree, @NotNull IBrowserTreeNode folder,
			@NotNull Set<IBrowserTreeNode> candidateTreeNodesForCollapse,
			@NotNull List<IDevice> devicesToProcess)
	{
		List<TreeNode> fullPath = new ArrayList<>();
		TreeNode curNode = folder;
		while (curNode != null) {
			fullPath.add(curNode);
			curNode = curNode.getParent();
		}
		CollectionUtils.reverse(fullPath);
		final TreePath folderPath = new TreePath(fullPath.toArray());
		final boolean expanded = tree.isExpanded(folderPath);
		if (!expanded) {
			candidateTreeNodesForCollapse.add(folder);
			tree.expandPath(folderPath);
		}
		//need to consider filterable tree. so use model.
		final TreeModel treeModel = tree.getModel();
		final int childCount = treeModel.getChildCount(folder);
		final List<IDevice> deviceCandidates = new ArrayList<>(childCount);
		for (int idx = 0; idx < childCount; ++idx) {
			final IBrowserTreeNode treeNode =
					CommonUtils.cast(treeModel.getChild(folder, idx), IBrowserTreeNode.class);
			if (treeNode != null) {
				final IUIDObject nodeObject = treeNode.getUIDObject();
				if (nodeObject instanceof ILogicObject || nodeObject instanceof IInternalPosition) {
					if (nodeObject instanceof IDevice) {
						deviceCandidates.add((IDevice) nodeObject);
					}
				}
				else {
					//recurse through children.
					collectDevicesToProcess(tree, treeNode, candidateTreeNodesForCollapse, deviceCandidates);
				}
			}
		}
		if (deviceCandidates.isEmpty()) {
			if (candidateTreeNodesForCollapse.remove(folder)) {
				tree.collapsePath(folderPath);
			}
		}
		else {
			devicesToProcess.addAll(deviceCandidates);
		}
	}
}
