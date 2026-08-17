/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

// chs imports

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.SelectHelper;
import chs.caf.caplet.selection.ISelectClient;

public class SelectAction extends chs.caplets.shared.actions.SelectAction
{

	public SelectAction(ICapletController controller)
	{
		super(controller);
	}

	protected void initSelectHelper()
	{
		// Create a selector to do all of the work.
		m_selectHelper = new SelectHelper(getController(),
				getController().getSelectMgr().getPreSelections(),
				getEventDistributor(),
				m_selectClient);
	}

	//
	// Return OUR select client...
	//
	protected ISelectClient getInitSelectClient()
	{
		return new chs.caplets.symbol.actions.SelectActionClient(this, getController());
	}

	public String getActionUIClass()
	{
		return SelectActionUI.class.getName();
	}
}
