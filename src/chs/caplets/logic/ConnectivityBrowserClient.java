/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2023 Siemens
 */

package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IAssemblyIterator;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceIterator;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.INetConductorIterator;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISpliceIterator;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.IWireConductorIterator;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IAssembledObject;
import chs.common.ICHSIterator;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDObject;
import chs.images.CHSImageLoader;
import chs.utilities.BuildInfo;
import chs.utility.helpers.LibraryHelper;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConnectivityBrowserClient extends BrowserClientHelper
{

	private Reference m_model;
	private IUIDObject m_connectivities = null;
	private IUIDObject m_orphaned = null;

	private IUIDObject m_multicores = null;

	private List<IUID> m_children = null;

	private Map m_wedges;

	private Icon m_root_icon;

	private List m_orphanedDevices = null;

	@Nullable private List m_orphanedConnectors = null;

	@Nullable private List m_orphanedSplices = null;

	@Nullable private List m_orphanedNets = null;

	@Nullable private List m_orphanedWires = null;

	private boolean m_isOrphanedNode = false;

	private Comparator m_comp = null;

	private IUID currDesignUID = null;
	// private AssemblySearcher m_assemblySearcher;

	/**
	 * Implementation of the BrowserClient to supply information to the browser tree
	 */
	public ConnectivityBrowserClient(ICapletController cont)
	{
		super(cont);
		m_controller = cont;
		m_model = new WeakReference(cont.getCapletModel());
		//
		//setCurrentDesign();

		if (getModel() != null) {
			m_wedges = new HashMap();
			m_folderIcon = CHSImageLoader
					.loadImageIcon("chs/images/general/ico_folder.gif");
			m_root_icon = CHSImageLoader
					.loadImageIcon("chs/images/app/ico_diagram.gif");
			// create the top-level folders for devices, conductors, ...
			setRootObject(getModel().getDesign());
			// m_assemblySearcher = new
			// AssemblySearcher((ISchemDiagram)getRootObject());

			// setup the static children of this browser tree
			m_connectivities = createFolder("With Representation");
			m_orphaned = createFolder("Without Representation");
			m_children = new ArrayList<IUID>(5);
			m_children.add(m_connectivities.getUID());

			final boolean devExtensions = BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled();
			if (devExtensions) {
				m_children.add(m_orphaned.getUID());
			}

			m_comp = new NodeComparator();
		}
	}

	private Model getModel()
	{
		return (Model) m_model.get();
	}

	/**
	 * gdh 12/29/03 code copied from getIcon(); return tool tip text
	 */
	public String getToolTipText(IUID uid, IUID parentUID)
	{
		IUIDObject obj = getObject(uid);
		/*if (obj instanceof chs.cof.logical.cable.IPinList) {
			chs.cof.logical.cable.IPinList pinlist = (chs.cof.logical.cable.IPinList) obj;
			IAssembly ass = ((ILogicObject) pinlist).getAssembly();
			if (ass != null)
				return ass.getName(); // you're an ass
		}

		if (obj instanceof chs.cof.logical.cable.IConductor) {
			chs.cof.logical.cable.IConductor cableObj = ((chs.cof.logical.cable.IConductor) obj);
			IMulticore core = cableObj.getMulticore();
			if (core != null)
				return core.getName();

		}
		if (obj instanceof IMulticore) {
			IMulticore mc = (IMulticore) obj;
			IAssembly ass = mc.getAssembly();
			if (ass != null)
				return ass.getName();
		}
		if (obj instanceof IAssembly) {
			IAssembly assy = (IAssembly) obj;
			IAssembly parent = assy.getAssembly();
			if (parent != null) {
				return parent.getName();
			}
		}*/

		if (obj instanceof ILogicObject) {
			return uid.toString();
		}

		return null;
	}

	/**
	 * Icon based on type
	 */
	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{

		if (uid == getRoot()) {
			return m_root_icon;
		}

		Icon chosenIcon = null;
		IUIDObject obj = getObject(uid);

		if (obj instanceof ILogicObject) {
			chosenIcon = IconUtils.getIcon(obj);
		}
		else if (obj instanceof BrowserFolder) {
			// check for one of our folders
			chosenIcon = m_folderIcon;
		}
		else {
			throw new RuntimeException("Unexpected object type for icon: "
					+ getObject(uid));
		}

		return chosenIcon;
	}

	/**
	 * Default implementation just returns object from UIDmgr
	 */
	@Nullable
	public IUIDObject getObject(IUID uid)
	{
		IUIDObject uidObj = (IUIDObject) m_wedges.get(uid);
		if (uidObj != null) {
			return uidObj;
		}
		uidObj = super.getObject(uid);
		if (uidObj != null) {
			return uidObj;
		}
		uidObj = LibraryHelper.getLibraryInnerCore(uid);
		return uidObj;
	}

	/**
	 * handle double click
	 */
	public void activateObject(IUID uid)
	{
		IAction action = null;

		if (action != null && action.isEnabled()) {
			ActionEvent ae = new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, "doubleclick", 0);
			CAFUtils.getInstance().getActiveActionMgr().actionPerformed(action, ae);
		}
	}

	private List getPins(chs.cof.logical.cable.IConductor cond)
	{
		List vec = new ArrayList();
		for (IAbstractPinIterator apitr = cond.getPins(); apitr.hasNext(); ) {
			IAbstractPin child = apitr.getNext();
			vec.add(child.getUID());
		}
		return vec;
	}

