/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.CreateShieldConductorAction;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.common.IUIDObjectIterator;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * Instantiate shield conductor from browser tab
 *
 * @author chandras on 22-10-2021.
 */
public class AddSharedShieldAction extends CreateShieldConductorAction
{

	private ISpecialSelectMgr m_specialSelectMgr;
	@Nullable private IMulticore m_selectedMulticore;

	public AddSharedShieldAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		m_specialSelectMgr = libSelectMgr;
		final Action actionUI = getActionUI();
		if (actionUI != null) {
			ActionEntry actionEntry = new ActionEntry(actionUI, (String) actionUI.getValue(Action.SHORT_DESCRIPTION))
			{
				public boolean shouldDisplay()
				{
					return getOperand() != null;
				}
			};
			m_specialSelectMgr.contextMenuAddAction(actionEntry);
		}
	}

	@Nullable private IMulticore getOperand()
	{
		IUIDObjectIterator selectedObjects = m_specialSelectMgr.getSelectedObjects();
		if (selectedObjects.getSize() == 1) {
			ISharedConductor sharedConductor = CommonUtils.cast(selectedObjects.getNext(), ISharedConductor.class);
			if (sharedConductor != null && sharedConductor.getType().equalsIgnoreCase(ISharedConductor.SHIELD_TYPE)) {
				ISharedMulticore sharedMulticore = sharedConductor.getMulticore();
				IConnectivity connectivity = getLocalModel().getDesign().getConnectivity();
				return connectivity != null && sharedMulticore != null ?
						connectivity.findSharedMulticore(sharedMulticore) : null;
			}
		}
		return null;
	}

	@Override public String getActionUIClass()
	{
		return AddSharedShieldActionUI.class.getName();
	}

	@Override public boolean checkSnap(@Nullable IDynamicSnap dynSnap)
	{
		if (dynSnap != null && m_selectedMulticore != null && !checkHookupSnap(dynSnap, m_selectedMulticore)) {
			return false;
		}
		return super.checkSnap(dynSnap);
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		m_selectedMulticore = getOperand();
		IActionEnum status = super.onActivate(e);
		//Setup only after snap helper is initialized in super.onActivate.
		setupSnapHelper(m_selectedMulticore);
		return status;
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
}
