/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.logic.commands.ShieldTerminationUtils;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedDevice;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.cmd.CreateSchemGeneralHighwayCmd;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyActionContextForErrorReport;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.MovePinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.common.ILocation;
import chs.common.IPropertiedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.caf.utils.IPinProxy;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.PortHelper;
import chs.utility.UnitTestDataCaptureHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.SchemPinListHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.StackedPinHelper;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by IntelliJ IDEA. User: bkadukun Date: Mar 1, 2011 Time: 7:10:36 PM To change this template use File |
 * Settings | File Templates.
 */
public class EditStackPinActionHelper extends AbstractPinActionHelper
{

	private ISchemStackPin m_stackPin;
	private IAbstractSchemPin[] stackedPins;
	private ISchemStackPin m_matedStackPin = null;
	private boolean m_delete = false;
	private List<IAbstractPin> m_pinsToDelete;

	public EditStackPinActionHelper(ControllerActionRT action, boolean requirePlacement, boolean useBoundaryExtensions)
	{
		super(action, requirePlacement, useBoundaryExtensions);
	}

	protected boolean isStackPinAllowed()
	{
		return true;
	}

	protected void generateConstraintDynamicGraphics()
	{
		if (m_stackPin != null) {
			if (isLastPinOfStackGoingToBeCommited(getNumberOfPinsUnderPlacement())) {
				removeObjectFromAlreadyOccupiedPlaces(m_stackPin);
			}
			else {
				addObjectToAlreadyOccupiedPlaces(m_stackPin);
			}
		}
		super.generateConstraintDynamicGraphics();
	}

	public boolean setupStackForEdit(IPinList pinList, Collection<? extends IPinProxy> existingConnectivity,
			ISchemStackPin stackPin, boolean delete)
	{
		m_stackPin = stackPin;
		m_matedStackPin = ConnectionHelper.getConnectedStackedPin(m_stackPin);
		m_delete = delete;
		if (!m_delete) {
			return setUp(pinList, existingConnectivity);
		}
		else {
			//this is must for any pinactionhelper before taking its help.
			setupPinPlacementController(pinList, false);
			m_pinsToDelete = new ArrayList<IAbstractPin>(existingConnectivity.size());
			for (Object object : existingConnectivity) {
				if (object instanceof IPinProxy) {
					IPinProxy pinProxy = (IPinProxy) object;
					m_pinsToDelete.add(pinProxy.getCablePin());
				}
				else if (object instanceof IAbstractPin) {
					m_pinsToDelete.add((IAbstractPin) object);
				}
			}
			return true;
		}
	}

	@NotNull @Override
	protected PinPlacementConstraintsHolder getPinPlacementConstraintsHolder(@NotNull IPinList candidate,
			boolean includeBoundaryExtensions, ISheet sheet, IGfxContext context)
	{
		stackedPins = new IAbstractSchemPin[1];
		stackedPins[0] = m_stackPin;
		return getPinPlacementConstraints(candidate, includeBoundaryExtensions, sheet, context)
				.getHolder();
	}

	@NotNull @Override
	protected PinPlacementConstraints getPinPlacementConstraints(@NotNull IPinList candidate,
			boolean includeBoundaryExtensions, ISheet sheet, IGfxContext context)
	{
		if (stackedPins == null) {
			stackedPins = new IAbstractSchemPin[]{m_stackPin};
		}
		return new MovePinPlacementConstraints(candidate, stackedPins, m_genParams.getSpacing(),
				includeBoundaryExtensions, true, false);
	}


	private boolean isLastPinOfStackGoingToBeCommited(int currBatchSize)
	{
		if (m_stackPin == null) {
			return false;
		}
		int numberOfPinsToPlace = getNumberOfPinsToPlace();
		int alreadyCommited = countOfPinsPendingToAdd();
		int totalStackPinCount = m_stackPin.getAllConnectivity().size();
		return alreadyCommited + numberOfPinsToPlace == totalStackPinCount && numberOfPinsToPlace == currBatchSize;
	}

	protected boolean isActionAllowed(IAbstractPin placementPin, @NotNull Point currPt,
			boolean assumeInfiniteExtBoundary)
	{
		if (super.isActionAllowed(placementPin, currPt, assumeInfiniteExtBoundary)) {
			chs.cof.logical.cable.IPinList cablePlForStackPin = determineCablePinListForStackPlacement();
			if (cablePlForStackPin == null) {
				return false;
			}
			IGfxObject object = getPinPlacementController().getObjectAtCurrentValidPoint(currPt, cablePlForStackPin);
			if (m_stackPin != null && object == m_stackPin) {
				return isLastPinOfStackGoingToBeCommited(getNumberOfPinsUnderPlacement());
			}
			else {
				return !(object instanceof ISchemStackPin);
			}
		}
		return false;
	}

	protected boolean isValidBoundaryExtent(@Nullable IAbstractPin placementPin, @NotNull Point currPt)
	{
		return m_matedStackPin == null ||
				!getPinPlacementController().containsMatchingPinOnMatedPinLists(currPt, placementPin);
	}

	protected boolean editingStack()
	{
		return true;
	}

	public boolean isDeleteAction()
	{
		return m_delete;
	}

	private IEditStackExecuter executer;

	public boolean editStackPin(IPinList pinList, ISchemDiagram diagram, Predicate<Set<ILogicObject>> lockObjs)
	{
		ILogicDesign design = diagram.getDesign();
		Set<IPinList> pinListsToProcess = getEditedPinLists(pinList);
		boolean execute = true;
		if (design != null && (design.isUnderConcurrentEdit() || Environment.isUnitTest())) {
			executer = new EditStackLockCollector();
			editStackPin(pinListsToProcess, diagram);
			Set<ILogicObject> lockables = ((EditStackLockCollector) executer).getLockables();
			execute = lockObjs.test(lockables);
		}
		if (execute) {
			executer = new EditStackExecuter();
			editStackPin(pinListsToProcess, diagram);
		}
		return true;
	}

	private boolean shoulExecute()
	{
		return executer instanceof EditStackExecuter;
	}

	private void editStackPin(@NotNull Collection<IPinList> pinListsToProcess, ISchemDiagram diagram)
	{
		//must call this before processing add pins.
		ensurePlacingPinArgsDistributed();
		if (shoulExecute()) {
			transferPreemiesToCDH();
		}
		for (IPinList pinList : pinListsToProcess) {
			doEditStackPin(pinList, diagram);
		}
	}

	private void doEditStackPin(IPinList pinList, ISchemDiagram diagram)
	{
		Set<IAbstractSchemPin> existingPins = null;

		if (isUnitTestDataCapture()) {
			if (!m_delete) {
				existingPins = new HashSet<IAbstractSchemPin>();
				existingPins.addAll(pinList.getAllPins());
			}
			createPropertyForStackpinLocation(!m_delete ? UnitTestDataCaptureHelper.ActionType.UNSTACK :
					UnitTestDataCaptureHelper.ActionType.DELETEPIN_IN_STACK);
		}
		IPinList matePinList = null;
		List<AddPinArgs> pinsToPlace = getPinsCommitedToPlace(pinList);
		Collection<IPin> newPins = new ArrayList<>(pinsToPlace.size());
		if (m_delete) {
			deletePinsFromStackPin(diagram);
		}
		else {
			matePinList = unStackPinsFromStack(pinList, diagram, newPins);
		}

		//LOGIC-9690:Duplicate pins are getting created while doing edit stack
		SchemPinListHelper.purgeOverlappingPinPlaceHolders(pinList, matePinList);
		executer.regeneratePinList(pinList, diagram, matePinList);

		if (isUnitTestDataCapture() && existingPins != null) {
			createProperyForUnstackedPins(pinList, existingPins);
		}
		setIsReference(false);
	}

	private boolean isUnitTestDataCapture()
	{
		return UnitTestDataCaptureHelper.isEnabledUnitTestcapture() && shoulExecute();
	}

	private void addObjectForRefresh(@Nullable IPinList pinList)
	{
		if (pinList != null) {
			RegenerateGraphicsAction.getInstance().addObjectForRefresh(pinList);
		}
	}

