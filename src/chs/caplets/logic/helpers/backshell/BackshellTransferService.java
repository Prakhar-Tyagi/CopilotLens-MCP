/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.helpers.backshell;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.snapping.SameSidePinListFinder;
import chs.caplets.logic.merge.IBackshellTransferService;
import chs.caplets.logic.merge.PinListSticher;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBackshellTerminationIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cofUtils.parameterized.BackshellGraphicsRebuilder;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinCreationDataProvider;
import chs.cofUtils.parameterized.PinGroupInfo;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.ILocation;
import chs.subsystem.backshell.impl.BackshellServices;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service class responsible for transferring backshell terminations from a source device connector
 */
public class BackshellTransferService implements IBackshellTransferService
{

	@NotNull private final BackshellBuilder m_backshellBuilder;
	@NotNull private final List<IValidator> m_validators;
	@NotNull private final BackshellTransferMessageHelper m_transferMessageHelper;
	@NotNull private BackshellTransferReporter m_reporter;
	@NotNull private BackshellCleanupHelper m_backshellCleanupHelper;
	@NotNull private IBackshellPinOverlapResolver m_backshellPinOverlapResolver;
	@NotNull private final Set<IPinList> m_affectedSchemDevices = new HashSet<>();
	@NotNull private final Set<IPinList> m_affectedSchemPlugConnectors = new HashSet<>();

	public BackshellTransferService(@NotNull IBackshellPinOverlapResolver backshellPinOverlapResolver)
	{
		m_backshellPinOverlapResolver = backshellPinOverlapResolver;
		m_backshellBuilder = new BackshellBuilder();
		m_validators = List.of(
				new LibraryPartValidator(),
				new SharedObjectValidator()
		);
		m_reporter = new BackshellTransferReporter();
		m_transferMessageHelper = new BackshellTransferMessageHelper();
		m_backshellCleanupHelper = new BackshellCleanupHelper();
	}

	private void prepareAndTransferBackshellTerminations(@NotNull IDeviceConnector deviceConnector,
			@NotNull IPinList schemDevice, @NotNull IPinList schemConnector, boolean processAllInstance)
	{
		IPinList targetSchemConnector = SchemPinListHelper.determineModularRootPinList(schemConnector);
		IConnector targetPlugConnector = (IConnector) targetSchemConnector.getConnectivity();
		ISharedPinList sharedConnector = targetPlugConnector.getSharedPinList();
		for (IValidator validator : m_validators) {
			if (!validator.validate(deviceConnector, targetPlugConnector, m_reporter)) {
				return;
			}
		}

		if (sharedConnector != null && m_reporter.isSharedConnectorLockNeeded()) {
			try (LockedSharedConnectorInBackshellTransfer lockHelper = new LockedSharedConnectorInBackshellTransfer()) {
				if (lockHelper.attemptLock(deviceConnector, targetPlugConnector, sharedConnector, m_reporter)) {
					transferDSCBackshell(deviceConnector, schemDevice, targetSchemConnector, processAllInstance);
				}
			}
		}
		else {
			transferDSCBackshell(deviceConnector, schemDevice, targetSchemConnector, processAllInstance);
		}
	}

	private void transferDSCBackshell(@NotNull IDeviceConnector deviceConnector,
			@NotNull IPinList schemDevice, @NotNull IPinList targetSchemConnector, boolean processAllInstance)
	{
		IBackshell sourceBackshell = deviceConnector.getBackshell();
		if (sourceBackshell == null) {
			return;
		}

		IBackshellTerminationIterator terminations = sourceBackshell.getBackshellTerminations();
		if (terminations == null) {
			return;
		}

		IConnector targetPlugConnector = (IConnector) targetSchemConnector.getConnectivity();
		Map<IBackshellTermination, IBackshellTermination> sourceTermToTargetTermMap = new HashMap<>();

		// Step 1: Create/reuse target backshell + termination at connectivity level
		IBackshell targetBackshell = m_backshellBuilder.getOrCreateBackshell(sourceBackshell, targetPlugConnector);
		for (IBackshellTermination sourceTermination : terminations) {
			IBackshellTermination targetTermination =
					m_backshellBuilder.getOrCreateTargetTermination(sourceTermination, targetBackshell);
			sourceTermToTargetTermMap.put(sourceTermination, targetTermination);
		}

		BackshellTransferContext backshellTransferContext =
				new BackshellTransferContext(schemDevice, targetPlugConnector, sourceBackshell, targetBackshell,
						sourceTermToTargetTermMap, processAllInstance);

		// Step 2: Transfer connectivity-level conductor connections
		sourceTermToTargetTermMap.forEach((t1, t2) -> PinUtils.transferConductorConnections(t1, t2));

		// Step 3: transfer the schem backshell termination to plug schem connector
		transferSchematicInstances(backshellTransferContext, targetSchemConnector);
	}

