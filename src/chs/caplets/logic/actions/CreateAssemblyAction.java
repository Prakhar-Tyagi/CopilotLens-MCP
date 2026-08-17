/*
 * Copyright 2003-2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.AssemblyObjectProxy;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.IAssemblyActionClient;
import chs.caf.caplet.helpers.browser.BrowserTreeHelperCellRenderer;
import chs.caf.caplet.helpers.browser.ConnectivityNameFilter;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.helpers.browser.LockedLogicObjectNodeDimmer;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.LogicObjectComparator;
import chs.caplets.logic.TransientLogicBrowserClient;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.common.IDesignContainer;
import chs.common.IDesignObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.ITreeSearchFilter;
import chs.utility.AssemblyAdapter;
import chs.utility.AssemblyPreferences;
import chs.utility.LogicDesignObjectHierarchyHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateAssemblyAction extends ControllerActionRT implements IAssemblyActionClient
{

	private ICapletModel m_model;
	private CreateAssemblyActionHelper m_helper = null;
	private ITreeSearchFilter m_connectivityNameFilter = null;
	private AssemblyProxyComparator m_assemblyProxyComparator = null;
	private TransientAssemblyClient m_browserClient = null;

	public CreateAssemblyAction(ICapletController controller)
	{
		super(controller);
		m_model = controller.getCapletModel();
	}

	public String getActionUIClass()
	{
		return CreateAssemblyActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		getDesign().refresh();
		m_browserClient = new TransientAssemblyClient();
		m_browserClient.setShowAssemblySubElements(false);
		m_browserClient.setObjectActivation(false);
		m_helper = new CreateAssemblyActionHelper(new AssemblyProxyComparator(), this, m_browserClient,
				new LogicDesignObjectHierarchyHelper());
		return m_helper.onActivate();
	}

	@Override public IDesignContainer getDesign()
	{
		return ((ILogicModel) m_model).getDesign();
	}

	@NotNull @Override public ICapletModel getCapletModel()
	{
		return m_model;
	}

	@Override public IConnectivity getConnectivity()
	{
		return ((ILogicModel) m_model).getDesign().getConnectivity();
	}

	public Set<IUID> getSelectedPhysicalObjects()
	{
		SelectSet sset = getController().getSelectMgr().getCurrentSelections();
		Set<IUID> physicalObjs = new HashSet<IUID>();
		if (sset.getSelectCount() > 0) {
			// First, squirrel away the UIDs of the objects in the list.
			// Now go through the selected objects...
			for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
				IUIDObject obj = iter.getNext();
				IUID uid = null;
				if (obj instanceof chs.cof.logical.schem.IConductor
						|| obj instanceof IPinList) {
					uid = ((IRepresentedObject) obj).getRawConnectivity().getUID();
				}
				else if (obj instanceof chs.cof.logical.schem.IShieldBody) {
					chs.cof.logical.schem.IShieldBody sb = (chs.cof.logical.schem.IShieldBody) obj;
					uid = sb.getConnectivity().getMulticore().getUID();
				}
				else if (obj instanceof IMulticore || obj instanceof IAssembly || obj instanceof IBackshell) {
					uid = obj.getUID();
				}
				if (uid != null) {
					physicalObjs.add(uid);
				}
			}
		}
		return physicalObjs;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful && m_helper.onTerminate(successful)) {
			return true;
		}

		return successful;
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	public void actionPerformed(ActionEvent e)
	{
		m_helper.actionPerformed(e);
	}

	public void destroy()
	{
		super.destroy();
		m_helper = null;
		m_model = null;
	}

	private class TransientAssemblyClient extends TransientLogicBrowserClient
	{

		private Map<IUID, AssemblyObjectProxy> uidVsTreeNode = new HashMap<IUID, AssemblyObjectProxy>();

		TransientAssemblyClient()
		{
			super(m_model.getController(), false);
		}

		@NotNull @Override public TreeCellRenderer createTreeCellRenderer()
		{
			TreeCellRenderer treeCellRenderer = super.createTreeCellRenderer();
			((BrowserTreeHelperCellRenderer) treeCellRenderer).setTreeNodeDimmer(
					LockedLogicObjectNodeDimmer.getInstance());
			return treeCellRenderer;
		}

		@NotNull public IBrowserTreeNode createTreeNode(IUID uid, IUID parentUID)
		{
			AssemblyObjectProxy oldNode = uidVsTreeNode.get(uid);
			IUIDObject object = super.getObject(uid);
			AssemblyObjectProxy objectProxy;
			if (object != null
					&& !IAbstractPin.class.isAssignableFrom(object.getClass())
					&& IDesignObject.class.isAssignableFrom(object.getClass())) {
				AssemblyObjectProxy.NODE_STATUS node_status = isPseudoParent(uid)
						? AssemblyObjectProxy.NODE_STATUS.EXCLUDED : AssemblyObjectProxy.NODE_STATUS.INCLUDED;
				objectProxy = new AssemblyObjectProxy(object, uid, this, node_status, null);
			}
			else {
				objectProxy = new AssemblyObjectProxy("", uid, this, AssemblyObjectProxy.NODE_STATUS.PLACE_HOLDER
						, null);
			}

			objectProxy.copyNameInfoFrom(oldNode);
			uidVsTreeNode.put(uid, objectProxy);
			return objectProxy;
		}

		public IBrowserTreeNode createAssemblyTreeNode(IUID assemblyUid, IUID uid, JTree tree)
		{
			return createTreeNode(uid, null);
		}

		public void updateRenamedNestedAssemblies(Collection<AssemblyObjectProxy> renamedAssemblies)
		{
			for (AssemblyObjectProxy objProxy : renamedAssemblies) {
				uidVsTreeNode.put(objProxy.getUID(), objProxy);
			}
		}
	}

	public String getText(AssemblyObjectProxy node)
	{
		String name = "";

		if (node.isAnyAssemblyType()) {
			name = node.getName();
		}

		String title = "";
		if (node.isAnyAssemblyType()) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Assembly.text");
		}
		else if (node.getUIDObject() instanceof INetConductor) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Net.text");
		}
		else if (node.getUIDObject() instanceof IWireConductor) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Wire.text");
		}
		else if (node.getUIDObject() instanceof IShieldConductor) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Shield.text");
		}
		else if (node.getUIDObject() instanceof IDevice) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Device.text");
		}
		else if (node.getUIDObject() instanceof IInlinePlugConnector) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.InlinePlug.text");
		}
		else if (node.getUIDObject() instanceof IInlineJackConnector) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.InlineJack.text");
		}
		else if (node.getUIDObject() instanceof IPlugConnector) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Plug.text");
		}
		else if (node.getUIDObject() instanceof IJackConnector) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Receptacle.text");
		}
		else if (node.getUIDObject() instanceof ISplice) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Splice.text");
		}
		else if (node.getUIDObject() instanceof IMulticore) {
			title = ResourceMgr.getString(CreateAssemblyAction.class, "CreateAssemblyAction.Term.Group.text");
		}
		return title + " " + name;
	}

//***************************************************************************

	class AssemblyProxyComparator extends LogicObjectComparator
	{
		private Map<ILogicObject, String> nameCache = new HashMap<>();
		private Map<Object, Integer> ordinalCache = new HashMap<>();

		private AssemblyAdapter.AssemblyDummy m_dummy1 = new AssemblyAdapter.AssemblyDummy();
		private AssemblyAdapter.AssemblyDummy m_dummy2 = new AssemblyAdapter.AssemblyDummy();

		public int compare(Object o1, Object o2)
		{
			m_dummy1.setName(null);
			m_dummy2.setName(null);
			return super.compare(o1, o2);
		}

		@NotNull @Override protected String getName(ILogicObject object)
		{
			if(nameCache.containsKey(object)){
				return nameCache.get(object);
			}
			String objectName = super.getName(object);
			nameCache.put(object, objectName);
			return objectName;
		}

		@Override protected int getOrdinal(@NotNull Object object, @Nullable ILogicObject logicObject)
		{
			if(ordinalCache.containsKey(object)){
				return ordinalCache.get(object);
			}

			int ordinal =  super.getOrdinal(object, logicObject);
			ordinalCache.put(object, ordinal);
			return ordinal;
		}

		protected ILogicObject getLogicObject(Object obj)
		{
			AssemblyObjectProxy proxy = (AssemblyObjectProxy) obj;

			if (proxy.getUIDObject() != null) {
				obj = proxy.getUIDObject();
			}
			else if (!m_dummy1.isNameSet()) {
				m_dummy1.setName(proxy.getName());
				obj = m_dummy1;
			}
			else {
				m_dummy2.setName(proxy.getName());
				obj = m_dummy2;
			}
			return super.getLogicObject(obj);
		}

		protected int getOrdinalForType(ILogicObject lObj)
		{
			// Type order:
			// Assembly
			// Device
			// Plug
			// Jack
			// Inline
			// Splice
			// Backshell
			// Pin
			// Interconnect
			// Net
			// Wire
			// Shield
			// Highway
			// Multicore
			// null (gets passed here for other stuff like folders)

			// NOTE: the numbers don't matter here, just the order
			if (lObj == null) {
				// TODO jacobt FEAT13040 : Non LogicObjects now appear last after sorting - check other clients OK with this
				// changed in 2008.2 to appear last instead of first (-1)
				//noinspection MagicNumber
				return 15;
			}

			else if (lObj instanceof IMulticore) {
				//noinspection MagicNumber
				return 14;
			}
			else if (lObj instanceof IHighway) {
				//noinspection MagicNumber
				return 13;
			}
			else if (lObj instanceof IShieldConductor) {
				//noinspection MagicNumber
				return 12;
			}
			else if (lObj instanceof IWireConductor) {
				return 11;
			}
			else if (lObj instanceof INetConductor) {
				return 10;
			}
			else if (lObj instanceof IInterconnectConductor) {
				return 9;
			}
			else if (lObj instanceof IShieldBody) {
				// these should be the first child below a multicore node in the Logic Design Browser
				return 8;
			}
			else if (lObj instanceof IAbstractPin) {
				return 7;
			}
			else if (lObj instanceof IBackshell) {
				return 6;
			}
			else if (lObj instanceof ISplice) {
				return 5;
			}
			else if (lObj instanceof IInlinePlugConnector || lObj instanceof IInlineJackConnector) {
				return 4;
			}
			else if (lObj instanceof IJackConnector) {
				return 3;
			}
			else if (lObj instanceof IPlugConnector) {
				return 2;
			}
			else if (lObj instanceof IDevice) {
				return 1;
			}
			else if (lObj instanceof IAssembly) {
				//noinspection MagicNumber
				return 0;
			}

			assert false : "Unknown logic object : " + lObj;
			return -1;
		}
	}

	public List<AssemblyObjectProxy> deleteAssembly(Collection<AssemblyObjectProxy> removedAssemblies)
	{
		List<AssemblyObjectProxy> deletedProxies = new LinkedList<AssemblyObjectProxy>();
		List<ILogicObject> changeNameSpaceObjects = new ArrayList<ILogicObject>(removedAssemblies.size());
		for (AssemblyObjectProxy delProxy : removedAssemblies) {
			//Check if there usages of this cable assembly in other diagram
			ISchemDiagram activeDiagram = ((ILogicModel) m_model).getDiagram();
			IDesignWideUsageMgr dwum = activeDiagram.getDesign().getDesignWideUsageMgr();
			IAssembly delAsm = (IAssembly) delProxy.getUIDObject();
			boolean deleteAssembly = !dwum.usedOnAnotherDiagram(delAsm, activeDiagram);
			for (IDiagramObject schemAssembly : activeDiagram.getRepresentations(delAsm.getUID())) {
				schemAssembly.delete();
			}
			if (!deleteAssembly) {
				continue;
			}
			deletedProxies.remove(delProxy);
			for (int idx = 0; idx < delProxy.getChildCount(); idx++) {
				AssemblyObjectProxy childProxy = (AssemblyObjectProxy) delProxy.getChildAt(idx);
				if (!childProxy.isNewAssembly()) {
					ILogicObject lObj = (ILogicObject) childProxy.getUIDObject();
					INameMgr nameMgr = lObj.getNameMgr();
					nameMgr.removeObject(lObj);
					changeNameSpaceObjects.add(lObj);
				}
			}

			delAsm.delete();
			getConnectivity().removeAssembly(delAsm);
		}

		for (Object changeNameSpaceObject : changeNameSpaceObjects) {
			ILogicObject lObj = (ILogicObject) changeNameSpaceObject;
			INameMgr nameMgr = lObj.getNameMgr();
			nameMgr.addObject(lObj);
		}
		return deletedProxies;
	}

	public List<Pair<IUID, Boolean>> getAssemblyPrefs(IProject project, List<IUIDObject> objLists)
	{
		return AssemblyPreferences.getLogicAssemblyPrefs(project, objLists);
	}

	public ITreeSearchFilter getTreeSearchFilter()
	{
		if (m_connectivityNameFilter == null) {
			m_connectivityNameFilter = new ConnectivityNameFilter();
		}
		return m_connectivityNameFilter;
	}

	public boolean isAllowDuplicateAssemblies()
	{
		return true;
	}

	public void updateNameCacheOnAssemblies(Collection<AssemblyObjectProxy> renamedAssemblies)
	{
		m_browserClient.updateRenamedNestedAssemblies(renamedAssemblies);
	}

	public boolean isEmptyAssembliesAllowed()
	{
		return true;
	}

	@Override public IBaseDiagram getDiagram()
	{
		return ((ILogicModel) getController().getCapletModel()).getDiagram();
	}
}