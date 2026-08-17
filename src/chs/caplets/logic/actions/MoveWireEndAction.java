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
import chs.caf.IBasicDrawingActivityHandler;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.creation.CreateByMultipointAction;
import chs.caf.caplet.helpers.snapping.ISnapThroughConnectorController;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.helpers.snapping.SnapThroughConnectorHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.logic.LogicSelectHelper;
import chs.caplets.logic.Model;
import chs.caplets.shared.actions.LogicStretchManipulator;
import chs.caplets.shared.actions.SelectAction;
import chs.caplets.shared.actions.SymDeviceTemporaryPlaceHolderCreationHelper;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.ILine;
import chs.cof.draw.ISheet;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.parameterized.AddPinPlacementConstraints;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.NodeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class MoveWireEndAction: Responsible for moving Wire, Net & Shield body Ends.
 */
public class MoveWireEndAction extends ControllerActionRT implements IBasicDrawingActivityHandler,
		ISnapThroughConnectorController
{

	private ICompoundObject m_guideLineCompound;
	private ILine m_line1;
	private ILine m_line2;
	private boolean m_ctrlPressed = false;
	private boolean isInvokedThroughStretch = false;
	@NotNull private final SymDeviceTemporaryPlaceHolderCreationHelper helper;

	public enum POINT_POSITION
	{

		START, END
	}

	private enum DISCONNECT_TYPE
	{

		/**
		 * Segment disconnected from a pinlist
		 */
		FROM_PINLIST,
		/**
		 * Segment disconnected from a highway
		 */
		FROM_HIGHWAY,

		NONE;

		public static DISCONNECT_TYPE getDisconnectType(ISegment selectedSegment, Point ucpInWorldCoordinates)
		{
			IJoint nearestJoint = getNearestJoint(selectedSegment, ucpInWorldCoordinates);
			DISCONNECT_TYPE disconnectType = NONE;
			if (isAttachedToHighway(nearestJoint)) {
				disconnectType = FROM_HIGHWAY;
			}
			else if (isAttachedToPin(nearestJoint)) {
				disconnectType = FROM_PINLIST;
			}
			return disconnectType;
		}

		private static boolean isAttachedToHighway(IJoint joint)
		{
			return joint != null && !joint.getAssociations(IHighwaySegment.class).isEmpty();
		}
	}

	private static final int MAX_COLOR = 255;
	private static final int m_tmpPinSize = 200;
	private static final int m_tmpPinSizeBy2 = 100;

	private IDynamicGfxService m_dynamics;
	private IGrid m_grid;
	private Cursor m_addPinValidCursor = null;
	private ILine m_feedback;
	private IGfxObject m_pinPoint;
	private IWritableGfxAttribute m_red;

	protected ISegment m_selectedSegment = null;
	protected IPin m_selectedPin = null;
	protected Point m_selectedPoint = null;
	protected POINT_POSITION m_pointPosition;

	private IPinList m_destPinList;
	private PinPlacementConstraintsHolder constraintsHolder;
	private IPin[] m_pins = null;

	private GeneratorParameters m_genParams;

	private DISCONNECT_TYPE m_disconnectType;
	@Nullable private SnapHelper m_snapHelper;

	/**
	 * Flag is set when the mouse is moved after activating the action
	 */
	private boolean m_didMouseMoved;

	public MoveWireEndAction(ICapletController controller)
	{
		this(controller, null, null);
	}

	public MoveWireEndAction(ICapletController controller, IPinList destPinList, Point destPoint)
	{
		super(controller);

		// Create our "pivot point"
		IGfxModel model = (IGfxModel) controller.getCapletModel();
		m_dynamics = model.getDynamicGfxService();
		if (m_addPinValidCursor == null) {
			m_addPinValidCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_pin.gif", new Point(7, 7));
		}

		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		m_red = drawFactory.constructAttribute(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setFillBackgroundColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setFillPattern(IFillPattern.PATTERN_SOLID);

		IWritableGfxAttribute green = drawFactory.constructAttribute(drawFactory.lookupColor("pin"));
		green.setFillBackgroundColor(drawFactory.lookupColor("pin"));
		green.setFillPattern(IFillPattern.PATTERN_SOLID);

		m_destPinList = destPinList;
		m_selectedPoint = destPoint;
		createGuideLines();

		// Skip placeholder creation for devices that have no symbol parameterized (no schematic symbol assigned yet)
		helper = new SymDeviceTemporaryPlaceHolderCreationHelper(
				this::getDeviceWithSymbol,
				devWithSymbol -> devWithSymbol.getParameterized() == null);
	}

	/**
	 * Gets the Enabled attribute of the Delete Action
	 *
	 * @return The Enabled value
	 */
	public boolean isEnabled()
	{
		return hasOperands(getController().getSelectMgr().getPreSelections()) && super.isEnabled();
	}

	protected boolean hasOperands(SelectSet sset)
	{
		SelectionFilter segmentFilter = new SelectionFilter();
		segmentFilter.addOnlyClass(ISegment.class);
		SelectSet segmentSelections = new SelectSet();
		segmentSelections.setSelectionFilter(segmentFilter);
		segmentSelections.add(sset);

		ISegment selectedSegment = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof ISegment) {
				if (selectedSegment != null) {
					return false;
				}
				selectedSegment = (ISegment) obj;
			}
		}

		if (selectedSegment == null || sset.getSelectCount() > 2) {
			return false;
		}

		if (DiagramHelper.getDiagram(selectedSegment) != CAFUtils.getInstance().getActiveDiagram()) {
			return false;
		}

		return (isAttachedTo(selectedSegment.getStartJoint()) || isAttachedTo(selectedSegment.getEndJoint()))
				&& !areAllEndsConnectedToNetSplices(selectedSegment) ;
	}

	/**
	 * return true when the schem conductor ends connected to Net splices
	 *
	 * @param segment the segment to check
	 * @return true when the conductor ends connected to Net splices
	 */
	private boolean areAllEndsConnectedToNetSplices(@NotNull ISegment segment)
	{
		chs.cof.logical.schem.IConductor schemConductor = segment.getConductor();
		/* In case of conductors has no pins , means the ends is not connected to net splices! and we shall return false */
		if (schemConductor == null || schemConductor.getPins() == null || schemConductor.getPins().isEmpty()) {
			return false;
		}

		for(IPin pin : schemConductor.getPins()) {
			IDiagramObject owner = pin.getParent();
			if (owner instanceof chs.cof.logical.schem.IPinList) {
				chs.cof.logical.schem.IPinList schemPinList = (chs.cof.logical.schem.IPinList) owner;
				chs.cof.logical.cable.IPinList cablePinList = schemPinList.getConnectivity();
				ISplice splice = CommonUtils.cast(schemPinList.getConnectivity(), ISplice.class);
				if (!(splice != null
						&& ConnectionHelper.isSpliceConnectedToNetAndMCShield(splice))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns the flag indicating whether there exists pin or highway associated with the specified joint.
	 *
	 * @param joint pin or highway associations to determine at this joint
	 *
	 * @return <i>true</i> when there is a pin or highway associated with the specified joint.
	 */
	private static boolean isAttachedTo(@Nullable IJoint joint)
	{
		return joint != null && (!joint.getAssociations(IPin.class).isEmpty() ||
				!joint.getAssociations(IHighwaySegment.class).isEmpty());
	}

	private static boolean isAttachedToPin(IJoint joint)
	{
		return joint != null && !joint.getAssociations(IPin.class).isEmpty();
	}

//	private static boolean isAttachedToHighway(IJoint joint)
//	{
//		return joint != null && !joint.getAssociations(IHighwaySegment.class).isEmpty();
//	}

	public IActionEnum onActivate(ActionEvent e)
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		IGriddable gridholder = (IGriddable) gview.getSheet();
		m_grid = gridholder.getGrid();
		if (e.getSource() instanceof LogicStretchManipulator) {
			isInvokedThroughStretch = true;
		}
		// Iterate each selection and split the line
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		setOperands(preSelections);

		// this actions lifetime is that of the caplet, ensure m_constraints is empty before we start
//		m_constraints.clear();

		if (m_pins != null && m_pins.length > 0) {
			m_genParams = DiagramHelper.createGeneratorParameters(m_pins[0]);
			helper.addTempPlaceHolderForDevicesWithSymbols(m_destPinList, m_grid, m_genParams);
		}
		else {
			m_genParams = new GeneratorParameters(getModel().getDiagram().getGrid().getGridSpacing());
		}

		//Add Transient graphics for conductor
		setupCondTransientGraphics();

		return IActionEnum.eActivated;
	}

	private void setupCondTransientGraphics()
	{
//		int gp = m_grid.getGridSpacing() / 2;
		m_feedback = FactoryMgr.getDrawFactory().constructLine(m_selectedSegment.getStartPoint().getX(),
				m_selectedSegment.getStartPoint().getY(), m_selectedSegment.getEndPoint().getX(),
				m_selectedSegment.getEndPoint().getY());
		m_dynamics.addTransientGfx(m_feedback);
	}

	protected void setOperands(@NotNull SelectSet sset)
	{
		m_pins = null;

		//Red Feedback when the user is trying to place Wire End in free space
		m_pinPoint = FactoryMgr.getDrawFactory().constructRectangle(0, 0, m_tmpPinSize, m_tmpPinSize);
		m_pinPoint.setAttribute(m_red);

		IPin selectedPin = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof ISegment) {
				m_selectedSegment = (ISegment) obj;
			}
			else if (obj instanceof IPin) {
				if (selectedPin != null) {
					break;
				}
				selectedPin = (IPin) obj;
			}
		}

		// 1) Conductor - PinList connection
		// 2) Conductor - Highway connection
		//Get the last clicked Point
		Point userClickedPoint = LogicSelectHelper.getLastClickedPoint();
		Point ucpInWorldCoordinates = new Point(0, 0);
		if (userClickedPoint != null) {
			ucpInWorldCoordinates = convertToWorldCoordinate(userClickedPoint);
		}

		m_disconnectType = DISCONNECT_TYPE.getDisconnectType(m_selectedSegment, ucpInWorldCoordinates);
		IJoint nearestJoint = getNearestJoint(m_selectedSegment, ucpInWorldCoordinates);
		if (m_disconnectType == DISCONNECT_TYPE.FROM_PINLIST) {// && nearestJoint != null) {
			assert isAttachedToPin(nearestJoint);
			m_selectedPin = nearestJoint.getAssociations(IPin.class).iterator().next();
			m_destPinList = (IPinList) m_selectedPin.getParent();
			// save the selected pins
			m_pins = new IPin[1];
			m_pins[0] = m_selectedPin;
		}

		m_pointPosition = nearestJoint == m_selectedSegment.getStartJoint() ?
				POINT_POSITION.START : POINT_POSITION.END;
	}

	@NotNull private Point convertToWorldCoordinate(@NotNull Point userClickedPoint)
	{
		Point ucpInWorldCoordinates = CAFUtils.getInstance()
				.getWorldPoint(userClickedPoint, CAFUtils.getInstance().getActiveCapletView());
		ucpInWorldCoordinates
				.setLocation(m_grid.snap(ucpInWorldCoordinates.x), m_grid.snap(ucpInWorldCoordinates.y));
		return ucpInWorldCoordinates;
	}

	public boolean onTerminate(boolean successful)
	{
		try {
			boolean bEditOk = true;
			if (successful && m_selectedPoint != null) {
				bEditOk = editModel();
			}
			// Get rid of our transient graphics
			removeTransientGfx();
			m_dynamics.removeAllDynamicGfx();
			m_dynamics.removeAllTransientGfx();
			return bEditOk;
		}
		finally {
			cleanUpPostAction();
		}
	}

	private void cleanUpPostAction()
	{
		constraintsHolder = null;
		m_destPinList = null;
		m_pins = null;
		m_selectedPin = null;
		m_selectedSegment = null;
		m_pointPosition = null;
		m_snapHelper = null;
		m_ctrlPressed = false;
		isInvokedThroughStretch = false;
		SnapThroughConnectorHelper.clearCachedObjects();
	}

	public String getActionUIClass()
	{
		return MoveWireEndActionUI.class.getName();
	}

	// Do the model edit
	private boolean editModel()
	{
		//Iterate over the selection list
		// Find the schemConductor
		// Find the Pin to be removed from the schemConductor identified in the above step
		// Snag the schemConductor to the new location
		IConductor schemConductor = m_selectedSegment.getConductor();
		ConductorRouteAction.getInstance().addConductorForRoute(schemConductor);
		chs.cof.logical.cable.IConductor cond1 = schemConductor.getConnectivity();

		if (m_disconnectType == DISCONNECT_TYPE.FROM_HIGHWAY) {
			disconnectHighway();
		}
		else {
			disConnectPinList(cond1);
		}


		List<IDynamicGfxMediator> objectsToConnect = new ArrayList<>();
		objectsToConnect.add((IDynamicGfxMediator) m_selectedSegment);
		getSnapHelper().connectObjects(objectsToConnect);

		return true;
	}

	protected void disConnectPinList(chs.cof.logical.cable.IConductor cableConductor)
	{
		IAbstractPin cablePin = m_selectedPin.getConnectivity();
		IConductor schemConductor = m_selectedSegment.getConductor();

		IJoint pinJoint = m_selectedPin.getJoint();
		if (pinJoint != null) {
			NodeHelper.separateConductorAtNode(schemConductor, m_selectedPin.getJoint(), FactoryMgr.getCommonFactory(),
					FactoryMgr.getSchemFactory());
		}
		else {
			assert false : "Selected pin must have a joint ";
		}
		if (!ConnectionHelper.hasMultipleConnections(cablePin, m_selectedPin, cableConductor, schemConductor)) {
			cablePin.removeConductor(cableConductor);
			cableConductor.removePin(cablePin);
		}

		moveAndConnectSegmentEnd();
	}

	protected void disconnectHighway()
	{
		IConductor schemConductor = m_selectedSegment.getConductor();
		IJoint nearestJoint = (m_pointPosition == POINT_POSITION.START) ? m_selectedSegment.getStartJoint() :
				m_selectedSegment.getEndJoint();
		// get the schematic connections first. We need it because there is no link from
		// conductors to highways.
		if (nearestJoint != null) {
			Set<IHighwaySegment> associations = nearestJoint.getAssociations(IHighwaySegment.class);
			NodeHelper.separateConductorAtNode(schemConductor, nearestJoint, FactoryMgr.getCommonFactory(),
					FactoryMgr.getSchemFactory());

			disconnectLogicallyIfNoConnections(schemConductor, associations);
		}
		else {
			assert false : "Selected Segment must have a joint";
		}

		moveAndConnectSegmentEnd();
	}

	/**
	 * Will disconnect the conductor logically from associated highways. This function assumes that schematic connection
	 * are disconnected prior to this call.
	 *
	 * @param schemConductor The schematic connector to disconnect
	 * @param associations Highway segments associated to the given schematic conductor
	 */
	private void disconnectLogicallyIfNoConnections(IConductor schemConductor, Set<IHighwaySegment> associations)
	{
		for (IHighwaySegment highwaySegment : associations) {
			IHighwaySchematic schematicHighway = highwaySegment.getHighway();
			chs.cof.logical.cable.IConductor connectivityConductor = schemConductor.getConnectivity();
			IGeneralHighway connectivityHighway = HighwayHelper.toGeneralHighway(schematicHighway);
			if (connectivityHighway != null && !HighwayHelper.hasOtherHighwayConnection(connectivityConductor,
					connectivityHighway, Collections.emptySet(), Collections.emptySet())) {
				connectivityHighway.removeConductor((IHighwayConductor) connectivityConductor);
			}
		}
	}

	private void moveAndConnectSegmentEnd()
	{
		Point newPoint = new Point(m_selectedPoint.x, m_selectedPoint.y);
		ILocation newLoc = new Location(newPoint);

		//Update the EndPoint with the snap Point
		if (m_pointPosition == POINT_POSITION.START) {
			m_selectedSegment.setStartPoint(newLoc);
		}
		else {
			assert m_pointPosition == POINT_POSITION.END;
			m_selectedSegment.setEndPoint(newLoc);
		}

		createConnectivity(m_selectedSegment);
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

	public void mouseClicked(MouseEvent e)
	{
		if (m_didMouseMoved) {
			m_didMouseMoved = false;
			getSnapHelper().markSnap(e.getModifiersEx());
			terminateMoveWireEndAction();
		}
	}

	@Override public void mousePressed(MouseEvent e)
	{
	}

	private void terminateMoveWireEndAction()
	{
		getController().getActionMgr().terminateActiveAction(true);
	}

	public void mouseReleased(MouseEvent e)
	{
		disableBaseSelectAction(e);
		if (m_didMouseMoved && isInvokedThroughStretch) {
			m_didMouseMoved = false;
			getSnapHelper().markSnap(e.getModifiersEx());
			terminateMoveWireEndAction();
		}
	}

	private void disableBaseSelectAction(MouseEvent e)
	{
		IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
		if(actionMgr == null){
			return;
		}
		//forward event to base select action to disable it
		IAction baseAction = actionMgr.getBaseAction();
		if(SelectAction.class.isAssignableFrom(baseAction.getClass())){
			baseAction.getEventDistributor().mouseReleased(e);
		}
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mouseMoved(MouseEvent e)
	{
		setCursor(m_addPinValidCursor);

		m_didMouseMoved = true;
		m_selectedPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_selectedPoint.setLocation(m_grid.snap(m_selectedPoint.x), m_grid.snap(m_selectedPoint.y));

		handleMouseMovedToAnotherInstance(e, m_destPinList);

		//drawing snap graphics (guided lines)
		Point wpoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		Point point = getSnapHelper()
				.snappedPoint(wpoint, ModelUtils.getSnapRadius(e.getSource()), getSnappingSourceClass(), e);
		m_selectedPoint.setLocation(point.getX(), point.getY());
		adjustCondTransientGraphics();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void adjustCondTransientGraphics()
	{
		if (m_pointPosition == POINT_POSITION.START) {
			m_feedback.getStartPoint().setX(m_selectedPoint.x);
			m_feedback.getStartPoint().setY(m_selectedPoint.y);
		}
		else if (m_pointPosition == POINT_POSITION.END) {
			m_feedback.getEndPoint().setX(m_selectedPoint.x);
			m_feedback.getEndPoint().setY(m_selectedPoint.y);
		}
		m_dynamics.addTransientGfx(m_feedback);
	}

	/**
	 * Finds the offset of the closest pin to the given point.
	 */
	public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
//		m_dragging = true;
	}

	private void handleMouseMovedToAnotherInstance(MouseEvent e, IPinList oldPinList)
	{
		//Handle the case when the mouse moves to another instance of the pinllist
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		SelectSet selectedObjects = view.OnSelectPoint(e, null, false);
		//filter selections to only pin or pinlist objects
		SelectionFilter pinListFitler = new SelectionFilter();
		pinListFitler.addOnlyClass(IPinList.class);
		pinListFitler.addOnlyClass(IPin.class);
		pinListFitler.addOnlyClass(IHighwaySchematic.class);
		pinListFitler.addOnlyClass(IHighwaySegment.class);
		pinListFitler.addOnlyClass(IGeneralHighway.class);
		List<Selection> selectionsList = null;
		if (selectedObjects != null) {
			selectionsList = selectedObjects.getFilteredSelections(pinListFitler);
		}

		if (selectionsList != null && !selectionsList.isEmpty()) {
			Selection s = selectionsList.iterator().next();
			IUIDObject selectedObject = s.getObject();
			IPinList newPinList = null;
			if (selectedObject instanceof IPin) {
				//if the cursor moved to a pin get the parent pinlist
//				newPinList = (IPinList) ((IPin) selectedObject).getParent();
				newPinList = (IPinList) ((IDiagramObject) selectedObject).getParent();
			}
			else if (selectedObject instanceof IPinList) {//if(selectedObject instanceof IPinList) {
				newPinList = (IPinList) s.getObject();
			}
			if (newPinList != null && newPinList.getConnectivity().getLibraryRef() != null) {
				newPinList = null;
			}
			if (newPinList != null && newPinList.getConnectivity() instanceof IBlockDevice) {
				newPinList = null;
			}
			if (newPinList != null) {
				changeDestination(newPinList);
			}
			else {
				removeTransientGfx();
				helper.removeTempPlaceHoldersForDevicesWithSymbols(m_destPinList);
			}
		}
		else {
			helper.removeTempPlaceHoldersForDevicesWithSymbols(oldPinList);
			//remove the PinPlacementConstraints from the first instance
//			m_constraints.clear();
			removeTransientGfx();
//			m_tmpPoint.x = m_selectedPoint.x + m_pinLocations[i].x - m_tmpPinSize / 2;
//			m_tmpPoint.y = selectedPoint.y + m_pinLocations[i].y - m_tmpPinSize / 2;
			ILocation loc = new Location(m_selectedPoint.x - m_tmpPinSizeBy2, m_selectedPoint.y - m_tmpPinSizeBy2);
			m_pinPoint.getLocation().setLocation(loc.getX(), loc.getY());
			m_pinPoint.setAttribute(m_red);
			m_dynamics.addTransientGfx(m_pinPoint);
		}
	}

	protected void changeDestination(IPinList newPinList)
	{
		helper.removeTempPlaceHoldersForDevicesWithSymbols(m_destPinList);
		helper.addTempPlaceHolderForDevicesWithSymbols(newPinList, m_grid, m_genParams);

		m_destPinList = newPinList;

		//remove the PinPlacementConstraints from the first instance
//		m_constraints.clear();
		removeTransientGfx();

		//Draw the PinPlacementConstraints on the new instance
//		if (!handleConnectorAttachedToDevice(m_destPinList)) {
		addPinPlacementConstraints();
//		}
	}

	protected void addPinPlacementConstraints()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		ISheet sheet = view.getSheet();
		IGfxContext context = view.getGfxContext();
		final AddPinPlacementConstraints placementConstraints =
				new AddPinPlacementConstraints(null, m_destPinList, m_genParams.getSpacing(), false, sheet, context);
		constraintsHolder = placementConstraints.getHolder();

		for (IGfxObject tobj : constraintsHolder.getBoundaryExtensions()) {
			m_dynamics.addTransientGfx(tobj);
		}
		for (IGfxObject circle : constraintsHolder.getValidMovePositions()) {
			m_dynamics.addTransientGfx(circle);
		}
	}

	@Nullable protected IPinList getDeviceWithSymbol(IPinList pinList)
	{
		if (pinList == null) {
			return null;
		}
		if (pinList.getSharedObject() != null) {
			return null;
		}
		// cope with splices
		if (pinList.getConnectivity() instanceof IDevice ||
				pinList.getConnectivity() instanceof ISplice) {
			if (pinList.getParameterized() == null) {
				return pinList;
			}
		}
		else {
			// WE have a connector, see if its attached device has a symbol, if so return it
//			IPinList theAttachedPinList = PinPlacementHelper.getAttachedPinlist(pinList);
			chs.cof.logical.cable.IPinList connector = pinList.getConnectivity();
			IDevice owner = (connector instanceof IDeviceOwnedConnector) ?
					((IDeviceOwned) connector).getOwner(IDevice.class) : null;
			if (owner != null) {
				IPinList theAttachedPinList = PinPlacementHelper.getAttachedDevice(owner, pinList);

				if (theAttachedPinList != null && theAttachedPinList.getConnectivity() instanceof IDevice) {
					if (theAttachedPinList.getParameterized() == null) {
						return theAttachedPinList;
					}
				}
			}
		}
		return null;
	}

	protected void removeTransientGfx()
	{
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private static String getLocationKey(int x, int y)
	{
		return x + "," + y;
	}

	private Model getModel()
	{
		return (Model) getController().getCapletModel();
	}

	private static boolean isJointConnectedToNetSplicePin(@Nullable IJoint Joint)
	{
		if(Joint == null) {
			return false;
		}
		return Joint.getAssociations(IPin.class).stream()
				.anyMatch(pin -> pin.getConnectivity().getOwner() instanceof ISplice
						&& ConnectionHelper.isSpliceConnectedToNetAndMCShield((ISplice) pin.getConnectivity().getOwner()));
	}

	@Nullable private static IJoint getNearestJoint(ISegment seg, Point point)
	{
		ILocation location = new Location(point);
		boolean isStart = false;
		IJoint nearestJoint = seg.getEndJoint();

		/* if both ends was connected to net splices the action will be disabled from beginning ,
		 so if the end is net splice means the start is not net splice and we need to return it as we disable disconnection for net splices */
		if ((seg.getStartPoint().distance(location) <= seg.getEndPoint().distance(location) && !isJointConnectedToNetSplicePin(seg.getStartJoint()))
		|| isJointConnectedToNetSplicePin(nearestJoint))
		{
			nearestJoint = seg.getStartJoint();
			isStart = true;
		}

		if (nearestJoint == null || !isAttachedTo(nearestJoint)) {
			return isStart ? seg.getEndJoint() : seg.getStartJoint();
		}
		return nearestJoint;
	}

	public void createConnectivity(ISegment segment)
	{
		IGfxView gview = (IGfxView) CAFUtils.getInstance().getViewForObject(segment);
		if (gview != null) {
			// use ConnectionHelper to connect Moving Conductor to Pinlist
			ConnectionHelper ch = new ConnectionHelper();
			IBaseDiagram baseDiag = DiagramHelper.getBaseDiagram(segment);
			ch.connectLogicSegmentToPinLists(segment, gview.getGfxContext(), baseDiag);
			if (baseDiag != null) {
				baseDiag.refreshRepresentations();
			}
		}
	}

	@Override public void constrainPoint(Point pt)
	{

	}

	@Override public boolean checkSnap(@Nullable IDynamicSnap dynSnap)
	{
		return true;
	}

	@NotNull @Override public IDynamicGfxService getDynamicGfxService()
	{
		return m_dynamics;
	}

	@Override public boolean isSnapToSubGridEnabled()
	{
		return false;
	}

	@Nullable @Override public IGfxObject getAlternateGraphics(IDynamicSnap snap)
	{
		Set<Class<?>> validGuidLineClasses = guideLineClasses();
		if (CreateByMultipointAction.createGuidedLinesGraphic(snap, validGuidLineClasses, m_guideLineCompound, m_line1, m_line2)) {
			return m_guideLineCompound;
		}
		return null;
	}

	@NotNull private Set<Class<?>> guideLineClasses()
	{
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(IWireConductor.class);
		result.add(INetConductor.class);
		return result;
	}

	@Override public boolean isRetainLastSnapEnabled()
	{
		return false;
	}

	@NotNull @Override public SnapType getRequiredSnapType()
	{
		return  SnapType.UNCONSTRAINED;
	}

	@NotNull @Override public Class<?> getSnappingSourceClass()
	{
		ILogicObject snapSourceObject = getSnapSourceObject();
		if (snapSourceObject != null) {
			return snapSourceObject.getClass();
		}
		return IConductor.class;
	}

	@Override public void modifySnapCandidates(Collection<IDynamicGfxMediator> snapList, Point hitPoint,
			Collection<IDynamicGfxMediator> avoidMediators)
	{

	}

	private void createGuideLines()
	{
		IDrawFactory drawFact = FactoryMgr.getDrawFactory();
		m_guideLineCompound = drawFact.createCompoundObject();
		m_line1 = drawFact.constructLine(0, 0, 0, 0);
		m_line2 = drawFact.constructLine(0, 0, 0, 0);
		m_guideLineCompound.addObject(m_line1);
		m_guideLineCompound.addObject(m_line2);
	}

	@Nullable @Override public String getStatusbarText()
	{
		return ResourceMgr.getString(MoveWireEndAction.class, "MoveWireEndAction.statusbar.text");
	}

	@Override public boolean isSnapThroughConnectorEnabled()
	{
		return getModel().getAutoGenerateConnectorMode() && isSnappingThroughConnectorAllowed();
	}

	protected boolean isSnappingThroughConnectorAllowed()
	{
		Set<Class<? extends ILogicObject>> validTypes = Set.of(IWireConductor.class, INetConductor.class);
		return validTypes.stream().anyMatch(type -> type.isAssignableFrom(getSnappingSourceClass()));
	}

	@Nullable @Override public ILogicObject getSnapSourceObject()
	{
		return m_selectedSegment.getConductor().getConnectivity();
	}

	@Override public boolean overrideLastSnapped()
	{
		return false;
	}

	@NotNull protected SnapHelper getSnapHelper()
	{
		if (m_snapHelper == null) {
			m_snapHelper = new SnapThroughConnectorHelper(this, true, false);
		}
		return m_snapHelper;
	}

	@Override public void keyTyped(KeyEvent e)
	{

	}

	@Override public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL && !m_ctrlPressed) {
			GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
			if (gfxView != null) {
				Point currentMousePoint = getCurrentMousePoint(gfxView);
				m_ctrlPressed = true;
				triggerMouseMoveEvent(gfxView, currentMousePoint, InputEvent.CTRL_DOWN_MASK);
			}
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL && m_ctrlPressed) {
			GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
			if (gfxView != null) {
				Point currentMousePoint = getCurrentMousePoint(gfxView);
				m_ctrlPressed = false;
				triggerMouseMoveEvent(gfxView, currentMousePoint, 0);
			}
		}
	}

	@NotNull private Point getCurrentMousePoint(@NotNull GfxView gfxView)
	{
		return gfxView.convertWorldPointToViewComponentPoint(gfxView.getCurrentMouseLocation());
	}

	private void triggerMouseMoveEvent(Component source, @NotNull Point point, int modifier)
	{
		mouseMoved(new MouseEvent(source, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
				modifier, point.x, point.y, 0, false));
	}
}