	@Nullable
	private IPinList unStackPinsFromStack(IPinList pinList, ISchemDiagram diagram,
			Collection<IPin> newPins)
	{
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		GeneratorParameters gp = new GeneratorParameters(diagram.getGrid(), styleSet);
		Generator generator = Generator.getGenerator();
		generator.generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		List<Pair<AddPinArgs, IPin>> deferedPinArgs = new ArrayList<>();
		List<AddPinArgs> pinToMoveToStack = new ArrayList<AddPinArgs>();
		for (AddPinArgs args : getPinsCommitedToPlace(pinList)) {
			if (!args.isStackPin()) {
				IPin newSchemPin = unstackPins(pinList, args, diagram);
				deferedPinArgs.add(new Pair<AddPinArgs, IPin>(args, newSchemPin));
				if (newSchemPin != null) {
					newPins.add(newSchemPin);
				}
			}
			else {
				pinToMoveToStack.add(args);
			}
		}
		generator.generate(pinList, gp, Generator.NOREGENERATE_PROPERTIES, false);
		for (Pair<AddPinArgs, IPin> pinArgsIPinPair : deferedPinArgs) {
			IPin newMatedSchemPin = null;
			if (m_matedStackPin != null) {
				newMatedSchemPin =
						createMatedSchemPin(pinArgsIPinPair.getFirst(), styleSet, pinArgsIPinPair.getSecond());
			}
			unstackPinsFromStackPins(pinArgsIPinPair.getFirst().getPin(), pinArgsIPinPair.getSecond(),
					newMatedSchemPin);
			updatePortGraphicsOfConductorsOfPin(diagram, pinArgsIPinPair.getSecond());
		}
		if (!pinToMoveToStack.isEmpty()) {
			executer.movePinsIntoNewStackPin(pinToMoveToStack);
		}

		// Delete stack pin if all pins are deleted from the stack pin
		IPinList matePinList = deleteEmptyStacks(diagram);
		ConductorRouteAction.getInstance().addPinsForRoute(newPins);
		return matePinList;
	}

	@Nullable
	private IPinList deleteEmptyStacks(ISchemDiagram diagram)
	{
		IPinList matePinList = null;
		if (m_stackPin.getAllConnectivity().isEmpty()) {
			Collection<IUIDObject> toDelete = new ArrayList<IUIDObject>();
			toDelete.add(m_stackPin); // No need to add mated stack pin to the delete list

			// Gets deletable highway segments connected to stack pin and its mate
			List<ISchemStackPin> stackPins = new ArrayList<ISchemStackPin>();
			stackPins.add(m_stackPin);
			if (m_matedStackPin != null) {
				assert m_matedStackPin.getAllConnectivity().isEmpty();
				stackPins.add(m_matedStackPin);
				matePinList = (IPinList) m_matedStackPin.getParent();
			}
			Set<IHighwaySchematic> highways = new HashSet<IHighwaySchematic>();
			for (ISchemStackPin stackPin : stackPins) {
				for (IHighwaySegment seg : stackPin.getHighwaySegments()) {
					if (isDeletableSegmentAfterUnstack(stackPin, seg)) {
						toDelete.add(seg);
					}
					IJoint otherJoint =
							(seg.getStartJoint() == stackPin.getJoint()) ? seg.getEndJoint() : seg.getStartJoint();
					toDelete.addAll(getRedundentSegments(seg, otherJoint));
					highways.add(seg.getHighway());
				}
			}

			DeleteHelper.getInstance().delete(diagram, toDelete, true);
			CreationDeletionHelper.getTheCreationHelper().processObjects();

			// Delete highway now if it is not connected to any stack pin or conductor
			toDelete.clear();
			for (IHighwaySchematic highway : highways) {
				if (isDeletable(highway)) {
					for (IHighwaySegment seg : highway.getObjects(IHighwaySegment.class)) {
						toDelete.add(seg);
					}
				}
			}
			DeleteHelper.getInstance().delete(diagram, toDelete, true);
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}
		return matePinList;
	}

	protected Set<IHighwaySegment> getRedundentSegments(IHighwaySegment highwaySeg, @Nullable IJoint lastJoint)
	{
		Set<IHighwaySegment> segments = new HashSet<IHighwaySegment>();
		IJoint nextJoint = lastJoint;
		IHighwaySegment prevSegment = highwaySeg;

		while (nextJoint != null && !isJunction(nextJoint) &&
				getNonDeletableAssociations(nextJoint, IHighwaySegment.class).size() == 2) {
			segments.add(prevSegment);
			Set<IHighwaySegment> segs = getNonDeletableAssociations(nextJoint, IHighwaySegment.class);
			segs.remove(prevSegment);
			IHighwaySegment nextSegment = segs.iterator().next();
			nextJoint = nextSegment.getStartJoint() == nextJoint ? nextSegment.getEndJoint() :
					nextSegment.getStartJoint();
			prevSegment = nextSegment;
		}

		if (nextJoint == null || ((nextJoint.getAssociations(ISegment.class).isEmpty() ||
				getNonDeletableAssociations(nextJoint, IHighwaySegment.class).size() > 1) &&
				nextJoint.getAssociations(IAbstractSchemPin.class).isEmpty())) {
			segments.add(prevSegment);
		}

		return segments;
	}

	private <T extends IUIDObject> Set<T> getNonDeletableAssociations(IJoint nextJoint, Class<T> clazz)
	{
		Set<T> assObjs = nextJoint.getAssociations(clazz);
		Set<T> nondeleted = new HashSet<T>(assObjs.size());
		for (T seg : assObjs) {
			if (!CreationDeletionHelper.getTheCreationHelper().goingToDelete(seg)) {
				nondeleted.add(seg);
			}
		}
		return nondeleted;
	}

	private boolean isJunction(IJoint nextJoint)
	{
		return getNonDeletableAssociations(nextJoint, IDiagramObject.class).size() > 2 ||
				!nextJoint.getAssociations(IAbstractSchemPin.class).isEmpty() ||
				!nextJoint.getAssociations(ISegment.class).isEmpty();
	}

	@Nullable
	private IPin unstackPins(IPinList pinList, AddPinArgs args, ISchemDiagram diagram)
	{
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		IPin newSchemPin = createNewSchemPin(pinList, args, styleSet);

		markDiagramForHCGeneration(pinList);

		return newSchemPin;
	}

	@Nullable
	private IPin createNewSchemPin(IPinList pinList, AddPinArgs args, IPreferenceSet styleSet)
	{
		if (shoulExecute()) {
			IPin newSchemPin = createPin(getModel().getDiagram(), args.getPoint(), pinList,
					getConnectivityPinOwner(pinList.getConnectivity()), args.getPin(), args.getInternalPin(),
					args.getName(), styleSet);
			return newSchemPin;
		}
		return null;
	}

	private void updatePortGraphicsOfConductorsOfPin(ISchemDiagram diagram, @Nullable IPin newSchemPin)
	{
		if (newSchemPin != null) {
			for (Object cond : newSchemPin.getConductors()) {
				if (cond instanceof IConductor) {
					PortHelper.updatePortGfx((ICompoundObject) cond, diagram.getGrid().getGridSpacing());
				}
				else {
					assert false : "Pin should not be connected to any object other than conductor";
				}
			}
		}
	}

	@Nullable
	private IPin createMatedSchemPin(AddPinArgs args, IPreferenceSet styleSet, @Nullable IPin newSchemPin)
	{
		if (!shoulExecute()) {
			return null;
		}
		IPin newMatedSchemPin = null;
		IAbstractPin connectedPin = getConnectedPinFromStack(args.getPin(), m_matedStackPin);
		if (newSchemPin != null && connectedPin != null && m_matedStackPin.hasPin(connectedPin)) {
			// Create mated schem pin here

			IPinList matedPinList = (IPinList) m_matedStackPin.getParent();
			assert matedPinList != null;

			//Gets matching object for newly created pin
			IGfxObject match = getMatchingObject(newSchemPin);

			// match can be stack if user selected stack pin location to place a pin
			if (match == null || match instanceof IPinPlaceholder || match == m_matedStackPin) {
				//the modularize call might update the location. so create copy.
				ILocation location = FactoryMgr.getCommonFactory().constructLocation(
						(match != null) ? match.getLocation() :
								getMatchLocation(newSchemPin, m_stackPin, m_matedStackPin));
				if (match != null) {
					assert match.getContainer() instanceof IPinList :
							"Container of match object must be a pinlist";
					matedPinList = (IPinList) match.getContainer();
				}

				//determine the modular target mate pinlist.
				matedPinList = determineModularTargetMatePinlist(connectedPin, matedPinList, location);

				//the above call might update the location. so determine the pin point after modularization only.
				Point2D matePinPoint = new Point2D.Double(location.getX(), location.getY());

				newMatedSchemPin = createPin(getModel().getDiagram(), matePinPoint, matedPinList,
						getConnectivityPinOwner(matedPinList.getConnectivity()), connectedPin, null,
						connectedPin.getName(), styleSet);
				markDiagramForHCGeneration(matedPinList);
			}
			else {
				assert false : "Invalid match found here!!!";
			}
		}
		return newMatedSchemPin;
	}

