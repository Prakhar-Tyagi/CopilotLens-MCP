/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.shared.actions.AbstractMovePinAction;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.IRectangle;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceLikePinlist;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.HarnessConnectorsGenerator;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.Extent;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Side;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.services.dynamicgfx.DynamicGfxFactoryHelper;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IResizableDynamicCompound;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.IXMLTags;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.ResizeHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static chs.cof.logical.cable.HarnessConnectorGenerationEnum.TypeAutomaticallyGenerated;

/**
 * Class MovePinAction: Responsible for moving pins:   This class will handle:
 * <p>
 * a/ moving mated/non mated pins for devices with symbols. Rather than creating custom code we will treat a device with
 * a symbol as if it was parameterized. This is done by creating temporary placeholders on the symbol boundary. However
 * we must ensure these temp placeholders are deleted before we edit the data model, otherwise they will snapshotted and
 * will appear after undo.
 * <p>
 * b/ Move mated/non mated pins within the same device. Single PinPlacementConstraint created
 * <p>
 * c/ Move mated/non mated pin on a connector and across connectors. (i.e when the connectors have no library parts). To
 * support move across to different connectors we will create an PinPlacementConstraints per Connector. Additionally we
 * need to guard against creating duplicate pin name on the connector. Hence we will automatically generate unquie
 * names
 * <p>
 * The green circles indicate valid position and the blue circle indicate the matching pins. This is especially useful
 * for showing the possible mated pin locations.
 * <p>
 * As a debug aid, we will also assert if we place  Pins or placeholders on top of each other. A clear sign that move
 * has screwed up somewhere. Please enable in PinPlacementConstraints as well.
 * <p>
 * Collaborations:
 * <p>
 * PinPlacementConstraints. This provides the allowable pin positions and boundary extenension creation.
 * <p>
 * Generator: This provides way to recreate the pinlist extent once a pin has been moved.
 * <p>
 * ConnectionHelper: Provide cability to connect pins and find match pins/placeholders.
 */

public class MovePinAction extends AbstractMovePinAction
{

	private static final boolean DEBUG_PIN_POSITION = false;

	// used in prompting for pin names
	private Map<IPin, String> m_newConnPinNames;

	private ListMap<IAbstractSchemPin, IAbstractSchemPin> m_pinMateMap;

	//this flag is set when the mouse is moved after activating the action
	private boolean didMouseMove;
	private Map<IAbstractSchemPin, ManageAssociations> associationsMap;

	public MovePinAction(ICapletController controller)
	{
		this(controller, null, null);
	}

	private void init()
	{

		// used in prompting for pin names
		m_newConnPinNames = new HashMap<IPin, String>();
		//The map will be filled with each moving pin along with its mate and any pin that will be stopmed by the moving pin
		m_pinMateMap = new ListMap<IAbstractSchemPin, IAbstractSchemPin>();
	}

	public MovePinAction(ICapletController controller, @Nullable IPinList destPinList, @Nullable Point destPoint)
	{
		super(controller, destPinList, destPoint);
		init();
	}

	public String getActionUIClass()
	{
		return MovePinActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		didMouseMove = false;
		IActionEnum ae = super.onActivate(e);

		// this actions lifetime is that of the caplet, ensure m_constraints is empty before we start
		m_constraintsHolders.clear();
		m_newConnPinNames.clear();
		m_pinMateMap.clear();

		if (ae == IActionEnum.eActivated && m_pins != null && m_pins.length > 0) {

			m_genParams = DiagramHelper.createGeneratorParameters(m_pins[0]);

			assert m_destPinList != null;
			addTempPlaceHolderForDevicesWithSymbols(m_destPinList);

			//will store each pin along with its side before the movement for the pinlist and any attached connector
			// used for text alignement after the move
			IPinList prevAttachedDevice = null;
			for (IPinList candidate : m_destPinList.getCandidates()) {
				cacheAllPinSides(candidate);
				if (!areMovingPinsOnSameSide(candidate)) {
					String message = ResourceMgr
							.getString(AbstractMovePinAction.class, "MovePinAction.PinsNotOnSameSide.Error",
									getNameForTooltip());
					showErrorMessage(message);
					return IActionEnum.eCanceled;
				}
				IPinList theAttachedDevice = PinPlacementHelper.getAttachedDevice(candidate);
				if (prevAttachedDevice == null) {
					prevAttachedDevice = theAttachedDevice;
				}
				if (prevAttachedDevice != null && theAttachedDevice != null &&
						prevAttachedDevice != theAttachedDevice) {
					String message = ResourceMgr
							.getString(AbstractMovePinAction.class, "MovePinAction.AttachedToMultipleDevices.Error");
					showErrorMessage(message);
					return IActionEnum.eCanceled;
				}
			}

			handlePinPlacementConstraints(m_destPinList);

			dumpPinListPositions(m_pins);
			return IActionEnum.eActivated;
		}
		return IActionEnum.eActivated;
	}

	private void showErrorMessage(@NotNull String message)
	{
		IOutputWindow win = CAFUtils.getInstance().getOutputWindow();
		win.sendApplicationMessage(HTMLHelper.color(IXMLTags.RED, message));
	}

	private void dumpPinListPositions(@NotNull IAbstractSchemPin... pins)
	{
		Set<IPinList> candidates = new LinkedHashSet<>();
		for (IAbstractSchemPin pin : pins) {
			candidates.add((IPinList) pin.getParent());
		}
		for (IPinList candidate : candidates) {
			dumpPinListPositions(candidate);
		}
	}

	private boolean areMovingPinsOnSameSide(IPinList parent)
	{
		IExtent extent = ConnectionHelper.getAbsExtent(parent);
		Side pinSide = null;
		for (IAbstractSchemPin aPin : m_pins) {
			ILocation location = aPin.getAbsLocation();
			Side currentSide = getPointSide(extent, new Point(location.getX(), location.getY()));
			if (pinSide != null) {
				if (!pinSide.equals(currentSide)) {
					return false;
				}
			}
			else {
				pinSide = currentSide;
			}
		}
		return true;
	}

