/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */

package chs.caplets.logic;

import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.IAppActionMgr;
import chs.caf.caplet.EmptySpecialSelectMgr;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.caf.caplet.helpers.browser.BrowserTreeHelperCellRenderer;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.helpers.browser.LockedTreeNodeDimmer;
import chs.caf.helpers.ui.common.ProjectTreeTooltipUtils;
import chs.caplets.logic.actions.AddLibraryInnercoreNetAction;
import chs.caplets.logic.actions.AddLibraryInnercoreWireAction;
import chs.caplets.logic.actions.AddPinListAction;
import chs.caplets.logic.actions.AddShieldConductorAction;
import chs.caplets.logic.actions.BatchDevicePlacementAction;
import chs.caplets.logic.actions.CreateChamferedNetInstanceAction;
import chs.caplets.logic.actions.CreateChamferedWireInstanceAction;
import chs.caplets.logic.actions.CreateLayoutComponentInstanceAction;
import chs.caplets.logic.actions.shared.AddConductorAction;
import chs.caplets.logic.actions.shared.AddGeneralHighwayAction;
import chs.caplets.logic.actions.shared.AddSingleLineAction;
import chs.caplets.logic.actions.shared.ICreateConductorInstanceAction;
import chs.caplets.shared.LogicDesignBrowserClient;
import chs.caplets.topo.browser.IAssocObjectsPlacementStatus;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayIterator;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.IInternalPositionsContainer;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPanelLayoutDuctComponent;
import chs.cof.logical.cable.IPanelLayoutOtherComponent;
import chs.cof.logical.cable.IPanelLayoutRailComponent;
import chs.cof.logical.cable.IPinConnection;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IRingTerminal;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.ISingleLineIterator;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.LogicOtherComponentTypeEnum;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IOptionExpression;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.common.DesignUtils;
import chs.common.IDesignContainer;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.IUIDObjectIterator;
import chs.common.UIDObjectIterator;
import chs.common.UIDUtils;
import chs.common.styles.IStyleableDiagram;
import chs.common.styles.IStyleableObject;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialog;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CapabilityHelper;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.SupportedFeatureInfo;
import chs.utility.DesignObjectHierarchyHelper;
import chs.utility.DiagramHelper;
import chs.utility.LogicDesignObjectHierarchyHelper;
import chs.utility.helpers.DSCWithBackshellPlaceholderHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.logic.ModularConnectorDisplayHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// TODO jacobt FEAT13040 : Check design browser performance

public class BrowserClient extends LogicDesignBrowserClient
{

	private Map<IUIDObject, LogicFolder> m_folderObjectMap;
	private WeakReference<ICapletModel> model;
	protected Comparator<IUID> cmp = new BrowserTreeNodeComparator();
	private IUID parentNodeUID;
	private boolean autoExpandNewObject = false;
	private List<IUID> m_firstLevelChildrens;

	private static final String REVISION_TIP = ResourceMgr.getString(BrowserClient.class, "BrowserClient.Revision.Tip");
	private static final String DIAGRAM_TIP = ResourceMgr.getString(BrowserClient.class, "BrowserClient.Diagram.Tip");
	private static final String OPTION_TIP = ResourceMgr.getString(BrowserClient.class, "BrowserClient.Option.Tip");
	private static final String NO_ACCESS = ResourceMgr.getString(SwapOutSharedObjectRevisionDialog.class,
			"SwapOutSharedObjectRevisionDialog.domainNoAccess.noPermissionText");

	protected enum LogicFolder
	{
		CONDUCTORS(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Conductors.text"), IConductor.class,
				SupportedFeatureInfo.Feature.LOGIC),
		SPLICE(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Splices.text"), ISplice.class,
				SupportedFeatureInfo.Feature.LOGIC),
		INLINE(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Inlines.text"), IGenericInlineConnector.class,
				SupportedFeatureInfo.Feature.LOGIC),
		CONNECTOR(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Connectors.text"), IConnector.class,
				SupportedFeatureInfo.Feature.LOGIC),
		RING_TERMINAL(ResourceMgr.getString(BrowserClient.class, "BrowserClient.RingTerminals.text"),
				IRingTerminal.class, SupportedFeatureInfo.Feature.LOGIC),
		OVERBRAID(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Overbraids.text"), IOverbraid.class,
				SupportedFeatureInfo.Feature.OVERBRAIDS),
		ASSEMBLY(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Assemblies.text"), IAssembly.class,
				SupportedFeatureInfo.Feature.ASSEMBLIES),
		MOUNTINGRAIL(ResourceMgr.getString(BrowserClient.class, "BrowserClient.MountingRails.text"),
				IPanelLayoutRailComponent.class, SupportedFeatureInfo.Feature.LOGIC),
		WIREDUCT(ResourceMgr.getString(BrowserClient.class, "BrowserClient.WireDucts.text"),
				IPanelLayoutDuctComponent.class, SupportedFeatureInfo.Feature.LOGIC),
		OTHERCOMPONENT(ResourceMgr.getString(BrowserClient.class, "BrowserClient.OtherComponents.text"),
				IPanelLayoutOtherComponent.class, SupportedFeatureInfo.Feature.LOGIC),
		DEVICE(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Devices.text"), IDevice.class,
				SupportedFeatureInfo.Feature.LOGIC),
		MULTICORE(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Multicores.text"), IMulticore.class,
				SupportedFeatureInfo.Feature.LOGIC),
		HIGHWAYS(ResourceMgr.getString(BrowserClient.class, "BrowserClient.Highways.text"), IGeneralHighway.class,
				SupportedFeatureInfo.Feature.LOGIC),
		SINGLE_LINES(ResourceMgr.getString(BrowserClient.class, "BrowserClient.SingleLines.text"), ISingleLine.class,
				SupportedFeatureInfo.Feature.LOGIC),
		LOGIC_BLOCKS(ResourceMgr.getString(BrowserClient.class, "BrowserClient.BlockDevices.text"), IBlockDevice.class,
				SupportedFeatureInfo.Feature.LOGIC_BLOCKS),
		FUNCTION_COMPONENT(ResourceMgr.getString(BrowserClient.class, "BrowserClient.FunctionComponents.text"),
				IFunction.class, SupportedFeatureInfo.Feature.ARTISAN_FUNCTION),
		FUNCTION_CONDUCTOR(ResourceMgr.getString(BrowserClient.class, "BrowserClient.FunctionConductors.text"),
				IFunctionConductor.class, SupportedFeatureInfo.Feature.ARTISAN_FUNCTION),
		//		FUNCTION_BLOCK(ResourceMgr.getString(BrowserClient.class, "BrowserClient.FunctionBlocks.text"),
//				IFunctionBlock.class, SupportedFeatureInfo.Feature.ARTISAN_FUNCTION),
//		FUNCTION_HIGHWAYS(ResourceMgr.getString(BrowserClient.class, "BrowserClient.FunctionHighways.text"),
//				IFunctionHighway.class, SupportedFeatureInfo.Feature.ARTISAN_FUNCTION);
		FUNCTION_MESSAGE(ResourceMgr.getString(BrowserClient.class, "BrowserClient.FunctionMessages.text"),
				IFunctionMessage.class, SupportedFeatureInfo.Feature.ARTISAN_FUNCTION);

		private String displayName;
		private Class<? extends IUIDObject> type;
		private SupportedFeatureInfo.Feature featureInfo;

		LogicFolder(String displayName, Class<? extends IUIDObject> type, SupportedFeatureInfo.Feature info)
		{
			this.displayName = displayName;
			this.type = type;
			featureInfo = info;
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public Class<? extends IUIDObject> getType()
		{
			return type;
		}

		public SupportedFeatureInfo.Feature getFeatureInfo()
		{
			return featureInfo;
		}

		public boolean isCompatible(@NotNull IUIDObject object)
		{
			if (type == IRingTerminal.class) {
				return object instanceof IConnector && ((IConnector) object).isRingTerminal();
			}

			if (type == IConnector.class) {
				return object instanceof IConnector && !((IConnector) object).isRingTerminal();
			}
			return type.isAssignableFrom(object.getClass());
		}
	}

	protected Set<String> m_skippedFolders;
	protected LogicDesignObjectHierarchyHelper m_designObjectHierarchyHelper;
	private Map<String, BrowserFolder> m_unplacedFolderMap;
	private LockedTreeNodeDimmer treeNodeDimmer;

	public BrowserClient(ICapletController controller)
	{
		super(controller);
		model = new WeakReference<ICapletModel>(controller.getCapletModel());
		setRootObject(getModel().getDesign());
		m_designObjectHierarchyHelper = new LogicDesignObjectHierarchyHelper();
		m_skippedFolders = new HashSet<String>();
		m_folderObjectMap = new HashMap<IUIDObject, LogicFolder>();
		m_unplacedFolderMap = new HashMap<>();
		m_firstLevelChildrens = new ArrayList<IUID>();
		// setup the static children of this browser tree
		buildChildrenFolders();
	}

	public void setTreeNodeDimmer(LockedTreeNodeDimmer treeNodeDimmer)
	{
		this.treeNodeDimmer = treeNodeDimmer;
	}

	protected void buildChildrenFolders()
	{

		boolean limitedSet = AppInfo.isCapitalArchitect() || AppInfo.isCapitalCapture();

		obtainSkippedFolders();

		createDeviceFolder();

		if (!limitedSet) {

			createConnetorFolder();

			createLogicBlockFolder();

			createInlinesFolder();

			createSpliceFolder();

			createRingTerminalFolder();
		}

		createConductorFolder();

		createMulticoreFolder();

		if (!limitedSet) {
			createOverbraidsFolder();
		}
		if (!AppInfo.isCapitalArchitect()) {
			createHighwaysFolder();

			//we are not supporting single lines in derivative tools
			if (!AppInfo.isCapitalDerivative()) {
				createSingleLinesFolder();
			}
		}
		createAssembliesFolder();
		createMountingRailsFolder();
		createWireDuctsFolder();
		createOtherComponentsFolder();
	}

	@Nullable protected LogicFolder getLogicFolderForObject(IUIDObject obj)
	{
		LogicFolder folder = null;
		if (obj != null) {
			for (LogicFolder fldr : LogicFolder.values()) {
				if (fldr.isCompatible(obj)) {
					folder = fldr;
					break;
				}
			}
		}
		return folder;
	}

	@Nullable protected BrowserFolder createObjectFolder(LogicFolder folder)
	{
		BrowserFolder browserFolder = null;
		if (folder != null && CapabilityHelper.supports(folder.getFeatureInfo())) {
			browserFolder = createFolder(folder.getDisplayName());
			m_folderObjectMap.put(browserFolder, folder);
			m_firstLevelChildrens.add(browserFolder.getUID());
		}
		return browserFolder;
	}

	protected final void doCreateObjectFolder(LogicFolder folder)
	{
		if (!m_skippedFolders.contains(folder.getDisplayName())) {
			createObjectFolder(folder);
		}
	}

	private void createAssembliesFolder()
	{
		doCreateObjectFolder(LogicFolder.ASSEMBLY);
	}

	private void createOtherComponentsFolder()
	{
		doCreateObjectFolder(LogicFolder.OTHERCOMPONENT);
	}

	private void createWireDuctsFolder()
	{
		doCreateObjectFolder(LogicFolder.WIREDUCT);
	}

	private void createMountingRailsFolder()
	{
		doCreateObjectFolder(LogicFolder.MOUNTINGRAIL);
	}

	private void createHighwaysFolder()
	{
		doCreateObjectFolder(LogicFolder.HIGHWAYS);
	}

	private void createSingleLinesFolder()
	{
		doCreateObjectFolder(LogicFolder.SINGLE_LINES);
	}

	private void createOverbraidsFolder()
	{
		doCreateObjectFolder(LogicFolder.OVERBRAID);
	}

	private void createMulticoreFolder()
	{
		doCreateObjectFolder(LogicFolder.MULTICORE);
	}

	private void createRingTerminalFolder()
	{
		doCreateObjectFolder(LogicFolder.RING_TERMINAL);
	}

	private void createSpliceFolder()
	{
		doCreateObjectFolder(LogicFolder.SPLICE);
	}

	private void createInlinesFolder()
	{
		doCreateObjectFolder(LogicFolder.INLINE);
	}

	private void createDeviceFolder()
	{
		doCreateObjectFolder(LogicFolder.DEVICE);
	}

	protected void createConductorFolder()
	{
		doCreateObjectFolder(LogicFolder.CONDUCTORS);
	}

	protected void createConnetorFolder()
	{
		doCreateObjectFolder(LogicFolder.CONNECTOR);
	}

	protected void createLogicBlockFolder()
	{
		doCreateObjectFolder(LogicFolder.LOGIC_BLOCKS);
	}

	/**
	 * is this object selectable in the browser tree?
	 */
	public boolean isSelectable(IUID uid)
	{
		if (uid == getRoot()) {
			return true;        // Can select the root!  Properties on designs.
		}
		IUIDObject obj = getObject(uid);
		//noinspection RedundantIfStatement
		if (obj instanceof BrowserFolder || obj instanceof ILibraryInnerCore || obj instanceof ILibraryObject) {
			return false;
		}

		return true;
	}

