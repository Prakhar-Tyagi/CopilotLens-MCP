/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.quickedit.IQuickEditPanelNotifier;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IGuard;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.subsystem.sharedobject.finder.SharedFinderServices;
import chs.system.UIDMgr;
import chs.utilities.BuildInfo;
import chs.utilities.Environment;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.SharePinListLockHandler;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.List;

public class ShareAction extends BaseShareAction implements IQuickEditPanelNotifier
{

	private static boolean isShareOfUnplacedObjectsDisabled = true; //this is enabled only in UT mode.
	private static boolean allowShareOfPlacedPinListWithUnplacedPins = true; //dts0100925645 this is default enabled.

	public ShareAction(ICapletController controller)
	{
		super(controller);
		Model model = (Model) controller.getCapletModel();
		m_pinListHelper = new SharePinListActionHelper(
				getController().getCaplet().getFIB(),
				model, false);
		m_condGroupHelper = new ShareConductorGroupActionHelper(getController().getCaplet().getFIB(),
				((ILogicModel) controller.getCapletModel()).getDesign());
		m_conductorHelper = new ShareConductorActionHelper(((ILogicModel) controller.getCapletModel()).getDesign(),
				model.getDiagram());
		m_FunctionConductorHelper = new ShareConductorActionHelper(((ILogicModel) controller.getCapletModel()).getDesign(),
				model.getDiagram());
		m_highwayHelper = new ShareHighwayActionHelper(((ILogicModel) controller.getCapletModel()).getDesign(),
				model.getDiagram());
		m_singleLineHelper = new ShareSingleLineActionHelper(((ILogicModel) controller.getCapletModel()).getDesign(),
				model.getDiagram());
		m_functionMessageHelper =
				new ShareFunctionMessageActionHelper(((ILogicModel) controller.getCapletModel()).getDesign(),
						model.getDiagram());
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		try (IGuard ignored = SharedFinderServices.instance().registerCacheServiceProvider()) {
			UnplacedObjectShareabilityControl.pushControl(isShareOfUnplacedObjectsDisabled,
					allowShareOfPlacedPinListWithUnplacedPins);
			IActionEnum result = super.onActivate(e);
			if (result == IActionEnum.eCanceled || checkFrozen() == IActionEnum.eCanceled) {
				return IActionEnum.eCanceled;
			}
			Model model = (Model) getController().getCapletModel();
			return m_helper.setup(m_operands, CAFUtils.getInstance().getDialogTitleByAction(this), model.getDiagram());
		}
		finally {
			UnplacedObjectShareabilityControl.popControl();
		}
	}

	protected IActionEnum checkFrozen()
	{
		if (BaseShareActionHelper.isFrozenSharedObjectsRequired(m_design)) {
			boolean confirmFreeze =
					MessageHelper.showOkCancelDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
							ResourceMgr.getString(ShareConductorGroupActionHelper.class,
									"ShareAction.WillFreeze.Heading.text", m_design.getFullName()),
							ResourceMgr.getString(ShareConductorGroupActionHelper.class,
									"ShareAction.WillFreeze.Message.text", m_logicObjName));
			if (!confirmFreeze) {
				return IActionEnum.eCanceled;
			}
		}
		return IActionEnum.eActivated;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean editSuccessful = false;
		try {
			UnplacedObjectShareabilityControl.pushControl(isShareOfUnplacedObjectsDisabled,
					allowShareOfPlacedPinListWithUnplacedPins);
			editSuccessful = super.onTerminate(successful);
			if (editSuccessful) {
				IProject project = m_design.getProject();
				String projectUid = project.getUID().getString();
				String sharedStr = ResourceMgr.getString(AuditableEventType.class, "AuditableEventType.SHARED");
				IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
				IUID sharedObjectUid = m_helper.getSharedObjectUID();
				if (sharedObjectUid != null) {
					m_newlySharedObjUid = sharedObjectUid.getString();
				}
				int evtType = m_helper.isNewSharedObject() ? AuditableEventType.SHARED_OBJECT_ADDED :
						AuditableEventType.SHARED_OBJECT_MODIFIED;
				auditLogger.postEvent(evtType, sharedStr, projectUid, getNewlySharedObjectName(), m_newlySharedObjUid);
				getController().getSelectMgr().notifySelectionChanged();
			}
		}
		finally {
			if (m_helper != null) {
				m_helper.cleanup();
			}
			UnplacedObjectShareabilityControl.popControl();
		}

