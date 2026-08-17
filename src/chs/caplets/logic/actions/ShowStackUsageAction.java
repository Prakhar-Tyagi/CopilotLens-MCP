/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2011-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ViewActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.IDesignSharedUsageManagerLifecycle;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinUsage;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.logical.shared.SharedObjectsFinderInDesigns;
import chs.cof.logical.shared.SharedUsageDesignsIdentifier;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IReadOnlyNamedObject;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.logic.DesignSharedObjectHelper;
import chs.utility.logic.LogicDesignUsageSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA. User: creddy Date: May 18, 2011 Time: 4:35:24 PM To change this template use File |
 * Settings | File Templates.
 */
public class ShowStackUsageAction extends ViewActionRT implements ICtxMenuProvider
{

	private static Cursor m_cursor = CAFUtils.getInstance().loadCursor(Cursor.WAIT_CURSOR);
	@Nullable private IDiagramObject m_stackedObject = null;
	@Nullable private IProject m_project = null;
	@Nullable private ILogicDesign m_design = null;

	private List<ShowStackUsageDialog.StackedObjectUsageData> m_stackedUsageData;
	private List<ShowStackUsageDialog.StackedObjectUsageData> m_connectedBlockData;
	private Set<IDesign> m_explicitlyLoadedDesigns;

	public static final Comparator<ShowStackUsageDialog.StackedObjectUsageData> dataComparator =
			new Comparator<ShowStackUsageDialog.StackedObjectUsageData>()
			{
				public int compare(ShowStackUsageDialog.StackedObjectUsageData o1,
						ShowStackUsageDialog.StackedObjectUsageData o2)
				{
					int sortOrder1 = o1.getSortOrder();
					int sortOrder2 = o2.getSortOrder();

					if (sortOrder1 > 0 && sortOrder2 > 0) {
						return sortOrder1 - sortOrder2;
					}
					IUIDObject obj1 = o1.getSourceObject();
					IUIDObject obj2 = o2.getSourceObject();
					if (obj1 instanceof ILogicObject && obj2 instanceof ILogicObject) {
						return ((IReadOnlyNamedObject) obj1).getName()
								.compareToIgnoreCase(((IReadOnlyNamedObject) obj2).getName());
					}
					else if (obj1 instanceof ISharedObject && obj2 instanceof ISharedObject) {
						return ((IReadOnlyNamedObject) obj1).getName()
								.compareToIgnoreCase(((IReadOnlyNamedObject) obj2).getName());
					}
					else {
						return obj1.getUID().compareTo(obj1.getUID());
					}
				}
			};
	@NotNull private Set<ILogicDesign> m_designsForSharedUsages = Collections.emptySet();

	public ShowStackUsageAction(ICapletView view)
	{
		super(view);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_stackedObject = getOperand(getController().getSelectMgr().getPreSelections());

		ILogicObject logicObject = getLogicObject(m_stackedObject);
		if (logicObject != null) {
			m_design = logicObject.getLogicDesign();
			if (m_design != null) {
				m_project = m_design.getProject();
			}
		}

		if (m_project == null || m_design == null) {
			return IActionEnum.eCompleted;
		}

		if (m_stackedObject != null) {
			showUsages();
		}
		m_stackedUsageData.clear();
		m_connectedBlockData.clear();
		m_explicitlyLoadedDesigns.clear();
		return IActionEnum.eCompleted;
	}

	@Nullable
	private ILogicObject getLogicObject(@Nullable IDiagramObject stackedObject)
	{
		IHighwaySchematic highwaySchematic = getGeneralHighwaySchematic(stackedObject);
		if (highwaySchematic != null) {
			return highwaySchematic.getConnectivity();
		}
		if (stackedObject instanceof ISchemStackPin stackPin) {
			IPinList pinListSchem = (IPinList) stackPin.getParent();
			if (pinListSchem != null) {
				return pinListSchem.getConnectivity();
			}
		}
		return null;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		return successful;
	}

