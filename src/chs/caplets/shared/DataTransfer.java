/*
 * Copyright 2002-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;
//chs imports

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.PasteAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.helpers.BaseDataTransfer;
import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.draw.IAbsoluteLocationConvertible;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxGroup;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.ILocationChangeNotifiable;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPropertiedGfxGroup;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.drawplus.table.ITableGroup;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.DesignUtils;
import chs.common.IDesignContainer;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.common.styles.IFixPositionNotifiable;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ISharedObjectAvailabilityChecker;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains caplet specific implementations of the IDataTransfer functions used for transfering data around
 * using the clipboard. <br/>
 * <p>
 * The current implementation of this simply uses the Replictor functionality along with the current select set to
 * copy/paste objects.  We do not have cut/copy/paste functionality outside of CAF. In other words, this implementation
 * is not going to use the clipboard with XML.
 */
public abstract class DataTransfer extends BaseDataTransfer implements MouseListener, MouseMotionListener
{

	/**
	 * These are the object that will be added to the CreationDeletionHelper.  Note that these can't be added at the
	 * time of copy because they aren't completely created yet.
	 */
	protected Set<IUIDObject> m_creationObjects;

	protected boolean m_overrideBufferClear;

	protected boolean transDrawn = false;
	protected Set<IUIDObject> newObjCache = new LinkedHashSet<>();
	protected boolean mouseMoved = false;

	protected DataTransfer()
	{
		m_creationObjects = new HashSet<IUIDObject>();
		m_overrideBufferClear = false;
		m_lastCopiedObjects = new HashSet<IUIDObject>();
	}

	protected boolean isIndicatorIncluded(SelectSet sset, IJoint j)
	{
		if (j == null) {
			return false;
		}
		Collection<IShieldBodyHookup> c = j.getAssociations(IShieldBodyHookup.class);
		for (IShieldBodyHookup sbh : c) {
			IShieldBody sb = sbh.getShieldBody();
			if (sset.contains(sb.getUID())) {
				return true;
			}
		}
		return false;
	}

	@NotNull protected Set<IGfxObject> getFilteredObjectSet(SelectSet selSet)
	{
		SelectedUIDObjectIterator objIter = selSet.getSelectedUIDObjects();
		//skip this object if its parent is already in the selection set.
		//otherwise we will get double addition to undo queue runtime exception issue.
		Set<IGfxObject> filteredGfxObjects = new HashSet<IGfxObject>();
		while (objIter.hasNext()) {
			IUIDObject origObject = objIter.getNext();
			if (origObject instanceof IDiagramObject) {
				if (isAssociatedGraphicObject((IGfxObject) origObject)) {
					IDiagramObject associatedObj = (IDiagramObject) origObject;
					IDiagramObject parent = associatedObj.getParent();
					//search till the parent is null or schem diagram.
					while (parent != null && !(parent instanceof ISchemDiagram)) {
						if (selSet.contains(parent.getUID())) {
							filteredGfxObjects.add((IGfxObject) origObject);
							break;
						}
						parent = parent.getParent();
					}
				}
			}
		}
		return filteredGfxObjects;
	}

