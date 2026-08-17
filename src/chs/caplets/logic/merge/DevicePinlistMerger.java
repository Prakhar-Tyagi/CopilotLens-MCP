package chs.caplets.logic.merge;

import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.library.FootprintSource;
import chs.cof.logical.FootprintUtils;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceConnectorIterator;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPrivilegedDevicePin;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.user.UserFootprintHelper;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryCavityContainer;
import chs.cof.parts.ILibraryDevicePin;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorDCFeedback;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utilities.SortedList;
import chs.utility.DiagramHelper;
import chs.utility.helpers.DeviceConnectorDeletionTracker;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.UserFootprintUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 17-Mar-2010 Time: 14:27:53
 */
public class DevicePinlistMerger extends PinlistMerger
{

	private Map<IDevicePin, IDeviceConnPin> m_deviceConnectorPinToDevicePin = new HashMap<IDevicePin, IDeviceConnPin>();
	private final boolean m_hasFootprintContext;
	private final Map<IDeviceConnector, IMergeActionChange> m_deviceConnectorsToReport = new HashMap<>();

	public DevicePinlistMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
		IDevice srcDevice = CommonUtils.cast(sourceLogicObject, IDevice.class);
		IDevice tgtDevice = CommonUtils.cast(targetLogicObject, IDevice.class);
		m_hasFootprintContext = (srcDevice != null && tgtDevice != null) && (
				FootprintUtils.hasFootprintContext(srcDevice) || FootprintUtils.hasFootprintContext(tgtDevice));
	}

	protected void preMerge()
	{
		super.preMerge();
		Set<ISchemDiagram> diagrams = new HashSet<>();
		diagrams.addAll(getDiagramsForLogicObjects(getTargetLogicObject(), getSourceLogicObject()));
		for (ISchemDiagram diagram : diagrams) {
			diagram.loadToMemory();
		}
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		IDevice sourceDevice = (IDevice) sourceLogicObject;
		IDevice targetDevice = (IDevice) targetLogicObject;

		mergeDeviceConnectors(sourceDevice, targetDevice);

		super.mergeChildrenConnectivity(sourceLogicObject, targetLogicObject);
		// Moving the plug connectors to target device, *after* merging pins
		// because at that stage we'll only move plugs that still have connected pins to the device
		// as connector pins maybe disconnected when merging device pins
		DeviceAndConnectorMergerHandler.mergeConnectors(sourceDevice, targetDevice);

		//during the above merging operation some device connector, its pins and device pins
		//move from source to target. which leaves some link between dev-conn-pin and dev-pin
		//invalid i.e dev-conn-pin of source refering to devpin from target or vice-versa.
		//this cross linking can goof-up device connector regeneration later.
		ensureValidMatingBetweenDevicePinAndDevConnPins(sourceDevice);
		ensureValidMatingBetweenDevicePinAndDevConnPins(targetDevice);
	}

	private void ensureValidMatingBetweenDevicePinAndDevConnPins(IDevice device)
	{
		IDeviceConnectorIterator deviceConnectors = device.getDeviceConnectors();
		for (IDeviceConnector deviceConnector : deviceConnectors) {
			for (IAbstractPin pin : deviceConnector.getPins()) {
				IDeviceConnPin deviceConnPin = CommonUtils.cast(pin, IDeviceConnPin.class);
				IDevicePin devicePin = deviceConnPin != null ? deviceConnPin.getDevicePin() : null;
				if (devicePin != null && (devicePin.getOwner() != device)) {
					devicePin.setDeviceConnectorPin(null);
					deviceConnPin.setDevicePin(null);
				}
			}
		}
	}

	@Override protected void mergePin(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent,
			IAbstractPin sourcePin,
			IAbstractPin targetPin)
	{
		super.mergePin(sourceParent, targetParent, sourcePin, targetPin);

		if (sourcePin instanceof IDevicePin) {
			IDevicePin devPinToFix = CommonUtils.cast(targetPin != null ? targetPin : sourcePin, IDevicePin.class);
			IDeviceConnPin deviceConnPin = m_deviceConnectorPinToDevicePin.get(sourcePin);
			if (deviceConnPin != null && devPinToFix != null) {
				devPinToFix.setDeviceConnectorPin(deviceConnPin);
				deviceConnPin.setDevicePin(devPinToFix);
			}
		}
	}

	private void mergeDeviceConnectors(@NotNull IDevice sourceDevice, @NotNull IDevice targetDevice)
	{
		transferDeviceConnectorProperties(sourceDevice, targetDevice);

		IUserDeviceFootprint sourceFP = UserFootprintHelper.generateDefaultDeviceFootprint(sourceDevice);
		IUserDeviceFootprint targetFP = UserFootprintHelper.generateDefaultDeviceFootprint(targetDevice);

		DeviceConnectorMergeHandler mergeHandler = new DeviceConnectorMergeHandler(sourceDevice, targetDevice, this);

		Function<String, String> pinMapper = Function.identity();
		String renamePrefix = sourceDevice.getName();
		boolean mergeDeviceConnectors = Optional.ofNullable(sourceDevice.getProject())
				.map(IProject::getPreferences)
				.map(IProjectPreferenceMgr::isMergeDeviceConnectorsEnabled)
				.orElse(false);
		UserFootprintUtils.processFootprintMerge(renamePrefix, sourceFP, targetFP, pinMapper, mergeHandler,
				mergeDeviceConnectors);
		m_deviceConnectorsToReport.putAll(mergeHandler.getDeviceConnectorFeedbacks());

		for (Map.Entry<IDeviceConnector, String> entry : mergeHandler.getSourceDevConnsToMove().entrySet()) {
			IDeviceConnector srcDC = entry.getKey();
			Set<IDevicePin> candidatePinsToMove = mergeHandler.getSourceDevConnPinsToMove().pullReadOnlySafeSet(srcDC);
			for (IDeviceConnPin srcDCPin : CollectionUtils.filterByClass(srcDC.getPins(), IDeviceConnPin.class)) {
				IDevicePin srcDevicePin = srcDCPin.getDevicePin();
				if (srcDevicePin != null && candidatePinsToMove.contains(srcDevicePin)) {
					addDeviceConnectorPinMapping(srcDCPin, srcDevicePin);
				}
			}
			srcDC.setName(entry.getValue());
			srcDC.setOwner(targetDevice);
			setAppropriateFootprintSource(sourceDevice, targetDevice);
			addMapping(srcDC, srcDC);
		}
	}

	private void transferDeviceConnectorProperties(@NotNull IDevice sourceDevice, @NotNull IDevice targetDevice)
	{
		SetMap<chs.cof.logical.cable.IPinList, chs.cof.logical.cable.IPinList> candidatePinLists =
				SetMap.createShallowSetMap();
		//we must get the device connectors from source device. so using name based pin mapping.
		Map<String, IAbstractPin> sourcePins = new HashMap<>();
		sourceDevice.getPins().forEach(p -> sourcePins.put(p.getName(), p));
		Map<IAbstractPin, IAbstractPin> candidatePins = new HashMap<>();
		for (IAbstractPin targetPin : targetDevice.getPins()) {
			IDevicePin sourceDevPin = CommonUtils.cast(sourcePins.get(targetPin.getName()), IDevicePin.class);
			IDeviceConnPin sourceDevConnPin = sourceDevPin != null ? sourceDevPin.getDeviceConnectorPin() : null;
			chs.cof.logical.cable.IPinList sourceDevConn =
					(sourceDevConnPin != null) ? sourceDevConnPin.getOwner() : null;
			if (sourceDevConn != null) {
				IDevicePin targetDevPin = CommonUtils.cast(targetPin, IDevicePin.class);
				IDeviceConnPin targetDevConnPin = targetDevPin != null ? targetDevPin.getDeviceConnectorPin() : null;
				chs.cof.logical.cable.IPinList targetDevConn =
						targetDevConnPin != null ? targetDevConnPin.getOwner() : null;
				if (targetDevConn != null) {
					candidatePinLists.add(targetDevConn, sourceDevConn);
					candidatePins.put(targetDevConnPin, sourceDevConnPin);
				}
			}
		}

		SortedList<chs.cof.logical.cable.IPinList> devConnsList =
				new SortedList<>(new NamedObjectComparator<>(true, true, true));
		for (Map.Entry<chs.cof.logical.cable.IPinList, Set<chs.cof.logical.cable.IPinList>> entry : candidatePinLists.entrySet()) {
			devConnsList.clear();
			devConnsList.addAll(entry.getValue());
			for (chs.cof.logical.cable.IPinList sourceDevConn : devConnsList) {
				//must retain the source properties to be processed for other candidates.
				PropertyHelper.transferProperties(sourceDevConn, entry.getKey(), false);
				mergeAttributes(sourceDevConn, entry.getKey());
			}
		}

		for (Map.Entry<chs.cof.logical.cable.IPinList, Set<chs.cof.logical.cable.IPinList>> entry : candidatePinLists.entrySet()) {
			for (chs.cof.logical.cable.IPinList devConn : entry.getValue()) {
				devConn.removeAllProperties();
			}
		}

		for (Map.Entry<IAbstractPin, IAbstractPin> entry : candidatePins.entrySet()) {
			mergeProperties(entry.getValue(), entry.getKey());
			mergeAttributes(entry.getValue(), entry.getKey());
		}
	}

	private void setAppropriateFootprintSource(@NotNull IDevice sourceDevice, @NotNull IDevice targetDevice)
	{
		// FootprintUtils.determineFootprintSource(targetDevice) gives FootprintSource.UserDefined by default for a device even if target.getStoredDeviceSideFootprintSource is FootprintSource.None
		if (FootprintUtils.determineFootprintSource(targetDevice) == FootprintSource.UserDefined &&
				FootprintUtils.determineFootprintSource(sourceDevice) == FootprintSource.UserDefined) {
			targetDevice.setDeviceSideFootprintSource(FootprintSource.UserDefined);
		}
	}

	void addDeviceConnectorPinMapping(@NotNull IDeviceConnPin srcDCPin, @NotNull IDevicePin srcDevicePin)
	{
		m_deviceConnectorPinToDevicePin.put(srcDevicePin, srcDCPin);
	}

	@Override
	protected void fixupConnections(IAbstractPin pin, IAbstractPin targetPin)
	{

		assertValidityOfMerge(pin, targetPin);
		super.fixupConnections(pin, targetPin);

		if (pin instanceof IDevicePin && targetPin instanceof IDevicePin) {
			IDevicePin sourceDevicePin = (IDevicePin) pin;
			IDevicePin targetDevicePin = (IDevicePin) targetPin;
			IDevicePin connectedDevicePin = sourceDevicePin.getConnectedDevicePin();
			IDevicePin connectedTargetDevicePin = targetDevicePin.getConnectedDevicePin();
			if (connectedDevicePin != null && connectedTargetDevicePin == null) {
				targetDevicePin.setConnectedDevicePin(connectedDevicePin);
			}
		}
	}

	@Override
	protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);

		//Disconnect any invalid attachments
		IPinList schemDevice = (IPinList) sourceSchemObject;
		for (IPinList schemAttachedPinlist : schemDevice.getAttachedPinListObjects()) {
			if (!(schemAttachedPinlist.getConnectivity() instanceof IDeviceConnector)) {
				detachUnconnectedConnector(schemAttachedPinlist, schemDevice);
			}
		}

		resetSchematicsConnectivity(sourceSchemObject, targetlogicObject);

		for (IPinList attachedSchem : ((IPinList) sourceSchemObject).getAttachedPinListObjects()) {
			if (attachedSchem.getConnectivity() instanceof IDeviceConnector) {
				attachedSchem.setConnectivity(
						(chs.cof.logical.cable.IPinList) getMappedValue(attachedSchem.getConnectivity()));
			}
		}
	}

	@Override
	protected void postMergingComplete()
	{
		IDevice targetDevice = CommonUtils.cast(getTargetLogicObject(), IDevice.class);
		if (targetDevice == null || !m_hasFootprintContext) {
			return;
		}
		Generator generator = Generator.getGenerator();
		generator.regenerateDeviceConnectors(targetDevice, FactoryMgr.getCommonFactory(), true);
		DeviceConnectorDeletionTracker.execute(() -> {
			processSchematicsFor(getTargetLogicObject(), new ISchematicProcessor()
			{
				public void process(IConnectivityRef schemObject)
				{
					if (isSchematicProcessed(schemObject.getUID())) {
						return;
					}
					regenerateConnectors(schemObject);
				}
			});
		});
		super.postMergingComplete();
	}

	private void regenerateConnectors(IConnectivityRef schemObject)
	{
		Generator generator = Generator.getGenerator();
		generator.regenerateSchemDeviceConnectors((IPinList) schemObject,
				DiagramHelper.createGeneratorParameters((IDiagramObject) schemObject),
				new GeneratorDCFeedback());
		//device connector type of footprint will also be performing ghc now.
		ISchemDiagram diagram = DiagramHelper.getDiagram((IDiagramObject) schemObject);
		assert diagram != null;
		GenerateHarnessConnActionHelper harnessGeneratorHelper = new GenerateHarnessConnActionHelper(diagram);
		harnessGeneratorHelper.generateHarnessConnectorsForPinlist((IPinList) schemObject);
	}

	@Override
	public void addProcessedSchematic(@NotNull IConnectivityRef schemSourceObject)
	{
		//processing is still pending on source if there are device connectors associated. regenerateConnectors is still pending on them.
		if (!m_hasFootprintContext) {
			super.addProcessedSchematic(schemSourceObject);
		}
	}

	public static Mergeable areMergeable(IDevice sourceObject, IDevice targetObject)
	{
		Mergeable canMerge = SymbolDevicePinlistMerger.areMergable(sourceObject, targetObject);
		if (canMerge != Mergeable.Possible) {
			return canMerge;
		}

		Map<String, IAbstractPin> srcPinsMap = new HashMap<String, IAbstractPin>();
		Mergeable isMergeable = Mergeable.Possible;

		Collection<IAbstractPin> devicePins = sourceObject.getPinCollection();
		for (IAbstractPin pin : devicePins) {
			srcPinsMap.put(pin.getName(), pin);
		}

		Collection<IAbstractPin> tgtPins = targetObject.getPinCollection();
		Map<String, IAbstractPin> tgtPinsMap = new HashMap<String, IAbstractPin>();
		for (IAbstractPin pin : tgtPins) {
			tgtPinsMap.put(pin.getName(), pin);
		}

		for (String srcPinName : srcPinsMap.keySet()) {
			IAbstractPin tgtPin = tgtPinsMap.get(srcPinName);
			if (tgtPin != null) {
				IAbstractPin srcPin = srcPinsMap.get(srcPinName);
				if (srcPin.isMated() && tgtPin.isMated()) {
					// Anything other than ring terminals, canNOT be merged/transferred into target
					if (!isTransferRingTerminals(tgtPin, srcPin)) {
						isMergeable = Mergeable.SourceAndTargetHaveDifferentMates;
						break;
					}
				}
			}
		}

		if (isMergeable == Mergeable.Possible) {
			isMergeable = areStudDefinitionsMatchLibrary(sourceObject, targetObject);
		}
		return isMergeable;
	}

	private static Mergeable areStudDefinitionsMatchLibrary(IDevice sourceObject, IDevice targetObject)
	{
		// This method is not checking the default library cavity rules - like numpins, cavity names.
		// These default library rules are validated in PinListMerger.areMergeable().
		if (sourceObject.getLibraryObject() != null && targetObject.getLibraryObject() != null) {
			// at this stage where
			return Mergeable.Possible;
		}

		ILibraryBaseObject libraryObject = sourceObject.getLibraryObject();
		IDevice pinlist = targetObject;
		if (libraryObject == null) {
			libraryObject = targetObject.getLibraryObject();
			pinlist = sourceObject;
		}
		if (libraryObject != null && libraryObject instanceof ILibraryCavityContainer) {
			Set<ILibraryCavity> cavities = LibraryHelper.getCavities((ILibraryObject) libraryObject);
			Map<String, Boolean> nameVsStud = new HashMap<String, Boolean>();
			for (ILibraryCavity cavity : cavities) {
				if (cavity instanceof ILibraryDevicePin) {
					nameVsStud.put(cavity.getName(), ((ILibraryDevicePin) cavity).getStud().isTrue());
				}
			}

			for (IAbstractPin pin : pinlist.getPinCollection()) {
				Boolean isLibPinStud = nameVsStud.get(pin.getName());
				if (isLibPinStud != null) {
					// STUD is priority.
					// If the lib pin is stud and other pin is NORMAL then the NORMAL can be converted to STUD,
					// as the library also matches.
					// if the library pin is NORMAL (NON-STUD) and the other pin is STUD, then the priority for
					// STUD cannot be honored, because library defintion does not match. So always make this case
					// fail.
					if (!isLibPinStud && isLibPinStud != pin.isStudPin()) {
						return Mergeable.ConversionOfStudToNormalNotPossible;
					}
				}
			}
		}
		return Mergeable.Possible;
	}

	private static boolean isTransferRingTerminals(IAbstractPin tgtPin, IAbstractPin srcPin)
	{
		Collection<chs.cof.logical.cable.IPinList> srcConnectedPinLists = srcPin.getConnectedPinLists();
		Collection<chs.cof.logical.cable.IPinList> tgtConnectedPinLists = tgtPin.getConnectedPinLists();

		return (srcPin.isStudPin() || tgtPin.isStudPin())
				&& isAllRingTerminals(srcConnectedPinLists) && isAllRingTerminals(tgtConnectedPinLists);
	}

	private static boolean isAllRingTerminals(Collection<chs.cof.logical.cable.IPinList> pinLists)
	{
		boolean isAllRingTerms = true;
		for (chs.cof.logical.cable.IPinList pinList : pinLists) {
			if (pinList instanceof IConnector) {
				if (!((IConnector) pinList).isRingTerminal()) {
					isAllRingTerms = false;
				}
			}
			else {
				isAllRingTerms = false;
				break;
			}
		}
		return isAllRingTerms;
	}

	private void assertValidityOfMerge(IAbstractPin pin, IAbstractPin targetPin)
	{
		boolean isValid = false;
		if (!targetPin.isMated() || !pin.isMated()) {
			isValid = true;
		}
		else // both are mated.
			if (isTransferRingTerminals(targetPin, pin)) {
				isValid = true;
			}

		assert isValid : "The source and target pins should not be mated";
	}

	protected void mergePinAttributes(IAbstractPin sourcePin, @Nullable IAbstractPin targetPin)
	{
		if (!(sourcePin instanceof IDevicePin)) {
			// we get IDeviceConnPin also to this method.
			return;
		}

		if (targetPin == null) {
			boolean isStud = evaulateStudAttrValue(sourcePin);
			((IPrivilegedDevicePin) sourcePin).setStud(isStud); // the source pin will become the target pin.
		}
		else {
			if (((IDevicePin) sourcePin).isStud()) {
				((IPrivilegedDevicePin) targetPin).setStud(true);
			}
		}
	}

	protected boolean evaulateStudAttrValue(IAbstractPin pin)
	{
		if (!(pin instanceof IDevicePin)) {
			// we get IDeviceConnPin also to this method.
			return false;
		}

		ILibraryBaseObject libraryObject = getSourceLogicObject().getLibraryObject();
		IDevice device = (IDevice) getSourceLogicObject();
		if (libraryObject == null) {
			libraryObject = getTargetLogicObject().getLibraryObject();
			device = (IDevice) getTargetLogicObject();
		}
		// if there is library part:
		// if there is no target pin, let the library decide the stud attribute
		// if there is no source pin, let the library decide the stud attribute on the target pin
		boolean isStud = ((IDevicePin) pin).isStud();
		if (libraryObject != null && libraryObject instanceof ILibraryCavityContainer) {
			ILibraryCavity libCavity = LibraryHelper.getLibraryCavity(device, pin);
			if (libCavity instanceof ILibraryDevicePin) { //SP1510: dts0101152173: [CH] java.lang.NullPointerException  at chs.caplets.logic.merge.DevicePinlistMerger.evaulateStudAttrValue(DevicePinlistMerger
				isStud = ((ILibraryDevicePin) libCavity).getStud().isTrue();
			}
			//else
			//There might be few pins on device which are not available in library.
			//Update part might not have been performed after renaming a pin in library.
			//In which case, LibraryHelper.getLibraryCavity(device, pin); returns null
		}

		return isStud;
	}

	protected final void mergeAbstractPins(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		List<IAbstractPin> tgtOnlyPins = getPinsOnlyOnTargetPL(sourceParent, targetParent);
		super.mergeAbstractPins(sourceParent, targetParent);

		for (IAbstractPin devPin : tgtOnlyPins) {
			if (devPin instanceof IPrivilegedDevicePin) {
				boolean isStud = evaulateStudAttrValue(devPin);
				((IPrivilegedDevicePin) devPin).setStud(isStud);
			}
		}
	}

	private List<IAbstractPin> getPinsOnlyOnTargetPL(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		List<IAbstractPin> tgtOnlyPins = new LinkedList<IAbstractPin>();
		Map<IAbstractPin, IAbstractPin> tmpPinMapping = getPinMappingForMerge(targetParent, sourceParent);
		for (Map.Entry<IAbstractPin, IAbstractPin> entry : tmpPinMapping.entrySet()) {
			IAbstractPin srcPin = entry.getKey();
			if (entry.getValue() == null) {
				tgtOnlyPins.add(srcPin);
			}
		}

		return tgtOnlyPins;
	}

	@Override protected void reportChanges()
	{
		super.reportChanges();
		Set<IDeviceConnector> invalidDeviceConnectors = m_deviceConnectorsToReport.keySet().stream()
				.filter(IUIDObject::isDeletedObject)
				.collect(Collectors.toSet());
		invalidDeviceConnectors.stream().forEach(m_deviceConnectorsToReport::remove);
		m_deviceConnectorsToReport.values().stream()
				.sorted((change1, change2) -> AlphaNumComparator.getCaseSensitiveComparator()
						.compare(change1.getSourceObjectName(), change2.getSourceObjectName()))
				.forEach(m_reporter::report);
	}
}