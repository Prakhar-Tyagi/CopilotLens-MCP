/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.helpers.SharedObjectUpdateHelper;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedGroundDevice;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.IWriteableDSUM;
import chs.cof.logical.shared.RefreshHelper;
import chs.cof.project.IProject;
import chs.cog.IPrivilegedCOGPersistent;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialogCreator;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.helpers.revisioning.SharedObjectSwapOutHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwapOutSharedObjectRevisionAction extends ControllerActionRT implements ICtxMenuProvider
{

	private final ILogicDesign m_design;

	public SwapOutSharedObjectRevisionAction(ICapletController controller)
	{
		super(controller);
		m_design = getDesign(controller);
		setUndoableAction(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		Set<IRevisionedSharedObject> sharedObjects = getSharedObject();
		SharedObjectUpdateHelper.updateCorrespondingSharedObjectManager(sharedObjects, m_design.getProject());

		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		// start an edit - if an error occurs we will abort this edit, we will expect transactional code underneath
		// to rollback.
		getController().getUndoableContainer().endEdit();
		getController().getUndoableContainer().clear();

		Set<IRevisionedSharedObject> sharedObjects = getSharedObject();

		Map<Set<IRevisionedSharedObject>, String> listOfCompatibleRevisions;
		if (sharedObjects.size() == 1) {
			listOfCompatibleRevisions =
					SharedObjectSwapOutHelper.getCompatibleRevision(sharedObjects.iterator().next(), m_design);
		}
		// else it is an inline
		else {
			Iterator<IRevisionedSharedObject> iterator = sharedObjects.iterator();
			IRevisionedSharedObject revObject1 = iterator.next();
			IRevisionedSharedObject revObject2 = iterator.next();
			listOfCompatibleRevisions =
					SharedObjectSwapOutHelper.getCompatibleInlineRevisions((ISharedConnector) revObject1,
							(ISharedConnector) revObject2, m_design);
		}

		// if there are no compatible revisions then just let know and return
		if (listOfCompatibleRevisions.isEmpty()) {
			Frame parentWindow = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			String heading = ResourceMgr.getString(SwapOutSharedObjectRevisionAction.class,
					"SwapOutSharedObjectRevisionAction.NoCompatibleRevisions.Heading");
			String message = ResourceMgr.getString(SwapOutSharedObjectRevisionAction.class,
					"SwapOutSharedObjectRevisionAction.NoCompatibleRevisions.Text");
			MessageHelper.showErrorMessage(parentWindow, heading, message);
			return false;
		}

		Set<ISchemDiagram> usageDiagrams = new HashSet<>();
		for(ISharedObject sharedObject : sharedObjects){
			usageDiagrams.addAll(m_design.getSharedUsageMgr().getUsageDiagrams(sharedObject));
		}

		// display the compatible revisions in a dialgue and allow the user to choose one to swap with
		IProject project = sharedObjects.iterator().next().getProject();
		Frame window = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
		boolean dataChanged = swapOutRevision(project, window, sharedObjects, listOfCompatibleRevisions);

		if (dataChanged) {
			regenerateUsages(usageDiagrams);
		}

		// refresh
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}

		List<IUID> list = Collections.emptyList();
		ModelChangeEvent poste = new ModelChangeEvent(getController().getCapletModel(), list);
		getController().getCapletModel().notifyModelChange(poste);

		return true;
	}

	private void regenerateUsages(Set<ISchemDiagram> usageDiagrams)
	{
		//Loading old usage diagrams to be able to regenerate usages for whole design
		RefreshHelper.ensureDiagramsAreRefreshedAndFullyLoaded(usageDiagrams);
		// Swapping shared object revisions does not impact schem objects, as such they are not modified, hence
		// they will not recieve notification in the DSUM, hence To compensate we just regenerate the usages.
		// we must regenerate usages for the whole design here (dts0100568580)
		((IWriteableDSUM) m_design.getSharedUsageMgr()).regenerateUsages();

		// Save the DesignSharedUsageMgr
		// FEAT00015777 - TransientUsages
		//m_design.getSharedUsageMgr().save();
		//	m_design.getSharedUsageMgr().saveTransientUsages();

		//dts0101364171 - We need to mark diagrams for which usages will get modified to be able to save them
		usageDiagrams.forEach(usageDiagram -> {
			if (!usageDiagram.isModified() && usageDiagram instanceof IPrivilegedCOGPersistent) {
				((IPrivilegedCOGPersistent) usageDiagram).modified();
			}
		});
	}

	protected boolean swapOutRevision(IProject project, Frame parentWindow, Set<IRevisionedSharedObject> sharedObjects,
			Map<Set<IRevisionedSharedObject>, String> listOfCompatibleRevisions)
	{
		SwapOutSharedObjectRevisionDialogCreator dialog =
				new SwapOutSharedObjectRevisionDialogCreator(project,
						m_design, sharedObjects, listOfCompatibleRevisions, parentWindow, false);

		boolean dataChanged = dialog.lockAndShow();

		//if there was an error, the code underneath will of rolled back, and we can abort this edit.
		if (dialog.shouldCallCAFUndo()) {
			getController().getUndoableContainer().cancelEdit();
		}
		return dataChanged;
	}

	@Nullable
	private static ILogicDesign getDesign(ICapletController controller)
	{
		ICapletModel model = controller.getCapletModel();
		if (model instanceof ILogicModel) {
			return ((ILogicModel) model).getDesign();
		}

		return null;
	}

	public String getActionUIClass()
	{
		return SwapOutSharedObjectRevisionActionUI.class.getName();
	}

	@Override
	protected boolean checkCache()
	{
		return false;
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			return false;
		}
		//
		// If we are in a transaction boundary, we MUST wait
		//
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		if (btm.isWithinBoundary()) {
			return false;
		}
		Set<IRevisionedSharedObject> operands = getSharedObject();

		// check it is not one of the excluded types (i.e. a inner conductor or multicore)
		if (operands.size() == 1) {
			IRevisionedSharedObject operand = operands.iterator().next();

			if (operand instanceof ISharedConductor && ((ISharedConductor) operand).getMulticore() != null) {
				// it is an instnace of an inner core of a multicore
				return false;
			}
			if (operand instanceof ISharedMulticore sharedMulticore) {
				if (sharedMulticore.getParent() != null) {
					// it is an instance of an inner core of a multicore
					return false;
				}
				if (SingleLineHelper.isMulticorePartOfAnySingleLine(sharedMulticore)) {
					//it is Single Line's multicore
					return false;
				}
			}
			if (operand instanceof ISharedConnector &&
					((ISharedConnector) operand).getOccupiedPosition() != null) {
				return false;
			}
			if (operand instanceof ISharedGroundDevice) {
				return false;
			}
		}

		return !operands.isEmpty() && getController().getCapletModel().isEditable() && super.isEnabled();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Set<IRevisionedSharedObject> sharedObject = getSharedObject();
		if (!sharedObject.isEmpty()) {
			SwapOutSharedObjectRevisionActionUI action = ((SwapOutSharedObjectRevisionActionUI) getActionUI());
			action.updateUI();
			container.add(new ActionEntry(action));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	protected Set<IRevisionedSharedObject> getSharedObject()
	{
		ICapletController controller = getController();
		SelectSet selections = controller.getSelectMgr().getPreSelections();

		// When a user select a conductor then the selection also includes a list of segments.  We do not care about
		// these segments, so we need a list of selections without the segments
		Set<IUIDObject> setOfSelectedObjsWithoutSegments = new HashSet<IUIDObject>();
		for (SelectedUIDObjectIterator selectedObjectIter = selections.getSelectedUIDObjects();
				selectedObjectIter.hasNext(); ) {
			IUIDObject uidObj = selectedObjectIter.getNext();
			if (!(uidObj instanceof ISegment) && isAcceptableObjectType(uidObj)) {
				setOfSelectedObjsWithoutSegments.add(uidObj);
			}
		}

		Set<IRevisionedSharedObject> sharedObjects = new HashSet<IRevisionedSharedObject>();

		// Is it a Single object (i.e. not a inline or a multi select)
		if (setOfSelectedObjsWithoutSegments.size() == 1) {
			IUIDObject uidObj = setOfSelectedObjsWithoutSegments.iterator().next();

			//Is it a net or wire?
			if (uidObj instanceof chs.cof.logical.schem.IConductor) {
				chs.cof.logical.schem.IConductor cond = (chs.cof.logical.schem.IConductor) uidObj;
				IConductor connectivity = cond.getConnectivity();

				if (connectivity.getSharedConductor() != null) {
					sharedObjects.add(connectivity.getSharedConductor());
				}
			}

			// is it a pin list?
			if (uidObj instanceof IPinList) {
				IPinList pinlist = (IPinList) uidObj;

				chs.cof.logical.cable.IPinList connectivity = pinlist.getConnectivity();

				if (connectivity != null && connectivity.getSharedPinList() != null) {
					ISharedPinList sharedPinList = connectivity.getSharedPinList();

					if (sharedPinList instanceof ISharedConnector &&
							!((ISharedConnector) sharedPinList).getMates().isEmpty()) {
						// it half an inline. so we are not interested in it
					}
					else if (sharedPinList instanceof ISharedDeviceConnector) {
						// it is a device connector so we are not interested in it
					}
					else {
						sharedObjects.add(sharedPinList);
					}
				}
			}

			// is it a multicore?
			else if (uidObj instanceof IMulticore && !(uidObj instanceof IOverbraid)) {
				IMulticore mc = (IMulticore) uidObj;
				ISharedMulticore sharedMulticore = mc.getSharedMulticore();
				if (sharedMulticore != null && sharedMulticore.getParent() == null) {
					sharedObjects.add(sharedMulticore);
				}
			}
		}
		else if (setOfSelectedObjsWithoutSegments.size() == 2) {
			Iterator<IUIDObject> iterator = setOfSelectedObjsWithoutSegments.iterator();

			// Fix for defect dts0100918649.
			IUIDObject uidObj1 = null;
			IUIDObject uidObj2 = null;

			if (iterator.hasNext()) {
				uidObj1 = iterator.next();
			}

			if (iterator.hasNext()) {
				uidObj2 = iterator.next();
			}

			// is it a inline
			if (uidObj1 instanceof IPinList && uidObj2 instanceof IPinList) {
				// check they are mated
				IPinList cafPintList1 = (IPinList) uidObj1;
				chs.cof.logical.cable.IPinList connectivity1 = cafPintList1.getConnectivity();
				IPinList cafPintList2 = (IPinList) uidObj2;
				chs.cof.logical.cable.IPinList connectivity2 = cafPintList2.getConnectivity();
				//  connectivity1.getSharedPinList() is check for null and is invariant between calls
				//noinspection ConstantConditions
				if (connectivity1 instanceof IConnector && connectivity2 instanceof IConnector &&
						connectivity1.getSharedPinList() != null && connectivity2.getSharedPinList() != null) {
					ISharedPinList sharedPL1 = connectivity1.getSharedPinList();
					if (sharedPL1 instanceof ISharedConnector) { // skip shared device connectors
						ISharedConnector sharedConnector1 = (ISharedConnector) sharedPL1;
						Set<ISharedConnector> mates = sharedConnector1.getMates();
						if (!mates.isEmpty()) {
							ISharedConnector sharedConnector2 = mates.iterator().next();
							if (mates.contains(sharedConnector2)) {
								// we have an inline
								sharedObjects.add((IRevisionedSharedObject) sharedPL1);
								sharedObjects.add((IRevisionedSharedObject) sharedConnector2);
							}
						}
					}
				}
			}
		}

		return sharedObjects;
	}

	private boolean isAcceptableObjectType(@Nullable IUIDObject uidObj)
	{
		//separate handling of Multicore (as Multicore doesn't have diagram representation)
		return uidObj instanceof IDiagramObject || uidObj instanceof IMulticore;
	}
}
