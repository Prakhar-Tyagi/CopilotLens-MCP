package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ITransientDesignBrowserClient;
import chs.caf.caplet.helpers.assembly.AssemblyActionHelper;
import chs.caf.caplet.helpers.AssemblyObjectProxy;
import chs.caf.caplet.helpers.IAssemblyActionClient;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.helpers.browser.LockedLogicObjectNodeDimmer;
import chs.cof.draw.IColor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.ILogicObject;
import chs.cofUtils.logical.concurrency.IConcurrentEditReporter;
import chs.cofUtils.logical.concurrency.LogicConcurrentEditReporter;
import chs.common.IAssembledObject;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.DesignObjectHierarchyHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Frame;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateAssemblyActionHelper extends AssemblyActionHelper
{

	public CreateAssemblyActionHelper(Comparator<Object> comp,
			IAssemblyActionClient actionClient, ITransientDesignBrowserClient client,
			@NotNull DesignObjectHierarchyHelper designObjectHierarchyHelper)
	{
		super(comp, actionClient, client, designObjectHierarchyHelper);
	}

	@NotNull @Override protected CreateAssemblyDialog newAssemblyDialog(@NotNull Frame owner)
	{
		return new LogicCreateAssemblyDialog(owner, getModel(), getTreeBrowserClient());
	}

	@Override protected boolean supportsObjectLocking()
	{
		return true;
	}

	@Override protected boolean canAddInMultiUserMode(List<IBrowserTreeNode> selLeft, TreePath[] selRight)
	{
		for (IBrowserTreeNode treeNode : selLeft) {
			if (isTreeNodeLockedInAnotherSession(treeNode)) {
				return false;
			}
		}

		for (TreePath treePath : selRight) {
			AssemblyObjectProxy proxy = (AssemblyObjectProxy) treePath.getLastPathComponent();
			if (isTreeNodeLockedInAnotherSession(proxy)) {
				return false;
			}
		}
		return true;
	}

	@Override protected boolean canRemoveInMultiUserMode(TreePath[] selRight)
	{
		for (TreePath treePath : selRight) {
			AssemblyObjectProxy proxy = (AssemblyObjectProxy) treePath.getLastPathComponent();
			if (isTreeNodeLockedInAnotherSession(proxy)) {
				return false;
			}

			AssemblyObjectProxy parentNode = (AssemblyObjectProxy) proxy.getParent();
			if (parentNode != null && isTreeNodeLockedInAnotherSession(parentNode)) {
				return false;
			}
		}
		return true;
	}

	@Override protected boolean canDeleteInMultiUserMode(TreePath[] selRight)
	{
		for (TreePath treePath : selRight) {
			AssemblyObjectProxy proxy = (AssemblyObjectProxy) treePath.getLastPathComponent();
			if (isTreeNodeLockedInAnotherSession(proxy)) {
				return false;
			}

			AssemblyObjectProxy parentProxy = (AssemblyObjectProxy) proxy.getParent();
			if (parentProxy != null && isTreeNodeLockedInAnotherSession(parentProxy)) {
				return false;
			}

			IUIDObject iuidObject = proxy.getUIDObject();
			if (iuidObject instanceof IAssembly) {
				for (IAssembledObject assembledObject : ((IAssembly) iuidObject).getElements()) {
					if (assembledObject instanceof ILogicObject &&
							LogicObjectLockFinder.isLogicObjectLockedInOtherSession((ILogicObject) assembledObject)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override protected boolean changesCorrespondToCurrentSession(Collection<IUIDObject> changedObjectsToBeChecked)
	{
		// check that these changes are made in this session and are not a result of refresh (triggered by lock) by checking that the corresponding objects are locked
		return changedObjectsToBeChecked.stream()
				.filter(changedObject -> changedObject != null)
				.allMatch(LogicObjectLockFinder::isEditable);
	}

	protected boolean isTreeNodeLockedInAnotherSession(IBrowserTreeNode treeNode)
	{
		IUIDObject iuidObject = treeNode.getUIDObject();
		return (iuidObject instanceof ILogicObject &&
				LogicObjectLockFinder.isLogicObjectLockedInOtherSession((ILogicObject) iuidObject));
	}

	@Override protected boolean canProceedInMultiUserMode()
	{
		Map<IUIDObject, AssemblyHierarchyInfo> mapBeforeRefresh = new HashMap<IUIDObject, AssemblyHierarchyInfo>();
		Set<IAssembledObject> editedSet = new HashSet<IAssembledObject>();
		for (AssemblyObjectProxy addedAssembly : m_addedAssemblies) {
			for (int i = 0; i < addedAssembly.getChildCount(); i++) {
				AssemblyObjectProxy child = (AssemblyObjectProxy) addedAssembly.getChildAt(i);
				if (child.getUIDObject() instanceof IAssembledObject) {
					editedSet.add((IAssembledObject) child.getUIDObject());
				}
			}
		}

		for (AssemblyObjectProxy assemblyObjectProxy : m_modifiedAssemblies) {
			IAssembledObject assemblyObject = (IAssembledObject) assemblyObjectProxy.getUIDObject();
			editedSet.add(assemblyObject);

			editedSet.addAll(getModifiedObjects(assemblyObjectProxy));
		}

		for (AssemblyObjectProxy assemblyObjectProxy : m_removedAssemblies) {
			IAssembledObject assemblyObject = (IAssembledObject) assemblyObjectProxy.getUIDObject();
			editedSet.add(assemblyObject);
			if (assemblyObject instanceof IAssembly) {
				editedSet.addAll(((IAssembly) assemblyObject).getElements());
			}
			IAssembly parentAssembly = assemblyObject.getAssembly();
			if (parentAssembly != null) {
				editedSet.add(parentAssembly);
			}
		}

		for (IAssembledObject editedObject : editedSet) {
			mapBeforeRefresh.put(editedObject, new AssemblyHierarchyInfo(editedObject));
		}

		Collection<IUID> lockFailedUIDs = LogicObjectLockFinder.tryEdit((ILogicDesign) m_design, editedSet);

		if (lockFailedUIDs.isEmpty()) {
			return isTheStructureSameAfterRefresh(mapBeforeRefresh, editedSet);
		}
		else {
			String actionMessage = ResourceMgr
					.getString(CreateAssemblyActionHelper.class, "CreateAssemblyActionHelper.actionFailure.message");
			CreateMulticoreActionHelper
					.reportLockFailure((ILogicDesign) m_design, actionMessage, lockFailedUIDs,
							new LogicConcurrentEditReporter());
			return false;
		}
	}

	private Set<IAssembledObject> getModifiedObjects(AssemblyObjectProxy assemblyProxy)
	{
		Set<IAssembledObject> modifiedObjects = new HashSet<IAssembledObject>();

		List<AssemblyObjectProxy> assemblyProxyChildren = new LinkedList<AssemblyObjectProxy>();
		assemblyProxy.childrenVec(assemblyProxyChildren);

		// has any object been added to the tree
		for (AssemblyObjectProxy childProxy : assemblyProxyChildren) {
			AssemblyObjectProxy parentAssemblyProxy = getAssemblyParentProxy(childProxy);
			IAssembledObject childObject = (IAssembledObject) childProxy.getUIDObject();
			if (childObject != null && parentAssemblyProxy != null) {
				if (childObject.getAssembly() != parentAssemblyProxy.getUIDObject()) {
					modifiedObjects.add(childObject);
				}
			}
		}

		// has any object been moved out of the tree
		Set<IUIDObject> objectsInTree = assemblyProxyChildren.stream()
				.map(childProxy -> childProxy.getUIDObject())
				.collect(Collectors.toSet());

		IAssembly assemblyObject = (IAssembly) assemblyProxy.getUIDObject();
		assemblyObject.getElements().stream()
				.filter(assembledObject -> !(objectsInTree.contains(assembledObject)))
				.forEach(movedOutOfAssembly -> modifiedObjects.add(movedOutOfAssembly));

		return modifiedObjects;
	}

	private boolean isTheStructureSameAfterRefresh(Map<IUIDObject, AssemblyHierarchyInfo> mapBeforeRefresh,
			Set<IAssembledObject> editedSet)
	{
		for (IUIDObject editedObject : editedSet) {
			//after refresh we might (generally) get different uid object.
			IAssembledObject editObjAfterRefresh = (IAssembledObject) UIDMgr.getObject(editedObject.getUID());
			AssemblyHierarchyInfo hierarchyInfoAfterRefresh = new AssemblyHierarchyInfo(editObjAfterRefresh);
			if (!hierarchyInfoAfterRefresh.compare(mapBeforeRefresh.get(editedObject))) {
				String name = ((IReadOnlyNamedObject) editedObject).getName();
				IConcurrentEditReporter reporter = new LogicConcurrentEditReporter();
				String errorMessage = ResourceMgr.getString(CreateAssemblyActionHelper.class,
						"CreateAssemblyActionHelper.structureModified.message", name);
				reporter.report(HTMLHelper.color(IColor.RED, errorMessage));
				return false;
			}
		}
		return true;
	}

	public class LogicCreateAssemblyDialog extends CreateAssemblyDialog
	{

		LogicCreateAssemblyDialog(@NotNull Frame frame, @NotNull ICapletModel model,
				@NotNull ITransientDesignBrowserClient client)
		{
			super(frame, model, client);
			TreeCellRenderer renderer = getAssembliesTree().getCellRenderer();
			if (renderer instanceof CreateAssemblyTreeRenderer) {
				((CreateAssemblyTreeRenderer) renderer).setTreeNodeDimmer(LockedLogicObjectNodeDimmer.getInstance());
			}
		}
	}
}
