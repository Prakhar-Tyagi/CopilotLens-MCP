package chs.caplets.shared;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.action.utility.ActionUtilities;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.BrowserActionContainer;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObjectChangeEvent;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.AttributeType;
import chs.common.attr.IAttributeTypes;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.TreeUtils;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.ui.CHSSwingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;

public class GroupingByAttributesTree extends BrowserTreeHelper
{

	public static String ObjectTypeAttribute = "ObjectType";
	public static Collection<String> applicableAttributes =
			Arrays.asList(IAttributeTypes.IEC_LOCATION, IAttributeTypes.IEC_FUNCTION);
	@Nullable protected TreePathNodeFinder treePathNodeFinder;
	protected GroupAttributeConfigurator groupAttributeConfigurator;
	private IUID projectUID;

	/**
	 * Construct a browser tree
	 *
	 * @param client The client implementation for the tree to get information from
	 * @param name Name of the tree
	 */
	public GroupingByAttributesTree(@NotNull IBrowserClient client, String name,
			GroupAttributeConfigurator configurator)
	{
		super(client, name);
		ILogicDesign logicDesign = getDesign();
		if (logicDesign != null) {
			IUID projectUID1 = logicDesign.getProject() != null ? logicDesign.getProject().getUID() : null;
			assert projectUID1 != null;
			projectUID = projectUID1;
			setDropMode(DropMode.ON);
		}
		setRootVisible(false);

		groupAttributeConfigurator = configurator;
	}

	protected Boolean designEditable(ILogicDesign logicDesign)
	{
		boolean isDesignEditable =
				(logicDesign.isEditable() && !CAFUtils.getInstance().isDesignOpenReadOnly(logicDesign));
		return logicDesign.isLocked() && isDesignEditable;
	}


	public void treeConfigurationChanged(int index, Collection<IGroupAttributeAddEntry> attributes)
	{

		groupAttributeConfigurator.setChildAttributes(attributes);
		rebuildTree();
		setPreferences(index, attributes);
	}

	@Nullable protected TransferHandler constructTransferTreeHandler()
	{
		final ILogicDesign design = getDesign();
		if (design != null) {
			return new GroupingAttributeTreeTransferHandler(
					(pathToDroppedNode, paths) -> expandAllNodesAddedAfterDrop(pathToDroppedNode, paths), (node) -> {

				return findDescendentExpandedStateOfNode(node);
			}, () -> designEditable(design));
		}
		return super.constructTransferTreeHandler();
	}

	@NotNull @Override
	protected IBrowserTreeNode createChildNode(IBrowserTreeNode parent, IUID uid)
	{
		IBrowserTreeNode browserTreeNode = super.createChildNode(parent, uid);
		if (treePathNodeFinder != null) {
			treePathNodeFinder.newChildCreated(parent, browserTreeNode);
		}
		return browserTreeNode;
	}

	public void rebuildTree()
	{
		setTreeDirty(true);
		rebuild(Collections.emptyList());
	}

