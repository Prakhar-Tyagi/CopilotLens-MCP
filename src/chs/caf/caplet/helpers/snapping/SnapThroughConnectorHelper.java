/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caf.caplet.helpers.snapping;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caplets.logic.AutoGenerateConnectorSupport;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.ConnectionFlow;
import chs.caplets.logic.actions.JoinPinlistsHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.CAFSchemSnapHelper;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinObject;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.Side;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.gfx.GfxView;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CommonUtils;
import chs.utilities.IXMLTags;
import chs.utilities.MapMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.GfxUtils;
import chs.utility.UserPreferenceUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DeviceConnectionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.helpers.SegmentHelper;
import chs.utility.logic.PinUtils;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper for snapping conductor to auto generated connector
 */
public class SnapThroughConnectorHelper extends SnapHelper
{

	private static final String CTRL_KEY_TEXT = ResourceMgr.getString(SnapThroughConnectorHelper.class,
			"SnapThroughConnectorHelper.CtrlKeyText");
	private static final String NEW_CONNECTOR_TOOLTIP = ResourceMgr.getString(SnapThroughConnectorHelper.class,
			"SnapThroughConnectorHelper.SwitchToNewConnectorTooltip", HTMLHelper.bold(CTRL_KEY_TEXT));
	private static final String FUNCTIONALITY_TOGGLE_TOOLTIP = ResourceMgr.getString(SnapThroughConnectorHelper.class,
			"SnapThroughConnectorHelper.AutoGenerateConnectorToggleOffGuidanceTooltip");
	private static final double DOUBLE_PRECISION = 0.01;
	private static MapMap<IPinList, IAbstractSchemPin, IAbstractSchemPin> m_devicePinToAttachedPinsCache =
			new MapMap<>();
	@NotNull private Map<IDynamicSnap, SchemConnectorPlaceholder> m_snapPointToConnectorInfo;
	@Nullable private Pair<IDynamicSnap, SchemConnectorPlaceholder> m_lastSnapPointToConnectorInfo;
	@NotNull private ISnapThroughConnectorController m_snapController;
	public static final String NEW_CONNECTOR_PREFIX = "P-";

	public SnapThroughConnectorHelper(@NotNull ISnapThroughConnectorController snapController, boolean m_snapToGrid,
			boolean m_snapToSubGrid)
	{
		super(snapController, m_snapToGrid, m_snapToSubGrid);
		m_snapController = snapController;
		m_snapPointToConnectorInfo = new LinkedHashMap<>();
		m_lastSnapPointToConnectorInfo = null;
	}

	public static boolean isConnectionAvailable(@NotNull SchemConnectorPlaceholder connectorSnap,
			@Nullable ILogicObject snapSourceObject)
	{
		boolean isSourceConnectable = true;
		if (snapSourceObject instanceof IWireConductor) {
			IWireConductor wire = (IWireConductor) snapSourceObject;
			IAbstractPin targetPin = null;
			if (connectorSnap.getOwner().isMated() &&
					connectorSnap.getTargetPinObject() instanceof IPin) {
				IPin targetSchemPin = (IPin) connectorSnap.getTargetPinObject();
				IHarnessPlugConnector existingConnector =
						connectorSnap.getOwner().getExistingConnector();
				if (existingConnector != null) {
					targetPin = targetSchemPin.getConnectivity().getConnectedPin(existingConnector);
				}
			}
			if (targetPin != null) {
				isSourceConnectable = ConductorHelper.availableConnection(wire, targetPin);
			}
			else {
				isSourceConnectable = ConductorHelper.availableConnection(wire);
			}
		}
		return isSourceConnectable;
	}

	public static void clearCachedObjects()
	{
		m_devicePinToAttachedPinsCache.clear();
	}

	@NotNull @Override
	public Point snappedPoint(@NotNull Point currpt, int radius, Class<?> snappingSource, boolean ctrlDown)
	{
		updateToolTipText(null);
		m_snapController.updateCursor(true);
		Point snappedPoint = super.snappedPoint(currpt, radius, snappingSource, ctrlDown);
		SchemConnectorPlaceholder schemConnectorPlaceholder = snapThroughConnector(getLastSnapped(), ctrlDown);
		if (schemConnectorPlaceholder != null) {
			snappedPoint.setLocation(schemConnectorPlaceholder.getSnapPoint());
		}
		return snappedPoint;
	}

