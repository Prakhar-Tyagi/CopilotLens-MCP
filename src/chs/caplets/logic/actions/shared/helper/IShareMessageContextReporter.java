package chs.caplets.logic.actions.shared.helper;

import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;

public interface IShareMessageContextReporter extends IMessageReporterWithContext
{

	@NotNull IMessageContext getMessageContext();
}
