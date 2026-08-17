/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICaplet;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */

/**
 * Created by jamesmw User: jamesmw Date: 22-Jun-2007 Time: 17:04:05
 */
public class SharePortActionUI extends ShareActionUI
{

	public SharePortActionUI(ICaplet controller)
	{
		super(controller);
	}

	public String getActionClass()
	{
		return SharePortAction.class.getName();
	}
}
