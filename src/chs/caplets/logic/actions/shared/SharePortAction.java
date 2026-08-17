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
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.logical.cable.IConductor;
import chs.common.IUIDObject;

import javax.swing.Action;
import java.awt.event.ActionEvent;

public class SharePortAction extends ShareAction
{

	private ISpecialSelectMgr specialSelectMgr;

	public SharePortAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);
		specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							if (specialSelectMgr != null && specialSelectMgr.getSelectedObjects().getSize() == 1) {
								IUIDObject uidObj = specialSelectMgr.getSelectedObjects().getNext();
								if (uidObj instanceof IConductor) {
									IConductor netCond = (IConductor) uidObj;
									if (netCond.getSharedConductor() == null) {
										return true;
									}
								}
							}
							return false;
						}

						public String getName()
						{
							return (String) getAction().getValue(Action.NAME);
						}
					}
			);
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		IUIDObject uidObj = specialSelectMgr.getSelectedObjects().getNext();
		if (uidObj instanceof IConductor) {
			IConductor connCond = (IConductor) uidObj;
			if (connCond.getSharedConductor() == null) {
				m_helper = m_conductorHelper;
				m_operands = new BaseShareActionOperands();
				m_operands.target = connCond;
				m_logicObjName = connCond.getName();
				m_newlySharedObjName = m_logicObjName;
				m_newlySharedObjUid = connCond.getUID().getString();
			}
		}
		else {
			return IActionEnum.eCanceled;
		}
		if (checkFrozen() == IActionEnum.eCanceled) {
			return IActionEnum.eCanceled;
		}
		Model model = (Model) getController().getCapletModel();
		return m_helper.setup(m_operands, CAFUtils.getInstance().getDialogTitleByAction(this), model.getDiagram());
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		if (!getController().getCapletModel().isEditable() || !isModeEnabled()) {
			return false; // Unable to change this design, so can't do it.
		}
		if (specialSelectMgr != null && specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof IConductor) {
				IConductor netCond = (IConductor) uidObj;
				if (netCond.getSharedConductor() == null) {
					return true;
				}
			}
		}
		return false;
	}

	public String getActionUIClass()
	{
		return SharePortActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}
}
