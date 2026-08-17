/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateGeneralHighwayAction;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.IObjectInUseService;
import chs.utility.PortHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.helpers.HighwayShareHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 13, 2004 Time: 9:14:52 AM
 */
public class AddSharedGeneralHighwayAction extends CreateGeneralHighwayAction
{
	private ISpecialSelectMgr m_specialSelectMgr;
	private ISharedGeneralHighway m_sharedHighway;

	public AddSharedGeneralHighwayAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);

		m_specialSelectMgr = libSelectMgr;
		if (getActionUI() != null && libSelectMgr != null) {
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
		m_sharedHighway = getOperand();
		if (m_sharedHighway == null) {
			return IActionEnum.eCanceled;
		}

		if (!LogicObjectLockFinder.tryEdit(m_sharedHighway)) {
			return IActionEnum.eCanceled;
		}

		if (!refresh()) {
			return IActionEnum.eCanceled;
		}

		if (AddSharedHelper.isSharedObjectPermissionDenied()) {
			return IActionEnum.eCanceled;
		}

		final ILogicDesign logicDesign = getLogicModel().getDesign();
		// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
		final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
		if (!new SharedObjectAvailabilityChecker().check(m_sharedHighway, logicDesign, reporter, true, true)) {
			return IActionEnum.eCanceled;
		}

		if (!ShareConcurrencyHelper.trySharedObjectPlacement(getLogicModel().getDesign(),
				Collections.singleton(m_sharedHighway)).contains(m_sharedHighway)) {
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
			if (ok && !m_sharedHighway.lockForExclusiveRead()) {
				LogicActionMessageHelper.warnLocked(m_sharedHighway);
				ok = false;
			}

			ok = super.onTerminate(ok);
		}
		finally {
			m_sharedHighway.unlock();
		}

		m_sharedHighway = null;
		return ok;
	}

	/**
	 * Refreshed the shared wire - returns if the shared wire still exists.
	 *
	 * @return TRUE if the object exists and was refreshed  FALSE if the shared object no longer exists
	 */
	private boolean refresh()
	{
		RefreshStatusEnum rs = m_sharedHighway.refresh();
		if (rs != RefreshStatusEnum.eRefreshNotNeeded) {
			getLogicModel().getDesign().getProject().getSharedConductorMgr().fireChangeEvent();
		}
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(AddSharedGeneralHighwayAction.class, "AddSharedGeneralHighwayAction.SharedHighwayDeleted",
							m_sharedHighway.getName()),
					"");
			return false;
		}
		return true;
	}

	protected IGfxObject constructDisplayObject(List<ISmartPoint> smartPoints)
	{
		// If this isn't the first time this net has been placed in this design, use the existing connectivity.
		// design/connectivity won't be null here
		//noinspection ConstantConditions
		IHighway cableHighway = getLogicModel().getDesign().getConnectivity().findSharedHighway(m_sharedHighway);
		getCommand().setCableHighway(cableHighway);

		IHighwaySchematic schemHighway = (IHighwaySchematic) super.constructDisplayObject(smartPoints);
		//why do we need it? causing issue in multi-user scenarios. trying to load all the diagrams of design.
		//HighwayShareHelper.getInstance().share(schemHighway.getConnectivity(), m_sharedHighway);
		if (m_sharedHighway != null && schemHighway != null) {
			if (cableHighway == null) {
				HighwayShareHelper.shareHighwayInto(schemHighway.getConnectivity(), m_sharedHighway);
			}
			PortHelper.addPortGfx(schemHighway, getLogicModel().getDiagram().getGrid().getGridSpacing());
			schemHighway.setHome(!m_sharedHighway.isUsed(IObjectInUseService.OBJECT_IN_USE));
		}
		return schemHighway;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		return !FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary() && super.isEnabled() &&
				getOperand() != null;
	}

	public String getActionUIClass()
	{
		return AddSharedGeneralHighwayActionUI.class.getName();
	}

	@Nullable
	protected ISharedGeneralHighway getOperand()
	{
		if (m_specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_specialSelectMgr.getSelectedObjects().getNext();
			if (SingleLineHelper.isSharedSingleLine(uidObj)) {
				//this is a shared cable but not shared highway
				return null;
			}
			if (uidObj instanceof ISharedGeneralHighway) {
				return (ISharedGeneralHighway) uidObj;
			}
		}
		return null;
	}
}
