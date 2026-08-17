package chs.caplets.logic.actions.shared.autoshare;

import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageCollectorAndReporter;
import chs.utility.IMessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class FetchOffPagesContextMessageReporter implements IMessageCollectorAndReporter
{

	@Nullable private IMessageContext mCurrentContext;
	@NotNull private IMessageCollectorAndReporter mPublisherReporterDelegate;
//	private final Collection<Message> storedMessages = new ArrayList<>();

	public FetchOffPagesContextMessageReporter(@NotNull IMessageCollectorAndReporter delegate)
	{
		mPublisherReporterDelegate = delegate;
	}

	@Override
	public void report(@NotNull PromptSeverity severity, @NotNull String message, @NotNull IMessageContext context)
	{
		IMessageContext currentContext = context;
		if (context == IMessageContext.UndeterminedContext && mCurrentContext != null) {
			currentContext = mCurrentContext;
		}
		storeMessage(severity, message, currentContext);
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message)
	{
		report(severity, message, IMessageContext.EmptyContext);
	}

	public void setCurrentContextObject(@Nullable IMessageContext currentContextObject)
	{
		mCurrentContext = currentContextObject;
	}

	public void storeMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
//		storedMessages.add(new Message(severity, message, context));
		mPublisherReporterDelegate.storeMessage(severity, message, context);
	}

	public void reportStoredMessages(@NotNull Predicate<PromptSeverity> severityfilter)
	{
//		storedMessages.stream()
//				.filter(message -> severityfilter.test(message.getSeverity()))
//				.forEach(message -> mPublisherReporterDelegate.report(message.getSeverity(), message.getMessage(),
//						message.getContext()));
//		storedMessages.clear();
		mPublisherReporterDelegate.reportStoredMessages(severityfilter);
	}

//	private static class Message
//	{
//
//		@NotNull private PromptSeverity m_severity;
//		@NotNull private String m_message;
//		@NotNull private IMessageContext m_context;
//
//		Message(@NotNull PromptSeverity severity, @NotNull String message, @NotNull IMessageContext context)
//		{
//			m_severity = severity;
//			m_message = message;
//			m_context = context;
//		}
//
//		@NotNull public PromptSeverity getSeverity()
//		{
//			return m_severity;
//		}
//
//		@NotNull public String getMessage()
//		{
//			return m_message;
//		}
//
//		@NotNull public IMessageContext getContext()
//		{
//			return m_context;
//		}
//	}
}
