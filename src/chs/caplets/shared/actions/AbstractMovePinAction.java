/*
 * Copyright 2007-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
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
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.logic.actions.AbstractPinActionHelper;
import chs.caplets.logic.actions.MovePinActionUtils;
import chs.caplets.shared.DecrementSpace;
import chs.caplets.shared.IncrementSpace;
import chs.caplets.shared.PlaceAdjacent;
import chs.caplets.shared.ReverseDirection;
import chs.caplets.shared.ReversePinOrder;
import chs.cof.draw.HorizJustificationEnum;
import chs.cof.draw.ICircle;
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.IText;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.cof.draw.VertJustificationEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.MovePinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.common.Side;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.common.geom.GeometryUtils;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.PinPlaceholderProviderForSymbolledDeviceInMove;
import chs.utility.logic.ModularConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractMovePinAction extends ControllerActionRT
		implements MouseListener, MouseMotionListener, ICtxMenuProvider, KeyListener
{

	public static final int DEBUG_MATCHINGPIN_RADIUS = 300;
	// Return is state from PinPlacementConstraints
	protected abstract int validPinPlacement(Point point, IAbstractSchemPin pin);

	@Nullable protected abstract IGfxObject getMatchingTransientObject(Point point); // shows connected pin
	// This is computed at invoke time, and then reused to show the matching pin dyanmics.

	protected GeneratorParameters m_genParams;
	protected Map<IPinList, PinPlacementConstraintsHolder> m_constraintsHolders = new LinkedHashMap<>();
	protected IDynamicGfxService m_dynamics;

	@Nullable protected ModularSchemPinListInfo m_destPinList;
	// there is a one-to-one relationship between the next two arrays
	protected IAbstractSchemPin[] m_pins = null;
	protected Point[] m_pinLocations = null;

	protected Point m_tmpPoint = new Point(0, 0);
	protected boolean isVertical;
	@Nullable protected Point m_selectedPoint;
	protected Point m_currMousePoint;
	protected Map<IAbstractSchemPin, Side> m_pinSidesBeforeMove = new HashMap<IAbstractSchemPin, Side>();

	private IGfxObject[] m_pinPoints;
	private IText[] m_pinTexts;
	private IWritableGfxAttribute m_red;
	private IWritableGfxAttribute m_green;
	private Cursor m_addPinValidCursor = null;
	private Cursor m_addPinSwapCursor = null;
	private Cursor m_addPinInvalidCursor = null;

	private static final int MAX_COLOR = 255;
	protected static final int m_tmpPinSize = 200;

	private static final boolean DEBUG_MATCHING_OBJECTS = false;
	private static final boolean DEBUG_EXTENTS = false;
	private static final int BLUE = 0x000000FF;
	private static final int RED = 0xFFFF0000;
	private static final int GREEN = 0xFF00FF00;

	protected IGrid m_grid;
	private List<IGfxObject> transientGraphics = new ArrayList<IGfxObject>();

	protected boolean m_dragging;

	private Map<Integer, Consumer<Integer>> keyHandlers = new HashMap<>();
	private final SymDeviceTemporaryPlaceHolderCreationHelper symDevPlaceHolderCreationHelper;

	protected AbstractMovePinAction(ICapletController controller, @Nullable IPinList destPinList,
			@Nullable Point destPoint)
	{
		super(controller);

		// Create our "pivot point"
		IGfxModel model = (IGfxModel) controller.getCapletModel();
		m_dynamics = model.getDynamicGfxService();
		if (m_addPinValidCursor == null) {
			m_addPinValidCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_pin.gif", new Point(7, 7));
			m_addPinSwapCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_pin_swap.gif", new Point(7, 7));
			m_addPinInvalidCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_cantaddpin.gif", new Point(7, 7));
		}

		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		m_red = drawFactory.constructAttribute(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setFillBackgroundColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_red.setFillPattern(IFillPattern.PATTERN_SOLID);

		m_green = drawFactory.constructAttribute(drawFactory.lookupColor("pin"));
		m_green.setFillBackgroundColor(drawFactory.lookupColor("pin"));
		m_green.setFillPattern(IFillPattern.PATTERN_SOLID);

		m_destPinList = destPinList != null ? new ModularSchemPinListInfo(destPinList) : null;
		m_selectedPoint = destPoint;

		registerKeyHandlers();

		symDevPlaceHolderCreationHelper = new SymDeviceTemporaryPlaceHolderCreationHelper(this::getDeviceWithSymbol);
	}

	private void registerKeyHandlers()
	{
		keyHandlers.put(KeyEvent.VK_A, (keyCode) -> {
			new PlaceAdjacent().adjust(m_pinLocations, isVertical);
		});
		Consumer<Integer> consumerSpaceIncr = (keyCode) -> {
			new IncrementSpace().adjust(m_pinLocations, isVertical);
		};
		keyHandlers.put(KeyEvent.VK_1, consumerSpaceIncr);
		keyHandlers.put(KeyEvent.VK_NUMPAD1, consumerSpaceIncr);
		Consumer<Integer> consumerSpaceDecr = (keyCode) -> {
			new DecrementSpace().adjust(m_pinLocations, isVertical);
		};
		keyHandlers.put(KeyEvent.VK_2, consumerSpaceDecr);
		keyHandlers.put(KeyEvent.VK_NUMPAD2, consumerSpaceDecr);
		keyHandlers.put(KeyEvent.VK_R, (keyCode) -> {
			new ReverseDirection().adjust(m_pinLocations, isVertical);
		});
		keyHandlers.put(KeyEvent.VK_P, (keyCode) -> {
			new ReversePinOrder().adjust(m_pinLocations, isVertical);
		});
	}

	public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();
		Consumer<Integer> keyConsumer = keyHandlers.get(keyCode);
		if (keyConsumer != null) {
			keyConsumer.accept(keyCode);
			if (m_selectedPoint == null) {
				m_selectedPoint = m_currMousePoint;
			}
			triggerMouseMoved();
			e.consume();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public String getActionUIClass()
	{
		return AbstractMovePinActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		return hasOperands(getController().getSelectMgr().getPreSelections()) &&
				getController().getCapletModel().isEditable() && super.isEnabled();
	}

	private boolean isValidPinForMove(IAbstractSchemPin pin)
	{
		chs.cof.logical.cable.IPinList owner = ((IPinList) pin.getParent()).getConnectivity();
		boolean isNicePin;
		if (owner instanceof ISplice && owner.getSymbolRef() == null) {        // ignore splice [if it has no symbol]
			isNicePin = false;
		}
		else if (pin instanceof IPin && ((IConnectivityRef) pin).getConnectivity() instanceof IBackshellTermination) {
			isNicePin = true;
		}
		else {
			//melmorsy - FEAT12331
			//it's no more allowed to move a pin that belongs to an automatically created harness conenctor
			// while being under a design abstraction that allows automatic creation of harness connectors
			isNicePin = PinListHelper.isEditableHarnessConnector((IPinList) pin.getParent());
		}
		return isNicePin;
	}

	protected boolean hasOperands(SelectSet sset)
	{
		ModularSchemPinListInfo modularSchemPinListInfo = null;
		Integer prev_x = null;
		Integer prev_y = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!(obj instanceof IAbstractSchemPin)) {
				continue;
			}
			IAbstractSchemPin pin = (IAbstractSchemPin) obj;
			if (modularSchemPinListInfo == null) {
				IPinList pinlist = (IPinList) pin.getParent();

				if (pinlist == null) {
					return false;
				}

				//Move Pins action is not enabled for Ring Terminals
				if (IConnector.Statics.isRingTerminalTypeConnector(pinlist.getConnectivity())) {
					return false;
				}

				// All bets are off if the pinlist is not on the active diagram
				if (DiagramHelper.getDiagram(pinlist) != CAFUtils.getInstance().getActiveDiagram()) {
					return false;
				}

				modularSchemPinListInfo = new ModularSchemPinListInfo(pinlist);
			}
			else if (!modularSchemPinListInfo.getCandidates().contains(pin.getParent())) {
				return false;
			}

			if (!isValidPinForMove(pin)) {
				return false;
			}

			ILocation absLocation = pin.getAbsLocation();
			if (prev_x == null && prev_y == null) {
				//this is first pin.
				prev_x = absLocation.getX();
				prev_y = absLocation.getY();
			}
			else if (prev_x != null && prev_y != null) {
				//this is second pin. determine the orientation.
				if (prev_x == absLocation.getX()) {
					//vertical alignment
					prev_y = null;
				}
				else if (prev_y == absLocation.getY()) {
					//horizontal alignment
					prev_x = null;
				}
				else {
					return false;
				}
			}
			else if (prev_x != null) {
				if (prev_x != absLocation.getX()) {
					//breaking vertical alignment
					return false;
				}
			}
			else {
				if (prev_y != absLocation.getY()) {
					//breaking horizontal alignment
					return false;
				}
			}
		}

		IPinList anchor = modularSchemPinListInfo != null ? modularSchemPinListInfo.getAnchor() : null;
		if (anchor != null && modularSchemPinListInfo.isLogicObjectLockedInOtherSession()) {
			return false;
		}
		return anchor != null;
	}

	protected void setOperands(SelectSet sset)
	{
		m_pins = null;
		m_pinLocations = null;

		List<IAbstractSchemPin> pins = new ArrayList<IAbstractSchemPin>();
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if ((obj instanceof IAbstractSchemPin)) {
				if (isValidPinForMove((IAbstractSchemPin) obj)) {
					pins.add((IAbstractSchemPin) obj);
				}
			}
		}

		if (pins.isEmpty()) {
			return;
		}

		// save the selected pins
		m_pins = new IAbstractSchemPin[pins.size()];
		for (int i = 0; i < m_pins.length; i++) {
			m_pins[i] = pins.get(i);
		}
		//set the current schematic PinList of it was not set in the constructor
		if (m_destPinList == null) {
			IPinList candidate = (IPinList) m_pins[0].getParent();
			assert candidate != null;
			m_destPinList = new ModularSchemPinListInfo(candidate);
		}

		IExtent extent = ConnectionHelper.getAbsExtent(m_destPinList.getAnchor());

		ILocation movePinLocation = m_pins[0].getAbsLocation();
		isVertical = isPointOnVerticalSide(extent, new Point(movePinLocation.getX(), movePinLocation.getY()));

		// save the locations of the pins
		m_pinLocations = new Point[m_pins.length];
		for (int i = 0; i < m_pinLocations.length; i++) {
			ILocation loc = m_pins[i].getAbsLocation();
			m_pinLocations[i] = new Point(loc.getX(), loc.getY());
		}

		// init array for dynamic graphics of floating pins
		m_pinPoints = new IGfxObject[m_pinLocations.length];
		for (int i = 0; i < m_pinLocations.length; i++) {
			m_pinPoints[i] = FactoryMgr.getDrawFactory().constructRectangle(0, 0, m_tmpPinSize, m_tmpPinSize);
			m_pinPoints[i].setAttribute(m_red);
		}

		sort();
		normalize();
		m_pinTexts = new IText[m_pinLocations.length];
		for (int i = 0; i < m_pins.length; i++) {
			IText dummyPinNameText =
					FactoryMgr.getDrawFactory().constructText(0, 0, (3 * m_grid.getGridSpacing()) / 4, 0, "");
			dummyPinNameText.setHorizontalJustification(HorizJustificationEnum.JustMiddle);
			dummyPinNameText.setVerticalJustification(VertJustificationEnum.JustCenter);
			dummyPinNameText.setString(m_pins[i] instanceof IConnectivityRef ? evaluatePinName(m_pins[i]) : "");
			m_pinTexts[i] = dummyPinNameText;
		}
	}

	@Nullable private String evaluatePinName(@NotNull IAbstractSchemPin m_pin)
	{
		ILogicObject cablePin = ((IConnectivityRef) m_pin).getConnectivity();
		IAttribute nameAttribute = cablePin.getAttribute(IAttributeTypes.NAME);
		return nameAttribute != null ? nameAttribute.getAsString() : "";
	}

	private void swap(int i, int j)
	{
		IAbstractSchemPin tpin = m_pins[i];
		m_pins[i] = m_pins[j];
		m_pins[j] = tpin;

		Point tpoint = m_pinLocations[i];
		m_pinLocations[i] = m_pinLocations[j];
		m_pinLocations[j] = tpoint;
	}

	private void sort()
	{
		boolean stop;

		// sort on y axis
		do {
			stop = true;
			for (int i = 0; i < m_pinLocations.length - 1; i++) {
				if (m_pinLocations[i].y > m_pinLocations[i + 1].y) {
					stop = false;
					swap(i, i + 1);
				}
			}
		}
		while (!stop);

		// sort on x-axis
		do {
			stop = true;
			for (int i = 0; i < m_pinLocations.length - 1; i++) {
				if (m_pinLocations[i].x > m_pinLocations[i + 1].x) {
					stop = false;
					swap(i, i + 1);
				}
			}
		}
		while (!stop);
	}

	private void normalize()
	{
		int shiftx = m_pinLocations[0].x;
		int shifty = m_pinLocations[0].y;

		for (Point m_pinLocation : m_pinLocations) {
			m_pinLocation.x -= shiftx;
			m_pinLocation.y -= shifty;
		}
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AbstractMovePinAction.class, "MovePinAction.StatusBar.Msg");
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_pinSidesBeforeMove.clear();
		// Get a grid
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		IGriddable gridholder = (IGriddable) gview.getSheet();
		m_grid = gridholder.getGrid();
		// Iterate each selection and split the line
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		setOperands(preSelections);
		m_dynamics.removeAllDynamicGfx();
		removeTransientGfx();
		m_dragging = false;

		return IActionEnum.eActivated;
	}

	protected void removeTransientGfx()
	{
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		// commit
		getController().getActionMgr().terminateActiveAction(true);
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseMoved(MouseEvent e)
	{
		m_selectedPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_selectedPoint.setLocation(m_grid.snap(m_selectedPoint.x), m_grid.snap(m_selectedPoint.y));
		m_currMousePoint = m_selectedPoint;

		assert m_destPinList != null;
		//FEAT00013690 - Drag & Drop pin placement between object
		handleMouseMovedToAnotherInstance(e, m_destPinList.getAnchor());

		triggerMouseMoved();
	}

	protected void triggerMouseMoved()
	{
		assert m_destPinList != null;
		IExtent extent = ExtentHelper.getAbsExtent(m_destPinList.getAnchor());
		//Point point = PinListHelper.getRelativeToPinList(m_destPinList, m_selectedPoint);

		assert m_selectedPoint != null;
		Side currentMouseSide = getPointSide(extent, m_selectedPoint);

		if (m_pins.length > 1) {
			String popupTooltip = ResourceMgr.getString(AbstractMovePinAction.class,
					"MovePinAction.SpacingControl.tooltip", getNameForTooltip());
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			GfxView gview = (GfxView) view;
			Point tooltipDeviceLocation =
					AbstractPinActionHelper
							.determineTooltipDeviceLocation(gview, m_selectedPoint, currentMouseSide);
			gview.showTooltipAtLocation(popupTooltip, tooltipDeviceLocation);
		}

		Boolean currentVertical = currentMouseSide.isVertical();

		int valid = validPoints(m_selectedPoint, currentVertical);
		if ((valid & PinPlacementConstraintsHolder.PLACEMENT_NO) != 0) {
			valid = PinPlacementConstraintsHolder.PLACEMENT_NO;
		}
		if (valid == PinPlacementConstraintsHolder.PLACEMENT_YES) {
			//
			// Valid Location.
			//

			displayTransientPinNames(isVertical);
			setCursor(m_addPinValidCursor);
			setColor(m_green);
		}
		else if ((valid & PinPlacementConstraintsHolder.PLACEMENT_WILLSWAP) ==
				PinPlacementConstraintsHolder.PLACEMENT_WILLSWAP) {
			//
			// Valid Location, but will do some pin swapping...
			//
			displayTransientPinNames(isVertical);
			setCursor(m_addPinSwapCursor);
			setColor(m_green);
		}
		else {
			//
			// Invalid Location.
			//
			m_selectedPoint = null;
			setCursor(m_addPinInvalidCursor);
			setColor(m_red);
		}
		CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
	}

	@NotNull protected String getNameForTooltip()
	{
		return ResourceMgr.getString(AbstractMovePinAction.class, "MovePinAction.tooltipName.text");
	}

	private boolean isPointOnVerticalSide(IExtent extent, @NotNull Point point)
	{

		//Point point = PinListHelper.getRelativeToPinList(m_destPinList, m_selectedPoint);

		Side currentSide = getPointSide(extent, point);
		return currentSide.isVertical();
	}

	protected Side getPointSide(IExtent extent, @NotNull Point point)
	{
		Side currPointSide;
		if (extent.getLeft() == point.x || extent.getRight() == point.x) {
			if (extent.getLeft() == point.x) {
				currPointSide = Side.LEFT;
			}
			else {
				currPointSide = Side.RIGHT;
			}
		}
		else if (extent.getTop() == point.y || extent.getBottom() == point.y) {

			if (extent.getTop() == point.y) {
				currPointSide = Side.TOP;
			}
			else {
				currPointSide = Side.BOTTOM;
			}
		}
		else {
			Side side = Side.getSide(extent,
					FactoryMgr.getCommonFactory().constructLocation(point.x, point.y));
			currPointSide = side;
		}
		return currPointSide;
	}

	/**
	 * //FEAT00013690 - Drag & Drop pin placement between object checks if the mouse pointer moved to another instance
	 * or another valid pinlist for pin placement
	 */
	private void handleMouseMovedToAnotherInstance(MouseEvent e, IPinList oldPinList)
	{
		//Handle the case when the mouse moves to another instance of the pinllist
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (m_selectedPoint != null) {
			SelectSet selectedObjects = view.OnSelectPoint(m_selectedPoint, e, null);
			if (selectedObjects != null) {
				handleMouseMovedToAnotherInstance(selectedObjects, oldPinList);
			}
		}
	}

	protected final void handleMouseMovedToAnotherInstance(SelectSet selectedObjects, IPinList oldPinList)
	{
		//filter selections to only pin or pinlist objects
		SelectionFilter pinListFitler = new SelectionFilter();
		pinListFitler.addOnlyClass(IPinList.class);
		pinListFitler.addOnlyClass(IPin.class);
		List<Selection> selectionsList = selectedObjects.getFilteredSelections(pinListFitler);

		if (!selectionsList.isEmpty()) {
			Selection s = selectionsList.iterator().next();
			IUIDObject selectedObject = s.getObject();
			IPinList newPinList;
			if (selectedObject instanceof IPin) {
				//if the cursor moved to a pin get the parent pinlist
				newPinList = (IPinList) ((IDiagramObject) selectedObject).getParent();
			}
			else {
				newPinList = (IPinList) s.getObject();
			}
			handleMouseMovedToAnotherInstance(oldPinList, newPinList);
		}
	}

	protected final void handleMouseMovedToAnotherInstance(@Nullable IPinList oldPinList, @Nullable IPinList newPinList)
	{
		//dts0101306213/LOGIC-7036 Performance issue while moving harness connector pin on device
		IPinList oldAnchor = oldPinList != null ? new ModularSchemPinListInfo(oldPinList).getAnchor() : null;
		IPinList newAnchor = newPinList != null ? new ModularSchemPinListInfo(newPinList).getAnchor() : null;
		if (newAnchor != null && oldAnchor != null && oldAnchor != newAnchor &&
				isValidPinListForPinMove(newAnchor, oldAnchor)) {
			changeDestPinList(newAnchor);
		}
	}

	/**
	 * returns true if the pins array contains pins that are on the opposite sides of the pinlist
	 *
	 * @param pins moving pins
	 * @param pinSides a map containting pin along with it's side on the pinlist
	 *
	 * @return if on opposite side
	 */
	private boolean areMovingPinsOnOppositeSides(IAbstractSchemPin[] pins, Map<IAbstractSchemPin, Side> pinSides)
	{
		if (pins.length > 1) {
			Set<Side> movingPinsSides = new HashSet<Side>();
			for (IAbstractSchemPin movingPin : pins) {
				movingPinsSides.add(pinSides.get(movingPin));
			}
			return movingPinsSides.contains(Side.LEFT) && movingPinsSides.contains(Side.RIGHT) ||
					movingPinsSides.contains(Side.TOP) && movingPinsSides.contains(Side.BOTTOM);
		}

		return false;
	}

	protected boolean isValidPinListForPinMove(@NotNull IPinList newPinList, @NotNull IPinList currentPinList)
	{
		if (newPinList == currentPinList) {
			return true;
		}
		if (areMovingPinsOnOppositeSides(m_pins, m_pinSidesBeforeMove)) {
			//moving pins that are on opposite sides is not available accross different instances of the pinlist
			//as the width of the destination pin list can be different than the source pinlist
			return false;
		}
		//We cant move a pin from or to a device with symbol
		if (!isSymbol(currentPinList) && !isSymbol(newPinList)) {
			//mouse moved to a different schematic PinList
			IPinList movedPinParent = (IPinList) m_pins[0].getParent();
			assert movedPinParent != null;
			chs.cof.logical.cable.IPinList newCablePinList =
					MovePinActionUtils.determineModularRoot(newPinList.getConnectivity());
			chs.cof.logical.cable.IPinList oldCablePinList =
					MovePinActionUtils.determineModularRoot(movedPinParent.getConnectivity());
			if (newCablePinList == oldCablePinList) {
				if (PinPlacementHelper
						.areConnectorsConnectedToDifferentSymbolledDeviceInstances(newPinList, movedPinParent)) {
					return false;
				}
				//dts0100958697 Pin movement of a plug-jack connector pair is incorrect.
				return !(oldCablePinList instanceof IConnector) ||
						MovePinActionUtils.isMovementAcrossConnectorsValid(newPinList, m_pins);
			}
//			else if (newCablePinList instanceof IConnector && oldCablePinList instanceof IConnector) {
//				//we moved from connector attached to device1
//				//to another connector attached to another instance of device1
//				//TODO nagamani - this is true ONLY for device owned connectors.
//				if (isDeviceOwnedConnector(newPinList) && isDeviceOwnedConnector(movedPinParent)) {
//					IPinList devAttachedToFirstConnector = PinPlacementHelper
//							.getAttachedDevice(((IDeviceOwnedConnector) movedPinParent.getConnectivity()).getOwner(
//									IDevice.class),
//									movedPinParent);
//					if (devAttachedToFirstConnector != null) {
//						IPinList devAttachedToSecondConnector = PinPlacementHelper
//								.getAttachedDevice(((IDeviceOwnedConnector) newPinList.getConnectivity()).getOwner(
//										IDevice.class),
//										newPinList);
//						if (devAttachedToSecondConnector != null) {
//							if (devAttachedToFirstConnector.getConnectivity() ==
//									devAttachedToSecondConnector.getConnectivity()) {
//								//Device Pins on Symbols can't be moved
//								if ((devAttachedToFirstConnector != devAttachedToSecondConnector) &&
//										isSymbol(devAttachedToFirstConnector) ||
//										isSymbol(devAttachedToSecondConnector)) {
//									return false;
//								}
//
//								return isValidConnectorForMovingPins(m_pins, newPinList);
//							}
//						}
//					}
//				}
//			}
		}
		return false;
	}

	protected boolean isValidConnectorForMovingPins(@NotNull Set<IAbstractSchemPin> movingPins, IPinList destConnector)
	{
		for (IAbstractSchemPin pin : movingPins) {
			if (!PinPlacementHelper.isValidConnectorForMove(pin, destConnector.getConnectivity())) {
				return false;
			}
		}
		return true;
	}

	private boolean isSymbol(IPinList pinList)
	{
		return pinList.getParameterized() == null;
	}

	protected void addTempPlaceHolderForDevicesWithSymbols(ModularSchemPinListInfo pinList)
	{
		symDevPlaceHolderCreationHelper.addTempPlaceHolderForDevicesWithSymbols(pinList, m_grid, m_genParams);
	}

	protected void removeTempPlaceHoldersForDevicesWithSymbols(ModularSchemPinListInfo pinList)
	{
		symDevPlaceHolderCreationHelper.removeTempPlaceHoldersForDevicesWithSymbols(pinList);
	}

	protected PinPlacementConstraintsHolder getPinPlacementContraints(IPinList schemParentPinlist,
			boolean showBoundaryExt)
	{
		IAbstractSchemPin[] pins = new IAbstractSchemPin[m_pins.length];
		int i = 0;
		for (IAbstractSchemPin pin : m_pins) {
			pins[i] = pin;
			i++;
		}
		final PinPlacementConstraints placementConstraints =
				new MovePinPlacementConstraints(schemParentPinlist, pins, m_genParams.getSpacing(), showBoundaryExt,
						false, false);
		return placementConstraints.getHolder();
	}

	protected void addPinPlacementConstraints(IPinList schemParentPinlist, boolean showBoundaryExt)
	{
		// *** important we can not assume that m_pins[0].getParent() == schemParentPinlist, this is true when moving
		// *** a device pin, but not when moving a connector pin. Since we create PinPlacementConstraints for each
		// *** connector attached to a device.
		final PinPlacementConstraintsHolder holder = getPinPlacementContraints(schemParentPinlist, showBoundaryExt);
		for (IGfxObject tobj : holder.getBoundaryExtensions()) {
			m_dynamics.addTransientGfx(tobj);
		}
		for (IGfxObject circle : holder.getValidMovePositions()) {
			m_dynamics.addTransientGfx(circle);
		}

		// Enable to debug matching pins positions
		if (DEBUG_MATCHING_OBJECTS) {
			for (IGfxObject circle : debugMatchingPinsList(holder)) {
				m_dynamics.addTransientGfx(circle);
			}
		}
		if (DEBUG_EXTENTS) {
			m_dynamics.addTransientGfx(PinPlacementHelper.constructPinlistNoTextExtent(schemParentPinlist, BLUE));
			m_dynamics.addTransientGfx(PinPlacementHelper.constructPinlistExtent(schemParentPinlist, RED));
			m_dynamics
					.addTransientGfx(PinPlacementHelper.constructExtentHelperNonTextExtent(schemParentPinlist, GREEN));
			m_dynamics.addTransientGfx(PinPlacementHelper.constructExtentHelperPinExtent(schemParentPinlist, GREEN));
			m_dynamics.addTransientGfx(
					PinPlacementHelper.constructExtentHelperPinAndPlaceHolderExtent(schemParentPinlist, RED));
		}

		// show the initial matching pin. This is only shown if we support mated pin moves
		for (IGfxObject circle : holder.matchingPins(m_pins)) {
			m_dynamics.addTransientGfx(circle);
		}

		// Store this constraint. We have mutiple constraints when moving connector pins.
		m_constraintsHolders.put(schemParentPinlist, holder);
	}

	private Collection<IGfxObject> debugMatchingPinsList(PinPlacementConstraintsHolder holder)
	{
		IDrawFactory drawfac = FactoryMgr.getDrawFactory();
		IWritableGfxAttribute matchingPinAttr =
				drawfac.constructGfxAttribute(drawfac.lookupColor("transient"), 2, LineStyle.NO_STYLE);
		IColor blue = drawfac.constructColorRGB(BLUE);
		matchingPinAttr.setColor(blue);

		Collection<IGfxObject> values = holder.getMatchingValues();
		Collection<IGfxObject> debugMatchingPinsList = new ArrayList<IGfxObject>(values.size());
		for (IGfxObject gobj : values) {
			if (gobj != null) {
				Point pt = PinPlacementHelper.getTransformedCoord(gobj);
				ICircle circle = drawfac.constructCircle(pt.x, pt.y, DEBUG_MATCHINGPIN_RADIUS);
				circle.setAttribute(matchingPinAttr);
				debugMatchingPinsList.add(circle);
			}
		}
		return debugMatchingPinsList;
	}

	/**
	 * add pin plcaement constraints for connector attached to a device and all the other attached connectors
	 *
	 * @param schemParentPinlist the connector pinlist
	 */
	protected void handlePinPlacementConstraints(@NotNull ModularSchemPinListInfo schemParentPinlist)
	{
		Set<IPinList> attachedDevices = new HashSet<>();
		IPinList anchorConnector = schemParentPinlist.getAnchor();
		Set<IPinList> modularCandidates = schemParentPinlist.getCandidates();
		for (IPinList candidate : modularCandidates) {
			IPinList theAttachedDevice = PinPlacementHelper.getAttachedDevice(candidate);
			if (theAttachedDevice != null) {
				attachedDevices.add(theAttachedDevice);
			}
		}

		if (attachedDevices.size() == 1) {
			IPinList theAttachedDevice = attachedDevices.iterator().next();
			// Get all the attached connectors for the device and add placeholders for valid connectors
			Set<IPinList> allAttachedConnectors = new HashSet<>();
			for (IPinList theAttachedConnector : theAttachedDevice.getAttachedPinListObjects()) {
				if (!allAttachedConnectors.contains(theAttachedConnector) &&
						theAttachedConnector.getConnectivity() instanceof IHarnessPlugConnector) {
					allAttachedConnectors.addAll(new ModularSchemPinListInfo(theAttachedConnector).getCandidates());
				}
			}
			chs.cof.logical.cable.IPinList modularRoot =
					MovePinActionUtils.determineModularRoot(anchorConnector.getConnectivity());
			SetMap<chs.cof.logical.cable.IPinList, IAbstractSchemPin> distributedPins = SetMap.createShallowSetMap();
			for (IAbstractSchemPin pin : m_pins) {
				IPinList parent = (IPinList) pin.getParent();
				assert parent != null;
				distributedPins.add(parent.getConnectivity(), pin);
			}
			//this check is done only for same modular connector in connectvity.
			//check for only the schempins matching with connectivity.
			Set<IPinList> allowedAttachedConns = new HashSet<>();
			for (IPinList theAttachedConnector : allAttachedConnectors) {
				Set<IAbstractSchemPin> movingPins =
						distributedPins.pullReadOnlySafeSet(theAttachedConnector.getConnectivity());
				if (MovePinActionUtils.determineModularRoot(theAttachedConnector.getConnectivity()) == modularRoot
						&& isValidConnectorForMovingPins(movingPins, theAttachedConnector)) {
					allowedAttachedConns.add(theAttachedConnector);
				}
			}

			//create constraints only using anchor. the constraints holder can handle modular schematics.
			Map<IPinList, ModularSchemPinListInfo> modularGroup = new HashMap<>();
			ModularConnectorHelper.generateModularGrouping(allowedAttachedConns, modularGroup);
			for (IPinList theAttachedConnector : modularGroup.keySet()) {
				boolean showBoundExt = modularCandidates.contains(theAttachedConnector);
				addPinPlacementConstraints(theAttachedConnector, showBoundExt);
			}
		}
		else {
			addPinPlacementConstraints(anchorConnector, true);
		}
	}

	/**
	 * //FEAT00013690 - Drag & Drop pin placement between object changes the destination pinlist for pin movement and
	 * add the pinplacement constraints on it
	 */
	protected void changeDestPinList(@NotNull IPinList newPinList)
	{
		assert m_destPinList != null;
		removeTempPlaceHoldersForDevicesWithSymbols(m_destPinList);

		m_destPinList = new ModularSchemPinListInfo(newPinList);

		addTempPlaceHolderForDevicesWithSymbols(m_destPinList);

		//remove the PinPlacementConstraints from the first instance
		m_constraintsHolders.clear();
		removeTransientGfx();

		handlePinPlacementConstraints(m_destPinList);
	}

	@Nullable protected IPinList getDeviceWithSymbol(IPinList pinList)
	{
		// can happen if on terminate the action was unsucessfull
		if (m_pins == null) {
			return null;
		}

		return PinPlaceholderProviderForSymbolledDeviceInMove.getDeviceWithSymbol(pinList);
	}

	private void setColor(IGfxAttribute color)
	{
		for (int i = 0; i < m_pinLocations.length; i++) {
			m_pinPoints[i].setAttribute(color);
		}
	}

	private static String getLocationKey(int x, int y)
	{
		return x + "," + y;
	}

	private void displayTransientPinNames(boolean vertical)
	{
		for (int i = 0; i < m_pinTexts.length; i++) {
			IText nameText = m_pinTexts[i];
			IExtent ext = nameText.getExtent();
			ILocation loc = nameText.getLocation();
			ext.setBounds(0, 0, ext.getWidth(), ext.getHeight());
			ILocation pinLocation = m_pinPoints[i].getLocation();

			int textOffset = m_grid.getGridSpacing() / 2;
			if (vertical) {
				loc.setLocation(pinLocation.getX(), pinLocation.getY() + textOffset);
				nameText.setHorizontalJustification(HorizJustificationEnum.JustRight);
				nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
				nameText.setRotation(GeometryUtils.NINETY_DEGREES);
			}
			else {
				loc.setLocation(pinLocation.getX() + textOffset, pinLocation.getY());
				nameText.setHorizontalJustification(HorizJustificationEnum.JustLeft);
				nameText.setVerticalJustification(VertJustificationEnum.JustCenter);
				nameText.setRotation(GeometryUtils.ZERO_DEGREES);
			}
			m_dynamics.addTransientGfx(nameText);
			transientGraphics.add(nameText);
		}
	}

	protected int validPoints(Point selectedPoint, @Nullable Boolean currentIsVertical)
	{
		clearTransientGraphics();

		int valid = PinPlacementConstraintsHolder.PLACEMENT_YES;
		for (int i = 0; i < m_pinLocations.length; i++) {
			if (currentIsVertical != null && (isVertical ^ currentIsVertical)) {

				int currentX = m_pinLocations[i].x;
				m_pinLocations[i].x = m_pinLocations[i].y;
				m_pinLocations[i].y = currentX;
			}

			m_tmpPoint.x = selectedPoint.x + m_pinLocations[i].x - m_tmpPinSize / 2;
			m_tmpPoint.y = selectedPoint.y + m_pinLocations[i].y - m_tmpPinSize / 2;
			// setup some graphics while we are at it...
			m_pinPoints[i].getLocation().setLocation(m_tmpPoint.x, m_tmpPoint.y);
			m_dynamics.addTransientGfx(m_pinPoints[i]);

			m_tmpPoint.setLocation(m_grid.snap(m_tmpPoint.x), m_grid.snap(m_tmpPoint.y));

			// setup matching location graphics, this will show connected pin drawghost
			IGfxObject matchObject = getMatchingTransientObject(m_tmpPoint);
			if (matchObject != null && ConnectionHelper.isPinConnected(m_pins[i])) {
				transientGraphics.add(matchObject);
				m_dynamics.addTransientGfx(matchObject);
			}

			valid |= validPinPlacement(m_tmpPoint, m_pins[i]);
		}
		isVertical = currentIsVertical != null ? currentIsVertical : isVertical;

		return valid;
	}

	private void clearTransientGraphics()
	{
		for (IGfxObject gobj : transientGraphics) {
			m_dynamics.removeTransientGfx(gobj);
		}
		transientGraphics.clear();
	}

	/**
	 * Finds the offset of the closest pin to the given point.
	 */
	public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
		m_dragging = true;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it
		if (hasOperands(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
