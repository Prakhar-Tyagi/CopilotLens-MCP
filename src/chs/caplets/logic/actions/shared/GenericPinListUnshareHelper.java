/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2013-2025 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.analysis.IDesignPort;
import chs.analysis.IPort;
import chs.analysis.IVHDLFailureDataMapping;
import chs.analysis.IVHDLModelMapping;
import chs.analysis.sv.model.mapping.DesignPort;
import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.ICrossReferenceable;
import chs.cof.drawplus.IXRefTextContainer;
import chs.cof.library.IFootprintable;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBlock;
import chs.cof.logical.cable.ICavitiesOwner;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListIterator;
import chs.cof.project.IProject;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyActionContextForErrorReport;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IAttributePropertyProvider;
import chs.common.IMultiSymbolledPinlist;
import chs.common.IReadOnlyNamedObject;
import chs.common.IReference;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.ctf.ui.form.RenameDialog;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.Environment;
import chs.utilities.ListMap;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.DiagramHelper;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.analysis.AnalysisUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DeviceConnectorDeletionTracker;
import chs.utility.helpers.LanguageDictionaryHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PinListShareHelper;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.helpers.UnsharePinListLockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 29 Apr, 2013 Time: 3:40:23 PM
 */
public class GenericPinListUnshareHelper
{

	protected chs.cof.logical.cable.IPinList cablePinList;
	@Nullable protected IGenericInlineConnector cablePinListMate;
	protected Collection<IPinList> schemPinLists;

	protected ILogicDesign design;
	@Nullable protected ISchemDiagram m_diagram;
	private String m_newName = null;
	private String m_newMateName = null;
	@Nullable private String shortDesc = null;
	@Nullable private String mateShortDesc = null;

	private Map<IAbstractPin, IAbstractPin> matePinMap;

	// information extracted from usages at the start of the action
	protected Map<IUID, ISymbolDef> oldSymDefMap;
	protected Map<IUID, Integer> oldSymInstNumMap;
	protected ListMap<IUID, PinUsageInfo> oldPinUsageInfoMap;

	protected Replicator replicator = null;
	private List<IAbstractPin> alreadyReplicatedPins = null;
	protected SetMap<IPin, IWireConductor> m_InvalidWireConnections;
	protected SetMap<IPin, IPin> m_schemPinConnectionsToDisconnect;

	private PinListConnectionTransferer plConnectionTransfer = null;
	protected Map<IAbstractPin, Set<ISchemStackPin>> pin_stackPinReps_Map;

