/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.shared.actions.AddToStackPinActionUI;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.MovePinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utility.UnitTestDataCaptureHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.StackedPinHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 3 Mar, 2011 Time: 1:06:35 PM To change this template use File |
 * Settings | File Templates.
 */

public class AddToStackPinAction extends AbstractCreateStackPinAction
{

	public AddToStackPinAction(ICapletController controller)
	{
		this(controller, null, null);
	}

	protected AddToStackPinAction(ICapletController controller, IPinList destPinList, Point destPoint)
	{
		super(controller, destPinList, destPoint);
	}

	protected void addPinPlacementConstraints(IPinList schemParentPinlist)
	{
		addPinPlacementConstraints(schemParentPinlist, false);
	}

	protected void addPinPlacementConstraints(IPinList schemParentPinlist, boolean showBoundaryExt)
	{
		final PinPlacementConstraints placementConstraints =
				new MovePinPlacementConstraints(schemParentPinlist, m_pins, m_genParams.getSpacing(), showBoundaryExt,
						false, false);
		final PinPlacementConstraintsHolder holder = placementConstraints.getHolder();
		chs.cof.logical.cable.IPinList connector = determineCablePinlist(m_pins[0]);
		for (IGfxObject circle : holder.getValidMovePositions()) {

			ILocation loc = circle.getLocation();
			IGfxObject obj = holder.getObjectAt(new Point(loc.getX(), loc.getY()));
			if (obj instanceof ISchemStackPin && isMatchingStackPin((ISchemStackPin) obj, connector)
					&& StackPinActionHelper.isValidToAddToStackedPin(m_pins, obj)) {
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
		IUIDObject container = cablePL.getDesignContainer();
		if (!(container instanceof IDesign)) {
			return false;
		}

		ISchemDiagram diagram = anchor.getDiagram();
		if (diagram == null) {
			return false;
		}

		// Get the StackPin at that location
		ISchemStackPin stackpin = getSelectedStackPin();
		if (stackpin == null) {
			return false;
		}

		IPinList destPinList = (IPinList) stackpin.getParent();
		if (destPinList == null) {
			return false;
		}

		// Creates properties required for unit testing if "Unit Test Data Capture" is enabled in DEBUG mode
		createProperiesForUnitTest(UnitTestDataCaptureHelper.ActionType.ADDTOSTACK);

		Generator generator = Generator.getGenerator();

		//there exists some connected pinlists...Do create equivalne stack pin on them
		//collect the mated schem pins
		ISchemStackPin matedStackedPin =
				StackPinActionHelper.addConnectedPinsToMatedStack(m_pins, stackpin, generator, m_genParams);

		Collection<IConductor> connectedConductors = new LinkedHashSet<>();
		for (IPin pin : m_pins) {
			connectedConductors.addAll(CollectionUtils.filterByClass(pin.getConductors(), IConductor.class));
		}
		//Add selected pins to StackPin
		StackPinActionHelper.addPinsToStackPin(destPinList, stackpin, m_pins);

		//remove the selected schem pins from the parent pinlist
		StackPinActionHelper.removeSchemPins(m_pins);

		generator.generate(destPinList, m_genParams, Generator.NOREGENERATE_PROPERTIES, false);
		destPinList.regenerateDiagramObject();

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
		diagram.refreshRepresentations();
		ConductorRouteAction.getInstance().addConductorsForRoute(connectedConductors);

		Collection<ILogicObject> objects = getLocableMulticoresAndShields(generator, diagram);

		if (!lockObjects(getDesign(), objects)) {
			return false;
		}
		return true;
	}

	@Override Set<ILogicObject> getLockableObjects()
	{
		Set<ILogicObject> lockableObjects = new HashSet<>();
		for (IPinList candidate : m_destPinList.getCandidates()) {
			lockableObjects.add(candidate.getConnectivity());
		}

		Map<IPin, IPin> matedPins = StackPinActionHelper.getMatedPins(m_pins);
		if (matedPins != null) {
			for (IPin matedPin : matedPins.values()) {
				lockableObjects.add(matedPin.getConnectivity());
			}
		}
		lockableObjects.addAll(getLockableConductorsAndHighways(m_pins));
		lockableObjects.addAll(getConnectedHighways());

		return lockableObjects;
	}

	private Set<IHighway> getConnectedHighways()
	{
		Set<IHighway> connectedHighways = new HashSet<>();
		ISchemStackPin stackPin = getSelectedStackPin();
		if (stackPin != null) {
			connectedHighways.addAll(getConnectedHighways(stackPin));
			ISchemStackPin matedStack = ConnectionHelper.getConnectedStackedPin(stackPin);
			if (matedStack != null) {
				connectedHighways.addAll(getConnectedHighways(matedStack));
			}
		}

		return connectedHighways;
	}

	private Set<IHighway> getConnectedHighways(ISchemStackPin stackPin)
	{
		Set<IHighway> connectedHighways = new HashSet<>();
		stackPin.getConnectedHighways().stream()
				.map(schemHighway -> schemHighway.getConnectivity())
				.forEach(hw -> connectedHighways.add(hw));
		return connectedHighways;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		didMouseMove = true;
		m_selectedPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_selectedPoint.setLocation(m_grid.snap(m_selectedPoint.x), m_grid.snap(m_selectedPoint.y));

		int valid = validPoints(m_selectedPoint);
		if ((valid & PinPlacementConstraintsHolder.PLACEMENT_NO) != 0) {
			valid = PinPlacementConstraintsHolder.PLACEMENT_NO;
		}
		if (valid == PinPlacementConstraintsHolder.PLACEMENT_YES) {
			// Valid Location.
			setCursor(m_addStackPinValidCursor);
			dyn.setAttribute(m_greenAttr);
			createConnectionTransientGraphics();
		}
		else {
			// Invalid Location.
			m_selectedPoint = null;
			setCursor(m_addStackPinInvalidCursor);
			dyn.setAttribute(m_redAttr);
		}
		CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eTransient);
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it
		Action action = getActionUI();
		if (hasOperands(selections)) {
			container.add(new ActionEntry(action));
		}
	}

	protected int validStackPinLocation(Point point)
	{
		Stack<ISchemStackPin> matchingConnectivityStackPins = new Stack<>();
		collectMatchingConnectivityStackPins(point, matchingConnectivityStackPins);
		for (ISchemStackPin matchingConnectivityStackPin : matchingConnectivityStackPins) {
			if (StackPinActionHelper.isValidToAddToStackedPin(m_pins, matchingConnectivityStackPin)) {
				return PinPlacementConstraintsHolder.PLACEMENT_YES;
			}
		}
		return PinPlacementConstraintsHolder.PLACEMENT_NO;
	}

	protected boolean hasOperands(SelectSet selectSet)
	{

		OperandFinder operandFinder = new OperandFinder();
		for (SelectedUIDObjectIterator iter = selectSet.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!(obj instanceof IPin)) {
				continue;
			}
			IPin pin = (IPin) obj;

			if (!operandFinder.processPin(pin)) {
				return false;
			}
		}

		IPinList pinlist = operandFinder.getPinlist();
		Set<IAbstractPin> pins = operandFinder.getPins();

		if (pinlist == null) {
			return false;
		}

		if (!PinListHelper.isEditableHarnessConnector(pinlist) ||
				!StackedPinHelper.isPinSetOnSameFootprintConnector(pinlist, pins)) {
			return false;
		}

		if (!hasValidStack(pinlist, operandFinder.getMatingState() == StackPinActionHelper.MATING_STATE.MATING)) {
			return false;
		}
		return operandFinder.areLocksAvailable();
	}

