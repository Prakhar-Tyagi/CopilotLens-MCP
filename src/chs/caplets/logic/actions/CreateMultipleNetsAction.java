/*
 * Copyright 2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.INetConductor;

/**
 * A create tool to make multiple conductors in one action.
 * <p/>
 * created Dec 11, 2014
 */
public class CreateMultipleNetsAction extends CreateMultipleConductorsAction
{

	/**
	 * Constructor for the CreateConductorAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateMultipleNetsAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateMultipleNetsActionUI.class.getName();
	}

	protected Class<INetConductor> getConductorType()
	{
		return INetConductor.class;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<?> snappingSource()
	{
		return INetConductor.class;
	}

	protected String getCursorImage()
	{
		return "chs/images/app/cur_net.gif";
	}

	@Override protected boolean shouldSnapGuides()
	{
		return m_helper.isActive();
	}
}