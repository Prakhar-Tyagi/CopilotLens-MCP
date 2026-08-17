/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.shared.ICreateConductorInstanceAction;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.services.dynamicgfx.IDynamicSnap;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * Instantiate shield conductor from browser tab
 *
 * @author chandras on 22-10-2021.
 */
public class AddShieldConductorAction extends CreateShieldConductorAction implements ICreateConductorInstanceAction
{

	public AddShieldConductorAction(ICapletController controller)
	{
		super(controller);
		mCreateCondInstanceHelper.doNotIgnoreShieldConductor();
	}

	@Override public String getActionUIClass()
	{
		return AddShieldConductorActionUI.class.getName();
	}

	@Override public boolean checkSnap(@Nullable IDynamicSnap dynSnap)
	{
		IMulticore selectedMulticore = getSelectedMulticore();
		if (dynSnap != null && selectedMulticore != null && !checkHookupSnap(dynSnap, selectedMulticore)) {
			return false;
		}
		return super.checkSnap(dynSnap);
	}

	@Nullable private IMulticore getSelectedMulticore()
	{
		IConductor conductor = mCreateCondInstanceHelper.getSelectedConductorObject();
		IMulticore selectedMulticore = conductor != null ? conductor.getMulticore() : null;
		return selectedMulticore;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful);
		mCreateCondInstanceHelper.onTerminate();
		return ok;
	}

	public boolean isEnabled()
	{
		return super.isEnabled() && mCreateCondInstanceHelper.isReadyForActivation(IShieldConductor.class);
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		if (!mCreateCondInstanceHelper.onActivate(this::refresh, getLocalModel().getDesign(), IShieldConductor.class)) {
			return IActionEnum.eCanceled;
		}
		IActionEnum status = super.onActivate(e);
		//Setup only after snap helper is initialized in super.onActivate.
		setupSnapHelper(getSelectedMulticore());
		return status;
	}

	@Override public boolean isReadyForActivation()
	{
		return isEnabled();
	}
}
