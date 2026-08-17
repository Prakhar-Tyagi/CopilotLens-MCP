/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caplets.logic.actions.shared.IHyperLinkStatusMessage;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for status message
 */
public interface IHarnessPropagateStatusMessage extends IHyperLinkStatusMessage
{

	@NotNull IHarnessPropagateStatusMessageGroup getGroup();

	boolean shouldPropgate();

	@NotNull IUID getObjectId();

	@Nullable IUID getDesignId();

	boolean isSharedRow();

	@NotNull String getObjectType();

	@NotNull String getPreviousHarness();

	@NotNull String getCurrentHarness();

	void setupPropagationStatus(boolean propagate);

	boolean isEditable();

	@NotNull HarnessPropagateMessageType getMessageType();

	void setMessage(String message);
}
