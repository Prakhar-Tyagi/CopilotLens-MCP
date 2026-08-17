/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Reporter to capture feedbacks during batch share
 */
public class BatchShareReporter implements IMessageReporterWithContext
{

	@NotNull private Consumer<IBatchShareStatusMessage> delegateReporter;
	@Nullable private IMessageContext m_currentContext;

	public BatchShareReporter(@NotNull Consumer<IBatchShareStatusMessage> delegate)
	{
		delegateReporter = delegate;
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		IMessageContext reportingContext = context;
		if (IMessageContext.UndeterminedContext == context && m_currentContext != null) {
			reportingContext = m_currentContext;
		}
		if (reportingContext.getObjectsInContext().isEmpty()) {
			reportMessage(severity, message, null);
		}
		else {
			reportingContext.getObjectsInContext()
					.forEach(obj -> reportMessage(severity, message, obj));
		}
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message)
	{
		reportMessage(severity, message, null);
	}

	private void reportMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@Nullable Object object)
	{
		delegateReporter.accept(new BatchShareStatusMessage(severity, message, object));
	}

	@Override public void setCurrentContextObject(@Nullable IMessageContext currentContextObject)
	{
		m_currentContext = currentContextObject;
	}
}
