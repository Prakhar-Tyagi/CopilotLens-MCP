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
import chs.cof.logical.cable.LogicOtherComponentTypeEnum;
import org.jetbrains.annotations.NotNull;

public class CreateDuctWithoutPartAndSymbolAction extends CreateLayoutComponentWithoutPartAndSymbolAction
{

	public CreateDuctWithoutPartAndSymbolAction(ICapletController controller)
	{
		super(controller, CreateDuctWithoutPartAndSymbolActionUI.class.getName());
	}

	@NotNull protected LogicOtherComponentTypeEnum determineDefaultComponentType()
	{
		return LogicOtherComponentTypeEnum.DUCT;
	}
}