	@Nullable
	private IDiagramObject getOperand(SelectSet selectSet)
	{
		int schemStackPinCount = 0;
		int highwayCount = 0;
		IDiagramObject operand = null;

		for (SelectedUIDObjectIterator iter = selectSet.getSelectedUIDObjects(); iter.hasNext(); ) {
			IHighwaySchematic highwaySchematic = null;
			IUIDObject obj = iter.getNext();
			if (obj instanceof ISchemStackPin stackPin) {
				if (stackPin.getNumPins() < 1) { // Skip empty stack pins
					continue;
				}
				IPinList pinlist = (IPinList) stackPin.getParent();
				// All bets are off if the pinlist is not on the active diagram
				if (DiagramHelper.getDiagram(pinlist) == CAFUtils.getInstance().getActiveDiagram()) {
					operand = stackPin;
					schemStackPinCount++;
				}
			}
			else if ((highwaySchematic = getGeneralHighwaySchematic(obj)) != null) {
				// All bets are off if the pinlist is not on the active diagram
				if (DiagramHelper.getDiagram(highwaySchematic) == CAFUtils.getInstance().getActiveDiagram()) {
					if (highwaySchematic != operand) {
						highwayCount++;
					}
					operand = highwaySchematic;
				}
			}
		}

		if (schemStackPinCount == 1 || highwayCount == 1) {
			return operand;
		}
		else {
			return null;
		}
	}

	@Override public boolean isEnabled()
	{
		return hasOperands(getController().getSelectMgr().getPreSelections()) &&
				getController().getCapletModel().isEditable() && super.isEnabled();
	}

	protected boolean hasOperands(SelectSet sset)
	{
		return (getOperand(sset) != null);
	}

	private void showUsages()
	{
		m_stackedUsageData = new SortedList<ShowStackUsageDialog.StackedObjectUsageData>(dataComparator);
		m_connectedBlockData = new SortedList<ShowStackUsageDialog.StackedObjectUsageData>(dataComparator);
		m_explicitlyLoadedDesigns = new HashSet<IDesign>();
		if (!getUsageData()) {
			return;
		}

		for (IDesign unLoadDesign : m_explicitlyLoadedDesigns) {
			unLoadDesign.unloadFromMemory();
		}
		m_explicitlyLoadedDesigns.clear();
		showDialog();
	}

	protected List<ShowStackUsageDialog.StackedObjectUsageData> getStackedUsages()
	{
		CollectionUtils.add(m_connectedBlockData, m_stackedUsageData);
		return m_stackedUsageData;
	}

	protected void showDialog()
	{
		ShowStackUsageDialog dlg = new ShowStackUsageDialog(CAFUtils.getInstance().getDialogFrame(),
				m_stackedUsageData, m_stackedObject, m_connectedBlockData);
		dlg.showData();
	}

	private boolean getUsageData()
	{
		ensureSharedUsagesAreLoadedAndUptodate();
		if (m_stackedObject instanceof ISchemStackPin) {
			return getUsageDataforStackPin((ISchemStackPin) m_stackedObject);
		}

		IHighwaySchematic highwaySchematic = null;
		if ((highwaySchematic = getGeneralHighwaySchematic(m_stackedObject)) != null) {
			ensureSharedUsagesForAssociatedDesigns(highwaySchematic);
			getUsageDataforStackHighway(highwaySchematic);
			getConnectedBlockUsageDataforHighway(highwaySchematic);
			return true;
		}
		return false;
	}

	private void ensureSharedUsagesForAssociatedDesigns(@NotNull IHighwaySchematic highwaySchematic)
	{
		Set<ILogicDesign> associatedDesigns = getAssociatedDesigns(highwaySchematic);
		IDesignSharedUsageManagerLifecycle iDesignSharedUsageManagerLifecycle =
				Objects.requireNonNull(m_project).designSharedUsageManagerLifecycle();
		if (!associatedDesigns.isEmpty()) {
			iDesignSharedUsageManagerLifecycle.loadSharedUsageManagers(associatedDesigns,
					IDesignSharedUsageManagerLifecycle.LoadPolicy.LOAD_AND_REFRESH_BOTH);
		}
	}

