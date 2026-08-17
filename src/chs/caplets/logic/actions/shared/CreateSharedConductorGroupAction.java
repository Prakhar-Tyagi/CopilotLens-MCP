/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.caplets.logic.actions.CreateMulticoreAction;
import chs.system.FactoryMgr;

import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 5, 2004 Time: 12:15:51 PM
 */
public class CreateSharedConductorGroupAction extends CreateMulticoreAction
{

	public CreateSharedConductorGroupAction(ICapletController controller)
	{
		super(controller);
		setEditScope(MulticoreEditPanel.SHARED_SCOPE);
	}

	public String getActionUIClass()
	{
		return CreateSharedConductorGroupActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		return super.isEnabled();
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}