		return editSuccessful;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return ShareActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// Disabled for normal use
		//
		BaseShareActionOperands shareOperands = getShareOperands(selections);
		if (shareOperands != null) {
			if (isValidByLockingConstraint(shareOperands)) {
				container.add(new ActionEntry(getActionUI()));
			}
		}
	}

	public boolean isValidByLockingConstraint(@NotNull BaseShareActionOperands shareOperands)
	{
		IPinList cablePinList = shareOperands.getCablePinList();
		if (cablePinList != null) {
			return SharePinListLockHandler.checkLockOnSourceImpactedObjectForShare(cablePinList);
		}
		if (shareOperands.target instanceof IMulticore) {
			IMulticore multicore = (IMulticore) shareOperands.target;
			return ShareConcurrencyHelper.checkLockOnSourceImpactedObjectForShare(multicore);
		}
		return true;
	}

	// Enabled if there are any IParameterized objects seAlected.
	public boolean isEnabled()
	{
		//reset to original its must.
		m_disabledReason = null;
		if (!super.isEnabled()) {
			return false;
		}
		//
		// Is there a shareable object selected? Yes & parameterized -> OK.
		//
		BaseShareActionOperands operands = getShareOperands(getController().getSelectMgr().getPreSelections());
		if (operands != null) {
			if (!isValidByLockingConstraint(operands)) {
				m_disabledReason = ResourceMgr.getString(ShareAction.class, "ShareAction.DisableReason.Lock.text");
				return false;
			}
		}
		//we can have cases of unplaced objects where menu will be shown
		//in context menu but as disabled and with reason as tooltip
		return (operands != null && !shouldShowMenuAsDisabled(operands));
	}

	@Nullable public static BaseShareActionOperands getShareOperands(SelectSet selections)
	{
		final List<IUIDObject> uidObjects = getSelectedUIDObjectList(selections);
		final BaseShareActionOperands operands =
				BaseShareActionHelper.getShareOperands(uidObjects, BaseShareActionOperandStrategy.getInstance());

		final BaseShareActionOperands finalOperand =
				BaseShareActionOperandStrategy.getInstance().isShareable(operands.getShareabilityStatus()) ? operands :
						null;

		if (finalOperand != null && finalOperand.getLogicObject() instanceof IGroundDevice &&
				!BuildInfo.getBuildInfo().areQAExtensionsEnabled()) {
			return null;
		}
		return finalOperand;
	}

	public boolean shouldShowMenuAsDisabled(BaseShareActionOperands operands)
	{
		//dts0100809894: if we have a shared conductor part of a shared multicore which has a special usage
		//such that multicore is used in the design but not the shared conductor because this shared cond
		//has no diagram usage and the design is also not save yet. in this case we shouldn't be able to delete
		//the shared conductor otherwise there will be left a cable conductor whose shared conductor is deleted.
		//The above mentioned issue is on all logic objects. we can face serious issues because we will be able
		//to delete shared objects if there are no representations in saved state or no reference to the shared
		//object in design connectivity. so we will be disabling share operation which could potentially
		//lead to such scenarions. chandras
		StringBuilder disableReason = new StringBuilder();
		boolean status = BaseShareActionHelper.shouldDisableShareForUnplaced(operands, disableReason,
				isShareOfUnplacedObjectsDisabled, allowShareOfPlacedPinListWithUnplacedPins);
		if (status && !StringUtils.isBlank(disableReason.toString())) {
			m_disabledReason = disableReason.toString();
		}
		return status;
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.

	private String getNewlySharedObjectName()
	{
		String sharedObjectName = m_newlySharedObjName;
		IRevisionedSharedObject newSharedObject =
				UIDMgr.getObjectOfType(m_newlySharedObjUid, IRevisionedSharedObject.class);
		if (newSharedObject != null) {
			sharedObjectName = newSharedObject.getFullName();
		}
		return sharedObjectName;
	}

	public static void enableShareOfUnplacedObjects()
	{
		if (Environment.isUnitTest()) {
			isShareOfUnplacedObjectsDisabled = false;
		}
	}

	public static void disableShareOfUnplacedObjects()
	{
		if (Environment.isUnitTest()) {
			isShareOfUnplacedObjectsDisabled = true;
		}
	}

	public static void allowShareOfPlacedPinListWithUnplacedPins()
	{
		if (Environment.isUnitTest()) {
			allowShareOfPlacedPinListWithUnplacedPins = true;
		}
	}

	public static void disallowShareOfPlacedPinListWithUnplacedPins()
	{
		if (Environment.isUnitTest()) {
			allowShareOfPlacedPinListWithUnplacedPins = false;
		}
	}
}