/*	private List getPins(IDevice cond) {
		List vec = new ArrayList();
		for (IAbstractPinIterator apitr = cond.getPins(); apitr.hasNext();) {
			IAbstractPin child = apitr.getNext();
			vec.add(child.getUID());
		}
		return vec;
	}*/

	private List getPins(chs.cof.logical.cable.IPinList cond)
	{
		List vec = new ArrayList();
		for (IAbstractPinIterator apitr = cond.getPins(); apitr.hasNext(); ) {
			IAbstractPin child = apitr.getNext();
			vec.add(child.getUID());
		}

		if (cond instanceof IConnector) {
			IConnector connector = (IConnector) cond;
			IBackshell bshell = connector.getBackshell();
			if (bshell != null) {
				vec.add(bshell.getUID());
			}
		}
		return vec;
	}

/*	private List getPins(ISplice sp) {
		List vec = new ArrayList();
		for (IAbstractPinIterator apitr = sp.getPins(); apitr.hasNext();) {
			IAbstractPin child = apitr.getNext();
			vec.add(child.getUID());
		}
		return vec;
	}
*/

	private List getConductors(IAbstractPin pin)
	{
		List vec = new ArrayList();
		for (IConductorIterator apitr = pin.getConductors(); apitr.hasNext(); ) {
			chs.cof.logical.cable.IConductor child = apitr.getNext();
			vec.add(child.getUID());
		}
		Collection<IAbstractPin> connectedPins = pin.getConnectedPins();
		if (connectedPins != null) {
			for (IAbstractPin connectedPin : connectedPins) {
				vec.add(connectedPin.getOwner().getUID());
			}
		}
		return vec;
	}

	/**
	 * check to see if there are devices on this diagram.
	 */
	private boolean hasConnectivity()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			if (conn.hasPinLists() || conn.hasConductors()) {
				return true;
			}
		}
		return false;
	}

	private boolean hasDevices()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			return conn.getNumDevices() > 0;
		}
		return false;
	}

	private boolean hasConnectors()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.getNumConnectors() > 0;
		}
		return false;
	}

	private boolean hasMulticores()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.hasMulticores();
		}
		return false;
	}

	private boolean hasAssemblies()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.hasAssemblies();
		}
		return false;
	}

	private boolean hasBlockDevices()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.getNumBlockDevices() > 0;
		}
		return false;
	}

	private boolean hasWires()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.getNumWireConductors() > 0;
		}
		return false;
	}

	private boolean hasNets()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			return conn.getNumNetConductors() > 0;
		}
		return false;
	}

	private boolean hasSplices()
	{
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			return conn.hasSplices();
		}
		return false;
	}

	private boolean hasOrphanedConnectivity()
	{
		boolean orphanedChildren = false;
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			if (conn.getNumDevices() > 0) {
				Set set = new HashSet();
				for (IDeviceIterator devItr = getModel().getDesign()
						.getConnectivity().getDevices(); devItr.hasNext(); ) {
					IDevice device = devItr.getNext();
					boolean orphaned = true;
					for (ICHSIterator<ISchemDiagram> diagItr = getModel().getDesign()
							.getDiagrams(); diagItr.hasNext(); ) {
						ISchemDiagram diag = diagItr.getNext();
						if (diag.getRepresentations(device.getUID()).getSize() > 0) {
							orphaned = false;
							break;
						}
					}
					if (orphaned) {
						set.add(device.getUID());
					}
				}
				if (set.size() > 0) {
					m_orphanedDevices = new ArrayList(set);
					orphanedChildren = true;
				}
				else {
					m_orphanedDevices = null;
				}
			}

			if (conn.getNumConnectors() > 0) {
				Set set = new HashSet();
				for (IConnectorIterator itr = getModel().getDesign()
						.getConnectivity().getConnectors(); itr.hasNext(); ) {
					IConnector con = itr.getNext();
					boolean orphaned = true;
					for (ICHSIterator<ISchemDiagram> diagItr = getModel().getDesign()
							.getDiagrams(); diagItr.hasNext(); ) {
						ISchemDiagram diag = diagItr.getNext();
						if (diag.getRepresentations(con.getUID()).getSize() > 0) {
							orphaned = false;
							break;
						}
					}
					if (orphaned) {
						set.add(con.getUID());
					}
				}
				if (set.size() > 0) {
					m_orphanedConnectors = new ArrayList(set);
					orphanedChildren = true;
				}
				else {
					m_orphanedConnectors = null;
				}
			}

			if (conn.getNumNetConductors() > 0) {
				Set set = new HashSet();
				for (INetConductorIterator itr = getModel().getDesign()
						.getConnectivity().getNetConductors(); itr.hasNext(); ) {
					chs.cof.logical.cable.INetConductor con = itr.getNext();
					boolean orphaned = true;
					for (ICHSIterator<ISchemDiagram> diagItr = getModel().getDesign()
							.getDiagrams(); diagItr.hasNext(); ) {
						ISchemDiagram diag = diagItr.getNext();
						if (diag.getRepresentations(con.getUID()).getSize() > 0) {
							orphaned = false;
							break;
						}
					}
					if (orphaned) {
						set.add(con.getUID());
					}
				}
				if (set.size() > 0) {
					m_orphanedNets = new ArrayList(set);
					orphanedChildren = true;
				}
				else {
					m_orphanedNets = null;
				}
			}

			if (conn.getNumWireConductors() > 0) {
				Set set = new HashSet();
				for (IWireConductorIterator itr = getModel().getDesign()
						.getConnectivity().getWireConductors(); itr.hasNext(); ) {
					chs.cof.logical.cable.IWireConductor con = itr.getNext();
					boolean orphaned = true;
					for (ICHSIterator<ISchemDiagram> diagItr = getModel().getDesign()
							.getDiagrams(); diagItr.hasNext(); ) {
						ISchemDiagram diag = diagItr.getNext();
						if (diag.getRepresentations(con.getUID()).getSize() > 0) {
							orphaned = false;
							break;
						}
					}
					if (orphaned) {
						set.add(con.getUID());
					}
				}
				if (set.size() > 0) {
					m_orphanedWires = new ArrayList(set);
					orphanedChildren = true;
				}
				else {
					m_orphanedWires = null;
				}
			}

			if (conn.hasSplices()) {
				Set set = new HashSet();
				for (ISpliceIterator itr = getModel().getDesign().getConnectivity()
						.getSplices(); itr.hasNext(); ) {
					ISplice con = itr.getNext();
					boolean orphaned = true;
					for (ICHSIterator<ISchemDiagram> diagItr = getModel().getDesign()
							.getDiagrams(); diagItr.hasNext(); ) {
						ISchemDiagram diag = diagItr.getNext();
						if (diag.getRepresentations(con.getUID()).getSize() > 0) {
							orphaned = false;
							break;
						}
					}
					if (orphaned) {
						set.add(con.getUID());
					}
				}
				if (set.size() > 0) {
					m_orphanedSplices = new ArrayList(set);
					orphanedChildren = true;
				}
				else {
					m_orphanedSplices = null;
				}
			}
		}
		return orphanedChildren;
	}

	/**
	 * Return the devices referenced in this model
	 */
	private List getConnectivities()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			assert conn != null;
			if (conn.getNumDevices() > 0) {
				set.add(createFolder("Devices").getUID());
			}

			/*
					 * if(conn.getNumConductors () > 0) { set.add(createFolder("Nets")); }
					 */
			if (conn.getNumConnectors() > 0) {
				set.add(createFolder("Connectors").getUID());
			}

			/*
					 * if(conn.getNumGroundDevices() > 0) {
					 * set.add(createFolder("Devices")); }
					 */
			if (conn.getNumNetConductors() > 0) {
				set.add(createFolder("Nets").getUID());
			}

			if (conn.hasSplices()) {
				set.add(createFolder("Splices").getUID());
			}

			if (conn.getNumWireConductors() > 0) {
				set.add(createFolder("Wires").getUID());
			}

			if (conn.hasAssemblies()) {
				set.add(createFolder("Assemblies").getUID());
			}

			if (conn.hasMulticores()) {
				set.add(createFolder("Multicores").getUID());
			}
		}
		return new ArrayList(set);
	}

	private List getOrphanedConnectivity()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			IConnectivity conn = getModel().getDesign().getConnectivity();

			if (m_orphanedDevices != null) {
				set.add(createFolder("Orphaned Devices").getUID());
			}

			if (m_orphanedConnectors != null) {
				set.add(createFolder("Orphaned Connectors").getUID());
			}

			if (m_orphanedNets != null) {
				set.add(createFolder("Orphaned Nets").getUID());
			}

			if (m_orphanedSplices != null) {
				set.add(createFolder("Orphaned Splices").getUID());
			}

			if (m_orphanedWires != null) {
				set.add(createFolder("Orphaned Wires").getUID());
			}
		}
		return new ArrayList(set);
	}

	private List getDevices()
	{
		Set set = new HashSet();

		if (getModel() != null) {
			for (IDeviceIterator devItr = getModel().getDesign().getConnectivity()
					.getDevices(); devItr.hasNext(); ) {
				IDevice device = devItr.getNext();
				if (m_orphanedDevices == null) {
					set.add(device.getUID());
				}
				else if (!m_orphanedDevices.contains(device.getUID())) {
					set.add(device.getUID());
				}
			}
		}
		return new ArrayList(set);
	}

	private List getMulticores()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			for (IMulticoreIterator itr = getModel().getDesign().getConnectivity().getMulticores(); itr.hasNext(); ) {
				IMulticore mc = itr.getNext();
				set.add(mc.getUID());
			}
		}
		return new ArrayList(set);
	}

	private List getAssemblies()
	{
		Set set = new HashSet();

		if (getModel() != null) {
			for (IAssemblyIterator itr = getModel().getDesign().getConnectivity().getAssemblies(true);
					itr.hasNext(); ) {
				IAssembly assemb = itr.getNext();
				set.add(assemb.getUID());
			}
		}

		return new ArrayList(set);
	}

	private List getConnectors()
	{
		Set set = new HashSet();

		if (getModel() != null) {
			for (IConnectorIterator connItr = getModel().getDesign()
					.getConnectivity().getConnectors(); connItr.hasNext(); ) {
				IConnector connector = connItr.getNext();
				if (m_orphanedConnectors == null) {
					set.add(connector.getUID());
				}
				else if (!m_orphanedConnectors.contains(connector.getUID())) {
					set.add(connector.getUID());
				}
			}
		}
		return new ArrayList(set);
	}

	private List getWires()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			for (IWireConductorIterator wireItr = getModel().getDesign()
					.getConnectivity().getWireConductors(); wireItr.hasNext(); ) {
				IWireConductor wire = wireItr.getNext();
				if (m_orphanedWires == null) {
					set.add(wire.getUID());
				}
				else if (!m_orphanedWires.contains(wire.getUID())) {
					set.add(wire.getUID());
				}
			}
		}
		return new ArrayList(set);
	}

	private List getNets()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			for (INetConductorIterator netItr = getModel().getDesign()
					.getConnectivity().getNetConductors(); netItr.hasNext(); ) {
				INetConductor net = netItr.getNext();
				if (m_orphanedNets == null) {
					set.add(net.getUID());
				}
				else if (!m_orphanedNets.contains(net.getUID())) {
					set.add(net.getUID());
				}
			}
		}
		return new ArrayList(set);
	}

	private List getSplices()
	{
		Set set = new HashSet();
		if (getModel() != null) {
			for (ISpliceIterator spliceItr = getModel().getDesign()
					.getConnectivity().getSplices(); spliceItr.hasNext(); ) {
				ISplice splice = spliceItr.getNext();

				if (m_orphanedSplices == null) {
					set.add(splice.getUID());
				}
				else if (!m_orphanedSplices.contains(splice.getUID())) {
					set.add(splice.getUID());
				}
			}
		}
		return new ArrayList(set);
	}

	/**
	 * Return the conductors referenced in this model
	 */

	/**
	 * is this object selectable in the browser tree?
	 */
	public boolean isSelectable(IUID uid)
	{
		if (uid == getRoot()) {
			return true; // Can select the root! Properties on diagrams.
		}
		IUIDObject obj = getObject(uid);
		return !(obj instanceof BrowserFolder);
	}

	public List<IUID> getChildren(IUID uid)
	{
		if (uid == getRoot()) {
			return m_children;
		}
		IUIDObject obj = getObject(uid);
		List vecChildren = null;
		if (obj instanceof BrowserFolder) {
			if (obj == m_connectivities) {
				vecChildren = getConnectivities();
			}
			else if (obj == m_orphaned) {
				vecChildren = getOrphanedConnectivity();
			}
			else if ("Devices".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getDevices();
			}
			else if ("Wires".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getWires();
			}
			else if ("Nets".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getNets();
			}
			else if ("Splices".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getSplices();
			}
			else if ("Multicores".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getMulticores();
			}
			else if ("Assemblies".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getAssemblies();
			}
			else if ("Connectors".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = getConnectors();
			}
			else if ("Orphaned Devices".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = m_orphanedDevices;
			}
			else if ("Orphaned Wires".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = m_orphanedWires;
			}
			else if ("Orphaned Nets".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = m_orphanedNets;
			}
			else if ("Orphaned Splices".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = m_orphanedSplices;
			}
			else if ("Orphaned Connectors".equals(((IReadOnlyNamedObject) obj).getName())) {
				vecChildren = m_orphanedConnectors;
			}
		}
		else if (obj instanceof chs.cof.logical.cable.IPinList) {
			vecChildren = getPins((chs.cof.logical.cable.IPinList) obj);
		}
		else if (obj instanceof chs.cof.logical.cable.IConductor) {
			vecChildren = getPins((chs.cof.logical.cable.IConductor) obj);
		}/* else if (obj instanceof ISplice) {
			vecChildren = getPins((ISplice) obj);
		} */
		else if (obj instanceof IAbstractPin) {
			vecChildren = getConductors((IAbstractPin) obj);
		}
		else if (obj instanceof IAssembly) {
			vecChildren = getAssemblyElements((IAssembly) obj);
		}
		else if (obj instanceof IMulticore) {
			vecChildren = getMulticoreChildren((IMulticore) obj);
		}

		if (vecChildren == null) {
			return null;
		}
		// now sort the vector
		Collections.sort(vecChildren, m_comp);

		return vecChildren;
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		// return false;
		IUIDObject obj = getObject(uid);

		// first check for one of our folders
		if (obj instanceof BrowserFolder) {
			// now find out which type it is
			if (obj == m_connectivities) {
				return hasConnectivity();
			}
			else if (obj == m_orphaned) {
				return hasOrphanedConnectivity();
			}
			else if ("Devices".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasDevices();
			}
			else if ("Wires".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasWires();
			}
			else if ("Nets".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasNets();
			}
			else if ("Splices".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasSplices();
			}
			else if ("Connectors".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasConnectors();
			}
			else if ("Multicores".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasMulticores();
			}
			else if ("Assemblies".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasAssemblies();
			}
			else if ("Blocks".equals(((IReadOnlyNamedObject) obj).getName())) {
				return hasBlockDevices();
			}
			else if ("Orphaned Devices".equals(((IReadOnlyNamedObject) obj).getName())) {
				return m_orphanedDevices != null;
			}
			else if ("Orphaned Wires".equals(((IReadOnlyNamedObject) obj).getName())) {
				return m_orphanedWires != null;
			}
			else if ("Orphaned Nets".equals(((IReadOnlyNamedObject) obj).getName())) {
				return m_orphanedNets != null;
			}
			else if ("Orphaned Splices".equals(((IReadOnlyNamedObject) obj).getName())) {
				return m_orphanedSplices != null;
			}
			else if ("Orphaned Connectors".equals(((IReadOnlyNamedObject) obj).getName())) {
				return m_orphanedConnectors != null;
			}
		}
		else {
			if (obj instanceof chs.cof.logical.cable.IPinList) {
				Collection pins = getPins((chs.cof.logical.cable.IPinList) obj);
				if (pins.isEmpty()) {
					return false;
				}
				return true;
			} /*else if (obj instanceof IConnector) {
				Collection pins = getPins((IConnector) obj);
				if (pins.isEmpty()) {
					return false;
				}
				return true;
			} else if (obj instanceof ISplice) {
				Collection pins = getPins((ISplice) obj);
				if (pins.isEmpty()) {
					return false;
				}
				return true;
			} */
			else if (obj instanceof chs.cof.logical.cable.IConductor) {
				Collection pins = getPins((chs.cof.logical.cable.IConductor) obj);
				if (pins.isEmpty()) {
					return false;
				}
				return true;
			}
			else if (obj instanceof IAbstractPin) {
				Collection pins = getConductors((IAbstractPin) obj);
				if (pins.isEmpty()) {
					return false;
				}
				return true;
			}
			else if (obj instanceof IMulticore) {
				Collection children = getMulticoreChildren((IMulticore) obj);
				if (children.isEmpty()) {
					return false;
				}
				return true;
			}
			else if (obj instanceof IAssembly) {
				Collection children = getAssemblyElements((IAssembly) obj);
				if (children.isEmpty()) {
					return false;
				}
				return true;
			}
		}

		return false;
	}

	public List getPathFromRoot(IUID uid)
	{
		return null;
	}

	public IUID getUIDToReplace(IUID newuid)
	{
		return null;
	}

	public int newObjectExpansion(IUID newuid)
	{
		return EXPAND_NONE;
	}

	private List getMulticoreChildren(IMulticore mc)
	{
		List vec = new ArrayList();

		IMulticoreIterator iter = mc.getMulticores();
		while (iter.hasNext()) {
			IMulticore child = iter.getNext();
			vec.add(child.getUID());
		}

		IConductorIterator condIter = mc.getConductors();
		while (condIter.hasNext()) {
			chs.cof.logical.cable.IConductor child = condIter.getNext();
			vec.add(child.getUID());
		}

		// get the shield bodies
		IShieldBody sb = mc.getShieldBody();
		if (sb != null) {
			vec.add(sb.getUID());
		}

		return vec;
	}

	private List getAssemblyElements(IAssembly assem)
	{
		List vec = new ArrayList();
		for (IAssembledObject assembledObject : assem.getElements()) {
			if (assembledObject instanceof ILogicObject) {
				vec.add(assembledObject.getUID());
			}
		}

		return vec;
	}

	public UIDObject getConnectiviyUID(UIDObject uidObj)
	{

		return null;
	}

	@Override @Nullable public String doGetPresentationName(IUID uid)
	{
		StringBuffer objName = new StringBuffer(super.doGetPresentationName(uid));
		IUIDObject obj = getObject(uid);
		if (obj instanceof ILogicObject) {
			List objDiagram = CAFUtils.getInstance().getConnectivityOpenDiagrams((ILogicObject) obj);

			for (Iterator itr = objDiagram.iterator(); itr.hasNext(); ) {
				ISchemDiagram diag = (ISchemDiagram) itr.next();
				IDiagramObjectIterator diagObjItr = diag.getRepresentations(uid);
				if (diagObjItr.hasNext()) {
					objName.append(" (").append(diag.getName()).append(") ");
				}
			}
		}
		return objName.toString();
	}

	private class NodeComparator implements Comparator
	{

		private LogicObjectComparator lObjComparator = new LogicObjectComparator();

		public int compare(Object o1, Object o2)
		{
			IUIDObject lObj1 = getObject((IUID) o1);
			IUIDObject lObj2 = getObject((IUID) o2);

			if (lObj1 instanceof BrowserFolder && lObj2 instanceof BrowserFolder) {
				return (((IReadOnlyNamedObject) lObj1).getName()).compareTo(((IReadOnlyNamedObject) lObj2).getName());
			}

			/*if (lObj1 instanceof IBrowserMasquerade)
			{
				lObj1 = ((IBrowserMasquerade)lObj1).getRealObject();
			}
			if (lObj2 instanceof IBrowserMasquerade)
			{
				lObj2 = ((IBrowserMasquerade)lObj2).getRealObject();
			}*/
			return lObjComparator.compare(lObj1, lObj2);
		}
	}

	public boolean isDesignChanged()
	{
		if (getModel() == null) {
			return false;
		}
		return (currDesignUID != getModel().getDesign().getUID());
	}

	public void setCurrentDesign()
	{
		if (getModel() != null) {
			currDesignUID = getModel().getDesign().getUID();
		}
	}
}
