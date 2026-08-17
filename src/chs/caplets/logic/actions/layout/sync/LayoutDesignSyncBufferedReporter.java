/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.logical.ILayoutLogicDesign;
import chs.common.sync.AbstractBaseSync;
import chs.common.sync.BufferedSyncReporter;
import chs.common.sync.ISyncRule;
import chs.ctf.ui.utility.statusmessage.IDesignStatusMessage;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.LogTabType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class LayoutDesignSyncBufferedReporter extends BufferedSyncReporter<ILayoutLogicDesign>
{

	LayoutDesignSyncBufferedReporter(ILayoutLogicDesign design, boolean outputMessages)
	{
		super(design, outputMessages);
	}

	@NotNull @Override protected String getResourceNameSuffix()
	{
		return ".layout";
	}

	@NotNull protected LogTabType getLogDestination()
	{
		return LogTabType.TAB_LAYOUT_SYNC;
	}

	@Override protected void outputBufferedMessagesToLogWindow(@NotNull List<IDesignStatusMessage> messages,
			boolean activateOutputWindow)
	{
		LogHelper.outputMessages(getLogDestination(), messages, activateOutputWindow);
	}

	@Override protected void performFirstMessageInitialization()
	{
		LogHelper.clear(getLogDestination());
	}

	@NotNull @Override protected String getCurrentMessageSourceDisplay(@NotNull String messageSource)
	{
		if (AbstractBaseSync.CHECKING_ASSOCIATED_DESIGNS.equals(messageSource)) {
			return super.getCurrentMessageSourceDisplay(messageSource);
		}
		final ISyncRule<ILayoutLogicDesign> currRule = getCurrentRule();
		if (currRule == null) {
			return ResourceMgr.getString(LayoutDesignSyncBufferedReporter.class,
					"LayoutDesignSyncBufferedReporter." + messageSource);
		}
		return ResourceMgr.getString(currRule.getClass(), messageSource + ".source");
	}

	@Nullable @Override protected ISyncRule<ILayoutLogicDesign> getCurrentRule()
	{
		return currentRule;
	}
}