	@NotNull private IPinList determineModularTargetMatePinlist(@NotNull IAbstractPin connectedPin,
			@NotNull IPinList matedPinList, @NotNull ILocation location)
	{
		if (matedPinList.getConnectivity() instanceof IConnector) {
			IPinList anchor = new ModularSchemPinListInfo(matedPinList).getAnchor();
			ILocation absLocation =
					CoordinateHelper.getAbsLocation(matedPinList, location.getX(), location.getY());
			ILocation relativeToAnchorLocation =
					CoordinateHelper.getRelativeLocation(anchor, absLocation.getX(), absLocation.getY());
			Point relPt = new Point(relativeToAnchorLocation.getX(), relativeToAnchorLocation.getY());
			chs.cof.logical.cable.IPinList cablePinlist = connectedPin.getOwner();
			assert cablePinlist != null;
			List<StackPinArgs> stackPinArgs = Collections.singletonList(new StackPinArgs(relPt, cablePinlist));
			ISchemDiagram diagram = DiagramHelper.getDiagram(anchor);
			assert diagram != null;
			ListMap<IPinList, StackPinArgs> distributedPinArgs = new ListMap<>();
			ConnectorHelper.distributeAddPinArgsToPinLists(anchor, diagram,
					stackPinArgs, (pl, a) -> distributedPinArgs.add(pl, (StackPinArgs) a));
			Set<Map.Entry<IPinList, List<StackPinArgs>>> entries = distributedPinArgs.entrySet();
			assert entries.size() == 1;
			Map.Entry<IPinList, List<StackPinArgs>> entry = entries.iterator().next();
			List<StackPinArgs> pinArgs = entry.getValue();
			assert pinArgs.size() == 1;
			Point2D point = pinArgs.iterator().next().getPoint();
			location.setLocation(CommonUtils.toInteger(point.getX()), CommonUtils.toInteger(point.getY()));
			return entry.getKey();
		}
		return matedPinList;
	}

	private ILocation getMatchLocation(@NotNull IAbstractSchemPin newSchemPin, @NotNull ISchemStackPin stack,
			@NotNull ISchemStackPin matedStack)
	{
		ILocation matedStackloc = getLocation(matedStack);
		ILocation pinlocation = getLocation(newSchemPin);
		ILocation stackloc = getLocation(stack);
		int xdiff = pinlocation.getX() - stackloc.getX();
		int ydiff = pinlocation.getY() - stackloc.getY();
		IDiagramObject pinlist = matedStack.getParent();
		assert pinlist != null;
		return CoordinateHelper.getRelativeLocation(pinlist, matedStackloc.getX() + xdiff,
				matedStackloc.getY() + ydiff);
	}

	private ILocation getLocation(@NotNull IAbstractSchemPin newSchemPin)
	{
		IDiagramObject pinlist = newSchemPin.getParent();
		assert pinlist != null;
		return CoordinateHelper.getAbsLocation(pinlist, newSchemPin.getLocation().getX(),
				newSchemPin.getLocation().getY());
	}

	@Nullable
	private IGfxObject getMatchingObject(IAbstractSchemPin pin)
	{
		IPinList pinlist = (IPinList) pin.getParent();
		if (pinlist != null) {
			for (IPinList attachedPL : pinlist.getAttachedPinListObjects()) {
				ConnectionHelper chelper = new ConnectionHelper(pinlist);
				chelper.resetPinList(attachedPL);
				if (chelper.allowConnection()) {
					IGfxObject match = chelper.getMatchingPinPosition(pin, pinlist);
					if (match != null) {
						return match;
					}
				}
			}
		}
		else {
			assert false : "Parent of the pin should not be null";
		}
		return null;
	}

	private void markDiagramForHCGeneration(IPinList pinList)
	{
		// ConnectionHelper should take care of adding pins to the other half, but it is not working for shared (and libraried?) inlines
		if (pinList.getSharedObject() != null && pinList.getSharedObject() instanceof ISharedDevice &&
				PinListHelper.isHarnessFootprinted(pinList.getConnectivity())) {
			ISharedDevice shDev = (ISharedDevice) pinList.getSharedObject();
			assert shDev != null;
			ISchemDiagram diagram = DiagramHelper.getDiagram(pinList);
			assert diagram != null;
			shDev.markDiagramForHCGeneration(diagram.getUID(), true);
		}
	}

	private IPin createPin(ISchemDiagram diagram, Point2D p2, IPinList paramObj, chs.cof.logical.cable.IPinList device,
			IAbstractPin existingConnectivity, @Nullable IInternalPin internalPin, String pinName,
			IPreferenceSet styleSet)
	{
		IGrid grid = diagram.getGrid();
		// Add the pin (it Generates the gfx for us)
		Collection<IPinList> oldePinLists = paramObj.getAttachedPinListObjects();
		// Not sure why styleset is required to construct parameters. Generator seems to be ineterested only in grid spacing and width
		// Check and remove if not required
		GeneratorParameters gp = new GeneratorParameters(grid, styleSet);
		Generator generator = Generator.getGenerator();
		IPin newpin = AddPinHelper.generatePin(paramObj, device, (int) p2.getX(), (int) p2.getY(), grid,
				existingConnectivity, internalPin, styleSet, null, null, null, generator, gp, oldePinLists);
		if (pinName != null && !StringUtils.equals(pinName, newpin.getConnectivity().getName())) {
			newpin.getConnectivity().setName(pinName);
		}
		return newpin;
	}

	/**
	 * Disconnects conductor from highway connected to stack pin and connects this conductor to newly created schem pin.
	 * If conductor is logically connected to un-stacked pin and conductor is not connected to highway, new segmnet is
	 * schematic conductor is created which connects newly created schem pin for un-stacked pin and highway connected to
	 * stack pin.
	 *
	 * @param pinTobeUntacked Pin to be unstacked
	 * @param newSchemPin New schematic pin for un-stacked pin
	 * @param newMatedSchemPin New schematic pin for un-stacked mated pin
	 */
	private void unstackPinsFromStackPins(IAbstractPin pinTobeUntacked, @Nullable IPin newSchemPin,
			@Nullable IPin newMatedSchemPin)
	{
		IJoint stackJoint = m_stackPin.getJoint();
		IAbstractPin matedPinToUnstack = getConnectedPinFromStack(pinTobeUntacked, m_matedStackPin);
		executer.addObjectForRoute(newSchemPin);

		executer.removePinFromStack(pinTobeUntacked, m_stackPin);
		// Removes conductor from old highway and connects to newly created schem pin
		unstackConductorFromHighway(pinTobeUntacked, newSchemPin, m_stackPin, getHighwaySegments(stackJoint));

		if (m_matedStackPin != null && matedPinToUnstack != null) {
			executer.removePinFromStack(matedPinToUnstack, m_matedStackPin);
			unstackConductorFromHighway(matedPinToUnstack, newMatedSchemPin, m_matedStackPin,
					getHighwaySegments(m_matedStackPin.getJoint()));
			executer.addObjectForRoute(newMatedSchemPin);
		}
	}

	private Set<IHighwaySegment> getHighwaySegments(IJoint stackJoint)
	{
		return stackJoint != null ? stackJoint.getAssociations(IHighwaySegment.class) : Collections.emptySet();
	}