	@Override
	protected boolean isWireSnappable(@NotNull IDynamicSnap dynamicSnap)
	{
		ILogicObject sourceObject = m_snapController.getSnapSourceObject();
		boolean wireSnapAllowed = m_snapController.checkWireCanBeSnapped(dynamicSnap, sourceObject);
		if (!wireSnapAllowed) {
			updateToolTipText(ResourceMgr.getString(CAFSchemSnapHelper.class,
					"CAFSchemSnapHelper.CannotConnectWire"));
			m_snapController.updateCursor(false);
		}
		return wireSnapAllowed;
	}

	@Nullable private SchemConnectorPlaceholder snapThroughConnector(@Nullable IDynamicSnap snap, boolean isControlDown)
	{
		if (m_snapPointToConnectorInfo.containsKey(snap)) {
			return m_snapPointToConnectorInfo.get(snap);
		}
		if (m_lastSnapPointToConnectorInfo != null) {
			IGfxObject lastAddedGfx =
					m_lastSnapPointToConnectorInfo.getSecond().getGfxObject();
			getDynamicGfxService().removeTransientGfx(lastAddedGfx);
			m_lastSnapPointToConnectorInfo = null;
		}
		IDynamicSnap lastAddedSnap = getLastAddedSnap();
		if (lastAddedSnap != null && m_snapPointToConnectorInfo.containsKey(lastAddedSnap) &&
				m_snapController.overrideLastSnapped()) {
			discardSnap(lastAddedSnap);
		}
		if (snap != null && m_snapController.isSnapThroughConnectorEnabled()) {
			SchemConnectorPlaceholder schemConnectorPlaceholder =
					getConnectorPlaceholder(snap, isControlDown, m_snapPointToConnectorInfo.values());
			if (schemConnectorPlaceholder != null) {
				boolean isSourceConnectable = m_snapController.isConnectionAvailable(schemConnectorPlaceholder);
				if (!isSourceConnectable) {
					return null;
				}
				IGfxObject connectorGfx = schemConnectorPlaceholder.getGfxObject();
				getDynamicGfxService().addTransientGfx(connectorGfx);
				CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
				m_lastSnapPointToConnectorInfo = new Pair<>(snap, schemConnectorPlaceholder);
				updateToolTipText(getTooltipText(schemConnectorPlaceholder));
				return schemConnectorPlaceholder;
			}
		}
		return null;
	}

	public static boolean checkWireCanBeSnapped(@NotNull IDynamicSnap snap, @Nullable ILogicObject logicObject)
	{
		if (logicObject != null && logicObject instanceof IWireConductor && logicObject.getSharedObject() == null) {
			IWireConductor wireConductor = (IWireConductor) logicObject;
			for (Iterator<IDynamicGfxMediator> it = snap.getMediators(); it.hasNext(); ) {
				IDynamicGfxMediator mediator = it.next();
				IPinObject pinObject = CommonUtils.cast(mediator, IPinObject.class);
				if (pinObject instanceof IPin) {
					IPin schemPin = (IPin) pinObject;
					IAbstractPin cablePin = schemPin.getConnectivity();
					return ConductorHelper.availableConnection(wireConductor, cablePin);
				}
				else if (pinObject instanceof IPinPlaceholder) {
					return ConductorHelper.availableConnection(wireConductor);
				}
			}
		}
		return true;
	}

	@Nullable public static String getTooltipText(@NotNull SchemConnectorPlaceholder schemConnectorPlaceholder)
	{
		List<String> tooltipTexts = new ArrayList<>();
		if (schemConnectorPlaceholder.isNearByConnector()) {
			tooltipTexts.add(NEW_CONNECTOR_TOOLTIP);
		}
		String storedPreference =
				UserPreferenceUtils.getClassPreferences(Model.class).get(AutoGenerateConnectorSupport.PREF_KEY_AUTO_GENERATE_CONNECTOR, null);
		if (storedPreference == null) {
			tooltipTexts.add(FUNCTIONALITY_TOGGLE_TOOLTIP);
		}
		return tooltipTexts.isEmpty() ? null : tooltipTexts.stream().collect(
				Collectors.joining(HTMLHelper.lineBreak(), HTMLHelper.getHTMLHeader(), HTMLHelper.getHTMLTrailer()));
	}

