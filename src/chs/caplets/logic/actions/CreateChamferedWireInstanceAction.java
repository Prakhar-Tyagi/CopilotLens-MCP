/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;

import java.awt.event.ActionEvent;

public class CreateChamferedWireInstanceAction extends CreateMultipleWiresAction
{

	public CreateChamferedWireInstanceAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return CreateChamferedWireInstanceActionUI.class.getName();
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		if (!m_helper.onActivate(this::refresh, getLocalModel().getDesign(), getConductorType())) {
			return IActionEnum.eCanceled;
		}
		return super.onActivate(e);
	}

	public boolean isEnabled()
	{
		return super.isEnabled() && m_helper.isReadyForActivation(getConductorType());
	}
}