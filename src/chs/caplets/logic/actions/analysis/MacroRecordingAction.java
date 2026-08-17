/*
 * Copyright 2008-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;

import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: zali Date: Jan 22, 2008 Time: 11:18:22 AM To change this template use File | Settings
 * | File Templates.
 */
public class MacroRecordingAction extends ControllerActionRT
{

	private Model m_model;

	public MacroRecordingAction(ICapletController c)
	{
		super(c);
		m_model = (Model) c.getCapletModel();
		setUndoableAction(false);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		boolean isRecording = "true".equals(System.getProperty("AnalysisLogger.dynamic.enabled", "false"));
		if (isRecording) {
			// if we are recording we toggle the recording state to off
			System.setProperty("AnalysisLogger.dynamic.enabled", "false");
			((MacroRecordingActionUI) this.getActionUI()).adjustUI(!isRecording);
		}
		else {
			// if we are not recording we turn on the recording
			System.setProperty("AnalysisLogger.dynamic.enabled", "true");
			((MacroRecordingActionUI) this.getActionUI()).adjustUI(!isRecording);
		}
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return false;
	}

	public String getActionUIClass()
	{
		return MacroRecordingActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		return AnalysisServices.isActionSupportedInMUMode(m_model.getDesign()) && super.isEnabled();
	}

	@Override public boolean enabledInReadOnly()
	{
		return true;
	}

	@Override protected boolean checkCache()
	{
		return false;
	}
}