	/**
	 * Updates ALL schematic instances across ALL diagrams in the design.
	 * For each diagram:
	 * 1) Find all schematic device instances referencing the source device
	 * 2) For each source BT schem pin found on device instances:
	 * a) Transfer (reuse) the existing source schem pin to the target plug-side schematic connector
	 * b) Update the pin's connectivity to point to the target backshell termination
	 * c) The pin retains its joints/segments, avoiding the need for joint transfer or deletion
	 */
	private void transferSchematicInstances(@NotNull BackshellTransferContext context,
			@NotNull IPinList preferredSchemConnector)
	{
		IPinList sourcePinList = context.getSchemDevice();
		IDevice device = (IDevice) sourcePinList.getConnectivity();
		ILogicDesign logicDesign = device.getLogicDesign();
		if (logicDesign == null) {
			return;
		}

		//handle current schem device
		IPinList schemDevice = context.getSchemDevice();
		ISchemDiagram diagram = DiagramHelper.getDiagram(schemDevice);
		assert diagram != null;
		transferPinsFromSchemDevice(context, schemDevice, diagram, preferredSchemConnector);

		if (context.isProcessAllInstance()) {
			IDesignWideUsageMgr dwum = logicDesign.getDesignWideUsageMgr();
			for (IDesignSharedUsage usage : dwum.getUsages(device)) {
				ISchemDiagram usage_diagram = usage.getDiagram();
				usage_diagram.loadToMemory();
				IPinList usageSchemDevice = CommonUtils.cast(usage.getDiagramObject(), IPinList.class);
				if (usageSchemDevice == null || usageSchemDevice.equals(schemDevice)) {
					//skip already processed schem device or invalid usage
					continue;
				}
				transferPinsFromSchemDevice(context, usageSchemDevice, usage_diagram, null);
			}
		}
	}

