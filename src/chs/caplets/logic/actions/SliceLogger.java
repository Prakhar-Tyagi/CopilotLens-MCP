/*
 * Copyright 2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.AbstractSliceLogger;
import chs.common.IUIDObject;
import chs.utilities.BuildInfo;
import chs.utilities.ResourceMgr;
import chs.utility.NamedObjectUtils;
import chs.utility.ui.HTMLHelper;

public class SliceLogger extends AbstractSliceLogger
{

	private static boolean mLoggingEnabled = BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
			BuildInfo.getBuildInfo().areQAExtensionsEnabled();

	public SliceLogger()
	{
		super(ResourceMgr.getString(SliceLogger.class, "SliceLogger.output.tabname"));
	}

	@Override protected boolean isDebugEnabled()
	{
		return mLoggingEnabled;
	}

	@Override public void logUserMessage(String msg, IUIDObject... objs)
	{

		if (isDebugEnabled()) {
			Object[] links = new Object[objs.length];
			int linkIndex = 0;
			for (IUIDObject obj : objs) {
				links[linkIndex] = HTMLHelper.link(obj, NamedObjectUtils.getName(obj));
			}
			String outmsg = ResourceMgr.getString(SliceAction.class, "SliceAction.output.msg." + msg, links);
			log(outmsg);
		}
	}

	@Override public void prepareForSlice()
	{
		mOutputWindow.clearPane(mTabName);
	}

	@Override public void logDebugMessage(String msg, IUIDObject... objs)
	{
		logUserMessage(msg, objs);
	}
}
