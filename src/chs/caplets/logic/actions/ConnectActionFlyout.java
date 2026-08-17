/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.FlyoutActionContainer;

/**
 * This class is only here to prevent spurious isEnabled checks getting called on the ConnectActions.
 * <p/>
 * In future we could also extend it to provide caching of isEnabled results from similar Connect actions.
 */
public class ConnectActionFlyout extends FlyoutActionContainer
{

	public ConnectActionFlyout(ICapletController controller, String sActionUIClass)
	{
		super(controller, sActionUIClass);
	}
}
