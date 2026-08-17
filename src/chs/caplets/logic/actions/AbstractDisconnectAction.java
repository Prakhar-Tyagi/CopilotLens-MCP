package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SchemPinListHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDisconnectAction extends ControllerActionRT implements ICtxMenuProvider
{

	AbstractDisconnectAction(ICapletController controller)
	{
		super(controller);
	}

	@Nullable protected SelectSet getExtendedSelections(@Nullable SelectSet currentSelection)
	{
		if (currentSelection == null) {
			return null;
		}
		SelectSet extSelections = new SelectSet();
		SelectionIterator iter = currentSelection.getSelected();
		while (iter.hasNext()) {
			IUIDObject object = iter.getNext().getObject();
			IPinList pinList = CommonUtils.cast(object, IPinList.class);
			if (pinList != null) {
				Set<IPinList> visited = new HashSet<>();
				ConnectorHelper.collectModularConnectorSchematicPinLists(pinList, visited);
				for (IPinList meber : visited) {
					extSelections.add(meber, false);
				}
			}
			else {
				extSelections.add(object, false);
			}
		}
		return extSelections;
	}

	/**
	 * Gets the Enabled attribute of the Delete Action
	 *
	 * @return The Enabled value
	 */
	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable() || !super.isEnabled()) {// eg. read-only model
			return false;
		}
		// enable this thing only if there are items selected
		for (IDiagramObject diagramObject : LogicMultiUserSelectionFilter.getValidDiagramObjectOperands(
				getExtendedSelections(getController().getSelectMgr().getPreSelections()))) {

			if (diagramObject instanceof IPin) {
				// is the schem pin connected to anything that could be disconnected?
				if (hasConnectionsToDisconnect((IPin) diagramObject)) {
					return true;
				}
			}
			else if (diagramObject instanceof ISchemStackPin) {
				// is the schem pin connected to anything that could be disconnected?
				if (hasConnectionsToDisconnect((ISchemStackPin) diagramObject)) {
					return true;
				}
			}
			else if (diagramObject instanceof IPinList) {
				//disable disconnect from option from splice daisy chain
				ISplice splice = CommonUtils.cast(((IPinList) diagramObject).getConnectivity(), ISplice.class);
				if((splice != null) && ConnectionHelper.isSpliceConnectedToNetAndMCShield(splice)) {
					return false;
				}
				// is any schem pin connected to anything that could be disconnected?
				IPinList pinList = (IPinList) diagramObject;
				for (IAbstractSchemPin schemPin : pinList.getAllPins()) {
					if (schemPin instanceof IPin) {
						if (hasConnectionsToDisconnect((IPin) schemPin)) {
							return true;
						}
					}
					else if (schemPin instanceof ISchemStackPin) {
						if (hasConnectionsToDisconnect((ISchemStackPin) schemPin)) {
							return true;
						}
					}
				}
				// is the pinlist attached to anything that could be unattached?
				if (hasAttachedObjectsToRemove(pinList)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Does a pinlist have any attached objects that can be removed by this action?
	 *
	 * @param pinList The schematic pinlist to check
	 *
	 * @return the result of the check
	 */
	private static boolean hasAttachedObjectsToRemove(IPinList pinList)
	{
		return !getRemovableAttachedObjects(pinList).isEmpty();
	}

	private static Set<IPinList> getRemovableAttachedObjects(IPinList pinList)
	{
		Set<IPinList> candidates = new HashSet<>();
		for (IPinList attachedPL : pinList.getAttachedPinListObjects()) {
			if (ConnectionHelper.canRemoveAttachedObject(pinList, attachedPL)) {
				candidates.add(attachedPL);
			}
		}
		return Collections.unmodifiableSet(candidates);
	}

	private boolean hasConnectionsToDisconnect(IPin pin)
	{
		boolean isInlinePin = SchemPinListHelper.isInlinePin(pin);
		IJoint joint = pin.getJoint();
		if (joint != null) {
			for (IDiagramObject diagObj : joint.getAssociations()) {
				if ((!isInlinePin && diagObj instanceof IPin) || diagObj instanceof ISegment) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasConnectionsToDisconnect(ISchemStackPin pin)
	{
		IJoint joint = pin.getJoint();
		if (joint != null) {
			for (IDiagramObject diagObj : joint.getAssociations()) {
				if (diagObj instanceof ISchemStackPin || diagObj instanceof IHighwaySegment) {
					return true;
				}
			}
		}
		return false;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean bEditOk = true;

		// Delete all of the selected objects
		if (successful) {
			bEditOk = editModel();
		}

		return bEditOk;
	}

	// Do the model edit
	private boolean editModel()
	{
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		SetMap<IUID, IUID> lockMap = new SetMap<>();
		Set<ILogicObject> lockCandidates = new HashSet<>();
		// loop through select objects and delete them
		ILogicDesign design = null;
		SelectSet extendedSelections = getExtendedSelections(getController().getSelectMgr().getPreSelections());
		for (IDiagramObject obj : LogicMultiUserSelectionFilter.getValidDiagramObjectOperands(extendedSelections)) {
			Set<ILogicObject> logicObjects = ConnectionHelper.collectLocksForDisconnect(obj);
			Set<IUID> iuidSet = lockMap.getSet(obj.getUID());
			for (ILogicObject logicObject : logicObjects) {
				iuidSet.add(logicObject.getUID());
				lockCandidates.add(logicObject);
				if (design == null) {
					design = logicObject.getLogicDesign();
				}
			}
		}
		if (design != null) {
			LogicObjectLockFinder.tryEdit(design, lockCandidates);
		}

		Set<IPinList> allPinListsDisconnected = new HashSet<>();
		for (Map.Entry<IUID, Set<IUID>> entry : lockMap.entrySet()) {
			//skip deleted objects after refresh.
			IUIDObject object = UIDMgr.getNonDeletedObject(entry.getKey());
			if (object != null) {
				boolean isReadyToDisconnect = true;
				for (IUID iuid : entry.getValue()) {
					ILogicObject logicObject = UIDMgr.getObjectOfType(iuid, ILogicObject.class);
					if (logicObject == null || !logicObject.isEditable()) {
						isReadyToDisconnect = false;
					}
				}
				if (isReadyToDisconnect) {
					ConnectionHelper.disconnect(object);
					if (object instanceof IPinList) {
						allPinListsDisconnected.add((IPinList) object);
					}
				}
			}
		}

		if (extendedSelections != null) {
			Map<IPinList, IPinList> candidatesForDetach = new HashMap<>();
			for (IPinList pinList : allPinListsDisconnected) {
				IPinList parentSchemPinList = ConnectorHelper.getParentSchemPinList(pinList);
				if (parentSchemPinList != null && !extendedSelections.contains(parentSchemPinList.getUID())) {
					candidatesForDetach.put(pinList, parentSchemPinList);
				}
			}
			for (Map.Entry<IPinList, IPinList> entry : candidatesForDetach.entrySet()) {
				IPinList anchor = entry.getKey();
				IPinList parent = entry.getValue();
				parent.removeAttachedObject(anchor);
				anchor.removeAttachedObject(parent);
				ISchemDiagram diagram = DiagramHelper.getDiagram(anchor);
				assert diagram != null;
				ConnectorHelper.ensureModularSchematics(anchor, diagram);
			}
		}

		return true;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && isEnabled()) {
			container.add(new ActionEntry(actionUI));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
