/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.common.IUID;
import chs.ctf.ui.utility.statusmessage.IStatus;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface IHyperLinkStatusMessage
{

	boolean isSharedObjectLink();

	@NotNull IStatus getStatus();

	@NotNull String getMessage();

	@NotNull String getDesignName();

	@NotNull String getObjectDetailText();

	@NotNull String getObjectDetailLink();

	@NotNull static String getHyperlink(@NotNull IUID designUID, @NotNull IUID objectUID)
	{
		return designUID.getString() + '&' + objectUID.getString();
	}
}
