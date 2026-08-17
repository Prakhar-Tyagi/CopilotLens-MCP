/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.caplets.logic.actions.shared.helper.PinReuseHandler;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 24, 2005 Time: 1:12:11 PM
 */
public class AutoReuseView
{

	@NotNull protected PinReuseHandler mHandler;

	private boolean isEnabled = true;

	public static final int SPACE = 10;

	public AutoReuseView(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@NotNull IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		mHandler = new PinReuseHandler(model, design, reporter, isBulkShare);
	}

	public void makeAllPinsReusable()
	{
		if (isEnabled) {
			if (mHandler.allowAddAll()) {
				mHandler.makeAllPinsReusable();
			}
		}
	}

	public void init()
	{
		mHandler.init();
	}

	public void setEnabled(boolean enabled)
	{
		isEnabled = enabled;
	}
}
