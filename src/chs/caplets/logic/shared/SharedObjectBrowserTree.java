/*
 * Copyright 2004-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IProjectChangeListener;
import chs.caf.IWindowMgr;
import chs.caf.ProjectChangeEvent;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.ViewChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionMgrListener;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.GenericBrowserTree;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.ui.form.filter.ISharedTreeFilterOption;
import chs.ctf.ui.form.filter.SharedObjectFilterOptionType;
import chs.ctf.ui.form.filter.SharedObjectTreeFilter;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ui.tree.FilterableTree;
import chs.utilities.ui.tree.IExclusiveOptionStyleTreeFilter;
import chs.utilities.ui.tree.IFilterableTreeModel;
import chs.utilities.ui.tree.ITreeFilter;
import chs.utilities.ui.tree.ITreeFilterOption;
import com.mentor.customLookandFeel.CustomFlatTreeUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ToolTipManager;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedObjectBrowserTree extends GenericBrowserTree implements IDisplayContextListener,
		IProjectChangeListener, IActionMgrListener
{

	private TreeUI m_treeUI;
	private boolean startDisplayOfTree;
	private boolean deferTreeBuild;
	private boolean rebuildCalled;
	private Set<IUID> affectedObjectUIDs = new HashSet<>();

	public SharedObjectBrowserTree(IBrowserClient client, String name)
	{
		super(client, name);
		m_treeUI = null;
		initFilters();
	}

	@Override protected void createTreeAndInitializeCellRenderers()
	{
		if (startDisplayOfTree) {
			super.createTreeAndInitializeCellRenderers();
		}
	}

	private void initFilters()
	{
		final SharedObjectTreeFilter sharedObjectFilter = new SharedObjectTreeFilter();
		sharedObjectFilter.initialize(getDesign(), getDefaultFilterOption());
		addFilter(sharedObjectFilter);
		// Set [a] filter as active.
		activateFilter(sharedObjectFilter);
		sharedObjectFilter.setOptionActive(sharedObjectFilter.getActiveOption());
	}

	protected SharedObjectFilterOptionType getDefaultFilterOption()
	{
		return SharedObjectFilterOptionType.NOFILTER;
	}

	protected void registerListeners()
	{
		// add this as a model change listener
		m_client.getController().getCapletModel().addModelChangeListener(this);
		ToolTipManager.sharedInstance().registerComponent(this);
		IWindowMgr windowManager = CAFUtils.getInstance().getWindowMgr();
		if (windowManager != null) {
			windowManager.addDisplayContextListener(this);
		}
		getBrowserClient().getController().getCaplet().getFIB().getProjectMgr().addProjectChangeListener(this);
	}

	protected void unregisterListeners()
	{
		m_client.getController().getCapletModel().removeModelChangeListener(this);
		ToolTipManager.sharedInstance().unregisterComponent(this);
		IWindowMgr windowManager = CAFUtils.getInstance().getWindowMgr();
		if (windowManager != null) {
			windowManager.removeDisplayContextListener(this);
		}
		getBrowserClient().getController().getCaplet().getFIB().getProjectMgr().removeProjectChangeListener(this);
	}

	private ILogicDesign getDesign()
	{
		Model model = (Model) getBrowserClient().getController().getCapletModel();
		return model.getDesign();
	}

	@Override public void destroy()
	{
		super.destroy();
		unregisterListeners();
	}

	@Override public boolean includeIfChildrenIncluded(Object obj)
	{
		return true;
	}

	@Override protected boolean doPrepareForFiltering()
	{
		boolean doPrepare = super.doPrepareForFiltering();
		if (doPrepare) {
			createChildNodes((DefaultMutableTreeNode) getModel().getRoot(), true, false);
			m_selectionModel.setSelectionFromMgr();
		}
		return doPrepare;
	}

	@NotNull @Override protected IFilterableTreeModel createFilterableTreeModel(final MutableTreeNode rootNode)
	{
		return new SharedObjectFilterableTreeModel(rootNode);
	}

	@Override protected void addChildren(IBrowserTreeNode childNode, IBrowserTreeNode parent)
	{
		if (someFilterEnabled()) {
			Collection<IUID> children = m_client.getChildren(childNode);
			addChildren(childNode, children);
		}
		else {
			super.addChildren(childNode, parent);
		}
	}

	protected TreeUI treeUIToSet(TreeUI uiToSet)
	{
		if (m_treeUI != uiToSet) {
			m_treeUI = treeUI();
		}
		return m_treeUI;
	}

	private TreeUI treeUI()
	{
		return new CustomFlatTreeUI()
		{
			/**
			 * Overridden here to avoid expansion of nodes on double click
			 *
			 * @param event The MouseEvent
			 *
			 * @return true iff the mouse event should trigger expand/collapse of the tree node
			 */
			@Override protected boolean isToggleEvent(MouseEvent event)
			{
				if (event.getClickCount() > 1) {
					// don't allow expansion or collapse of a tree node on double click if the object at node is shared object because here double click is for instantiation
					TreePath path = getPathForLocation(event.getX(), event.getY());
					if (path != null) {
						Object pathComponent = path.getLastPathComponent();
						if (pathComponent instanceof IBrowserTreeNode && m_client != null) {
							IUIDObject uidObj = m_client.getObject(((IBrowserTreeNode) pathComponent).getUID());
							if (uidObj instanceof IRevisionedSharedObject) {
								return false;
							}
						}
					}
				}
				return super.isToggleEvent(event);
			}
		};
	}

	@Override protected void filterUpdated(boolean userInput)
	{
		updateFilterUI();
	}

	@Override public void windowChanged(WindowChangeEvent wce)
	{
		ICAFWindow newWindow = wce.getNewWindow();
		if (newWindow instanceof ICapletWindow) {
			if (((ICapletWindow) newWindow).getController() == getBrowserClient().getController()) {
				ITreeFilterOption activeOption = getActiveFilterOption();
				if (activeOption != null && shouldRefreshTreeOnActiveWindowChange(activeOption)) {
					refreshTree();
				}
			}
		}
	}

	private boolean shouldRefreshTreeOnActiveWindowChange(ITreeFilterOption option)
	{
		return option instanceof ISharedTreeFilterOption &&
				((ISharedTreeFilterOption) option).shouldRefreshOnActiveWindowChange();
	}

	protected void refreshTree()
	{
		List<TreePath> expandedState = getCurrentlyExpandedPaths();
		if (mFilterableTreeModel != null) {
			mFilterableTreeModel.nodeStructureChanged((TreeNode) getModel().getRoot());
			expandPaths(expandedState);
		}
	}

	@Override public void postWindowChanged(WindowChangeEvent wce)
	{

	}

	@Override public void viewChanged(ViewChangeEvent vce)
	{

	}

	@Nullable private ITreeFilterOption getActiveFilterOption()
	{
		for (ITreeFilter filterOption : getActiveFilters()) {
			if (filterOption instanceof IExclusiveOptionStyleTreeFilter) {
				return ((IExclusiveOptionStyleTreeFilter) filterOption).getActiveOption();
			}
		}
		return null;
	}

	@Override public void projectChanged(ProjectChangeEvent e)
	{
		IProject project = e.getProject();
		if (project == null || getBrowserClient().getRoot() != project.getUID()) {
			return;
		}
		IBrowserTreeNode root = CommonUtils.cast(getModel().getRoot(), IBrowserTreeNode.class);
		if (root != null && projectNameEdited(root)) {
			root.resetDisplayName();
			refreshTree();
		}
		else if (shouldRefreshTree()) {
			refreshTree();
		}
	}

	private boolean projectNameEdited(@NotNull IBrowserTreeNode root)
	{
		IBrowserTreeNode newDummyNode = getBrowserClient().createTreeNode(getBrowserClient().getRoot(), null);
		return !root.toString().equals(newDummyNode.toString());
	}

	private boolean shouldRefreshTree()
	{
		return isShowActiveBuildListFilterOptionSelected() &&
				(getDesign() == FactoryMgr.getCAFUtils().getActiveDesignContainer());
	}

	protected void rebuild(Collection<IUID> newUIDs)
	{
		if (deferTreeBuild) {
			affectedObjectUIDs.addAll(newUIDs);
			rebuildCalled = true;
			return;
		}

		if (startDisplayOfTree) {
			super.rebuild(newUIDs);
		}
	}

	public void startDisplayOfTree()
	{

		if (!startDisplayOfTree) {
			startDisplayOfTree = true;
			createTreeAndInitializeCellRenderers();
		}
		startDisplayOfTree = true;
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
		if (startDisplayOfTree) {
			super.modelChanged(e);
		}
	}

	private boolean isShowActiveBuildListFilterOptionSelected()
	{
		ITreeFilterOption activeFilterOption = getActiveFilterOption();
		if (activeFilterOption != null) {
			String filterName = activeFilterOption.getFilterName();
			return SharedObjectFilterOptionType.ACTIVE_BUILDLIST_FILTER.getName().equals(filterName) ||
					SharedObjectFilterOptionType.ACTIVE_BUILDLISTANDDESIGNABASTRACTION_FILTER.getName().equals(
							filterName);
		}
		return false;
	}

	@Override public void activateEnded(IAction action, @Nullable IActionEnum status)
	{

	}

	@Override public void terminateStarted(IAction action)
	{
		deferTreeBuild = true;
		rebuildCalled = false;
		affectedObjectUIDs.clear();
	}

	@Override public void terminateEnded(IAction action, boolean status)
	{
		deferTreeBuild = false;
		if (rebuildCalled) {
			notifyTheTreeOfModelChange(new ArrayList<>(affectedObjectUIDs));
		}
		rebuildCalled = false;
		affectedObjectUIDs.clear();
	}

	public class SharedObjectFilterableTreeModel extends FilterableTree.FilterableTreeModel
	{

		public SharedObjectFilterableTreeModel(TreeNode root)
		{
			super(root);
		}

		@Override protected boolean includeFilteredInNode(Object obj, boolean examined)
		{
			if (examined) {
				return true;
			}

			// in the logic tree (and probably all BrowserTreeHelper trees?) we need to watch out for "dummy nodes"
			// we can't just expand everything behind the scenes because that may cause excesss loading of diagrams
			if (obj instanceof IBrowserTreeNode) {
				IBrowserTreeNode treeNode = (IBrowserTreeNode) obj;
				if (treeNode.getUID() == null) {
					return true;
				}
			}

			return super.includeFilteredInNode(obj, examined);
		}
	}

	@Override protected void handleLeftClick(MouseEvent e, TreePath selPath)
	{
		TreePath anchorPath = getAnchorSelectionPath();
		if (e.isShiftDown() && anchorPath != null && !e.isControlDown()) {
			SelectSet selections = getCurrentSelections();
			updateShiftSelectLMB(((IBrowserTreeNode) selPath.getLastPathComponent()).getUID(),
					selections);
		}
		addSelectionPath(selPath);
	}

	private void updateShiftSelectLMB(IUID uid, SelectSet selections)
	{

	}
}