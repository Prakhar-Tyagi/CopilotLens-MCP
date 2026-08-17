/*
 * Copyright 2006-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.DeleteContext;
import chs.cof.drawplus.IConnected;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUIDObject;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Subclass of Logic Delete action to handle optional deletion of connectivity.
 */
public class PurgeAction extends DeleteAction
{

	public PurgeAction(ICapletController controller)
	{
		super(controller);
		deleteConnectivity = true;
	}

	@Override @NotNull public String getActionUIClass()
	{
		return PurgeActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// NO-OP here - this action does not get into the context menu
	}

	protected boolean isDeletable(@NotNull SelectSet sset)
	{
		// collect up the logic objects and do some basic checking as we go
		Set<ILogicObject> logicObjects = new HashSet<ILogicObject>();
		for (SelectedUIDObjectIterator it = sset.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();
			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(obj);
			if (logicObject != null) {
				logicObjects.add(logicObject);
			}

			// for everything except direct selection of logic objects we do the same as Delete
			if (!(obj instanceof ILogicObject) && !isDeletable(obj, sset)) {
				// follow the same rules as delete for non-logic objects
				return false;
			}
		}

		// only enabled if the selection references some connectivity object
		if (logicObjects.isEmpty()) {
			return false;
		}

		// only enabled if all of the logic objects are deletable
		// i.e. if we have selected all representations and all representations are on the current diagram
		// re-use the code used by DeleteHelper to partition the deleted objects and determine this
		ILogicModel model = (ILogicModel) getController().getCapletModel();
		DeleteContext context = new DeleteContext(model.getDiagram(), sset, true);
		for (ILogicObject logicObject : logicObjects) {
			if (!isPurgeable(logicObject, context)) {
				return false;
			}
		}

		return true;
	}

	private boolean isPurgeable(ILogicObject logicObject, DeleteContext context)
	{
		if (logicObject instanceof chs.cof.logical.cable.IConductor) {
			return isConductorPurgeable((chs.cof.logical.cable.IConductor) logicObject, context);
		}
		if (logicObject instanceof chs.cof.logical.cable.IPinList) {
			return isPinListPurgeable((chs.cof.logical.cable.IPinList) logicObject, context);
		}
		if (logicObject instanceof IAbstractPin) {
			return isPinPurgeable((IAbstractPin) logicObject, context);
		}
		if (logicObject instanceof IShieldBody) {
			return false; // these go when the multicore does
		}
		if (logicObject instanceof IMulticore) {
			return isMulticorePurgeable((IMulticore) logicObject, context);
		}
		if (logicObject instanceof IAssembly) {
			return isAssemblyPurgeable((IAssembly) logicObject, context);
		}
		assert false : "Unexpected Logic object type";
		return false;
	}

	private boolean isAssemblyPurgeable(IAssembly assembly, DeleteContext context)
	{
		// TODO jacobt FEAT13040 : Currently we're stuck with assemblies
		return false;
	}

	private boolean isMulticorePurgeable(IMulticore multicore, DeleteContext context)
	{
		// multicores are purgeable if all inners are
		// TODO jacobt FEAT13040 : Optimize out double check of logic objects here?
		for (IConductorIterator it = multicore.getConductors(); it.hasNext(); ) {
			if (!isConductorPurgeable(it.getNext(), context)) {
				return false;
			}
		}
		for (IMulticoreIterator it = multicore.getMulticores(); it.hasNext(); ) {
			if (!isMulticorePurgeable(it.getNext(), context)) {
				return false;
			}
		}
		return true;
	}

	private boolean isPinPurgeable(IAbstractPin pin, DeleteContext context)
	{
		// a logic pin may be purged if:
		// * All representatinos were included in the selection
		// * All representations are on the active diagram (checked elsewhere)
		int usageCount = getDWUM(pin).getDesignSharedUsageCount(pin);
		int count = 0;
		for (IPin schemPin : context.getPins()) {
			if (schemPin.getConnectivity() == pin) {
				++count;
			}
		}
		return count >= usageCount;
	}

	private boolean isPinListPurgeable(chs.cof.logical.cable.IPinList pinlist, DeleteContext context)
	{
		// a logic pinlist may be purged if:
		// * All representatinos were included in the selection
		// * All representations are on the active diagram (checked elsewhere)
		int usageCount = getDWUM(pinlist).getDesignSharedUsageCount(pinlist);
		int count = 0;
		for (IPinList schemPinlist : context.getPinLists()) {
			if (schemPinlist.getConnectivity() == pinlist) {
				++count;
			}
		}
		return count >= usageCount;
	}

	private boolean isConductorPurgeable(chs.cof.logical.cable.IConductor conductor, DeleteContext context)
	{
		// a logic conductor may be purged if:
		// * All representations were included in the selection (now in the delete context)
		// * All representations are on the active diagram (checked elsewhere)
		// * Each representation has all segments selected (now in the context)
		// TODO jacobt FEAT13040 - refactor this?  isDeletable could be declared on the delete context?
		int usageCount = getDWUM(conductor).getDesignSharedUsageCount(conductor);

		Set<IConductor> schemConductors = context.getConductors().getSet(conductor);
		if (schemConductors.size() < usageCount) {
			return false;
		}

		Set<ISegment> segments = context.getSegments();
		for (IConductor schemConductor : schemConductors) {
			for (IConnected connected : schemConductor.getSegments()) {
				if (connected instanceof ISegment) {
					// IJ doesnt like it with or without the cast
					//noinspection RedundantCast
					if (!segments.contains((ISegment) connected)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private IDesignWideUsageMgr getDWUM(ILogicObject logicObject)
	{
		ILogicDesign design = logicObject.getLogicDesign();
		assert design != null;
		return design.getDesignWideUsageMgr();
	}
}
