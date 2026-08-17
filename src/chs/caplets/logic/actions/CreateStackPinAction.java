/*
 * Copyright 2012 Mentor Graphics Corporation
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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.shared.actions.CreateStackPinActionUI;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.MovePinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.ICommonFactory;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListMap;
import chs.utility.DiagramHelper;
import chs.utility.UnitTestDataCaptureHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.StackedPinHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 8 Feb, 2011 Time: 2:20:22 PM To change this template use File |
 * Settings | File Templates.
 */
public class CreateStackPinAction extends AbstractCreateStackPinAction
{

	private boolean m_ctrlDown = false;

	public CreateStackPinAction(ICapletController controller)
	{
		this(controller, null, null);
	}

	protected CreateStackPinAction(ICapletController controller, IPinList destPinList, Point destPoint)
	{
		super(controller, destPinList, destPoint);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_ctrlDown = false;
		return super.onActivate(e);
	}

	protected void addPinPlacementConstraints(IPinList schemParentPinlist)
	{
		addPinPlacementConstraints(schemParentPinlist, true);
	}

	protected void addPinPlacementConstraints(IPinList schemParentPinlist, boolean showBoundaryExt)
	{
		// *** important we can not assume that m_pins[0].getParent() == schemParentPinlist, this is true when moving
		// *** a device pin, but not when moving a connector pin. Since we create PinPlacementConstraints for each
		// *** connector attached to a device.

		final PinPlacementConstraints placementConstraints =
				new MovePinPlacementConstraints(schemParentPinlist, m_pins, m_genParams.getSpacing(), showBoundaryExt,
						false, true);
		final PinPlacementConstraintsHolder holder = placementConstraints.getHolder();
		for (IGfxObject tobj : holder.getBoundaryExtensions()) {
			m_dynamics.addTransientGfx(tobj);
		}
		for (IGfxObject circle : holder.getValidMovePositions()) {
			ILocation loc = circle.getLocation();
			IGfxObject obj = holder.getObjectAt(new Point(loc.getX(), loc.getY()));
			if (obj instanceof IPin) {
				IPin pin = (IPin) obj;
				for (IPin m_pin : m_pins) {
					if (pin == m_pin) {
						m_dynamics.addTransientGfx(circle);
					}
				}
			}
			else if (obj instanceof ISchemStackPin) {
				if (m_ctrlDown) {
					m_dynamics.addTransientGfx(circle);
				}
			}
			else {
				m_dynamics.addTransientGfx(circle);
			}
		}

		// show the initial matching pin. This is only shown if we support mated pin moves
		for (IGfxObject circle : holder.matchingPins(m_pins)) {
			m_dynamics.addTransientGfx(circle);
		}

		// Store this constraint. We have mutiple constraints when moving connector pins.
		m_constraintsHolders.put(schemParentPinlist, holder);
	}

