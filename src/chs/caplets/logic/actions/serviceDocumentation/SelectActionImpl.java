/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caplets.shared.actions.SelectAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.ISelectClient;

public class SelectActionImpl extends SelectAction
{

	public SelectActionImpl(ICapletController controller)
	{
		super(controller);
	}

	@Override protected ISelectClient getInitSelectClient()
	{
		return new SelectActionClient(this,getController());
	}

	@Override protected void initSelectHelper()
	{
		super.initSelectHelper();
	}

	@Override public String getActionUIClass()
	{
		return SelectActionUI.class.getName();
	}
}
