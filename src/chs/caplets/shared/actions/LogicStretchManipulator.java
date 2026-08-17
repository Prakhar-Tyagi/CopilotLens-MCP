/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2024 Siemens
 */
package chs.caplets.shared.actions;

import chs.caf.CAFProfilingKey;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.SectorHierarchyFinder;
import chs.caf.caplet.helpers.StretchManipulator;
import chs.caf.caplet.helpers.snapping.ModelUtils;
import chs.caf.caplet.helpers.snapping.SchemConnectorPlaceholder;
import chs.caf.caplet.helpers.snapping.SingleLineConnectionHelper;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.caf.caplet.helpers.snapping.SnapThroughConnectorHelper;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.MoveConductorDecorations;
import chs.caplets.logic.actions.MoveWireEndAction;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IAnchorable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.CAFSchemSnapHelper;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemSector;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ILockableDynamicGfxMediator;
import chs.services.dynamicgfx.ISmartPointIterator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.gfx.TransformGfxHelper;
import chs.utility.helpers.CompositeConnectivityModifier;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.SwingUtilities;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p/> Stretch implementation of IManipulate. It handles basic grip-point movement.  This class only really manipulates
 * "ISmartPoint" objects. Indirectly this will affect IDynamicGfx objects. </p> <p> Ulimately the model is affected
 * through the abstract IDynamicGfxMediator interface. </p>
 */
public class LogicStretchManipulator extends StretchManipulator
{

	private CompositeConnectivityModifier mConnectivityMaker;
	private boolean m_ctrlPressed = false;
	@Nullable private SingleLineConnectionHelper singleLineConnectionHelper;

	/**
	 * Constructor for the CapitalLogic StretchMainipulator object
	 *
	 * @param dynamics Description of Parameter
	 */
	public LogicStretchManipulator(IDynamicGfxService dynamics, IGfxModel model)
	{
		super(dynamics, model);
		mConnectivityMaker = new CompositeConnectivityModifier();
	}

	@Override protected void moveDecorations(List<IDynamicGfx> selectedDynamicGraphics)
	{
		MoveConductorDecorations.move(selectedDynamicGraphics);
	}

	@Override public boolean startDrag(MouseEvent downEvent)
	{
		if (!hasDirectSmartPointForDrag(downEvent) && invokeMoveEndAction()) {
			return false;
		}
		boolean started = super.startDrag(downEvent);
		setSingleLineConnectionHelper();
		return started;
	}

	@Override public void drag(MouseEvent event)
	{
		if (singleLineConnectionHelper != null) {
			singleLineConnectionHelper.handleSingleLineEvent(event);
		}
		super.drag(event);
	}

	private void setSingleLineConnectionHelper()
	{
		if (SnapHelper.isSnapSourceFromSingleLine(getSnapSource()) && getSnapSource().getEndPointDragged()) {
			singleLineConnectionHelper = new SingleLineConnectionHelper(getDynamics());
		}
		else {
			singleLineConnectionHelper = null;
		}
	}

	/**
	 * @param downEvent Mousedown event
	 * @return true object has direct smart points for dragging
	 */
	public boolean hasDirectSmartPointForDrag(@NotNull MouseEvent downEvent)
	{
		ISmartPointIterator spiter = getSmartPointIterator(downEvent.getPoint());
		return spiter.hasNext();
	}

	/**
	 * @return true if moveEndAction is invoked else return false.
	 */
	private boolean invokeMoveEndAction()
	{
		final IActionMgr actionMgr = CAFUtils.getInstance().getActiveCapletController().getActionMgr();
		final ActionRT moveWireEndAction = (ActionRT) actionMgr.findAction(MoveWireEndAction.class.getName());

		if (!draggedObjectIsLogicSegment() || moveWireEndAction == null ||
				!isActionEnabled(moveWireEndAction)) {
			return false;
		}

		SwingUtilities.invokeLater(() ->
		{
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
			actionMgr.actionPerformed(moveWireEndAction, ae);
		});
		return true;
	}

	private boolean isActionEnabled(@NotNull ActionRT moveWireEndAction)
	{
		return actionUIEnabled(moveWireEndAction) &&
				moveWireEndAction.isEnabled();
	}

