/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2024 Siemens
 */

package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.UndoDisableForSharedObjectSave;
import chs.caplets.logic.function.FunctionController;
import chs.caplets.logic.helpers.DeletabilityChecker;
import chs.caplets.logic.helpers.IDeletabilityChecker;
import chs.caplets.logic.merge.ConductorMerger;
import chs.caplets.shared.DataTransfer;
import chs.caplets.shared.IGfxDisplayableModel;
import chs.cof.draw.IBoundedText;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxGroup;
import chs.cof.draw.IGfxGroupable;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.ISheet;
import chs.cof.draw.IText;
import chs.cof.drawplus.IAnchorable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IDecorative;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IPropertiedCommentSymbol;
import chs.cof.drawplus.IPropertiedGfxGroup;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IPropertiedText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.layout.ISymbolDatumLayout;
import chs.cof.drawplus.table.IBasicTable;
import chs.cof.drawplus.table.ITableDataProvider;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.AssemblyTypeEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IChainSegment;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILayoutXYDimension;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IMultipleConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinOwner;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedGroundDevice;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IBaseModuledObject;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.cofUtils.scrubber.CommonInvalidStateScrubber;
import chs.cofUtils.scrubber.CommonScrubbableChecker;
import chs.cofUtils.scrubber.ICommonInvalidStateHandler;
import chs.cog.IPersistenceSession;
import chs.common.IAssembledObject;
import chs.common.IDesignContainer;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.INamedObject;
import chs.common.IObjectFilter;
import chs.common.IParameterized;
import chs.common.IProjectPreferenceMgr;
import chs.common.IReadOnlyNamedObject;
import chs.common.IShortDescriptionObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.RefreshStatusEnum;
import chs.common.UIDUtils;
import chs.common.attr.IAttributeTypes;
import chs.ctf.print.PrintRegionHelper;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.IXMLTags;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.ui.OptionalDialog;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.PortHelper;
import chs.utility.PrintRegionUtils;
import chs.utility.ProjectHelper;
import chs.utility.Replicator;
import chs.utility.attr.AttributeUtils;
import chs.utility.gfx.IGfxObjectVisitor;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.ILockableLogicObject;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.StackedPinHelper;
import chs.utility.logic.DaisyChainCreationHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.PinUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.SymbolInstanceHelper;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * * This class contains caplet specific implementations of the IDataTransfer functions used for transfering data *
 * around using the clipboard. * * The current implementation of this simply uses the Replictor functionality along with
 * the current select set * to copy/paste objects.  The first release will not have cut/copy/paste functionality outside
 * of CAF. In other words, * this implementation is not going to use the clipboard with XML. The reason for this is
 * simply speed of * implementation.  Additionally, there is not an outstanding requirement to be able to paste outside
 * of a Java * environment at this time. *
 */
public class LogicDataTransfer extends DataTransfer
{

	/**
	 * Used to make sure that a conductor isn't copied twice.
	 */
	private Set<IConductor> m_duplicatedConductors;

	/**
	 * Used to remember which replicated conductors were only partially replicated (not all segments were selected.)
	 */
	private List<IConductor> m_partialConductors;

	/**
	 * Used to make sure that a conductor isn't copied twice.
	 */
	private Set<IHighwaySchematic> m_duplicatedHighways;

	private Set<String> m_sharedNames;

	private Map<IPropertiedGraphic, IDiagramObject> m_associatedGraphicObjects;

	/**
	 * Used to cache copied multicores
	 */
	protected Set<IMulticore> m_multicores;
	private Set<IPinList> m_devices;

	private TextScalingVisitor m_textScalingVisitor = new TextScalingVisitor();

	private static boolean enableWarnings = true; // prevent unit test hang with warning prompt

	//These are used when many paste operations are performed for the same copy.
	protected Map<IConnectivityRef, ILogicObject> m_recorderdConnectivity;
	protected Map<ILogicObject, ILogicObject> m_newVsOriginalConnectivity;
	@NotNull private Map<ILogicObject, ISharedObject> m_newVsSharedConnectivity;
	protected SetMap<ISchemStackPin, IAbstractPin> m_stackPinRecorderdConnectivity;

	//	private Map<IBlock, IBlock> m_newBlockVsOldBlock;
	private Map<ISchemInternalLink, IInternalLink> m_newSchemLinkVsOldCableLink;

	private Map<ISchemSector, ISchemSector> m_newSchemSectorVsOldSchemSector;

	@Nullable protected IECAttributeResolver attributeResolver;
	private SelectSet m_pastedSelectSet = new SelectSet();

	protected List<IUIDObject> extendedDeletionList = new ArrayList<>();
	protected List<DisconnectionPair<?,?>> disconnectionsToProcess = new ArrayList<>();

	private SharedObjectAvailabilityChecker availabilityChecker;

	public LogicDataTransfer()
	{
		m_partialConductors = new ArrayList<IConductor>();
		m_multicores = new HashSet<IMulticore>();
		m_devices = new HashSet<IPinList>();
		m_sharedNames = new HashSet<>();
		m_recorderdConnectivity = null;
		m_newVsOriginalConnectivity = null;
		m_newVsSharedConnectivity = new HashMap<>();
		attributeResolver = null;
	}

	/**
	 * Disable warning dialogs generated by this object.
	 * <p>
	 * This is to prevent unit tests hanging with the "Copying of shared devices and connectors is not allowed" warning,
	 * but really we should refactor the code such that datamodel changes are done by commands that don't directly
	 * invoke the UI.
	 * <p>
	 * So then we can get a much higher unit test coverage without having to resort to hacks like this.
	 *
	 * @param enabled true to enable warnings
	 */
	public static void setEnableWarnings(boolean enabled)
	{
		enableWarnings = enabled;
	}

	@Override public boolean doCAFCopy(@NotNull ICapletController controller)
	{
		ISchemDiagram diagram = getSourceDiagram();
		if (diagram != null) {
			attributeResolver = new IECAttributeResolver(diagram);
		}
		return super.doCAFCopy(controller);
	}

	@Override public boolean doCAFCut(@NotNull ICapletController controller)
	{
		if (!canDeleteObjects(controller)) {
			String failReason =
					ResourceMgr.getString(LogicDataTransfer.class, "LogicDataTransfer.cutAction.failed.reason");
			CAFUtils.getInstance().sendApplicationMessage(HTMLHelper.color(IXMLTags.RED, failReason));
			doCleanUp();
			return false;
		}

		performPreCut();

		doCAFCopy(controller);
		//assign names on cut - for ghost graphics
		assignOldNameAndShortDesc();
		handleSchemSectors();
		collectSharedObjectsName();

		if (!deleteSelectedObjects()) {
			doCleanUp();
			return false;
		}

		performDisconnections();

		performPostCut();

		//controller.getSelectMgr().getPreSelections().clear();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		return true;
	}

	private void performDisconnections()
	{
		for (DisconnectionPair<?,?> disconnection : disconnectionsToProcess) {
			disconnection.disconnect();
		}
	}

	private abstract static class DisconnectionPair<X extends ILogicObject, Y extends ILogicObject>
	{

		protected X object1;
		protected Y object2;

		private DisconnectionPair(@NotNull X logicObject1, @NotNull Y logicObject2)
		{
			object1 = logicObject1;
			object2 = logicObject2;
		}

		@NotNull public X getObject1()
		{
			return object1;
		}

		@NotNull public Y getObject2()
		{
			return object2;
		}

		abstract boolean canDisconnectAfterRefresh();
		abstract void disconnect();
	}

	private class ConductorPinDisconnectionPair extends DisconnectionPair<chs.cof.logical.cable.IConductor, IAbstractPin>
	{

		private ConductorPinDisconnectionPair(@NotNull chs.cof.logical.cable.IConductor conductor, @NotNull IAbstractPin pin)
		{
			super(conductor, pin);
		}

		@Override boolean canDisconnectAfterRefresh()
		{
			Set<IConductor> schemConductors = getSchemConductorsInSelection(object1);
			return !hasOtherReps(object2, object1, schemConductors);
		}

		@Override void disconnect()
		{
			object2.removeConductor(object1);
		}
	}

	private class PinConductorDisconnectionPair extends DisconnectionPair<IAbstractPin, chs.cof.logical.cable.IConductor>
	{

		private PinConductorDisconnectionPair(@NotNull IAbstractPin pin,
				@NotNull chs.cof.logical.cable.IConductor conductor)
		{
			super(pin, conductor);
		}

		@Override boolean canDisconnectAfterRefresh()
		{
			Set<IAbstractSchemPin> schemPins = getSchemPinsInSelection(object1);
			return !hasOtherRepsOfPin(object1, object2, schemPins);
		}

		@Override void disconnect()
		{
			object1.removeConductor(object2);
		}
	}

	private class PinToPinDisconnectionPair extends DisconnectionPair<IAbstractPin, IAbstractPin>
	{

		private PinToPinDisconnectionPair(@NotNull IAbstractPin pin1, @NotNull IAbstractPin pin2)
		{
			super(pin1, pin2);
		}

		@Override boolean canDisconnectAfterRefresh()
		{
			Set<IAbstractSchemPin> schemPins = getSchemPinsInSelection(object1);

			ILogicDesign logicDesign = CommonUtils.cast(getSourceDesign(), ILogicDesign.class);
			if (logicDesign == null) {
				return false;
			}

			return !ConnectionHelper
					.areConnectedInOtherInstances(logicDesign, object1, object2, schemPins);
		}

		@Override void disconnect()
		{
			object1.removeConnectedPin(object2);
		}
	}

	private class DevicePinToDevicePinDisconnectionPair extends DisconnectionPair<IDevicePin, IDevicePin>
	{

		private DevicePinToDevicePinDisconnectionPair(@NotNull IDevicePin pin1, @NotNull IDevicePin pin2)
		{
			super(pin1, pin2);
		}

		@Override boolean canDisconnectAfterRefresh()
		{
			Set<IAbstractSchemPin> schemPins = getSchemPinsInSelection(object1);
			Set<IPin> schemDevicePins = schemPins.stream().filter(schemPin -> schemPin instanceof IPin)
						.map(schemPin -> (IPin)schemPin)
						.collect(Collectors.toSet());

			return !ConnectionHelper.hasOtherInstancesForConnection(object1, schemDevicePins);
		}

		@Override void disconnect()
		{
			object1.setConnectedDevicePin(null);
		}
	}

	private boolean lockObjectsToBeDisconnected()
	{
		ILogicDesign logicDesign = CommonUtils.cast(getSourceDesign(), ILogicDesign.class);
		if (logicDesign == null) {
			return false;
		}

		if (!logicDesign.isUnderConcurrentEdit()) {
			return true;
		}

		List<ILogicObject> logicObjectsToLock = new ArrayList<>();
		for (DisconnectionPair<?,?> disconnection : disconnectionsToProcess) {
			logicObjectsToLock.add(disconnection.getObject1());
			logicObjectsToLock.add(disconnection.getObject2());
		}

		Set<ISchemDiagram> diagramsToBeLocked = new HashSet<>();
		for (ILogicObject logicObject : logicObjectsToLock) {
			logicDesign.getDesignWideUsageMgr().getUsageDiagrams(logicObject).stream()
					.filter(aDiag -> !aDiag.isEditable()).collect(Collectors.toCollection(() -> diagramsToBeLocked));
		}

		logicDesign.lockDiagrams(diagramsToBeLocked);
		Collection<ISchemDiagram> diagramsNotLocked =
				diagramsToBeLocked.stream().filter(aDiagram -> !aDiagram.isEditable())
						.collect(Collectors.toList());
		if (!diagramsNotLocked.isEmpty()) {
			String diagramNames = diagramsNotLocked.stream().map(diagram -> diagram.getName())
					.collect(Collectors.joining(","));
			String key = "LogicDataTransfer.cutAction.lockDiagramsFailed";
			String message = ResourceMgr.getString(getClass(), key, diagramNames);
			CAFUtils.getInstance().sendApplicationMessage(message);
			return false;
		}

		Collection<IUID> lockFailedUIDs =
				LogicObjectLockFinder.tryEdit(logicDesign, logicObjectsToLock);

		if (!lockFailedUIDs.isEmpty()) {
			Set<String> logicObjectNames = new HashSet<>();
			logicObjectsToLock.stream().filter(logicObject -> !logicObject.isEditable())
					.forEach(logicObject -> {
						if (logicObject instanceof ILockableLogicObject) {
							ILogicObject rootLockable = ((ILockableLogicObject) logicObject).getRootLockable();
							if (rootLockable != null) {
								logicObjectNames.add(rootLockable.getName());
							}
						}
					});
			String names = logicObjectNames.stream().collect(Collectors.joining(","));
			String key = "LogicDataTransfer.cutAction.lockObjectsFailed";
			String message = ResourceMgr.getString(getClass(), key, names);
			CAFUtils.getInstance().sendApplicationMessage(message);
			return false;
		}
		// ensure that disconnects can still happen after refresh
		Set<DisconnectionPair<?, ?>> invalidDisconnections = disconnectionsToProcess.stream()
				.filter(disconnectionPair -> !disconnectionPair.canDisconnectAfterRefresh())
				.collect(Collectors.toSet());
		disconnectionsToProcess.removeAll(invalidDisconnections);
		return true;
	}

	private void handleSchemSectors()
	{
		for (Map.Entry<ISchemSector, ISchemSector> entry : m_newSchemSectorVsOldSchemSector.entrySet()) {
			ISchemSector newSchemSector = entry.getKey();
			ISchemSector oldSchemSector = entry.getValue();
			//set the name
			newSchemSector.setName(oldSchemSector.getName());
			//set the short desc
			String shortDescription = oldSchemSector.getShortDescription();
			if (shortDescription != null) {
				newSchemSector.setShortDescription(shortDescription);
			}
		}
	}

	private void performPreCut()
	{
		clearPasteBuffer();
		m_underCut = true;
		m_cutInProgress = true;
		m_overrideBufferClear = true;
		m_sharedNames.clear();
	}

	private void collectSharedObjectsName()
	{
		SelectedUIDObjectIterator objectIterator = getCurrentSelection().getSelectedUIDObjects();
		while (objectIterator.hasNext()) {
			IUIDObject uidObject = objectIterator.getNext();
			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(uidObject);
			if (logicObject != null && logicObject.isShared()) {
				String logicObjectName = logicObject.getName();
				if (logicObject instanceof IGenericPin) {
					chs.cof.logical.cable.IPinList pinList = ((IGenericPin) logicObject).getOwner();
					if (pinList != null) {
						logicObjectName = pinList.getName();
					}
				}
				m_sharedNames.add(logicObjectName);
			}
		}
	}

	@NotNull public Set<String> getSharedObjectsUnderCut()
	{
		return m_sharedNames;
	}

	private void performPostCut()
	{
		m_overrideBufferClear = false;
		toBeDeleted.clear();
		extendedDeletionList.clear();
		disconnectionsToProcess.clear();
	}

	private boolean deleteSelectedObjects()
	{
		populateObjectsToBeDeleted();
		populatePossibleDisconnections();
		if (!lockObjectsToBeDisconnected()) {
			return false;
		}

		CutDeletionHandler deletionHandler = new CutDeletionHandler(toBeDeleted, getSourceDiagram());
		return deletionHandler.delete();
	}

	private void populatePossibleDisconnections()
	{
		List<IAbstractSchemPin> schemPinsToDelete = toBeDeleted.stream().filter(object -> object instanceof IPinList)
				.flatMap(pinList -> ((IPinOwner) pinList).getAllPins().stream())
				.collect(Collectors.toList());
		extendedDeletionList.addAll(toBeDeleted);
		extendedDeletionList.addAll(schemPinsToDelete);

		SetMap<IAbstractPin, IAbstractSchemPin> connVsSchemPins = new SetMap<>();
		SetMap<chs.cof.logical.cable.IConductor, IConductor> connVsSchemConductors = new SetMap<>();
		Set<ILogicObject> impactedConnObjects = new HashSet<>();

		collectImpactedObjectInfo(impactedConnObjects, connVsSchemPins, connVsSchemConductors);

		for (ILogicObject logicObject : impactedConnObjects) {

			IAbstractPin pin = CommonUtils.cast(logicObject, IAbstractPin.class);
			if (pin != null) {
				processPinToCalculateDisconnects(impactedConnObjects, pin, connVsSchemPins.getSet(pin));
				continue;
			}

			chs.cof.logical.cable.IConductor conductor = CommonUtils.cast(logicObject,
					chs.cof.logical.cable.IConductor.class);
			if (conductor != null) {
				processConductorToCalculateDisconnects(impactedConnObjects, conductor, connVsSchemConductors.getSet(conductor));
			}
		}
	}

	private void collectImpactedObjectInfo(@NotNull Set<ILogicObject> impactedConnObjects,
			@NotNull SetMap<IAbstractPin, IAbstractSchemPin> connVsSchemPins,
			@NotNull SetMap<chs.cof.logical.cable.IConductor, IConductor> connVsSchemConductors)
	{
		for (IUIDObject uidObject : extendedDeletionList) {
			if (uidObject instanceof IMultipleConnectivityRef) {
				Collection<? extends ILogicObject> allConnectivities =
						((IMultipleConnectivityRef) uidObject).getAllConnectivity();
				impactedConnObjects.addAll(allConnectivities);
				if (uidObject instanceof IAbstractSchemPin) {
					allConnectivities
							.forEach(o -> connVsSchemPins.add((IAbstractPin) o, (IAbstractSchemPin) uidObject));
				}
			}
			else {
				ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(uidObject);
				if (logicObject instanceof IAbstractPin) {
					IAbstractPin abstractPin = (IAbstractPin) logicObject;
					impactedConnObjects.add(abstractPin);
					if (isSchemPinOf(uidObject, abstractPin)) {
						connVsSchemPins.add(abstractPin, (IAbstractSchemPin) uidObject);
					}
				}
				if (logicObject instanceof chs.cof.logical.cable.IConductor) {
					chs.cof.logical.cable.IConductor conductor = (chs.cof.logical.cable.IConductor) logicObject;
					impactedConnObjects.add(conductor);
					if (isSchemConductorOf(uidObject, conductor)) {
						connVsSchemConductors.add(conductor, (IConductor) uidObject);
					}
				}
			}
		}
	}

