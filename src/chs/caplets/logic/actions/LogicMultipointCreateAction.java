/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ISegmentSnapLockAndEdit;
import chs.caf.caplet.helpers.ISegmentSnapLockAndEditProvider;
import chs.caf.caplet.helpers.creation.CreateByMultipointAction;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.CreateConductorInstanceActionHelper;
import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.ICompositeTextDecorationText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.utilities.CollectionUtils;
import chs.utilities.Pair;
import chs.utilities.stream.StreamUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public abstract class LogicMultipointCreateAction extends CreateByMultipointAction
{

	protected final CreateConductorInstanceActionHelper mCreateCondInstanceHelper;

	protected LogicMultipointCreateAction(ICapletController controller, boolean snapToGrid, boolean snapToSubgrid)
	{
		super(controller, snapToGrid, snapToSubgrid);
		mCreateCondInstanceHelper = new CreateConductorInstanceActionHelper(controller);
	}

	protected boolean getOrthoMode(MouseEvent e)
	{
		boolean omode = false;
		IGfxModel model = getModel();
		if (model instanceof Model) {
			Model logicModel = (Model) model;
			omode = logicModel.getOrthogonal();
		}
		return omode;
	}

	/**
	 * This isn't a good place for this function, but its the first common class for AddSharedNet and AddSharedWire...
	 * go figure.  Sorry, but I'm dumping it here.
	 *
	 * @param sharedMcore
	 * @param schemCond
	 */
	protected void addShieldWithLibraryPart(ISharedMulticore sharedMcore, IConductor schemCond)
	{
		if (!(sharedMcore instanceof ISharedOverbraid) && sharedMcore != null && sharedMcore.isPartAssigned() &&
				sharedMcore.getShield() != null) {
			// If this is part of a multicore and there is a library part assigned to it, and it has a shield
			// we must be sure to add the shield at this point too (assuming it hasn't been added already).
			// We can do that by using the AddLibraryInnercoreShieldAction - once we know all the library parts are
			// correctly loaded.
			ISharedConductor shield = sharedMcore.getShield();
			ILibraryMulticore lmw = (ILibraryMulticore) sharedMcore.getLibraryObject();
			if (lmw != null && lmw.getMulticoreDetail() != null) {
				Set<ILibraryInnerCore> wires = lmw.getMulticoreDetail().getInnerCores();
				for (ILibraryInnerCore wire : wires) {
					if (wire.getUID() == shield.getLibraryRef()) {
						IAction action =
								getController().getAction(AddLibraryInnercoreShieldAction.class);
						List<IMulticore> ancestors = new ArrayList<IMulticore>();
						IMulticore mc = schemCond.getConnectivity().getMulticore();
						if (mc.getShield() == null) {
							ancestors.add(mc);
							((AddLibraryInnercoreShieldAction) action).convertToShield(wire, ancestors);
						}
					}
				}
			}
		}
	}

	protected boolean refresh(ISharedConductor sharedConductor, IProject project)
	{
		return mCreateCondInstanceHelper.refresh(sharedConductor, project);
	}

	@Override protected boolean connectObjects()
	{
		if (super.connectObjects()) {

			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			Iterator<IUIDObject> itr = cdh.getNewObjectsToProcess();
			//if auto route signal enabled, this is to traverse shields connected to splice when wire conductor is created from hookup and no need to traverse if segment itself is a shiled conductor.
			if (ConductorRouteAction.getInstance().isEnableTraverseRouting()) {
				StreamUtils.asStream(itr)
						.filter(obj -> obj instanceof IConductor &&
								!(((IConnectivityRef) obj).getConnectivity() instanceof IShieldConductor))
						.flatMap(schemCond -> ((IConductor) schemCond).getPins().stream())
						.filter(schemPin -> schemPin.getConnectivity() instanceof ISplicePin)
						.flatMap(schemPin -> schemPin.getConductors().stream())
						.filter(schemCond -> schemCond.getConnectivity() instanceof IShieldConductor)
						.forEach(schemCond -> {
							ConductorRouteAction.getInstance().addConductorForRoute(schemCond);
						});
			}

			updateCompositeTexts(itr);

			return true;
		}
		return false;
	}

	public static void updateCompositeTexts(Iterator<IUIDObject> itr)
	{
		Collection<ICompoundObject> compObjs = new LinkedHashSet<ICompoundObject>();
		Collection<ICompositeTextDecorationText> compTxts = new LinkedHashSet<ICompositeTextDecorationText>();
		while (itr.hasNext()) {
			IUIDObject uidObject = itr.next();

			if (uidObject instanceof ILogicSegment) {
				IDiagramObject parent = ((IDiagramObject) uidObject).getParent();
				if (parent instanceof ILogicSegmentContainer) {
					compObjs.addAll(((ISegmentCollector) parent).getSegmentsOfType(ICompoundObject.class));
				}
			}

			if (uidObject instanceof ICompoundObject) {
				compObjs.add((ICompoundObject) uidObject);
			}

			if (uidObject instanceof ICompositeTextDecorationText) {
				compTxts.add((ICompositeTextDecorationText) uidObject);
			}
		}

		for (ICompoundObject compObj : compObjs) {
			compTxts.addAll(compObj.getObjects(ICompositeTextDecorationText.class));
		}

		for (ICompositeTextDecorationText compTxt : compTxts) {
			compTxt.updateCompositeText();
		}
	}

	@Override protected void connectObjectToSnapPoints()
	{
		ILogicDesign logicDesign = ((ILogicModel) getModel()).getDesign();
		if (logicDesign != null && logicDesign.isUnderConcurrentEdit()) {
			Collection<IDynamicGfxMediator> connectingObjects = connectingObjects();
			boolean allObjectsOfProviderType = true;
			for (IDynamicGfxMediator anObject : connectingObjects) {
				if (!(anObject instanceof ISegmentSnapLockAndEditProvider)) {
					allObjectsOfProviderType = false;
					break;
				}
			}
			getSnapHelper().endSnapping(connectingObjects);
			if (!allObjectsOfProviderType) {
				super.connectObjectToSnapPoints();
			}
			Collection<ISegmentSnapLockAndEdit> handlers = new ArrayList<>();
			Collection<Pair<IDynamicSnap, Integer>> allSnapPoints = getSnapHelper().getAllSnapped();
			Collection<ILogicObject> logicObjectsToLock = new LinkedHashSet<ILogicObject>();

			collectLockables(connectingObjects, handlers, allSnapPoints, logicObjectsToLock);
			Set<IUID> lockFailedObjectUIDs = performLocks(handlers, allSnapPoints, logicObjectsToLock);
			applyEdits(handlers, allSnapPoints, lockFailedObjectUIDs);
		}
		else {
			super.connectObjectToSnapPoints();
		}
	}

	private void collectLockables(Collection<IDynamicGfxMediator> connectingObjects,
			Collection<ISegmentSnapLockAndEdit> handlers, Collection<Pair<IDynamicSnap, Integer>> allSnapPoints,
			Collection<ILogicObject> logicObjectsToLock)
	{
		Collection<ISegmentSnapLockAndEditProvider> providers =
				CollectionUtils.getObjectList(connectingObjects, ISegmentSnapLockAndEditProvider.class);
		Collection<ILogicObject> objecstToLockInMergeSegments = new LinkedHashSet<>();

		for (ISegmentSnapLockAndEditProvider aProvider : providers) {
			ISegmentSnapLockAndEdit lockAndEditHandler = aProvider.createLogicObjectLockAndEdit();

			if (lockAndEditHandler == null ||
					!lockAndEditHandler
							.collectObjectsToLock(allSnapPoints, logicObjectsToLock, objecstToLockInMergeSegments)) {
				continue;
			}

			handlers.add(lockAndEditHandler);
		}
	}

	@NotNull protected Set<IUID> performLocks(Collection<ISegmentSnapLockAndEdit> handlers,
			Collection<Pair<IDynamicSnap, Integer>> allSnapPoints, Collection<ILogicObject> logicObjectsToLock)
	{
		Set<IUID> lockFailedObjectUIDs = new HashSet<>(
				LogicObjectLockFinder.tryEdit(((ILogicModel) getModel()).getDesign(), logicObjectsToLock));
		for (ISegmentSnapLockAndEdit handler : handlers) {
			Predicate<IDynamicSnap> snapsToIgnoreAfterLock =
					handler.getSnapsToIgnoreAfterLock(lockFailedObjectUIDs);
			Collection<ILogicObject> logicObjectsToLockAfterRefresh = new LinkedHashSet<>();
			handler.collectObjectsToLockAfterRefresh(allSnapPoints, snapsToIgnoreAfterLock,
					logicObjectsToLockAfterRefresh);
			if (!logicObjectsToLock.containsAll(logicObjectsToLockAfterRefresh)) {
				lockFailedObjectUIDs.addAll(LogicObjectLockFinder
						.tryEdit(((ILogicModel) getModel()).getDesign(), logicObjectsToLockAfterRefresh));
			}
		}
		return lockFailedObjectUIDs;
	}

	private void applyEdits(Collection<ISegmentSnapLockAndEdit> handlers,
			Collection<Pair<IDynamicSnap, Integer>> allSnapPoints, Set<IUID> lockFailedObjectUIDs)
	{
		for (ISegmentSnapLockAndEdit handler : handlers) {
			Predicate<IDynamicSnap> predicate = handler.getSnapsToIgnoreAfterLock(lockFailedObjectUIDs);
			handler.completeEdits(allSnapPoints, predicate);
		}
	}
}
