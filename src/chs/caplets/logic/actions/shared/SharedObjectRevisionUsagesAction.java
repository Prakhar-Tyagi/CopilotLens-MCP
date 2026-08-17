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
import chs.caf.IFIB;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedHighway;
import chs.cof.logical.shared.ISharedMessageSignal;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.ctf.ui.form.sharedobjectrevisioning.ShowSharedObjectUsagesHelper;
import chs.system.FactoryMgr;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.persist.LockableHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;

/**
 * FEAT00013725 - Automated handling of shared object revisions
 * <p/>
 * This is an action handler for displaying the usages of the selected SharedObject Revision
 * <p/>
 *
 * @author ntewari
 */
public class SharedObjectRevisionUsagesAction extends ControllerActionRT
{

	private ISpecialSelectMgr m_sharedSelectMgr;
	private IRevisionedSharedObject m_revObject;

	public SharedObjectRevisionUsagesAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null) {
			m_sharedSelectMgr.contextMenuAddAction(new ActionEntry(getActionUI(),
					(String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
			{
				public boolean shouldDisplay()
				{
					boolean shouldDisplay = getOperand() != null && isEnabled();
					if (!shouldDisplay) {
						Action ui = getActionUI();
						if (ui == null) {
							return false;
						}
						((IUpdateableAction) ui).updateUI();
					}
					return shouldDisplay;
				}
			});
		}
		setUndoableAction(false);
	}

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

		if (operand instanceof ISharedMessageSignal) {
			return false;
		}
		if (operand instanceof ISharedConductor && ((ISharedConductor) operand).getMulticore() != null) {
			// it is an instnace of an inner core of an overbraid or multicore
			return false;
		}
		if (operand instanceof ISharedMulticore sharedMulticore) {
			if(sharedMulticore.getParent() != null){
				// it is an instance of an inner core of an overbraid or multicore
				return false;
			}
			if(SingleLineHelper.isMulticorePartOfAnySingleLine(sharedMulticore)){
				//it is Single Line's multicore
				return false;
			}
		}
		if (operand instanceof ISharedSingleLine) {
			// action enabled for a single line
			return true;
		}
		if (operand instanceof ISharedHighway) {
			// action not applicable for a highway
			return false;
		}
		return super.isEnabled();
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
			IProject project = FactoryMgr.getSystemFactory().getCAFUtils().getCurrentProject();
			Frame window = null;
			if (getController() != null && getController().getCaplet() != null &&
					getController().getCaplet().getFIB() != null) {
				IFIB fib = getController().getCaplet().getFIB();
				if (fib != null && fib.getWindowMgr() != null) {
					window = fib.getWindowMgr().getDialogFrame();
				}
			}
			AbstractAction zoomAction = ZoomSelectedUsagesAction.getZoomTreeMenuAction();
			ShowSharedObjectUsagesHelper.showSharedObjectUsagesDialog(project, m_revObject, zoomAction, window);
		}
		return true;
	}

	public String getActionUIClass()
	{
		return SharedObjectRevisionUsagesActionUI.class.getName();
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

		if (LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), m_revObject)) {
			return IActionEnum.eCompleted;
		}
		else {
			LogicActionMessageHelper.warnRevisionedSharedObjectDeleted(m_revObject);
			return IActionEnum.eCanceled;
		}
	}
}