	// Removes conductor from old highway and connects to newly created schem pin
	private void unstackConductorFromHighway(IAbstractPin unstackedPin, @Nullable IPin newSchemPin,
			ISchemStackPin stackPin,
			Set<IHighwaySegment> highwaySegs)
	{
		// If Highway segment is connected to stack pin, connec
		if (highwaySegs != null && !highwaySegs.isEmpty()) {
			IHighwaySegment highwaySeg = highwaySegs.iterator().next();
			IHighwaySchematic highwaySchem = highwaySeg.getHighway();

			Set<chs.cof.logical.cable.IConductor> connectedConductors = unstackedPin.getConductorsAsSet();

			for (chs.cof.logical.cable.IConductor cableCond : connectedConductors) {

				if (cableCond instanceof IShieldConductor && cableCond.getMulticore() != null) {
					// Shield conductor can be part of muticore, if so it must be connected to some hookup.
					// But, hookup can not be there and even we do not know where the hookup is. So we do not create shield conductor

					//SP1406-dts0101044951 Unstacking pins which have shield terminations loses the termination
					Collection<IPin> pinCollection = new ArrayList<IPin>();
					pinCollection.add(newSchemPin);
					Map<IShieldConductor, Collection<IPin>> map = new HashMap<IShieldConductor, Collection<IPin>>();
					map.put((IShieldConductor) cableCond, pinCollection);
					executer.createShield(newSchemPin, cableCond, map);

					removeStackedPinconductorInterface(unstackedPin, cableCond, stackPin, highwaySchem, newSchemPin,
							null);
					continue;
				}
				IPinList pinList = (IPinList) stackPin.getParent();
				assert pinList != null;
				Pair<ISegment, IJoint> condSegmentJointPir =
						getSegmentInterfacedWithHighway(pinList, highwaySchem, cableCond);

				if (condSegmentJointPir != null) {    // Conductor connected to pin is interfacing with highway
					ISegment condSegment = condSegmentJointPir.getFirst();
					IJoint condHWJoint = condSegmentJointPir.getSecond();
					executer.moveConductorSegment(newSchemPin, condSegment, condHWJoint);

					removeStackedPinconductorInterface(unstackedPin, cableCond, stackPin, highwaySchem, newSchemPin,
							condSegment.getConductor());
					addObjectGenerateGraphics(condSegment);
				}
				else {    // Conductor is connected to pin, but its diagram object is not connected to highway
					createCondcutorForUnstackedPin(newSchemPin, stackPin, highwaySeg, cableCond, unstackedPin);
				}
			}
		}
	}

	@Nullable
	private Pair<ISegment, IJoint> getSegmentInterfacedWithHighway(IPinList pinList, IHighwaySchematic highwaySchem,
			chs.cof.logical.cable.IConductor cableCond)
	{
		Pair<ISegment, IJoint> condSegmentJointPir = null;
		if (pinList != null) {
			// Get segment of conductor interfaced with highway and connected to unstacking pin , this segment should be connected to pin after unstack
			List<Pair<ISegment, IJoint>> condSegmentJointPirs =
					getConnectoredSegmnet(highwaySchem, cableCond.getUID());

			for (Pair<ISegment, IJoint> segJointPair : condSegmentJointPirs) {
				// If conductor is connected to a pin of same pinlist on which we are unstackuing, we should not connect pin to that conductor
				ISegment seg = segJointPair.getFirst();
				if (seg.getConductor().getConnectedObjects().isEmpty()) {
					condSegmentJointPir = segJointPair;
					break;
				}

				for (Object obj : seg.getConductor().getConnectedObjects()) {
					if (obj instanceof IAbstractSchemPin && obj != pinList &&
							pinList != ((IDiagramObject) obj).getParent()) {
						condSegmentJointPir = segJointPair;
						break;
					}
				}
				if (condSegmentJointPir != null) {
					break;
				}
			}
		}
		return condSegmentJointPir;
	}

	private void createCondcutorForUnstackedPin(@Nullable IPin newSchemPin, ISchemStackPin stackPin,
			IHighwaySegment highwaySeg, chs.cof.logical.cable.IConductor cableCond, IAbstractPin pinToRemove)
	{

		List<Point> newSegmentPoints = new ArrayList<Point>();

		IHighwaySchematic highwaySchem = highwaySeg.getHighway();
		boolean condOtherEndConnected = isConnectedToOtherEnd(highwaySchem, cableCond.getUID(), stackPin);

		IJoint highwayJoint = populateNewSegmentPointsForUnstack(newSchemPin, stackPin, highwaySeg, newSegmentPoints,
				condOtherEndConnected);

		ISegment condSegment = executer.createConductor(newSchemPin, stackPin, cableCond, newSegmentPoints);
		IConductor schemConductor = condSegment != null ? (IConductor) condSegment.getParent() : null;

		if (condOtherEndConnected) {
			// conductor is connected to stack pin throgh highway, segment should be interfaced with highway
			interfaceWithhighwayJoint(highwayJoint, condSegment);
			executer.addConductorFromHighway((IHighwayConductor) cableCond, highwaySchem);
		}
		else {
			removeStackedPinconductorInterface(pinToRemove, cableCond, stackPin, highwaySchem, newSchemPin,
					schemConductor);
		}
		addObjectGenerateGraphics(condSegment);
	}

	@Nullable
	private IJoint populateNewSegmentPointsForUnstack(@Nullable IPin newSchemPin, ISchemStackPin stackPin,
			IHighwaySegment highwaySeg, List<Point> newSegmentPoints, boolean condOtherEndConnected)
	{
		if (newSchemPin != null) {
			IJoint highwayJoint = null;
			if (condOtherEndConnected) {
				// Other end of the conductor is connected to a stackpin though highway, so conductor should interface with highway
				ILocation otherEnd = getOtherEnd(highwaySeg, stackPin.getJoint());
				highwayJoint = createJointAt(highwaySeg, otherEnd);
				assert highwayJoint != null;
				newSegmentPoints.add(new Point(highwayJoint.getX(), highwayJoint.getY()));
			}
			else {    // Other end of the conductor is not connected to stack pin (Un-routed conductor)
				ILocation location = HighwayHelper.getDefaultStackpinHighwayLocation(newSchemPin);
				newSegmentPoints.add(new Point(location.getX(), location.getY()));
			}

			ILocation point = newSchemPin.getAbsLocation();
			newSegmentPoints.add(new Point(point.getX(), point.getY()));
			return highwayJoint;
		}
		return null;
	}

	private void interfaceWithhighwayJoint(@Nullable IJoint highwayJoint, @Nullable ISegment condSegment)
	{
		if (condSegment != null && shoulExecute() && highwayJoint != null) {
			if (Double.compare(condSegment.getStartPoint().distance(highwayJoint), 0) == 0) {
				condSegment.setStartJoint(highwayJoint);
			}
			else {
				condSegment.setEndJoint(highwayJoint);
			}
		}
	}

	private void addObjectGenerateGraphics(@Nullable ISegment condSegment)
	{
		if (condSegment != null && shoulExecute()) {
			RegenerateGraphicsAction.getInstance().addObjectForRegenrate(condSegment.getConductor());
		}
	}

	private boolean arePointsEqual(IJoint pinJoint, ILocation point)
	{
		return Double.compare(point.distance(pinJoint), 0) == 0;
	}

	/**
	 * Removes stacked pin conductor from highway connectivity if conductor is not connected to any other pins of the
	 * connected stacked pins to this highway and its other instances of the highway
	 *
	 * @param cablePin Cable pin connected to conductor
	 * @param cableCond Conductor whose interface to be removed from the highway
	 * @param stackedPin Stacked pin from which we are disconnecting
	 * @param highwaySchem Highway from which conductor interface to be moved
	 * @param excludePinConn Newly created pin instance
	 * @param excludeCondConn New instance of the conductor
	 */
	private void removeStackedPinconductorInterface(IAbstractPin cablePin, chs.cof.logical.cable.IConductor cableCond,
			ISchemStackPin stackedPin, IHighwaySchematic highwaySchem, @Nullable IPin excludePinConn,
			@Nullable IConductor excludeCondConn)
	{
		Set<IConductor> excludeConds = new HashSet<IConductor>(1);
		if (excludeCondConn != null) {
			excludeConds.add(excludeCondConn);
		}

		Set<IHighwaySchematic> excludeHighways = new HashSet<IHighwaySchematic>(1);
		excludeHighways.add(highwaySchem);

		Set<IAbstractSchemPin> excludePins = new HashSet<IAbstractSchemPin>(2);
		if (excludePinConn != null) {
			excludePins.add(excludePinConn);
		}
		excludePins.add(stackedPin);


		ILogicDesign design = cableCond.getLogicDesign();
		if (design != null) {
			Collection<IDiagramObject> representations =
					design.getDesignWideUsageMgr().getRepresentations(highwaySchem.getConnectivity());

			// Check whether there is any other connection between cable pin and highway
			boolean removeStackedPinCond = !HighwayHelper.isConductorConnectedThroughOtherStackPin(cableCond,
					Set.of(stackedPin), representations);
			removeStackedPinCond &= executer.shouldRemoveStackedPinConductor(cablePin);
			if (removeStackedPinCond) {
				executer.removeStackedPinConductor((IHighwayConductor) cableCond, highwaySchem);
			}

			// Checks whether instance of the conductor is interfaced with the highway in some other place
			if (shouldRemoveConductorFromHighway(cableCond, highwaySchem) &&
					!HighwayHelper.hasOtherHighwayConnection(cableCond, excludeConds,
							excludeHighways,representations)) {
				executer.removeConductorFromHighway((IHighwayConductor) cableCond, highwaySchem);
			}
		}
	}

