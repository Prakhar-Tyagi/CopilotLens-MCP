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

import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.actions.AddBSTerminationActionHelper;
import chs.caplets.logic.shared.AddSharedPinDialog;
import chs.cof.logical.shared.ISharedPinList;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;

/**
 * DIfference between this helper and its parent is that this uses the publisher dialog, instead of the parent one
 */
public class PublisherAddBSTerminationActionHelper extends AddBSTerminationActionHelper
{

	public PublisherAddBSTerminationActionHelper(ControllerActionRT actionRT)
	{
		super(actionRT);
	}

	@NotNull @Override
	protected AddSharedPinDialog getSharedPinSelectionDialog(Frame parentFrame, ISharedPinList sharedPinList)
	{
		return new PublisherAddSharedPinDialog(parentFrame, getDialogTitle(), sharedPinList);
	}
}
