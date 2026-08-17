/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2025 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.AbstractShareHighwayActionHelper;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.SharedObjectAbstractionMatcher;
import chs.utility.ui.LockInfoDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareHighwayActionHelper extends AbstractShareHighwayActionHelper
{

	@NotNull private IMessageReporterWithContext mMessageReporter;
	private boolean mIsBulkPromotion;

	public AutoShareHighwayActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion)
	{
		super(design, diagram);
		mMessageReporter = reporter;
		mIsBulkPromotion = isBulkPromotion;
	}

	protected void reportError(@NotNull String message, @NotNull IMessageContext context)
	{
		mMessageReporter.report(PromptSeverity.ERROR, message, context);
	}

	@NotNull protected IMessageContext getMessageContext()
	{
		final IUIDObject contextObject = m_schemObject != null ? m_schemObject : m_logicObject;
		return contextObject != null ? IMessageContext.createContext(contextObject) :
				IMessageContext.UndeterminedContext;
	}

	@Override protected int handleDuplicateName(@NotNull String name, @NotNull String objectType)
	{
		final String message = ResourceMgr.getString(AutoShareHighwayActionHelper.class,
				"AutoShareHighwayActionHelper.NameExistsError.Message.text", StringUtils.toLowerCase(objectType));
		reportError(message, getMessageContext());
		// Result All actually corrosponds to cancel
		return MessageHelper.RESULT_ALL;
	}

	@Override protected void reportSharedObjectMgrLocked()
	{
		Pair<String, String> displayName = LockInfoDialog.getCategoryAndNameForDisplay(m_sharedObjectMgr);
		final String message = ResourceMgr.getString(AutoShareHighwayActionHelper.class,
				"AutoShareHighwayActionHelper.SharedCondMgrLocked.msg", displayName.getSecond());
		reportError(message, IMessageContext.EmptyContext);
	}

	@Override protected void reportSharedObjectDeleted(@NotNull ISharedObject shareIntoObj)
	{
		final String message = ResourceMgr.getString(AutoShareHighwayActionHelper.class,
				"AutoShareHighwayActionHelper.SharedObjectDeleted.Text", shareIntoObj.getName());
		reportError(message, IMessageContext.EmptyContext);
	}

	@Override protected void attemptShare(@NotNull ISharedObjectModificationObserver observer, @NotNull Runnable sharingActivity)
	{
		sharingActivity.run();
		observer.setModified();
	}

	@Override protected boolean isBulkPromotion()
	{
		return mIsBulkPromotion;
	}

	@Override protected boolean isChangeReportingRequired()
	{
		return false;
	}

	@Override protected boolean hasDuplicateName(@NotNull ISharedGeneralHighway sharedObject)
	{
		return sharedObject.getName().equalsIgnoreCase(m_logicObject.getName()) && SharedObjectAbstractionMatcher
				.areAbstractionsSame(sharedObject.getDesignAbstraction(), m_design.getDesignAbstraction());
	}
}
