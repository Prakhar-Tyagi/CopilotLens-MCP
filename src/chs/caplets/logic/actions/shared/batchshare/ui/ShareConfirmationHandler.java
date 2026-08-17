/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for displaying and processing confirmation dialogs in batch share/unshare operations.
 * <p>
 * This utility class provides a standardized way to prompt users for confirmation before executing
 * share or unshare actions. It creates and displays question dialogs with customizable messages
 * and choice buttons, ensuring user intent is confirmed.
 */
public class ShareConfirmationHandler
{

	private ShareConfirmationHandler()
	{
		// Private constructor to prevent instantiation of this utility class
	}

	public static boolean confirmAction(@NotNull SimpleOkCancelDialog parent, @NotNull String promptKey,
			@NotNull String proceedKey, @NotNull String cancelKey, int objectCount)
	{
		ResourceBasedMessageContent content = new ResourceBasedMessageContent(parent, promptKey);
		content.setImplicationsParameters(objectCount);

		Choice proceed = new Choice(parent, proceedKey);
		Choice cancel = new Choice(parent, cancelKey);

		return Question.show(content, proceed, cancel).equals(cancel);
	}
}