	@NotNull private Set<ILogicDesign> getAssociatedDesigns(@NotNull IHighwaySchematic highwaySchematic)
	{
		Set<IBlockDevice> blockDevices = getBlocksConnectedToHighway(highwaySchematic);
		if (blockDevices == null) {
			return Set.of();
		}
		return blockDevices.stream().map(block -> block.getAssociatedDesign(null))
				.filter(design -> design instanceof ILogicDesign)
				.filter(design -> isPartOfActiveBL(design))
				.map(design -> (ILogicDesign) design)
				.collect(Collectors.toSet());
	}

	private boolean isPartOfActiveBL(@NotNull IDesignContainer design)
	{
		IBuildList activeBuildList = m_project != null ? m_project.getBuildListMgr().getActiveBuildList() : null;
		if (activeBuildList != null) {
			boolean isPartOfActiveBL =
					m_design != null && !activeBuildList.containsDesignUID(m_design.getUID());
			return isPartOfActiveBL || activeBuildList.containsDesignUID(design.getUID());
		}

		return true;
	}

	private void addToUsageList(IUIDObject sourceObj, String sourcePinName, String targetPinName, String conductorName)
	{
		addToUsageList(m_stackedUsageData, sourceObj, sourcePinName, targetPinName, conductorName, 0);
	}

	private void addToUsageList(List<ShowStackUsageDialog.StackedObjectUsageData> usageData,
			IUIDObject sourceObj,
			String sourcePinName,
			String targetPinName, String conductorName, Integer sortOrder)
	{
		ShowStackUsageDialog.StackedObjectUsageData usage =
				new ShowStackUsageDialog.StackedObjectUsageData(sourceObj, sourcePinName,
						targetPinName, conductorName, sortOrder == null ? 0 : sortOrder);
		usageData.add(usage);
	}

	private void getConnectedBlockUsageDataforHighway(@NotNull IHighwaySchematic highwaySchematic)
	{
		Set<IBlockDevice> blocksConnectedToHWs = getBlocksConnectedToHighway(highwaySchematic);
		if (blocksConnectedToHWs == null) {
            return;
        }

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		SetMap<ISharedConductor, IUID> conductorsToConnectedBlocksMap = new SetMap<ISharedConductor, IUID>();
		for (IBlockDevice block : blocksConnectedToHWs) {
			IDesignContainer associatedDesign = block.getAssociatedDesign(null);
			if (associatedDesign instanceof ILogicDesign) {
				Iterator<ISharedConductor> sharedConds =
						DesignSharedObjectHelper.getSharedConductors((ILogicDesign) associatedDesign, false, false);
				while (sharedConds.hasNext()) {
					final ISharedConductor sharedConductor = sharedConds.next();
					Set<IUID> blockDevices = conductorsToConnectedBlocksMap.getSet(sharedConductor);
					blockDevices.add(block.getUID());
				}
			}
		}

		if (!conductorsToConnectedBlocksMap.isEmpty()) {
			for (Map.Entry<ISharedConductor, Set<IUID>> blockConductorSetEntry : conductorsToConnectedBlocksMap
					.entrySet()) {
				final Set<IUID> connectedBlocks = blockConductorSetEntry.getValue();
				if (connectedBlocks.size() > 1) {
					String blockNames = getConcatenatedObjNames(connectedBlocks);
					final ISharedConductor sharedConductor = blockConductorSetEntry.getKey();
					addToUsageList(m_connectedBlockData, sharedConductor, blockNames, "", sharedConductor.getName(),
							0);
				}
			}
		}
	}

	@Nullable private Set<IBlockDevice> getBlocksConnectedToHighway(@NotNull IHighwaySchematic highwaySchematic)
	{
		final IHighway highway = highwaySchematic.getConnectivity();

		IConnectivity designConnectivity = null;
		if (m_design != null) {
			designConnectivity = m_design.getConnectivity();
		}

		if (designConnectivity == null) {
			return null;
		}

        Set<IBlockDevice> blocksConnectedToHWs = new HashSet<IBlockDevice>();
        for (IBlockDevice blockDevice : designConnectivity.getBlockDevices()) {
			if (blockDevice.getInterfacedHighways().contains(highway)) {
				blocksConnectedToHWs.add(blockDevice);
			}
		}
		return blocksConnectedToHWs;
	}

