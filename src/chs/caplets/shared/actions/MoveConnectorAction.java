package chs.caplets.shared.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.MoveConnectorActionUI;
import chs.caplets.logic.actions.MovePinAction;
import chs.caplets.logic.actions.PinNameTextJustificationHandler;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.ITransform;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinListGenerationParams;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.cofUtils.parameterized.PinlistSideCalculator;
import chs.common.Extent;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.common.Side;
import chs.common.styles.IStyleableObject;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.BaseDeviceConnectionHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.PinListConnectionHelper;
import chs.utility.helpers.PinPlaceholderProviderForSymbolledDeviceInMove;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.TransformHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Action to move selected connectors around the attached device
 */
public class MoveConnectorAction extends ControllerActionRT
		implements MouseListener, MouseMotionListener, ICtxMenuProvider, KeyListener
{

	@Nullable protected SelectedConnectorDynamicGfx selectedConnectorParams;
	private static Comparator<LocationAndAbstractPin> comparator =
			(locAndPin1, locAndPin2) -> locAndPin1.compareTo(locAndPin2);

	private static class LocationAndAbstractPin
	{

		private Integer x_y;

		LocationAndAbstractPin(Integer x_y)
		{
			this.x_y = x_y;
		}

		LocationAndAbstractPin(Side side, IAbstractSchemPin pin)
		{
			if (side.isRight() || side.isLeft()) {
				x_y = pin.getAbsLocation().getY();
			}
			else {
				x_y = pin.getAbsLocation().getX();
			}
		}

		int compareTo(LocationAndAbstractPin another)
		{
			return Integer.compare(x_y, another.x_y);
		}
	}

	protected static class MatedDeviceConstraints
	{

		public static final LocationAndAbstractPin[] EMPTY_ARRAY = new LocationAndAbstractPin[0];
		private PinSideCalculator pinSideCalculator;

		private Map<Side, ArrayList<LocationAndAbstractPin>> sortedPinsOnASide = new EnumMap<>(Side.class);
		private Map<Side, ArrayList<IExtent>> pinListExtents = new EnumMap<>(Side.class);

		boolean init(IPinList conn, IPinList matedDevice, Collection<IAbstractSchemPin> matchingMatePins,
				IExtent deviceSchemExtent)
		{
			pinSideCalculator = PinSideCalculator.createAbsolute(matedDevice);
			matedDevice.getAllPins().stream()
					.filter(aPin ->
							PinlistSideCalculator.getBoundarySide(deviceSchemExtent, aPin.getAbsLocation()) != null &&
									!matchingMatePins.contains(aPin)).forEach(aPin -> {

				Side side = pinSideCalculator.getSide(aPin);
				List<LocationAndAbstractPin> sortedPinsOnThisSide =
						sortedPinsOnASide.computeIfAbsent(side, currentSide -> new ArrayList<>());
				sortedPinsOnThisSide.add(new LocationAndAbstractPin(side, aPin));
			});
			sortedPinsOnASide.entrySet().forEach(anEntry -> Collections.sort(anEntry.getValue(),
					comparator
			));
			for (IPinList attachedPinlist : matedDevice.getAttachedPinListObjects()) {
				if (attachedPinlist == conn || !(attachedPinlist.getConnectivity() instanceof IPlugConnector)) {
					continue;
				}
				IExtent attachedConnExtent = ConnectionHelper.getAbsExtent(attachedPinlist);
				Pair<Side, Integer> sideAndBoundary =
						PinlistSideCalculator.getSideAndBoundary(attachedConnExtent, deviceSchemExtent);
				if (sideAndBoundary == null) {
					continue;
				}
				List<IExtent> pinListExtentsOnASide =
						pinListExtents.computeIfAbsent(sideAndBoundary.getFirst(), currentSide -> new ArrayList<>());

				pinListExtentsOnASide.add(attachedConnExtent);
			}
			return true;
		}

		private boolean isRestricted(IExtent extent, Side side)
		{
			List<LocationAndAbstractPin> pinsOnASide = sortedPinsOnASide.get(side);
			if (pinsOnASide == null) {
				return false;
			}
			LocationAndAbstractPin[] array = pinsOnASide.toArray(EMPTY_ARRAY);
			//check if there are pins on device between the first pin that is above the top of connector extent and the first pin that is below the connector extent
			boolean connectorSpansAcrossDevPin;
			if (side.isLeft() || side.isRight()) {

				int lowerIndex = Arrays.binarySearch(array, new LocationAndAbstractPin(extent.getBottom()), comparator);

				int upperIndex = Arrays.binarySearch(array, new LocationAndAbstractPin(extent.getTop()), comparator);
				connectorSpansAcrossDevPin = connectorSpansDevicePins(lowerIndex, upperIndex);
			}
			else {
				int leftIndex = Arrays.binarySearch(array, new LocationAndAbstractPin(extent.getLeft()), comparator);
				int rightIndex = Arrays.binarySearch(array, new LocationAndAbstractPin(extent.getRight()), comparator);
				connectorSpansAcrossDevPin = connectorSpansDevicePins(leftIndex, rightIndex);
			}
			if (connectorSpansAcrossDevPin) {
				return true;
			}
			if (pinListExtents.get(side) != null) {
				for (IExtent attachedConnExtent : pinListExtents.get(side)) {
					if (!connectorDoesNotIntersectOther(extent, attachedConnExtent, side)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean connectorDoesNotIntersectOther(IExtent extent, IExtent attachedConnExtent, Side side)
		{
			if (side.isRight() || side.isLeft()) {
				return extent.getTop() < attachedConnExtent.getBottom() ||
						extent.getBottom() > attachedConnExtent.getTop();
			}
			else {
				return extent.getLeft() > attachedConnExtent.getRight() ||
						extent.getRight() < attachedConnExtent.getLeft();
			}
		}

		private boolean connectorSpansDevicePins(int givenLowerIndex, int givenUpperIndex)
		{
			int lowerIndex = givenLowerIndex;
			if (lowerIndex >= 0) {
				return true;
			}
			lowerIndex = Math.abs(lowerIndex + 1);
			int upperIndex = givenUpperIndex;
			if (upperIndex >= 0) {
				return true;
			}
			upperIndex = Math.abs(upperIndex + 1);
			return upperIndex > lowerIndex;
		}
	}

	protected static class SelectedConnectorDynamicGfx
	{

		public static final int MINUS_NINETY = -90;
		public static final int NINETY = 90;
		protected List<IPinList> m_draggingPlugs;
		private IPinList owner;
		protected Side originalConnectorSide;
		protected int originalSideIndex;

		protected ITransform originalTransform;

		protected List<IDynamicGfx> dynamicGfx = new ArrayList<>();
		protected IExtent deviceExtent;
		protected List<IExtent> parameterizedExtents;
		protected Collection<MovePinAction.ManageAssociations> associations;

		protected List<Map<IAbstractSchemPin, IAbstractSchemPin>> matchingSchemPins;
		private boolean reversed = false;

		protected MatedDeviceConstraints matedDeviceConstraints;

		private IGrid m_grid;
		private ICapletController controller;

		@Nullable private PinPlaceholderProviderForSymbolledDeviceInMove
				pinPlaceholderForSymbolledDevice;

		protected int offset;
		private Map<IAbstractSchemPin, ILocation> devicePinsLocationBeforeMove = new HashMap<>();
		private static IWritableGfxAttribute transientGfxAttr =
				FactoryMgr.getDrawFactory().constructAttribute(FactoryMgr.getDrawFactory().lookupColor("transient"));

		private static IWritableGfxAttribute gfxAttribute =
				FactoryMgr.getDrawFactory().constructAttribute(FactoryMgr.getDrawFactory().lookupColor("select"));

		private ILocation cachedLocation;
		private ILocation preAdjustedAnchorLocation;

		SelectedConnectorDynamicGfx(List<IPinList> connectorSchems, IPinList deviceOwner, ICapletController controller)
		{
			m_draggingPlugs = connectorSchems;
			owner = deviceOwner;
			matchingSchemPins = new ArrayList<>();
			associations = new ArrayList<>();
			parameterizedExtents = new ArrayList<>();
			this.controller = controller;
		}

		boolean init()
		{

			IDynamicGfxFactory dynamicGfxFactory =
					((IGfxModel) controller.getCapletModel()).getDynamicGfxService().getFactory();

			for (IPinList draggingPlug : m_draggingPlugs) {
				IDynamicGfx createdDynamicGfx =
						((IDynamicGfxMediator) draggingPlug).createDynamic(dynamicGfxFactory, null, false, true);
				IParameterized parameterized = draggingPlug.getParameterized();
				if (createdDynamicGfx == null || parameterized == null) {
					return false;
				}
				createdDynamicGfx.setAttribute(gfxAttribute);
				dynamicGfx.add(createdDynamicGfx);
				((IGfxModel) controller.getCapletModel()).getDynamicGfxService().addTransientGfx(createdDynamicGfx);
				parameterizedExtents.add(new Extent(parameterized.getExtent()));
			}

			cachedLocation = m_draggingPlugs.get(0).getAbsLocation();

			originalTransform = FactoryMgr.getDrawFactory().createTransform();
			originalTransform.setTransform(dynamicGfx.get(0).getTransform());

			offset = Math.abs(parameterizedExtents.get(0).getY());

			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			GfxView gview = (GfxView) view;
			IGriddable gridholder = (IGriddable) gview.getSheet();
			m_grid = gridholder.getGrid();
			pinPlaceholderForSymbolledDevice = getPinplaceholderForSymbol();
			if (pinPlaceholderForSymbolledDevice != null) {
				deviceExtent = ConnectionHelper
						.recalculateExtentForSymbolWhenPinplaceholderAdded(
								pinPlaceholderForSymbolledDevice, owner);
			}
			else {
				deviceExtent = ConnectionHelper.getAbsExtent(owner);
			}

			IExtent connectorExtent = ConnectionHelper.getAbsExtent(m_draggingPlugs.get(0));
			Pair<Side, Integer> sideAndBoundary =
					PinlistSideCalculator
							.getSideAndBoundary(connectorExtent, deviceExtent);

			if (sideAndBoundary == null) {
				return false;
			}
			originalConnectorSide = sideAndBoundary.getFirst();
			originalSideIndex = getSideIndex(originalConnectorSide);
			for (IPinList draggingPlug : m_draggingPlugs) {
				List<MovePinAction.ManageAssociations> assoc = draggingPlug.getAllPins().stream()
						.map(aPin -> new MovePinAction.ManageAssociations(aPin))
						.collect(Collectors.toList());
				associations.addAll(assoc);
			}

			for (IPinList draggingPlug : m_draggingPlugs) {
				Map<IAbstractSchemPin, IAbstractSchemPin> matchPinsForPlug = draggingPlug.getAllPins().stream()
						.filter(aPin -> !PinPlacementHelper.getConnectedAbstractSchemPins(aPin).isEmpty())
						.collect(Collectors.toMap(aPin -> aPin,
								aPin -> PinPlacementHelper.getConnectedAbstractSchemPins(aPin).iterator().next()));
				matchingSchemPins.add(matchPinsForPlug);
			}
			if (owner.getParameterized() != null) {
				devicePinsLocationBeforeMove = matchingSchemPins.stream().flatMap(map -> map.values().stream())
						.collect(Collectors.toMap(aPin -> aPin, aPin -> aPin.getLocation()));
			}
			matedDeviceConstraints =
					new MatedDeviceConstraints();
			return matedDeviceConstraints.init(m_draggingPlugs.get(0), owner, matchingSchemPins.get(0).values(), deviceExtent);
		}

		public void moveDynamicGfx(Point moveTo)
		{
			CAFUtils.getInstance().getStatusBar().setStatusText(ResourceMgr.getString(MoveConnectorAction.class,
					"MoveConnectorAction.ReverseDirection.text"));

			ILocation currentLocation = new Location(m_grid.snap(moveTo.x), m_grid.snap(moveTo.y));

			Side side = PinlistSideCalculator.getPointSide(deviceExtent, currentLocation);
			int deltaX = currentLocation.getX() - cachedLocation.getX();
			int deltaY = currentLocation.getY() - cachedLocation.getY();
			rotateAndMoveDynamicGraphicsAsRequired(side, deltaX, deltaY);
			cachedLocation = currentLocation;
			if (reversed) {
				reverseDynamicGraphics();
			}
			adjustDynamicGraphicsUsingOffset();
			changeColourOfDynamicGfxAsRequired();
		}

		private void adjustDynamicGraphicsUsingOffset()
		{
			ILocation currentLocation = cachedLocation;
			Side side = PinlistSideCalculator.getPointSide(deviceExtent, currentLocation);
			IExtent dynamicGfxExtent = ExtentHelper.getAbsExtent(dynamicGfx.get(0), parameterizedExtents.get(0));
			//always make sure cursor is within extent of moving connector
			if (side.isLeft() || side.isRight()) {
				if (currentLocation.getY() < dynamicGfxExtent.getBottom()) {
					for (IDynamicGfx dyGfx : dynamicGfx) {
						int y = dyGfx.getLocation().getY() - offset;
						dyGfx.getLocation().setY(y);
					}
				}
				else if (currentLocation.getY() > dynamicGfxExtent.getTop()) {
					for (IDynamicGfx dyGfx : dynamicGfx) {
						int y = dyGfx.getLocation().getY() + offset;
						dyGfx.getLocation().setY(y);
					}
				}
			}
			else {
				if (currentLocation.getX() < dynamicGfxExtent.getLeft()) {
					for (IDynamicGfx dyGfx : dynamicGfx) {
						int x = dyGfx.getLocation().getX() - offset;
						dyGfx.getLocation().setX(x);
					}
				}
				else if (currentLocation.getX() > dynamicGfxExtent.getRight()) {
					for (IDynamicGfx dyGfx : dynamicGfx) {
						int x = dyGfx.getLocation().getX() + offset;
						dyGfx.getLocation().setX(x);
					}
				}
			}
		}

		private void rotateAndMoveDynamicGraphicsAsRequired(@NotNull Side side, int deltaX, int deltaY)
		{
			ILocation anchorLoc = m_draggingPlugs.get(0).getAbsLocation();
			for (int i = 0; i < dynamicGfx.size(); i++) {
				IDynamicGfx dyGfx = dynamicGfx.get(i);
				ITransform transform = FactoryMgr.getDrawFactory().createTransform();
				transform.setTransform(m_draggingPlugs.get(i).getTransform());

				rotateOrFlipAsNeeded(side, dyGfx, transform);
				ILocation oldLocation = preAdjustedAnchorLocation == null ? dyGfx.getLocation() : preAdjustedAnchorLocation;
				ILocation currentPinlistLocation = m_draggingPlugs.get(i).getAbsLocation();
				if (i != 0) {
					TransformHelper th = TransformHelper.getTransformInfo(m_draggingPlugs.get(i).getTransform().getAffineTransform());
					Point destination = computeTransformedDelta(anchorLoc, currentPinlistLocation, transform, th);
					dyGfx.setLocation(new Location(dynamicGfx.get(0).getLocation().getX() + destination.x,
							dynamicGfx.get(0).getLocation().getY() + destination.y));
				}
				else {
					dyGfx.setLocation(new Location(oldLocation.getX() + deltaX, oldLocation.getY() + deltaY));
					preAdjustedAnchorLocation = new Location(dynamicGfx.get(0).getLocation());
				}
			}
		}

		private void rotateOrFlipAsNeeded(@NotNull Side side, IDynamicGfx dyGfx,
				ITransform transform)
		{
			int nextSideIndex = getSideIndex(side);
			ITransform rotate = FactoryMgr.getDrawFactory().createTransform();

			if (originalSideIndex != nextSideIndex) {

				if ((side.isLeft() || side.isRight()) &&
						(originalConnectorSide.isRight() || originalConnectorSide.isLeft())) {
					//flip about y-axis
					rotate.scale(-1, 1);
				}
				else if ((side.isBottom() || side.isTop()) &&
						(originalConnectorSide.isTop() || originalConnectorSide.isBottom())) {
					//flip about x-axis
					rotate.scale(1, -1);
				}

				else {
					rotate.rotate(Math.toRadians((nextSideIndex - originalSideIndex) * MINUS_NINETY));
				}

				transform.preConcatenate(rotate);
			}
			dyGfx.setTransform(transform);
		}

		@NotNull private Point computeTransformedDelta(@NotNull ILocation anchorLoc,
				@NotNull ILocation currentPinlistLocation, @NotNull ITransform transform, @NotNull TransformHelper th)
		{
			Point destination = new Point();
			if (originalConnectorSide == Side.LEFT || originalConnectorSide == Side.RIGHT ) {
				if (th.getRotation() == 0) {
					transform.getAffineTransform().transform(new Point(0,
						currentPinlistLocation.getY() - anchorLoc.getY()), destination);
				}
				else {
					transform.getAffineTransform().transform(new Point(0,
							anchorLoc.getY() - currentPinlistLocation.getY()), destination);
				}
			}
			else if (originalConnectorSide == Side.BOTTOM || originalConnectorSide == Side.TOP) {
				if (th.getRotation() == NINETY) {
					transform.getAffineTransform().transform(new Point(0,
							currentPinlistLocation.getX() - anchorLoc.getX()), destination);
				}
				else {
					transform.getAffineTransform().transform(new Point(0,
							anchorLoc.getX() - currentPinlistLocation.getX()), destination);
				}
			}
			return destination;
		}

		private void changeColourOfDynamicGfxAsRequired()
		{
			ILocation currentLocation = cachedLocation;
			Side side = PinlistSideCalculator.getPointSide(deviceExtent, currentLocation);
			boolean withInExtent = isWithinExtent(side);

			if (isCursorWithinExtent(deviceExtent, currentLocation) &&
					PinlistSideCalculator.getBoundarySide(deviceExtent, currentLocation) != null) {
				((IGfxModel) controller.getCapletModel()).getDynamicGfxService().addTransientGfx(dynamicGfx.get(0));

				if (!withInExtent || isRestrictedForAnyConnector(side)) {

					for (IDynamicGfx dyGfx : dynamicGfx) {
						dyGfx.setAttribute(transientGfxAttr);
					}
				}
				else {

					for (IDynamicGfx dyGfx : dynamicGfx) {
						dyGfx.setAttribute(gfxAttribute);
					}
				}
			}
			else {
				for (IDynamicGfx dyGfx : dynamicGfx) {
					dyGfx.setAttribute(transientGfxAttr);
				}
			}
		}

		protected void reverseDynamicGraphics()
		{
			Side side = PinlistSideCalculator.getPointSide(deviceExtent, dynamicGfx.get(0).getLocation());
			ILocation anchorLoc = dynamicGfx.get(0).getLocation();
			for (int i = 0; i < dynamicGfx.size(); i++) {
				IDynamicGfx dyGfx = dynamicGfx.get(i);
				ITransform transform = FactoryMgr.getDrawFactory().createTransform();
				transform.setTransform(dyGfx.getTransform());
				ITransform rotate = FactoryMgr.getDrawFactory().createTransform();
				if (side == Side.LEFT || side == Side.RIGHT) {
					if(i != 0) {
						dyGfx.setLocation(new Location(dyGfx.getLocation().getX(),
								2 * anchorLoc.getY() - dyGfx.getLocation().getY()));
					}
					rotate.scale(1, -1);
				}
				else {
					if (i != 0) {
						dyGfx.setLocation(new Location(2 * anchorLoc.getX() - dyGfx.getLocation().getX(),
								dyGfx.getLocation().getY()));
					}
					rotate.scale(-1, 1);
				}
				transform.preConcatenate(rotate);
				dyGfx.setTransform(transform);
			}
		}

		private boolean isRestrictedForAnyConnector(Side side)
		{
			for (int i = 0; i < dynamicGfx.size(); i++) {
				IDynamicGfx gfx = dynamicGfx.get(i);
				IExtent dynamicGfxExtent = ExtentHelper.getAbsExtent(gfx, new Extent(parameterizedExtents.get(i)));
				if (matedDeviceConstraints.isRestricted(dynamicGfxExtent, side)) {
					return true;
				}
			}
			return false;
		}

		protected boolean isCursorWithinExtent(IExtent extent, ILocation point)
		{

			return point.getY() <= extent.getTop() && point.getY() >= extent.getBottom() &&
					point.getX() >= extent.getLeft() && point.getX() <= extent.getRight();
		}

		private boolean isWithinExtent(Side side)
		{
			for (int i = 0; i < dynamicGfx.size(); i++) {
				IExtent dynamicGfxExtent = ExtentHelper.getAbsExtent(dynamicGfx.get(i), new Extent(parameterizedExtents.get(i)));
				boolean withInExtent = false;
				if (side.isLeft() || side.isRight()) {
					int connTop = dynamicGfxExtent.getTop();
					int connBottom = dynamicGfxExtent.getBottom();
					if (connTop < deviceExtent.getTop() && connBottom > deviceExtent.getBottom()) {
						withInExtent = true;
					}
				}
				else if (side.isTop() || side.isBottom()) {
					int connLeft = dynamicGfxExtent.getLeft();
					int connRight = dynamicGfxExtent.getRight();
					if (connRight < deviceExtent.getRight() && connLeft > deviceExtent.getLeft()) {
						withInExtent = true;
					}
				}
				if (!withInExtent) {
					return false;
				}
			}
			return true;
		}

		@Nullable private Pair<Side, Integer> getSideAndBoundaryUsingDynmaicGfx()
		{
			IExtent dynamicGfxExtent = ExtentHelper.getAbsExtent(dynamicGfx.get(0), parameterizedExtents.get(0));
			return PinlistSideCalculator.getSideAndBoundary(dynamicGfxExtent, deviceExtent);
		}

		@Nullable private Pair<Side, Integer> applyToPinlist()
		{
			Pair<Side, Integer> sideAndBoundary = getSideAndBoundaryUsingDynmaicGfx();
			if (sideAndBoundary != null) {

				if (isWithinExtent(sideAndBoundary.getFirst())) {
					for (int i = 0; i < m_draggingPlugs.size(); i++) {
						IPinList draggingPlug = m_draggingPlugs.get(i);
						((IDynamicGfxMediator) draggingPlug)
								.applyEdits(controller.getCapletModel(), dynamicGfx.get(i), false, null);
					}

					return PinlistSideCalculator
							.getSideAndBoundary(ConnectionHelper.getAbsExtent(m_draggingPlugs.get(0)), deviceExtent);
				}
			}
			return null;
		}

		@Nullable
		private Map<IAbstractSchemPin, ILocation> populateDevicePinLocations()
		{
			Map<IAbstractSchemPin, ILocation> devicePinsLocation = new HashMap<>();
			for (int i = 0; i < m_draggingPlugs.size(); i++) {
				IPinList draggingPlug = m_draggingPlugs.get(i);
				PinListConnectionHelper pinListConnectionHelper =
						ConnectionHelper.createInstance(draggingPlug, owner,
								new Consumer<PinListConnectionHelper>()
								{
									@Override public void accept(PinListConnectionHelper pinListConnectionHelper)
									{
										if (pinListConnectionHelper instanceof BaseDeviceConnectionHelper) {
											pinPlaceholderForSymbolledDevice = getPinplaceholderForSymbol();
											if (pinPlaceholderForSymbolledDevice != null) {
												((BaseDeviceConnectionHelper) pinListConnectionHelper)
														.setPinPlaceHolderForSymbol(pinPlaceholderForSymbolledDevice);
											}
										}
									}
								});
				if (pinListConnectionHelper == null) {
					return null;
				}


				for (Map.Entry<IAbstractSchemPin, IAbstractSchemPin> pinPairs : matchingSchemPins.get(i).entrySet()) {
					ILocation matchingGfx = pinListConnectionHelper.getMatchingLocation(pinPairs.getKey(), false);
					if (matchingGfx == null) {
						return null;
					}
					devicePinsLocation.put(pinPairs.getValue(),
							new Location(matchingGfx.getX(), matchingGfx.getY()));
				}
			}
			return devicePinsLocation;
		}

		private boolean moveDevicePins(Map<IAbstractSchemPin, ILocation> devicePinLocations)
		{

			for (Map.Entry<IAbstractSchemPin, ILocation> devicePinAndLocation : devicePinLocations.entrySet()) {
				IAbstractSchemPin aPin = devicePinAndLocation.getKey();
				aPin.setLocation(devicePinAndLocation.getValue());
				PinNameTextJustificationHandler.justifyPinNameText(aPin);
				if (aPin instanceof IStyleableObject) {
					((IStyleableObject) aPin).applyStyle();
				}
			}
			if (devicePinsLocationBeforeMove != null) {
				Set<String> pinLocations = owner.getAllPins().stream()
						.map(aPin -> aPin.getLocation().getX() + "," + aPin.getLocation().getY())
						.collect(Collectors.toSet());

				for (ILocation location : devicePinsLocationBeforeMove.values()) {
					if (!pinLocations.contains(location.getX() + "," + location.getY())) {
						IPinPlaceholder ph =
								FactoryMgr.getSchemFactory().createPinPlaceholder(location.getX(), location.getY());
						owner.addObject(ph);
					}
				}
				List<IPinPlaceholder> pinPlaceholdersToDelete = owner.getObjects(IPinPlaceholder.class).stream()
						.filter(aPh -> pinLocations.contains(aPh.getLocation().getX() + "," + aPh.getLocation().getY()))
						.collect(Collectors.toList());
				pinPlaceholdersToDelete.forEach(aPh -> owner.removeObject(aPh));
			}
			return true;
		}

		private boolean transferAssociations()
		{
			associations.forEach(anAssociation -> anAssociation.updateConnection());
			return true;
		}

		private int getSideIndex(Side side)
		{

			if (side.isBottom()) {
				return 1;
			}
			if (side.isLeft()) {
				return 2;
			}
			if (side.isTop()) {
				return 3;
			}
			return 0; //default and for right return 0;
		}

		public boolean isRestricted()
		{
			for (int i = 0; i < dynamicGfx.size(); i++) {
				Side side = PinlistSideCalculator.getPointSide(deviceExtent, dynamicGfx.get(i).getLocation());
				IExtent dynamicGfxExtent = ExtentHelper.getAbsExtent(dynamicGfx.get(i), parameterizedExtents.get(i));
				if (matedDeviceConstraints.isRestricted(dynamicGfxExtent, side)) {
					return true;
				}
			}
			return false;
		}

		@Nullable private PinPlaceholderProviderForSymbolledDeviceInMove getPinplaceholderForSymbol()
		{
			if (owner.getParameterized() != null) {
				return null;
			}
			if (pinPlaceholderForSymbolledDevice == null) {
				GeneratorParameters gp = DiagramHelper.createGeneratorParameters(owner.getDiagram());
				pinPlaceholderForSymbolledDevice =
						new PinPlaceholderProviderForSymbolledDeviceInMove(owner, m_grid, gp);
				pinPlaceholderForSymbolledDevice
						.createTempPlaceHolderForDevicesWithSymbols();
			}
			return pinPlaceholderForSymbolledDevice;
		}

		public IPinList getOwner()
		{
			return owner;
		}

		public void toggleReversed()
		{
			reversed = !reversed;
		}
	}

	public MoveConnectorAction(ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return MoveConnectorActionUI.class.getName();
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		OperandData operandData = getOperands();
		if (operandData == null) {
			return IActionEnum.eCanceled;
		}

		if (operandData.getConnectors() == null) {
			String message = ResourceMgr
					.getString(MoveConnectorAction.class, "MoveConnectorAction.connectorsOnDifferentSides.text");
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
			return IActionEnum.eCanceled;
		}
		selectedConnectorParams =
				new SelectedConnectorDynamicGfx(operandData.getConnectors(), operandData.getDevice(), getController());
		if (!selectedConnectorParams.init()) {
			String msg = ResourceMgr
					.getString(MoveConnectorAction.class, "MoveConnectorAction.cannotMoveConnector.text");
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
			cleanup();
			return IActionEnum.eCanceled;
		}
		return IActionEnum.eActivated;
	}

	@Override protected boolean onTerminate(boolean successful)
	{

		try {
			((IGfxModel) getController().getCapletModel()).getDynamicGfxService().removeAllTransientGfx();
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view != null) {
				view.invalidate(IViewInvalidationEnum.eTransient);
			}
			CAFUtils.getInstance().getStatusBar().setStatusText("");
			if (successful && selectedConnectorParams != null) {
				try (ConnectionHelper.NoConnectivityEditGuard noConnEdut = new ConnectionHelper.NoConnectivityEditGuard()) {

					if (!selectedConnectorParams.isRestricted()) {

						Pair<Side, Integer> sideAndBoundaryAfterConnMove = selectedConnectorParams.applyToPinlist();
						if (sideAndBoundaryAfterConnMove != null) {

							Map<IAbstractSchemPin, ILocation> devicePinLocations =
									selectedConnectorParams.populateDevicePinLocations();
							if (devicePinLocations != null &&
									selectedConnectorParams.moveDevicePins(devicePinLocations)) {

								if (selectedConnectorParams.transferAssociations()) {
									IPinList deviceOwner = selectedConnectorParams.getOwner();
									ISchemDiagram diagram = DiagramHelper.getDiagram(deviceOwner);
									Generator generator = Generator.getGenerator();
									GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
									generator.generate(
											PinListGenerationParams.getInstance(
													deviceOwner, gp, Generator.NOREGENERATE_PROPERTIES, true));

									diagram.refreshRepresentations();
									return true;
								}
							}
						}
					}
				}
			}
		}
		finally {
			cleanup();
		}
		return false;
	}

	@Override public void keyTyped(KeyEvent e)
	{

	}

	@Override public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_R) {
			if (selectedConnectorParams != null) {
				selectedConnectorParams.toggleReversed();
				selectedConnectorParams.reverseDynamicGraphics();
				selectedConnectorParams.adjustDynamicGraphicsUsingOffset();
				selectedConnectorParams.changeColourOfDynamicGfxAsRequired();
				e.consume();
				CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
			}
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{

	}

	protected void cleanup()
	{
		selectedConnectorParams = null;
	}

	@Override public boolean isEnabled()
	{
		if (getOperands() != null) {
			return super.isEnabled();
		}
		return false;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action action = getActionUI();
		if (getOperands(selections) != null && action != null) {
			container.add(new ActionEntry(action));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		getController().getActionMgr().terminateActiveAction(true);
	}

	@Override public void mousePressed(MouseEvent e)
	{

	}

	@Override public void mouseReleased(MouseEvent e)
	{

	}

	@Override public void mouseEntered(MouseEvent e)
	{

	}

	@Override public void mouseExited(MouseEvent e)
	{

	}

	@Override public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		if (selectedConnectorParams != null) {
			Point point = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
			selectedConnectorParams.moveDynamicGfx(point);

			CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	@Nullable protected OperandData getOperands()
	{
		return getOperands(getController().getSelectMgr().getPreSelections());
	}

	@Nullable protected OperandData getOperands(SelectSet inputSet)
	{
		//when selected from browser tree both connectivity and schem objects will be in selection.
		Set<IPlugConnector> plugConnectors =
				inputSet.getSelectedUIDS()
						.stream()
						.map(ReferenceHelper::reduceToConnectivityObject)
						.filter(connectivityObject -> connectivityObject instanceof IPlugConnector &&
								connectivityObject instanceof IDeviceOwned &&
								((IDeviceOwned) connectivityObject).getOwner() !=
										null)
						.map(aPlugConn -> (IPlugConnector) aPlugConn)
						.limit(2)
						.collect(Collectors.toSet());

		List<IPinList> schemObjectsSelected = inputSet.getSelectedObjects(IPinList.class);
		if (schemObjectsSelected.size() < 1 || plugConnectors.size() < 1) {
			return null;
		}
		for (SelectedUIDObjectIterator iter = inputSet.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!isSchemOrCablePlugConnector(obj)) {
				return null;
			}
		}
		Set<IPinList> attachedDevices = new HashSet<>();
		for (IPinList schemObjectSelected : schemObjectsSelected) {
			List<IPinList> attDevices = schemObjectSelected.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR).stream()
					.filter(aObj -> aObj.getConnectivity() instanceof IDevice).collect(
							Collectors.toList());
			attachedDevices.addAll(attDevices);
		}
		if (attachedDevices.size() != 1) {
			return null ; // connectors connected to diff devices are selected
		}
		Side connectorSide = areMovingConnectorsOnSameSide(schemObjectsSelected, attachedDevices.iterator().next());
		if (connectorSide == null) {
			return new OperandData(null, attachedDevices.iterator().next());
		}

		for (IPinList draggingPinlist : schemObjectsSelected) {
			boolean doesDraggingPinlistHavePins = draggingPinlist.getAllPins(false).stream()
					.anyMatch(pin -> !(pin instanceof IPin &&
							((IConnectivityRef) pin).getConnectivity() instanceof IBackshellTermination));

			if (!doesDraggingPinlistHavePins) {
				return null;
			}

			List<IPinList> attachedDevice = draggingPinlist.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR).stream()
					.filter(aObj -> aObj.getConnectivity() instanceof IDevice).collect(
							Collectors.toList());
			if(attachedDevice.size() == 1){
				Collection<IPinList> parent = draggingPinlist.getAttachedPinListObjects(IPinList.ONLY_MODULAR_PARENT);
				Collection<IPinList> children = draggingPinlist.getAttachedPinListObjects(IPinList.ONLY_MODULAR_CHILDREN);
				if(!children.isEmpty() || !parent.isEmpty()){
					return null;
				}
			}
		}

		List<IPinList> sortedSchemPinlists = schemObjectsSelected.stream().sorted((a, b) -> Integer
				.compare(getCoOrdOfInterest(a.getLocation(), connectorSide),
						getCoOrdOfInterest(b.getLocation(), connectorSide)))
				.collect(Collectors.toList());

		return attachedDevices.size() == 1 ? new OperandData(sortedSchemPinlists, attachedDevices.iterator().next()) : null;
	}

	private boolean isSchemOrCablePlugConnector(@NotNull IUIDObject obj)
	{
		return obj instanceof IPlugConnector ||
				(obj instanceof IPinList && ((IConnectivityRef) obj).getConnectivity() instanceof IPlugConnector);
	}

	private int getCoOrdOfInterest(ILocation location, Side connectorSide) {
		if (connectorSide == Side.LEFT || connectorSide == Side.RIGHT) {
			return location.getY();
		}
		else {
			return location.getX();
		}
	}

	@Nullable private Side areMovingConnectorsOnSameSide(List<IPinList> movingPinlists, IPinList schemDevice)
	{
		IExtent deviceExtent = ConnectionHelper.getAbsExtent(schemDevice);
		Side pinSide = null;
		for (IPinList movingPinlist : movingPinlists) {
			IExtent attachedConnExtent = ConnectionHelper.getAbsExtent(movingPinlist);
			Pair<Side, Integer> sideAndBoundary =
					PinlistSideCalculator.getSideAndBoundary(attachedConnExtent, deviceExtent);
			if (sideAndBoundary == null) {
				return null;
			}
			Side currentSide = sideAndBoundary.getFirst();
			if (pinSide != null) {
				if (!pinSide.equals(currentSide)) {
					return null;
				}
			}
			else {
				pinSide = currentSide;
			}
		}
		return pinSide;
	}

	private static class OperandData
	{

		@Nullable public List<IPinList> getConnectors()
		{
			return connectors;
		}

		@NotNull public IPinList getDevice()
		{
			return device;
		}

		@Nullable private List<IPinList> connectors;
		@NotNull private IPinList device;

		OperandData(@Nullable List<IPinList> connectors, @NotNull IPinList device)
		{
			this.connectors = connectors;
			this.device = device;
		}
	}
}