	private boolean actionUIEnabled(@NotNull ActionRT moveWireEndAction)
	{
		final Action action = moveWireEndAction.getActionUI();
		return action != null && action.isEnabled();
	}

	private boolean draggedObjectIsLogicSegment()
	{
		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (selectMgr == null) {
			return false;
		}

		SelectSet selectSet = selectMgr.getPreSelections();
		if (selectSet.getSelected().hasNext()) {
			Selection selection = selectSet.getSelected().getNext();
			IUIDObject selectedObject = selection.getObject();
			return selectedObject instanceof ILogicSegment;
		}
		return false;
	}

	@Override public void endDrag(Point point, Object source, boolean isCtrlDown, InputEvent upEvent)
	{
		mConnectivityMaker.reset();

		Profiler profiler = ProfilingService.createAndStartProfiler(CAFProfilingKey.DIAGRAM_INTERACTION.getKeyName());

		Point currPoint = CAFUtils.getInstance().getWorldPoint(point, source);
		setPoint(currPoint, ModelUtils.getSnapRadius(source), isCtrlDown);

		List<IDynamicGfx> sortedSelectionList = getSelected();
		List<IDynamicGfx> allMovingObject = CollectionUtils.createListNoNulls(sortedSelectionList);
		allMovingObject.addAll(CollectionUtils.createList(connectedGraphics()));

		// Cache IECName changes
		IECAttributeResolver attributeResolver = cacheIECAttributes(allMovingObject);

		// Apply edits
		applyEdits(allMovingObject);

		resolveIECAttributes(attributeResolver);

		List<IPinList> addedSchemConnectors = Collections.emptyList();
		ILogicObject sourceObject = m_snappingSource == null ? null :
				ReferenceHelper.reduceToLogicObject(m_snappingSource.getSourceObject());
		if (m_lastSnapped != null && isAutoGenerateConnectorEnabled() &&
				(sourceObject instanceof IWireConductor || sourceObject instanceof INetConductor)) {
			SchemConnectorPlaceholder schemConnectorPlaceholder = SnapThroughConnectorHelper.getConnectorPlaceholder(
					m_lastSnapped, isCtrlDown, Collections.emptySet());
			if (schemConnectorPlaceholder != null &&
					SnapThroughConnectorHelper.isConnectionAvailable(schemConnectorPlaceholder, sourceObject)) {
				ILogicDesign logicDesign = sourceObject.getLogicDesign();
				boolean sourceObjectEditable = true;
				if (logicDesign != null && logicDesign.isUnderConcurrentEdit()) {
					sourceObjectEditable = LogicObjectLockFinder.tryEdit(sourceObject);
				}
				if (schemConnectorPlaceholder.areReferencesEditable() && sourceObjectEditable) {
					addedSchemConnectors = SnapThroughConnectorHelper.updateSnapsWithConnectors(Map.of(m_lastSnapped,
							schemConnectorPlaceholder), sourceObject);
				}
				else {
					m_lastSnapped = null;
				}
			}
		}
		Iterator<Pair<IDynamicSnap, Integer>> snapIterator = getSnapsIterator(m_lastSnapped, upEvent);

		// Collect connectivity makers -- SnapHandlers and Segment ConnectivityMakers
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			if (mediator instanceof ILockableDynamicGfxMediator) {
				ILockableDynamicGfxMediator lockableMediator = (ILockableDynamicGfxMediator) mediator;
				mConnectivityMaker.collectLockableEdits(lockableMediator, dynGfx, snapIterator);
				mConnectivityMaker.createConnectivity(
						(ILockableDynamicGfxMediator) mediator, getDynamics().getOwner(), dynGfx, this);
			}
		}
		if (singleLineConnectionHelper != null) {
			singleLineConnectionHelper.handleTerminate();
		}

		// Perform lockable connectivity edits
		boolean doneAddingConnectivity = mConnectivityMaker.performConcurrentEditWithLockAttempt();