	public GenericPinListUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		// TODO jacobt FEAT14396 : change signature to pass ILogicDesign
		design = (ILogicDesign) theDesign;
		m_diagram = diagram;
	}

	public boolean initializePinList(@Nullable ISchemDiagram diagram,
			@Nullable chs.cof.logical.cable.IPinList pinList, @Nullable IGenericInlineConnector mate,
			Collection<IPinList> schmPinlists)
	{
		cablePinList = pinList;
		if (cablePinList == null) {
			return false;
		}
		cablePinListMate = mate;
		schemPinLists = schmPinlists;
		plConnectionTransfer = null;

		m_diagram = diagram;
		initializeFields();
		return true;
	}

	private void rebuildOperands()
	{
		List<IPinList> validCandidates = new ArrayList<>(schemPinLists.size());
		for (IPinList schemPinList : schemPinLists) {
			IPinList objectOfType = UIDMgr.getObjectOfType(schemPinList.getUID(), IPinList.class);
			if (objectOfType != null) {
				validCandidates.add(objectOfType);
			}
		}
		schemPinLists.clear();
		schemPinLists.addAll(validCandidates);
	}

	@NotNull public IActionEnum setup(BaseShareActionOperands operands, @Nullable ISchemDiagram diagram)
	{
		getOutputWindow().clearPane("Unshare Action");
		if (!initializePinList(diagram, operands.getCablePinList(), operands.getCablePinListMate(),
				operands.getPinListRepresentations())) {
			return IActionEnum.eCanceled;
		}

		boolean allUnshared = determineToUnshareAll(design.getDesignWideUsageMgr());
		if (!lockEffectedObjects(allUnshared)) {
			return IActionEnum.eCanceled;
		}
		//the operands might have been invalid after refresh of diagrams.
		rebuildOperands();
		if (!promptRenameLocalPinList(cablePinList, cablePinListMate)) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

//	private void displayErrorMessageDiagramLockFailure()
//	{
//		ResourceBasedMessageContent content =
//				new ResourceBasedMessageContent(GenericPinListUnshareHelper.class,
//						"GenericPinListUnshareHelper.diagram.LockFailures");
//		content.setContextParameters(COFTypeEnum.getDisplayableTypeName(cablePinList));
//		content.setImplicationsParameters(cablePinList.getName());
//		Message.show(PromptSeverity.WARNING, content);
//	}

	private boolean lockEffectedObjects(boolean unshareAll)
	{
		UnsharePinListLockHandler lockHandler = new UnsharePinListLockHandler();
		Set<IPinList> schemObjsToLock = new HashSet<>(schemPinLists);
		schemObjsToLock.addAll(getAdditionalSchemObjectsToProcess());
		Set<ILogicObject> lockables = lockHandler.getEffectedObjects(getLockableCableObjects(), schemObjsToLock,
				unshareAll);
		if (!lockObjects(design, lockables, pinlist -> getLockMessagePrefix(pinlist),
				getActionContextForLockErrorReporting())) {
			return false;
		}
		if (!verifyUsageValidityAfterLock(unshareAll)) {
			return false;
		}
		return lockDiagrams(lockHandler, schemObjsToLock);
	}

	private boolean lockDiagrams(UnsharePinListLockHandler lockHandler, Collection<IPinList> schemPinListsT)
	{
		Set<ISchemDiagram> diagramsToLock = lockHandler.getDiagramsToLock(schemPinListsT);
		Set<ISchemDiagram> lockFailures = lockHandler.lockEffectedDiagramsForUnshare(design, diagramsToLock);
		if (!lockFailures.isEmpty()) {
			return false;
		}
		ShareConcurrencyHelper.blockDiagramsFromGettingUnlockedAtEndOfAction(diagramsToLock);
		return true;
	}

	@NotNull private String getLockMessagePrefix(chs.cof.logical.cable.IPinList pinlist)
	{
		String objectsLink = LogicConcurrencyLogger.getInstance()
				.buildObjectsLink(design, Collections.singleton(pinlist.getUID()));
		return ResourceMgr.getString(GenericPinListUnshareHelper.class,
				"GenericPinListUnshareHelper.error.unableToLock", objectsLink);
	}

	protected boolean verifyUsageValidityAfterLock(boolean unshareAll)
	{
		if (unshareAll && !isOkForUnshareAll()) {
			String message = ResourceMgr
					.getString(GenericPinListUnshareHelper.class, "GenericPinListUnshareHelper.error.objectPlaced");
			getOutputWindow()
					.sendApplicationMessage(getLockMessagePrefix(cablePinList) + " - " + message);
			return false;
		}
		return true;
	}

	private IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	protected boolean isOkForUnshareAll()
	{
		return schemPinLists.size() == design.getDesignWideUsageMgr().getDesignSharedUsageCount(cablePinList);
	}

	private boolean lockObjects(ILogicDesign logicDesign, Collection<ILogicObject> objectsToLock,
			Function<chs.cof.logical.cable.IPinList, String> lockMsgPrefix,
			ILogicConcurrencyActionContextForErrorReport context)
	{
		Set<IUID> failedObjects = LogicObjectLockFinder.tryEdit(logicDesign, objectsToLock);
		if (!failedObjects.isEmpty()) {
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(logicDesign, lockMsgPrefix.apply(cablePinList), failedObjects,
							message -> getOutputWindow().sendApplicationMessage(message), true, context);
			return false;
		}
		return true;
	}

	private ILogicConcurrencyActionContextForErrorReport getActionContextForLockErrorReporting()
	{
		return new ILogicConcurrencyActionContextForErrorReport()
		{
			@Override public String getContext()
			{
				return ResourceMgr
						.getString(GenericPinListUnshareHelper.class,
								"GenericPinListUnshareHelper.LockFailures.context",
								COFTypeEnum.getDisplayableTypeName(cablePinList));
			}

			@Override public String getObjectImplications()
			{
				return ResourceMgr
						.getString(GenericPinListUnshareHelper.class,
								"GenericPinListUnshareHelper.LockFailures.implications");
			}
		};
	}

	protected void initializeFields()
	{
		replicator = new Replicator(Replicator.COPY);

		matePinMap = new HashMap<IAbstractPin, IAbstractPin>();
		oldSymDefMap = new HashMap<IUID, ISymbolDef>();
		oldSymInstNumMap = new HashMap<IUID, Integer>();
		oldPinUsageInfoMap = new ListMap<IUID, PinUsageInfo>();
		alreadyReplicatedPins = new ArrayList<IAbstractPin>();
	}

	public boolean doEdit()
	{
		boolean success;
		try {
			DeviceConnectorDeletionTracker.getInstance().startOperation();
			success = unsharePinList();
		}
		finally {
			DeviceConnectorDeletionTracker.getInstance().endOperation();
		}
		return success;
	}

	/**
	 * Unshares the connectivity / schem pinlists specified during the setup of this object.
	 * <p>
	 * Currently limited to either unsharing a single instance of a pinlist, or all instances (could be 0, 1 or many)
	 * <p>
	 * Breaks the links in the connectivity to the shared object if there are no other instances of that shared object
	 * in the design, otherwise creates new connectivity objects and makes the schem point to them.  Handles all the
	 * pins of a pinlist in the same way.
	 *
	 * @return success
	 */
	private boolean unsharePinList()
	{
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		chs.cof.logical.cable.IPinList unsharedPinList = cablePinList;
		chs.cof.logical.cable.IPinList unsharedPinListMate = cablePinListMate;

		ISharedPinList spl = cablePinList.getSharedPinList();
		ISharedConnector splMate = null;
		if (spl instanceof ISharedConnector) {
			splMate = ((ISharedConnector) spl).getMate();
		}
		boolean allUnshared = determineToUnshareAll(dwum);

		buildOldUsagesMap();

		populatePinStackPinInfo();

		Map<IAbstractPin, ISharedPin> connectivityToSharedPin = new HashMap<IAbstractPin, ISharedPin>();
		if (allUnshared) {
			// If this shared pinlist is only used once, then make the connectivity local
			unshareThisPinList();

			// dts0100875226 - FEAT00015778:Unshare action after swapout leading to exception.
			// Unshare the pins *before* calling replicator.replicateCopyableObject - can lead to modifying unlocked
			// Shared Pins as a side effect.
			connectivityToSharedPin.putAll(getConnectivityToSharedPinLink(cablePinList));
			unsharePins();

			// Copy the details from the shared obj to the local obj *after* ensuring all objects in the hierarchy are
			// unshared - we have no locks on shared objects so we must not modify them through side effects
			copyInfoFromSharedToLogicObject(spl);
		}
		else {
			if (schemPinLists.size() > 1) {
				// limitation - we can't handle this case yet
				assert false : "Unshare action should not be enabled for this case";
				return false;
			}
		}
		handleUnplacedLogicObjects(spl, connectivityToSharedPin);

		Set<ISchemDiagram> diagramsAffected = new HashSet<ISchemDiagram>();
		if (m_diagram != null) {
			diagramsAffected.add(m_diagram); //we will always refresh representations on current diagram. why??
		}
		if (!allUnshared && schemPinLists.size() == 1) {
			IPinList schemPinList = schemPinLists.iterator().next();
			IPinList schemMate = (IPinList) SchemPinListHelper.getInlineMateObject(schemPinList);

			UnshareSingleInstance unshareSingleInstance =
					new UnshareSingleInstance(unsharedPinListMate, spl, splMate, schemPinList, schemMate)
							.invoke();
			unsharedPinList = unshareSingleInstance.getUnsharedPinList();
			unsharedPinListMate = unshareSingleInstance.getUnsharedPinListMate();
			if (schemMate != null) {
				schemPinLists.add(schemMate);
			}
		}
		schemPinLists.addAll(getAdditionalSchemObjectsToProcess());
		for (IPinList schemPinList : schemPinLists) {
			diagramsAffected.add(DiagramHelper.getDiagram(schemPinList));

			if (allUnshared) {
				// Properties have already been copied from shared to connectivity object
				// update any prop texts that still refer to the shared prop
				PropertyHelper.updatePropertyTexts(schemPinList, schemPinList.getConnectivity());
				doPinListSpecificProcessing(spl, connectivityToSharedPin, schemPinList);
				transferModelMappingToCablePinList(spl);
			}

			updateInternalConnectivity(schemPinList);
			disconnectPinLists(schemPinList);

			ISchemDiagram diagram = schemPinList.getDiagram();
			if (diagram.isEditable()) {
				removeXrefText(schemPinList);
				regenerateDeviceConnectors(schemPinList);
			}
		}

		//dts0100742133 - rename the pinlist only when the existing cable is unshared
		if (!unsharedPinList.isShared()) {
			// rename the pinlist / mate if alternative connectivity names were specified
			renamePinlist(unsharedPinList, unsharedPinListMate);
		}
		postUnshare(diagramsAffected);

		return true;
	}

	private void postUnshare(@NotNull Set<ISchemDiagram> diagramsAffected)
	{
		//dts0100929150 : ST121BASH37 - Merge into in leading into class cast exception
		//need to representations on all affected diagrams. otherwsie subsequent action may face issues.
		for (ISchemDiagram diagramAffected : diagramsAffected) {
			diagramAffected.refreshRepresentations();
		}
		for (IPinList schemPinList : schemPinLists) {
			disconnectMultiTermedConductors(schemPinList);
		}
	}

	private void disconnectMultiTermedConductors(IPinList schemPinList)
	{
		for (IAbstractPin abstractPin : schemPinList.getCablePins(false)) {
			for (chs.cof.logical.cable.IConductor conductor : abstractPin.getConductors()) {
				if (conductor.hasInvalidMultiTerminations()) {
					ConnectionHelper.disconnect(abstractPin, conductor, true);
				}
			}
		}
	}

	private void regenerateDeviceConnectors(IPinList schemPinList)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(schemPinList);
		if (diagram == null) {
			return;
		}
		// Refresh the graphic
		Generator generator = Generator.getGenerator();
		GeneratorParameters genParams = new GeneratorParameters();
		genParams.setSpacing(diagram.getGrid().getGridSpacing());
		generator.generate(schemPinList, genParams, Generator.NOREGENERATE_PROPERTIES, false);

		if (schemPinList.getConnectivity() instanceof IDevice) {
			generator.rebuildDeviceConnectors(schemPinList, genParams, null);
			generator.regenerateSchemDeviceConnectors(schemPinList, genParams, null);
		}
	}

	protected Collection<? extends IPinList> getAdditionalSchemObjectsToProcess()
	{
		return Collections.EMPTY_SET;
	}

	protected void handleUnplacedLogicObjects(ISharedPinList spl, Map<IAbstractPin, ISharedPin> connectivityToSharedPin)
	{
	}

	protected void updateInternalConnectivity(IPinList schemPinlist)
	{
	}

	protected void doPinListSpecificProcessing(ISharedPinList spl,
			Map<IAbstractPin, ISharedPin> connectivityToSharedPin,
			IPinList schemPinList)
	{
	}

	protected void copyInfoFromSharedToLogicObject(ISharedPinList spl)
	{
		replicator.replicateCopyableObject(spl, cablePinList);

		cablePinList.setOverriddenAnalysisInterfaces(spl.getOverriddenAnalysisInterfaces());
		cablePinList.setOverriddenAnalysisFailureModes(spl.getOverriddenAnalysisFailureModes());
	}

	protected void transferModelMappingToCablePinList(ISharedPinList spl)
	{
		IVHDLModelMapping mapping = spl.getModelMapping();

		if (mapping != null) {
			cablePinList.setModelMapping(getModelMappingToBeAppliedOnUnshared(cablePinList, spl, mapping));
		}
	}

	protected IVHDLModelMapping getModelMappingToBeAppliedOnUnshared(chs.cof.logical.cable.IPinList pinList,
			ISharedPinList spl,
			@NotNull IVHDLModelMapping modelMapping)
	{
		IVHDLModelMapping targetModelMapping = modelMapping.getClone();
		Map<IDesignPort, IDesignPort> sourceDestinationPortMap = new HashMap<>();
		sourceDestinationPortMap.put(DesignPort.UNDEFINED, DesignPort.UNDEFINED);
		for (Map.Entry<IPort, IDesignPort> portMapEntry : modelMapping.getPortMapping().entrySet()) {
			IDesignPort targetDesignPort = getTargetPort(pinList, spl, portMapEntry.getValue());
			sourceDestinationPortMap.put(portMapEntry.getValue(), targetDesignPort);
			targetModelMapping
					.addPortMapping(portMapEntry.getKey(), targetDesignPort);
		}

		Set<IVHDLFailureDataMapping> failureDataMappings = new LinkedHashSet<>();
		for (IVHDLFailureDataMapping failure : targetModelMapping.getFailures()) {
			IDesignPort targetPort = sourceDestinationPortMap.get(failure.getDesignPort());
			if (targetPort != null) {
				failure.setDesignPort(targetPort);
				failureDataMappings.add(failure);
			}
		}
		targetModelMapping.setFailureMapping(failureDataMappings);

		targetModelMapping.setGroundPin(sourceDestinationPortMap.get(targetModelMapping.getGroundPin()));
		targetModelMapping.setPowerPin(sourceDestinationPortMap.get(targetModelMapping.getPowerPin()));
		return targetModelMapping;
	}

	private IDesignPort getTargetPort(chs.cof.logical.cable.IPinList pinlist, ISharedPinList spl, IDesignPort port)
	{
		IDesignPort targetPort = DesignPort.UNDEFINED;

		if (port != DesignPort.UNDEFINED) {
			IReadOnlyNamedObject designObject = (IReadOnlyNamedObject) port.getDesignObject();
			if (designObject != null) {
				IAttributePropertyProvider targetPin =
						pinlist.getPins().find(pin -> designObject.getName().equals(pin.getName()));
				if (targetPin == null && pinlist instanceof IDevice &&
						((IMultiSymbolledPinlist) pinlist).getSymbols().getSize() != 0 &&
						designObject instanceof ISharedPin) {
					targetPin = ((IDevice) pinlist).getInternalPins()
							.find(ipin -> getAllSymbolConnectityPins(spl, (ISharedPin) designObject)
									.contains(ipin.getSymbolReference()));
				}
				targetPort =
						targetPin != null ? new DesignPort(targetPin) : DesignPort.UNDEFINED;
			}
		}

		return targetPort;
	}

	@NotNull
	private Set<IUID> getAllSymbolConnectityPins(ISharedPinList sharedPinList, ISharedPin sharedPin)
	{
		Set<IUID> symConnectivityPins = new LinkedHashSet<>();

		for (IUID symSchemPinUID : sharedPinList.getAllSymbolPins(sharedPin)) {
			IPin symSchemPin = (IPin) UIDMgr.getNonDeletedObject(symSchemPinUID);
			if (symSchemPin != null) {
				IUID symConnectivityPin = symSchemPin.getConnectivityUID();
				if (symConnectivityPin != null) {
					symConnectivityPins.add(symConnectivityPin);
				}
			}
		}

		return symConnectivityPins;
	}

	protected void unsharePins()
	{
		unsharePins(cablePinList);
	}

	protected void unshareThisPinList()
	{
		cablePinList.setSharedPinList(null);
	}

	protected Set<ILogicObject> getLockableCableObjects()
	{
		return Collections.singleton(cablePinList);
	}

	protected void populatePinStackPinInfo()
	{
		//FEAT13786 - populate all the stack pin usages of all the cable pins (for mate also, if exists)
		//TODO FEAT13786- Why do we bother when there is only single representation of the PL "or" when all the existing representations are selected
		//TODO FEAT13786- optimization possible to do this only in the case where new cable PL is created.
		pin_stackPinReps_Map = new HashMap<IAbstractPin, Set<ISchemStackPin>>();
		populatePinStackPinRepsMap(cablePinList.getPins());
	}

	protected boolean determineToUnshareAll(IDesignWideUsageMgr dwum)
	{
		return schemPinLists.size() == dwum.getDesignSharedUsageCount(cablePinList);
	}

	private Map<IAbstractPin, ISharedPin> getConnectivityToSharedPinLink(chs.cof.logical.cable.IPinList list)
	{
		Map<IAbstractPin, ISharedPin> links = new HashMap<IAbstractPin, ISharedPin>();
		for (IAbstractPin pin : list.getPinCollection()) {
			if (pin.getSharedPin() != null) {
				links.put(pin, pin.getSharedPin());
			}
		}
		return links;
	}

	/**
	 * given a shematic pin list that references a symbol, and a schematic symbol pin uid. This method will search for
	 * the symbol pin in the symbol referenced by the pin list.
	 *
	 * @param schemSymPinUID
	 * @param parentSchematic
	 *
	 * @return
	 */
	@Nullable private IPin getCorrespondingSymbolPin(IUID schemSymPinUID, IPinList parentSchematic)
	{
		ISymbolDef symbolDef = parentSchematic.getSymbolDef();
		if (symbolDef == null || schemSymPinUID == null) {
			return null;
		}
		//FEAT00013786: stack pins are not expected in symbols
		for (IPin p : symbolDef.getPinList().getPins()) {
			if (p.getUID() == schemSymPinUID) {
				return p;
			}
		}
		return null;
	}

	//Replicate the logic compositeblocks in connectivity section if there are any.
	private void replicatBlocks(chs.cof.logical.cable.IPinList unsharedPinList)
	{
		if (cablePinList instanceof IDevice && unsharedPinList instanceof IDevice) {
			for (IBlock blk : ((IDevice) cablePinList).getBlocks()) {
				replicator.replicateBlock((IDevice) cablePinList, (IDevice) unsharedPinList, blk);
			}
		}
	}

	protected void buildOldUsagesMap()
	{
		//dts0100589133 Device loses its symbol association after it is unshared
		//we need to keep track of the shared usages of the pinList before it gets unshared
		//because after unsharing all the instances of the pinList, getDesignSharedUses will not return any usage
		//as it will look for usages of the non-shared cablePinList which will not be added to the SharedUsageManager
		//until the UnShare Action is completed

		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();

		for (IAbstractPin pin : getPinsForUsages(cablePinList)) {
			for (IDesignSharedUsage usage : dwum.getUsages(pin)) {
				PinUsageInfo pinUsageInfo = new PinUsageInfo(usage.getDiagramObjectUID(), usage.getDiagramUID());
				oldPinUsageInfoMap.add(pin.getUID(), pinUsageInfo);
			}
		}
	}

	/**
	 * If its connector , get backshell pins as well.
	 * If its device, also get backshell pins from device connectors.
	 *
	 * @param pinlist - cable pinlist
	 *
	 * @return - all pins
	 */
	protected Collection<IAbstractPin> getPinsForUsages(chs.cof.logical.cable.IPinList pinlist)
	{
		Collection<IAbstractPin> pinSet = new LinkedHashSet<IAbstractPin>();
		pinSet.addAll(pinlist.getPinCollection());
		if (pinlist instanceof IConnector) {
			IBackshell backshell = ((IConnector) pinlist).getBackshell();
			if (backshell != null) {
				pinSet.addAll(backshell.getPinCollection());
			}
		}
		if (pinlist instanceof IDevice) {
			for (IDeviceConnector devConn : ((IDevice) pinlist).getDeviceConnectors()) {
				IBackshell backshell = devConn.getBackshell();
				if (backshell != null) {
					pinSet.addAll(backshell.getPinCollection());
				}
			}
		}
		return pinSet;
	}

	protected void unsharePins(chs.cof.logical.cable.IPinList theCablePinList)
	{
		for (IAbstractPin cablePin : theCablePinList.getPins()) {
			// Make all the pins local too
			// Clear the shared pin before copying the info, or it just gets set back on the shared pin again. doh.
			unsharePin(cablePin);
		}
	}

	protected void renamePinlist(chs.cof.logical.cable.IPinList pinlist, @Nullable chs.cof.logical.cable.IPinList mate)
	{
		if (m_newName != null) {
			//dts0100693684 Unshare function changes a device name automatically even though the user want to
			//  keep same name as shared device.
			pinlist.setName(null);
			pinlist.setName(m_newName);
		}
		if (shortDesc != null) {
			pinlist.setShortDescription(shortDesc);
		}
		if (mate != null) {
			if (m_newMateName != null) {
				mate.setName(m_newMateName);
			}
			if (mateShortDesc != null) {
				mate.setShortDescription(mateShortDesc);
			}
		}
	}

	private void updateConnectedPinlistOwnership(chs.cof.logical.cable.IPinList oldConnectivity, IPinList schemPinList,
			chs.cof.logical.cable.IPinList newConnectivity, IDesignWideUsageMgr dwum)
	{
		boolean disconnect = false;
		if (oldConnectivity instanceof IDevice) {
			// If we're unsharing a device, then reparent (or disconnect) and plugs connected to us
			for (IPinList attachedPinList : schemPinList.getAttachedPinListObjects()) {
				if (attachedPinList.getConnectivity() instanceof IHarnessPlugConnector) { // Should always be true?
					// If this connected plug is not shared, just reparent to this new connectivity object
					/*if (attachedPinList.getSharedObject() == null) {
						   int repObjCount =
								   attachedPinList.getDiagram().getRepresentations(attachedPinList.getConnectivity().
										   getUID()).getSize();
						   // If the connected plug is not shared but there is more than one instance in the diagram then
						   // disconnect the schem. This is typically for DWOs.
						   if (repObjCount > 1) {
							   disconnect = true;
						   }
						   else {
							   ((IDeviceOwned) attachedPinList.getConnectivity()).setOwner((IDevice) newConnectivity);
						   }
					   }*/
					//else {
					// If the connected plug is not shared but there is more than one instance in the diagram then
					// disconnect the schem. This is typically for DWOs.
					IDeviceOwned deviceOwnedObject = (IDeviceOwned) attachedPinList.getConnectivity();
					IDevice newDeviceConnectivity = (IDevice) newConnectivity;
					chs.cof.logical.cable.IPinList plug = attachedPinList.getConnectivity();
					if (dwum.getDesignSharedUsageCount(plug) > 1) {
						disconnect = true;
					}
					else if (newDeviceConnectivity.isConnected(deviceOwnedObject)) {
						// if there is only one instance of the plug we are safe to reparent

						deviceOwnedObject.setOwner(newDeviceConnectivity);
					}
					//}
				}
			}
		}
		else {
			if (oldConnectivity instanceof IConnector && ((IConnector) oldConnectivity).isInline()) {
				// Don't do any disconnect action for inlines.
				return;
			}
			for (IPinList attachedPinList : schemPinList.getAttachedPinListObjects()) {
				ISharedObject sharedPinList = attachedPinList.getSharedObject();
				if (sharedPinList != null) {
					IDesignSharedUsageMgr dsum = design.getSharedUsageMgr();
					if (dsum.getDesignSharedUsageCount(sharedPinList) > 1) {
						// This object is shared, and used multiple times, disconnect
						disconnect = true;
					}
				}
			}

			// If we're keeping the connector connected to the pin list then we need to ensure that
			// the owner is set on the connector (it will not be by default on a newly replicated
			// connector - which will occur if there is more than one instance of the shared connector
			// in the design)
			if (!disconnect && newConnectivity instanceof IDeviceOwned) {
				// If we're attached to something then set that to be our owner. We ASSUME that there is
				// only one device to which we're connected...
				for (IPinList attachedPinList : schemPinList.getAttachedPinListObjects()) {
					if (attachedPinList != null && attachedPinList.getConnectivity() instanceof IDevice) {
						((IDeviceOwned) newConnectivity).setOwner((IDevice) attachedPinList.getConnectivity());
					}
				}
			}
		}

		if (disconnect) {
			ConnectionHelper.disconnect(schemPinList);
			DeviceConnectorDeletionTracker.getInstance().removeSchemDeviceConnectors(schemPinList,
					DiagramHelper.getDiagram(schemPinList));
		}
	}

