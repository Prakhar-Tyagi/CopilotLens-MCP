package chs.caplets.logic.actions.shared;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISegment;

import java.util.List;
import java.util.Set;

public class RouteUnrouteActionHelper
{

	private RouteUnrouteActionHelper()
	{
	}

	public enum CONNECTED_STATE
	{

		NOTCONNECTED,
		CONNECTED_TO_OTHER_OBJECT,
		CONNECTED_TO_SAME_OBJECT
	}

	public static CONNECTED_STATE getConnectedState(ISegment segment, boolean startNode, List<ISegment> selectedSegList)
	{
		IJoint connectedNode;
		if (startNode) {
			connectedNode = segment.getStartNode();
		}
		else {
			connectedNode = segment.getEndNode();
		}
		if (connectedNode == null) {
			return CONNECTED_STATE.NOTCONNECTED;
		}
		for (IDiagramObjectIterator diagObjItr = connectedNode.getAssociations(); diagObjItr.hasNext(); ) {
			IDiagramObject diagramObject = diagObjItr.getNext();
			if (diagramObject instanceof IPin) {
				return CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
			}
		}
		CONNECTED_STATE connectedState = CONNECTED_STATE.NOTCONNECTED;

		for (IDiagramObjectIterator diagObjItr = connectedNode.getAssociations(); diagObjItr.hasNext(); ) {
			IDiagramObject diagramObject = diagObjItr.getNext();
			if (diagramObject != segment) {
				if (diagramObject instanceof ISegment) {
					if (((ISegment) diagramObject).getConductor() != segment.getConductor()) {
						connectedState = CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
					}
					else {
						connectedState = CONNECTED_STATE.CONNECTED_TO_SAME_OBJECT;
					}
				}
				else if (!(diagramObject instanceof IPort)) {
					connectedState = CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
				}
			}
		}
		if (connectedState == CONNECTED_STATE.CONNECTED_TO_SAME_OBJECT) {
			Set<ISegment> connectedSegments = connectedNode.getAssociations(ISegment.class);
			if (connectedSegments.size() > 2) {
				boolean otherNotSegmentsSelected = true;
				for (ISegment conSegment : connectedSegments) {
					if (conSegment != segment && selectedSegList.contains(conSegment)) {
						otherNotSegmentsSelected = false;
					}
				}
				if (otherNotSegmentsSelected) {
					return CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
				}
			}
		}
		return connectedState;
	}

	public static boolean isConnectedToPinList(ISegment segment, boolean startNode)
	{
		IJoint connectedNode;
		if (startNode) {
			connectedNode = segment.getStartNode();
		}
		else {
			connectedNode = segment.getEndNode();
		}
		if (connectedNode == null) {
			return false;
		}

		for (IDiagramObjectIterator diagObjItr = connectedNode.getAssociations(); diagObjItr.hasNext(); ) {
			IDiagramObject diagramObject = diagObjItr.getNext();
			if (diagramObject instanceof IPin) {
				return false;
			}
		}

		for (IDiagramObjectIterator diagObjItr = connectedNode.getAssociations(); diagObjItr.hasNext(); ) {
			IDiagramObject diagramObject = diagObjItr.getNext();
			if (diagramObject != segment) {
				if (diagramObject instanceof ISegment) {
					ISegment connectedSegment = (ISegment) diagramObject;
					if (connectedSegment.getConductor() == segment.getConductor()) {
						if (connectedSegment.getEndNode() == connectedNode) {
							if (isConnectedToPinList(connectedSegment, true)) {
								return true;
							}
						}
						else {
							if (isConnectedToPinList(connectedSegment, false)) {
								return true;
							}
						}
					}
				}
				else if (!(diagramObject instanceof IPort)) {
					return true;
				}
			}
		}
		return false;
	}

	public static SegmentStatusFinder createSegmentStatusFinder(ISegment segment, List<ISegment> selectedSegList)
	{
		return new SegmentStatusFinder(segment, selectedSegList);
	}

	public static class SegmentStatusFinder
	{

		private RouteUnrouteActionHelper.CONNECTED_STATE startNodeConnectionStatus;

		private RouteUnrouteActionHelper.CONNECTED_STATE endNodeConnectionStatus;

		public SegmentStatusFinder(ISegment segment, List<ISegment> selectedSegList)
		{
			startNodeConnectionStatus = getConnectedState(segment, true, selectedSegList);
			endNodeConnectionStatus = getConnectedState(segment, false, selectedSegList);
		}

		public boolean bothEndsConnectedToOtherObjects()
		{
			return isStartConnectedOtherObject() && isEndConnectedOtherObject();
		}

		public boolean isStartConnectedOtherObject()
		{
			return startNodeConnectionStatus == CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
		}

		public boolean isEndConnectedOtherObject()
		{
			return endNodeConnectionStatus == CONNECTED_STATE.CONNECTED_TO_OTHER_OBJECT;
		}

		public boolean isStartConnectedToSameOwner()
		{
			return startNodeConnectionStatus == CONNECTED_STATE.CONNECTED_TO_SAME_OBJECT;
		}

		public boolean isEndConnectedToSameOwner()
		{
			return endNodeConnectionStatus == CONNECTED_STATE.CONNECTED_TO_SAME_OBJECT;
		}
	}
}