	public static void updateToolTipText(@Nullable String toolTipText)
	{
		GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
		if (gfxView != null) {
			if (toolTipText == null) {
				gfxView.clearPopupTooltip();
			}
			else {
				Point devicePoint = gfxView.worldToDevice(gfxView.getCurrentMouseLocation());
				int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
				Point toolTipPoint = new Point(devicePoint.x + tooltipShift, devicePoint.y + tooltipShift);
				gfxView.showTooltipAtLocation(toolTipText, toolTipPoint);
			}
		}
	}

	@Override
	@Nullable public IDynamicSnap markSnap(int modifiers)
	{
		IDynamicSnap snap = super.markSnap(modifiers);
		if (m_lastSnapPointToConnectorInfo != null && m_lastSnapPointToConnectorInfo.getFirst() == snap) {
			updateToolTipText(null);
			SchemConnectorPlaceholder schemConnectorPlaceholder = m_lastSnapPointToConnectorInfo.getSecond();
			schemConnectorPlaceholder.removeNewConnectorText();
			m_snapPointToConnectorInfo.put(snap, schemConnectorPlaceholder);
			m_lastSnapPointToConnectorInfo = null;
		}
		return snap;
	}

	@Override
	public void discardSnap(@Nullable IDynamicSnap snapToDiscard)
	{
		super.discardSnap(snapToDiscard);
		discardConnectorSnap(snapToDiscard);
	}

