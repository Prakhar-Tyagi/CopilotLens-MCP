/*
 * Copyright 2010-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.RouteUnrouteActionHelper;
import chs.cof.draw.IConnectedPath;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.ILine;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.services.dynamicgfx.DynamicEndSnap;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.GridHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SegmentHelper;
import chs.utility.logic.LogicConnectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RouteIntoHighwayAction extends AbstractRouteUnRouteHighwayAction
{

	private Model m_model = null;
	private ICapletController m_controller;

	public RouteIntoHighwayAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_controller = controller;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean bEditOk = true;
		if (successful) {
			SelectSet preSelections = m_controller.getSelectMgr().getPreSelections();
			IHighwaySchematic highwaySchem = getSelectedHighway(preSelections);
			if (highwaySchem == null) {
				assert false : "Highway should not be null here";
				return false;
			}

			if (!lockObjects(Collections.singleton(highwaySchem.getConnectivity()))) {
				return false;
			}
			bEditOk = routeIntoHighway();
		}
		return bEditOk;
	}

	private boolean lockObjects(Collection<ILogicObject> logicObjects)
	{
		Collection<IUID> lockFailrues = LogicObjectLockFinder.tryEdit(m_model.getDesign(), logicObjects);
		if (!lockFailrues.isEmpty()) {
			LogicConcurrencyLogger.getInstance().reportLockFailure(m_model.getDesign(), getLockMessagePrefix(),
					lockFailrues, message -> reportLockFailures(message));
			return false;
		}
		return true;
	}

	private void reportLockFailures(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}

	private boolean routeIntoHighway()
	{

		// we take a copy of the preselections at the start because we ARE going to add to the contents, both here
		// and in the associated object selection. A new select set will have no listeners and will prevent the UImanager
		// being updated every time (multiple time) an object is added.
		// Once all the additional objects have been added to the new set, we simply set the preselections in the select
		// manager to our new content. As a result ONE notification is issued as opposed to hundreds. Funnily enough, the
		// speed of the action increases substantially too. You can thank me later.
		SelectSet preSelections = new SelectSet();

		preSelections.add(getPreSelections());

		ISchemDiagram diagram = m_model.getDiagram();

		// Contains schem segments
		Map<IConductor, List<ISegment>> selectedSegmentMap = getSelectedSegments(diagram, preSelections);
		IHighwaySchematic highwaySchem = getSelectedHighway(preSelections);
		if (highwaySchem == null) {
			assert false : "Highway should not be null here";
			return false;
		}
		removeAlreadyInterfaced(selectedSegmentMap, highwaySchem);

		removeShieldConductors(selectedSegmentMap);

		Map<IConductor, Map<ISegment, ILocation>> segemntsToRoute = new HashMap<>();
		Map<IConductor, Collection<IUIDObject>> segemntsToDeleted = new HashMap<>();
		Set<ILogicObject> objectsToLock = new HashSet<>();
		BiConsumer<Boolean, ILogicObject> objectLocker = ConductorLocker(objectsToLock);
		for (IConductor conductor : getConductorsToProcess(selectedSegmentMap.keySet())) {

			List<ISegment> selectedSegList = selectedSegmentMap.get(conductor);

			Collection<IUIDObject> segmentsTobeDeleted = new ArrayList<IUIDObject>();
			Set<ISegment> segments = conductor.getSegmentsOfType(ISegment.class);

			Consumer<Boolean> objLocker = shouldLock -> objectLocker.accept(shouldLock, conductor.getConnectivity());
			Map<ISegment, ILocation> segmentsToInterface =
					populateSegmentsToInterface(selectedSegList, segmentsTobeDeleted, objLocker);
			if (segments.size() == segmentsTobeDeleted.size()) { //If all the segments of the conductor are selected
				extractSegmentToConnect(segments, segmentsTobeDeleted, highwaySchem, segmentsToInterface);
			}
			segemntsToRoute.put(conductor, segmentsToInterface);

			prepareForConnection(segmentsToInterface, highwaySchem, segmentsTobeDeleted);

			// Prevent conductor deletion (dts0100681506)
			ensureConductorDoesnotGetsDeleted(highwaySchem, conductor, segmentsTobeDeleted, segmentsToInterface);

			objectsToLock.addAll(getConnectedHighways(conductor));

			segemntsToDeleted.put(conductor, segmentsTobeDeleted);
		}

//		Collection<IConductor> shields = getShieldsConnectedToMulticores(selectedSegmentMap.keySet());
//		objectsToLock.addAll(getLockableObjectsForShields(shields));

		if (!lockObjects(objectsToLock)) {
			return false;
		}

//		if (!lockRequiredDiagrams(diagram, selectedSegmentMap.keySet())) {
//			return false;
//		}

		for (IConductor conductor : getConductorsToProcess(segemntsToRoute.keySet())) {

			deleteSegments(diagram, segemntsToDeleted.get(conductor));

			Map<ISegment, ILocation> segmentsToInterface = segemntsToRoute.get(conductor);

			conductor.makeContinuous();

			//Sorts segments of conductors to be routed based on its location
			routeSegmentsIntoHighway(preSelections, highwaySchem, segmentsToInterface);
		}

		// set the preselections in the select manager. ONE selection changed notification!
		m_controller.getSelectMgr().getPreSelections().setSelections(preSelections);

		return true;
	}

	@NotNull protected Collection<IConductor> getConductorsToProcess(Set<IConductor> conductors)
	{
		return conductors;
	}

	private Collection<IConductor> getShieldsConnectedToMulticores(Collection<IConductor> conductors)
	{
		return conductors.stream()
				.filter(conductor1 -> isShieldConductor(conductor1))
				.collect(Collectors.toSet());
	}

	private boolean isShieldConductor(IConductor conductor)
	{
		chs.cof.logical.cable.IConductor connectivity = conductor.getConnectivity();
		return connectivity instanceof IShieldConductor;
	}

//	private Collection<ILogicObject> getLockableObjectsForShields(Collection<IConductor> conductors)
//	{
//		Collection<ILogicObject> objects = new HashSet<>();
//		for (IConductor conductor : conductors) {
//			for (IPin pin : conductor.getPins()) {
//				objects.add(pin.getConnectivity());
//			}
//		}
//		return objects;
//	}

//	private boolean lockRequiredDiagrams(ISchemDiagram diagram, Set<IConductor> conductors)
//	{
//		Set<chs.cof.logical.cable.IConductor> innerCores = getInnercoreConductors(conductors);
//		if (!innerCores.isEmpty()) {
//			ILogicDesign design = diagram.getDesign();
//			assert design != null;
//			if (!new LogicDiagramLockFinder().lockAffectedDiagrams(design,
//					error -> reportLockFailures(getLockMessagePrefix() + "-" + error), innerCores)) {
//				return false;
//			}
//		}
//		return true;
//	}

//	private Set<chs.cof.logical.cable.IConductor> getInnercoreConductors(Set<IConductor> conductors)
//	{
//		Set<chs.cof.logical.cable.IConductor> innerCores = new HashSet<>();
//		for (IConductor cond1 : conductors) {
//			if (isShieldConductor(cond1)) {
//				innerCores.add(cond1.getConnectivity());
//			}
//		}
//		return innerCores;
//	}

	private Set<IHighway> getConnectedHighways(IConductor conductor)
	{
		return conductor.connectedHighways().stream()
				.map(highwaySchematic -> highwaySchematic.getConnectivity())
				.collect(Collectors.toSet());
	}

	@NotNull private BiConsumer<Boolean, ILogicObject> ConductorLocker(final Set<ILogicObject> objectsToLock)
	{
		return new BiConsumer<Boolean, ILogicObject>()
		{
			@Override public void accept(Boolean t, ILogicObject u)
			{
				if (t) {
					objectsToLock.add(u);
				}
			}
		};
	}

	private void ensureConductorDoesnotGetsDeleted(IHighwaySchematic highwaySchem, IConductor conductor,
			Collection<IUIDObject> segmentsTobeDeleted, Map<ISegment, ILocation> interfaceLocations)
	{
		Collection<ISegment> condSegments = conductor.getObjects(ISegment.class);
		if (interfaceLocations.isEmpty() && condSegments.size() == segmentsTobeDeleted.size()) {
			ISegment nonOverlappingSeg = getNonOverlappingSegment(conductor, highwaySchem);
			if (nonOverlappingSeg != null) {
				segmentsTobeDeleted.remove(nonOverlappingSeg);
				interfaceLocations.put(nonOverlappingSeg, null);
			}
			else {
				ISegment segment = condSegments.iterator().next();
				segmentsTobeDeleted.remove(segment);
				interfaceLocations.put(segment, null);
			}
		}
	}

	private void routeSegmentsIntoHighway(SelectSet preSelections, IHighwaySchematic highwaySchem,
			Map<ISegment, ILocation> segmentsToInterface)
	{
		Set<ISegment> segmentsToAutoRoute = new HashSet<ISegment>();
		for (ISegment segment : getSortedSegments(segmentsToInterface)) {
			Pair<ISegment, ILocation> segToInterface =
					new Pair<ISegment, ILocation>(segment, segmentsToInterface.get(segment));
			routeSegmentIntoHighway(preSelections, highwaySchem, segToInterface, segmentsToAutoRoute);
		}
		autoRouteSegments(segmentsToAutoRoute);
	}

	@NotNull private String getLockMessagePrefix()
	{
		return ResourceMgr.getString(RouteIntoHighwayAction.class, "LogicAction.error.unableToLock", getDisplayName());
	}

	private void autoRouteSegments(Set<ISegment> segmentsToAutoRoute)
	{
		for (ISegment segment : segmentsToAutoRoute) {
			IConductor conductorToRoute = segment.getConductor();

			if (conductorToRoute != null) {
				ConductorRouteAction.getInstance().addConductorForRoute(conductorToRoute);
			}
			else {
				StringBuilder builder = new StringBuilder();
				builder.append("conductor should not not be for segment ");
				builder.append(segment);
				assert false : builder.toString();
			}
		}
	}

	private void deleteSegments(ISchemDiagram diagram, Collection<IUIDObject> segmentsTobeDeleted)
	{
		if (segmentsTobeDeleted != null && !segmentsTobeDeleted.isEmpty()) {
			DeleteHelper.getInstance().delete(diagram, segmentsTobeDeleted, true);
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}
	}

	private List<ISegment> getSortedSegments(Map<ISegment, ILocation> connectionSegmentMap)
	{
		List<ISegment> segmentsToInterface = new ArrayList<ISegment>(connectionSegmentMap.keySet());
		Collections.sort(segmentsToInterface, new Comparator<ISegment>()
		{
			public int compare(ISegment o1, ISegment o2)
			{
				IBaseSegment mostBottemLeft = SegmentHelper.mostBottomLeft(o1, o2);
				if (mostBottemLeft == o1) {
					return 1;
				}
				return -1;
			}
		});
		return segmentsToInterface;
	}

	private void routeSegmentIntoHighway(SelectSet preSelections, IHighwaySchematic highwaySchem,
			Pair<ISegment, ILocation> segmentToInterface, Set<ISegment> segmentsToAutoRoute)
	{
		ISegment connectionSegment = segmentToInterface.getFirst();
		Set<IConductor> conductorsInterfaced = highwaySchem.getConductors();
		if (conductorsInterfaced.contains(connectionSegment.getConductor())) {
			// Conductor is already interfaced with highway, so do not interface this segment
			return;
		}
		boolean fromStartPoint = false;
		ILocation fromNode = segmentToInterface.getSecond();
		Map<ILocation, IConnectedPath> closestPath = new HashMap<ILocation, IConnectedPath>();
		ILocation pointOnHighway = null;
		if (fromNode != null) {
			fromStartPoint = isSameLocation(fromNode, connectionSegment.getStartPoint());
			ILocation fromPoint = fromStartPoint ? connectionSegment.getStartPoint() : connectionSegment.getEndPoint();
			pointOnHighway = getClosestPointOnHighway(fromPoint, highwaySchem, closestPath, true);
		}
		else {
			ILocation location = connectionSegment.getStartPoint();
			ILocation pointOnHighwayFromStart = getClosestPointOnHighway(location, highwaySchem, closestPath, true);

			Double distanceFromStartPoint = pointOnHighwayFromStart == null ? null :
					GfxUtils.getDesitanceBetweenPoints(location, pointOnHighwayFromStart);

			location = connectionSegment.getEndPoint();
			ILocation pointOnHighwayFromEnd = getClosestPointOnHighway(location, highwaySchem, closestPath, true);

			Double distanceFromEndPoint = pointOnHighwayFromEnd == null ? null :
					GfxUtils.getDesitanceBetweenPoints(location, pointOnHighwayFromEnd);

			if (distanceFromEndPoint != null && distanceFromStartPoint != null) {
				if (distanceFromStartPoint < GfxUtils.EPSILON_INT_DBL_CVR_ERR &&
						distanceFromEndPoint < GfxUtils.EPSILON_INT_DBL_CVR_ERR &&
						!isSegmentOverlappingHighway(connectionSegment, highwaySchem)) {
					pointOnHighway = pointOnHighwayFromStart;
					fromStartPoint = false;
				}
				else if (distanceFromStartPoint < GfxUtils.EPSILON_INT_DBL_CVR_ERR) {
					pointOnHighway = pointOnHighwayFromEnd;
					fromStartPoint = false;
				}
				else if (distanceFromEndPoint < GfxUtils.EPSILON_INT_DBL_CVR_ERR) {
					pointOnHighway = pointOnHighwayFromStart;
					fromStartPoint = true;
				}
				else if (distanceFromStartPoint > distanceFromEndPoint) {
					pointOnHighway = pointOnHighwayFromEnd;
					fromStartPoint = false;
				}
				else {
					pointOnHighway = pointOnHighwayFromStart;
					fromStartPoint = true;
				}
			}
			else {
				if (distanceFromStartPoint != null) {
					fromStartPoint = true;
					pointOnHighway = pointOnHighwayFromStart;
				}
				else if(distanceFromEndPoint != null){
					fromStartPoint = false;
					pointOnHighway = pointOnHighwayFromEnd;
				}
			}
		}

		if (pointOnHighway != null) {
			IHighwaySegment closesHighwaySegment = (IHighwaySegment) closestPath.get(pointOnHighway);

			ISchemStackPin overlappingStack =
					getStackPinIfOverlappingWithLocation(closesHighwaySegment, pointOnHighway);

			if (overlappingStack != null) {
				pointOnHighway = getValidLocationForInterfase(closesHighwaySegment, overlappingStack.getAbsLocation());
			}
			if (pointOnHighway != null) {

				Collection<Pair<IDynamicSnap, Integer>> snaps = getSnapOnHighway(pointOnHighway, closesHighwaySegment);

				IDynamicGfx dynamicGfx = createDynamicGfx(connectionSegment, !fromStartPoint, pointOnHighway);

				if (dynamicGfx != null) {
					IDynamicGfxMediator connectionSegment1 = (IDynamicGfxMediator) connectionSegment;
					connectionSegment1.applyEdits(m_model, dynamicGfx, true, null);
					//Note:Some segments which are overlapping with highway segment can become zero legth and may be deleted
					if (connectSegmentWithSnaps(snaps, connectionSegment1)) {
						conductorsInterfaced.add(connectionSegment.getConductor());
						segmentsToAutoRoute.add(connectionSegment);
						if (connectionSegment.isSelectable() && !preSelections.contains(connectionSegment.getUID())) {
							preSelections.add(new Selection(connectionSegment));
						}
					}
				}
			}
		}
	}

	@Nullable
	private IDynamicGfx createDynamicGfx(ISegment connectionSegment, boolean startPoint, ILocation pointOnHighway)
	{
		ILocation point = createLocation(connectionSegment, startPoint);
		setLocation(connectionSegment, pointOnHighway, startPoint);

		IDynamicGfx dynamicGfx =
				((IDynamicGfxMediator) connectionSegment).createDynamic(getDynamicFactory(), null, true, true);

		setLocation(connectionSegment, point, startPoint);
		return dynamicGfx;
	}

	private void setLocation(ISegment connectionSegment, ILocation location, boolean startPoint)
	{
		if (startPoint) {
			connectionSegment.setStartPoint(location);
		}
		else {
			connectionSegment.setEndPoint(location);
		}
	}

	private ILocation createLocation(ISegment connectionSegment, boolean startPoint)
	{
		ILocation point = new Location();
		if (startPoint) {
			point.setX(connectionSegment.getStartPoint().getX());
			point.setY(connectionSegment.getStartPoint().getY());
		}
		else {
			point.setX(connectionSegment.getEndPoint().getX());
			point.setY(connectionSegment.getEndPoint().getY());
		}
		return point;
	}

	private Collection<Pair<IDynamicSnap, Integer>> getSnapOnHighway(ILocation location, IHighwaySegment highwaySegment)
	{
		Collection<Pair<IDynamicSnap, Integer>> snaps = new ArrayList<Pair<IDynamicSnap, Integer>>();
		DynamicEndSnap des = new DynamicEndSnap(new Point(location.getX(), location.getY()), null);
		des.addMediator((IDynamicGfxMediator) highwaySegment);
		snaps.add(new Pair<IDynamicSnap, Integer>(des, 0));
		return snaps;
	}

	private Map<IConductor, List<ISegment>> getSelectedSegments(ISchemDiagram diagram, SelectSet preSelections)
	{
		Map<IConductor, List<ISegment>> selectedSegmentMap = new HashMap<IConductor, List<ISegment>>();
		for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof ISegment) {
				IOutputWindow output = CAFUtils.getInstance().getOutputWindow();
				Set<IConductor> processedConductors = new HashSet<IConductor>();
				if (!checkIfValidConductor(obj, diagram, output, processedConductors)) {
					continue;
				}
				ISegment selectedSeg = (ISegment) obj;
				List<ISegment> segments = selectedSegmentMap.get(selectedSeg.getConductor());
				if (segments == null) {
					segments = new ArrayList<ISegment>();
					selectedSegmentMap.put(selectedSeg.getConductor(), segments);
				}
				if (!segments.contains(selectedSeg)) {
					segments.add((ISegment) obj);
				}
			}
		}
		return selectedSegmentMap;
	}

	@Nullable private IHighwaySchematic getSelectedHighway(SelectSet preSelections)
	{
		IHighwaySchematic highwaySchem = null;

		for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof IHighwaySegment) {
				IHighwaySegment highwaySegment = (IHighwaySegment) obj;
				assert highwaySchem == null || highwaySchem == highwaySegment.getHighway();
				highwaySchem = highwaySegment.getHighway();
			}
		}
		return highwaySchem;
	}

	private void prepareForConnection(Map<ISegment, ILocation> connectionSegmentMap, IHighwaySchematic highwaySchem,
			Collection<IUIDObject> toDelete)
	{
		Set<ISegment> deletedSegments = new HashSet<ISegment>();
		Map<ISegment, ILocation> newSegmentMap = new HashMap<ISegment, ILocation>();
		for (ISegment segment : connectionSegmentMap.keySet()) {
			ILocation location = connectionSegmentMap.get(segment);
			if (location != null) {
				disConnectLocation(segment, location);
			}
			Map<ISegment, ILocation> newSegMap =
					getNonOverLappingConductorSeg(segment, location, highwaySchem, toDelete, deletedSegments);
			if (newSegMap != null) {
				for (ISegment seg : newSegMap.keySet()) {
					if (seg != segment) {
						newSegmentMap.put(seg, newSegMap.get(seg));
					}
				}
			}
		}

		for (ISegment deletedSeg : deletedSegments) {
			if (connectionSegmentMap.containsKey(deletedSeg)) {
				connectionSegmentMap.remove(deletedSeg);
			}
		}

		for (ISegment newSegment : newSegmentMap.keySet()) {
			ILocation location = newSegmentMap.get(newSegment);
			connectionSegmentMap.put(newSegment, location);
			disConnectLocation(newSegment, location);
			toDelete.remove(newSegment);
		}
	}

	private void disConnectLocation(ISegment segment, ILocation location)
	{
		if (segment.getStartPoint() == location) {
			IJoint endJoint = segment.getEndNode();
			if (endJoint != null) {
				segment.eraseNode(endJoint);
			}
		}
		else {
			IJoint startJoint = segment.getStartNode();
			if (startJoint != null) {
				segment.eraseNode(startJoint);
			}
		}
	}

	@Nullable private ISegment getNonOverlappingSegment(IConductor conductor, IHighwaySchematic highwaySchem)
	{
		for (ISegment segment : conductor.getObjects(ISegment.class)) {
			if (!isSegmentOverlappingHighway(segment, highwaySchem)) {
				return segment;
			}
		}
		return null;
	}

	@Nullable
	private Map<ISegment, ILocation> getNonOverLappingConductorSeg(ISegment segment, @Nullable ILocation jointLocation,
			IHighwaySchematic highwaySchem, Collection<IUIDObject> toDelete, Set<ISegment> deletedSegs)
	{
		if (isSegmentOverlappingHighway(segment, highwaySchem)) {
			toDelete.add(segment);
			deletedSegs.add(segment);
			Set<ISegment> otherConnectedSegments;
			if (segment.getStartPoint() == jointLocation) {
				otherConnectedSegments = getOtherConnectedSegments(segment, segment.getStartNode());
				for (ISegment seg : otherConnectedSegments) {
					ILocation location = getOtherPointOnSegment(seg, jointLocation);
					Map<ISegment, ILocation> newSeg =
							getNonOverLappingConductorSeg(seg, location, highwaySchem, toDelete, deletedSegs);
					if (newSeg != null) {
						return newSeg;
					}
				}
			}
			else if (segment.getEndPoint() == jointLocation) {
				otherConnectedSegments = getOtherConnectedSegments(segment, segment.getEndNode());
				for (ISegment seg : otherConnectedSegments) {
					ILocation location = getOtherPointOnSegment(seg, jointLocation);
					Map<ISegment, ILocation> newSeg =
							getNonOverLappingConductorSeg(seg, location, highwaySchem, toDelete, deletedSegs);
					if (newSeg != null) {
						return newSeg;
					}
				}
			}
		}
		else {
			Map<ISegment, ILocation> segMap = new HashMap<ISegment, ILocation>(1);
			segMap.put(segment, jointLocation);
			return segMap;
		}
		return null;
	}

	private boolean isLogicSegmentCompatibleForRouteInto(ILogicSegment line1)
	{
		ILocation l1P1 = line1.getStartPoint();
		ILocation l1P2 = line1.getEndPoint();

		return !l1P1.equals(l1P2);
	}

	private boolean isSegmentOverlappingHighway(ISegment segment, IHighwaySchematic highwaySchem)
	{
		List<IConnectedPath> paths = new ArrayList<IConnectedPath>();
		boolean overlapping = false;
		if (!isLogicSegmentCompatibleForRouteInto(segment)) {
			return false;
		}
		for (IGfxObjectIterator highwaySegItr = highwaySchem.getObjects(); highwaySegItr.hasNext(); ) {
			IGfxObject highway = highwaySegItr.getNext();
			if (highway instanceof IHighwaySegment) {
				if (isLogicSegmentCompatibleForRouteInto((IHighwaySegment) highway)) {
					paths.add((IConnectedPath) highway);
					if (GfxUtils.lineSegmentsOverlap((ILine) highway, segment)) {
						overlapping = true;
					}
				}
			}
		}
		return overlapping && GfxUtils.isPointOnPath(paths, segment.getStartPoint()) &&
				GfxUtils.isPointOnPath(paths, segment.getEndPoint());
	}

	private Map<ISegment, ILocation> populateSegmentsToInterface(List<ISegment> selectedSegList,
			Collection<IUIDObject> toDelete, Consumer<Boolean> shouldLockConductor)
	{
		Map<ISegment, ILocation> connectionSegmentMap = new HashMap<ISegment, ILocation>();
		for (ISegment segment : selectedSegList) {

			RouteUnrouteActionHelper.SegmentStatusFinder segStatusFinder
					= RouteUnrouteActionHelper.createSegmentStatusFinder(segment, selectedSegList);

			IObjectFilter<Object> filter = obj -> obj instanceof ISegment segT &&
					segT.getConductor() == segment.getConductor();

			if (segStatusFinder.bothEndsConnectedToOtherObjects()) {
				connectEndpointsToOtherObjects(connectionSegmentMap, segment);
				shouldLockConductor.accept(true);
			}
			else if (segStatusFinder.isStartConnectedOtherObject()) {
				if (segStatusFinder.isEndConnectedToSameOwner()) {
					connectStartNodeToOtherObject(selectedSegList, connectionSegmentMap, segment, filter);
				}
				else {
					connectionSegmentMap.put(segment, segment.getStartPoint());
				}
			}
			else if (segStatusFinder.isEndConnectedOtherObject()) {
				if (segStatusFinder.isStartConnectedToSameOwner()) {
					connectEndNodeToOtherObject(selectedSegList, connectionSegmentMap, segment, filter);
				}
				else {
					connectionSegmentMap.put(segment, segment.getEndPoint());
				}
			}
			if (!segStatusFinder.isStartConnectedOtherObject() && !segStatusFinder.isEndConnectedOtherObject()) {
				if ((segStatusFinder.isStartConnectedToSameOwner() || segStatusFinder.isEndConnectedToSameOwner())) {
					toDelete.add(segment);
					if (connectionSegmentMap.containsKey(segment)) {
						connectionSegmentMap.remove(segment);
					}
					selectNeighborSegments(selectedSegList, connectionSegmentMap, segment, true);
					selectNeighborSegments(selectedSegList, connectionSegmentMap, segment, false);
				}
				else {
					connectionSegmentMap.put(segment, null);
				}
			}
		}
		return connectionSegmentMap;
	}

	private void selectNeighborSegments(List<ISegment> segmentList, Map<ISegment, ILocation> connectionSegmentMap,
			ISegment segment, boolean startNode)
	{

		IJoint node = startNode ? segment.getStartNode() : segment.getEndNode();
		Set<ISegment> neighborsSegs = getOtherConnectedSegments(segment, node);
		Set<ISegment> nonSelectedNeighborSegs = new HashSet<ISegment>();
		for (ISegment segment1 : neighborsSegs) {
			if (!segmentList.contains(segment1)) {
				nonSelectedNeighborSegs.add(segment1);
			}
		}
		if (nonSelectedNeighborSegs.size() == 1) {
			ISegment rightSeg = nonSelectedNeighborSegs.iterator().next();
			ILocation point = startNode ? segment.getStartPoint() : segment.getEndPoint();
			ILocation location = getOtherPointOnSegment(rightSeg, point);
			assert location != null;
			connectionSegmentMap.put(rightSeg, location);
		}
	}

	private void connectEndNodeToOtherObject(List<ISegment> selectedSegList,
			Map<ISegment, ILocation> connectionSegmentMap, ISegment segment, IObjectFilter<Object> filter)
	{
		IJoint startNode = segment.getStartNode();
		connectionSegmentMap.put(segment, segment.getEndPoint());

		Set<ISegment> segments = startNode.getAssociations(filter);
		if (segments.size() > 2) {
			//startNode.removeAssociation(segment);
			//segment.eraseNode(startNode);
		}
		else {
			ISegment otherSegment = null;
			for (ISegment seg : segments) {
				if (seg != segment) {
					otherSegment = seg;
				}
			}
			assert otherSegment != null;
			//startNode.removeAssociation(segment);
			//segment.eraseNode(startNode);
			if (!selectedSegList.contains(otherSegment)) {
				if (otherSegment.getEndNode() == startNode) {
					connectionSegmentMap.put(otherSegment, otherSegment.getStartPoint());
				}
				else if (otherSegment.getStartNode() == startNode) {
					connectionSegmentMap.put(otherSegment, otherSegment.getEndPoint());
				}
				//startNode.removeAssociation(otherSegment);
				//otherSegment.eraseNode(startNode);
			}
		}
	}

	private void connectStartNodeToOtherObject(List<ISegment> selectedSegList,
			Map<ISegment, ILocation> connectionSegmentMap, ISegment segment, IObjectFilter<Object> filter)
	{
		IJoint endNode = segment.getEndNode();
		connectionSegmentMap.put(segment, segment.getStartPoint());

		Set<ISegment> segments = endNode.getAssociations(filter);
		if (segments.size() > 2) {
			//endNode.removeAssociation(segment);
			//segment.eraseNode(endNode);
		}
		else {
			ISegment otherSegment = null;
			for (ISegment seg : segments) {
				if (seg != segment) {
					otherSegment = seg;
				}
			}
			assert otherSegment != null;
			//endNode.removeAssociation(segment);
			//segment.eraseNode(endNode);
			if (!selectedSegList.contains(otherSegment)) {
				if (otherSegment.getEndNode() == endNode) {
					connectionSegmentMap.put(otherSegment, otherSegment.getStartPoint());
				}
				else if (otherSegment.getStartNode() == endNode) {
					connectionSegmentMap.put(otherSegment, otherSegment.getEndPoint());
				}
				//endNode.removeAssociation(otherSegment);
				//otherSegment.eraseNode(endNode);
			}
		}
	}

	private void connectEndpointsToOtherObjects(Map<ISegment, ILocation> connectionSegmentMap, ISegment segment)
	{
		ILocation startPoint = segment.getStartPoint();
		ILocation endPoint = segment.getEndPoint();
		ILocation midPoint = getMidPoint(startPoint, endPoint);
		ISegment newSegment = (ISegment) LogicConnectionUtils.disconnectSegment(segment, midPoint);
		if (segment.getStartNode() != null) {
			connectionSegmentMap.put(segment, segment.getStartPoint());
		}
		if (segment.getEndNode() != null) {
			connectionSegmentMap.put(segment, segment.getEndPoint());
		}

		assert newSegment != null;
		if (newSegment.getStartNode() != null) {
			connectionSegmentMap.put(newSegment, newSegment.getStartPoint());
		}
		if (newSegment.getEndNode() != null) {
			connectionSegmentMap.put(newSegment, newSegment.getEndPoint());
		}
	}

	@NotNull private ILocation getMidPoint(ILocation startPoint, ILocation endPoint)
	{
		return new Location((startPoint.getX() + endPoint.getX()) / 2,
				(startPoint.getY() + endPoint.getY()) / 2);
	}

	private void extractSegmentToConnect(Set<ISegment> conductorSegments, Collection<IUIDObject> toDelete,
			IHighwaySchematic highwaySchem, Map<ISegment, ILocation> connectionSegmentMap)
	{
		ILocation shortestLocation = null;
		ISegment shotestSegment = null;
		double shortestdistance = -1;
		for (ISegment segment : conductorSegments) {
			if (toDelete.contains(segment)) {
				Map<ILocation, IConnectedPath> closestPath = new HashMap<ILocation, IConnectedPath>();
				ILocation location = segment.getStartPoint();
				ILocation pointOnHighway = getClosestPointOnHighway(location, highwaySchem, closestPath, true);

				Double sdistance =
						pointOnHighway == null ? null : GfxUtils.getDesitanceBetweenPoints(location, pointOnHighway);
				ILocation sLocation = sdistance != null ? location : null;

				location = segment.getEndPoint();
				pointOnHighway = getClosestPointOnHighway(location, highwaySchem, closestPath, true);

				Double distance =
						pointOnHighway == null ? null : GfxUtils.getDesitanceBetweenPoints(location, pointOnHighway);
				if (distance != null && sdistance != null) {
					if (distance > 0 && distance < sdistance) {
						sdistance = distance;
						sLocation = location;
					}
				}
				else if (distance != null) {
					sLocation = location;
					sdistance = distance;
				}

				// If segment point overlapping with highway segment
				if (sdistance != null && distance != null) {
					if ((!(sdistance > 0) && !(sdistance < 0)) || (!(distance > 0) && !(distance < 0))) {
						if (sdistance > 0) {
							shortestLocation = sLocation;
							shotestSegment = segment;
							break;
						}
						else if (distance > 0) {
							shortestLocation = location;
							shotestSegment = segment;
							break;
						}
					}

					if (shortestdistance < 0 || sdistance < shortestdistance) {
						shortestdistance = sdistance;
						shortestLocation = sLocation;
						shotestSegment = segment;
					}
				}
			}
		}
		if (shotestSegment != null) {
			toDelete.remove(shotestSegment);
			connectionSegmentMap.put(shotestSegment, shortestLocation);
		}
	}

	@NotNull private IDynamicGfxFactory getDynamicFactory()
	{
		if (m_model != null) {
			IDynamicGfxService dynService = m_model.getDynamicGfxService();
			return dynService.getFactory();
		}
		assert false : "Model should not be null here";
		return null;
	}

	private boolean isSameLocation(ILocation location1, ILocation location2)
	{
		return (location1.getX() == location2.getX() && location1.getY() == location2.getY());
	}

	@Nullable private ILocation getOtherPointOnSegment(ISegment segment, ILocation location)
	{
		ILocation tempLocation = segment.getStartPoint();
		if (tempLocation.getX() == location.getX() && tempLocation.getY() == location.getY()) {
			return segment.getEndPoint();
		}
		tempLocation = segment.getEndPoint();
		if (tempLocation.getX() == location.getX() && tempLocation.getY() == location.getY()) {
			return segment.getStartPoint();
		}
		return null;
	}

	private Set<ISegment> getOtherConnectedSegments(ISegment segment, IJoint connectedNode)
	{
		Set<ISegment> connectedSegments = new HashSet<ISegment>();
		if (connectedNode != null) {
			IDiagramObjectIterator diagramObjIterator = connectedNode.getAssociations();
			for (IDiagramObject digramObject : diagramObjIterator) {
				if (digramObject != segment && digramObject instanceof ISegment) {
					connectedSegments.add((ISegment) digramObject);
				}
			}
		}
		return connectedSegments;
	}

	private boolean checkIfValidConductor(IUIDObject obj, ISchemDiagram diagram, IOutputWindow output,
			Set<IConductor> processedConductors)
	{
		// If the selection is not part of current diagram, we can't route it,
		if (DiagramHelper.getDiagram((IDiagramObject) obj) != diagram) {
			if (output != null) {
				IConductor conductor = null;
				if (obj instanceof IConductor) {
					conductor = (IConductor) obj;
				}
				else if (obj instanceof ISegment) {
					conductor = ((ISegment) obj).getConductor();
				}
				if (conductor != null && processedConductors.add(conductor)) {

					output.sendApplicationMessage(
							ResourceMgr.getString(RouteIntoHighwayAction.class, "Invalid conductor",
									conductor.getConnectivity().getName()));
				}
			}
			return false;
		}
		return true;
	}

	@Nullable private ILocation getClosestPointOnHighway(ILocation fromLocation, IHighwaySchematic highwaySchem,
			Map<ILocation, IConnectedPath> closedPathMap, boolean ignoreZeroLengthSegents)
	{

		List<IConnectedPath> paths = new ArrayList<IConnectedPath>();
		Collection<IHighwaySegment> highwaySegments = highwaySchem.getSegmentsOfType(IHighwaySegment.class);
		if (ignoreZeroLengthSegents) {
			highwaySegments =
					highwaySegments.stream().filter(aSeggment -> isLogicSegmentCompatibleForRouteInto(aSeggment))
							.collect(
									Collectors.toList());
		}
		paths.addAll(highwaySegments);

		ILocation pointOnHighway = GfxUtils.getClosestPointOnPath(paths, fromLocation, closedPathMap);

		if (pointOnHighway != null) {

			IGrid grid = GridHelper.getGrid(highwaySchem);
			assert grid != null;
			int left = grid.snap(pointOnHighway.getX());
			int right = grid.snap(pointOnHighway.getY());
			pointOnHighway.setX(left);
			pointOnHighway.setY(right);
			return pointOnHighway;
		}
		return null;
	}

	@Nullable
	private ILocation getValidLocationForInterfase(IHighwaySegment segment, ILocation invalidLocation)
	{
		ILocation bestLoc = null;
		if (segment.getStartPoint().equals(invalidLocation)) {
			if (isOkForInterfaceSegment(segment.getEndJoint())) {
				bestLoc = segment.getEndPoint();
			}
		}
		else {
			if (isOkForInterfaceSegment(segment.getStartJoint())) {
				bestLoc = segment.getStartPoint();
			}
		}

		if (bestLoc == null) {
			bestLoc = getMidPoint(segment.getStartPoint(), segment.getEndPoint());
		}
		return bestLoc;
	}

	private boolean isOkForInterfaceSegment(IJoint joint)
	{
		return joint == null || joint.getAssociations(ISchemStackPin.class).isEmpty();
	}

	@Nullable private ISchemStackPin getStackPinIfOverlappingWithLocation(IHighwaySegment highwaySegment,
			ILocation pointOnHighway)
	{

		IJoint startJoint = highwaySegment.getStartJoint();
		if (startJoint != null && highwaySegment.getStartPoint().equals(pointOnHighway)) {
			Set<ISchemStackPin> associations = startJoint.getAssociations(ISchemStackPin.class);
			if (!associations.isEmpty()) {
				return associations.iterator().next();
			}
		}

		IJoint endJoint = highwaySegment.getEndJoint();
		if (endJoint != null && highwaySegment.getEndPoint().equals(pointOnHighway)) {
			Set<ISchemStackPin> associations = endJoint.getAssociations(ISchemStackPin.class);
			if (!associations.isEmpty()) {
				return associations.iterator().next();
			}
		}
		return null;
	}

	public boolean isEnabled()
	{
		if (m_model != null && !m_model.isEditable()) {
			return false;
		}

		SelectSet preSelections = getController().getSelectMgr().getPreSelections();

		return isActionApplicable(preSelections) && super.isEnabled();
	}

	@Override boolean isSelectionValidForAction(SelectedObjectSet selectedObjectSet)
	{
		Set<IHighwaySchematic> highwaySchematics = new HashSet<>();
		Set<IConductor> conductors = new HashSet<>();
		collectHighwaysAndConductors(selectedObjectSet.getDiagramObjects(), highwaySchematics, conductors);
		IHighwaySchematic highwaySchematic = null;
		//there sshould be only one highway selection.
		if (highwaySchematics.size() == 1) {

			highwaySchematic = highwaySchematics.iterator().next();
			boolean nonZeroSegment = highwaySchematic.getSegmentsOfType(IHighwaySegment.class).stream()
					.filter(aSegment -> isLogicSegmentCompatibleForRouteInto(aSegment)).findFirst().isPresent();
			if (!nonZeroSegment) {
				highwaySchematic = null;
			}
		}

		return conductors.size() >= 1 && (highwaySchematic != null);
	}

	@NotNull protected Collection<ILogicObject> getLockables(SelectedObjectSet objectSet)
	{
		Set<ILogicObject> lockables = new HashSet<>();
		objectSet.getLogicObjects().stream().forEach(object -> {
			if (isHighwayOrConductor(object)) {
				lockables.add(object);
			}
		});
		return lockables;
	}

	private boolean isHighwayOrConductor(ILogicObject logicObj)
	{
		return (logicObj instanceof IHighwayConductor || logicObj instanceof IHighway);
	}

	public String getActionUIClass()
	{
		return RouteIntoHighwayActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isActionApplicable(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	private void collectHighwaysAndConductors(Collection<IDiagramObject> diagramObjects,
			Set<IHighwaySchematic> highwaySchematics, Set<IConductor> conductors)
	{
		diagramObjects.stream().forEach(x -> {
			if (x instanceof IHighwaySchematic) {
				highwaySchematics.add((IHighwaySchematic) x);
			}
			if (x instanceof IConductor && !isShieldConductor((IConductor) x)) {
				conductors.add((IConductor) x);
			}
		});
	}

	private void removeAlreadyInterfaced(Map<IConductor, List<ISegment>> conductors, IHighwaySchematic highway)
	{
		for (IConductor cond : highway.getConductors()) {
			conductors.remove(cond);
		}
	}

	private void removeShieldConductors(Map<IConductor, List<ISegment>> conductors)
	{
		Collection<IConductor> shields = getShieldsConnectedToMulticores(conductors.keySet());
		for (IConductor shield : shields) {
			conductors.remove(shield);
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{

	}
}
