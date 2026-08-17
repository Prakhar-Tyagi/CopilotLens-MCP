/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;

public class CreateOtherComponentWithoutPartAndSymbolAction extends CreateLayoutComponentWithoutPartAndSymbolAction
{

	public CreateOtherComponentWithoutPartAndSymbolAction(ICapletController controller)
	{
		super(controller, CreateOtherComponentWithoutPartAndSymbolActionUI.class.getName());
	}
}