	private void processPinToCalculateDisconnects(@NotNull Set<ILogicObject> impactedConnObjects,
			@NotNull IAbstractPin pin, @NotNull Set<IAbstractSchemPin> schemPins)
	{
		ILogicDesign logicDesign = CommonUtils.cast(getSourceDesign(), ILogicDesign.class);
		if (logicDesign == null) {
			return;
		}

		for (chs.cof.logical.cable.IConductor conductor : pin.getConductorsAsSet()) {
			if (!impactedConnObjects.contains(conductor)) {
				if (!hasOtherRepsOfPin(pin, conductor, schemPins)) {
					disconnectionsToProcess.add(new PinConductorDisconnectionPair(pin, conductor));
				}
			}
		}
		for (IAbstractPin connectedPin : pin.getConnectedPins()) {
			if (!impactedConnObjects.contains(connectedPin)) {
				if (!ConnectionHelper
						.areConnectedInOtherInstances(logicDesign, pin, connectedPin, schemPins)) {
					disconnectionsToProcess.add(new PinToPinDisconnectionPair(pin, connectedPin));
				}
			}
		}
		if (pin instanceof IDevicePin) {
			IDevicePin connectedDevicePin = ((IDevicePin) pin).getConnectedDevicePin();
			if (connectedDevicePin != null && !impactedConnObjects.contains(connectedDevicePin)) {
				Set<IPin> schemDevicePins = schemPins.stream().filter(schemPin -> schemPin instanceof IPin)
						.map(schemPin -> (IPin)schemPin)
						.collect(Collectors.toSet());
				if (!ConnectionHelper
						.hasOtherInstancesForConnection((IDevicePin)pin, schemDevicePins)) {
					disconnectionsToProcess
							.add(new DevicePinToDevicePinDisconnectionPair((IDevicePin) pin, connectedDevicePin));
				}
			}
		}
	}

	@NotNull private Set<IAbstractSchemPin> getSchemPinsInSelection(@NotNull IAbstractPin pin)
	{
		return extendedDeletionList.stream()
				.filter(obj -> isSchemPinOf(obj, pin))
				.map(obj -> (IAbstractSchemPin)obj)
				.collect(Collectors.toSet());
	}

	private boolean isSchemPinOf(@NotNull IUIDObject object, @NotNull IAbstractPin pin)
	{
		if (object instanceof IAbstractSchemPin) {
			if (object instanceof IPin && ((IConnectivityRef) object).getConnectivity() == pin) {
				return true;
			}
			return object instanceof ISchemStackPin && ((ISchemStackPin) object).getAllConnectivity().contains(pin);
		}
		return false;
	}

	private void processConductorToCalculateDisconnects(@NotNull Set<ILogicObject> impactedConnObjects,
			@NotNull chs.cof.logical.cable.IConductor conductor, @NotNull Set<IConductor> schemConductors)
	{
		conductor.getAllPins().stream()
				.filter(pin -> !impactedConnObjects.contains(pin)) // partial selection
				.filter(pin -> !hasOtherReps(pin, conductor, schemConductors)) // no other representation where they are schematically connected
				.forEach(pin -> disconnectionsToProcess.add(new ConductorPinDisconnectionPair(conductor, pin)));
	}

	@NotNull private Set<IConductor> getSchemConductorsInSelection(@NotNull chs.cof.logical.cable.IConductor conductor)
	{
		return extendedDeletionList.stream().filter(obj -> obj instanceof IConductor)
				.filter(object -> ((IConnectivityRef) object).getConnectivity() == conductor)
				.map(obj -> (IConductor) obj)
				.filter(schemCond -> isNotPartiallySelected(schemCond))
				.collect(Collectors.toSet());
	}

	private boolean isSchemConductorOf(@NotNull IUIDObject object, @NotNull chs.cof.logical.cable.IConductor conductor)
	{
		return object instanceof IConductor && ((IConnectivityRef) object).getConnectivity() == conductor &&
				isNotPartiallySelected((IConductor) object);
	}

	private boolean isNotPartiallySelected(@NotNull IConductor schemCond)
	{
		Collection<ISegment> allConductorSegments =
				CollectionUtils.getObjectList(schemCond.getObjects(), ISegment.class);
		boolean allSelected = extendedDeletionList.containsAll(allConductorSegments);
		boolean noneSelected = extendedDeletionList.stream().noneMatch(obj -> allConductorSegments.contains(obj));
		return allSelected || noneSelected;
	}

	private boolean hasOtherReps(@NotNull IAbstractPin pin, @NotNull chs.cof.logical.cable.IConductor cond,
			@NotNull Set<IConductor> schemConductorsToExclude)
	{
		ILogicDesign logicDesign = CommonUtils.cast(getSourceDesign(), ILogicDesign.class);
		if (logicDesign == null) {
			return false;
		}
		IDesignWideUsageMgr dwum = logicDesign.getDesignWideUsageMgr();
		if (schemConductorsToExclude.size() == dwum.getDesignSharedUsageCount(cond)) {
			return false;
		}

		Set<IAbstractSchemPin> schemPinsToExclude = schemConductorsToExclude.stream()
				.flatMap(conductor -> conductor.getPins().stream())
				.collect(Collectors.toSet());
		return ConnectionHelper.hasMultipleConnections(pin, schemPinsToExclude, cond, schemConductorsToExclude, null);
	}

	private boolean hasOtherRepsOfPin(@NotNull IAbstractPin pin, @NotNull chs.cof.logical.cable.IConductor cond,
			@NotNull Set<IAbstractSchemPin> schemPinsToExclude)
	{
		ILogicDesign logicDesign = CommonUtils.cast(getSourceDesign(), ILogicDesign.class);
		if (logicDesign == null) {
			return false;
		}
		IDesignWideUsageMgr dwum = logicDesign.getDesignWideUsageMgr();
		if (schemPinsToExclude.size() == dwum.getDesignSharedUsageCount(pin)) {
			return false;
		}

		Set<IConductor> schemConductorsToExclude = schemPinsToExclude.stream()
				.filter(schemPin -> schemPin instanceof IPin)
				.map(schemPin -> (IPin) schemPin)
				.flatMap(schemPin -> schemPin.getConductors().stream())
				.collect(Collectors.toSet());
		return ConnectionHelper.hasMultipleConnections(pin, schemPinsToExclude, cond, schemConductorsToExclude, null);
	}

	private void populateObjectsToBeDeleted()
	{
		SelectedUIDObjectIterator cutObjIter = getCurrentSelection().getSelectedUIDObjects();
		while (cutObjIter.hasNext()) {
			IUIDObject uidObj = cutObjIter.getNext();
			if (uidObj instanceof IDiagramObject) {
				if (isObjectCopyable((IDiagramObject) uidObj) || uidObj instanceof ILogicSegment) {
					if (uidObj instanceof IPropertiedGraphic && isDecorative(uidObj)) {
						continue;
					}
					toBeDeleted.add(uidObj);
				}
			}
			else if (uidObj instanceof IAssembly || uidObj instanceof IMulticore) {
				toBeDeleted.add(uidObj);
			}
		}
	}

	private void doCleanUp()
	{
		m_overrideBufferClear = false;
		clearPasteBuffer();
		toBeDeleted.clear();
		m_sharedNames.clear();
		extendedDeletionList.clear();
		disconnectionsToProcess.clear();
	}

	@Override public boolean isCutAllowed(ICapletController controller)
	{
		if (isModelEditableForCut(controller)) {
			return isCopyAllowed(controller) && !wereObjectsLocked(controller);
		}
		return false;
	}

	protected boolean isModelEditableForCut(ICapletController controller)
	{
		//disallowing in capture for time-being
		if (controller instanceof FunctionController) {
			return false;
		}
		return controller != null && controller.getCapletModel() != null && controller.getCapletModel().isEditable();
	}

	private boolean wereObjectsLocked(ICapletController controller)
	{
		ILogicDesign design = getSourceDiagram().getDesign();
		if (design != null && design.isUnderConcurrentEdit()) {
			SelectedUIDObjectIterator selectedUIDObjects = getPreSelections(controller).getSelectedUIDObjects();
			Collection<ILogicObject> objectsToCheck = new ArrayList<>();
			while (selectedUIDObjects.hasNext()) {
				IUIDObject uidObj = selectedUIDObjects.getNext();
				ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(uidObj);
				if (logicObject != null) {
					objectsToCheck.add(logicObject);
				}
			}
			return LogicObjectLockFinder.isAnyLockedInOtherSession(objectsToCheck);
		}
		return false;
	}

	protected boolean canDeleteObjects(ICapletController controller)
	{
		SelectSet selection = getPreSelections(controller);
		SelectedUIDObjectIterator selectedUIDObjects = selection.getSelectedUIDObjects();
		while (selectedUIDObjects.hasNext()) {
			IUIDObject uidObject = selectedUIDObjects.getNext();
			IDeletabilityChecker deletabilityChecker = new DeletabilityChecker();
			if (!deletabilityChecker.canDelete(uidObject, selection)) {
				return false;
			}
		}
		return true;
	}


	@Nullable @Override public String getDisabledTooltipForCut(ICapletController controller)
	{
		if (isModelEditableForCut(controller) && !getPreSelections(controller).isEmpty()) {
			return ResourceMgr.getString(LogicDataTransfer.class, "LogicDataTransfer.cutAction.disabled.short.desc");
		}
		return null;
	}

	/**
	 * If doing a paste from a copy, this will duplicate the itesm to be copied and paste them down.
	 *
	 * @return True if successful.
	 */
	public boolean doCAFPaste(@NotNull ICapletController controller)
	{
		super.doCAFPaste(controller);
		// we need to have all of the objects in the UIDMgr first, so put them back
		addOrRemovePreviousCopiedObjectsFromUIDMgr(true);

		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
		// find out if the source of this data transfer is the same as the current caplet model
		// so that shield bodies can do the right thing on paste
		boolean isDataTransferFromCurrentCapletModel = (capView.getCapletModel() == getSourceCapletModel());

		if (isPasteinSameDesign() && !isPreserveConnectivityAllowed()) {
			displayRestrictedSharedPinPasteWarning();
			addOrRemovePreviousCopiedObjectsFromUIDMgr(false);
			return false;
		}

		if (capView instanceof GfxView) {
			UndoDisableForSharedObjectSave undoDisabler = new UndoDisableForSharedObjectSave();
			IPersistenceSession persistenceSession = FactoryMgr.getCHSSystem().getPersistenceSession();
			try {
				if (persistenceSession != null) {
					persistenceSession.addListener(undoDisabler);
				}

				boolean bPasteSuccessful =
						doCAFPaste(controller, (GfxView) capView, isDataTransferFromCurrentCapletModel);
				return bPasteSuccessful;
			}
			finally {
				setCutInProgress(false);
				if (persistenceSession != null) {
					undoDisabler.clearUndo();
					persistenceSession.removeListener(undoDisabler);
				}
			}
		}
		else {
			// well, something didn't go well, remove all of the objects we added in
			addOrRemovePreviousCopiedObjectsFromUIDMgr(false);
			return false;
		}
	}

	private void reportFailedPasteSpecial()
	{
		CAFUtils cafUtils = CAFUtils.getInstance();
		cafUtils.getOutputWindow().sendApplicationMessage(
				ResourceMgr.getString(LogicDataTransfer.class, "LogicDataTransfer.pasteAction.lockObjectsFailed"));
	}

	@Override public void reportOnOutputWindow()
	{
		if (!isCutInProgess() || m_sharedNames.isEmpty()) {
			return;
		}
		CAFUtils cafUtils = CAFUtils.getInstance();
		if (!cafUtils.getActiveCapletView().getCapletModel().equals(getSourceCapletModel())) {
			cafUtils.getOutputWindow().sendApplicationMessage(getOutputMessageForShared());
		}
	}

	@NotNull private String getOutputMessageForShared()
	{
		Set<String> sortedSet =
				CollectionUtils.createAndSortSet(m_sharedNames.iterator(), new AlphaNumComparator<String>());
		String collection = StringUtils.convertCollectionToString(sortedSet, ",");
		String message =
				ResourceMgr.getString(LogicDataTransfer.class, "LogicDataTransfer.pasteAction.displaySharedNames.text", collection);
		return message;
	}

