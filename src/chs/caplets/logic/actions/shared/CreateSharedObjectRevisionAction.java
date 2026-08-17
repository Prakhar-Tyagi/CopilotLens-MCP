/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IUpdateableAction;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedGroundDevice;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.ctf.ui.form.sharedobjectrevisioning.ISharedObjectRevisioningDialogCreatorClient;
import chs.ctf.ui.form.sharedobjectrevisioning.SharedObjectRevisioningDialogCreator;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectDetailsCreator;
import chs.ctf.ui.form.sharedobjectrevisioning.SwapOutSharedObjectRevisionDialog;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.Environment;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.SharedInlineHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateSharedObjectRevisionAction extends ControllerActionRT implements
		ISharedObjectRevisioningDialogCreatorClient
{

	private ISpecialSelectMgr m_sharedSelectMgr;
	protected IRevisionedSharedObject m_revObject;

	public CreateSharedObjectRevisionAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
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
						if (getActionUI() != null) {
							((IUpdateableAction) getActionUI()).updateUI();
						}
					}
					return shouldDisplay && super.shouldDisplay();
				}
			});
		}
		setUndoableAction(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_revObject = getOperand();
		if (m_revObject == null) {
			return IActionEnum.eCanceled;
		}

		final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
		if (!new SharedObjectAvailabilityChecker().check(m_revObject, null, reporter)) {
			return IActionEnum.eCanceled;
		}

		//(12.1)dts0100842008 FEAT15866 - Performance - Create shared object Revision Dialog to come-up:
//		Set<IRevisionedSharedObject> sharedObjects = new HashSet<IRevisionedSharedObject>();
//		sharedObjects.add(m_revObject);
//		SharedObjectUpdateHelper.updateCorrespondingSharedObjectManager(sharedObjects, m_revObject.getProject());
//
//		if (LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), m_revObject)) {
//			return IActionEnum.eCompleted;
//		}
//		else {
//			LogicActionMessageHelper.warnRevisionedSharedObjectDeleted(m_revObject);
//			return IActionEnum.eCanceled;
//		}
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {

			Set<IRevisionedSharedObject> revObjects = new ListSet<IRevisionedSharedObject>();
			SharedObjectRevisioningDialogCreator dialogCreator;
			final IProject project = m_revObject.getProject();
			if ((m_revObject instanceof ISharedConnector && ((ISharedConnector) m_revObject).getMates().size() == 1) &&
					(
							((ISharedPinList) m_revObject).getType().equals(PinListTypeEnum.TypeInlineJack) ||
									((ISharedPinList) m_revObject).getType().equals(PinListTypeEnum.TypeInlinePlug)
					)) {

				revObjects.add(m_revObject);
				revObjects.addAll(((ISharedConnector) m_revObject).getMates());
				if (SharedInlineHelper.getHelper().isSharedInlineInvalidForRevise(revObjects)) {
					return false;
				}
				dialogCreator = new SharedObjectRevisioningDialogCreator(this, revObjects, project, true);
			}
			else {
				revObjects.add(m_revObject);
				dialogCreator = createDialogCreator(project);
			}

			Frame window = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			boolean dataChanged = dialogCreator.lockAndShow(window);
			if (dataChanged) {
				Model model = (Model) getController().getCapletModel();
				IDesign des = model.getDesign();
				final IProject designProject = des.getProject();
				assert designProject != null;
				SharedObjectMgr.fireChangeEventForManagers(designProject.getSharedConductorMgr(), designProject.getSharedPinListMgr());
			}
			return dataChanged;
		}
		return true;
	}

	@NotNull protected SharedObjectRevisioningDialogCreator createDialogCreator(@NotNull IProject project)
	{
		return new SharedObjectRevisioningDialogCreator(this, Collections.singleton(m_revObject), project, true);
	}

	public String getActionUIClass()
	{
		return CreateSharedObjectRevisionActionUI.class.getName();
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return false;
	}

	@SuppressWarnings({"IfStatementWithIdenticalBranches"})
	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		
		IRevisionedSharedObject operand = getOperand();
		if (operand == null) {
			return false;
		}

		if(operand instanceof ISharedMessageSignal){
			return false;
		}
		if (operand instanceof ISharedConductor && ((ISharedConductor) operand).getMulticore() != null) {
			// it is an instance of an inner core of an overbraid or multicore
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
		if (operand instanceof ISharedGeneralHighway) {
			// action not applicable for a highway
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

	/**
	 * swaps the sourece shared object revison with the
	 *
	 * @param source source shared object/s for swap out
	 * @param target target shared object/s for swap out - i.e new revisions of source
	 *
	 * @return true if swap out process is completed else return false
	 */
	private boolean swapOut(@NotNull Collection<IRevisionedSharedObject> source,
			@NotNull Collection<IRevisionedSharedObject> target)
	{
		assert (source.size() == target.size());

		boolean didComplete = false;
		if (didUserConfirmSwapOut()) {
			Map<Set<IRevisionedSharedObject>, String> compatibleRevisions =
					new HashMap<Set<IRevisionedSharedObject>, String>();
			final Set<IRevisionedSharedObject> targetSet = CollectionUtils.createSet(target);
			compatibleRevisions.put(targetSet, "");
			Frame parentWindow = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			SwapOutSharedObjectRevisionDialog parent = new SwapOutSharedObjectRevisionDialog(parentWindow,
					null, source, compatibleRevisions);

			parent.setNewRevisions(targetSet);
			IProject project = FactoryMgr.getSystemFactory().getCAFUtils().getCurrentProject();
			boolean bLifeCycleModified =
					CAFUtils.getInstance().getActiveCapletController().getCaplet().getLifecycle().isModified(project);
			SwapOutSharedObjectDetailsCreator detailsCreator = createSwapOutSharedObjectDetailsCreator(project, parent,
					bLifeCycleModified);

			// get the list of all the opened diagrams. These designs should not be unlocked after the swap action
			Set<IDesign> openedDesigns = new HashSet<IDesign>();
			for (IBaseDiagram diagram : CAFUtils.getInstance().getOpenDiagrams()) {
				if (diagram.getDesignContainer() instanceof IDesign) {
					openedDesigns.add((IDesign) diagram.getDesignContainer());
				}
			}
			// set the filter in the details creator before the real action
			detailsCreator.setFilterWhileLocking(openedDesigns);
			// invoke the process swap out
			detailsCreator.createAndShow(false);
			try {
				// dts0100799060 - If Dialog is not cancelled save newly created revision and process swap out on it.
				if (didUserConfirmSwapOutDetails(parent)) {
					SharedObjectRevisionHelper.save(target, project);
					if (detailsCreator.processSwapOut()) {
						didComplete = true;
					}
					else {
						return false;
					}
				}
				else {
					return false;
				}
			}
			catch (UserSessionException ue) {
				showException(ue);
				return false;
			}
			// get the list of modified designs
			Set<ILogicDesign> modifiedDesigns = detailsCreator.getModifiedDesigns();
			// if any existing designs are modified then we need to refresh the views for them
			if (modifiedDesigns != null && !modifiedDesigns.isEmpty()) {
				// refresh the views for all the modified designs
				CAFCommandHelper cmdHelper = new CAFCommandHelper();
				for (ILogicDesign design : modifiedDesigns) {
					// Schematic objects have probably changed hence refresh views for any opened designs
					if (openedDesigns.contains(design)) {
						cmdHelper.clearDesignUndoableContainer(design);
						cmdHelper.refreshViews(design);
					}
				}
			}

			// get the set of newly created designs
			Set<IDesign> createdDesigns = detailsCreator.getCreatedDesigns();
			// if any new designs are created,we need to update the project tree
			if (createdDesigns != null && !createdDesigns.isEmpty()) {
				// Make the node for the target revision appear in the ProjectWindow tree widget.
				CAFUtils.getInstance().getFIB().getProjectMgr().projectChanged(
						FactoryMgr.getSystemFactory().getCAFUtils().getCurrentProject());
			}
		}
		return didComplete;
	}

	@NotNull
	protected SwapOutSharedObjectDetailsCreator createSwapOutSharedObjectDetailsCreator(@NotNull IProject project,
			@NotNull SwapOutSharedObjectRevisionDialog parent, boolean bLifeCycleModified)
	{
		return new SwapOutSharedObjectDetailsCreator(project, parent, bLifeCycleModified);
	}

	protected boolean didUserConfirmSwapOutDetails(@Nullable SwapOutSharedObjectRevisionDialog parent)
	{
		return parent != null && !parent.isCancelled();
	}

	protected boolean didUserConfirmSwapOut()
	{
		ConfirmSaveDialog confirm = new ConfirmSaveDialog(
				EnhancedSwapOutSharedObjectRevisionAction.class.getName(),
				ResourceMgr.getString(EnhancedSwapOutSharedObjectRevisionAction.class,
						"EnhancedSwapOutSharedObjectRevisionAction.confirm.title"),
				ResourceMgr.getString(EnhancedSwapOutSharedObjectRevisionAction.class,
						"EnhancedSwapOutSharedObjectRevisionAction.confirm.msg"));

		return !confirm.userCanceled();
	}

	private void showException(Exception ex)
	{
		Environment.getExceptionDisplay().displayException(ex, ex.getMessage());
	}

	@Override public boolean doPostUiCreateRevision(@NotNull Collection<IRevisionedSharedObject> source,
			@NotNull Collection<IRevisionedSharedObject> target)
	{
		boolean didComplete = false;

		try {
			// dts0100799060 - If swap out on revision is not needed, we save newly created
			// revision here.
			SharedObjectRevisionHelper.save(target, m_revObject.getProject());
			didComplete = true;
		}
		catch (UserSessionException ue) {
			showException(ue);
		}
		return didComplete;
	}

	@Override public boolean doPostUiCreateRevisionAndSwapOut(@NotNull Collection<IRevisionedSharedObject> source,
			@NotNull Collection<IRevisionedSharedObject> target)
	{
		// dts0100625640
		// dts0100627638
		// NOTE : Please note that creation of shared object and swap out happens in the same controller action
		// Even though, we above code would have called the Flush for the newly created shared object, the
		// persistence will happen only after this call terminates i.e when transaction boundary exits.
		// In the swap out capability we try to refresh the shared object managers which will internally
		// try to reload the shared objects. When comaparing with the DB it finds the new shared object
		// missing and goes ahead to delete it from the UIDMgr. This will create issues for swap out.
		// Hence we should disable the refresh for the shared object Managers assuming that its already done
		// while activating this action.
		boolean didComplete = false;
		final ISharedPinListMgr sharedPinListMgr = m_revObject.getProject().getSharedPinListMgr();
		final ISharedConductorMgr sharedConductorMgr = m_revObject.getProject().getSharedConductorMgr();
		boolean refreshPinListMgr = sharedPinListMgr.isRefreshEnabled();
		boolean refreshConductorMgr = sharedConductorMgr.isRefreshEnabled();
		try {
			sharedPinListMgr.setRefreshEnabled(false);
			sharedConductorMgr.setRefreshEnabled(false);
			// swap the source revision with the newly created revision
			didComplete = swapOut(source, target);
		}
		finally {
			// revert the refresh enabled status back to the original one
			sharedPinListMgr.setRefreshEnabled(refreshPinListMgr);
			sharedConductorMgr.setRefreshEnabled(refreshConductorMgr);
		}
		return didComplete;
	}
}
