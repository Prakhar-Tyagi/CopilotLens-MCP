/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * enum for share,shareinto actions
 */
public enum Action
{
	SHARE(ResourceMgr.getString(Action.class, "Action.share.text")),
	SHARE_INTO(ResourceMgr.getString(Action.class, "Action.shareinto.text"));

	private String displayName;

	Action(String displayName)
	{
		this.displayName = displayName;
	}

	@NotNull @Override public String toString()
	{
		return displayName;
	}
}