	private boolean getUsageDataforStackHighway(@NotNull IHighwaySchematic stackHighway)
	{
		IGeneralHighway highway = HighwayHelper.toGeneralHighway(stackHighway);
		if (highway == null || m_project == null) {
			return false;
		}
		Collection<ILogicDesign> designsForSharedUsages = gatherDesigns();
		Set<IUID> designScope = new HashSet<IUID>();

		Set<IUID> condSet = new HashSet<IUID>();

		IProjectSharedUsageView projShareUsageView = m_project.getSharedUsageView();

		// 1. Get all stacked conductors + interfaced conductors
		getConductorsFromHWInstance(highway, condSet);

		// 2. Get connected stack pins and get the wires connected to them on all other designs
		getConductorsOnConnectedStackPins(stackHighway, designsForSharedUsages, designScope, condSet,
				projShareUsageView);

		for (IUID uid : condSet) {
			IUIDObject uidObj = UIDMgr.getObject(uid);
			Set<IUID> pinsConnectedToCond = new HashSet<IUID>();
			String condName = "";
			boolean bValidConductor = false;
			if (uidObj instanceof IConductor cond) {
				bValidConductor = true;
				condName = cond.getName();
				getConductorTerminations(pinsConnectedToCond, cond);
			}
			else if (uidObj instanceof ISharedConductor sharedCond) {
				bValidConductor = true;
				condName = sharedCond.getName();
				collectDesignScope(designsForSharedUsages, designScope);
				Collection<ISharedUsage> sharedCondUsages =
						projShareUsageView.getUsagesOf(ISharedUsage.class, sharedCond, designScope);
				for (ISharedUsage condUsage : sharedCondUsages) {
					IDesignContainer dc = m_project.getDesignMgr().getDesign(condUsage.getDesignUID());
					if (dc instanceof ILogicDesign) {
						loadDesignIfNotLoaded(dc);
						ILogicObject logObj = condUsage.getLogicObject();
						if (logObj instanceof IConductor cond) {
							getConductorTerminations(pinsConnectedToCond, cond);
						}
					}
				}
			}

			if (bValidConductor) {
				String pinNames = getConcatenatedObjNames(pinsConnectedToCond);
				addToUsageList(uidObj, pinNames, "", condName);
			}
		}
		return true;
	}

	private void loadDesignIfNotLoaded(IDesignContainer dc)
	{
		ILogicDesign iDesign = (ILogicDesign) dc;
		if (!iDesign.isLoadedInMemory()) {
			iDesign.loadToMemory();
			m_explicitlyLoadedDesigns.add(iDesign);
		}
	}

