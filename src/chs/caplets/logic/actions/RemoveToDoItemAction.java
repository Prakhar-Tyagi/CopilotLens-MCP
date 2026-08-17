/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConfirmSaveDialog;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utility.logic.ILogicModel;

import javax.swing.Action;
import java.awt.event.ActionEvent;

public class RemoveToDoItemAction extends ControllerActionRT
{

	ISpecialSelectMgr m_selector;
	protected ConfirmSaveDialog m_confirmDialog;

	protected IInterconnectSourceInfo m_isi = null;
	IInterconnectToDoItem m_item = null;
	private ILogicDesign m_design;
	private ISchemDiagram m_diagram;

	public RemoveToDoItemAction(ICapletController controller, ISpecialSelectMgr toDoSelectMgr)
	{
		super(controller);

		ILogicModel logicModel = (ILogicModel) controller.getCapletModel();
		m_design = (ILogicDesign) logicModel.getDesign();
		m_diagram = logicModel.getDiagram();
		m_selector = toDoSelectMgr;

		m_item = null;
		m_isi = null;

		if (getActionUI() != null) {
			m_selector.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							return isEnabled();
						}
					});
			String title = ConfirmSaveDialog.getTitleForType(ConfirmSaveDialog.TYPE_WARNING);
			m_confirmDialog = new ConfirmSaveDialog(getClass().getName(), title,
					ResourceMgr.getString(RemoveToDoItemAction.class, "RemoveToDoItemAction.confirm.message"), false);
			m_confirmDialog.setHeader((String) getActionUI().getValue(Action.NAME));
		}
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		if (m_isi != null && m_item != null // Paranoid check
				&& !m_confirmDialog.userCanceled()) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			m_isi.removeToDoItem(m_item);
		}
		return successful;
	}

	public void destroy()
	{
		if (m_confirmDialog != null) {
			m_confirmDialog.dispose();
			m_confirmDialog = null;
		}

		m_selector = null;
		m_isi = null;
		m_item = null;
		m_design = null;
		m_diagram = null;
		super.destroy();
	}

	public String getActionUIClass()
	{
		return RemoveToDoItemActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		m_isi = null;
		m_item = null;

		// Paranoid check - the current diagram must be the one to which the To Do List applies
		IInterconnectSourceInfo isi = m_design.getInterconnectSourceInfo();
		if (isi != null && m_diagram.getUID().isEquiv(isi.getDiagramUID())) {
			// A single To Do Item must be selected in the To Do browser
			if (m_selector.getSelectedObjects().getSize() == 1) {
				IUIDObject uidObj = m_selector.getSelectedObjects().getNext();
				if (uidObj instanceof IInterconnectToDoItem) {
					m_isi = isi;
					m_item = (IInterconnectToDoItem) uidObj;
					return super.isEnabled();
				}
			}
		}

		return false;
	}
}
