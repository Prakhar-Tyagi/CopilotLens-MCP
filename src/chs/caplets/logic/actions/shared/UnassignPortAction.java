/*
 * Copyright 2007-2013 Mentor Graphics Corporation
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
import chs.caf.ICtxMenuProvider;
import chs.caf.IUpdateableAction;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.cmd.UnassignPortedConductorCmd;
import chs.common.IUIDObject;

import java.awt.event.ActionEvent;

public class UnassignPortAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IConductor schemConductor = null;
	private UnassignPortedConductorCmd cmd = null;

	public UnassignPortAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		if (schemConductor != null) {
			cmd = new UnassignPortedConductorCmd(new CAFCommandHelper(), schemConductor.getUID());
			return IActionEnum.eCompleted;
		}
		return IActionEnum.eCanceled;
	}

	protected boolean onTerminate(boolean successful)
	{

		boolean ok = successful && cmd.execute();
		cmd = null;
		schemConductor = null;
		return ok;
	}

	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {
			return false; // Unable to change this design, so can't do it.
		}
		chs.cof.logical.cable.IConductor operand = getOperand();
		if (operand != null) {
			((IUpdateableAction) getActionUI()).updateUI();
			return super.isEnabled();
		}
		return false;
	}

	public String getActionUIClass()
	{
		return UnassignPortActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperand() != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public chs.cof.logical.cable.IConductor getOperand()
	{
		schemConductor = null;
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IConductor && schemConductor == null) {
				chs.cof.logical.cable.IConductor connectivity = ((IConductor) uidObj).getConnectivity();
				if (connectivity.getSharedConductor() == null) {
					schemConductor = (IConductor) uidObj;
					continue;
				}
			}
			else if (uidObj instanceof ISegment) {
				// Don't care about segments
				continue;
			}
			// If it's not a conductor or a segments, fail
			return null;
		}
		return schemConductor == null ? null : schemConductor.getConnectivity();
	}
}