	/**
	 * Overridden here to display various Strings for Logic schem pinlist nodes
	 *
	 * @param uid The uid of the object represented by the node
	 * @return A String to display in the browser tree for this client
	 */
	@Override @Nullable public String doGetPresentationName(IUID uid)
	{
		IUIDObject uidObject = getObject(uid);
		if (uidObject instanceof chs.cof.logical.schem.IPinList) {
			return getPresentationName((chs.cof.logical.schem.IPinList) uidObject);
		}
		if (isPositionedObject(uidObject)) {
			return getPresentationNameForPositionedObject((IInternalPositionedObject) uidObject);
		}
		return super.doGetPresentationName(uid);
	}

	@NotNull private String getPresentationNameForPositionedObject(@NotNull IInternalPositionedObject positionedObject)
	{
		return ModularConnectorDisplayHelper.generatePositionDisplayString(positionedObject);
	}

	private String getPresentationName(chs.cof.logical.schem.IPinList pl)
	{
		// use the instantiated symbol/block name if there was one
		String name = null;
		IBlock block = pl.getBlock();
		if (block != null) {
			name = block.getName();
		}
		if (name == null) {
			ISymbolRef symRef = pl.getSymbolRef();
			if (symRef != null) {
				ISymbolDef symDef = UIDMgr.getObjectOfType(symRef.getSymbolUID(), ISymbolDef.class);
				if (symDef != null) {
					name = symDef.getName();
				}
			}
		}
		if (name == null) {
			name = pl.getConnectivity().getName();
		}

		// append up to 2 pins
		StringBuilder sout = new StringBuilder();
		sout.append(name);
		IUIDObjectCollection<IPin> pins = pl.getPins();
		int count = 0;
		for (IPin pin : pins) {
			IAbstractPin apin = pin.getConnectivity();
			// note that we don't bother with this bit for backshells
			if (!(apin instanceof IBackshellTermination)) {
				// TODO jacobt FEAT13040 : i18n here for punctuation
				if (count == 0) {
					sout.append(" (");
				}
				if (count == 1) {
					sout.append(',');
				}
				if (count == 2) {
					sout.append(",...");
					break;
				}
				sout.append(apin.getName());
				++count;
			}
		}
		if (count > 0) {
			sout.append(')');
		}
		return sout.toString();
	}

	public String getToolTipText(IUID uid, @Nullable IUID parentUID)
	{
		IUIDObject obj = getObject(uid);
		if (obj == null) {
			return null;
		}

		StringBuilder buf = new StringBuilder();
		buf.append("<html><body>");

		String dimmedToolTipText = null;
		if (treeNodeDimmer != null) {
			if (treeNodeDimmer.shouldDimTheTreeUID(uid)) {
				dimmedToolTipText = treeNodeDimmer.getToolTipText(uid);
			}
			else if (treeNodeDimmer.shouldDimTheTreeUID(parentUID)) {
				dimmedToolTipText = treeNodeDimmer.getToolTipText(parentUID);
			}
		}
		if (dimmedToolTipText != null) {
			buf.append(dimmedToolTipText);
			buf.append("<br>");
		}

		// start with the diagram name if we have one
		ILogicObject logObj = ReferenceHelper.reduceToLogicObject(obj);
		if (obj instanceof IDiagramObject) {
			// diagram is loaded - get the diagram from the usage anyway
			IDiagramObject diagramObj = (IDiagramObject) obj;
			ISchemDiagram schemDiagram = DiagramHelper.getDiagram(diagramObj);
			if (schemDiagram != null) {
				buf.append(schemDiagram.getName()).append("<br>");
			}

			logObj = ReferenceHelper.reduceToLogicObject(obj);
		}
		else if (obj instanceof ILogicObject) {
			// tooltip to show the diagram(s) in which a connectivity object is used
			// TODO jacobt FEAT13040 : logic object tooltip not in requirement - check with marketing
			// TODO jacobt FEAT13040 : i18n tooltips
			logObj = (ILogicObject) obj;
			IDesignContainer blkAssociatedDesign =
					(obj instanceof IBlockDevice) ? ((IBlockDevice) obj).getAssociatedDesign(null) : null;
			if (blkAssociatedDesign != null) {
				buf.append(ProjectTreeTooltipUtils.getToolTipText(blkAssociatedDesign));
			}
			else {
				int count = 0;
				for (IDesignSharedUsage usage : getDWUM().getUsages(logObj)) {
					if (count == 1) {
						buf.append(",...");
						break;
					}
					buf.append("<b>").append(DIAGRAM_TIP).append(":</b>");
					buf.append(usage.getDiagramName());
					++count;
				}
				if (count > 0) {
					buf.append("<br>");
				}
			}
		}
		if (logObj != null) {
			ISharedObject sharedObj = logObj.getSharedObject();
			if (sharedObj != null) {
				if (sharedObj instanceof IRevisionedSharedObject) {
					IRevisionedSharedObject revObj = (IRevisionedSharedObject) sharedObj;
					buf.append("<b>").append(REVISION_TIP).append(":</b> ").append(revObj.getRevision()).append("<br>");
				}
				IOptionExpression optionExpression = sharedObj.getOptionExpression();
				appendOptionExpression(buf, optionExpression);
			}
			else {
				IOptionExpression optionExpression = logObj.getOptionExpression();
				appendOptionExpression(buf, optionExpression);
			}
		}
		if (logObj instanceof IAssembly) {
			buf.append("<B>");
			buf.append(ResourceMgr.getString(BrowserClient.class, "BrowserClient.AssemblyType")).append(' ');
			buf.append("</B>");
			buf.append(((IAssembly) logObj).getAssemblyType().toString());
		}
		if ("<html><body>".equals(buf.toString())) {
			return null;
		}
		buf.append("</html></body>");

		// TODO jacobt "Cross reference information" tooltip - think it just means zone info - check with marketing
		return buf.toString();
	}

	private void appendOptionExpression(StringBuilder buf, IOptionExpression optionExpression)
	{
		if (optionExpression != null && !StringUtils.isBlank(optionExpression.getExpression())) {
			StringBuilder sb = new StringBuilder();
			sb.append("<b>").append(OPTION_TIP).append(":</b> ").append(optionExpression.getExpression())
					.append("<br>");
			buf.append(StringUtils.wrapText(sb.toString()));
		}
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		// this can be null in some unit tests
		if (getModel().getDesign().getConnectivity() == null) {
			return false;
		}

		// the root node always has children
		if (uid == getRoot()) {
			return true;
		}

		setParentUID(parentUID);

		IUIDObject parentObject = getObject(parentUID);
		if (IAssembly.class.isAssignableFrom(parentObject.getClass())) {
			List<IUID> assemblyChildren = getAssembledChildren(parentUID, uid);
			return (assemblyChildren != null && !assemblyChildren.isEmpty());
		}

		// first check for one of our folders
		IUIDObject obj = getObject(uid);
		if (obj instanceof BrowserFolder) {
			// now find out which type it is
			return browserFolderHasChildren(obj);
		}
		else {
			// the connectivity nodes
			return connectivityObjectsHasChildrens(obj);
		}
	}

	protected boolean connectivityObjectsHasChildrens(IUIDObject obj)
	{
		if (obj instanceof IPinList) {
			return hasChildren((IPinList) obj, parentNodeUID);
		}
		if (obj instanceof ILogicOtherComponent) {
			return hasChildren((ILogicOtherComponent) obj);
		}
		if (obj instanceof IConductor) {
			IUIDObject parentObj = getObject(parentNodeUID);
			if (parentObj instanceof IHighwaySchematic || parentObj instanceof IHighway) {
				return false;
				//return !getHighwayConductorChildren((IHighwaySchematic) parentObj, (IConductor) obj).isEmpty();
			}
			if (obj instanceof IFunctionMessage) {
				return hasChildren((IFunctionMessage) obj);
			}
			if (obj instanceof IFunctionConductor && ((IFunctionConductor) obj).isAssociatedMessageSignal()) {
				return hasChildrenOfMessageSignal((IFunctionConductor) obj);
			}
			return hasChildren((IConductor) obj);
		}
		if (obj instanceof IMulticore) {
			// TODO jacobt FEAT13040 : check perf here - seems horribly wasetful but prob. doesn't really matter
			// Multicore in logic cannot exist without shield body, which implies there is always atleast one child
			return !isSingleLineParent(parentNodeUID);
		}
		if (obj instanceof ILibraryInnerCore) {
			ILibraryInnerCore inner = (ILibraryInnerCore) obj;
			return !LibraryHelper.isInnerCoreLeaf(inner) && !LibraryHelper.getInnerCores(inner).isEmpty();
		}
		if (obj instanceof IAssembly) {
			return hasAssemblyChildren(obj);
		}
		//this should be processed first before IHighway
		if (obj instanceof ISingleLine) {
			return hasChildren((ISingleLine) obj);
		}
		if (obj instanceof IGeneralHighway) {
			return hasChildren((IGeneralHighway) obj);
		}

		// the schematic nodes (may be implemented via usages later)
		if (obj instanceof chs.cof.logical.schem.IPinList) {
			return !getChildren((chs.cof.logical.schem.IPinList) obj).isEmpty();
		}
		if (obj instanceof chs.cof.logical.schem.IConductor) {
			// For message schematic pull signals, if not ports under it.
			if (((IConnectivityRef) obj).getConnectivity() instanceof IFunctionMessage) {
				return !getSignalsOrPortsUnderMessage((chs.cof.logical.schem.IConductor) obj).isEmpty();
			}

			return !getChildren((chs.cof.logical.schem.IConductor) obj).isEmpty();
		}
		if (obj instanceof IHighwaySchematic) {
			return !getChildren((IHighwaySchematic) obj).isEmpty();
		}
		if (obj instanceof ISchemStackPin) {
			return !getChildren((ISchemStackPin) obj).isEmpty();
		}
		if (obj instanceof IInternalPosition) {
			return !getChildren((IInternalPosition) obj).isEmpty();
		}
		if (obj instanceof ISharedInternalPosition) {
			return !getChildren((ISharedInternalPosition) obj).isEmpty();
		}
		return false;
	}

	private boolean hasChildren(@NotNull IFunctionMessage obj)
	{
		return !obj.getActiveSignals().isEmpty() || obj.getPins().hasNext() ||
				getDWUM().getDesignSharedUsageCount(obj) > 1;
	}

	private boolean hasChildrenOfMessageSignal(@NotNull IFunctionConductor obj)
	{
		IUIDObject parentObj = UIDMgr.getObject(parentNodeUID);
		if (parentObj instanceof chs.cof.logical.schem.IConductor) {
			return !((chs.cof.logical.schem.IConductor) parentObj).getPins().isEmpty();
		}
		else {
			return hasChildren((IConductor) obj.associatedFunctionMessage());
		}
	}

	protected boolean browserFolderHasChildren(IUIDObject obj)
	{
		LogicFolder logicFolder = m_folderObjectMap.get(obj);
		if (logicFolder != null) {
			if (logicFolder == LogicFolder.DEVICE) {
				return hasDevices();
			}
			if (logicFolder == LogicFolder.CONNECTOR) {
				return hasConnectors();
			}
			if (logicFolder == LogicFolder.RING_TERMINAL) {
				return hasRingTerminals();
			}
			if (logicFolder == LogicFolder.INLINE) {
				return hasInlines();
			}
			if (logicFolder == LogicFolder.SPLICE) {
				return hasSplices();
			}
			if (logicFolder == LogicFolder.CONDUCTORS) {
				return hasConductors();
			}
			if (logicFolder == LogicFolder.HIGHWAYS/* || logicFolder == LogicFolder.FUNCTION_HIGHWAYS*/) {
				return hasHighways();
			}
			if (logicFolder == LogicFolder.SINGLE_LINES) {
				return hasSingleLines();
			}
			if (logicFolder == LogicFolder.MULTICORE) {
				return hasMulticores(false);
			}
			if (logicFolder == LogicFolder.OVERBRAID) {
				return hasMulticores(true);
			}
			if (logicFolder == LogicFolder.ASSEMBLY) {
				return hasAssemblies();
			}
			if (logicFolder == LogicFolder.LOGIC_BLOCKS/* || logicFolder == LogicFolder.FUNCTION_BLOCK*/) {
				return hasBlockDevices();
			}
			if (logicFolder == LogicFolder.OTHERCOMPONENT) {
				return hasOtherComponents();
			}
			if (logicFolder == LogicFolder.MOUNTINGRAIL) {
				return hasMountingRails();
			}
			if (logicFolder == LogicFolder.WIREDUCT) {
				return hasWireDucts();
			}
			if (logicFolder == LogicFolder.FUNCTION_COMPONENT) {
				return hasFunctionComponents();
			}
			if (logicFolder == LogicFolder.FUNCTION_CONDUCTOR) {
				return hasFunctionConductors();
			}
			if (logicFolder == LogicFolder.FUNCTION_MESSAGE) {
				return hasFunctionMessages();
			}
		}
		// Unplaced folder?
		IUIDObject parentObj = getObject(parentNodeUID);
		logicFolder = m_folderObjectMap.get(parentObj);
		if (logicFolder != null) {
			if (logicFolder == LogicFolder.DEVICE) {
				return hasUnusedDevices();
			}
			else if (logicFolder == LogicFolder.CONNECTOR) {
				return hasUnusedConnectors();
			}
			else if (logicFolder == LogicFolder.RING_TERMINAL) {
				return hasUnusedRingTerminals();
			}
			else if (logicFolder == LogicFolder.INLINE) {
				return hasUnusedInlines();
			}
			else if (logicFolder == LogicFolder.SPLICE) {
				return hasUnusedSplices();
			}
			else if (logicFolder == LogicFolder.CONDUCTORS) {
				return hasUnusedConductors();
			}
			else if (logicFolder == LogicFolder.HIGHWAYS/* || logicFolder == LogicFolder.FUNCTION_HIGHWAYS*/) {
				return hasUnusedHighways();
			}
			else if (logicFolder == LogicFolder.SINGLE_LINES/* || logicFolder == LogicFolder.FUNCTION_HIGHWAYS*/) {
				return hasUnusedSingleLines();
			}
			else if (logicFolder == LogicFolder.LOGIC_BLOCKS/* || logicFolder == LogicFolder.FUNCTION_BLOCK*/) {
				return hasUnusedBlockDevices();
			}
			else if (logicFolder == LogicFolder.OTHERCOMPONENT) {
				return hasUnusedOtherComponents();
			}
			else if (logicFolder == LogicFolder.WIREDUCT) {
				return hasUnusedWireDucts();
			}
			else if (logicFolder == LogicFolder.MOUNTINGRAIL) {
				return hasUnusedMountingRails();
			}
			else if (logicFolder == LogicFolder.FUNCTION_COMPONENT) {
				return hasUnusedFunctionComponents();
			}
			else if (logicFolder == LogicFolder.FUNCTION_CONDUCTOR) {
				return hasUnusedFunctionConductors();
			}
			else if (logicFolder == LogicFolder.FUNCTION_MESSAGE) {
				return hasUnusedFunctionMessages();
			}
			else if (logicFolder == LogicFolder.MULTICORE) {
				return hasUnusedMulticore();
			}
		}
		if (parentObj instanceof IPinList) {
			return hasUnusedObject(((IPinList) parentObj).getPins());
		}
		if (parentObj instanceof IConductor) {
			return hasUnusedObject(((IPinConnection) parentObj).getPins());
		}
		return false;
	}

