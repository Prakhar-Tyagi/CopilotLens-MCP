/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2025 Siemens
 */
package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.AbstractShareConductorGroupActionHelper;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.ui.LockInfoDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareConductorGroupActionHelper extends AbstractShareConductorGroupActionHelper
{

	@NotNull protected IMessageReporterWithContext mMessageReporter;
	private boolean m_isBulkPromotion;

	public AutoShareConductorGroupActionHelper(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion)
	{
		super(design);
		mMessageReporter = reporter;
		m_isBulkPromotion = isBulkPromotion;
	}

	@Override
	protected boolean createAndValidateMulticoreShareContextProvider(@NotNull IMulticore multicore, String dialogTitle)
	{
		AutoShareMulticoreContextProvider multicoreShareContextProvider =
				createAutoShareMulticoreContextProvider(multicore);
		if (multicoreShareContextProvider == null) {
			return false;
		}
		m_multicoreShareContextProvider = multicoreShareContextProvider;
		return multicoreShareContextProvider.validate();
	}

	@Nullable
	protected AutoShareMulticoreContextProvider createAutoShareMulticoreContextProvider(@NotNull IMulticore multicore)
	{
		return new AutoShareMulticoreContextProvider(multicore, m_design, mMessageReporter);
	}

	@NotNull @Override
	protected Runnable getConflictResolver(@NotNull ISharedObjectModificationObserver observer)
	{
		return () -> {
		};
	}

	protected void sendMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		mMessageReporter.report(severity, message, context);
	}

	protected void reportSharedCondMgrLocked(ISharedConductorMgr sharedCondrMgr)
	{
		Pair<String, String> displayName = LockInfoDialog.getCategoryAndNameForDisplay(sharedCondrMgr);
		final String message = ResourceMgr.getString(AutoShareConductorGroupActionHelper.class,
				"AutoShareConductorGroupActionHelper.SharedCondMgrLocked.msg", displayName.getSecond());
		sendMessage(PromptSeverity.ERROR, message, IMessageContext.EmptyContext);
	}

	@Override protected boolean isBulkPromotion()
	{
		return m_isBulkPromotion;
	}

	@Override protected boolean isChangeReportingRequired()
	{
		return false;
	}

	@Override protected boolean shouldSyncWithLibraryPart()
	{
		return false;
	}
}