//	private void backupInternalLinkPinRefs(chs.cof.logical.cable.IPinList src)
//	{
//		if (src instanceof IDevice) {
//			LinksToUpdateStartPin.clear();
//			LinksToUpdateEndPin.clear();
//			IDevice dev = (IDevice) src;
//			for (IInternalLink link : dev.getInternalLinkCollection()) {
//				IGenericPin startPin = link.getStartPin();
//				IGenericPin endPin = link.getEndPin();
//				if (startPin != null && startPin instanceof IAbstractPin) {
//					LinksToUpdateStartPin.put(link.getUID(), startPin.getUID());
//				}
//				if (endPin != null && endPin instanceof IAbstractPin) {
//					LinksToUpdateEndPin.put(link.getUID(), endPin.getUID());
//				}
//			}
//		}
//	}

	private void replicatePins(IPinList thePinList, chs.cof.logical.cable.IPinList theCablePinList,
			chs.cof.logical.cable.IPinList newPinList, ISharedPinList spl,
			@NotNull Set<IAbstractPin> unplacedPinsToDelete)
	{
		if (getPinListConnectionTransferer() != null) {
			getPinListConnectionTransferer().init();
		}
		for (IPin schemPin : thePinList.getPins()) {
			IAbstractPin connPin = schemPin.getConnectivity();
			alreadyReplicatedPins.add(connPin);
			// replicate the connectivity pin.
			IAbstractPin newCablePin = replicateConnectivityPin(theCablePinList, newPinList, connPin, true);

			IPin symPin = getCorrespondingSymbolPin(schemPin.getSymbolPinUIDFromSchematic(), thePinList);
			if (symPin != null) {
				newCablePin.setSymbolName(symPin.getConnectivity().getName());
			}
			// Make sure the schem object points to our new connectivity object
			schemPin.setConnectivity(newCablePin);

			// Now we have local instance of this device, it may be connected to a condutor, and so we should
			// tell the conductor about it.
			reconnectConductors(schemPin, connPin, newCablePin);

			// Copy the symbol pin refs from the schem object to the connectivity object
			copySymbolPinRefs(thePinList, spl, connPin.getSharedPin(), newCablePin);

			//dts0100800070 - ST-Bash70 -[CH] java.lang.AssertionError: Null property on prop text.  at chs.caplets.logic.actions.shared.UnsharePinListActionHelper.up
			PropertyHelper.updatePropertyTexts(schemPin, newCablePin);

			// TODO jacobt FEAT13040 test/handle pin mates & backshells
			if (!(connPin instanceof IBackshellTermination)) {
				if (theCablePinList != cablePinListMate && cablePinListMate != null) {
					// If this is an inline, and this is the plug side then store their mate pins for later re-match
					matePinMap.put(((IConnectorPin) connPin).getMatedPin(), newCablePin);
				}
				else if (cablePinListMate != null) {
					// If we're an inline and this is the jack side, mate up the new set of abstract pins correctly.
					//This is the first connected pin added.
					if (matePinMap.get(connPin) != null) {
						matePinMap.get(connPin).forceConnection(newCablePin);
					}
				}

				// Deal with the connected pins, and remove the old connectivity object if necessary
				reconnectPins(connPin, newCablePin, unplacedPinsToDelete);
			}
		}
		//FEAT13786- some cable pins may be solely represented by stackpin, we may have to create new cable pins for such pins.
		for (ISchemStackPin stackPin : thePinList.getStackPins()) {
			for (IAbstractPin connPin : stackPin.getAllConnectivity()) {
				IAbstractPin newCablePin;
				IUIDObject uidObj = replicator.getNewObject(connPin.getUID());
				if (uidObj != null && uidObj instanceof IAbstractPin) {
					//get the already replicated pin.
					newCablePin = (IAbstractPin) uidObj;
				}
				else {
					// replicate the connectivity pin.
					newCablePin = replicateConnectivityPin(theCablePinList, newPinList, connPin, true);
				}
				alreadyReplicatedPins.add(connPin);
				for (chs.cof.logical.cable.IConductor cond : connPin.getConductorsAsSet()) {
					newCablePin.addConductor(cond);
				}

				if (!(connPin instanceof IBackshellTermination)) {
					if (theCablePinList != cablePinListMate && cablePinListMate != null) {
						// If this is an inline, and this is the plug side then store their mate pins for later re-match
						matePinMap.put(((IConnectorPin) connPin).getMatedPin(), newCablePin);
					}
					else if (cablePinListMate != null) {
						// If we're an inline and this is the jack side, mate up the new set of abstract pins correctly.
						if (matePinMap.get(connPin) != null) {
							matePinMap.get(connPin).forceConnection(newCablePin);
						}
					}
					// Deal with the connected pins, and remove the old connectivity object if necessary
					reconnectPins(connPin, newCablePin, unplacedPinsToDelete);
				}
			}
		}
	}

	/**
	 * This method will replicate the connectivity pin without caring about any connections. It will take care of
	 * properties and attributes.
	 *
	 * @param theCablePinList
	 * @param newPinList
	 * @param connPin
	 * @param replicateSymbolReferences this will indicate whether it is required for the new pin to reference the same
	 *                                  symbol as the original pin or not.
	 *
	 * @return the replicated connectivity pin.
	 */
	protected IAbstractPin replicateConnectivityPin(chs.cof.logical.cable.IPinList theCablePinList,
			chs.cof.logical.cable.IPinList newPinList,
			IAbstractPin connPin, boolean replicateSymbolReferences)
	{
		IAbstractPin newCablePin;
		if (!(connPin instanceof IBackshellTermination)) {
			newCablePin = (IAbstractPin) replicator.replicatePin(theCablePinList, newPinList, connPin);

			if (connPin.getBlockRef() != null && replicateSymbolReferences) {
//				srcPinBlockRefs.put(newCablePin.getUID(), connPin.getBlockRef());
			}
		}
		else {
			IBackshellTermination bst = (IBackshellTermination) connPin;
			IBackshell destinationBackshell = findDestinationBackshell(bst);
			newCablePin = replicator.replicate(bst, destinationBackshell, true);
		}

		newCablePin.setName(connPin.getName());
		// Todo moattia: Do we need those, They are set in the replicator anyway.
		if (connPin.isShared()) {
			replicator.replicateCopyableObject(connPin.getSharedPin(), newCablePin);
		}

		if (!replicateSymbolReferences) {
			((IReference) newCablePin).setReference(null);
		}

		return newCablePin;
	}

	/**
	 * Finds the destination backshell for replicating a backshell termination.
	 *
	 * @param bst         the source backshell termination
	 * @return the destination backshell where the new termination should be added
	 */
	@NotNull private IBackshell findDestinationBackshell(@NotNull IBackshellTermination bst)
	{
		IBackshell sourceBackshell = (IBackshell) bst.getOwner();
		assert sourceBackshell != null;
		IUIDObject replicatedObj = replicator.getNewObject(sourceBackshell.getUID());
		if (replicatedObj instanceof IBackshell replicatedBackshell) {
			return replicatedBackshell;
		}
		throw new IllegalStateException("Could not find destination backshell for backshell termination '"
				+ bst.getName() + "'. Ensure device connectors and their backshells are replicated "
				+ "before replicating backshell termination pins.");
	}

	private void replicateInternalConnectivity(chs.cof.logical.cable.IPinList srcPinList,
			chs.cof.logical.cable.IPinList destPinList, IPinList schemObj, Replicator replicator)
	{
		IDevice src = (IDevice) srcPinList;
		IDevice dest = (IDevice) destPinList;
		ISharedPinList sharedPinList = src.getSharedPinList();
		assert sharedPinList != null;

		if (!dest.getSymbolReferences().isEmpty()) {
			Map<IUID, IAbstractPin> destPinsSymbolRefsList = new HashMap<IUID, IAbstractPin>();
			for (IAbstractPin pin : dest.getPins()) {
				if (pin.getReference() != null) {
					destPinsSymbolRefsList.put(pin.getReference(), pin);
				}
			}
			//Copy all the internalPins from source to destination pinlist
			for (IInternalPin pin : src.getInternalPins()) {
				if (pin.getReference() == null || !destPinsSymbolRefsList.containsKey(pin.getReference())) {
					replicator.replicateCablePinsAsInternal(pin, dest);
				}
				else {
					replicator.setNewObject(pin.getUID(), destPinsSymbolRefsList.get(pin.getReference()));
				}
			}

			//replicate the remaining external pins as internal in target unsharedPinlist
			for (IAbstractPin extPin : src.getPins()) {
				if (!alreadyReplicatedPins.contains(extPin)) {
					IPin srcPin = sharedPinList.getSymbolPin(extPin.getSharedPin());
					if (srcPin != null) {
						String srcPinRef = srcPin.getConnectivity().getUID().getString();
						boolean shouldMakeInternal = true;
						for (IAbstractPin pin : dest.getPins()) {
							IUID destPinRef = pin.getReference();
							if (destPinRef != null && destPinRef.getString().equalsIgnoreCase(srcPinRef)) {
								shouldMakeInternal = false;
								break;
							}
						}
						if (shouldMakeInternal) {
							IInternalPin pin = replicator.replicateCablePinsAsInternal(extPin, dest);
							pin.setName(extPin.getName());
						}
					}
				}
			}
		}

		//replicate the internalLinks and adjust their start and end Pins   both at source and target cable objects
		for (IInternalLink link : src.getInternalLinkCollection()) {
			replicator.replicateInternalLink(src, dest, link, true);
		}

		purgeTheDanglingLinks(dest);
		purgeTheDanglingLinks(src);

		schemObj.removeSchemInternalLinks();
		schemObj.removeSchemInternalPins();
	}

	private void purgeTheDanglingLinks(IDevice pinList)
	{
		//Remove all the dangling links from the device
		if (pinList.hasInternalConnectivity()) {
			for (IInternalLink link : pinList.getInternalLinkCollection()) {
				if (link.getStartPin() == null || link.getEndPin() == null) {
					CreationDeletionHelper.getTheCreationHelper().addDeletionObject(link);
				}
			}
		}
	}

	private void reconnectConductors(IPin schemPin, IAbstractPin oldPin, IAbstractPin newPin)
	{
		// The existing connectivity pin needs to update itself so it only refers to the conductors it is physically
		// connected to
		Set<IConductor> allConds = new HashSet<IConductor>();
		boolean bRemoveOldPinConductors = true;
		for (PinUsageInfo pinUsageInfo : getOldPinUsageInfoList(oldPin)) {
			IAbstractSchemPin otherSchemPin = pinUsageInfo.getPinForceLoad();
			if (otherSchemPin != null && otherSchemPin != schemPin) {
				if (otherSchemPin instanceof IPin) {
					for (Object condObj : ((IPin) otherSchemPin).getConductors()) {
						allConds.add((IConductor) condObj);
					}
				}
				else if (otherSchemPin instanceof ISchemStackPin) {
					bRemoveOldPinConductors = false;
				}
			}
		}

		if (bRemoveOldPinConductors) {
			oldPin.removeAllConductors();
			//for (IConductor cond : allConds) {
			//	oldPin.addConductor(cond.getConnectivity());
			//}
			for (IConductor cond : allConds) {
				boolean addConductorToOldPin = true;
				if (oldPin instanceof ISplicePin) {
					addConductorToOldPin = shouldAddConductorToOldPin(oldPin, cond);
				}
				if (addConductorToOldPin) {
					oldPin.addConductor(cond.getConnectivity());
				}
			}
		}
		// The new connectivity object only connects to the conductors it is physically connected to.
		for (Object obj : schemPin.getConductors()) {
			IConductor cond = (IConductor) obj;
			newPin.addConductor(cond.getConnectivity());
		}
	}

	private boolean shouldAddConductorToOldPin(IAbstractPin oldPin, IConductor cond)
	{
		chs.cof.logical.cable.IConductor wireConductor = cond.getConnectivity();
		Set<ISplice> centreStripSplices = Collections.emptySet();
		if (wireConductor instanceof IWireConductor) {
			IWireConductor iWireConductor = (IWireConductor) wireConductor;
			centreStripSplices = iWireConductor.getCenterStripSplicesAsSet();
		}
		if (!centreStripSplices.isEmpty()) {
			// DO NOT Add Center Strip Splices...
			// These are still in the 'conductor.getCenterStripSplices()' at this point and
			// should not be re-added to the pins of the IWireConductor
			chs.cof.logical.cable.IPinList owner = oldPin.getOwner();
			if (owner instanceof ISplice) {
				ISplice spliceOwner = (ISplice) owner;
				if (centreStripSplices.contains(spliceOwner)) {
					return false;
				}
			}
		}
		return true;
	}

	private List<PinUsageInfo> getOldPinUsageInfoList(IAbstractPin oldPin)
	{
		List<PinUsageInfo> list = oldPinUsageInfoMap.getList(oldPin.getUID());
		if (list == null) {
			list = Collections.emptyList();
		}
		return list;
	}

	private void reconnectPins(IAbstractPin oldPin, IAbstractPin newPin,
			@NotNull Set<IAbstractPin> unplacedPinsToDelete)
	{
		List<PinUsageInfo> oldUIDs = getOldPinUsageInfoList(oldPin);
		int oldUsageCount = oldUIDs == null ? 0 : oldUIDs.size();
		// dts0100694175- depend on the PinListConnectionTransferer to determine which pin connections can
		// be transferred to the unshared pin list.
		PinListConnectionTransferer connectionTransferer = getPinListConnectionTransferer();
		if (connectionTransferer != null && connectionTransferer.shouldTransferAnyPinConnection(oldPin)) {
			connectionTransferer.transferPinConnections(oldPin, newPin);
		}
		//dts0100806046 [CH] java.lang.NullPointerException  at chs.caplets.logic.actions.LogicTableDataChangeAction.getEndPinListsFromPins(LogicTabl
		if (oldUIDs != null) {
			for (PinUsageInfo usage : oldUIDs) {
				IAbstractSchemPin usedSchemPin = usage.getPinForceLoad();
				if (usedSchemPin instanceof IGenericSchemPin) {
					if (((IConnectivityRef) usedSchemPin).getConnectivity().getUID() != oldPin.getUID()) {
						oldUsageCount--;
					}
				}
			}
		}
		if (oldUsageCount == 0) {
			// Delete the pin connectivity, this will remove it from the UIDMgr, and most importantly remove it from
			// it's owner.
			oldPin.clearConnectedPin();
			fixupInternalConnectivityInSrc(oldPin, newPin);
			//collect this unplaced pin to delete at end otherwise this would cause
			//issue with device connector replication because the device connector
			//pin will not have corresponding source device pin and thus replication
			//failing to connect the device connector pin to device pin.
			unplacedPinsToDelete.add(oldPin);
		}
	}

	private void fixupInternalConnectivityInSrc(IAbstractPin oldPin, IAbstractPin newPin)
	{
		if (oldPin.getOwner() instanceof IDevice &&
				!((IDevice) oldPin.getOwner()).getInternalLinkCollection().isEmpty()) {
			IInternalPin newInternalPin = replicator.replicateCablePinsAsInternal(newPin, oldPin.getOwner());
			newInternalPin.setName(oldPin.getName());
			newInternalPin.setBlockRef(oldPin.getBlockRef());
			newInternalPin.setReference(newPin.getReference());
			updateInternalLinks(oldPin.getOwner(), oldPin, newInternalPin);
			replicator.setNewObject(newInternalPin.getUID(), newPin);
		}
	}

	private void updateInternalLinks(chs.cof.logical.cable.IPinList owner, IGenericPin oldPin,
			IGenericPin newPin)
	{
		if (!(owner instanceof IDevice)) {
			return;
		}
		IDevice device = ((IDevice) owner);
		for (IInternalLink link : device.getInternalLinkCollection()) {
			IGenericPin startPin = link.getStartPin();
			IGenericPin endPin = link.getEndPin();
			if (startPin != null && startPin.getUID() == oldPin.getUID()) {
				link.setStartPin(newPin);
			}
			if (endPin != null && endPin.getUID() == oldPin.getUID()) {
				link.setEndPin(newPin);
			}
		}
	}


	private void replicateDeviceConnectorsWithoutPins(@NotNull chs.cof.logical.cable.IPinList theCablePinList,
			@NotNull chs.cof.logical.cable.IPinList newPinList)
	{
		if (theCablePinList instanceof IDevice) {
			for (IDeviceConnector devConn : ((IDevice) theCablePinList).getDeviceConnectors()) {
				IDeviceConnector newDevConn =
						(IDeviceConnector) replicator.replicatePinListConnectivity(devConn, true, false, true);
				replicator.replicateCopyableObject(devConn.getSharedPinList(), newDevConn);
				((IDevice) newPinList).addDeviceConnector(newDevConn);
			}
		}
	}


	private void replicateDeviceConnectorPins(@NotNull chs.cof.logical.cable.IPinList theCablePinList)
	{
		if (theCablePinList instanceof IDevice) {
			for (IDeviceConnector devConn : ((IDevice) theCablePinList).getDeviceConnectors()) {
				IUIDObject replicatedObj = replicator.getNewObject(devConn.getUID());
				if (replicatedObj instanceof IDeviceConnector newDevConn) {
					for (IAbstractPin absConPin : devConn.getPins()) {
						IDeviceConnPin devConPin = (IDeviceConnPin) absConPin;
						IAbstractPin newDevConPin =
								(IAbstractPin) replicator.replicatePin(devConn, newDevConn, devConPin);
						replicator.replicateCopyableObject(devConPin.getSharedPin(), newDevConPin);
					}
				}
			}
		}
	}

