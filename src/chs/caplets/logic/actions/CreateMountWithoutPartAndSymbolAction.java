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

public class CreateMountWithoutPartAndSymbolAction extends CreateLayoutComponentWithoutPartAndSymbolAction
{

	public CreateMountWithoutPartAndSymbolAction(ICapletController controller)
	{
		super(controller, CreateMountWithoutPartAndSymbolActionUI.class.getName());
	}

	@NotNull protected LogicOtherComponentTypeEnum determineDefaultComponentType()
	{
		return LogicOtherComponentTypeEnum.RAIL;
	}
}
