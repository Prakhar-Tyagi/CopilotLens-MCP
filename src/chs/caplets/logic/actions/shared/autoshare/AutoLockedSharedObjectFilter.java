/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2026 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.caplets.logic.shared.AbstractLockedSharedObjectFilter;
import chs.cof.logical.IDesign;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.common.IRevisionedObject;
import chs.common.RefreshStatusEnum;
import chs.task.ReleaseLevelResultHtmlDisplayer;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoLockedSharedObjectFilter extends AbstractLockedSharedObjectFilter
{

	private boolean mIsBulkShare;
	@NotNull private IShareMessageContextReporter mReporter;

	public AutoLockedSharedObjectFilter(boolean isBulk, @NotNull IShareMessageContextReporter reporter)
	{
		mIsBulkShare = isBulk;
		mReporter = reporter;
	}

	@NotNull @Override protected ISharedObjectAvailabilityReporter getSharedObjectAvailabilityReporter()
	{
		return new AutoSharedObjectAvailabilityReporter();
	}

	protected RefreshStatusEnum refresh(@NotNull ISharedPinList newSPL)
	{
		return mIsBulkShare ? RefreshStatusEnum.eRefreshNotNeeded : newSPL.refresh();
	}

	protected boolean lock(@NotNull ISharedPinList newSPL)
	{
		return mIsBulkShare ? newSPL.isLocked() : SharedPinListHelper.lock(newSPL);
	}

	protected void unlock(@NotNull ISharedPinList newSPL)
	{
		if (!mIsBulkShare) {
			SharedPinListHelper.unlock(newSPL);
		}
	}

	protected void onSharedPinlistDeleted(@NotNull ISharedPinList newSPL)
	{
		mReporter.report(PromptSeverity.ERROR, LogicActionMessageHelper.getSharedPinlistDeletedMessage(newSPL),
				mReporter.getMessageContext());
	}

	private class AutoSharedObjectAvailabilityReporter implements ISharedObjectAvailabilityReporter
	{

		@Override
		public void report(@NotNull FailureReason reason, @NotNull ISharedObject sharedObject, @Nullable IDesign design)
		{
			final String sharedObjectReportableName = sharedObject instanceof IRevisionedObject ?
					((IRevisionedObject) sharedObject).getFullName() : sharedObject.getReportableName();
			final String sharedObjType =
					StringUtils.toLowerCase(ReleaseLevelResultHtmlDisplayer.typeString(sharedObject));
			String message = null;
			switch (reason) {
				case DOMAIN_ON_SHARED_OBJECT:
					message = ResourceMgr.getString(AutoLockedSharedObjectFilter.class,
							"AutoLockedSharedObjectFilter.InaccessibleDomainOnSharedObject.text", sharedObjType,
							sharedObjectReportableName);
					break;
				case DOMAIN_ON_ASSOCIATED_PART:
					message = ResourceMgr.getString(AutoLockedSharedObjectFilter.class,
							"AutoLockedSharedObjectFilter.InaccessibleDomainOnAssociatedPart.text", sharedObjType,
							sharedObjectReportableName);
					break;
				case DOMAIN_ON_ASSOCIATED_ICD:
					message = ResourceMgr.getString(AutoLockedSharedObjectFilter.class,
							"AutoLockedSharedObjectFilter.InaccessibleDomainOnAssociatedICD.text", sharedObjType,
							sharedObjectReportableName);
					break;
				case FROZEN_STATUS_SHARED_OBJECT:
					message = ResourceMgr.getString(AutoLockedSharedObjectFilter.class,
							"AutoLockedSharedObjectFilter.FrozenSharedObjectRequired.text");
					break;
			}
			if (message != null) {
				mReporter.report(PromptSeverity.ERROR, message, mReporter.getMessageContext());
			}
		}
	}
}
