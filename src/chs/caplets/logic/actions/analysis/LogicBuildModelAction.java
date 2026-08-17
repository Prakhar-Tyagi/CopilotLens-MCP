/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.IAnalysisAttachmentTargetProvider;
import chs.caf.cafmain.actions.analysis.BuildModelAction;
import chs.caf.caplet.ICapletController;

/**
 * @author rharring
 */
public class LogicBuildModelAction extends BuildModelAction
{

	/**
	 * Creates a new instance of LogicBuildModelAction
	 * <p>
	 * Simply passes the params up to the super class.
	 */
	public LogicBuildModelAction(ICapletController controller,
			IAnalysisAttachmentTargetProvider provider,
			boolean symbolOnly,
			boolean symReadOnly,
			boolean instReadOnly)
	{
		super(controller, provider, symbolOnly, symReadOnly, instReadOnly);
	}

	public String getActionUIClass()
	{
		return LogicBuildModelActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		if (!m_provider.doesContainerSupportActionInMUMode()) {
			return false;
		}
		return super.isEnabled();
	}
}

