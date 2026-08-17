/*
 * Copyright 2003-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.SmartEditAction;
import chs.caf.caplet.helpers.SmartEditActionUI;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.cof.drawplus.IDiagramObject;
import chs.common.IUIDObject;
import chs.utility.preferences.StylebleObjectUtility;

import java.awt.event.ActionEvent;

public class PublisherSmartEditAction extends SmartEditAction
{

	public PublisherSmartEditAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected void initStylableObjectUtility()
	{
		super.initStylableObjectUtility();
		m_stylebleObjectUtility = new StylebleObjectUtility();
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		if (CAFUtils.getInstance().getFIB().isTaskActive(IFIB.TASK_SAVE) ||
				CAFUtils.getInstance().getUserSession() == null) {
			return false;
		}
		return getController().getCapletModel().isEditable() && isModeEnabled();
	}

	protected IAction getDelegateAction(IUIDObject object)
	{
		ICapletController controller = getController();
		SelectSet preSelections = controller.getSelectMgr().getPreSelections();
		preSelections.remove(m_object.getUID());
		preSelections.add(new Selection(object));
		return controller.getAction(PublisherSmartEditPropertiesAction.class);
	}

	/**
	 * //	 * Return our matching ActionUI class //
	 */
	public String getActionUIClass()
	{
		String actionUIClassName = SmartEditActionUI.class.getName();
		return actionUIClassName;
	}

	protected void invokeEditDialog()
	{
		//this functions similar to the Edit dialog of logic.
		if ((m_objectAttProvider != null) && (m_objectAttProvider instanceof IUIDObject)) {
			IDiagramObject parent = ((IDiagramObject) m_object).getParent();
			IAction action = getDelegateAction(parent);
			SelectSet selections = getController().getSelectMgr().getPreSelections();
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action.getActionName(), 0);
			getController().getActionMgr().actionPerformed(action, ae);
			assert parent != null;
			selections.remove(parent.getUID());
		}
	}
}
