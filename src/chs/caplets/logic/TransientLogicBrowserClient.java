/*
 * Copyright 2011-2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ITransientDesignBrowserClient;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.browser.TransientBrowserClientDelegate;
import chs.cof.logical.IInternalPositionBase;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.IInternalPositionsContainerBase;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IRingTerminal;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.ISplice;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.utility.LogicDesignObjectHierarchyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeCellRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: brangan Date: Apr 28, 2011 Time: 3:21:54 PM To change this template use File |
 * Settings | File Templates.
 */
public class TransientLogicBrowserClient extends BrowserClient implements IModelChangeListener,
		ITransientDesignBrowserClient
{

	private TransientBrowserClientDelegate m_browserClinetDelegate = null;
	private Model m_model;
	private boolean m_isShowPins = true;
	private boolean m_isShowMulticoreChildren = false;
	private boolean m_isShowShieldBody = false;
	private boolean m_isShowAssemblyChildren = false;
	private boolean m_isShowConductorChildren = false;
	private boolean m_isAllowObjSelectionOnDiagram = false;
	private boolean m_isRemoveRepresentedObject = true;
	private boolean m_isEnabledObjectActivation = true;

	/**
	 * Implementation of the BrowserClient to supply information to the browser tree
	 *
	 * @param controller - controller
	 * @param isShowPins - true to show pins in the tree
	 */
	public TransientLogicBrowserClient(ICapletController controller, boolean isShowPins)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_isShowPins = isShowPins;
		init();
	}

	private void init()
	{
		m_browserClinetDelegate = new TransientBrowserClientDelegate(this, new LogicDesignObjectHierarchyHelper());
		setRootObject(m_model.getDesign());
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
		m_browserClinetDelegate.modelChanged(e);
	}

	public int newObjectExpansion(IUID newuid)
	{
		return EXPAND_COMPLETELY_AND_SELECT_SELF;
	}

	@Nullable public List<IUID> getPathFromRoot(IUID uid)
	{
		// Is there a direct parent folder?
		IUIDObject parentFolder = getParentFolderUID(uid);

		if (uid != null && parentFolder != null) {
			// Design node
			List<IUID> path = new ArrayList<IUID>(3);
			path.add(getRoot());
			path.add(parentFolder.getUID());
			path.add(uid);
			return path;
		}

		return null;
	}

	@Nullable private IUIDObject getParentFolderUID(IUID uid)
	{
		IUIDObject obj = getObject(uid);
		LogicFolder folder = getLogicFolderForObject(obj);
		if (folder != null && folder == LogicFolder.ASSEMBLY) {
			return getObject(getRoot());
		}

		if (obj instanceof IAssembly) {
			return getObjectFolder(LogicFolder.ASSEMBLY);
		}

		return null;
	}

	protected void obtainSkippedFolders()
	{
		m_skippedFolders.add(LogicFolder.HIGHWAYS.getDisplayName());
		m_skippedFolders.add(LogicFolder.SINGLE_LINES.getDisplayName());
		m_skippedFolders.add(LogicFolder.LOGIC_BLOCKS.getDisplayName());
		m_skippedFolders.add(LogicFolder.MOUNTINGRAIL.getDisplayName());
		m_skippedFolders.add(LogicFolder.WIREDUCT.getDisplayName());
		m_skippedFolders.add(LogicFolder.OTHERCOMPONENT.getDisplayName());
	}

	protected List<IUID> getMulticores(boolean overbraid)
	{
		List<IUID> list = overbraid? super.getOverBraids() : super.getMulticores();
		List<IUID> unsharedList = removeSharedMulticores(list, overbraid);
		List<IUID> result = !overbraid ? m_designObjectHierarchyHelper.removeInnerMultiCoresOfOverBraid(unsharedList)
				: unsharedList;
		return m_browserClinetDelegate.getResultantObjects(result, IMulticore.class);
	}

	protected List<IUID> getDevices(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getDevices(false), super.getUnusedDevices());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IDevice.class);
	}

	protected List<IUID> getBlockDevices(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getBlockDevices(false), super.getUnusedBlockDevices());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IBlockDevice.class);
	}

	protected List<IUID> getFunctionComponents(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getFunctionComponents(false), super.getUnusedFunctionComponents());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IFunction.class);
	}

	protected List<IUID> getFunctionConductors(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getFunctionConductors(false), super.getUnusedFunctionConductors());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IFunctionConductor.class);
	}

	protected List<IUID> getConnectors(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getConnectors(false), super.getUnusedConnectors());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IConnector.class);
	}

	protected List<IUID> getRingTerminals(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getRingTerminals(false), super.getUnusedRingTerminals());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IRingTerminal.class);
	}

	protected List<IUID> getSplices(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getSplices(false), super.getUnusedSplices());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), ISplice.class);
	}

	protected List<IUID> getConductors(boolean isCreateUnusedFolder)
	{
		List<IUID> conductors = getCombinedList(super.getConductors(false), super.getUnusedConductors());
		m_browserClinetDelegate.removeMulticoreConductors(conductors);
		return m_browserClinetDelegate.getResultantObjects(removeSharedConductors(conductors), IConductor.class);
	}

	private List<IUID> getCombinedList(List<IUID> list1, List<IUID> list2)
	{
		List<IUID> result = new ArrayList<IUID>(list1.size() + list2.size());
		result.addAll(list1);
		result.addAll(list2);

		return result;
	}

	private List<IUID> removeSharedPinLists(List<IUID> objs)
	{
		Set<IPinList> listSet = UIDUtils.convertToObjectSet(objs, IPinList.class);
		List<IUID> returnList = new ArrayList<IUID>(objs.size());
		for (IPinList obj : listSet) {
			if (obj.getSharedPinList() == null) {
				returnList.add(obj.getUID());
			}
		}
		return returnList;
	}

	private List<IUID> removeSharedConductors(List<IUID> objs)
	{
		Set<IConductor> listSet = UIDUtils.convertToObjectSet(objs, IConductor.class);
		List<IUID> returnList = new ArrayList<IUID>(objs.size());
		for (IConductor obj : listSet) {
			if (!obj.isShared()) {
				returnList.add(obj.getUID());
			}
		}
		return returnList;
	}

	@Override public void filterPseudoParents(Collection<IUID> uids)
	{
		m_browserClinetDelegate.filterPseudoParents(uids);
	}

	public boolean isPseudoParent(IUID uid)
	{
		return m_browserClinetDelegate.isPesudoParent(uid);
	}

	@NotNull public TreeCellRenderer createTreeCellRenderer()
	{
		return m_browserClinetDelegate.createTreeCellRenderer();
	}

	public void setShowAssemblySubElements(boolean show)
	{
		m_browserClinetDelegate.setShowAssemblySubElements(show);
	}

	@Nullable protected List<IUID> getBackshellChildren(IBackshell backshell)
	{
		if (m_isShowPins) {
			super.getBackshellChildren(backshell);
		}
		return Collections.emptyList();
	}

	@Nullable protected List<IUID> getChildren(IPinList pl)
	{
		Set<Class<? extends IUIDObject>> types = new HashSet<Class<? extends IUIDObject>>();
		types.add(IBackshell.class);

		List<IUID> inputList = getPinlistChildren(pl, false);
		addSchemPinListChildren(inputList);
		removeRepresentedObjects(inputList);

		return m_browserClinetDelegate.getResultantObjects(inputList, types);
	}

	protected List<IUID> getChildren(chs.cof.logical.schem.IPinList pl)
	{
		List<IUID> uidChildren = new LinkedList<IUID>();
		if (m_isShowPins) {
			//Todo: implement On Demand.
		}
		IPinList cpl = pl.getConnectivity();
		if (cpl instanceof IConnector) {
			IBackshell backshell = ((IConnector) cpl).getBackshell();
			if (backshell != null) {
				uidChildren.add(backshell.getUID());
			}
			for (IInternalPositionedObject poObj : ((IConnector) cpl).getPositionedObjects()) {
				uidChildren.add(poObj.getUID());
			}
		}

		return uidChildren;
	}

	protected <T extends IInternalPositionBase> Collection<IUID> getPositionsOrPositionedObjects(
			IInternalPositionsContainerBase<T> positionContainer)
	{
		List<IUID> positionedObjs = new LinkedList<IUID>(super.getPositionsOrPositionedObjects(positionContainer));
		return m_browserClinetDelegate.getResultantObjects(positionedObjs, IInternalPositionedObject.class);
	}

	protected List<IUID> getAssemblies()
	{
		return m_browserClinetDelegate.getResultantObjects(super.getAssemblies(), IAssembly.class);
	}

	@Nullable public List<IUID> getAssembledChildren(IUID assemblyUid, IUID uid)
	{
		if (m_isShowAssemblyChildren) {
			// Todo - implement on demand.
		}
		return Collections.emptyList();
	}

	protected List<IUID> getAssemblyElements(IUIDObject obj)
	{
		return m_browserClinetDelegate.getResultantAssemblyChildren(super.getAssemblyElements(obj));
	}

	public List<IUID> getMulticoreChildren(IMulticore mc)
	{
		List<IUID> mcChildren = Collections.emptyList();
		if (m_isShowMulticoreChildren) {
			// Todo - implement on demand.
		}
		else {
			if (mc instanceof IOverbraid) {
				List<IUID> list = super.getMulticoreChildren(mc);
				IShieldBody shieldBody = mc.getShieldBody();
				if (shieldBody != null && !m_isShowShieldBody) {
					IUID uid = shieldBody.getUID();
					list.remove(uid);
				}
				Set<Class<? extends IUIDObject>> types = new HashSet<Class<? extends IUIDObject>>();
				types.add(IPhysicalConductor.class);
				types.add(IShieldBody.class);
				types.add(IMulticore.class);
				types.add(INetConductor.class);

				mcChildren = m_browserClinetDelegate.getResultantObjects(list, types);
			}
		}

		return mcChildren;
	}

	@Nullable protected List<IUID> getChildren(IConductor conductor)
	{
		if (m_isShowConductorChildren) {
			// Todo - implement on demand.
		}
		return Collections.emptyList();
	}

	protected List<IUID> getInlines(boolean isCreateUnusedFolder)
	{
		List<IUID> list = getCombinedList(super.getInlines(false), super.getUnusedInlines());
		return m_browserClinetDelegate.getResultantObjects(removeSharedPinLists(list), IConnector.class);
	}

	protected boolean hasChildren(IConductor conductor)
	{
		if (m_isShowConductorChildren) {
			// Todo - implement on demand.
		}
		return false;
	}

	protected boolean hasBackshellChildren(IBackshell backshell)
	{
		if (m_isShowPins) {
			// Todo - implement on demand.
		}

		return false; // backshell with no pins never has children
	}

	protected boolean hasAssemblyChildren(IUIDObject obj)
	{
		if (m_isShowAssemblyChildren) {
			// Todo - implement on demand.
		}
		return false;
	}

	protected boolean hasChildren(IPinList pl, IUID parentUID)
	{
		if (m_isShowPins) {
			// Todo - implement on demand.
		}
		if (IConnector.class.isAssignableFrom(pl.getClass())) {
			return ((IConnector) pl).getBackshell() != null || !((IConnector) pl).getPositionedObjects().isEmpty();
		}
		return false;
	}

	public boolean isAllowSelectionOnDiagram()
	{
		if (!m_isAllowObjSelectionOnDiagram) {
			// Todo: implement on Demand
		}
		return false;
	}

	protected void removeRepresentedObjects(Collection<IUID> collection)
	{
		if (m_isRemoveRepresentedObject) {
			super.removeRepresentedObjects(collection);
		}
		else {
			// Todo : implement on demand
		}
	}

	protected boolean hasAssemblies()
	{
		List<IUID> list = m_browserClinetDelegate.getResultantObjects(super.getAssemblies(), IAssembly.class);
		return list != null && !list.isEmpty();
	}

	private List<IUID> removeSharedMulticores(List<IUID> objs, boolean overbraid)
	{
		Set<IMulticore> listSet = UIDUtils.convertToObjectSet(objs, IMulticore.class);
		List<IUID> returnList = new ArrayList<IUID>(objs.size());
		for (IMulticore obj : listSet) {
			if (overbraid) {
				if (IOverbraid.class.isAssignableFrom(obj.getClass())
						&& ((IOverbraid) obj).getSharedOverbraid() == null) {
					returnList.add(obj.getUID());
				}
			}
			else if (obj.getSharedMulticore() == null) {
				returnList.add(obj.getUID());
			}
		}
		return returnList;
	}

	public String getAssemblyFolderName()
	{
		return LogicFolder.ASSEMBLY.getDisplayName();
	}

	public void addNewObject(IUIDObject uidObj)
	{
		m_browserClinetDelegate.addNewObject(uidObj);
	}

	/**
	 * Handle a double click of object represented by uid
	 * <p/>
	 *
	 * @param uid The uid of the object at the node double-clicked
	 */
	@SuppressWarnings({"NoopMethodInAbstractClass"})
	public void activateObject(IUID uid)
	{
		if (m_isEnabledObjectActivation) {
			super.activateObject(uid);
		}
	}

	/**
	 * Allow Editing of objects using edit dialog. e.g double click on an object
	 */
	public void setObjectActivation(boolean isEnabledObjectActivation)
	{
		m_isEnabledObjectActivation = isEnabledObjectActivation;
	}
}