		// Add connectivity for non lockable edits
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			if (m_lastSnapped != null && mediator != null && !(mediator instanceof ILockableDynamicGfxMediator)) {
				doneAddingConnectivity = mediator.addConnectivity(snapIterator);
			}
		}

		// Lock and add connectivity for connected graphics
		// We don't add new connectivity on a move so we need not pass in
		// connectivity information.
		mConnectivityMaker.reset();
		IDynamicSnap lastSnapped = doneAddingConnectivity ? null : m_lastSnapped;
		if (lastSnapped != null) {
			List<IDynamicGfx> connectedGraphics = CollectionUtils.createList(connectedGraphics());
			Iterator<Pair<IDynamicSnap, Integer>> snapsIterator = getSnapsIterator(lastSnapped, upEvent);
			lockAndAddConnectivity(connectedGraphics, snapsIterator);
		}

		// apply to all the selected items
		// Create connectivity for non lockable objects
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			if (mediator != null && !(mediator instanceof ILockableDynamicGfxMediator)) {
				mediator.createConnectivity(getDynamics().getOwner(), dynGfx, this);
			}
		}

		moveDecorations(sortedSelectionList);

		removeMovingGraphics();

		for (IPinList addedSchemConnector : addedSchemConnectors) {
			SnapThroughConnectorHelper.joinConnectorWithNearByPinlists(addedSchemConnector);
		}

		if (m_snappedGfx != null) {
			getDynamics().removeTransientGfx(m_snappedGfx);
		}

		// Reset all of the internal variables
		reset();

		stopAndLogEndDragProfiling(profiler, "StretchManipulator");
	}

	private void resolveIECAttributes(@Nullable IECAttributeResolver attributeResolver)
	{
		if (attributeResolver == null) {
			return;
		}
		final IBaseDiagram diagram = CAFUtils.getInstance().getActiveDiagram();
		if (diagram instanceof ISchemDiagram) {
			final ILogicDesign design = ((ISchemDiagram) diagram).getDesign();
			if (design != null) {
				attributeResolver.resolveAttributes(design, (ISchemDiagram) diagram);
			}
		}
	}

	@Nullable private IECAttributeResolver cacheIECAttributes(List<IDynamicGfx> allMovingObject)
	{
		boolean isSymbolCaplet = FactoryMgr.getSystemFactory().getCAFUtils().isSymbolCaplet();
		if (!isSymbolCaplet) {
			final ISchemDiagram diagram =
					CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISchemDiagram.class);
			if (diagram != null) {
				SectorHierarchyFinder finder = new SectorHierarchyFinder(diagram);
				IECAttributeResolver attributeResolver = new IECAttributeResolver(finder);
				for (IDynamicGfx dynGfx : allMovingObject) {
					IDynamicGfxMediator mediator = dynGfx.getMediator();
					if (mediator instanceof IPinList) {
						attributeResolver.addPinList((IPinList) mediator, false);
					}
					if (mediator instanceof ISchemSector) {
						ISchemSector sector = (ISchemSector) mediator;
						attributeResolver.addSector(sector, false);
						for (IGfxObject gfxObject : finder.getObjectsContainedInSector(sector)) {
							attributeResolver.addGfxObject(gfxObject, false);
						}
					}
				}
				return attributeResolver;
			}
		}
		return null;
	}

	@Override protected void reset()
	{
		mConnectivityMaker.reset();
		m_ctrlPressed = false;
		SnapThroughConnectorHelper.clearCachedObjects();
		super.reset();
	}

	private boolean lockAndAddConnectivity(List<IDynamicGfx> sortedSelectionList,
			Iterator<Pair<IDynamicSnap, Integer>> snapIterator)
	{
		Collection<IUIDObject> toBeLocked = collectLockables(sortedSelectionList, snapIterator);

		// Lock of the objects
		Set<IUID> lockFailedObjectUIDs = new HashSet<>(CompositeConnectivityModifier.lockObjects(toBeLocked));

		// Lock any additional objects after refresh
		toBeLocked.clear();
		toBeLocked = mConnectivityMaker.getLockablesAfterRefresh(lockFailedObjectUIDs);

		// Lock additional objects
		lockFailedObjectUIDs.addAll(CompositeConnectivityModifier.lockObjects(toBeLocked));

		return mConnectivityMaker.performConcurrentEdit(lockFailedObjectUIDs);
	}

	/**
	 * Applies the edits to the non-selected items.
	 *
	 * @param lastSnapped The IDynamicSnap representing the last item "snapped" to.
	 * @param event       upEvent
	 */
	@Override protected void applyConnectedEdits(@Nullable IDynamicSnap lastSnapped, @Nullable InputEvent event)
	{
		// Apply the edits to all the 'indirectly edited objects'
		mConnectivityMaker.reset();
		if (lastSnapped != null) {
			List<IDynamicGfx> connectedGraphics = CollectionUtils.createList(connectedGraphics());
			Iterator<Pair<IDynamicSnap, Integer>> snapsIterator = getSnapsIterator(lastSnapped, event);
			lockAndAddConnectivity(connectedGraphics, snapsIterator);
		}
	}

	/**
	 * This is called by the move and stretch manipulater by objects that defer creation of connectivity until all
	 * objects have been moved (very important for inlines).
	 *
	 * @param sortedSelectionList The selected list of be told to create their connectivity
	 */
	@Override protected void createConnectivity(List<IDynamicGfx> sortedSelectionList)
	{
		// apply to all the selected items
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			mediator.createConnectivity(getDynamics().getOwner(), dynGfx, this);
		}
	}

	@NotNull private Iterator<Pair<IDynamicSnap, Integer>> getSnapsIterator(IDynamicSnap lastSnapped,
			@Nullable InputEvent upEvent)
	{
		final int modifiers = upEvent != null ? upEvent.getModifiers() : 0;
		Pair<IDynamicSnap, Integer> pair = new Pair<IDynamicSnap, Integer>(lastSnapped, modifiers);
		Collection<Pair<IDynamicSnap, Integer>> tempVec = Collections.singletonList(pair);
		return tempVec.iterator();
	}

	private void applyEdits(List<IDynamicGfx> sortedSelectionList)
	{
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			// Note that it is important that the applyEdits for the selected mediator be
			// called AFTER the applyEdits for the connected objects. This is because of
			// the 'implicitly created' segments need to update connectivity before the
			// location information can be updated.
			if (mediator != null) {
				if (mediator instanceof ILockableDynamicGfxMediator) {
					mConnectivityMaker.applyEdits(
							(ILockableDynamicGfxMediator) mediator, getDynamics().getOwner(), dynGfx, true, this);
				}
				else {
					mediator.applyEdits(getDynamics().getOwner(), dynGfx, true, this);
				}
				if (mediator instanceof IAnchorable) {
					TransformGfxHelper.updateEditedObjectAnchors((IAnchorable) mediator);
				}
			}
		}
	}

	@NotNull private Collection<IUIDObject> collectLockables(List<IDynamicGfx> sortedSelectionList,
			Iterator<Pair<IDynamicSnap, Integer>> snapIterator)
	{
		Collection<IUIDObject> toBeLocked = new ArrayList<>();
		for (IDynamicGfx dynGfx : sortedSelectionList) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			if (mediator instanceof ILockableDynamicGfxMediator) {
				ILockableDynamicGfxMediator lockableMediator = (ILockableDynamicGfxMediator) mediator;
				mConnectivityMaker.collectLockableEdits(lockableMediator, dynGfx, snapIterator);
				Collection<IUIDObject> lockables = mConnectivityMaker.getLockables();
				toBeLocked.addAll(lockables);
			}
		}
		return toBeLocked;
	}

	@NotNull public Collection<Integer> getGripRadiusCandidates()
	{
		return getViewSensitiveStretchGripRadiusCandidates();
	}

	@Override protected Point setPoint(Point currpt, int radius, boolean isControlDown)
	{
		SnapThroughConnectorHelper.updateToolTipText(null);
		updateCursor(getCursor());
		if (m_snappingSource != null) {
			m_snappingSource.setManipulatorType(Type.STRETCH);
		}
		Point point = super.setPoint(currpt, radius, isControlDown);
		ILogicObject sourceObject = m_snappingSource == null ? null :
				ReferenceHelper.reduceToLogicObject(m_snappingSource.getSourceObject());
		if (isAutoGenerateConnectorEnabled() && m_lastSnapped != null &&
				(sourceObject instanceof IWireConductor || sourceObject instanceof INetConductor)) {
			SchemConnectorPlaceholder schemConnectorPlaceholder = SnapThroughConnectorHelper.getConnectorPlaceholder(
					m_lastSnapped, isControlDown, Collections.emptySet());
			if (schemConnectorPlaceholder != null &&
					SnapThroughConnectorHelper.isConnectionAvailable(schemConnectorPlaceholder, sourceObject)) {
				SnapThroughConnectorHelper
						.updateToolTipText(SnapThroughConnectorHelper.getTooltipText(schemConnectorPlaceholder));
				ICompoundObject compoundObject = FactoryMgr.getDrawFactory().createCompoundObject();
				if (m_snappedGfx != null) {
					getDynamics().removeTransientGfx(m_snappedGfx);
					compoundObject.addObject(m_snappedGfx);
				}
				compoundObject.addObject(schemConnectorPlaceholder.getGfxObject());
				m_snappedGfx = compoundObject;
				getDynamics().addTransientGfx(m_snappedGfx);
				point.setLocation(schemConnectorPlaceholder.getSnapPoint());
				m_lastSnapped.getPoint().setLocation(point);
				if (m_nonOrthoPoint != null) {
					if (m_nonOrthoPoint.requiresLocationDeltas()) {

						Point delta = new Point(point.x - m_lastPoint.x, point.y - m_lastPoint.y);
						m_lastPoint = point;
						m_nonOrthoPoint.setLocked(true);
						m_nonOrthoPoint.applyAbsoluteDelta(delta, true, m_context);
						m_nonOrthoPoint.setLocked(false);
					}
					else {
						m_nonOrthoPoint.setAbsoluteLocation(point, true, m_context);
					}
				}
			}
		}
		return point;
	}

	@Override
	protected boolean checkWireCanBeSnapped(@NotNull IDynamicSnap dynamicSnap)
	{
		ILogicObject sourceObject = m_snappingSource == null ? null :
				ReferenceHelper.reduceToLogicObject(m_snappingSource.getSourceObject());
		boolean wireSnapAllowed = SnapThroughConnectorHelper.checkWireCanBeSnapped(dynamicSnap, sourceObject);
		if (!wireSnapAllowed) {
			SnapThroughConnectorHelper.updateToolTipText(ResourceMgr.getString(CAFSchemSnapHelper.class,
					"CAFSchemSnapHelper.CannotConnectWire"));
			updateCursor(CAFSchemSnapHelper.getCannotAddWireCursor());
		}
		return wireSnapAllowed;
	}

	private void updateCursor(Cursor cursor)
	{
		ICapletView currView = CAFUtils.getInstance().getActiveCapletView();
		currView.setViewCurrentCursor(cursor);
	}

	private boolean isAutoGenerateConnectorEnabled()
	{
		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
		if (capView != null) {
			ICapletModel capModel = CAFUtils.getInstance().getActiveCapletView().getCapletModel();
			if (capModel != null && capModel instanceof ILogicModel) {
				return ((ILogicModel) capModel).getAutoGenerateConnectorMode();
			}
		}
		return false;
	}

	@Override public void keyPressed(KeyEvent e)
	{
		if (m_isDragStarted) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL && !m_ctrlPressed) {
				GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
				if (gfxView != null) {
					Point currentMousePoint = getCurrentMousePoint(gfxView);
					m_ctrlPressed = true;
					triggerDrag(gfxView, currentMousePoint, InputEvent.CTRL_DOWN_MASK);
				}
			}
			else {
				super.keyPressed(e);
			}
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{
		if (m_isDragStarted) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL && m_ctrlPressed) {
				GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
				if (gfxView != null) {
					Point currentMousePoint = getCurrentMousePoint(gfxView);
					m_ctrlPressed = false;
					triggerDrag(gfxView, currentMousePoint, 0);
				}
			}
			else {
				super.keyReleased(e);
			}
		}
	}

	@NotNull private Point getCurrentMousePoint(@NotNull GfxView gfxView)
	{
		return gfxView.convertWorldPointToViewComponentPoint(gfxView.getCurrentMouseLocation());
	}

	private void triggerDrag(Component source, @NotNull Point point, int modifier)
	{
		drag(new MouseEvent(source, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
				modifier, point.x, point.y, 0, false));
	}
}