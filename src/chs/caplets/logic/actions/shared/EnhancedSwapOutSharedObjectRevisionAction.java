/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2009-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IUpdateableAction;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.helpers.SharedObjectUpdateHelper;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedGroundDevice;
import chs.cof.logical.shared.ISharedHighway;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.ui.form.sharedobjectrevisioning.EnhancedSwapOutSharedObjectRevisionDialog;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialog;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialogCreator;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.SharedInlineHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.revisioning.SharedObjectSwapOutHelper;
import chs.utility.persist.LockableHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FEAT00013725 - Automated handling of shared object revisions
 * <p>
 * This is an Enhanced Action for Swapping Out the SharedObject Revision. This action will take care of swapping the
 * data across the project and is not limited to the opened design.
 * <p>
 *
 * @author ntewari
 */
public class EnhancedSwapOutSharedObjectRevisionAction extends ControllerActionRT
{

	private ISpecialSelectMgr m_sharedSelectMgr;
	private IRevisionedSharedObject m_revObject;

	public EnhancedSwapOutSharedObjectRevisionAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null) {
			m_sharedSelectMgr.contextMenuAddAction(new ActionEntry(getActionUI(),
					(String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
			{
				public boolean shouldDisplay()
				{
					boolean shouldDisplay =
							getOperand() != null && (ActionRT.isDesignUnderConcurrentEdit() || isEnabled());
					if (!shouldDisplay) {
						Action ui = getActionUI();
						if (ui == null) {
							return false;
						}
						((IUpdateableAction) ui).updateUI();
					}
					return shouldDisplay && super.shouldDisplay();
				}
			});
		}
		setUndoableAction(false);
	}

	@Override protected boolean checkCache()
	{
		return false;
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		IRevisionedObject operand = getOperand();
		if (operand == null) {
			return false;
		}
		
		if (operand instanceof ISharedMessageSignal) {
			return false;
		}
		if (operand instanceof ISharedConductor && ((ISharedConductor) operand).getMulticore() != null) {
			// it is an instnace of an inner core of an overbraid or multicore
			return false;
		}
		if (operand instanceof ISharedMulticore sharedMulticore) {
			if (sharedMulticore.getParent() != null) {
				// it is an instance of an inner core of an overbraid or multicore
				return false;
			}
			if (SingleLineHelper.isMulticorePartOfAnySingleLine(sharedMulticore)) {
				//it is Single Line's multicore
				return false;
			}
		}
		if (operand instanceof ISharedOverbraid) {
			// we do not allow the revision of overbraids.
			return false;
		}
		if (operand instanceof ISharedConnector && ((ISharedConnector) operand).getOccupiedPosition() != null) {
			return false;
		}
		if (operand instanceof ISharedGroundDevice) {
			return false;
		}
		if (operand instanceof ISharedSingleLine) {
			return true;
		}
		if (operand instanceof ISharedHighway) {
			return false;
		}

		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	@Nullable
	private IRevisionedSharedObject getOperand()
	{
		if (m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_sharedSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof IRevisionedSharedObject) {
				return (IRevisionedSharedObject) uidObj;
			}
		}
		return null;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {

			ConfirmSaveDialog confirm = new ConfirmSaveDialog(
					EnhancedSwapOutSharedObjectRevisionAction.class.getName(),
					ResourceMgr.getString(EnhancedSwapOutSharedObjectRevisionAction.class,
							"EnhancedSwapOutSharedObjectRevisionAction.confirm.title"),
					ResourceMgr.getString(EnhancedSwapOutSharedObjectRevisionAction.class,
							"EnhancedSwapOutSharedObjectRevisionAction.confirm.msg"));

			if (!confirm.userCanceled()) {
				Map<Set<IRevisionedSharedObject>, String> compatibleRevisions;
				Set<IRevisionedSharedObject> revObjects = new LinkedHashSet<IRevisionedSharedObject>();
				if ((m_revObject instanceof ISharedConnector &&
						((ISharedConnector) m_revObject).getMates().size() == 1) &&
						(
								((ISharedPinList) m_revObject).getType().equals(PinListTypeEnum.TypeInlineJack) ||
										((ISharedPinList) m_revObject).getType().equals(PinListTypeEnum.TypeInlinePlug)
						)) {
					// dts0100623799 Feat13725 - SharedObjectHandling- Shared Inline - Swap out re-visioning ui
					// is displaying plug in last.
					// Add the Plug first in the list and then add the recepticle
					if (((ISharedPinList) m_revObject).getType().equals(PinListTypeEnum.TypeInlineJack)) {
						revObjects.addAll(((ISharedConnector) m_revObject).getMates());
						revObjects.add(m_revObject);
					}
					else {
						revObjects.add(m_revObject);
						revObjects.addAll(((ISharedConnector) m_revObject).getMates());
					}

					if (SharedInlineHelper.getHelper().isSharedInlineInvalidForSwapOut(revObjects)) {
						return false;
					}

					Iterator<IRevisionedSharedObject> iterator = revObjects.iterator();
					IRevisionedSharedObject revObject1 = iterator.next();
					IRevisionedSharedObject revObject2 = iterator.next();
					compatibleRevisions =
							SharedObjectSwapOutHelper.getCompatibleInlineRevisions((ISharedConnector) revObject1,
									(ISharedConnector) revObject2, null);
				}
				else {
					revObjects.add(m_revObject);
					compatibleRevisions =
							SharedObjectSwapOutHelper.getCompatibleRevision(revObjects.iterator().next(), null);
				}

				Frame window = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
				// if there are no compatible revisions then just let know and return
				if (compatibleRevisions.isEmpty()) {
					String heading = ResourceMgr.getString(SwapOutSharedObjectRevisionAction.class,
							"SwapOutSharedObjectRevisionAction.NoCompatibleRevisions.Heading");
					String message = ResourceMgr.getString(SwapOutSharedObjectRevisionAction.class,
							"SwapOutSharedObjectRevisionAction.NoCompatibleRevisions.Text");
					MessageHelper.showErrorMessage(window, heading, message);
					return false;
				}

				// display the compatible revisions in a dialgue and allow the user to choose one to swap with
				IProject project = revObjects.iterator().next().getProject();
				SwapOutSharedObjectRevisionDialogCreator dialog = new SwapOutSharedObjectRevisionDialogCreator(project,
						null, revObjects, compatibleRevisions, window, true);

				// get the list of all the opened diagrams. These designs should not be unlocked after the swap action
				Set<IDesign> openedDesigns = new HashSet<IDesign>();
				for (IBaseDiagram diagram : CAFUtils.getInstance().getOpenDiagrams()) {
					if (diagram.getDesignContainer() instanceof IDesign) {
						openedDesigns.add((IDesign) diagram.getDesignContainer());
					}
				}
				SwapOutSharedObjectRevisionDialog gui = dialog.getGUI();
				if (gui instanceof EnhancedSwapOutSharedObjectRevisionDialog) {
					((EnhancedSwapOutSharedObjectRevisionDialog) gui).getDetailsCreator()
							.setFilterWhileLocking(openedDesigns);
					boolean bLifeCycleModified =
							CAFUtils.getInstance().getActiveCapletController().getCaplet().getLifecycle()
									.isModified(project);
					((EnhancedSwapOutSharedObjectRevisionDialog) gui).getDetailsCreator()
							.setLifeCycleModified(bLifeCycleModified);
				}

				dialog.lockAndShow();
				// get the list of all the modified designs
				if (gui instanceof EnhancedSwapOutSharedObjectRevisionDialog) {

					// get the list of modified designs
					Set<ILogicDesign> modifiedDesigns =
							((EnhancedSwapOutSharedObjectRevisionDialog) gui).getDetailsCreator().getModifiedDesigns();

					// if any existing designs are modified
					if (modifiedDesigns != null && !modifiedDesigns.isEmpty()) {
						// refresh the views for all the modified designs
						CAFCommandHelper cmdHelper = new CAFCommandHelper();
						for (ILogicDesign design : modifiedDesigns) {
							// Schematic objects have probably changed hence refresh views for any opened designs
							if (openedDesigns.contains(design)) {
								//dts0100810864 Undo queue should be cleared.
								cmdHelper.clearDesignUndoableContainer(design);
								cmdHelper.refreshViews(design);
							}
						}
					}

					// get the set of newly created designs
					Set<IDesign> createdDesigns =
							((EnhancedSwapOutSharedObjectRevisionDialog) gui).getDetailsCreator().getCreatedDesigns();

					// if any new designs are created,we need to update the project tree
					if (createdDesigns != null && !createdDesigns.isEmpty()) {
						// Make the node for the target revision appear in the ProjectWindow tree widget.
						CAFUtils.getInstance().getFIB().getProjectMgr().projectChanged(project);
					}
				}

				List<IUID> list = Collections.emptyList();
				ModelChangeEvent poste = new ModelChangeEvent(getController().getCapletModel(), list);
				// dts0100810864 Below call and some other previous calls like refreshViews sets the modiied flag to true
				// But swapout revision saves all the opened designs also. So should we not set modified flag to false ?
				getController().getCapletModel().notifyModelChange(poste);
			}
		}
		return true;
	}

	@NotNull public String getActionUIClass()
	{
		return EnhancedSwapOutSharedObjectRevisionActionUI.class.getName();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_revObject = getOperand();
		if (m_revObject == null) {
			return IActionEnum.eCanceled;
		}
		Set<IRevisionedSharedObject> sharedObjects = new HashSet<IRevisionedSharedObject>();
		sharedObjects.add(m_revObject);
		SharedObjectUpdateHelper.updateCorrespondingSharedObjectManager(sharedObjects, m_revObject.getProject());

		if (LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), m_revObject)) {
			return IActionEnum.eCompleted;
		}
		else {
			LogicActionMessageHelper.warnRevisionedSharedObjectDeleted(m_revObject);
			return IActionEnum.eCanceled;
		}
	}
}