	private boolean doCAFPaste(ICapletController controller, GfxView capView,
			boolean isDataTransferFromCurrentCapletModel)
	{
		GfxView gview = capView;
		ISheet targetDiagram = gview.getSheet();
//			IGrid destGrid = ((IGriddable) targetDiagram).getGrid();
		final double scale = GfxUtils.SCALE_ONE;
		m_textScalingVisitor.setScale(scale);
		Generator generator = Generator.getGenerator();
		Set<IPinList> modularSchematicCandidates = new HashSet<>();
		if (m_objectBuffer != null) {
			filterNonPasteableContent(m_objectBuffer);
			List<IUIDObject> bufferCopy = new ArrayList<IUIDObject>(m_objectBuffer);

			Point delta = getOffset(gview);

			IDesignContainer design = getSourceDesign();

			if (isPreservingObjectNames() && isPasteinSameDesign()) {

				if (design instanceof ILogicDesign && ((ILogicDesign) design).isUnderConcurrentEdit()) {
					ILogicDesign logicDesign = (ILogicDesign) design;
					if (!areAllObjectsInPasteLocked(logicDesign, bufferCopy)) {
						reportFailedPasteSpecial();
						return false;
					}
				}
			}
			Set<ILocation> offsetSet = new HashSet<ILocation>();
			Collection<IConductor> conductors = new ArrayList<IConductor>();
			List<IShieldBody> shieldBodies = new ArrayList<IShieldBody>();
			List<IHighwaySchematic> schemHighways = new ArrayList<IHighwaySchematic>();
			Set<chs.cof.logical.cable.IConductor> cableConds = new HashSet<chs.cof.logical.cable.IConductor>();
			Set<chs.cof.logical.cable.IConductor> condInHighway = new HashSet<chs.cof.logical.cable.IConductor>();
			List<IChainSegmentContainer> daisyChains = new ArrayList<>();
			List<IShieldBody> shieldBodiesToDiscard = new ArrayList<IShieldBody>();
			Set<IUIDObject> pastedConnectivityObjs = new HashSet<IUIDObject>();
			Set<IPropertiedGraphic> pastedPropertiedGraphics = new HashSet<>();
			for (IUIDObject uidObj : bufferCopy) {
				boolean didPasteObject = true;
				ICompoundObject parent = targetDiagram;
				if (uidObj instanceof IGfxObject) {
					IGfxObject gfxObj = (IGfxObject) uidObj;

					if (gfxObj instanceof IConductor) {
						pasteConductors(scale, delta, offsetSet, conductors, cableConds, (ICompoundObject) gfxObj);
					}
					else if (gfxObj instanceof IHighwaySchematic) {
						pasteHighways(scale, delta, offsetSet, schemHighways, condInHighway, (IHighwaySchematic) gfxObj);
					}
					else if (gfxObj instanceof IChainSegmentContainer) {
						pasteChainSegments(scale, delta, offsetSet, daisyChains, (ICompoundObject) gfxObj);
					}
					else {
						//retrieve the source owner of a propertied graphic object if any.
						IDiagramObject container = m_associatedGraphicObjects.get(gfxObj);
						//we will paste associated graphic objects associated to original owner
						//only if source diagram and target diagram are same. otherwise it
						//would be pasted on target diagram as direct child of diagram.
						if (container instanceof ICompoundObject) {
							ISchemDiagram sourceDiagram = DiagramHelper.getDiagram(container);
							if (sourceDiagram == targetDiagram) {
								parent = (ICompoundObject) container;
							}
						}

						if (gfxObj instanceof IPropertiedGfxGroup) {
							// Get start location
							transformGfxGroup(offsetSet, gview, delta, scale, gfxObj);

							// Make sure all of the group's members are in the parent!
							Replicator.gfxGroupAddChildern(parent, (IGfxGroup) gfxObj);
						}

						else {
							movePastedObject(targetDiagram, delta, offsetSet, gfxObj);
							if (gfxObj instanceof IShieldBody) {
								if (pasteShieldBodies(shieldBodies, shieldBodiesToDiscard, (IShieldBody) gfxObj,
										isDataTransferFromCurrentCapletModel)) {
									continue;
								}
							}
							if (gfxObj instanceof chs.cof.logical.schem.IAssembly) {
								IGfxObjectIterator iter = ((ICompoundObject) gfxObj).getObjects();
								while (iter.hasNext()) {
									IGfxObject gobj = iter.next();
									if (gobj instanceof IPropertiedGraphic) {
										m_pastedSelectSet.add(new Selection(((IUIDObject) gobj)));
									}
								}
							}
						}
					}

					/*							if (gfxObj instanceof IChainSegment) {
												daisyChains.add((IChainSegment) gfxObj);
												continue;
											}*/

					IProject project = getProject(targetDiagram);
					if (project != null) {
						handleSymbolDictionaryEntries(project, gfxObj);
						handleImageDictionaryEntries(project, gfxObj);
					}

					parent.addObject(gfxObj);
					if (gfxObj instanceof IPinList) {
						IPinList pinList = (IPinList) gfxObj;

						chs.cof.logical.cable.IPinList cablePL = pinList.getConnectivity();
						if (cablePL instanceof IDevice) {

							for (IDiagramObject child : pinList.getAttachedObjects()) {
								if (child instanceof IPinList) {
									IPinList childPinList = ((IPinList) child);
									if (childPinList.getConnectivity() instanceof IDeviceConnector) {
										if (project != null) {
											handleSymbolDictionaryEntries(project, childPinList);
											handleImageDictionaryEntries(project, childPinList);
										}
										parent.addObject(child);
									}
								}
							}
						}
						if (cablePL instanceof IConnector) {
							modularSchematicCandidates.add(pinList);
						}
					}
					if (gfxObj instanceof IRepresentedObject) {
						IRepresentedObject iRepresentedObject = (IRepresentedObject) gfxObj;
						addToConnectivity(iRepresentedObject, controller);
						PrintRegionUtils.renameComponentPrintRegions(iRepresentedObject);
						pastedConnectivityObjs.add(iRepresentedObject.getRawConnectivity());
					}
					if (gfxObj instanceof IPropertiedGraphic) {
						pastedPropertiedGraphics.add((IPropertiedGraphic) gfxObj);
					}
					if (gfxObj instanceof ISymbolDatumLayout &&
							((IDecorative) gfxObj).getDecorationUID() == null) {
						PrintRegionUtils.renameFreeSymbolPrintRegions(gfxObj);
					}
				}
				// all objects that aren't represented (like multicores and assemblies)
				else if (uidObj instanceof ILogicObject) {
					addToConnectivity(uidObj, controller);
					pastedConnectivityObjs.add(uidObj);
				}
				// don't notify of selection change till all are selected
				m_pastedSelectSet.add(new Selection(uidObj));
				if (uidObj instanceof IAnchorable) {
					((IAnchorable) uidObj).updateAnchors();
				}
			}
			resetRepeatedCopyFlag();
			// PW - 09/08/03
			// Add connectivity to the newly pasted MC. This will be useful if we are
			// copying a MC without ShieldBody, since only if the MC has ShieldBody that
			// the above code set the new MC's connectivity
			for (IMulticore mc : m_multicores) {
				addToConnectivity(mc, controller);
				pastedConnectivityObjs.add(mc);
			}

			// Add cable conductors which does not have any diagram object of it, but represented in highway
			for (chs.cof.logical.cable.IConductor cableCond1 : condInHighway) {
				if (!cableConds.contains(cableCond1)) {
					addToConnectivity(cableCond1, controller);
					pastedConnectivityObjs.add(cableCond1);
				}
			}

			boolean didPasteObject = true;
			for (IShieldBody sb : shieldBodies) {
				try {
					Collection<IConductor> shields = sb.getAllDirectlyConnectedShields();
					if (!shields.isEmpty()) {
						for (IConductor shield : shields) {
							IUIDObjectCollection<IPin> pins = shield.getPins();
							for (IPin pin : pins) {
								ConnectionHelper.addPhysicalShieldConnection(shield, pin);
							}
						}
					}
				}
				catch (Exception e) {
					//
					// It's not -> it was not added to the list.
					//
					didPasteObject = false;
				}

				if (didPasteObject) {
					addToConnectivity((IRepresentedObject) sb, controller);
					pastedConnectivityObjs.add(sb.getRawConnectivity());
				}
			}

			//dts0100598142 - VALIDATION FAILURE: This conductor SHIELD12 has out of sync schem/connectivty Multicore references.
			for (IConductor conductor : conductors) {
				if (conductor.getConnectivity() instanceof IShieldConductor) {
					IShieldConductor cableShieldCond = (IShieldConductor) conductor.getConnectivity();
					if (cableShieldCond.getMulticore() == null) {
						for (ISegment segment : conductor.getSegmentsOfType(ISegment.class)) {
							IJoint startNode = segment.getStartNode();
							if (startNode != null &&
									!startNode.getAssociations(IShieldBodyHookup.class).isEmpty()) {
								segment.setStartNode(null);
							}
							IJoint endNode = segment.getEndNode();
							if (endNode != null && !endNode.getAssociations(IShieldBodyHookup.class).isEmpty()) {
								segment.setEndNode(null);
							}
						}
					}
				}
			}

			GeneratorParameters genParams = new GeneratorParameters(
					CAFUtils.getInstance().getActiveDiagram().getGrid().getGridSpacing());
			for (IShieldBody shieldBody : shieldBodies) {
				generator.generateShieldBody(shieldBody, genParams);
			}
			//dts0100882052 ST C1 Bash12:Copy paste action on multicore with shield leading to exception.
			List<IConductor> toDel = new ArrayList<IConductor>();
			for (IConductor conductor : conductors) {
				if (conductor.getConnectivity() instanceof IShieldConductor) {
					if (isDanglingShield_Of_A_Multicore(conductor)) {
						//If there is any indicator of this MC at this location, hook it up
						//otherwise?
						toDel.add(conductor);
					}
				}
			}

			for (IConductor condToRemove : toDel) {
				conductors.remove(condToRemove);
				condToRemove.delete();
			}

			//Extended fix for SP1206-dts0100849946 Diagram contents will not copy using the advanced copy CTRL+SHIFT+V method
			Set<IChainSegmentContainer> daisyChainsToIgnore = new HashSet<IChainSegmentContainer>();
			for (IChainSegmentContainer chain : daisyChains) {
				for (IShieldBodyHookup hookup : chain.getAttachedHookups()) {
					if (!shieldBodies.contains(hookup.getShieldBody())) {
						//The corresponding connected shield body is not being pasted. Ignore this chain segment
						daisyChainsToIgnore.add(chain);
						break;
					}
				}
			}
			for (IChainSegmentContainer chain : daisyChains) {
				if (!daisyChainsToIgnore.contains(chain)) {
					targetDiagram.addObject(chain);
					// don't notify of selection change till all are selected
					m_pastedSelectSet.add(new Selection(chain));
				}
				else {
					//SP1304_dts0100947590: AutoFail Regression:Copy paste across design resulting into exception
//						removeObject(chain);
					m_objectBuffer.remove(chain);
					m_creationObjects.remove(chain);
					chain.delete();
				}
			}
			for (IShieldBody sb : shieldBodiesToDiscard) {
				handleNonPasteableObject(sb);
			}
			for (IConductor cond : m_partialConductors) {
				Set<IConductor> newConds = cond.makeContinuous();
				for (IConductor schemCond : newConds) {
					if (schemCond.getConnectivity() != null &&
							m_newVsOriginalConnectivity.get(schemCond.getConnectivity()) != null) {
						m_recorderdConnectivity
								.put(schemCond, m_newVsOriginalConnectivity.get(schemCond.getConnectivity()));
						m_pastedSelectSet.add(new Selection(schemCond));
					}
				}
			}

			// fix up the name manager for the contents of assemblies
			for (Object obj : m_creationObjects) {
				if (obj instanceof IAssembly) {
					IAssembly assy = (IAssembly) obj;
					INameMgr nameMgr = assy.getNameMgr();
					Set<IAssembledObject> assembledObjectSet = assy.getElements();
					for (IAssembledObject assembledObject : assembledObjectSet) {
						if (assembledObject instanceof ILogicObject) {
							ILogicObject lObj = (ILogicObject) assembledObject;
							// since the objects are scoped within the name name space of the assembly,
							// setup the name manager correctly
							lObj.setNameMgr(nameMgr);
						}
					}
				}
			}

			//IESCD-1101:VALIDATION FAILURE: Field chs.cof.drawplus.DiagramAttributeText.m_objRef
			ICommonInvalidStateHandler scrubberHelper = new CommonInvalidStateScrubber();
			for (IPropertiedGraphic pastedPropertiedGraphic : pastedPropertiedGraphics) {
				CommonScrubbableChecker.checkAttributeTextsOnPropertiedGraphic(scrubberHelper, pastedPropertiedGraphic);
			}

			// dts0100586677 - Copy/Paste MC with shields do not display ports and x-ref
			// After we conductors have been added to the object model, we can now determine if
			// each conductor requires a port on the end.
			for (IConductor conductor : conductors) {
				Model model =
						(Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel();
				PortHelper.updatePortGfx(conductor, model.getDiagram().getGrid().getGridSpacing());
			}

			for (IHighwaySchematic highwaySchem : schemHighways) {
				Model model =
						(Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel();
				PortHelper.updatePortGfx(highwaySchem, model.getDiagram().getGrid().getGridSpacing());
			}

			manageUndo(false);

			IProject targetProject = getProject(targetDiagram);
			if (targetProject != null && targetProject != getSourceProject()) {
				resetModuleCodesOnObjects(pastedConnectivityObjs);
			}
		}

		// perform any renamings required post paste...
		performPostPaste();
		//report the shared objects
		reportOnOutputWindow();
		m_sharedNames.clear();

		if (m_objectBuffer != null) {
			updateSelections(controller);
		}

		for (IPinList pl : m_devices) {
			if (pl.getConnectivity() instanceof IDevice) {
				IDevice dev = (IDevice) pl.getConnectivity();
				if (dev.getNumDeviceConnectors() > 0) {
					// Generally the regenerate will not need to no anything, but when
					// it does the graphic attributes of the DCs will be lost
					generator.regenerateSchemDeviceConnectors(pl, new GeneratorParameters());
				}
			}
		}
		if (isPreserveConnectivityAllowed()) {

			Map<IPinList, ModularSchemPinListInfo> modularGroup = new HashMap<>();
			ModularConnectorHelper.generateModularGrouping(modularSchematicCandidates, modularGroup);
			for (IPinList schematicCandidate : modularGroup.keySet()) {
				IConnector connector = CommonUtils.cast(schematicCandidate.getConnectivity(), IConnector.class);
				if (connector != null) {
					int cableDepth = ConnectorHelper.getCableModularDepth(connector);
					int schemDepth = ConnectorHelper.getSchematicModularDepth(schematicCandidate);
					if (cableDepth > schemDepth) {
						ConnectorHelper.ensureModularSchematics(schematicCandidate, (ISchemDiagram) targetDiagram);
					}
				}
			}
		}
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		if (targetDiagram instanceof IBaseDiagram) {
			((IBaseDiagram) targetDiagram).refreshRepresentations();
		}
		return true;
	}

	private void handleConnectorAssembly(@NotNull IUIDObject uidObj)
	{
		// LOGIC-12996 - validation failure on creating connector assembly without main connector
		IAssembly assembly = CommonUtils.cast(uidObj, IAssembly.class);
		if (assembly != null && assembly.getAssemblyType() == AssemblyTypeEnum.CONNECTORASSEMBLY &&
				assembly.getPrimaryAssemblyConnector() == null) {
			assembly.setAssemblyType(AssemblyTypeEnum.STANDARDASSEMBLY);
		}
	}

	protected void filterNonPasteableContent(Collection<IUIDObject> objectBuffer)
	{
	}

	protected void updateSelections(ICapletController controller)
	{
		//remove highlight form objects while doing copy-paste
		controller.getSelectMgr().getCurrentSelections().setShowHighlights(false, false);
		// select the pasted objects
		// note that this causes 2x notification (one for the clear, one for the add) of selection change
		// could get this down to 1x notification like in SymbolDataTransfer,
		// but for some reason we get a ConcurrentModificationException on the Selection map when we try this?
		controller.getSelectMgr().getPreSelections().clear();
		controller.getSelectMgr().getPreSelections().add(m_pastedSelectSet);
	}

	private boolean pasteShieldBodies(List<IShieldBody> shieldBodies, List<IShieldBody> shieldBodiesToDiscard,
			IShieldBody gfxObj, boolean isDataTransferFromCurrentCapletModel)
	{
		//
		// Make sure the connectivity portion is present already
		// [if we did a copy [or indicator]/ dleete all, then paste, it won't be.
		//
		IShieldBody sb = gfxObj;

		chs.cof.logical.cable.IShieldBody sbc = sb.getConnectivity();
		// make sure that this is the same caplet model and that the
		// multicore has been copied, otherwise it can't be pasted
		IMulticore mult = sbc.getMulticore();
		if ((!isDataTransferFromCurrentCapletModel) &&
				!m_multicores.contains(mult)) {
			//SP1310-dts0100931815
			shieldBodiesToDiscard.add(sb);
			return true;
		}
		else {
			// consider this shield later
			shieldBodies.add(sb);
		}
		return false;
	}

	private void movePastedObject(ISheet targetDiagram, Point delta, Set<ILocation> offsetSet, IGfxObject gfxObj)
	{
		int relDeltaX = delta.x;
		int relDeltaY = delta.y;
		IDiagramObject container = m_associatedGraphicObjects.get(gfxObj);
		if (container instanceof ICompoundObject) {
			ISchemDiagram sourceDiagram = DiagramHelper.getDiagram(container);
			if (sourceDiagram == targetDiagram) {
				ILocation gfxObjLocation = gfxObj.getLocation();
				ILocation currLoc = FactoryMgr.getCommonFactory().constructLocation(gfxObjLocation);
				currLoc.applyDelta(relDeltaX, relDeltaY);
				ILocation newRelLoc = CoordinateHelper.getRelativeLocation(container, currLoc.getX(), currLoc.getY());
				relDeltaX = newRelLoc.getX() - gfxObjLocation.getX();
				relDeltaY = newRelLoc.getY() - gfxObjLocation.getY();
			}
		}
		moveGfxObject(gfxObj, relDeltaX, relDeltaY, offsetSet);
		if (gfxObj instanceof IPinList) {
			m_devices.add((IPinList) gfxObj);
			// Need to also offset the locations of attached DeviceConnectors
			moveAttachedPinlists(offsetSet, delta.x, delta.y, (IPinList) gfxObj);
		}
	}

	private void pasteChainSegments(double scale, Point delta, Set<ILocation> offsetSet,
			List<IChainSegmentContainer> daisyChains, ICompoundObject gfxObj)
	{
		daisyChains.add((IChainSegmentContainer) gfxObj);
		transformConductorSegments(gfxObj, delta, offsetSet, scale);
	}

	private void pasteHighways(double scale, Point delta, Set<ILocation> offsetSet,
			List<IHighwaySchematic> schemHighways, Set<chs.cof.logical.cable.IConductor> condInHighway,
			IHighwaySchematic gfxObj)
	{
		schemHighways.add(gfxObj);
		transformConductorSegments(gfxObj, delta, offsetSet, scale);
		IConductorIterator condIterator = HighwayHelper.toStackPinConductors(gfxObj.getConnectivity());
		while ((condIterator.hasNext())) {
			condInHighway.add(condIterator.getNext());
		}
	}

	private void pasteConductors(double scale, Point delta, Set<ILocation> offsetSet, Collection<IConductor> conductors,
			Set<chs.cof.logical.cable.IConductor> cableConds, ICompoundObject gfxObj)
	{
		IConductor schemCond = (IConductor) gfxObj;
		ConductorMerger.processCompositeDecorationTexts(schemCond);
		chs.cof.logical.cable.IConductor cableCond = schemCond.getConnectivity();
		conductors.add(schemCond);
		cableConds.add(cableCond);
		transformConductorSegments(gfxObj, delta, offsetSet, scale);
		// This branch appears to be dead code; when user uses Shift-Ctrl-V to paste preserving names
		// we replace the connectivity object on the pasted object by the original.
		// See assignOldConnectivity() below
		if (cableCond.isShared()) {
			Model model =
					(Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel();
			PortHelper.addPortGfx(schemCond, model.getDiagram().getGrid().getGridSpacing());
		}
	}

	@NotNull protected Point getOffset(GfxView gview)
	{
		Point delta = new Point();
		int offset = getOffsetForView(gview);
		if (NotaRepeatedCopy || !mouseMoved) {
			delta.setLocation(offset, offset);
		}
		else if (m_sourcePoint != null && m_PrevPoint != null) {
			IGrid grid = gview.getGridConfig().getGrid();
			delta.setLocation(m_PrevPoint.x - grid.snap(m_sourcePoint.x), m_PrevPoint.y - grid.snap(m_sourcePoint.y));
		}
		else {
			offset = super.getOffsetForView(gview);
			delta.setLocation(offset, offset);
		}
		return delta;
	}

	@Override public void notifyCopySuccess(boolean notify)
	{
		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
		if (capView instanceof GfxView) {
			GfxView gview = (GfxView) capView;
			ISheet targetDiagram = gview.getSheet();
			if (targetDiagram instanceof IBaseDiagram) {
				((IBaseDiagram) targetDiagram).refreshRepresentations();
			}
		}
		super.notifyCopySuccess(notify);
	}

	protected void handleNonPasteableObject(IShieldBody sb)
	{
		m_objectBuffer.remove(sb);
		m_creationObjects.remove(sb);
		sb.delete();
	}

	protected boolean isDanglingShield_Of_A_Multicore(IConductor conductor)
	{
		IShieldConductor cableShieldCond = (IShieldConductor) conductor.getConnectivity();
		if (cableShieldCond.getMulticore() != null) {
			boolean bDanglingShield = true;
			for (ISegment segment : conductor.getSegmentsOfType(ISegment.class)) {
				IJoint startNode = segment.getStartNode();
				IJoint endNode = segment.getEndNode();
				boolean bStartNodeConnectedToHookupOrPin = (startNode != null &&
						(!startNode.getAssociations(IShieldBodyHookup.class).isEmpty()));
				boolean bEndNodeConnectedToHookupOrPin = (endNode != null &&
						(!endNode.getAssociations(IShieldBodyHookup.class).isEmpty()));
				if (bStartNodeConnectedToHookupOrPin || bEndNodeConnectedToHookupOrPin) {
					bDanglingShield = false;
					break;
				}
			}
			return bDanglingShield;
		}
		return false;
	}

	private Set<IUIDObject> getAdditionModuledObjects(Collection<IUIDObject> uidObjects)
	{
		Set<IUIDObject> additionalObjects = new HashSet<IUIDObject>();
		for (IMulticore mc : m_multicores) {
			additionalObjects.add(mc);
			additionalObjects.addAll(getMulticoreModuledChildren(mc));
		}

		for (IUIDObject object : uidObjects) {
			if (object instanceof IHighwaySchematic) {
				IConductorIterator condIterator =
						HighwayHelper.toStackPinConductors(((IHighwaySchematic) object).getConnectivity());
				while ((condIterator.hasNext())) {
					additionalObjects.add(condIterator.getNext());
				}
			}
			else if (object instanceof IPinList) {
				chs.cof.logical.cable.IPinList cablePL = ((IPinList) object).getConnectivity();
				if (cablePL instanceof IConnector) {
					additionalObjects.addAll(getConnectorModuledChildren((IConnector) cablePL));
				}
			}
			else if (object instanceof IConnector) {
				additionalObjects.addAll(getConnectorModuledChildren((IConnector) object));
			}
			else if (object instanceof IMulticore) {
				additionalObjects.addAll(getMulticoreModuledChildren((IMulticore) object));
			}
		}
		return additionalObjects;
	}

	private Set<IUIDObject> getMulticoreModuledChildren(IMulticore mult)
	{
		Set<IUIDObject> children = new HashSet<IUIDObject>(1);
		chs.cof.logical.cable.IShieldBody sb = mult.getShieldBody();
		if (sb != null) {
			IShieldConductor shield = sb.getShieldConductor();
			if (shield != null) {
				children.add(shield);
			}
		}
		return children;
	}

	private Set<IUIDObject> getConnectorModuledChildren(IConnector connector)
	{
		Set<IUIDObject> children = new HashSet<IUIDObject>(1);
		IBackshell backshell = connector.getBackshell();
		if (backshell != null) {
			children.add(backshell);
		}
		return children;
	}

	private void resetModuleCodesOnObjects(Collection<IUIDObject> uidObjects)
	{
		Set<IUIDObject> objectSet = new HashSet<IUIDObject>(uidObjects);
		objectSet.addAll(getAdditionModuledObjects(objectSet));
		for (IBaseModuledObject moduledObject : reduceToModuleCodeObjects(objectSet)) {
			moduledObject.setModuleCodeInformation(null);
		}
	}

	private Set<IBaseModuledObject> reduceToModuleCodeObjects(Collection<IUIDObject> uidObjects)
	{
		Set<IBaseModuledObject> moduledObjects = new HashSet<IBaseModuledObject>();
		for (IUIDObject uidObj : uidObjects) {
			IUIDObject uidObject = UIDMgr.getNonDeletedObject(uidObj.getUID());
			if (uidObject != null) {
				IBaseModuledObject moduledObj = ReferenceHelper.reduceToLogicModuledObject(uidObj);
				if (moduledObj != null) {
					moduledObjects.add(moduledObj);
				}
			}
		}
		return moduledObjects;
	}

	private void addToConnectivity(IRepresentedObject repObj, ICapletController controller)
	{
		addToConnectivity(repObj.getRawConnectivity(), controller);
	}

	private void addToConnectivity(IUIDObject obj,
			ICapletController controller)
	{
		if (obj == null) {
			return;
		}
		ICapletModel capModel = controller.getCapletModel();
		IConnectivity conn = null;
		ILogicDesign logicDesign = null;
		if (capModel instanceof IGfxDisplayableModel) {
			IGfxDisplayableModel dispModel = (IGfxDisplayableModel) capModel;
			ISheet sht = dispModel.getSheet();
			if (sht instanceof ISchemDiagram) {
				ISchemDiagram sdiag = (ISchemDiagram) sht;
				logicDesign = sdiag.getDesign();
				if (logicDesign != null) {
					conn = logicDesign.getConnectivity();
				}
			}
		}

		if (conn != null && obj instanceof ILogicObject) {
			handleConnectorAssembly(obj);
			LogicUtils.addToConnectivity((ILogicObject) obj, conn);
		}
	}

	void addInlineOtherHalves(SelectSet selSet)
	{
		if (!getAssemblyExpandOption()) {
			return;
		}
		Set<IConnector> singleHalves = new HashSet<IConnector>();
		for (SelectedUIDObjectIterator sit = selSet.getSelectedUIDObjects(); sit.hasNext(); ) {
			IUIDObject obj = sit.getNext();
			if (obj instanceof IAssembly) {
				Set<IConnector> connectorsSingleHalf = ((IAssembly) obj).getConnectorsSingleHalf();
				singleHalves.addAll(connectorsSingleHalf.stream()
						.filter(connector -> connector instanceof IGenericInlineConnector).collect(
								Collectors.toSet()));
			}
		}

		for (IConnector lObj : singleHalves) {
			for (Iterator<IDiagramObject> objectIterator = getSourceDiagram().getVisibleRepresentations(lObj.getUID());
					objectIterator.hasNext(); ) {
				IDiagramObject dObj = objectIterator.next();
				if (dObj instanceof IPinList) {
					selSet.add(new Selection(dObj));
				}
			}
		}
	}

	protected ISchemDiagram getSourceDiagram()
	{
		Model model = (Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel();
		return model.getDiagram();
	}

	protected void expandSelection(SelectSet selSet)
	{
		SelectSet expandSet = new SelectSet();
		// remove any shield bodies
		SelectSet removedShieldBodies = new SelectSet();
		IBaseDiagram diagram = getSourceDiagram();
		SetMap<chs.cof.logical.cable.IConductor, IConductor> connVsSchemConductors = getConnVsSchemConductors(selSet);

		for (SelectionIterator it = selSet.getSelected(); it.hasNext(); ) {
			Selection sel = it.getNext();
			Class<?> selClass = sel.getSelectionClass();
			if (IAssembly.class.isAssignableFrom(selClass)) {
				if (getAssemblyExpandOption()) {
					expandSet.add(sel);
				}
			}
			if (IMulticore.class.isAssignableFrom(selClass)) {
				// make sure that shield bodies are included
				// if all the multicore conductors are included then propagate possible missing shield bodies
				if (areAllConductorsIncluded((IMulticore) sel.getObject(), selSet, connVsSchemConductors)) {
					expandSet.add(propagateShieldBodies(selSet, (IMulticore) sel.getObject()));
					expandSet.add(sel);
				}
			}
			else if (IShieldBody.class.isAssignableFrom(selClass)) {
				// remove invalid shield bodies.
				chs.cof.logical.cable.IShieldBody sb = ((IShieldBody) sel.getObject()).getConnectivity();
				if (areAllConductorsIncluded(sb.getMulticore(), selSet, connVsSchemConductors)) {
					for (chs.cof.logical.cable.IConductor conductor : sb.getMulticore().getConductors()) {
						expandSet.add(new Selection(connVsSchemConductors.getSet(conductor).iterator().next()));
					}
				}
			}
			else if (chs.cof.logical.cable.IShieldBody.class.isAssignableFrom(selClass)) {
				chs.cof.logical.cable.IShieldBody sb = (chs.cof.logical.cable.IShieldBody) sel.getObject();
				if (!areAllConductorsIncluded(sb.getMulticore(), selSet, connVsSchemConductors)) {
					removedShieldBodies.add(sel);
				}
				else {
					expandSet.add(propagateShieldBodies(selSet, sb.getMulticore()));
					expandSet.add(new Selection(sb.getMulticore()));
				}
			}
			else if (IPinList.class.isAssignableFrom(selClass)) {
				// find out if it is an inline connector and ensure that the other half is selected
				IPinList pl = (IPinList) UIDMgr.getObject(sel.getUID());
				chs.cof.logical.cable.IPinList cablePl = pl.getConnectivity();
				// see if this is an inline
				if ((cablePl instanceof IInlineJackConnector) ||
						(cablePl instanceof IInlinePlugConnector)) {
					// select all of the objects, should only be one, but we'll do a loop just to make sure
					for (IDiagramObjectIterator diter = pl.getAttachedObjects(); diter.hasNext(); ) {
						IDiagramObject obj = diter.getNext();
						expandSet.add(new Selection(obj));
					}
				}

				if (cablePl instanceof IConnector) {
					IConnector connector = (IConnector) cablePl;
					if (connector.isConnectorAssembly()) {
						expandSet.add(new Selection(connector.getAssembly()));
					}
				}
				// jyang - Don't expand IDevice for IDeviceConnectors, replicator now will do it.
			}
		}
		m_selectExpander.propagate(expandSet, diagram);
		selSet.remove(removedShieldBodies);
		selSet.add(expandSet);
	}

	@NotNull
	private SetMap<chs.cof.logical.cable.IConductor, IConductor> getConnVsSchemConductors(@NotNull SelectSet selSet)
	{
		SetMap<chs.cof.logical.cable.IConductor, IConductor> connVsSchemConductors = new SetMap<>();

		selSet.getFilteredSelections(new SelectionFilter(IConductor.class))
				.stream().map(sel -> (IConductor)sel.getObject())
				.filter(Objects::nonNull)
				.forEach(cond -> connVsSchemConductors.add(cond.getConnectivity(), cond));

		return connVsSchemConductors;
	}

	// dts0100586787- get possibly not included multicore indicators.

	/**
	 * returns the missing shield bodies of mc
	 *
	 * @param selSet Selections to be copied
	 * @param mc The owner multicore for these indicators
	 *
	 * @return Multicore indicators that are not found in the selection set and that have corresponding shield
	 * conductors selected
	 */
	private SelectSet propagateShieldBodies(SelectSet selSet, IMulticore mc)
	{
		//
		IShieldConductor shieldCable = mc.getShield();
		SelectSet shieldBodies = new SelectSet();
		SelectionIterator sit = selSet.getSelected();
		while (sit.hasNext()) {
			Selection select = sit.getNext();
			if (IConductor.class.isAssignableFrom(select.getSelectionClass())) {
				// get conductors only
				IConductor shield = (IConductor) select.getObject();
				if (shieldCable == shield.getConnectivity()) {
					// get the shield body of that object
					IShieldBody shieldBody = shield.getHookup().getShieldBody();
					// if it is not already selected then select it
					if (!selSet.contains(shieldBody.getUID())) {
						shieldBodies.add(new Selection(shieldBody));
					}
				}
			}
		}
		return shieldBodies;
	}

	/**
	 * given a multicore and a selection set it will check if the multicore's conductors are selected or not. this
	 * function will look for connectivity only. for complex cases where we need to know the exact schematic instances
	 * we use the overloaded one.
	 *
	 * @param multicore the multicore
	 * @param set the selection set
	 * @param connVsSchemConductors Map of connectivity vs set of schem conductors
	 * @return true if all conductors are selected, false otherwise.
	 */
	private boolean areAllConductorsIncluded(@NotNull IMulticore multicore, @NotNull SelectSet set,
			@NotNull SetMap<chs.cof.logical.cable.IConductor, IConductor> connVsSchemConductors)
	{
		for (chs.cof.logical.cable.IConductor cond : multicore.getConductors()) {
			if (set.contains(cond.getUID())) {
				// found.
				continue;
			}
			if (!connVsSchemConductors.containsKey(cond)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Replicates a group of objects in a selection set.  Returns the objects that have been replicated.
	 *
	 * @param selSet The selected items to be replicated
	 *
	 * @return The copies of all the selected items.
	 */
	public Collection<IUIDObject> replicatedSet(IDataTransferReplicator replicator, SelectSet selSet)
	{
		assert replicator instanceof Replicator;
		Replicator aReplicator = (Replicator) replicator;

		expandSelection(selSet);
		addInlineOtherHalves(selSet);
		addModularChildHierarchy(selSet);
		// dts0100572025 - ClassCastException thrown on trying to Delete the Contents of the Wiring Design.
		// don't allow Copy/Paste of multiple design wide instances
		if (!checkCopyAllowed(selSet)) {
			return Collections.emptyList();
		}

		m_associatedGraphicObjects = new HashMap<>();
		m_duplicatedConductors = new HashSet<IConductor>();
		m_duplicatedHighways = new HashSet<IHighwaySchematic>();
		m_newSchemSectorVsOldSchemSector = new HashMap<>();
		Collection<IUIDObject> newObjs = new ArrayList<IUIDObject>();
		Set<IGfxObject> filteredGfxObjects = getFilteredObjectSet(selSet);
		SelectedUIDObjectIterator objIter = selSet.getSelectedUIDObjects();
		List<IBaseSegment> segments = new ArrayList<IBaseSegment>();
//		List<IChainSegmentContainer> deferredChains = new ArrayList<>();
		Collection<ILogicObject> processedConnectivities = new ArrayList<ILogicObject>();
		List<Pair<IPinList, IPinList>> modularSchemCandidates = new ArrayList<>();
		SelectSet selections = new SelectedObjectsByTypeProvider(selSet);
		populateSharedObjectAccessibilityMap(selSet);

		while (objIter.hasNext()) {
			IUIDObject origObject = objIter.getNext();
			if (origObject instanceof IDiagramObject) {

				//dts0101439710 - disallow copying shared objects with no permission
				if (isObjectCopyable((IDiagramObject) origObject) || origObject instanceof ILogicSegment) {
					ISharedObject sharedObject = getSharedObject(origObject);
					if (!canCopySharedObject(sharedObject)) {
						continue;
					}
				}

				// If it is not copyable skip it.  Only top level objects should be copied.
				if (!isObjectCopyable((IDiagramObject) origObject)) {
					if (origObject instanceof ILogicSegment) {
						// if it is a segment, add it to the list.
						segments.add((IBaseSegment) origObject);
					}
					continue;
				}

/*				if (origObject instanceof IChainSegment) {
					//
					// If the end hookups/indicators are not on the list, then
					// we bail on this object.
					//
					IChainSegment cseg = (IChainSegment) origObject;
					if (isIndicatorIncluded(selSet, cseg.getStartJoint()) &&
							isIndicatorIncluded(selSet, cseg.getEndJoint())) {
						segments.add(cseg);
					}
					continue;
				}*/
				if (origObject instanceof IChainSegmentContainer) {
					//
					// If the end hookups/indicators are not on the list or not all segments are selected, then
					// we bail on this object.
					//
					IChainSegmentContainer chain = (IChainSegmentContainer) origObject;
					if (!areAllIndicatorsIncludedInSelection(selSet, chain) || !areAllSegmentsSelected(selSet, chain)) {
						continue;
					}
					segments.addAll(chain.getSegmentsOfType(IChainSegment.class));
				}

				if (aReplicator.hasReplicated(origObject.getUID())) {   // Already replicated - skip this object
					continue;
				}

				if (filteredGfxObjects.contains(origObject)) {
					continue;
				}

				IUIDObject newObject = null;
				// dts0100572025 check if we had created this connectivity before,
				// i.e. this is an instance of a previously copied object.
				if (origObject instanceof IConnectivityRef) {
					IConnectivityRef conref = (IConnectivityRef) origObject;
					if (conref.getConnectivity() != null &&
							processedConnectivities.contains(conref.getConnectivity())) {
						// newObject is expected to be an instance of another already copied object
						newObject = copiedObj(aReplicator, origObject, selections, false);
					}
					else {
						newObject = copiedObj(aReplicator, origObject, selections);
						// now we have created a new connectivity. record it.
						processedConnectivities.add(conref.getConnectivity());
					}
				}
				else {
					newObject = copiedObj(aReplicator, origObject, selections);
				}
				if (newObject != null) {
					if (origObject instanceof IPinList && newObject instanceof IPinList) {
						modularSchemCandidates.add(new Pair<>((IPinList) origObject, (IPinList) newObject));
					}
					addNewSelection(newObject, origObject, newObjs);
				}
			}
			else if (origObject instanceof IMulticore) {
				aReplicator.addReferencedMulticore((IMulticore) origObject);
			}
			else if (origObject instanceof IAssembly) {
				IUIDObject newObject = copiedObj(aReplicator, origObject, selections);
				IDiagramObject schemAssembly = null;
				IDiagramObject oldSchemAssembly = null;
				if (newObject != null) {
					schemAssembly = getSourceDiagram().getRepresentation(newObject.getUID());
					oldSchemAssembly = getSourceDiagram().getRepresentation(origObject.getUID());
					addNewSelection(newObject, origObject, newObjs);
					if (oldSchemAssembly != null && schemAssembly == null) {
						schemAssembly =
								aReplicator.replicateSchemAssembly((chs.cof.logical.schem.IAssembly) oldSchemAssembly,
										(IAssembly) newObject);
					}
				}
				replicateSchemAssemblyContents(aReplicator, newObjs, schemAssembly, oldSchemAssembly, selSet);
			}
		}

		// At this point m_replicator knows everything it duplicated and no connectivity has been replicated.
		// Now the connectiivty is copied in the following manner:
		// - Go through every selected segment, duplicating the nodes if they connect objects that have been duplicated.
		// - For the logical connectivity if a node points to a pin that has been duplicated then that hookup is taken
		//   care of in the node duplication code.
		Set<IJoint> jointBucket = new LinkedHashSet<IJoint>();
		// we may not connect some segments to pins. adding this sort will provide consistency.
		Collections.sort(segments, getSegmentComparator());
		for (IBaseSegment seg : segments) {

			IJoint jointCopy = aReplicator.replicateNode(seg.getStartJoint(), seg instanceof IChainSegment);
			if (jointCopy != null) {
				m_creationObjects.add(jointCopy);
				jointBucket.add(jointCopy);
			}

			jointCopy = aReplicator.replicateNode(seg.getEndJoint(), seg instanceof IChainSegment);
			if (jointCopy != null) {
				m_creationObjects.add(jointCopy);
				jointBucket.add(jointCopy);
			}

			if (seg instanceof IChainSegment) {
				IChainSegment newSeg = aReplicator.replicateChain((IChainSegment) seg);
				IChainSegmentContainer oldChain = ((IChainSegment) seg).getDaisyChain();
				IUIDObject newChain = aReplicator.getNewObject(oldChain.getUID());
				assert newChain instanceof IChainSegmentContainer;
				((ICompoundObject) newChain).addObject(newSeg);
			}
		}
		for (Iterator<IJoint> bucketIter = jointBucket.iterator(); bucketIter != null && bucketIter.hasNext(); ) {
			IJoint joint = bucketIter.next();

			// If we have copied multiple segments whose start/end points connected together and the pin or
			// (for DR 410872) ShieldBodyHookup has been copied then all is well. However, if the segments have been
			// copied but not the pin/hookup, we must disconnect the segments.
			if (!isConnectedToPin(joint) && !isConnectedToHookup(joint) && !isSegmentHighwayConnection(joint)) {
				disconnectUnrelatedSegments(joint);
			}
		}
		buildModularConnectorAssociations(aReplicator, selSet);
		aReplicator.relinkHighways(m_duplicatedHighways);

		resetWireEndDetails(aReplicator);

		// now take care of the multicores
		m_multicores.clear();
		m_devices.clear();
		m_creationObjects.addAll(aReplicator.resolveConnectivity(m_multicores));
		//do this processing only after attachments are resolved.
		for (Pair<IPinList, IPinList> entry : modularSchemCandidates) {
			IPinList origObject = entry.getKey();
			IPinList newObject = entry.getValue();
			//for modular connector the replicated objects graphics should reflect as if at top level.
			//can't use applystyle because object is not added to diagram. so trying to appy default style.
			//is this risky? not sure. because at paste the applystyle would be applied.
			if (ConnectorHelper.getParentSchemPinList(origObject) != null
					&& ConnectorHelper.getParentSchemPinList(newObject) == null) {
				IParameterized params = newObject.getParameterized();
				if (params != null) {
					params.removeAllParameterContainers();
					ISchemDiagram diagram = DiagramHelper.getDiagram(origObject);
					assert diagram != null;
					GeneratorParameters genParams = DiagramHelper.createGeneratorParameters(diagram);
					Generator generator = Generator.getGenerator();
					GeneratorStyle gs = generator.getStyle();
					PinListTypeEnum subType = PinListTypeEnum.from_connectivity(newObject.getConnectivity());
					ConnectorHelper.addDefaults(params, gs, subType);
					generator.generateConnector(newObject, genParams, Generator.NOREGENERATE_PROPERTIES);
				}
			}
		}
		// dts0100875977 CSM - ClassCastException on undo operation after apply style
		IProject project = getSourceProject();
		for (IUIDObject newObj : newObjs) {
			if (project != null) {
				handleSymbolDictionaryEntries(project, newObj);
				handleImageDictionaryEntries(project, newObj);
			}
			if (newObj instanceof ITableDataProvider) {
				// during replication of pin, it is not connected to conductors, after replication we set the connections
				// so we need to update the table data
				// Note: LogicUpdateStyledGraphicsHandler though handles refresh of modified object, we need to do this
				// here because the order of model changed event listeners is such that  LogicUpdateStyledGraphicsHandler
				// is being invoked after CrossReferenceMonitor::nudgeViews() which calls recalculteExtent on Table Family
				// which in turn finds that that there are more tables than that should be as the table data is not yet refreshed
				// so it goes and deletes some of the tables
				((ITableDataProvider) newObj).refreshTables(); // this will update the table data
			}
		}

		removeDeletedCopiedObjects(m_creationObjects);

		if (attributeResolver != null) {
			attributeResolver.replaceCopiedObjects(replicator);
		}
		return newObjs;
	}

	@Override protected IDataTransferReplicator createReplicator(@NotNull SelectSet selection)
	{
		IDataTransferReplicator replicatorObj = super.createReplicator(selection);
		((Replicator)replicatorObj).removeFilter(IAttributeTypes.SHORT_DESCRIPTION);
		return replicatorObj;
	}

	private void removeDeletedCopiedObjects(@NotNull final Set<IUIDObject> copiedObjects)
	{
		Set<IUIDObject> deletedObjects = new HashSet<>();
		for(IUIDObject uidObj : copiedObjects) {
			if(uidObj.isDeletedObject()) {
				deletedObjects.add(uidObj);
			}
		}
		copiedObjects.removeAll(deletedObjects);
	}

	private void resetWireEndDetails(Replicator aReplicator)
	{
		for (Map.Entry<ILogicObject, ILogicObject> replicatedConnectivity : aReplicator.getNewConnVsOldConnMap()
				.entrySet()) {
			IPhysicalConductor
					newPhysicalConductor = CommonUtils.cast(replicatedConnectivity.getKey(), IPhysicalConductor.class);
			IPhysicalConductor oldPhysicalConductor =
					CommonUtils.cast(replicatedConnectivity.getValue(), IPhysicalConductor.class);

			if (newPhysicalConductor != null && oldPhysicalConductor != null) {
				resetStartPin(aReplicator, newPhysicalConductor, oldPhysicalConductor);
				resetWireEndDetails(aReplicator, newPhysicalConductor, oldPhysicalConductor);
			}
		}
	}

	private void resetWireEndDetails(@NotNull Replicator aReplicator, @NotNull IPhysicalConductor newPhysicalConductor,
			@NotNull IPhysicalConductor oldPhysicalConductor)
	{
		IAbstractPin newStartPin = newPhysicalConductor.getStartPin();
		if (newStartPin != null) {
			IAbstractPin oldPinOfNewStartPin = aReplicator.getOldObject(newStartPin, IAbstractPin.class);
			if (oldPhysicalConductor.getStartPin() != oldPinOfNewStartPin) {
				newPhysicalConductor.setTerminalMaterialSpecEnd1(oldPhysicalConductor.getTerminalMaterialSpecEnd2());
				newPhysicalConductor.setTerminalPartSpecEnd1(oldPhysicalConductor.getTerminalPartSpecEnd2());
				newPhysicalConductor.setSealPartSpecEnd1(oldPhysicalConductor.getSealPartSpecEnd2());
			}
			IAbstractPin newEndPin = newPhysicalConductor.getEndPin();
			if (newEndPin == null) {
				// Swapping end1 and end2
				newPhysicalConductor.setTerminalMaterialSpecEnd2(oldPhysicalConductor.getTerminalMaterialSpecEnd1());
				newPhysicalConductor.setTerminalPartSpecEnd2(oldPhysicalConductor.getTerminalPartSpecEnd1());
				newPhysicalConductor.setSealPartSpecEnd2(oldPhysicalConductor.getSealPartSpecEnd1());
			}
			else {
				IAbstractPin oldPinOfNewEndPin = aReplicator.getOldObject(newEndPin, IAbstractPin.class);
				if (oldPhysicalConductor.getStartPin() == oldPinOfNewEndPin) {
					// Swapping end1 and end2
					newPhysicalConductor
							.setTerminalMaterialSpecEnd2(oldPhysicalConductor.getTerminalMaterialSpecEnd1());
					newPhysicalConductor.setTerminalPartSpecEnd2(oldPhysicalConductor.getTerminalPartSpecEnd1());
					newPhysicalConductor.setSealPartSpecEnd2(oldPhysicalConductor.getSealPartSpecEnd1());
				}
				else {
					newPhysicalConductor
							.setTerminalMaterialSpecEnd2(oldPhysicalConductor.getTerminalMaterialSpecEnd2());
					newPhysicalConductor.setTerminalPartSpecEnd2(oldPhysicalConductor.getTerminalPartSpecEnd2());
					newPhysicalConductor.setSealPartSpecEnd2(oldPhysicalConductor.getSealPartSpecEnd2());
				}
			}
		}
	}

	private void resetStartPin(@NotNull Replicator aReplicator, @NotNull IPhysicalConductor newPhysicalConductor,
			IPhysicalConductor oldPhysicalConductor)
	{
		IAbstractPin oldStartPin = oldPhysicalConductor.getStartPin();
		if (oldStartPin != null) {
			IAbstractPin newPin = aReplicator.getNewObject(oldStartPin.getUID(), IAbstractPin.class);
			if (newPin != null && newPhysicalConductor.getPinSet().contains(newPin)) {
				newPhysicalConductor.setStartPinId(newPin.getUID());
			}
		}
	}

	private void addModularChildHierarchy(SelectSet selSet)
	{
		Set<IPinList> modularSchems = new HashSet<>();
		for (SelectedUIDObjectIterator sit = selSet.getSelectedUIDObjects(); sit.hasNext(); ) {
			IUIDObject obj = sit.getNext();
			if (obj instanceof IPinList) {
				Set<IPinList> visited = new HashSet<>();
				ConnectorHelper.collectModularConnectorSchematicPinLists((IPinList) obj, visited);
				modularSchems.addAll(visited);
			}
		}
		for (IPinList modularSchem : modularSchems) {
			selSet.add(modularSchem);
		}
	}

	protected void buildModularConnectorAssociations(Replicator replicator, SelectSet selSet)
	{
		for (SelectedUIDObjectIterator it = selSet.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();
			if (obj instanceof IPinList && ((IPinList) obj).getConnectivity() instanceof IConnector) {
				IConnector connector = (IConnector) ((IPinList) obj).getConnectivity();
				replicator.buildModularConnectorAssociations(connector);
			}
		}
	}

	private boolean isSegmentHighwayConnection(IJoint joint)
	{
		return !joint.getAssociations(IHighwaySegment.class).isEmpty() &&
				!joint.getAssociations(ISegment.class).isEmpty();
	}

	private Comparator<IBaseSegment> getSegmentComparator()
	{
		return new Comparator<IBaseSegment>()
		{
			@Override public int compare(IBaseSegment o1, IBaseSegment o2)
			{
				int result = comparePoints(o1.getStartPoint(), o2.getStartPoint());
				if (result == 0) {
					return comparePoints(o1.getEndPoint(), o2.getEndPoint());
				}
				return result;
			}

			private int comparePoints(ILocation point1, ILocation point2)
			{
				if (point1.getY() == point2.getY()) {
					return ((Integer) point1.getX()).compareTo(point2.getX());
				}
				if (point1.getY() < point2.getY()) {
					return -1;
				}
				return 1;
			}
		};
	}

	private void replicateSchemAssemblyContents(Replicator replicator, Collection<IUIDObject> newObjs,
			IDiagramObject schemAssembly, IDiagramObject oldSchemAssembly, SelectSet selSet)
	{
		if (schemAssembly != null) {
			newObjs.add(schemAssembly);
			IGfxObjectIterator iter = ((ICompoundObject) oldSchemAssembly).getObjects();
			while (iter.hasNext()) {
				IGfxObject gobj = iter.next();
				if (gobj instanceof IPropertiedGraphic && selSet.contains(((IUIDObject) gobj).getUID())) {
					IPropertiedGraphic propObject = (IPropertiedGraphic) gobj;
					IUIDObject newObj = null;
					if (propObject instanceof IPropertiedGfxGroup) {
						newObj = (IUIDObject) replicator.replicateGfx(propObject, 1.0);
					}
					else if (!AttributeUtils.isGroupedGfxObject(propObject)) {
						newObj = replicator.replicatePropertiedGraphic(propObject);
					}
					if (newObj != null) {
						Replicator.reparentObject((IDiagramObject) newObj, (ICompoundObject) schemAssembly);
					}
				}
			}
		}
	}

	/**
	 * Check that Copy of the selection is allowed here
	 *
	 * @param selectSet The SelectSet
	 *
	 * @return true if enabled, otherwise false and an error prompt is shown
	 */
	private boolean checkCopyAllowed(SelectSet selectSet)
	{
		return true;
	}

	/**
	 * Before this is called there could be a condition where two segments from different conductors are connected
	 * graphically but not logically.  This is the case where there are two seperate conductors ending at the same pin.
	 * If you copy/past both these connectors then, when pasted, they will graphically drag together but will represent
	 * two conductors - bad bad bad...
	 * <p>
	 * The fix for this problem is to seperate all the conductors graphically at this point.   DR 9615
	 *
	 * @param nodeCopy Node to resolve graphical connectivity for.
	 */
	private void disconnectUnrelatedSegments(IJoint nodeCopy)
	{
		// Algorithm is:
		//    - Create a map of Conductor -> joint pairs.
		//    - Go through each segment and replace the references of "nodeCopy" with the proper joint that
		//      corresponds to the right conductor.
		Map<IConductor, IJoint> condJointMap = new LinkedHashMap<IConductor, IJoint>();
		Map<IHighwaySchematic, IJoint> highwayJointMap = new LinkedHashMap<IHighwaySchematic, IJoint>();

		boolean nodeCopyUsed = false;

		for (IDiagramObjectIterator itr = nodeCopy.getAssociations(); ((itr != null) && (itr.hasNext())); ) {
			IDiagramObject dobj = itr.getNext();

			if (dobj instanceof ISegment) {
				ISegment segment = (ISegment) dobj;

				IJoint segJoint = condJointMap.get(segment.getConductor());
				if (segJoint == null) {
					if (!nodeCopyUsed) {
						condJointMap.put(segment.getConductor(), nodeCopy);
						removeOtherSegments(nodeCopy, segment);
						nodeCopyUsed = true;
						// Don't need to switch the joint around because it is the passed in one
					}
					else {
						// The joint passed in has been used, but we need another.
						segJoint = createEmptyJoint(nodeCopy);
						m_creationObjects.add(segJoint);
						segJoint.addAssociation(segment);
						condJointMap.put(segment.getConductor(), segJoint);
						replaceJoint(segment, nodeCopy, segJoint);
					}
				}
				else {
					replaceJoint(segment, nodeCopy, segJoint);
				}
			}
			else if (dobj instanceof IHighwaySegment) {
				IHighwaySegment segment = (IHighwaySegment) dobj;

				IJoint segJoint = highwayJointMap.get(segment.getHighway());
				if (segJoint == null) {
					if (!nodeCopyUsed) {
						highwayJointMap.put(segment.getHighway(), nodeCopy);
						removeOtherSegments(nodeCopy, segment);
						nodeCopyUsed = true;
						// Don't need to switch the joint around because it is the passed in one
					}
					else {
						// The joint passed in has been used, but we need another.
						segJoint = createEmptyJoint(nodeCopy);
						m_creationObjects.add(segJoint);
						segJoint.addAssociation(segment);
						highwayJointMap.put(segment.getHighway(), segJoint);
						replaceJoint(segment, nodeCopy, segJoint);
					}
				}
				else {
					replaceJoint(segment, nodeCopy, segJoint);
				}
			}
		}
	}

	/**
	 * Removes all the segments from the passed in node that don't share the same conductor as the passed in segment
	 *
	 * @param nodeCopy Joint to be fixed up
	 * @param segment Segment to fix joint up against.
	 */
	private void removeOtherSegments(IJoint nodeCopy, ISegment segment)
	{
		IConductor baselineCond = segment.getConductor();

		Set<ISegment> segToDel = new LinkedHashSet<ISegment>();
		for (IDiagramObjectIterator itr = nodeCopy.getAssociations(); itr != null && itr.hasNext(); ) {
			IDiagramObject dobj = itr.getNext();

			if (dobj instanceof ISegment) {
				ISegment seg = (ISegment) dobj;
				if (!seg.getConductor().equals(baselineCond)) {
					segToDel.add(seg);
				}
			}
		}
		for (ISegment dobj : segToDel) {
			nodeCopy.removeAssociation(dobj);
		}
	}

	private void removeOtherSegments(IJoint nodeCopy, IHighwaySegment segment)
	{
		IHighwaySchematic baselineHighway = segment.getHighway();

		Set<IHighwaySegment> segToDel = new LinkedHashSet<IHighwaySegment>();
		for (IDiagramObjectIterator itr = nodeCopy.getAssociations(); itr != null && itr.hasNext(); ) {
			IDiagramObject dobj = itr.getNext();

			if (dobj instanceof IHighwaySegment) {
				IHighwaySegment seg = (IHighwaySegment) dobj;
				if (!seg.getHighway().equals(baselineHighway)) {
					segToDel.add(seg);
				}
			}
		}
		for (IHighwaySegment dobj : segToDel) {
			nodeCopy.removeAssociation(dobj);
		}
	}

	private IJoint createEmptyJoint(ILocation loc)
	{
		ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		IUID uid = FactoryMgr.createUID();
		IJoint joint = schemFactory.createNode(uid);
		joint.setX(loc.getX());
		joint.setY(loc.getY());

		return joint;
	}

	private void replaceJoint(ILogicSegment segment, IJoint toReplaceJoint, IJoint replaceWithJoint)
	{
		//To Fix dts0100709179-[CH] CreateSpliceAction: Validation Failure Detected:  VALIDATION FAILURE: This conductor SHIELD279 has out of sync schem/connectivty Multicore
		//.equals() is replaced with ==
		if ((segment.getStartJoint() != null) && (segment.getStartJoint() == toReplaceJoint)) {
			segment.setStartJoint(replaceWithJoint);
		}

		if ((segment.getEndJoint() != null) && (segment.getEndJoint() == toReplaceJoint)) {
			segment.setEndJoint(replaceWithJoint);
		}
	}

	/**
	 * Simple test to see if the given joint is attached to any pin.  Should probably be in some helper somewhere.
	 *
	 * @param joint Given joint to be connected.
	 *
	 * @return boolean, true if connected.
	 */
	private boolean isConnectedToPin(IJoint joint)
	{
		for (IDiagramObjectIterator itr = joint.getAssociations(); itr != null && itr.hasNext(); ) {
			IDiagramObject dobj = itr.getNext();

			if (dobj instanceof IPin || dobj instanceof ISchemStackPin) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tests to see if a joint is connected to a ShieldBodyHookup.
	 */
	private boolean isConnectedToHookup(IJoint joint)
	{
		for (IDiagramObjectIterator itr = joint.getAssociations(); itr != null && itr.hasNext(); ) {
			IDiagramObject dobj = itr.getNext();

			if (dobj instanceof IShieldBodyHookup) {
				return true;
			}
		}
		return false;
	}

	/**
	 * gdh 10/22/03 3681 When a shared object is copied the graphics are copied, but the "shared" is not. This method
	 * will revert the graphics to default settings, somehow.
	 */
	private void regenerateGraphics(IPinList orig, IPinList copy)
	{
		Generator generator = Generator.getGenerator();
		if (!generator.isBroken(orig)) {
			return;
		}

		//
		// Do not regenerate Device Connectors to remove the brokenness - they stay broken
		//
		chs.cof.logical.cable.IPinList plc = orig.getConnectivity();
		if (plc instanceof IConnector && !(plc instanceof IDeviceConnector)) {
			IBaseDiagram diagram = getSourceDiagram();
			if (diagram != null) {
				GeneratorParameters genParams = new GeneratorParameters(diagram.getGrid().getGridSpacing());
				generator.generateConnector(copy, genParams, plc.isShared(),
						Generator.NOREGENERATE_PROPERTIES); // false = shared;
			}
		}
	}

	/**
	 * Given a UID object this will duplicate it using the replicator.  This is really a convenience method designed to
	 * call the appropriate method on the replicator based on the given object type.
	 *
	 * @param replicator for replicating the object
	 * @param origObject The original UID object
	 * @param selections the selection set
	 *
	 * @return A duplicate version of the object passed in.
	 */
	@Nullable
	private IUIDObject copiedObj(Replicator replicator, IUIDObject origObject, SelectSet selections)
	{
		return copiedObj(replicator, origObject, selections, true);
	}

	/**
	 * Given a UID object this will duplicate it using the replicator.  This is really a convenience method designed to
	 * call the appropriate method on the replicator based on the given object type.
	 *
	 * @param replicator for replicating the object
	 * @param origObject The original UID object
	 * @param selections the selection set
	 * @param createConnectivity false if we want to create schematics only. Note that the connectivity will be set the
	 * connectivity of the origObject
	 *
	 * @return A duplicate version of the object passed in.
	 */
	@Nullable
	private IUIDObject copiedObj(Replicator replicator, IUIDObject origObject, SelectSet selections,
			boolean createConnectivity)
	{
		if (origObject instanceof IGfxGroupable && ((IGfxGroupable) origObject).isGrouped()) {
			// Grouped objects will be copied as members of the group so don't copy them here
			return null;
		}
		getAttributeResolver();
		IUIDObject copiedObject = null;
		if (origObject instanceof IPinList) {
			final Set<IHighwaySegment> selectedHWSegments = new HashSet<IHighwaySegment>();
			selectedHWSegments.addAll(selections.getSelectedObjects(IHighwaySegment.class));
			IObjectFilter<IGfxObject> gfxFilter = new IObjectFilter<IGfxObject>()
			{
				@Override public boolean accept(IGfxObject obj)
				{
					return acceptChildGfxObjectForPinList(selectedHWSegments, obj);
				}
			};
			IPinList origPL = (IPinList) origObject;
			IPinList copiedPinList = replicate(origPL, replicator, createConnectivity, gfxFilter);
			if (attributeResolver != null) {
				attributeResolver.addPinList(origPL, copiedPinList, false);
				for (IPinList origDevConn : origPL.getAttachedPinListObjects(IPinList.ONLY_DEVICE_CONNECTORS)) {
					IPinList copiedDevConn =
							CommonUtils.cast(replicator.getNewObject(origDevConn.getUID()), IPinList.class);
					if (copiedDevConn != null) {
						attributeResolver.addPinList(origDevConn, copiedDevConn, false);
					}
				}
			}

			copiedObject = copiedPinList;
			copyOverriddenAnalysisAttributes(origPL, copiedPinList);
			regenerateGraphics(origPL, copiedPinList);
			ISymbolRef symref = origPL.getSymbolRef();
			if (symref != null) {

				ISymbolRef sref = FactoryMgr.getSymbolFactory().
						constructSymbolRefTimestamped(symref.getSymbolUID(), symref.getTimestamp());
				chs.cof.logical.cable.IPinList cablePinlist = copiedPinList.getConnectivity();
				if (cablePinlist.canMaintainMultipleSymbols()) {
					cablePinlist.addSymbolRefIfCanMaintainMultipleSymbols(sref);
				}
				else {
					cablePinlist.setSymbolRef(sref);
				}
			}
			copiedPinList.setSymbolInformation(origPL.getSymbolDefUID(), origPL.getBlockUID(),
					origPL.getSymbolInstanceNumber());
			copiedPinList.setCompositeInstance(origPL.isCompositeInstance());
		}
		else if (origObject instanceof ISchemOtherComponent) {
			final ISchemOtherComponent origComp = (ISchemOtherComponent) origObject;
			copiedObject = replicator.replicateLogicOtherComponent(origComp, 1.0, createConnectivity);
		}
		else if (origObject instanceof ISchemSector) {
			ISchemSector origSchemSector = (ISchemSector) origObject;
			ISchemSector copiedSchemSector = replicator.replicate(origSchemSector);
			m_newSchemSectorVsOldSchemSector.put(copiedSchemSector, origSchemSector);
			if (attributeResolver != null) {
				attributeResolver.addSector(origSchemSector, copiedSchemSector, false);
			}
			copiedObject = copiedSchemSector;
//			regenerateGraphics(origSchemSector, copiedSchemSector);
		}
		else if (origObject instanceof IPropertiedCommentSymbol &&
				((IDecorative) origObject).getDecorationUID() == null) {
			// do copy comment symbols created manually. related to dts0100704593.
			copiedObject = replicator.replicate((IPropertiedCommentSymbol) origObject);
		}
		else if (origObject instanceof ILogicSegment) {
			copiedObject = replicator.replicateSegment((ILogicSegment) origObject);
		}
		else if (origObject instanceof IConductor) {
			IConductor origCond = (IConductor) origObject;
			if (m_duplicatedConductors.contains(origCond)) {
				return null;
			}
			m_duplicatedConductors.add(origCond);
			// Copy the conductor
			IConductor schemCond = replicator.replicateConductor(origCond, createConnectivity);
			copiedObject = schemCond;

			// Copy the conductor's segments.
			IGfxObjectIterator giter = origCond.getObjects();
			boolean partial = false;
			while (giter.hasNext()) {
				IGfxObject gobj = giter.getNext();
				if (gobj instanceof ISegment) {
					IUIDObject uidObj = (IUIDObject) gobj;
					// Don't want to copy the segment if it is not selected
					if (selections.contains(uidObj.getUID())) {
						ISegment newSeg = (ISegment) copiedObj(replicator, uidObj, selections, createConnectivity);
						if (newSeg != null && schemCond != null) {
							schemCond.addObject(newSeg);
						}
					}
					else {
						partial = true;
					}
				}
			}
			if (partial) {
				m_partialConductors.add(schemCond);
			}
		}
		else if (origObject instanceof IHighwaySchematic) {
			IHighwaySchematic origHighway = (IHighwaySchematic) origObject;
			if (m_duplicatedHighways.contains(origHighway)) {
				return null;
			}
			m_duplicatedHighways.add(origHighway);
			// Copy the conductor
			IHighwaySchematic schemHighway = replicator.replicateHighway(origHighway, createConnectivity);
			copiedObject = schemHighway;

			// Copy the conductor's segments.
			IGfxObjectIterator giter = origHighway.getObjects();
			while (giter.hasNext()) {
				IGfxObject gobj = giter.getNext();
				if (gobj instanceof ILogicSegment) {
					IUIDObject uidObj = (IUIDObject) gobj;
					// Don't want to copy the segment if it is not selected
					if (selections.contains(uidObj.getUID())) {
						ILogicSegment newSeg =
								(ILogicSegment) copiedObj(replicator, uidObj, selections, createConnectivity);
						if (newSeg != null && schemHighway != null) {
							schemHighway.addObject(newSeg);
						}
					}
				}
			}

			for (IUID pinUID : origHighway.getConnectedStackPins()) {
				IUIDObject newStackPin = replicator.getNewObject(pinUID);
				// If stack pin was also copied, then add replicated stack pin to it to highway
				if (newStackPin != null && schemHighway != null) {
					assert newStackPin instanceof ISchemStackPin;
					schemHighway.addSchemStackPin(newStackPin.getUID());
					if (SingleLineHelper.isSingleLineSchematic(schemHighway)) {
						SingleLineHelper.addSingleLineEnd((ISingleLine) schemHighway.getConnectivity(),
								(ISchemStackPin) newStackPin);
					}
				}
			}
		}
		else if (origObject instanceof IShieldBody) {
			copiedObject = replicator.replicateShieldBody((IShieldBody) origObject);
		}
		else if (origObject instanceof IChainSegment) {
			copiedObject = replicator.replicateChain((IChainSegment) origObject);
		}
		else if (origObject instanceof IChainSegmentContainer) {
			IChainSegmentContainer origChain = (IChainSegmentContainer) origObject;
			IChainSegmentContainer replicatedChain = replicator.replicateChainContainer(origChain);

/*			// Copy the conductor's segments.
			IGfxObjectIterator giter = origChain.getObjects();
			while (giter.hasNext()) {
				IGfxObject gobj = giter.getNext();
				if (gobj instanceof IChainSegment) {
					IUIDObject uidObj = (IUIDObject) gobj;
					IUIDObject newSeg = replicator.getNewObject(uidObj.getUID());
					if (newSeg != null && replicatedChain != null) {
						replicatedChain.addObject((IChainSegment) newSeg);
					}
				}
			}*/

			copiedObject = replicatedChain;
		}
		else if (origObject instanceof IPropertiedGraphic) {
			//dts0100704593: not allowing copy of leaderlines (style object)
			if (!isDecorative(origObject)) {
				copiedObject = replicator.replicatePropertiedGraphic((IPropertiedGraphic) origObject);
			}
		}
		else if (origObject instanceof IAssembly) {
			copiedObject = replicator.replicateAssembly((IAssembly) origObject);
		}
		return copiedObject;
	}

	protected IPinList replicate(IPinList origPL, Replicator replicator, boolean createConnectivity,
			IObjectFilter<IGfxObject> gfxFilter)
	{
		return replicator.replicate(origPL, 1.0, createConnectivity, gfxFilter, null);
	}

	protected boolean acceptChildGfxObjectForPinList(Set<IHighwaySegment> selectedHWSegments, IGfxObject obj)
	{
		ISchemStackPin emptyStackPin = StackedPinHelper.getEmptySchemStackPin(obj);
		if (emptyStackPin != null) {
			for (IHighwaySegment hwSeg : emptyStackPin.getHighwaySegments()) {
				if (selectedHWSegments.contains(hwSeg)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	@Nullable private IECAttributeResolver getAttributeResolver()
	{
		if (attributeResolver == null) {
			ISchemDiagram diagram = CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISchemDiagram.class);
			if (diagram != null) {
				attributeResolver = new IECAttributeResolver(diagram);
			}
		}
		return attributeResolver;
	}

	@Override public void resolveOwnerForAssociatedObjects(IDataTransferReplicator replicator, SelectSet curSels)
	{
		SelectedUIDObjectIterator selectedUIDObjects = curSels.getSelectedUIDObjects();
		while (selectedUIDObjects.hasNext()) {
			IUIDObject origObject = selectedUIDObjects.getNext();
			IUIDObject copiedObject = replicator.getNewObject(origObject.getUID());
			if (origObject instanceof IGfxObject && copiedObject instanceof IGfxObject) {
				IGfxObject copiedGfxObj = (IGfxObject) copiedObject;
				if (copiedGfxObj.getContainer() == null && isAssociatedGraphicObject((IGfxObject) origObject)) {
					//IESCD-1183: we can't make this copied object owner the source object's owner
					//because the source object's owner wouldn't be under copy and source diagram
					//could be read-only and not locked. to show transient graphics on the target diagram
					//this copied object would be tried to move around and that would cause the source
					//diagram to get marked as modified and would issue exception if read-only.
					IDiagramObject sourceOwner =
							origObject instanceof IDiagramObject ? ((IDiagramObject) origObject).getParent() : null;
					if (copiedGfxObj instanceof IPropertiedGraphic && sourceOwner != null) {
						m_associatedGraphicObjects.put((IPropertiedGraphic) copiedGfxObj, sourceOwner);
						ILocation absGfxLocation = CoordinateHelper.getAbsGfxLocation((IGfxObject) origObject, 0, 0);
						ILocation currLoc = copiedGfxObj.getLocation();
						int x_delta = absGfxLocation.getX() - currLoc.getX();
						int y_delta = absGfxLocation.getY() - currLoc.getY();
						copiedGfxObj.move(x_delta, y_delta);
					}
				}
			}
		}
	}

	private void copyOverriddenAnalysisAttributes(IPinList origPL, IPinList copiedPinList)
	{
		chs.cof.logical.cable.IPinList sourceConnectivity = origPL.getConnectivity();
		chs.cof.logical.cable.IPinList destConnectivity = copiedPinList.getConnectivity();

		destConnectivity.setOverriddenAnalysisInterfaces(sourceConnectivity.getOverriddenAnalysisInterfaces());
		destConnectivity.setOverriddenAnalysisFailureModes(sourceConnectivity.getOverriddenAnalysisFailureModes());
	}

	private boolean isDecorative(IUIDObject origObject)
	{
		return origObject instanceof IDecorative && ((IDecorative) origObject).getDecorationUID() != null;
	}

	/**
	 * Clear our internal paste buffer
	 */
	public void clearPasteBuffer()
	{
		super.clearPasteBuffer();
		m_partialConductors.clear();
		m_pastedSelectSet.clear();
		if (m_associatedGraphicObjects != null) {
			m_associatedGraphicObjects.clear();
		}
	}

	public boolean isObjectCopyable(IDiagramObject obj)
	{
		// Device connectors are NOT copyable
		if (obj instanceof IPinList) {
			IPinList plist = (IPinList) obj;
			if (plist.getConnectivity() instanceof IDeviceConnector) {
				return false;
			}
		}
		// Currently there is no replicator support for tables so disable copy of tables
		if (obj instanceof IBasicTable) {
			return false;
		}
		if (PrintRegionHelper.isPrintRegionRelatedObj(obj)) {
			return false;
		}
		if (obj instanceof ILayoutXYDimension) {
			return false;
		}
		if (isAssociatedGraphicObject(obj)) {
			return true;
		}

		// Only top-level objects may be copied.
		return obj.getParent() == null || obj.getParent() instanceof ISheet;
	}

	public boolean isObjectPastable(IDiagramObject obj)
	{
		return true;
	}

	public boolean isSharedPinlist(IDiagramObject obj)
	{
		if (obj instanceof IPinList) {
			IPinList plist = (IPinList) obj;
			chs.cof.logical.cable.IPinList cablePinlist = plist.getConnectivity();
			if (cablePinlist.getSharedPinList() != null) {
				return true;
			}
		}
		return false;
	}

	private static class TextScalingVisitor implements IGfxObjectVisitor
	{

		private double m_scale = GfxUtils.SCALE_ONE;

		public void setScale(double scale)
		{
			m_scale = scale;
		}

		public boolean visitGfxObject(IGfxObject gfxObj)
		{
			resizeIfTextObject(gfxObj);
			return false;
		}

		@SuppressWarnings({"NumericCastThatLosesPrecision"})
		public boolean resizeIfTextObject(IGfxObject gfxObj)
		{
			if (gfxObj instanceof IText) {
				((IText) gfxObj).setHeight((int) ((double) ((IText) gfxObj).getHeight() * m_scale));
				//(SP1210)dts0100902135 Text on a border with 'Shrink to fit' disappears, shrinks and/or moves when
				// used on a metric sheet but not when used on an imperial sheet
				IExtent sext = ((IText) gfxObj).getExternalBounds();
				if (((IText) gfxObj).isExtentAdjustable() && sext != null) {
					int ew = sext.getWidth();
					int eh = sext.getHeight();
					//
					// Bounded Text.
					//
					sext = FactoryMgr.getCommonFactory()
							.constructExtent((int) (sext.getX() * m_scale), (int) (sext.getY() * m_scale),
									(int) (ew * m_scale), (int) (eh * m_scale));
					if (gfxObj instanceof IPropertiedText) {
						((IPropertiedText) gfxObj).getGfxObject().setExternalBounds(sext);
					}
					else if (gfxObj instanceof IPropText) {
						((IPropText) gfxObj).setExternalBounds(sext.getWidth(), sext.getHeight());
					}
					else if (gfxObj instanceof IBoundedText) {
						((IBoundedText) gfxObj).setExternalBounds(sext);
					}
				}
				return true;
			}
			return false;
		}

		public boolean visitCompoundObject(ICompoundObject compObj)
		{
			return true;
		}
	}

	/**
	 * Can this data transfer preserve object names ?
	 *
	 * @return boolean, true if this data transfer support object naming preservation.
	 */
	public boolean canPreserveObjectNames()
	{
		return true;
	}

	@Override public String getStatusBarMessage()
	{
		return ResourceMgr.getString(LogicDataTransfer.class, "LogicDataTransfer.StatusBar.PasteReadyPreservingMsg");
	}

	private void transformConductorSegments(ICompoundObject schemCond, Point delta, Set<ILocation> offsetSet,
			double scale)
	{
		for (IGfxObjectIterator iter = schemCond.getObjects(); iter.hasNext(); ) {
			IGfxObject gobj = iter.getNext();
			if (gobj instanceof IBaseSegment) {
				transformSegment((IBaseSegment) gobj, delta, offsetSet, scale);
			}
		}
	}

	private void transformSegment(IBaseSegment seg, Point delta, Set<ILocation> offsetSet, double scale)
	{

		ILocation loc = seg.getLocation();
		offsetObject(loc, delta, offsetSet, scale);
		loc = seg.getStartPoint();
		offsetObject(loc, delta, offsetSet, scale);
		loc = seg.getEndPoint();
		offsetObject(loc, delta, offsetSet, scale);

		// May seem strange, but it marks the extent as dirty.
		seg.setLocation(seg.getLocation());

		// DR  436404: force recalculation of the extent, this triggers repositioning of any
		// LeaderLines attached to the segment.
		seg.getExtent();
	}

	/**
	 * This method performs post copy operations notably acquiring the naming map from the replicator.
	 *
	 * @param rep, the replicator used for the copy operation
	 */
	protected void performPostCopy(IDataTransferReplicator rep)
	{
		super.performPostCopy(rep);
		assert rep instanceof Replicator;
		Replicator replicator = (Replicator) rep;

		Map<IConnectivityRef, ILogicObject> newSchemVsOldConn = null;
		Map<IConnectivityRef, ILogicObject> schemVsConn = replicator.getNewSchemVsOldConnectivityMap();
		if (schemVsConn != null) {
			newSchemVsOldConn = new HashMap<IConnectivityRef, ILogicObject>(schemVsConn);
		}

		Map<ILogicObject, ILogicObject> newConnVsOldConn = null;
		Map<ILogicObject, ILogicObject> connVsOldConnMap = replicator.getNewConnVsOldConnMap();
		if (connVsOldConnMap != null) {
			newConnVsOldConn = new HashMap<ILogicObject, ILogicObject>(connVsOldConnMap);
		}

		SetMap<ISchemStackPin, IAbstractPin> pinSetMap = replicator.getNewStackVsOldPinMap();
		SetMap<ISchemStackPin, IAbstractPin> newStackPinVsOldPins = null;
		if (pinSetMap != null) {
			newStackPinVsOldPins = new SetMap<ISchemStackPin, IAbstractPin>(pinSetMap);
		}

		if (m_recorderdConnectivity != null) {
			mergeOrigConnectivities(newSchemVsOldConn, newStackPinVsOldPins, newConnVsOldConn);
		}
		else {
			m_recorderdConnectivity = newSchemVsOldConn;
			m_newVsOriginalConnectivity = newConnVsOldConn;
			m_stackPinRecorderdConnectivity = newStackPinVsOldPins;
			m_newVsOriginalConnectivity.entrySet()
					.forEach(e -> recordNewVsSharedConnectivity(e.getKey(), e.getValue()));
		}
//		m_newBlockVsOldBlock = replicator.getNewBlockVsOldBlock();
		m_newSchemLinkVsOldCableLink = replicator.getNewSchemLinkVsOldCableLink();
	}

	private void recordNewVsSharedConnectivity(@NotNull ILogicObject newConn, @NotNull ILogicObject oldConn)
	{
		ISharedObject sharedObject = oldConn.getSharedObject();
		if (newConn instanceof IAbstractPin && sharedObject != null) {
			m_newVsSharedConnectivity.put(newConn, sharedObject);
		}
	}

	/**
	 * @param newSchemeVsOldConnectivity - Map of new scheme object and connectivity of old scheme object where its was
	 * created from
	 * @param newStackPinVsOldPins - Map of stacked pin v/s old connectivity
	 * @param newConnectivityVsOldConnectivity - Map of new scheme object and connectivity of old scheme object where
	 * its was created from
	 */
	private void mergeOrigConnectivities(Map<IConnectivityRef, ILogicObject> newSchemeVsOldConnectivity,
			SetMap<ISchemStackPin, IAbstractPin> newStackPinVsOldPins,
			Map<ILogicObject, ILogicObject> newConnectivityVsOldConnectivity)
	{
		Map<IConnectivityRef, ILogicObject> newSchemeVsOldConnectivityMap = newSchemeVsOldConnectivity;

		for (Map.Entry<IConnectivityRef, ILogicObject> entry : newSchemeVsOldConnectivityMap.entrySet()) {
			IConnectivityRef schemObject = entry.getKey();
			ILogicObject oldConnectivity = entry.getValue();
			ILogicObject origConnectivity = m_newVsOriginalConnectivity.get(oldConnectivity);
			if (origConnectivity != null) {
				newSchemeVsOldConnectivityMap.put(schemObject, origConnectivity);
			}
		}
		m_recorderdConnectivity = newSchemeVsOldConnectivityMap;

		if (newStackPinVsOldPins != null) {
			for (Map.Entry<ISchemStackPin, Set<IAbstractPin>> entry : newStackPinVsOldPins.entrySet()) {
				ISchemStackPin stackPin = entry.getKey();
				Set<IAbstractPin> pins = entry.getValue();
				Set<IAbstractPin> newPins = new LinkedHashSet<IAbstractPin>();
				for (IAbstractPin pin : pins) {
					IAbstractPin newPin = (IAbstractPin) m_newVsOriginalConnectivity.get(pin);
					if (newPin != null) {
						newPins.add(newPin);
					}
				}

				if (newPins.size() == pins.size()) {
					newStackPinVsOldPins.put(stackPin, newPins);
				}
			}
		}
		m_stackPinRecorderdConnectivity = newStackPinVsOldPins;

		//Then update m_newVsOriginalConnectivity as well with new entries
		Map<ILogicObject, ILogicObject> mapping = newConnectivityVsOldConnectivity;
		for (Map.Entry<ILogicObject, ILogicObject> entry : newConnectivityVsOldConnectivity.entrySet()) {
			ILogicObject newConnectivity = entry.getKey();
			ILogicObject oldConnectivity = entry.getValue();
			ILogicObject origConnectivity = m_newVsOriginalConnectivity.get(oldConnectivity);
			if (origConnectivity != null) {
				mapping.put(newConnectivity, origConnectivity);
			}
			recordNewVsSharedConnectivity(newConnectivity, oldConnectivity);
			ISharedObject origSharedObject = m_newVsSharedConnectivity.get(oldConnectivity);
			if (origSharedObject != null) {
				m_newVsSharedConnectivity.remove(oldConnectivity);
				m_newVsSharedConnectivity.put(newConnectivity, origSharedObject);
			}
		}
		m_newVsOriginalConnectivity = mapping;
	}

	protected void performPostPaste()
	{
		if (isPreservingObjectNames()) {
			//Set old name and short description
			assignOldNameAndShortDesc();

			//Set the old connectivity
			ISchemDiagram diag = ((ISchemDiagram) CAFUtils.getInstance().getActiveDiagram());
			assert diag != null;
			IDesignWideUsageMgr dwum = diag.getDesign().getDesignWideUsageMgr();
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();

			if (isPasteinSameDesign()) {
				assignOldConnectivity(diag, dwum, cdh);
				assignOldPinsToStack(cdh);
			}
			// if cut in progress, first paste after cut, resolve IEC attributes
			if (isCutInProgess()) {
				resolveIECAttributes();
			}
		}
		else {
			resolveIECAttributes();
		}
		attributeResolver = null;
		setAssemblyExpandOption(false);
	}

	private void resolveIECAttributes()
	{
		if (attributeResolver != null) {
			ISchemDiagram diag = ((ISchemDiagram) CAFUtils.getInstance().getActiveDiagram());
			assert diag != null;
			ILogicDesign design = diag.getDesign();
			assert design != null;
			attributeResolver.resolveAttributes(design, diag);
		}
	}

	private void assignOldNameAndShortDesc()
	{
		for (IReadOnlyNamedObject obj : m_recordedNames.keySet()) {
			if (obj instanceof INamedObject) {
				((INamedObject) obj).setName(m_recordedNames.get(obj));
			}
		}

		// Set the short description if any
		if (m_recordedShortDescriptions != null) {
			for (IShortDescriptionObject logObj : m_recordedShortDescriptions.keySet()) {
				logObj.setShortDescription(m_recordedShortDescriptions.get(logObj));
			}
		}
	}

	private boolean isPreserveConnectivityAllowed()
	{
		if (isPreservingObjectNames()) {
			ISchemDiagram diag = ((ISchemDiagram) CAFUtils.getInstance().getActiveDiagram());
			assert diag != null;
			if (disallowLogicPinDuplication(diag.getDesign().getProject())) {
				for (IUIDObject origObject : m_origExtSelectedObjects) {
					if (origObject instanceof IPinList) {
						if (!containsOnlyReusablePins((IPinOwner) origObject)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	protected boolean containsOnlyReusablePins(IPinOwner origObject)
	{
		for (IAbstractSchemPin schemPin : origObject.getAllPins()) {
			boolean allPinsReusable = true;
			for (IAbstractPin absPin : PinUtils.getAllDesignPins(schemPin)) {
				if (!absPin.isReusable() && !absPin.isStudPin()) {
					allPinsReusable = false;
				}
			}
			if (allPinsReusable) {
				continue;
			}
			if (schemPin instanceof IGenericSchemPin) {
				if (!((IGenericSchemPin) schemPin).isReference()) {
					return false;
				}
			}
			else if (schemPin instanceof ISchemStackPin) {
				return false;
			}
		}
		return true;
	}

	protected void displayRestrictedSharedPinPasteWarning()
	{
		if (!enableWarnings) {
			return; // this only happens for unit tests
		}

		String key = getClass().getName() + "sharedrestrictedpinpaste";
		String msg = getDuplicatePinWarningMessage();
		String windowMsg = ResourceMgr
				.getString(LogicDataTransfer.class, "LogicDataTransfer.restricitedpinpaste.windowMessage.text");
		String header = ResourceMgr
				.getString(LogicDataTransfer.class, "LogicDataTransfer.restricitedpinpaste.warningHeading.text");

		OptionalDialog warnDialog =
				new OptionalDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), header, msg, key);
		if (warnDialog.showMe()) {
			warnDialog.displayWarningMessage();
		}
		else {
			LogHelper.printMsg(windowMsg);
		}
	}

	protected String getDuplicatePinWarningMessage()
	{
		return ResourceMgr
				.getString(LogicDataTransfer.class, "LogicDataTransfer.restricitedpinpaste.dupPinWarningMessage.text");
	}

	private void assignOldPinsToStack(CreationDeletionHelper cdh) //STACKED PIN COMMENT
	{
		if (m_stackPinRecorderdConnectivity != null && !m_stackPinRecorderdConnectivity.isEmpty()) {
			for (ISchemStackPin stackPin : m_stackPinRecorderdConnectivity.keySet()) {
				for (IAbstractPin pin : stackPin.getAllConnectivity()) {
					stackPin.removePinFromStack(pin);
					//cdh.addDeletionObject(pin);
				}

				for (IAbstractPin pin : m_stackPinRecorderdConnectivity.get(stackPin)) {
					stackPin.addPinToStack(pin);
				}
			}
		}
	}

	protected boolean isPasteinSameDesign()
	{
		IBaseDiagram baseDiagram = CAFUtils.getInstance().getActiveDiagram();
		return baseDiagram != null && getSourceDesign() == baseDiagram.getDesignContainer();
	}

	protected void assignOldConnectivity(ISchemDiagram diag, IDesignWideUsageMgr dwum, CreationDeletionHelper cdh)
	{
		if (m_recorderdConnectivity != null && !m_recorderdConnectivity.isEmpty()) {
			Set<IAssembly> assembliestobeRemoved = new HashSet<IAssembly>();
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap = new HashMap<IAssembly, IAssembly>();
			int grid = diag.getGrid().getGridSpacing();
			List<IConductor> candidateConductors = new ArrayList<>();
			Set<IShieldBody> shieldBodies = new HashSet<>();

			for (IConnectivityRef uidObj : m_recorderdConnectivity.keySet()) {

				if (uidObj instanceof IPinList) {
					reassignConnectivityforPinList(cdh, assembliestobeRemoved, newVsOldCableAssemblyMap,
							(IPinList) uidObj);
				}
				/* design wide objects are not required for other components as of now
				else if (uidObj instanceof ISchemOtherComponent) {
					reassignConnectivityforLayoutComonent(cdh, assembliestobeRemoved, newVsOldCableAssemblyMap,
							(ISchemOtherComponent) uidObj);
				}*/
				else if (uidObj instanceof IGenericSchemPin) {
					reassignConnectivityForPin((IGenericSchemPin) uidObj);
				}
				else if (uidObj instanceof IConductor) {
					candidateConductors.add((IConductor) uidObj);
				}
				else if (uidObj instanceof IShieldBody) {
					shieldBodies.add((IShieldBody) uidObj);
					reassignConnectivityforShieldBody(uidObj);
				}
				else if (uidObj instanceof IHighwaySchematic) {
					reassignConnectivityforHighway(dwum, cdh, uidObj, grid);
				}
				else if (uidObj instanceof chs.cof.logical.schem.IAssembly) {
					reassignConnectivityforSchemAssembly(diag, cdh, uidObj, assembliestobeRemoved,
							newVsOldCableAssemblyMap);
				}
			}

			Set<IMulticore> multicorestobeRemoved = new HashSet<IMulticore>();
			reassignConnectivityforconductors(diag, dwum, cdh, multicorestobeRemoved, assembliestobeRemoved,
					newVsOldCableAssemblyMap, candidateConductors, grid);

			//Specially for Shield Conductors
			for (IConnectivityRef uidObj : m_recorderdConnectivity.keySet()) {

				if (uidObj.getConnectivity() instanceof IShieldConductor) {
					reassignConnectivityforShieldconductor(
							assembliestobeRemoved, newVsOldCableAssemblyMap,
							uidObj);
				}
			}

			for (IConnectivityRef uidObj : m_recorderdConnectivity.keySet()) {
				if (uidObj instanceof IGenericSchemPin) {
					setSymbolRefOnSchemPin(uidObj);
				}
			}

			for (IMulticore mcore : multicorestobeRemoved) {
				assembliestobeRemoved.add(mcore.getAssembly());
				cdh.addDeletionObject(mcore);
			}

			processDaisyChainShieldConnections(shieldBodies);

			processAssemblies(cdh, assembliestobeRemoved, newVsOldCableAssemblyMap, diag);
			//SP1206 - dts0100839921 [CH] java.lang.ClassCastException: chs.caf.caplet.helpers.VoidUndoObjectHelper
			// cannot be cast to chs.cof.logical.cable.IGener
			//Reassign the new schem internal links to point to old connectivity
			for (ISchemInternalLink link : m_newSchemLinkVsOldCableLink.keySet()) {
				link.setConnectivity(m_newSchemLinkVsOldCableLink.get(link));
			}
		}
	}

	private void reassignConnectivityForPin(@NotNull IGenericSchemPin schemPin)
	{
		IGenericPin origPin = CommonUtils.cast(m_recorderdConnectivity.get(schemPin), IGenericPin.class);
		//This can happen in case of cut on reference shared pin as unplace deletes the connectivity.
		//we are re-instantiating the pin here with copied connectivity.
		if (origPin == null || origPin.isDeletedObject()) {
			IAbstractPin newPin = (IAbstractPin) schemPin.getConnectivity();
			chs.cof.logical.cable.IPinList newPinList = newPin.getOwner();
			if (newPinList != null) {
				newPinList.removePin(newPin);
			}
			ISharedPin sharedPin = CommonUtils.cast(m_newVsSharedConnectivity.get(newPin), ISharedPin.class);
			if (sharedPin != null) {
				newPin.setSharedPin(sharedPin);
			}
			chs.cof.logical.cable.IPinList origPinList =
					(chs.cof.logical.cable.IPinList) m_newVsOriginalConnectivity.get(newPinList);
			origPinList.addPin(newPin);
			m_recorderdConnectivity.put(schemPin, newPin);
			m_newVsOriginalConnectivity.put(newPin, newPin);
		}
		else {
			schemPin.setConnectivity(origPin);
		}
	}

	private void processDaisyChainShieldConnections(@NotNull Set<IShieldBody> shieldBodies)
	{
		if (!m_cutInProgress) {
			return;
		}

		// we disconnect shield-to-pin connection when we delete/break the daisy chain
		// re-evaluate shield connections after paste

		Set<IChainSegmentContainer> shieldChains = new HashSet<>();
		for (IShieldBody shieldBody : shieldBodies) {
			Collection<IShieldBodyHookup> shieldBodyHookups = shieldBody.getShieldBodyHookups();
			for (IShieldBodyHookup shieldBodyHookup : shieldBodyHookups) {
				shieldChains.addAll(shieldBodyHookup.getShieldChains());
			}
		}
		for (IChainSegmentContainer chain : shieldChains) {
			Iterator<IShieldBodyHookup> iterator = chain.getAttachedHookups().iterator();
			IShieldBodyHookup hookup1 = iterator.hasNext() ? iterator.next() : null;
			IShieldBodyHookup hookup2 = iterator.hasNext() ? iterator.next() : null;
			if (hookup1 != null && hookup2 != null) {
				DaisyChainCreationHelper.connectShield(hookup1, hookup2);
			}
		}
	}

	private void setSymbolRefOnSchemPin(@NotNull IConnectivityRef uidObj)
	{
		IGenericSchemPin schemPin = CommonUtils.cast(uidObj, IGenericSchemPin.class);
		assert schemPin != null;

		ISharedPin sharedPin = schemPin.getConnectivity().getSharedPin();
		if (sharedPin == null) {
			return;
		}

		ISharedPinList sharedPinList = sharedPin.getOwner();
		IPinList schemPinList = CommonUtils.cast(schemPin.getParent(), IPinList.class);
		assert schemPinList != null;

		int symbolInstanceNumber = schemPinList.getSymbolInstanceNumber();
		ISymbolRef symbolRef = schemPinList.getSymbolRef();
		IUID symbolDefUID = ((sharedPinList instanceof ISharedGroundDevice) && symbolRef != null)
				? symbolRef.getSymbolUID()
				: schemPinList.getSymbolDefUID();

		if (sharedPinList instanceof ISharedGroundDevice && symbolInstanceNumber == -1 && symbolDefUID != null) {
			symbolInstanceNumber =
					SymbolInstanceHelper.getValidInstanceNumber(schemPinList, sharedPinList, symbolDefUID);
		}

		IPin symbolPin = sharedPinList.getSymbolPin(symbolDefUID, symbolInstanceNumber, sharedPin);
		if (symbolPin != null) {
			schemPin.setSymbolPinUID(symbolPin.getUID());
		}
	}

	private void processAssemblies(CreationDeletionHelper cdh, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap, ISchemDiagram diagram)
	{
		for (IAssembly assembly : assembliestobeRemoved) {
			if (assembly != null) {
				if (newVsOldCableAssemblyMap.get(assembly) != null) {
					IAssembly cableAssembly = newVsOldCableAssemblyMap.get(assembly);
					m_pastedSelectSet.add(new Selection(cableAssembly));
					if (diagram.getRepresentation(cableAssembly) != null) {
						m_pastedSelectSet.add(new Selection(diagram.getRepresentation(cableAssembly)));
//						for(IGfxObject gfxObj : m_schemAssemblygraphics.keySet()){
//							IUIDObject uidObj = m_schemAssemblygraphics.get(gfxObj);
//							pastedSelectSet.add(new Selection(diagram.getRepresentation(cableAssembly)));
//						}
					}
				}
				m_pastedSelectSet.remove(assembly.getUID());
				cdh.addDeletionObject(assembly);
			}
		}
	}

	private void reassignConnectivityforHighway(IDesignWideUsageMgr dwum, CreationDeletionHelper cdh,
			IConnectivityRef uidObj, int grid)
	{
		IHighwaySchematic schemHighway = (IHighwaySchematic) uidObj;
		IHighway connToBeDeleted = schemHighway.getConnectivity();

		IGeneralHighway oldHighway = CommonUtils.cast(m_recorderdConnectivity.get(uidObj), IGeneralHighway.class);

		if(oldHighway != null) {
			IConductorIterator newConductors = HighwayHelper.toAllConductors(connToBeDeleted);
			newConductors.stream().forEach(cond -> {
				IHighwayConductor oldHighwayConductor =
						CommonUtils.cast(m_newVsOriginalConnectivity.get(cond), IHighwayConductor.class);
				if (oldHighwayConductor != null) {
					oldHighway.addConductor(oldHighwayConductor);
				}
			});
		}

		updatePortGraphics(dwum, uidObj);
		schemHighway.setConnectivity((IHighway) m_recorderdConnectivity.get(uidObj));
		PortHelper.updatePortGfx(schemHighway, grid);
		if (LogicUtils.getLogicObjectUsageCount(connToBeDeleted) == 0) {
			cdh.addDeletionObject(connToBeDeleted);
			IConductorIterator condIt = HighwayHelper.toStackPinConductors(connToBeDeleted);
			while (condIt.hasNext()) {
				cdh.addDeletionObject(condIt.next());
			}
		}

		schemHighway.reassignConnectivityforCompositeTextDecorations();
	}

	private void reassignConnectivityforShieldBody(IConnectivityRef uidObj)
	{
		IShieldBody newSchemShieldBody = (IShieldBody) uidObj;
		IMulticore multicoreToBeSet = (IMulticore) m_recorderdConnectivity.get(uidObj);
		newSchemShieldBody.setConnectivity(multicoreToBeSet.getShieldBody());

		//Shield Body Hookups are replicated by default though they are not available for Shared Multicore
		Set<IShieldBodyHookup> hookupsToBeDeleted = new HashSet<IShieldBodyHookup>();
		ISharedMulticore sharedMulticore = multicoreToBeSet.getSharedMulticore();
		if (sharedMulticore != null && sharedMulticore.getShield() == null) {
			for (IShieldBodyHookup hookup : newSchemShieldBody.getObjects(IShieldBodyHookup.class)) {
				hookupsToBeDeleted.add(hookup);
			}
			for (IShieldBodyHookup hookup : hookupsToBeDeleted) {
				newSchemShieldBody.removeObject(hookup);
			}
		}
	}

	private Collection<ILogicObject> findLogicObjectsToLockForReassign(IUIDObject uidObj)
	{
		Collection<ILogicObject> logicObjectsToLock = new LinkedHashSet<>();
		if (uidObj instanceof IConnectivityRef) {
			ILogicObject sourceLogicObject = m_recorderdConnectivity.get(uidObj);
			if (sourceLogicObject != null) {
				logicObjectsToLock.add(sourceLogicObject);
			}
		}

		return logicObjectsToLock;
	}

	private void reassignConnectivityforPinList(CreationDeletionHelper cdh, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap, IPinList uidObj)
	{
		reassignConnectivityforConnRef(cdh, assembliestobeRemoved, newVsOldCableAssemblyMap, uidObj,
				connRef -> connRef.getConnectivity(), (connRef, logObj) -> connRef.setConnectivity(logObj));
	}

	private <T extends IConnectivityRef, S extends ILogicObject> void reassignConnectivityforConnRef(
			CreationDeletionHelper cdh, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap, T uidObj,
			Function<T, S> getConn, BiConsumer<T, S> setConn)
	{
		S connToBeDeleted = getConn.apply(uidObj);
		S oldConnectivity = (S) m_recorderdConnectivity.get(uidObj);
		if (connToBeDeleted != null) {
			if (connToBeDeleted.getAssembly() != null) {
				assembliestobeRemoved.add(connToBeDeleted.getAssembly());
				newVsOldCableAssemblyMap.put(connToBeDeleted.getAssembly(), oldConnectivity.getAssembly());
			}
			setConn.accept(uidObj, oldConnectivity);
			if (LogicUtils.getLogicObjectUsageCount(connToBeDeleted) == 0) {
				cdh.addDeletionObject(connToBeDeleted);
			}
		}
	}

	private void reassignConnectivityforLayoutComonent(CreationDeletionHelper cdh, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap, ISchemOtherComponent uidObj)
	{
		reassignConnectivityforConnRef(cdh, assembliestobeRemoved, newVsOldCableAssemblyMap, uidObj,
				connRef -> connRef.getConnectivity(), (connRef, logObj) -> connRef.setConnectivity(logObj));
	}

	private void reassignConnectivityforconductors(ISchemDiagram diag, IDesignWideUsageMgr dwum,
			CreationDeletionHelper cdh, Set<IMulticore> multicorestobeRemoved, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap,
			List<? extends IConnectivityRef> conductors, int grid)
	{
		Map<chs.cof.logical.cable.IConductor, chs.cof.logical.cable.IConductor> condsToBeProcessed =
				new LinkedHashMap<>();
		for (IConnectivityRef uidObj : conductors) {
			chs.cof.logical.cable.IConductor connToBeDeleted = ((IConductor) uidObj).getConnectivity();
			chs.cof.logical.cable.IConductor connToBeSet =
					(chs.cof.logical.cable.IConductor) m_recorderdConnectivity.get(uidObj);

			if (connToBeDeleted != null && connToBeDeleted.getAssembly() != null) {
				assembliestobeRemoved.add(connToBeDeleted.getAssembly());
				newVsOldCableAssemblyMap.put(connToBeDeleted.getAssembly(), connToBeSet.getAssembly());
			}

			if (connToBeDeleted != null && connToBeDeleted instanceof IShieldConductor &&
					!canUseExistingShield(uidObj)) {
				continue;
			}

			if (connToBeDeleted != null) {
				IMulticore multicore = connToBeDeleted.getMulticore();
				if (multicore != null) {
					multicorestobeRemoved.add(multicore);
					IMulticore root = multicore.getRootMulticore();
					multicorestobeRemoved.addAll(root.getAllMulticoresInHierarchy());
				}
			}

			updatePortGraphics(dwum, uidObj);
			((IConductor) uidObj).setConnectivity(connToBeSet);

			Set<IHighwaySchematic> highwaySchematics = ((IConductor) uidObj).connectedHighways();
			long count = highwaySchematics.stream().filter(highway -> !highway.getStackedPinConductors().isEmpty()).count();
			if (m_cutInProgress && count == 0) {
				PortHelper.updatePortGfx(uidObj, grid);
			}
			else {
				PortHelper.addPortGfx(uidObj, grid);
			}

			if (connToBeDeleted != null) {
				condsToBeProcessed.put(connToBeDeleted, connToBeSet);
			}
		}

		Set<IMulticore> seen = new HashSet<IMulticore>();
		for (Map.Entry<chs.cof.logical.cable.IConductor, chs.cof.logical.cable.IConductor> entry : condsToBeProcessed
				.entrySet()) {
			chs.cof.logical.cable.IConductor connToBeDeleted = entry.getKey();
			chs.cof.logical.cable.IConductor connToBeSet = entry.getValue();
			//Add Indicator if it is part of Mutlicore
			if (connToBeDeleted != null && connToBeSet.getMulticore() != null &&
					!(connToBeSet instanceof IShieldConductor)) {
				diag.refreshRepresentations();
				SharedConductorHelper.fixupParentageForConductor(connToBeSet, diag.getDesign(), diag, seen, true);
			}
			if (connToBeDeleted != null && LogicUtils.getLogicObjectUsageCount(connToBeDeleted) == 0) {
				cdh.addDeletionObject(connToBeDeleted);
			}
		}
	}

	private boolean canUseExistingShield(IConnectivityRef uidObj)
	{
		ILogicObject connToBeDeleted = uidObj.getConnectivity();
		ILogicObject connToBeSet = m_recorderdConnectivity.get(uidObj);
		if (connToBeDeleted instanceof IShieldConductor && connToBeSet instanceof IShieldConductor) {
			IShieldConductor shieldCondToDelete = (IShieldConductor) connToBeDeleted;
			IShieldConductor shieldCondToSet = (IShieldConductor) connToBeSet;
			if (shieldCondToDelete.getMulticore() == null && shieldCondToSet.getMulticore() == null) {
				return true;
			}
			if (shieldCondToDelete.getMulticore() == null || (shieldCondToSet.getMulticore() != null &&
					!m_multicores.contains(shieldCondToDelete.getMulticore()))) {
				return false;
			}
			else {
				return true;
			}
		}
		return false;
	}

	private void reassignConnectivityforShieldconductor(Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap, IConnectivityRef uidObj)
	{
		chs.cof.logical.cable.IConductor connToBeDeleted = ((IConductor) uidObj).getConnectivity();
		chs.cof.logical.cable.IConductor connToBeSet =
				(chs.cof.logical.cable.IConductor) m_recorderdConnectivity.get(uidObj);

		if (connToBeDeleted.getAssembly() != null && connToBeDeleted.getAssembly() != connToBeSet.getAssembly()) {
			assembliestobeRemoved.add(connToBeDeleted.getAssembly());
			newVsOldCableAssemblyMap.put(connToBeDeleted.getAssembly(), connToBeSet.getAssembly());
		}
		if (!canUseExistingShield(uidObj)) {
			processShieldConductors(uidObj, connToBeDeleted, connToBeSet);
		}
		else {
			((IConductor) uidObj).setConnectivity(connToBeSet);
		}

//		if (connToBeDeleted instanceof IShieldConductor && (connToBeDeleted.getMulticore() == null ||
//				connToBeSet.getMulticore() != null && !m_multicores.contains(connToBeDeleted.getMulticore()))) {
//			processShieldConductors(uidObj, connToBeDeleted, connToBeSet);
//		}
	}

	private void processShieldConductors(IConnectivityRef uidObj, chs.cof.logical.cable.IConductor connToBeDeleted,
			chs.cof.logical.cable.IConductor connToBeSet)
	{
		for (IPin pin : ((IConductor) uidObj).getPins()) {
			pin.getConnectivity().addConductor(connToBeDeleted); // add new Indicator
		}
		if (connToBeSet.getAssembly() != null) {
			connToBeSet.getAssembly().addElement(connToBeDeleted);
		}
	}

	private void reassignConnectivityforSchemAssembly(ISchemDiagram diag, CreationDeletionHelper cdh,
			IConnectivityRef uidObj, Set<IAssembly> assembliestobeRemoved,
			Map<IAssembly, IAssembly> newVsOldCableAssemblyMap)
	{
		chs.cof.logical.schem.IAssembly schemAssembly = (chs.cof.logical.schem.IAssembly) uidObj;
		IAssembly cableAssemblytobeDeleted = schemAssembly.getConnectivity();
		IAssembly connectivityAssembly = (IAssembly) m_recorderdConnectivity.get(uidObj);
		chs.cof.logical.schem.IAssembly origSchemAssembly =
				(chs.cof.logical.schem.IAssembly) diag
						.getRepresentation(connectivityAssembly);
		IGfxObjectIterator gobjs = schemAssembly.getObjects();
		if (origSchemAssembly != null) {
			while (gobjs.hasNext()) {
				IGfxObject gobj = gobjs.getNext();
				schemAssembly.removeObject(gobj);
				origSchemAssembly.addObject(gobj);
			}

			cdh.addDeletionObject(schemAssembly);
		}
		else {
			schemAssembly.setConnectivity(connectivityAssembly);
			diag.addObject(schemAssembly);
		}
		newVsOldCableAssemblyMap.put(cableAssemblytobeDeleted, connectivityAssembly);
		assembliestobeRemoved.add(cableAssemblytobeDeleted);
	}

	private void updatePortGraphics(@NotNull IDesignWideUsageMgr dwum, IConnectivityRef uidObj)
	{
		if (dwum.getDesignSharedUsageCount(
				m_recorderdConnectivity.get(uidObj)) == 1) {
			for (IDiagramObject rep : dwum.getRepresentations(
					m_recorderdConnectivity.get(uidObj))) {
				ISchemDiagram diagram = DiagramHelper.getDiagram(rep);
				if (rep instanceof ICompoundObject && diagram != null && diagram.isEditable()) {
					PortHelper.addPortGfx((ICompoundObject) rep, diagram.getGrid().getGridSpacing());
				}
			}
		}
	}

	private boolean disallowLogicPinDuplication(IProject project)
	{
		IProjectPreferenceMgr preferences = ProjectHelper.getProjectPreferences(project);
		return preferences.getDisallowLogicPinDuplication();
	}

	protected boolean areAllObjectsInPasteLocked(ILogicDesign logicDesign, List<IUIDObject> bufferCopy)
	{
		Collection<ILogicObject> mustLockObjects = new LinkedHashSet<>();
		Collection<ILogicObject> logicObjectsToLock = new LinkedHashSet<>();
		for (IUIDObject uidObj : bufferCopy) {
			if (uidObj instanceof IConductor &&
					((IConductor) uidObj).getConnectivity() instanceof IShieldConductor &&
					!canUseExistingShield((IConductor) uidObj)) {
				IConductor schemShieldCond = (IConductor) uidObj;

				ILogicObject cableShieldThatWillBeUsed = m_recorderdConnectivity.get(schemShieldCond);
				if (cableShieldThatWillBeUsed instanceof IShieldConductor) {
					mustLockObjects.addAll(((IShieldConductor) cableShieldThatWillBeUsed).getPinSet());
				}
			}

			logicObjectsToLock.addAll(findLogicObjectsToLockForReassign(uidObj));
		}

		for (ILogicObject objectToLock : logicObjectsToLock) {
			mustLockObjects.add(objectToLock);
			if (objectToLock instanceof IMulticore) {
				IMulticore multicoreToCopy = (IMulticore) objectToLock;
				mustLockObjects.add(multicoreToCopy.getShieldBody());
				mustLockObjects.addAll(multicoreToCopy.getAllConductorsInHierarchy());
			}
		}

		Collection<IUID> lockFailedUIDs =
				LogicObjectLockFinder.tryEdit(logicDesign, mustLockObjects);
		if (!lockFailedUIDs.isEmpty()) {
			addOrRemovePreviousCopiedObjectsFromUIDMgr(false);
			return false;
		}
		Collection<IUIDObject> nonDeletedObjects =
				UIDUtils.convertToNonDeletedUIDObjects(
						UIDUtils.convertToUIDSet(logicObjectsToLock));
		if (!nonDeletedObjects.containsAll(logicObjectsToLock)) {
			addOrRemovePreviousCopiedObjectsFromUIDMgr(false);
			return false;
		}
		return true;
	}

	protected boolean needsDictionaryEntries()
	{
		return true;
	}

	private static class SelectedObjectsByTypeProvider extends SelectSet
	{
		@NotNull private Map<Class<?>, List<?>> m_selectedObjectsByType;

		SelectedObjectsByTypeProvider(@NotNull SelectSet selectSet)
		{
			add(selectSet);
			m_selectedObjectsByType = new HashMap<>();
		}

		@SuppressWarnings("unchecked")
		@Override
		@NotNull public <T> List<T> getSelectedObjects(@NotNull Class<T> type)
		{
			if (!m_selectedObjectsByType.containsKey(type)) {
				m_selectedObjectsByType.put(type, super.getSelectedObjects(type));
			}
			return (List<T>) m_selectedObjectsByType.get(type);
		}
	}

	private boolean canCopySharedObject(@Nullable ISharedObject sharedObject)
	{
		if (sharedObject == null) {
			return true;
		}
		Boolean canCopySharedObject = availabilityChecker.getCachedResult(sharedObject);
       	return canCopySharedObject != null ? canCopySharedObject : true;
	}

	private void populateSharedObjectAccessibilityMap(SelectSet selSet)
	{
		Collection<ISharedLockableUpdateableObject> sharedObjects = collectCopyableSharedObjects(selSet);
		Map<IUID, RefreshStatusEnum> refreshedObjects = BatchLockRefreshHelper.batchRefresh(sharedObjects);
		//check domain only if object exists
		availabilityChecker = new SharedObjectAvailabilityChecker(sharedObjects.stream()
				.filter(obj -> refreshedObjects.get(obj.getUID()) != RefreshStatusEnum.eObjectDoesNotExist)
				.collect(Collectors.toSet()), null,
				ISharedObjectAvailabilityReporter.NULL_REPORTER, false);
	}

	@NotNull private Collection<ISharedLockableUpdateableObject> collectCopyableSharedObjects(SelectSet selSet)
	{
		SelectedUIDObjectIterator objectIterator = selSet.getSelectedUIDObjects();
		Collection<ISharedLockableUpdateableObject> sharedObjects = new ArrayList<>();
		while (objectIterator.hasNext()) {
			IUIDObject origObject = objectIterator.getNext();
			if (origObject instanceof IDiagramObject) {
				if (isObjectCopyable((IDiagramObject) origObject) || origObject instanceof ILogicSegment) {
					ISharedObject sharedObject = getSharedObject(origObject);
					if (sharedObject != null) {
						ISharedLockableUpdateableObject
								sharedLockableObj =
								CommonUtils.cast(sharedObject, ISharedLockableUpdateableObject.class);
						if (sharedLockableObj != null) {
							sharedObjects.add(sharedLockableObj);
						}
					}
				}
			}
		}
		return sharedObjects;
	}

}
