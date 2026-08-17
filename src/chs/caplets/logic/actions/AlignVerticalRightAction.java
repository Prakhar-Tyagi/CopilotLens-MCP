/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.utility.placement.IPlacementDirector;
import chs.utility.placement.LogicAlignmentDirector;
import chs.utility.placement.PlacementConstants;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Aug 2, 2004 Time: 1:19:34 PM
 */
public class AlignVerticalRightAction extends AbstractPlacementAction
{

	public AlignVerticalRightAction(ICapletController controller)
	{
		super(controller);
	}

	protected IPlacementDirector createPlacementDirector()
	{
		return new LogicAlignmentDirector(PlacementConstants.VERTICAL, PlacementConstants.RIGHT);
	}

	public String getActionUIClass()
	{
		return AlignVerticalRightActionUI.class.getName();
	}
}