	protected boolean hasAssemblyChildren(IUIDObject obj)
	{
		return !((IAssembly) obj).getElements().isEmpty();
	}

	private boolean hasChildren(IGeneralHighway highway)
	{
		return highway.getAllConductors().hasNext() || getDWUM().getDesignSharedUsageCount(highway) > 1;
	}

	private boolean hasChildren(ISingleLine singleLine)
	{
		return (getDWUM().getDesignSharedUsageCount(singleLine) > 1) || singleLine.findNumberOfConnectedEnds() > 0;
	}

	protected boolean hasChildren(IConductor conductor)
	{
		// if a conductor is connected to any pins it will have children of some sort
		if (conductor.getPins().hasNext()) {
			return true;
		}

		// otherwise if it has >1 schematic usage it will have the schems as children
		return getDWUM().getDesignSharedUsageCount(conductor) > 1;
	}

	public void setParentUID(@Nullable IUID uid)
	{
		parentNodeUID = uid;
	}

	/**
	 * Overridden here so that we can make use of the parent node in getChildren(uid).
	 * <p>
	 * Currently done to allow backshells below schematic instances to show the correct terminations (previously done
	 * via BackshellMasquerade)
	 *
	 * @param node The browser node
	 * @return The possibly null collection of child UIDs
	 */
	@Nullable public Collection<IUID> getChildren(IBrowserTreeNode node)
	{
		TreeNode parentNode = node.getParent();
		if (parentNode instanceof IBrowserTreeNode) {
			setParentUID(((IBrowserTreeNode) parentNode).getUID());
		}
		else {
			setParentUID(null);
		}
		return super.getChildren(node);
	}

	@Nullable public Icon getIcon(IUID uid, @Nullable IAssocObjectsPlacementStatus.PLACED_STATE placementStatus)
	{
		return super.getIcon(uid, placementStatus);
	}

	/**
	 * Overridden here to handle double click on various node types
	 *
	 * @param uid UID of the object activated (e.g via dbl click
	 */
	public void activateObject(IUID uid)
	{
		IUIDObject obj = getObject(uid);

		// Zoom Selected for diagram objects
		if (obj instanceof IDiagramObject) {
			// zoom selected on double clicking a diagram object
			// shame about the hardcode but it seems the usual way
			IAppActionMgr actionMgr = CAFUtils.getInstance().getFIB().getAppActionMgr();
			// TODO jacobt FEAT13040 : ZoomSelectedAction.class.getName() also works?
			AppAction action = actionMgr.getAction("chs.services.gfx.actions.ZoomSelectedActionUI");
			if (action != null) {
				ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "zoomselected");
				action.actionPerformed(ae);
			}
			else {
				assert false : "No Zoom selected action?";
			}
			return;
		}

		// Add instance for logic objects (or objects masquerading as them)
		IAction action = null;
		if (obj instanceof ILogicObject) {
			// add an instance of the selected pinlist/pin/conductor

			if (obj instanceof IFunctionConductor && ((IFunctionConductor) obj).isAssociatedMessageSignal()) {
				return;
			}

			if (obj instanceof IConductor) {
				List<IAction> actionsToCheck = new ArrayList<>(2);
				if (ConductorRouteAction.getInstance().isThreePhaseRouting()) {
					actionsToCheck.add(getController().getAction(CreateChamferedNetInstanceAction.class));
					actionsToCheck.add(getController().getAction(CreateChamferedWireInstanceAction.class));
				}
				else {
					actionsToCheck.add(getController().getAction(AddConductorAction.class));
				}
				actionsToCheck.add(getController().getAction(AddShieldConductorAction.class));
				for (IAction candidate : actionsToCheck) {
					ICreateConductorInstanceAction instanceAction =
							CommonUtils.cast(candidate, ICreateConductorInstanceAction.class);
					if (instanceAction != null && instanceAction.isReadyForActivation()) {
						action = candidate;
						break;
					}
				}
			}
			else if (obj instanceof IHighway) {
				action = SingleLineHelper.isSingleLineHighway((IHighway) obj) ?
						getController().getAction(AddSingleLineAction.class) : //handle Cable instance creation
						getController().getAction(AddGeneralHighwayAction.class);
			}
			else if (obj instanceof IPinList) {
				IPinList pl = (IPinList) obj;
				ISharedPinList sharedPinList = pl.getSharedPinList();
				if (sharedPinList != null) {
					Class<? extends IAction> actionClass =
							SharedObjectActionUtil.determinePinListActionClass(sharedPinList);
					if (actionClass != null) {
						try {
							Constructor<? extends IAction> constructor =
									actionClass.getConstructor(ICapletController.class, ISpecialSelectMgr.class);
							action = constructor.newInstance(getController(), new EmptySpecialSelectMgr()
							{
								@Override public IUIDObjectIterator getSelectedObjects()
								{
									return new UIDObjectIterator(Collections.singletonList(sharedPinList));
								}
							});
						}
						catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
							   InvocationTargetException ignore) {

						}
					}
				}
				else {
					final ICapletController controller = getController();
					if (obj instanceof IDevice) {
						action = controller.getAction(BatchDevicePlacementAction.class);
					}
					if (action == null) {
						action = controller.getAction(AddPinListAction.class);
					}
				}
			}
			else if (obj instanceof ILogicOtherComponent) {
				action = getController().getAction(CreateLayoutComponentInstanceAction.class);
			}
		}
		else if (obj instanceof ILibraryInnerCore) {
			action = getLibraryInnerCoreAction();
		}