	public void setPreferences(int index, Collection<IGroupAttributeAddEntry> attributes)
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		preferences = preferences.node("GroupingByAttributeTree");
		try {
			preferences = preferences.node("path" + index);
			preferences = preferences.node("attributes");
			preferences.clear();
			for (IGroupAttributeAddEntry anAttribute : attributes) {
				StringJoiner joiner = new StringJoiner("|");
				if (anAttribute.seperator() != null) {

					for (String aSeperator : anAttribute.seperator()) {
						//first is a regular expression to find all the occurances of | and second one is a string \|

						joiner.add(aSeperator.replaceAll("\\|", Matcher.quoteReplacement("\\") + "|"));
					}
				}
				String preferenceKey =
						anAttribute.isAttribute() ? anAttribute.getName() : "Property" + anAttribute.getName();
				String preferenceValue = joiner.toString();
				if (preferenceKey.length() <= Preferences.MAX_KEY_LENGTH &&
						preferenceValue.length() <= Preferences.MAX_VALUE_LENGTH) {
					preferences.put(preferenceKey, preferenceValue);
				}
			}
		}
		catch (BackingStoreException ignored) {

		}
	}

	protected List<List<String>> findDescendentExpandedStateOfNode(DefaultMutableTreeNode node)
	{

		List<List<String>> treePathToExpand = new ArrayList<>();
		Runnable descendentPaths = () -> {
			TreePath thisNodePath = TreeUtils.getPath(node);
			int pathLengthToIgnore = thisNodePath.getPath().length - 1;

			Enumeration<TreePath>
					eExpandedNodes = getExpandedDescendants(thisNodePath);
			if (eExpandedNodes != null) {
				while (eExpandedNodes.hasMoreElements()) {

					List<String> thisPathExpanded = new ArrayList<>();
					TreePath aTreePath = eExpandedNodes.nextElement();
					Object[] treePathObject = aTreePath.getPath();
					for (int i = pathLengthToIgnore; i < treePathObject.length; i++) {
						thisPathExpanded.add(treePathObject[i].toString());
					}

					treePathToExpand.add(thisPathExpanded);
				}
			}
		};
		invokdEditTask(descendentPaths);

		return treePathToExpand;
	}

	@Override protected void registerListeners()
	{
		super.registerListeners();
		IProject project = UIDMgr.getObjectOfType(projectUID, IProject.class);
		LogicBrowserTree.SharedObjectChangeListenerAdder.registerForSharedObjectChanges(project, this);
	}

	protected void invokdEditTask(Runnable task)
	{
		CHSSwingUtils.invoke(task, true);
	}

	@Override protected BrowserTreeSelectionModel createBrowserSelectionModel()
	{
		return new LogicBrowserTree.LogicBrowserTreeSelectionModel(m_client, this)
		{
			@NotNull @Override protected IUID getTreeNodeUidFromDataModelUid(@NotNull IUID datamodelUid)
			{

				IUIDObject connectivityUID = ReferenceHelper.reduceToConnectivityObject(datamodelUid);

				return (connectivityUID != null ? connectivityUID.getUID() : datamodelUid);
			}
		};
	}

	protected void setTreeDirty(boolean dirty)
	{

		if (groupAttributeConfigurator != null && dirty) {

			groupAttributeConfigurator.reset();
		}

		super.setTreeDirty(dirty);
	}

	@Override public boolean isPopulateControllerActions(IBrowserTreeNode treeNode)
	{
		return checkLogicObjectNode(treeNode);
	}

	@Override public boolean isPopulateAppActions(IBrowserTreeNode treeNode)
	{
		return checkLogicObjectNode(treeNode);
	}

	private boolean checkLogicObjectNode(IBrowserTreeNode browserTreeNode)
	{
		if (browserTreeNode == null) {
			return false;
		}
		IUIDObject userObject = browserTreeNode.getUIDObject();
		return userObject instanceof ILogicObject;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{

		IBrowserTreeNode browserTreeNode = null;
		if (container instanceof BrowserActionContainer) {
			BrowserActionContainer browserActionContainer = (BrowserActionContainer) container;
			browserTreeNode = browserActionContainer.getContextNode();
		}
		if (browserTreeNode != null) {
			IUIDObject browserTreeObject = browserTreeNode.getUIDObject();
//			if (browserTreeObject instanceof IGroupAttributeAddendum) {
//
//				GroupAttributeAddHierarchyAction action = new GroupAttributeAddHierarchyAction(
//						new GroupAttributeAddHierarchyAction.AttributeParams(this));
//
//				container.add(new ActionEntry(action));
//				container.add(new ActionSeparator());
//				container.add(new ActionEntry(new CollapseTreeNodeAction(this, null)));
//				container.add(new ActionEntry(new CompletelyExpandTreeNodeAction(this, null)));
//				container.add(new ActionEntry(new CompletelyCollapseTreeNodeAction(this, null)));
//			}
			if (browserTreeObject instanceof GroupAttributeConfigurator.IGroupAttributeConfiguratorNode) {
				ActionUtilities.addTreeActions(container, this);
			}
		}
	}

	public Collection<IGroupAttributeAddEntry> getChildAttributesConfigured()
	{
		return groupAttributeConfigurator.getChildAttributesConfigured();
	}

	@Override protected void rebuild(Collection<IUID> newUIDs)
	{
		try {
			treePathNodeFinder = new TreePathNodeFinder();
			super.rebuild(newUIDs);
		}
		finally {

			treePathNodeFinder = null;
		}
	}

	@Nullable private ILogicDesign getDesign()
	{
		ICapletModel capletModel = m_client.getController().getCapletModel();
		if (capletModel instanceof ILogicModel) {
			return ((ILogicModel) capletModel).getDesign();
		}
		return null;
	}

	private static class TreePathNodeFinder
	{

		private Map<IBrowserTreeNode, TreeNodePathFinderDetail> detailsPerNode = new LinkedHashMap<>();

		void newChildCreated(IBrowserTreeNode parent, IBrowserTreeNode child)
		{
			TreeNodePathFinderDetail childDetails = detailsPerNode.get(parent);
			if (childDetails == null) {
				childDetails = new TreeNodePathFinderDetail();
			}
			detailsPerNode.put(parent, childDetails);
			childDetails.addChild(child);
		}

		@Nullable TreePath findPath(TreePath oldPath, IBrowserTreeNode root)
		{
			Object[] path = oldPath.getPath();

			return findPath(path, root);
		}

		@Nullable TreePath findPath(Object[] oldPath, IBrowserTreeNode subTreeNode)
		{
			IBrowserTreeNode currentNewTreeNode = subTreeNode;

			int index = 0;
			Object[] newPathNodes = new Object[oldPath.length];
			TreeNodePathFinderDetail details = null;
			for (Object oldTreeNode : oldPath) {

				if (oldTreeNode instanceof IBrowserTreeNode) {

					String oldTreeNodeValue = oldTreeNode.toString();

					if (details != null) {
						currentNewTreeNode = details.getChildBrowserTreeNode(oldTreeNodeValue);
					}

					if (currentNewTreeNode != null) {

						newPathNodes[index] = currentNewTreeNode;
						index++;
						details = detailsPerNode.get(currentNewTreeNode);
					}
					else {
						return null;
					}
				}
			}
			return new TreePath(Arrays.copyOf(newPathNodes, index));
		}
	}

	@Nullable protected TreePath createNewPathFromOld(int oldPathLength, @NotNull TreePath oldPath)
	{
		assert treePathNodeFinder != null;
		return treePathNodeFinder.findPath(oldPath, (IBrowserTreeNode) getModel().getRoot());
	}

	void expandAllNodesAddedAfterDrop(List<String> pathToDraggedNode,
			List<List<String>> childrenToBeExpanded)
	{
		JTree tree = this;
		Runnable expandChildren = new Runnable()
		{

			@Override public void run()
			{

				for (List<String> aChildToExpand : childrenToBeExpanded) {
					List<String> fullPath = new ArrayList<>(pathToDraggedNode.size() + aChildToExpand.size());
					fullPath.addAll(pathToDraggedNode);
					fullPath.addAll(aChildToExpand);

					fullPath.remove(0);
					IBrowserTreeNode currentSubtreeNode = (IBrowserTreeNode) tree.getModel().getRoot();

					expandPathAndParents(TreeUtils.getPath((DefaultMutableTreeNode) currentSubtreeNode));
					for (String aChildValue : fullPath) {

						currentSubtreeNode = findChildNode(currentSubtreeNode, aChildValue);
						if (currentSubtreeNode == null) {
							return;
						}
						expandPathAndParents(TreeUtils.getPath((DefaultMutableTreeNode) currentSubtreeNode));
					}
				}
			}
		};

		invokeExpansionMethodWithDefferredRendering(expandChildren);
	}

	@Nullable IBrowserTreeNode findChildNode(IBrowserTreeNode subTreeNode, String child)
	{

		int childCount = subTreeNode.getChildCount();
		for (int i = 0; i < childCount; i++) {
			TreeNode childTreeNode = subTreeNode.getChildAt(i);
			String childValue = subTreeNode.getChildAt(i).toString();
			if (childValue.equals(child)) {
				return (IBrowserTreeNode) childTreeNode;
			}
		}
		return null;
	}

	private static class TreeNodePathFinderDetail
	{

		private Map<String, IBrowserTreeNode> childNodes = new HashMap<>(1);

		void addChild(IBrowserTreeNode childNode)
		{

			childNodes.put(childNode.toString(), childNode);
		}

		IBrowserTreeNode getChildBrowserTreeNode(String text)
		{
			return childNodes.get(text);
		}
	}

	@Override public void stateChanged(ISharedObjectChangeEvent event)
	{
		Set<IUID> eventDetails = event.getDetails();
		if (eventDetails != null && !eventDetails.isEmpty()) {
			super.stateChanged(event);
		}
	}

	@Override public boolean isEventDependent()
	{
		return true;
	}

	protected void unregisterListeners()
	{
		super.unregisterListeners();
		IProject project = UIDMgr.getObjectOfType(projectUID, IProject.class);
		LogicBrowserTree.SharedObjectChangeListenerAdder.deRegisterForSharedObjectChanges(project, this);
	}

	public static class AttributeConfigured implements IGroupAttributeAddEntry
	{

		private Collection<String> seperator;

		private String name;

		private boolean isAttribute;

		public AttributeConfigured(String name, boolean isAttribute)
		{
			this.name = name;
			this.isAttribute = isAttribute;
		}

		@Override public String getDisplayName()
		{

			return isAttribute ? ResourceMgr.getString(AttributeType.class, "AttributeType." + name) : name;
		}

		@Override public String getName()
		{
			return name;
		}

		@Override public void addSeperator(String givenSeperator)
		{
			if (seperator == null) {
				seperator = new ArrayList<>();
			}
			seperator.add(givenSeperator);
		}

		@Override public Collection<String> seperator()
		{
			return seperator;
		}

		@Override public void setSeperator(Collection<String> seperators)
		{
			seperator = seperators;
		}

		public boolean isAttribute()
		{
			return isAttribute;
		}
	}
}