	private void getConductorsOnConnectedStackPins(IHighwaySchematic stackHighway,
			Collection<ILogicDesign> designsForSharedUsages, Set<IUID> designScope, Set<IUID> condSet,
			IProjectSharedUsageView projShareUsageView)
	{
		for (IUID stackPinID : stackHighway.getConnectedStackPins()) {
			ISchemStackPin pinStack = UIDMgr.getObjectOfType(stackPinID, ISchemStackPin.class);
			if (pinStack != null) {
				for (IAbstractPin pin : pinStack.getAllConnectivity()) {
					ISharedPin sharedPin = pin.getSharedPin();
					if (sharedPin == null) {
						for (IConductor cond : pin.getConductorsAsSet()) {
							addToConductorSet(condSet, cond);
						}
					}
					else {
						collectDesignScope(designsForSharedUsages, designScope);
						Collection<ISharedPinUsage> sharedPinUsages =
								projShareUsageView.getUsagesOf(ISharedPinUsage.class, sharedPin, designScope);
						for (ISharedPinUsage pinUsage : sharedPinUsages) {
							IDesignContainer dc =
									m_project != null ? m_project.getDesignMgr().getDesign(pinUsage.getDesignUID()) :
											null;
							if (dc instanceof ILogicDesign) {
								loadDesignIfNotLoaded(dc);
								ILogicObject logObj = pinUsage.getLogicObject();
								if (logObj instanceof IAbstractPin otherPin) {
									for (IConductor cond : otherPin.getConductorsAsSet()) {
										addToConductorSet(condSet, cond);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void getConductorsFromHWInstance(IGeneralHighway highway, Set<IUID> condSet)
	{
		for (IConductor cond : highway.getAllConductors()) {
			addToConductorSet(condSet, cond);
		}
	}

	private void getConductorTerminations(Set<IUID> pinsConnectedToCond, IConductor cond)
	{
		getTargetsForShield(pinsConnectedToCond, cond);

		for (IAbstractPin pin : cond.getAllPins()) {
			addToPinSet(pinsConnectedToCond, pin);
		}
	}

	private void getTargetsForShield(Set<IUID> pinsConnectedToCond, IConductor cond)
	{
		if (cond instanceof IShieldConductor) {
			IMulticore mc = cond.getMulticore();
			if (mc != null) {
				pinsConnectedToCond.add(mc.getUID());
			}
		}
	}

	private String getConcatenatedObjNames(Set<IUID> pinsConnectedToCond)
	{
		SortedList<String> sortedNamesList = new SortedList<String>(String.CASE_INSENSITIVE_ORDER);
		for (IUID targetUID : pinsConnectedToCond) {
			IUIDObject targetObj = UIDMgr.getObject(targetUID);
			String formattedName = null;
			if (targetObj instanceof IAbstractPin targetPin) {
				chs.cof.logical.cable.IPinList targetPL = targetPin.getOwner();
				formattedName = getFormattedPinName(targetPin, targetPL);
			}
			else if (targetObj instanceof ISharedPin targetPin) {
				ISharedPinList targetPL = targetPin.getOwner();
				formattedName = getFormattedPinName(targetPin, targetPL);
			}
			else if (targetObj instanceof IMulticore) {
				formattedName = ((IAbstractMulticore) targetObj).getName();
			}
			else if (targetObj instanceof IBlockDevice) {
				formattedName = ((IReadOnlyNamedObject) targetObj).getName();
			}

			if (!StringUtils.isBlank(formattedName)) {
				sortedNamesList.addSorted(formattedName);
			}
		}

		return StringUtils.convertCollectionToString(sortedNamesList, StringUtils.COMMA_SPACE);
	}

	private String getFormattedPinName(IReadOnlyNamedObject namedObject, IReadOnlyNamedObject preFixer)
	{
		String devName = preFixer.getName();
		String pinName = namedObject.getName();
		StringBuilder formattedName = new StringBuilder();
		formattedName.append(devName);
		formattedName.append(":");
		formattedName.append(pinName);
		return formattedName.toString();
	}

	private void addToPinSet(Set<IUID> pinsConnectedToCond, IAbstractPin pin)
	{
		ISharedPin sharedPin = pin.getSharedPin();
		if (sharedPin == null) {
			pinsConnectedToCond.add(pin.getUID());
		}
		else {
			pinsConnectedToCond.add(sharedPin.getUID());
		}
	}

	private void addToConductorSet(Set<IUID> condSet, IConductor cond)
	{
		ISharedConductor sharedCond = cond.getSharedConductor();
		if (sharedCond != null) {
			condSet.add(sharedCond.getUID());
		}
		else {
			condSet.add(cond.getUID());
		}
	}

	private boolean getUsageDataforStackPin(ISchemStackPin stackPin)
	{
		IPinList pl = (IPinList) stackPin.getParent();
		Map<String, Integer> cavityToSortOrder = new HashMap<String, Integer>();
		if (pl != null) {
			chs.cof.logical.cable.IPinList cablePL = pl.getConnectivity();
			if (cablePL != null) {
				ILibraryBaseObject libObj = cablePL.getLibraryObject();
				if (libObj != null) {
					for (ILibraryCavity cavity : ((ILibraryCavityContainer) libObj).getCavities()) {
						cavityToSortOrder.put(cavity.getName(), cavity.getSortOrder());
					}
				}
			}
		}

		Collection<ILogicDesign> designsForSharedUsages = gatherDesigns();
		Set<IUID> designScope = new HashSet<IUID>();

		IProjectSharedUsageView projShareUsageView = Objects.requireNonNull(m_project).getSharedUsageView();
		for (IAbstractPin sourcePin : stackPin.getAllConnectivity()) {
			ISharedPin sharedPin = sourcePin.getSharedPin();
			Set<IAbstractPin> pinSet = new HashSet<IAbstractPin>();
			IUID sourcePinID = sourcePin.getUID();
			if (sharedPin == null) {
				pinSet.add(sourcePin);
			}
			else {
				sourcePinID = sharedPin.getUID();
				getAllInstancesOfSharedPin(designsForSharedUsages, designScope, projShareUsageView, sharedPin, pinSet);
			}
			Set<IConductor> condSet =
					getConnectedConductors(designsForSharedUsages, designScope, projShareUsageView, pinSet);

			Map<IUID, Set<IUID>> condToTargetPinsMap = getConductorToPinMap(sourcePinID, condSet);

			for (Map.Entry<IUID, Set<IUID>> entry : condToTargetPinsMap.entrySet()) {
				Set<IUID> targetPins = entry.getValue();
				String targetObjNames = getConcatenatedObjNames(targetPins);

				IUID condID = entry.getKey();
				IUIDObject condObj = UIDMgr.getObject(condID);
				String condName = "";
				if (condObj instanceof ISharedConductor) {
					condName = ((IRevisionedObject) condObj).getName();
				}
				else if (condObj instanceof IConductor) {
					condName = ((IReadOnlyNamedObject) condObj).getName();
				}

				addToUsageList(m_stackedUsageData, sourcePin, sourcePin.getName(), targetObjNames, condName,
						cavityToSortOrder.get(sourcePin.getName()));
			}
			if (condToTargetPinsMap.isEmpty()) {
				addToUsageList(m_stackedUsageData, sourcePin, sourcePin.getName(), "", "",
						cavityToSortOrder.get(sourcePin.getName()));
			}
			condToTargetPinsMap.clear();
		}
		return true;
	}

	private Map<IUID, Set<IUID>> getConductorToPinMap(IUID sourcePinID, Set<IConductor> condSet)
	{
		Map<IUID, Set<IUID>> condToTargetPinsMap = new HashMap<IUID, Set<IUID>>();
		for (IConductor usedCond : condSet) {
			ISharedConductor sharedCond = usedCond.getSharedConductor();
			IUID condID = usedCond.getUID();
			if (sharedCond != null) {
				condID = sharedCond.getUID();
			}
			Set<IUID> targetPins = new HashSet<IUID>();

			getConductorTerminations(targetPins, usedCond);
			targetPins.remove(sourcePinID);
			Set<IUID> existingTargetPins = condToTargetPinsMap.get(condID);
			if (existingTargetPins == null) {
				condToTargetPinsMap.put(condID, targetPins);
			}
			else {
				existingTargetPins.addAll(targetPins);
			}
		}
		return condToTargetPinsMap;
	}

	private Set<IConductor> getConnectedConductors(Collection<ILogicDesign> designsForSharedUsages,
			Set<IUID> designScope, IProjectSharedUsageView projShareUsageView, Set<IAbstractPin> pinSet)
	{
		Set<IConductor> condSet = new HashSet<IConductor>();
		for (IAbstractPin pin : pinSet) {
			for (IConductor cond : pin.getConductorsAsSet()) {
				ISharedConductor sharedCond = cond.getSharedConductor();
				if (sharedCond == null) {
					condSet.add(cond);
				}
				else {
					collectDesignScope(designsForSharedUsages, designScope);
					Collection<ISharedUsage> sharedCondUsages =
							projShareUsageView.getUsagesOf(ISharedUsage.class, sharedCond, designScope);
					for (ISharedUsage condUsage : sharedCondUsages) {
						IDesignContainer dc =
								m_project != null ? m_project.getDesignMgr().getDesign(condUsage.getDesignUID()) : null;
						if (dc instanceof ILogicDesign) {
							loadDesignIfNotLoaded(dc);
							ILogicObject logObj = condUsage.getLogicObject();
							if (logObj instanceof IConductor) {
								condSet.add((IConductor) logObj);
							}
						}
					}
				}
			}
		}
		return condSet;
	}

	private void getAllInstancesOfSharedPin(Collection<ILogicDesign> designsForSharedUsages, Set<IUID> designScope,
			IProjectSharedUsageView projShareUsageView, ISharedPin sharedPin, Set<IAbstractPin> pinSet)
	{
		collectDesignScope(designsForSharedUsages, designScope);
		Collection<ISharedPinUsage> sharedPinUsages =
				projShareUsageView.getUsagesOf(ISharedPinUsage.class, sharedPin, designScope);
		for (ISharedPinUsage pinUsage : sharedPinUsages) {
			IDesignContainer dc =
					m_project != null ? m_project.getDesignMgr().getDesign(pinUsage.getDesignUID()) : null;
			if (dc != null && dc instanceof ILogicDesign) {
				loadDesignIfNotLoaded(dc);
				ILogicObject logObj = pinUsage.getLogicObject();
				if (logObj instanceof IAbstractPin) {
					pinSet.add((IAbstractPin) logObj);
				}
			}
		}
	}

	private void collectDesignScope(Collection<ILogicDesign> designsForSharedUsages, Set<IUID> designScope)
	{
		for (ILogicDesign design : designsForSharedUsages) {
			designScope.add(design.getUID());
		}
	}

	private void ensureSharedUsagesAreLoadedAndUptodate()
	{
		Set<ISharedObject> projectSharedObjects = new HashSet<>();
		SharedObjectsFinderInDesigns.gatherUsedSharedObjects(Collections.singletonList(m_design), projectSharedObjects);
		m_designsForSharedUsages =
				SharedUsageDesignsIdentifier.determineDesignsWithDSUMLoad(Objects.requireNonNull(m_project), projectSharedObjects,
						LogicDesignUsageSearchScope.SCOPE_PREFERENCE.DESIGNS_FOR_XREF, m_design);
	}

	@NotNull private Collection<ILogicDesign> gatherDesigns()
	{
		// if there is an active build list return all designs in the build list, otherwise return all designs
		List<ILogicDesign> designs = new ArrayList<ILogicDesign>();
		IBuildList activeBuildList = Objects.requireNonNull(m_project).getBuildListMgr().getActiveBuildList();
		if (activeBuildList == null || !activeBuildList.containsDesignUID(Objects.requireNonNull(m_design).getUID())) {
			designs.addAll(m_designsForSharedUsages);
		}
		else {
			for (IDesignDescriptor designDescriptor : activeBuildList.getDesignDescriptors()) {
				ILogicDesign loadedDesignContainer =
						CommonUtils.cast(designDescriptor.getLoadedDesignContainer(), ILogicDesign.class);
				if (loadedDesignContainer != null && m_designsForSharedUsages.contains(loadedDesignContainer)) {
					designs.add(loadedDesignContainer);
				}
			}
		}
		return designs;
	}

	@Override public String getActionUIClass()
	{
		return ShowStackUsageActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (hasOperands(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override public Cursor getCursor()
	{
		return m_cursor;
	}

	@Nullable private static <T> IHighwaySchematic getGeneralHighwaySchematic(@Nullable T object)
	{
		if (object instanceof IHighwaySchematic highwaySchematic &&
				HighwayHelper.isGeneralHighwaySchematic(highwaySchematic)) {
			return highwaySchematic;
		}
		else if (object instanceof IHighwaySegment highwaySegment &&
				HighwayHelper.isGeneralHighwaySegment(highwaySegment)) {
			return highwaySegment.getHighway();
		}

		return null;
	}
}