	private void discardConnectorSnap(@Nullable IDynamicSnap snapToDiscard)
	{
		if (m_snapPointToConnectorInfo.containsKey(snapToDiscard)) {
			SchemConnectorPlaceholder schemConnectorPlaceholder = m_snapPointToConnectorInfo.get(snapToDiscard);
			getDynamicGfxService().removeTransientGfx(schemConnectorPlaceholder.getGfxObject());
			m_snapPointToConnectorInfo.remove(snapToDiscard);
			CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void handleConnectorSnaps(
			Collection<IDynamicGfxMediator> connectingObjects)
	{
		Map<IDynamicSnap, SchemConnectorPlaceholder> connectorSnaps = getValidConnectorSnaps();
		Set<IDynamicSnap> invalidConnectorSnaps =
				m_snapPointToConnectorInfo.keySet().stream().collect(Collectors.toSet());
		invalidConnectorSnaps.removeAll(connectorSnaps.keySet());
		invalidConnectorSnaps.forEach(this::discardSnap);
		List<IPinList> connectors = updateSnapsWithConnectors(connectorSnaps, m_snapController.getSnapSourceObject());
		List<Pair<IDynamicSnap, Integer>> realconnectorSnaps =
				getAllSnapped().stream().filter(snap -> m_snapPointToConnectorInfo.containsKey(snap.getFirst()))
						.collect(Collectors.toList());
		for (IDynamicGfxMediator med : connectingObjects) {
			med.addConnectivity(realconnectorSnaps.iterator());
		}
		for (IPinList schemConnector : connectors) {
			joinConnectorWithNearByPinlists(schemConnector);
		}
		m_snapPointToConnectorInfo.keySet().stream().collect(Collectors.toSet()).forEach(this::discardSnap);
		m_snapPointToConnectorInfo.clear();
	}

	public static void joinConnectorWithNearByPinlists(@NotNull IPinList schemConnector)
	{
		if (isDeletedObject(schemConnector)) {
			return;
		}
		List<IPinList> candidateSchemConnectors = getCandidateSchemConnectors(schemConnector);

		IExtent connectorExtent = ExtentHelper.getAbsNonTextExtent(schemConnector);
		IPinList leftOrTopConnector = candidateSchemConnectors.stream().filter(candidateConnector -> {
			Side side = Side.getSide(connectorExtent, candidateConnector.getAbsLocation());
			return side.isLeft() || side.isTop();
		}).findFirst().orElse(null);
		IPinList rightOrBottomConnector = candidateSchemConnectors.stream().filter(candidateConnector -> {
			Side side = Side.getSide(connectorExtent, candidateConnector.getAbsLocation());
			return side.isRight() || side.isBottom();
		}).findFirst().orElse(null);
		JoinPinlistsHelper helper = new JoinPinlistsHelper();
		candidateSchemConnectors.removeIf(candidateConnector -> candidateConnector != leftOrTopConnector &&
				candidateConnector != rightOrBottomConnector);
		for (IPinList candidateSchemConnector : candidateSchemConnectors) {
			if (helper.isStitchPossibleOnPinlistsSelected(List.of(schemConnector, candidateSchemConnector)) &&
					helper.hasValidOperand()) {
				helper.completeEdits();
			}
		}
	}

	@NotNull private static List<IPinList> getCandidateSchemConnectors(@NotNull IPinList schemConnector)
	{
		IPinList attachedDevice =
				schemConnector.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR)
						.stream()
						.filter(pinList -> pinList.getConnectivity() instanceof IDevice)
						.findFirst()
						.orElse(null);
		if (attachedDevice == null) {
			return new  ArrayList<>();
		}
		return new SameSidePinListFinder().findMatchingPinListsOnSameSide(
				attachedDevice,
				(IConnector) schemConnector.getConnectivity(),
				schemConnector,
				pinList -> !schemConnector.equals(pinList));
	}

	private static boolean isDeletedObject(@NotNull IUIDObject uidObject)
	{
		return uidObject.isDeletedObject() ||
				CreationDeletionHelper.getTheCreationHelper().goingToDelete(uidObject);
	}

	@NotNull public static List<IPinList> updateSnapsWithConnectors(
			@NotNull Map<IDynamicSnap, SchemConnectorPlaceholder> connectorSnaps, @Nullable ILogicObject snapSource)
	{
		List<IPinList> addedConnectors = new ArrayList<>();
		Set<ConnectorPlaceholder> connectorsToBeDeleted = new HashSet<>();
		for (IDynamicSnap connectorSnap : connectorSnaps.keySet()) {
			SchemConnectorPlaceholder schemConnectorPlaceholder = connectorSnaps.get(connectorSnap);
			List<IDynamicGfxMediator> mediators = new ArrayList<>();
			connectorSnap.getMediators().forEachRemaining(mediators::add);
			mediators.forEach(connectorSnap::removeMediator);
			IPinList pinList = schemConnectorPlaceholder.transformToSchemConnector();
			if (pinList == null) {
				continue;
			}
			IUIDObjectCollection<IAbstractSchemPin> allPins = pinList.getAllPins();
			if (!allPins.isEmpty()) {
				addedConnectors.add(pinList);
				IAbstractSchemPin schemPin = allPins.iterator().next();
				if (schemPin instanceof ISchemStackPin &&
						!canConnectToStackPin((ISchemStackPin) schemPin, snapSource)) {
					String errorMsg = ResourceMgr.getString(SnapThroughConnectorHelper.class,
							"SnapThroughConnectorHelper.CannotConnectToStackPinError",
							pinList.getConnectivity().getName());
					reportMessageInOutputWindow(errorMsg);
				}
				else if (schemPin instanceof IDynamicGfxMediator) {
					connectorSnap.addMediator((IDynamicGfxMediator) schemPin);
				}
			}
			else {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(pinList);
				IPin targetSchemPin = CommonUtils.cast(schemConnectorPlaceholder.getTargetPinObject(), IPin.class);
				IAbstractPin targetCablePin = targetSchemPin == null ? null : targetSchemPin.getConnectivity();
				chs.cof.logical.cable.IPinList owner = targetCablePin == null ? null : targetCablePin.getOwner();
				if (targetCablePin != null && owner != null) {
					String errorMsg = ResourceMgr.getString(SnapThroughConnectorHelper.class,
							"SnapThroughConnectorHelper.ConnectingDeviceWithConnectorError", owner.getName(),
							targetCablePin.getName());
					reportMessageInOutputWindow(errorMsg);
				}
				if (schemConnectorPlaceholder.getOwner().getExistingConnector() == null) {
					connectorsToBeDeleted.add(schemConnectorPlaceholder.getOwner());
				}
			}
			connectorSnap.getPoint().setLocation(schemConnectorPlaceholder.getSnapPoint());
		}
		for (ConnectorPlaceholder connectorPlaceholder : connectorsToBeDeleted) {
			ISchemDiagram diagram = connectorPlaceholder.getDiagram();
			IHarnessPlugConnector connector = connectorPlaceholder.getTranformedConnector();
			boolean hasNonDeletedUsages = diagram.getRepresentations(connector.getUID()).stream()
					.filter(Predicate.not(SnapThroughConnectorHelper::isDeletedObject)).count() > 0;
			if (!hasNonDeletedUsages) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(connector);
			}
		}
		return addedConnectors;
	}