	protected boolean editModel()
	{
		IPinList anchor = m_destPinList.getAnchor();
		chs.cof.logical.cable.IPinList cablePL = anchor.getConnectivity();
		ISchemDiagram diagram = DiagramHelper.getDiagram(anchor);
		if (diagram == null) {
			return false;
		}

		IUIDObject container = cablePL.getDesignContainer();
		if (!(container instanceof IDesign)) {
			return false;
		}

		// Creates properties required for unit testing if "Unit Test Data Capture" is enabled in DEBUG mode
		createProperiesForUnitTest(UnitTestDataCaptureHelper.ActionType.CREATE);

		ISchemStackPin stackpin = constructSchemStackPin(anchor, diagram);

		IPinList destPinList = (IPinList) stackpin.getParent();
		assert destPinList != null;

		//Add selected pins to StackPin
		StackPinActionHelper.addPinsToStackPin(destPinList, stackpin, m_pins);

		Generator generator = Generator.getGenerator();

		//If there exists some connected pinlists...Do create/update equivalent stack pin on them
		ISchemStackPin matedStackedPin =
				StackPinActionHelper.createMatedStackedPin(m_pins, stackpin, generator, m_genParams);
		//remove the selected schem pins from the parent pinlist
		Set<IPinList> parents = StackPinActionHelper.removeSchemPins(m_pins);

		generator.generate(destPinList, m_genParams, Generator.NOREGENERATE_PROPERTIES, false);

		for (IPinList parent : parents) {
			parent.regenerateDiagramObject();
		}

		if (PinListHelper.isHarnessFootprintedAndAllowAutoCreation(destPinList)) {
			GenerateHarnessConnActionHelper harnessGenerator = new GenerateHarnessConnActionHelper(diagram);
			harnessGenerator.generateHarnessConnectorsForPinlist(destPinList);
		}
		//device connector type of footprint will also be performing ghc now.
		if (cablePL instanceof IDevice) {
			generator.rebuildDeviceConnectors(destPinList, m_genParams, null);
		}
		AddPinActionModel model = new AddPinActionModel(destPinList);
		if (matedStackedPin != null) {
			IDiagramObject diagramObject = matedStackedPin.getParent();
			if (diagramObject instanceof IPinList) {
				model.addMatePinList(destPinList, (IPinList) diagramObject);
			}
		}
		model.registerNewPin(stackpin, matedStackedPin);
		ObjectConnectionsGetter.createConnectionSchematics(model, diagram);
		ConductorRouteAction.getInstance().addPinForRoute(stackpin);

		Collection<ILogicObject> objects = getLocableMulticoresAndShields(generator, diagram);

		if (!lockObjects(getDesign(), objects)) {
			return false;
		}
		diagram.refreshRepresentations();
		return true;
	}

	private ISchemStackPin constructSchemStackPin(IPinList anchor, ISchemDiagram diagram)
	{
		// Check if there is a StackPin at that location
		ISchemStackPin stackpin = getSelectedStackPin();
		if (stackpin != null) {
			return stackpin;
		}

		ILocation stackLoc = CoordinateHelper.getRelativeLocation(anchor, m_selectedPoint.x, m_selectedPoint.y);
		chs.cof.logical.cable.IPinList cablePinlist = determineCablePinlist(m_pins[0]);
		ListMap<IPinList, StackPinArgs> distributedPinArgs = new ListMap<>();
		Point relPt = new Point(stackLoc.getX(), stackLoc.getY());
		StackPinArgs stackPinArg = new StackPinArgs(relPt, cablePinlist);
		List<StackPinArgs> stackPinArgs = Collections.singletonList(stackPinArg);

		if (cablePinlist instanceof IConnector) {
			ConnectorHelper.distributeAddPinArgsToPinLists(anchor, diagram,
					stackPinArgs, (pl, a) -> distributedPinArgs.add(pl, (StackPinArgs) a));
			assert distributedPinArgs.keySet().size() == 1;
		}
		else {
			distributedPinArgs.addAll(anchor, stackPinArgs);
		}

		IPinList tgtPinList = distributedPinArgs.keySet().iterator().next();
		// Create a new StackPin.
		Point2D stackPinArgPoint = stackPinArg.getPoint();
		int x = CommonUtils.toInteger(stackPinArgPoint.getX());
		int y = CommonUtils.toInteger(stackPinArgPoint.getY());
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		stackpin = StackedPinHelper.createAndAddStackPin(tgtPinList, commonFactory.constructLocation(x, y));
		RegenerateGraphicsAction.getInstance().addObjectForRegenrate(stackpin);
		return stackpin;
	}

	@Override Set<ILogicObject> getLockableObjects()
	{
		Set<ILogicObject> lockableObjects = new HashSet<>();
		for (IPinList candidate : m_destPinList.getCandidates()) {
			lockableObjects.add(candidate.getConnectivity());
		}
		lockableObjects.addAll(getLockableConductorsAndHighways(m_pins));
		return lockableObjects;
	}

	@Override public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
		m_dragging = true;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		didMouseMove = true;
		m_selectedPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_selectedPoint.setLocation(m_grid.snap(m_selectedPoint.x), m_grid.snap(m_selectedPoint.y));

		int mouseMods = e.getModifiers();
		int ctrlMask = InputEvent.CTRL_MASK;
		m_ctrlDown = ((mouseMods & ctrlMask) == InputEvent.CTRL_MASK);