//	private void replicateHarnessConnectors(chs.cof.logical.cable.IPinList theCablePinList,
//			chs.cof.logical.cable.IPinList newPinList)
//	{
//		if (theCablePinList instanceof IDevice) {
//			for (IConnector conn : ((IDevice) theCablePinList).getConnectors()) {
//				IHarnessPlugConnector newConn =
//						(IHarnessPlugConnector) replicator.replicatePinListConnectivity(conn, true);
//				replicator.replicateCopyableObject(conn.getSharedPinList(), newConn);
//				((IDevice) newPinList).addConnector(newConn);
//				for (IAbstractPin absConPin : conn.getPins()) {
//					IConnectorPin conPin = (IConnectorPin) absConPin;
//					IAbstractPin newConPin = replicator.replicatePin(conn, newConn, conPin);
//					replicator.replicateCopyableObject(conPin.getSharedPin(), newConPin);
//				}
//			}
//		}
//	}

	protected void unsharePinList(chs.cof.logical.cable.IPinList pinlist)
	{
		ISharedPinList spl = pinlist.getSharedPinList();
		pinlist.setSharedPinList(null);
		replicator.replicateCopyableObject(spl, pinlist);
	}

	@Nullable protected IBackshell getBackshell(@Nullable chs.cof.logical.cable.IPinList pinlist)
	{
		if (pinlist instanceof IConnector) {
			return ((ICavitiesOwner) pinlist).getBackshell();
		}
		return null;
	}

	protected void unshareBackShells(@Nullable chs.cof.logical.cable.IPinList pinlist)
	{
		IBackshell backshell = getBackshell(pinlist);
		if (backshell != null) {
			for (IBackshellTermination term : backshell.getBackshellTerminations()) {
				unsharePin(term);
			}
			// COG: as replicator.replicateCopyableObject(...) will affect pins
			// we need to unshare the pins first.
			unsharePinList(backshell);

			// For device connectors, the shared backshell symbol ref is stored on the ISharedBackshell
			// (owned by the shared DC via ISharedBackshellOwner). replicateCopyableObject does not
			// copy the symbol ref to the base backshell, so copy it explicitly here.
			if (pinlist != null) {
				if(pinlist instanceof IDeviceConnector) {
					ISharedPinList sharedPL = pinlist.getSharedPinList();
					if (sharedPL instanceof ISharedBackshellOwner sharedOwner) {
						ISharedBackshell sharedBS = sharedOwner.getBackshell();
						if (sharedBS != null) {
							ISymbolRef sharedSymRef = sharedBS.getPinSymbolRef();
							if (sharedSymRef != null) {
								backshell.setSymbolRef(sharedSymRef);
							}
						}
					}
				}
			}
		}
	}

	protected void unsharePin(IAbstractPin cablePin)
	{
		ISharedPin spin = cablePin.getSharedPin();
		if (spin == null) {
			// In the case where a shared object has a device connector, and the device connector library part had
			// an extra pin added to it post sharing, but before unsharing, there will be no shared pin.
			return;
		}

		Map<IPinList, IPin> symbols = null;

		if (schemPinLists != null && cablePin.getOwner().canMaintainMultipleSymbols()) {
			symbols = getSymbolsForSharedPin(schemPinLists, spin.getOwner(), spin);
		}

		cablePin.setSharedPin(null);

		Set<IAbstractPin> cablePins = new HashSet<IAbstractPin>();
		cablePins.add(cablePin);

		if (symbols != null && symbols.size() > 1) {
			createConnectivityPinsForDifferentSymbols(cablePin, symbols, cablePins);
		}

		for (IAbstractPin pin : cablePins) {
			replicator.replicateCopyableObject(spin, pin);
			PropertyHelper.transferProperties(replicator, spin, pin, null);
			// dts0100572990 restore the name values and flags to its zero state before giving it a name
			pin.setName(null);
			pin.setName(spin.getName());
			IAttribute attribute = spin.getAttribute(IAttributeTypes.SHORT_DESCRIPTION);
			String pinShortDesc =
					attribute == null ? null : LanguageDictionaryHelper.getUntranslatedValueAsString(attribute);
			pin.setShortDescription(pinShortDesc);
		}
	}

	protected void createConnectivityPinsForDifferentSymbols(IAbstractPin cablePin, Map<IPinList, IPin> symbols,
			Set<IAbstractPin> cablePins)
	{

	}

	private static void disconnectPinLists(IPinList thePinList)
	{
		if (thePinList.getConnectivity() instanceof ISplice) {
			return;
		}

		for (IPinList pl : thePinList.getAttachedPinListObjects()) {
			boolean shouldDetach = true;
			// if both are devices.
			if (thePinList.getConnectivity() instanceof IDevice && pl.getConnectivity() instanceof IDevice) {
				// and they have some schem connections
				if (!ConnectionHelper.getDeviceToDeviceSchemConnections(thePinList, pl).isEmpty()) {
					// don't detach them.
					shouldDetach = false;
				}
			}
			// else if one of them is a connector
			else {

				shouldDetach = !PinListHelper.isHarnessFootprintedAndAllowAutoCreation(thePinList)
						&& !PinListHelper.isAutoCreatedConnectorAndAllowAutoCreation(pl);
				// and the device is not footprinted or the design is not in HC autogeneration mode
				if (shouldDetach) {
					// If no pins are connected to other pins now, then disconnect the schem objects too.
					ConnectionHelper chelper = ConnectionHelper.getConnectionHelper(thePinList, pl);
					if (chelper == null) {
						continue;
					}
					IPinList connector = pl.getConnectivity() instanceof IConnector ? pl : thePinList;
					// check for any schem connection between them
					for (IAbstractSchemPin schemPin : connector.getAllPins()) {
						IAbstractSchemPin cpin = chelper.getConnectedPin(schemPin);
						if (cpin != null) {
							if (cpin instanceof IPin) {
								assert schemPin instanceof IPin;
								if (((IPin) schemPin).getConnectivity().isConnected(((IPin) cpin).getConnectivity())) {
									shouldDetach = false;
									break;
								}
							}
							else if (cpin instanceof ISchemStackPin) {
								assert schemPin instanceof ISchemStackPin;
								shouldDetach = false;
								break;
							}
						}
					}
				}
			}
			if (shouldDetach) {
				// Disconnect the connectors only, don't for the pins connected to conductors to be disconnected.
				if (PinListShareHelper.shouldDisconnect(pl)) {
					thePinList.removeAttachedObject(pl);
					pl.removeAttachedObject(thePinList);
				}
			}
		}
	}

	protected static void removeXrefText(IPinList thePinList)
	{
		// Remove the cross reference text since we're no longer shared
		thePinList.setCrossReferencesMarkedVisible(false);
		Iterator<IXRefTextContainer> xrefIter = thePinList.getXRefTextContainers();
		Set<IXRefTextContainer> xrefs = new HashSet<IXRefTextContainer>();
		while (xrefIter.hasNext()) {
			IXRefTextContainer obj = xrefIter.next();
			ICrossReferenceable crossThing = obj.getCrossReferenceable();
			if (crossThing == thePinList) {
				xrefs.add(obj);
			}
		}

		for (IXRefTextContainer ref : xrefs) {
			thePinList.removeXRefTextContainer(ref);
		}
	}

	public void cleanup()
	{
		cablePinList = null;
		cablePinListMate = null;
		matePinMap = null;
		oldSymDefMap = null;
		oldSymInstNumMap = null;
		oldPinUsageInfoMap = null;
		replicator = null;
		alreadyReplicatedPins = null;
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
	}

	public boolean isNewSharedObject()
	{
		return false;
	}

	public void setNewName(@NotNull String newName)
	{
		m_newName = newName;
	}

	public void setNewMateName(@NotNull String newName)
	{
		m_newMateName = newName;
	}

	/**
	 * Issue a prompt for specifying a new name for the unshared pinlist and possibly it's mate
	 *
	 * @param pinlist  The connectivity pinlist that is about to be unshared
	 * @param mateConn The connectivity pinlist mate that is about to be unshared
	 *
	 * @return true unless one of the prompts was cancelled
	 */
	protected boolean promptRenameLocalPinList(@NotNull chs.cof.logical.cable.IPinList pinlist,
			@Nullable chs.cof.logical.cable.IPinList mateConn)
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null || Environment.isUnitTest()) {
			// If the view is null - most likely we're in a unit test - let not both with the rename options
			m_newName = pinlist.getName();
			return true;
		}

		String label = "UnsharePinListActionHelper.Rename.Name.label";
		if (mateConn != null) {
			label = "UnsharePinListActionHelper.Rename.Recep.label";
		}
		InlineRenameDialog dialog = new InlineRenameDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				pinlist, mateConn,
				ResourceMgr.getString(UnsharePinListActionHelper.class,
						getRenamePinListDialogTitleKey()),
				ResourceMgr.getString(UnsharePinListActionHelper.class,
						label), false, design.getProject());

		dialog.getOkButton().setEnabled(true);
		dialog.setVisible(true);
		m_newName = dialog.getNewName();
		if (mateConn != null) {
			m_newMateName = dialog.getNewMateName();
		}
		if (m_newName == null) {
			return false;
		}
		IAttribute attribute = pinlist.getAttribute(IAttributeTypes.SHORT_DESCRIPTION);
		shortDesc = attribute == null ? null : LanguageDictionaryHelper.getUntranslatedValueAsString(attribute);
		IAttribute mateAttribute = mateConn == null ? null : mateConn.getAttribute(IAttributeTypes.SHORT_DESCRIPTION);
		mateShortDesc =
				mateAttribute == null ? null : LanguageDictionaryHelper.getUntranslatedValueAsString(mateAttribute);

		ISharedPinListIterator condIter = ((ISharedFullyLoadedPinListMgr)design.getProject().getSharedPinListMgr()).getSharedPinLists();
		boolean sharedObjectMatchFound = false;
		boolean sharedMateObjectMatchFound = false;
		while (condIter.hasNext()) {
			ISharedPinList sharedCond = condIter.getNext();
			if (!sharedObjectMatchFound && sharedCond.getName().equals(m_newName)) {
				// If there is a shared pinlist with this name already, then warn.
				if (!warningDialog(getNameErrorForSharedObjectMessageKey(), m_newName)) {
					return false;
				}
				if (mateConn == null || sharedMateObjectMatchFound) {
					// No mate (or it's already warned?  Our job here is done.
					break;
				}
				// Lets not warn again, since they don't seem to mind duplicates, there could well be more ;)
				sharedObjectMatchFound = true;
			}
			if (mateConn != null && !sharedMateObjectMatchFound && sharedCond.getName().equals(m_newMateName)) {
				// Warn if the mate name matches an already existing shared pinlist
				if (!warningDialog(getNameErrorForSharedObjectMessageKey(), m_newMateName)) {
					return false;
				}
				if (sharedObjectMatchFound) {
					// We've found both possibly cases?  Our job here is done...
					break;
				}
				// Lets not warn again, since they don't seem to mind duplicates, there could well be more ;)
				sharedMateObjectMatchFound = true;
			}
		}

		// Are there any local pin lists with the same name, warn about those too!!
		String warningString = getNameAlreadyExistsMessageText();
		if (pinlist.getNameMgr().nameExists(m_newName, pinlist)) {
			if (!warningDialog(warningString, m_newName)) {
				return false;
			}
		}
		// WOW!!  There are even mated pinlists with matching name?! Defo warn here, that's crazy.
		// This could lead to FOUR name clash warning dialogs being thrown, but it's what QA seem to think is correct
		// I love QA, don't you?  dts0100391756
		if (mateConn != null && pinlist.getNameMgr().nameExists(m_newMateName, mateConn)) {
			if (!warningDialog(warningString, m_newMateName)) {
				return false;
			}
		}

		return true;
	}

	@NotNull protected String getNameAlreadyExistsMessageText()
	{
		return "UnsharePinListActionHelper.NameExistsError.Message.text";
	}

	@NotNull protected String getNameErrorForSharedObjectMessageKey()
	{
		return "UnsharePinListActionHelper.NameExistsError.SharedMessage.text";
	}

	@NotNull protected String getRenamePinListDialogTitleKey()
	{
		return "UnsharePinListActionHelper.Rename.Title";
	}

	private static boolean warningDialog(@NotNull String warningString, @NotNull String nameClashString)
	{
		return MessageHelper.showDoNotShowThisMessageAgainYesNoDialogue(
				CAFUtils.getInstance().getWindowMgr().getDialogFrame(), "",
				ResourceMgr.getString(UnsharePinListActionHelper.class,
						"UnsharePinListActionHelper.NameExistsError.Header.text"),
				ResourceMgr.getString(UnsharePinListActionHelper.class,
						warningString, nameClashString) +
						ResourceMgr.getString(UnsharePinListActionHelper.class,
								"UnsharePinListActionHelper.NameExistsError.Question.text"));
	}

	public static class InlineRenameDialog extends RenameDialog
	{

		private IStringProperty mateNameProperty = null;
		private IReadOnlyNamedObject mateObj = null;

		public InlineRenameDialog(Frame parent, IReadOnlyNamedObject namedObj, IReadOnlyNamedObject mate, String title,
				String label, boolean useWarning, IProject proj)
		{
			super(parent, title);
			m_heading = null;
			m_name = namedObj.getName();
			m_namedObj = namedObj;
			m_useWarning = useWarning;
			m_label = label;
			m_project = proj;
			mateObj = mate;
			initDialog(true);
		}

		// Return null if valid; errmsg otherwise.  Check name is not empty and not a duplicate

		public String checkValidName(String newName, String oldname)
		{
			if (StringUtils.getTrimmed(newName) == null) {
				return "";
			}
			if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
				return ResourceMgr.getString(UnsharePinListActionHelper.class,
						"UnsharePinListActionHelper.NameTooLong",
						String.valueOf(CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH));
			}
			// Other than that, always return true - any name is valid
			return null;
		}

		// Need to override this too as we don't mind if the name is the same as the original.

		public String getNewName()
		{
			return getNewName(getNameProperty());
		}

		@Nullable public String getNewMateName()
		{
			return getNewName(mateNameProperty);
		}

		@Nullable private String getNewName(IStringProperty property)
		{
			if (isCancelled()) {
				return null;
			}
			String nn = property.getValue().trim();
			if (nn != null && nn.length() > 0) {
				return nn;
			}
			else {
				return null;
			}
		}

		public void initDialog(boolean buildPropertyPanel)
		{
			super.initDialog(mateObj == null);

			if (mateObj != null && buildPropertyPanel) {
				String label = ResourceMgr.getString(UnsharePinListActionHelper.class,
						"UnsharePinListActionHelper.Rename.Plug.label");
				mateNameProperty = addRenameStringField(label, mateObj, mateObj.getName());
				m_namesPanel = new PropertyPanel("DiagramAttrs", m_nameGroup);
				addAdditionalPanel(m_namesPanel);
			}
		}

		public IStringProperty getMateNameProperty()
		{
			return mateNameProperty;
		}
	}

	/**
	 * Used to hold on to usage info.  We can't rely on the usagemgr because it gets updated as soon as datamodel
	 * changes are done during the course of the action.
	 */
	public static class PinUsageInfo
	{

		private IUID pinUID;
		private IUID diagramUID;

		public PinUsageInfo(IUID pinUID, IUID diagramUID)
		{
			this.pinUID = pinUID;
			this.diagramUID = diagramUID;
		}

		@Nullable public IAbstractSchemPin getPinForceLoad()
		{
			IAbstractSchemPin pin = UIDMgr.getObjectOfType(pinUID, IAbstractSchemPin.class);
			if (pin == null) {
				ISchemDiagram diagram = UIDMgr.getObjectOfType(diagramUID, ISchemDiagram.class);
				if (diagram != null) {
					diagram.getBackground(); // force diagram load
					pin = UIDMgr.getObjectOfType(pinUID, IAbstractSchemPin.class);
				}
			}
			return pin;
		}
	}

	@Nullable protected PinListConnectionTransferer getPinListConnectionTransferer()
	{
		if (plConnectionTransfer == null) {
			if (cablePinList instanceof IDevice) {
				plConnectionTransfer =
						new DeviceConnectionTransferer(new HashSet<IPinList>(schemPinLists), cablePinList);
			}
			else if (cablePinList instanceof IConnector) {
				plConnectionTransfer =
						new ConnectorConnectionTransferer(new HashSet<IPinList>(schemPinLists), cablePinList);
			}
		}
		return plConnectionTransfer;
	}

	//TODO FEAT13786- Same function in PinListShareHelper also..Move to some common helper class
	protected void populatePinStackPinRepsMap(IAbstractPinIterator pinIter)
	{
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		for (IAbstractPinIterator pinit = pinIter; pinit.hasNext(); ) {
			Set<ISchemStackPin> stackPinUsages = new HashSet<ISchemStackPin>();
			IAbstractPin pin = pinit.getNext();
			for (IDesignSharedUsage usage : dwum.getUsages(pin)) {
				IBaseShareableDiagramObject diagObj = usage.getDiagramObject();
				if (diagObj == null) {
					ISchemDiagram diagram = design.getDiagram(usage.getDiagramUID());
					if (diagram != null) {
						diagram.getBackground();
						diagObj = usage.getDiagramObject();
					}
				}
				if (diagObj instanceof ISchemStackPin) {
					stackPinUsages.add((ISchemStackPin) diagObj);
				}
			}
			if (!stackPinUsages.isEmpty()) {
				pin_stackPinReps_Map.put(pin, stackPinUsages);
			}
		}
	}

	private void updateStackPinContents()
	{
		//For each of the cable pin that has stackpin representation,
		for (IAbstractPin oldPin : pin_stackPinReps_Map.keySet()) {
			//Get the corresponding new pin and if its owner is not shared,
			IAbstractPin newPin = (IAbstractPin) replicator.getNewObject(oldPin.getUID());
			if (newPin != null && !newPin.getOwner().isShared()) {
				for (ISchemStackPin stackPin : pin_stackPinReps_Map.get(oldPin)) {
					//just update those stackpin representations who belong to unshared instances
					if (!((IPinList) stackPin.getParent()).getConnectivity().isShared()) {
						stackPin.removePinFromStack(oldPin);
						stackPin.addPinToStack(newPin);
					}
				}
			}
		}
	}

	protected abstract class PinListConnectionTransferer
	{

		protected final Set<IPinList> owners;
		protected final chs.cof.logical.cable.IPinList connectivityPinList;

		protected Map<String, Collection<IAbstractPin>> transferableConnections = null;

		protected PinListConnectionTransferer(@NotNull Set<IPinList> schemPinLists
				, @NotNull chs.cof.logical.cable.IPinList connectivityPL)
		{
			owners = schemPinLists;
			connectivityPinList = connectivityPL;
		}

		protected Map<String, Collection<IAbstractPin>> getTransferableConnections()
		{
			return transferableConnections;
		}

		/**
		 * are there any connected pin for the given pin that can be transfered to the replicated pin
		 *
		 * @param pin the connectivity pin on the pin list we are unsharing.
		 *
		 * @return true if we can transfer some pin(s) mated with the connectivity pin.
		 */
		public boolean shouldTransferAnyPinConnection(IAbstractPin pin)
		{
			return transferAllowed(pin);
		}

		public void init()
		{
			constructTransferableConnections();
		}

		/**
		 * Can we transfer some connected pins of this pin to its replicated pin?
		 *
		 * @param pin the connectivity pin on the pin list we are unsharing.
		 *
		 * @return true if we can transfer some pin(s) mated with the connectivity pin.
		 */
		private boolean transferAllowed(IAbstractPin pin)
		{
			if (transferableConnections == null) {
				constructTransferableConnections();
			}
			for (String s : transferableConnections.keySet()) {
				if (pin.getUID().getString().equals(s)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * for each pin on the unshared schematics, calculate the mated pins that can be safely transfered to the
		 * corresponding replicated pin. The connections are transferable in these cases: a) No pin instance on the
		 * schematic pin list that is unsharing has an equal instance in any other place in the design and all the
		 * connectivity pins are available on the schematic pin list. This is typically the trivial case when there is
		 * only one instance of the pin list and we are unsharing it. b) There some other instances of the connectivity
		 * pin list but non of them contains the instances of the pins on the schematic pin list we are unsharing. if
		 * they do have some instances of these pins, we will not transfer the mated pin(s) because the original pins
		 * still exist.
		 */
		private void constructTransferableConnections()
		{
			if (transferableConnections == null) {
				transferableConnections = new HashMap<String, Collection<IAbstractPin>>();

				// this will meet case b.
				addEntriesForTransferableConnections();

				pupulateTransferableConnections();
			}
		}

		/**
		 * populate the set of mated pins that can be later transfered to point to the unshared replicates of the
		 * unsharable pin list.
		 */
		protected abstract void pupulateTransferableConnections();

		protected boolean isTransferablePinList(IConnector transferable)
		{
			for (IConnectorPin transferablePin : transferable.getConnectorPins()) {
				if (!isTransferablePin(transferablePin)) {
					return false;
				}
			}

			return true;
		}

		protected boolean isTransferablePin(IConnectorPin pin)
		{
			if (!pin.isMated()) {
				return true;
			}

			IAbstractPin unsharablePin = getTransferablePin(pin);

			for (String devicePin : getTransferableConnections().keySet()) {
				if (unsharablePin.getUID().getString().equals(devicePin)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * get the pin that is going to be transfered and point to the new pin. This should always be a connector pin.
		 *
		 * @param pin the pin that is going to be unshared.
		 *
		 * @return a pin that we should later reconnect to another pin.
		 */
		protected abstract IAbstractPin getTransferablePin(IConnectorPin pin);

		/**
		 * initializes the working set for pins that we may transfer mated pins for.
		 */
		protected void addEntriesForTransferableConnections()
		{
			for (IAbstractPin apin : connectivityPinList.getPinCollection()) {
				if (hasPotentialTransferableConnection(apin)) {
					putEmptyTransferableConnection(apin);
				}
			}
		}

		/**
		 * check if the mated pin(s) of the given pin may be considered for transferring to the unshared replicate. This
		 * will check the connectivity pins in the shared connectivity pin list, if that connectivity pin has all of its
		 * instances on the schematics we are going to unshare, then there is an opportunity to transfer the mated pins
		 * to point to the unshared pin.
		 *
		 * @param apin pin
		 *
		 * @return true if the connected pins may be transfered later, false otherwise
		 */
		private boolean hasPotentialTransferableConnection(IAbstractPin apin)
		{
			int usageCount = getPinUsagesCount(apin);
			for (IPinList schemOwner : owners) {
				for (IPin schemPin : schemOwner.getPins()) {
					IUID pinUID = schemPin.getConnectivityUID();
					if (pinUID != null) {
						if (pinUID.isEquiv(apin.getUID())) {
							usageCount--;
						}
					}
				}

				for (ISchemStackPin pinStack : schemOwner.getStackPins()) {
					if (pinStack.hasPin(apin)) {
						usageCount--;
					}
				}
			}
			return usageCount == 0;
		}

		/**
		 * add an entry for this pin so that it would be later populated with the mated pins that can be then transfered
		 * to poin on the unshared replicate.
		 *
		 * @param apin pin
		 */
		protected abstract void putEmptyTransferableConnection(IAbstractPin apin);

		/**
		 * get the number of schematic usages of the given connectivity pin within the design.
		 *
		 * @param apin pin
		 *
		 * @return number of schematic usages for the connectivity pin.
		 */
		protected int getPinUsagesCount(IAbstractPin apin)
		{
			List<PinUsageInfo> oldUIDs = getOldPinUsageInfoList(apin);
			return oldUIDs == null ? 0 : oldUIDs.size();
		}

		@Nullable protected IAbstractPin getPin(String uid)
		{
			chs.cof.logical.cable.IPinList owner = getConnectivityPinList();
			if (owner != null) {
				return owner.findPinById(FactoryMgr.getCommonFactory().constructUID(uid));
			}
			return null;
		}

		public abstract void transferPinConnections(IAbstractPin oldPin, IAbstractPin newPin);

		protected void transferConnectorPinConnection(IAbstractPin oldPin, IAbstractPin newPin,
				IAbstractPin pinToTransfer)
		{
			if (pinToTransfer != null) {
				oldPin.removeConnectedPin(pinToTransfer);
				pinToTransfer.forceConnection(newPin);
			}
		}

		protected abstract chs.cof.logical.cable.IPinList getConnectivityPinList();
	}

	protected class DeviceConnectionTransferer extends PinListConnectionTransferer
	{

		public DeviceConnectionTransferer(@NotNull Set<IPinList> schemPinLists
				, @NotNull chs.cof.logical.cable.IPinList connectivityPL)
		{

			super(schemPinLists, connectivityPL);
		}

		protected void putEmptyTransferableConnection(IAbstractPin apin)
		{
			getTransferableConnections().put(apin.getUID().getString(), new LinkedList<IAbstractPin>());
		}

		@Override public void transferPinConnections(IAbstractPin oldPin, IAbstractPin newPin)
		{
			Map<String, Collection<IAbstractPin>> connections = getTransferableConnections();
			if (connections != null) {
				for (Map.Entry<String, Collection<IAbstractPin>> entry : connections.entrySet()) {
					if (oldPin.getUID().getString().equals(entry.getKey())) {
						for (IAbstractPin tobeTransferred : entry.getValue()) {
							if (tobeTransferred instanceof IDevicePin) {
								transferDevicePinConnection(oldPin, newPin, (IDevicePin) tobeTransferred);
							}
							else {
								transferConnectorPinConnection(oldPin, newPin, tobeTransferred);
							}
						}
						break;
					}
				}
			}
		}

		private void transferDevicePinConnection(IAbstractPin oldPin, IAbstractPin newPin, IDevicePin pinToTransfer)
		{
			if (pinToTransfer != null && oldPin instanceof IDevicePin && newPin instanceof IDevicePin) {
				IDevicePin oldDevPin = (IDevicePin) oldPin;
				IDevicePin newDevPin = (IDevicePin) newPin;
				oldDevPin.setConnectedDevicePin(null);
				newDevPin.setConnectedDevicePin(pinToTransfer);
			}
		}

		/**
		 * we are now working with pins that do not have other instances on other schematic instances of the
		 * connectivity pin list. we now want to get the transferable pins on mated devices and/or connectors a)
		 * connectors: we should be able to transfer all pins on the connector but we have to make sure that all the
		 * mated connectivity pins on the connector are mated to those pins we have collected on the schem pin list b)
		 * devices: we should be able to transfer all mated device pins because device mating allows the n-to-n
		 * matings.
		 */
		protected void pupulateTransferableConnections()
		{
			Set<chs.cof.logical.cable.IPinList> transferableConnectors = getTransferableConnectors();

			for (Map.Entry<String, Collection<IAbstractPin>> entry : transferableConnections.entrySet()) {
				IDevicePin unsharablePin = getPin(entry.getKey());

				addTransferableConnectorPin(transferableConnectors, entry, unsharablePin);

				addTransferableDevicePin(entry, unsharablePin);
			}
		}

		/**
		 * get all the connectors with pins where if those pins are connected, they are connected to the device pins we
		 * have already collected before in transferableConnections
		 *
		 * @return connectors that can be transfered to point at the unshared device instance.
		 */
		private Set<chs.cof.logical.cable.IPinList> getTransferableConnectors()
		{
			Set<chs.cof.logical.cable.IPinList> connectors = new HashSet<chs.cof.logical.cable.IPinList>();
			IDevice device = getConnectivityPinList();
			if (device != null) {
				for (IConnector attachedConnector : device.getConnectors()) {
					if (isTransferablePinList(attachedConnector)) {
						connectors.add(attachedConnector);
					}
				}
			}
			return connectors;
		}

		private void addTransferableConnectorPin(Set<chs.cof.logical.cable.IPinList> transferableConnectors,
				Map.Entry<String, Collection<IAbstractPin>> entry, IDevicePin unsharablePin)
		{
			if (unsharablePin != null) {
				Collection<IAbstractPin> connectedPins = unsharablePin.getConnectedPins();
				for (IAbstractPin connectorPin : connectedPins) {
					if (connectorPin != null && transferableConnectors.contains(connectorPin.getOwner())) {
						entry.getValue().add(connectorPin);
					}
				}
			}
		}

		private void addTransferableDevicePin(Map.Entry<String, Collection<IAbstractPin>> transferableEntry,
				IDevicePin unsharablePin)
		{
			if (unsharablePin != null) {
				if (getConnectivityPinList().acceptsDeviceMating() && unsharablePin.getConnectedDevicePin() != null) {
					transferableEntry.getValue().add(unsharablePin.getConnectedDevicePin());
				}
			}
		}

		@Nullable protected IDevicePin getPin(String uid)
		{
			return (IDevicePin) super.getPin(uid);
		}

		protected IDevice getConnectivityPinList()
		{
			return (IDevice) connectivityPinList;
		}

		protected IAbstractPin getTransferablePin(IConnectorPin pin)
		{
			return pin.getConnectedPinForConnectorPin();
		}
	}

	protected class ConnectorConnectionTransferer extends PinListConnectionTransferer
	{

		public ConnectorConnectionTransferer(@NotNull Set<IPinList> schemPinLists
				, @NotNull chs.cof.logical.cable.IPinList connectivityPL)
		{

			super(schemPinLists, connectivityPL);
		}

		/**
		 * transferable connections are those pin connections that will not result in a device/connector pin mated to 2
		 * different connector pins. if all the instances of pins (that have connections) are on the schematic connector
		 * then we consider its pins in the transferable connections. if there is at least 1 pin that has a connected
		 * pin on another schematic instance then we can't transfer connections because the shared pinlist is still
		 * mated with the mated pins list even if the pins on the schematic connector are not the full set of the
		 * connectivity pins.
		 */
		@Override protected void pupulateTransferableConnections()
		{
			IConnector unsharable = getConnectivityPinList();
			if (unsharable != null && isTransferablePinList(unsharable)) {
				for (Map.Entry<String, Collection<IAbstractPin>> entry : transferableConnections.entrySet()) {
					IConnectorPin unsharablePin = getPin(entry.getKey());
					if (unsharablePin != null) {
						entry.getValue().add(unsharablePin.getConnectedPinForConnectorPin());
					}
				}
			}
		}

		@Nullable @Override protected IAbstractPin getTransferablePin(IConnectorPin pin)
		{
			for (String pinUID : getTransferableConnections().keySet()) {
				if (pin.getUID().getString().equals(pinUID)) {
					return pin;
				}
			}
			return null;
		}

		@Override protected void putEmptyTransferableConnection(IAbstractPin apin)
		{
			getTransferableConnections().put(apin.getUID().getString(), new LinkedList<IAbstractPin>());
		}

		@Override public void transferPinConnections(IAbstractPin oldPin, IAbstractPin newPin)
		{
			Map<String, Collection<IAbstractPin>> connections = getTransferableConnections();
			if (connections != null) {
				for (Map.Entry<String, Collection<IAbstractPin>> entry : connections.entrySet()) {
					if (oldPin.getUID().getString().equals(entry.getKey())) {
						for (IAbstractPin toBeTransferred : entry.getValue()) {
							transferConnectorPinConnection(oldPin, newPin, toBeTransferred);
						}
						return;
					}
				}
			}
		}

		protected boolean isTransferablePin(IConnectorPin pin)
		{
			IAbstractPin transferablePin = getTransferablePin(pin);
			return transferablePin == null || super.isTransferablePin(pin);
		}

		@Override protected IConnector getConnectivityPinList()
		{
			return (IConnector) connectivityPinList;
		}

		@Nullable protected IConnectorPin getPin(String uid)
		{
			return (IConnectorPin) super.getPin(uid);
		}
	}

	protected void setSymbolRef(IPinList thePinList,
			ISharedPinList spl, chs.cof.logical.cable.IPinList newPinList, boolean bAllUnshare)
	{
		IUID uid = thePinList.getUID();
		ISymbolDef symDef = oldSymDefMap.get(uid);
		if (symDef != null) {
			ISymbolRef symref = spl.getSymbolRef(symDef.getUID(), oldSymInstNumMap.get(uid));
			//We are setting symbolRef on a single instanceof unshared device. Replicator might have copied all the
			// symbol refs of the source shared object. We don't want all of them. So, clear them

			if (bAllUnshare) {
				if (newPinList.canMaintainMultipleSymbols()) {
					// we can't create an analysis block for this symbol here because suppose that this symbol
					// was attached a different analysis model.
					newPinList.addSymbolRefIfCanMaintainMultipleSymbols(symref);
				}
				else {
					newPinList.setSymbolRef(symref);
				}
			}
			else {
				newPinList.setSymbolRef(symref);
			}
		}
	}

	protected static void copySymbolPinRefs(IPinList thePinList, ISharedPinList spl, ISharedPin spin,
			IAbstractPin cablePin)
	{
		SymbolUtils.copySymbolPinReference(thePinList, spl, spin, cablePin);

		// Clear the symbol pins on each schem pin.
		if (thePinList != null) {
			//FEAT00013786: stack pins are expected in symbols
			for (IPin schemPin : thePinList.getPins()) {
				if (schemPin.getSymbolPin() != null) {
					schemPin.setSymbolPin(null);
				}
			}
		}
	}

	protected Map<IPinList, IPin> getSymbolsForSharedPin(Collection<IPinList> schems,
			ISharedPinList spl, ISharedPin spin)
	{
		Map<IPinList, IPin> pinlistToSymbolPin = new HashMap<IPinList, IPin>();

		for (IPinList schem : schems) {
			if (schem.getSymbolDefUID() != null) {
				// if it is symbolled.
				//FEAT00013786: stack pins are not expected in symbols
				for (IPin p : schem.getPins()) {
					if (p.getSharedObject() == spin) {
						IPin symbolPin = null;
						if (spl != null) {
							symbolPin =
									spl.getSymbolPin(schem.getSymbolDefUID(), schem.getSymbolInstanceNumber(), spin);
						}
						if (symbolPin != null && !pinlistToSymbolPin.containsValue(symbolPin)) {
							pinlistToSymbolPin.put(schem, symbolPin);
						}
					}
				}
			}
		}

		return pinlistToSymbolPin;
	}

	protected class UnshareSingleInstance
	{

		private chs.cof.logical.cable.IPinList unsharedPinListMate;
		private ISharedPinList spl;
		private ISharedConnector splMate;
		private IPinList schemPinList;
		private IPinList schemMate;
		private chs.cof.logical.cable.IPinList unsharedPinList;

		public UnshareSingleInstance(chs.cof.logical.cable.IPinList unsharedPinListMate, ISharedPinList spl,
				ISharedConnector splMate,
				IPinList schemPinList, IPinList schemMate)
		{
			this.unsharedPinListMate = unsharedPinListMate;
			this.spl = spl;
			this.splMate = splMate;
			this.schemPinList = schemPinList;
			this.schemMate = schemMate;
		}

		public chs.cof.logical.cable.IPinList getUnsharedPinList()
		{
			return unsharedPinList;
		}

		public chs.cof.logical.cable.IPinList getUnsharedPinListMate()
		{
			return unsharedPinListMate;
		}

		public UnshareSingleInstance invoke()
		{

			validateInputData();

			// If there are multiple instances of this shared object then we need to leave the connectivity where it
			// is (i.e. refering back to the shared obj) and replicate connectivity so we have a local one.
			unsharedPinList = replicator.replicatePinListConnectivity(cablePinList, true, false, true);
			// for some unknown reason the replicator doesn't set the name for us
			// if the user chose something else it will be set below
			replicator.replicateCopyableObject(spl, unsharedPinList);
			unsharedPinList.setName(cablePinList.getName());
			if (schemMate != null) {
				unsharedPinListMate = replicator.replicatePinListConnectivity(cablePinListMate, true, false, true);
				replicator.replicateCopyableObject(splMate, unsharedPinListMate);
				assert cablePinListMate != null;
				unsharedPinListMate.setName(cablePinListMate.getName());
				//dts0100601783-Application errors on calling Share Into at an inline.
				//mate the inline halfs together.
				if (unsharedPinList instanceof IGenericInlineConnector
						&& unsharedPinListMate instanceof IGenericInlineConnector) {
					((IGenericInlineConnector) unsharedPinList)
							.addMate((IGenericInlineConnector) unsharedPinListMate);
					((IGenericInlineConnector) unsharedPinListMate)
							.addMate((IGenericInlineConnector) unsharedPinList);
				}
			}
			if (unsharedPinList instanceof IDevice) {
				Replicator.copyFootprintRef(spl, (IFootprintable) unsharedPinList);
			}

			// For shared pinlists the symbol is stored on the schem object as each instance of the connectivity object
			// could use a different schem.  When we unshare, this needs to be put on the unshared connectivity object.
			setSymbolRef(schemPinList, spl, unsharedPinList, false);

			schemPinList.setConnectivity(unsharedPinList);
			IConnectivity connectivity = design.getConnectivity();
			assert connectivity != null;
			connectivity.addPinList(unsharedPinList);
			if (unsharedPinListMate != null) {
				connectivity.addPinList(unsharedPinListMate);
				((IGenericInlineConnector) cablePinList).addMate(cablePinListMate);
				assert cablePinListMate != null;
				cablePinListMate.addMate((IGenericInlineConnector) cablePinList);
				assert schemMate != null;
				schemMate.setConnectivity(unsharedPinListMate);
			}

			// Now do the same for each of the pins - can't use the replicator as it uses references back to the
			// original shared pin
			//backupInternalLinkPinRefs(cablePinList);
			replicatBlocks(unsharedPinList);
			// Phase 1: Replicate device connector (including backshells) without their pins.
			// This must happen before replicatePins so that backshell termination pins on device connectors
			// can find their destination backshell via findDestinationBackshell.
			replicateDeviceConnectorsWithoutPins(cablePinList, unsharedPinList);
			Set<IAbstractPin> unplacedPinsToDelete = new HashSet<>();
			replicatePins(schemPinList, cablePinList, unsharedPinList, spl, unplacedPinsToDelete);
			AnalysisUtils.replicateSVModelMapping(cablePinList, unsharedPinList);
			if (schemMate != null) {
				replicatePins(schemMate, cablePinListMate, unsharedPinListMate, splMate, unplacedPinsToDelete);
				AnalysisUtils.replicateSVModelMapping(cablePinListMate, unsharedPinListMate);
				if (unsharedPinList instanceof IGenericInlineConnector) {
					cablePinListMate.removeMate((IGenericInlineConnector) unsharedPinList);
				}
				if (cablePinList instanceof IGenericInlineConnector &&
						unsharedPinListMate instanceof IGenericInlineConnector) {
					((IGenericInlineConnector) cablePinList).removeMate((IGenericInlineConnector) unsharedPinListMate);
				}
			}
			if (cablePinList instanceof IDevice && ((IDevice) cablePinList).hasInternalConnectivity() &&
					unsharedPinList instanceof IDevice) {
				replicateInternalConnectivity(cablePinList, unsharedPinList, schemPinList, replicator
				);
			}
			updateStackPinContents();
			PinListHelper.removeUnreferencedBlocks(unsharedPinList);
			// Phase 2: Replicate device connector pins after device pins have been replicated.
			// This ensures linkDevConnPinToDevicePin can form the connection between
			// new device connector pins and new device pins.
			replicateDeviceConnectorPins(cablePinList);

			// TODO jacobt FEAT13040 (or melmorsy?) : does this have to be reinstated ?
			// if not then we can remove it and also the unused replicateHarnessConnectors method
			//if(PinListHelper.isHarnessFootprintedAndAllowAutoCreation(cablePinList))
			//{
			//	replicateHarnessConnectors(cablePinList,  unsharedPinList);
			//}

			// Now update any connections between this pinlist and its connected pinlists
			updateConnectedPinlistOwnership(cablePinList, schemPinList, unsharedPinList,
					design.getDesignWideUsageMgr());
			//delete unplaced pins at last.
			unplacedPinsToDelete.forEach(p -> p.delete());
			return this;
		}

		protected void validateInputData()
		{
			// if we're in this branch, it should only be for selection of a single instance
			assert schemPinLists.size() == 1;
		}
	}

	public void setSchemPinListsForUT(Collection<IPinList> pinLists)
	{
		schemPinLists = pinLists;
	}
}