	private static boolean canConnectToStackPin(@NotNull ISchemStackPin stackPin, @Nullable ILogicObject snapSource)
	{
		Set<? extends IAbstractPin> pinsInStack = stackPin.getAllConnectivity();
		return pinsInStack.stream()
				.filter(pin -> snapSource instanceof IConductor ?
						SegmentHelper.canConnectToStackedPin(pin, (IConductor) snapSource) :
						pin.getConductorsAsSet().isEmpty())
				.count() > 0;
	}

	private static void reportMessageInOutputWindow(@NotNull String msg)
	{
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		if (outputWindow != null) {
			outputWindow.sendMessage(HTMLHelper.color(IXMLTags.RED, msg), LogTabType.TAB_HCONN.getLabel(), true);
		}
	}

	@NotNull private Map<IDynamicSnap, SchemConnectorPlaceholder> getValidConnectorSnaps()
	{
		Map<IDynamicSnap, SchemConnectorPlaceholder> validSnaps = new LinkedHashMap<>();
		for (IDynamicSnap snap : m_snapPointToConnectorInfo.keySet()) {
			SchemConnectorPlaceholder schemConnectorPlaceholder = m_snapPointToConnectorInfo.get(snap);
			if (schemConnectorPlaceholder.areReferencesEditable()) {
				validSnaps.put(snap, schemConnectorPlaceholder);
			}
		}
		return validSnaps;
	}

	@Nullable
	public static SchemConnectorPlaceholder getConnectorPlaceholder(@NotNull IDynamicSnap snap, boolean isControlDown,
			@NotNull Collection<SchemConnectorPlaceholder> existingConnectorPlaceholders)
	{
		SchemConnectorPlaceholder schemConnectorPlaceholder = null;
		for (Iterator<IDynamicGfxMediator> it = snap.getMediators(); it.hasNext(); ) {
			IDynamicGfxMediator mediator = it.next();
			IPinList schemPinlist = null;
			IPinObject pinObject = CommonUtils.cast(mediator, IPinObject.class);
			if (pinObject instanceof IAbstractSchemPin) {
				IAbstractSchemPin schemPin = (IAbstractSchemPin) pinObject;
				if (schemPin.isConnectedToSomething() || PinUtils.isStudPin(schemPin) || !schemPin.isDevicePin()) {
					return null;
				}
				schemPinlist = CommonUtils.cast(schemPin.getParent(), IPinList.class);
			}
			else if (pinObject instanceof IPinPlaceholder) {
				IPinPlaceholder pinPlaceholder = (IPinPlaceholder) pinObject;
				schemPinlist = CommonUtils.cast(pinPlaceholder.getOwner(), IPinList.class);
			}
			IDevice device =
					schemPinlist != null ? CommonUtils.cast(schemPinlist.getConnectivity(), IDevice.class) :
							null;

			if (device != null) {
				Set<IHarnessPlugConnector> matedConnectors = getMatedConnectors(pinObject);
				if (matedConnectors.size() > 1 || !isValidToCreateConnector(pinObject)) {
					return null;
				}
				IHarnessPlugConnector candidateConnector =
						matedConnectors.isEmpty() ? null : matedConnectors.iterator().next();
				ConnectorPlaceholder connectorPlaceholder = null;
				boolean nearBy = false;
				if (candidateConnector != null) {
					connectorPlaceholder =
							new ConnectorPlaceholder(schemPinlist.getDiagram(), candidateConnector, true);
				}
				else if (!isControlDown) {
					connectorPlaceholder = getNearestConnector(schemPinlist, pinObject, existingConnectorPlaceholders);
					if (connectorPlaceholder != null) {
						nearBy = true;
					}
				}
				if (connectorPlaceholder == null) {
					long newConnectorIndex = 1 + existingConnectorPlaceholders.stream()
							.map(SchemConnectorPlaceholder::getOwner)
							.filter(connPlaceholder -> connPlaceholder.getExistingConnector() == null)
							.distinct().count();
					connectorPlaceholder = new ConnectorPlaceholder(schemPinlist.getDiagram(),
							NEW_CONNECTOR_PREFIX + newConnectorIndex);
				}
				schemConnectorPlaceholder = new SchemConnectorPlaceholder(pinObject, connectorPlaceholder, nearBy);
				break;
			}
		}
		return schemConnectorPlaceholder;
	}

