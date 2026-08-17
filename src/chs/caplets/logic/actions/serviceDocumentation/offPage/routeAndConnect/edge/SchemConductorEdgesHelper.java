package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SchemConductorEdgesHelper
{

	@NotNull public SchemConductorEdges getSchemConductorEdges(ILocation firstLocation, ILocation secondLocation,
			List<ILogicSegment> allSegments)
	{
		SchemConductorEdge edge1 = null;
		SchemConductorEdge edge2 = null;
		for (ILogicSegment segment : allSegments) {
			ILocation startPoint = segment.getStartPoint();
			ILocation endPoint = segment.getEndPoint();
			if (startPoint.equals(firstLocation)) {
				edge1 = new SchemConductorEdge(startPoint, segment);
			}
			if (endPoint.equals(firstLocation)) {
				edge1 = new SchemConductorEdge(endPoint, segment);
			}
			if (startPoint.equals(secondLocation)) {
				edge2 = new SchemConductorEdge(startPoint, segment);
			}
			if (endPoint.equals(secondLocation)) {
				edge2 = new SchemConductorEdge(endPoint, segment);
			}
		}
		assert edge1 != null;
		assert edge2 != null;
		return new SchemConductorEdges(edge1, edge2);
	}

	@Nullable public SchemConductorEdges getEdgesOfSameConductor(Collection<ILogicSegment> segmentList)
	{
		return getEdges(segmentList, false);
	}

	@Nullable public SchemConductorEdges getEdgesOfDifferentConductors(Collection<ILogicSegment> segmentList)
	{
		return getEdges(segmentList, true);
	}

	@Nullable
	private SchemConductorEdges getEdges(Collection<ILogicSegment> segmentList, boolean segmentsOfDiffConductor)
	{
		SchemConductorEdge edge1 = null;
		SchemConductorEdge edge2 = null;
		for (ILogicSegment segment : segmentList) {
			SegmentJointsProvider provider = new SegmentJointsProvider(segment);
			IJoint startJoint = provider.getStartJoint();
			IJoint endJoint = provider.getEndJoint();
			boolean startJointConnectible = connectableJoint(startJoint, segment);
			boolean endJointConnectible = connectableJoint(endJoint, segment);
			if (startJointConnectible && endJointConnectible && !segmentsOfDiffConductor) {
				edge1 = createStartEdge(segment);
				edge2 = createEndEdge(segment);
				return new SchemConductorEdges(edge1, edge2);
			}
			else if (startJointConnectible) {
				if (edge1 == null) {
					edge1 = createStartEdge(segment);
				}
				else {
					edge2 = createStartEdge(segment);
				}
			}
			else if (endJointConnectible) {
				if (edge1 == null) {
					edge1 = createEndEdge(segment);
				}
				else {
					edge2 = createEndEdge(segment);
				}
			}
		}
		if (edge1 != null && edge2 != null) {
			return getJoinEdges(segmentsOfDiffConductor, edge1, edge2);
		}
		else if (edge1 != null) {
			return new SchemConductorEdges(edge1, null);
		}
		else {
			return null;
		}
	}

	private static class SegmentJointsProvider
	{

		@Nullable private IJoint startJoint;
		@Nullable private IJoint endJoint;

		private SegmentJointsProvider(ILogicSegment segment)
		{
			startJoint = segment.getStartJoint();
			endJoint = segment.getEndJoint();
		}

		@Nullable private IJoint getStartJoint()
		{
			return startJoint;
		}

		@Nullable private IJoint getEndJoint()
		{
			return endJoint;
		}
	}

	private SchemConductorEdge createStartEdge(ILogicSegment segment)
	{
		SegmentJointsProvider provider = new SegmentJointsProvider(segment);
		IJoint joint = provider.getStartJoint();
		ILocation startJoint = joint != null ? joint : segment.getStartPoint();
		return new SchemConductorEdge(startJoint, segment);
	}

	private SchemConductorEdge createEndEdge(ILogicSegment segment)
	{
		SegmentJointsProvider provider = new SegmentJointsProvider(segment);
		IJoint joint = provider.getEndJoint();
		ILocation endJoint = joint != null ? joint : segment.getEndPoint();
		return new SchemConductorEdge(endJoint, segment);
	}

	@NotNull private SchemConductorEdges getJoinEdges(boolean sort, @NotNull SchemConductorEdge edge1, @NotNull
			SchemConductorEdge edge2)
	{
		if (sort) {
			if (compare(edge1, edge2) > 0) {
				return new SchemConductorEdges(edge1, edge2);
			}
			else {
				return new SchemConductorEdges(edge2, edge1);
			}
		}
		else {
			return new SchemConductorEdges(edge1, edge2);
		}
	}

	private static boolean connectableJoint(@Nullable IJoint joint, ILogicSegment segment)
	{
		if (joint == null) {
			return true;
		}
		int jointNumAssociations = joint.getNumAssociations();
		if (jointNumAssociations == 1) {
			List<IDiagramObject> connections = joint
					.getAssociations()
					.stream()
					.collect(Collectors.toList());
			IDiagramObject obj1 = connections.get(0);
			return obj1 instanceof IBaseSegment;
		}
		if (jointNumAssociations > 2) {
			return segment instanceof IHighwaySegment;
		}
		if (jointNumAssociations == 2) {
			List<IDiagramObject> connections = joint
					.getAssociations()
					.stream()
					.collect(Collectors.toList());
			IDiagramObject obj1 = connections.get(0);
			IDiagramObject obj2 = connections.get(1);
			if (obj1 instanceof IBaseSegment) {
				if (obj2 instanceof IPort) {
					return true;
				}
				if (obj2 instanceof ISegment && segment instanceof IHighwaySegment) {
					return true;
				}
			}
			if (obj2 instanceof IBaseSegment) {
				return obj1 instanceof IPort;
			}
		}
		return false;
	}

	private static int compare(SchemConductorEdge edge1, SchemConductorEdge edge2)
	{
		ILocation j1 = edge1.getJoinLocation();
		ILocation j2 = edge2.getJoinLocation();
		if (j1 == null && j2 == null) {
			return 0;
		}
		if (j1 == null) {
			return -1;
		}
		if (j2 == null) {
			return 1;
		}
		return compare(j1, j2);
	}

	private static int compare(ILocation j1, ILocation j2)
	{
		int firstY = j1.getY();
		int firstX = j1.getX();
		int secondY = j2.getY();
		int secondX = j2.getX();
		return compare(firstX, firstY, secondX, secondY);
	}

	private static int compare(int firstX, int firstY, int secondX, int secondY)
	{
		if (firstY > secondY) {
			return 1;
		}
		if (firstY < secondY) {
			return -1;
		}
		if (firstX > secondX) {
			return 1;
		}
		else if (firstX < secondX) {
			return -1;
		}
		else {
			return 0;
		}
	}
}
