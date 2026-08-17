/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2024 Siemens
 */
package chs.caplets.logic.shared;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.IResource;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.browser.BaseTreeSearchFilter;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.caf.caplet.helpers.browser.BrowserTreeWorker;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.logic.ISharedObjectToolbarProvider;
import chs.caplets.logic.Model;
import chs.caplets.logic.SharedObjectActionUtil;
import chs.caplets.logic.actions.EditSharedOverbraidAction;
import chs.caplets.logic.actions.shared.AddPortAction;
import chs.caplets.logic.actions.shared.AddSharedGeneralHighwayAction;
import chs.caplets.logic.actions.shared.AddSharedMessageAction;
import chs.caplets.logic.actions.shared.AddSharedNetAction;
import chs.caplets.logic.actions.shared.AddSharedShieldAction;
import chs.caplets.logic.actions.shared.AddSharedSignalAction;
import chs.caplets.logic.actions.shared.AddSharedSingleLineAction;
import chs.caplets.logic.actions.shared.AddSharedWireAction;
import chs.caplets.logic.actions.shared.ISharedObjectBrowserAction;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.IInternalPositionBase;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedAbstractable;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFunctionConductor;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedHighway;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.logical.shared.ISharedSplice;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IOptionExpression;
import chs.cof.project.IOptionedObject;
import chs.cof.project.IProject;
import chs.cof.security.IUserAccountDomain;
import chs.common.IDesignAbstraction;
import chs.common.INamedObject;
import chs.common.IReadOnlyNamedObject;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDProvider;
import chs.common.UIDObject;
import chs.common.introspection.ObjectRelationship;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialog;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.MapMap;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.filter.IMatcher;
import chs.utility.ICDUtils;
import chs.utility.SharedObjectComparator;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.revisioning.SharedObjectTreeProcessor;
import chs.utility.logic.UserAccountDomainInfo;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SharedObjectBrowserClient extends BrowserClientHelper
{

	public static final String INNER_CORES_TEXT = ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.Multicores.InnerCores.text");
	public static final String REVISIONS_TEXT = ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.Multicores.Revisions.text");
	public static final String REVISION_TIP = ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.tip.Revisions.text");
	public static final String OPTION_TIP = ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.tip.Option.text");
	private static final String SHAREDSIGNALS_TEXT =
			ResourceMgr
					.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.SharedSignalsFolder.text");
	private static final String SHAREDMESSAGES_TEXT =
			ResourceMgr
					.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.SharedMessagesFolder.text");
	private static final String SHAREDFUNCTIONS_TEXT =
			ResourceMgr
					.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.SharedFunctionsFolder.text");

	private static final String HIGHWAYS_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.HighwaysFolder.text");
	private static final String SINGLE_LINES_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.SingleLinesFolder.text");
	private static final String DEVICES_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.DevicesFolder.text");
	private static final String CONNECTORS_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.ConnectorsFolder.text");
	private static final String RINGTERMINALS_TEXT =
			ResourceMgr
					.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.RingTerminalsFolder.text");
	private static final String INLINES_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.InlinesFolder.text");
	private static final String SPLICES_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.SplicesFolder.text");
	private static final String SHARED_CONDUCTORS_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class,
					"SharedObjectBrowserClient.SharedConductorsFolder.text");
	private static final String MULTICORES_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.MulticoresFolder.text");
	private static final String OVERBRAIDS_TEXT =
			ResourceMgr.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.OverbraidsFolder.text");
	private static final String DESIGNABSTRACTION_TIP = ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.tip.DesignAbstraction.text");
	private static final String NO_PERMISSION_NODE = ResourceMgr.getString(SwapOutSharedObjectRevisionDialog.class,
			"SwapOutSharedObjectRevisionDialog.domainNoAccess.noPermissionText");
	private static final String NO_PERMISSION_NODE_TOOLTIP =  ResourceMgr
			.getString(SharedObjectBrowserClient.class, "SharedObjectBrowserClient.InAccessible.SharedObject.tooltip");
	private static final Object MULTICORE_TYPE = new Object();
	private static final Object OVERBRAID_TYPE = new Object();
	private static final Object MESSAGE_TYPE = new Object();
	private static final Object CONDUCTOR_TYPE = new Object();
	private static String HTML_HEADER = "<html><body>";
	private static String HTML_FOOTER = "</body></html>";
	private final List<IUID> m_folders = new ArrayList<IUID>();
	private final SharedObjectBrowserClientComparator m_comparator = new SharedObjectBrowserClientComparator();
	private ILogicDesign m_design;
	private JToolBar toolbar = null;
	private BrowserFolder m_devicesFolder;
	private BrowserFolder m_connectorsFolder;
	private BrowserFolder m_ringTerminalsFolder;
	private BrowserFolder m_splicesFolder;
	private BrowserFolder m_inlinesFolder;
	private BrowserFolder sharedConductorsFolder;
	private BrowserFolder sharedMessagesFolder;
	private BrowserFolder m_highwaysFolder;
	private BrowserFolder m_singleLinesFolder;
	private BrowserFolder m_multicoresFolder;
	private BrowserFolder m_sharedFunctionsFolder;
	private BrowserFolder m_overbraidsFolder;
	private SharedObjectTreeProcessor.Tree m_sharedHighwayTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedSingleLineTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedDeviceTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedConnectorTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedRingTerminalTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedInlineTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedSpliceTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedConductorTreeStructure;
	private SharedObjectTreeProcessor.Tree m_sharedFunctionTreeStructure;
	private SharedObjectTreeProcessor.Tree portedConductorTreeStructure;
	private Map<IUIDObject, SharedObjectTreeProcessor.Node> uidToNodeMap;
	private Map<ISharedMulticore, InnerCoresTreeItem> innerCoresMap =
			new HashMap<ISharedMulticore, InnerCoresTreeItem>();
	private Map<ISharedMulticore, MulticoreRevisionsTreeItem> multicoreRevisionMap =
			new HashMap<ISharedMulticore, MulticoreRevisionsTreeItem>();
	private Map<ISharedFunctionMessage, FunctionMessageRevisionTreeItem> functionMessageRevisionMap =
			new HashMap<ISharedFunctionMessage, FunctionMessageRevisionTreeItem>();
	private Map<ISharedPinList, ModularConnectorRevisionsTreeItem> modularConnectorRevisionMap =
			new HashMap<ISharedPinList, ModularConnectorRevisionsTreeItem>();
	private MapMap<Object, IDesignAbstraction, AbstractionDeferrer> abstractionDefererMap =
			new MapMap<Object, IDesignAbstraction, AbstractionDeferrer>();
	@Nullable private BuildSharedObjectsUsedInADesign m_buildSharedObjectsUsedInADesign;
	@Nullable private SharedObjectsByAbstraction sharedObjectsByAbstraction;
	@Nullable private SharedTreeBackgroundCache sharedTreeBackgroundCache;

	private Map<ISharedObject, NoAccessNodeTreeItem> noAccessNodeMap =
			new HashMap<>();
	private Set<IUserAccountDomain> userAccountDomains = new HashSet<>();
	@Nullable private BrowserTreeWorker browserTreeWorker = null;
	private boolean destroyed = false;

	public SharedObjectBrowserClient(ICapletController controller)
	{
		super(controller);
		Model model = (Model) controller.getCapletModel();
		m_design = model.getDesign();
		IProject project = m_design.getProject();
		setRootObject(project);
		m_searchFilter = new SharedBrowserTreeFilter();
		createFolderNodes();
		populateUserAccountDomains();
	}

	private void populateUserAccountDomains()
	{
		final Set<IUserAccountDomain> accountDomains = ICDUtils.getUserAccountDomains();
		if (accountDomains != null) {
			userAccountDomains = accountDomains;
		}
	}

	private void createFoldersForFunctionalDesign()
	{
		sharedConductorsFolder = createFolder(SHAREDSIGNALS_TEXT);
		m_folders.add(sharedConductorsFolder.getUID());
		sharedMessagesFolder = createFolder(SHAREDMESSAGES_TEXT);
		m_folders.add(sharedMessagesFolder.getUID());
		m_sharedFunctionsFolder = createFolder(SHAREDFUNCTIONS_TEXT);
		m_folders.add(m_sharedFunctionsFolder.getUID());
	}

	private void createFolderNodes()
	{
		if (isBrowserForFunctionalDesign()) {
			createFoldersForFunctionalDesign();
		}
		else {
			createFoldersForLogicalDesign();
		}
	}

	private void createFoldersForLogicalDesign()
	{
		boolean limitedSet = AppInfo.isCapitalArchitect() || AppInfo.isCapitalCapture();
		// setup the static children of this browser tree
		m_devicesFolder = createFolder(DEVICES_TEXT);
		m_folders.add(m_devicesFolder.getUID());
		if (!limitedSet) {
			m_connectorsFolder = createFolder(CONNECTORS_TEXT);
			m_folders.add(m_connectorsFolder.getUID());
			m_inlinesFolder = createFolder(INLINES_TEXT);
			m_folders.add(m_inlinesFolder.getUID());
			m_splicesFolder = createFolder(SPLICES_TEXT);
			m_folders.add(m_splicesFolder.getUID());
			m_ringTerminalsFolder = createFolder(RINGTERMINALS_TEXT);
			m_folders.add(m_ringTerminalsFolder.getUID());
		}
		sharedConductorsFolder = createFolder(SHARED_CONDUCTORS_TEXT);
		m_folders.add(sharedConductorsFolder.getUID());
		if (AppInfo.isCapitalLogic() || AppInfo.isCapitalCapture()) {
			m_highwaysFolder = createFolder(HIGHWAYS_TEXT);
			m_folders.add(m_highwaysFolder.getUID());

			//we are not supporting single lines in derivative tools
			if(!AppInfo.isCapitalDerivative()) {
				m_singleLinesFolder = createFolder(SINGLE_LINES_TEXT);
				m_folders.add(m_singleLinesFolder.getUID());
			}
		}
		m_multicoresFolder = createFolder(MULTICORES_TEXT);
		m_folders.add(m_multicoresFolder.getUID());
		if (!limitedSet) {
			m_overbraidsFolder = createFolder(OVERBRAIDS_TEXT);
			m_folders.add(m_overbraidsFolder.getUID());
		}
	}

	private static List<IUID> getSharedConductorGroupChildren(ISharedMulticore smc)
	{
		List<IUID> sharedConductorGroupChildren = new ArrayList<IUID>();
		for (ISharedConductorIterator scIt = smc.getConductors(); scIt.hasNext(); ) {
			ISharedConductor sc = scIt.next();
			sharedConductorGroupChildren.add(sc.getUID());
		}
		ISharedConductor shield = smc.getShield();
		if (shield != null) {
			sharedConductorGroupChildren.add(shield.getUID());
		}
		for (ISharedMulticoreIterator smIt = smc.getMulticores(); smIt.hasNext(); ) {
			ISharedMulticore sm = smIt.next();
			sharedConductorGroupChildren.add(sm.getUID());
		}
		return sharedConductorGroupChildren;
	}

	private void clearAndRemoveObjects(Map<?, ? extends UIDObject> map) {
		for (UIDObject treeItem : map.values()) {
			m_uidMgr.removeObject(treeItem.getUID());
		}
		map.clear();
	}

	public void destroy() {
		destroyed = true;
		if (browserTreeWorker != null) {
			browserTreeWorker.cancel(true);
		}
		super.destroy();

		clearAndRemoveObjects(innerCoresMap);
		clearAndRemoveObjects(multicoreRevisionMap);
		clearAndRemoveObjects(functionMessageRevisionMap);
		clearAndRemoveObjects(modularConnectorRevisionMap);
		clearAndRemoveObjects(noAccessNodeMap);

		if (toolbar != null) {
			toolbar.removeAll();
			toolbar = null;
		}

		// jmyvon: added for dts0100718708
		if (uidToNodeMap != null) {
			uidToNodeMap.clear();
			uidToNodeMap = null;
		}
		abstractionDefererMap.clear();
		m_sharedHighwayTreeStructure = null;
		m_sharedSingleLineTreeStructure = null;
		m_sharedDeviceTreeStructure = null;
		m_sharedConnectorTreeStructure = null;
		m_sharedRingTerminalTreeStructure = null;
		m_sharedInlineTreeStructure = null;
		m_sharedSpliceTreeStructure = null;
		m_sharedConductorTreeStructure = null;
		portedConductorTreeStructure = null;

		m_design = null;
	}

	@Override public boolean isDestroyed()
	{
		return destroyed;
	}
	@Override public void startCreation()
	{
		m_comparator.clearCache();
		sharedObjectsByAbstraction = null;
		sharedTreeBackgroundCache = null;
		m_buildSharedObjectsUsedInADesign = null;
		UserAccountDomainInfo.initializeCache();
		new LoadPartsForModularConnectorBrowserIcon(getSharedObjectsByAbstraction()).loadParts();
	}

	@Override public void endCreation()
	{
		m_comparator.clearCache();
		sharedObjectsByAbstraction = null;
		sharedTreeBackgroundCache = null;
		m_buildSharedObjectsUsedInADesign = null;
		UserAccountDomainInfo.clearCache();
	}

	public List<IUID> getChildren(IUID uid)
	{
		if (uid == getRoot()) {
			return getFoldersList();
		}

		return getChildrenList(uid, true);
	}

	public List<IUID> getChildrenWithoutSort(IUID uid)
	{
		if (uid == getRoot()) {
			return getFoldersList();
		}

		return getChildrenList(uid, false);
	}

	private List<IUID> getFoldersList()
	{
		// this means we must be in a refreshing , so reaquire the sharedPinList and SharedConductor structure.
		uidToNodeMap = new HashMap<IUIDObject, SharedObjectTreeProcessor.Node>();

		List<ISharedPinList> sharedPinLists = new ArrayList<ISharedPinList>();

		Set<PinListTypeEnum> pinListTypes =
				Set.of(PinListTypeEnum.TypeDevice, PinListTypeEnum.TypeInterconnectDevice, PinListTypeEnum.TypeGround);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedDeviceTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedDeviceTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		pinListTypes = Set.of(PinListTypeEnum.TypePlug, PinListTypeEnum.TypeJack, PinListTypeEnum.TypeInterconnectConnector);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedConnectorTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedConnectorTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		pinListTypes = Set.of(PinListTypeEnum.TypeInlinePlug, PinListTypeEnum.TypeInlineJack,
				PinListTypeEnum.TypeInlineInterconnectPlug, PinListTypeEnum.TypeInlineInterconnectJack);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedInlineTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedInlineTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		pinListTypes = Set.of(PinListTypeEnum.TypeRingTerminal);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedRingTerminalTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedRingTerminalTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		pinListTypes = Set.of(PinListTypeEnum.TypeSplice);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedSpliceTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedSpliceTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		pinListTypes = Set.of(PinListTypeEnum.TypeFunction);
		sharedPinLists.addAll(getSharedObjectsByAbstraction().getSharedPinLists(null, pinListTypes));

		m_sharedFunctionTreeStructure = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
		uidToNodeMap.putAll(m_sharedFunctionTreeStructure.getUIDObjectToNodeMapping());
		sharedPinLists.clear();

		List<ISharedAbstractable> condMgrObjects = getFilteredSharedCondMgrObjects(null);
		m_sharedConductorTreeStructure =
				SharedObjectTreeProcessor.buildTreeForConductorMgrForDesignAbstraction(condMgrObjects);
		uidToNodeMap.putAll(m_sharedConductorTreeStructure.getUIDObjectToNodeMapping());
		portedConductorTreeStructure =
				SharedObjectTreeProcessor.buildTreeForUnabstractable(m_design);
		uidToNodeMap.putAll(portedConductorTreeStructure.getUIDObjectToNodeMapping());

		List<ISharedAbstractable> filteredHighways = getFilteredHighways(null);
		m_sharedHighwayTreeStructure =
				SharedObjectTreeProcessor.buildHighwaysTreeForDesignAbstraction(filteredHighways);
		uidToNodeMap.putAll(m_sharedHighwayTreeStructure.getUIDObjectToNodeMapping());

		List<ISharedAbstractable> filteredSingleLines = getFilteredSingleLines(null);
		m_sharedSingleLineTreeStructure =
				SharedObjectTreeProcessor.buildSingleLinesTreeForDesignAbstraction(filteredSingleLines);
		uidToNodeMap.putAll(m_sharedSingleLineTreeStructure.getUIDObjectToNodeMapping());
		updateModularRevisionMap();

		return m_folders;
	}

	private void updateModularRevisionMap()
	{
		Set<ISharedPinList> keysSet = new HashSet<ISharedPinList>(modularConnectorRevisionMap.keySet());
		for (ISharedPinList pinlist : keysSet) {
			if (!sharedConnectorExistsInConnectorTree(pinlist)) {
				modularConnectorRevisionMap.remove(pinlist);
			}
		}
	}

	private boolean sharedConnectorExistsInConnectorTree(ISharedPinList pinlist)
	{
		return m_sharedConnectorTreeStructure.getUIDObjectToNodeMapping().containsKey(pinlist);
	}

	public List<IUID> getChildrenList(IUID uid, boolean shouldSortChildren)
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		List<IUID> children = Collections.emptyList();
		IUIDObject obj = getObject(uid);
		//(SP1202)dts0100799426-Shared Interconnect Connectors are not shown in the Shared Object List in Capital Logic if a Design Abstraction is assigned to the design.
		Set<PinListTypeEnum> pinlistTypes = new HashSet<PinListTypeEnum>(1);

		if (obj == m_devicesFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeInterconnectDevice);
			pinlistTypes.add(PinListTypeEnum.TypeGround);
			pinlistTypes.add(PinListTypeEnum.TypeDevice);
			children = getSharedPinLists(null, pinlistTypes, m_sharedDeviceTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedDeviceTreeStructure);
		}
		else if (obj == m_sharedFunctionsFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeFunction);
			children = getSharedPinLists(null, pinlistTypes, m_sharedFunctionTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedFunctionTreeStructure);
		}
		else if (obj == m_connectorsFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeInterconnectConnector);
			pinlistTypes.add(PinListTypeEnum.TypePlug);
			children = getSharedPinLists(null, pinlistTypes, m_sharedConnectorTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedConnectorTreeStructure);
		}
		else if (obj == m_ringTerminalsFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeRingTerminal);
			children = getSharedPinLists(null, pinlistTypes, m_sharedRingTerminalTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedRingTerminalTreeStructure);
		}
		else if (obj == m_inlinesFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeInlinePlug);
			children = getSharedPinLists(null, pinlistTypes, m_sharedInlineTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedInlineTreeStructure);
		}
		else if (obj == m_splicesFolder) {
			pinlistTypes.add(PinListTypeEnum.TypeSplice);
			children = getSharedPinLists(null, pinlistTypes, m_sharedSpliceTreeStructure);
			abstractions = getSharedPinListAbstractions(pinlistTypes, m_sharedSpliceTreeStructure);
		}
		else if (obj == sharedConductorsFolder) {
			children = getSharedConductors(null);
			abstractions = getSharedConductorAbstractions();
		}
		else if (obj == sharedMessagesFolder) {
			children = getSharedMessages(null);
			abstractions = getSharedMessageAbstractions();
		}
		else if (obj == m_highwaysFolder) {
			children = getSharedGeneralHighways(null);
			abstractions = getSharedGeneralHighwayAbstractions();
		}
		else if (obj == m_singleLinesFolder) {
			children = getSharedSingleLines(null);
			abstractions = getSharedSingleLineAbstrations();
		}
		else if (obj == m_multicoresFolder) {
			children = getSharedMulticores(null);
			abstractions = getSharedMulticoreAbstractions();
		}
		else if (obj == m_overbraidsFolder) {
			children = getSharedOverbraids(null);
			abstractions = getSharedOverbraidsAbstrations();
		}
		else if (obj instanceof ISharedGeneralHighway) {
			children = getSharedHighwayConductors((ISharedGeneralHighway) obj);
		}
		else if (obj instanceof ISharedFunctionMessage) {
			if(getRevisionChildren((IRevisionedObject) obj).isEmpty()){
				children = getSharedMessageActiveSignals((ISharedFunctionMessage) obj);
			}else{
				children = getFunctionMessageTreeItems((ISharedFunctionMessage)obj);
			}
		}
		else if (obj instanceof ISharedMulticore smc) {
			if (smc.getParent() != null) {
				// it is a inner core
				children = getSharedConductorGroupChildren(smc);
			}
			else {
				//top level multicore
				children = getMulticoreTreeItems(smc, false);
			}
		}
		else if (obj instanceof InnerCoresTreeItem) {
			children = getSharedConductorGroupChildren(((InnerCoresTreeItem) obj).getMulticore());
		}
		else if (obj instanceof MulticoreRevisionsTreeItem) {
			children = getRevisionChildren(((MulticoreRevisionsTreeItem) obj).getMulticore());
		}
		else if (obj instanceof FunctionMessageRevisionTreeItem) {
			children = getRevisionChildren(((FunctionMessageRevisionTreeItem) obj).getMessage());
		}
		else if (obj instanceof AbstractionDeferrer) {
			AbstractionDeferrer deferrer = (AbstractionDeferrer) obj;
			children = deferrer.getChildren();
			uidToNodeMap.putAll(deferrer.getTree().getUIDObjectToNodeMapping());
		}
		else if (obj instanceof ModularConnectorRevisionsTreeItem) {
			ISharedConnector sharedConnector = ((ModularConnectorRevisionsTreeItem) obj).getConnector();
			children = getRevisionsOfSharedConnector(getRevisionChildren(sharedConnector), sharedConnector);
		}
		else if (isModularConnector(obj)) {
			children = getModularConnectorTreeItems((ISharedConnector) obj, false);
		}
		else if (obj instanceof IRevisionedObject) {
			children = getRevisionChildren((IRevisionedObject) obj);
		}
		else if (obj instanceof ISharedInternalPosition) {
			children = getPositionChildren((ISharedInternalPosition) obj);
		}
		else if (obj instanceof NoAccessNodeTreeItem) {
			final ISharedObject sharedObject = ((NoAccessNodeTreeItem) obj).getSharedObject();
			if (sharedObject instanceof ISharedMulticore) {
				children = getMulticoreTreeItems((ISharedMulticore) sharedObject, true);
			}
			else if (isModularConnector(sharedObject)) {
				children = getModularConnectorTreeItems((ISharedConnector) sharedObject, true);
			}
			else if (sharedObject instanceof IRevisionedObject) {
				children = getRevisionChildren((IRevisionedObject) sharedObject);
			}
		}

		// todo jmyvon: can this be optimised (sort called for each entry)?
		if (shouldSortChildren) {
			Collections.sort(abstractions, m_comparator);
			Collections.sort(children, m_comparator);
		}
		abstractions.addAll(children);

		return abstractions;
	}

	private List<IUID> getSharedMessageActiveSignals(ISharedFunctionMessage sharedFunctionMessage)
	{
		return sharedFunctionMessage.getActiveSignals().stream()
				.map(IUIDProvider::getUID)
				.collect(Collectors.toList());
	}

	private boolean isModularConnector(@Nullable IUIDObject obj)
	{
		return obj instanceof ISharedConnector && ((ISharedConnector) obj).isModularParent();
	}

	private List<IUID> getSharedHighwayConductors(ISharedGeneralHighway sharedHighway)
	{
		Set<ISharedConductor> sharedConductors = sharedHighway.getSharedConductors();
		List<IUID> childConductors = new ArrayList<IUID>(sharedConductors.size());

		for (ISharedConductor sharedConductor : sharedConductors) {
			if (sharedConductor.isAccesible(userAccountDomains)) {
				childConductors.add(sharedConductor.getUID());
			}
		}
		return childConductors;
	}

	@NotNull private List<IUID> getSharedPinListAbstractions(@NotNull Set<PinListTypeEnum> pinlistTypes,
															 SharedObjectTreeProcessor.Tree inTree)
	{
		Set<IDesignAbstraction> designAbstractionsTree = getDesignAbstractionsForTypes(pinlistTypes);
		List<IUID> abstractions = new ArrayList<IUID>();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			AbstractionDeferrer ad = abstractionDefererMap.get(pinlistTypes, abstraction);
			if (ad == null) {
				// dts0100691115: Fix up collapsing of the expanded tree after creating an shared object instance
				List<ISharedPinList> filteredSharedPinLists = getFilteredSharedPinLists(abstraction, pinlistTypes);
				ad = AbstractionDeferrer.constructAbstractionDeffererForPinLists(abstraction, filteredSharedPinLists);
				abstractionDefererMap.put(pinlistTypes, abstraction, ad);
			}
			ad.getChildren().clear();
			ad.getChildren().addAll(getSharedPinLists(abstraction, pinlistTypes, inTree));
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	@NotNull private Set<IDesignAbstraction> getDesignAbstractionsForTypes(@NotNull Set<PinListTypeEnum> pinlistTypes)
	{
		Set<PinListTypeEnum> typesToConsider = new HashSet<>(pinlistTypes);
		if (pinlistTypes.contains(PinListTypeEnum.TypePlug)) {
			typesToConsider.add(PinListTypeEnum.TypeJack);
		}

		Set<IDesignAbstraction> abstractions = new HashSet<>();
		for (PinListTypeEnum typeEnum : typesToConsider) {
			abstractions.addAll(getSharedObjectsByAbstraction().getAbstractionsForPinListType(typeEnum));
		}
		return abstractions;
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		return !getChildrenWithoutSort(uid).isEmpty();
	}

	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{
		IUIDObject obj = getObject(uid);
		if (obj instanceof ISharedPinList) {
			// (simons) we attempt to gray out any shared pinlist not assigned to the design

			if (getSharedObjetsUsedInDesign().isSharedPinlistUsedInDesign((ISharedPinList) obj)) {
				if (obj instanceof ISharedSplice) {
					ILogicObject pinList = getCableObject((ISharedObject) obj);
					boolean isCenterStripped = IconUtils.isCenterStrippedSplice(pinList);
					if (isCenterStripped) {
						return IconUtils.getSharedCenterStrippedWireIcon(IconUtils.ACTIVE);
					}
				}

				return IconUtils.getIcon(obj);
			}

			return IconUtils.getIcon(obj, IconUtils.INACTIVE);
		}
		else if (obj instanceof AbstractionDeferrer) {
			return IconUtils.getDesignAbstractionIcon(IconUtils.ACTIVE);
		}
		else if (obj instanceof InnerCoresTreeItem) {

			IconUtils state = IconUtils.INACTIVE;

			if (getSharedObjetsUsedInDesign()
					.isSharedMulticoreUsedInDesign(((InnerCoresTreeItem) obj).getMulticore())) {
				state = IconUtils.ACTIVE;
			}

			return IconUtils.getInnerCoresIcon(state);
		}
		else if (obj instanceof MulticoreRevisionsTreeItem) {
			return IconUtils.getMulticoreRevisionsIcon();
		}
		else if (obj instanceof FunctionMessageRevisionTreeItem) {
			return IconUtils.getMulticoreRevisionsIcon();
		}
		else if (obj instanceof ModularConnectorRevisionsTreeItem) {
			return IconUtils.getMulticoreRevisionsIcon();
		}
		else if (obj instanceof NoAccessNodeTreeItem) {
			return IconUtils.getInaccessibleSharedObjectIcon();
		}
		else if (obj instanceof ISharedMulticore sharedMulticore) {

			IconUtils state = IconUtils.INACTIVE;

			if (getSharedObjetsUsedInDesign().isSharedMulticoreUsedInDesign(sharedMulticore)) {
				state = IconUtils.ACTIVE;
			}

			return IconUtils.getIcon(sharedMulticore, state);
		}
		else if (obj instanceof ISharedObject) {
			IconUtils state = IconUtils.INACTIVE;
			if (m_design.getSharedUsageMgr().hasUsage((ISharedObject) obj)) {
				state = IconUtils.ACTIVE;
			}
			if (state == IconUtils.ACTIVE && obj instanceof ISharedConductor) {
				ILogicObject conductor = getCableObject((ISharedObject) obj);
				boolean isCenterStripped = IconUtils.isCenterStrippedWire(conductor);
				if (isCenterStripped) {
					return IconUtils.getSharedCenterStrippedWireIcon(state);
				}
			}
			return IconUtils.getIcon(obj, state);
		}
		else {
			return super.getIcon(uid);
		}
	}

	@Nullable private ILogicObject getCableObject(@NotNull ISharedObject sharedObject)
	{
		IConnectivity connectivity = m_design.getConnectivity();
		if (connectivity != null) {
			return connectivity.findLogicObjectForShared(sharedObject);
		}
		return null;
	}

	@NotNull private List<IUID> getSharedPinLists(IDesignAbstraction abstraction, Set<PinListTypeEnum> pinlistTypes,
												  SharedObjectTreeProcessor.Tree inTree)
	{

		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = inTree;
		}
		else {
			List<ISharedPinList> filteredSharedPinLists = getFilteredSharedPinLists(abstraction, pinlistTypes);
			tree = SharedObjectTreeProcessor.buildTreeForDesignAbstraction(filteredSharedPinLists);
			AbstractionDeferrer abstractionDeferrer = abstractionDefererMap.get(pinlistTypes, abstraction);
			abstractionDeferrer.setTree(tree);
		}

		List<IUID> sharedDevices = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			if (child instanceof SharedObjectTreeProcessor.NodeWithRevisionedObjects) {
				for (IRevisionedObject object : ((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child)
						.getObjects()) {
					ISharedPinList pinList = (ISharedPinList) object;
					collectSharedTreeItems(sharedDevices, pinList);
				}
			}
		}

		return sharedDevices;
	}

	@NotNull private List<ISharedPinList> getFilteredSharedPinLists(@Nullable IDesignAbstraction abstraction,
																	@NotNull Set<PinListTypeEnum> pinlistTypes)
	{
		Set<PinListTypeEnum> typesToFilter = new HashSet<>(pinlistTypes);
		if (pinlistTypes.contains(PinListTypeEnum.TypeInlinePlug) ||
				pinlistTypes.contains(PinListTypeEnum.TypeInlineInterconnectPlug)) {
			typesToFilter.add(PinListTypeEnum.TypeInlineJack);
			typesToFilter.add(PinListTypeEnum.TypeInlineInterconnectJack);
		}
		else if (pinlistTypes.contains(PinListTypeEnum.TypePlug)) {
			typesToFilter.add(PinListTypeEnum.TypeJack);
		}

		return getSharedObjectsByAbstraction().getSharedPinLists(abstraction, typesToFilter);
	}

	private void collectSharedTreeItems(List<IUID> sharedObjects, @NotNull ISharedObject sharedObject)
	{
		if (sharedObject.isAccesible(userAccountDomains)) {
			sharedObjects.add(sharedObject.getUID());
		}
		else {
			final IUID dummyTreeItem = getNoAccessTreeItem(sharedObject);
			sharedObjects.add(dummyTreeItem);
		}
	}

	@NotNull private List<IUID> getSharedConductors(IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedConductorTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredObjects = getFilteredSharedCondMgrObjects(abstraction);
			tree = SharedObjectTreeProcessor.buildTreeForConductorMgrForDesignAbstraction(filteredObjects);
			AbstractionDeferrer abstractionDeferrer = abstractionDefererMap.get(CONDUCTOR_TYPE, abstraction);
			abstractionDeferrer.setTree(tree);
		}

		return getSharedConductorsToDisplay(tree);
	}

	@NotNull private List<IUID> getSharedConductorsToDisplay(SharedObjectTreeProcessor.Tree tree)
	{
		List<IUID> sharedConductors = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			Object object = ((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child).getObjects().get(0);
			if (object instanceof ISharedConductor) {
				ISharedConductor sharedConductor = (ISharedConductor) object;
				boolean isFunctionDesignBrowser = isBrowserForFunctionalDesign();
				if (isFunctionDesignBrowser) {
					if (sharedConductor instanceof ISharedFunctionConductor &&
							!((ISharedFunctionConductor) sharedConductor).isMessageSignal()) {
						sharedConductors.add(sharedConductor.getUID());
					}
				}
				else {
					if (sharedConductor.getMulticore() == null && !sharedConductor.isShield() &&
							!sharedConductor.isSignal() && !sharedConductor.isMessage()) {
						collectSharedTreeItems(sharedConductors, sharedConductor);
					}
				}
			}
		}
		return sharedConductors;
	}

	private boolean isBrowserForFunctionalDesign()
	{
		return m_design instanceof IFunctionLogicDesign;
	}

	@NotNull private List<IUID> getSharedGeneralHighways(IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedHighwayTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredHighways = getFilteredHighways(abstraction);
			tree = SharedObjectTreeProcessor.buildHighwaysTreeForDesignAbstraction(filteredHighways);
		}

		List<IUID> sharedGeneralHighways = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			ISharedGeneralHighway sharedGeneralHighway =
					(ISharedGeneralHighway) ((SharedObjectTreeProcessor.NodeWithSharedObject) child).getSharedObject();
			collectSharedTreeItems(sharedGeneralHighways, sharedGeneralHighway);
		}

		return sharedGeneralHighways;
	}

	@NotNull private List<IUID> getSharedSingleLines(@Nullable IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedSingleLineTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredSingleLines = getFilteredSingleLines(abstraction);
			tree = SharedObjectTreeProcessor.buildSingleLinesTreeForDesignAbstraction(filteredSingleLines);
		}

		List<IUID> sharedSingleLines = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			List<IRevisionedObject> sharedObjects =
					((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child).getObjects();
			sharedObjects.stream()
					.map(sharedObject -> CommonUtils.cast(sharedObject, ISharedSingleLine.class))
					.filter(Objects::nonNull)
					.forEach(singleLine -> collectSharedTreeItems(sharedSingleLines, singleLine));
		}

		return sharedSingleLines;
	}

	@NotNull private List<IUID> getSharedConductorAbstractions()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree = getSharedConductorsAbstraction();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			AbstractionDeferrer ad = abstractionDefererMap.get(CONDUCTOR_TYPE, abstraction);
			if (ad == null) {
				// dts0100691115: Fix up collapsing of the expanded tree after creating an shared object instance
				List<ISharedAbstractable> sharedCondMgrObjects = getFilteredSharedCondMgrObjects(abstraction);
				ad = AbstractionDeferrer.constructAbstractionDeffererForCondMgr(abstraction, sharedCondMgrObjects);
				abstractionDefererMap.put(CONDUCTOR_TYPE, abstraction, ad);
			}
			ad.getChildren().clear();
			ad.getChildren().addAll(getSharedConductors(abstraction));
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	@NotNull private List<ISharedAbstractable> getFilteredSharedCondMgrObjects(@Nullable IDesignAbstraction abstraction)
	{
		List<ISharedAbstractable> filteredObjects = new ArrayList<>();
		filteredObjects.addAll(getSharedObjectsByAbstraction().getSharedCondMgrObjectsByAbstraction(abstraction));
		return filteredObjects;
	}

	@NotNull private List<ISharedAbstractable> getFilteredHighways(@Nullable IDesignAbstraction abstraction)
	{
		List<ISharedAbstractable> filteredObjects = new ArrayList<>();
		filteredObjects.addAll(getSharedObjectsByAbstraction().getSharedGeneralHighwaysByAbstraction(abstraction));
		return filteredObjects;
	}

	@NotNull private List<ISharedAbstractable> getFilteredSingleLines(@Nullable IDesignAbstraction abstraction)
	{
		List<ISharedAbstractable> filteredObjects = new ArrayList<>();
		filteredObjects.addAll(getSharedObjectsByAbstraction().getSharedSingleLinesByAbstraction(abstraction));
		return filteredObjects;
	}

	@NotNull private Set<IDesignAbstraction> getSharedConductorsAbstraction()
	{
		if (isBrowserForFunctionalDesign()) {
			return getSharedObjectsByAbstraction().getAbstractionsForFunctionConductors();
		}
		return getSharedObjectsByAbstraction().getAbstractionsForLogicalConductors();
	}

	@NotNull private List<IUID> getSharedGeneralHighwayAbstractions()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree = getSharedObjectsByAbstraction().getAbstractionsForGeneralHighways();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			List<ISharedAbstractable> filteredHighways = getFilteredHighways(abstraction);
			SharedObjectTreeProcessor.Tree tree =
					SharedObjectTreeProcessor.buildHighwaysTreeForDesignAbstraction(filteredHighways);
			AbstractionDeferrer ad = new AbstractionDeferrer(abstraction, tree);
			ad.getChildren().addAll(getSharedGeneralHighways(abstraction));
			m_uidMgr.addObject(ad);
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	@NotNull private List<IUID> getSharedSingleLineAbstrations()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree = getSharedObjectsByAbstraction().getAbstractionsForSingleLines();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			List<ISharedAbstractable> filteredSingleLines = getFilteredSingleLines(abstraction);
			SharedObjectTreeProcessor.Tree tree =
					SharedObjectTreeProcessor.buildSingleLinesTreeForDesignAbstraction(filteredSingleLines);
			AbstractionDeferrer ad = new AbstractionDeferrer(abstraction, tree);
			ad.getChildren().addAll(getSharedSingleLines(abstraction));
			m_uidMgr.addObject(ad);
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	/**
	 * Get shared conductor groups for the project
	 *
	 * @param abstraction Abstraction to get Shared Multicores for
	 *
	 * @return a list of the conductor groups requested
	 */
	@NotNull private List<IUID> getSharedMulticores(IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedConductorTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredObjects = getFilteredSharedCondMgrObjects(abstraction);
			tree = SharedObjectTreeProcessor.buildTreeForConductorMgrForDesignAbstraction(filteredObjects);
			AbstractionDeferrer abstractionDeferrer = abstractionDefererMap.get(MULTICORE_TYPE, abstraction);
			abstractionDeferrer.setTree(tree);
		}

		List<IUID> sharedConductorGroups = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			Object object = ((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child).getObjects().get(0);
			if (object instanceof ISharedMulticore && !(object instanceof ISharedOverbraid)) {
				ISharedMulticore sharedMulticore = (ISharedMulticore) object;
				if (sharedMulticore.getParent() == null || sharedMulticore.getParent() instanceof ISharedOverbraid) {
					collectSharedTreeItems(sharedConductorGroups, sharedMulticore);
				}
			}
		}

		return sharedConductorGroups;
	}

	@NotNull private List<IUID> getSharedMulticoreAbstractions()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree =
				getSharedObjectsByAbstraction().getAbstractionsForTopLevelMulticores();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			AbstractionDeferrer ad = abstractionDefererMap.get(MULTICORE_TYPE, abstraction);
			if (ad == null) {
				// dts0100691115: Fix up collapsing of the expanded tree after creating an shared object instance
				List<ISharedAbstractable> sharedCondMgrObjects = getFilteredSharedCondMgrObjects(abstraction);
				ad = AbstractionDeferrer.constructAbstractionDeffererForCondMgr(abstraction, sharedCondMgrObjects);
				abstractionDefererMap.put(MULTICORE_TYPE, abstraction, ad);
			}
			ad.getChildren().clear();
			ad.getChildren().addAll(getSharedMulticores(abstraction));
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	@NotNull private List<IUID> getSharedOverbraids(IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedConductorTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredObjects = getFilteredSharedCondMgrObjects(abstraction);
			tree = SharedObjectTreeProcessor.buildTreeForConductorMgrForDesignAbstraction(filteredObjects);
			AbstractionDeferrer abstractionDeferrer = abstractionDefererMap.get(OVERBRAID_TYPE, abstraction);
			abstractionDeferrer.setTree(tree);
		}

		List<IUID> sharedOverbraids = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			Object object = ((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child).getObjects().get(0);
			if (object instanceof ISharedOverbraid) {
				ISharedOverbraid sharedOverBraid = (ISharedOverbraid) object;
				collectSharedTreeItems(sharedOverbraids, sharedOverBraid);
			}
		}

		return sharedOverbraids;
	}

	@NotNull private List<IUID> getSharedMessages(IDesignAbstraction abstraction)
	{
		SharedObjectTreeProcessor.Tree tree;
		if (abstraction == null) {
			tree = m_sharedConductorTreeStructure;
		}
		else {
			List<ISharedAbstractable> filteredObjects = getFilteredSharedCondMgrObjects(abstraction);
			tree = SharedObjectTreeProcessor.buildTreeForConductorMgrForDesignAbstraction(filteredObjects);
			AbstractionDeferrer abstractionDeferrer = abstractionDefererMap.get(MESSAGE_TYPE, abstraction);
			abstractionDeferrer.setTree(tree);
		}

		List<IUID> sharedFunctionMessageUIDs = new ArrayList<IUID>();
		for (SharedObjectTreeProcessor.Node child : tree.getChildren()) {
			Object object = ((SharedObjectTreeProcessor.NodeWithRevisionedObjects) child).getObjects().get(0);
			if (object instanceof ISharedFunctionMessage) {
				ISharedFunctionMessage sharedfuncMessage = (ISharedFunctionMessage) object;
				sharedFunctionMessageUIDs.add(sharedfuncMessage.getUID());
			}
		}

		return sharedFunctionMessageUIDs;
	}

	@NotNull private List<IUID> getSharedMessageAbstractions()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree =
				getSharedObjectsByAbstraction().getAbstractionsForTopLevelOverbraids();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			AbstractionDeferrer ad = abstractionDefererMap.get(MESSAGE_TYPE, abstraction);
			if (ad == null) {
				List<ISharedAbstractable> sharedCondMgrObjects = getFilteredSharedCondMgrObjects(abstraction);
				ad = AbstractionDeferrer.constructAbstractionDeffererForCondMgr(abstraction, sharedCondMgrObjects);
				abstractionDefererMap.put(MESSAGE_TYPE, abstraction, ad);
			}
			ad.getChildren().clear();
			ad.getChildren().addAll(getSharedMessages(abstraction));
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	@NotNull private List<IUID> getSharedOverbraidsAbstrations()
	{
		List<IUID> abstractions = new ArrayList<IUID>();
		Collection<IDesignAbstraction> designAbstractionsTree =
				getSharedObjectsByAbstraction().getAbstractionsForTopLevelOverbraids();
		for (IDesignAbstraction abstraction : designAbstractionsTree) {
			AbstractionDeferrer ad = abstractionDefererMap.get(OVERBRAID_TYPE, abstraction);
			if (ad == null) {
				// dts0100691115: Fix up collapsing of the expanded tree after creating an shared object instance
				List<ISharedAbstractable> sharedCondMgrObjects = getFilteredSharedCondMgrObjects(abstraction);
				ad = AbstractionDeferrer.constructAbstractionDeffererForCondMgr(abstraction, sharedCondMgrObjects);
				abstractionDefererMap.put(OVERBRAID_TYPE, abstraction, ad);
			}
			ad.getChildren().clear();
			ad.getChildren().addAll(getSharedOverbraids(abstraction));
			abstractions.add(ad.getUID());
		}
		return abstractions;
	}

	private List<IUID> getModularConnectorTreeItems(ISharedConnector connector, boolean ignorePositionObjects)
	{
		List<IUID> childObjects = new ArrayList<IUID>();
		childObjects.addAll(getRevisionChildren(connector));

		List<IUID> revisions = getRevisionsOfSharedConnector(childObjects, connector);
		if (ignorePositionObjects) {
			childObjects.clear();
		}
		if (!revisions.isEmpty() && connector.isModularParent()) {
			childObjects.removeAll(revisions);

			ModularConnectorRevisionsTreeItem revisionsTreeItem = modularConnectorRevisionMap.get(connector);
			if (revisionsTreeItem == null) {
				revisionsTreeItem = new ModularConnectorRevisionsTreeItem(connector);
				m_uidMgr.addObject(revisionsTreeItem);
				modularConnectorRevisionMap.put(connector, revisionsTreeItem);
			}
			childObjects.add(revisionsTreeItem.getUID());
		}
		return childObjects;
	}

	@NotNull private IUID getNoAccessTreeItem(ISharedObject sharedObject)
	{
		NoAccessNodeTreeItem revisionsTreeItem = noAccessNodeMap.get(sharedObject);
		if (revisionsTreeItem == null) {
			revisionsTreeItem = new NoAccessNodeTreeItem(sharedObject);
			m_uidMgr.addObject(revisionsTreeItem);
			noAccessNodeMap.put(sharedObject, revisionsTreeItem);
		}
		return revisionsTreeItem.getUID();
	}

	private List<IUID> getRevisionsOfSharedConnector(List<IUID> childConnectorUIDs, ISharedConnector sharedConnector)
	{
		List<IUID> revisions = new ArrayList<IUID>();
		for (IUID sharedConnUID : childConnectorUIDs) {
			ISharedConnector childConn = UIDMgr.getObjectOfType(sharedConnUID, ISharedConnector.class);
			if (childConn != null && childConn.getParentId() == sharedConnector.getUID()) {
				revisions.add(sharedConnUID);
			}
		}
		return revisions;
	}

	private List<IUID> getFunctionMessageTreeItems(ISharedFunctionMessage msg)
	{
		List<IUID> nodes = new ArrayList<IUID>();
		nodes.addAll(getSharedMessageActiveSignals(msg));
		if (!getRevisionChildren(msg).isEmpty()) {
			FunctionMessageRevisionTreeItem msgRev = functionMessageRevisionMap.get(msg);
			if (msgRev == null) {
				msgRev = new FunctionMessageRevisionTreeItem(msg);
				m_uidMgr.addObject(msgRev);
				functionMessageRevisionMap.put(msg, msgRev);
			}
			nodes.add(msgRev.getUID());
		}
		return nodes;
	}

	private List<IUID> getMulticoreTreeItems(ISharedMulticore smc, boolean ignoreInnerCores)
	{
		List<IUID> nodes = new ArrayList<IUID>();

		if(!ignoreInnerCores) {
			InnerCoresTreeItem icn = innerCoresMap.get(smc);
			if (icn == null) {
				icn = new InnerCoresTreeItem(smc);
				m_uidMgr.addObject(icn);
				innerCoresMap.put(smc, icn);
			}
			nodes.add(icn.getUID());
		}

		// only place a revision node if there are revisions
		if (!getRevisionChildren(smc).isEmpty()) {
			MulticoreRevisionsTreeItem mrn = multicoreRevisionMap.get(smc);
			if (mrn == null) {
				mrn = new MulticoreRevisionsTreeItem(smc);
				m_uidMgr.addObject(mrn);
				multicoreRevisionMap.put(smc, mrn);
			}
			nodes.add(mrn.getUID());
		}

		return nodes;
	}

	private List<IUID> getPositionChildren(ISharedInternalPosition position)
	{
		List<IUID> children = new ArrayList<IUID>();
		for (IInternalPositionedObject posObj : position.getPositionedObjects()) {
			children.add(posObj.getUID());
		}

		return children;
	}

	@NotNull private List<IUID> getRevisionChildren(IRevisionedObject revObject)
	{
		SharedObjectTreeProcessor.Node treeNode = uidToNodeMap.get(revObject);

		if (treeNode != null) {
			List<IUID> children = new ArrayList<IUID>();
			Collection<SharedObjectTreeProcessor.Node> children1 = treeNode.getChildren();
			for (SharedObjectTreeProcessor.Node child : children1) {

				if (child instanceof SharedObjectTreeProcessor.NodeWithRevisionedObjects) {

					SharedObjectTreeProcessor.NodeWithRevisionedObjects childNode =
							(SharedObjectTreeProcessor.NodeWithRevisionedObjects) child;
					// is it an inline
					if (revObject instanceof ISharedConnector &&
							((ISharedConnector) revObject).getMates().size() == 1) {
						if (((ISharedConnector) revObject).isAccesible(userAccountDomains)) {
							//is it a plug
							if (((ISharedPinList) revObject).getType() == PinListTypeEnum.TypeInlinePlug) {
								IRevisionedObject object = childNode.getObjects().get(0);
								collectRevisionedTreeItem(children, object);
							}
							else if (((ISharedPinList) revObject).getType() == PinListTypeEnum.TypeInlineJack) {
								// it is a recepticle
								IRevisionedObject object = childNode.getObjects().get(1);
								collectRevisionedTreeItem(children, object);
							}
						}
					}
					else {
						IRevisionedObject object = childNode.getObjects().get(0);
						collectRevisionedTreeItem(children, object);
					}
				}
				else if (child instanceof SharedObjectTreeProcessor.NodeWithSharedPositionObject) {
					ISharedInternalPosition object =
							((SharedObjectTreeProcessor.NodeWithSharedPositionObject) child).getObject();
					children.add(object.getUID());
				}
			}
			return children;
		}
		else {
			return Collections.emptyList();
		}
	}

	private void collectRevisionedTreeItem(List<IUID> children, IRevisionedObject object)
	{
		if (object instanceof ISharedObject) {
			collectSharedTreeItems(children, (ISharedObject) object);
		}
		else {
			children.add(object.getUID());
		}
	}

	@Nullable public String getToolTipText(IUID uid, IUID parentUID)
	{
		if (getBrowserTreeWorker() != null) {
			return HTML_HEADER + "Loading..." + HTML_FOOTER;
		}
		IUIDObject uidObj = getObject(uid);
		StringBuilder toolTipContent = new StringBuilder();
		//

		if (uidObj instanceof IRevisionedObject) {
			String revision = ((IRevisionedObject) uidObj).getRevision();
			if (!StringUtils.isBlank(revision)) {
				toolTipContent.append("<b>");
				toolTipContent.append(REVISION_TIP);
				toolTipContent.append(":</b> ");
				toolTipContent.append(revision);
			}
		}

		if (uidObj instanceof IOptionedObject) {
			IOptionExpression optionExpression = ((IOptionedObject) uidObj).getOptionExpression();
			if (optionExpression != null && !StringUtils.isBlank(optionExpression.getExpression())) {
				StringBuilder builder = new StringBuilder();
				toolTipContent.append("<br>");
				builder.append("<b>");
				builder.append(OPTION_TIP);
				builder.append(":</b> ");
				builder.append(optionExpression.getExpression());
				toolTipContent.append(StringUtils.wrapText(builder.toString()));
			}
		}

		if (uidObj instanceof ISharedAbstractable) {
			IDesignAbstraction abs = ((ISharedAbstractable) uidObj).getDesignAbstraction();
			if (abs != null) {
				toolTipContent.append("<br>");
				toolTipContent.append("<b>");
				toolTipContent.append(DESIGNABSTRACTION_TIP);
				toolTipContent.append(":</b> ");
				toolTipContent.append(abs.getName());
			}
		}

		if (uidObj instanceof NoAccessNodeTreeItem) {
			toolTipContent.append(NO_PERMISSION_NODE_TOOLTIP);
		}

		if (!StringUtils.isBlank(toolTipContent.toString())) {
			return HTML_HEADER + toolTipContent.toString() + HTML_FOOTER;
		}
		else {
			return null;
		}
	}

	public void activateObject(IUID uid)
	{
		IAction action = null;
		IUIDObject object = getObject(uid);
		if (object instanceof ISharedConductor) {
			ISharedConductor sc = (ISharedConductor) object;
			String oldType = sc.getType();
			if (sc.needsRefresh()) {
				sc.refresh();
			}
			String newType = sc.getType();
			if (!oldType.equals(newType)) {
				//Display message
				String header = ResourceMgr.getString(SharedObjectBrowserClient.class,
						"SharedObjectBrowserClient.SharedConductor.Header.text");
				String msg = ResourceMgr.getString(SharedObjectBrowserClient.class,
						"SharedObjectBrowserClient.SharedConductor.Msg.text");
				MessageHelper.showInformationMessage(null, header, msg);
				return;
			}

			if (sc.isWire()) {
				action = getController().getAction(AddSharedWireAction.class);
			}
			else if (sc.isNet()) {
				action = getController().getAction(AddSharedNetAction.class);
			}
			else if (sc.isShield()) {
				action = getController().getAction(AddSharedShieldAction.class);
			}
			else if (sc.isSignal()) {
				action = getController().getAction(AddSharedSignalAction.class);
			}
			else if (sc.isMessage()) {
				action = getController().getAction(AddSharedMessageAction.class);
			}
		}
		else if (object instanceof ISharedHighway) {
			action = SingleLineHelper.isSharedSingleLine(object) ?
					getController().getAction(AddSharedSingleLineAction.class) : //handle shared Single Line instance creation
					getController().getAction(AddSharedGeneralHighwayAction.class);
		}
		else if (object instanceof ISharedOverbraid ||
				(m_overbraidsFolder != null && m_overbraidsFolder.getUID().isEquiv(uid))) {
			action = getController().getAction(EditSharedOverbraidAction.class);
		}
		else if (object instanceof ISharedPinList) {
			Class<? extends IAction> actionClass =
					SharedObjectActionUtil.determinePinListActionClass((ISharedPinList) object);
			if (actionClass != null) {
				action = getController().getAction(actionClass);
			}
		}
		else if (object instanceof IConductor) {
			action = getController().getAction(AddPortAction.class);
		}

		if (action != null && action.isEnabled()) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleclick", 0);
			IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actionMgr != null) {
				actionMgr.actionPerformed(action, ae);
			}
		}
	}

	@Override @Nullable public String doGetPresentationName(IUID uid)
	{
		IUIDObject object = getObject(uid);
		String name = object != null ? getPresentationNameForShared(object) : null;
		return name != null ? name : super.doGetPresentationName(uid);
	}

	public JPanel buildToolbar()
	{
		JPanel toolbarPanel = new JPanel();
		toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		IResource resource = getController().getCaplet().getResource();
		if (resource instanceof ISharedObjectToolbarProvider) {
			ActionContainer ac = ((ISharedObjectToolbarProvider) resource).getSharedToolbar();
			toolbar = ResourceHolder.createToolBar((String) ac.getValue(Action.NAME), ac.getMembers(), null, m_controller);
			toolbar.setBorder(null);
			toolbarPanel.add(toolbar);
			toolbarPanel.repaint();
		}
		return toolbarPanel;
	}

	public void selectionChanged()
	{
		if (toolbar != null) {
			toolbar.updateUI();
			toolbar.validate();
		}
	}

	@Override
	@Nullable public IUIDObject getObject(@Nullable IUID uid)
	{
		IUIDObject uidObject = super.getObject(uid);
		if (isDeletedSharedObject(uidObject)) {
			return null;
		}
		return uidObject;
	}

	private boolean isDeletedSharedObject(@Nullable IUIDObject uidObject)
	{
		return uidObject instanceof ISharedObject && ((ISharedObject) uidObject).getSharedObjectMgr() == null;
	}

	@NotNull protected BuildSharedObjectsUsedInADesign getSharedObjetsUsedInDesign()
	{
		if (m_buildSharedObjectsUsedInADesign == null) {
			m_buildSharedObjectsUsedInADesign = new BuildSharedObjectsUsedInADesign(m_design);
		}
		return m_buildSharedObjectsUsedInADesign;
	}

	@NotNull private SharedObjectsByAbstraction getSharedObjectsByAbstraction()
	{
		if (sharedObjectsByAbstraction == null) {
			sharedObjectsByAbstraction = new SharedObjectsByAbstraction(getSharedTreeBackgroundCache());
		}
		return sharedObjectsByAbstraction;
	}

	/**
	 Store data in SharedPinListMgr and SharedConductorMgr to prevent ConcurrentModificationException
	 when the background thread and EDT access or update the shared tree at the same time.
	 */
	@Override public void prepareForBackGroundThread()
	{
		sharedTreeBackgroundCache = null;
		sharedObjectsByAbstraction = null;
	}

	@NotNull private SharedTreeBackgroundCache getSharedTreeBackgroundCache()
	{
		if (sharedTreeBackgroundCache == null) {
			sharedTreeBackgroundCache = new SharedTreeBackgroundCache(Objects.requireNonNull(m_design.getProject()));
		}
		return sharedTreeBackgroundCache;
	}

	@Override public boolean isBrowserTreeWorkerSupported()
	{
		return true;
	}

	@Nullable @Override public BrowserTreeWorker getBrowserTreeWorker()
	{
		return browserTreeWorker;
	}

	@Override public void setBrowserTreeWorker(@Nullable BrowserTreeWorker browserTreeWorker)
	{
		this.browserTreeWorker = browserTreeWorker;
	}
	@Override public void onTreeConstructionStarted()
	{
		ISharedObjectBrowserAction.setTreeConstructionComplete(false);
		updateToolbarUI();
	}
	@Override public void onTreeConstructionFinished()
	{
		ISharedObjectBrowserAction.setTreeConstructionComplete(true);
		updateToolbarUI();
	}
	private void updateToolbarUI() {
		if (toolbar != null) {
			//update the state of the buttons in the toolbar
			for (Component component : toolbar.getComponents()) {
				if (component instanceof AbstractButton) {
					Action action = ((AbstractButton) component).getAction();
					if (action != null) {
						((IUpdateableAction) action).updateUI();
					}
				}
			}
		}
	}

	private abstract static class AbstractTreeItem extends UIDObject implements INamedObject
	{

		@SuppressWarnings({"NoopMethodInAbstractClass"}) public void setName(String name)
		{
		}

		@Nullable public INamedObject getSuperordinate()
		{
			return null;
		}

		@Nullable public IUID getSuperordinateUID()
		{
			return null;
		}
	}

	private static class AbstractionDeferrer extends AbstractTreeItem
	{

		private final IDesignAbstraction m_abs;

		@ObjectRelationship(type = ObjectRelationship.Type.CHILD)
		private final List<IUID> children;

		@ObjectRelationship(type = ObjectRelationship.Type.NONE)
		private SharedObjectTreeProcessor.Tree m_tree;

		AbstractionDeferrer(IDesignAbstraction dabs, SharedObjectTreeProcessor.Tree tree)
		{
			m_tree = tree;
			m_abs = dabs;
			children = new ArrayList<IUID>();
		}

		@NotNull
		public static AbstractionDeferrer constructAbstractionDeffererForPinLists(IDesignAbstraction abstraction,
																				  @NotNull List<ISharedPinList> sharedPinLists)
		{
			SharedObjectTreeProcessor.Tree tree =
					SharedObjectTreeProcessor.buildTreeForDesignAbstraction(sharedPinLists);
			AbstractionDeferrer ad = new AbstractionDeferrer(abstraction, tree);
			UIDMgr.addObject(ad);
			return ad;
		}

		@NotNull
		public static AbstractionDeferrer constructAbstractionDeffererForCondMgr(IDesignAbstraction abstraction,
																				 @NotNull List<ISharedAbstractable> sharedCondMgrObjects)
		{
			SharedObjectTreeProcessor.Tree tree = SharedObjectTreeProcessor
					.buildTreeForConductorMgrForDesignAbstraction(sharedCondMgrObjects);
			AbstractionDeferrer ad = new AbstractionDeferrer(abstraction, tree);
			UIDMgr.addObject(ad);
			return ad;
		}

		public String getName()
		{
			return m_abs.getName();
		}

		public List<IUID> getChildren()
		{
			return children;
		}

		public SharedObjectTreeProcessor.Tree getTree()
		{
			return m_tree;
		}

		public void setTree(SharedObjectTreeProcessor.Tree tree)
		{
			m_tree = tree;
		}
	}

	private static class MulticoreRevisionsTreeItem extends AbstractTreeItem
	{

		private final ISharedMulticore m_multicore;

		MulticoreRevisionsTreeItem(ISharedMulticore multicore)
		{
			m_multicore = multicore;
		}

		public String getName()
		{
			return REVISIONS_TEXT;
		}

		public ISharedMulticore getMulticore()
		{
			return m_multicore;
		}
	}

	private static class FunctionMessageRevisionTreeItem extends AbstractTreeItem
	{

		private final ISharedFunctionMessage m_functionMessage;

		FunctionMessageRevisionTreeItem(ISharedFunctionMessage functionMessage)
		{
			m_functionMessage = functionMessage;
		}

		public String getName()
		{
			return REVISIONS_TEXT;
		}

		public ISharedFunctionMessage getMessage()
		{
			return m_functionMessage;
		}
	}

	// This is used for displaying "Revisions" folder under modular shared connector
	private static class ModularConnectorRevisionsTreeItem extends AbstractTreeItem
	{

		private final ISharedConnector m_connector;

		ModularConnectorRevisionsTreeItem(ISharedConnector connector)
		{
			m_connector = connector;
		}

		public String getName()
		{
			return REVISIONS_TEXT;
		}

		public ISharedConnector getConnector()
		{
			return m_connector;
		}
	}

	private static class InnerCoresTreeItem extends AbstractTreeItem
	{

		private final ISharedMulticore m_multicore;

		InnerCoresTreeItem(ISharedMulticore multicore)
		{
			m_multicore = multicore;
		}

		public String getName()
		{
			return INNER_CORES_TEXT;
		}

		public ISharedMulticore getMulticore()
		{
			return m_multicore;
		}
	}

	private static class NoAccessNodeTreeItem extends AbstractTreeItem
	{

		private final ISharedObject m_sharedObject;

		NoAccessNodeTreeItem(ISharedObject sharedObject)
		{
			m_sharedObject = sharedObject;
		}

		@NotNull public String getName()
		{
			return NO_PERMISSION_NODE;
		}

		@NotNull public ISharedObject getSharedObject()
		{
			return m_sharedObject;
		}
	}

	private static class SharedBrowserTreeFilter extends BaseTreeSearchFilter
	{

		public boolean filterIn(Object obj)
		{
			if (obj instanceof IBrowserTreeNode) {
				return true;
			}
			assert false : "Non - IBrowserTreeNode found in the browser tree";
			return false;
		}

		@Override public boolean matches(@NotNull Object obj, @NotNull IMatcher matcher)
		{
			if (obj instanceof IBrowserTreeNode) {
				IBrowserTreeNode node = (IBrowserTreeNode) obj;
				IUIDObject uidObject = node.getUIDObject();
				IReadOnlyNamedObject namedObject = CommonUtils.cast(uidObject, IReadOnlyNamedObject.class);
				if (namedObject instanceof ISharedConnector) {
					ISharedConnector sharedConnector = (ISharedConnector) namedObject;
					if (sharedConnector.getOccupiedPosition() != null) {
						String positionName = sharedConnector.getOccupiedPosition().getName();
						String filterStr = positionName + ":" + sharedConnector.getName();
						if (matcher.isMatch(filterStr)) {
							return true;
						}
					}
				}
				String filterString = namedObject != null ? namedObject.getName() : "";
				return filterString != null && matcher.isMatch(filterString);
			}
			return false;
		}
	}

	private class SharedObjectBrowserClientComparator implements Comparator<Object>
	{

		private final SharedObjectComparator sharedComp = SharedObjectComparator.getCachedSharedObjectComparator();
		private final Comparator<Object> nameComp = AlphaNumComparator.getUniqueObjectAsUniqueComparator();

		public int compare(Object o1, Object o2)
		{
			Object localO2 = o2;
			Object localO1 = o1;
			if (localO1 instanceof IUID) {
				localO1 = getObject((IUID) localO1);
			}
			if (localO2 instanceof IUID) {
				localO2 = getObject((IUID) localO2);
			}
			// sorting of positioned object should be based on occupied position
			if (localO1 instanceof IInternalPositionedObject) {
				IInternalPositionBase position = ((IInternalPositionedObject) localO1).getOccupiedPosition();
				localO1 = position != null ? position : localO1;
			}
			if (localO2 instanceof IInternalPositionedObject) {
				IInternalPositionBase position = ((IInternalPositionedObject) localO2).getOccupiedPosition();
				localO2 = position != null ? position : localO2;
			}

			if (localO1 instanceof NoAccessNodeTreeItem) {
				localO1 = ((NoAccessNodeTreeItem) localO1).getSharedObject();
			}

			if (localO2 instanceof NoAccessNodeTreeItem) {
				localO2 = ((NoAccessNodeTreeItem) localO2).getSharedObject();
			}

			if (localO1 instanceof AbstractionDeferrer && !(localO2 instanceof AbstractionDeferrer)) {
				return -1;
			}
			else if (localO1 instanceof AbstractionDeferrer) {
				AbstractionDeferrer a1 = (AbstractionDeferrer) localO1;
				AbstractionDeferrer a2 = (AbstractionDeferrer) localO2;
				return a1.getName().compareTo(a2.getName());
			}
			else if (localO1 instanceof ISharedObject && localO2 instanceof ISharedObject) {
				return sharedComp.compare(localO1, localO2);
			}
			else if (localO1 instanceof ISharedInternalPosition && localO2 instanceof ISharedInternalPosition) {
				return ((ISharedInternalPosition) localO1).compare((ISharedInternalPosition) localO2);
			}
			else if (localO1 instanceof FunctionMessageRevisionTreeItem) {
				return 1;
			}
			else if (localO2 instanceof FunctionMessageRevisionTreeItem) {
				return -1;
			}
			else if (localO1 instanceof IReadOnlyNamedObject && localO2 instanceof IReadOnlyNamedObject) {
				return nameComp.compare(((IReadOnlyNamedObject) localO1).getName(),
						((IReadOnlyNamedObject) localO2).getName());
			}
			else if (localO2 instanceof ModularConnectorRevisionsTreeItem) {
				return -1;
			}
			else if (localO1 instanceof ModularConnectorRevisionsTreeItem) {
				return 1;
			}
			else {
				return nameComp.compare(localO1, localO2);
			}
		}

		public void clearCache()
		{
			sharedComp.clearCache();
		}
	}
}