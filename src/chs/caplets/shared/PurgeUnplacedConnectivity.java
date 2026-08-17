/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.LogicUpdateStyledGraphicsHandler;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISingleLine;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.system.IDeleteContext;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.PinListHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class PurgeUnplacedConnectivity
{

	private Collection<IUIDObject> changeddObjectsUIDs;
	private Collection<IUIDObject> deletedObjectsUIDs;

	public PurgeUnplacedConnectivity()
	{
		changeddObjectsUIDs = new HashSet<IUIDObject>();
		deletedObjectsUIDs = new HashSet<IUIDObject>();
	}

	/**
	 * Handle unplaced objects on a Logic design that is about to be saved.  These get deleted if a preference to do so
	 * is set.
	 *
	 * @param design The design
	 * @param model  - model
	 */
	public void handleUnplacedObjects(ILogicDesign design, @Nullable Model model)
	{
		if (design == null || design.getConnectivity() == null) {
			// this can happen if the Save is called by side-effect of some other action (e.g. New)
			return;
		}
		// cautiously get the preference from the project
		if (LogicUtils.isPurgeOnSaveEnabled(design)) {

			// delete any object that has not representation in the design
			purge(design);

			// if unplaced objects were purged, we have to trigger a notification that the model changed
			notifyModel(design, model);
		}
	}

	public void forcePurgeUnplacedObjects(@NotNull ILogicDesign design, @Nullable Model model)
	{
		if (design.getConnectivity() == null) {
			return;
		}

		purge(design);
		notifyModel(design, model);
	}

	private void notifyModel(ILogicDesign design, @Nullable Model model)
	{
		if (model != null && (!deletedObjectsUIDs.isEmpty() || !changeddObjectsUIDs.isEmpty())) {
			// todo review this with Steve Bold, purged should be passed as deleted list param, wrong constructor was being
			// used for model change event
			ModelChangeEvent e =
					new ModelChangeEvent(model,
							UIDUtils.convertToUID(changeddObjectsUIDs),
							Collections.<IUID>emptyList(),
							UIDUtils.convertToUID(deletedObjectsUIDs));

			// dts0100873170 - if the purged pins are being referred in the table data then we need to
			// update the table
			//todo review this with Steve Bold, similar defect was fixed by him in MoveToAction (DR 874681)
			//model.notifyPreModelChange(e); // this will call all model change listeners can this cause some undesired effect??
			LogicUpdateStyledGraphicsHandler updateStyledGraphicsHandler = new LogicUpdateStyledGraphicsHandler();
			updateStyledGraphicsHandler.updateStyledGraphics(design, Collections.<IUIDObject>emptyList(),
					changeddObjectsUIDs,
					deletedObjectsUIDs);

			model.notifyModelChange(e);
		}
	}

	/**
	 * Delete unplaced connectivitity objects from the design.
	 *
	 * @param design The design
	 */
	private void purge(ILogicDesign design)
	{
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		IConnectivity conn = design.getConnectivity();
		assert conn != null;
		Collection<IUIDObject> objectsToPurge = new ArrayList<IUIDObject>();

		collectPinsAndPinLists(design, conn, objectsToPurge);

		collectConductors(design, conn, objectsToPurge);

		collectSingleLinesAndHighWays(design, conn, objectsToPurge);

		collectMultiCores(conn, design, objectsToPurge);

		if (objectsToPurge.isEmpty()) {
			return;
		}

		IDeleteContext context =
				DeleteHelper.getInstance().delete(design, objectsToPurge, "PURGE", true);
		changeddObjectsUIDs.addAll(context.getAssemblyPotentials());
		deletedObjectsUIDs.addAll(objectsToPurge);

		//Purge might have created some new objects (internal pins).
		// In case of normal action, endEdit would increment the timestamp of undo container
		// For Purge, do it explicitly(dts0101166309)
		final ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		if (controller != null) {
			controller.getUndoableContainer().incTimestamp();
		}
		// In case of normal controller action, it would clear the CDH.
		//For Purge, it has to be done explicitly (dts0100806844)
		cdh.processObjects();
	}

	private void collectSingleLinesAndHighWays(ILogicDesign design, IConnectivity conn, Collection<IUIDObject> objectsToPurge)
	{
		for (ISingleLine singleLine : conn.getSingleLines()) {
			if (isQualifiedForPurge(singleLine, design)) {
				objectsToPurge.add(singleLine);
			}
		}
		for (IHighway highway : conn.getHighways()) {
			if (isQualifiedForPurge(highway, design)) {
				objectsToPurge.add(highway);
			}
		}
	}

	private void collectConductors(ILogicDesign design, IConnectivity conn, Collection<IUIDObject> objectsToPurge)
	{
		for (IConductor conductor : conn.getConductors()) {
			// Only delete standalone conductors (i.e., not part of any multicore).
			// Conductors inside multicore are handled separately in the multicore purge logic below.
			//
			// Note: Conductors in a multicore (including shields used for implicit connectivity
			// via daisy chains) must not be deleted here to preserve unplaced multicore functionality.
			// Deletion of such conductors is allowed only when the entire multicore is eligible for purge.
			if (conductor.getMulticore() == null && !isAssociatedFunctionSignal(conductor) &&
					isQualifiedForPurge(conductor, design)) {
				objectsToPurge.add(conductor);
			}
		}
	}

	private void collectPinsAndPinLists(ILogicDesign design, IConnectivity conn, Collection<IUIDObject> objectsToPurge)
	{
		for (IPinList pinlist : conn.getPinLists()) {
			// pins
			for (IAbstractPin pin : pinlist.getPins()) {
				if (isQualifiedForPurge(pin, design)) {
					objectsToPurge.add(pin);
				}
			}

			// the pinlist itself
			if (isQualifiedForPurge(pinlist, design) && !isModularConnector(pinlist)) {
				objectsToPurge.add(pinlist);
			}
		}
	}

	private void collectMultiCores(IConnectivity conn, ILogicDesign design, Collection<IUIDObject> objectsToPurge)
	{
		for (IMulticore multicore : conn.getMulticores()) {
			if (isQualifiedForPurge(multicore, design) && !design.getDesignWideUsageMgr().hasMulticoreUsage(multicore)) {
				objectsToPurge.addAll(multicore.getAllMulticoresInHierarchy());
				objectsToPurge.addAll(multicore.getRootMulticore().getAllConductorsInHierarchy(true));
			}
		}
	}

	private boolean isAssociatedFunctionSignal(IConductor conductor)
	{
		return (conductor instanceof IFunctionConductor) &&
				((IFunctionConductor) conductor).isAssociatedMessageSignal();
	}

	public static boolean isModularConnector(ILogicObject obj)
	{
		if (obj instanceof IConnector) {
			IConnector connector = (IConnector) obj;
			if (PinListHelper.isModularConnector(connector) || PinListHelper.isChildConnector(connector)) {
				return true;
			}
		}
		return false;
	}

//	public Collection<IUIDObject> getModifiedObjects()
//	{
//		return changeddObjectsUIDs;
//	}

	public Collection<IUIDObject> getDeletedObjects()
	{
		return deletedObjectsUIDs;
	}

	private boolean isQualifiedForPurge(ILogicObject obj, ILogicDesign design)
	{
		ILogicObject purgubleObj = obj;
		if (obj instanceof IGenericPin) {
			purgubleObj = ((IGenericPin) obj).getOwner();
		}

		if (obj instanceof IConnectorPin && ((IConnectorPin) obj).isBlockedCavity()) {
			return false;
		}
		if (isLibrariedAssemblyPart(purgubleObj)) {
			return false;
		}
		//has usage is faster and will hash over the respective store for shared/non-shared objects. we should be
		//going for exaustive approach only if it fails. otherwise we will have unnecessary performance issues.
		return !design.getDesignWideUsageMgr().hasUsage(obj);
	}

	//Check whether the object is a part of a libraried assembly
	public static boolean isLibrariedAssemblyPart(@Nullable ILogicObject obj)
	{
		ILogicObject currentRoot = obj;
		while (currentRoot != null) {
			IAssembly assembly = currentRoot.getAssembly();
			if (assembly != null && assembly.isPartAssigned()) {
				return true;
			}
			currentRoot = assembly;
		}
		return false;
	}

//	// dts0100884057 S4:C1:BASH22:Logic assembly table does not refreshes for object when they are deleted on save operation (objects are in unplaced state)
//	private class PurgedObjectsInfo
//	{
//
//		// dts0100910830 & dts0100922693, if we store purged objects as uid then if we are performing
//		// a non undoable action which internally calls save e.g. create New diagram then when the purged objects
//		// are deleted, from uid we cannot get the original object basically there is no IDeletedObject available
//		// as a result of this we cannot determine what types of object are deleted and hence we cannot determine
//		// what types of tables to be updated
//		// because of this changed the type from uid to uidobject
//
//		// this contains all the purged objects
//		private List<IUIDObject> m_purgedObjs;
//		// this contains the objects that got modified as a result of purged object deletion e.g. assembly
//		private List<IUIDObject> m_modifiedObjs;
//
//		private void addPurgedObject(@NotNull IUIDObject purgedObj)
//		{
//			if (m_purgedObjs == null) {
//				m_purgedObjs = new ArrayList<IUIDObject>();
//			}
//			if (!m_purgedObjs.contains(purgedObj)) {
//				m_purgedObjs.add(purgedObj);
//			}
//			// purged object need not be in modified list
//			if (m_modifiedObjs != null && m_modifiedObjs.contains(purgedObj)) {
//				m_modifiedObjs.remove(purgedObj);
//			}
//		}
//
//		@NotNull private List<IUIDObject> getPurgedObjects()
//		{
//			return m_purgedObjs == null ? Collections.<IUIDObject>emptyList() : m_purgedObjs;
//		}
//
//		private void addModifiedbject(@NotNull IUIDObject modifiedObj)
//		{
//			if (m_purgedObjs != null && m_purgedObjs.contains(modifiedObj)) {
//				return;
//			}
//
//			if (m_modifiedObjs == null) {
//				m_modifiedObjs = new ArrayList<IUIDObject>();
//			}
//			// if the modified object is not present in purged object and it is not already in modified object list
//			// only then add it
//			if (!m_modifiedObjs.contains(modifiedObj)) {
//				m_modifiedObjs.add(modifiedObj);
//			}
//		}
//
//		@NotNull private List<IUIDObject> getModifiedObjects()
//		{
//			return m_modifiedObjs == null ? Collections.<IUIDObject>emptyList() : m_modifiedObjs;
//		}
//
//		private boolean isEmpty()
//		{
//			return getPurgedObjects().isEmpty() && getModifiedObjects().isEmpty();
//		}
//	}
}