package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.SelectionUtils;
import chs.caplets.logic.DeleteShieldBodiesHelper;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utility.DiagramHelper;
import chs.utility.UnitTestDataCaptureHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.logic.ModularConnectorHelper;
import chs.view.assist.IPinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Abstract class for create pin stack and for add pins to existing pin stack
 */
public abstract class AbstractCreateStackPinAction extends ControllerActionRT
		implements MouseListener, MouseMotionListener, ICtxMenuProvider
{

	protected GeneratorParameters m_genParams;
	protected Map<IPinList, PinPlacementConstraintsHolder> m_constraintsHolders = new LinkedHashMap<>();
	protected IDynamicGfxService m_dynamics;

	protected ModularSchemPinListInfo m_destPinList;
	protected IPin[] m_pins = null;
	protected IGfxObject dyn = null;

	protected Point m_tmpPoint = new Point(0, 0);
	protected Point m_selectedPoint;

	protected IWritableGfxAttribute m_redAttr;
	protected IWritableGfxAttribute m_greenAttr;
	protected Cursor m_addStackPinValidCursor = null;
	protected Cursor m_addStackPinInvalidCursor = null;

	private static final int MAX_COLOR = 255;
	protected static final int m_tmpPinSize = 200;
	//protected static final int R270 = 270;

	protected IGrid m_grid;
	private List<IGfxObject> transientGraphics = new ArrayList<IGfxObject>();

	protected boolean m_dragging;

	//this flag is set when the mouse is moved after activating the action
	protected boolean didMouseMove;

	protected AbstractCreateStackPinAction(ICapletController controller, IPinList destPinList, Point destPoint)
	{
		super(controller);
		initializeCurserGraphics(controller);
		if (destPinList != null) {
			m_destPinList = new ModularSchemPinListInfo(destPinList);
		}
		m_selectedPoint = destPoint;
	}

//	protected AbstractCreateStackPinAction(ICapletController controller, String instanceName)
//	{
//		super(controller, instanceName);
//	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		// Get a grid
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		IGriddable gridholder = (IGriddable) gview.getSheet();
		m_grid = gridholder.getGrid();

		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		setOperands(preSelections);
		m_dynamics.removeAllDynamicGfx();
		removeTransientGfx();
		m_dragging = false;
		m_constraintsHolders.clear();

		if (m_pins != null && m_pins.length > 0) {
			m_genParams = DiagramHelper.createGeneratorParameters(m_pins[0]);
			handlePinPlacementConstraints(m_destPinList);
			return IActionEnum.eActivated;
		}
		return IActionEnum.eActivated;
	}

	/**
	 * add pin stack plcaement constraints for connector attached to a device and all the other attached connectors
	 *
	 * @param schemParentPinlist the connector pinlist
	 *
	 * @return boolean
	 */
	protected void handlePinPlacementConstraints(@NotNull ModularSchemPinListInfo destPinList)
	{
		//handleConnectorAttachedToDevice
		Set<IPinList> attachedDevices = new HashSet<>();
		for (IPinList candidate : destPinList.getCandidates()) {
			IPinList theAttachedDevice = PinPlacementHelper.getAttachedDevice(candidate);
			if (theAttachedDevice != null) {
				attachedDevices.add(theAttachedDevice);
			}
		}

		IPinList anchor = destPinList.getAnchor();
		if (attachedDevices.isEmpty()) {
			// We have either: device, connector attached to connector or unattached device/connector
			addPinPlacementConstraints(anchor);
			return;
		}

		if (!PinPlacementHelper.allowConnectedPinMove(attachedDevices)) {
			// We have either: device, connector attached to connector or unattached device/connector
			addPinPlacementConstraints(anchor);
			return;
		}

		// Currently we are allowing to create pin stack from pins of same pinlist(connector)
		for (IPinList candidate : destPinList.getCandidates()) {
			if (isValidConnectorForMovingPins(m_pins, candidate)) {
				addPinPlacementConstraints(anchor, true);
				break;
			}
		}
	}

	protected boolean isValidConnectorForMovingPins(IPin[] movingPins, IPinList destConnector)
	{
		for (IPin pin : movingPins) {
			if (!PinPlacementHelper.isValidConnectorForMove(pin, destConnector.getConnectivity())) {
				return false;
			}
		}
		return true;
	}

	protected abstract void addPinPlacementConstraints(IPinList schemParentPinlist);

	protected abstract void addPinPlacementConstraints(IPinList schemParentPinlist, boolean showBoundaryExt);

	protected boolean selectionHasObjectsFromNonActiveDiagram()
	{
		IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		return activeDiagram == null || SelectionUtils.hasOtherDiagramSelection(preSelections, activeDiagram);
	}

	protected void removeTransientGfx()
	{
		// This can be called before m_constraints is setup
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}

		for (PinPlacementConstraintsHolder holder : m_constraintsHolders.values()) {
			holder.clearDynamics();
		}
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		// if we have a valid pin move, edit the data model
		boolean bEditOk = true;
		if (successful && m_selectedPoint != null &&
				((validPoints(m_selectedPoint) & PinPlacementConstraintsHolder.PLACEMENT_NO) == 0)) {
			if (!lockObjects(getDesign(), getLockableObjects())) {
				bEditOk = false;
			}
			else {
				bEditOk = editModel();
			}
		}

		// Get rid of our transient graphics
		removeTransientGfx();
		m_constraintsHolders.clear();
		m_destPinList = null;

		return bEditOk;
	}

	protected ILogicDesign getDesign()
	{
		ILogicDesign design = m_destPinList.getAnchor().getDiagram().getDesign();
		assert design != null;
		return design;
	}

	protected chs.cof.logical.cable.IPinList determineCablePinlist(@NotNull IAbstractSchemPin pin)
	{
		IPinList srcPinList = (IPinList) (pin.getParent());
		assert srcPinList != null;
		chs.cof.logical.cable.IPinList srcCablePL = srcPinList.getConnectivity();
		assert srcCablePL != null;
		return srcCablePL;
	}

	@Nullable protected ISchemStackPin getSelectedStackPin()
	{
		Stack<ISchemStackPin> matchingConnectivityStackPins = new Stack<>();
		collectMatchingConnectivityStackPins(m_selectedPoint, matchingConnectivityStackPins);
		return matchingConnectivityStackPins.isEmpty() ? null : matchingConnectivityStackPins.peek();
	}

	protected final boolean isMatchingStackPin(ISchemStackPin matchObj, chs.cof.logical.cable.IPinList connector)
	{
		IPinList parent = (IPinList) matchObj.getParent();
		assert parent != null;
		return (parent.getConnectivity() == connector);
	}

	protected final void collectMatchingConnectivityStackPins(@NotNull Point point,
			@NotNull Stack<ISchemStackPin> matchingConnectivityStackPins)
	{
		chs.cof.logical.cable.IPinList connector = determineCablePinlist(m_pins[0]);
		chs.cof.logical.cable.IPinList srcModRoot = ModularConnectorHelper.determineAeroModularRoot(connector);
		for (Map.Entry<IPinList, PinPlacementConstraintsHolder> entry : m_constraintsHolders.entrySet()) {
			if (srcModRoot != ModularConnectorHelper.determineAeroModularRoot(entry.getKey().getConnectivity())) {
				continue;
			}
			IGfxObject matchObj = entry.getValue().getObjectAt(point);
			if (matchObj instanceof ISchemStackPin && isMatchingStackPin((ISchemStackPin) matchObj, connector)) {
				matchingConnectivityStackPins.push((ISchemStackPin) matchObj);
			}
		}
	}

	protected Collection<ILogicObject> getLocableMulticoresAndShields(Generator generator, ISchemDiagram diagram)
	{
		Collection<IConductor> conductors = getConductorsToBeDeleted();

		DeleteShieldBodiesHelper shieldBodiesHelper = new DeleteShieldBodiesHelper();

		Set<IMulticore> mcs = new HashSet<>();
		shieldBodiesHelper.collectToBeEmptyShieldBodies(conductors, mcs, diagram, generator);
		Collection<ILogicObject> deletableShields = shieldBodiesHelper.getLogicObjectsToLock();

		Collection<ILogicObject> lockableObjects = new HashSet<>();
		lockableObjects.addAll(deletableShields);
		for (ILogicObject logicObject : deletableShields) {
			if (logicObject instanceof IShieldConductor) {
				lockableObjects.add(((chs.cof.logical.cable.IConductor) logicObject).getMulticore());
			}
		}
		return lockableObjects;
	}

	@NotNull private Collection<IConductor> getConductorsToBeDeleted()
	{
		Collection<IConductor> conductors = new HashSet<>();
		return CreationDeletionHelper.getTheCreationHelper().processOnDeletionObjects(delObjectsToProcess -> {
			while (delObjectsToProcess.hasNext()) {
				IUIDObject objectToBeDeleted = delObjectsToProcess.next();
				if (objectToBeDeleted instanceof IConductor) {
					IConductor conductor = (IConductor) objectToBeDeleted;
					conductors.add(conductor);
				}
			}
			return conductors;
		});
	}

	protected boolean lockObjects(ILogicDesign logicDesign, Collection<ILogicObject> objectsToLock)
	{
		Set<IUID> failedObjects = LogicObjectLockFinder.tryEdit(logicDesign, objectsToLock);
		if (!failedObjects.isEmpty()) {
			return false;
		}
		for (ILogicObject logicObject : objectsToLock) {
			logicObject.concurrencyLockableEdited();
		}
		return true;
	}

	abstract Set<ILogicObject> getLockableObjects();

	protected abstract boolean editModel();

	/**
	 * Creates non-undoabble properties on the pinlist which are used as input for unit testing. These properties are
	 * created iff "Unit Test Data Capture" is enabled in DEBUG mode
	 *
	 * @param actionType Type of the action
	 */
	protected void createProperiesForUnitTest(UnitTestDataCaptureHelper.ActionType actionType)
	{
		if (!UnitTestDataCaptureHelper.isEnabledUnitTestcapture()) {
			return;
		}

		StringBuilder pinUIDs = new StringBuilder();
		String seprator = ";";
		boolean first = true;
		IPinList parent = null;
		for (IPin pin : m_pins) {
			if (first) {
				first = false;
				parent = (IPinList) pin.getParent();
			}
			else {
				pinUIDs.append(seprator);
			}
			pinUIDs.append(pin.getUID().getString());
		}
		if (pinUIDs.length() != 0 && parent != null) {

			UnitTestDataCaptureHelper
					.createPropertyForUnitTest(parent, actionType, "PINLIST_UID", parent.getUID().toString());
			StringBuilder stackLocation = new StringBuilder();
			stackLocation.append(m_selectedPoint.x).append(',').append(m_selectedPoint.y);

			UnitTestDataCaptureHelper
					.createPropertyForUnitTest(parent, actionType, "STACKPIN_LOCATION", stackLocation.toString());

			UnitTestDataCaptureHelper.createPropertyForUnitTest(parent, actionType, "PIN_UIDS", pinUIDs.toString());
		}
	}

	protected int validPoints(Point selectedPoint)
	{
		clearTransientGraphics();

		m_tmpPoint.x = selectedPoint.x - m_tmpPinSize / 2;
		m_tmpPoint.y = selectedPoint.y - m_tmpPinSize / 2;
		// setup some graphics while we are at it...
		dyn.getLocation().setLocation(m_tmpPoint.x, m_tmpPoint.y);
		m_dynamics.addTransientGfx(dyn);
		m_tmpPoint.setLocation(m_grid.snap(m_tmpPoint.x), m_grid.snap(m_tmpPoint.y));

		// setup matching location graphics, this will show connected pin drawghost
		List<PinPlacementConstraintsHolder> holders = CollectionUtils.createListNoNulls(m_constraintsHolders.values());
		IGfxObject matchObject = StackPinActionHelper.getMatchingTransientObject(holders, m_tmpPoint);
		if (matchObject != null && getMatchingObject(holders, m_pins[0]) != null) {
			transientGraphics.add(matchObject);
			m_dynamics.addTransientGfx(matchObject);
		}

		return validStackPinLocation(m_tmpPoint);
	}

	@Nullable private IGfxObject getMatchingObject(List<PinPlacementConstraintsHolder> constraintsHolders,
			IGfxObject object)
	{
		for (PinPlacementConstraintsHolder holder : constraintsHolders) {
			IGfxObject matchObj = holder.getMatchingObject(object);
			if (matchObj != null) {
				return matchObj;
			}
		}
		return null;
	}

	protected abstract int validStackPinLocation(Point point);

	private void clearTransientGraphics()
	{
		for (IGfxObject gobj : transientGraphics) {
			m_dynamics.removeTransientGfx(gobj);
		}
		transientGraphics.clear();
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	protected void setOperands(SelectSet sset)
	{
		m_pins = null;

		List<IPin> pins = new ArrayList<IPin>();
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if ((obj instanceof IPin)) {
				IPin pin = (IPin) obj;
				if (!pin.getConnectivity().isInterconnect()) {
					pins.add((IPin) obj);
				}
			}
		}

		if (pins.isEmpty()) {
			return;
		}

		// save the selected pins
		m_pins = new IPin[pins.size()];
		for (int i = 0; i < m_pins.length; i++) {
			m_pins[i] = pins.get(i);
		}
		//set the current schematic PinList of it was not set in the constructor
		if (m_destPinList == null) {
			IPinList parent = (IPinList) m_pins[0].getParent();
			assert parent != null;
			m_destPinList = new ModularSchemPinListInfo(parent);
		}

		// init dynamic graphics of floating stackpin
		dyn = FactoryMgr.getDrawFactory().constructRectangle(0, 0, m_tmpPinSize, m_tmpPinSize);
		dyn.setAttribute(m_redAttr);
	}

	private void initializeCurserGraphics(ICapletController controller)
	{
		// Create our "pivot point"
		IGfxModel model = (IGfxModel) controller.getCapletModel();
		m_dynamics = model.getDynamicGfxService();
		if (m_addStackPinValidCursor == null) {
			m_addStackPinValidCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_pin.gif", new Point(7, 7));
			m_addStackPinInvalidCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_cantaddpin.gif", new Point(7, 7));
		}

		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		m_redAttr = drawFactory.constructAttribute(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_redAttr.setColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_redAttr.setFillBackgroundColor(drawFactory.constructColorRGB(MAX_COLOR, 0, 0));
		m_redAttr.setFillPattern(IFillPattern.PATTERN_SOLID);

		m_greenAttr = drawFactory.constructAttribute(drawFactory.lookupColor("pin"));
		m_greenAttr.setFillBackgroundColor(drawFactory.lookupColor("pin"));
		m_greenAttr.setFillPattern(IFillPattern.PATTERN_SOLID);
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		if (didMouseMove) {
			didMouseMove = false;
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	@Override public void mousePressed(MouseEvent e)
	{
	}

	@Override public void mouseReleased(MouseEvent e)
	{

		if (m_dragging) {
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	@Override public void mouseEntered(MouseEvent e)
	{
	}

	@Override public void mouseExited(MouseEvent e)
	{
	}

	@Override public void mouseDragged(MouseEvent e)
	{
	}

	protected void createConnectionTransientGraphics()
	{
		IPinInfo pinInfo = new IPinInfo()
		{
			@Nullable @Override public ILocation getAbsLocation(String pinName)
			{
				return FactoryMgr.getCommonFactory()
						.constructLocation(m_grid.snap(m_selectedPoint.x), m_grid.snap(m_selectedPoint.y));
			}

			@Nullable @Override public IAbstractPin getCablePin(String pinName)
			{
				for (IPin schemPin : m_pins) {
					if (pinName.equalsIgnoreCase(schemPin.getConnectivity().getName())) {
						return schemPin.getConnectivity();
					}
				}
				return null;
			}

			@Nullable @Override public IAbstractSchemPin getOriginatingSchemPin(String pinName)
			{
				return null;
			}
		};
		ISchemDiagram diagram = DiagramHelper.getDiagram(m_destPinList.getAnchor());

		List<String> pinsToTransientGfx = new ArrayList<>(m_pins.length);
		for (IPin schemPin : m_pins) {
			pinsToTransientGfx.add(schemPin.getConnectivity().getName());
		}

		assert diagram != null;
		List<IDynamicGfx> transientDynGfx = new ArrayList<>();
		ObjectConnectionsGetter.createTransientGraphics(pinsToTransientGfx, diagram, pinInfo, m_dynamics,
				transientDynGfx);
		for (IDynamicGfx dynamicGfx : transientDynGfx) {
			transientGraphics.add(dynamicGfx);
		}
	}

	@NotNull protected Set<ILogicObject> getLockableConductorsAndHighways(IPin[] pins)
	{
		Set<ILogicObject> lockables = new HashSet<>();
		Set<chs.cof.logical.cable.IPinList> matedPinlists = new HashSet<>();
		for (IPin pin : pins) {
			lockables.addAll(getLockableObjects(pin));
			if (!pin.getConnectivity().getConnectedPins().isEmpty()) {
				for (IPin matePin : PinPlacementHelper.getConnectedSchemPins(pin)) {
					lockables.addAll(getLockableObjects(matePin));
					matedPinlists.add(matePin.getConnectivity().getOwner());
				}
			}
		}
		lockables.addAll(matedPinlists);

		return lockables;
	}

	private Set<ILogicObject> getLockableObjects(IPin pin)
	{
		Set<ILogicObject> lockables = new HashSet<>();
		lockables.addAll(getConnectedConductors(pin));
		collectMergableEntities(pin, lockables);
		// Shouldn't filter out highways based on connection to stack pin, some highways can be dangling in the connection graph
		return lockables;
	}

	private void collectMergableEntities(@NotNull IPin pin, @NotNull Set<ILogicObject> lockables)
	{
		IJoint joint = pin.getJoint();
		if (joint == null) {
			return;
		}
		for (ISegment attachedSegment : joint.getAssociations(ISegment.class)) {
			lockables.addAll(collectMergableEntities(attachedSegment,
					attachedSegment.getStartJoint() == joint ? attachedSegment.getEndJoint() :
							attachedSegment.getStartJoint()));
		}
	}

	@NotNull private Collection<? extends ILogicObject> collectMergableEntities(@NotNull ISegment segment,
			@Nullable IJoint joint)
	{
		if (joint == null) {
			return Collections.emptySet();
		}
		Set<ILogicObject> lockableObjects = new HashSet<>();
		Set<List<IUID>> pathsTerminatingAtHighways = getAllPathsInvolvingHighways(segment, joint, new HashSet<>());
		pathsTerminatingAtHighways.stream().forEach(list -> list.stream().forEach(segmentUID -> {
			IHighwaySegment hSeg = UIDMgr.getObjectOfType(segmentUID, IHighwaySegment.class);
			if (hSeg != null) {
				lockableObjects.add(hSeg.getHighway().getConnectivity());
			}
			ISegment seg = UIDMgr.getObjectOfType(segmentUID, ISegment.class);
			if (seg != null) {
				lockableObjects.add(seg.getConductor().getConnectivity());
			}
		}));

		return lockableObjects;
	}

	@NotNull
	private Set<List<IUID>> getAllPathsInvolvingHighways(@NotNull ILogicSegment segment,
			@Nullable IJoint joint,
			@NotNull Set<ILogicSegment> alreadyVisitedSegments)
	{
		if (joint == null || alreadyVisitedSegments.contains(segment)) {
			return Collections.emptySet();
		}
		alreadyVisitedSegments.add(segment);

		Set<List<IUID>> result = new HashSet<>();
		if (!joint.getAssociations(IAbstractSchemPin.class).isEmpty()) {
			return result;
		}

		for (ILogicSegment otherSegment : joint.getAssociations(ILogicSegment.class)) {
			if (otherSegment == segment) {
				continue;
			}
			if (otherSegment instanceof IHighwaySegment) {
				// collecting this segment anyway and then check for other connections
				List<IUID> logicSegments = new ArrayList<>();
				logicSegments.add(otherSegment.getUID());
				result.add(logicSegments);
			}

			result.addAll(
					getAllPathsInvolvingHighways(otherSegment,
							otherSegment.getStartJoint() == joint ? otherSegment.getEndJoint() :
									otherSegment.getStartJoint(), alreadyVisitedSegments));
		}

		if (!result.isEmpty()) {
			result.stream().forEach(list -> list.add(segment.getUID()));
		}
		return result;
	}

	private Set<chs.cof.logical.cable.IConductor> getConnectedConductors(IPin pin)
	{
		Set<chs.cof.logical.cable.IConductor> lockables = new HashSet<>();
		for (IConductor conductor : pin.getConductors()) {
			lockables.add(conductor.getConnectivity());
		}
		return lockables;
	}
}
