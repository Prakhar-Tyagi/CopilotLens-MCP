/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceLikePinlist;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinListGenerationParams;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IParameterized;
import chs.common.Side;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.MapMap;
import chs.utilities.Pair;
import chs.utility.DiagramHelper;
import chs.utility.EndLineStyleUtils;
import chs.utility.PortHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.LibraryObjectInfoCache;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinListConnectionHelper;
import chs.utility.helpers.PinListHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MovePinHandler
{

	private int gridSpacing;
	private boolean moveJointOnMovingPin;

	private static final boolean DEBUG_OVERLAPPING = false;   // always enable for DEBUG
	private Generator m_generator;
	protected GeneratorParameters m_genParams;
	private PinNameTextJustificationHandler pinJustificationHandler;
	private List<Pair<IPinList, IPinList>> mModifiedPinListPairs = new ArrayList<>();

	@Nullable private Side newPinMoveSide;

	MovePinHandler(int gridSpacing, boolean moveJointOnMovingPin, @Nullable Side newPinMoveSide)
	{
		this.gridSpacing = gridSpacing;
		this.moveJointOnMovingPin = moveJointOnMovingPin;

		m_generator = Generator.getGenerator();
		pinJustificationHandler = new PinNameTextJustificationHandler();
		mModifiedPinListPairs.clear();
		this.newPinMoveSide = newPinMoveSide;
	}

	public List<Pair<IPinList, IPinList>> getModifiedPinListPairs()
	{
		return Collections.unmodifiableList(mModifiedPinListPairs);
	}

	public Collection<ILogicObject> getLockables(IAbstractSchemPin movingPin, int x, int y, IPinList destPinList)
	{
		Collection<ILogicObject> toBeLocked = new HashSet<>();

		// First check if pin is moved within same mated schem
		final IPinList movingPinParent = CommonUtils.cast(movingPin.getParent(), IPinList.class);
		if (movingPinParent == null) {
			return Collections.emptySet();
		}

		/// Moving pin is not connected
		if (!isConnectedInConnectivity(movingPin)) {
			return toBeLocked;
		}

		final chs.cof.logical.cable.IPinList movingPinParentCablePL = movingPinParent.getConnectivity();
		final chs.cof.logical.cable.IPinList destCablePL = destPinList.getConnectivity();

		Collection<IPinList> srcConnectedSchemPLs =
				getConnectedPinLists(movingPin, movingPinParent, movingPinParentCablePL);
		if (srcConnectedSchemPLs.isEmpty()) {
			return toBeLocked;
		}

		ILogicObject srcConnectedPL = null;
		for (IPinList srcConnectedSchemPL : srcConnectedSchemPLs) {
			srcConnectedPL = srcConnectedSchemPL.getConnectivity();
			if (srcConnectedPL != null) {
				break;
			}
		}

		// Now we know it has connected schem pin
		Collection<IPinList> destConnectedSchemPLs =
				getCandidateMatesAtDest(movingPinParent, destPinList, movingPinParentCablePL, x, y);

		if (movingPinParentCablePL == destCablePL) {
			// dest loc can be PH, Pin -- Ignore empty location as this will not change connectivity
			Collection<ILogicObject> destConnectedCablePLs = new HashSet<>();
			if (destConnectedSchemPLs.isEmpty()) {
				if (destCablePL instanceof IConnector && destPinList.getAttachedPinListObjects().isEmpty()) {
					toBeLocked.add(destCablePL);
					toBeLocked.add(srcConnectedPL);
				}
			}
			else /*if (!destConnectedSchemPLs.isEmpty())*/ {
				boolean bSourceAndTargetMatches = false;
				for (IPinList destConnectedSchemPL : destConnectedSchemPLs) {
					if (srcConnectedSchemPLs.contains(destConnectedSchemPL)) {
						bSourceAndTargetMatches = true;
						break;
					}
					final ILogicObject destConnectedCablePL = destConnectedSchemPL.getConnectivity();
					if (destConnectedCablePL == srcConnectedPL) {
						bSourceAndTargetMatches = true;
						break;
					}
					destConnectedCablePLs.add(destConnectedCablePL);
				}
				if (!bSourceAndTargetMatches) {
					toBeLocked.add(destCablePL);
					toBeLocked.add(srcConnectedPL);
					toBeLocked.addAll(destConnectedCablePLs);
				}
			}
		}
		else /*if (movingPinParentCablePL != destCablePL)*/ {
			toBeLocked.add(movingPinParentCablePL);
			toBeLocked.add(destCablePL);
			toBeLocked.add(srcConnectedPL);
		}
		return toBeLocked;
	}

	@NotNull protected Collection<IPinList> getCandidateMatesAtDest(IPinList movingPinParent, IPinList destPinList,
			chs.cof.logical.cable.IPinList movingPinParentCablePL, int x, int y)
	{
		Collection<IPinList> destConnectedSchemPLs = new HashSet<>();
		final IGfxObject objectAtTargetLoc = getObjectAt(destPinList, x, y);
		if (objectAtTargetLoc instanceof IAbstractSchemPin) {
			IAbstractSchemPin stompedPin = (IAbstractSchemPin) objectAtTargetLoc;
			IPinList stompedTgtSchemPL = CommonUtils.cast(stompedPin.getParent(), IPinList.class);
			assert stompedTgtSchemPL != null;
			destConnectedSchemPLs =
					getConnectedPinLists(stompedPin, stompedTgtSchemPL, stompedTgtSchemPL.getConnectivity());
		}
		else if (objectAtTargetLoc instanceof IPinPlaceholder) {
			destConnectedSchemPLs =
					ConnectionHelper.getMatchingPinlists((IPinPlaceholder) objectAtTargetLoc, destPinList);
		}
		else if (destPinList.getParameterized() == null) {
			// Collect existing connected pins and its parents
			ConnectionHelper connectionHelper = new ConnectionHelper(movingPinParent);
			for (IPinList attachedPinList : movingPinParent.getAttachedPinListObjects()) {
				final chs.cof.logical.cable.IPinList attachedCablePL = attachedPinList.getConnectivity();
				if (movingPinParentCablePL instanceof IDevice && attachedCablePL instanceof IDevice) {
					continue;
				}
				if (attachedCablePL instanceof IConnector && ((IConnector) attachedCablePL).isRingTerminal()) {
					continue;
				}
				if (!(attachedCablePL instanceof IDeviceLikePinlist)) {
					connectionHelper.resetPinList(attachedPinList);
				}
				final ILocation matchLoc = connectionHelper
						.getMatchingLocation(FactoryMgr.getCommonFactory().constructLocation(x, y), destPinList);
				if (matchLoc != null) {
					final ILocation absLocation =
							CoordinateHelper.getAbsLocation(attachedPinList, matchLoc.getX(), matchLoc.getY());
					final IGfxObject objectAt = getObjectAt(attachedPinList, absLocation.getX(), absLocation.getY());
					if (objectAt instanceof IAbstractSchemPin || objectAt instanceof IPinPlaceholder) {
						destConnectedSchemPLs.add(attachedPinList);
					}
				}
			}
		}
		return destConnectedSchemPLs;
	}

	@NotNull protected Set<IPinList> getConnectedPinLists(IAbstractSchemPin movingPin, IPinList movingPinParent,
			chs.cof.logical.cable.IPinList movingPinParentCablePL)
	{
		Set<IPinList> connectedSchemPLs = new HashSet<>();
		// Collect existing connected pins and its parents
		ConnectionHelper sourceConnectionHelper = new ConnectionHelper(movingPinParent);
		for (IPinList attachedPinList : movingPinParent.getAttachedPinListObjects()) {
			final chs.cof.logical.cable.IPinList attachedCablePL = attachedPinList.getConnectivity();
			if (movingPinParentCablePL instanceof IDevice && attachedCablePL instanceof IDevice) {
				continue;
			}
			if (attachedCablePL instanceof IConnector && ((IConnector) attachedCablePL).isRingTerminal()) {
				continue;
			}
			if (!(attachedCablePL instanceof IDeviceLikePinlist)) {
				sourceConnectionHelper.resetPinList(attachedPinList);
			}
			final IAbstractSchemPin connectedSchemPin = sourceConnectionHelper.getConnectedPin(movingPin);
			if (movingPin.isConnected(connectedSchemPin)) {
				connectedSchemPLs.add(attachedPinList);
			}
		}
		return connectedSchemPLs;
	}

	private boolean isConnectedInConnectivity(IAbstractSchemPin movingPin)
	{
		boolean isMatedPL = true;
		if (movingPin instanceof IPin) {
			final IAbstractPin movingCablePin = ((IPin) movingPin).getConnectivity();
			if (!movingCablePin.isMated()) {
				isMatedPL = false;
			}
		}
		else if (movingPin instanceof ISchemStackPin) {
			for (IAbstractPin abstractPin : ((ISchemStackPin) movingPin).getAllConnectivity()) {
				if (!abstractPin.isMated()) {
					isMatedPL = false;
					break;
				}
			}
		}
		return isMatedPL;
	}

	public static class SwapInfo
	{

		private IAbstractSchemPin swapPin;
		private ILocation newSwapPinAbsLoc;

		SwapInfo(@Nullable IAbstractSchemPin swapPin, @Nullable ILocation location)
		{
			this.swapPin = swapPin;
			this.newSwapPinAbsLoc = location;
		}
	}

	public void movePinToLocation(IAbstractSchemPin theMovingPin, int x, int y,
			@Nullable ILocation matchLocOnDeviceSymbol,
			IPinList destPinList, Set<IAbstractSchemPin> modifiedPins,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap,
			Set<IAbstractSchemPin> processedDevicepin, Map<IAbstractSchemPin, Side> pinSideBeforeMove,
			Map<IPin, String> newConnPinNames, boolean ignoreMate)
	{
		movePinToLocation(theMovingPin, x, y, matchLocOnDeviceSymbol, destPinList, modifiedPins, pinMateMap,
				processedDevicepin, pinSideBeforeMove, newConnPinNames, ignoreMate, null);
	}

	public void movePinToLocation(IAbstractSchemPin theMovingPin, int x, int y,
			@Nullable ILocation matchLocOnDeviceSymbol,
			IPinList destPinList, Set<IAbstractSchemPin> modifiedPins,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap,
			Set<IAbstractSchemPin> processedDevicepin, Map<IAbstractSchemPin, Side> pinSideBeforeMove,
			Map<IPin, String> newConnPinNames, boolean ignoreMate, @Nullable SwapInfo info)
	{
		movePinToLocation(theMovingPin, x, y, matchLocOnDeviceSymbol, destPinList, modifiedPins, pinMateMap,
				processedDevicepin, pinSideBeforeMove, newConnPinNames, ignoreMate, false, info);
	}

	/**
	 * //FEAT00013690 - Drag & Drop pin placement between object This Method can be used to move a pin along with its
	 * mate from a pinlist to any instance of the pinlist also can move a pin from a connector attached to device to any
	 * connector attached to any instance of the device
	 *
	 * @param theMovingPin the pin being moved
	 * @param x X coordinate of the destination location
	 * @param y Y coordinate of the destination location
	 * @param matchLocOnDeviceSymbol matching location if the moving pin is mated to a device with symbol
	 * @param modifiedPins all modified pins because of moving pin
	 */

	public void movePinToLocation(IAbstractSchemPin theMovingPin, int x, int y,
			@Nullable ILocation matchLocOnDeviceSymbol,
			IPinList destPinList, Set<IAbstractSchemPin> modifiedPins,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap,
			Set<IAbstractSchemPin> processedDevicepin, Map<IAbstractSchemPin, Side> pinSideBeforeMove,
			Map<IPin, String> newConnPinNames, boolean ignoreMate, boolean ignoreRegenrateGfx,
			@Nullable SwapInfo swapInfo)
	{
		/*=================================Initialization==========================================*/
		if (m_genParams == null) {
			m_genParams = DiagramHelper.createGeneratorParameters(theMovingPin);
		}
		ISchemDiagram sheet = CAFUtils.getInstance().getActiveDiagram(ISchemDiagram.class);
		// Store the move pins and stomped pin in editedPins, plus old location for later processing
		IPinList movingPinOldParent = (IPinList) theMovingPin.getParent();
		assert movingPinOldParent != null;
		ILocation oldLoc = theMovingPin.getLocation();
		ILocation oldAbsLoc = theMovingPin.getAbsLocation();
		boolean isPinParentADeviceWithAutoCreateHC =
				PinListHelper.isHarnessFootprintedAndAllowAutoCreation(movingPinOldParent) &&
						hasHarnessFootprint(movingPinOldParent.getConnectivity());

		// Always disconnect the originating pin
		Map<IAbstractSchemPin, ConnectionHelper> helperMap = new HashMap<IAbstractSchemPin, ConnectionHelper>();
		Map<IAbstractSchemPin, IPinList> pinOldParentMap = new HashMap<IAbstractSchemPin, IPinList>();
		//This Map should have the device that will be attached to the connector after the pins are moved
		IAbstractSchemPin devPin = null;
		Map<IAbstractSchemPin, IAbstractSchemPin> devicePinMateMap =
				new HashMap<IAbstractSchemPin, IAbstractSchemPin>();
		boolean allowConnPinMove = PinPlacementHelper.allowConnectedPinMove(movingPinOldParent);

		// dont allow connected pin move for interconnect devices or connectors, or non parameterized objects
		// ** This is done by looking at the connectivity of the attached object, SO ITS IS IMPORTANT we do this check
		// ** BEFORE we disconnect the connected pins, below. The disconnect can cause parent pinlist to detach

		/*=========================================================================================*/
		if (theMovingPin.isDevicePin()) {   //this check is only done for Device and Device Connectors.
			devPin = theMovingPin;
			if (!processedDevicepin.add(theMovingPin)) {
				return;
			}
		}
		else {
			IAbstractSchemPin mp = PinPlacementHelper.getSingleConnectedAbstractSchemPin(theMovingPin);
			if (mp != null && mp.isDevicePin()) {
				devPin = mp;
				processedDevicepin.add(mp);
			}
		}
		IPin mateDevPin = null;
		IDevicePin mateDevPinConn = null;
		IPinList devicePinOldParent = null;
		IPinList mateDevice = null;
		if (devPin != null && devPin instanceof IPin) {
			IDevicePin dpin = (IDevicePin) ((IConnectivityRef) devPin).getConnectivity();
			devicePinOldParent = (IPinList) devPin.getParent();
			mateDevPinConn = dpin.getConnectedDevicePin();
			if (mateDevPinConn != null) {
				mateDevPin = ConnectionHelper.getConnectedDevicePin((IPin) devPin);
				if (mateDevPin != null) {
					mateDevice = (IPinList) mateDevPin.getParent();
				}
			}
		}

		//Store the Pins that will be modified
		Map<IAbstractSchemPin, Pair<IPinList, Point>> editedPins = new LinkedHashMap<>(2);
		editedPins.put(theMovingPin, new Pair<>(destPinList, new Point(x, y)));
		IAbstractSchemPin stompedPin = getPinToSwapWith(destPinList, x, y);
		if (stompedPin != null) {
			ILocation stompedPinAbsLoc = theMovingPin.getAbsLocation();
			int locX = stompedPinAbsLoc.getX();
			int locY = stompedPinAbsLoc.getY();
			editedPins.put(stompedPin, new Pair<>(movingPinOldParent, new Point(locX, locY)));
		}
		modifiedPins.addAll(editedPins.keySet());

		ModularConnectorAttachedPinListsInfoGenerator modularConnectorAttachedPinListsInfoGenerator =
				new ModularConnectorAttachedPinListsInfoGenerator();
		if (!ignoreMate) {
			//for modular schematic the swap attachment might be different pairs.
			//so compute those pairs for each movingpin and swapedpin both.
			for (Map.Entry<IAbstractSchemPin, Pair<IPinList, Point>> entry : editedPins.entrySet()) {
				IAbstractSchemPin pin = entry.getKey();
				IPinList pinParent = (IPinList) pin.getParent();
				assert pinParent != null;
				IPinList destPL = entry.getValue().getFirst();
				Point location = entry.getValue().getSecond();
				modularConnectorAttachedPinListsInfoGenerator
						.collectConnectorAttachedPLsInfo(pin, pinParent, destPL, location.x, location.y);
			}
		}
		if (!ignoreMate) {
			// we need to disconnect all device pins that are going to be moved.
			disconnectEditedPins(editedPins.keySet(), pinOldParentMap, sheet, helperMap, devicePinMateMap, pinMateMap);
		}

		// Move the the pin and the stomped pin if any. Then add a placeholder to the vacated position
		movePin(theMovingPin, destPinList, x, y, swapInfo);

		// This will also regenerate schem Device connectors if required.
		// There is a BUG in the generator, for schem device connectrs. If we SWAP device pinss on adjacent sides, then
		// the generator does not force a generation, because it thinks the device connectors are current.
		// (it cant handle pin swaps). Because no schem generation happen, we end up with the pins graphics, extending
		// past the extent. This plays havoc, with connectorHelper, as we then never mate.
		// Ref: dts0100445993 Swap mated pins around the corner of a device with split connectors - Wrong behaviour
		// To compensate for this, we add backdoor way of ignoring, if the device connectors are current
		// therby forcing regeneration.
		chs.cof.logical.cable.IPinList cpl = movingPinOldParent.getConnectivity();
		regenerateGfxIfNeeded(theMovingPin, ignoreRegenrateGfx, movingPinOldParent);

		if (DEBUG_OVERLAPPING) {
			assert (checkOverlappingPinOrPlaceHolders(movingPinOldParent));
		}

		// Now move any connected pins. This will handle stomping of connected pins. Even if the connected pins
		// belong to different connectors. This **ASSUMES** that the connected pin have been disconnected

		// Handle the special case, of moving a connector pin mated to a device which has a symbol
		if (!ignoreMate) {
			MapMap<IAbstractSchemPin, IPinList, IPinList> pinListAttachedToConnectorForPins =
					modularConnectorAttachedPinListsInfoGenerator.generate();
			if (matchLocOnDeviceSymbol != null &&
					((IConnectivityRef) theMovingPin.getParent()).getConnectivity() instanceof IConnector) {
				Map<IPinList, IPinList> pinListAttachedToConnector =
						pinListAttachedToConnectorForPins.pullMap(theMovingPin);
				moveMatedPinOnDevSymbol(theMovingPin, matchLocOnDeviceSymbol, pinMateMap, pinListAttachedToConnector);
			}
			else if (allowConnPinMove) {
				for (IAbstractSchemPin movedPin : editedPins.keySet()) {
					for (IAbstractSchemPin matedPin : pinMateMap.pullReadOnlySafeList(movedPin)) {
						if (matedPin != null) {
							Map<IPinList, IPinList> pinListAttachedToConnector =
									pinListAttachedToConnectorForPins.pullMap(movedPin);
							Map<Pair<IPinList, IPinList>, PinListConnectionHelper> pinlistConnHelperCache =
									buildPinListConnectionHelperCache(pinListAttachedToConnector);
							// Keep a tab on the connected pin parent, as this parent could change. We need this parent to add
							// a place holder
							if (moveConnectedPin(matedPin, movedPin, pinListAttachedToConnector, helperMap,
									pinOldParentMap, oldLoc, oldAbsLoc, pinSideBeforeMove, pinlistConnHelperCache)) {
								//in case of modular connector we may get new schem connector created for mated pin.
								//so pinlist must be attached to moved pin parent. otherwise this will not be
								//counted as connected schem pins and mated pin might be deleted later.
								IPinList matedPinNewParent = (IPinList) matedPin.getParent();
								assert matedPinNewParent != null;
								IPinList movedPinNewParent = (IPinList) movedPin.getParent();
								assert movedPinNewParent != null;
								matedPinNewParent.addAttachedObject(movedPinNewParent);
								movedPinNewParent.addAttachedObject(matedPinNewParent);
							}
						}
					}
				}
			}
			//if the device has harness footprint with auto harness connector generation we will not connect the pins
			//to the new locations as GenerateharnessConnectorActionHelper will take over in editModel
			if (!isPinParentADeviceWithAutoCreateHC) {
				connectPins(allowConnPinMove, editedPins.keySet(), pinMateMap, newConnPinNames);
			}
		}

		if (devPin instanceof IPin) {
			if (mateDevPinConn != null && ConnectionHelper.getConnectedDevicePin((IPin) devPin) == null) {
				connectDevicePins(devPin, mateDevPinConn);
			}
			if (mateDevPin != null && ConnectionHelper.getConnectedDevicePin(mateDevPin) == null) {
				IDevicePin dpin = (IDevicePin) ((IConnectivityRef) devPin).getConnectivity();
				connectDevicePins(mateDevPin, dpin);
			}
			// move the connected device pin only if it is on the same device.
			if (mateDevPin != null && mateDevice != null && devicePinOldParent == devPin.getParent()
					&& ConnectionHelper.getConnectedDevicePin(mateDevPin) == null
					&& ConnectionHelper.getConnectedDevicePin((IPin) devPin) == null) {
				// we have to get the absolute location here since we would then get the relative location
				// for the destination pinlist.
				ILocation newLoc = devPin.getAbsLocation(devPin.getLocation());
				if (mateDevice.getSymbolDef() != null) {
					// if we are on a symbol and we moved the pin outside the boundary, then we should disconnect
					if (PinPlacementHelper
							.onBoundary(ExtentHelper.getAbsExtent(mateDevice, mateDevice.getExtent()), newLoc)
							|| ExtentHelper.getAbsExtent(mateDevice, mateDevice.getExtent()).containsCoord(newLoc)) {
						movePinToLocation(mateDevPin, newLoc.getX(), newLoc.getY(), null, mateDevice, modifiedPins,
								pinMateMap, processedDevicepin, pinSideBeforeMove, newConnPinNames, ignoreMate);
					}
				}
				else if (allowMateDevicePinMove(mateDevPin, newLoc, mateDevice)) {
					movePinToLocation(mateDevPin, newLoc.getX(), newLoc.getY(), null, mateDevice, modifiedPins,
							pinMateMap, processedDevicepin, pinSideBeforeMove, newConnPinNames, ignoreMate);
				}

				// connect the device pins again.
				connectDevicePins(devicePinMateMap);
			}
		}
	}

	@NotNull private Map<Pair<IPinList, IPinList>, PinListConnectionHelper> buildPinListConnectionHelperCache(
			@Nullable Map<IPinList, IPinList> pinListAttachedToConnector)
	{
		Map<Pair<IPinList, IPinList>, PinListConnectionHelper> pinlistConnHelperCache = new HashMap<>();
		if (pinListAttachedToConnector != null) {
			for (IPinList pinList : pinListAttachedToConnector.keySet()) {
				IPinList attachedPinlist = pinListAttachedToConnector.get(pinList);
				if (attachedPinlist != null) {
					PinListConnectionHelper connectionHelper = ConnectionHelper.createInstance(pinList,
							attachedPinlist, null, true);
					pinlistConnHelperCache
							.put(new Pair<IPinList, IPinList>(pinList, attachedPinlist), connectionHelper);
				}
			}
		}
		return pinlistConnHelperCache;
	}

	private static boolean hasHarnessFootprint(@NotNull chs.cof.logical.cable.IPinList pinList)
	{
		IDevice device = CommonUtils.cast(pinList, IDevice.class);
		if (device != null) {
			ILibraryDeviceFootprint footprint = device.getFootprint();
			return footprint != null &&
					footprint.getFootprintType() == ILibraryDeviceFootprint.FootprintType.HARNESS_CONNECTOR;
		}
		return false;
	}

	protected void regenerateGfxIfNeeded(IAbstractSchemPin theMovingPin, boolean ignoreRegenrateGfx,
			IPinList movingPinOldParent)
	{
		if (!ignoreRegenrateGfx) {
			regenerateGfxForPinParent(theMovingPin, movingPinOldParent);
		}
	}

	private class ModularConnectorAttachedPinListsInfoGenerator
	{

		private MapMap<IAbstractSchemPin, IPinList, Pair<IPinList, chs.cof.logical.cable.IPinList>>
				m_plAttachedToConnectorForPins = new MapMap<>(LinkedHashMap.class, LinkedHashMap.class);

		protected void collectConnectorAttachedPLsInfo(IAbstractSchemPin movingPin,
				IPinList movingPinParent, IPinList destPinList, int x, int y)
		{
			final chs.cof.logical.cable.IPinList parentCablePL = movingPinParent.getConnectivity();
			if ((parentCablePL instanceof IConnector)) {

				//this entry is actually not being used. and need not be modularized. the value
				//of the entry need not be necessarily the exact child of modular schematics.
				//if the anchor are same for source and destination of moving pin.
				//use the anchor of mated pin old parent as the destination anchor for mated pin also.
				//for non modular scenario this would be happeing because key for src and dest would
				//be same. need to behave the same way for modular also.
				IPinList movingPinAnchor = new ModularSchemPinListInfo(movingPinParent).getAnchor();
				IPinList destAnchor = new ModularSchemPinListInfo(destPinList).getAnchor();

				//Store (devices-connectors) Attached to source and destination Connectors before disconnecting the Connected Pins
				IDeviceOwnedConnector connector = CommonUtils.cast(parentCablePL, IDeviceOwnedConnector.class);
				IBaseDevice ownerDevice = null;
				if (connector != null) {
					ownerDevice = connector.getOwner(IBaseDevice.class);
				}
				if (ownerDevice != null) {
					IPinList attachedDevice = PinPlacementHelper.getAttachedDevice(ownerDevice, movingPinParent);
					register(movingPin, movingPinAnchor, attachedDevice, attachedDevice);
					IPinList destPinListMate = PinPlacementHelper.getAttachedDevice(ownerDevice, destPinList);
					register(movingPin, destAnchor, destPinListMate, destPinListMate);
				}
				else {
					IPinList matePinOldParent = MovePinActionUtils.getAttachedPinlistCorrespondingToGivenPin(movingPin);
					register(movingPin, movingPinAnchor, matePinOldParent, matePinOldParent);
					IPinList destPLforMatedPin = MovePinActionUtils.getAttachedPinlistCorrespondingToGivenLocation(
							destPinList, x, y, matePinOldParent != null ? matePinOldParent.getConnectivity() : null);
					boolean srcAndDestDiffferentForMovingPin = movingPinAnchor != destAnchor;
					if (destPLforMatedPin != null || srcAndDestDiffferentForMovingPin) {
						if (srcAndDestDiffferentForMovingPin && destPLforMatedPin == null && matePinOldParent != null) {
							destPLforMatedPin = MovePinActionUtils
									.getNearestMateOfGivenType(destPinList, matePinOldParent.getConnectivity(), x, y);
						}
						register(movingPin, destAnchor, destPLforMatedPin, matePinOldParent);
					}
				}
			}
		}

		private void register(@NotNull IAbstractSchemPin movingPin, @NotNull IPinList destAnchor,
				@Nullable IPinList destPLforMatedPin, @Nullable IPinList destCableMatch)
		{
			//noinspection ConstantConditions
			m_plAttachedToConnectorForPins.put(movingPin, destAnchor,
					destPLforMatedPin != null ? new Pair<>(destPLforMatedPin,
							destCableMatch != null ? destCableMatch.getConnectivity() :
									destPLforMatedPin.getConnectivity()) : null);
		}

		@NotNull public MapMap<IAbstractSchemPin, IPinList, IPinList> generate()
		{
			//map of map is not efficient for memory. however the entries
			//would be around 4 only. so need not to worry about that.
			//we need construct the exact modular member where pin would be transfered. otherwise
			//attachment key would be incorrect and thus mated pin would not be transfered correctly.
			MapMap<IAbstractSchemPin, IPinList, IPinList> result = new MapMap<>();
			List<MatedMovePinModularizationData> modularizePins = new ArrayList<>();
			for (IAbstractSchemPin movedPin : m_plAttachedToConnectorForPins.keySet()) {
				Map<IPinList, Pair<IPinList, chs.cof.logical.cable.IPinList>> pairMap =
						m_plAttachedToConnectorForPins.pullMap(movedPin);
				if (pairMap != null) {
					generate(movedPin, pairMap, result, modularizePins);
				}
			}

			constructModularMemberToMovePinTo(modularizePins);

			for (MatedMovePinModularizationData modularizePin : modularizePins) {
				IAbstractSchemPin sourcePin = modularizePin.getSourcePin();
				IPinList sourcePinParent = (IPinList) sourcePin.getParent();
				assert sourcePinParent != null;
				result.put(sourcePin, sourcePinParent, modularizePin.getModularTarget());
			}
			return result;
		}

		private void generate(@NotNull IAbstractSchemPin movedPin,
				@NotNull Map<IPinList, Pair<IPinList, chs.cof.logical.cable.IPinList>> pairMap,
				@NotNull MapMap<IAbstractSchemPin, IPinList, IPinList> result,
				@NotNull List<MatedMovePinModularizationData> modularizePins)
		{
			IPinList movedPinParent = (IPinList) movedPin.getParent();
			assert movedPinParent != null;
			IPinList movedPinAnchor = new ModularSchemPinListInfo(movedPinParent).getAnchor();
			for (Map.Entry<IPinList, Pair<IPinList, chs.cof.logical.cable.IPinList>> entry : pairMap.entrySet()) {
				Pair<IPinList, chs.cof.logical.cable.IPinList> value = entry.getValue();
				if (value != null) {
					IPinList destAnchor = new ModularSchemPinListInfo(entry.getKey()).getAnchor();
					if (destAnchor == movedPinAnchor) {
						IPinList destPLforMatedPin = value.getFirst();
						chs.cof.logical.cable.IPinList destCableMatch = value.getSecond();
						if (destCableMatch instanceof IConnector) {
							//now we would modularize the mated pinlist and find the actual candidate
							//pinlist where moved pin's original mated pin would move to.
							ILocation movedPinAbs = movedPin.getAbsLocation();
							ILocation movedPinMatedAbs = determineMatePinPositionForConnector(
									movedPinAbs.getX(), movedPinAbs.getY(), destPLforMatedPin);
							IAbstractSchemPin pinToSwapWith = getPinToSwapWith(destPLforMatedPin,
									movedPinMatedAbs.getX(), movedPinMatedAbs.getY());
							modularizePins.add(new MatedMovePinModularizationData(movedPin,
									new Point(movedPinMatedAbs.getX(), movedPinMatedAbs.getY()),
									destPLforMatedPin, destCableMatch, pinToSwapWith));
						}
						else if (destPLforMatedPin != null) {
							result.put(movedPin, movedPinParent, destPLforMatedPin);
						}
					}
				}
			}
		}

		@NotNull
		private ILocation determineMatePinPositionForConnector(int x, int y, @NotNull IPinList destPinlistForMated)
		{
			int side = destPinlistForMated.getConnectivity() instanceof IPlugConnector ? 1 : 0;
			int referenceWidth = destPinlistForMated.getReferenceWidth();
			IParameterized parameterized = destPinlistForMated.getParameterized();
			assert parameterized != null;
			int matedPinRel_x = parameterized.getExtent().getLeft() + side * referenceWidth;

			ILocation destPinRelPt = CoordinateHelper.getRelativeLocation(destPinlistForMated, x, y);
			int matedPinRel_y = destPinRelPt.getY();
			return CoordinateHelper.getAbsGfxLocation(destPinlistForMated, matedPinRel_x, matedPinRel_y);
		}
	}

	private static class MatedMovePinModularizationData extends MovePinModularizationData
	{

		private IAbstractSchemPin m_sourcepin;
		@Nullable private IAbstractSchemPin m_matpin;
		//when we want to re-attach the pin we would need this.
		@Nullable private IPinList m_matedPinParent;

		private MatedMovePinModularizationData(@NotNull IAbstractSchemPin sourcepin,
				@NotNull Point absLoc, @NotNull IPinList anchor,
				@NotNull chs.cof.logical.cable.IPinList cable,
				@Nullable IAbstractSchemPin matePin)
		{
			super(absLoc, anchor, cable);
			m_sourcepin = sourcepin;
			if (matePin != null) {
				m_matpin = matePin;
				IPinList parent = (IPinList) matePin.getParent();
				assert parent != null;
				m_matedPinParent = parent;
			}
		}

		@NotNull public IAbstractSchemPin getSourcePin()
		{
			return m_sourcepin;
		}

		@Override public boolean isPinUnderModularization(@NotNull IAbstractSchemPin pin)
		{
			return m_matpin != null && m_matedPinParent != null && pin == m_matpin;
		}
	}

	/**
	 * //FEAT00013690 - Drag & Drop pin placement between object moved the mated device pin if we have a connector mated
	 * to a device with symbol
	 *
	 * @param movedPin moved connector pin
	 * @param matchLocOnDeviceSymbol matching location on the device with symbol
	 * @param pinMateMap a map that contains the pin along with its mate
	 * @param connectorAttachedDevMap a map that contains the connectors and the devices that should be attached
	 */

	private void moveMatedPinOnDevSymbol(IAbstractSchemPin movedPin, ILocation matchLocOnDeviceSymbol,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap,
			@Nullable Map<IPinList, IPinList> connectorAttachedDevMap)
	{
		IPinList movedPinParent = (IPinList) movedPin.getParent();
		if (movedPinParent != null) {
			for (IAbstractSchemPin matedPin : pinMateMap.pullReadOnlySafeList(movedPin)) {
				IPinList matedPinOldParent = (IPinList) matedPin.getParent();
				moveMatedPinOnDevSymbol(matchLocOnDeviceSymbol, connectorAttachedDevMap, movedPinParent, matedPin);
				if (matedPinOldParent != null) {
					regenerateGfxForPinParent(matedPin, matedPinOldParent);
				}
			}
		}
	}

	private void moveMatedPinOnDevSymbol(ILocation matchLocOnDeviceSymbol,
			@Nullable Map<IPinList, IPinList> connectorAttachedDevMap, IPinList movedPinParent,
			IAbstractSchemPin matedPin)
	{
		if (matedPin != null) {
			IPinList matedPinParent = (IPinList) matedPin.getParent();

			if (connectorAttachedDevMap != null && matedPinParent != null &&
					matedPinParent.getParameterized() == null) {
				IPinList matedPinNewParent = connectorAttachedDevMap.get(movedPinParent);

				ILocation absDestLoc = CoordinateHelper.getAbsLocation(
						matedPinNewParent, matchLocOnDeviceSymbol.getX(), matchLocOnDeviceSymbol.getY());

				movePin(matedPin, matedPinNewParent, absDestLoc.getX(),
						absDestLoc.getY(), null);
			}
		}
	}

	private boolean moveConnectedPin(IAbstractSchemPin matedPin, IAbstractSchemPin movedPin,
			@Nullable Map<IPinList, IPinList> pinListAttachedToConnector,
			Map<? extends IAbstractSchemPin, ConnectionHelper> helperMap,
			Map<? extends IAbstractSchemPin, IPinList> pinOldParentMap, ILocation oldLoc, ILocation oldAbsLoc,
			Map<IAbstractSchemPin, Side> pinSideBeforeMove,
			Map<Pair<IPinList, IPinList>, PinListConnectionHelper> connectionHelperCache)
	{
		ISchemDiagram sheet = CAFUtils.getInstance().getActiveDiagram(ISchemDiagram.class);
		ISchemFactory schemFact = FactoryMgr.getSchemFactory();
		IPinList matedPinOldParent = (IPinList) matedPin.getParent();

		// Use relative sides to determine side before move
		int oldSide = ExtentHelper.getSide(matedPin.getLocation(), ExtentHelper.getNonTextExtent(matedPin.getParent()));
		if (oldSide == ExtentHelper.SIDE_UNKNOWN) {
			oldSide =
					ExtentHelper.getSide(matedPin.getLocation(),
							ExtentHelper.getPinExtent(matedPin.getParent(), null, true));
		}
		PinNameTextJustificationHandler.TextAttrHolder nt1 =
				PinNameTextJustificationHandler.getTextAttributeHolder(matedPin);

		if (isMatedPinMoveAllowed(movedPin, matedPin, pinListAttachedToConnector)) {
			// We moved a pin on one member of a mated connector pair or a pair of connected reusable pins.

			// Create connectionHelper at the destination (this was moved earlier)
			ConnectionHelper destHelper = new ConnectionHelper();
			destHelper.examine(movedPin, sheet);

			// Create placeholder at the old location of the connected pin. But only if object supports it.
			// ie devices with symbols are not parameterized, and dont support placeholders.
			IPinPlaceholder thePinPlaceHolder = null;
			if (matedPinOldParent.getParameterized() != null) {
				thePinPlaceHolder = schemFact.createPinPlaceholder(matedPin.getLocation().getX(),
						matedPin.getLocation().getY());
			}
			ConnectionHelper sourceHelper = helperMap.get(movedPin);

			// Try to move the connected pin on the **same** connector/device
			boolean mateMoved = true;
			if (!sourceHelper.moveConnectedPin(movedPin, pinOldParentMap.get(movedPin), matedPin,
					pinListAttachedToConnector, connectionHelperCache)) {
				// handle move **across** connectors
				if (!destHelper.moveConnectedPin(movedPin, pinOldParentMap.get(movedPin), matedPin,
						pinListAttachedToConnector, connectionHelperCache)) {
					// A boundary extension move is on the same side as the moving pin
					mateMoved = doParallelConnectedPinMove(movedPin, matedPin, oldLoc, oldAbsLoc);
				}
			}
			if (mateMoved && matedPin instanceof IPin &&
					((IConnectivityRef) matedPin).getConnectivity() instanceof IDevicePin) {
				// if mate is device pin then it may be connected to a device pin
				// if so, then we would disconnect them.
				//ConnectionHelper.disconnectDeviceConnectedPin(matedPin, commFact, false);

			}
			//After moving connected pin it could have a new parent, hence use old parent to add placeholder

			if (thePinPlaceHolder != null) {
				matedPinOldParent.addObject(thePinPlaceHolder);
			}
			// Pin and PinPlaceholders can end up occupying the same location we need to generate
			// old/new parents to remove these coincident PinPlaceHolders
			regenerateGfxForPinParent(matedPin, matedPinOldParent);
			// justify after regenerateGfx so that side calc is correct - Note depending on what moved, it may
			// not actually be a connector pin.
			if (matedPin instanceof IPin &&
					!(((IGenericSchemPin) matedPin).getConnectivity().getOwner() instanceof IConnector)) {
				pinJustificationHandler.justifyDevicePinNameText(matedPin, oldSide, nt1);
			}
			return mateMoved;
		}
		return false;
	}

	private void connectPins(boolean allowConnPinMove, Set<? extends IAbstractSchemPin> editedPins,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap, Map<IPin, String> newConnPinNames)
	{
		// Connect to a pin in the new location. It might be a connected pin that we disconnected
		// and moved, it might be one that was already in place, or we might have to create one.
		// when we are swapping its easier to manage if the move of the connected pins, is followed by connect
		if (allowConnPinMove) {
			for (Map.Entry<IAbstractSchemPin, List<IAbstractSchemPin>> entry : pinMateMap.entrySet()) {
				IAbstractSchemPin theMovedPin = entry.getKey();
				ensureUniquePinName(theMovedPin, newConnPinNames);
				if (DEBUG_OVERLAPPING) {
					assert (checkOverlappingPinOrPlaceHolders((IPinList) theMovedPin.getParent()));
				}
				for (IAbstractSchemPin theMovedConnectedPin : entry.getValue()) {
					ensureUniquePinName(theMovedConnectedPin, newConnPinNames);

					if (DEBUG_OVERLAPPING) {
						assert (checkOverlappingPinOrPlaceHolders((IPinList) theMovedPin.getParent()));
						assert (checkOverlappingPinOrPlaceHolders((IPinList) theMovedConnectedPin.getParent()));
					}
					connectPin(theMovedPin, theMovedConnectedPin);
				}
			}
		}
		// Here we connect up the moved pin with whatever we can.  Must also consider the "stomped on" pin.
		for (IAbstractSchemPin modifiedPin : editedPins) {
			connectPin(modifiedPin);
		}
	}

	private void connectPin(@NotNull IAbstractSchemPin thePinToConnect, IAbstractSchemPin thePinToConnectTo)
	{
		if (thePinToConnectTo != null) {
			ISchemDiagram sheet = CAFUtils.getInstance().getActiveDiagram(ISchemDiagram.class);

			IGrid grid = Objects.requireNonNull(((IGriddable) sheet)).getGrid();
			IPinList device = (IPinList) thePinToConnect.getParent();
			IPinList connector = (IPinList) thePinToConnectTo.getParent();

			if (!(Objects.requireNonNull(device).getConnectivity() instanceof IBaseDevice)) {
				device = connector;
				connector = (IPinList) thePinToConnect.getParent();
			}
			ConnectionHelper destHelper2 = new ConnectionHelper(device);
			destHelper2.resetPinList(connector);
			destHelper2.connectPin(thePinToConnect, grid, false, false, new LibraryObjectInfoCache());
		}
		else {
			connectPin(thePinToConnect);
		}
	}

	private void connectPin(IAbstractSchemPin thePinToConnect)
	{
		ISchemDiagram sheet = CAFUtils.getInstance().getActiveDiagram(ISchemDiagram.class);

		IGrid grid = Objects.requireNonNull(((IGriddable) sheet)).getGrid();

		ConnectionHelper destHelper2 = new ConnectionHelper();
		destHelper2.examine(thePinToConnect, sheet);
		destHelper2.connectPin(thePinToConnect, grid, false, false, new LibraryObjectInfoCache());
	}

	private void connectDevicePins(Map<IAbstractSchemPin, IAbstractSchemPin> pinMap)
	{
		//Todo Moattia: Connect the pin again to the other device pin.
		for (IAbstractSchemPin thePinToConnect : pinMap.keySet()) {
			//This is only for Device Pin and not applicable for Block Device Pin.
			if (thePinToConnect.isDevicePin()) {
				IAbstractSchemPin cdpin = pinMap.get(thePinToConnect);
				if (cdpin != null) {
					ConnectionHelper.connectDevicePins(thePinToConnect, cdpin);
				}
			}
		}
		pinMap.clear();
	}

	private void connectDevicePins(IAbstractSchemPin thePinToConnect, IDevicePin cdpin)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(thePinToConnect);
		for (IDiagramObject dObj : diagram.getRepresentations(cdpin.getUID())) {
			if (dObj instanceof IPin && thePinToConnect instanceof IPin) {
				if (ConnectionHelper.connectDevicePins(thePinToConnect, (IAbstractSchemPin) dObj)) {
					// connected, no more work to do.
					break;
				}
			}
			else if (dObj instanceof ISchemStackPin && thePinToConnect instanceof ISchemStackPin) {
				if (ConnectionHelper.connectStackedPin((ISchemStackPin) thePinToConnect, (ISchemStackPin) dObj)) {
					// connected, no more work to do.
					break;
				}
			}
		}
	}

	private void disconnectEditedPins(Set<? extends IAbstractSchemPin> editedPins,
			Map<IAbstractSchemPin, IPinList> pinOldParentMap,
			ISchemDiagram sheet, Map<IAbstractSchemPin, ConnectionHelper> helperMap,
			Map<IAbstractSchemPin, IAbstractSchemPin> devicePinMateMap,
			ListMap<IAbstractSchemPin, IAbstractSchemPin> pinMateMap)
	{
		for (IAbstractSchemPin discoPin : editedPins) {

			final IPinList discoPinParent = (IPinList) discoPin.getParent();
			pinOldParentMap.put(discoPin, discoPinParent);

			if (discoPin instanceof IPin) {
				ConnectionHelper origHelper = new ConnectionHelper();
				origHelper.examineConnectivity((IPin) discoPin, sheet, true);
				helperMap.put(discoPin, origHelper);
			}
			else if (discoPin instanceof ISchemStackPin) {
				ConnectionHelper origHelper = new ConnectionHelper();
				origHelper.examine(discoPin, sheet);
				helperMap.put(discoPin, origHelper);
			}

			//			IAbstractSchemPin connPin = PinPlacementHelper.getConnectedAbstractSchemPin(discoPin);
			for (IAbstractSchemPin connPin : PinPlacementHelper.getConnectedAbstractSchemPins(discoPin)) {
				if (connPin != null) {
					if (connPin.isDevicePin()) { // this is applicable for only device pin.
						if (connPin instanceof IPin &&
								(((IDevicePin) ((IConnectivityRef) connPin).getConnectivity())
										.getConnectedDevicePin() != null)) {
							devicePinMateMap.put(connPin, ConnectionHelper.getConnectedDevicePin((IPin) connPin));
						}
						else if (connPin instanceof ISchemStackPin) {
							ISchemStackPin connectedPin =
									ConnectionHelper.getConnectedStackedPin((ISchemStackPin) connPin);
							if (connectedPin != null) {
								devicePinMateMap.put(connPin, connectedPin);
							}
						}
					}

					if (!PinListHelper.isHarnessFootprintedAndAllowAutoCreation(discoPinParent)) {
						final IPinList connPinParent = (IPinList) connPin.getParent();
						if (connPinParent != null) {
							mModifiedPinListPairs.add(new Pair<IPinList, IPinList>(discoPinParent, connPinParent));
							if (LogicObjectLockFinder.isEditable(discoPinParent) &&
									LogicObjectLockFinder.isEditable(connPinParent)) {
								ConnectionHelper.disconnectAbstractPin(connPin, false);
							}
						}
					}

					pinMateMap.add(discoPin, connPin);
				}
			}
			if (discoPin.isDevicePin()) { // this is applicable for only device pin and not for Block device pin
				if (discoPin instanceof IPin &&
						(((IDevicePin) ((IConnectivityRef) discoPin).getConnectivity()).getConnectedDevicePin() !=
								null)) {
					IPin cdpin = ConnectionHelper.getConnectedDevicePin((IPin) discoPin);
					if (cdpin != null) {
						devicePinMateMap.put(discoPin, cdpin);
						mModifiedPinListPairs
								.add(new Pair<IPinList, IPinList>(discoPinParent, (IPinList) cdpin.getParent()));
						if (LogicObjectLockFinder.isEditable(discoPinParent) &&
								LogicObjectLockFinder.isEditable(cdpin)) {
							ConnectionHelper.disconnectDeviceConnectedPin((IPin) discoPin, false);
						}
					}
				}
				// Stacked pins can never be Device connected pin or vice versa
				//				else if (discoPin instanceof ISchemStackPin) {
				//					ISchemStackPin connectedStpin = ConnectionHelper.getConnectedStackedPin((ISchemStackPin) discoPin);
				//					if (connectedStpin != null) {
				//						devicePinMateMap.put(discoPin, connectedStpin);
				//						ConnectionHelper.disconnectConnectedStackPin((ISchemStackPin) discoPin, false);
				//					}
				//				}

			}
		}
	}

	private void ensureUniquePinName(IAbstractSchemPin movingPin, Map<IPin, String> newConnPinNames)
	{
		if (movingPin instanceof IPin) {
			String newPinName = newConnPinNames.get(movingPin);
			if (newPinName != null) {
				((IConnectivityRef) movingPin).getConnectivity().setName(newPinName);
			}
		}
	}

	private void regenerateGfxForPinParent(IAbstractSchemPin movedPin, IPinList oldPinOwner)
	{
		GeneratorParameters gp = new GeneratorParameters(m_genParams);
		gp.setNewObject(false);
		IPinList movingPinOwnerAfterMove = (IPinList) movedPin.getParent();
		if (movingPinOwnerAfterMove != oldPinOwner) {
			m_generator.generate(
					PinListGenerationParams.getInstance(movingPinOwnerAfterMove, gp,
							Generator.NOREGENERATE_PROPERTIES, true));
		}
		m_generator.generate(PinListGenerationParams.getInstance(oldPinOwner, gp,
				Generator.NOREGENERATE_PROPERTIES, true));
	}

	/**
	 * Moves a connector pin mated to a moved device pin to the boundary extension on the same side
	 *
	 * @param theMovedPin the moved device pin
	 * @param connectedPin the mated connector pin
	 * @param oldMovedPinLoc old location of the moving pin
	 */
	private static boolean doParallelConnectedPinMove(IAbstractSchemPin theMovedPin, IAbstractSchemPin connectedPin,
			ILocation oldMovedPinLoc, ILocation oldMovedPinAbsLoc)
	{
		// do a parallel pin move , ie we are moving along the extension boundary. The new position may well be outside
		// of the extent of the device, we rely on the generator to fix this afterwards.
		ILocation newRelLoc = null;
		ILocation absMovedPinCoord = theMovedPin.getAbsLocation();

		if (IConnector.Statics.isRingTerminalTypeConnector(connectedPin.getParent())) {
			// dts0100923994 Drag and drop a device pin, that is mated with a ring terminal
			ILocation currentAbsLoc = theMovedPin.getAbsLocation();
			int delta_x = currentAbsLoc.getX() - oldMovedPinAbsLoc.getX();
			int delta_y = currentAbsLoc.getY() - oldMovedPinAbsLoc.getY();
			connectedPin.getParent().move(delta_x, delta_y);
			//the pin's location relative to its parent ring terminal should not have changed
			newRelLoc = FactoryMgr.getCommonFactory().constructLocation(connectedPin.getLocation());
		}
		else {
			ILocation newMoveRelLoc = CoordinateHelper.getRelLocation(connectedPin, absMovedPinCoord.getX(),
					absMovedPinCoord.getY());

			// Move connected pin to parallel location  relative to the moved pin
			if (oldMovedPinLoc.getX() == theMovedPin.getLocation().getX()) { // vertical movement

				newRelLoc = FactoryMgr.getCommonFactory().constructLocation(connectedPin.getLocation());
				newRelLoc.setY(newMoveRelLoc.getY());
			}
			else if (oldMovedPinLoc.getY() == theMovedPin.getLocation().getY()) { // horiz movement

				ILocation absConnPinCoord = connectedPin.getAbsLocation();
				newRelLoc = CoordinateHelper.getRelLocation(connectedPin, absMovedPinCoord.getX(),
						absConnPinCoord.getY());
			}
		}

		if (newRelLoc != null) {
			connectedPin.setLocation(newRelLoc);

			// Move the node too.
			IJoint node = connectedPin.getJoint();
			if (node != null) {
				ILocation newAbsLoc = connectedPin.getAbsLocation(newRelLoc.getX(), newRelLoc.getY());
				node.setX(newAbsLoc.getX());
				node.setY(newAbsLoc.getY());
			}
			return true;
		}
		return false;
	}

	private static boolean checkOverlappingPinOrPlaceHolders(IPinList pinList)
	{
		Map<String, IGfxObject> placeholders = new HashMap<String, IGfxObject>();
		IPinList anchor = new ModularSchemPinListInfo(pinList).getAnchor();

		for (IGfxObjectIterator gitr = pinList.getObjects(); gitr.hasNext(); ) {
			IGfxObject gobj = gitr.getNext();
			if (gobj instanceof IPinPlaceholder || gobj instanceof IPin) {

				// convert graphics object location to be relative to its parent
				Point p = PinPlacementHelper.getTransformedLocation(pinList, gobj, anchor);
				StringBuilder sb = new StringBuilder();
				sb.append(p.x);
				sb.append(',');
				sb.append(p.y);
				String locationKey = sb.toString();

				// duplicate pin or place holder at same location
				IGfxObject theObject = placeholders.get(locationKey);
				if (theObject != null) {
					return false;
				}

				placeholders.put(locationKey, gobj);
			}
		}
		return true;
	}

	private static boolean isMatedPinMoveAllowed(IAbstractSchemPin movedPin, IAbstractSchemPin matedPin,
			@Nullable Map<IPinList, IPinList> pinListAttachedToConnector)
	{
		if (matedPin == null) {
			return false;
		}

		if (movedPin instanceof IPin && ((IGenericSchemPin) movedPin).getConnectivity().getSharedPin() != null &&
				((IGenericSchemPin) movedPin).getConnectivity().getSharedPin().isReusable()) {
			return true;
		}

		chs.cof.logical.cable.IPinList movedPinOwner =
				movedPin.getParent() instanceof IPinList ? ((IPinList) movedPin.getParent()).getConnectivity() : null;
		chs.cof.logical.cable.IPinList matedPinOwner =
				matedPin.getParent() instanceof IPinList ? ((IPinList) matedPin.getParent()).getConnectivity() : null;

		//dts0100590943-should not move connector pins if we are going to generate harness connectors automatically. Doing
		// so will corrupt the connectivity connectors which are going to be reused in GHC
		if (movedPinOwner != null && PinListHelper.isHarnessFootprintedAndAllowAutoCreation(movedPinOwner) &&
				hasHarnessFootprint(movedPinOwner)) {
			return false;
		}
		if ((movedPinOwner instanceof IConnector && matedPinOwner instanceof IBaseDevice) ||
				(movedPinOwner instanceof IConnector && matedPinOwner instanceof IConnector)) {
			//disallow moving a (device-connector) pin mated to a connector pin
			// when the connector pin is moved to an instance of the connector that is not attached to a device
			return (pinListAttachedToConnector != null && pinListAttachedToConnector.get(movedPin.getParent()) != null);
		}
		return (matedPinOwner instanceof IConnector && movedPinOwner instanceof IBaseDevice);
	}

	private boolean allowMateDevicePinMove(@NotNull IPin pin, @NotNull ILocation newLoc, @NotNull IPinList mateDev)
	{
		ILocation oldMateLoc = pin.getAbsLocation(pin.getLocation());
		IExtent devAbsExtent = ExtentHelper.getAbsExtent(mateDev, ExtentHelper.getNonTextExtent(mateDev));
		Side pinSide = Side.getSide(devAbsExtent, pin.getAbsLocation());
		if ((pinSide.isLeft() || pinSide.isRight()) && newLoc.getX() != oldMateLoc.getX()) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ((pinSide.isTop() || pinSide.isBottom()) && newLoc.getY() != oldMateLoc.getY()) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the pin that will ber stomped (swapped) when this pin is placed at x,y * @param pin The source pin * @param
	 * x
	 *
	 * @param pin the pin being moved
	 * @param x x cord
	 * @param y y cord
	 *
	 * @return IPin the stopmed pin  The target pin, or null if none.
	 */

	@Nullable
	public static IAbstractSchemPin getPinToSwapWith(IPinList destPinList, int x, int y)
	{
		IAbstractSchemPin pinToSwapWith = null;
		for (IPinList candidate : new ModularSchemPinListInfo(destPinList).getCandidates()) {
			for (IAbstractSchemPin p : candidate.getObjects(IAbstractSchemPin.class)) {
				ILocation pinAbsLoc = p.getAbsLocation();
				if (pinAbsLoc.getX() == x && pinAbsLoc.getY() == y) {
					pinToSwapWith = p;
					break;
				}
			}
		}
		return pinToSwapWith;
	}

	@Nullable private IGfxObject getObjectAt(IPinList destPinList, int x, int y)
	{
		//Get the X,Y location relative to the destination pinlist
		ILocation newLoc = CoordinateHelper.getDirectParentRelLocation(destPinList, x, y);
		for (IGfxObject gfxObject : destPinList.getObjects()) {
			if (gfxObject instanceof IAbstractSchemPin || gfxObject instanceof IPinPlaceholder) {
				if (gfxObject.getLocation().getX() == newLoc.getX() &&
						gfxObject.getLocation().getY() == newLoc.getY()) {
					return gfxObject;
				}
			}
		}
		return null;
	}

	private abstract static class MovePinModularizationData extends AbstractAddPinArgs
	{

		private Point m_absLoc;
		private IPinList m_anchor;
		private chs.cof.logical.cable.IPinList m_cable;
		private IPinList m_modularTarget;

		private MovePinModularizationData(@NotNull Point absLoc, @NotNull IPinList anchor,
				@NotNull chs.cof.logical.cable.IPinList cable)
		{
			super(new Point());
			m_absLoc = absLoc;
			m_anchor = anchor;
			m_cable = cable;
			m_modularTarget = anchor;
		}

		public void setModularTarget(@NotNull IPinList modularTarget)
		{
			m_modularTarget = modularTarget;
		}

		@NotNull public IPinList getModularTarget()
		{
			return m_modularTarget;
		}

		@Nullable @Override public chs.cof.logical.cable.IPinList getCablePinlist()
		{
			return m_cable;
		}

		@NotNull public IPinList getAnchor()
		{
			return m_anchor;
		}

		@NotNull public Point getAbsLocation()
		{
			return m_absLoc;
		}

		public boolean isPinUnderModularization(@NotNull IAbstractSchemPin pin)
		{
			return false;
		}
	}

	private static class SourceMovePinModularizationData extends MovePinModularizationData
	{

		private IAbstractSchemPin m_pin;

		private SourceMovePinModularizationData(@NotNull IAbstractSchemPin pin,
				@NotNull Point absLoc, @NotNull IPinList anchor,
				@NotNull chs.cof.logical.cable.IPinList cable)
		{
			super(absLoc, anchor, cable);
			m_pin = pin;
			IPinList parent = (IPinList) m_pin.getParent();
			assert parent != null;
		}

		@NotNull public IAbstractSchemPin getSourcePin()
		{
			return m_pin;
		}

		@Override public boolean isPinUnderModularization(@NotNull IAbstractSchemPin pin)
		{
			return pin == m_pin;
		}
	}

	protected boolean movePin(IAbstractSchemPin pin, IPinList destPinList, int x, int y, @Nullable SwapInfo info)
	{

		IAbstractSchemPin pinToSwapWith = info == null ? getPinToSwapWith(destPinList, x, y) : info.swapPin;

		if (pinToSwapWith == pin) {
			return false; // It's ME! - do nothing.
		}

		ILocation altLoc = info == null ? CoordinateHelper.getAbsLocation(pin, 0, 0) : info.newSwapPinAbsLoc;

		IPinList srcPinList = (IPinList) pin.getParent();
		assert srcPinList != null;

		List<SourceMovePinModularizationData> modularizePins = new ArrayList<>();
		chs.cof.logical.cable.IPinList cablePinList = srcPinList.getConnectivity();
		modularizePins.add(new SourceMovePinModularizationData(pin, new Point(x, y), destPinList, cablePinList));

		if (pinToSwapWith != null) {
			int altLocX = altLoc.getX();
			int altLocY = altLoc.getY();
			IPinList swapPL = (IPinList) pinToSwapWith.getParent();
			assert swapPL != null;
			modularizePins.add(new SourceMovePinModularizationData(pinToSwapWith, new Point(altLocX, altLocY),
					srcPinList, swapPL.getConnectivity()));
		}

		//modularize the pinlists before moving the pins to its actula traget.
		constructModularMemberToMovePinTo(modularizePins);

		for (SourceMovePinModularizationData moduarizePin : modularizePins) {
			//
			// Remember info about of name text...
			//
			IAbstractSchemPin sourcePin = moduarizePin.getSourcePin();
			PinNameTextJustificationHandler.TextAttrHolder nt =
					PinNameTextJustificationHandler.getTextAttributeHolder(sourcePin);
			Point absLoc = moduarizePin.getAbsLocation();
			reallyMovePin(sourcePin, moduarizePin.getModularTarget(), absLoc.x, absLoc.y, nt);
		}

		return true;
	}

	private void constructModularMemberToMovePinTo(@NotNull List<? extends MovePinModularizationData> pins)
	{
		ListMap<IPinList, MovePinModularizationData> pinsToMoveTo = new ListMap<>();
		for (MovePinModularizationData pin : pins) {
			IPinList pinListToMovePinTo = pin.getAnchor();
			if (pinListToMovePinTo.getConnectivity() instanceof IConnector) {
				IPinList anchor = new ModularSchemPinListInfo(pinListToMovePinTo).getAnchor();
				IParameterized parameterized = anchor.getParameterized();
				assert parameterized != null;

				int side = anchor.getConnectivity() instanceof IPlugConnector ? 1 : 0;
				int referenceWidth = anchor.getReferenceWidth();
				Point relPt = PinListHelper.getRelativeToPinList(anchor, pin.getAbsLocation());
				int rel_x = parameterized.getExtent().getLeft() + side * referenceWidth;
				int rel_y = relPt.y;

				pin.setPoint(new Point(rel_x, rel_y));
				pinsToMoveTo.add(anchor, pin);
			}
		}

		for (Map.Entry<IPinList, List<MovePinModularizationData>> entry : pinsToMoveTo.entrySet()) {
			IPinList anchor = entry.getKey();
			ISchemDiagram diagram = DiagramHelper.getDiagram(anchor);
			assert diagram != null;
			List<MovePinModularizationData> pinsToAdd = entry.getValue();
			IObjectFilter<IAbstractSchemPin> filter = p -> {
				for (MovePinModularizationData pinArg : pinsToAdd) {
					if (pinArg.isPinUnderModularization(p)) {
						return false;
					}
				}
				return true;
			};
			ConnectorHelper.distributeAddPinArgsToPinLists(anchor, diagram,
					pinsToAdd, (pl, a) -> ((MovePinModularizationData) a).setModularTarget(pl), filter);
		}
	}

	protected void reallyMovePin(IAbstractSchemPin pin, IPinList destPinList, int x, int y,
			PinNameTextJustificationHandler.TextAttrHolder txtholder)
	{
		// Use relative sides to determine side before move
		int oldSide = PinNameTextJustificationHandler.getSideAsInteger(pin);
		//if pin is being moved to a different pinList instance
		IPinList parentPinList = (IPinList) pin.getParent();

		if (parentPinList != destPinList) {
			if (!ConnectionHelper.movePinToAnotherInstance(pin, parentPinList, destPinList)) {
				return;
			}
		}
		// Move the pin
		ILocation newLoc = CoordinateHelper.getRelLocation(pin, x, y);
		pin.setLocation(newLoc);
		if (moveJointOnMovingPin) {
			moveJoint(pin, x, y);
		}
		if (pin instanceof IPin && ((IConnectivityRef) pin).getConnectivity() instanceof IBackshellTermination) {
			return;
		}
		if (pin instanceof IPin && !(((IGenericSchemPin) pin).getConnectivity().getOwner() instanceof IConnector)) {
			pinJustificationHandler.justifyDevicePinNameText(pin, oldSide, txtholder);
		}
	}

	void moveJoint(IAbstractSchemPin pin, int x, int y)
	{

		// Move the node too.
		IJoint node = pin.getJoint();
		if (node != null) {
			node.setX(x);
			node.setY(y);
		}
		adjustLineStyleForSegments(pin);
	}

	public void adjustLineStyleForSegments(@NotNull IAbstractSchemPin pin)
	{
		if (pin.getJoint() != null) {
			Set<ILogicSegment> logicSegments = pin.getJoint().getAssociations(ILogicSegment.class);
			for (ILogicSegment seg : logicSegments) {
				if (seg.getParameterized() != null) {
					EndLineStyleUtils
							.updateEndLineStyle(seg, seg.getStartPoint(), seg.getEndPoint(),
									gridSpacing);
				}
				PortHelper.regeneratePortGfx(seg, gridSpacing);
			}
		}
	}
}