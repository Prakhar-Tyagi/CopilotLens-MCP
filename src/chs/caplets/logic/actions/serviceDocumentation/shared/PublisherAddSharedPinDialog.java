/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.caplets.logic.shared.AddSharedPinDialog;
import chs.cof.logical.shared.ISharedPinList;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;

/**
 * Place shared pin dialog in publisher, does not allow stackpin to be placed
 */
public class PublisherAddSharedPinDialog extends AddSharedPinDialog
{

	public PublisherAddSharedPinDialog(Frame frame,
			String title, @Nullable ISharedPinList sharedPinList)
	{
		super(frame, title, sharedPinList);
	}

	@Override protected void createAsStackOption()
	{

	}
}
