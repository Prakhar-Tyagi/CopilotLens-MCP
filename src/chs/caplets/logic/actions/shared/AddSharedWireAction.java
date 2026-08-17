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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateWireAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 9, 2004 Time: 1:42:50 PM
 */
public class AddSharedWireAction extends CreateWireAction
{

	private ISpecialSelectMgr m_specialSelectMgr;
	private IUID m_sharedWireUID;

	public AddSharedWireAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
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
		m_sharedWireUID = getOperand();
		if (m_sharedWireUID == null) {
			return IActionEnum.eCanceled;
		}
		ILogicDesign logicDesign = getLogicModel().getDesign();
		ISharedConductor sharedWire = UIDMgr.getObjectOfType(m_sharedWireUID, ISharedConductor.class);
		if (sharedWire == null) {
			return IActionEnum.eCanceled;
		}


		if (mCreateCondInstanceHelper.isSharedConductorUnusable(sharedWire, logicDesign, this::refresh)) {
			return IActionEnum.eCanceled;
		}
		if (!ShareConcurrencyHelper.trySharedObjectPlacement(logicDesign, Collections.singleton(sharedWire))
				.contains(sharedWire)) {
			return IActionEnum.eCanceled;
		}

		Set<ILogicObject> candidatesToLock =
				ShareConcurrencyHelper.getCorrespondingLogicObjectsToLock(logicDesign, sharedWire);
		if (!candidatesToLock.isEmpty() && !LogicObjectLockFinder.tryEdit(logicDesign, candidatesToLock).isEmpty()) {
			return IActionEnum.eCanceled;
		}

		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = successful;
		ISharedConductor sharedCond = UIDMgr.getObjectOfType(m_sharedWireUID, ISharedConductor.class);
		try {
			if (ok && !refresh(sharedCond, getLogicModel().getDesign().getProject())) {
				ok = false;
			}

//			if (ok && !m_sharedWire.lockForExclusiveRead()) {
//				LogicActionMessageHelper.warnLocked(m_sharedWire);
//				ok = false;
//			}

			ok = super.onTerminate(ok);

			if (ok) {
				assert sharedCond != null;
				ISharedMulticore mcore = sharedCond.getMulticore();
				addShieldWithLibraryPart(mcore, getCommand().getConductor());
			}
		}
		finally {
			assert sharedCond != null;
			sharedCond.unlock();
		}

		m_sharedWireUID = null;
		return ok;
	}

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		// If this isn't the first time this wire has been placed in this design, use the existing connectivity.
		// design&connectivity won't be null here
		//noinspection ConstantConditions
		ISharedConductor sharedConductor = UIDMgr.getObjectOfType(m_sharedWireUID, ISharedConductor.class);
		ConductorDisplayObjectConstructionHelper helper = new ConductorDisplayObjectConstructionHelper(getLogicModel(), getCommand(), this::getSchemConductor);
		return helper.constructDisplayObject(smartPoints, sharedConductor);
	}

	private IConductor getSchemConductor(List<ISmartPoint> smartPoints){
		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(smartPoints);
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
		return AddSharedWireActionUI.class.getName();
	}

	@Nullable
	private IUID getOperand()
	{
		if (m_specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedConductor) {
				ISharedConductor shareConductor = (ISharedConductor) uidObj;
				final ILogicDesign design = ((ILogicModel) getModel()).getDesign();
				if (shareConductor.isWire()) {
					return shareConductor.getUID();
				}
			}
		}
		return null;
	}
}