	private static boolean isValidToCreateConnector(@NotNull IPinObject pinObject)
	{
		if (pinObject instanceof ISchemStackPin) {
			Set<? extends IAbstractPin> stackPins = ((ISchemStackPin) pinObject).getAllConnectivity();
			if (stackPins.isEmpty()) {
				return false;
			}
			Set<IHarnessPlugConnector> matedConnectorsRef = null;
			for (IAbstractPin pin : stackPins) {
				Set<IHarnessPlugConnector> matedConnectors = getMatedConnectors(pin);
				if (matedConnectorsRef == null) {
					matedConnectorsRef = new HashSet<>(matedConnectors);
				}
				else if (matedConnectorsRef.size() != matedConnectors.size() ||
						!matedConnectorsRef.containsAll(matedConnectors)) {
					return false;
				}
			}
		}
		return true;
	}

	@NotNull private static Set<IHarnessPlugConnector> getMatedConnectors(@NotNull IPinObject pinObject)
	{
		if (pinObject instanceof IPin) {
			IAbstractPin cablePin = ((IPin) pinObject).getConnectivity();
			return getMatedConnectors(cablePin);
		}
		if (pinObject instanceof ISchemStackPin) {
			Set<? extends IAbstractPin> stackPins = ((ISchemStackPin) pinObject).getAllConnectivity();
			Set<IHarnessPlugConnector> matedConnectors = new HashSet<>();
			for (IAbstractPin pin : stackPins) {
				matedConnectors.addAll(getMatedConnectors(pin));
			}
			return matedConnectors;
		}
		return Collections.emptySet();
	}

	@NotNull private static Set<IHarnessPlugConnector> getMatedConnectors(@NotNull IAbstractPin pin)
	{
		return pin.getConnectedPinLists().stream()
				.filter(IHarnessPlugConnector.class::isInstance)
				.map(IHarnessPlugConnector.class::cast)
				.collect(Collectors.toSet());
	}