	public void mouseReleased(MouseEvent e)
	{
		if (m_dragging) {
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		//we need to check that the mouse was moved after activating the action
		//as the action could be started from LogicSelecthelper after a long click
		//and mouseClicked is called right after activating the action causing the action to terminate
		if (didMouseMove) {
			didMouseMove = false;
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		didMouseMove = true;
		super.mouseMoved(e);
	}

	public static class ManageAssociations
	{

		private Map<ILogicObject, Stream<IDiagramObject>> existingConnectedPinJoints;
		private Map<ILogicObject, IJoint> connectedPinsLocation;
		@Nullable private Stream<IDiagramObject> thisPinJointContents;
		@Nullable private IJoint thisPinJoint;
		private IAbstractSchemPin schemPin;

		public ManageAssociations(IAbstractSchemPin schemPin)
		{
			this.schemPin = schemPin;
			Collection<IAbstractSchemPin> connPins = PinPlacementHelper.getConnectedAbstractSchemPins(schemPin);
			connectedPinsLocation = connPins.stream().filter(aPin -> aPin.getJoint() != null)
					.collect(Collectors.toMap(aPin -> getSchemPinRepId(aPin), pin -> pin.getJoint()));
			existingConnectedPinJoints = connPins.stream().filter(aPin -> aPin.getJoint() != null)
					.collect(Collectors.toMap(aPin -> getSchemPinRepId(aPin),
							pin -> pin.getJoint().getAssociations().stream()
									.filter(iDiagramObject -> !(iDiagramObject instanceof IAbstractSchemPin))));
			thisPinJoint = schemPin.getJoint();

			thisPinJointContents = thisPinJoint != null ? thisPinJoint.getAssociations().stream()
					.filter(iDiagramObject -> !(iDiagramObject instanceof IAbstractSchemPin)) : null;
		}

		@Nullable private ILogicObject getSchemPinRepId(IAbstractSchemPin aPin)
		{
			IDiagramObject pinParent = aPin.getParent();
			if (pinParent instanceof IConnectivityRef) {
				return ((IConnectivityRef) pinParent).getConnectivity();
			}

			return null;
		}

		public void updateConnection()
		{
			Collection<IAbstractSchemPin> connPins = PinPlacementHelper.getConnectedAbstractSchemPins(schemPin);
			connPins.stream().forEach(aPin -> {
				ILogicObject cPin = getSchemPinRepId(aPin);
				Stream<IDiagramObject> jointDiagramObjects = existingConnectedPinJoints.get(cPin);

				if (jointDiagramObjects != null) {

					Set<ILogicSegment> segments =
							jointDiagramObjects.filter(iDiagramObject -> iDiagramObject instanceof ILogicSegment)
									.map(iDiagramObject -> (ILogicSegment) iDiagramObject).collect(Collectors.toSet());

					IJoint currentJoint = aPin.getJoint();
					if (currentJoint != null) {
						currentJoint.removeAssociation(aPin);
						aPin.setJoint(null);
					}
					HarnessConnectorsGenerator
							.reConnectSegmentsWithNewSchmPin(aPin, connectedPinsLocation.get(cPin),
									segments);
				}
			});
			if (thisPinJointContents != null) {
				Set<ILogicSegment> segments =
						thisPinJointContents.filter(iDiagramObject -> iDiagramObject instanceof ILogicSegment)
								.map(iDiagramObject -> (ILogicSegment) iDiagramObject).collect(Collectors.toSet());
				if (!segments.isEmpty() && thisPinJoint != null) {

					HarnessConnectorsGenerator
							.reConnectSegmentsWithNewSchmPin(schemPin,
									thisPinJoint,
									segments);
				}
			}
		}
	}

	public boolean onTerminate(boolean successful)
	{
		try (ConnectionHelper.NoConnectivityEditGuard ignored = new ConnectionHelper.NoConnectivityEditGuard()) {
			//cache pin sides for the destination pin list to use it for text alignment
			assert m_destPinList != null;
			if (needToCacheDestPinListPinSides()) {
				for (IPinList candidate : m_destPinList.getCandidates()) {
					cacheAllPinSides(candidate);
				}
			}
			// if we have a valid pin move, edit the data model
			boolean bEditOk = true;

			if (successful && m_selectedPoint != null &&
					((validPoints(m_selectedPoint, null) & PinPlacementConstraintsHolder.PLACEMENT_NO) == 0)) {
				bEditOk = editModel();
			}

			// Get rid of our transient graphics, this uses m_constraints, so dont clear it just yet.
			removeTransientGfx();

			// must clear as MovePinAction lifetime is that of caplet, Must clear here to handle multiple pin case
			m_constraintsHolders.clear();
			m_newConnPinNames.clear();
			m_pinMateMap.clear();

			// if the action is unsucessful then m_pins can be null, or even what it points to can be null
			if (m_pins != null && m_pins.length > 0 && m_pins[0].getParent() != null) {
				removeTempPlaceHoldersForDevicesWithSymbols(m_destPinList);
			}

			m_destPinList = null;

			return bEditOk;
		}
	}

	private boolean needToCacheDestPinListPinSides()
	{
		IPinList movedPinsParent = (IPinList) m_pins[0].getParent();
		assert movedPinsParent != null;
		assert m_destPinList != null;
		IPinList destPLAnchor = m_destPinList.getAnchor();
		IPinList srcPLAnchor = new ModularSchemPinListInfo(movedPinsParent).getAnchor();
		if (srcPLAnchor == destPLAnchor) {
			return false;
		}
		chs.cof.logical.cable.IPinList sourcePL = srcPLAnchor.getConnectivity();
		chs.cof.logical.cable.IPinList targetPL = destPLAnchor.getConnectivity();
		if (sourcePL instanceof IDeviceOwnedConnector && targetPL instanceof IDeviceOwnedConnector) {
			//if source and destination pinlist are connectors attached to the same device
			IBaseDevice sourceOwnerOwner = ((IDeviceOwned) sourcePL).getOwner();
			IBaseDevice targetOwnerOwner = ((IDeviceOwned) targetPL).getOwner();
			if (sourceOwnerOwner != null && targetOwnerOwner != null) {
				IPinList srcAttachedPinList = PinPlacementHelper.getAttachedDevice(sourceOwnerOwner, srcPLAnchor);
				IPinList destAttachedPinList = PinPlacementHelper.getAttachedDevice(targetOwnerOwner, destPLAnchor);
				return destAttachedPinList == null || srcAttachedPinList != destAttachedPinList;
			}
		}
		return true;
	}

	protected boolean editModel()
	{
		if (!lockSPLs()) {
			return false;
		}
		try {
			LogicUtils.deferRegenerationOfSchemDeviceConnectors();
			// For devices with symbols, when moving connector pins to a different connector, create the dest _matching_
			// pin locations, before destroying the temp place holders. Since we created placeholders for devices with
			// symbol, take advantage of this to compute the connectedPinsLocation of matching pin on the device, corresponding to the
			// moving connector pin.
			Map<IGfxObject, ILocation> matchMap = createDestinationPinLocations();

			// **** Note well, this must be done before we make any data model changes below, otherwise the temporary place
			// **** holders will get snapshotted and added on undo's. so we will end up placeholder for devices with symbols.
			// **** this is not immediatley obvious. Need to add a validation for this.
			assert m_destPinList != null;
			removeTempPlaceHoldersForDevicesWithSymbols(m_destPinList);

			Map<IAbstractSchemPin, ILocation> oldLocations = new HashMap<IAbstractSchemPin, ILocation>(m_pins.length);
			associationsMap =
					Arrays.stream(m_pins).collect(Collectors.toMap(aPin -> aPin, aPin -> new ManageAssociations(aPin)));
			IPinList destPinListAnchor = m_destPinList.getAnchor();
			IExtent destExtent = ConnectionHelper.getAbsExtent(destPinListAnchor);
			assert m_selectedPoint != null;
			Side side = getPointSide(destExtent, m_selectedPoint);
			MovePinHandler movePinHandler = new MovePinHandler(m_grid.getGridSpacing(), false, side);

			final ILogicDesign design = destPinListAnchor.getDiagram().getDesign();
			assert design != null;
			if (design.isUnderConcurrentEdit()) {
				if (!lockObjects(design, movePinHandler)) {
					return false;
				}
			}
			Set<IAbstractSchemPin> modifiedPins = new LinkedHashSet<IAbstractSchemPin>();
			boolean actuallyDidSomething = movePins(matchMap, oldLocations, modifiedPins, movePinHandler, side);

			boolean backShellMoved = false;
			for (IAbstractSchemPin pin : modifiedPins) {
				if (!backShellMoved && pin instanceof IConnectivityRef) {
					backShellMoved = IBackshellTermination.class
							.isAssignableFrom(((IConnectivityRef) pin).getConnectivity().getClass());
				}
			}
			if (backShellMoved) {
				final Iterator<IPinList> attachedPinlistIterator =
						destPinListAnchor.getAttachedPinListObjects().iterator();
				if (attachedPinlistIterator.hasNext()) {
					ResizeHelper
							.resizeInlineForBackshellTerminations(attachedPinlistIterator.next(), destPinListAnchor);
				}
			}

			return actuallyDidSomething;
		}
		finally {
			unlockSPLs();
		}
	}

	private boolean lockObjects(ILogicDesign design, MovePinHandler movePinHandler)
	{
		assert m_destPinList != null;
		Collection<ILogicObject> toBeLocked = new HashSet<>();
		for (IPinList candidate : m_destPinList.getCandidates()) {
			for (int i = 0; i < m_pins.length; i++) {
				calculateTempPoint(i);
				toBeLocked.addAll(movePinHandler.getLockables(m_pins[i], m_tmpPoint.x, m_tmpPoint.y, candidate));
			}
		}
		if (!toBeLocked.isEmpty()) {
			final Set<IUID> lockFailures = LogicObjectLockFinder.tryEdit(design, toBeLocked);
			if (!lockFailures.isEmpty()) {
				LogicConcurrencyLogger.getInstance().reportLockFailure(design, getActionTitle(), lockFailures,
						message -> reportLockFailures(message));
				return false;
			}
		}
		return true;
	}

	private String getActionTitle()
	{
		Action actionUI = getActionUI();
		assert actionUI != null;
		return (String) actionUI.getValue(Action.NAME);
	}

	private void reportLockFailures(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}

	@SuppressWarnings("OverlyLongMethod")
	private boolean movePins(Map<IGfxObject, ILocation> matchMap, Map<IAbstractSchemPin, ILocation> oldLocations,
			Set<IAbstractSchemPin> modifiedPins, MovePinHandler movePinHandler, Side side)
	{
		IPinList originalParent = (IPinList) m_pins[0].getParent();
		assert originalParent != null;
		ISchemDiagram diagram = originalParent.getDiagram();
		ILocation pinLocation = m_pins[0].getAbsLocation();
		calculateTempPoint(0);
		int dir = 1;
		int startIndex = 0;
		Predicate<Integer> predicate = i -> i < m_pins.length;
		if (side.isLeft() || side.isRight()) {
			if (m_tmpPoint.y > pinLocation.getY()) {
				dir = -1;
				startIndex = m_pins.length - 1;
				predicate = i -> (i >= 0);
			}
		}
		else {
			if (m_tmpPoint.x > pinLocation.getX()) {
				dir = -1;
				startIndex = m_pins.length - 1;
				predicate = i -> (i >= 0);
			}
		}
		Map<Integer, Integer> freeLocations = createOverlappingOldAndNewIndices();

		assert m_destPinList != null;
		Collection<IAbstractSchemPin> schemPinssToDelete = new ArrayList<>();
		boolean actuallyDidSomething = false;

		Set<IAbstractSchemPin> processedDevicePin = new HashSet<>();
		Collection<IAbstractSchemPin> schemPinsToSwap = new ArrayList<>();
		IPinList anchor = m_destPinList.getAnchor();

		//must collect the original parents before pin-movements.
		Set<IPinList> originalPinParents = new HashSet<>();
		for (IAbstractSchemPin pin : m_pins) {
			IPinList parent = (IPinList) pin.getParent();
			assert parent != null;
			originalPinParents.add(parent);
		}

		for (int i = startIndex; predicate.test(i); i += dir) {
			processedDevicePin.clear();
			if (freeLocations.get(i) != null && freeLocations.get(i) == i) {
				//pin moving to it's own location
				continue;
			}
			calculateTempPoint(i);
			IAbstractSchemPin schemPin = m_pins[i];
			oldLocations.put(schemPin, FactoryMgr.getCommonFactory().constructLocation(schemPin.getAbsLocation()));
			actuallyDidSomething = true;

			Collection<IAbstractSchemPin> connPins = PinPlacementHelper.getConnectedAbstractSchemPins(schemPin);
			schemPinssToDelete.addAll(connPins);

			int abs_x = m_tmpPoint.x;
			int abs_y = m_tmpPoint.y;
			IAbstractSchemPin pinToSwapWith = MovePinHandler.getPinToSwapWith(anchor, abs_x, abs_y);
			MovePinHandler.SwapInfo swapInfo;
			if (pinToSwapWith != null) {

				int freeIndex = getFreeIndex(freeLocations, i);

				ILocation swapLocation = m_pins[freeIndex].getAbsLocation();

				schemPinsToSwap.add(pinToSwapWith);
				associationsMap.put(pinToSwapWith, new ManageAssociations(pinToSwapWith));
				Collection<IAbstractSchemPin> connectedAddPins =
						PinPlacementHelper.getConnectedAbstractSchemPins(pinToSwapWith);
				schemPinssToDelete.addAll(connectedAddPins);
				swapInfo = new MovePinHandler.SwapInfo(pinToSwapWith, swapLocation);
			}
			else {
				swapInfo = new MovePinHandler.SwapInfo(null, null);
			}
			boolean ignoreMate = originalParent.getConnectivity() instanceof IDevice;
			if (!ignoreMate) {
				swapInfo = null;
			}

			movePinHandler.movePinToLocation(schemPin, abs_x, abs_y, matchMap.get(schemPin),
					anchor, modifiedPins, m_pinMateMap, processedDevicePin, m_pinSidesBeforeMove,
					m_newConnPinNames, ignoreMate, true, swapInfo);
		}

		if (actuallyDidSomething) {
			Generator generator = Generator.getGenerator();
			Set<IPinList> affectedPinLists = new HashSet<>();
			affectedPinLists.addAll(new ModularSchemPinListInfo(originalParent).getCandidates());
			affectedPinLists.addAll(m_destPinList.getCandidates());

			for (IPinList affectedPinList : affectedPinLists) {
				PinListAddPinHelper.regeneratePinListGraphics(affectedPinList, diagram, generator, true);
			}

			Iterator<IAbstractSchemPin> schemPinIterator = schemPinssToDelete.iterator();
			while (schemPinIterator.hasNext()) {
				IAbstractSchemPin pin = schemPinIterator.next();
				if (pin.getParent() != null) {
					Collection<IAbstractSchemPin> connSchemPins = PinPlacementHelper.getConnectedAbstractSchemPins(pin);
					if (!connSchemPins.isEmpty()) {

						schemPinIterator.remove();
					}
				}
			}

			associationsMap.keySet().forEach(aPin ->
			{
				associationsMap.get(aPin).updateConnection();
				movePinHandler.adjustLineStyleForSegments(aPin);
			});

			Collection<IUIDObject> schemObjectsToDelete = new ArrayList<>(schemPinssToDelete);

			for (IAbstractSchemPin aPin : m_pins) {
				ConductorRouteAction.getInstance().addPinForRoute(aPin);
				Collection<IAbstractSchemPin> connectedSchemPins =
						PinPlacementHelper.getConnectedAbstractSchemPins(aPin);
				ConductorRouteAction.getInstance().addPinsForRoute(connectedSchemPins);
			}

			Map<IPinList, AttachedPinlistForDeleteOrDisconnect> diconnectOrDelete = new HashMap<>();
			//process only the original pinlits whose pins are being moved.
			for (IPinList affectedPinList : originalPinParents) {
				AttachedPinlistForDeleteOrDisconnect objectsToDelete =
						collectObjectsToDelete(affectedPinList, schemPinssToDelete);
				diconnectOrDelete.put(affectedPinList, objectsToDelete);
				schemObjectsToDelete.addAll(objectsToDelete.getAttachedObjectsToDelete());
			}

			Collection<IAbstractSchemPin> connectedSchemPins = schemPinsToSwap.stream()
					.flatMap(aPin -> {
						return PinPlacementHelper.getConnectedAbstractSchemPins(aPin).stream();
					})
					.collect(Collectors.toSet());
			ConductorRouteAction.getInstance().addPinsForRoute(schemPinsToSwap);
			ConductorRouteAction.getInstance().addPinsForRoute(connectedSchemPins);

			Collection<IUIDObject> toBeActuallyDeleted = filterNonBlankModularParents(schemObjectsToDelete);
			DeleteHelper.getInstance().delete(diagram, toBeActuallyDeleted, false);
			forceDeleteIfNotAlreadyDeleted(toBeActuallyDeleted);

			for (Map.Entry<IPinList, AttachedPinlistForDeleteOrDisconnect> entry : diconnectOrDelete.entrySet()) {
				entry.getValue().getAttachedObjectsForDisconnect()
						.forEach(anAttachedPinlist -> {
							anAttachedPinlist.removeAttachedObject(entry.getKey());
							entry.getKey().removeAttachedObject(anAttachedPinlist);
						});
			}

			for (IPinList affectedPinList : affectedPinLists) {
				resizeAffectedPinlist(affectedPinList);
			}
		}
		return actuallyDidSomething;
	}

	@NotNull private Collection<IUIDObject> filterNonBlankModularParents(
			@NotNull Collection<IUIDObject> schemObjectsToDelete)
	{
		Set<IUIDObject> result = new HashSet<>(schemObjectsToDelete);
		Set<IPinList> markedForDeletion = CollectionUtils.getObjects(schemObjectsToDelete, IPinList.class);

		Map<IPinList, ModularSchemPinListInfo> modularGroup = new HashMap<>();
		ModularConnectorHelper.generateModularGrouping(markedForDeletion, modularGroup);

		for (Map.Entry<IPinList, ModularSchemPinListInfo> entry : modularGroup.entrySet()) {
			//first mark blank pinlists also for deletion.
			Set<IPinList> candidates = entry.getValue().getCandidates();
			for (IPinList candidate : candidates) {
				if (candidate.getAllPins().isEmpty()) {
					markedForDeletion.add(candidate);
				}
			}
		}

		//add additional objects for deletion.
		result.addAll(markedForDeletion);

		for (Map.Entry<IPinList, ModularSchemPinListInfo> entry : modularGroup.entrySet()) {
			//now unmark the parent hierarchy for each members not marked for deletion.
			Set<IPinList> candidates = entry.getValue().getCandidates();
			for (IPinList candidate : candidates) {
				if (!markedForDeletion.contains(candidate)) {
					IPinList parentSchemPinList = ConnectorHelper.getParentSchemPinList(candidate);
					while (parentSchemPinList != null) {
						result.remove(parentSchemPinList);
						parentSchemPinList = ConnectorHelper.getParentSchemPinList(parentSchemPinList);
					}
				}
			}
		}

		return result;
	}

	private void forceDeleteIfNotAlreadyDeleted(Collection<IUIDObject> schemObjectsToDelete)
	{
		// DeleteHelper may not delete some of the objects passed to it for deletion, depending upon the checks in it
		// ex : Pins belonging to plug connectors that are not editable (like harness connectors of a libraried part), are not deleted by DeleteHelper.
		// All such objects will be forcefully deleted with the below code
		for (IUIDObject schemObject : schemObjectsToDelete) {
			if (schemObject != null) {
				if (UIDMgr.getNonDeletedObject(schemObject.getUID()) != null &&
						!CreationDeletionHelper.getTheCreationHelper().isObjectAddedForDelete(schemObject)) {
					schemObject.delete();
				}
			}
		}
	}

	private void resizeAffectedPinlist(@NotNull IPinList pinlist)
	{
		if (pinlist.getConnectivity() instanceof IDevice) {
			resizeAttachedPinlists(pinlist);
		}
		else if (pinlist.getConnectivity() instanceof IDeviceOwned &&
				((IDeviceOwned) pinlist.getConnectivity()).getOwner() != null) {
			resizePinlist(pinlist);
		}
	}

	private void resizeAttachedPinlists(@NotNull IPinList pinlist)
	{
		for (IPinList attachedPinlist : pinlist.getAttachedPinListObjects()) {

			resizePinlist(attachedPinlist);
		}
	}

	private void resizePinlist(IPinList pinlist)
	{
		if (pinlist.getParameterized() != null && pinlist.getConnectivity() instanceof IDeviceOwned &&
				!(pinlist.getConnectivity() instanceof IDeviceConnector)) {
			ResizeHelper resizeHelper = new ResizeHelper(pinlist, m_genParams);
			resizeVertically(pinlist, pinlist.getDiagram().getGrid(), resizeHelper, false);
			resizeVertically(pinlist, pinlist.getDiagram().getGrid(), resizeHelper, true);
		}
	}

	private void resizeVertically(IPinList pinlist, IGrid grid, ResizeHelper resizeHelper, boolean fromBottom)
	{
		IResizableDynamicCompound dynamicCompound =
				getResizableDynamicCompoundForPinList((IDynamicGfxMediator) pinlist);
		if (dynamicCompound == null) {
			return;
		}
		IExtent origExtent = dynamicCompound.getInitialExtent();
		IExtent extent = new Extent(origExtent);
		ExtentHelper.untransformExtent(pinlist.getTransform(), extent);
		IRectangle rectGfx = FactoryMgr.getDrawFactory().constructRectangle(0, 0, 1, 1);
		rectGfx.setWidth(extent.getWidth());
		rectGfx.setHeight(extent.getHeight());
		rectGfx.getLocation().setLocation(extent.getX(), extent.getY());
		if (fromBottom) {
			ILocation rectGfxLocation = rectGfx.getLocation();
			rectGfx.getLocation().setLocation(rectGfxLocation.getX(),
					rectGfxLocation.getY() + (rectGfx.getHeight() - grid.getGridSpacing()));
		}
		rectGfx.setHeight(grid.getGridSpacing());
		resizeHelper.resizePinList(rectGfx, grid, origExtent);
	}

	@Nullable private IResizableDynamicCompound getResizableDynamicCompoundForPinList(IDynamicGfxMediator pinlist)
	{
		IDynamicGfxFactory dynamicGfxFactoryHelper = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
		IDynamicGfx dynamic = pinlist.createDynamic(dynamicGfxFactoryHelper, null, false, true);
		return CommonUtils.cast(dynamic, IResizableDynamicCompound.class);
	}

	private static class AttachedPinlistForDeleteOrDisconnect
	{

		private Collection<IUIDObject> attachedObjectsToDelete = new ArrayList<>();
		private Collection<IPinList> attachedObjectsForDisconnect = new ArrayList<>();

		void addAttachedPinlistForDelete(IUIDObject pinList)
		{
			attachedObjectsToDelete.add(pinList);
		}

		void addAttachedObjectForDisconnect(IPinList pinList)
		{
			attachedObjectsForDisconnect.add(pinList);
		}

		protected Collection<IUIDObject> getAttachedObjectsToDelete()
		{
			return attachedObjectsToDelete;
		}

		protected Collection<IPinList> getAttachedObjectsForDisconnect()
		{
			return attachedObjectsForDisconnect;
		}
	}

	private AttachedPinlistForDeleteOrDisconnect collectObjectsToDelete(IPinList movedPinsOldParent,
			Collection<IAbstractSchemPin> schemPinsToDelete)
	{

		AttachedPinlistForDeleteOrDisconnect attachedPinlistForDeleteOrDisconnect =
				new AttachedPinlistForDeleteOrDisconnect();
		Collection<IAbstractSchemPin> pinsPresentOnOldParent = movedPinsOldParent.getAllPins().stream()
				.filter(aPin -> !schemPinsToDelete.contains(aPin)).collect(Collectors.toSet());

		if (movedPinsOldParent.getConnectivity() instanceof IGenericInlineConnector &&
				!pinsPresentOnOldParent.isEmpty()) {
			return attachedPinlistForDeleteOrDisconnect;
		}
		//skip the modular parent child.
		for (IPinList attachedPinlist : movedPinsOldParent.getAttachedPinListObjects()) {
			chs.cof.logical.cable.IPinList attachedPinlistConnectivity = attachedPinlist.getConnectivity();
			if (attachedPinlistConnectivity instanceof IDeviceConnector) {
				continue;
			}
			if (attachedPinlistConnectivity == movedPinsOldParent.getConnectivity()) {
				// case of composite symbol
				continue;
			}
			List<IAbstractSchemPin> attachedPinsAfterMove =
					attachedPinlist.getAllPins().stream().filter(aPin -> !schemPinsToDelete.contains(aPin))
							.collect(Collectors.toList());
			if (attachedPinsAfterMove.isEmpty()) {

				attachedPinlistForDeleteOrDisconnect.addAttachedPinlistForDelete(attachedPinlist);
			}
			else if (attachedPinlistConnectivity instanceof IGenericInlineConnector) {
				return attachedPinlistForDeleteOrDisconnect;
			}
			else if (attachedPinlistConnectivity instanceof IBaseDevice) {
				IHarnessPlugConnector plugConnectorInstanceInMovePin = null;
				//do not detach a plug connector that is automatically generated.

				if (movedPinsOldParent.getConnectivity() instanceof IHarnessPlugConnector) {
					plugConnectorInstanceInMovePin = (IHarnessPlugConnector) movedPinsOldParent.getConnectivity();
				}
				if (plugConnectorInstanceInMovePin == null ||
						plugConnectorInstanceInMovePin.getGenerationType() == TypeAutomaticallyGenerated) {
					continue;
				}

				boolean disconnect = canDetachAfterMovePin(attachedPinsAfterMove, pinsPresentOnOldParent);
				if (disconnect) {
					attachedPinlistForDeleteOrDisconnect.addAttachedObjectForDisconnect(attachedPinlist);
				}
			}
			else if (attachedPinlistConnectivity instanceof IConnector) {

				boolean disconnect = canDetachAfterMovePin(attachedPinsAfterMove, pinsPresentOnOldParent);
				if (disconnect) {
					attachedPinlistForDeleteOrDisconnect.addAttachedObjectForDisconnect(attachedPinlist);
				}
			}
		}

		if (pinsPresentOnOldParent.isEmpty()) {
			attachedPinlistForDeleteOrDisconnect.addAttachedPinlistForDelete(movedPinsOldParent);
		}
		return attachedPinlistForDeleteOrDisconnect;
	}

	private boolean canDetachAfterMovePin(Collection<IAbstractSchemPin> attachedPinsAfterMove,
			Collection<IAbstractSchemPin> pinsPresentOnOldParent)
	{
		boolean disconnect = true;
		for (IAbstractSchemPin aPin : attachedPinsAfterMove) {
			Collection<IAbstractSchemPin> connPins = PinPlacementHelper.getConnectedAbstractSchemPins(aPin);
			connPins.retainAll(pinsPresentOnOldParent);
			if (!connPins.isEmpty()) {
				disconnect = false;
				break;
			}
		}
		return disconnect;
	}

	@SuppressWarnings("unused")
	private boolean detachPinlists(MovePinHandler movePinHandler, ILogicDesign design)
	{
		for (Pair<IPinList, IPinList> pinListPair : movePinHandler.getModifiedPinListPairs()) {
			final IPinList pinlist = pinListPair.getFirst();
			final IPinList pinlistMate = pinListPair.getSecond();
			final chs.cof.logical.cable.IPinList cablePL = pinlist.getConnectivity();
			final chs.cof.logical.cable.IPinList mateCablePL = pinlistMate.getConnectivity();
			if (cablePL instanceof IGenericInlineConnector || mateCablePL instanceof IGenericInlineConnector) {
				continue;
			}

			ConnectionHelper helper = getConnectionHelper(pinlist, pinlistMate, cablePL, mateCablePL);
			boolean bDetach = true;
			boolean bDeviceToDevicePair = cablePL instanceof IDevice && mateCablePL instanceof IDevice;
			Collection<ILogicObject> toBeLocked = new HashSet<>();
			Collection<IPin> toBeDisconnected = new HashSet<>();
			for (IAbstractSchemPin schemPin : pinlist.getAllPins()) {
				if (bDeviceToDevicePair && schemPin instanceof IPin) {
					bDetach = detachDeviceToDeviceConnectedPins(cablePL, helper, toBeLocked, toBeDisconnected,
							(IPin) schemPin);
				}
				else {
					final IAbstractSchemPin connectedPin = helper.getConnectedPin(schemPin);
					if (schemPin.isConnected(connectedPin)) {
						bDetach = false;
						break;
					}
				}
			}
			if (bDetach) {
				pinlist.removeAttachedObject(pinlistMate);
				pinlistMate.removeAttachedObject(pinlist);
			}
			if (!toBeDisconnected.isEmpty()) {
				final Set<IUID> lockFailures = LogicObjectLockFinder.tryEdit(design, toBeLocked);
				if (lockFailures.isEmpty()) {
					for (IPin pin : toBeDisconnected) {
						ConnectionHelper.disconnectDeviceConnectedPin(pin, false);
					}
				}
				else {
					LogicConcurrencyLogger.getInstance().reportLockFailure(design, getActionTitle(), lockFailures,
							message -> reportLockFailures(message));
					return false;
				}
			}
		}
		return true;
	}

	private boolean detachDeviceToDeviceConnectedPins(chs.cof.logical.cable.IPinList cablePL, ConnectionHelper helper,
			Collection<ILogicObject> toBeLocked, Collection<IPin> toBeDisconnected, IPin schemPin)
	{
		boolean bDetach = true;
		IDevicePin devicePin = CommonUtils.cast(schemPin.getConnectivity(), IDevicePin.class);
		if (devicePin != null) {
			final IDevicePin connectedDevicePin = devicePin.getConnectedDevicePin();
			if (connectedDevicePin != null) {
				final IPin connectedPin = helper.getConnectedPin(schemPin);
				if (connectedPin == null && !ConnectionHelper.hasOtherInstancesForConnection(schemPin)) {
					toBeLocked.add(cablePL);
					toBeLocked.add(connectedDevicePin);
					toBeDisconnected.add(schemPin);
				}
				else {
					bDetach = false;
				}
			}
		}
		return bDetach;
	}

	@NotNull private ConnectionHelper getConnectionHelper(IPinList pinlist, IPinList pinlistMate,
			chs.cof.logical.cable.IPinList cablePL, chs.cof.logical.cable.IPinList mateCablePL)
	{
		ConnectionHelper helper;
		if (cablePL instanceof IDeviceLikePinlist) {
			helper = new ConnectionHelper(pinlist);
			helper.resetPinList(pinlistMate);
		}
		else if (mateCablePL instanceof IDeviceLikePinlist) {
			helper = new ConnectionHelper(pinlistMate);
			helper.resetPinList(pinlist);
		}
		else {
			helper = new ConnectionHelper(pinlist);
			helper.resetPinList(pinlistMate);
		}
		return helper;
	}

	private boolean lockSPLs()
	{
		assert m_destPinList != null;
		for (ISharedPinList spl : m_destPinList.determineSharedCandidatesForLock()) {
			if (!spl.lockForExclusiveRead()) {
				CTFLockUpdateHelper.displayLockFailureDialog(spl, UtilsHelper.getCHSSystem().getPersistenceSession());
				return false;
			}
		}
		return true;
	}

	private void unlockSPLs()
	{
		assert m_destPinList != null;
		for (ISharedPinList spl : m_destPinList.determineSharedCandidatesForLock()) {
			spl.unlock();
		}
	}

	/**
	 * For symbol we do not have placeholders, however we have added them as a temporary measure. However these
	 * placeholder need to be removed before we start editing the Model. Hence whilst we still have placeholder we can
	 * use them to determine the destination connectedPinsLocation. We must use locations and not the placeholders as
	 * they will be blown away. When moving a connector pin we need the matching connectedPinsLocation on the Symboled
	 * Device. i.e for both move within the same parent and move accross to a different connector.
	 */
	private Map<IGfxObject, ILocation> createDestinationPinLocations()
	{
		// Only for the case where we are moving a connector pin and are attached to a device with a symbol
		Map<IGfxObject, ILocation> matchMap = new HashMap<IGfxObject, ILocation>(m_pins.length);
		IPinList theMovingPinsParent = (IPinList) m_pins[0].getParent();
		assert theMovingPinsParent != null;
		if (!(theMovingPinsParent.getConnectivity() instanceof IConnector)) {
			return matchMap;
		}

		if (getDeviceWithSymbol(theMovingPinsParent) == null) {
			return matchMap;
		}

		for (int i = 0; i < m_pins.length; i++) {
			calculateTempPoint(i);
			ILocation loc = null;
			IGfxObject matchObj = getMatchingObjectAt(m_tmpPoint);
			if (matchObj != null) {
				ILocation matchLoc = matchObj.getLocation();
				loc = FactoryMgr.getCommonFactory().createLocation();
				loc.setLocation(matchLoc.getX(), matchLoc.getY());
			}
			matchMap.put(m_pins[i], loc);
		}
		return matchMap;
	}

	private void calculateTempPoint(int i)
	{
		assert m_selectedPoint != null;
		m_tmpPoint.x = m_selectedPoint.x + m_pinLocations[i].x - m_tmpPinSize / 2;
		m_tmpPoint.y = m_selectedPoint.y + m_pinLocations[i].y - m_tmpPinSize / 2;
		m_tmpPoint.setLocation(m_grid.snap(m_tmpPoint.x), m_grid.snap(m_tmpPoint.y));
	}

	private Integer getFreeIndex(Map<Integer, Integer> overlappingIndices, int startIndex)
	{
		Integer nextIndex = overlappingIndices.get(startIndex);

		Integer index = startIndex;
		while (nextIndex != null) {
			if (nextIndex == startIndex) {
				// cycle detected
				return overlappingIndices.get(startIndex);
			}
			index = nextIndex;
			nextIndex = overlappingIndices.get(index);
		}
		return index;
	}

	private Map<Integer, Integer> createOverlappingOldAndNewIndices()
	{
		Map<String, Integer> locationKeys = new HashMap<>();
		for (int i = 0; i < m_pins.length; i++) {
			calculateTempPoint(i);

			locationKeys.put(m_tmpPoint.x + "," + m_tmpPoint.y, i);
		}
		Map<Integer, Integer> overlappingIndices = new HashMap<>();
		for (int i = 0; i < m_pins.length; i++) {
			ILocation location = m_pins[i].getAbsLocation();

			Integer newMoveIndex = locationKeys.get(location.getX() + "," + location.getY());
			if (newMoveIndex != null) {
				overlappingIndices.put(i, newMoveIndex);
			}
		}
		return overlappingIndices;
	}

	private static void dumpPinListPositions(IPinList pinList)
	{
		if (DEBUG_PIN_POSITION) {
			assert pinList != null;
			System.out.println("\nPinlist " + pinList.getConnectivity().getName());
			// Get the graphic extent of the object.
			IExtent pinExtent;
			IParameterized par = pinList.getParameterized();
			if (par == null) {
				pinExtent = pinList.getNoTextExtent();
			}
			else {
				pinExtent = par.getExtent();
			}
			System.out.println(pinExtent);

			for (IGfxObjectIterator gitr = pinList.getObjects(); gitr.hasNext(); ) {
				IGfxObject gobj = gitr.getNext();
				if (gobj instanceof IPinPlaceholder || gobj instanceof IPin) {

					Side side = Side.getSide(pinExtent, gobj.getLocation());

					if (gobj instanceof IPinPlaceholder) {
						System.out.println(new StringBuilder().append("PinPlaceHolder ").append(gobj.getLocation())
								.append("   ").append(side).append("  ").toString());
					}
					else {
						IPin pin = (IPin) gobj;
						System.out.println(
								new StringBuilder().append("Pin ").append(pin.getConnectivity().getName()).append("  ")
										.append(gobj.getLocation()).append("   ").append(side).toString());
					}
				}
			}
		}
	}

	/**
	 * Caches pin sides for the pinlist and all attachedPinLists
	 *
	 * @param pinList the pinlist
	 */
	private void cacheAllPinSides(IPinList pinList)
	{
		if (pinList.getConnectivity() instanceof IConnector) {
			cachePinSide(pinList);
//			IPinList attachedPinList = PinPlacementHelper.getAttachedPinlist(pinList);
//			if (attachedPinList != null) {
			for (IPinList attachedPinList : pinList.getAttachedPinListObjects()) {
				if (attachedPinList.getConnectivity() instanceof IBaseDevice) {
					cacheDeviceOrSplicePinSides(attachedPinList);
				}
				else {
					cachePinSide(attachedPinList);
				}
			}
		}
		else {
			cacheDeviceOrSplicePinSides(pinList);
		}
	}

	private void cachePinSide(IPinList pinList)
	{
		IExtent absExtent = ExtentHelper.getAbsExtent(pinList, ExtentHelper.getNonTextExtent(pinList));
		for (IGfxObjectIterator gitr = pinList.getObjects(); gitr.hasNext(); ) {
			IGfxObject gobj = gitr.getNext();
			if (gobj instanceof IPin) {
				IPin thePin = (IPin) gobj;
				Side side = Side.getSide(absExtent, thePin.getAbsLocation());
				m_pinSidesBeforeMove.put(thePin, side);
			}
		}
	}

	private Set<IPinList> cachedPinLists = new HashSet<IPinList>();

	private void cacheDeviceOrSplicePinSides(IPinList pinListToCache)
	{
		chs.cof.logical.cable.IPinList cablePinList = pinListToCache.getConnectivity();
		assert (cablePinList instanceof IDeviceLikePinlist || cablePinList instanceof ISplice);
		//Cach Pin Sides for the Splice (needed if we have a splice with symbol)
		//Cache pinsides for the device and all the attached connectors
		cachePinSide(pinListToCache);
		if (cablePinList instanceof IDeviceLikePinlist) {
			for (IDiagramObjectIterator attachedObjects = pinListToCache.getAttachedObjects();
					attachedObjects.hasNext(); ) {
				IDiagramObject attachedObj = attachedObjects.next();
				if (attachedObj instanceof IPinList && !cachedPinLists.contains(attachedObj)) {
					cachedPinLists.add((IPinList) attachedObj);
					if (((IConnectivityRef) attachedObj).getConnectivity() instanceof IBaseDevice) {
						cacheDeviceOrSplicePinSides((IPinList) attachedObj);
					}
					cachePinSide((IPinList) attachedObj);
				}
			}
		}
	}

	protected int validPinPlacement(Point point, IAbstractSchemPin pin)
	{
		for (PinPlacementConstraintsHolder holder : m_constraintsHolders.values()) {
			if (m_destPinList != null && holder.sourceAnchorMatching(m_destPinList)) {
				//For the time being, don't allow placing the pin where a stackpin is located
				int valid = holder.allow(point, pin);
				if (valid != PinPlacementConstraintsHolder.PLACEMENT_NO) {
					return valid;
				}
			}
		}
		return PinPlacementConstraintsHolder.PLACEMENT_NO;
	}

	protected void removeTransientGfx()
	{
		// This can be called before m_constraints is setup
		super.removeTransientGfx();
		for (PinPlacementConstraintsHolder holder : m_constraintsHolders.values()) {
			holder.clearDynamics();
		}
	}

	@Nullable protected IGfxObject getMatchingTransientObject(Point point)
	{
		for (PinPlacementConstraintsHolder holder : m_constraintsHolders.values()) {
			IGfxObject matchObj = holder.getMatchingLocDynamics(point);
			if (matchObj != null) {
				return matchObj;
			}
		}
		return null;
	}

	@Nullable protected IGfxObject getMatchingObjectAt(Point theDestLocation)
	{
		for (PinPlacementConstraintsHolder holder : m_constraintsHolders.values()) {
			IGfxObject matchObj = holder.getMatchingObjectAt(theDestLocation);
			if (matchObj != null) {
				return matchObj;
			}
		}
		return null;
	}
}
