/*
 * Copyright 2009 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;

public class CreateInternalLinkResistanceAction extends CreateInternalLinkAction
{

	/**
	 * Constructor for the CreateLineAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateInternalLinkResistanceAction(ICapletController controller)
	{
		super(controller, "Resistance");
	}

	public String getActionUIClass()
	{
		return CreateInternalLinkResistanceActionUI.class.getName();
	}
}