	private boolean shouldRemoveConductorFromHighway(chs.cof.logical.cable.IConductor cableCond,
			IHighwaySchematic highwaySchem)
	{
		return executer.shouldRemoveConductorFromHighway(cableCond, highwaySchem);
	}

	private boolean isDeletableSegmentAfterUnstack(ISchemStackPin stackPin, IHighwaySegment highwaySeg)
	{
		IJoint otherJoint = highwaySeg.getStartJoint() == stackPin.getJoint() ?
				highwaySeg.getEndJoint() : highwaySeg.getStartJoint();
		return otherJoint != null && otherJoint.getAssociations(ISchemStackPin.class).isEmpty() &&
				(otherJoint.getAssociations(ISegment.class).isEmpty() ||
						otherJoint.getAssociations(IHighwaySegment.class).size() > 1);
	}

	/**
	 * Deletes pin present in the stack pin. Removes pin from the stack pin if there are other instances of the pin.
	 * Removes pin fromthe stack pin as well as deletes the pin if there are no other instances of these pin.
	 *
	 * @param diagram Diagram
	 */
	private void deletePinsFromStackPin(ISchemDiagram diagram)
	{
		StringBuilder deletedPinNames = collectedPinsToDeleteIfUnitTestCapture();

		boolean deletingAllPins = m_pinsToDelete.containsAll(m_stackPin.getAllConnectivity());

		if (deletingAllPins) {
			executer.deleteStackPin(diagram, m_stackPin);
		}
		else {
			deleteSelectedPins();
		}

		createDeletePinNamesPropertyForUnstack(deletedPinNames);
	}

	private void deleteSelectedPins()
	{
		for (IAbstractPin pinInStack : m_pinsToDelete) {
			executer.deletePinFromStack(pinInStack, m_stackPin);

			IAbstractPin matedPin = getConnectedPinFromStack(pinInStack, m_matedStackPin);
			assert m_matedStackPin == null || matedPin != null : "pin in mated stack must have connected pin";

			if (m_matedStackPin != null && matedPin != null) {
				executer.deletePinFromStack(matedPin, m_matedStackPin);
			}
			updateHighwayConnectivity(m_stackPin, m_matedStackPin, pinInStack);
		}
	}

	@NotNull
	private StringBuilder collectedPinsToDeleteIfUnitTestCapture()
	{
		StringBuilder deletedPinNames = new StringBuilder();
		if (isUnitTestDataCapture()) {
			for (IAbstractPin pinInStack : m_pinsToDelete) {
				deletedPinNames.append(pinInStack.getName()).append(';');
			}
		}
		return deletedPinNames;
	}

	@Nullable
	private IAbstractPin getConnectedPinFromStack(IAbstractPin pinInStack, ISchemStackPin stackPin)
	{
		IPinList matedPinList = stackPin != null ? (IPinList) stackPin.getParent() : null;
		return matedPinList != null ? pinInStack.getConnectedPin(matedPinList.getConnectivity()) : null;
	}

	// Updates the connectivity of highway connected to stack.
	private void updateHighwayConnectivity(ISchemStackPin stackPin, @Nullable ISchemStackPin matedStackPin,
			IAbstractPin pinInStack)
	{
		updateHighwayConnectivity(stackPin, pinInStack);
		if (matedStackPin != null) {
			IAbstractPin matedPin = getConnectedPinFromStack(pinInStack, matedStackPin);
			assert matedPin != null : "Pin within stack must have mated pin if stack is mated";
			updateHighwayConnectivity(matedStackPin, matedPin);
		}
	}

	private void updateHighwayConnectivity(ISchemStackPin stackPin, IAbstractPin pinConnectedToCond)
	{
		Set<IHighwaySchematic> connectedHWs = stackPin.getConnectedHighways();
		if (connectedHWs.isEmpty()) {
			// Nothing to update
			return;
		}
		// There can be at most one connected highwaySchem. Not sure why we have a Set here !?
		assert connectedHWs.size() == 1;
		IHighwaySchematic highway = connectedHWs.iterator().next();

		// Just for double check
		if (highway == null) {
			return;
		}

		IDesignWideUsageMgr dwum = getDesignWideUsageMgr();

		for (chs.cof.logical.cable.IConductor cableCond : pinConnectedToCond.getConductorsAsSet()) {
			removeStackedPinconductorInterface(pinConnectedToCond, cableCond, stackPin, highway, null, null);
			// Updates port graphics for the conductors
			updatePortGraphics(dwum, cableCond);
		}
	}

	private void updatePortGraphics(IDesignWideUsageMgr dwum, chs.cof.logical.cable.IConductor cableCond)
	{
		executer.updatePortGraphics(dwum, cableCond);
	}

	private IDesignWideUsageMgr getDesignWideUsageMgr()
	{
		ILogicDesign design = getModel().getDesign();
		return design.getDesignWideUsageMgr();
	}

	private void createDeletePinNamesPropertyForUnstack(StringBuilder deletedPinNames)
	{
		if (deletedPinNames != null && isUnitTestDataCapture()) {
			// removes char ; at end of the string builder
			String pinNames = deletedPinNames.toString();
			if (pinNames.endsWith(";")) {
				pinNames = pinNames.substring(0, pinNames.length() - 1);
			}
			deletedPinNames.replace(deletedPinNames.length() - 1, deletedPinNames.length() - 1, "");
			IDiagramObject pinlist = m_stackPin.getParent();
			assert pinlist != null;
			UnitTestDataCaptureHelper.createPropertyForUnitTest((IPropertiedObject) pinlist,
					UnitTestDataCaptureHelper.ActionType.DELETEPIN_IN_STACK, "PINNAMES", pinNames);
		}
	}

	// Creates new stack pin and moves selected pins from old stack pin
	protected void movePinsIntoNewStackPin(List<AddPinArgs> pinstoUnstack)
	{
		Point2D newLocation = pinstoUnstack.iterator().next().getPoint();

		List<IAbstractPin> pins = new ArrayList<IAbstractPin>(pinstoUnstack.size());
		for (AddPinArgs pinOrg : pinstoUnstack) {
			IAbstractPin pin = pinOrg.getPin();
			pins.add(pin);
			m_stackPin.removePinFromStack(pin);
		}

		IPinList pinlist = (IPinList) m_stackPin.getParent();
		assert pinlist != null;
		ISchemStackPin stackpin = StackedPinHelper.createAndAddStackPin(pinlist,
				new Location((int) newLocation.getX(), (int) newLocation.getY()));
		RegenerateGraphicsAction.getInstance().addObjectForRegenrate(stackpin);

		for (IAbstractPin pin : pins) {
			stackpin.addPinToStack(pin);
		}

		IJoint joint = m_stackPin.getJoint();
		if (joint != null) {
			Set<IHighwaySegment> highwaySegs = joint.getAssociations(IHighwaySegment.class);
			movePinsToNewStackPin(stackpin, joint, highwaySegs);
		}

		if (m_matedStackPin != null) {
			IPinList matePinList = (IPinList) m_matedStackPin.getParent();
			assert matePinList != null;

			IGfxObject match = getMatchingObject(stackpin);
			ILocation location = null;
			if (match == null || match == m_matedStackPin || match instanceof IPinPlaceholder) {
				location = (match != null) ? match.getLocation() :
						getMatchLocation(stackpin, m_stackPin, m_matedStackPin);
				if (match != null) {
					assert match.getContainer() instanceof IPinList : "Container of match object must be a pinlist";
					matePinList = (IPinList) match.getContainer();
				}
			}
			else {
				assert false : "Invalid match found here!!!";
			}

			if (location != null) {
				ISchemStackPin newMatedStackPin =
						StackedPinHelper.createAndAddStackPin(matePinList, location);
				RegenerateGraphicsAction.getInstance().addObjectForRegenrate(newMatedStackPin);

				for (IAbstractPin pin : pins) {
					IAbstractPin matedPin = getConnectedPinFromStack(pin, m_matedStackPin);
					if (matedPin != null) {
						m_matedStackPin.removePinFromStack(matedPin);
						newMatedStackPin.addPinToStack(matedPin);
					}
					else {
						assert false : "Mated pin should not be null";
					}
				}

				IJoint matedJoint = m_matedStackPin.getJoint();
				if (matedJoint != null) {
					Set<IHighwaySegment> highwaySegs1 = matedJoint.getAssociations(IHighwaySegment.class);
					movePinsToNewStackPin(newMatedStackPin, matedJoint, highwaySegs1);
				}
				matePinList.regenerateDiagramObject();
			}
		}
	}