	@Nullable private static ConnectorPlaceholder getNearestConnector(@NotNull IPinList schemDevice,
			@NotNull IPinObject srcPin, @NotNull Collection<SchemConnectorPlaceholder> existingConnectorPlaceholders)
	{
		Set<IPinList> candidateAttachedConnectors = getCandidateAttachedConnectors(schemDevice);
		Map<IAbstractSchemPin, IAbstractSchemPin> pinToAttachedPins =
				getPinToAttachedPins(schemDevice, candidateAttachedConnectors, DeviceConnectionHelper::new);
		IAbstractSchemPin nearestDevicePin = null;

		IExtent deviceExtent = ExtentHelper.getAbsNonTextExtent(schemDevice);
		ILocation srcPinLocation = CoordinateHelper.getAbsGfxLocation(srcPin, 0, 0);
		Side srcPinSide = Side.getSide(deviceExtent, srcPinLocation);
		for (IAbstractSchemPin candidatePin : pinToAttachedPins.keySet()) {
			if (nearestDevicePin == null) {
				nearestDevicePin = candidatePin;
			}
			else {
				Side candidatePinSide = Side.getSide(deviceExtent, candidatePin.getAbsLocation());
				Side nearestPinSide = Side.getSide(deviceExtent, nearestDevicePin.getAbsLocation());
				if ((nearestPinSide == srcPinSide) ^ (candidatePinSide == srcPinSide)) {
					nearestDevicePin = candidatePinSide == srcPinSide ? candidatePin : nearestDevicePin;
				}
				else {
					double candidatePinDistance = candidatePin.getAbsLocation().distance(srcPinLocation);
					double nearestPinDistance = nearestDevicePin.getAbsLocation().distance(srcPinLocation);
					if (Math.abs(nearestPinDistance - candidatePinDistance) > DOUBLE_PRECISION) {
						nearestDevicePin = nearestPinDistance > candidatePinDistance ? candidatePin : nearestDevicePin;
					}
					else {
						String candidateParentName = getParentPinlistName(pinToAttachedPins.get(candidatePin));
						String nearestParentName = getParentPinlistName(pinToAttachedPins.get(nearestDevicePin));
						int compare = AlphaNumComparator.<String>getCaseSensitiveComparator()
								.compare(nearestParentName, candidateParentName);
						if (compare == 0) {
							compare = nearestDevicePin.getUID().compareTo(candidatePin.getUID());
						}
						if (compare > 0) {
							nearestDevicePin = candidatePin;
						}
					}
				}
			}
		}
		ConnectorPlaceholder nearestConnectorPlaceholder = null;
		if (nearestDevicePin != null) {
			IAbstractSchemPin nearestConnectorPin = pinToAttachedPins.get(nearestDevicePin);
			if (nearestConnectorPin != null) {
				IPinList candidateSchemConnector = CommonUtils.cast(nearestConnectorPin.getParent(), IPinList.class);
				if (candidateSchemConnector != null) {
					IHarnessPlugConnector nearestConnector =
							CommonUtils.cast(candidateSchemConnector.getConnectivity(), IHarnessPlugConnector.class);
					if (nearestConnector != null) {
						nearestConnectorPlaceholder =
								new ConnectorPlaceholder(candidateSchemConnector.getDiagram(), nearestConnector, false);
					}
				}
			}
		}
		SchemConnectorPlaceholder nearestExistingConnectorPlaceholder = null;
		for (SchemConnectorPlaceholder existingConnectorPlaceholder : existingConnectorPlaceholders) {
			if (existingConnectorPlaceholder.getPinParent(existingConnectorPlaceholder.getTargetPinObject()) !=
					schemDevice) {
				continue;
			}
			if (nearestExistingConnectorPlaceholder == null) {
				nearestExistingConnectorPlaceholder = existingConnectorPlaceholder;
			}
			else {
				IPinObject nearestPinObject = nearestExistingConnectorPlaceholder.getTargetPinObject();
				IPinObject candidatePinObject = existingConnectorPlaceholder.getTargetPinObject();
				ILocation nearestPinObjectLocation = CoordinateHelper.getAbsGfxLocation(nearestPinObject, 0, 0);
				ILocation candidatePinObjectLocation = CoordinateHelper.getAbsGfxLocation(candidatePinObject, 0, 0);
				Side nearestPinSide = Side.getSide(deviceExtent, nearestPinObjectLocation);
				Side candidatePinSide = Side.getSide(deviceExtent, candidatePinObjectLocation);
				if ((nearestPinSide == srcPinSide) ^ (candidatePinSide == srcPinSide)) {
					nearestExistingConnectorPlaceholder =
							candidatePinSide == srcPinSide ? existingConnectorPlaceholder :
									nearestExistingConnectorPlaceholder;
				}
				else {
					double candidatePinDistance = candidatePinObjectLocation.distance(srcPinLocation);
					double nearestPinDistance = nearestPinObjectLocation.distance(srcPinLocation);
					if (Math.abs(nearestPinDistance - candidatePinDistance) > DOUBLE_PRECISION) {
						nearestExistingConnectorPlaceholder =
								nearestPinDistance > candidatePinDistance ? existingConnectorPlaceholder :
										nearestExistingConnectorPlaceholder;
					}
					else {
						String candidateParentName = existingConnectorPlaceholder.getOwner().getName();
						String nearestParentName = nearestExistingConnectorPlaceholder.getOwner().getName();
						int compare = AlphaNumComparator.<String>getCaseSensitiveComparator()
								.compare(nearestParentName, candidateParentName);
						if (compare > 0) {
							nearestExistingConnectorPlaceholder = existingConnectorPlaceholder;
						}
					}
				}
			}
		}
		if (nearestConnectorPlaceholder == null && nearestExistingConnectorPlaceholder != null) {
			nearestConnectorPlaceholder = nearestExistingConnectorPlaceholder.getOwner();
		}
		if (nearestDevicePin != null && nearestConnectorPlaceholder != null &&
				nearestExistingConnectorPlaceholder != null) {
			IPinObject nearestPinObject = nearestDevicePin;
			IPinObject candidatePinObject = nearestExistingConnectorPlaceholder.getTargetPinObject();
			ILocation nearestPinObjectLocation = CoordinateHelper.getAbsGfxLocation(nearestPinObject, 0, 0);
			ILocation candidatePinObjectLocation = CoordinateHelper.getAbsGfxLocation(candidatePinObject, 0, 0);
			Side nearestPinSide = Side.getSide(deviceExtent, nearestPinObjectLocation);
			Side candidatePinSide = Side.getSide(deviceExtent, candidatePinObjectLocation);
			if ((nearestPinSide == srcPinSide) ^ (candidatePinSide == srcPinSide)) {
				nearestConnectorPlaceholder =
						candidatePinSide == srcPinSide ? nearestExistingConnectorPlaceholder.getOwner() :
								nearestConnectorPlaceholder;
			}
			else {
				double candidatePinDistance = candidatePinObjectLocation.distance(srcPinLocation);
				double nearestPinDistance = nearestPinObjectLocation.distance(srcPinLocation);
				if (Math.abs(nearestPinDistance - candidatePinDistance) > DOUBLE_PRECISION) {
					nearestConnectorPlaceholder =
							nearestPinDistance > candidatePinDistance ? nearestExistingConnectorPlaceholder.getOwner() :
									nearestConnectorPlaceholder;
				}
				else {
					String candidateParentName = nearestExistingConnectorPlaceholder.getOwner().getName();
					String nearestParentName = nearestConnectorPlaceholder.getName();
					int compare = AlphaNumComparator.<String>getCaseSensitiveComparator()
							.compare(nearestParentName, candidateParentName);
					if (compare > 0) {
						nearestConnectorPlaceholder = nearestExistingConnectorPlaceholder.getOwner();
					}
				}
			}
		}
		return nearestConnectorPlaceholder;
	}

