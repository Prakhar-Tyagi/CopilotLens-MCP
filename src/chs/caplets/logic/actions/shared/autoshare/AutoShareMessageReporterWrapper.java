package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AutoShareMessageReporterWrapper implements IShareMessageContextReporter
{

	@NotNull private IMessageReporterWithContext mReporter;
	@NotNull private Supplier<IMessageContext> mContextProvider;

	public AutoShareMessageReporterWrapper(@NotNull IMessageReporterWithContext reporter,
			@NotNull Supplier<IMessageContext> contextProvider)
	{
		mReporter = reporter;
		mContextProvider = contextProvider;
	}

	@Override
	public void report(@NotNull PromptSeverity severity, @NotNull String message, @NotNull IMessageContext context)
	{
		mReporter.report(severity, message, context);
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message)
	{
		mReporter.report(severity, message);
	}

	@NotNull public IMessageContext getMessageContext()
	{
		return mContextProvider.get();
	}
}