	protected void movePinsToNewStackPin(ISchemStackPin stackpin, IJoint joint, Set<IHighwaySegment> highwaySegs)
	{
		if (!highwaySegs.isEmpty()) {
			IHighwaySegment highwaySeg = highwaySegs.iterator().next();
			IHighwaySchematic highwaySchem = highwaySeg.getHighway();

			ILocation otherEnd = getOtherEnd(highwaySeg, joint);

			IJoint highwayJoint = createJointAt(highwaySeg, otherEnd);
			List<Point> newSegmentPoints = new ArrayList<Point>();
			newSegmentPoints.add(new Point(Objects.requireNonNull(highwayJoint).getX(), Objects.requireNonNull(highwayJoint).getY()));
			newSegmentPoints.add(new Point(stackpin.getAbsLocation().getX(), stackpin.getAbsLocation().getY()));

			List<IHighwaySegment> segments = CreateSchemGeneralHighwayCmd.createSegments(newSegmentPoints, highwaySchem, true);
			IHighwaySegment newSeg = segments.iterator().next();
			newSeg.setParent(highwaySchem);

			IJoint stackPinNode = FactoryMgr.getSchemFactory()
					.constructNode(FactoryMgr.getCommonFactory().createUID(), stackpin.getAbsLocation().getX(),
							stackpin.getAbsLocation().getY());
			stackpin.setJoint(stackPinNode);

			if (arePointsEqual(stackPinNode, newSeg.getStartPoint())) {
				newSeg.setStartJoint(stackPinNode);
				newSeg.setEndJoint(highwayJoint);
			}
			else {
				newSeg.setEndJoint(stackPinNode);
				newSeg.setStartJoint(highwayJoint);
			}
			highwaySchem.addSchemStackPin(stackpin.getUID());
		}
	}

	/**
	 * Gets the segment of the conductor connected to the highway
	 *
	 * @param highway Schematic highway
	 * @param condUID UID of the cable conductor
	 *
	 * @return Segment and joint on which this segment is connected if conductor is connected to highway else NULL
	 */
	private List<Pair<ISegment, IJoint>> getConnectoredSegmnet(IHighwaySchematic highway, IUID condUID)
	{
		Set<IJoint> highwayJoints = getNodes(highway);
		List<Pair<ISegment, IJoint>> connectedSegs = new ArrayList<Pair<ISegment, IJoint>>();
		for (IJoint highwayNode : highwayJoints) {
			for (ISegment segment : highwayNode.getAssociations(ISegment.class)) {
				IConductor conductor = segment.getConductor();
				IUID connectivityUID = conductor.getConnectivityUID();
				assert connectivityUID != null;
				if (connectivityUID.equals(condUID)) {
					connectedSegs.add(new Pair<ISegment, IJoint>(segment, highwayNode));
				}
			}
		}
		return connectedSegs;
	}

