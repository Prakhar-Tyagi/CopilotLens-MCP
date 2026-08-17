package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.action.dragdrop.BasicTreeDragDropActionInvocationControl;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.helpers.datatransfer.DnDParameters;
import chs.caf.helpers.datatransfer.IDnDParameterInfo;
import chs.caplets.shared.actions.GroupAttributeUpdateAction;
import chs.caplets.shared.actions.GroupAttributeUpdateActionUI;
import chs.caplets.shared.actions.GroupAttributeUpdateData;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroupingAttributeTreeTransferHandler extends TransferHandler
{

	private BiConsumer<List<String>, List<List<String>>> callbackAfterTreeNodeDrop;
	private Function<DefaultMutableTreeNode, List<List<String>>> treePathFinder;
	private Supplier<Boolean> designStatusCheck;

	GroupingAttributeTreeTransferHandler(BiConsumer<List<String>, List<List<String>>> callback,
			Function<DefaultMutableTreeNode, List<List<String>>> treePathFinder, Supplier<Boolean> designStatusCheck)
	{
		callbackAfterTreeNodeDrop = callback;
		this.treePathFinder = treePathFinder;
		this.designStatusCheck = designStatusCheck;
	}

	@Override
	public int getSourceActions(@NotNull JComponent c)
	{
		return MOVE;
	}

	@Nullable @Override
	protected Transferable createTransferable(@NotNull JComponent c)
	{
		if (c instanceof GroupingByAttributesTree) {
			if (!designStatusCheck.get()) {
				return null;
			}
			GroupingByAttributesTree tree = (GroupingByAttributesTree) c;

			TreePath[] selectionPaths = tree.getSelectionPaths();
			if (selectionPaths == null) {
				return null;
			}
			List<DefaultMutableTreeNode> selectedLogicObjects = getLogicObjectsInSelection(tree);
			List<DefaultMutableTreeNode> selectedAttributeValueNodes = getAttributeValueNodesSelected(tree);

			if (selectedLogicObjects != null) {
				return new GroupingAttributeTreeTransferable(selectedLogicObjects, tree);
			}
			if (selectedAttributeValueNodes != null) {
				return new GroupingAttributeTreeTransferable(selectedAttributeValueNodes, tree);
			}
		}
		return null;
	}

	@Nullable private List<DefaultMutableTreeNode> getAttributeValueNodesSelected(JTree tree)
	{
		TreePath[] selectionPaths = tree.getSelectionPaths();
		if (selectionPaths == null) {
			return null;
		}
		List<DefaultMutableTreeNode> selectedObjects = new ArrayList<>();
		TreeNode parentNode = null;
		for (TreePath treePath : selectionPaths) {
			Object lastPathComponent = treePath.getLastPathComponent();
			if (lastPathComponent instanceof DefaultMutableTreeNode) {
				IGroupedAttributeModifier attrValueNode =
						UIDMgr.getObjectOfType((IUID) ((DefaultMutableTreeNode) lastPathComponent).getUserObject(),
								IGroupedAttributeModifier.class);

				if (attrValueNode != null) {
					TreeNode thisNodeParent = ((TreeNode) lastPathComponent).getParent();
					if (parentNode == null || thisNodeParent == parentNode) {
						selectedObjects.add((DefaultMutableTreeNode) lastPathComponent);
						parentNode = thisNodeParent;
					}
					else {
						return null; // all the logic objects selected should be of the same parent.
					}
				}
				else {
					return null; // all the selections should be of type logic objects.
				}
			}
		}
		return selectedObjects;
	}

	@Nullable private List<DefaultMutableTreeNode> getLogicObjectsInSelection(JTree tree)
	{
		TreePath[] selectionPaths = tree.getSelectionPaths();
		if (selectionPaths == null) {
			return null;
		}
		List<DefaultMutableTreeNode> selectedObjects = new ArrayList<>();
		TreeNode parentNode = null;
		for (TreePath treePath : selectionPaths) {
			Object lastPathComponent = treePath.getLastPathComponent();
			if (lastPathComponent instanceof DefaultMutableTreeNode) {
				ILogicObject logicObject =
						UIDMgr.getObjectOfType((IUID) ((DefaultMutableTreeNode) lastPathComponent).getUserObject(),
								ILogicObject.class);

				if (logicObject != null) {
					if (logicObject instanceof IConductor && ((IConductor) logicObject).getMulticore() != null) {
						return null;
					}
					if (logicObject instanceof IMulticore && ((IMulticore) logicObject).getParent() != null) {
						return null;
					}
					TreeNode thisNodeParent = ((TreeNode) lastPathComponent).getParent();
					if (parentNode == null || thisNodeParent == parentNode) {
						selectedObjects.add((DefaultMutableTreeNode) lastPathComponent);
						parentNode = thisNodeParent;
					}
					else {
						return null; // all the logic objects selected should be of the same parent.
					}
				}
				else {
					return null; // all the selections should be of type logic objects.
				}
			}
		}
		return selectedObjects;
	}

	@Override public boolean canImport(@NotNull TransferHandler.TransferSupport support)
	{
		if (!(support.isDrop() && (support.getDropAction() == TransferHandler.MOVE))) {
			return false;
		}

		GroupingAttrDndParameters dnDParameters = new GroupingAttrDndParameters(support);
		return dnDParameters.validate(targetTreeNode -> {
			if (targetTreeNode instanceof IBrowserTreeNode) {
				IUIDObject userObject = ((IBrowserTreeNode) targetTreeNode).getUIDObject();
				IGroupedAttributeModifier groupedAttributeModifier =
						CommonUtils.cast(userObject, IGroupedAttributeModifier.class);
				if (groupedAttributeModifier != null) {
					Pair<String, String> attributeNameValue = groupedAttributeModifier.getAttributeNameValue();
					boolean isAttribute =
							groupedAttributeModifier instanceof GroupAttributeConfigurator.IGroupAttributeConfiguratorNode &&
									((GroupAttributeConfigurator.IGroupAttributeConfiguratorNode) groupedAttributeModifier)
											.isAttribute();
					if (!GroupingByAttributesTree.applicableAttributes.contains(attributeNameValue.getFirst()) &&
							isAttribute) {
						return false;
					}
					Collection<DefaultMutableTreeNode> transferringData = dnDParameters.getTransferableData();
					if (!transferringData.isEmpty()) {
						DefaultMutableTreeNode transferingTreeNode = transferringData.iterator().next();
						ILogicObject transferingLogicObject =
								UIDMgr.getObjectOfType((IUID) transferingTreeNode.getUserObject(), ILogicObject.class);
						if (transferingLogicObject != null) {
							if (transferingTreeNode.isNodeAncestor(targetTreeNode)) {
								return false;
							}
							return true;
						}
						IGroupedAttributeModifier transferingFolderNode =
								UIDMgr.getObjectOfType((IUID) transferingTreeNode.getUserObject(),
										IGroupedAttributeModifier.class);
						if (transferingFolderNode != null) {
							return groupedAttributeModifier.isAcceptable(transferingFolderNode);
						}
					}
				}
			}
			return false;
		});
	}

	@Nullable private Collection<ILogicObject> getLogicObjectsBeingDragged(List<IDnDParameterInfo> sourceData)
	{

		Collection<ILogicObject> logicObjectsToUpdate = new ArrayList<>();
		for (IDnDParameterInfo aSourceData : sourceData) {
			Object userObject = aSourceData.getUserObject();
			if (userObject instanceof IUID) {
				ILogicObject logicObject = UIDMgr.getObjectOfType((IUID) userObject, ILogicObject.class);
				if (logicObject == null) {
					return null;
				}
				logicObjectsToUpdate.add(logicObject);
			}
		}
		return logicObjectsToUpdate;
	}

	@Nullable
	private Map<IDnDParameterInfo, IGroupedAttributeModifier> getAttributeValueNodesBeingDragged(
			List<IDnDParameterInfo> sourceData)
	{

		Map<IDnDParameterInfo, IGroupedAttributeModifier>
				subTreeNodesToUpdate = new LinkedHashMap<>();
		for (IDnDParameterInfo aSourceData : sourceData) {

			Object userObject = aSourceData.getUserObject();
			if (userObject instanceof IUID) {
				IGroupedAttributeModifier
						aDraggedNode = UIDMgr.getObjectOfType((IUID) userObject,
						IGroupedAttributeModifier.class);
				if (aDraggedNode == null) {
					return null;
				}
				subTreeNodesToUpdate.put(aSourceData, aDraggedNode);
			}
		}
		return subTreeNodesToUpdate;
	}

	@Override public boolean importData(@NotNull TransferSupport support)
	{
		if (!canImport(support)) {
			return false;
		}
		GroupingAttrDndParameters dnDParameters = new GroupingAttrDndParameters(support);
		IDnDParameterInfo targetInfo = dnDParameters.getTargetInfo();
		DefaultMutableTreeNode targetTreeNode = dnDParameters.getTargetTreeNode();

		IGroupedAttributeModifier groupedAttributeModifier =
				UIDMgr.getObjectOfType((IUID) targetInfo.getUserObject(), IGroupedAttributeModifier.class);
		if (groupedAttributeModifier == null) {
			return false;
		}
		List<GroupAttributeUpdateData> attributeValuesToApply = getAttributeValuesToApply(targetInfo);
		if (attributeValuesToApply == null) {
			return false;
		}

		List<IDnDParameterInfo> sourceData = dnDParameters.getTransferableDataInfo();
		Collection<ILogicObject> logicObjectsToUpdate = getLogicObjectsBeingDragged(sourceData);
		Map<IDnDParameterInfo, IGroupedAttributeModifier>
				attributeValueNodesDragged =
				getAttributeValueNodesBeingDragged(sourceData);
		List<List<String>> childPathsToExpand = new ArrayList<>(1);
		List<GroupAttributeUpdateAction.UpdateParams> updateParams = new ArrayList<>();
		if (logicObjectsToUpdate != null && !logicObjectsToUpdate.isEmpty()) {
			GroupAttributeUpdateAction.UpdateParams updateParam =
					new GroupAttributeUpdateAction.UpdateParams(logicObjectsToUpdate, attributeValuesToApply);
			updateParams.add(updateParam);
			ILogicObject aLogicObject = logicObjectsToUpdate.iterator().next();
			childPathsToExpand = Arrays.asList(groupedAttributeModifier.getExpectedChildAttributeValue(aLogicObject));
		}
		else if (attributeValueNodesDragged != null && !attributeValueNodesDragged.isEmpty()) {
			Collection<DefaultMutableTreeNode> defaultMutableTreeNodes = dnDParameters.getTransferableData();
			for (DefaultMutableTreeNode aTreeNode : defaultMutableTreeNodes) {
				childPathsToExpand.addAll(treePathFinder.apply(aTreeNode));
			}

			for (IDnDParameterInfo aSourceData : attributeValueNodesDragged.keySet()) {

				IGroupedAttributeModifier aNode =
						attributeValueNodesDragged.get(aSourceData);
				Pair<String, String> attrNameValue = aNode.getAttributeNameValue();

				if (!attrNameValue.getFirst().equals(attributeValuesToApply.get(0).getName())) {

					Collection<IUID> logicObjectUIDs =
							aNode.recursiveGetAllChildObjects();
					Collection<ILogicObject> logicObjects =
							UIDUtils.convertToObjectSet(logicObjectUIDs, ILogicObject.class);
					GroupAttributeUpdateAction.UpdateParams updateParam =
							new GroupAttributeUpdateAction.UpdateParams(
									logicObjects,
									attributeValuesToApply);
					updateParams.add(updateParam);
				}
				else if (attrNameValue.getFirst().equals(attributeValuesToApply.get(0).getName())) {

					Map<String, Collection<ILogicObject>> logicObjectsWithsameAttrValues =
							aNode.getTrailingValueForAttributeBelowThisLevelInTree(ILogicObject.class);
					for (String aTrailing : logicObjectsWithsameAttrValues.keySet()) {
						List<GroupAttributeUpdateData> attributeValuesToApplyForThis =
								new ArrayList<>(attributeValuesToApply);

						GroupAttributeUpdateData lastAttributeCopy =
								attributeValuesToApplyForThis.remove(0);
						GroupAttributeUpdateData lastAttributeValue = new GroupAttributeUpdateData(lastAttributeCopy);
						String fullAttributeValue = lastAttributeValue.getValue() + aTrailing;
						lastAttributeValue.setValue(fullAttributeValue);
						attributeValuesToApplyForThis.add(lastAttributeValue);
						GroupAttributeUpdateAction.UpdateParams updateParam =
								new GroupAttributeUpdateAction.UpdateParams(
										logicObjectsWithsameAttrValues.get(aTrailing),
										attributeValuesToApplyForThis);
						updateParams.add(updateParam);
					}
				}
			}
		}

		if (!updateParams.isEmpty()) {

			IAction action =
					new GroupAttributeUpdateAction(CAFUtils.getInstance().getActiveCapletController(),
							updateParams);

			IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actionMgr != null && action.isEnabled()) {

				ActionEvent actionEvent =
						new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
								ResourceMgr.getStringForMenu(GroupAttributeUpdateActionUI.class,
										"GroupAttributeUpdateActionUI.Update.Name"));
				actionEvent.setSource(this); //to map the current location correctly wrt the source
				actionMgr.actionPerformed(action, actionEvent);
				if (childPathsToExpand.isEmpty()) {
					childPathsToExpand.add(Arrays.asList());
					//expand atleast the target dropped node.
				}
				callbackAfterTreeNodeDrop.accept(getTargetTreeNodePath(targetTreeNode), childPathsToExpand);
			}
		}

		return false;
	}

	@Nullable List<GroupAttributeUpdateData> getAttributeValuesToApply(IDnDParameterInfo targetInfo)
	{
		IDnDParameterInfo currentTargetInfo = targetInfo;
		List<String> attributesAlreadyVisited = new ArrayList<>();
		List<GroupAttributeUpdateData> attributeValuesToApply = new ArrayList<>();
		while (currentTargetInfo != null) {

			GroupAttributeUpdateData currentAttributeValue = getAttributeValue(currentTargetInfo.getUserObject());
			if (currentAttributeValue != null && !attributesAlreadyVisited.contains(currentAttributeValue.getName())) {

				if (!GroupingByAttributesTree.ObjectTypeAttribute.equals(currentAttributeValue.getName())) {

					attributeValuesToApply.add(currentAttributeValue);
					attributesAlreadyVisited.add(currentAttributeValue.getName());
				}
			}

			currentTargetInfo = currentTargetInfo.getParentInfo();
		}
		return attributeValuesToApply;
	}

	@Nullable private GroupAttributeUpdateData getAttributeValue(Object userObject)
	{
		if (userObject instanceof IUID) {
			IGroupedAttributeModifier groupedAttributeModifier =
					UIDMgr.getObjectOfType((IUID) userObject, IGroupedAttributeModifier.class);
			if (groupedAttributeModifier != null) {
				boolean isAttribute =
						groupedAttributeModifier instanceof GroupAttributeConfigurator.IGroupAttributeConfiguratorNode &&
								((GroupAttributeConfigurator.IGroupAttributeConfiguratorNode) groupedAttributeModifier)
										.isAttribute();

				return new GroupAttributeUpdateData(
						groupedAttributeModifier.getAttributeNameValue().getFirst(),
						groupedAttributeModifier.getAttributeNameValue().getSecond(), isAttribute);
			}
		}
		return null;
	}

	private static class GroupingAttrDndParameters extends DnDParameters
	{

		private GroupingAttrDndParameters(@NotNull TransferSupport tSupport)
		{
			super(tSupport);
		}

		@Override protected DataFlavor createDataFlavor()
		{
			return BasicTreeDragDropActionInvocationControl
					.createTransferDataFlavors(GroupingAttributeTreeTransferable.class);
		}

		@NotNull @Override protected DefaultMutableTreeNode getTargetTreeNode()
		{
			return super.getTargetTreeNode();
		}

		@NotNull protected Collection<DefaultMutableTreeNode> getTransferableData()
		{
			return transferableData;
		}
	}

	private List<String> getTargetTreeNodePath(DefaultMutableTreeNode targetNode)
	{
		List<String> targetNodePath = new ArrayList<>();
		DefaultMutableTreeNode currentNode = targetNode;
		while (currentNode != null) {
			targetNodePath.add(currentNode.toString());
			currentNode = (DefaultMutableTreeNode) currentNode.getParent();
		}

		CollectionUtils.reverse(targetNodePath);
		return targetNodePath;
	}
}

