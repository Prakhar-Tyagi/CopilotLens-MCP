/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2015-2024 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.ISegmentSnapLockAndEdit;
import chs.caf.caplet.helpers.ISegmentSnapLockAndEditProvider;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.common.IUIDProvider;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.Pair;
import chs.utility.DiagramHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRouteUnRouteHighwayAction extends ControllerActionRT implements ICtxMenuProvider
{

	AbstractRouteUnRouteHighwayAction(ICapletController controller)
	{
		super(controller);
	}

	protected boolean isActionApplicable(SelectSet selectionSet)
	{
		return isActionApplicable(getUIDObjects(selectionSet));
	}

	protected boolean isActionApplicable(SelectionIterator selectionIterator)
	{
		return isActionApplicable(getUIDObjects(selectionIterator));
	}

	private boolean isActionApplicable(Set<IUIDObject> uidObjects)
	{
		SelectedObjectSet selectedObjects = new SelectedObjectSet(uidObjects).invoke();

		if (!areObjectsOfHighwayOrConductor(selectedObjects.getLogicObjects())) {
			return false;
		}
		if (!objectOfSameDiagram(selectedObjects.getDiagramObjects())) {
			return false;
		}

		boolean enableAction = isSelectionValidForAction(selectedObjects);
		return enableAction && areLockable(getLockables(selectedObjects));
	}

	abstract boolean isSelectionValidForAction(SelectedObjectSet selectedObjectSet);

	abstract Collection<ILogicObject> getLockables(SelectedObjectSet objectSet);

	@NotNull private Set<IUIDObject> getUIDObjects(SelectSet selectionSet)
	{
		SelectionIterator selectionIterator = selectionSet.getSelected();

		return getUIDObjects(selectionIterator);
	}

	@NotNull private Set<IUIDObject> getUIDObjects(SelectionIterator selectionIterator)
	{
		Set<IUIDObject> uidObjects = new HashSet<>();
		while (selectionIterator.hasNext()) {
			Selection selection = selectionIterator.getNext();
			uidObjects.add(selection.getObject());
		}
		return uidObjects;
	}

	private boolean areObjectsOfHighwayOrConductor(Collection<ILogicObject> logicObjects)
	{
		return logicObjects.stream().allMatch(obj -> obj instanceof IHighwayConductor || obj instanceof IGeneralHighway);
	}

	private boolean objectOfSameDiagram(Collection<IDiagramObject> objects)
	{
		IBaseDiagram diagram = CAFUtils.getInstance().getActiveDiagram();
		for (IDiagramObject diagramObj : objects) {
			ISchemDiagram tempDiagram = DiagramHelper.getDiagram(diagramObj);
			if (diagram == null) {
				diagram = tempDiagram;
			}
			else if (diagram != tempDiagram) {
				return false;
			}
		}

		return true;
	}

	protected String getDisplayName()
	{
		return (String) getActionUI().getValue(Action.NAME);
	}

	protected boolean areLockable(Collection<ILogicObject> lockables)
	{
		for (ILogicObject object : lockables) {
			if (LogicObjectLockFinder.isLogicObjectLockedInOtherSession(object)) {
				return false;
			}
		}
		return true;
	}

	protected boolean connectSegmentWithSnaps(Collection<Pair<IDynamicSnap, Integer>> snaps,
			IDynamicGfxMediator segment)
	{
		ISegmentSnapLockAndEditProvider segmentWrapper = null;
		if (segment instanceof IUIDObject) {
			segmentWrapper = UIDMgr.getObjectOfType(((IUIDProvider) segment).getUID(),
					ISegmentSnapLockAndEditProvider.class);
		}
		if (segmentWrapper != null) {
			ISegmentSnapLockAndEdit logicObjectLockAndEdit = segmentWrapper.createLogicObjectLockAndEdit();
			return logicObjectLockAndEdit != null && logicObjectLockAndEdit.completeEdits(snaps, x -> true);
		}
		return segment.addConnectivity(snaps.iterator());
	}

	protected static class SelectedObjectSet
	{

		private Set<IUIDObject> uidObjects;
		private Collection<IDiagramObject> diagramObjects;
		private Collection<ILogicObject> logicObjects;

		SelectedObjectSet(Set<IUIDObject> uidObjects)
		{
			this.uidObjects = uidObjects;
		}

		public Collection<IDiagramObject> getDiagramObjects()
		{
			return diagramObjects;
		}

		public Collection<ILogicObject> getLogicObjects()
		{
			return logicObjects;
		}

		public SelectedObjectSet invoke()
		{
			diagramObjects = CollectionUtils.getObjects(uidObjects, IDiagramObject.class);
			logicObjects = CollectionUtils.getObjects(uidObjects, ILogicObject.class);
			logicObjects.addAll(getConnectivityObjects());
			return this;
		}

		@NotNull private Collection<ILogicObject> getConnectivityObjects()
		{
			Collection<ILogicObject> cableObjs = new HashSet<>();
			diagramObjects.stream().forEach(x -> {
				if (x instanceof IConnectivityRef) {
					ILogicObject connectivity = ((IConnectivityRef) x).getConnectivity();
					if (connectivity != null) {
						cableObjs.add(connectivity);
					}
				}
			});
			return cableObjs;
		}
	}
}