		int valid = validPoints(m_selectedPoint);
		if ((valid & PinPlacementConstraintsHolder.PLACEMENT_NO) != 0) {
			valid = PinPlacementConstraintsHolder.PLACEMENT_NO;
		}
		if (valid == PinPlacementConstraintsHolder.PLACEMENT_YES) {
			// Valid Location.
			setCursor(m_addStackPinValidCursor);
			setColor(m_greenAttr);

			createConnectionTransientGraphics();
		}
		else {
			// Invalid Location.
			m_selectedPoint = null;
			setCursor(m_addStackPinInvalidCursor);
			setColor(m_redAttr);
		}
		CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
	}

	private void setColor(IGfxAttribute color)
	{
		dyn.setAttribute(color);
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it
		if (hasOperands(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	protected int validStackPinLocation(Point point)
	{
		for (Map.Entry<IPinList, PinPlacementConstraintsHolder> entry : m_constraintsHolders.entrySet()) {
			PinPlacementConstraintsHolder holder = entry.getValue();
			IGfxObject obj = holder.getObjectAt(point);
			if (obj instanceof IPinPlaceholder && StackPinActionHelper.isValidToAddToStackedPin(m_pins, obj)) {
				return PinPlacementConstraintsHolder.PLACEMENT_YES;
			}
			if (obj instanceof ISchemStackPin) {
				if (m_ctrlDown && StackPinActionHelper.isValidToAddToStackedPin(m_pins, obj)) {
					return PinPlacementConstraintsHolder.PLACEMENT_YES;
				}
			}
			else if (obj instanceof IPin) {
				IPin pin = (IPin) obj;
				for (IPin m_pin : m_pins) {
					if (pin == m_pin) {
						return PinPlacementConstraintsHolder.PLACEMENT_YES;
					}
				}
			}
			//should be able to place along the bounday extensions
			if (holder.allowOnBoundaryExt(point) == PinPlacementConstraintsHolder.PLACEMENT_YES) {
				return PinPlacementConstraintsHolder.PLACEMENT_YES;
			}
		}
		return PinPlacementConstraintsHolder.PLACEMENT_NO;
	}

	protected boolean hasOperands(SelectSet sset)
	{

		OperandFinder operandFinder = new OperandFinder();
		boolean valid = true;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof ISchemStackPin) {
				valid = false;
				break;
			}
			if (!(obj instanceof IPin)) {
				continue;
			}

			IPin pin = (IPin) obj;

			if (!operandFinder.processPin(pin)) {
				valid = false;
				break;
			}
		}

		if (!valid) {
			return false;
		}

		IPinList pinlist = operandFinder.getPinlist();
		Set<IAbstractPin> pins = operandFinder.getPins();

		if (pinlist != null) {
			if (!PinListHelper.isEditableHarnessConnector(pinlist)) {
				return false;
			}
			if (!StackedPinHelper.isPinSetOnSameFootprintConnector(pinlist, pins)) {
				return false;
			}
			return operandFinder.areLocksAvailable();
		}
		return false;
	}

	@Override public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		if (selectionHasObjectsFromNonActiveDiagram()) {
			return false;
		}

		boolean hasValidOperands = hasOperands(getController().getSelectMgr().getPreSelections());
		return hasValidOperands && getController().getCapletModel().isEditable();
	}

	@Override public String getActionUIClass()
	{
		return CreateStackPinActionUI.class.getName();
	}

	private static class OperandFinder
	{

		private IPinList pinlist = null;
		private IPinList matedPilist = null;
		private chs.cof.logical.cable.IPinList devConnPinList = null;
		private StackPinActionHelper.MATING_STATE pinMatingState;
		private StackPinActionHelper.MATING_STATE devConnPinMatingState;
		private Set<IUID> connPinSet = new HashSet<IUID>();  //List of connectivity UID of pins
		private Set<IAbstractPin> pins = new HashSet<IAbstractPin>();

		OperandFinder()
		{
			pinMatingState = StackPinActionHelper.MATING_STATE.MIX;
			devConnPinMatingState = StackPinActionHelper.MATING_STATE.MIX;
		}

		private boolean areLocksAvailable()
		{
			Set<ILogicObject> logicObjects = getLockableObjects();

			for (ILogicObject lokable : logicObjects) {
				if (LogicObjectLockFinder.isLogicObjectLockedInOtherSession(lokable)) {
					return false;
				}
			}
			return true;
		}

		@NotNull private Set<ILogicObject> getLockableObjects()
		{
			Set<ILogicObject> logicObjects = new HashSet<>();
			if (pinlist != null) {
				logicObjects.add(pinlist.getConnectivity());
			}
			if (matedPilist != null) {
				logicObjects.add(matedPilist.getConnectivity());
			}

			for (IAbstractPin pin : pins) {
				for (IConductor conductor : pin.getConductors()) {
					logicObjects.add(conductor);
				}
			}
			return logicObjects;
		}

		private boolean processPin(IPin pin)
		{

			if (pin.getConnectivity().isInterconnect()) {
				return false;
			}

			if (!connPinSet.add(pin.getConnectivityUID())) {
				return false;
			}
			if (pinlist == null) {
				pinlist = (IPinList) pin.getParent();
			}
			assert pinlist != null;
			if (!StackPinActionHelper.isPinValidToAddToStack(pinlist, pin)) {
				return false;
			}

			//For sure, this pin cannot have >1 connected schem pins. Such pins would have already been filtered in StackPinActionHelper.isPinValidToAddToStack
			IPin matepin = PinPlacementHelper.getSingleConnectedSchemPin(pin);

			StackPinActionHelper.MATING_STATE currentMatingState = getMatingState(matepin != null);
			if (pinMatingState == StackPinActionHelper.MATING_STATE.MIX) {
				// If it is first iteration
				pinMatingState = currentMatingState;
			}
			else if (pinMatingState != currentMatingState) {
				return false;
			}

			if (matepin != null) {
				IDiagramObject pinList = matepin.getParent();
				if (pinList != null) {
					if (matedPilist == null) {  //first time => populate the matedPilist info
						matedPilist = (IPinList) pinList;
					}
					if (!isValidMate(matedPilist, matepin, (IConnectivityRef) pinList)) {
						return false;
					}
				}
			}
			//this action remains disable if the selected pins are a mix of pins with and without matepins
			if ((matepin != null && matedPilist == null)) {
				return false;
			}

			if (pin.getConnectivity() instanceof IDevicePin) {
				IDevicePin devPin = (IDevicePin) pin.getConnectivity();
				IDeviceConnPin devConnectorPin = devPin.getDeviceConnectorPin();

				StackPinActionHelper.MATING_STATE currentDevConnState = getMatingState(devConnectorPin != null);

				if (devConnPinMatingState == StackPinActionHelper.MATING_STATE.MIX) {
					// If it is first iteration
					devConnPinMatingState = currentDevConnState;
				}
				else if (devConnPinMatingState != currentDevConnState) {
					return false;
				}

				if (devConnectorPin != null) {
					if (devConnPinList == null) {  //first time => populate the matedPilist info
						devConnPinList = devConnectorPin.getOwner();
					}
					else if (devConnPinList != devConnectorPin.getOwner()) {
						return false;
					}
				}
				//this action remains disable if the selected pins are a mix of pins with and without matepins
				if ((devConnectorPin != null && devConnPinList == null)) {
					return false;
				}
			}
			pins.add(pin.getConnectivity());

			return true;
		}

		public Set<IAbstractPin> getPins()
		{
			return pins;
		}

		public IPinList getPinlist()
		{
			return pinlist;
		}

		@NotNull private StackPinActionHelper.MATING_STATE getMatingState(boolean hasMate)
		{
			return hasMate ? StackPinActionHelper.MATING_STATE.MATING :
					StackPinActionHelper.MATING_STATE.NON_MATING;
		}

		private boolean isValidMate(IPinList mate, IPin matepin, IConnectivityRef pinList)
		{
			if (!StackPinActionHelper.isPinValidToAddToStack(mate, matepin)) {
				return false;
			}
			return matedPilist.getConnectivity() == pinList.getConnectivity();
		}
	}
}
