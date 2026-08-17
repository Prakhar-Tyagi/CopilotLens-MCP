/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.utilities.ui;

import chs.caplets.logic.actions.shared.helper.AddRemovePinHandler;
import com.mentor.customLookandFeel.CustomJButton;

/**
 * RemoveButton for SharedPinListAddRemoveButtons
 */
public class SharedPinListAddButton extends CustomJButton
{

	private AddRemovePinHandler mHandler;

	public SharedPinListAddButton(String text, AddRemovePinHandler mHandler)
	{
		super(text);
		this.mHandler = mHandler;
	}

	public void setEnabled(boolean b)
	{
		super.setEnabled(b && mHandler.allowAddPins());
	}
}
