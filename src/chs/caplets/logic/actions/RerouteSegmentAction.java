/*
/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.creation.CreateByMultipointAction;
import chs.caf.caplet.helpers.creation.ISnapController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.services.dynamicgfx.DynamicRerouteLine;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.EndLineStyleUtils;
import chs.utility.Replicator;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.SegmentHelperInfo;
import chs.utility.helpers.TextHelper;
import chs.utility.ui.ArrowPropertiesComponent;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Matt Boyd
 */
public class RerouteSegmentAction extends CreateByMultipointAction implements MouseListener,
		MouseMotionListener, ISnapController, ICtxMenuProvider
{

	private static Cursor m_wireCursor = null;
	private static Cursor m_netCursor = null;
	private static Cursor m_shieldCursor = null;

	private ISegment m_segment = null;
	private boolean m_vertical = false;

	/**
	 * @param controller
	 */
	public RerouteSegmentAction(ICapletController controller)
	{
		super(controller);
		if (m_wireCursor == null) {
			m_wireCursor = CHSImageLoader.loadCursor(CHSImages.WIRE_CURSOR, new Point(7, 7), CHSImages.WIRE_CURSOR);
		}
		if (m_netCursor == null) {
			m_netCursor = CHSImageLoader.loadCursor(CHSImages.NET_CURSOR, new Point(7, 7), CHSImages.NET_CURSOR);
		}
		if (m_shieldCursor == null) {
			m_shieldCursor =
					CHSImageLoader.loadCursor(CHSImages.SHIELD_CURSOR, new Point(7, 7), CHSImages.SHIELD_CURSOR);
		}
	}

	/**
	 * @see chs.caf.caplet.helpers.ActionRT#onActivate(java.awt.event.ActionEvent)
	 */
	public IActionEnum onActivate(ActionEvent e)
	{
		m_segment = null;
		m_vertical = false;
		return super.onActivate(e);
	}

	/**
	 * @see chs.caf.caplet.action.IAction#isEnabled()
	 */
	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		return RerouteSegmentActionUI.isEnabled(getController().getSelectMgr()) && isModeEnabled();
	}

	/**
	 * @see chs.caf.caplet.action.IAction#getActionUIClass()
	 */
	public String getActionUIClass()
	{
		return RerouteSegmentActionUI.class.getName();
	}

	/**
	 * @see chs.caf.caplet.action.IAction#getStatusbarText()
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateDeviceAction.class, "RerouteSegmentAction.StatusBar.text");
	}

	/**
	 * @see chs.caf.caplet.action.IAction#getCursor()
	 */
	@Nullable public Cursor getCursor()
	{
		ISegment selectedSegment = getSelectedSegment();

		chs.cof.logical.cable.IConductor cond = Objects.requireNonNull(selectedSegment).getConductor().getConnectivity();
		if (cond instanceof IWireConductor) {
			return m_wireCursor;
		}
		if (cond instanceof INetConductor) {
			return m_netCursor;
		}
		if (cond instanceof IShieldConductor) {
			return m_shieldCursor;
		}
		return m_wireCursor;
	}

	/**
	 * @see chs.caf.caplet.helpers.creation.ISnapController#isSnapToSubGridEnabled()
	 */
	public boolean isSnapToSubGridEnabled()
	{
		// todo Auto-generated method stub
		return false;
	}

	/**
	 * @see chs.caf.caplet.helpers.creation.CreateByPointAction#markAndForward(java.awt.Point,int)
	 */
	protected boolean markAndForward(Point pt, int modifiers)
	{
		if (pt != null) {
			int a = 0;
			int b = 0;
			if (m_vertical) {
				a = m_segment.getStartPoint().getY();
				b = m_segment.getEndPoint().getY();
				if (pt.y < Math.min(a, b)) {
					pt.y = Math.min(a, b);
				}
				else if (pt.y > Math.max(a, b)) {
					pt.y = Math.max(a, b);
				}
			}
			else {
				a = m_segment.getStartPoint().getX();
				b = m_segment.getEndPoint().getX();
				if (pt.x < Math.min(a, b)) {
					pt.x = Math.min(a, b);
				}
				else if (pt.x > Math.max(a, b)) {
					pt.x = Math.max(a, b);
				}
			}
		}
		return super.markAndForward(pt, modifiers);
	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2) {
			getController().getActionMgr().terminateActiveAction(false);
		}
		else {
			super.mouseClicked(e);
		}
	}

	/**
	 * @see chs.caf.caplet.helpers.creation.CreateByPointAction#constructDisplayObject(java.util.List
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		IConductor conductor = m_segment.getConductor();
		Point startPoint = null;
		Point driftPoint = null;
		Point endPoint = null;
		for (Iterator pts = m_point_list.iterator(); pts.hasNext();) {
			ISmartPoint pt = (ISmartPoint) pts.next();
			if (startPoint == null) {
				startPoint = pt.getAbsoluteLocation();
				if (m_vertical) {
					startPoint.x = m_segment.getStartPoint().getX();
				}
				else {
					startPoint.y = m_segment.getStartPoint().getY();
				}
			}
			else if (endPoint == null) {
				endPoint = pt.getAbsoluteLocation();
				if (m_vertical) {
					endPoint.x = m_segment.getStartPoint().getX();
				}
				else {
					endPoint.y = m_segment.getStartPoint().getY();
				}
			}
			else if (driftPoint == null) {
				driftPoint = pt.getAbsoluteLocation();
			}
		}
		reroute(m_segment, startPoint, endPoint, driftPoint);

		return conductor;
	}

	/**
	 * @see chs.caf.caplet.helpers.creation.CreateByPointAction#constructDynGfx(java.awt.Point)
	 */
	protected IDynamicGfx constructDynGfx(Point refPoint)
	{
		List<Point> vec = List.of(refPoint);
		boolean vertical = isVertical();
		DynamicRerouteLine dynLine = (DynamicRerouteLine) getDynamicGfxService().getFactory()
				.constructRerouteLine(vertical, vec, new Point(0, 0), true);

		ISegment seg = Objects.requireNonNull(getSelectedSegment());

		int start = vertical ? seg.getStartPoint().getY() : seg.getStartPoint().getX();
		int end = vertical ? seg.getEndPoint().getY() : seg.getEndPoint().getX();
		dynLine.setMin(Math.min(start, end));
		dynLine.setMax(Math.max(start, end));
		dynLine.setPosition(vertical ? seg.getStartPoint().getX() : seg.getStartPoint().getY());

		return dynLine;
	}

	/**
	 * @see chs.caf.caplet.helpers.creation.CreateByPointAction#snappingSource()
	 */
	protected Class snappingSource()
	{
		return chs.cof.logical.cable.IConductor.class;
	}

	protected boolean isVertical()
	{
		ISegment segment = getSelectedSegment();
		return (m_segment.getStartPoint().getX() == m_segment.getEndPoint().getX());
	}

	/**
	 * Returns the first of the selected {@link ISegment ISegments}.
	 *
	 * @return
	 */
	@Nullable protected ISegment getSelectedSegment()
	{
		if (m_segment == null) {
			for (SelectedUIDObjectIterator iter =
					getController().getSelectMgr().getPreSelections().getSelectedUIDObjects(); iter.hasNext();) {
				IUIDObject obj = iter.getNext();
				if (obj instanceof ISegment) {
					m_segment = (ISegment) obj;
					ILocation start = m_segment.getStartPoint();
					ILocation end = m_segment.getEndPoint();
					if (start.getX() == end.getX()) {
						m_vertical = true;
					}
				}
			}
		}
		return m_segment;
	}

	/**
	 * Creates the new segments, splits the existing segment, and connects them all together.
	 *
	 * @param seg
	 * @param startPoint
	 * @param endPoint
	 * @param driftPoint
	 */
	protected void reroute(ISegment seg, Point startPoint, Point endPoint, Point driftPoint)
	{

		if (startPoint == null || endPoint == null || driftPoint == null
				|| (startPoint.x == endPoint.x && startPoint.y == endPoint.y)
				|| (m_vertical && driftPoint.x == startPoint.x)
				|| (!m_vertical && driftPoint.y == startPoint.y)) {
			getController().getActionMgr().terminateActiveAction(false);
			return;
		}

		IConductor schemCond = seg.getConductor();
		ISchemFactory schemFact = FactoryMgr.getSchemFactory();

		// gdh 12/26/03 re: 6469
		ArrowPropertiesComponent arrows = new ArrowPropertiesComponent(null);
		String left = arrows.getEndStyle(schemCond, EndLineStyleUtils.LEFT_STYLE);
		String right = arrows.getEndStyle(schemCond, EndLineStyleUtils.RIGHT_STYLE);

		// Puts the start/end points in the same order as the start/end of the segment
		// Maintains direction of segment 
		if (m_vertical) {
			if (seg.getStartPoint().getY() < seg.getEndPoint().getY()) {
				if (startPoint.y > endPoint.y) {
					int y = startPoint.y;
					startPoint.y = endPoint.y;
					endPoint.y = y;
				}
			}
			else {
				if (startPoint.y < endPoint.y) {
					int y = startPoint.y;
					startPoint.y = endPoint.y;
					endPoint.y = y;
				}
			}
		}
		else {
			if (seg.getStartPoint().getX() < seg.getEndPoint().getX()) {
				if (startPoint.x > endPoint.x) {
					int x = startPoint.x;
					startPoint.x = endPoint.x;
					endPoint.x = x;
				}
			}
			else {
				if (startPoint.x < endPoint.x) {
					int x = startPoint.x;
					startPoint.x = endPoint.x;
					endPoint.x = x;
				}
			}
		}

		Point pt1 = null;
		Point pt2 = null;
		if (m_vertical) {
			pt1 = new Point(driftPoint.x, startPoint.y);
			pt2 = new Point(driftPoint.x, endPoint.y);
		}
		else {
			pt1 = new Point(startPoint.x, driftPoint.y);
			pt2 = new Point(endPoint.x, driftPoint.y);
		}

		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();

		// Creates the start segment
		IUID uid = null;
		uid = CAFUtils.getInstance().getCommonFactory().createUID();
		ISegment startSeg = schemFact.constructSegment(uid, startPoint.x, startPoint.y, pt1.x, pt1.y);
		cdh.addCreationObject(startSeg);
		schemCond.addObject(startSeg);
		startSeg.setParent(schemCond);

		// Creates the drift segment (section that drifted away from the original)
		uid = CAFUtils.getInstance().getCommonFactory().createUID();
		ISegment driftSeg = schemFact.constructSegment(uid, pt1.x, pt1.y, pt2.x, pt2.y);
		cdh.addCreationObject(driftSeg);
		schemCond.addObject(driftSeg);
		driftSeg.setParent(schemCond);

		// Creates a new node and hooks up the segments.
		uid = CAFUtils.getInstance().getCommonFactory().createUID();
		IJoint node = schemFact.constructNode(uid, pt1.x, pt1.y);
		cdh.addCreationObject(node);
		startSeg.setEndNode(node);
		driftSeg.setStartNode(node);

		// Creates the end segment
		uid = CAFUtils.getInstance().getCommonFactory().createUID();
		ISegment endSeg = schemFact.constructSegment(uid, pt2.x, pt2.y, endPoint.x, endPoint.y);
		cdh.addCreationObject(endSeg);
		schemCond.addObject(endSeg);
		endSeg.setParent(schemCond);

		for (ISegment newSeg : List.of(startSeg, driftSeg, endSeg)) {
			Replicator.copyCrossingStyle(seg, newSeg);
		}

		// Create a new node and hook up the segments.
		uid = CAFUtils.getInstance().getCommonFactory().createUID();
		node = schemFact.constructNode(uid, pt2.x, pt2.y);
		cdh.addCreationObject(node);
		driftSeg.setEndNode(node);
		endSeg.setStartNode(node);

		// Connects the new section to the original segment
		ILocation segStartPoint = seg.getStartPoint();
		ILocation segEndPoint = seg.getEndPoint();
		// Compares the split points with the end points of the original segment to determine if we split the segment
		boolean sameStartPoints = (startPoint.x == segStartPoint.getX() && startPoint.y == segStartPoint.getY());
		boolean sameEndPoints = (endPoint.x == segEndPoint.getX() && endPoint.y == segEndPoint.getY());
		if (sameStartPoints && sameEndPoints) {
			// The original segment is completely replaced by the new section
			// The segment will be removed from the conductor
			// Removes the segment from the nodes
			IJoint startNode = seg.getStartNode();
			if (startNode != null) {
				startNode.removeAssociation(seg);
				seg.eraseNode(startNode);
			}
			else {
				uid = CAFUtils.getInstance().getCommonFactory().createUID();
				startNode = schemFact.constructNode(uid, startPoint.x, startPoint.y);
				cdh.addCreationObject(startNode);
			}
			IJoint endNode = seg.getEndNode();
			if (endNode != null) {
				endNode.removeAssociation(seg);
				seg.eraseNode(endNode);
			}
			else {
				uid = CAFUtils.getInstance().getCommonFactory().createUID();
				endNode = schemFact.constructNode(uid, endPoint.x, endPoint.y);
				cdh.addCreationObject(endNode);
			}
			schemCond.removeObject(seg);
			cdh.addDeletionObject(seg);

			startSeg.setStartNode(startNode);
			endSeg.setEndNode(endNode);

			TextHelper.addSegmentContainerNameText(schemCond, schemCond.getConnectivity(),
					DiagramHelper.getBaseDiagram(schemCond));
		}
		else if (sameStartPoints) {
			// Original segment shifts toward its end
			IJoint startNode = seg.getStartNode();
			if (startNode != null) {
				startNode.removeAssociation(seg);
				seg.eraseNode(startNode);
			}
			else {
				uid = CAFUtils.getInstance().getCommonFactory().createUID();
				startNode = schemFact.constructNode(uid, startPoint.x, startPoint.y);
				cdh.addCreationObject(startNode);
			}
			uid = CAFUtils.getInstance().getCommonFactory().createUID();
			IJoint endNode = schemFact.constructNode(uid, endPoint.x, endPoint.y);
			cdh.addCreationObject(endNode);

			seg.setStartNode(endNode);
			endSeg.setEndNode(endNode);

			startSeg.setStartNode(startNode);

			seg.realignTextPosition();
		}
		else if (sameEndPoints) {
			// Original segment shifts toward its start
			IJoint endNode = seg.getEndNode();
			if (endNode != null) {
				endNode.removeAssociation(seg);
				seg.eraseNode(endNode);
			}
			else {
				uid = CAFUtils.getInstance().getCommonFactory().createUID();
				endNode = schemFact.constructNode(uid, endPoint.x, endPoint.y);
				cdh.addCreationObject(endNode);
			}
			uid = CAFUtils.getInstance().getCommonFactory().createUID();
			IJoint startNode = schemFact.constructNode(uid, startPoint.x, startPoint.y);
			cdh.addCreationObject(startNode);

			seg.setEndNode(startNode);
			startSeg.setStartNode(startNode);

			endSeg.setEndNode(endNode);

			seg.realignTextPosition();
		}
		else {
			// Splits the segment at the start point
			SegmentHelperInfo info =
					SegmentHelper.splitSegment(CAFUtils.getInstance().getCommonFactory(), schemFact, seg, startPoint);
			// Gets the new segment created from the split
			ISegment newSegment = info.getSegment();

			IJoint startNode = info.getNode1();
			IJoint endNode = info.getNode2();

			endNode.setX(endPoint.x);
			endNode.setY(endPoint.y);

			endSeg.setEndNode(endNode);

			startSeg.setStartNode(startNode);

			seg.realignTextPosition();
		}

		// reset our arrow heads, if any
		arrows.applyStyles(schemCond, seg, left, right);
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
