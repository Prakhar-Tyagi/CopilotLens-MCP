/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.utilities.ui;

import chs.caplets.logic.actions.shared.SharedPinListAddRemoveButtons;
import chs.caplets.logic.actions.shared.helper.AddRemovePinHandler;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import com.mentor.customLookandFeel.CustomJButton;

import javax.swing.JList;

/**
 * RemoveButton for SharedPinListAddRemoveButtons
 */
public class SharedPinListRenameButton extends CustomJButton
{

	private JList<IPinProxy> m_proxyList;
	private AddRemovePinHandler mHandler;
	private String TooltipForRenameForFrozenNotAllowed;
	private String TooltipForRenameButtonWhenNoObjectSelected;
	private String TooltipForRenameButton;

	public SharedPinListRenameButton(String text, JList<IPinProxy> m_proxyList, AddRemovePinHandler mHandler,
			String TooltipForRenameButton, String TooltipForRenameButtonWhenNoObjectSelected, String TooltipForRenameForFrozenNotAllowed)
	{
		super(text);
		this.m_proxyList = m_proxyList;
		this.mHandler = mHandler;
		this.TooltipForRenameButton = TooltipForRenameButton;
		this.TooltipForRenameButtonWhenNoObjectSelected = TooltipForRenameButtonWhenNoObjectSelected;
		this.TooltipForRenameForFrozenNotAllowed = TooltipForRenameForFrozenNotAllowed;
	}

	public void setEnabled(boolean b)
	{
		if (mHandler.isFrozenSharedPinList()) {
			setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					TooltipForRenameForFrozenNotAllowed));
		}
		else if (m_proxyList.getSelectedIndices().length != 1) {
			setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					TooltipForRenameButtonWhenNoObjectSelected));
		}
		else {
			setToolTipText(ResourceMgr.getString(SharedPinListAddRemoveButtons.class,
					TooltipForRenameButton));
		}
		super.setEnabled(b && m_proxyList.getSelectedIndices().length == 1);
	}
}
