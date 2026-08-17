/*
 * Copyright 2010-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.schem.IShieldBody;
import chs.common.IUIDObject;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Jul 19, 2010 Time: 3:33:27 PM To change this template use File |
 * Settings | File Templates.
 */
public class AddCommentSymbolToMCAction extends AddCommentSymbolAction
{

	public AddCommentSymbolToMCAction(ICapletController controller)
	{
		super(controller);
	}

	protected void determinePreSelectedOwner()
	{
		final IShieldBody operand = getOperand(getController().getSelectMgr().getPreSelections());
		if (operand != null) {
			m_owningObject = operand;
		}
	}

	@Override public boolean isEnabled()
	{
		// ActionHierarchy this action must call super.isEnabled.
		// This will make enabling and disabling from the framework difficult
		return getOperand(getController().getSelectMgr().getPreSelections()) != null && super.isEnabled();
	}

	@Override public String getActionUIClass()
	{
		return AddCommentSymbolToMCActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		final Action actionUI = getActionUI();
		if (getOperand(selections) != null && actionUI != null) {
			container.add(new ActionEntry(actionUI));
		}
	}

	@Nullable private IShieldBody getOperand(SelectSet sset)
	{
		IShieldBody indicatorBody = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IShieldBody) {
				indicatorBody = (IShieldBody) uidObj;
				//TODO - break here
			}
		}
		return indicatorBody;
	}
}
