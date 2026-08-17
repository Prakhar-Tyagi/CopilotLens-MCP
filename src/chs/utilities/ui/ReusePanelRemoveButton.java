/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.utilities.ui;

import chs.caplets.logic.actions.shared.ReusePanel;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import com.mentor.customLookandFeel.CustomJButton;

import javax.swing.JList;

/**
 * RemoveButton for ReusePanel
 */
public class ReusePanelRemoveButton extends CustomJButton
{

	private JList<IPinProxy> reusableListUI;
	private String removeTooltip;
	private String removeDisabledTooltip;

	public ReusePanelRemoveButton(String text, JList<IPinProxy> reusableListUI, String removeTooltip,
			String removeDisabledTooltip)
	{
		super(text);
		this.reusableListUI = reusableListUI;
		this.removeTooltip = removeTooltip;
		this.removeDisabledTooltip = removeDisabledTooltip;
	}

	public void setEnabled(boolean b)
	{
		if (b || reusableListUI.getSelectedValuesList().isEmpty()) {
			setToolTipText(ResourceMgr.getString(ReusePanel.class, removeTooltip));
		}
		else {
			setToolTipText(ResourceMgr.getString(ReusePanel.class, removeDisabledTooltip));
		}
		super.setEnabled(b);
	}
}
