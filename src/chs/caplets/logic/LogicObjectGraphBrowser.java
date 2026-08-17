/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.dev.utils.ObjectGraphBrowser;

public class LogicObjectGraphBrowser extends ObjectGraphBrowser
{

	private static ConnectivityBrowserDialog mDialog = null;

	/**
	 * @see chs.system.ObjectBrowser#doInvokeConnectivityBrowser()
	 */
	protected void doInvokeConnectivityBrowser()
	{
		// For attach/detach of listeners we create the dialog each time.
		if ((mDialog != null) && mDialog.isVisible()) {
			return;
		}
		//noinspection AssignmentToStaticFieldFromInstanceMethod
		mDialog = new ConnectivityBrowserDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				"Connectivity Browser");
	}
}

