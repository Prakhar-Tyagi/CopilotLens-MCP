/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caplets.logic.actions.shared.autoshare.AbstractAutoShareExecutor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.common.ILockable;
import chs.common.IUIDObject;
import chs.common.NonEditableReason;
import chs.common.NonEditableReasonInfo;
import chs.common.validation.ValidationHelper;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.system.FactoryMgr;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.VoidLogger;
import chs.utility.helpers.LockSharedPinListHelper;
import chs.utility.helpers.LogTabInfo;
import chs.utility.helpers.LogTabType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

abstract class AbstractBulkAutoShareCmd extends CHSCommand
{

	@NotNull protected IProject m_project;
	@NotNull protected ILogicDesign m_design;
	@NotNull protected IMessageReporterWithContext m_messageReporter;
	@NotNull protected Set<ILockable> m_acquiredLocks = new HashSet<>();
	@NotNull protected Collection<String> m_storedAuditLogIds = new ArrayList<>();

	AbstractBulkAutoShareCmd(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext messageReporter)
	{
		super(new CAFCommandHelper());
		m_design = design;
		assert design.getProject() != null;
		m_project = design.getProject();
		m_messageReporter = messageReporter;
	}

	@Override public boolean doExecuteAllowed()
	{
		acquireLocks();
		boolean okToExecute = m_design.isLocked() && m_project.getSharedPinListMgr().isLocked() &&
				m_project.getSharedConductorMgr().isLocked();
		if (!m_design.isEditable()) {
			okToExecute = false;
			reportDesignNotEditable();
		}
		if (!okToExecute) {
			releaseAcquiredLocks();
		}
		return okToExecute;
	}

	private void reportDesignNotEditable()
	{
		NonEditableReasonInfo reasonInfo = m_design.getNonEditableReasonInfo();
		if (reasonInfo != null) {
			String msgKey;
			NonEditableReason reason = reasonInfo.getReason();
			if (NonEditableReason.RELEASE_LEVEL_NO_DESIGN_EDIT.equals(reason)) {
				msgKey = "AbstractBulkAutoShareCmd.designnoteditable.reason.releaselevel";
			}
			else if (NonEditableReason.USER_DOMAIN_MISMATCH.equals(reason)) {
				msgKey = "AbstractBulkAutoShareCmd.designnoteditable.reason.userdomainmismatch";
			}
			else if (NonEditableReason.DESIGN_WAS_GENERATED.equals(reason)) {
				msgKey = "AbstractBulkAutoShareCmd.designnoteditable.reason.generated";
			}
			else {
				msgKey = "AbstractBulkAutoShareCmd.designnoteditable.reason.unspecified";
			}
			String message = ResourceMgr.getString(AbstractBulkAutoShareCmd.class, msgKey, m_design.getFullName());
			if (message != null) {
				m_messageReporter.report(PromptSeverity.ERROR, message, IMessageContext.createContext(m_design));
			}
		}
	}

	@Override protected void doStart()
	{
		getCommandHelper().enterTransactionBoundary(this);
	}

	protected boolean doShare(@NotNull AbstractAutoShareExecutor autoShareExecutor,
			@NotNull IUIDObject objectToBeShared)
	{
		m_messageReporter.setCurrentContextObject(IMessageContext.createContext(objectToBeShared));
		try {
			if (!autoShareExecutor.execute(objectToBeShared)) {
				return false;
			}
		}
		finally {
			m_messageReporter.setCurrentContextObject(null);
		}
		return true;
	}

	@Override protected void doEnd(boolean executeOk)
	{
		getCommandHelper().exitTransactionBoundary(this, executeOk);
		getCommandHelper().processEdtRequests();
		releaseAcquiredLocks();
		if (!executeOk) {
			revertMemoryState();
			discardAuditLogs();
		}
		else {
			postAuditLogs();
		}
	}

	protected void revertMemoryState()
	{
		if (FactoryMgr.getCHSSystem().getUserSession() == null) {
			return;
		}
		ISharedPinListMgr sharedPinListMgr = m_project.getSharedPinListMgr();
		sharedPinListMgr.revert();
		/* TODO : pinlists also needs to be reverted
		for (ISharedPinList sharedPinList : ((ISharedFullyLoadedPinListMgr)sharedPinListMgr).getSharedPinLists()) {
			if (!sharedPinList.isSkeleton() && sharedPinList.isModified()) {
				sharedPinList.revert();
			}
		}*/
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		sharedConductorMgr.setTimeModified(-1);
		sharedConductorMgr.unloadChildren();
		sharedConductorMgr.refresh();
	}

	protected void saveSharedPinlistMgr()
	{
		ISharedPinListMgr sharedPinListMgr = m_project.getSharedPinListMgr();
		ValidationHelper.validateAfterGeneration(sharedPinListMgr);
		sharedPinListMgr.save();
	}

	protected void acquireLocks()
	{
		lockAndReportOnFailure(m_design);
		lockAndReportOnFailure(m_project.getSharedPinListMgr());
		lockAndReportOnFailure(m_project.getSharedConductorMgr());
	}

	protected void lockAndReportOnFailure(@NotNull ILockable lockable)
	{
		LOCK_RESULT lockResult = lockObject(lockable);
		if (lockResult == LOCK_RESULT.ATTAINED_LOCK) {
			m_acquiredLocks.add(lockable);
		}
		else if (lockResult == LOCK_RESULT.LOCK_FAILED) {
			reportLockFailure(lockable);
		}
	}

	protected void releaseAcquiredLocks()
	{
		Set<ISharedPinList> lockedSharedPinlists = new HashSet<>();
		for (ILockable lockedObject : m_acquiredLocks) {
			if (lockedObject instanceof ISharedPinList) {
				lockedSharedPinlists.add((ISharedPinList) lockedObject);
			}
			else {
				lockedObject.unlock();
			}
		}
		if (!lockedSharedPinlists.isEmpty()) {
			LockSharedPinListHelper.unlockMultipleSharedPinLists(lockedSharedPinlists);
		}
		m_acquiredLocks.clear();
	}

	protected void postAuditLogs()
	{
		IAuditTrailLogger auditLogger = getAuditLogger();
		auditLogger.postStoredEvents(m_storedAuditLogIds);
		m_storedAuditLogIds.clear();
	}

	private void discardAuditLogs()
	{
		IAuditTrailLogger auditLogger = getAuditLogger();
		auditLogger.discardStoredEvents(m_storedAuditLogIds);
		m_storedAuditLogIds.clear();
	}

	@NotNull protected IAuditTrailLogger getAuditLogger()
	{
		return CAFUtils.getInstance().getAuditLogger();
	}

	protected void reportLockFailure(@NotNull ILockable lockable)
	{
		CTFLockUpdateHelper.logLockFailure(lockable, new LockFailureReporter(m_messageReporter), LogTabType.TAB_DEBUG,
				false);
	}

	private static class LockFailureReporter extends VoidLogger
	{

		@NotNull private IMessageReporterWithContext m_messageReporter;

		LockFailureReporter(@NotNull IMessageReporterWithContext messageReporter)
		{
			m_messageReporter = messageReporter;
		}

		@Override public void logMsg(String tab, String msg)
		{
			if (msg != null) {
				m_messageReporter.report(PromptSeverity.ERROR, msg);
			}
		}

		@Override public void logMsg(@NotNull LogTabInfo tabInfo, @NotNull String msg)
		{
			logMsg(tabInfo.getTabName(), msg);
		}
	}
}