	private void transferPinsFromSchemDevice(@NotNull BackshellTransferContext context,
			@NotNull IPinList schemDevice, @NotNull ISchemDiagram diagram,
			@Nullable IPinList preferredSchemConnector)
	{
		Set<IPin> sourceSchemBSTerms =
				BackshellServices.getInstance()
						.getSchemPinProvider()
						.getBackshellTerminationPins(schemDevice, IPin.class);
		for (IPin sourceSchemBSTerm : sourceSchemBSTerms) {
			IBackshellTermination targetBSTerm =
					context.getTargetTermination((IBackshellTermination) sourceSchemBSTerm.getConnectivity());
			if (targetBSTerm == null) {
				continue;
			}

			// Track source termination and affected PinList for cleanup
			m_backshellCleanupHelper.addAffectedBackshell(context.getSourceBackshell(), context);
			addAffectedSchemDevice(schemDevice);
			IConnector targetPlugConnector = context.getTargetPlugConnector();
			transferSchematicPin(sourceSchemBSTerm, targetBSTerm, schemDevice, diagram, targetPlugConnector,
					preferredSchemConnector);

			//add affected pinlists for schematic regeneration
			for (IPinList attachedPinList : schemDevice.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)) {
				if (attachedPinList.getConnectivity() instanceof IPlugConnector) {
					addAffectedSchemPlugConnector(attachedPinList);
				}
			}
		}
	}

	/**
	 * Tracks a schem device that has been affected by pin transfer and needs regeneration.
	 */
	public void addAffectedSchemDevice(@NotNull IPinList pinList)
	{
		m_affectedSchemDevices.add(pinList);
	}

	/**
	 * Tracks a schem connector that has been affected by pin transfer and needs rebuild of backshell graphics.
	 */
	public void addAffectedSchemPlugConnector(@NotNull IPinList pinList)
	{
		m_affectedSchemPlugConnectors.add(pinList);
	}

	@Nullable
	private IPinList getAttachedSchemConnector(@NotNull IPinList schemDevice, @NotNull IConnector targetPlugConnector,
			@NotNull IPin schemBSTerm)
	{
		List<IPinList> candidateSchemConnectors =
				new SameSidePinListFinder()
						.findMatchingPinListsOnSameSide(schemDevice, targetPlugConnector, schemBSTerm, pinList -> true);
		if (candidateSchemConnectors.isEmpty()) {
			return null;
		}
		return getBestFitCandidateSchemConnector(candidateSchemConnectors);
	}

	@Nullable private IPinList getBestFitCandidateSchemConnector(@NotNull List<IPinList> candidateSchemConnectors)
	{
		return candidateSchemConnectors.getFirst();
	}

	/**
	 * Transfers a single schematic pin from the source device to the target connector
	 * by reusing the existing sourcePin rather than creating a new one.
	 * <p>
	 * If the target connector already has a matching position on the diagram, the pin is
	 * moved to that location. Otherwise, a new harness connector is generated and placed
	 * next to the source device, and the pin is moved there.
	 * <p>
	 * The sourcePin is removed from the source device, added to the target connector,
	 * relocated, and its connectivity is updated to the target termination.
	 * This avoids creation of new schem pins and eliminates the need to delete the source pin.
	 * <p>
	 * When {@code matchedPosition} is an existing {@link IPin} for a <em>different</em>
	 * termination, the transferred pin lands on top of it. The displaced existing pin is added
	 * to {@code coLocatedPins} so the caller can relocate it after all transfers finish.
	 *
	 * @param sourcePin               the schematic pin to transfer
	 * @param targetTerm              the target backshell termination
	 * @param schemDevice             the source schematic device
	 * @param diagram                 the schematic diagram containing the source device
	 * @param targetPlugConnector     the target plug connector
	 * @param preferredSchemConnector preferred target schem connector, or {@code null} to auto-detect
	 */
	private void transferSchematicPin(@NotNull IPin sourcePin,
			@NotNull IBackshellTermination targetTerm,
			@NotNull IPinList schemDevice,
			@NotNull ISchemDiagram diagram,
			@NotNull IConnector targetPlugConnector, @Nullable IPinList preferredSchemConnector)
	{
		IPinList targetSchemConnector = preferredSchemConnector != null
				? preferredSchemConnector
				: getAttachedSchemConnector(schemDevice, targetPlugConnector, sourcePin);

		IGfxObject matchedPosition = getMatchedPosition(sourcePin, schemDevice, targetSchemConnector);
		if (matchedPosition != null) {
			transferPinToMatchedPosition(sourcePin, targetTerm, schemDevice, targetSchemConnector, matchedPosition);
		}
		else {
			transferPinWithNewConnector(sourcePin, targetTerm, schemDevice, diagram, targetSchemConnector,
					targetPlugConnector);
		}
	}

	@Nullable
	private IGfxObject getMatchedPosition(@NotNull IPin sourcePin, @NotNull IPinList schemDevice,
			@Nullable IPinList targetSchemConnector)
	{
		if (targetSchemConnector == null) {
			return null;
		}
		ConnectionHelper connectionHelper = ConnectionHelper.getConnectionHelper(schemDevice, targetSchemConnector);
		if (connectionHelper == null) {
			return null;
		}
		return connectionHelper.getMatchingPinPosition(sourcePin, schemDevice);
	}

	/**
	 * Transfers the existing sourceSchemPin from the source device to the target connector
	 * at the matched position. The pin is removed from the source device, added to the
	 * target connector, relocated, and its connectivity is updated.
	 * <p>
	 * The matchedPosition is a child of targetPinList, so its {@code getLocation()} returns
	 * coordinates relative to targetPinList — which is the correct coordinate system for the
	 * sourceSchemPin after being moved into targetPinList.
	 */
	private void transferPinToMatchedPosition(@NotNull IPin sourceSchemPin,
			@NotNull IBackshellTermination targetTermination,
			@NotNull IPinList sourceSchemDevice,
			@NotNull IPinList targetPinList,
			@NotNull IGfxObject matchedPosition)
	{
		// matchedPosition.getLocation() is already relative to targetPinList
		ILocation newRelativeLocation =
				ILocation.fromXY(matchedPosition.getLocation().getX(), matchedPosition.getLocation().getY());

		// Remove sourceSchemPin from source device and add to target connector
		sourceSchemDevice.removeObject(sourceSchemPin);
		targetPinList.addObject(sourceSchemPin);

		// Set the pin's location to the matched position's relative coordinates within the target PinList
		sourceSchemPin.setLocation(newRelativeLocation);
		sourceSchemPin.setConnectivity(targetTermination);

		// Update the joint to the pin's new absolute position
		updateJointLocation(sourceSchemPin);

		//we need to delete the pin place holder from target pin list if the matched position is pin place holder,
		// otherwise the pin place holder will be overlapped by transferred pin and may snap to wire further in the flow,
		if (matchedPosition instanceof IPinPlaceholder ph) {
			targetPinList.removeObject(ph);
		}
		else if (matchedPosition instanceof IPin existingPin) {
			m_backshellPinOverlapResolver.resolveOverlappedPins(sourceSchemPin, existingPin);
		}
	}

	/**
	 * Creates a new empty harness connector schematic positioned relative to the source device,
	 * then transfers the existing sourcePin from the source device into the new connector.
	 * <p>
	 * The empty connector is created by calling {@code Generator.generateHarnessConnector} with
	 * an empty {@link PinCreationDataProvider}, which generates the connector body, graphics,
	 * and orientation without creating any pins. The sourcePin is then moved into the connector
	 * and placed at the standard pin position within the connector's local coordinate system.
	 * <p>
	 * In a generated harness connector, pins are placed at {@code (parameterizedExtent.getWidth(), 0)}
	 * in the connector's local coordinate system (as done by {@code Generator.addHarnConnPins}).
	 * After setting this relative location, the joint is updated to the pin's new absolute position.
	 */
	private void transferPinWithNewConnector(@NotNull IPin sourcePin,
			@NotNull IBackshellTermination targetTermination,
			@NotNull IPinList sourceSchemDevice,
			@NotNull ISchemDiagram diagram,
			@Nullable IPinList targetSchemConnector,
			@NotNull IConnector targetPlugConnector)
	{
		PinSideCalculator.RelativeSide relSide = new PinSideCalculator.RelativeSide(sourceSchemDevice);
		int side = relSide.theSide(sourcePin);
		PinGroupInfo pinGroupInfo = new PinGroupInfo("Group_0", side);
		pinGroupInfo.addPin(sourcePin);

		// Create an empty PinCreationDataProvider so that generateHarnessConnector
		// builds the connector body/graphics/orientation without creating any pins
		PinCreationDataProvider emptyPinData = new PinCreationDataProvider(Map.of());

		IPinList schematicConnector = Generator.getGenerator().generateHarnessConnector(
				pinGroupInfo,
				emptyPinData,
				targetPlugConnector,
				sourceSchemDevice,
				new GeneratorParameters(diagram.getGrid().getGridSpacing()));
		if (targetSchemConnector != null) {
			schematicConnector.setReferenceWidth(targetSchemConnector.getReferenceWidth());
		}

		attachDeviceAndConnector(sourceSchemDevice, schematicConnector);

		// Transfer the sourcePin from source device to the new empty connector
		sourceSchemDevice.removeObject(sourcePin);
		schematicConnector.addObject(sourcePin);

		// Set the pin's location to the standard pin position within the connector's
		// local coordinate system. In a generated harness connector, the pin x-position
		// is at the parameterized extent's width, and y-offset is 0 for the first pin.
		// This matches the placement done by Generator.addHarnConnPins:
		//   AddPinHelper.generatePin(schemConn, connector, paramExt.getWidth(), offset, ...)
		int pinX = Objects.requireNonNull(schematicConnector.getParameterized()).getExtent().getWidth();
		int pinY = 0;
		sourcePin.setLocation(ILocation.fromXY(pinX, pinY));

		// Update the joint to the pin's new absolute position
		updateJointLocation(sourcePin);

		// Update the pin's connectivity to the target termination
		sourcePin.setConnectivity(targetTermination);

		if (targetSchemConnector != null) {
			new PinListSticher().stichPinLists(targetSchemConnector, schematicConnector);
		}
	}

	private void attachDeviceAndConnector(@NotNull IPinList sourceSchemDevice, @NotNull IPinList schematicConnector)
	{
		if (sourceSchemDevice.hasAttachedObject(schematicConnector)) {
			//if already attached then do nothing
			return;
		}
		// to avoid scurbing on invalid attached schem connecter (LogicScrubbableChecker.checkDeviceOwnedConnector),
		// here we are attaching schem connector only if it is owned by sourceSchemDevice
		if (schematicConnector.getConnectivity() instanceof IDeviceOwnedConnector deviceOwnedConnector &&
				deviceOwnedConnector.getOwner() != null &&
				deviceOwnedConnector.getOwner().equals(sourceSchemDevice.getConnectivity())) {
			sourceSchemDevice.addAttachedObject(schematicConnector);
			schematicConnector.addAttachedObject(sourceSchemDevice);
		}
	}

	/**
	 * Updates the joint's absolute location to match the pin's new absolute position.
	 * <p>
	 * Joints store their position in absolute (diagram) coordinates. When a pin is moved
	 * from one PinList to another and its relative location is updated, the associated
	 * joint's position must also be updated to reflect the pin's new absolute location.
	 * <p>
	 * This mirrors the logic in {@code GenericSchemPin.setupJointLocation()}, which is
	 * called by {@code GenericSchemPin.move()} but NOT by {@code setLocation()}.
	 *
	 * @param pin the pin whose joint location needs to be updated
	 */
	private void updateJointLocation(@NotNull IPin pin)
	{
		IJoint joint = pin.getJoint();
		if (joint != null) {
			ILocation pinAbsLoc = pin.getAbsLocation(pin.getLocation().getX(), pin.getLocation().getY());
			joint.setLocation(pinAbsLoc.getX(), pinAbsLoc.getY());
			ConductorRouteAction.getInstance().addPinForRoute(pin);
		}
	}

	private void showMessages()
	{
		m_transferMessageHelper.addMessagesFromReport(m_reporter);
		m_transferMessageHelper.flushMessages();
	}

	public void doTransfer(@NotNull IDeviceConnector deviceConnector,
			@NotNull IPinList schemDevice, @NotNull IPinList schemConnector, boolean processAllInstance)
	{
		prepareAndTransferBackshellTerminations(deviceConnector, schemDevice, schemConnector, processAllInstance);
		regenerateAffectedSchemDevices();
		rebuildBackshellGraphicsOnAffectedConnectors();
		m_backshellCleanupHelper.cleanupBackshellTerminations(m_reporter);
		showMessages();
		clear();
	}

	private void regenerateAffectedSchemDevices()
	{
		for (IPinList affectedPinList : m_affectedSchemDevices) {
			ISchemDiagram diagram = DiagramHelper.getDiagram(affectedPinList);
			assert diagram != null;
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			Generator.getGenerator().regenerateSchemDeviceConnectors(affectedPinList, gp);
		}
		m_affectedSchemDevices.clear();
	}

	private void rebuildBackshellGraphicsOnAffectedConnectors()
	{
		for (IPinList schemPlugConnector : m_affectedSchemPlugConnectors) {
			if (schemPlugConnector.getConnectivity() instanceof IPlugConnector plugConnector) {
				IBackshell backshell = plugConnector.getBackshell();
				ISchemDiagram diagram = DiagramHelper.getDiagram(schemPlugConnector);
				if (backshell != null && diagram != null) {
					ConnectorHelper.ensureModularSchematics(schemPlugConnector, diagram);
					new BackshellGraphicsRebuilder()
							.rebuildAllBackshellGraphics(schemPlugConnector, backshell.getSymbolRef());
				}
			}
		}
		m_affectedSchemPlugConnectors.clear();
	}

	@Override public void setExternalMessageConsumer(@NotNull Consumer<String> externalMessageConsumer)
	{
		m_transferMessageHelper.setExternalMessageConsumer(externalMessageConsumer);
	}

	private void clear()
	{
		m_reporter.clear();
		m_transferMessageHelper.clear();
	}
}