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
import chs.cof.logical.cable.IWireConductor;

/**
 * A create tool to make multiple conductors in one action.
 * <p/>
 * created Dec 11, 2014
 */
public class CreateMultipleWiresAction extends CreateMultipleConductorsAction
{

	/**
	 * Constructor for the CreateConductorAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateMultipleWiresAction(ICapletController controller)
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
		return CreateMultipleWiresActionUI.class.getName();
	}

	protected Class<IWireConductor> getConductorType()
	{
		return IWireConductor.class;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<?> snappingSource()
	{
		return IWireConductor.class;
	}

	protected String getCursorImage()
	{
		return "chs/images/app/cur_wire.gif";
	}

	@Override protected boolean shouldSnapGuides()
	{
		return false;
	}
}