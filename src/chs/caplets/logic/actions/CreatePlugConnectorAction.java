/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;

/**
 * @author Matt Boyd
 */
public class CreatePlugConnectorAction extends CreateConnectorAction
{

	/**
	 * Constructor for CreatePlugConnectorAction.
	 *
	 * @param controller
	 */
	public CreatePlugConnectorAction(ICapletController controller)
	{
		super(controller);

		setSubType(PLUG_CONNECTOR);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreatePlugConnectorActionUI.class.getName();
	}
}