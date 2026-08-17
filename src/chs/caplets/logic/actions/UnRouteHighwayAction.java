/*
 * Copyright 2011-2012 Mentor Graphics Corporation
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
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.ILocation;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.DynamicEndSnap;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.GfxUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.NodeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnRouteHighwayAction extends AbstractRouteUnRouteHighwayAction
{

	private Model m_model = null;
	private CreateSchemConductorCmd m_cmd;

	public UnRouteHighwayAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_cmd = new CreateSchemConductorCmd();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean bEditOk = true;
		if (successful) {
			bEditOk = unRouteHighway();
		}
		return bEditOk;
	}

	/**
	 * Un routes(disconnects) selected conductor from highway or un-routes all conductors interfaced with selected
	 * highway
	 *
	 * @return true if un-route was successful
	 */
	private boolean unRouteHighway()
	{
		SelectSet preSelections = new SelectSet();
		preSelections.add(getPreSelections());
		ISchemDiagram diagram = m_model.getDiagram();

		UnrouteHighwayHelper helper = new UnrouteHighwayHelper(preSelections, diagram);

		if (!lockObjects(helper)) {
			return false;
		}

		Map<IHighwaySchematic, Set<chs.cof.logical.cable.IConductor>> cableConductorMap =
				helper.getCableConductorMap();

		Set<ISegment> segmentsToRoute = new HashSet<ISegment>();
		Set<IConductor> netsToRoute = new HashSet<IConductor>();

		for (IHighwaySchematic highwaySchm : cableConductorMap.keySet()) {
			Set<chs.cof.logical.cable.IConductor> conductors = cableConductorMap.get(highwaySchm);
			Set<ISegment> segmentsOnHighway = helper.getInterfacedSegments(highwaySchm);
			for (chs.cof.logical.cable.IConductor conductor : conductors) {
				IConductor newSchemConductor;
				Map<ISegment, Map<IHighwaySchematic, IJoint>> jointMap = helper.getConductorInterfacesOnHighways(
						conductor);
				if (conductor instanceof INetConductor) {
					newSchemConductor = connectAllUnroutedNets(netsToRoute, highwaySchm, segmentsOnHighway, jointMap,
							preSelections);
				}
				else { // If selected conductor is wire

					newSchemConductor =
							processUnrouteWireConductor(segmentsToRoute, highwaySchm, segmentsOnHighway, jointMap,
									helper.getSchemCodnuctors(conductor));
				}

				// If newly created conductor has not joined to the segments selected, so deleted it
				if (newSchemConductor != null && newSchemConductor.getConnectivity() != conductor) {
					deleteConductor(newSchemConductor, diagram);
				}
				else if (newSchemConductor != null) {
					// Select segment of newly formed conductor
					selectConductor(preSelections, newSchemConductor);
				}
				removeHighwayConnection(highwaySchm, conductor);
			}
		}

		deleteSelectedHighways(helper.getSchemHighways(), helper.getSelectedSchemHighways(), diagram);

		autoRoute(segmentsToRoute, netsToRoute);

		return true;
	}

	private boolean lockObjects(UnrouteHighwayHelper helper)
	{
		Set<IUID> lockFailures = LogicObjectLockFinder.tryEdit(m_model.getDesign(), helper.getHighways());
		if (!lockFailures.isEmpty()) {
			LogicConcurrencyLogger.getInstance().reportLockFailure(m_model.getDesign(), getDisplayName(), lockFailures,
					message -> reportLockFailures(message));
			return false;
		}
		return true;
	}

	private void reportLockFailures(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}

	private void autoRoute(Set<ISegment> segmentsToRoute, Set<IConductor> netsToRoute)
	{
		ConductorRouteAction.getInstance().addConductorsForRoute(netsToRoute);
		for (ISegment segment : segmentsToRoute) {
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

	@Nullable
	private IConductor processUnrouteWireConductor(Set<ISegment> segmentsToRoute, IHighwaySchematic highwaySchm,
			Set<ISegment> segmentsOnHighway, Map<ISegment, Map<IHighwaySchematic, IJoint>> jointMap,
			Set<IConductor> selectedSchemConductors)
	{
		SelectSet selectionSet = getController().getSelectMgr().getPreSelections();
		Map<ISegment, IJoint> segmentsToJointMap = new HashMap<ISegment, IJoint>();
		Set<ISegment> selectedSegments = new HashSet<ISegment>();
		for (ISegment segment : jointMap.keySet()) {

			if (!segmentsOnHighway.contains(segment)) {
				continue;
			}
			Map<IHighwaySchematic, IJoint> highwayJointMap = jointMap.get(segment);
			IJoint joint = highwayJointMap.get(highwaySchm);
			assert joint != null;
			IConductor schemCondcutor = segment.getConductor();

			NodeHelper.separateConductorAtNode(schemCondcutor, joint,
					FactoryMgr.getCommonFactory(), FactoryMgr.getSchemFactory());

			// Disconnect segments of same conductor other wise this may cause loop in conductor
			disconnectSegmentsFromConductor(schemCondcutor, joint);

			segmentsToJointMap.put(segment, joint);

			if (selectedSchemConductors != null &&
					selectedSchemConductors.contains(segment.getConductor())) {
				selectedSegments.add(segment);
			}

			segmentsToRoute.add(segment);
			if (segment.isSelectable()) {
				selectionSet.add(new Selection(segment));
			}
		}

		if (segmentsToJointMap.size() > 1) {
			return connectSegmentsOnHighway(segmentsToJointMap, selectedSegments, highwaySchm);
		}
		return null;
	}

	@Nullable private IConductor connectAllUnroutedNets(Set<IConductor> netsToRoute, IHighwaySchematic highwaySchm,
			Set<ISegment> segmentsOnHighway, Map<ISegment, Map<IHighwaySchematic, IJoint>> jointMap,
			SelectSet preSelections)
	{
		IConductor newSchemConductor = unrouteNetConductor(jointMap, segmentsOnHighway,
				highwaySchm, preSelections);// selected conductor is net, connect all unrouted nets into one
		if (newSchemConductor != null) {
			netsToRoute.add(newSchemConductor);
		}
		return newSchemConductor;
	}

	private void disconnectSegmentsFromConductor(IConductor schemCondcutor, IJoint joint)
	{
		for (ISegment seg : schemCondcutor.getObjects(ISegment.class)) {
			IJoint jointNode = null;
			if (seg.getStartNode() != null && (seg.getStartPoint().getX() == joint.getX() &&
					seg.getStartPoint().getY() == joint.getY())) {
				jointNode = seg.getStartNode();
			}
			else if (seg.getEndNode() != null && (seg.getEndNode().getX() == joint.getX() &&
					seg.getEndNode().getY() == joint.getY())) {
				jointNode = seg.getEndNode();
			}
			if (jointNode != null) {
				if (jointNode.getAssociations(ISegment.class).size() > 1) {
					for (ISegment segment1 : jointNode.getAssociations(ISegment.class)) {
						ISegment removeSeg =
								jointNode.getAssociations(ISegment.class).iterator().next();
						jointNode.removeAssociation(segment1);
						removeSeg.eraseNode(jointNode);
					}
				}
			}
		}
	}

	private boolean removeHighwayConnection(IHighwaySchematic highwaySchm,
			chs.cof.logical.cable.IConductor cableConductor)
	{
		IHighway cableHighway = highwaySchm.getConnectivity();
		ILogicDesign logicDesign = m_model.getDesign();
		IDesignWideUsageMgr dwum = logicDesign.getDesignWideUsageMgr();
		boolean hasOtherConnection = false;

		Collection<ISchemDiagram> diagrams = DiagramHelper.getCommonDiagrams(dwum, cableHighway, cableConductor);
		for (IDiagramObject diagramObject : dwum
				.getRepresentations(cableHighway, diagrams.toArray(ISchemDiagram.BLANK_ARRAY))) {
			if (diagramObject != highwaySchm && diagramObject instanceof IHighwaySchematic) {
				for (IConductor coductor : ((IHighwaySchematic) diagramObject).getConductors()) {
					if (coductor.getConnectivity() == cableConductor) {
						hasOtherConnection = true;
						break;
					}
				}
			}
		}
		if (!hasOtherConnection && cableHighway instanceof IGeneralHighway) {
			((IGeneralHighway) cableHighway).removeConductor((IHighwayConductor) cableConductor);
			return true;
		}
		return false;
	}

	private void deleteSelectedHighways(Set<IHighwaySchematic> schemHighways,
			Set<IHighwaySchematic> selectedSchemHighways, ISchemDiagram diagram)
	{
		for (IHighwaySchematic highwaySchem : schemHighways) {
			if (!selectedSchemHighways.contains(highwaySchem)) {
				if (highwaySchem.getConductors().isEmpty()) {
					selectedSchemHighways.add(highwaySchem);
				}
			}
		}

		for (IHighwaySchematic highwaySchematic : selectedSchemHighways) {
			Collection<IUIDObject> deleteObjects = new HashSet<IUIDObject>();
			for (IConnected seg : highwaySchematic.getSegments()) {
				deleteObjects.add(seg);
			}
			DeleteHelper.getInstance().delete(diagram, deleteObjects, true);
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}
	}

	@Nullable private IConductor unrouteNetConductor(Map<ISegment, Map<IHighwaySchematic, IJoint>> jointMap,
			Set<ISegment> segmentsOnHighway, IHighwaySchematic highwaySchm, SelectSet preSelections)
	{
		SelectSet selectionSet = getController().getSelectMgr().getPreSelections();
		Map<ISegment, IJoint> segmentsToJointMap = new HashMap<ISegment, IJoint>();
		for (ISegment segment : jointMap.keySet()) {
			if (!segmentsOnHighway.contains(segment)) {  // segment is not on highway, skip un-route
				continue;
			}
			Map<IHighwaySchematic, IJoint> highwayJointMap = jointMap.get(segment);
			IJoint joint = highwayJointMap.get(highwaySchm);
			assert joint != null;
			IConductor schemCondcutor = segment.getConductor();
			NodeHelper.separateConductorAtNode(schemCondcutor, joint,
					FactoryMgr.getCommonFactory(), FactoryMgr.getSchemFactory());

			segmentsToJointMap.put(segment, joint);

			if (segment.isSelectable()) {
				preSelections.add(new Selection(segment));
			}
		}

		Map<Point, List<ISegment>> pointToSegmentMap = new HashMap<Point, List<ISegment>>();
		IJoint startJoint = null;
		ISegment startSegment = null;

		Set<IConductor> joinConductors = new HashSet<IConductor>();
		IConductor schemConductor = null;
		for (ISegment segment : segmentsToJointMap.keySet()) {
			//a segment of same conductor already selected for connection; do not add this segment
			if (joinConductors.contains(segment.getConductor())) {
				continue;
			}
			joinConductors.add(segment.getConductor());

			if (startJoint == null) {
				startSegment = segment;
				startJoint = segmentsToJointMap.get(segment);
				continue;
			}
			IJoint endJoint = segmentsToJointMap.get(segment);
			if (pointToSegmentMap.isEmpty()) {
				schemConductor =
						joinSegments(startSegment, startJoint, segment, endJoint, pointToSegmentMap, highwaySchm);
				selectConductor(preSelections, schemConductor);
				continue;
			}

			List<Object> pathObjects = NodeHelper.getPathBetween(highwaySchm, startJoint, endJoint);
			if (!pathObjects.isEmpty()) {
				startJoint = null;
				startSegment = null;
				for (Object pathObject : pathObjects) {
					if (pathObject instanceof ILocation) {
						ILocation locationObject = (ILocation) pathObject;
						//connectionLocations.add(locationObject);
						Point point = new Point(locationObject.getX(), locationObject.getY());
						if (pointToSegmentMap.containsKey(point)) {
							startJoint = (IJoint) locationObject;
							List<ISegment> segments = pointToSegmentMap.get(point);
							startSegment = segments.iterator().next();
						}
					}
				}
			}

			assert startSegment != null;

			schemConductor =
					joinSegments(startSegment, startJoint, segment, endJoint, pointToSegmentMap, highwaySchm);
		}
		return schemConductor;
	}

	private void deleteConductor(IConductor newSchemConductor, ISchemDiagram diagram)
	{
		Collection<IUIDObject> deleteObjects = new HashSet<IUIDObject>();
		for (IConnected seg : newSchemConductor.getSegments()) {
			deleteObjects.add(seg);
		}
		DeleteHelper.getInstance().delete(diagram, deleteObjects, true);
		CreationDeletionHelper.getTheCreationHelper().processObjects();
	}

	private void selectConductor(SelectSet preSelections, IConductor conductor)
	{
		SelectSet selectionSet = getController().getSelectMgr().getPreSelections();
		if (conductor != null) {
			for (IGfxObjectIterator diagramObjInterator = conductor.getObjects();
					diagramObjInterator.hasNext(); ) {
				IGfxObject object = diagramObjInterator.getNext();
				if (object instanceof ISegment) {
					ISegment segment = (ISegment) object;
					if (segment.isSelectable()) {
						preSelections.add(new Selection(segment));
					}
				}
			}
		}
	}

	/**
	 * Connects two instance of the same conductor present on the highway into one
	 *
	 * @param segmentsToJointMap Map of segments interfacing highway to joint on the highway
	 * @param selectedSegments selected conductor stances of the conductor
	 * @param highwaySchematic Highway
	 *
	 * @return condcutor newly formed condcutor after joining the segments
	 */
	@Nullable private IConductor connectSegmentsOnHighway(Map<ISegment, IJoint> segmentsToJointMap,
			Set<ISegment> selectedSegments, IHighwaySchematic highwaySchematic)
	{
		IConductor conductor = null;
		if (segmentsToJointMap.size() > 1) {
			Map<ISegment, IJoint> connectionMap = new HashMap<ISegment, IJoint>(2);
			Map<ISegment, IJoint> selectionMap = new HashMap<ISegment, IJoint>(2);
			for (Iterator<ISegment> selectedSegIterator = selectedSegments.iterator();
					selectedSegIterator.hasNext() && connectionMap.size() < 2; ) {
				ISegment segment = selectedSegIterator.next();
				IJoint joint = segmentsToJointMap.get(segment);

				// Selected segments will be given preference after segments connected to pinlist for the connection
				if (selectionMap.size() < 2) {
					selectionMap.put(segment, joint);
				}
				IJoint segmentStart = segment.getStartNode();
				boolean startNode = true;
				if (segmentStart == null || (((segmentStart.getX() - joint.getX()) == 0) &&
						((segmentStart.getY() - joint.getY()) == 0))) {
					startNode = false;
				}
				if (RouteUnrouteActionHelper
						.isConnectedToPinList(segment, startNode)) {
					if (connectionMap.size() < 2) {
						updateConnectionMap(connectionMap, segment, joint);
					}
				}
			}

			// Selected segments will be given more preference than non-selected segmnets for connection
			if (connectionMap.size() < 2) {
				for (ISegment seg : selectionMap.keySet()) {
					if (!connectionMap.containsKey(seg)) {
						updateConnectionMap(connectionMap, seg, selectionMap.get(seg));
						if (connectionMap.size() > 1) {
							break;
						}
					}
				}
			}
			Map<ISegment, IJoint> tempMap = new HashMap<ISegment, IJoint>(2);
			if (connectionMap.size() < 2) {
				for (ISegment segment : segmentsToJointMap.keySet()) {
					if (!connectionMap.containsKey(segment)) {
						IJoint joint = segmentsToJointMap.get(segment);
						updateConnectionMap(tempMap, segment, joint);
						boolean startNode = true;
						IJoint segmentStart = segment.getStartNode();
						if (segmentStart == null || (((segmentStart.getX() - joint.getX()) == 0) &&
								((segmentStart.getY() - joint.getY()) == 0))) {
							startNode = false;
						}

						if (RouteUnrouteActionHelper
								.isConnectedToPinList(segment, startNode)) {
							if (connectionMap.size() < 2) {
								updateConnectionMap(connectionMap, segment, joint);
								if (connectionMap.size() > 1) {
									break;
								}
							}
						}
					}
				}
			}
			if (connectionMap.size() < 2) {
				for (ISegment segment : tempMap.keySet()) {
					if (!connectionMap.containsKey(segment)) {
						updateConnectionMap(connectionMap, segment, tempMap.get(segment));
						if (connectionMap.size() > 1) {
							break;
						}
					}
				}
			}

			conductor = joinSegments(highwaySchematic, connectionMap);
		}
		return conductor;
	}

	@Nullable private IConductor joinSegments(IHighwaySchematic highwaySchematic, Map<ISegment, IJoint> connectionMap)
	{
		IJoint startJoint = null;
		ISegment startSegment = null;
		IJoint endJoint = null;
		ISegment endSegment = null;

		for (ISegment segment : connectionMap.keySet()) {
			IJoint joint = connectionMap.get(segment);
			if (startSegment == null) {
				startSegment = segment;
				startJoint = joint;
			}
			else if (endSegment == null) {
				endSegment = segment;
				endJoint = joint;
			}
		}

		if (startSegment != null && endSegment != null) {
			Map<Point, List<ISegment>> pointToSegmentMap = new HashMap<Point, List<ISegment>>();
			return joinSegments(startSegment, startJoint, endSegment, endJoint, pointToSegmentMap,
					highwaySchematic);
		}
		return null;
	}

	private boolean updateConnectionMap(Map<ISegment, IJoint> connectionMap, ISegment segment, IJoint joint)
	{
		for (ISegment seg : connectionMap.keySet()) {
			// A segment of same conductor is added for connection; do not add this segment
			if (seg.getConductor() == segment.getConductor()) {
				return false;
			}
		}
		connectionMap.put(segment, joint);
		return true;
	}

	private IConductor joinSegments(ISegment startSegment, IJoint startJoint, ISegment endSegment, IJoint endJoint,
			Map<Point, List<ISegment>> newSegmentsMap, IHighwaySchematic highwaySchematic)
	{
		IConductor cond = startSegment.getConductor();
		if (startJoint != null) {
			List<Object> pathObjects = NodeHelper.getPathBetween(highwaySchematic, startJoint, endJoint);
			List<ILocation> connectionLocations = new ArrayList<ILocation>();
			for (Object pathObject : pathObjects) {
				if (pathObject instanceof ILocation) {
					connectionLocations.add((ILocation) pathObject);
				}
			}

			List<Point> connectionPoints = new ArrayList<Point>();
			List<ILocation> nonColleanearLocations = GfxUtils.getNonCollinearPoints(connectionLocations);
			for (ILocation location : nonColleanearLocations) {
				connectionPoints.add(new Point(location.getX(), location.getY()));
			}

			Collection<Pair<IDynamicSnap, Integer>> snaps = new ArrayList<Pair<IDynamicSnap, Integer>>();
			Point point = new Point(startJoint.getX(), startJoint.getY());
			DynamicEndSnap des = new DynamicEndSnap(point, null);
			des.addMediator((IDynamicGfxMediator) startSegment);
			snaps.add(new Pair<IDynamicSnap, Integer>(des, 0));

			if (startJoint != endJoint) {
				point = new Point(endJoint.getX(), endJoint.getY());
				des = new DynamicEndSnap(point, null);
				des.addMediator((IDynamicGfxMediator) endSegment);
				snaps.add(new Pair<IDynamicSnap, Integer>(des, 0));

				Class<? extends IHighwayConductor> clazz;
				chs.cof.logical.cable.IConductor cableConductor = cond.getConnectivity();
				if (cableConductor instanceof INetConductor) {
					clazz = INetConductor.class;
				}
				else {
					clazz = IWireConductor.class;
				}

				if (connectionPoints.size() > 1) {
					List<ISegment> newSegments = constructConductor(connectionPoints, clazz);
					boolean joinedsegments = false;
					for (Object newSegment : newSegments) {
						if (newSegment instanceof IDynamicGfxMediator) {
							if (connectSegmentWithSnaps(snaps, (IDynamicGfxMediator) newSegment)) {
								if (newSegment instanceof ISegment) {
									cond = ((ISegment) newSegment).getConductor();
								}
								joinedsegments = true;
							}
						}

						if (newSegmentsMap != null) {
							if (newSegment instanceof ISegment) {
								addSegmentIntoMap(newSegmentsMap, (ISegment) newSegment);
							}
						}
					}
					assert joinedsegments : "Failed to create new segments ";
				}
			}
			else {
				if (endSegment instanceof IDynamicGfxMediator) {
					boolean createdNewSegment =
							((IDynamicGfxMediator) endSegment).addConnectivity(snaps.iterator());
					assert createdNewSegment : "Failed to create new segments ";
				}
				cond = endSegment.getConductor();
			}
		}
		return cond;
	}

	private void addSegmentIntoMap(Map<Point, List<ISegment>> newSegmentsMap, ISegment newSegment)
	{
		ISegment segment = newSegment;

		IJoint startNode = segment.getStartNode();
		if (startNode != null) {
			Point startPoint = new Point(startNode.getX(), startNode.getY());
			addSegment(newSegmentsMap, segment, startPoint);
		}

		IJoint endNode = segment.getEndNode();
		if (endNode != null) {
			Point endPoint = new Point(endNode.getX(), endNode.getY());
			addSegment(newSegmentsMap, segment, endPoint);
		}
	}

	private void addSegment(Map<Point, List<ISegment>> newSegmentsMap, ISegment segment, Point point)
	{
		List<ISegment> segments = newSegmentsMap.get(point);
		if (segments == null) {
			segments = new ArrayList<ISegment>();
			newSegmentsMap.put(point, segments);
		}
		if (!segments.contains(segment)) {
			segments.add(segment);
		}
	}

	private List<ISegment> constructConductor(List<Point> points, Class<? extends IHighwayConductor> clazz)
	{
		m_cmd.setDesign(m_model.getDesign());
		m_cmd.setDiagram(m_model.getDiagram());
		m_cmd.setPoints(points);
		m_cmd.setConductorType(clazz);
		m_cmd.setPrune(true);

		m_cmd.execute();
		return m_cmd.getSegments();
	}

	public boolean isEnabled()
	{
		if (m_model != null && !m_model.isEditable()) {
			return false;
		}
		return isActionApplicable(getController().getSelectMgr().getPreSelections().getSelected()) && super.isEnabled();
	}

	@Override boolean isSelectionValidForAction(SelectedObjectSet selectedObjectSet)
	{
		int numberOfConducorsSelected = 0;
		int numberOfHighwaysSelected = 0;
		for (IDiagramObject diagramObject : selectedObjectSet.getDiagramObjects()) {
			if (diagramObject instanceof IConductor) {
				IConductor conductor = (IConductor) diagramObject;
				if (conductor.getNumberOfInterfacedHighways() > 0) {
					for (IHighwaySchematic highway : conductor.connectedHighways()) {
						if (highway.isConnectedToStackedPins()) {
							return false;
						}
					}
					numberOfConducorsSelected++;
				}
				else {
					return false;
				}
			}
			else if (diagramObject instanceof IHighwaySchematic) {
				if (((IHighwaySchematic) diagramObject).isConnectedToStackedPins()) {
					return false;
				}
				numberOfHighwaysSelected++;
			}
		}

		return numberOfHighwaysSelected > 0 || numberOfConducorsSelected > 0;
	}

	@NotNull protected Collection<ILogicObject> getLockables(SelectedObjectSet objectSet)
	{
		Set<ILogicObject> logicObjects = new HashSet<>();
		Collection<IDiagramObject> diagramObjects = objectSet.getDiagramObjects();
		diagramObjects.stream().
				forEach(diaObj -> collectLockable(logicObjects, diaObj));
		return logicObjects;
	}

	private void collectLockable(Set<ILogicObject> lockableObjs, IDiagramObject diagramObj)
	{
		if (diagramObj instanceof IHighwaySchematic) {
			IHighwaySchematic highway = (IHighwaySchematic) diagramObj;
			lockableObjs.add(highway.getConnectivity());
			highway.getConductors()
					.forEach(conductor -> lockableObjs.add(conductor.getConnectivity()));
		}
		else if (diagramObj instanceof IConductor) {
			IConductor conductor = (IConductor) diagramObj;
			lockableObjs.add(((IConnectivityRef) diagramObj).getConnectivity());
			conductor.connectedHighways().stream()
					.forEach(highway -> lockableObjs.add(highway.getConnectivity()));
		}
	}

	public String getActionUIClass()
	{
		return UnRouteHighwayActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isActionApplicable(selections.getSelected())) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	private static class UnrouteHighwayHelper
	{

		private SelectSet preSelections;
		private ISchemDiagram diagram;
		private Map<IHighwaySchematic, Set<chs.cof.logical.cable.IConductor>> cableConductorMap =
				new HashMap<IHighwaySchematic, Set<chs.cof.logical.cable.IConductor>>(); //Conductors to un-route
		private Set<IHighwaySchematic> selectedSchemHighways = new HashSet<IHighwaySchematic>();
		private Map<chs.cof.logical.cable.IConductor, Set<IConductor>> selectedSchemCondctorMap =
				new HashMap<chs.cof.logical.cable.IConductor, Set<IConductor>>(); // Schematic conductors selected

		//Map of segment joints interfaced with highway
		private Map<chs.cof.logical.cable.IConductor, Map<ISegment, Map<IHighwaySchematic, IJoint>>>
				segmentInterfaceMap =
				new HashMap<chs.cof.logical.cable.IConductor, Map<ISegment, Map<IHighwaySchematic, IJoint>>>();
		private Map<IHighwaySchematic, Set<ISegment>> segmentsOnHighwayMap =
				new HashMap<IHighwaySchematic, Set<ISegment>>();

		private Set<IHighwaySchematic> schemHighways = new HashSet<IHighwaySchematic>();

		UnrouteHighwayHelper(SelectSet preSelections, ISchemDiagram diagram)
		{
			this.preSelections = preSelections;
			this.diagram = diagram;
			initialize();
		}

		public Map<IHighwaySchematic, Set<chs.cof.logical.cable.IConductor>> getCableConductorMap()
		{
			return cableConductorMap;
		}

		Map<ISegment, Map<IHighwaySchematic, IJoint>> getConductorInterfacesOnHighways(
				chs.cof.logical.cable.IConductor conductor)
		{
			return segmentInterfaceMap.get(conductor);
		}

		Set<IConductor> getSchemCodnuctors(chs.cof.logical.cable.IConductor conductor)
		{
			return selectedSchemCondctorMap.get(conductor);
		}

		Set<ISegment> getInterfacedSegments(IHighwaySchematic highwaySchematic)
		{
			return segmentsOnHighwayMap.get(highwaySchematic);
		}

		public Set<IHighwaySchematic> getSchemHighways()
		{
			return schemHighways;
		}

		public Set<IHighwaySchematic> getSelectedSchemHighways()
		{
			return selectedSchemHighways;
		}

		public Collection<ILogicObject> getLockables()
		{
			Set<ILogicObject> lockableObjects = new HashSet<>();
			lockableObjects.addAll(selectedSchemCondctorMap.keySet());
			lockableObjects.addAll(getHighways());
			return lockableObjects;
		}

		private Set<IHighway> getHighways()
		{
			Set<IHighway> lockableObjects = new HashSet<>();
			schemHighways.stream().forEach(schemHighway -> lockableObjects.add(schemHighway.getConnectivity()));
			return lockableObjects;
		}

		private void initialize()
		{

			collateSegments();

			schemHighways.addAll(cableConductorMap.keySet());
			schemHighways.addAll(selectedSchemHighways);

			for (IHighwaySchematic schemHighway : schemHighways) {
				Set<chs.cof.logical.cable.IConductor> conductorsConnectedToHighway =
						cableConductorMap.get(schemHighway);
				Set<ISegment> segmentsOnHighwayToRoute = segmentsOnHighwayMap.get(schemHighway);
				if (segmentsOnHighwayToRoute == null) {
					segmentsOnHighwayToRoute = new HashSet<ISegment>();
					segmentsOnHighwayMap.put(schemHighway, segmentsOnHighwayToRoute);
				}

				processHighwaySegments(schemHighway, conductorsConnectedToHighway, segmentsOnHighwayToRoute);
			}
		}

		private void collateSegments()
		{
			for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
				IUIDObject selectedObject = iter.getNext();
				if (selectedObject instanceof ISegment) {
					collateSegments(selectedObject);
				}
				else if (selectedObject instanceof IHighwaySegment) {
					collateHighwaySegments((IHighwaySegment) selectedObject);
				}
			}
		}

		private void collateSegments(IUIDObject selectedObject)
		{
			IOutputWindow output = CAFUtils.getInstance().getOutputWindow();
			Set<IConductor> processedConductors = new HashSet<IConductor>();
			if (!checkIfValidConductor(selectedObject, output, processedConductors)) {
				return;
			}
			IConductor selectedConductor = ((ISegment) selectedObject).getConductor();
			chs.cof.logical.cable.IConductor cableCond = selectedConductor.getConnectivity();
			Set<IConductor> selectedConductors = selectedSchemCondctorMap.get(cableCond);
			if (selectedConductors == null) {
				selectedConductors = new HashSet<IConductor>();
				selectedSchemCondctorMap.put(cableCond, selectedConductors);
			}
			selectedConductors.add(selectedConductor);

			Set<IHighwaySchematic> highways = selectedConductor.connectedHighways();
			for (IHighwaySchematic highwaySchm : highways) {
				Set<chs.cof.logical.cable.IConductor> conductors = cableConductorMap.get(highwaySchm);
				if (conductors == null) {
					conductors = new HashSet<chs.cof.logical.cable.IConductor>();
					cableConductorMap.put(highwaySchm, conductors);
				}
				conductors.add(selectedConductor.getConnectivity());
			}
		}

		private boolean checkIfValidConductor(IUIDObject obj, IOutputWindow output, Set<IConductor> processedConductors)
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

		private void processHighwaySegments(IHighwaySchematic schemHighway,
				Set<chs.cof.logical.cable.IConductor> conductorsConnectedToHighway,
				Set<ISegment> segmentsOnHighwayToRoute)
		{
			for (IConnected seg : schemHighway.getSegments()) {
				assert seg instanceof IHighwaySegment : "highways segments should be IHighwaySegments";
				IJoint[] highwaysNodes = {seg.getStartJoint(), seg.getEndJoint()};
				for (IJoint highwayNode : highwaysNodes) {
					if (highwayNode != null) {
						for (ISegment segment : highwayNode.getAssociations(ISegment.class)) {
							IConductor schemConductor = segment.getConductor();
							chs.cof.logical.cable.IConductor cableConductor = schemConductor.getConnectivity();
							if (conductorsConnectedToHighway.contains(cableConductor)) {
								Map<ISegment, Map<IHighwaySchematic, IJoint>> jointMap =
										segmentInterfaceMap.get(cableConductor);
								if (jointMap == null) {
									jointMap = new HashMap<ISegment, Map<IHighwaySchematic, IJoint>>();
									segmentInterfaceMap.put(cableConductor, jointMap);
								}
								Map<IHighwaySchematic, IJoint> highwayToJoint = jointMap.get(segment);
								if (highwayToJoint == null) {
									highwayToJoint = new HashMap<IHighwaySchematic, IJoint>();
									jointMap.put(segment, highwayToJoint);
								}
								highwayToJoint.put(schemHighway, highwayNode);
								segmentsOnHighwayToRoute.add(segment);
							}
						}
					}
				}
			}
		}

		private void collateHighwaySegments(IHighwaySegment highwaySegment)
		{
			IHighwaySchematic highwaySchem = highwaySegment.getHighway();
			selectedSchemHighways.add(highwaySchem);

			Set<IConductor> conductors = highwaySchem.getConductors(new IObjectFilter<IConductor>()
			{
				public boolean accept(IConductor obj)
				{
					return true;
				}
			});

			Set<chs.cof.logical.cable.IConductor> cableConductors = cableConductorMap.get(highwaySchem);
			if (cableConductors == null) {
				cableConductors = new HashSet<chs.cof.logical.cable.IConductor>();
				cableConductorMap.put(highwaySchem, cableConductors);
			}
			for (IConductor schemConductor : conductors) {
				cableConductors.add(schemConductor.getConnectivity());
			}
		}
	}
}
