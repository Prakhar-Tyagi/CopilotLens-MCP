package chs.caplets.logic.actions.serviceDocumentation.offPage.messages;

import chs.api.servicedoc.statusmessage.AbstractPackagerStatusMessage;
import chs.api.servicedoc.statusmessage.IssueReporterProvider;
import chs.api.servicedoc.statusmessage.XIssueReporter;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageCollectorAndReporter;
import chs.utility.IMessageContext;
import com.mentor.chs.api.IXObject;
import com.mentor.chs.plugin.drc.IXDRCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessageReporterWithContext implements IMessageCollectorAndReporter
{

	private final XIssueReporter m_issueReporter;
	@Nullable private XIssueReporter m_currentIssueReporter = null;
	private String m_messageCategory;
	private String m_messageSource;
	private IssueReporterProvider m_issueReporterProvider;

	public MessageReporterWithContext(String meesageSource, String messageCategory,
			IssueReporterProvider issueReporterProvider)
	{
		m_messageSource = meesageSource;
		m_messageCategory =messageCategory;
		m_issueReporterProvider = issueReporterProvider;
		m_issueReporter = createXIssueReporter();
	}

	private XIssueReporter createXIssueReporter()
	{
		return m_issueReporterProvider.createIssueReporter(m_messageSource, m_messageCategory);
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		reportMessageWithContext(severity, message, context, m_issueReporter);
	}

	@Override public void report(@NotNull PromptSeverity severity, @NotNull String message)
	{
		reportMessageWithContext(severity, message, IMessageContext.EmptyContext, m_issueReporter);
	}

	@Override public void storeMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		if(m_currentIssueReporter == null) {
			m_currentIssueReporter = createXIssueReporter();
		}
		reportMessageWithContext(severity, message, context, m_currentIssueReporter);
	}

	@Override public void reportStoredMessages(@NotNull Predicate<PromptSeverity> severityfilter)
	{
		if (m_currentIssueReporter != null) {
			Set<AbstractPackagerStatusMessage> issues = m_currentIssueReporter
					.getIssues()
					.stream()
					.filter(issue -> (severityfilter.test(getSeverity(issue.getSeverity()))))
					.filter(msg -> msg instanceof AbstractPackagerStatusMessage)
					.map(msg -> (AbstractPackagerStatusMessage) msg)
					.collect(Collectors.toSet());
			m_issueReporter.addIssues(issues);
		}
		m_currentIssueReporter = null;
	}

	protected void reportMessageWithContext(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext messageContext, XIssueReporter issueReporter)
	{
		IXDRCheck.Severity xSeverity = getSeverity(severity);
		List<IXObject> ixObjects = messageContext.getObjectsInContext()
				.stream()
				.filter(o -> o instanceof IUIDObject)
				.map(o -> (IUIDObject) o)
				.map(this::getIxObject)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		IXObject[] list = new IXObject[ixObjects.size()];
		ixObjects.toArray(list);
		issueReporter.report(xSeverity, message, list);
	}

	@Nullable private IXObject getIxObject(IUIDObject o)
	{
		IXObject ixObject = null;
		try {
			ixObject = FactoryMgr.getAPIFactory().createXObject(o);
		}
		catch (Exception ignored) {
		}
		return ixObject;
	}

	@NotNull private IXDRCheck.Severity getSeverity(PromptSeverity severity)
	{
		if (severity == PromptSeverity.ERROR) {
			return IXDRCheck.Severity.Error;
		}
		if (severity == PromptSeverity.INFORMATION) {
			return IXDRCheck.Severity.Information;
		}
		if (severity == PromptSeverity.WARNING) {
			return IXDRCheck.Severity.Warning;
		}
		return IXDRCheck.Severity.Information;
	}

	@NotNull private PromptSeverity getSeverity(IXDRCheck.Severity severity)
	{
		if (severity == IXDRCheck.Severity.Error) {
			return PromptSeverity.ERROR;
		}
		if (severity == IXDRCheck.Severity.Information) {
			return PromptSeverity.INFORMATION;
		}
		if (severity == IXDRCheck.Severity.Warning) {
			return PromptSeverity.WARNING;
		}
		return PromptSeverity.INFORMATION;
	}

	public XIssueReporter getIssueReporter()
	{
		return m_issueReporter;
	}
}
