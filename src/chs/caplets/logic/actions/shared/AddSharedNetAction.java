/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateConductorAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 13, 2004 Time: 9:14:52 AM
 */
public class AddSharedNetAction extends CreateConductorAction
{

	private ISpecialSelectMgr m_specialSelectMgr;
	private ISharedConductor m_sharedNet;

	public AddSharedNetAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);

		m_specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			m_specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							return getOperand() != null;
						}
					});
		}
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_sharedNet = getOperand();
		if (m_sharedNet == null) {
			return IActionEnum.eCanceled;
		}

		final ILogicDesign logicDesign = getLogicModel().getDesign();

		if (mCreateCondInstanceHelper.isSharedConductorUnusable(m_sharedNet, logicDesign, this::refresh)) {
			return IActionEnum.eCanceled;
		}

		if (!ShareConcurrencyHelper.trySharedObjectPlacement(logicDesign, Collections.singleton(m_sharedNet))
				.contains(m_sharedNet)) {
			return IActionEnum.eCanceled;
		}

		Set<ILogicObject> candidatesToLock =
				ShareConcurrencyHelper.getCorrespondingLogicObjectsToLock(logicDesign, m_sharedNet);
		if (!candidatesToLock.isEmpty() && !LogicObjectLockFinder.tryEdit(logicDesign, candidatesToLock).isEmpty()) {
			return IActionEnum.eCanceled;
		}

		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = successful;
		try {
			if (ok && !refresh()) {
				ok = false;
			}

//			if (ok && !m_sharedNet.lockForExclusiveRead()) {
//				LogicActionMessageHelper.warnLocked(m_sharedNet);
//				ok = false;
//			}

			chs.cof.logical.schem.IConductor conductor = null;
			if (ok && getCommand() != null) {
				// need to get the conductor before terminating super, as that will clear the command
				conductor = getCommand().getConductor();
			}

			ok = super.onTerminate(ok);

			if (ok && conductor != null) {
				ISharedMulticore mcore = m_sharedNet.getMulticore();
				addShieldWithLibraryPart(mcore, conductor);
			}
		}
		finally {
			m_sharedNet.unlock();
		}

		m_sharedNet = null;
		return ok;
	}

	/**
	 * Refreshed the shared wire - returns if the shared wire still exists.
	 *
	 * @return TRUE if the object exists and was refreshed  FALSE if the shared object no longer exists
	 */
	private boolean refresh()
	{
		RefreshStatusEnum rs = m_sharedNet.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(AddSharedNetAction.class, "AddSharedNetAction.SharedNetDeleted",
							m_sharedNet.getName()),
					"");
			getLogicModel().getDesign().getProject().getSharedConductorMgr().refresh();
			getLogicModel().getDesign().getProject().getSharedConductorMgr().fireChangeEvent();
			return false;
		}
		return true;
	}

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		// If this isn't the first time this net has been placed in this design, use the existing connectivity.
		// design/connectivity won't be null here
		//noinspection ConstantConditions
		IConductor cableCond = getLogicModel().getDesign().getConnectivity().findSharedConductor(m_sharedNet);
		getCommand().setCableConductor(cableCond);

		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(smartPoints);
		SharedConductorHelper
				.assignToShared(schemCond, m_sharedNet, getLogicModel().getDesign(), getLogicModel().getDiagram());
		return schemCond;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		return super.isEnabled() && getOperand() != null;
	}

	public String getActionUIClass()
	{
		return AddSharedNetActionUI.class.getName();
	}

	@Nullable
	private ISharedConductor getOperand()
	{
		if (m_specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedConductor) {
				ISharedConductor shareConductor = (ISharedConductor) uidObj;
				final ILogicDesign design = ((ILogicModel) getModel()).getDesign();
				if (shareConductor.getType().equalsIgnoreCase(ISharedConductor.NET_TYPE)) {
					return shareConductor;
				}
			}
		}
		return null;
	}
}