	/**
	 * Checks if the conductor is connected between two stack pins
	 *
	 * @param highway Schematic highway
	 * @param condUID UID of the cable conductor
	 * @param stackPin Stack pin to which highway is connected to
	 *
	 * @return true if conductor has connection with some is the stack pin at other end
	 */
	private boolean isConnectedToOtherEnd(IHighwaySchematic highway, IUID condUID, ISchemStackPin stackPin)
	{
		Set<IJoint> highwayNodes = getNodes(highway);
		for (IJoint highwayNode : highwayNodes) {
			for (ISchemStackPin otherStackPin : highwayNode.getAssociations(ISchemStackPin.class)) {
				if (otherStackPin != stackPin) {
					for (IAbstractPin pin : otherStackPin.getAllConnectivity()) {
						for (chs.cof.logical.cable.IConductor cod : pin.getConductorsAsSet()) {
							if (cod.getUID() == condUID) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private Set<IJoint> getNodes(IHighwaySchematic highway)
	{
		Set<IJoint> highwayNodes = new HashSet<IJoint>();
		for (IConnected seg : highway.getSegments()) {
			assert seg instanceof IHighwaySegment : "highways segments should be IHighwaySegments";
			if (seg.getStartJoint() != null) {
				highwayNodes.add(seg.getStartJoint());
			}
			if (seg.getEndJoint() != null) {
				highwayNodes.add(seg.getEndJoint());
			}
		}
		return highwayNodes;
	}

	private boolean isDeletable(IHighwaySchematic highway)
	{
		Set<IJoint> highwayNodes = getNodes(highway);
		for (IJoint highwayNode : highwayNodes) {
			if (!highwayNode.getAssociations(ISchemStackPin.class).isEmpty() ||
					!highwayNode.getAssociations(ISegment.class).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private int getNumberOfSegmentsOfConductorConnected(IHighwaySchematic highway, IUID condUID)
	{
		Set<IJoint> highwayNodes = getNodes(highway);
		int count = 0;
		for (IJoint highwayNode : highwayNodes) {
			for (ISegment segment : highwayNode.getAssociations(ISegment.class)) {
				if (segment.getConductor().getConnectivity().getUID() == condUID) {
					count++;
				}
			}
		}
		return count;
	}

	@Nullable
	private ILocation getOtherEnd(IHighwaySegment highwaySeg, IJoint joint)
	{
		IJoint otherEnd = null;
		if (highwaySeg.getStartPoint() == joint) {
			ILocation location = highwaySeg.getEndPoint();
			if (location instanceof IJoint) {
				otherEnd = (IJoint) location;
			}
			else {
				otherEnd = FactoryMgr.getSchemFactory().constructNode(
						FactoryMgr.getCommonFactory().createUID(), location.getX(), location.getY());
				highwaySeg.setEndJoint(otherEnd);
			}
		}
		else if (highwaySeg.getEndPoint() == joint) {
			ILocation location = highwaySeg.getStartPoint();
			if (location instanceof IJoint) {
				otherEnd = (IJoint) location;
			}
			else {
				otherEnd = FactoryMgr.getSchemFactory().constructNode(
						FactoryMgr.getCommonFactory().createUID(), location.getX(), location.getY());
			}
		}
		return otherEnd;
	}

	/**
	 * Creates joint at given location if it is not there
	 *
	 * @param highwaySeg Highway segment
	 * @param location location
	 *
	 * @return Joint
	 */
	@Nullable
	private IJoint createJointAt(IHighwaySegment highwaySeg, @Nullable ILocation location)
	{
		if (location instanceof IJoint && !((IJoint) location).getAssociations(ISchemStackPin.class).isEmpty()) {
			return SegmentHelper.getMidJointOftheHighwaySegment(highwaySeg);
		}
		IJoint otherEnd = null;
		if (!(location instanceof IJoint) && location != null) {
			if (highwaySeg.getStartPoint() == location) {
				otherEnd = FactoryMgr.getSchemFactory().constructNode(
						FactoryMgr.getCommonFactory().createUID(), location.getX(), location.getY());
				highwaySeg.setStartJoint(otherEnd);
			}
			else if (highwaySeg.getEndPoint() == location) {
				otherEnd = FactoryMgr.getSchemFactory().constructNode(
						FactoryMgr.getCommonFactory().createUID(), location.getX(), location.getY());
				highwaySeg.setEndJoint(otherEnd);
			}
		}
		else {
			return (IJoint) location;
		}

		return otherEnd;
	}

	/**
	 * Creates non-undoabble properties on the pinlist which are used as input for unit testing. This method should be
	 * called iff "Unit Test Data Capture" is enabled in DEBUG mode
	 *
	 * @param actionType Type of the action UNSTACK/DELETEPIN_IN_STACK
	 */
	private void createPropertyForStackpinLocation(UnitTestDataCaptureHelper.ActionType actionType)
	{
		IPinList pinlist = (IPinList) m_stackPin.getParent();
		assert pinlist != null : "Pin stack owner cannot be null";
		UnitTestDataCaptureHelper
				.createPropertyForUnitTest(pinlist, actionType, "STACKPIN_UID", m_stackPin.getUID().getString());

		UnitTestDataCaptureHelper
				.createPropertyForUnitTest(pinlist, actionType, "PINLIST_UID", pinlist.getUID().getString());
	}

	/**
	 * Creates non-undoabble properties on the pinlist which are used as input for unit testing. This method should be
	 * called iff "Unit Test Data Capture" is enabled in DEBUG mode
	 *
	 * @param pinList Pinlist on which properties to be created
	 * @param existingPins pins present on the pinlist before unstacking pins
	 */
	private void createProperyForUnstackedPins(IPinList pinList, Set<IAbstractSchemPin> existingPins)
	{
		StringBuilder pinNames = new StringBuilder();
		StringBuilder locations = new StringBuilder();
		StringBuilder pinNamesForStack = new StringBuilder();
		String seprator = ";";
		boolean first = true;
		ISchemStackPin stackPin = null;
		for (IAbstractSchemPin pin : pinList.getAllPins()) {
			if (!existingPins.contains(pin)) {
				if (pin instanceof ISchemStackPin && m_stackPin != pin) {
					stackPin = (ISchemStackPin) pin;
				}
				else if (pin instanceof IPin) {
					if (first) {
						first = false;
					}
					else {
						pinNames.append(seprator);
						locations.append(seprator);
					}
					IPin schemPin = (IPin) pin;
					pinNames.append(schemPin.getConnectivity().getName());
					locations.append(schemPin.getAbsLocation().getX()).append(',')
							.append(schemPin.getAbsLocation().getY());
				}
			}
		}

		if (stackPin != null) {
			for (IAbstractPin cablePin : stackPin.getAllConnectivity()) {
				if (first) {
					first = false;
				}
				else {
					pinNames.append(seprator);
				}
				pinNames.append(cablePin.getName());
			}
			StringBuilder stackPinLocation = new StringBuilder();
			stackPinLocation.append(stackPin.getAbsLocation().getX()).append(',')
					.append(stackPin.getAbsLocation().getY());

			UnitTestDataCaptureHelper.createPropertyForUnitTest(pinList, UnitTestDataCaptureHelper.ActionType.UNSTACK,
					"STACKPIN_LOCATION", stackPinLocation.toString());
		}

		if (pinNames.length() != 0) {
			pinNames.append(pinNamesForStack);
			UnitTestDataCaptureHelper
					.createPropertyForUnitTest(pinList, UnitTestDataCaptureHelper.ActionType.UNSTACK, "PINNAMES",
							pinNames.toString());
			UnitTestDataCaptureHelper
					.createPropertyForUnitTest(pinList, UnitTestDataCaptureHelper.ActionType.UNSTACK, "PIN_LOCATIONS",
							locations.toString());
		}
	}

	public boolean lockObjects(Function<ISchemStackPin, String> lockMsgPrefix,
			ILogicConcurrencyActionContextForErrorReport context)
	{
		Collection<ILogicObject> objectsToLock = getLockableObjects();

		return lockObjects(lockMsgPrefix, context, objectsToLock);
	}

	public boolean lockObjects(Function<ISchemStackPin, String> lockMsgPrefix,
			ILogicConcurrencyActionContextForErrorReport context, Collection<ILogicObject> objectsToLock)
	{
		ILogicDesign design = getModel().getDesign();
		Set<IUID> failedObjects = LogicObjectLockFinder.tryEdit(design, objectsToLock);
		if (!failedObjects.isEmpty()) {
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(design, lockMsgPrefix.apply(m_stackPin), failedObjects,
							message -> getOutputWindow().sendApplicationMessage(message), true, context);
			return false;
		}
		for (ILogicObject logicObject : objectsToLock) {
			logicObject.concurrencyLockableEdited();
		}
		return true;
	}

	private IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	private List<ILogicObject> getLockableObjects()
	{
		IPinList pinList = getPinPlacementController().getAnchor();
		List<ILogicObject> objects = new ArrayList<>();
		objects.add(pinList.getConnectivity());
		IPinList matedPinList = m_matedStackPin != null ? (IPinList) m_matedStackPin.getParent() : null;
		if (matedPinList != null) {
			objects.add(matedPinList.getConnectivity());
		}

		return objects;
	}

	private interface IEditStackExecuter
	{

		void removePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin);

		void deletePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin);

		boolean shouldRemoveConductorFromHighway(chs.cof.logical.cable.IConductor cableCond,
				IHighwaySchematic highwaySchem);

		void removeConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem);

		void addConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem);

		void removeStackedPinConductor(IHighwayConductor cableCond, IHighwaySchematic highwaySchem);

		void movePinsIntoNewStackPin(List<AddPinArgs> pinstoUnstack);

		boolean shouldRemoveStackedPinConductor(IAbstractPin pin);

		void addObjectForRoute(@Nullable IPin schemPin);

		ISegment createConductor(@Nullable IPin newSchemPin, ISchemStackPin stackPin,
				chs.cof.logical.cable.IConductor cableCond,
				List<Point> newSegmentPoints);

		void updatePortGraphics(IDesignWideUsageMgr dwum, chs.cof.logical.cable.IConductor cableCond);

		void moveConductorSegment(@Nullable IPin newSchemPin, ISegment condSegment, IJoint condHWJoint);

		void regeneratePinList(IPinList pinList, ISchemDiagram diagram, @Nullable IPinList matePinList);

		void deleteStackPin(ISchemDiagram diagram, ISchemStackPin stackPin);

		void createShield(@Nullable IPin newSchemPin, chs.cof.logical.cable.IConductor cableCond,
				Map<IShieldConductor, Collection<IPin>> map);
	}

	private class EditStackLockCollector implements IEditStackExecuter
	{

		private Set<ILogicObject> lockables = new HashSet<>();

		public void removePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin)
		{

		}

		@Override
		public void deletePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin)
		{

			Map<chs.cof.logical.cable.IConductor, Integer> conductorConnections = getConductorConnections(matedPin);

			for (chs.cof.logical.cable.IConductor conductor : conductorConnections.keySet()) {
				Integer count = conductorConnections.get(conductor);
				if (count != null && count <= 1) {
					lockables.add(matedPin);
					lockables.add(conductor);
				}
			}
		}

		@NotNull
		private Map<chs.cof.logical.cable.IConductor, Integer> getConductorConnections(IAbstractPin matedPin)
		{
			ILogicDesign design = getModel().getDesign();
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();

			Map<chs.cof.logical.cable.IConductor, Integer> conductorConnections = new HashMap<>();
			List<IDesignSharedUsage> usages = dwum.getUsages(matedPin);
			for (IDesignSharedUsage usage : usages) {
				IDiagramObject object = usage.getDiagramObject();
				if (object instanceof IPin) {
					updateMap(conductorConnections, getConnectedConductors((IPin) object));
				}
				else if (object instanceof ISchemStackPin) {
					ISchemStackPin stackPin1 = (ISchemStackPin) object;
					if (!stackPin1.getConnectedHighways().isEmpty()) {
						updateMap(conductorConnections, matedPin.getConductorsAsSet());
					}
				}
			}
			return conductorConnections;
		}

		@NotNull
		private Set<chs.cof.logical.cable.IConductor> getConnectedConductors(IPin object)
		{
			IPin pin = object;
			Set<chs.cof.logical.cable.IConductor> connectedConductors = new HashSet<>();
			for (IConductor conductor : pin.getConductors()) {
				connectedConductors.add(conductor.getConnectivity());
			}
			return connectedConductors;
		}

		private void updateMap(Map<chs.cof.logical.cable.IConductor, Integer> usageCount,
				Set<chs.cof.logical.cable.IConductor> connectedConductors)
		{
			for (chs.cof.logical.cable.IConductor conductor : connectedConductors) {
				Integer count = usageCount.get(conductor);
				if (count == null) {
					count = 0;
				}
				usageCount.put(conductor, count + 1);
			}
		}

		public boolean shouldRemoveConductorFromHighway(chs.cof.logical.cable.IConductor cableCond,
				IHighwaySchematic highwaySchem)
		{
			int numberOfSegmentsOfConductorConnected =
					getNumberOfSegmentsOfConductorConnected(highwaySchem, cableCond.getUID());
			return numberOfSegmentsOfConductorConnected <= 1;
		}

		public void removeConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			lockables.add(highwaySchem.getConnectivity());
		}

		public void removeStackedPinConductor(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			lockables.add(highwaySchem.getConnectivity());
		}

		@Override
		public void movePinsIntoNewStackPin(List<AddPinArgs> pinstoUnstack)
		{

		}

		@Override
		public void addConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			lockables.add(highwaySchem.getConnectivity());
		}

		public boolean shouldRemoveStackedPinConductor(IAbstractPin pin)
		{
			return HighwayHelper.getNumberOfStackedPinConductorInterfaceWithHighway(pin) == 1;
		}

		public Set<ILogicObject> getLockables()
		{
			return lockables;
		}

		@Override
		public void addObjectForRoute(@Nullable IPin schemPin)
		{

		}

		@Nullable
		@Override
		public ISegment createConductor(IPin newSchemPin, ISchemStackPin stackPin,
				chs.cof.logical.cable.IConductor cableCond,
				List<Point> newSegmentPoints)
		{
			return null;
		}

		@Override
		public void updatePortGraphics(IDesignWideUsageMgr dwum, chs.cof.logical.cable.IConductor cableCond)
		{

		}

		@Override
		public void moveConductorSegment(@Nullable IPin newSchemPin, ISegment condSegment, IJoint condHWJoint)
		{

		}

		@Override
		public void regeneratePinList(IPinList pinList, ISchemDiagram diagram, IPinList matePinList)
		{

		}

		@Override
		public void deleteStackPin(ISchemDiagram diagram, ISchemStackPin stackPin)
		{
			ISchemStackPin connectedStack = ConnectionHelper.getConnectedStackedPin(stackPin);
			for (IAbstractPin pin : stackPin.getAllConnectivity()) {
				deletePinFromStack(pin, stackPin);
				if (connectedStack != null) {
					for (IAbstractPin matedPin : pin.getConnectedPins()) {
						deletePinFromStack(matedPin, connectedStack);
					}
				}
			}
		}

		@Override
		public void createShield(@Nullable IPin newSchemPin, chs.cof.logical.cable.IConductor cableCond,
				Map<IShieldConductor, Collection<IPin>> map)
		{

		}
	}

	private class EditStackExecuter implements IEditStackExecuter
	{

		@Override
		public void removePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin)
		{
			stackPin.removePinFromStack(matedPin);
		}

		@Override
		public void deletePinFromStack(IAbstractPin matedPin, ISchemStackPin stackPin)
		{
			ILogicDesign design = getModel().getDesign();
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			if (dwum.getDesignSharedUsageCount(matedPin) <= 1) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(matedPin);
			}
			stackPin.removePinFromStack(matedPin);
		}

