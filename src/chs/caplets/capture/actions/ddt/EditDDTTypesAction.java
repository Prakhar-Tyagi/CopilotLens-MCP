/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.project.IProject;
import chs.cof.project.IProjectFactory;
import chs.cof.project.ddtrans.IDDTTypeMgr;
import chs.common.ICommonFactory;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;

import java.awt.Frame;
import java.awt.event.ActionEvent;

/**
 * Action to edit existing and create new ddt types.
 */
public class EditDDTTypesAction extends ControllerActionRT
{

	private EditDDTTypeMgrDialog m_dialog;
	private EditDDTTypesDialogModel m_dialogModel;
	private LockUpdateHelper m_lockUpdateHelper;

	public EditDDTTypesAction(ICapletController controller)
	{
		super(controller);
		IProject proj = CAFUtils.getInstance().getCurrentProject();
		IDDTTypeMgr typeMgr = proj.getDDTTypeMgr();
		if (typeMgr == null) {
			IProjectFactory projFact = FactoryMgr.getProjectFactory();
			ICommonFactory commonFact = CAFUtils.getInstance().getCommonFactory();
			typeMgr = projFact.constructDDTTypeMgr(commonFact.createUID());
		}
		m_dialogModel = new EditDDTTypesDialogModel(typeMgr);
		Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		m_dialog = new EditDDTTypeMgrDialog(frame, m_dialogModel);
	}

	public void destroy()
	{
		super.destroy();	//To change body of overridden methods use File | Settings | File Templates.
		//
		// Get rid of myself...
		if (m_dialog == null || !m_dialog.isVisible()) {
			return;
		}
		m_dialog.setVisible(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IProject proj = CAFUtils.getInstance().getCurrentProject();
		IDDTTypeMgr typeMgr = proj.getDDTTypeMgr();
		if (typeMgr == null) {
			IProjectFactory projFact = FactoryMgr.getProjectFactory();
			ICommonFactory commonFact = CAFUtils.getInstance().getCommonFactory();
			typeMgr = projFact.constructDDTTypeMgr(commonFact.createUID());
		}
		m_lockUpdateHelper = new LockUpdateHelper(typeMgr);
		boolean success = m_lockUpdateHelper.lockAndRefresh();
		if (!success) {
			return IActionEnum.eCanceled;
		}
		m_dialogModel.setTypeMgr(typeMgr);
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean ok = true;
		boolean unlocked = false;
		if (successful) {
			m_dialog.reset();
			m_dialog.pack();
			m_dialog.setVisible(true);
			if (m_dialog.wasValidated()) {
				IProjectFactory projFact = FactoryMgr.getProjectFactory();
				// Apply the edits
				m_dialogModel.applyTransientChanges(projFact);
				IProject proj = CAFUtils.getInstance().getCurrentProject();
				proj.setDDTTMgr(m_dialogModel.getTypeMgr());
				m_lockUpdateHelper.flushAndUnlock(true);
				unlocked = true;
			}
		}
		if (!unlocked) {
			m_lockUpdateHelper.flushAndUnlock(false);
		}
		return ok;
	}

	public String getActionUIClass()
	{
		return EditDDTTypesActionUI.class.getName();
	}
}
