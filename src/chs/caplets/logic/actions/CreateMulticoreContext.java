package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.helpers.MCProxy;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.IndicatorRefresher;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedFactory;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedOverbraidIterator;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.DiagramRepresentationUpdater;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.cog.PersistenceLockFailureCheckedException;
import chs.cog.PersistenceStateException;
import chs.common.IDesignContainer;
import chs.common.IGuard;
import chs.common.INamedPropertiedObject;
import chs.common.IStringIterator;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.dataservices.CapitalDataServices;
import chs.dataservices.LightWeightUsage;
import chs.dataservices.SharedObjectUsageInfo;
import chs.system.FactoryMgr;
import chs.system.IMemoryManager;
import chs.system.IProjectMemorySnapshot;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DiagramHelper;
import chs.utility.Placement;
import chs.utility.Replicator;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.MulticoreUtils;
import chs.utility.persist.promise.IPromise;
import chs.utility.persist.promise.PromiseFactory;
import chs.utility.project.LogicDesignPromiseHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CreateMulticoreContext
{

	public static final int MAX_MESSAGE_BODY_LENGTH = 40;
	private IProject m_project;
	private ILogicDesign m_design;
	private ISchemDiagram m_diagram;
	private MCProxy m_root;
	private int m_editScope = MulticoreEditPanel.LOCAL_SCOPE;
	private COFTypeEnum m_editType = COFTypeEnum.Multicore;
	private ILibraryPartSelection m_libObj;
	private Map<MCProxy, ILibraryPartSelection> proxyLibMCmap = new HashMap<MCProxy, ILibraryPartSelection>();
	private Map<MCProxy, ILibraryInnerCore> proxylibMCInnerCoreMap = new HashMap<MCProxy, ILibraryInnerCore>();
	@NotNull protected SchematicIndicatorUpdateProcessor schematicUpdateProcessor;

	public CreateMulticoreContext(ILogicDesign design, ISchemDiagram diagram)
	{
		init(design, diagram);
	}

	public enum MCAcceptenceResult
	{
		ACCEPTABLE,
		FROZENSHARED,
		OVERBRAIDCHILD,
		ASSEMBLYCHILD,
		SHIELDCOND,
		INTERCONNECTCOND,
		UNKNOWNERROR,
		INNERMC
	}

	public void init(ILogicDesign design, ISchemDiagram diagram)
	{
		m_design = design;
		m_diagram = diagram;
		m_project = m_design.getProject();
		schematicUpdateProcessor = new SchematicIndicatorUpdateProcessor();
	}

	public ILogicDesign getDesign()
	{
		return m_design;
	}

	public ISchemDiagram getDiagram()
	{
		return m_diagram;
	}

	public int getEditScope()
	{
		return m_editScope;
	}

	public boolean isLocalEditScope()
	{
		return (m_editScope == MulticoreEditPanel.LOCAL_SCOPE);
	}

	public boolean isSharedEditScope()
	{
		return (m_editScope == MulticoreEditPanel.SHARED_SCOPE);
	}

	public IProject getProject()
	{
		return m_project;
	}

	public MCProxy getProxyRoot()
	{
		return m_root;
	}

	public COFTypeEnum getEditType()
	{
		return m_editType;
	}

	public boolean isOverbraidEditType()
	{
		return (m_editType == COFTypeEnum.Overbraid);
	}

	public boolean isMulticoreEditType()
	{
		return (m_editType == COFTypeEnum.Multicore);
	}

	public void setEditScope(int editScope)
	{
		if (editScope == MulticoreEditPanel.LOCAL_SCOPE || editScope == MulticoreEditPanel.SHARED_SCOPE) {
			m_editScope = editScope;
		}
	}

	public void setProxyRoot(MCProxy proxyRoot)
	{
		m_root = proxyRoot;
	}

	public void setEditType(COFTypeEnum type)
	{
		if (type == COFTypeEnum.Multicore || type == COFTypeEnum.Overbraid) {
			m_editType = type;
		}
	}

	public void setLibObj(ILibraryPartSelection libObj)
	{
		m_libObj = libObj;
	}

	public void setProxyLibMCmap(Map<MCProxy, ILibraryPartSelection> proxyLibMCmap)
	{
		this.proxyLibMCmap = proxyLibMCmap;
	}

	public void setProxylibMCInnerCoreMap(Map<MCProxy, ILibraryInnerCore> proxylibMCInnerCoreMap)
	{
		this.proxylibMCInnerCoreMap = proxylibMCInnerCoreMap;
	}

	public ISharedConductorMgr getSharedConductorMgr()
	{
		return m_project.getSharedConductorMgr();
	}

	public IConnectivity getConnectivity()
	{
		IConnectivity connectivity = m_design.getConnectivity();
		assert connectivity != null;
		return connectivity;
	}

	public void destroy()
	{
		m_design = null;
		m_diagram = null;
		m_project = null;
		m_root = null; // Need to clear the whole tree ?
		m_libObj = null;
		proxyLibMCmap.clear();
		proxylibMCInnerCoreMap.clear();
		schematicUpdateProcessor.destroy();
	}

	public void buildProxiesForDWConductors(Map<IMulticore, MCProxy> multicores,
			Map<IRevisionedSharedObject, MCProxy> shared)
	{
		IDesign design = getDesign();
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		Set<IConductor> doneConductors = new HashSet<IConductor>();
		for (IConductorIterator iter = connectivity.getConductors(); iter.hasNext(); ) {
			IConductor conductor = iter.getNext();

			if (!MCAcceptenceResult.ACCEPTABLE.equals(accept(conductor, false))) {
				continue;
			}

			// avoid duplicate nodes if multiple representations for a conductor
			if (!doneConductors.add(conductor)) {
				continue;
			}

			//
			// Create a node for this.
			//
			MCProxy node = new MCProxy(conductor);
			ISharedConductor shCond = conductor.getSharedConductor();
			if (shCond != null) {
				shared.put(shCond, node);
			}
			IMulticore mc = conductor.getMulticore();
			//
			// Null MC -> get(null)
			//
			MCProxy parent = null;
			MCProxy child = node;
			//
			do {
				//
				// Get it., and go up the stack.
				//
				boolean isNewNode = false;

				// This check also guards against mc == null, because multicores.get(null) returns m_root,
				// which is always a non-null value.
				MCProxy pp = multicores.get(mc);
				if (pp == null) {
					isNewNode = true;
					pp = new MCProxy(mc);
					multicores.put(mc, pp);
					final ISharedMulticore sharedMulticore = mc != null ? mc.getSharedMulticore() : null;
					if (sharedMulticore != null) {
						shared.put(sharedMulticore, pp);
					}
					if (parent == null) {
						parent = pp;
					}
					//
					// Add the indicators for this multicore...
					//

					IShieldBody sb = mc != null ? mc.getShieldBody() : null;
					if (sb != null) {
						MCProxy mcsb = new MCProxy(sb.getType(), sb);
						pp.addChild(mcsb);
					}
				}

				pp.addChild(child);
				if (!isNewNode) {
					break; // joined an existing path.
				}
				child = pp;
				mc = mc != null ? mc.getParent() : null;
			} while (true);
		}
	}

	private void buildProxiesForMCs(Map<IMulticore, MCProxy> multicores)
	{
		if (isLocalEditScope()) {
			IConnectivity connectivity = getConnectivity();
			assert connectivity != null;
			for (IMulticoreIterator iter = connectivity.getMulticores(); iter.hasNext(); ) {
				IMulticore libmc = iter.getNext();
				if (multicores.get(libmc) == null && libmc.getSharedMulticore() == null) {

					// DR 380523 : Multicores inside an overbraid should not be shown in the multicores dialog.
					if (isOverbraidEditType() || MCAcceptenceResult.ACCEPTABLE.equals(accept(libmc))) {
						MCProxy mcp = new MCProxy(libmc);
						getProxyRoot().addChild(mcp);
						multicores.put(libmc, mcp);
					}
				}
			}
		}
	}

	public MCAcceptenceResult accept(IMulticore root)
	{

		MCAcceptenceResult acceptenceResult = MCAcceptenceResult.ACCEPTABLE;
		if (root.getSharedObject() != null) {
			acceptenceResult = isSharedObjectApplicable(root.getSharedObject());
		}
		if (MCAcceptenceResult.ACCEPTABLE.equals(acceptenceResult)) {
			if (isOverbraidEditType() || !isInsideOverbraid(root)) {
				return MCAcceptenceResult.ACCEPTABLE;
			}
			return MCAcceptenceResult.OVERBRAIDCHILD;
		}
		return acceptenceResult;
	}

	private boolean isInsideOverbraid(IMulticore root)
	{
		for (IMulticore mc = root; mc != null; mc = mc.getParent()) {
			if (mc instanceof IOverbraid) {
				return true;
			}
		}
		return false;
	}

	public MCAcceptenceResult accept(IConductor conductor, boolean acceptAssemblyChild)
	{

		// Can't use it if it's in an assembly
		if (!acceptAssemblyChild && isMulticoreEditType() && conductor.getAssembly() != null) {
			return MCAcceptenceResult.ASSEMBLYCHILD;
		}

		// Pass over shield conductors
		if (conductor instanceof IShieldConductor) {
			return MCAcceptenceResult.SHIELDCOND;
		}

		// Pass over interconnect conductors
		if (conductor instanceof IInterconnectConductor) {
			return MCAcceptenceResult.INTERCONNECTCOND;
		}

		// Can't mess with overbraids when editiing multicores
		if (isMulticoreEditType()) {
			for (IMulticore mc = conductor.getMulticore(); mc != null; mc = mc.getParent()) {
				if (mc instanceof IOverbraid) {
					return MCAcceptenceResult.OVERBRAIDCHILD;
				}
			}
		}
		// shared conductor checks
		ISharedConductor scond = conductor.getSharedConductor();
		if (scond != null) {
			// dts0100567509 - DATA CORRUPTION: Shared conductor owned by 2 shared multicores following multi-session edit
			// don't add conductor already in a shared MC
			// check needs to be here in case the shared conductor has been added to a shared MC in another session

			MCAcceptenceResult mcAcceptenceResult = isSharedObjectApplicable(scond);

			if (!mcAcceptenceResult.equals(MCAcceptenceResult.ACCEPTABLE)) {
				return mcAcceptenceResult;
			}

			//dts0100528691: it checks if the conductor is added to a shared multicore to prevent the shared conductors
			//to have two parent multicore at the same time, and in the same time we check whether this conductor is added in another design.
			//because we can't change the connectivity of other designs.
//				if (isUsedinAnotherDesign(scond)) {
//					return false;
//				}
		}

		// Now consider sharedness.
		// In the special case where we are in CREATE_ONLY mode and we're creating shared objects, we'll take both
		// shared and unshared conductors (the unshared will be shared if they end up inside a shared object.)
		// Otherwise the conductor must match the scope: only unshared/unported in local mode, only shared in shared
		// mode.
		if (isLocalEditScope() && conductor.getSharedConductor() == null) {
			return MCAcceptenceResult.ACCEPTABLE;
		}
		if (isSharedEditScope()) {
			IMulticore rootMC = conductor.getRootMulticore();
			// Shared assemblies are not supported by the object model
			boolean noAssemblyChild =
					!(rootMC instanceof IOverbraid && LogicUtils.isAnyInnerCoreAssembled((IOverbraid) rootMC));
			if (!noAssemblyChild) {
				return MCAcceptenceResult.ASSEMBLYCHILD;
			}
			return MCAcceptenceResult.ACCEPTABLE;
		}

		return MCAcceptenceResult.UNKNOWNERROR;
	}

	private void addSharedConductor(@NotNull ISharedConductor sc, MCProxy parent,
			Map<IRevisionedSharedObject, MCProxy> found)
	{
		MCProxy proxy = found.get(sc);
		if (proxy == null) {
			ILogicDesign design = getDesign();
			IConductor conductor = design.getSharedUsageMgr().getConductor(sc);
			if (conductor == null) {
				conductor = getConnectivity().findSharedConductor(sc);
			}

			if (conductor != null) {
				proxy = new MCProxy(conductor);
			}
			else {
				proxy = new MCProxy(sc);
			}

			found.put(sc, proxy);
			parent.addChild(proxy);
		}
	}

	private void addSharedMulticore(ISharedMulticore sm, MCProxy parent,
			Map<IRevisionedSharedObject, MCProxy> found)
	{
		MCProxy proxy = found.get(sm);
		if (proxy == null) {
			ILogicDesign design = getDesign();
			IMulticore mc = design.getSharedUsageMgr().getMulticore(sm);
			if (mc == null) {
				mc = getConnectivity().findSharedMulticore(sm);
			}
			if (mc != null) {
				proxy = new MCProxy(mc);
			}
			else {
				proxy = new MCProxy(sm);
			}
			found.put(sm, proxy);
			parent.addChild(proxy);
			for (IStringIterator indit = sm.getIndicators(); indit.hasNext(); ) {
				String ind = indit.getNext();
				proxy.addChild(new MCProxy(ind, MCProxy.MCP_INDICATOR));
			}
		}
		for (ISharedMulticoreIterator smit = sm.getMulticores(); smit.hasNext(); ) {
			addSharedMulticore(smit.getNext(), proxy, found);
		}
		for (ISharedConductorIterator scit = sm.getConductors(); scit.hasNext(); ) {
			addSharedConductor(scit.getNext(), proxy, found);
		}
	}

	public MCAcceptenceResult isSharedObjectApplicable(ISharedObject sharedObjectToCheck)
	{

		if (isLocalEditScope() || sharedObjectToCheck.isFrozen()) {
			return MCAcceptenceResult.FROZENSHARED;
		}
		if (sharedObjectToCheck instanceof ISharedMulticore) {
			ISharedMulticore smc = (ISharedMulticore) sharedObjectToCheck;
			return smc.getParent() != null ? MCAcceptenceResult.INNERMC : MCAcceptenceResult.ACCEPTABLE;
		}
		if (sharedObjectToCheck instanceof ISharedConductor) {
			ISharedConductor sc = (ISharedConductor) sharedObjectToCheck;
			return sc.getMulticore() == null && !sc.isShield() ? MCAcceptenceResult.ACCEPTABLE :
					MCAcceptenceResult.SHIELDCOND;
		}
		return MCAcceptenceResult.ACCEPTABLE;
	}

	public void addSharedObjects(Map<IRevisionedSharedObject, MCProxy> shared)
	{
		if (isSharedEditScope()) {
			ISharedConductorMgr iscm = getSharedConductorMgr();

			for (ISharedMulticoreIterator smit = iscm.getAccessibleSharedMulticores(); smit.hasNext(); ) {
				ISharedMulticore sm = smit.getNext();
				MCAcceptenceResult result = isSharedObjectApplicable(sm);
				if (result.equals(MCAcceptenceResult.ACCEPTABLE)) {
					addSharedMulticore(sm, getProxyRoot(), shared);
				}
			}
			if (isOverbraidEditType()) {
				for (ISharedOverbraidIterator soit = iscm.getAccessibleSharedOverbraids(); soit.hasNext(); ) {
					ISharedOverbraid so = soit.getNext();
					MCAcceptenceResult result = isSharedObjectApplicable(so);
					if (MCAcceptenceResult.ACCEPTABLE.equals(result)) {
						addSharedMulticore(so, getProxyRoot(), shared);
					}
				}
			}
			for (ISharedConductorIterator scit = iscm.getAccessibleLogicSharedConductors(); scit.hasNext(); ) {
				ISharedConductor sc = scit.getNext();

				//
				// DO NOT add shields to the list.
				//
				//check if the shared conductor is used in other design, prevent adding it,
				// because we can't change the connectivity of the closed designs
				MCAcceptenceResult result = isSharedObjectApplicable(sc);
				if (MCAcceptenceResult.ACCEPTABLE.equals(result)) {
					addSharedConductor(sc, getProxyRoot(), shared);
				}
			}
		}
	}

	private void updateAllMissingInnercores()
	{
		for (int i = 0; i < m_root.getChildCount(); i++) {
			updateMissingInnercores(m_root.getChildProxyAt(i));
		}
	}

	/* The purpose of this function is to complete the Proxy hierarchy for a libraried MC with 'unassigned' or
		'paritally assigned' innercores, so that the "Generic MC" dialog displays the complete MC tree. It works this way:
		1. Get all the already created childProxies      => "existingChildProxies"
		2. Get all the child cable objects of the multicore  => "libCableObjMap"
		3. Get all the innercores of the library definition. For each of the innercore, it falls into of these category
			 a) There exists a corresponding cable object and a proxy already created
			 (wire assigned to innercore "or" innerMCs with atleast one innercore assigned )
				 action=> there can be innerMCs with incomplete proxy tree underneath it..complete the hierarchy for it.
			 b) There exists a corresponding cable object and no proxy is created yet
			(inner MCs with none of the innercores assigned "or" shields)
				 action=> create a child proxy for each cable obj and associate the cable object and the proxy
			 c) There is no corresponding cable object and no proxy as well(unassigned innercore wires)
				 action=> create the child proxy hierarchy blindly
		 4. A libraried MC is child of a non-libraried MC..Iterate all libraried children of MC and check if a proxy is
		  already created for it. If not, create a proxy. Update missing Innercores for this proxy.
		 */

	private static void updateMissingInnercores(@Nullable MCProxy proxy)
	{
		if (proxy != null && proxy.getRef() instanceof IMulticore) {
			IMulticore mc = (IMulticore) proxy.getRef();

			//1. Get existing childProxies
			Map<IUID, MCProxy> existingChildProxies = new HashMap<IUID, MCProxy>(proxy.getChildCount());
			for (int i = 0; i < proxy.getChildCount(); i++) {
				MCProxy childProxy = proxy.getChildProxyAt(i);
				assert childProxy != null;
				IUID ref;
				if (childProxy.getRef() instanceof ILogicObject) {
					ref = ((ILibrariedObject) childProxy.getRef()).getLibraryRef();
				}
				else {
					ref = childProxy.getRef().getUID();
				}
				if (ref != null) {
					existingChildProxies.put(ref, childProxy);
				}
			}

			if (mc.isPartAssigned() && !proxy.isOverbraidRef()) {
				//2. Get all cable objects of the Multicore
				Map<IUID, IUIDObject> libCableObjMap =
						new HashMap<IUID, IUIDObject>(
								mc.getNumConductorsIncludingShields() + mc.getNumMulticores());
				for (IConductorIterator condIt = mc.getConductorsIncludingShields(); condIt.hasNext(); ) {
					IConductor cond = condIt.getNext();
					if (cond.getInnercoreRef() != null) {
						libCableObjMap.put(cond.getInnercoreRef(), cond);
					}
				}
				for (IMulticoreIterator mcIt = mc.getMulticores(); mcIt.hasNext(); ) {
					IMulticore MC = mcIt.getNext();
					if (MC.getInnercoreRef() != null) {
						libCableObjMap.put(MC.getInnercoreRef(), MC);
					}
				}

				//3.Get all innercores defined in library for this Multicore
				boolean bTopLevelMCPartDefinition = mc.getPartNumber() != null && mc.getLibraryRef() != null;
				Collection<ILibraryInnerCore> innerCores =
						bTopLevelMCPartDefinition ? LibraryHelper.getInnerCores(mc.getLibraryObject()) :
								LibraryHelper.getInnerCores(mc.getInnercoreLibraryObject());
				for (ILibraryInnerCore innerCore : innerCores) {
					MCProxy existingChildProxy = existingChildProxies.get(innerCore.getUID());
					IUIDObject cableObj = libCableObjMap.get(innerCore.getUID());
					if (existingChildProxy == null) //proxy is not yet created for this innercore
					{
						if (cableObj != null) {
							//there exists a cable object for this innercore..but no proxy yet
							//could be a shield without a schem
							//could be a innerMC with none of the innerWires assigned
							MCProxy childProxy = new MCProxy(cableObj);
							proxy.addChild(childProxy);
							if (cableObj instanceof IMulticore) {
								updateMissingInnercores(childProxy);
							}
						}
						else {
							//there is no cable object for this innercore
							//Simply create the child hierarchy and add it to current proxy
							proxy.addChild(new MCProxy(innerCore));
						}
					}
					else {
						//a inner MC with atleast one of its innercore assigned to cable obj..
						// need to create proxies for remaining
						if (cableObj instanceof IMulticore) {
							updateMissingInnercores(existingChildProxy);
						}
					}
				}
			}
			else {
				//This seems to a MC with no library-part..one of its children may be a libraried MC
				for (IMulticoreIterator mcIt = mc.getMulticores(); mcIt.hasNext(); ) {
					IMulticore MC = mcIt.getNext();
					if (MC.isPartAssigned()) {
						MCProxy childProxy = existingChildProxies.get(MC.getLibraryRef());
						if (childProxy == null) {
							childProxy = new MCProxy(MC);
							proxy.addChild(childProxy);
						}
						updateMissingInnercores(childProxy);
					}
				}
			}
		}
	}

	public void buildProxyTree()
	{
		//
		// Go through the conductors/multicores on the sheet, and
		// duplicate them in a tree of MCProxies - we'll mess with this,
		// then rebuild on a successful termination.
		//
		MCProxy proxyRoot = new MCProxy("root", null);
		setProxyRoot(proxyRoot);
		ISharedConductorMgr iscm = getSharedConductorMgr();

		Map<IMulticore, MCProxy> multicores =
				new HashMap<IMulticore, MCProxy>(
						getConnectivity().getNumMulticores() / getDesign().getNumDiagrams());

		multicores.put(null, proxyRoot);

		Map<IRevisionedSharedObject, MCProxy> shared = new HashMap<IRevisionedSharedObject, MCProxy>(
				iscm.getAccessibleSharedMulticores().getSize() + iscm.getAccessibleSharedOverbraids().getSize());

		// Scan all conductors in connectivity, not only those on current diagram
		// This is for DWO completion and fix for dts0100674103
		buildProxiesForDWConductors(multicores, shared);

		// Add any top-level library multicores we missed because there are no conductors placed for them yet.
		// Scan all multicores in connectivity: the diagram-binding mechanism has been removed
		// This is for DWO completion and fix for dts0100674103
		buildProxiesForMCs(multicores);

		// Go to the shared conductor mgr and pick up the shared multicores and conductors
		addSharedObjects(shared);

		// Now go down through the whole tree, find all library multicores, and add nodes for innercores not yet placed
		//addInnercores(m_root);
		updateAllMissingInnercores();

		updateProxiesDisplayInformation(proxyRoot);
	}

	private void updateProxiesDisplayInformation(MCProxy root)
	{
		//Map of revison baseID to MCProxy objects
		Map<IUID, Set<MCProxy>> map = new HashMap<IUID, Set<MCProxy>>();
		extractSharedProxies(root, map);
		for (IUID baseID : map.keySet()) {
			Set<MCProxy> proxies = map.get(baseID);
			if (proxies.size() > 1) {
				for (MCProxy proxy : proxies) {
					proxy.setDisplayRevision(true);
				}
			}
		}
	}

	private void extractSharedProxies(@Nullable MCProxy proxy, Map<IUID, Set<MCProxy>> map)
	{
		if (proxy != null) {
			if (proxy.isSharedRef()) {
				IUID baseID = proxy.getSharedRef().getBaseId();
				Set<MCProxy> proxies = map.get(baseID);
				if (proxies == null) {
					proxies = new HashSet<MCProxy>();
					map.put(baseID, proxies);
				}
				proxies.add(proxy);
			}
			for (int i = 0; i < proxy.getChildCount(); i++) {
				extractSharedProxies(proxy.getChildProxyAt(i), map);
			}
		}
	}

	private void addImpactedDesign(Set<ILogicDesign> designsToBeLocked, Set<ILogicDesign> designsToBeUpdated,
			Set<ISharedObject> sharedObjs)
	{
		List<LightWeightUsage> lightWeightUsages = CapitalProjectDataServices.getDataServices().getSharedObjectUsageDataIncludingUnplaced(sharedObjs);

		SharedObjectUsageInfo sharedObjectUsageInfo = new SharedObjectUsageInfo();

		if(lightWeightUsages != null) {
			sharedObjectUsageInfo.prepareSharedConductorUsageData(lightWeightUsages);
		}

		Set<CapitalDataServices.SimpleDesignName> usedDesigns = new HashSet<>();
		usedDesigns.addAll(sharedObjectUsageInfo.getUsedDesignNames());
		addLoadedAndEditableImpactedDesigns(designsToBeLocked, designsToBeUpdated, usedDesigns);
	}

	private void addLoadedAndEditableImpactedDesigns(Set<ILogicDesign> designsToBeLocked,
			Set<ILogicDesign> designsToBeUpdated,
			Collection<CapitalProjectDataServices.SimpleDesignName> collDesignUsages)
	{

		// Load the designs using the UIDs
		Collection<IDesignContainer> designs = LogicUtils.loadDesigns(m_project.getDesignMgr(),
				collDesignUsages);

		//Update the lock and update design collections
		designs.stream().forEach(des -> {
			if (des instanceof ILogicDesign) {
				ILogicDesign logicDes = (ILogicDesign) des;
				if (des.isEditable()) {
					//try to lock and process all the editable designs.
					//designs would be loaded and processed if required.
					// Update all loaded and locked designs
					// Other designs will taken care in Connectivity.postLoadProcess
					designsToBeLocked.add(logicDes);
					designsToBeUpdated.add(logicDes);
				}
			}
		});
	}

	public void getImpactedDesigns(Set<ILogicDesign> designsToBeLocked, Set<ILogicDesign> designsToBeUpdated,
			Set<ISharedObject> sharedChanges)
	{
		if (isSharedEditScope()) {
			addImpactedDesign(designsToBeLocked, designsToBeUpdated, sharedChanges);

			designsToBeUpdated.remove(getDesign());
			designsToBeLocked.remove(getDesign());
		}
	}

	public boolean lockDesigns(Set<ISharedObject> sharedChanges, Set<ILogicDesign> designsImpacted,
			Set<ILogicDesign> lockedDesigns)
	{
		Set<ILogicDesign> designsToBeLocked = new HashSet<ILogicDesign>();
		for (ILogicDesign impactedDesign : designsImpacted) {
			if (!impactedDesign.isLocked()) {
				designsToBeLocked.add(impactedDesign);
			}
		}
		boolean bAllDesignModifiable = false;
		try {
			UtilsHelper.getPersistenceSession().batchAtomicLock(designsToBeLocked);
			lockedDesigns.addAll(designsToBeLocked);
			bAllDesignModifiable = true;
		}
		catch (PersistenceLockFailureCheckedException ignored) {
			showMULockError(sharedChanges, designsToBeLocked);
		}
		catch (PersistenceStateException ignored) {

			showMULockError(sharedChanges, designsToBeLocked);
		}

		return bAllDesignModifiable;
	}

	private String getShortListOfObjectNames(Iterator<? extends INamedPropertiedObject> objIter)
	{
		StringBuilder nameList = new StringBuilder();
		if (objIter.hasNext()) {
			nameList.append(objIter.next().getName());
			if (objIter.hasNext()) {
				nameList.append(", ");
				nameList.append(objIter.next().getName());
				nameList.append(",...");
			}
		}
		return nameList.toString();
	}

	protected void showMULockError(Set<ISharedObject> sharedChanges, Set<ILogicDesign> designsToBeLocked)
	{
		final Class<CreateMulticoreAction> cls = CreateMulticoreAction.class;

		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(cls, "CreateMulticoreAction.CreateSharedMCMULockError");
		if (isMulticoreEditType()) {
			content.setContextSuffixParameters("MC");
			content.setMessageSuffixParameters("MC");
		}
		else {
			content.setContextSuffixParameters("OB");
			content.setMessageSuffixParameters("OB");
		}
		if (sharedChanges.size() > 1) {
			content.setImplicationsSuffixParameters("Multiple",
					getShortListOfObjectNames(sharedChanges.iterator()),
					getShortListOfObjectNames(designsToBeLocked.iterator()));
		}
		else {
			content.setImplicationsSuffixParameters("Single",
					getShortListOfObjectNames(sharedChanges.iterator()),
					getShortListOfObjectNames(designsToBeLocked.iterator()));
		}
		Message.show(PromptSeverity.ERROR, content);
	}

	public void releaseDesignLocks(Set<ILogicDesign> lockedDesigns)
	{
		UtilsHelper.getPersistenceSession().batchUnlock(lockedDesigns);
		lockedDesigns.clear();
	}

	protected Set<IMulticore> processMulticoreAdditions(Collection<IUndoableObject> newObjects,
			Set<MCProxy> addedMCs,
			Set<MCProxy> modifiedMCs, Set<IRevisionedSharedObject> sharedCreations)
	{
		// Create new Multicores from the Added list and plug them in to their proxies.
		Set<IMulticore> librariedMulticores = new HashSet<IMulticore>();
		for (MCProxy mcp : addedMCs) {
			modifiedMCs.remove(mcp);
			IMulticore mc;
			if (mcp.isOverbraidRef()) {
				mc = FactoryMgr.getCablePropertiedFactory()
						.createOverbraid(FactoryMgr.getCommonFactory().createUID());
				LogicObjectLockFinder.tryEdit(getDesign(), mc);
				if (m_libObj != null) {
					mc.assignLibraryDetails(m_libObj);
				}
			}
			else {
				ILibraryPartSelection lm = proxyLibMCmap.get(mcp);
				if (lm != null) {
					mc = MulticoreLibraryHelper.createLibrariedMulticore(lm, getDesign());
					/*In case of normal MCs, Indicators are placed for top level MCs by iterating m_changedIndicators
					 list and add them to m_modifiedMulticores list (there is no shield body defined so far)
					In case of libraried MCs, shield body is created at the time of creation of MC itself.
					Hence, such MCs are not added to m_modifiedMulticores list..so, indicators are not generated
					So, add them in separate list "librariedMulticores" and process them separately
					and add the indicators*/
					librariedMulticores.addAll(mc.getAllMulticoresInHierarchy());
					updateProxyRef(mc, populateLibAndProxyMap(mcp));
				}
				else {
					mc = FactoryMgr.getCablePropertiedFactory()
							.createMulticore(FactoryMgr.getCommonFactory().createUID());
					LogicObjectLockFinder.tryEdit(getDesign(), mc);
				}
			}

			getConnectivity().addMulticore(mc);
			//
			// Change the ref to the new object.
			//
			mcp.setRef(mc);
			//
			if (mc instanceof IUndoableObject) {
				newObjects.add((IUndoableObject) mc);
			}

			if (isSharedEditScope()) {
				ISharedMulticore smc = createSharedMulticore(sharedCreations, mc);
				mcp.setSharedRef(smc);
			}
		}
		return librariedMulticores;
	}

	private void constructShieldConductorIfNeeded(@NotNull MCProxy mcp,
			@NotNull Collection<IUndoableObject> newObjects,
			@NotNull Set<IRevisionedSharedObject> sharedCreations)
	{
		ISharedMulticore smc = CommonUtils.cast(mcp.getSharedRef(), ISharedMulticore.class);
		if (smc != null) {
			if (isCandidateToCreateSharedShield(mcp, smc)) {
				createSharedShield(smc, sharedCreations);
			}
		}
		IMulticore mc = CommonUtils.cast(mcp.getRef(), IMulticore.class);
		if (mc != null) {
			if (isCandidateToCreateNonSharedShield(mcp, mc)) {
				createNonSharedShield(mc, newObjects, sharedCreations);
			}
		}
	}

	public void constructShieldConductorIfNeeded(@Nullable IMulticore mc,
			@Nullable ISharedMulticore smc,
			@NotNull Collection<IUndoableObject> newObjects,
			@NotNull Set<IRevisionedSharedObject> sharedCreations)
	{
		if (smc != null) {
			createSharedShield(smc, sharedCreations);
		}
		if (mc != null) {
			createNonSharedShield(mc, newObjects, sharedCreations);
		}
	}

	private void createNonSharedShield(@NotNull IMulticore mc, @NotNull Collection<IUndoableObject> newObjects,
			@NotNull Set<IRevisionedSharedObject> sharedCreations)
	{
		ISharedMulticore sharedMulticore = mc.getSharedMulticore();
		if (sharedMulticore == null) {
			//old legacy behavior. don't create shield in connectivity for an overbraid.
			if (!(mc instanceof IOverbraid)) {
				createShieldConductor(mc, newObjects);
			}
		}
		else {
			ISharedConductor sharedShield = createSharedShield(sharedMulticore, sharedCreations);
			//Don't create shield in connectivity for shared overbraids.
			IShieldConductor shieldCond = mc.getShield();
			if (!(mc instanceof IOverbraid)) {
				shieldCond = createShieldConductor(mc, newObjects);
				sharedShield.setName(shieldCond.getName());
			}
			if (shieldCond != null) {
				shieldCond.setSharedConductor(sharedShield);
			}
		}
	}

	@NotNull private IShieldConductor createShieldConductor(@NotNull IMulticore mc,
			@NotNull Collection<IUndoableObject> newObjects)
	{
		IShieldConductor shieldCond = mc.getShield();
		if (shieldCond != null) {
			return shieldCond;
		}
		shieldCond = FactoryMgr.getCablePropertiedFactory().createShieldConductor(
				FactoryMgr.getCommonFactory().createUID());
		mc.getConnectivity().addConductor(shieldCond);
		mc.setShield(shieldCond);
		newObjects.add((IUndoableObject) shieldCond);
		return shieldCond;
	}

	@NotNull private ISharedConductor createSharedShield(@NotNull ISharedMulticore smc,
			@NotNull Set<IRevisionedSharedObject> sharedCreations)
	{
		ISharedConductor sharedShield = smc.getShield();
		if (sharedShield != null) {
			return sharedShield;
		}
		ISharedFactory sharedFactory = UtilsHelper.getCHSUtils().getSharedFactory();
		sharedShield = sharedFactory.createSharedConductor(FactoryMgr.getCommonFactory().createUID());
		sharedShield.setType(ISharedConductor.SHIELD_TYPE);
		sharedShield.setName(smc.getName());
		sharedShield.setRevision(smc.getRevision());
		sharedShield.setDesignAbstraction(getDesign().getDesignAbstraction());
		ISharedConductorMgr iscm = getSharedConductorMgr();
		iscm.addSharedConductor(sharedShield);
		smc.setShield(sharedShield);
		sharedCreations.add(sharedShield);
		return sharedShield;
	}

	@NotNull
	public ISharedMulticore createSharedMulticore(Set<IRevisionedSharedObject> sharedCreations, IMulticore mc)
	{
		ISharedMulticore smc;
		if (isMulticoreEditType()) {
			smc = FactoryMgr.getSharedFactory().createSharedMulticore(FactoryMgr.createUID());
		}
		else {
			smc = FactoryMgr.getSharedFactory().createSharedOverbraid(FactoryMgr.createUID());
		}
		ISharedConductorMgr iscm = getSharedConductorMgr();
		smc.setManager(iscm);
		// Do this before hooking up so that the default naming works - otherwise the logic name is overriden by
		// the shared name, and you end up setting the shared name to itself - which by the way is null, which
		// is a bad thing.
		smc.setName(mc.getName());
		mc.setSharedMulticore(smc);
		sharedCreations.add(smc);
		smc.setDesignAbstraction(getDesign().getDesignAbstraction());
		return smc;
	}

	protected final boolean isCandidateToCreateSharedShield(MCProxy mcp, ISharedMulticore smc)
	{
		return (smc instanceof ISharedOverbraid || mcp.isShielded()) && smc.getShield() == null;
	}

	protected final boolean isCandidateToCreateNonSharedShield(MCProxy mcp, IMulticore mc)
	{
		return (mc instanceof IOverbraid || mcp.isShielded()) && mc.getShield() == null;
	}

	/*In case of libraried MC, MulticoreLibraryHelper::createLibrariedMulticore takes care of creating the cable objects
				for entire hierarchy of the library definition. Whereas the proxy hierarchy is created by
				EditMulticorePanel::addFromLibrary
				The proxy and the actual cable object needs to be associated.."libraryRef" common in both
				First populate a map of <libraryRef, Proxy> for each of the proxy in the hierarchy
				Later, traverse all the cable objects in the hierarchy.
				For each cable object, get the library ref, get the corresponding entry in <lib, Proxy> map and get the Proxy.
				Associate the proxy and cable object*/

	protected void updateProxyRef(IMulticore mc, Map<IUID, MCProxy> libProxyMap)
	{
		for (IMulticore childMC : mc.getMulticoresAsList()) {
			IUID libUID = childMC.getLibraryRef();
			MCProxy proxy = libProxyMap.get(libUID);
			if (proxy != null) {
				proxy.setRef(childMC);
				updateProxyRef(childMC, libProxyMap);
			}
		}
	}

	protected Map<IUID, MCProxy> populateLibAndProxyMap(MCProxy mcProxy)
	{
		Map<IUID, MCProxy> proxyMap = new HashMap<IUID, MCProxy>();
		int childCnt = mcProxy.getChildCount();
		for (int i = 0; i < childCnt; i++) {
			MCProxy childProxy = mcProxy.getChildProxyAt(i);
			assert childProxy != null;
			if (childProxy.isLibraryInnercoreRef()) {
				ILibraryInnerCore childLib = proxylibMCInnerCoreMap.get(childProxy);
				if (childLib != null) {
					proxyMap.put(childLib.getUID(), childProxy);
				}
				Map<IUID, MCProxy> innerChildMap = populateLibAndProxyMap(childProxy);
				for (IUID lib : innerChildMap.keySet()) {
					proxyMap.put(lib, innerChildMap.get(lib));
				}
			}
		}
		return proxyMap;
	}

	protected void createMissingLibraryInnerMCs(Set<MCProxy> modifiedMCs, Set<IMulticore> impactedParentMCs)
	{
		if (!isLocalEditScope()) {
			return;
		}
		for (MCProxy mcProxy : modifiedMCs) {
			if (mcProxy.getRef() instanceof IMulticore) {
				continue;
			}
			List<IUIDObject> ancestors = new ArrayList<IUIDObject>();
			List<MCProxy> proxies = new ArrayList<MCProxy>();

			IMulticore topMC = findandCreateTopLevelLibMC(mcProxy, ancestors, proxies);

			createInnerLibCoresIfNotCreated(impactedParentMCs, ancestors, proxies, topMC);
		}
	}

	protected void createInnerLibCoresIfNotCreated(Set<IMulticore> impactedParentMCs, List<IUIDObject> ancestors,
			List<MCProxy> proxies, IMulticore topMC)
	{
		IMulticore parentMC = topMC;
		for (int i = 1; i < ancestors.size(); i++) {
			IUIDObject ancestor = ancestors.get(i);
			MCProxy proxy = proxies.get(i);
			if (ancestor instanceof ILibraryInnerCore) {
				ILibraryInnerCore innercoreMC = (ILibraryInnerCore) ancestor;
				IMulticore newMC = MulticoreLibraryHelper
						.createLibrariedMulticore(innercoreMC, getDesign(), false);
				parentMC.addMulticore(newMC);
				impactedParentMCs.add(parentMC);
//						newMC.setHarness(parentMC.getHarness());
				proxy.setRef(newMC);
				parentMC = newMC;
			}
			else if (ancestor instanceof IMulticore) {
				parentMC = (IMulticore) ancestor;
			}
		}
	}

	protected IMulticore findandCreateTopLevelLibMC(MCProxy mcProxy, List<IUIDObject> ancestors, List<MCProxy> proxies)
	{
		if ((mcProxy.getRef() instanceof ILibraryInnerCore)) {
			//an unassigned libraried innerMC
			ancestors.add(0, mcProxy.getRef());
			proxies.add(0, mcProxy);
			boolean bTopLevelMC =
					mcProxy.getRef() instanceof ILibraryMulticore || mcProxy.getRef() instanceof IMulticore;
			while (!bTopLevelMC) {
				MCProxy parentProxy = mcProxy.getParentProxy();
				ancestors.add(0, parentProxy.getRef());
				proxies.add(0, parentProxy);
				bTopLevelMC = parentProxy.getRef() instanceof ILibraryMulticore ||
						parentProxy.getRef() instanceof IMulticore;
				mcProxy = parentProxy;
			}
		}

		IUIDObject topObject = ancestors.get(0);
		MCProxy topProxy = proxies.get(0);
		//			assert (topObject instanceof IMulticore || topObject instanceof ILibraryMulticore);
		IMulticore topMC = null;
		if (topObject instanceof ILibraryMulticore) {
			topMC = MulticoreLibraryHelper
					.createLibrariedMulticore(topObject, getDesign(), false);
			topProxy.setRef(topMC);
		}
		else if (topObject instanceof IMulticore) { // When assertions are disables, this condition is not always true
			topMC = (IMulticore) topObject;
		}
		else {
			assert false;
		}
		return topMC;
	}

	@SuppressWarnings("OverlyLongMethod")
	private void walkTree(@Nullable MCProxy mcp, Collection<IUndoableObject> newObjects, Set<MCProxy> removedMCs,
			Set<MCProxy> addedMCs, Set<ISharedObject> sharedChanges,
			SetMap<IUID, IUID> removedSharedMC2ConductorMap, Set<IMulticore> impactedParentMCs,
			Set<ISharedMulticore> impactedSharedParentMCs)
	{
		//
		// Hook up this object.
		//
		IConnectivity conn = getConnectivity();
		//
		IUIDObject logicRef = mcp != null ? mcp.getRef() : null;
		ISharedObject sharedRef = mcp != null ? mcp.getSharedRef() : null;
		IMulticore parentLogicRef = null;
		ISharedMulticore parentSharedRef = null;
		MCProxy parentProxy = mcp != null ? mcp.getParentProxy() : null;
		if (parentProxy != null) {
			if (parentProxy.getRef() instanceof IMulticore) {
				parentLogicRef = (IMulticore) parentProxy.getRef();
			}
			parentSharedRef = (ISharedMulticore) parentProxy.getSharedRef();
		}
		//
		if (logicRef instanceof IConductor) {
			IConductor cref = (IConductor) logicRef;
			IMulticore oldref = cref.getMulticore();
			//what if there exists no cable multicore corresponding to this shared multicore in current design? mcp.getParentProxy().getRef() is a TransientNamedObject
			// the parent of conductor in memory and the parent of proxy are different, only then go ahead and change, otherwise don't disturb
			if ((parentLogicRef == null || oldref != parentLogicRef)) {
				if (changesCorrespondToCurrentSession(Arrays.asList(oldref, parentLogicRef,
						logicRef))) { // also don't disturb if the change seen is the effect of a refresh
					IUID shieldLibRef = null;
					if (oldref != null) {
						shieldLibRef = removeConductorFromMC(mcp, parentLogicRef, cref, oldref);
					}
					// If an existing conductor is being added to a new shared parent, then we have to make sure it is shared, too.
					if (isSharedEditScope() && parentSharedRef != null) {
						sharedRef =
								upgradeConductorToShared(mcp, parentSharedRef, parentProxy, cref, sharedChanges);

						if (parentLogicRef == null) {
							parentLogicRef =
									createMulticoreForUnusedSharedMC(newObjects, conn, parentProxy,
											parentSharedRef);
						}
					}
					//
					// Now get ref owned by its new parent.
					//
					if (parentLogicRef != null) {
						addConductorToMC(mcp, parentLogicRef, cref, shieldLibRef);
						impactedParentMCs.add(parentLogicRef.getRootMulticore());
					}
				}
			}
		}
		else if (logicRef instanceof IMulticore) {
			IMulticore mref = (IMulticore) logicRef;
			//
			// Update the name... [if new , or modified]
			//
			if (addedMCs.contains(mcp) || mcp.wasNameModified()) {
				updateMCName(mcp, sharedRef, mref, addedMCs);
			}
			IMulticore oldref = mref.getParent();
			if (parentLogicRef == null || oldref !=
					parentLogicRef) { // the parent of multicore in memory and the parent of proxy are different, only then go ahead and change, otherwise don't disturb
				if (changesCorrespondToCurrentSession(Arrays.asList(oldref, parentLogicRef))) {
					if (oldref != null) {
						oldref.removeMulticore(mref);
					}
					// If an existing multicore is being added to a new shared parent, then we have to make sure it is shared, too.
					if (isSharedEditScope() && parentSharedRef != null) {
						if (sharedRef == null) {
							sharedRef = createSharedMC(parentSharedRef, mref);
							if (!parentProxy.isNew()) {
								sharedChanges.add(parentSharedRef);
							}
							mcp.setSharedRef(sharedRef);
						}

						if (parentLogicRef == null) {
							parentLogicRef =
									createMulticoreForUnusedSharedMC(newObjects, conn, parentProxy,
											parentSharedRef);
						}
					}

					//
					// Now get ref owned by its new parent.
					//
					if (parentLogicRef != null) {
						parentLogicRef.addMulticore(mref);
						impactedParentMCs.add(parentLogicRef.getRootMulticore());
//					mref.setHarness(parentLogicRef.getHarness());
					}
				}
			}
		}

		if (sharedRef instanceof ISharedConductor) {
			resetSharedConductorParent(logicRef, (ISharedConductor) sharedRef, parentLogicRef, parentSharedRef,
					sharedChanges, removedSharedMC2ConductorMap, impactedSharedParentMCs);
		}
		else if (sharedRef instanceof ISharedMulticore) {
			resetSharedMCParent(logicRef, (ISharedMulticore) sharedRef, parentLogicRef, parentSharedRef,
					sharedChanges, impactedSharedParentMCs);
		}

		if (mcp != null) {
			for (int i = 0; i < mcp.getChildCount(); i++) {
				walkTree(mcp.getChildProxyAt(i), newObjects, removedMCs, addedMCs, sharedChanges,
						removedSharedMC2ConductorMap, impactedParentMCs, impactedSharedParentMCs);
			}

			ISharedConductorMgr iscm = getSharedConductorMgr();

			if (mcp.isContainerRef()) {
				cleanUpMCs(mcp, sharedRef, iscm, removedMCs, addedMCs);
			}
		}
	}

	private boolean changesCorrespondToCurrentSession(Collection<IUIDObject> changedObjectsToBeChecked)
	{
		// check that these changes are made in this session and are not a result of refresh (triggered by lock) by checking that the corresponding objects are locked
		return changedObjectsToBeChecked.stream()
				.filter(changedObject -> changedObject != null)
				.allMatch(changedObject -> LogicObjectLockFinder.isEditable(changedObject));
	}

	private void lockRevisionsOfNewlyAddedInnercores(@Nullable MCProxy mcp, Set<IRevisionedSharedObject> lockedObjects)
	{
		if (mcp != null) {
			MCProxy parentProxy = mcp.getParentProxy();
			ISharedObject sharedRef = mcp.getSharedRef();
			ISharedMulticore parentSharedRef = null;
			if (parentProxy != null) {
				parentSharedRef = (ISharedMulticore) parentProxy.getSharedRef();
			}
			if (sharedRef instanceof ISharedConductor || sharedRef instanceof ISharedMulticore) {
				if (parentSharedRef != null && getParent(sharedRef) == null) {
					lockChildRevisions(lockedObjects, (IRevisionedSharedObject) sharedRef, parentSharedRef);
				}
			}

			for (int i = 0; i < mcp.getChildCount(); i++) {
				lockRevisionsOfNewlyAddedInnercores(mcp.getChildProxyAt(i), lockedObjects);
			}
		}
	}

	public void lockChildRevisions(Set<IRevisionedSharedObject> lockedObjs, IRevisionedSharedObject revisionedObj,
			ISharedMulticore parentSharedRef)
	{
		Set<IRevisionedSharedObject> objectsFailedToLock = new HashSet<IRevisionedSharedObject>();
		SharedObjectRevisionHelper.lockRevisionsDependentRevisions(revisionedObj, lockedObjs, objectsFailedToLock);

		if (!objectsFailedToLock.isEmpty()) {
			ILockInfo lockInfo = null;
			try {
				lockInfo = CAFUtils.getInstance().getUserSession()
						.getLockInfo(objectsFailedToLock.iterator().next().getUID().toString());
			}
			catch (UserSessionException e) {
				// write msg to log.
				System.out.println(e.aError);
				e.printStackTrace();
			}
			//noinspection StringToUpperCaseOrToLowerCaseWithoutLocale
			showErrorMessageForCannotBreakRevision(getObjectDisplayName(parentSharedRef).toLowerCase(),
					getObjectDisplayName(revisionedObj).toLowerCase() + " " + revisionedObj.getName(), lockInfo);
			throw new CreateMulticoreAction.SharedObjectLockException(
					new Throwable("Unable to lock revision of a shared object " + revisionedObj.getName()));
		}
	}

	private void showErrorMessageForCannotBreakRevision(String mutlicoretype, String objectName,
			@Nullable ILockInfo lockInfo)
	{

		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(CreateMulticoreAction.class,
				"CreateMulticoreAction.cannotbreakrevision.guidance", mutlicoretype, objectName));

		String messageBody;
		if (lockInfo != null) {
			messageBody = ResourceMgr
					.getString(AssociateConnectorDialog.class, "CreateMulticoreAction.cannotbreakrevision.body1",
							mutlicoretype, objectName, lockInfo.getUserName(), lockInfo.getTimeStamp());
		}
		else {
			messageBody = ResourceMgr.getString(AssociateConnectorDialog.class,
					"CreateMulticoreAction.cannotbreakrevision.body2", mutlicoretype, objectName);
		}

		messageBody = adjustMessage(messageBody);

		//noinspection deprecation
		MessageHelper.showErrorMessage(null,
				ResourceMgr.getString(CreateMulticoreAction.class,
						"CreateMulticoreAction.cannotbreakrevision.title", mutlicoretype),
				ResourceMgr.getString(CreateMulticoreAction.class,
						"CreateMulticoreAction.cannotbreakrevision.heading", mutlicoretype),
				messageBody, actionLabel);
	}

	private String adjustMessage(String messageBody)
	{
		if (messageBody.length() > MAX_MESSAGE_BODY_LENGTH) {
			int middleIndex = messageBody.length() / 2;
			StringBuilder message = new StringBuilder("");
			String[] words = messageBody.split(" ");
			boolean addedNewLine = false;
			for (String word : words) {
				message.append(word).append(" ");
				if (!addedNewLine && message.length() > middleIndex - 5) {
					message.append("\n");
					addedNewLine = true;
				}
			}
			//noinspection AssignmentToMethodParameter
			messageBody = message.toString();
		}
		return messageBody;
	}

	@Nullable private ISharedMulticore getParent(ISharedObject sharedRef)
	{
		return (sharedRef instanceof ISharedConductor) ? ((ISharedConductor) sharedRef).getMulticore() :
				((ISharedMulticore) sharedRef).getParent();
	}

	protected ISharedObject createSharedMC(ISharedMulticore parentSharedRef, IMulticore mref)
	{
		return SharedConductorGroupHelper.share(mref, getDesign(), parentSharedRef, null);
	}

	@Nullable
	protected IUID removeConductorFromMC(MCProxy mcp, @Nullable IMulticore parentLogicRef, IConductor cref,
			IMulticore oldref)
	{
		IUID shieldLibRef = null;
		if (cref instanceof IShieldConductor) {
			shieldLibRef = cref.getLibraryRef();
			//noinspection ConstantConditions
			oldref.setShield(null);
		}
		else {
			boolean wasLibraried = mcp.isLibraryInnercoreRef();
			oldref.removeConductor(cref);
			if (parentLogicRef == null && wasLibraried) {
				cref.setLibraryRef(null);
			}
		}
		return shieldLibRef;
	}

	@Nullable
	private ISharedObject upgradeConductorToShared(MCProxy mcp, ISharedMulticore parentSharedRef,
			MCProxy parentProxy,
			IConductor cref, Set<ISharedObject> sharedChanges)
	{
		ISharedObject sharedRef = createSharedConductor(parentSharedRef, cref);
		if (!parentProxy.isNew()) {
			sharedChanges.add(parentSharedRef);
		}
		mcp.setSharedRef(sharedRef);
		return sharedRef;
	}

	@Nullable
	protected ISharedObject createSharedConductor(ISharedMulticore parentSharedRef, IConductor cref)
	{
		if (cref.getSharedConductor() == null) {
			IDesignWideUsageMgr dwum = ((ILogicDesign) cref.getDesignContainer()).getDesignWideUsageMgr();
			// to regenerate new usages properly in usage manager, all diagrams where the conductor is being used must be loaded(fix for dts0100999302)
			loadDiagramsOftheUsges(dwum.getUsages(cref));
		}
		//noinspection ConstantConditions
		return SharedConductorHelper.produceSharedConductor(cref, parentSharedRef, getDesign(), null);
	}

	private void loadDiagramsOftheUsges(List<IDesignSharedUsage> usages)
	{
		for (IDesignSharedUsage usage : usages) {
			ISchemDiagram diagram = usage.getDiagram();
			if (diagram != null) {
				diagram.loadToMemory();
			}
		}
	}

	public IMulticore createMulticoreForUnusedSharedMC(Collection<IUndoableObject> newObjects, IConnectivity conn,
			MCProxy parentProxy, ISharedMulticore parentSharedRef)
	{
		// If conductor is added to this is an existing shared MC/OB which is not used in current design
		// create connectivity for it
		IMulticore mc;
		if (parentProxy.isOverbraidRef()) {
			mc = FactoryMgr.getCablePropertiedFactory()
					.createOverbraid(FactoryMgr.getCommonFactory().createUID());
		}
		else {
			mc = FactoryMgr.getCablePropertiedFactory()
					.createMulticore(FactoryMgr.getCommonFactory().createUID());
		}
		LogicObjectLockFinder.tryEdit(getDesign(), mc);
		mc.setName(parentSharedRef.getName());
		conn.addMulticore(mc);
		Replicator.ensureShieldBodyOnLogicMulticore(getDesign(), mc);
		if (mc instanceof IUndoableObject) {
			newObjects.add((IUndoableObject) mc);
		}
		mc.setSharedMulticore(parentSharedRef);
		parentProxy.setRef(mc);
		return mc;
	}

	private void addConductorToMC(MCProxy mcp, IMulticore parentLogicRef, IConductor cref, @Nullable IUID shieldLibRef)
	{
		if (cref instanceof IShieldConductor) {
			parentLogicRef.setShield((IShieldConductor) cref);
			cref.setLibraryRef(shieldLibRef);
		}
		else {
			parentLogicRef.addConductor(cref);
			ILibraryInnerCore libRef = proxylibMCInnerCoreMap.get(mcp);
			if (libRef != null && parentLogicRef.isPartAssigned()) {
				cref.assignLibraryDetails(null);
				cref.setTypeCode("");
				cref.setTypeDescription("");
				cref.setLibraryRef(libRef.getUID());
				MulticoreLibraryHelper.addInnerCoreProperties(cref, libRef);
			}
		}
//			cref.setHarness(parentLogicRef.getHarness());
	}

	private void updateMCName(MCProxy mcp, ISharedObject sharedRef, IMulticore mref, Set<MCProxy> addedMCs)
	{
		//
		// If it is a brand new mcore, then both will be true...a
		//
		if (mcp.isUsingDefaultName() && !addedMCs.contains(mcp)) {
			//
			// Fall back onto the default naming scheme
			//
			//noinspection ConstantConditions
			mref.setName(null);
		}
		else if (!mcp.isUsingDefaultName()) {
			//
			// Use the name explicitly.
			//
			mref.setName(mcp.getName());
			if (sharedRef != null) {
				// Set it from the mcp - the logic mc name would be overriden by the shared name
				// which would end up setting the shared name to itself - which is null.
				sharedRef.setName(mcp.getName());
			}
		}
		mcp.setNameModified(false);
	}

	private void resetSharedConductorParent(IUIDObject logicRef, ISharedConductor sharedConductor,
			@Nullable IMulticore parentLogicRef, @Nullable ISharedMulticore parentSharedRef,
			Set<ISharedObject> sharedChanges,
			SetMap<IUID, IUID> removedSharedMC2ConductorMap, Set<ISharedMulticore> impactedSharedParentMCs)
	{
		ISharedMulticore oldParent = sharedConductor.getMulticore();
		if (oldParent != null) {
			removeSharedConductorFromSharedMC(sharedConductor, oldParent, removedSharedMC2ConductorMap);
		}
		if (parentSharedRef != null) {
			addSharedConductorToSharedMC(logicRef, parentLogicRef, parentSharedRef, sharedConductor, sharedChanges,
					impactedSharedParentMCs);
		}

		if (oldParent == null && parentSharedRef != null) {
			removeChildRevisionStrucAndUpdateParentValue(sharedConductor, parentSharedRef);
		}
	}

	protected void removeChildRevisionStrucAndUpdateParentValue(IRevisionedSharedObject sharedConductor,
			@NotNull IRevisionedSharedObject parentSharedRef)
	{
		removeRevisionStructure(sharedConductor);
		sharedConductor.setRevision(parentSharedRef.getRevision());
	}

	private String getObjectDisplayName(IRevisionedSharedObject revisionedObject)
	{
		if (revisionedObject instanceof ISharedConductor) {
			return ResourceMgr.getString(CreateMulticoreAction.class, "CreateMulticoreAction.SharedConductor.name");
		}
		if (revisionedObject instanceof ISharedOverbraid) {
			return ResourceMgr.getString(CreateMulticoreAction.class, "CreateMulticoreAction.SharedOverbraid.name");
		}
		if (revisionedObject instanceof ISharedMulticore) {
			return ResourceMgr.getString(CreateMulticoreAction.class, "CreateMulticoreAction.SharedMulticore.name");
		}
		return "Shared Object";
	}

	private void removeRevisionStructure(IRevisionedSharedObject revisionedObject)
			throws SharedObjectRevisionHelper.SharedObjectLockException
	{

		Set<IRevisionedSharedObject> modifiedObjs = null;
		Set<IRevisionedSharedObject> lockedObjs = new HashSet<IRevisionedSharedObject>();
		try {
			modifiedObjs = SharedObjectRevisionHelper.removeFromRevisionStructure(revisionedObject, lockedObjs);
			//noinspection ConstantConditions
			revisionedObject.setParentId(null);
			revisionedObject.setBaseId(revisionedObject.getUID());
		}
		finally {
			if (modifiedObjs != null) {
				saveSharedConductors(modifiedObjs);
			}
			for (IRevisionedSharedObject lockedObj : lockedObjs) {
				lockedObj.unlock();
			}
		}
	}

	private void saveSharedConductors(Set<IRevisionedSharedObject> modifiedObjs)
	{
		for (IRevisionedSharedObject revObject : modifiedObjs) {
			if (revObject instanceof ISharedConductor) {
				revObject.flush();
			}
		}
	}

	private void removeSharedConductorFromSharedMC(ISharedConductor sharedConductor, ISharedMulticore oldParent,
			SetMap<IUID, IUID> removedSharedMC2ConductorMap)
	{
		if (sharedConductor.isShield()) {
			oldParent.setShield(null);
		}
		else {
			oldParent.removeConductor(sharedConductor);
			removedSharedMC2ConductorMap.add(oldParent.getUID(), sharedConductor.getUID());
		}
	}

	private void addSharedConductorToSharedMC(IUIDObject logicRef, @Nullable IMulticore parentLogicRef,
			ISharedMulticore parentSharedRef, ISharedConductor sharedConductor, Set<ISharedObject> sharedChanges,
			Set<ISharedMulticore> impactedSharedParentMCs)
	{
		impactedSharedParentMCs.add(parentSharedRef);
		if (sharedConductor.isShield()) {
			parentSharedRef.setShield(sharedConductor);
		}
		else {
			parentSharedRef.addConductor(sharedConductor);
		}
//			sharedConductor.setHarness(parentSharedRef.getHarness());
		sharedChanges.add(sharedConductor);
		sharedChanges.add(parentSharedRef);
		// If we've added a shared conductor that has connectivity in the current design to a shared multicore
		// without connectivity in the current design, call the fixup method to create connectivity for the
		// shared multicore and add schem indicators.
		if (logicRef instanceof IConductor && parentLogicRef == null) {
			SharedConductorHelper.fixupParentageForConductor((IConductor) logicRef, getDesign());
		}
	}

	private void cleanUpMCs(MCProxy mcp, @Nullable ISharedObject sharedRef,
			ISharedConductorMgr iscm, Set<MCProxy> removedMCs,
			Set<MCProxy> addedMCs)
	{
		if (shouldDelete(mcp)) {
			removeMC(mcp, removedMCs, addedMCs);
		}
		else {
			// We now know for sure that this multicore will stick around, so if we just
			// created it and it is shared, add it to the managers.
			if (mcp.isNew() && sharedRef != null) {
				if (sharedRef instanceof ISharedOverbraid) {
					iscm.addSharedOverbraid((ISharedOverbraid) sharedRef);
				}
				else if (sharedRef instanceof ISharedMulticore) {
					iscm.addSharedMulticore((ISharedMulticore) sharedRef);
				}
			}

			removeIndicatorsIfAllLocalConductorsAreRemoved(mcp);
		}
	}

	private void resetSharedMCParent(IUIDObject logicRef, ISharedMulticore sharedMulticore,
			@Nullable IMulticore parentLogicRef,
			@Nullable ISharedMulticore parentSharedRef, Set<ISharedObject> sharedChanges,
			Set<ISharedMulticore> impactedSharedParentMCs)
			throws SharedObjectRevisionHelper.SharedObjectLockException
	{
		ISharedMulticore oldParent = sharedMulticore.getParent();
		if (oldParent != null) {
			oldParent.removeMulticore(sharedMulticore);
		}
		if (parentSharedRef != null) {
			impactedSharedParentMCs.add(parentSharedRef);
			parentSharedRef.addMulticore(sharedMulticore);
			sharedChanges.add(sharedMulticore);
			sharedChanges.add(parentSharedRef);
			// If we've added a shared conductor that has connectivity in the current design to a shared multicore
			// without connectivity in the current design, call the fixup method to create connectivity for the
			// shared multicore and add schem indicators.
			if (logicRef instanceof IMulticore && parentLogicRef == null) {
				SharedConductorHelper.fixupParentageForMulticore((IMulticore) logicRef, getDesign());
			}
		}
		if (oldParent == null && parentSharedRef != null) {
			removeChildRevisionStrucAndUpdateParentValue(sharedMulticore, parentSharedRef);
		}
	}

	private void removeMC(MCProxy mcp, Set<MCProxy> removedMCs, Set<MCProxy> addedMCs)
	{
		removedMCs.add(mcp);
		addedMCs.remove(mcp);
	}

	protected void removeIndicatorsIfAllLocalConductorsAreRemoved(MCProxy mcp)
	{
		if (!mcp.isShielded()) {
			IMulticore multicore = CommonUtils.cast(mcp.getRef(), IMulticore.class);
			// Register this multicore for shield removal only if the multicore has a shield
			// Otherwise we will end up loading diagrams unnecessarily
			if (multicore != null && multicore.getShield() != null) {
				schematicUpdateProcessor.registerCandidateToRemoveShield(mcp);
			}
		}
		if (countLocalConductors(mcp) == 0) {
			// All conductors have been removed from this multicore or overbraid. )It must have a library part,
			// otherwise we wouldn't be here.) Remove the schem indicators.
			schematicUpdateProcessor.registerCandidateToRemoveIndicators(mcp);
		}
	}

	private void processMulticoreRemovals(Collection<IUndoableObject> newObjects,
			Set<MCProxy> removedMCs, Set<MCProxy> changedIndicators, Set<MCProxy> modifiedMCs,
			Set<ISharedObject> sharedChanges, Set<IRevisionedSharedObject> sharedDeletions)
	{
		// Delete the Multicores on the Remove list.
		for (MCProxy badmp : removedMCs) {
			modifiedMCs.remove(badmp);
			for (int idx = 0; idx < badmp.getChildCount(); idx++) {
				MCProxy mcpi = badmp.getChildProxyAt(idx);
				modifiedMCs.remove(mcpi);
			}
			//
			// Get the object, then go and get its shield bodies.
			// and remove them and the gfx.
			//
			if (badmp.getRef() instanceof IMulticore) {
				IMulticore mcrem = (IMulticore) badmp.getRef();
				// Can we move this deletion outside of this loop ?
				schematicUpdateProcessor.registerCandidateToDeleteMulticore(mcrem);
				newObjects.remove(mcrem);
			}

			if (badmp.getSharedRef() instanceof ISharedMulticore) {
				// If a shared multicore got on the deletion list, it must be because all of its members with
				// representations in the current design have been removed. This assumption is valid because
				// that is the only way to delete a shared multicore at present (9/10/2005). If this changes,
				// and shared multicores may be deleted directly by using the Delete button, this logic
				// will have to change.

				// If all members of the shared multicore have been removed (not just the ones the current design
				// uses), the shared multicore must be deleted, too.
				ISharedConductorMgr iscm = getSharedConductorMgr();
				//					if (countAllConductors((ISharedMulticore) badmp.getSharedRef()) == 0) {
				if (((ISharedMulticore) badmp.getSharedRef()).getAllSharedConductorUIDSInHierarchy(false)
						.isEmpty()) {
					ISharedMulticore sharedMulticore = (ISharedMulticore) badmp.getSharedRef();
					ISharedMulticore sharedMulticoreParent = sharedMulticore.getParent();
					if (sharedMulticoreParent != null) {
						sharedMulticoreParent.removeMulticore(sharedMulticore);
						sharedChanges.add(sharedMulticoreParent);
					}
					if (sharedMulticore instanceof ISharedOverbraid) {
						iscm.removeSharedOverbraid((ISharedOverbraid) sharedMulticore);
					}
					else {
						iscm.removeSharedMulticore(sharedMulticore);
					}
					sharedDeletions.add(sharedMulticore);
					if (sharedMulticore.getShield() != null) {
						iscm.removeSharedConductor(sharedMulticore.getShield());
						sharedDeletions.add(sharedMulticore.getShield());
					}
					for (int i = 0; i < badmp.getChildCount(); i++) {
						MCProxy ri = badmp.getChildProxyAt(i);
						changedIndicators.remove(ri);
					}
				}
				else if (badmp.isNew()) {
					// If we're going to keep this shared multicore after all, and it is new,
					// add it to the managers.
					ISharedMulticore smc = (ISharedMulticore) badmp.getSharedRef();
					if (smc instanceof ISharedOverbraid) {
						iscm.addSharedOverbraid((ISharedOverbraid) smc);
					}
					else {
						iscm.addSharedMulticore(smc);
					}
				}
			}
		}
	}

	private void processChangedIndicators(Collection<IUndoableObject> newObjects, Set<MCProxy> changedIndicators,
			Set<MCProxy> modifiedMCs)
	{
		//
		// Next, go through the added/removed indicators.
		// and sort them out.
		//
		for (MCProxy indicatorProxy : changedIndicators) {
			MCProxy multicoreProxy = indicatorProxy.getParentProxy();
			String newIndicatorParamType = indicatorProxy.getName();
			boolean isTwistIndicator = IndicatorHelper.isTwistIndicator(newIndicatorParamType);
			String sheathGroupType = IndicatorHelper.getSheathTypeForIndicator(isTwistIndicator);
			IMulticore multicore = CommonUtils.cast(multicoreProxy.getRef(), IMulticore.class);
			ISharedMulticore sharedMulticore = CommonUtils.cast(multicoreProxy.getSharedRef(), ISharedMulticore.class);
			if (multicore != null) {
				IShieldBody sb = multicore.getShieldBody();
				if (sb == null) {
					// Create a shield body and place the indicators (this should only be necessary for new multicores)
					sb = FactoryMgr.getCablePropertiedFactory().createShieldBody(FactoryMgr.createUID(), multicore);
					LogicObjectLockFinder.tryEdit(getDesign(), multicore);
					sb.setMulticore(multicore);
					indicatorProxy.setRef(sb);
					if (sb instanceof IUndoableObject) {
						newObjects.add((IUndoableObject) sb);
					}
					multicore.setSheathType(sheathGroupType);
					sb.setType(newIndicatorParamType);
					// Add it to the modifiedMulticores so that can generate the indicators. Do not do it here as this
					// may have child multicores and we wont have created their indicators yets
					modifiedMCs.add(multicoreProxy);
				}
				else {
					// If the indicator type has really changed, regenerate the schem indicators for the shield body
					String oldIndicatorType = sb.getType();
					if (!newIndicatorParamType.equals(oldIndicatorType)) {
						multicore.setSheathType(sheathGroupType);
						sb.setType(newIndicatorParamType);
						schematicUpdateProcessor.registerCandidateToChangeIndicators(multicoreProxy);
					}
				}
			}
			else if (sharedMulticore != null) {
				IStringIterator indicators = sharedMulticore.getIndicators();
				String oldIndicatorType = indicators.hasNext() ? indicators.getNext() : "";
				if (!newIndicatorParamType.equals(oldIndicatorType)) {
					schematicUpdateProcessor.registerCandidateToChangeIndicators(multicoreProxy);
					sharedMulticore.removeAllIndicators();
					sharedMulticore.addIndicator(newIndicatorParamType);
					sharedMulticore.setSheathType(sheathGroupType);
				}
			}
		}
	}

	private void processModifiedMCs(Set<IMulticore> librariedMulticores, Set<MCProxy> modifiedMCs)
	{
		// Create or replace schem for modified multicores
		for (MCProxy mcp : modifiedMCs) {
			IMulticore mc = CommonUtils.cast(mcp.getRef(), IMulticore.class);
			if (mc != null) {
				//FEAT#15507 => In case of a libraried innercore MC, need to update indicators for top level MCs also
				updateRootMCifLibrariedMC(librariedMulticores, modifiedMCs, mc);
			}
			schematicUpdateProcessor.registerCandidateToCleanLibIndicators(mcp);
		}
	}

	protected void updateRootMCifLibrariedMC(Set<IMulticore> librariedMulticores, Set<MCProxy> modifiedMCs,
			IMulticore mc)
	{
		if (mc.isPartAssigned() && mc.getParent() != null) {
			IMulticore rootMC = mc.getRootMulticore();
			if (!modifiedMCs.contains(rootMC)) {
				librariedMulticores.add(mc.getRootMulticore());
			}
		}
	}

	private void placeIndicatorsForLibrariedMCs(Set<IMulticore> librariedMulticores)
	{
		for (IMulticore mc : librariedMulticores) {
			schematicUpdateProcessor.registerCandidateToPlaceIndicators(mc, mc.getSharedMulticore());
		}
	}

	protected void saveSharedObjectChanges(Set<ISharedObject> sharedChanges,
			SetMap<IUID, IUID> removedSharedMC2ConductorMap, Set<IRevisionedSharedObject> sharedCreations,
			Set<IRevisionedSharedObject> sharedDeletions) throws UserSessionException
	{
		// Save shared changes to database
		if (!sharedDeletions.isEmpty() || !sharedChanges.isEmpty() || !sharedCreations.isEmpty()) {
			IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
			String projectUidStr = m_project.getUID().getString();
			ISharedConductorMgr iscm = getSharedConductorMgr();
			sendAddRequestsToDB(auditLogger, projectUidStr, iscm, sharedCreations, sharedDeletions);
			sendUpdateRequeststoDB(sharedChanges, sharedCreations, sharedDeletions);
			sendDeletionRequestsToDB(auditLogger, projectUidStr, removedSharedMC2ConductorMap, sharedDeletions, iscm);
			iscm.fireChangeEvent();
		}
	}

	protected void saveDesign(@NotNull ILogicDesign impactedDesign)
			throws UserSessionException
	{
		if (isSharedEditScope()) {
			new CAFCommandHelper().saveDesign(impactedDesign);
		}
	}

	private void registerAffectedConductors(@NotNull MCProxy proxy)
	{
		schematicUpdateProcessor.registerAsAffectedConductor(proxy.getRef(), proxy.getSharedRef());
		for (int idx = 0; idx < proxy.getChildCount(); idx++) {
			MCProxy childProxy = proxy.getChildProxyAt(idx);
			if (childProxy != null) {
				registerAffectedConductors(childProxy);
			}
		}
	}

	public void processChanges(Set<ILogicDesign> designsImpacted, Set<MCProxy> addedMCs, Set<MCProxy> removedMCs,
			Set<MCProxy> modifiedMCs, Set<MCProxy> changedIndicators, Set<ISharedObject> sharedChanges)
			throws UserSessionException
	{
		addedMCs.forEach(this::registerAffectedConductors);
		removedMCs.forEach(this::registerAffectedConductors);
		modifiedMCs.forEach(this::registerAffectedConductors);
		changedIndicators.forEach(this::registerAffectedConductors);
		Set<MCProxy> impactedMCProxies = new HashSet<>(modifiedMCs);
		impactedMCProxies.addAll(removedMCs);
		impactedMCProxies.forEach(m -> schematicUpdateProcessor.registerCandidateToCleanBlankIndicators(m));
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		Collection<IUndoableObject> newObjects = new ArrayList<IUndoableObject>();
		Set<IRevisionedSharedObject> sharedCreations = new HashSet<IRevisionedSharedObject>();

		Set<IMulticore> librariedMulticores =
				processMulticoreAdditions(newObjects, addedMCs, modifiedMCs, sharedCreations);
		SetMap<IUID, IUID> removedSharedMC2ConductorMap = getSharedMCToCondcutorMap(removedMCs);
		Set<IRevisionedSharedObject> lockedObjs = new HashSet<IRevisionedSharedObject>();
		Set<IMulticore> impactedParentMCs = new HashSet<IMulticore>();
		Set<ISharedMulticore> impactedSharedParentMCs = new HashSet<ISharedMulticore>();

		try {
			lockRevisionsOfNewlyAddedInnercores(getProxyRoot(), lockedObjs);
			// Traverse the proxy tree and adjust the parent/child relationships
			createMissingLibraryInnerMCs(modifiedMCs, impactedParentMCs);
			createShieldForMCs(newObjects, addedMCs, sharedCreations);
			walkTree(getProxyRoot(), newObjects, removedMCs, addedMCs, sharedChanges,
					removedSharedMC2ConductorMap, impactedParentMCs, impactedSharedParentMCs);
			createShieldForMCs(newObjects, modifiedMCs, sharedCreations);
		}
		finally {
			for (ISharedLockableUpdateableObject sharedObj : lockedObjs) {
				sharedObj.unlock();
			}
		}

		Set<IRevisionedSharedObject> sharedDeletions = new HashSet<IRevisionedSharedObject>();
		processMulticoreRemovals(newObjects, removedMCs, changedIndicators, modifiedMCs, sharedChanges,
				sharedDeletions);

		propagateHarnessAttribute(impactedParentMCs, impactedSharedParentMCs);
		//
		// Reuse the generator...
		//
		processChangedIndicators(newObjects, changedIndicators, modifiedMCs);
		processModifiedMCs(librariedMulticores, modifiedMCs);

		//FEAT#15507=> In case of adding nested libraried MCs in "Generic MC" dialog, create indicators for all the
		//MCs in the hierarchy
		placeIndicatorsForLibrariedMCs(librariedMulticores);

		//do this in the end after the hierarchy is constructed so that the
		//impacted conductors and diagrams could be computed correctly.
		addedMCs.forEach(schematicUpdateProcessor::registerCandidateToCleanLibIndicators);

		//
		// Lastly, mark the new multicores as OK to snapshot.
		//
		for (IUndoableObject obj : newObjects) {
			cdh.addCreationObject(obj);
		}

		Consumer<ILogicDesign> designSchematicUpdate = schematicUpdateProcessor::regenerateMulticoreIndicators;
		saveSharedObjectChanges(sharedChanges, removedSharedMC2ConductorMap, sharedCreations, sharedDeletions);
		updateImpactedDesigns(getDesign(), designsImpacted, designSchematicUpdate, designSchematicUpdate);
	}

	private void createShieldForMCs(@NotNull Collection<IUndoableObject> newObjects, Set<MCProxy> MCProxies,
			Set<IRevisionedSharedObject> sharedCreations)
	{
		//handle the shield conductor creation. do it for new and modified both.
		for (MCProxy candidateForShield : MCProxies) {
			constructShieldConductorIfNeeded(candidateForShield, newObjects, sharedCreations);
		}
	}

	protected void propagateHarnessAttribute(Set<IMulticore> impactedParentMCs,
			Set<ISharedMulticore> impactedSharedParentMCs)
	{
		HarnessAttributeUpdater harnessAttributeUpdater = new HarnessAttributeUpdater();
		harnessAttributeUpdater.syncMulticores(impactedParentMCs, impactedSharedParentMCs);
	}

	private SetMap<IUID, IUID> getSharedMCToCondcutorMap(Set<MCProxy> removedMCs)
	{
		SetMap<IUID, IUID> removedSharedMC2ConductorMap = new SetMap<IUID, IUID>();
		for (MCProxy mcp : removedMCs) {
			if (mcp.getSharedRef() instanceof ISharedMulticore) {
				ISharedMulticore shMC = (ISharedMulticore) mcp.getSharedRef();
				for (ISharedConductor shCond : shMC.getConductorsIncludingShields()) {
					removedSharedMC2ConductorMap.add(shMC.getUID(), shCond.getUID());
				}
			}
		}
		return removedSharedMC2ConductorMap;
	}

	public void updateImpactedDesigns(@NotNull ILogicDesign currentDesign,
			@NotNull Set<ILogicDesign> otherDesignsImpacted,
			@NotNull Consumer<ILogicDesign> currDesignSchematicUpdate,
			@NotNull Consumer<ILogicDesign> otherDesignSchematicUpdate) throws UserSessionException
	{
		IPromise promise = PromiseFactory.createPromise();
		try {
			LogicDesignPromiseHelper.whilstFreezingDesignDependentLoadsDo(promise, m_project, Boolean.class, () -> {
				try {
					updateImpactedDesignsWithoutPromise(currentDesign, otherDesignsImpacted, currDesignSchematicUpdate,
							otherDesignSchematicUpdate);
					return true;
				}
				catch (UserSessionException e) {
					throw new RuntimeException(e);
				}
			});
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace();
			throw new UserSessionException(ex.getMessage());
		}

	}

	private void updateImpactedDesignsWithoutPromise(@NotNull ILogicDesign currentDesign,
			@NotNull Set<ILogicDesign> otherDesignsImpacted,
			@NotNull Consumer<ILogicDesign> currDesignSchematicUpdate,
			@NotNull Consumer<ILogicDesign> otherDesignSchematicUpdate) throws UserSessionException
	{
		currDesignSchematicUpdate.accept(currentDesign);
		saveDesign(currentDesign);
		IProject currentProject = currentDesign.getProject();
		IMemoryManager memoryManager = FactoryMgr.getMemoryManager();
		try (IProjectMemorySnapshot projectMemorySnapshot = memoryManager.snapshot(currentProject, true)) {
			for (ILogicDesign designToUpdate : otherDesignsImpacted) {
				updateImpactedDesign(designToUpdate, otherDesignSchematicUpdate);
				saveDesign(designToUpdate);
				memoryManager.revertDesignToSnapshot(designToUpdate, projectMemorySnapshot);
			}
		}
	}

	public void updateImpactedDesign(@NotNull ILogicDesign impactedDesign,
			@NotNull Consumer<ILogicDesign> otherDesignSchematicUpdate)
	{
		// CARCH-1291 - changed the creation deletion helper guard
		try (IGuard ignored = CreationDeletionHelper.createDisableCreationDeletionHelperInThreadGuard()) {
			IConnectivity connectivity = impactedDesign.getConnectivity();
			assert connectivity != null;
			SharedConductorHelper.fixMissingParentsOfAllSharedMCs(impactedDesign);
			SharedConductorHelper.fixMissingParentsOfAllSharedConductors(connectivity);
			//			SharedConductorHelper.fixMissingDescendantsOfAllSharedMCs(connectivity);
			otherDesignSchematicUpdate.accept(impactedDesign);
		}
	}

	private void sendAddRequestsToDB(IAuditTrailLogger auditLogger, String projectUidStr, ISharedConductorMgr iscm,
			Set<IRevisionedSharedObject> sharedCreations, Set<IRevisionedSharedObject> sharedDeletions)
	{
		for (IRevisionedSharedObject shared : sharedCreations) {
			if (!sharedDeletions.contains(shared)) {
				shared.flushNew(iscm.getObjType(), iscm);
				auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_ADDED, null, projectUidStr,
						shared.getFullName(), shared.getUID().getString());
			}
		}
	}

	@SuppressWarnings("RedundantThrowsDeclaration")
	protected void sendUpdateRequeststoDB(Set<ISharedObject> sharedChanges,
			Set<IRevisionedSharedObject> sharedCreations, Set<IRevisionedSharedObject> sharedDeletions)
			throws UserSessionException
	{
		IUserSession db = UtilsHelper.getCHSSystem().getData();
		if (db == null) { // If we don't have a connection, just return
			return;
		}
		for (ISharedObject shared : sharedChanges) {
			ISharedLockableUpdateableObject so = shared.getLockableUpdateableRoot();
			if (so == null) {
				throw new IllegalArgumentException("ISharedLockableUpdateableObject not found");
			}
			if (!sharedCreations.contains(shared) && !sharedDeletions.contains(shared)) {
				//dts0100882054 STC1 Bash12:Regression:Deleting MC from the shared tab leading to server side exception.
//					db.deleteData(shared.getUID().getString(), shared.getObjType());
//					so.flushNew(iscm.getObjType(), iscm);
				//Putting back this fix originally made for (dts0100882054). Now for below DR:
				// dts0100958882 Show usages are not correctly shown when the shared MC is edited in one design.
				so.flush();
			}
		}
	}

	protected void sendDeletionRequestsToDB(IAuditTrailLogger auditLogger, String projectUidStr,
			SetMap<IUID, IUID> removedSharedMC2ConductorMap, Set<IRevisionedSharedObject> sharedDeletions,
			ISharedConductorMgr sharedConductorMgr)
			throws UserSessionException
	{
		IUserSession db = UtilsHelper.getCHSSystem().getData();
		if (db == null) { // If we don't have a connection, just return
			return;
		}
		for (IRevisionedSharedObject shared : sharedDeletions) {
			db.deleteData(shared.getUID().getString(), shared.getObjType());
			auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_DELETED, null, projectUidStr,
					shared.getFullName(), shared.getUID().getString());
			if (shared instanceof ISharedMulticore) {
				Set<IUID> innerCores = removedSharedMC2ConductorMap.getSet(shared.getUID());
				for (IUID condUID : innerCores) {
					deleteSharedConductorMember(condUID, shared.getUID());
				}
			}
		}
		if (!sharedDeletions.isEmpty()) {
			sharedConductorMgr.flush();
		}
	}

	private static void deleteSharedConductorMember(IUID childConductor, IUID parentOverbraid)
			throws UserSessionException
	{
		IUserSession userSession = FactoryMgr.getSystemFactory().getCHSSystem().getUserSession();
		userSession.deleteAssociation("sharedconductormember", new String[]
						{"ref", "sharedmulticore_id"}
				,
				new String[]
						{childConductor.getString(), parentOverbraid.getString()});
	}

	private static boolean shouldDelete(MCProxy mcp)
	{
		// Libraried and non-libraried overbraids are treated in the same way, there is no differentiation between them
		return countLocalConductors(mcp) == 0 && (mcp.isOverbraidRef() || !mcp.isLibraryPartRef());
	}

	private static int countLocalConductors(MCProxy mcp)
	{
		return countConductorsAndContainers(mcp, true);
	}

	private static int countConductorsAndContainers(MCProxy mcp, boolean requireConnectivity)
	{
		int count = 0;
		for (int idx = 0; idx < mcp.getChildCount(); idx++) {
			MCProxy mcpc = mcp.getChildProxyAt(idx);
			assert mcpc != null;
			// To count, child must be a conductor, multicore or overbraid with connectivity in the current design
			if ((mcpc.isConductorRef() || (mcpc.isContainerRef() && !shouldDelete(mcpc))) &&
					(!requireConnectivity || mcpc.getRef() instanceof ILogicObject)) {
				count++;
			}
		}
		return count;
	}

	public boolean lockImpactedDesigns(Set<ILogicDesign> designsToBeUnlocked, Set<ISharedObject> sharedChanges,
			Set<ILogicDesign> designsToBeUpdated)
	{
		Set<ILogicDesign> designsToBeLocked = new HashSet<ILogicDesign>();
		getImpactedDesigns(designsToBeLocked, designsToBeUpdated, sharedChanges);
		return lockDesigns(sharedChanges, designsToBeLocked, designsToBeUnlocked);
	}

	protected class SchematicIndicatorUpdateProcessor
	{

		private Set<IUIDObject> m_placeIndicators = new HashSet<>();
		private Set<IUIDObject> m_changeIndicators = new HashSet<>();
		private Set<IUIDObject> m_removeShields = new HashSet<>();
		private Set<IUIDObject> m_deletions = new HashSet<>();
		private Set<IUIDObject> m_removeIndicators = new HashSet<>();
		private Set<IUIDObject> m_cleanBlankIndicators = new HashSet<>();
		private Set<IUIDObject> m_cleanLibIndicators = new HashSet<>();
		//this will hold the initial set of conductors in the impacted multi-cores.
		//will help accurately compute affected diagrams to process.
		private Set<IUIDObject> m_affectedConductors = new HashSet<>();

		private void doRegisterCandidate(@NotNull Set<IUIDObject> store, @NotNull MCProxy proxy)
		{
			IMulticore multicore = CommonUtils.cast(proxy.getRef(), IMulticore.class);
			ISharedMulticore sharedMulticore = CommonUtils.cast(proxy.getSharedRef(), ISharedMulticore.class);
			doRegisterCandidate(store, multicore, sharedMulticore);
		}

		private void doRegisterCandidate(@NotNull Set<IUIDObject> store, @Nullable IMulticore multicore,
				@Nullable ISharedMulticore sharedMulticore)
		{
			if (multicore != null) {
				if (store.add(multicore)) {
					m_affectedConductors.addAll(multicore.getAllConductorsInHierarchy(true));
				}
			}
			if (sharedMulticore != null) {
				if (store.add(sharedMulticore)) {
					for (ISharedConductor sharedConductor : sharedMulticore.getAllSharedConductorsInHierarchy(true)) {
						m_affectedConductors.add(sharedConductor);
					}
				}
			}
		}

		public void registerCandidateToPlaceIndicators(@Nullable IMulticore multicore,
				@Nullable ISharedMulticore sharedMulticore)
		{
			doRegisterCandidate(m_placeIndicators, multicore, sharedMulticore);
		}

		public void registerCandidateToChangeIndicators(@NotNull MCProxy proxy)
		{
			doRegisterCandidate(m_changeIndicators, proxy);
		}

		public void registerCandidateToRemoveShield(@NotNull MCProxy proxy)
		{
			//this case would arise only for current design only?
			doRegisterCandidate(m_removeShields, CommonUtils.cast(proxy.getRef(), IMulticore.class), null);
		}

		public void registerCandidateToDeleteMulticore(@NotNull IMulticore multicore)
		{
			//the deletion of multicore is applicable only on current design. so passing shared
			//component as null. in case of shared edit if we are creating a new multicore we
			//create a multicore in current design and if shared conductors are not placed in
			//current design then that newly created multicore is marked for deletion. currently
			//we don't allow a case where a multicore is needed to be deleted in another design.
			doRegisterCandidate(m_deletions, multicore, null);
		}

		public void registerCandidateToRemoveIndicators(@NotNull MCProxy proxy)
		{
			//this case would arise only for current design only?
			doRegisterCandidate(m_removeIndicators, CommonUtils.cast(proxy.getRef(), IMulticore.class), null);
		}

		public void registerCandidateToCleanBlankIndicators(@NotNull MCProxy proxy)
		{
			doRegisterCandidate(m_cleanBlankIndicators, proxy);
		}

		public void registerCandidateToCleanLibIndicators(@NotNull MCProxy proxy)
		{
			doRegisterCandidate(m_cleanLibIndicators, proxy);
		}

		public void destroy()
		{
			m_placeIndicators.clear();
			m_changeIndicators.clear();
			m_removeShields.clear();
			m_deletions.clear();
			m_removeIndicators.clear();
			m_cleanBlankIndicators.clear();
			m_cleanLibIndicators.clear();
		}

		public void registerAsAffectedConductor(@Nullable IUIDObject ref, @Nullable IUIDObject sharedRef)
		{
			IConductor conductor = CommonUtils.cast(ref, IConductor.class);
			ISharedConductor sharedConductor = CommonUtils.cast(sharedRef, ISharedConductor.class);
			if (conductor != null) {
				m_affectedConductors.add(conductor);
			}
			if (sharedConductor != null) {
				m_affectedConductors.add(sharedConductor);
			}
			IMulticore multicore = CommonUtils.cast(ref, IMulticore.class);
			ISharedMulticore sharedMulticore = CommonUtils.cast(sharedRef, ISharedMulticore.class);
			if (multicore != null) {
				m_affectedConductors.addAll(multicore.getAllConductorsInHierarchy(true));
			}
			if (sharedMulticore != null) {
				for (ISharedConductor sCond : sharedMulticore.getAllSharedConductorsInHierarchy(true)) {
					m_affectedConductors.add(sCond);
				}
			}
		}

		@NotNull private <T extends ILogicObject> Set<T> determineObjectsToProcess(@NotNull Set<IUIDObject> source,
				@NotNull Collection<T> allObjects)
		{
			Set<T> candidates = new HashSet<>();
			for (T object : allObjects) {
				if (source.contains(object)) {
					candidates.add(object);
				}
				ISharedObject sharedObject = object.getSharedObject();
				if (sharedObject != null) {
					if (source.contains(sharedObject)) {
						candidates.add(object);
					}
				}
			}
			return Collections.unmodifiableSet(candidates);
		}

		public void regenerateMulticoreIndicators(@NotNull ILogicDesign designToProcess)
		{
			IConnectivity connectivity = designToProcess.getConnectivity();
			List<IMulticore> allMulticores = Collections.emptyList();
			Set<IConductor> allConductors = Collections.emptySet();
			if (connectivity != null) {
				allMulticores = connectivity.getMulticores(true, false);
				allConductors = CollectionUtils.createSet(connectivity.getConductors());
			}

			Set<IMulticore> placeIndicators = determineObjectsToProcess(m_placeIndicators, allMulticores);
			Set<IMulticore> changeIndicators = determineObjectsToProcess(m_changeIndicators, allMulticores);
			Set<IMulticore> removeShields = determineObjectsToProcess(m_removeShields, allMulticores);
			Set<IMulticore> deletions = determineObjectsToProcess(m_deletions, allMulticores);
			Set<IMulticore> removeIndicators = determineObjectsToProcess(m_removeIndicators, allMulticores);
			Set<IMulticore> cleanBlankIndicators = determineObjectsToProcess(m_cleanBlankIndicators, allMulticores);
			Set<IMulticore> cleanLibIndicators = determineObjectsToProcess(m_cleanLibIndicators, allMulticores);

			final Set<ISchemDiagram> diagramsToProcess = new HashSet<>();
			IDesignWideUsageMgr designWideUsageMgr = designToProcess.getDesignWideUsageMgr();
			for (IConductor candidate : determineObjectsToProcess(m_affectedConductors, allConductors)) {
				diagramsToProcess.addAll(designWideUsageMgr.getUsageDiagrams(candidate));
			}
			//This is a performance issue because we are loading all diagrams.
			//but we have no options. cable shildbody doesn't belong to usage manager
			//usages and the conductors have already been removed from the multicore
			//so not able to use conductors to get schem conductor representations.
			//sorry no wasy to resolve this at this point???? so using all the diagrams??
			// deletion of shield bodies and shield conductors if possible, will be handled by Delete code
			//attempt locking these diagrams and also ensure they are loaded.
			designToProcess.lockDiagrams(diagramsToProcess);
			diagramsToProcess.forEach(ISchemDiagram::loadToMemory);

			new DiagramRepresentationUpdater(diagramsToProcess).processDiagrams((diagram) -> {
				processCandidatesToRemoveShield(removeShields, diagram);
				processCandidatesToRemoveIndicators(removeIndicators, diagram);
				processCandidatesToCleanLibIndicators(cleanLibIndicators, diagram);
				processCandidatesToDeleteMulticore(deletions, diagram);
				processCandidatesToCleanBlankIndicators(cleanBlankIndicators, diagram);
				processCandidatesToChangeIndicators(changeIndicators, diagram);
				processCandidatesToPlaceIndicators(placeIndicators, diagram);
			});
			if (diagramsToProcess.isEmpty()) {
				//we should be able to delete already empty multicores also.
				processCandidatesToDeleteMulticore(deletions, getDiagram());
			}
			//after schematic deletion of shields. shouldn't we delete the connectivity also. can they be chained shields?
		}

		private boolean shouldAddHookUps(@NotNull IMulticore mc)
		{
			ISharedMulticore sharedMC = mc.getSharedMulticore();
			return !isSharedEditScope() || mc.getShield() != null || (sharedMC != null && sharedMC.getShield() != null);
		}

		private void processCandidatesToDeleteMulticore(@NotNull Set<IMulticore> deletions,
				@NotNull ISchemDiagram diagram)
		{
			DeleteHelper.getInstance().delete(diagram, deletions, true);
		}

		private void processCandidatesToRemoveIndicators(@NotNull Set<IMulticore> removeIndicators,
				@NotNull ISchemDiagram diagram)
		{
			for (IMulticore removeIndicator : removeIndicators) {
				IShieldBody sb = removeIndicator.getShieldBody();
				if (sb != null) {
					for (IDiagramObject schemSB : diagram.getRepresentations(sb.getUID())) {
						schemSB.delete();
						diagram.removeObject(schemSB);
					}
				}
			}
		}

		private void processCandidatesToCleanBlankIndicators(@NotNull Set<IMulticore> cleanBlankIndicators,
				@NotNull ISchemDiagram diagram)
		{
			IndicatorRefresher.getIndicatorRefresher(diagram).removeIndicators();
		}

		private void processCandidatesToChangeIndicators(@NotNull Set<IMulticore> changeIndicators,
				@NotNull ISchemDiagram diagram)
		{
			for (IMulticore changeIndicator : changeIndicators) {
				MulticoreUtils.redrawSchemIndicators(changeIndicator, diagram, true);
			}
		}

		private void processCandidatesToPlaceIndicators(@NotNull Set<IMulticore> placeIndicators,
				@NotNull ISchemDiagram diagram)
		{
			Generator gen = Generator.getGenerator();
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			for (IMulticore placeIndicator : placeIndicators) {
				IShieldBody sb = placeIndicator.getShieldBody();
				if (sb != null) {
					boolean addHookups = shouldAddHookUps(placeIndicator);
					Placement.placeIndicators(gen, diagram, placeIndicator, sb, gp, addHookups, null, true, false);
					MulticoreUtils.redrawSchemIndicators(placeIndicator, diagram, false);
				}
			}
		}

		private void processCandidatesToCleanLibIndicators(@NotNull Set<IMulticore> cleanLibIndicators,
				@NotNull ISchemDiagram diagram)
		{
			Generator gen = Generator.getGenerator();
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			for (IMulticore cleanLibIndicator : cleanLibIndicators) {
				IShieldBody sb = cleanLibIndicator.getShieldBody();
				if (sb != null) {
					boolean shouldAddHookups = shouldAddHookUps(cleanLibIndicator);
					Placement.placeIndicators(gen, diagram, cleanLibIndicator, sb, gp, shouldAddHookups, null,
							true, false);
					MulticoreUtils.redrawSchemIndicators(cleanLibIndicator, diagram, false);
				}
				//FEAT#15507 - In case of libraried MC, there is a possibility that the wire and the indicator body are
				// present in the digram. Invoke the 'Generic MC' dialog and unassign all the innercores.(For normal MCs
				// , the MC would be deleted in such case). in such scenario, for libraried MCs, delete the indicator body.
				// If this happens to be a innerMC, it can happen that even its ancestors indicator body may also have to
				// be removed. (they themselves may not have any inner core wires).
				if (cleanLibIndicator.isPartAssigned() && sb != null) {
					Set<IConductor> conds = cleanLibIndicator.getAllConductorsInHierarchy(false);
					if (conds.isEmpty()) {
						IMulticore root = cleanLibIndicator.getRootMulticore();
						Set<IMulticore> mcSet = root.getAllMulticoresInHierarchy();
						for (IMulticore mcInHierarchy : mcSet) {
							if (mcInHierarchy.getShieldBody() != null && mcInHierarchy.isPartAssigned()) {
								if (mcInHierarchy.getAllConductorsInHierarchy().isEmpty()) {
									for (IDiagramObject so : diagram.getRepresentations(
											mcInHierarchy.getShieldBody().getUID())) {
										if (so instanceof chs.cof.logical.schem.IShieldBody) {
											so.delete();
										}
									}
								}
							}
						}
					}
				}
			}
		}

		private void processCandidatesToRemoveShield(@NotNull Set<IMulticore> removeShields,
				@NotNull ISchemDiagram diagram)
		{
			for (IMulticore removeShield : removeShields) {
				IShieldConductor shield = removeShield.getShield();
				if (shield != null) {
					// Remove the schem shields.
					for (IDiagramObject so : diagram.getRepresentations(shield.getUID())) {
						so.delete();
						diagram.removeObject(so);
					}
				}
			}
		}
	}
}
