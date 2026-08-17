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

import chs.caf.caplet.ICapletController;
import chs.cof.COFTypeEnum;

public class CreateOverbraidAction extends CreateMulticoreAction
{

	public CreateOverbraidAction(ICapletController controller)
	{
		super(controller);
		setEditType(COFTypeEnum.Overbraid);
	}

	public String getActionUIClass()
	{
		return CreateOverbraidActionUI.class.getName();
	}
}
