/*
 * Copyright 2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.AbstractContextAction;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IApplicationSpecificationAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.creation.BasicDrawingAction;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.CreateConductorInstanceActionHelper;
import chs.caplets.logic.actions.shared.ICreateConductorInstanceAction;
import chs.cof.draw.GfxDimEnum;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGrid;
import chs.cof.draw.IRectangle;
import chs.cof.draw.IText;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.cof.draw.LogicalGraphicSize;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.services.dynamicgfx.DynamicGfxFactoryHelper;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A create tool to make multiple conductors in one action.
 * <p/>
 * created Dec 11, 2014
 */
public abstract class CreateMultipleConductorsAction extends BasicDrawingAction implements
		ICreateConductorInstanceAction
{

	protected final CreateConductorInstanceActionHelper m_helper;

	/**
	 * Constructor for the CreateConductorAction object
	 *
	 * @param controller Description of the Parameter
	 */
	protected CreateMultipleConductorsAction(ICapletController controller)
	{
		super(controller, true, false);
		m_helper = new CreateConductorInstanceActionHelper(controller);
	}

	protected boolean refresh(ISharedConductor sharedConductor, IProject project)
	{
		return m_helper.refresh(sharedConductor, project);
	}

	@Override public boolean isReadyForActivation()
	{
		return isEnabled();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		resetTransientInformation();
		setupKeyHandlers();
		return super.onActivate(e);
	}

	protected Model getLocalModel()
	{
		return (Model) super.getModel();
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return CAFUtils.getInstance()
				.loadCursor(getController().getCaplet(), getCursorImage(), new Point(7, 7));
	}

	@Override public boolean onTerminate(boolean successful)
	{
		try {
			if (successful) {
				doCreateConductors();
			}
		}
		finally {
			cleanupTrans();
			resetTransientInformation();
			m_helper.onTerminate();
		}
		return successful;
	}

	private void doCreateConductors()
	{
		IConductor conductor = m_helper.getConductor();
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		SnapHelper.runUnderBoost(() -> {
			doCreateConductors(conductor, diagram);
			return Void.TYPE;
		});
	}

	private void doCreateConductors(@Nullable IConductor conductor, @NotNull ISchemDiagram diagram)
	{
		//the construction of segments and its connections to pins are marking
		//pinlist extent as dirty and thus during dynamic snap the extent is getting
		//recalculated which is redundant and costly. so in the above loop only
		//dynamic snaps are computed. the construction will start now.
		//can't separate out dynamic snapping and segment creation
		//into independent stages because co-located snapping
		//to support multi-term and multi-pin net will not work.
		//because we need a segement end to snap for next iteration.
		//so we need to do snap and create serially in the loop.
		for (TransientPathGuide line : m_transientStartPaths) {
			TransientSourceNode startNode = line.getStart();
			TransientEndNode endNode = line.getEnd();
			if (endNode == null) {
				continue;
			}
			List<Point> points = line.getPathLocations();
			if (points.size() < 2) {
				continue;
			}
			//need to re-snap the objects so that we can have multi-term at selected
			//pin-placeholder also. because a pin would have been created at the
			//pin-placeholder when first wire was processed. and the stored snap of
			//pin-placeholder doesn't connect to this newly created pin. And also
			//do this before segment creation because cmd doesn't create/connect to
			//pin and thus the current attempt would snap the segment and thus fail
			//create/connect pins.
			List<Pair<IDynamicSnap, Integer>> snaps = new ArrayList<>(2);
			DynamicSnapInfo snap = startNode.getSnap();
			if (snap != null) {
				snaps.add(new Pair<IDynamicSnap, Integer>(getCurrentDynamicSnap(snap.getSnap()), snap.getModifier()));
			}
			if (shouldSnapGuides()) {
				for (int idx = 1; idx < (points.size() - 1); ++idx) {
					snaps.add(new Pair<IDynamicSnap, Integer>(getDynamicSnap(points.get(idx)), 0));
				}
			}
			snap = endNode.getSnap();
			if (snap != null) {
				snaps.add(new Pair<IDynamicSnap, Integer>(getCurrentDynamicSnap(snap.getSnap()), snap.getModifier()));
			}
			Supplier<chs.cof.logical.schem.IConductor> schemCondCreate = () -> {
				CreateSchemConductorCmd cmd = new CreateSchemConductorCmd();
				cmd.setCableConductor(conductor);
				ILogicDesign design = getLocalModel().getDesign();
				cmd.setDesign(design);
				cmd.setDiagram(diagram);
				cmd.setPoints(points);
				cmd.setConductorType(getConductorType());
				cmd.setPrune(true);
				cmd.execute();
				for (ISegment iSegment : cmd.getSegments()) {
					if (iSegment instanceof IDynamicGfxMediator) {
						((IDynamicGfxMediator) iSegment).addConnectivity(snaps.iterator());
					}
				}
				transferChamferingInformation(line, cmd);
				chs.cof.logical.schem.IConductor schemCond = cmd.getConductor();
				ConductorRouteAction.getInstance().addConductorForRoute(schemCond);
				return schemCond;
			};
			if (conductor != null) {
				m_helper.processInstanceConductorCreation(conductor, diagram, schemCondCreate);
			}
			else {
				schemCondCreate.get();
			}
		}
	}

	private void transferChamferingInformation(@NotNull TransientPathGuide line, @NotNull CreateSchemConductorCmd cmd)
	{
		Map<Point, Boolean> chamferingInfo = new HashMap<>();
		List<TransientGuidePoint> allGuides = line.getAllGuides();
		for (TransientGuidePoint guidePoint : allGuides) {
			chamferingInfo.put(guidePoint, guidePoint.isChamfered());
		}
		chamferingInfo.put(allGuides.get(0), false);
		chamferingInfo.put(allGuides.get(allGuides.size() - 1), false);

		for (ISegment iSegment : cmd.getSegments()) {
			IJoint startJoint = iSegment.getStartJoint();
			IJoint endJoint = iSegment.getEndJoint();
			if (startJoint != null) {
				Boolean status = chamferingInfo.get(GfxUtils.getPoint(startJoint));
				if (status != null && status) {
					iSegment.setChamferedAtJoint(startJoint);
				}
			}
			if (endJoint != null) {
				Boolean status = chamferingInfo.get(GfxUtils.getPoint(endJoint));
				if (status != null && status) {
					iSegment.setChamferedAtJoint(endJoint);
				}
			}
		}
	}

	protected abstract Class<? extends IConductor> getConductorType();

	protected abstract String getCursorImage();

	protected abstract boolean shouldSnapGuides();

	private static class TransientPathGuide extends TransientObject
	{

		public static final double CHAMFER = 0.5;
		@NotNull private TransientSourceNode m_source;
		@NotNull private List<TransientGuidePoint> m_commitedNodes = new ArrayList<>();
		@Nullable private TransientEndNode m_target = null;
		@Nullable private TransientNode m_currentTransNode = null;

		private TransientPathGuide(@NotNull TransientSourceNode source)
		{
			m_source = source;
			m_commitedNodes.add(new TransientGuideEdgePoint(source.getLocation(), false, new ExtraBendGap(), false));
		}

		@Nullable public Point getCurrentTransientNodeLocation()
		{
			return m_currentTransNode != null ? m_currentTransNode.getLocation() : null;
		}

		public void reset(IDynamicGfxService dynGfxService)
		{
			purge(dynGfxService);
			if (m_target != null) {
				m_target.purge(dynGfxService);
				m_target = null;
			}
			if (!m_commitedNodes.isEmpty()) {
				//retain the 1st element. this is source node.
				TransientGuidePoint sourceNode = m_commitedNodes.get(0);
				m_commitedNodes.clear();
				m_commitedNodes.add(sourceNode);
			}
		}

		private void appendToPolyline(@NotNull List<Point> polyline, @NotNull Point pt)
		{
			if (!polyline.isEmpty() && pt.equals(polyline.get(polyline.size() - 1))) {
				return;
			}
			polyline.add(pt);
		}

		private void regenerateTransientGraphics(@NotNull IDynamicGfxService dynGfxService,
				@NotNull List<Pair<Point, Boolean>> nodes)
		{
			//purge all the dynamics of this path.
			purge(dynGfxService);
			int totalSizeOfNodes = nodes.size();
			if (totalSizeOfNodes < 2) {
				return;
			}
			List<Point> allPoints = new ArrayList<>();
			appendToPolyline(allPoints, nodes.get(0).getFirst());
			int lastIdx = totalSizeOfNodes - 1;
			for (int idx = 1; idx < lastIdx; idx++) {
				Pair<Point, Boolean> prevPoint = nodes.get(idx - 1);
				Pair<Point, Boolean> currPoint = nodes.get(idx);
				Pair<Point, Boolean> nextPoint = nodes.get(idx + 1);
				collectPolylinePoints(allPoints, prevPoint.getFirst(), currPoint.getFirst(), nextPoint.getFirst(),
						currPoint.getSecond());
			}
			appendToPolyline(allPoints, nodes.get(lastIdx).getFirst());

			if (allPoints.size() > 1) {
				IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
				m_dynGfx = factory.constructPolyline(allPoints, new Point(0, 0), true, false);
				if (m_dynGfx != null) {
					dynGfxService.addTransientGfx(m_dynGfx);
				}
			}
		}

		private void regenerateCommitedPartOfTransientGraphics(@NotNull IDynamicGfxService dynGfxService)
		{
			List<Pair<Point, Boolean>> nodes = new ArrayList<>(m_commitedNodes.size());
			for (TransientGuidePoint node : m_commitedNodes) {
				nodes.add(new Pair<>(node, node.isChamfered()));
			}
			regenerateTransientGraphics(dynGfxService, nodes);
		}

		private void collectPolylinePoints(List<Point> points, Point prevPoint, Point currPoint,
				Point nextPoint, boolean chamfer)
		{
			if (chamfer && arePerpendicular(prevPoint, currPoint, nextPoint)) {
				appendToPolyline(points, getShiftedStartPointOnVector(currPoint, prevPoint, CHAMFER));
				appendToPolyline(points, getShiftedStartPointOnVector(currPoint, nextPoint, CHAMFER));
			}
			else {
				appendToPolyline(points, currPoint);
			}
		}

		public void purge(IDynamicGfxService dynGfxService)
		{
			super.purge(dynGfxService);
			if (m_currentTransNode != null) {
				m_currentTransNode.purge(dynGfxService);
				m_currentTransNode = null;
			}
		}

		private void rebuildDynamicPartOfTransientGfx(@NotNull TransientStreamBendData bendData,
				IDynamicGfxService dynGfxService, boolean chamfer)
		{
			int totalSizeOfNodes = m_commitedNodes.size();
			List<Pair<Point, Boolean>> nodes = new ArrayList<>(totalSizeOfNodes + 2);
			for (TransientGuidePoint node : m_commitedNodes) {
				nodes.add(new Pair<>(node, node.isChamfered()));
			}
			if (m_target == null) {
				if (totalSizeOfNodes > 1) {
					TransientGuidePoint pt1 = m_commitedNodes.get(totalSizeOfNodes - 2);
					TransientGuidePoint pt2 = m_commitedNodes.get(totalSizeOfNodes - 1);
					Point pt3 = bendData.getTransformedSource();
					//remove the last node.
					if (!nodes.isEmpty() && areColinear(pt1, pt2, pt3)) {
						nodes.remove(nodes.size() - 1);
					}
				}
				//add additional nodes.
				nodes.add(new Pair<>(bendData.getTransformedSource(), chamfer));
				nodes.add(new Pair<>(bendData.getTarget(), chamfer));
			}
			regenerateTransientGraphics(dynGfxService, nodes);
		}

		@NotNull public TransientSourceNode getSourceNode()
		{
			return m_source;
		}

		@NotNull public TransientSourceNode getStart()
		{
			return m_source;
		}

		@Nullable public TransientEndNode getEnd()
		{
			return m_target;
		}

		public void commit(@NotNull TransientStreamBendData bendData, boolean folding, boolean chamfered,
				@NotNull IExtraBendGap extraBendGap, boolean align_state)
		{
			//source would be the first node which would never be removed.
			int size = m_commitedNodes.size();
			if (size == 0) {
				assert false : "This is not expected!!!";
				return;
			}
			Point transformedSource = bendData.getTransformedSource();
			Point sourcePt = m_commitedNodes.get(size - 1);
			boolean transSourceFolding = folding;
			boolean transSourceAlign = align_state;
			IExtraBendGap transSourceExtraBendGap = extraBendGap;
			boolean topNodeRemoved = false;
			if (size > 1) {
				//remove the last element
				Point pt1 = m_commitedNodes.get(size - 2);
				Point pt2 = sourcePt;
				Point pt3 = transformedSource;
				//these points are colinear then only remove
				if (areColinear(pt1, pt2, pt3)) {
					TransientGuidePoint origTopElem = m_commitedNodes.remove(size - 1);
					transSourceFolding = origTopElem.getFoldingState();
					transSourceAlign = origTopElem.getAlignState();
					transSourceExtraBendGap = origTopElem.getExtraBendGap();
					topNodeRemoved = true;
				}
			}

			if (topNodeRemoved || !sourcePt.equals(transformedSource)) {
				m_commitedNodes.add(constructGuidePoint(transformedSource, chamfered, transSourceFolding,
						transSourceExtraBendGap, transSourceAlign));
			}

			Point newCommit = bendData.getTarget();
			if (!newCommit.equals(transformedSource)) {
				m_commitedNodes.add(constructGuidePoint(newCommit, chamfered, folding, extraBendGap, align_state));
			}
		}

		private boolean areColinear(Point pt1, Point pt2, Point pt3)
		{
			return (pt3.x - pt1.x) * (pt2.y - pt1.y) == (pt3.y - pt1.y) * (pt2.x - pt1.x);
		}

		private TransientGuidePoint constructGuidePoint(Point pt, boolean chamfer, boolean folding,
				@NotNull IExtraBendGap extraBendGap, boolean align_state)
		{
			return chamfer ? new TransientGuideChamferPoint(pt, folding, extraBendGap, align_state) :
					new TransientGuideEdgePoint(pt, folding, extraBendGap, align_state);
		}

		public void done(@NotNull TransientEndNode target, IDynamicGfxService dynGfxService,
				@NotNull IExtraBendGap extraBendGap, boolean align_state)
		{
			m_target = target;
			m_commitedNodes.add(new TransientGuideEdgePoint(target.getLocation(), false, extraBendGap, align_state));
			regenerateCommitedPartOfTransientGraphics(dynGfxService);
		}

		public void moveTo(@NotNull TransientStreamBendData bendData, IDynamicGfxService dynGfxService,
				TransientNode currTransNode, boolean chamfer)
		{
			rebuildDynamicPartOfTransientGfx(bendData, dynGfxService, chamfer);
			m_currentTransNode = currTransNode;
		}

		public void complete(Function<TransientGuidePoint, TransientEndNode> endNodeFunction,
				IDynamicGfxService dynamicGfxService, @NotNull IExtraBendGap extraBendGap, boolean align_state)
		{
			if (m_commitedNodes.size() == 1) {
				Point location = m_commitedNodes.get(0);
				m_commitedNodes.add(new TransientGuideEdgePoint(location, false, extraBendGap, align_state));
			}
			if (m_commitedNodes.size() > 1) {
				m_target = endNodeFunction.apply(m_commitedNodes.get(m_commitedNodes.size() - 1));
				regenerateCommitedPartOfTransientGraphics(dynamicGfxService);
			}
		}

		public List<Point> getPathLocations()
		{
			return new ArrayList<Point>(m_commitedNodes);
		}

		public List<TransientGuidePoint> getAllGuides()
		{
			return Collections.unmodifiableList(m_commitedNodes);
		}

		public TransientStreamBendData constructBendData()
		{
			if (m_commitedNodes.isEmpty()) {
				return new TransientStreamBendData(null, m_source.getLocation());
			}
			if (m_commitedNodes.size() == 1) {
				return new TransientStreamBendData(null, m_commitedNodes.get(m_commitedNodes.size() - 1));
			}
			return new TransientStreamBendData(m_commitedNodes.get(m_commitedNodes.size() - 2),
					m_commitedNodes.get(m_commitedNodes.size() - 1));
		}

		public void setupAsCompleted()
		{
			IWritableGfxAttribute gfxAttr = FactoryMgr.getDrawFactory().createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.LONG_DASH_DOT);
			gfxAttr.setColor(FactoryMgr.getDrawFactory().constructColorRGB(0, 0, RGB_COMPONENT_MAX_VAL));
			gfxAttr.setGfxDimType(GfxDimEnum.DIM_GRAY);
			setupGfxAttribute(gfxAttr);
		}

		private void setupGfxAttribute(IWritableGfxAttribute gfxAttr)
		{
			if (m_dynGfx != null) {
				m_dynGfx.setAttribute(gfxAttr);
			}
		}

		public void setupAsAnchor(SELECTION_MODE m_mode)
		{
			IWritableGfxAttribute gfxAttr = FactoryMgr.getDrawFactory().createGfxAttribute();
			gfxAttr.setLineStyle(LineStyle.LONG_DASH_DOT);
			gfxAttr.setGfxDimType(GfxDimEnum.DIM_NONE);
			if (SELECTION_MODE.END.equals(m_mode)) {
				gfxAttr.setLineStyle(LineStyle.DOTTED);
				gfxAttr.setThickness(new LogicalGraphicSize(3));
			}
			setupGfxAttribute(gfxAttr);
		}

		public void setupAsFollower()
		{

		}
	}

	private interface IExtraBendGap
	{

		@NotNull IExtraBendGap copy();

		int computePermissibleExtraBandGap(int minGap);

		void startBendProcessing();

		void endBendProcessing();
	}

	private static class ExtraBendGap implements IExtraBendGap
	{

		private int m_additionalGap = 0;
		private int m_lowerBound = 0;

		private ExtraBendGap()
		{

		}

		@NotNull @Override public IExtraBendGap copy()
		{
			ExtraBendGap copy = new ExtraBendGap();
			copy.m_additionalGap = m_additionalGap;
			copy.m_lowerBound = m_lowerBound;
			return copy;
		}

		@Override public int computePermissibleExtraBandGap(int minGap)
		{
			int extraGap = 1 - minGap; //the resulting gap would always be > 0.
			m_lowerBound = Math.min(m_lowerBound, extraGap);
			return Math.max(m_additionalGap, extraGap);
		}

		@Override public void startBendProcessing()
		{
			m_lowerBound = 0;
		}

		@Override public void endBendProcessing()
		{
			//do not reset lower bound here because that will be used in decrement.
		}

		public void increment()
		{
			++m_additionalGap;
		}

		public void decrement()
		{
			if (m_additionalGap > m_lowerBound) {
				--m_additionalGap;
			}
		}

		public void reset()
		{
			m_additionalGap = 0;
			m_lowerBound = 0;
		}
	}

	private SELECTION_MODE m_mode = SELECTION_MODE.SOURCE;
	private Map<Integer, Consumer<Integer>> m_keyHandlers = new HashMap<>(8);
	private boolean m_chamfer = false;
	private boolean m_folding = false;
	private boolean m_align = false;
	private List<TransientPathGuide> m_transientStartPaths = new ArrayList<>();
	private int m_currAnchorSourceIdx = 0;
	@Nullable private MouseEvent m_startDragEvent;
	@Nullable private MouseEvent m_endDragEvent;
	@Nullable private IRectangle m_selectRect;
	@Nullable private DynamicSnapInfo m_lastNodeSnap = null;
	@Nullable private Point m_currMouseLoc = null;
	private ExtraBendGap m_bendExtraGap = new ExtraBendGap();
	private boolean m_isSupportingGuide = false;

	private void resetLastNodeSnap(int modifiers)
	{
		IDynamicSnap lastSnapped = getSnapHelper().getLastSnapped();
		m_lastNodeSnap = (lastSnapped != null) ? new DynamicSnapInfo(lastSnapped, modifiers) : null;
	}

	private IDynamicSnap getCurrentDynamicSnap(@NotNull IDynamicSnap snap)
	{
		Point point = snap.getPoint();
		IDynamicSnap lastSnapped = getDynamicSnap(point);
		return lastSnapped != null ? lastSnapped : snap;
	}

	private IDynamicSnap getDynamicSnap(Point point)
	{
		int snapRadius = ModelUtils.getSnapRadius(CAFUtils.getInstance().getActiveCapletView());
		SnapHelper snapHelper = getSnapHelper();
		snapHelper.snappedPoint(point, snapRadius, snappingSource(), false);
		return snapHelper.getLastSnapped();
	}

	private void resetTransientInformation()
	{
		m_isSupportingGuide = isSupportingGuides();
		m_keyHandlers.clear();
		m_mode = SELECTION_MODE.SOURCE;
		m_chamfer = false;
		m_folding = false;
		m_align = false;
		m_transientStartPaths.clear();
		m_currAnchorSourceIdx = 0;
		m_startDragEvent = null;
		m_endDragEvent = null;
		m_selectRect = null;
		m_lastNodeSnap = null;
		m_currMouseLoc = null;
		m_bendExtraGap.reset();
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.clearPopupTooltip();
		}
	}

	private static class DynamicSnapInfo
	{

		@NotNull private IDynamicSnap m_snap;
		private int m_modifier;

		private DynamicSnapInfo(@NotNull IDynamicSnap snap, int modifier)
		{
			m_snap = snap;
			m_modifier = modifier;
		}

		@NotNull public IDynamicSnap getSnap()
		{
			return m_snap;
		}

		public int getModifier()
		{
			return m_modifier;
		}
	}

	// Mouse Listener Interface implementations
	private enum SELECTION_MODE
	{
		SOURCE {
			public boolean needsUpdateOnBackupEvent()
			{
				return false;
			}

			public boolean needsUpdateOnMouseMovement()
			{
				return false;
			}

			public boolean needsToHandleOrthogonalRouting()
			{
				return false;
			}
		},
		END,
		GUIDE {
			public boolean needsUpdateOnDragEvent()
			{
				return false;
			}
		};

		public boolean needsUpdateOnDragEvent()
		{
			return true;
		}

		public boolean needsUpdateOnBackupEvent()
		{
			return true;
		}

		public boolean needsUpdateOnMouseMovement()
		{
			return true;
		}

		public boolean needsToHandleOrthogonalRouting()
		{
			return true;
		}
	}

	private static class TransientObject
	{

		protected static final int RGB_COMPONENT_MAX_VAL = 255;
		@Nullable protected IDynamicGfx m_dynGfx;

		private TransientObject()
		{
		}

		public void purge(IDynamicGfxService dynGfxService)
		{
			if (m_dynGfx != null) {
				dynGfxService.removeTransientGfx(m_dynGfx);
				m_dynGfx = null;
			}
		}
	}

	private abstract static class TransientGuidePoint extends Point
	{

		private boolean m_folding_state;
		private boolean m_align_state;
		@NotNull private IExtraBendGap m_extraBendGap;

		private TransientGuidePoint(Point location, boolean folding_state, @NotNull IExtraBendGap extraBendGap,
				boolean align_state)
		{
			super(location);
			m_folding_state = folding_state;
			m_align_state = align_state;
			m_extraBendGap = extraBendGap.copy();
		}

		public abstract boolean isChamfered();

		public boolean getFoldingState()
		{
			return m_folding_state;
		}

		@NotNull public IExtraBendGap getExtraBendGap()
		{
			return m_extraBendGap;
		}

		public boolean getAlignState()
		{
			return m_align_state;
		}
	}

	private static class TransientGuideEdgePoint extends TransientGuidePoint
	{

		private TransientGuideEdgePoint(@NotNull Point location, boolean folding_state,
				@NotNull IExtraBendGap extraBendGap, boolean align_state)
		{
			super(location, folding_state, extraBendGap, align_state);
		}

		@Override public boolean isChamfered()
		{
			return false;
		}
	}

	private static class TransientGuideChamferPoint extends TransientGuidePoint
	{

		private TransientGuideChamferPoint(Point location, boolean folding_state, @NotNull IExtraBendGap extraBendGap,
				boolean align_state)
		{
			super(location, folding_state, extraBendGap, align_state);
		}

		@Override public boolean isChamfered()
		{
			return true;
		}
	}

	private static class TransientNode extends TransientObject
	{

		protected static final int RADIUS = 480;
		protected Point m_location;

		private TransientNode(Point location, IDynamicGfxService dynGfxService)
		{
			m_location = location;
			IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
			m_dynGfx = factory.constructCircle(location, location, RADIUS, false);
			assert m_dynGfx != null;
			IWritableGfxAttribute gfxAttr = FactoryMgr.getDrawFactory().createGfxAttribute();
			setupFillColor(gfxAttr);
			m_dynGfx.setAttribute(gfxAttr);
			dynGfxService.addTransientGfx(m_dynGfx);
		}

		protected void setupFillColor(IWritableGfxAttribute gfxAttr)
		{
			gfxAttr.setFillPattern(IFillPattern.PATTERN_SOLID);
		}

		public Point getLocation()
		{
			return m_location;
		}

		public boolean match(Point location)
		{
			return m_location.equals(location);
		}
	}

	private static class TransientOrderedNode extends TransientNode
	{

		@Nullable private IDynamicGfx m_order;

		private TransientOrderedNode(Point location, IDynamicGfxService dynGfxService)
		{
			super(location, dynGfxService);
		}

		public void resetOrder(int order, IDynamicGfxService dynGfxService)
		{
			purgeOrderText(dynGfxService);
			IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
			IText text = FactoryMgr.getDrawFactory().createText();
			text.setString(Integer.toString(order));
			ILocation textLoc = FactoryMgr.getCommonFactory()
					.constructLocation(m_location.x + RADIUS, m_location.y + RADIUS);
			text.setHeight(2 * RADIUS);
			text.setHorizontalJustification(HorizJustificationEnum.JustLeft);
			text.setVerticalJustification(VertJustificationEnum.JustBottom);
			m_order = factory.constructText(text, false, false);
			m_order.setLocation(textLoc);
			IWritableGfxAttribute gfxAttr = FactoryMgr.getDrawFactory().createGfxAttribute();
			setupOrderColor(gfxAttr);
			m_order.setAttribute(gfxAttr);
			dynGfxService.addTransientGfx(m_order);
		}

		protected void setupOrderColor(IWritableGfxAttribute gfxAttr)
		{
		}

		private void purgeOrderText(IDynamicGfxService dynGfxService)
		{
			if (m_order != null) {
				dynGfxService.removeTransientGfx(m_order);
				m_order = null;
			}
		}

		public void purge(IDynamicGfxService dynGfxService)
		{
			super.purge(dynGfxService);
			purgeOrderText(dynGfxService);
		}
	}

	private static class TransientCommitedNode extends TransientOrderedNode
	{

		@Nullable private DynamicSnapInfo m_snap;

		private TransientCommitedNode(Point location, IDynamicGfxService dynGfxService,
				@Nullable DynamicSnapInfo snap)
		{
			super(location, dynGfxService);
			m_snap = snap;
		}

		@Nullable public DynamicSnapInfo getSnap()
		{
			return m_snap;
		}
	}

	private static class TransientGuideNode extends TransientOrderedNode
	{

		@Nullable private IDynamicGfx m_chamferDynGfx = null;

		private TransientGuideNode(Point location, IDynamicGfxService dynGfxService,
				boolean chamfer)
		{
			super(location, dynGfxService);

			List<Point> points = new ArrayList<>();
			if (chamfer) {
				Point center = location;
				points.add(new Point(center.x - (2 * RADIUS), center.y - RADIUS));
				points.add(new Point(center.x - (3 * RADIUS / 2), center.y - RADIUS));
				points.add(new Point(center.x - RADIUS, center.y - (3 * RADIUS / 2)));
				points.add(new Point(center.x - RADIUS, center.y - 2 * RADIUS));
			}
			else {
				Point center = location;
				points.add(new Point(center.x - (2 * RADIUS), center.y - RADIUS));
				points.add(new Point(center.x - RADIUS, center.y - RADIUS));
				points.add(new Point(center.x - RADIUS, center.y - 2 * RADIUS));
			}

			if (points.size() > 1) {
				IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
				m_chamferDynGfx = factory.constructPolyline(points, new Point(0, 0), true, false);
				if (m_chamferDynGfx != null) {
					dynGfxService.addTransientGfx(m_chamferDynGfx);
					m_chamferDynGfx.hideMarkers();
				}
			}
		}

		protected void setupFillColor(IWritableGfxAttribute gfxAttr)
		{
		}

		public void purge(IDynamicGfxService dynGfxService)
		{
			super.purge(dynGfxService);
			if (m_chamferDynGfx != null) {
				dynGfxService.removeTransientGfx(m_chamferDynGfx);
				m_chamferDynGfx = null;
			}
		}
	}

	private static class TransientSourceNode extends TransientCommitedNode
	{

		private TransientSourceNode(Point location, IDynamicGfxService dynGfxService, @Nullable DynamicSnapInfo snap)
		{
			super(location, dynGfxService, snap);
		}

		protected void setupOrderColor(IWritableGfxAttribute gfxAttr)
		{
			gfxAttr.setColor(FactoryMgr.getDrawFactory().constructColorRGB(RGB_COMPONENT_MAX_VAL, 0, 0));
		}

		protected void setupFillColor(IWritableGfxAttribute gfxAttr)
		{
			super.setupFillColor(gfxAttr);
			gfxAttr.setFillForegroundColor(FactoryMgr.getDrawFactory().constructColorRGB(RGB_COMPONENT_MAX_VAL, 0, 0));
		}
	}

	private static class TransientEndNode extends TransientCommitedNode
	{

		private TransientEndNode(Point location, IDynamicGfxService dynGfxService, @Nullable DynamicSnapInfo snap)
		{
			super(location, dynGfxService, snap);
		}

		protected void setupOrderColor(IWritableGfxAttribute gfxAttr)
		{
			gfxAttr.setColor(FactoryMgr.getDrawFactory().constructColorRGB(0, RGB_COMPONENT_MAX_VAL, 0));
		}

		protected void setupFillColor(IWritableGfxAttribute gfxAttr)
		{
			super.setupFillColor(gfxAttr);
			gfxAttr.setFillForegroundColor(FactoryMgr.getDrawFactory().constructColorRGB(0, RGB_COMPONENT_MAX_VAL, 0));
		}
	}

	private void processNewNodeSelection(MouseEvent e)
	{
		Point location = getSnappedLocation(e);
		processNewNodeSelection(location, m_folding, m_chamfer, m_align);
	}

	private void processNewNodeSelection(Point location, boolean folding, boolean chamfer, boolean align_state)
	{
		IDynamicGfxService dynamicGfxService = getDynamicGfxService();
		if (SELECTION_MODE.SOURCE.equals(m_mode)) {
			int mathcIdx = -1;
			int size = m_transientStartPaths.size();
			for (int idx = 0; idx < size; ++idx) {
				TransientCommitedNode transientStartNode = m_transientStartPaths.get(idx).getSourceNode();
				if (transientStartNode.match(location)) {
					transientStartNode.purge(dynamicGfxService);
					mathcIdx = idx;
					break;
				}
			}
			if (mathcIdx >= 0) {
				m_transientStartPaths.remove(mathcIdx);
				int newSize = m_transientStartPaths.size();
				for (int idx = mathcIdx; idx < newSize; ++idx) {
					TransientCommitedNode sourceNode = m_transientStartPaths.get(idx).getSourceNode();
					sourceNode.resetOrder(idx + 1, dynamicGfxService);
				}
			}
			else {
				TransientSourceNode node = new TransientSourceNode(location, dynamicGfxService, m_lastNodeSnap);
				m_transientStartPaths.add(new TransientPathGuide(node));
				node.resetOrder(m_transientStartPaths.size(), dynamicGfxService);
			}
		}
		else if (SELECTION_MODE.END.equals(m_mode)) {
			if (m_currAnchorSourceIdx < m_transientStartPaths.size() && m_currAnchorSourceIdx >= 0) {
				if (m_isSupportingGuide) {
					Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData = new HashMap<>();
					computeBendInformation(location, folding, m_bendExtraGap, align_state, transientStreamBendData);
					commitGuideNodes(folding, chamfer, m_bendExtraGap, align_state, transientStreamBendData);
					convertToCompleted(m_transientStartPaths.get(m_currAnchorSourceIdx), m_currAnchorSourceIdx,
							dynamicGfxService, m_bendExtraGap, align_state);
					++m_currAnchorSourceIdx;
					if (m_currAnchorSourceIdx < m_transientStartPaths.size() && m_currAnchorSourceIdx >= 0) {
						backup();
					}
				}
				else {
					TransientPathGuide transientPathGuide = m_transientStartPaths.get(m_currAnchorSourceIdx);
					TransientEndNode endNode = new TransientEndNode(location, dynamicGfxService, m_lastNodeSnap);
					endNode.resetOrder(m_currAnchorSourceIdx + 1, dynamicGfxService);
					transientPathGuide.done(endNode, dynamicGfxService, m_bendExtraGap, align_state);
					++m_currAnchorSourceIdx;
				}
			}
		}
		else if (SELECTION_MODE.GUIDE.equals(m_mode)) {
			Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData = new HashMap<>();
			computeBendInformation(location, folding, m_bendExtraGap, align_state, transientStreamBendData);
			commitGuideNodes(folding, chamfer, m_bendExtraGap, align_state, transientStreamBendData);
			for (Map.Entry<TransientPathGuide, TransientStreamBendData> entry : transientStreamBendData.entrySet()) {
				TransientPathGuide transientPathGuide = entry.getKey();
				transientPathGuide.regenerateCommitedPartOfTransientGraphics(dynamicGfxService);
			}
			m_bendExtraGap.reset();
		}
		getSnapHelper().clearSnapTransientGraphics();
		updateTransientView();
	}

	private void commitGuideNodes(boolean folding, boolean chamfer, @NotNull IExtraBendGap extraBendGap,
			boolean align_state, Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData)
	{
		for (Map.Entry<TransientPathGuide, TransientStreamBendData> entry : transientStreamBendData.entrySet()) {
			TransientPathGuide transientPathGuide = entry.getKey();
			transientPathGuide.commit(entry.getValue(), folding, chamfer, extraBendGap, align_state);
		}
	}

	private void computeBendInformation(Point location, boolean folding, @NotNull IExtraBendGap extraBendGap,
			boolean align_state, Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData)
	{
		List<TransientStreamBendData> orderedGuides = new ArrayList<>();
		for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
			TransientPathGuide transientPathGuide = m_transientStartPaths.get(idx);
			TransientStreamBendData bendData = transientPathGuide.constructBendData();
			orderedGuides.add(bendData);
			transientStreamBendData.put(transientPathGuide, bendData);
		}
		TransientStreamBendProcessor bendProcessor = new TransientStreamBendProcessor(extraBendGap);
		bendProcessor.process(orderedGuides, location, folding, needsToHandleOrthogonalRouting(), align_state);
	}

	private boolean needsToHandleOrthogonalRouting()
	{
		Model model = CommonUtils.cast(getModel(), Model.class);
		return m_isSupportingGuide && m_mode.needsToHandleOrthogonalRouting() && model != null &&
				model.getOrthogonal();
	}

	private Point getSnappedLocation(MouseEvent e)
	{
		Point currpt = deviceToWorld(e);
		return getSnappedLocation(currpt, e);
	}

	private Point getSnappedLocation(Point currpt, MouseEvent e)
	{
		boolean isCtrlDwn = e.isControlDown();
		int snapRadius = ModelUtils.getSnapRadius(e.getSource());
		return getSnapHelper().snappedPoint(currpt, snapRadius, snappingSource(), isCtrlDwn);
	}

	private Point deviceToWorld(MouseEvent e)
	{
		return CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
	}

	private void setupPathGuideDistinguishers()
	{
		for (int idx = 0; idx < m_transientStartPaths.size(); ++idx) {
			TransientPathGuide transientPathGuide = m_transientStartPaths.get(idx);
			if (idx == m_currAnchorSourceIdx && m_isSupportingGuide) {
				transientPathGuide.setupAsAnchor(m_mode);
			}
			else if (idx < m_currAnchorSourceIdx) {
				transientPathGuide.setupAsCompleted();
			}
			else {
				transientPathGuide.setupAsFollower();
			}
		}
	}

	private void updateTransientView()
	{
		setupPathGuideDistinguishers();
		updateTooltip();
		invalidateTransientView();
	}

	private void invalidateTransientView()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void updateTooltip()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			return;
		}
		view.clearPopupTooltip();

		if (!m_isSupportingGuide) {
			return;
		}

		if (SELECTION_MODE.SOURCE.equals(m_mode)) {
			return;
		}

		if (m_currAnchorSourceIdx < 0 || m_currAnchorSourceIdx >= m_transientStartPaths.size()) {
			//nothing under progress.
			return;
		}

		TransientPathGuide transientPathGuide = m_transientStartPaths.get(m_currAnchorSourceIdx);
		List<TransientGuidePoint> allGuides = transientPathGuide.getAllGuides();
		if (allGuides.isEmpty() || m_currMouseLoc == null) {
			//error condition.
			return;
		}

		String modeText = ResourceMgr.getString(CreateMultipleConductorsAction.class,
				SELECTION_MODE.GUIDE.equals(m_mode) ? "CreateMultipleConductorsAction.mode.guide.name" :
						"CreateMultipleConductorsAction.mode.dest.name");

		StringBuilder tooltipText = new StringBuilder("<html>");
		tooltipText.append(modeText);

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.options.text.G"));

		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.options.text.C"));

		//these options are meaningful only if there are more than 1 pending paths.
		if (m_currAnchorSourceIdx >= 0 && m_currAnchorSourceIdx < (m_transientStartPaths.size() - 1)) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.options.text.F"));

			if (needsToHandleOrthogonalRouting()) {
				tooltipText.append("<br>");
				tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
						"CreateMultipleConductorsAction.options.text.D"));
			}

			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.options.text.1"));

			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.options.text.2"));
		}

		tooltipText.append("</html>");

		IExtent viewExtentWorld = view.getGfxContext().getViewExtentWorld();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IExtent transNodesExt = commonFactory.createExtent();
		for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
			Point transNode = m_transientStartPaths.get(idx).getCurrentTransientNodeLocation();
			if (transNode != null && viewExtentWorld.containsCoord(transNode)) {
				transNodesExt.addUnionLocation(commonFactory.constructLocation(transNode.x, transNode.y));
			}
		}
		if (!transNodesExt.isValid()) {
			transNodesExt.addUnionLocation(commonFactory.constructLocation(m_currMouseLoc.x, m_currMouseLoc.y));
		}
		if (transNodesExt.isValid()) {
			Point devicePoint = view.worldToDevice(new Point(transNodesExt.getRight(), transNodesExt.getBottom()));
			int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
			int x = devicePoint.x + tooltipShift;
			int y = devicePoint.y + -tooltipShift;
			view.showTooltipAtLocation(tooltipText.toString(), new Point(x, y));
		}
		else {
			assert false;
		}
	}

	private void processEndOfDrag()
	{
		if (m_startDragEvent != null && m_endDragEvent != null && m_mode.needsUpdateOnDragEvent()) {
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view != null) {
				Point startPoint = deviceToWorld(m_startDragEvent);
				Point endPoint = deviceToWorld(m_endDragEvent);
				int bl_x = Math.min(startPoint.x, endPoint.x);
				int bl_y = Math.min(startPoint.y, endPoint.y);
				int tr_x = Math.max(startPoint.x, endPoint.x);
				int tr_y = Math.max(startPoint.y, endPoint.y);
				int width = tr_x - bl_x;
				int height = tr_y - bl_y;
				if (width > 0 && height > 0) {
					IExtent selectRect = FactoryMgr.getCommonFactory().constructExtent(bl_x, bl_y, width, height);
					SelectSet selections = view.getObjectsWithinSelectAperture(selectRect);
					if (selections != null) {
						final boolean ascend_x = endPoint.x > startPoint.x;
						final boolean ascend_y = endPoint.y > startPoint.y;
						Comparator<Point> vhComparator = new Comparator<Point>()
						{
							@Override public int compare(Point o1, Point o2)
							{
								int dy = doCompare(o1.y, o2.y);
								int status = ascend_y ? dy : -dy;
								if (status == 0) {
									int dx = doCompare(o1.x, o2.x);
									status = ascend_x ? dx : -dx;
								}
								return status;
							}

							private int doCompare(int a, int b)
							{
								return ((a == b) ? 0 : ((a < b) ? -1 : 1));
							}
						};
						Set<Point> selectedPinLocs = new TreeSet<>(vhComparator);
						for (IAbstractSchemPin pin : selections.getSelectedObjects(IAbstractSchemPin.class)) {
							selectedPinLocs.add(GfxUtils.getPoint(pin.getAbsLocation()));
						}
						SnapHelper.runUnderBoost(() -> {
							for (Point pinLoc : selectedPinLocs) {
								if (!isPendingTerminate()) {
									Point snapLoc = getSnappedLocation(pinLoc, m_endDragEvent);
									resetLastNodeSnap(m_endDragEvent.getModifiers());
									processNewNodeSelection(snapLoc, m_folding, m_chamfer, m_align);
								}
							}
							return Void.TYPE;
						});
						getSnapHelper().clearSnapTransientGraphics();
					}
				}
			}
		}
	}

	private void processCurrentTransientConductor(MouseEvent e)
	{
		Point snappedLocation = getSnappedLocation(e);
		processCurrentTransientConductor(snappedLocation, m_folding, m_chamfer, m_bendExtraGap, m_align);
	}

	private void processCurrentTransientConductor(Point snappedLocation, boolean folding, boolean chamfer,
			@NotNull IExtraBendGap extraBendGap, boolean align_state)
	{
		if (m_mode.needsUpdateOnMouseMovement()) {
			if (m_currAnchorSourceIdx >= 0 && m_currAnchorSourceIdx < m_transientStartPaths.size()) {
				IDynamicGfxService dynamicGfxService = getDynamicGfxService();
				Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData = new HashMap<>();
				List<TransientStreamBendData> orderedGuides = new ArrayList<>();
				if (m_isSupportingGuide) {
					for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
						TransientPathGuide transientPathGuide = m_transientStartPaths.get(idx);
						TransientStreamBendData bendData = transientPathGuide.constructBendData();
						orderedGuides.add(bendData);
						transientStreamBendData.put(transientPathGuide, bendData);
					}
				}
				else {
					TransientPathGuide transientPathGuide = m_transientStartPaths.get(m_currAnchorSourceIdx);
					TransientStreamBendData bendData = transientPathGuide.constructBendData();
					orderedGuides.add(bendData);
					transientStreamBendData.put(transientPathGuide, bendData);
				}
				TransientStreamBendProcessor bendProcessor = new TransientStreamBendProcessor(extraBendGap);
				bendProcessor.process(orderedGuides, snappedLocation, folding, needsToHandleOrthogonalRouting(),
						align_state);

				for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
					TransientPathGuide transientPathGuide = m_transientStartPaths.get(idx);
					TransientStreamBendData bendData = transientStreamBendData.get(transientPathGuide);
					if (bendData == null) {
						continue;
					}
					Point location = bendData.getTarget();
					if (m_isSupportingGuide) {
						TransientGuideNode currTransNode = new TransientGuideNode(location, dynamicGfxService, chamfer);
						currTransNode.resetOrder(idx + 1, dynamicGfxService);
						transientPathGuide.moveTo(bendData, dynamicGfxService, currTransNode, chamfer);
					}
					else {
						TransientNode currTransNode = new TransientNode(location, dynamicGfxService);
						transientPathGuide.moveTo(bendData, dynamicGfxService, currTransNode, chamfer);
					}
				}
			}
			updateTransientView();
		}
	}

	public void mousePressed(MouseEvent e)
	{
		m_startDragEvent = null;
		m_endDragEvent = null;
		discardSelectArea();
		super.mousePressed(e);
		resetLastNodeSnap(e.getModifiers());
	}

	protected boolean markAndForward(Point pt, int modifiers)
	{
		//we will not mark the snap in this action.
		return false;
	}

	public void mouseReleased(MouseEvent e)
	{
		if (m_startDragEvent != null) {
			m_endDragEvent = e;
			processEndOfDrag();
		}
		discardSelectArea();
		super.mouseReleased(e);
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() > 1 || isPendingTerminate()) {
			terminateAction();
		}
		else {
			processNewNodeSelection(e);
		}
	}

	private void terminateAction()
	{
		if (m_isSupportingGuide && SELECTION_MODE.GUIDE.equals(m_mode)) {
			if (m_currAnchorSourceIdx >= 0 && m_currAnchorSourceIdx < m_transientStartPaths.size()) {
				TransientPathGuide guide = m_transientStartPaths.get(m_currAnchorSourceIdx);
				Point pointToSelectNewNode = guide.getCurrentTransientNodeLocation();
				if (pointToSelectNewNode != null) {
					processNewNodeSelection(pointToSelectNewNode, m_folding, m_chamfer, m_align);
				}
			}
			finishRemaining();
		}
		getController().getActionMgr().terminateActiveAction(true);
	}

	private boolean isPendingTerminate()
	{
		return (!m_transientStartPaths.isEmpty() && (m_currAnchorSourceIdx >= m_transientStartPaths.size()));
	}

	@Override public boolean isEnabled()
	{
		// if we are in a transaction boundary, we MUST wait
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false; // wonder why this isn't in the super?
		}
		return super.isEnabled();
	}

	public void mouseDragged(MouseEvent e)
	{
		if (m_startDragEvent == null) {
			m_startDragEvent = e;
		}
		discardSelectArea();
		assert m_startDragEvent != null;
		Point stPt = deviceToWorld(m_startDragEvent);
		Point endPt = deviceToWorld(e);
		m_currMouseLoc = getSnappedLocation(e);
		m_selectRect = FactoryMgr.getDrawFactory().constructRectangle(stPt.x, stPt.y, endPt.x, endPt.y);
		assert m_selectRect != null;
		getDynamicGfxService().addTransientGfx(m_selectRect);
		super.mouseDragged(e);
		updateTransientView();
	}

	private void discardSelectArea()
	{
		if (m_selectRect != null) {
			getDynamicGfxService().removeTransientGfx(m_selectRect);
			m_selectRect = null;
			invalidateTransientView();
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		m_currMouseLoc = getSnappedLocation(e);
		if (SELECTION_MODE.SOURCE.equals(m_mode) && !m_transientStartPaths.isEmpty()) {
			if (m_helper.isActive() || !e.isControlDown()) {
				m_mode = m_isSupportingGuide ? SELECTION_MODE.GUIDE : SELECTION_MODE.END;
			}
		}
		if (!isPendingTerminate()) {
			processCurrentTransientConductor(e);
			super.mouseMoved(e);
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		// Put an entry on the menu to increment the radius by 10
		AbstractAction act = new BackupAction(this);

		act.putValue(Action.NAME, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.backup.action.name"));
		act.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.backup.action.name"));
		act.putValue(Action.LONG_DESCRIPTION, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.backup.action.description"));

		//putValue(SMALL_ICON, icon);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));

		// Put an entry on the menu to increment the radius by 10
		act = new CommitAction(this);
		act.putValue(Action.NAME, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.commit.action.name"));
		act.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.commit.action.name"));
		act.putValue(Action.LONG_DESCRIPTION, ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.commit.action.description"));
		//putValue(SMALL_ICON, icon);
		accel = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));
	}

	private class BackupAction extends AbstractContextAction
	{

		protected BackupAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.backup.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			backup();
		}

		public boolean isEnabled()
		{
			if (m_transientStartPaths.isEmpty()) {
				return false;
			}
			if (m_currAnchorSourceIdx == 0) {
				List<TransientGuidePoint> allGuides = m_transientStartPaths.get(m_currAnchorSourceIdx).getAllGuides();
				return allGuides.size() > 1;
			}
			return true;
		}
	}

	private void backup(List<TransientGuidePoint> allGuides)
	{
		//need a copy of the guides. otherwise during reset this might get cleared.
		//because the pathguide might return an unmodifiable collection.
		List<TransientGuidePoint> cachedGuides = new ArrayList<>(allGuides);
		IDynamicGfxService dynamicGfxService = getDynamicGfxService();
		for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
			m_transientStartPaths.get(idx).reset(dynamicGfxService);
		}
		if (m_isSupportingGuide) {
			int length = cachedGuides.size();
			for (int guideIdx = 1; guideIdx < (length - 1); ++guideIdx) {
				TransientGuidePoint transientGuidePoint = cachedGuides.get(guideIdx);
				boolean foldingState = transientGuidePoint.getFoldingState();
				boolean chamfered = transientGuidePoint.isChamfered();
				IExtraBendGap extraBendGap = transientGuidePoint.getExtraBendGap();
				boolean align_state = transientGuidePoint.getAlignState();
				Map<TransientPathGuide, TransientStreamBendData> transientStreamBendData = new HashMap<>();
				computeBendInformation(transientGuidePoint, foldingState, extraBendGap, align_state,
						transientStreamBendData);
				commitGuideNodes(foldingState, chamfered, extraBendGap, align_state, transientStreamBendData);
			}
			for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
				TransientPathGuide transientPathGuide = m_transientStartPaths.get(idx);
				transientPathGuide.regenerateCommitedPartOfTransientGraphics(dynamicGfxService);
			}
		}
	}

	private void backup()
	{
		if (m_transientStartPaths.isEmpty()) {
			return;
		}
		if (m_mode.needsUpdateOnBackupEvent() && m_currAnchorSourceIdx >= 0) {
			boolean allPathDone = m_currAnchorSourceIdx >= m_transientStartPaths.size();
			int index = Math.min(m_currAnchorSourceIdx, m_transientStartPaths.size() - 1);
			List<TransientGuidePoint> allGuides = m_transientStartPaths.get(index).getAllGuides();
			if (allPathDone) {
				m_currAnchorSourceIdx = index;
				backup(allGuides);
			}
			else if (allGuides.size() < 2) {
				//the current path is exausted. go back to previous completed path.
				if (m_currAnchorSourceIdx > 0) {
					--m_currAnchorSourceIdx;
					//need a copy of the guides. during reset this may get cleared.
					backup(m_transientStartPaths.get(m_currAnchorSourceIdx).getAllGuides());
				}
			}
			else {
				backup(allGuides);
			}
			redrawCurrentTransientLines();
		}
	}

	private void redrawCurrentTransientLines()
	{
		if (m_currMouseLoc != null) {
			processCurrentTransientConductor(m_currMouseLoc, m_folding, m_chamfer, m_bendExtraGap, m_align);
		}
	}

	private class CommitAction extends AbstractContextAction
	{

		protected CommitAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.commit.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			terminateAction();
		}

		public boolean isEnabled()
		{
			if (SELECTION_MODE.GUIDE.equals(m_mode)) {
				return (m_currAnchorSourceIdx >= 0) && (m_currAnchorSourceIdx <= m_transientStartPaths.size());
			}
			return m_currAnchorSourceIdx > 0;
		}
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateMultipleConductorsAction.class,
				"CreateMultipleConductorsAction.statusbar.text");
	}

	@SuppressWarnings("RedundantMethodOverride")
	public void keyTyped(KeyEvent e)
	{
		super.keyTyped(e);
	}

	public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();
		Consumer<Integer> keyHandler = m_keyHandlers.get(keyCode);
		if (keyHandler != null) {
			keyHandler.accept(keyCode);
		}
		else {
			super.keyPressed(e);
		}
	}

	@SuppressWarnings("RedundantMethodOverride")
	public void keyReleased(KeyEvent e)
	{
		super.keyReleased(e);
	}

	private void setupKeyHandlers()
	{
		m_keyHandlers.put(KeyEvent.VK_BACK_SPACE, (t) -> {
			backup();
		});

		m_keyHandlers.put(KeyEvent.VK_ENTER, (t) -> {
			terminateAction();
		});

		if (m_isSupportingGuide) {
			m_keyHandlers.put(KeyEvent.VK_G, (t) -> {
				switch (m_mode) {
					case END:
						m_mode = SELECTION_MODE.GUIDE;
						break;
					case GUIDE:
						m_mode = SELECTION_MODE.END;
						break;
					case SOURCE:
					default:
						break;
				}
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_F, (t) -> {
				m_folding = !m_folding;
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_C, (t) -> {
				m_chamfer = !m_chamfer;
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_1, (t) -> {
				m_bendExtraGap.increment();
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_2, (t) -> {
				m_bendExtraGap.decrement();
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_NUMPAD1, (t) -> {
				m_bendExtraGap.increment();
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_NUMPAD2, (t) -> {
				m_bendExtraGap.decrement();
				redrawCurrentTransientLines();
			});

			m_keyHandlers.put(KeyEvent.VK_D, (t) -> {
				m_align = !m_align;
				redrawCurrentTransientLines();
			});
		}
	}

	private void finishRemaining()
	{
		if (SELECTION_MODE.GUIDE.equals(m_mode)) {
			IDynamicGfxService dynamicGfxService = getDynamicGfxService();
			for (int idx = m_currAnchorSourceIdx; idx < m_transientStartPaths.size(); ++idx) {
				convertToCompleted(m_transientStartPaths.get(idx), idx, dynamicGfxService, m_bendExtraGap, m_align);
			}
			m_currAnchorSourceIdx = m_transientStartPaths.size();
			redrawCurrentTransientLines();
		}
	}

	private void convertToCompleted(TransientPathGuide transientPathGuide, int idx,
			IDynamicGfxService dynamicGfxService, @NotNull IExtraBendGap extraBendGap, boolean align_state)
	{
		final int order = idx + 1;
		Function<TransientGuidePoint, TransientEndNode> endNodeFunction = (location) -> {
			IDynamicSnap dynamicSnap = getDynamicSnap(location);
			DynamicSnapInfo snapInfo = (dynamicSnap != null) ? new DynamicSnapInfo(dynamicSnap, 0) : null;
			TransientEndNode endNode = new TransientEndNode(location, dynamicGfxService, snapInfo);
			endNode.resetOrder(order, dynamicGfxService);
			return endNode;
		};
		transientPathGuide.complete(endNodeFunction, dynamicGfxService, extraBendGap, align_state);
	}

	protected boolean isSupportingGuides()
	{
		return ConductorRouteAction.getInstance().isThreePhaseRouting();
	}

	private static class TransientStreamBendData
	{

		@Nullable private final Point m_justBeforeSource;
		@NotNull private final Point m_source;
		@NotNull private Point m_transformedSource;
		@NotNull private Point m_target;

		private TransientStreamBendData(@Nullable Point justBeforeSource, @NotNull Point source)
		{
			m_justBeforeSource = justBeforeSource != null ? new Point(justBeforeSource) : null;
			m_source = new Point(source);
			m_transformedSource = new Point(source);
			m_target = new Point(source);
		}

		@Nullable public Point getJustBeforeSource()
		{
			return m_justBeforeSource;
		}

		@NotNull public Point getSource()
		{
			return m_source;
		}

		@NotNull public Point getTransformedSource()
		{
			return m_transformedSource;
		}

		@NotNull public Point getTarget()
		{
			return m_target;
		}

		public void setTransformedSource(int x, int y)
		{
			m_transformedSource.setLocation(x, y);
		}

		public void setTarget(int x, int y)
		{
			m_target.setLocation(x, y);
		}
	}

	private static class TransientStreamBendProcessor
	{

		@NotNull private final IExtraBendGap m_extraBendGap;

		private TransientStreamBendProcessor(@NotNull IExtraBendGap extraBendGap)
		{
			m_extraBendGap = extraBendGap;
		}

		@NotNull private Point getProjectionOnPerpendicularLineToBase(
				@NotNull Point basePt1,
				@NotNull Point basePt2,
				@NotNull Point candidate
		)
		{
			int x1 = basePt1.x;
			int y1 = basePt1.y;

			double x21 = normalizeToGrid(basePt2.x - basePt1.x);
			double y21 = normalizeToGrid(basePt2.y - basePt1.y);
			double x31 = normalizeToGrid(candidate.x - basePt1.x);
			double y31 = normalizeToGrid(candidate.y - basePt1.y);

			//square of length of baseline
			double l = (x21 * x21) + (y21 * y21);
			//dot-product key for candidate point
			double m = (x31 * y21) - (y31 * x21);

			int x = x1 + normalizeToWorld(y21 * (m / l));
			int y = y1 - normalizeToWorld(x21 * (m / l));
			return new Point(x, y);
		}

		public void process(@NotNull List<TransientStreamBendData> stream, @NotNull Point anchor, boolean folding,
				boolean orthogonalRouting, boolean align)
		{
			if (stream.isEmpty()) {
				return;
			}
			try {
				m_extraBendGap.startBendProcessing();
				Point orthogonalAnchor = new Point(anchor);
				if (orthogonalRouting) {
					orthogonalAnchor = processForOrthogonalAdjustments(stream, anchor);
				}
				processForMovement(stream, orthogonalAnchor, folding);
				if (orthogonalRouting && align) {
					alignWithAnchor(stream);
				}
			}
			finally {
				m_extraBendGap.endBendProcessing();
			}
		}

		private enum Orientation
		{
			HORIZONTAL, VERTICAL, UNDEFINED
		}

		private void alignWithAnchor(@NotNull List<TransientStreamBendData> stream)
		{
			Orientation orientation = Orientation.UNDEFINED;
			for (TransientStreamBendData bendData : stream) {
				orientation = determineStreamDirection(bendData.getTarget(), bendData.getTransformedSource());
				if (orientation != Orientation.UNDEFINED) {
					break;
				}
			}

			if (orientation == Orientation.UNDEFINED) {
				for (TransientStreamBendData bendData : stream) {
					Point justBeforeSource = bendData.getJustBeforeSource();
					Point fixedJoint = justBeforeSource != null ? justBeforeSource : bendData.getSource();
					orientation = determineStreamDirection(bendData.getTransformedSource(), fixedJoint);
					if (orientation != Orientation.UNDEFINED) {
						break;
					}
				}
			}

			Point pivot = stream.get(0).getTarget();
			for (TransientStreamBendData bendData : stream) {
				Point target = bendData.getTarget();
				if (orientation == Orientation.HORIZONTAL) {
					bendData.setTarget(pivot.x, target.y);
				}
				else if (orientation == Orientation.VERTICAL) {
					bendData.setTarget(target.x, pivot.y);
				}
			}
		}

		@NotNull private Orientation determineStreamDirection(@NotNull Point pt1, @NotNull Point pt2)
		{
			if (pt1.y == pt2.y && pt1.x != pt2.x) {
				return Orientation.HORIZONTAL;
			}
			if (pt1.x == pt2.x && pt1.y != pt2.y) {
				return Orientation.VERTICAL;
			}
			return Orientation.UNDEFINED;
		}

		@NotNull private Point processForOrthogonalAdjustments(@NotNull List<TransientStreamBendData> stream,
				@NotNull Point anchor)
		{
			Point basePt1 = stream.get(0).getTransformedSource();
			Point basePt2 = anchor;
			//no need to do any processing of baseline is zero length.
			if (basePt1.equals(basePt2)) {
				return anchor;
			}

			Point adjustment = new Point(basePt2.x - basePt1.x, 0);
			for (TransientStreamBendData bendData : stream) {
				Point justBeforeSource = bendData.getJustBeforeSource();
				if (justBeforeSource == null) {
					//there is no guide point selected.
					int run = Math.abs(basePt2.x - basePt1.x);
					int rise = Math.abs(basePt2.y - basePt1.y);
					if (run > rise) {//horizontal routing
						return new Point(basePt2.x, basePt1.y);
					}
					else {//vertical routing
						return new Point(basePt1.x, basePt2.y);
					}
				}
				Point source = bendData.getTransformedSource();
				if (source.x == justBeforeSource.x && source.y != justBeforeSource.y) {
					adjustment = new Point(0, basePt2.y - basePt1.y);
				}
			}

			for (TransientStreamBendData bendData : stream) {
				Point source = bendData.getTransformedSource();
				bendData.setTransformedSource(source.x + adjustment.x, source.y + adjustment.y);
			}
			return anchor;
		}

		private void processForMovement(@NotNull List<TransientStreamBendData> stream,
				@NotNull Point anchor, boolean folding)
		{
			TransientStreamBendData anchorBendData = stream.get(0);
			Point basePt1 = anchorBendData.getTransformedSource();
			Point basePt2 = anchor;
			//no need to do any processing of baseline is zero length.
			if (basePt1.equals(basePt2)) {
				//the orthogonal adjustment should be transfered to target.
				for (TransientStreamBendData bendData : stream) {
					Point source = bendData.getTransformedSource();
					bendData.setTarget(source.x, source.y);
				}
				return;
			}
			ListMap<Point, TransientStreamBendData> bendGroups = new ListMap<>();
			for (TransientStreamBendData streamBendData : stream) {
				Point source = streamBendData.getTransformedSource();
				Point bendKey = getProjectionOnPerpendicularLineToBase(basePt1, basePt2, source);
				bendGroups.add(bendKey, streamBendData);
			}

			//compute the bending direction.
			Point pt1 = anchorBendData.getJustBeforeSource();
			Point pt2 = basePt1;
			if (pt1 == null) {
				//mimic a horizontal stream being bent.
				pt1 = new Point(basePt1.x - IGrid.GRID_SIZE, basePt1.y);
			}
			Point pt3 = basePt2;

			double x21 = normalizeToGrid(pt2.x - pt1.x);
			double y21 = normalizeToGrid(pt2.y - pt1.y);
			double x31 = normalizeToGrid(pt3.x - pt1.x);
			double y31 = normalizeToGrid(pt3.y - pt1.y);

			//check the direction of bend by anchor w.r.t the previous leg.
			boolean clockWiseBend = (y31 * x21) < (x31 * y21);
			if (folding) {
				clockWiseBend = !clockWiseBend;
			}

			for (Map.Entry<Point, List<TransientStreamBendData>> entry : bendGroups.entrySet()) {
				transform(entry.getValue(), basePt1, basePt2, clockWiseBend);
			}
		}

		@SuppressWarnings("OverlyLongMethod")
		private void transform(@NotNull List<TransientStreamBendData> stream, @NotNull Point basePt1,
				@NotNull Point basePt2, boolean clockWiseBend)
		{
			if (stream.isEmpty()) {
				return;
			}
			if (stream.size() == 1) {
				TransientStreamBendData bendData = stream.get(0);
				Point source = bendData.getTransformedSource();
				int x = source.x + basePt2.x - basePt1.x;
				int y = source.y + basePt2.y - basePt1.y;
				bendData.setTarget(x, y);
				return;
			}
			//transform for actual bend.
			Collections.sort(stream, (a, b) -> {
				Point sa = a.getTransformedSource();
				Point sb = b.getTransformedSource();
				return CommonUtils.comparePoints(sa.x, sa.y, sb.x, sb.y);
			});
			Point pivot = stream.get(0).getTransformedSource();
			int pivotIdx = 0;
			for (int idx = 0; idx < stream.size(); ++idx) {
				TransientStreamBendData bendData = stream.get(idx);
				if (bendData.getTransformedSource().equals(basePt1)) {
					pivot = basePt1;
					pivotIdx = idx;
					break;
				}
			}

			//compute the restricted extra bend gap.
			double minGap = Double.MAX_VALUE;
			Iterator<TransientStreamBendData> bendDataIterator = stream.iterator();
			Point prevItem = bendDataIterator.next().getTransformedSource();
			do {
				Point currItem = bendDataIterator.next().getTransformedSource();
				double dx = normalizeToGrid(currItem.x - prevItem.x);
				double dy = normalizeToGrid(currItem.y - prevItem.y);
				minGap = Math.min(minGap, Math.abs(dx != 0.0D ? dx : dy));
				prevItem = currItem;
			} while (bendDataIterator.hasNext());

			int extraBendGap = m_extraBendGap.computePermissibleExtraBandGap(new Double(minGap).intValue());

			//update the source transformation and target points
			int shiftX = basePt2.x - basePt1.x;
			int shiftY = basePt2.y - basePt1.y;
			for (int idx = 0; idx < stream.size(); ++idx) {
				TransientStreamBendData bendData = stream.get(idx);
				Point source = bendData.getTransformedSource();
				int deltaX = source.x - pivot.x;
				int deltaY = source.y - pivot.y;
				if (extraBendGap != 0) {
					double dx = normalizeToGrid(deltaX);
					double dy = normalizeToGrid(deltaY);
					double l = Math.sqrt(dx * dx + dy * dy);
					if (l != 0.0) {
						int distFromPivot = Math.abs(pivotIdx - idx);
						deltaX += normalizeToWorld(distFromPivot * extraBendGap * (dx / l));
						deltaY += normalizeToWorld(distFromPivot * extraBendGap * (dy / l));
					}
				}
				if (clockWiseBend) {
					bendData.setTransformedSource(source.x + deltaY, source.y - deltaX);
					bendData.setTarget(pivot.x + deltaY + shiftX, pivot.y - deltaX + shiftY);
				}
				else {
					bendData.setTransformedSource(source.x - deltaY, source.y + deltaX);
					bendData.setTarget(pivot.x - deltaY + shiftX, pivot.y + deltaX + shiftY);
				}
			}
		}
	}

	private static final double GRID_SIZE_FLOAT = (double) IGrid.GRID_SIZE;

	private static double normalizeToGrid(int p)
	{
		return (p / GRID_SIZE_FLOAT);
	}

	private static int normalizeToWorld(double p)
	{
		return new Double(p * GRID_SIZE_FLOAT).intValue();
	}

	private static boolean arePerpendicular(@NotNull Point first, @NotNull Point mid, @NotNull Point last)
	{
		if (first.equals(mid) || first.equals(last) || mid.equals(last)) {
			return false;
		}
		double x32 = normalizeToGrid(last.x - mid.x);
		double y32 = normalizeToGrid(last.y - mid.y);
		double x12 = normalizeToGrid(first.x - mid.x);
		double y12 = normalizeToGrid(first.y - mid.y);
		return (x12 * x32 + y12 * y32) == 0;
	}

	private static Point getShiftedStartPointOnVector(@NotNull Point start, @NotNull Point end, double numGrids)
	{
		double lengthOfSegment = start.distance(end);
		double multiplyFactor = (GRID_SIZE_FLOAT * numGrids) / lengthOfSegment;
		int x = new Double((end.getX() - start.getX()) * multiplyFactor).intValue();
		int y = new Double((end.getY() - start.getY()) * multiplyFactor).intValue();
		return new Point(start.x + x, start.y + y);
	}
}