		if (action != null && !CAFUtils.getInstance().getFIB().getUIMgr().isActionFilteredOut(action)) {
			int modifiers = 0;
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleclick", modifiers);
			IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actionMgr != null) {
				actionMgr.actionPerformed(action, ae);
			}
		}
	}

	@Nullable private IAction getLibraryInnerCoreAction()
	{
		List<IAction> actionsToCheck = new ArrayList<>();
		actionsToCheck.add(getController().getAction(AddLibraryInnercoreWireAction.class));
		actionsToCheck.add(getController().getAction(AddLibraryInnercoreNetAction.class));

		return actionsToCheck.stream()
				.filter(Objects::nonNull)
				.filter(IAction::isEnabled)
				.findFirst().orElse(null);
	}

	public List<IUID> getChildren(IUID uid)
	{
		if (uid == getRoot()) {
			return m_firstLevelChildrens;
		}

		IUIDObject obj = getObject(uid);
		List<IUID> uidChildren;
		List<IUID> otherChildren = new ArrayList<IUID>();
		if (obj instanceof BrowserFolder) {
			uidChildren = getFolderChildren(obj);
		}
		else {
			uidChildren = getConnectivityChildrens(obj, otherChildren);
		}
		// now sort the vector
		if (uidChildren != null) {
			if(childrenSortRequired(obj)) {
				Collections.sort(uidChildren, cmp);
			}
		}
		else {
			uidChildren = new ArrayList<IUID>(otherChildren.size());
		}
		// Add other childen which we have not added yet(like positions of a connector)
		uidChildren.addAll(otherChildren);
		return uidChildren;
	}

	private boolean childrenSortRequired(@Nullable IUIDObject object)
	{
		//sort not required for children of a Cable
		return !SingleLineHelper.isSingleLine(object);
	}

	@Nullable protected List<IUID> getConnectivityChildrens(IUIDObject obj, List<IUID> otherChildren)
	{
		List<IUID> connectivityChildrens = new ArrayList<IUID>();
		// the connectivity nodes
		if (obj instanceof IPinList) {
			connectivityChildrens = getChildren((IPinList) obj);
			if (obj instanceof IConnector) {
				otherChildren.addAll(getPositionsOrPositionedObjects((IConnector) obj));
			}
		}
		else if (obj instanceof IConductor) {
			// center-stripped splice has connectivity wire child (should it?) - wire has no children in this case
			IUIDObject parentObj = getObject(parentNodeUID);
			if (obj instanceof IFunctionMessage) {
				connectivityChildrens = getChildren((IFunctionMessage) obj);
			}
			else if (obj instanceof IFunctionConductor && ((IFunctionConductor) obj).isAssociatedMessageSignal()) {
				connectivityChildrens =
						getChildrenOfMessageSignal((IFunctionConductor) obj);
			}
			else if (parentObj instanceof IHighwaySchematic || parentObj instanceof IHighway) {
				//uidChildren = getHighwayConductorChildren((IHighwaySchematic) parentObj, (IConductor) obj);
			}
			else if (!(parentObj instanceof ISplice)) {
				connectivityChildrens = getChildren((IConductor) obj);
			}
		}
		else if (obj instanceof IMulticore) {
			connectivityChildrens = getMulticoreChildren((IMulticore) obj);
		}
		else if (obj instanceof ILibraryInnerCore) {
			connectivityChildrens = getInnercoreChildren((ILibraryInnerCore) obj);
		}
		else if (obj instanceof IAssembly) {
			connectivityChildrens = getAssemblyElements(obj);
		}
		else if (obj instanceof ILogicOtherComponent) {
			return getLayoutComponentRepresentations((ILogicOtherComponent) obj);
		}
		//this should be processed first before IHighway
		else if (obj instanceof ISingleLine) {
			connectivityChildrens = getChildren((ISingleLine) obj);
		}
		else if (obj instanceof IGeneralHighway) {
			connectivityChildrens = getChildren((IGeneralHighway) obj);
		}

		// the schematic nodes (may be implemented via usages later)
		else if (obj instanceof chs.cof.logical.schem.IPinList) {
			connectivityChildrens = getChildren((chs.cof.logical.schem.IPinList) obj);
		}
		else if (obj instanceof chs.cof.logical.schem.IConductor) {
			// For message schematic pull signals, if not ports under it.
			if (((IConnectivityRef) obj).getConnectivity() instanceof IFunctionMessage) {
				connectivityChildrens = getSignalsOrPortsUnderMessage((chs.cof.logical.schem.IConductor) obj);
			}
			else {
				connectivityChildrens = getChildren((chs.cof.logical.schem.IConductor) obj);
			}
		}
		else if (obj instanceof IHighwaySchematic) {
			IUIDObject parentObj = getObject(parentNodeUID);
			if (parentObj instanceof IConductor) {
				connectivityChildrens = getPinsConnectedThroughHighway((IConductor) parentObj, (IHighwaySchematic) obj);
			}
			else {
				connectivityChildrens = getChildren((IHighwaySchematic) obj);
			}
		}

		else if (obj instanceof ISchemStackPin) {
			connectivityChildrens = getChildren((ISchemStackPin) obj);
		}
		else if (obj instanceof IInternalPosition) {
			return getChildren((IInternalPosition) obj);
		}
		else if (obj instanceof ISharedInternalPosition) {
			return getChildren((ISharedInternalPosition) obj);
		}
		return connectivityChildrens;
	}

	protected List<IUID> getFolderChildren(IUIDObject obj)
	{
		List<IUID> uidChildren = new ArrayList<IUID>();
		LogicFolder logicFolder = m_folderObjectMap.get(obj);
		if (logicFolder != null) {
			if (logicFolder == LogicFolder.DEVICE) {
				uidChildren = getDevices(true);
			}
			else if (logicFolder == LogicFolder.CONNECTOR) {
				uidChildren = getConnectors(true);
			}
			else if (logicFolder == LogicFolder.RING_TERMINAL) {
				uidChildren = getRingTerminals(true);
			}
			else if (logicFolder == LogicFolder.INLINE) {
				uidChildren = getInlines(true);
			}
			else if (logicFolder == LogicFolder.SPLICE) {
				uidChildren = getSplices(true);
			}
			else if (logicFolder == LogicFolder.CONDUCTORS) {
				uidChildren = getConductors(true);
			}
			else if (logicFolder == LogicFolder.HIGHWAYS/* || logicFolder == LogicFolder.FUNCTION_HIGHWAYS*/) {
				uidChildren = getHighways();
			}
			else if (logicFolder == LogicFolder.SINGLE_LINES/* || logicFolder == LogicFolder.FUNCTION_HIGHWAYS*/) {
				uidChildren = getSingleLines();
			}
			else if (logicFolder == LogicFolder.MULTICORE) {
				uidChildren = getMulticores();
			}
			else if (logicFolder == LogicFolder.OVERBRAID) {
				uidChildren = getOverBraids();
			}
			else if (logicFolder == LogicFolder.ASSEMBLY) {
				uidChildren = getAssemblies();
			}
			else if (logicFolder == LogicFolder.LOGIC_BLOCKS/* || logicFolder == LogicFolder.FUNCTION_BLOCK*/) {
				uidChildren = getBlockDevices(true);
			}
			else if (logicFolder == LogicFolder.OTHERCOMPONENT) {
				uidChildren = getOtherComponents(true);
			}
			else if (logicFolder == LogicFolder.WIREDUCT) {
				uidChildren = getWireDucts(true);
			}
			else if (logicFolder == LogicFolder.MOUNTINGRAIL) {
				uidChildren = getMountingRails(true);
			}
			else if (logicFolder == LogicFolder.FUNCTION_COMPONENT) {
				uidChildren = getFunctionComponents(true);
			}
			else if (logicFolder == LogicFolder.FUNCTION_CONDUCTOR) {
				uidChildren = getFunctionConductors(true);
			}
			else if (logicFolder == LogicFolder.FUNCTION_MESSAGE) {
				uidChildren = getFunctionMessages(true);
			}
		}

		// Unplaced folder?
		IUIDObject parentObj = getObject(parentNodeUID);
		logicFolder = m_folderObjectMap.get(parentObj);
		if (logicFolder != null) {
			if (logicFolder == LogicFolder.DEVICE) {
				uidChildren = getUnusedDevices();
			}
			else if (logicFolder == LogicFolder.CONNECTOR) {
				uidChildren = getUnusedConnectors();
			}
			else if (logicFolder == LogicFolder.RING_TERMINAL) {
				uidChildren = getUnusedRingTerminals();
			}
			else if (logicFolder == LogicFolder.INLINE) {
				uidChildren = getUnusedInlines();
			}
			else if (logicFolder == LogicFolder.SPLICE) {
				uidChildren = getUnusedSplices();
			}
			else if (logicFolder == LogicFolder.CONDUCTORS) {
				uidChildren = getUnusedConductors();
			}
			else if (logicFolder == LogicFolder.HIGHWAYS) {
				uidChildren = getUnusedHighways();
			}
			else if (logicFolder == LogicFolder.SINGLE_LINES) {
				uidChildren = getUnusedSingleLines();
			}
			else if (logicFolder == LogicFolder.LOGIC_BLOCKS/* || logicFolder == LogicFolder.FUNCTION_BLOCK*/) {
				uidChildren = getUnusedBlockDevices();
			}
			else if (logicFolder == LogicFolder.OTHERCOMPONENT) {
				uidChildren = getUnusedOtherComponents();
			}
			else if (logicFolder == LogicFolder.WIREDUCT) {
				uidChildren = getUnusedWireDucts();
			}
			else if (logicFolder == LogicFolder.MOUNTINGRAIL) {
				uidChildren = getUnusedMountingRails();
			}
			else if (logicFolder == LogicFolder.FUNCTION_COMPONENT) {
				uidChildren = getUnusedFunctionComponents();
			}
			else if (logicFolder == LogicFolder.FUNCTION_CONDUCTOR) {
				uidChildren = getUnusedFunctionConductors();
			}
			else if (logicFolder == LogicFolder.FUNCTION_MESSAGE) {
				uidChildren = getUnusedFunctionMessages();
			}
			else if (logicFolder == LogicFolder.MULTICORE) {
				uidChildren = getUnusedMultiCores(false);
			}
		}
		if (parentObj instanceof IPinList) {
			// Unplaced Pins folder
			uidChildren = getUnusedPins((IPinList) parentObj);
		}
		if (parentObj instanceof IConductor) {
			// Unplaced Pins folder
			uidChildren = getUnusedPins((IConductor) parentObj);
		}
		return uidChildren;
	}

	private List<IUID> getMountingRails(boolean isCreateUnusedFolder)
	{
		return getOtherComponents(LogicOtherComponentTypeEnum.RAIL, "BrowserClient.Unplaced.MountingRails.text",
				isCreateUnusedFolder);
	}

	private List<IUID> getWireDucts(boolean isCreateUnusedFolder)
	{
		return getOtherComponents(LogicOtherComponentTypeEnum.DUCT, "BrowserClient.Unplaced.WireDucts.text",
				isCreateUnusedFolder);
	}

	private List<IUID> getOtherComponents(boolean isCreateUnusedFolder)
	{
		return getOtherComponents(LogicOtherComponentTypeEnum.GENERIC, "BrowserClient.Unplaced.OtherComponents.text",
				isCreateUnusedFolder);
	}

	protected List<IUID> getAssemblyElements(IUIDObject obj)
	{
		return createUIDList(((IAssembly) obj).getElements());
	}

	protected List<IUID> getLayoutComponentRepresentations(ILogicOtherComponent logicOtherComponent)
	{
		// if the highway is unplaced then it loses all conductors that are interfaced with the highway
		IDesignWideUsageMgr dwum = getDWUM();
		int usageCount = dwum.getDesignSharedUsageCount(logicOtherComponent);
		if (usageCount == 0) {
			return Collections.emptyList();
		}
		// connectivity layout component with representation(s) in the design
		// return a list of the schematic representations based on these usages
		// this *will* load diagrams but only possibly those on which the representations exist
		// we may consider optimizing this if performance/capacity of the browser becomes an issue
		Collection<IDiagramObject> diagramObjects = dwum.getRepresentations(logicOtherComponent);
		// Above call to get representations may load the diagram
		// This could refresh the design content if design is not locked or under concurrent edit
		if (diagramObjects.isEmpty()) {
			return Collections.emptyList();
		}
		// otherwise just return the UIDs of the schem pinlists we've just loaded
		return createUIDList(diagramObjects.iterator());
	}

	private List<IUID> getChildren(IHighway highway)
	{
		// if the highway is unplaced then it loses all conductors that are interfaced with the highway
		IDesignWideUsageMgr dwum = getDWUM();
		int usageCount = dwum.getDesignSharedUsageCount(highway);
		if (usageCount == 0) {
			return Collections.emptyList();
		}

		// connectivity pinlist with representation(s) in the design
		// return a list of the schematic representations based on these usages
		// this *will* load diagrams but only possibly those on which the representations exist
		// we may consider optimizing this if performance/capacity of the browser becomes an issue
		Collection<IDiagramObject> representations = dwum.getRepresentations(highway);
		// Above call to get representations may load the diagram
		// This could refresh the design content if design is not locked or under concurrent edit
		if (representations.isEmpty()) {
			return Collections.emptyList();
		}
		List<IUID> uidChildren;
		if (representations.size() == 1) {
			// single representation - miss out the schem pinlist level and just return it's children
			uidChildren = getChildren((IHighwaySchematic) representations.iterator().next());
		}
		else {
			// otherwise just return the UIDs of the schem pinlists we've just loaded
			uidChildren = createUIDList(representations.iterator());
		}

		return uidChildren;
	}

	@NotNull private List<IUID> getChildren(ISingleLine singleLine)
	{
		IDesignWideUsageMgr dwum = getDWUM();
		Collection<IDiagramObject> representations = dwum.getRepresentations(singleLine);
		List<IUID> uidChildren = new ArrayList<>();

		if (representations.size() == 1) {
			IHighwaySchematic highwaySchematic = (IHighwaySchematic) representations.stream().findAny().get();
			uidChildren.addAll(getChildren(highwaySchematic));
		}
		else {
			uidChildren.addAll(getSingleLineRepresentations(representations));
		}

		return uidChildren;
	}

	@NotNull private List<IUID> getSingleLineRepresentations(@NotNull Collection<IDiagramObject> representations)
	{
		List<IUID> singleLineRepresentations = new ArrayList<>();
		for (IDiagramObject representation : representations) {
			assert representation instanceof IHighwaySchematic : "Cable representation expected";
			singleLineRepresentations.add(representation.getUID());
		}

		sortSingleLineRepresentations(singleLineRepresentations);
		return singleLineRepresentations;
	}

	private List<IUID> getChildren(@NotNull IFunctionMessage message)
	{
		if (!message.getActiveSignals().isEmpty()) {
			IDesignWideUsageMgr dwum = getDWUM();
			int usageCount = dwum.getDesignSharedUsageCount(message);
			List<IUID> signals = new ArrayList<>();
			Set<IFunctionConductor> activeSignals = message.getActiveSignals();
			signals.addAll(activeSignals.stream()
					.map(sig -> sig.getUID())
					.collect(Collectors.toSet()));
			if (usageCount == 0) {
				return signals;
			}

			Collection<IDiagramObject> representations = dwum.getRepresentations(message);
			if (representations.size() == 1) {
				// single representation - miss out the message level and just return it's children
				return signals;
			}
			// otherwise just return the UIDs of the schem messages we've just loaded
			List<IUID> uidChildren = createUIDList(representations.iterator());
			return uidChildren;
		}
		else { // if there are no signals return pins on message/message representation
			IDesignWideUsageMgr dwum = getDWUM();

			int usageCount = dwum.getDesignSharedUsageCount(message);
			if (usageCount == 0) {
				return createUIDList(message.getPins());
			}

			Collection<IDiagramObject> representations = dwum.getRepresentations(message);
			if (representations.size() == 1) {
				// single representation - return schem pins or actual object pins.
				IDiagramObject diagObj = representations.iterator().next();
				if (diagObj instanceof chs.cof.logical.schem.IConductor) {
					List<IUID> uidChildren = getAllPins(message, representations,
							(chs.cof.logical.schem.IConductor) diagObj);
					return uidChildren;
				}
				return createUIDList(message.getPins());
			}
			// otherwise just return the UIDs of the schem messages we've just loaded
			List<IUID> uidChildren = createUIDList(representations.iterator());
			// add unconnected conductor pins too
			uidChildren.addAll(getCondPinsNotConnectedSchemtically(message, representations));
			return uidChildren;
		}
	}

	@NotNull private List<IUID> getChildren(IHighwaySchematic highwaySchematic)
	{
		List<IUID> uidChildren = new ArrayList<IUID>();

		IHighway highway = highwaySchematic.getConnectivity();
		if(SingleLineHelper.isSingleLineHighway(highway)){
			if(!highwaySchematic.getConnectedStackPins().isEmpty()) {
				//include ends (devices) that are connected via schematic stack pin
				extractCableHighwayChildren(highwaySchematic, uidChildren);
			}

			sortSingleLineChildrenByName(uidChildren);

			return uidChildren;
		}
		for (chs.cof.logical.schem.IConductor conductor : highwaySchematic.getConductors()) {
			IUID cableUID = conductor.getConnectivity().getUID();

			if (!uidChildren.contains(cableUID)) {
				uidChildren.add(cableUID);
			}
		}

		// Adds conductors that are connected to pins in the staked pin
		Set<IConductor> connectedConds = new HashSet<IConductor>();
		for (IUID pinUID : highwaySchematic.getConnectedStackPins()) {
			ISchemStackPin stackedPin = UIDMgr.getObjectOfType(pinUID, ISchemStackPin.class);
			if (stackedPin != null) {
				for (IAbstractPin pin : stackedPin.getAllConnectivity()) {
					connectedConds.addAll(pin.getConductorsAsSet());
				}
			}
		}
		for (IConductor conductor : HighwayHelper.toStackPinConductors(highway)) {
			IUID cableUID = conductor.getUID();
			if (!uidChildren.contains(cableUID) && connectedConds.contains(conductor)) {
				uidChildren.add(cableUID);
			}
		}
		return uidChildren;
	}

	/**
	 * Sorts single line children by their name.
	 * It is assumed that multicore is not part of the children list.
	 */
	private void sortSingleLineChildrenByName(@NotNull List<IUID> uidChildren)
	{
		Comparator<IUID> compareByName = (childUid1, childUid2) -> {
			String child1Name = getSingleLineChildName(childUid1);
			String child2Name = getSingleLineChildName(childUid2);
			return child1Name.compareTo(child2Name);
		};

		Collections.sort(uidChildren, compareByName);
	}

	@NotNull private String getSingleLineChildName(@NotNull IUID singleLineChildUid)
	{
		IUIDObject uidObject = UIDMgr.getObject(singleLineChildUid);
		String objectName = null;
		// Right now single line contains devices only as children.
		// Modify below code when single line starts supporting other types of children.
		if (uidObject instanceof chs.cof.logical.schem.IPinList device) {
			objectName = device.getConnectivity().getName();
		}

		assert objectName != null;
		return objectName;
	}

	private void sortSingleLineRepresentations(@NotNull List<IUID> uidRepresentations)
	{
		Comparator<IUID> compareByDiagramNames = (uid1, uid2) -> {
			String diagramName1 = getDiagramName(uid1);
			String diagramName2 = getDiagramName(uid2);

			if (!diagramName1.equals(diagramName2)) {
				return diagramName1.compareTo(diagramName2);
			}

			String objectName1 = getPresentationName(uid1);
			String objectName2 = getPresentationName(uid2);

			assert objectName1 != null;
			assert objectName2 != null;

			return objectName1.compareTo(objectName2);
		};

		Collections.sort(uidRepresentations, compareByDiagramNames);
	}

	@NotNull private String getDiagramName(@NotNull IUID uid)
	{
		IUIDObject uidObject = UIDMgr.getObject(uid);
		if (uidObject instanceof IStyleableObject styleableObject) {
			IStyleableDiagram diagram = styleableObject.getOwningStyleableDiagram();
			assert diagram != null;
			return diagram.getName();
		}

		return "";
	}

	private void extractCableHighwayChildren(IHighwaySchematic highwaySchematic, List<IUID> uidChildren)
	{
		ISingleLine cable = (ISingleLine) highwaySchematic.getConnectivity();
		ISchemDiagram diagram = DiagramHelper.getDiagram(highwaySchematic);
		String cableDiagram = diagram != null ? diagram.getName(): StringUtils.EMPTY_STRING;
		for (IUID stackPinUID : highwaySchematic.getConnectedStackPins()) {
			IStyleableObject stackPin = (IStyleableObject) stackPinUID.getObject();
			if(stackPin != null) {
				//add the connected end (device) as children only if both are in same diagram
				IStyleableDiagram stackPinDiagram = stackPin.getOwningStyleableDiagram();
				if(stackPinDiagram != null && stackPin.getParent() != null){
					IConnectivityRef parent = (IConnectivityRef) stackPin.getParent();
					IUID connectedUID = parent.getConnectivity().getUID();
					boolean isConnectedEnd = cable.isConnectedEnd(connectedUID);
					if(isConnectedEnd && StringUtils.equals(cableDiagram, stackPinDiagram.getName())){
						uidChildren.add(stackPin.getParent().getUID());
					}
				}
			}
		}
	}

	/**
	 * Gets Pins connected to given conductor through given given highway
	 *
	 * @param highwaySchematic Highway through which conductor connected to the pin
	 * @param cond             Conductor
	 * @return List of pins which are connected to given conductor through given highway
	 */
	private List<IUID> getPinsConnectedThroughHighway(IConductor cond, IHighwaySchematic highwaySchematic)
	{
		List<IUID> pins = new ArrayList<IUID>();
		for (IUID stackedPinUID : highwaySchematic.getConnectedStackPins()) {
			IUIDObject stakedPin = UIDMgr.getNonDeletedObject(stackedPinUID);
			if (stakedPin != null && stakedPin instanceof ISchemStackPin) {
				for (IAbstractPin pin : ((ISchemStackPin) stakedPin).getConnectedPins(cond)) {
					pins.add(pin.getUID());
				}
			}
		}
		return pins;
	}

	private List<IUID> getSignalsOrPortsUnderMessage(@NotNull chs.cof.logical.schem.IConductor conductor)
	{

		IFunctionMessage message = (IFunctionMessage) ((IConnectivityRef) conductor).getConnectivity();
		if (!message.getActiveSignals().isEmpty()) {
			List<IUID> signals = new ArrayList<>();
			Set<IFunctionConductor> activeSignals = message.getActiveSignals();
			signals.addAll(activeSignals.stream()
					.map(sig -> sig.getUID())
					.collect(Collectors.toSet()));
			return signals;
		}
		else {
			return createUIDList(conductor.getPins());
		}
	}

	@Nullable private List<IUID> getChildrenOfMessageSignal(@NotNull IFunctionConductor signal)
	{
		IUIDObject iuidObject = UIDMgr.getObject(parentNodeUID);
		if (iuidObject instanceof chs.cof.logical.schem.IConductor) {
			List<IUID> uidChildren = createUIDList(((chs.cof.logical.schem.IConductor) iuidObject).getPins());
			IDesignWideUsageMgr dwum = getDWUM();
			Collection<IDiagramObject> representations = dwum.getRepresentations(signal.associatedFunctionMessage());
			// add unconnected conductor pins too
			uidChildren.addAll(
					getCondPinsNotConnectedSchemtically(signal.associatedFunctionMessage(), representations));
			return uidChildren;
		}
		else {
			return getChildren((IConductor) signal.associatedFunctionMessage());
		}
	}

	@Nullable protected List<IUID> getChildren(IConductor conductor)
	{
		IDesignWideUsageMgr dwum = getDWUM();
		int usageCount = dwum.getDesignSharedUsageCount(conductor);

		// single instance or unplaced - just show the connectivity pins connected
		if (usageCount == 0) {
			return createUIDList(conductor.getPins());
		}

		// connectivity conductor with multiple representation(s) in the design
		// return a list of the schematic representations based on these usages
		// this *will* load diagrams but only possibly those on which the representations exist
		// we may consider optimizing this if performance/capacity of the browser becomes an issue
		Collection<IDiagramObject> representations = dwum.getRepresentations(conductor);
		// Above call to get representations may load the diagram
		// This could refresh the design content if design is not locked or under concurrent edit
		if (representations.isEmpty()) {
			if (!conductor.isRemotelyDeleted()) {
				return createUIDList(conductor.getPins());
			}
			else {
				return new ArrayList<IUID>();
			}
		}
		boolean isMCShieldWithMultiTerminations =
				conductor instanceof IShieldConductor && conductor.getPinSet().size() > 1
						&& conductor.getMulticore() != null;

		List<IUID> uidChildren = new ArrayList<IUID>();
		if (representations.size() == 1 && !isMCShieldWithMultiTerminations) {
			// single representation - miss out the schem level and just return it's children
			IDiagramObject diagObj = representations.iterator().next();
			if (diagObj instanceof chs.cof.logical.schem.IConductor) {
				uidChildren = getAllPins(conductor, representations, (chs.cof.logical.schem.IConductor) diagObj);
			}
			else if (diagObj instanceof IHighwaySchematic) {
				uidChildren.add(diagObj.getUID());
				if (uidChildren.size() < conductor.getPinSet().size()) {
					uidChildren.addAll(getCondPinsNotConnectedSchemtically(conductor, representations));
				}
			}
		}
		else {
			// otherwise just return the UIDs of the schems we've just loaded
			uidChildren = createUIDList(conductor instanceof IShieldConductor && conductor.getMulticore() != null ?
					getMCShieldRepresentations((IShieldConductor) conductor) : representations.iterator());
			// check for connections, if any conductor which has connectivity but not connected schematically then add connectivity pin.
			uidChildren.addAll(getCondPinsNotConnectedSchemtically(conductor, representations));
		}
		return uidChildren;
	}

	@NotNull
	private List<IUID> getAllPins(@NotNull IConductor conductor, @NotNull Collection<IDiagramObject> representations,
			@NotNull chs.cof.logical.schem.IConductor diagObj)
	{
		List<IUID> children = getChildren(diagObj);
		long connectedPinSize = UIDUtils.convertToObjectSet(children, IPin.class)
				.stream()
				.map(iPin -> iPin.getConnectivity())
				.distinct()
				.count();
		if (connectedPinSize < conductor.getPinSet().size()) {
			children.addAll(getCondPinsNotConnectedSchemtically(conductor, representations));
		}
		return children;
	}

	/**
	 * Get all the schem conductor pins from representaions and check with passed cable conductor pins.
	 * If any of the conductor pins are not schematically connected then return those.
	 *
	 * @param conductor       : conductor to check
	 * @param representations : diagram representations of conductor
	 * @return pins which has connectivity but not connected schematically
	 */
	@NotNull private List<IUID> getCondPinsNotConnectedSchemtically(@NotNull IConductor conductor,
			@NotNull Collection<IDiagramObject> representations)
	{
		List<IAbstractPin> placedPins = new ArrayList<>();
		for (IDiagramObject diagObj : representations) {
			if (diagObj instanceof chs.cof.logical.schem.IConductor) {
				List<IUID> diagObjChildren = getChildren((chs.cof.logical.schem.IConductor) diagObj);
				for (IUID iuid : diagObjChildren) {
					IUIDObject uidObject = getObject(iuid);
					if (uidObject instanceof IPin) {
						placedPins.add(((IPin) uidObject).getConnectivity());
					}
				}
			}
			else if (diagObj instanceof IHighwaySchematic) {
				List<IUID> pins = getPinsConnectedThroughHighway(conductor, (IHighwaySchematic) diagObj);
				for (IUID uid : pins) {
					IUIDObject iuidObject = getObject(uid);
					if (iuidObject instanceof IAbstractPin) {
						placedPins.add((IAbstractPin) iuidObject);
					}
				}
			}
		}
		List<IUID> uidList = new ArrayList<IUID>();
		for (IAbstractPinIterator it = conductor.getPins(); it.hasNext(); ) {
			IAbstractPin pin = it.next();
			if (!placedPins.contains(pin)) {
				uidList.add(pin.getUID());
			}
		}
		return uidList;
	}

	private Iterator<? extends IDiagramObject> getMCShieldRepresentations(IShieldConductor shield)
	{
		MCShieldSchemFinder finder = new MCShieldSchemFinder(shield);
		return finder.getRepresentations();
	}

	private List<IUID> getChildren(chs.cof.logical.schem.IConductor schemConductor)
	{
		// these are the pins of the conductor to which this schem is graphically connected
		// excluding center stripped splice pins which are not shown
		IWireConductor wire = CommonUtils.cast(schemConductor.getConnectivity(), IWireConductor.class);
		List<IUID> uidChildren = new ArrayList<IUID>();
		for (IPin schemPin : schemConductor.getPins()) {
			boolean isCenterStrip = false;
			if (wire != null) {
				IPinList owner = schemPin.getConnectivity().getOwner();
				if (owner instanceof ISplice) {
					ISplice splice = (ISplice) owner;
					for (ISplice centerStripSplice : wire.getCenterStripSplices()) {
						if (splice == centerStripSplice) {
							isCenterStrip = true;
						}
					}
				}
			}
			if (!isCenterStrip) {
				uidChildren.add(schemPin.getUID());
			}
		}
		return uidChildren;
	}

	@Nullable protected List<IUID> getChildren(IPinList pl)
	{
		return getPinlistChildren(pl, true);
	}

	@Nullable protected List<IUID> getPinlistChildren(IPinList pl, boolean includeRepresentation)
	{
		if (pl instanceof IBackshell) {
			// backshell connectivity nodes are very different to other pinlists
			return getBackshellChildren((IBackshell) pl);
		}
		List<IUID> uidChildren = new ArrayList<>();
		IDesignWideUsageMgr dwum = getDWUM();
		int usageCount = dwum.getDesignSharedUsageCount(pl);
		if (usageCount == 0) {
			// Unplaced pinlist - no children because we dont show the pins (or center stripped wires)
			return null;
		}

		if (includeRepresentation) {

			// connectivity pinlist with representation(s) in the design
			// return a list of the schematic representations based on these usages
			// this *will* load diagrams but only possibly those on which the representations exist
			// we may consider optimizing this if performance/capacity of the browser becomes an issue
			Collection<IDiagramObject> representations = dwum.getRepresentations(pl);
			// Above call to get representations may load the diagram
			// This could refresh the design content if design is not locked or under concurrent edit
			if (representations.isEmpty()) {
				return null;
			}

			if (representations.size() == 1) {
				// single representation - miss out the schem pinlist level and just return it's children
				uidChildren = getChildren((chs.cof.logical.schem.IPinList) representations.iterator().next());
			}
			else {
				// otherwise just return the UIDs of the schem pinlists we've just loaded
				uidChildren = createUIDList(representations.iterator());
			}

			// add Unplaced folder if any pins are unplaced
			if (hasUnusedObject(pl.getPins())) {
				String folderName;
				if (pl instanceof IFunction) {
					folderName = ResourceMgr.getString(BrowserClient.class, "BrowserClient.Unplaced.Ports.text");
				}
				else {
					folderName = ResourceMgr.getString(BrowserClient.class, "BrowserClient.Unplaced.Pins.text");
				}

				BrowserFolder unplaced = createFolder(folderName);
				uidChildren.add(unplaced.getUID());
			}
		}
		else if (usageCount == 1) {
			//handling backshell seperately when includeRepresentation == false
			if (pl instanceof IConnector) {
				IBackshell backshell = ((IConnector) pl).getBackshell();
				if (backshell != null) {
					uidChildren.add(backshell.getUID());
				}
			}
		}

		// connectivity wires are always children for center stripped splices
		if (pl instanceof ISplice) {
			uidChildren.addAll(createUIDList(((ISplice) pl).getCenterStrippedWires()));
		}

		// TODO jacobt FEAT13040 : don't add invisible schem objects - could be a problem without loading the schems

		return uidChildren;
	}

	@Nullable protected List<IUID> getBackshellChildren(IBackshell backshell)
	{
		// backshell nodes return backshell terminations of the schematic connector node that is the parent *node*
		// This schematic UID should have previously been stored
		IUIDObject parent = getObject(parentNodeUID);

		if (IAssembly.class.isAssignableFrom(parent.getClass())) {
			return Collections.emptyList();
		}

		chs.cof.logical.schem.IPinList parentSchem = getParentSchem(parent);

		// the children are the schematic backshell terminations of the parent node
		if (parentSchem == null) {
			assert false; // this may not always hold - e.g. when we do Unplaced folders, but currently it does.
			return null;
		}

		DSCWithBackshellPlaceholderHelper helper = new DSCWithBackshellPlaceholderHelper(parentSchem);
		chs.cof.logical.schem.IPinList plForPlaceholderCreation = helper.getPinListForPlaceholderCreation();
		List<IUID> uidChildren = new ArrayList<IUID>();
		// FEAT00013786: backshell terminations are not yet allowed in stack pins. so no need to iterate over stack pins.
		List<IPin> terminations = CollectionUtils.getFilteredCollection(plForPlaceholderCreation.getPins(),
				p -> isBackshellTerminationPin(backshell, p));
		if (parentSchem.getConnectivity() instanceof IDeviceConnector) {
			// Show backshell pin only under the schem DSC instance that contains it.
			// Map pins to their owning schematic DeviceConnector instance.
			// Required when multiple schem DSC representations exist.
			Map<IAbstractSchemPin, chs.cof.logical.schem.IPinList> schemPinToSchemDSCMap =
					SchemPinListHelper.mapPinToSchemDSC(plForPlaceholderCreation);
			for (IPin pin : terminations) {
				if (parentSchem.equals(schemPinToSchemDSCMap.get(pin))) {
					uidChildren.add(pin.getUID());
				}
			}
		}
		else {
			terminations.forEach(t -> uidChildren.add(t.getUID()));
		}
		return uidChildren;
	}

	private boolean isBackshellTerminationPin(@NotNull IBackshell backshell, @NotNull IPin pin)
	{
		return pin.getConnectivity() instanceof IBackshellTermination bsTerm && backshell.equals(bsTerm.getOwner());
	}

	@Nullable private chs.cof.logical.schem.IPinList getParentSchem(@Nullable IUIDObject parent)
	{
		if (parent instanceof chs.cof.logical.schem.IPinList parentPl) {
			return parentPl;
		}
		if (parent instanceof IConnector conn) {
			// we *should* only ever get here when there is a single usage of a connectivity connector with a backshell
			// if not then we'll assert at the end
			IDesignWideUsageMgr dwum = getDWUM();
			if (dwum.getDesignSharedUsageCount(conn) == 1) {
				Collection<IDiagramObject> reps = dwum.getRepresentations(conn);
				if (reps.size() == 1 && reps.iterator().next() instanceof chs.cof.logical.schem.IPinList pinList) {
					// it should meet all of these checks - or we'll assert below
					return pinList;
				}
			}
		}
		return null;
	}

	protected List<IUID> getChildren(chs.cof.logical.schem.IPinList pl)
	{
		if(isSingleLineParent(parentNodeUID)){
			//no need to display device details (e.g.: pins) for Single line's ends
			return Collections.emptyList();
		}
		// get the pins for this representation
		IPinList cpl = pl.getConnectivity();
		List<IUID> uidChildren = new ArrayList<IUID>();
		boolean isBackshell = cpl instanceof IBackshell;
		for (IPin pin : pl.getPins()) {
			boolean isBackshellTermination = pin.getConnectivity() instanceof IBackshellTermination;
			if (!(isBackshell ^ isBackshellTermination)) {
				uidChildren.add(pin.getUID());
			}
		}

		for (ISchemStackPin pin : pl.getStackPins()) {
			if (pin.getNumPins() > 0) {
				uidChildren.add(pin.getUID());
			}
			else if (pin.getNumPins() == 0 /*&& pl.getConnectivity() instanceof IBlockDevice*/) {
				for (IHighwaySchematic highwaySchematic : pin.getConnectedHighways()) {
					if(SingleLineHelper.isSingleLineSchematic(highwaySchematic)){
						//not required to show cables
						continue;
					}
					uidChildren.add(highwaySchematic.getUID());
				}
			}
		}

		if (cpl instanceof IConnector) {
			IBackshell backshell = ((IConnector) cpl).getBackshell();
			if (backshell != null) {
				uidChildren.add(backshell.getUID());
			}
		}
		//dts0100536430 - Fix: Iterate the device connectors if this is a schematic device
		if (cpl instanceof IDevice) {
			for (chs.cof.logical.schem.IPinList schemAttachment : pl.getAttachedPinListObjects()) {
				if (schemAttachment
						.getConnectivity() instanceof IDeviceConnector) {   //This is a device connector, add it to this representation children
					uidChildren.add(schemAttachment.getUID());
				}
			}
		}
		return uidChildren;
	}

	private List<IUID> getChildren(ISchemStackPin stackPin)
	{
		// get the pins for this representation
		List<IUID> uidChildren = new ArrayList<IUID>();
		for (IAbstractPin pin : stackPin.getAllConnectivity()) {
			uidChildren.add(pin.getUID());
		}
		return uidChildren;
	}

	private List<IUID> getChildren(IInternalPosition internalPosition)
	{
		List<IUID> uidChildren = new ArrayList<IUID>();

		for (IInternalPositionedObject positionedObj : internalPosition.getAssociatedObjects()) {
			uidChildren.add(positionedObj.getUID());
		}
		if (uidChildren.isEmpty() && internalPosition.getSharedPosition() != null) {
			uidChildren.addAll(getChildren(internalPosition.getSharedPosition()));
		}
		return uidChildren;
	}

	private List<IUID> getChildren(ISharedInternalPosition sharedPosition)
	{
		List<IUID> uidChildren = new ArrayList<IUID>();
		for (IInternalPositionedObject posObj : sharedPosition.getPositionedObjects()) {
			if (posObj instanceof IInternalPositionsContainer) {
				uidChildren.addAll(createUIDList(((IInternalPositionsContainer) posObj).getPositions()));
			}
		}
		return uidChildren;
	}

	public List<IUID> getMulticoreChildren(IMulticore mc)
	{
		List<IUID> multicoreChildren = m_designObjectHierarchyHelper.getMulticoreChildren(mc);
		IDesignContainer design = mc.getDesign();
		if (design != null && design.isReleased()) {
			multicoreChildren.removeIf(t -> t.getObject() instanceof ILibraryInnerCore);
		}
		return multicoreChildren;
	}

	private static List<IUID> getInnercoreChildren(ILibraryInnerCore ic)
	{
		List<IUID> vec = new ArrayList<IUID>();
		if (!LibraryHelper.isInnerCoreLeaf(ic)) {
			for (ILibraryInnerCore liwIt : LibraryHelper.getInnerCores(ic)) {
				vec.add(liwIt.getUID());
			}
		}
		return vec;
	}

	protected List<IUID> getDevices(boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		// add the various object types
		boolean allUsed = addUsedObjects(uids, conn.getDevices());
		allUsed = addUsedObjects(uids, conn.getInterconnectDevices()) && allUsed;
		allUsed = addUsedObjects(uids, conn.getGroundDevices()) && allUsed;

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Devices.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getUnusedBlockDevices()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getBlockDevices());
		return unused;
	}

	protected List<IUID> getUnusedFunctionComponents()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getFunctions());
		return unused;
	}

	protected List<IUID> getUnusedFunctionConductors()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getFunctionConductors());
		return unused;
	}

	protected List<IUID> getUnusedFunctionMessages()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getFunctionMessages());
		return unused;
	}

	protected List<IUID> getUnusedDevices()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getDevices());
		addUnusedObjects(unused, conn.getInterconnectDevices());
		addUnusedObjects(unused, conn.getGroundDevices());
		return unused;
	}

	@NotNull protected List<IUID> getUnusedMultiCores(boolean isOverBraid)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		Iterator<IMulticore> it = conn.getMulticores();
		IDesignWideUsageMgr designUsageMgr = getDWUM();
		while (it.hasNext()) {
			IMulticore current = it.next();
			// Skip if type doesn't match the requested filter
			if (current instanceof IOverbraid != isOverBraid) {
				continue;
			}
			if (!designUsageMgr.hasMulticoreUsage(current)){
				unused.add(current.getUID());
			}
		}
		return unused;
	}

	protected List<IUID> getUnusedConnectors()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		List<IUID> unused = new ArrayList<IUID>();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (!(connector instanceof IGenericInlineConnector || connector instanceof IDeviceConnector ||
					connector.isRingTerminal())) {
				if (!dwum.hasUsage(connector) && !PinListHelper.isModularConnector(connector) &&
						!PinListHelper.isChildConnector(connector)) {
					unused.add(connector.getUID());
				}
			}
		}
		return unused;
	}

	protected List<IUID> getUnusedRingTerminals()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		List<IUID> unused = new ArrayList<IUID>();
		for (IRingTerminal ringterminal : getDesignConnectivity().getRingTerminals()) {
			if (!dwum.hasUsage(ringterminal)) {
				unused.add(ringterminal.getUID());
			}
		}
		return unused;
	}

	protected List<IUID> getUnusedInlines()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		List<IUID> unused = new ArrayList<IUID>();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (connector instanceof IGenericInlineConnector) {
				if (!dwum.hasUsage(connector)) {
					unused.add(connector.getUID());
				}
			}
		}
		return unused;
	}

	protected List<IUID> getUnusedSplices()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getSplices());
		return unused;
	}

	private List<IUID> getUnusedPins(IPinList pl)
	{
		List<IUID> pins = new ArrayList<IUID>();
		addUnusedObjects(pins, pl.getPins().iterator());
		return pins;
	}

	private List<IUID> getUnusedPins(IConductor conductor)
	{
		List<IUID> pins = new ArrayList<IUID>();
		addUnusedObjects(pins, conductor.getPins().iterator());
		return pins;
	}

	private List<IUID> getUnusedConnectors(IInternalPosition position)
	{
		List<IUID> connUIds = new ArrayList<IUID>();
		Collection<IConnector> connectors = position.getAssociatedObjectsOfType(IConnector.class);
		addUnusedObjects(connUIds, connectors.iterator());
		return connUIds;
	}

	protected List<IUID> getUnusedConductors()
	{
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConductorIterator it = getDesignConnectivity().getConductors(); it.hasNext(); ) {
			IConductor cond = it.getNext();
			IMulticore mc = cond.getMulticore();
			if (mc == null) {
				if (!dwum.hasUsage((cond))) {
					uids.add(cond.getUID());
				}
			}
		}
		return uids;
	}

	protected List<IUID> getUnusedHighways()
	{
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		for (IHighwayIterator it = getDesignConnectivity().getHighways(); it.hasNext(); ) {
			IHighway highway = it.getNext();
			if (!dwum.hasUsage((highway))) {
				uids.add(highway.getUID());
			}
		}
		return uids;
	}

	@NotNull protected List<IUID> getUnusedSingleLines()
	{
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		for (ISingleLineIterator it = getDesignConnectivity().getSingleLines(); it.hasNext(); ) {
			IHighway singleLine = it.getNext();
			if (!dwum.hasUsage((singleLine))) {
				uids.add(singleLine.getUID());
			}
		}
		return uids;
	}

	protected List<IUID> getConnectors(boolean isCreateUnusedFolder)
	{
		// for connectors we exclude inlines and DCs
		List<IUID> uids = new ArrayList<IUID>();
		boolean allUsed = true;
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (!(connector instanceof IGenericInlineConnector || connector instanceof IDeviceConnector ||
					connector.isRingTerminal())) {
				if ((dwum.hasUsage(connector) || PinListHelper.isModularConnector(connector)) &&
						!PinListHelper.isChildConnector(connector)) {
					uids.add(connector.getUID());
				}
				else if (connector.getOccupiedPosition() == null) {
					allUsed = false;
				}
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Connectors.text", uids, isCreateUnusedFolder, allUsed);

		return uids;
	}

	protected List<IUID> getRingTerminals(boolean isCreateUnusedFolder)
	{
		List<IUID> uids = new ArrayList<IUID>();
		boolean allUsed = true;
		IDesignWideUsageMgr dwum = getDWUM();
		for (IRingTerminal connector : getDesignConnectivity().getRingTerminals()) {
			assert IConnector.Statics.isRingTerminalTypeConnector(connector) :
					"Expected Ring Terminal but a different connector was found.";

			if (dwum.hasUsage(connector)) {
				uids.add(connector.getUID());
			}
			else {
				allUsed = false;
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.RingTerminals.text", uids, isCreateUnusedFolder, allUsed);

		return uids;
	}

	protected List<IUID> getInlines(boolean isCreateUnusedFolder)
	{
		// for connectors we exclude inlines and DCs
		List<IUID> uids = new ArrayList<IUID>();
		boolean allUsed = true;
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (connector instanceof IGenericInlineConnector) {
				if (dwum.hasUsage(connector)) {
					uids.add(connector.getUID());
				}
				else {
					allUsed = false;
				}
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Inlines.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getSplices(boolean isCreateUnusedFolder)
	{
		List<IUID> uids = new ArrayList<IUID>();
		boolean allUsed = addUsedObjects(uids, getDesignConnectivity().getSplices());
		createUIDList(getDesignConnectivity().getSplices());

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Splices.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getConductors(boolean isCreateUnusedFolder)
	{
		// these are the conductors not in any MC (though they may be in an OB)
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		boolean allUsed = true;
		for (IConductorIterator it = getDesignConnectivity().getConductors(); it.hasNext(); ) {
			IConductor cond = it.getNext();
			IMulticore mc = cond.getMulticore();
			if (mc == null) {
				if (dwum.hasUsage(cond)) {
					uids.add(cond.getUID());
				}
				else {
					allUsed = false;
				}
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Conductors.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	private List<IUID> getHighways()
	{
		// these are the conductors not in any MC (though they may be in an OB)
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		boolean allUsed = true;
		for (IHighwayIterator it = getDesignConnectivity().getHighways(); it.hasNext(); ) {
			IHighway highway = it.getNext();
			if (dwum.hasUsage(highway)) {
				uids.add(highway.getUID());
			}
			else {
				allUsed = false;
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Highways.text", uids, true, allUsed);
		return uids;
	}

	private List<IUID> getSingleLines()
	{
		// these are the conductors not in any MC (though they may be in an OB)
		List<IUID> uids = new ArrayList<IUID>();
		IDesignWideUsageMgr dwum = getDWUM();
		boolean allUsed = true;
		for (ISingleLineIterator it = getDesignConnectivity().getSingleLines();
				it.hasNext(); ) {
			IHighway cable = it.getNext();
			if (dwum.hasUsage(cable)) {
				uids.add(cable.getUID());
			}
			else {
				allUsed = false;
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.SingleLines.text", uids, true, allUsed);
		return uids;
	}

	protected List<IUID> getOverBraids()
	{
		// get just the top level OBs
		List<IUID> uidList = new ArrayList<IUID>();

		for (IMulticoreIterator it = getDesignConnectivity().getMulticores(true); it.hasNext(); ) {
			IMulticore mc = it.next();
			if (mc instanceof IOverbraid){
				uidList.add(mc.getUID());
			}
		}
		return uidList;
	}
	protected List<IUID> getMulticores()
	{
		// get just the top level MCs or OBs
		List<IUID> uidList = new ArrayList<IUID>();
		boolean allUsed = true;
		IDesignWideUsageMgr designUsageMgr = getDWUM();

		for (IMulticoreIterator it = getDesignConnectivity().getMulticores(true); it.hasNext(); ) {
			IMulticore mc = it.next();
			if (mc instanceof IOverbraid){
				continue;
			}
			// in case of overBraids no need to check for usage, usage check will be implemented later
			if (designUsageMgr.hasMulticoreUsage(mc)){
				uidList.add(mc.getUID());
			}
			else {
				allUsed = false;
			}
		}

		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Muticores.text", uidList, true, allUsed);
		return uidList;
	}

	protected List<IUID> getAssemblies()
	{
		return createUIDList(getDesignConnectivity().getAssemblies(true));
	}

	protected List<IUID> getBlockDevices(boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		// add the various object types
		boolean allUsed = addUsedObjects(uids, conn.getBlockDevices());
		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.Blocks.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getFunctionComponents(boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		// add the various object types
		boolean allUsed = addUsedObjects(uids, conn.getFunctions());
		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.FunctionComponents.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getFunctionConductors(boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		// add the various object types
		boolean allUsed = addUsedObjects(uids, conn.getFunctionConductors());
		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.FunctionConductors.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	protected List<IUID> getFunctionMessages(boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		// add the various object types
		boolean allUsed = addUsedObjects(uids, conn.getFunctionMessages());
		// add an unplaced folder if required
		createUnplacedFolder("BrowserClient.Unplaced.FunctionMessages.text", uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	private boolean hasUnusedOtherComponents(LogicOtherComponentTypeEnum componentTypeEnum)
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getOtherComponents(), componentTypeEnum);
	}

	private boolean hasOtherComponents(LogicOtherComponentTypeEnum componentTypeEnum)
	{
		IConnectivity conn = getDesignConnectivity();
		return hasObject(conn.getOtherComponents(), componentTypeEnum);
	}

	private List<IUID> getOtherComponents(LogicOtherComponentTypeEnum componentTypeEnum, String uplacedFolderName,
			boolean isCreateUnusedFolder)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> uids = new ArrayList<IUID>();
		final boolean allUsed = addUsedObjects(uids, conn.getOtherComponents(), componentTypeEnum);
		// add an unplaced folder if required
		createUnplacedFolder(uplacedFolderName, uids, isCreateUnusedFolder, allUsed);
		return uids;
	}

	private List<IUID> getUnusedOtherComponents(LogicOtherComponentTypeEnum componentTypeEnum)
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused, conn.getOtherComponents(), componentTypeEnum);
		return unused;
	}

	private List<IUID> getUnusedOtherComponents()
	{
		return getUnusedOtherComponents(LogicOtherComponentTypeEnum.GENERIC);
	}

	private List<IUID> getUnusedMountingRails()
	{
		return getUnusedOtherComponents(LogicOtherComponentTypeEnum.RAIL);
	}

	private List<IUID> getUnusedWireDucts()
	{
		return getUnusedOtherComponents(LogicOtherComponentTypeEnum.DUCT);
	}

	protected IConnectivity getDesignConnectivity()
	{
		IConnectivity conn = getModel().getDesign().getConnectivity();
		assert conn != null; // keep IJ happy - it won't be null here
		return conn;
	}

	private IDesignWideUsageMgr getDWUM()
	{
		ILogicDesign design = (ILogicDesign) getModel().getDesign();
		return design.getDesignWideUsageMgr();
	}

	private List<IUID> createUIDList(Iterator<? extends IUIDObject> it)
	{
		List<IUID> uidList = new ArrayList<IUID>();
		while (it.hasNext()) {
			uidList.add(it.next().getUID());
		}
		return uidList;
	}

	private List<IUID> createUIDList(Collection<? extends IUIDObject> iuidObjects)
	{
		List<IUID> uidList = new ArrayList<IUID>();
		for (IUIDObject uidObject : iuidObjects) {
			uidList.add(uidObject.getUID());
		}
		return uidList;
	}

	/**
	 * Iterate over logic objects adding UIDs to the list if the object is used anywhere in the design according to
	 * usages.
	 *
	 * @param uids List of UIDs to add to
	 * @param it   Iterator of logic objects to examine
	 * @return true if all objects in the iterator were used
	 */
	private boolean addUsedObjects(List<IUID> uids, Iterator<? extends ILogicObject> it)
	{
		return addUsedObjects(uids, it, CommonUtils.getNoFilter());
	}

	private <T extends ILogicObject> boolean addUsedObjects(List<IUID> uids, Iterator<T> it,
			@NotNull IObjectFilter<T> filter)
	{
		boolean allUsed = true;
		IDesignWideUsageMgr dwum = getDWUM();
		while (it.hasNext()) {
			T logicObject = it.next();
			if (cancHeckDWUSage(logicObject) && filter.accept(logicObject)) {
				if (dwum.hasUsage(logicObject)) {
					uids.add(logicObject.getUID());
				}
				else {
					allUsed = false;
				}
			}
		}
		return allUsed;
	}

	protected boolean cancHeckDWUSage(ILogicObject object)
	{
		return true;
	}

	/**
	 * Iterate over logic objects adding UIDs to the list if the object is NOT used anywhere in the design according to
	 * usages.
	 *
	 * @param uids List of UIDs to add to
	 * @param it   Iterator of logic objects to examine
	 * @return true if any object was unused
	 */
	protected boolean addUnusedObjects(List<IUID> uids, Iterator<? extends ILogicObject> it)
	{
		return addUnusedObjects(uids, it, CommonUtils.getNoFilter());
	}

	protected <T extends ILogicObject> boolean addUnusedObjects(List<IUID> uids, Iterator<T> it,
			@NotNull IObjectFilter<T> filter)
	{
		// code duplication police - same logic as above but i didn't to convolute the callers with a flag
		boolean anyUnused = false;
		IDesignWideUsageMgr dwum = getDWUM();
		while (it.hasNext()) {
			T logicObject = it.next();
			if (filter.accept(logicObject) && !dwum.hasUsage(logicObject)) {
				uids.add(logicObject.getUID());
			}
			else {
				anyUnused = true;
			}
		}
		return anyUnused;
	}

	private boolean hasUnusedObject(Iterator<? extends ILogicObject> it)
	{
		return hasUnusedObject(it, CommonUtils.getNoFilter());
	}

	private <T extends ILogicObject> boolean hasUnusedObject(Iterator<T> it, IObjectFilter<T> filter)
	{
		IDesignWideUsageMgr dwum = getDWUM();
		while (it.hasNext()) {
			final T item = it.next();
			if (filter.accept(item) && !dwum.hasUsage(item)) {
				return true;
			}
		}
		return false;
	}

	private <T extends ILogicObject> boolean hasObject(Iterator<T> it, IObjectFilter<T> filter)
	{
		while (it.hasNext()) {
			final T item = it.next();
			if (filter.accept(item)) {
				return true;
			}
		}
		return false;
	}

	protected Model getModel()
	{
		return (Model) model.get();
	}

	protected boolean hasChildren(ILogicOtherComponent logicOtherComponent)
	{
		// any layout component with multiple representations will have those representations as children
		return getDWUM().getDesignSharedUsageCount(logicOtherComponent) > 1;
	}

	protected boolean hasChildren(IPinList pl, IUID parentUID)
	{
		if(isSingleLineParent(parentUID)){
			//no need to display device details (e.g.: pins) for Single line's ends
			return false;
		}
		// special case - backshell children depend on the parent, schem connector node
		if (pl instanceof IBackshell) {
			return hasBackshellChildren((IBackshell) pl);
		}

		// any pinlist with multiple representations will have those representations as children
		int repCount = getDWUM().getDesignSharedUsageCount(pl);
		if (repCount > 1) {
			return true;
		}

		// a single representation will have children if it has pins in the connectivity
		if (repCount == 1) {
			// TODO jacobt FEAT13040 : would liked to have done this check earlier to save going to the DSUM
			// does not work with unplaced pinlists because we can't compare the parent node UID against the top level pinlist folders
			// if a connectivity pinlist has a pin it will have either that pin as a child or something that is the parent of the pin
			// no need to waste time checking the DSUM for these common cases
			if (pl.getNumPins() > 0) {
				return true;
			}

			// special case - single representation of connector with backshell and no pins
			// the backshell will be shown as a child of the connector
			if (pl instanceof IConnector) {
				IBackshell backshell = ((IConnector) pl).getBackshell();
				if (backshell != null) {
					return true;
				}
				if (!((IConnector) pl).getPositions().isEmpty()) {
					return true;
				}
			}

			if (pl instanceof IBlockDevice) {
				IBlockDevice blockDevice = (IBlockDevice) pl;
				if (!blockDevice.getInterfacedHighways().isEmpty()) {
					return true;
				}
			}

			// a connectivity pinlist with a usage may have an Unplaced folder for any pins not used in the design
			//if (parentUID == devices || parentUID == connectors || parentUID == inlines || parentUID == splices) {
			if (m_folderObjectMap.get(parentUID) == LogicFolder.DEVICE ||
					m_folderObjectMap.get(parentUID) == LogicFolder.CONNECTOR ||
					m_folderObjectMap.get(parentUID) == LogicFolder.INLINE ||
					m_folderObjectMap.get(parentUID) == LogicFolder.SPLICE ||
					m_folderObjectMap.get(parentUID) == LogicFolder.FUNCTION_COMPONENT ||
					m_folderObjectMap.get(parentUID) == LogicFolder.LOGIC_BLOCKS) {
				assert repCount > 0; // otherwise parent folder should be "Unplaced"
				IDesignWideUsageMgr dwum = getDWUM();
				for (IAbstractPinIterator it = pl.getPins(); it.hasNext(); ) {
					IAbstractPin pin = it.next();
					if (!dwum.hasUsage(pin)) {
						return true;
					}
				}
			}
		}
		else {
			if (pl instanceof IConnector) {
				return !((IConnector) pl).getPositions().isEmpty();
			}
		}

		// either an unplaced pinlist a single represtation without pins
		return false;
	}

	private boolean isSingleLineParent(IUID parentUID)
	{
		IUIDObject parentObject = getObject(parentUID);

		if (parentObject instanceof ISingleLine) {
			return true;
		}

		if (parentObject instanceof IHighwaySchematic highwaySchematic) {
			return SingleLineHelper.isSingleLineSchematic(highwaySchematic);
		}

		return false;
	}

	protected boolean hasBackshellChildren(IBackshell backshell)
	{
		if (backshell.getNumPins() > 0) {
			// we have to get the terminations from the "parent" schem pinlist here
			// the schems should be loaded by now anyway
			List<IUID> terms = getBackshellChildren(backshell);
			return terms != null && !terms.isEmpty();
		}
		return false; // backshell with no pins never has children
	}

	private boolean hasDevices()
	{
		IConnectivity conn = getDesignConnectivity();
		return conn.getNumDevices() > 0 || conn.getNumGroundDevices() > 0 || conn.getNumInterconnectDevices() > 0;
	}

	private boolean hasUnusedBlockDevices()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getBlockDevices());
	}

	private boolean hasUnusedFunctionComponents()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getFunctions());
	}

	private boolean hasUnusedFunctionConductors()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getFunctionConductors());
	}

	private boolean hasUnusedFunctionMessages()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getFunctionMessages());
	}

	private boolean hasUnusedOtherComponents()
	{
		return hasUnusedOtherComponents(LogicOtherComponentTypeEnum.GENERIC);
	}

	private boolean hasUnusedMountingRails()
	{
		return hasUnusedOtherComponents(LogicOtherComponentTypeEnum.RAIL);
	}

	private boolean hasUnusedWireDucts()
	{
		return hasUnusedOtherComponents(LogicOtherComponentTypeEnum.DUCT);
	}

	private boolean hasUnusedDevices()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getDevices()) ||
				hasUnusedObject(conn.getGroundDevices()) ||
				hasUnusedObject(conn.getInterconnectDevices());
	}

	private boolean hasUnusedConnectors()
	{
		// for connectors we exclude inlines and DCs
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (!(connector instanceof IGenericInlineConnector || connector instanceof IDeviceConnector ||
					connector.isRingTerminal())) {
				if (!dwum.hasUsage(connector) && connector.getOccupiedPosition() == null) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasUnusedRingTerminals()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		for (IRingTerminal ringterminal : getDesignConnectivity().getRingTerminals()) {
			if (!dwum.hasUsage(ringterminal)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUnusedInlines()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (connector instanceof IGenericInlineConnector) {
				if (!dwum.hasUsage(connector)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasUnusedSplices()
	{
		IConnectivity conn = getDesignConnectivity();
		return hasUnusedObject(conn.getSplices());
	}

	private boolean hasUnusedConductors()
	{
		// these are conductors not in a MC
		IDesignWideUsageMgr dwum = getDWUM();
		for (IConductorIterator it = getDesignConnectivity().getConductors(); it.hasNext(); ) {
			IConductor conductor = it.next();
			IMulticore mc = conductor.getMulticore();
			if (mc == null) {
				if (!dwum.hasUsage(conductor)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasUnusedHighways()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		for (IHighwayIterator it = getDesignConnectivity().getHighways(); it.hasNext(); ) {
			IHighway highway = it.next();
			if (!dwum.hasUsage(highway)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUnusedSingleLines()
	{
		IDesignWideUsageMgr dwum = getDWUM();
		for (ISingleLineIterator it = getDesignConnectivity().getSingleLines();
				it.hasNext(); ) {
			IHighway cable = it.getNext();
			if (!dwum.hasUsage(cable)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUnusedMulticore()
	{
		IConnectivity conn = getDesignConnectivity();
		Iterator<IMulticore> it = conn.getMulticores();
		IDesignWideUsageMgr designUsageMgr = getDWUM();
		while (conn.getMulticores().hasNext()) {
			if (!designUsageMgr.hasMulticoreUsage(it.next())){
				return true;
			}
		}
		return false;
	}
	private boolean hasConnectors()
	{
		// for connectors we exclude inlines and DCs
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (!(connector instanceof IGenericInlineConnector || connector instanceof IDeviceConnector ||
					connector.isRingTerminal())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasRingTerminals()
	{
		return !(getDesignConnectivity().getRingTerminals().isEmpty());
	}

	private boolean hasInlines()
	{
		for (IConnectorIterator it = getDesignConnectivity().getConnectors(); it.hasNext(); ) {
			IConnector connector = it.getNext();
			if (connector instanceof IGenericInlineConnector) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSplices()
	{
		return getDesignConnectivity().hasSplices();
	}

	private boolean hasConductors()
	{
		// these are conductors not in a MC - but note that conductors in an overbraid *are* shown as conductors in their own right
		for (IConductorIterator it = getDesignConnectivity().getConductors(); it.hasNext(); ) {
			IConductor conductor = it.next();
			IMulticore mc = conductor.getMulticore();
			if (mc == null) {
				return true;
			}
		}
		return false;
	}

	private boolean hasHighways()
	{
		// there are highways
		return getDesignConnectivity().getNumHighways() > 0;
	}

	private boolean hasSingleLines()
	{
		return getDesignConnectivity().getSingleLines().hasNext();
	}

	private boolean hasMulticores(boolean overbraid)
	{
		// scan toplevels only because MCs and OBs dont mix
		for (IMulticoreIterator it = getDesignConnectivity().getMulticores(true); it.hasNext(); ) {
			IMulticore mc = it.next();
			if (mc instanceof IOverbraid) {
				if (overbraid) {
					return true;
				}
			}
			else {
				if (!overbraid) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean hasAssemblies()
	{
		return getDesignConnectivity().hasAssemblies();
	}

	protected boolean hasOtherComponents()
	{
		return hasOtherComponents(LogicOtherComponentTypeEnum.GENERIC);
	}

	private boolean hasWireDucts()
	{
		return hasOtherComponents(LogicOtherComponentTypeEnum.DUCT);
	}

	private boolean hasMountingRails()
	{
		return hasOtherComponents(LogicOtherComponentTypeEnum.RAIL);
	}

	protected boolean hasBlockDevices()
	{
		return getDesignConnectivity().getNumBlockDevices() > 0;
	}

	protected boolean hasFunctionComponents()
	{
		return getDesignConnectivity().getNumFunctions() > 0;
	}

	protected boolean hasFunctionConductors()
	{
		return getDesignConnectivity().getFunctionConductors().stream().filter(sig -> !sig.isAssociatedMessageSignal())
				.count() > 0;
	}

	protected boolean hasFunctionMessages()
	{
		return getDesignConnectivity().getNumFunctionMessages() > 0;
	}

	@Nullable public IUIDObject getParentFolder(IUID uid)
	{
		IUIDObject obj = getObject(uid);
		if (m_folderObjectMap.keySet().contains(obj)) {
			return getObject(getRoot());
		}
		LogicFolder folder = getLogicFolderForObject(obj);
		if (folder != null) {
			if (obj instanceof IConnector && !(obj instanceof IGenericInlineConnector)) {
				if (((IConnector) obj).isRingTerminal()) {
					folder = LogicFolder.RING_TERMINAL;
				}
				else {
					folder = LogicFolder.CONNECTOR;
				}
			}
			else if (obj instanceof IConductor && ((IConductor) obj).getRootMulticore() != null) {
				folder = ((IConductor) obj).getRootMulticore() instanceof IOverbraid ? LogicFolder.OVERBRAID :
						LogicFolder.MULTICORE;
			}
			return getObjectFolder(folder);
		}
		return null;
	}

	@Nullable protected IUIDObject getObjectFolder(LogicFolder folder, Collection<? extends IUIDObject> lookUpSet)
	{
		if (folder != null) {
			for (IUIDObject uidObject : lookUpSet) {
				if (folder.equals(m_folderObjectMap.get(uidObject))) {
					return uidObject;
				}
			}
		}
		return null;
	}

	@Nullable protected IUIDObject getObjectFolder(LogicFolder folder)
	{
		return getObjectFolder(folder, m_folderObjectMap.keySet());
	}

	public int newObjectExpansion(IUID newuid)
	{
		if (autoExpandNewObject) {
			// Check if first class object, in which case also select
			if (getParentFolder(newuid) != null) {
				return EXPAND_COMPLETELY_AND_SELECT_SELF;
			}
			return EXPAND_COMPLETELY;
		}
		return EXPAND_NONE;
	}

	@Nullable public List<IUID> getPathFromRoot(IUID uid)
	{
		// Is there a direct parent folder?
		IUIDObject parentFolder = getParentFolder(uid);

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

	public void setAutoExpandOnCreation(boolean enabled)
	{
		autoExpandNewObject = enabled;
	}

	protected void obtainSkippedFolders()
	{
		m_skippedFolders.add(LogicFolder.MOUNTINGRAIL.getDisplayName());
		m_skippedFolders.add(LogicFolder.WIREDUCT.getDisplayName());
		m_skippedFolders.add(LogicFolder.OTHERCOMPONENT.getDisplayName());
	}

	protected DesignObjectHierarchyHelper getDesignObjectHierarchyHelper()
	{
		return m_designObjectHierarchyHelper;
	}

	@Override @Nullable protected List<IUID> getChildrenOfAssemblyMember(@Nullable IUID assemblyChild)
	{
		IUIDObject object = getObject(assemblyChild);
		List<IUID> inputList = getChildren(assemblyChild);
		// creddy: This is confusing. represented objects are added and then they are removed as well.
		// creddy: Unnecessarily loading diagrams for nothing?
		if (object instanceof IPinList) {
			addSchemPinListChildren(inputList);
			removeRepresentedObjects(inputList);
		}
		else if (object instanceof IConductor) {
			removeRepresentedObjects(inputList);
		}

		return inputList;
	}

	protected void addSchemPinListChildren(List<IUID> inputList)
	{
		if (inputList != null) {
			List<IUID> schemPinListChildren = new LinkedList<IUID>();
			for (IUID uid : inputList) {
				IUIDObject object = getObject(uid);
				if (object != null && chs.cof.logical.schem.IPinList.class.isAssignableFrom(object.getClass())) {
					List<IUID> list = getChildren((chs.cof.logical.schem.IPinList) object);
					schemPinListChildren.addAll(list);
				}
			}
			inputList.addAll(schemPinListChildren);
		}
	}

	protected void removeRepresentedObjects(Collection<IUID> collection)
	{
		if (collection != null) {
			Collection<IUID> toRemove = new LinkedList<IUID>();
			for (IUID uid : collection) {
				IUIDObject object = getObject(uid);
				if (IRepresentedObject.class.isAssignableFrom(object.getClass())) {
					toRemove.add(uid);
				}
			}
			collection.removeAll(toRemove);
		}
	}

	@Nullable public String getPresentationName(IUID uid, IUID parentUid)
	{
		// If pin is to be shown under a node other than pinlist node,
		// pin presentation name should be prefixed with its parent presentation name(dts0100787475)
		IUIDObject object = UIDMgr.getNonDeletedObject(uid);
		IUIDObject objAtParentNode = DesignUtils.getNonDeletedObject(parentUid);
		if ((object instanceof IPin || object instanceof IAbstractPin) && isConductorOrHighway(objAtParentNode)) {
			return getPinPresentationName(uid, object);
		}
		if(isSingleLineParent(parentUid)){
			//this is required to avoid displaying further details for Single Line's child
			//e.g. device's pin name for the device entry under Single Line
			return super.doGetPresentationName(uid);
		}
		final String presentationName = doGetPresentationName(uid);
		if (object instanceof ILogicObject) {
			ISharedObject sharedObject = ((ILogicObject) object).getSharedObject();
			if (sharedObject != null && !sharedObject.isAccesible()) {
				StringBuilder buf = new StringBuilder();
				buf.append(HTMLHelper.getHTMLHeader());
				buf.append(presentationName);
				buf.append(HTMLHelper.italic(StringUtils.ensureNotNull(HTMLHelper.quoteChars(NO_ACCESS))));
				buf.append(HTMLHelper.getHTMLTrailer());
				return buf.toString();
			}
		}
		return presentationName;
	}

	@Override protected boolean isDimmedTreeNode(@NotNull IBrowserTreeNode node)
	{
		TreeNode parentNode = node.getParent();
		IUID parentNodeId = null;
		if (parentNode instanceof IBrowserTreeNode) {
			parentNodeId = (((IBrowserTreeNode) parentNode).getUID());
		}
		if (parentNodeId != null) {
			IUIDObject objAtParentNode = DesignUtils.getNonDeletedObject(parentNodeId);
			if (objAtParentNode != null) {
				IAbstractPin abstractPin = CommonUtils.cast(node.getUIDObject(), IAbstractPin.class);
				if (abstractPin != null && objAtParentNode instanceof IConductor) {
					return true;
				}
			}
		}
		return super.isDimmedTreeNode(node);
	}

	private boolean isConductorOrHighway(IUIDObject objAtParentNode)
	{
		return objAtParentNode instanceof IConductor || objAtParentNode instanceof chs.cof.logical.schem.IConductor ||
				objAtParentNode instanceof IHighway || objAtParentNode instanceof IHighwaySchematic;
	}

	protected String getPinPresentationName(IUID uid, IUIDObject object)
	{
		IAbstractPin cablePin = object instanceof IPin ? ((IPin) object).getConnectivity() : (IAbstractPin) object;
		String pinName = doGetPresentationName(uid);
		IUIDObject parentObj = cablePin != null ? cablePin.getOwner() : null;
		if (parentObj == null) {
			return pinName;
		}
		if (isPositionedObject(parentObj)) {
			return ModularConnectorDisplayHelper.generatePresentationPinNameForModularConnector(
					(IInternalPositionedObject) parentObj, pinName);
		}
		String parentName = doGetPresentationName(parentObj.getUID());
		return parentName != null ? parentName + ":" + pinName : pinName;
	}

	/**
	 * Create unplaced folder using the <code>folderNameResourceKey</code> for name, provided
	 * <code>isCreateUnusedFolder</code> is true and <code>allUsed</code> is false.
	 *
	 * @param folderNameResourceKey name of the folder
	 * @param uids                  adds the uid of the folder created/existing one to the list
	 * @param isCreateUnusedFolder  true to create folder
	 * @param allUsed               flag indicating if all the objects has got representation
	 */
	private void createUnplacedFolder(@NotNull String folderNameResourceKey, @NotNull List<IUID> uids,
			boolean isCreateUnusedFolder, boolean allUsed)
	{
		if (isCreateUnusedFolder && !allUsed) {
			BrowserFolder unplaced = m_unplacedFolderMap.get(folderNameResourceKey.trim());
			if (unplaced == null) {
				unplaced = createFolder(ResourceMgr.getString(BrowserClient.class, folderNameResourceKey));
				m_unplacedFolderMap.put(folderNameResourceKey, unplaced);
			}
			uids.add(unplaced.getUID());
		}
	}

	/**
	 * Default implementation just returns object from UIDmgr
	 */
	public IUIDObject getObject(IUID uid)
	{
		IUIDObject uidObj = super.getObject(uid);
		if (uidObj != null) {
			return uidObj;
		}
		uidObj = getObjectFromLibrary(uid);
		return uidObj;
	}

	@Nullable protected IUIDObject getObjectFromLibrary(IUID uid)
	{
		IUIDObject uidObj;
		uidObj = LibraryHelper.getLibraryInnerCore(uid);
		return uidObj;
	}

	@NotNull @Override public TreeCellRenderer createTreeCellRenderer()
	{
		TreeCellRenderer treeCellRenderer = super.createTreeCellRenderer();
		((BrowserTreeHelperCellRenderer) treeCellRenderer).setTreeNodeDimmer(treeNodeDimmer);
		return treeCellRenderer;
	}
}