	@Nullable private static String getParentPinlistName(@Nullable IAbstractSchemPin pin)
	{
		if (pin != null) {
			IPinList parent = CommonUtils.cast(pin.getParent(), IPinList.class);
			if (parent != null) {
				chs.cof.logical.cable.IPinList connectivity = parent.getConnectivity();
				if (connectivity != null) {
					return connectivity.getName();
				}
			}
		}
		return null;
	}

	@NotNull
	static Map<IAbstractSchemPin, IAbstractSchemPin> getPinToAttachedPins(@NotNull IPinList schemDevice,
			@NotNull Set<IPinList> candidateAttachedPinlists,
			@NotNull Function<IPinList, DeviceConnectionHelper> connectionHelperProvider)
	{
		if (!m_devicePinToAttachedPinsCache.containsKey(schemDevice)) {
			DeviceConnectionHelper connectionHelper = connectionHelperProvider.apply(schemDevice);
			connectionHelper.setConnectionFlow(ConnectionFlow.AutCreateConnectorConnection);
			Map<IAbstractSchemPin, IAbstractSchemPin> pinToAttachedConnectorPin = new HashMap<>();
			for (IPinList attachedPinlist : candidateAttachedPinlists) {
				connectionHelper.resetPinList(attachedPinlist);
				for (IAbstractSchemPin connectorPin : attachedPinlist.getAllPins()) {
					IAbstractSchemPin matchingPin =
							CommonUtils.cast(connectionHelper.getMatchingPin(connectorPin), IAbstractSchemPin.class);
					if (matchingPin != null) {
						pinToAttachedConnectorPin.put(matchingPin, connectorPin);
					}
				}
			}
			m_devicePinToAttachedPinsCache.put(schemDevice, pinToAttachedConnectorPin);
		}
		return m_devicePinToAttachedPinsCache.get(schemDevice);
	}

	@NotNull static Set<IPinList> getCandidateAttachedConnectors(@NotNull IPinList schemDevice)
	{
		return schemDevice.getAttachedObjects(IPinList.EXCLUDE_MODULAR).stream()
				.filter(IPinList.class::isInstance)
				.map(IPinList.class::cast)
				.filter(attachedPinlist -> {
					IHarnessPlugConnector connector = CommonUtils.cast(attachedPinlist.getConnectivity(),
							IHarnessPlugConnector.class);
					return connector != null && connector.getLibraryRef() == null && !connector.isShared() &&
							connector.isPlug() && connector.isNotRingTerminal() &&
							!IDeviceConnector.class.isInstance(connector);
				}).collect(Collectors.toSet());
	}

	@Override public void endSnapping(
			Collection<IDynamicGfxMediator> connectingObjects)
	{
		super.endSnapping(connectingObjects);
		handleConnectorSnaps(connectingObjects);
	}
}