		public boolean shouldRemoveConductorFromHighway(chs.cof.logical.cable.IConductor cableCond,
				IHighwaySchematic highwaySchem)
		{
			int numberOfSegmentsOfConductorConnected =
					getNumberOfSegmentsOfConductorConnected(highwaySchem, cableCond.getUID());
			return numberOfSegmentsOfConductorConnected == 0;
		}

		public void removeConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(highwaySchem);
			if (cableHighway != null) {
				cableHighway.removeConductor(cableCond);
			}
		}

		public void removeStackedPinConductor(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(highwaySchem);
			if (cableHighway != null) {
				cableHighway.removeStackPinConductor(cableCond);
			}
		}

		@Override
		public void movePinsIntoNewStackPin(List<AddPinArgs> pinstoUnstack)
		{
			EditStackPinActionHelper.this.movePinsIntoNewStackPin(pinstoUnstack);
		}

		@Override
		public void addConductorFromHighway(IHighwayConductor cableCond, IHighwaySchematic highwaySchem)
		{
			IGeneralHighway generalHighway = HighwayHelper.toGeneralHighway(highwaySchem);
			if (generalHighway != null) {
				generalHighway.addConductor(cableCond);
			}
		}

		public boolean shouldRemoveStackedPinConductor(IAbstractPin pin)
		{
			return HighwayHelper.getNumberOfStackedPinConductorInterfaceWithHighway(pin) == 0;
		}

		@Override
		public void addObjectForRoute(@Nullable IPin schemPin)
		{

			if (schemPin != null) {
				ConductorRouteAction.getInstance().addPinForRoute(schemPin);
				RegenerateGraphicsAction.getInstance().addObjectForRegenrate(schemPin);
			}
		}

		@Override
		public ISegment createConductor(IPin newSchemPin, ISchemStackPin stackPin,
				chs.cof.logical.cable.IConductor cableCond,
				List<Point> newSegmentPoints)
		{
			ISchemDiagram diagram = DiagramHelper.getDiagram(stackPin);
			assert diagram != null;
			IConductor schemConductor =
					CreateSchemConductorCmd.createConductor(diagram, newSegmentPoints, cableCond, true);
			CreationDeletionHelper.getTheCreationHelper().addCreationObject(schemConductor);

			Collection<ISegment> segments = schemConductor.getObjects(ISegment.class);
			ISegment condSegment = segments.iterator().next();

			newSchemPin.getConnectivity().addConductor(schemConductor.getConnectivity());
			IJoint pinJoint = getJointForPin(newSchemPin);

			if (arePointsEqual(pinJoint, condSegment.getStartPoint())) {
				condSegment.setStartJoint(pinJoint);
			}
			else {
				condSegment.setEndJoint(pinJoint);
			}
			return condSegment;
		}

		private IJoint getJointForPin(IPin newSchemPin)
		{
			IJoint pinJoint = newSchemPin.getJoint();
			if (pinJoint == null) {
				ILocation point = newSchemPin.getAbsLocation();
				pinJoint = FactoryMgr.getSchemFactory().constructNode(
						FactoryMgr.getCommonFactory().createUID(), point.getX(), point.getY());
				newSchemPin.setJoint(pinJoint);
			}
			return pinJoint;
		}

		@Override
		public void updatePortGraphics(IDesignWideUsageMgr dwum, chs.cof.logical.cable.IConductor cableCond)
		{
			List<IDesignSharedUsage> usages = dwum.getUsages(cableCond);
			for (IDesignSharedUsage usage : usages) {
				IDiagramObject diagramObject = usage.getDiagramObject();
				ISchemDiagram diagram = usage.getDiagram();
				if (diagramObject instanceof IConductor && diagram != null && diagram.isEditable()) {
					PortHelper.updatePortGfx((ICompoundObject) diagramObject, diagram.getGrid().getGridSpacing());
				}
			}
		}

		public void moveConductorSegment(@Nullable IPin newSchemPin, ISegment condSegment, IJoint condHWJoint)
		{
			if (newSchemPin != null) {
				condHWJoint.removeAssociation(condSegment);

				IJoint pinJoint = getJointForPin(newSchemPin);

				if (arePointsEqual(condHWJoint, condSegment.getStartPoint())) {
					condSegment.setStartJoint(pinJoint);
				}
				else if (arePointsEqual(condHWJoint, condSegment.getEndPoint())) {
					condSegment.setEndJoint(pinJoint);
				}

				if (condHWJoint.getAssociations().getSize() < 2) {
					deleteRedundentHighwaySegmentsAfterUnstack(condSegment, condHWJoint);
				}
			}
		}

		private void deleteRedundentHighwaySegmentsAfterUnstack(ISegment condSegment, IJoint condHWJoint)
		{
			Set<IHighwaySegment> segments = condHWJoint.getAssociations(IHighwaySegment.class);
			if (segments.size() == 1) {
				IHighwaySegment seg = segments.iterator().next();
				Collection<IUIDObject> toDelete = new ArrayList<IUIDObject>();
				toDelete.addAll(getRedundentSegments(seg,
						seg.getStartJoint() == condHWJoint ? seg.getEndJoint() : seg.getStartJoint()));
				if (!toDelete.isEmpty()) {
					ISchemDiagram diagram = DiagramHelper.getDiagram(condSegment);
					assert diagram != null;
					DeleteHelper.getInstance().delete(diagram, toDelete, true);
				}
			}
		}

		@Override
		public void regeneratePinList(IPinList pinList, ISchemDiagram diagram, IPinList matePinList)
		{
			if (pinList != null) {
				regenerateGraphics(pinList);
			}
			if (matePinList != null) {
				regenerateGraphics(matePinList);
			}

			if (pinList != null && PinListHelper.isHarnessFootprintedAndAllowAutoCreation(pinList)) {
				GenerateHarnessConnActionHelper generator = new GenerateHarnessConnActionHelper(diagram);
				generator.generateHarnessConnectorsForPinlist(pinList);
			}

			// Are these calls ok to regenerate sec reps ?
			addObjectForRefresh(pinList);
			addObjectForRefresh(matePinList);
		}

		public void deleteStackPin(ISchemDiagram diagram, ISchemStackPin stackPin)
		{
			Collection<IUIDObject> toDelete = new ArrayList<IUIDObject>();
			toDelete.add(stackPin);
			DeleteHelper.getInstance().delete(diagram, toDelete, true);
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}

		public void createShield(@Nullable IPin newSchemPin, chs.cof.logical.cable.IConductor cableCond,
				Map<IShieldConductor, Collection<IPin>> map)
		{
			if (newSchemPin != null) {
				IMulticore multicore = cableCond.getMulticore();
				assert multicore != null;
				ISchemDiagram diagram = newSchemPin.getDiagram();
				ShieldTerminationUtils.drawSchemShields(multicore, diagram, map, Collections.emptyList());
			}
		}
	}
}