	private boolean hasValidStack(IPinList pinlist, boolean mated)
	{
		if (mated) {
			return hasMatedStack(pinlist);
		}
		else {
			return hasNonMatedStack(pinlist);
		}
	}

	private boolean hasNonMatedStack(IPinList pinlist)
	{
		for (ISchemStackPin stackedPin : pinlist.getStackPins()) {
			if (!ConnectionHelper.hasMatedStackPin(stackedPin)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasMatedStack(IPinList pinlist)
	{
		for (ISchemStackPin stackedPin : pinlist.getStackPins()) {
			if (ConnectionHelper.hasMatedStackPin(stackedPin)) {
				return true;
			}
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

		return hasOperands(getController().getSelectMgr().getPreSelections()) &&
				getController().getCapletModel().isEditable();
	}

	@Override public String getActionUIClass()
	{
		return AddToStackPinActionUI.class.getName();
	}

	private static class OperandFinder
	{

		private IPinList pinlist = null;
		private IPinList connPinList = null;
		private StackPinActionHelper.MATING_STATE matingState = StackPinActionHelper.MATING_STATE.MIX;
		private Set<IAbstractPin> pins = new HashSet<IAbstractPin>();

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
			if (connPinList != null) {
				logicObjects.add(connPinList.getConnectivity());
			}
			for (IAbstractPin pin : pins) {
				for (chs.cof.logical.cable.IConductor conductor : pin.getConductors()) {
					logicObjects.add(conductor);
				}
			}
			return logicObjects;
		}

		private boolean processPin(IPin pin)
		{
			if (pinlist == null) {
				pinlist = (IPinList) pin.getParent();
			}

			if (pinlist != null && pinlist.getStackPins().isEmpty()) {
				//disable the action when there are no stackpins on the pinlist
				return false;
			}

			if (!StackPinActionHelper.isPinValidToAddToStack(pinlist, pin)) {
				return false;
			}

			IAbstractPin cablePin = pin.getConnectivity();

			//For sure, this pin cannot have >1 connected schem pins. Such pins would have already been filtered in StackPinActionHelper.isPinValidToAddToStack
			IPin matepin = PinPlacementHelper.getSingleConnectedSchemPin(pin);

			// Pins should not be added to stack if somes are mated and some are non-mated
			StackPinActionHelper.MATING_STATE currentMatingState =
					matepin != null ? StackPinActionHelper.MATING_STATE.MATING :
							StackPinActionHelper.MATING_STATE.NON_MATING;
			if (matingState == StackPinActionHelper.MATING_STATE.MIX) {
				// If it is first iteration
				matingState = currentMatingState;
			}
			else if (matingState != currentMatingState) {
				return false;
			}

			if (matepin != null) {
				IPinList parent = (IPinList) matepin.getParent();
				assert parent != null;
				if (connPinList == null) {  //first time => populate the connPinList info
					connPinList = parent;
				}
				else if (connPinList.getConnectivity() != parent.getConnectivity()) {
					//connPinList is present, this pin belongs to different conn pinlist
					return false;
				}
			}
			//this action remains disable if the selected pins are a mix of pins with and without matepins
			if ((matepin == null && connPinList != null)) {
				return false;
			}
			pins.add(cablePin);
			return true;
		}

		public IPinList getPinlist()
		{
			return pinlist;
		}

		public Set<IAbstractPin> getPins()
		{
			return pins;
		}

		public StackPinActionHelper.MATING_STATE getMatingState()
		{
			return matingState;
		}
	}
}