	protected boolean isAssociatedGraphicObject(IGfxObject origObject)
	{
		//the associated graphic objects will be supported to be copied and pasted at the same diagram only. and the
		//association will be maintained. so storing the original container in the copied object. we will not add this
		//object to the original container. the addition will happen in paste if the same diagram condition is satisfied.
		if (origObject instanceof IDiagramObject && AttributeUtils.isPropertiedGraphicsObject(origObject)) {
			if (!((IDiagramObject) origObject).isDirectDiagramChild()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * *CapitalLogic specific implementation of the isCopyAllowed method which is defined in the IDataTransfer interface
	 * *@return A string which represents a human readable description of the CapitalLogic object(s) that can be copied.
	 * *This return value will be "" if the selected items are not copyable.
	 */
	public boolean isCopyAllowed(ICapletController controller)
	{
		SelectSet sset = controller.getSelectMgr().getPreSelections();
		//ShieldBody copy/paste behaviour is different from shieldConductor. So we decided not
		//to copy them if shieldBody & ShieldConductor is selected but all multicore content is not
//		Collection<IShieldBody> shieldBodyWithShieldCollection = getShieldBodyWithShieldSelected(sset);
//		for (IShieldBody shieldBody : shieldBodyWithShieldCollection) {
//			//If this kind of shield body , all its conductor must be there in the selection list
//			ISchemDiagram diagram = DiagramHelper.getDiagram(shieldBody);
//			if (!allMulticoreContentsAreSelected(shieldBody.getConnectivity().getMulticore(), diagram, sset)) {
//				return false;
//			}
//		}
		Set<IGfxObject> filteredGfxObjects = getFilteredObjectSet(sset);
		SelectionIterator iter = sset.getSelected();
		while (iter.hasNext()) {
			Selection sel = iter.getNext();
			IUIDObject obj = DesignUtils.getLoadedObject(sel.getUID());
			if (filteredGfxObjects.contains(obj)) {
				continue;
			}
			if (obj instanceof IPinList) {
				IPinList pl = (IPinList) obj;
				chs.cof.logical.cable.IPinList cpl = pl.getConnectivity();
				if (cpl instanceof IDeviceConnector) {
					for (IDiagramObjectIterator itr = pl.getAttachedObjects(); itr.hasNext(); ) {
						IDiagramObject dobj = itr.getNext();
						if (!sset.contains(dobj.getUID())) {
							return false;
						}
					}
				}
			}
			if (obj instanceof ISchemDiagram) {
				return false;
			}
			if (obj instanceof IChainSegmentContainer) {
				//
				// If the end hookups/indicators are not on the list, then
				// we bail on this object.
				IChainSegmentContainer chain = (IChainSegmentContainer) obj;
				if (areAllIndicatorsIncludedInSelection(sset, chain) && areAllSegmentsSelected(sset, chain)) {
					return true;
				}
			}

			else if (obj instanceof IDiagramObject) {
				IDiagramObject diagObj = (IDiagramObject) obj;
				if (isObjectCopyable(diagObj)) {
					return true;
				}
			}
			else if (obj instanceof IMulticore || obj instanceof IAssembly) {   // Allow to copy multicore
				return true;
			}
		}
		return false;
	}

	@Nullable protected ISharedObject getSharedObject(IUIDObject obj)
	{
		ISharedObject sharedObject;
		if (obj instanceof IShieldBody) {
			chs.cof.logical.cable.IShieldBody shieldBody = ((IShieldBody) obj).getConnectivity();
			IMulticore multicore = shieldBody.getMulticore();
			sharedObject = multicore != null ? multicore.getSharedObject() : null;
		}
		else {
			sharedObject = ReferenceHelper.reduceToSharedObject(obj);
		}
		return sharedObject;
	}

	public boolean isPasteAllowed()
	{
		if (!pasteDataAvailable()) {
			return false;
		}
		return m_objectBuffer.stream().filter(obj -> {
			if (obj instanceof IDiagramObject) {
				return isObjectPastable((IDiagramObject) obj);
			}
			return true;
		}).count() > 0;
	}

	public boolean pasteDataAvailable()
	{
		if ((m_objectBuffer != null) && (m_objectBuffer.size() > 0)) {
			return true;
		}
		return false;
	}

	/**
	 * *@return true if the paste was successful and false if it failed for some reason
	 */
	public boolean doPaste()
	{
		boolean bSuccess = false;

		bSuccess = true;

		return bSuccess;
	}

	// The following extensions are to support cut/copy/paste based within CAF applications only. In other words,
	// implementations of the following are not required to use the standard clipboard.

	/**
	 * Method to remove copied objects from all of the automatically added places. Factory methods use
	 * CreationDeletionHelper and add to UIDMgr, which needs to be fixed up.
	 */
	protected void cleanUpAfterCopy()
	{
		// at this point clear the creation objects since the creation deletion helper will have
		// added a bunch of items that are not appropriate. Also, we can't explicitly remove objects
		// from the CDHelper because it has the side effect of adding them to deletion, so just clear
		// the objects we've created

		// remove all of the newly created objects from the UIDMgr
		Iterator<IUIDObject> iter = CreationDeletionHelper.getTheCreationHelper().getNewObjectsToProcess();
		// clean up the last set
		m_lastCopiedObjects.clear();
		removeObjectAndChildrenFromUIDMgr(iter, m_lastCopiedObjects);
		newObjCache.clear();
		// clear the new objects from the CD helper
		CreationDeletionHelper.getTheCreationHelper().clearNewObjects();
	}

	/**
	 * Simply saves off the items selected to be copied later.
	 *
	 * @return boolean, indicating whether the copy was succesful
	 */
	public boolean doCAFCopy(@NotNull ICapletController controller)
	{
		initCAFCopy();
		// set the source model
		setSourceCapletModel(controller.getCapletModel());
		setCurrentViewAsSourceView();
		GfxView gfxView = (GfxView) getSourceView();
		SelectSet curSels = controller.getSelectMgr().getPreSelections();
		return doCAFCopy(gfxView, curSels);
	}

	protected void initCAFCopy()
	{
		clearPasteBuffer();
		transDrawn = false;
		mouseMoved = false;
		m_overrideBufferClear = true;
	}

	public boolean doCAFCopy(@Nullable GfxView gview, SelectSet selectSet)
	{
		IDataTransferReplicator replicator = createReplicator(selectSet);
		m_objectBuffer = replicatedSet(replicator, selectSet);
		setCurrentSelection(selectSet);

		copyLayerNumber(replicator);
		resolveOwnerForAssociatedObjects(replicator, selectSet);
		rebuildAffectedGfxGroup();
		calculateSelectedObjsCenter();
		//ASSETS-9233:Table of Copy pasted device overlaps on the source table
		postProcessChildObjectsWithFixPosition(selectSet);
		// register any naming preservations.
		performPostCopy(replicator);
		cleanUpAfterCopy();
		if (!NotaRepeatedCopy) {
			cleanUpTransients();
		}
		if (gview != null && m_sourcePoint != null && NotaRepeatedCopy && isPasteEnabled()) {
			int offset = getOffset(gview);
			Point delta = getDelta(
					gview, new Point(m_sourcePoint.x + offset, m_sourcePoint.y + offset));
			moveCopiedTransients(gview, delta.x, delta.y);
		}
		updatePasteActionUI();
		m_overrideBufferClear = false;
		return true;
	}

	protected void updatePasteActionUI()
	{
		PasteAction pasteAction = (PasteAction) CAFUtils.getInstance().getActionUI(PasteAction.class.getName());
		if (pasteAction != null) {
			pasteAction.updateUI();
		}
	}

	private void rebuildAffectedGfxGroup()
	{
		List<IGfxGroup> topGroups = new ArrayList<>();
		for (IUIDObject object : m_objectBuffer) {
			if (object instanceof IGfxGroup && !((IGfxGroup) object).isGrouped()) {
				topGroups.add((IGfxGroup) object);
			}
		}
		for (IGfxGroup topGroup : topGroups) {
			rebuildGfxGroup(topGroup);
		}
	}

	private void rebuildGfxGroup(IGfxGroup gfxGroup)
	{
		for (IGfxObject gfxObject : gfxGroup.getGfxObjects()) {
			if (gfxObject instanceof IGfxGroup) {
				rebuildGfxGroup((IGfxGroup) gfxObject);
			}
		}
		gfxGroup.rebuildObjects();
	}

	//Make sure the Absolute Location is set on the child objects with fix position for all copied objects.
	private void postProcessChildObjectsWithFixPosition(SelectSet curSels)
	{
		SelectedUIDObjectIterator selectedUIDObjects = curSels.getSelectedUIDObjects();
		while (selectedUIDObjects.hasNext()) {
			IUIDObject selectedUIDObject = selectedUIDObjects.getNext();
			if (selectedUIDObject instanceof ICompoundObject &&
					selectedUIDObject instanceof ILocationChangeNotifiable) {
				processChildObjectsWithFixPosition((ICompoundObject) selectedUIDObject);
			}
		}
	}

	private void processChildObjectsWithFixPosition(@NotNull ICompoundObject newSchemPL)
	{
		Collection<IFixPositionNotifiable> fixedObjects = newSchemPL.getObjects(IFixPositionNotifiable.class);
		for (IFixPositionNotifiable fixedObject : fixedObjects) {
			if (fixedObject instanceof ITableGroup) {
				updateAbsoluteLocationOnTables((ITableGroup) fixedObject);
			}
		}
	}

	private void updateAbsoluteLocationOnTables(@NotNull ITableGroup tableGroup)
	{
		tableGroup.updateAbsoluteLocation();
		IGfxObjectIterator families = tableGroup.getObjects();
		for (IGfxObject family : families) {
			((IAbsoluteLocationConvertible) family).updateAbsoluteLocation();
			IGfxObjectIterator tables = ((ICompoundObject) family).getObjects();
			for (IGfxObject table : tables) {
				((IAbsoluteLocationConvertible) table).updateAbsoluteLocation();
			}
		}
	}

	public boolean doCAFPaste(@NotNull ICapletController controller)
	{
		transDrawn = false;
		cleanUpTransients();
		clearCurrentEdit();
		return true;
	}

	protected void clearCurrentEdit()
	{
		IUndoableContainer container = CAFUtils.getInstance().getCurrentUndoableContainer();
		if (container != null && container.getCurrentEdit() != null) {
			container.getCurrentEdit().clear();
		}
	}

	public void cleanUpTransients()
	{
		IGfxModel activeGfxModel = getActiveGfxModel();
		if (activeGfxModel != null) {
			IDynamicGfxService dynamics = activeGfxModel.getDynamicGfxService();
			if (dynamics != null) {
				dynamics.removeAllTransientGfx();
			}
		}
		IGfxModel sourceModel = getPreviousCapletModel();
		if (sourceModel != null) {
			IDynamicGfxService dynamicGfxService = sourceModel.getDynamicGfxService();
			if (dynamicGfxService != null) {
				dynamicGfxService.removeAllTransientGfx();
			}
		}
	}

	@Nullable protected IGfxModel getActiveGfxModel()
	{
		ICapletController activeCapletController = CAFUtils.getInstance().getActiveCapletController();
		return activeCapletController != null ?
				CommonUtils.cast(activeCapletController.getCapletModel(), IGfxModel.class) : null;
	}

	protected boolean areAllIndicatorsIncludedInSelection(SelectSet selSet, IChainSegmentContainer chain)
	{
		boolean allIndicatorsIncluded = true;
		for (IShieldBodyHookup hookup : chain.getAttachedHookups()) {
			if (!isIndicatorIncluded(selSet, hookup.getJoint())) {
				allIndicatorsIncluded = false;
				break;
			}
		}
		return allIndicatorsIncluded;
	}

	protected boolean areAllSegmentsSelected(SelectSet selSet, ISegmentCollector segmentCollector)
	{
		boolean allSegmentsSelected = true;
		for (IConnected seg : segmentCollector.getSegments()) {
			if (!selSet.contains(seg.getUID())) {
				allSegmentsSelected = false;
				break;
			}
		}
		return allSegmentsSelected;
	}

	public abstract void resolveOwnerForAssociatedObjects(IDataTransferReplicator replicator, SelectSet curSels);

	/**
	 * Deletes the current selection set and remembers it for a paste later.
	 *
	 * @return boolean, indicating whether the cut operation was successful
	 */
	public boolean doCAFCut(@NotNull ICapletController controller)
	{
		clearPasteBuffer();
		m_overrideBufferClear = true;

		// Cut what's selected
		SelectedUIDObjectIterator cutObjIter = controller.getSelectMgr().getPreSelections().getSelectedUIDObjects();
		while (cutObjIter.hasNext()) {
			IUIDObject uidObj = cutObjIter.getNext();
			CreationDeletionHelper.getTheCreationHelper().addDeletionObject(uidObj);
			m_objectBuffer.add(uidObj);
			m_creationObjects.add(uidObj);
			if (uidObj instanceof IRepresentedObject) {
				IRepresentedObject repObj = (IRepresentedObject) uidObj;
				m_creationObjects.add(repObj.getRawConnectivity());
			}
		}

		manageUndo(true);
		m_overrideBufferClear = false;

		controller.getSelectMgr().getPreSelections().clear();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		return true;
	}

	/**
	 * Do the create snapshots for the new objects.
	 *
	 * @param deleteOnly Give the ability to skip the creation step (for cut).
	 */

	protected void manageUndo(boolean deleteOnly)
	{
		CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
		if (!deleteOnly) {
			for (IUIDObject uidObj : m_creationObjects) {
				// DR 333562: attribute texts can be deleted and recreated during paste.
				if (!cdHelper.goingToDelete(uidObj)) {
					cdHelper.addCreationObject(uidObj);
				}
			}
			m_creationObjects.clear();
		}

		cdHelper.processObjects();
	}

//    protected void offsetObject(ILocation loc, GfxView gview, Set offsetSet)
//    {
//        if (offsetSet.contains(loc)) return;
//        offsetSet.add(loc);
//
//		ISheet sheet = gview.getSheet();
//
//		int offset = 0;
//		if (sheet instanceof IGriddable)
//		{
//			IGrid grid = ((IGriddable)sheet).getGrid();
//			offset = grid.getGridSpacing();
//		}
//
//        loc.setX(loc.getX() + offset);
//        loc.setY(loc.getY() + offset);
//    }

	/**
	 * recursively add all of the compound objects to the list of creation objects. This is necessary for compounds that
	 * contain compounds, like devices, that contain pins, that contain names, properties and all of the other types of
	 * objects
	 *
	 * @param co
	 */
	private void addCompoundsToCreationObjects(ICompoundObject co)
	{
		for (IGfxObjectIterator gitr = co.getObjects(); gitr.hasNext(); ) {
			IGfxObject go = gitr.getNext();
			// if this is compound within this compound, do it's subordinates
			if (go instanceof ICompoundObject) {
				addCompoundsToCreationObjects((ICompoundObject) go);
			}
			// add this object to the created objects
			if (go instanceof IUIDObject) {
				m_creationObjects.add((IUIDObject) go);
			}
		}
	}

	/**
	 * Adds an object to the selection set. A wrapper for a bit of bookkeeping.
	 *
	 * @param newObject
	 * @param origObject
	 * @param newObjs
	 */
	protected void addNewSelection(IUIDObject newObject, IUIDObject origObject, Collection<IUIDObject> newObjs)
	{
		m_creationObjects.add(newObject);
		if (newObject instanceof ICompoundObject) {
			// if the subordinate object is a compound, go add all of its subordinates
			addCompoundsToCreationObjects((ICompoundObject) newObject);
		}
		if (newObject instanceof IGfxObject) {
			IGfxObject newGfxObj = (IGfxObject) newObject;
			newGfxObj.getLocation().setX(((IGfxObject) origObject).getLocation().getX());
			newGfxObj.getLocation().setY(((IGfxObject) origObject).getLocation().getY());
		}

		if (newObject != null) {
			newObjs.add(newObject);
		}
	}

	/**
	 * Clear our internal paste buffer
	 */
	public void clearPasteBuffer()
	{
		if (m_overrideBufferClear) {
			return;
		}
		m_cutInProgress = false;
		m_underCut = false;
		if (m_objectBuffer != null) {
			m_objectBuffer.clear();
		}
		//
		// Remove all the objects in the creation list, from the UID Manager.
		//
		cleanUpTransients();
		if (m_creationObjects != null) {
			removeObjectAndChildrenFromUIDMgr(m_creationObjects.iterator(), null);
			m_creationObjects.clear();
		}
		updatePasteActionUI();
	}

	protected abstract boolean isObjectCopyable(IDiagramObject obj);

	protected abstract boolean isObjectPastable(IDiagramObject obj);

	protected void resetRepeatedCopyFlag()
	{
		NotaRepeatedCopy = persistCopyGraphics;
	}

	protected abstract Collection<IUIDObject> replicatedSet(IDataTransferReplicator replicator, SelectSet selSet);

	// Remove non-pasteable object from the objectBuffer and CreationDeletionHelper
	protected void removeObject(IUIDObject uo)
	{
		if (uo != null) {
			m_objectBuffer.remove(uo);
			m_creationObjects.remove(uo);
			CreationDeletionHelper.getTheCreationHelper().removeCreationObject(uo);
			if (uo instanceof ICompoundObject) {
				ICompoundObject co = (ICompoundObject) uo;
				for (IGfxObjectIterator gitr = co.getObjects(); gitr.hasNext(); ) {
					IGfxObject go = gitr.getNext();
					if (go instanceof IUIDObject) {
						removeObject((IUIDObject) go);
					}
				}
			}
			//dts0100513789 Validation failure when copying a splice symbol contents to another symbol type
			//If the removed object is a Pin delete its Connectivity
			if (uo instanceof IGenericSchemPin) {
				IGenericPin pinConnectivity = ((IGenericSchemPin) uo).getConnectivity();
				pinConnectivity.delete();
			}
			else if (uo instanceof ISchemInternalLink) {
				IInternalLink pinConnectivity = ((ISchemInternalLink) uo).getConnectivity();
				pinConnectivity.delete();
			}
			UIDMgr.removeObject(uo.getUID());
		}
	}

	protected void transformGfxGroup(Set<ILocation> offsetSet, @NotNull GfxView gview, @NotNull Point delta,
			double scale,
			@NotNull IGfxObject gfxObj)
	{
		ILocation loc = gfxObj.getLocation();

		// Offset the location
		offsetObject(loc, delta, offsetSet, scale);
		int dx = delta.x;
		int dy = delta.y;
		if (m_PrevPoint != null && NotaRepeatedCopy) {
			IGrid grid = gview.getGridConfig().getGrid();
			dx = m_PrevPoint.x - grid.snap(m_sourcePoint.x);
			dy = m_PrevPoint.y - grid.snap(m_sourcePoint.y);
		}
		// Now move the object and childern
		gfxObj.move(dx, dy);
	}

	public void setCopiedObjectsTransientVisibility(boolean b)
	{
		NotaRepeatedCopy = b;
	}

	public void setPersistCopyGraphics(boolean b)
	{
		persistCopyGraphics = b;
	}

	@Override public void mouseClicked(MouseEvent e)
	{

	}

	@Override public void mousePressed(MouseEvent e)
	{

	}

	@Override public void mouseReleased(MouseEvent e)
	{

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

	@Override public void mouseMoved(MouseEvent e)
	{
		mouseMoved = true;
		GfxView gfxView = (GfxView) (CAFUtils.getInstance().getActiveCapletView());
		Point curr = gfxView.deviceToWorld(e.getPoint());
		if (gfxView != getPreviousView() && transDrawn) {
			transDrawn = false;
			cleanUpTransients();
			if (needsDictionaryEntries()) {
				IProject project = getProject(gfxView.getDiagram());
				if (project != null && project != getSourceProject()) {
					m_objectBuffer.stream()
							.forEach(obj -> {
								handleSymbolDictionaryEntries(project, obj);
								handleImageDictionaryEntries(project, obj);
							});
				}
			}
		}
		if (m_PrevPoint == null) {
			m_PrevPoint = new Point();
		}
		if (isPasteEnabled()) {
			Point delta = getDelta(gfxView, curr);
			if (gfxView.getDiagram() != null && NotaRepeatedCopy) {
				moveCopiedTransients(gfxView, delta.x, delta.y);
			}
		}
	}

	@NotNull private Point getDelta(@NotNull GfxView gfxView, @NotNull Point curr)
	{
		Point delta = new Point();
		IGrid grid = gfxView.getGridConfig().getGrid();
		int snapX = grid.snap(curr.x);
		delta.x = snapX - grid.snap(m_PrevPoint.x);
		int snapY = grid.snap(curr.y);
		delta.y = snapY - grid.snap(m_PrevPoint.y);
		m_PrevPoint.setLocation(snapX, snapY);
		return delta;
	}

	protected void moveCopiedTransients(@NotNull GfxView gfxView, int deltaX, int deltaY)
	{
		IGfxModel activeGfxModel = getActiveGfxModel();
		if (activeGfxModel != null) {
			IDynamicGfxService gfxService = activeGfxModel.getDynamicGfxService();
			Set<IGfxObject> copiedObjs = m_objectBuffer.stream()
					.filter(o -> o instanceof IGfxObject)
					.map(o -> (IGfxObject) o)
					.collect(Collectors.toSet());

			Set<ILocation> pointsCache = new HashSet<>();
			copiedObjs.stream()
					.forEach(gfx ->
							moveCopiedTransient(gfxService, deltaX, deltaY, gfx, pointsCache));
			transDrawn = true;
			m_PrevModel = activeGfxModel;
			previousView = gfxView;
			gfxView.invalidate(IViewInvalidationEnum.eTransient);
			gfxView.refresh();
		}
	}

	protected void moveCopiedTransient(@NotNull IDynamicGfxService gfxService, int deltaX, int deltaY,
			@NotNull IGfxObject gfx,
			Set<ILocation> endPointsCache)
	{
		ILocation gfxLocation = gfx.getLocation();

		if (gfx instanceof ICompoundObject) {
			if (gfx instanceof IChainSegmentContainer || gfx instanceof IConductor ||
					gfx instanceof IHighwaySchematic) {
				for (IGfxObjectIterator iter = ((ICompoundObject) gfx).getObjects(); iter.hasNext(); ) {
					IGfxObject obj = iter.getNext();
					if (obj instanceof IBaseSegment) {
						IBaseSegment baseSegment = (IBaseSegment) obj;
						transformEndPoints(deltaX, deltaY, endPointsCache, baseSegment.getLocation());
						transformEndPoints(deltaX, deltaY, endPointsCache, baseSegment.getStartPoint());
						transformEndPoints(deltaX, deltaY, endPointsCache, baseSegment.getEndPoint());
					}
				}
			}
			else if (gfx instanceof IPropertiedGfxGroup) {
				transformEndPoints(deltaX, deltaY, endPointsCache, gfxLocation);
				//gfx.move(deltaX, deltaY);
			}
			else {
				moveGfxObject(gfx, deltaX, deltaY, endPointsCache);
				if (gfx instanceof IPinList) {
					moveAttachedPinlists(endPointsCache, deltaX, deltaY, (IPinList) gfx);
				}
			}
		}
		else {
			transformEndPoints(deltaX, deltaY, endPointsCache, gfxLocation);
		}
		if (!transDrawn) {
			if (!(gfx instanceof IDiagramObject) || isObjectPastable((IDiagramObject) gfx)) {
				gfxService.addTransientGfx(gfx);
			}
		}
	}

	private void transformEndPoints(int deltaX, int deltaY, Set<ILocation> endPointsCache, @NotNull ILocation location)
	{
		if (!endPointsCache.contains(location)) {
			endPointsCache.add(location);
			location.applyDelta(deltaX, deltaY);
		}
	}

	protected void moveGfxObject(@NotNull IGfxObject gfxObj, int offsetx, int offsety, Set<ILocation> offsetSet)
	{
		ILocation loc = gfxObj.getLocation();
		if (loc != null && !offsetSet.contains(loc)) {
			gfxObj.move(offsetx, offsety);
		}
	}

	protected void moveAttachedPinlists(Set<ILocation> offsetSet, int offsetx, int offsety, @NotNull IPinList gfxObj)
	{
		for (IPinList attachedPL : gfxObj.getAttachedPinListObjects()) {
			if (attachedPL.getConnectivity() instanceof IDeviceConnector) {
				moveGfxObject(attachedPL, offsetx, offsety, offsetSet);
			}
		}
	}

	protected int getOffsetForView(@NotNull IGfxView gfxView)
	{
		return NotaRepeatedCopy || mouseMoved ? 0 : super.getOffsetForView(gfxView);
	}

	@Nullable protected IProject getSourceProject()
	{
		IDesignContainer sourceDesign = getSourceDesign();
		return sourceDesign != null ? sourceDesign.getProject() : null;
	}

	protected boolean needsDictionaryEntries()
	{
		return false;
	}

	public void keyPressed(@NotNull KeyEvent ke)
	{
		if (ke.getKeyCode() == KeyEvent.VK_ESCAPE && NotaRepeatedCopy) {
			cleanUpTransients();
			persistCopyGraphics = false;
			NotaRepeatedCopy = false;
			if (m_sourcePoint != null) {
				m_sourcePoint.setLocation(m_PrevPoint.x, m_PrevPoint.y);
			}
			else if (m_PrevPoint != null) {
				m_sourcePoint = new Point(m_PrevPoint.x, m_PrevPoint.y);
			}
			GfxView gfxView = (GfxView) (CAFUtils.getInstance().getActiveCapletView());
			gfxView.invalidate(IViewInvalidationEnum.eFull);
		}
	}

	public Point getSourcePoint()
	{
		return m_sourcePoint;
	}
}
