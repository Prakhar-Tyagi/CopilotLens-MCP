/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2010-2024 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFProfilingKey;
import chs.caf.CAFUtils;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.helpers.LockedDecorationsHandlerOnMove;
import chs.caf.caplet.helpers.MoveManipulator;
import chs.caf.caplet.helpers.SectorHierarchyFinder;
import chs.caf.caplet.helpers.snapping.SingleLineConnectionHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IAnchorable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IPropText;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemSector;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.ILockableDynamicGfxMediator;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utility.gfx.TransformGfxHelper;
import chs.utility.helpers.CompositeConnectivityModifier;
import chs.utility.helpers.SingleLineHelper;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LogicMoveManipulator extends MoveManipulator
{

	private CompositeConnectivityModifier mConnectivityMaker;
	@Nullable private SingleLineConnectionHelper singleLineConnectionHelper;

	public LogicMoveManipulator(IDynamicGfxService dynamics, IGfxModel model)
	{
		super(dynamics, model);
		mConnectivityMaker = new CompositeConnectivityModifier();
	}

	@Override protected void reset()
	{
		mConnectivityMaker.reset();
		super.reset();
	}

	@Override protected void moveDecorations(List<IDynamicGfx> selectedDynamicGraphics)
	{
		MoveConductorDecorations.move(selectedDynamicGraphics);
	}

	@Override public boolean startDrag(Point pt, Object src, int inputModifiers)
	{
		boolean started = super.startDrag(pt, src, inputModifiers);
		setSingleLineConnectionHelper();
		return started;
	}

	private void setSingleLineConnectionHelper()
	{
		Iterator<IDynamicGfx> iterator = getSelectionDynamics();
		while (iterator.hasNext()) {
			IDynamicGfx gfx = iterator.next();
			if (gfx.getMediator() instanceof IHighwaySegment highwaySegment &&
					SingleLineHelper.isSingleLineSegment(highwaySegment)) {
				singleLineConnectionHelper = new SingleLineConnectionHelper(getDynamics(), gfx, connectedGraphicsSet());
				return;
			}
		}
		singleLineConnectionHelper = null;
	}

	@Override public void drag(MouseEvent event)
	{
		super.drag(event);
		if (singleLineConnectionHelper != null) {
			singleLineConnectionHelper.handleSingleLineEvent(event);
		}
	}

	@Override public void endDrag(InputEvent upEvent)
	{
		if (!m_isDragStarted) {
			return;
		}

		mConnectivityMaker.reset();

		Profiler profiler = ProfilingService.createAndStartProfiler(CAFProfilingKey.DIAGRAM_INTERACTION.getKeyName());

		Iterator<IDynamicGfx> iter = getSelectionDynamics();

		//Defect dts0100503239
		List<IDynamicGfx> sortedSelectionList = CollectionUtils.createList(sortSelectionDynamics(iter));
		List<IDynamicGfx> allMovingObject = CollectionUtils.createListNoNulls(sortedSelectionList);
		allMovingObject.addAll(CollectionUtils.createList(connectedGraphics()));

		// Cache IECName changes
		IECAttributeResolver attributeResolver = cacheIECAttributes(allMovingObject);

		boolean isSymbolCaplet = FactoryMgr.getSystemFactory().getCAFUtils().isSymbolCaplet();

		LockedDecorationsHandlerOnMove lockedDecorationsHandler = null;
		try {
			lockedDecorationsHandler =
					LockedDecorationsHandlerOnMove.start(isSymbolCaplet, sortedSelectionList.iterator());

			// Apply edits
			applyEdits(sortedSelectionList, upEvent, isSymbolCaplet);
		}
		finally {
			Optional.ofNullable(lockedDecorationsHandler).ifPresent(LockedDecorationsHandlerOnMove::end);
		}

		// We don't add new connectivity on a move so we need not pass in
		// connectivity information.
		// Apply the edits to all the 'indirectly edited objects'
		for (IDynamicGfx dynamic : connectedGraphicsSet()) {
			IDynamicGfxMediator mediator = dynamic.getMediator();
			if (mediator != null) {
				if (mediator instanceof ILockableDynamicGfxMediator) {
					mConnectivityMaker.applyEdits(
							(ILockableDynamicGfxMediator) mediator, getDynamics().getOwner(), dynamic, true, this);
				}
				else {
					mediator.applyEdits(getDynamics().getOwner(), dynamic, true, this);
				}
				if (mediator instanceof IAnchorable) {
					((IAnchorable) mediator).updateAnchors();
				}
			}
		}

		resolveIECAttributes(attributeResolver);

		if (allowConnectivityChange()) {
			// apply to all the selected items
			for (IDynamicGfx dynGfx : allMovingObject) {
				IDynamicGfxMediator mediator = dynGfx.getMediator();
				if (mediator instanceof ILockableDynamicGfxMediator) {
					mConnectivityMaker.createConnectivity(
							(ILockableDynamicGfxMediator) mediator, getDynamics().getOwner(), dynGfx, this);
				}
			}
		}

		if (singleLineConnectionHelper != null) {
			singleLineConnectionHelper.handleTerminate();
		}
		mConnectivityMaker.performConcurrentEditWithLockAttempt();

		moveDecorations(sortedSelectionList);
		removeMovingGraphics();

		// Reset all of the internal variables
		reset();

		stopAndLogEndDragProfiling(profiler, "MoveManipulator");
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

	private void applyEdits(List<IDynamicGfx> sortedDynamics, InputEvent upEvent, boolean isSymbolCaplet)
	{
		// dts0100443869 When creating a composite symbol, the user moves the blocks by selecting them using the select by area
		// the underlying cause of this defect is a block that has a proptext child, but the proptext does not have the block as a parent
		// this is the safest workaround for SP0802
		Set<IPropText> selectedBlockPropTexts = findSelectedBlockPropTexts();
		for (IDynamicGfx dynGfx : sortedDynamics) {
			IDynamicGfxMediator mediator = dynGfx.getMediator();
			if (mediator != null && !selectedBlockPropTexts.contains(mediator)) {
				applyEdits(upEvent, isSymbolCaplet, dynGfx, mediator);
			}
		}
	}

	private void applyEdits(@Nullable InputEvent upEvent, boolean isSymbolCaplet, @NotNull IDynamicGfx dynGfx,
			@NotNull IDynamicGfxMediator mediator)
	{
		// Note that it is important that the applyEdits for the selected mediator be
		// called AFTER the applyEdits for the connected objects. This is because of
		// the 'implicitly created' segments need to update connectivity before the
		// location information can be updated.
		if (!isSymbolCaplet || !dynGfx.isParentSelected()) {
			boolean isDatumText = false;
			//dts0100521801 Datum attribute texts are now part of the symbol extents. They cannot be moved.
			if (mediator instanceof IDiagramText) {
				isDatumText = ((IDiagramText) mediator).isDatumText();
			}
			if (!isDatumText) {
				if (upEvent != null) {
					setMoveModifiers(upEvent.getModifiers());
				}
				else {
					setMoveModifiers(0);
				}
				//mediator.applyEdits(getDynamics().getOwner(), dynGfx, m_allowConnectivityChange, this);
				applyEdits(mediator, dynGfx);
			}
			//dts0100563644 - Move Objects causes class cast exceptions. After Apply edits update the anchors.
			// Do not do it separately. update anchors is being done here so that we do not have to call update
			// anchors in recalculateExtent of DiagramObject
			updateAnchors(upEvent, mediator);
		}
	}

	private void updateAnchors(@Nullable InputEvent upEvent, @NotNull IDynamicGfxMediator mediator)
	{
		final boolean isRootDecoration = isRootDecoration(mediator);
		if (mediator instanceof IAnchorable) {
			if (upEvent != null) {
				TransformGfxHelper.updateEditedObjectAnchors((IAnchorable) mediator,
						false, upEvent.isShiftDown() && !isRootDecoration,
						isRootDecoration, false);
			}
			else {
				TransformGfxHelper.updateEditedObjectAnchors((IAnchorable) mediator);
			}
		}
	}

	@Override protected void applyEdits(IDynamicGfxMediator mediator, IDynamicGfx dynGfx)
	{
		if (mediator instanceof ILockableDynamicGfxMediator) {
			mConnectivityMaker.applyEdits((ILockableDynamicGfxMediator) mediator, getDynamics().getOwner(), dynGfx,
					allowConnectivityChange(), this);
		}
		else {
			super.applyEdits(mediator, dynGfx);
		}
	}
}
