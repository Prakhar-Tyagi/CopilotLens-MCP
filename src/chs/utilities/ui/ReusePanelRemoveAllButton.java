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
public class ReusePanelRemoveAllButton extends CustomJButton
{

	private JList<IPinProxy> reusableListUI;
	private String removeAllTooltip;
	private String removeAllDisabledTooltip;

	public ReusePanelRemoveAllButton(String text, JList<IPinProxy> reusableListUI, String removeAllTooltip,
			String removeAllDisabledTooltip)
	{
		super(text);
		this.reusableListUI = reusableListUI;
		this.removeAllTooltip = removeAllTooltip;
		this.removeAllDisabledTooltip = removeAllDisabledTooltip;
	}

	public void setEnabled(boolean b)
	{
		if (b || reusableListUI.getSelectedValuesList().isEmpty()) {
			setToolTipText(ResourceMgr.getString(ReusePanel.class, removeAllTooltip));
		}
		else {
			setToolTipText(ResourceMgr.getString(ReusePanel.class, removeAllDisabledTooltip));
		}
		super.setEnabled(b);
	}
}
