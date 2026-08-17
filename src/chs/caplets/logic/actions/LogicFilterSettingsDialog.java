/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.graphics.OptionFilterSettingsDialog;
import chs.caplets.logic.LogicFilterControl;
import chs.utilities.ResourceMgr;
import chs.utility.topology.utils.TooltipUtils;

/**
 * A dialog for editing LogicFilterControl
 */
public class LogicFilterSettingsDialog extends OptionFilterSettingsDialog
{

	public LogicFilterSettingsDialog(LogicFilterControl fc)
	{
		super(fc, ResourceMgr.getString(LogicFilterSettingsDialog.class, "LogicFilterSettingsDialog.title",
				fc.getProject().getName()));
	}

	public void cleanupOnDispose()
	{
		TooltipUtils.setTooltipDismissDelayDefault();
	}

	protected void addComponents()
	{
		super.addComponents();
		TooltipUtils.setTooltipDismissDelay_8_sec();
	}

	public LogicFilterControl getFilterControl()
	{
		return (LogicFilterControl) super.getFilterControl();
	}
}
