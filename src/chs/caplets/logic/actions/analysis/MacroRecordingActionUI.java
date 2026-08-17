/*
 * Copyright 2008-2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: zali Date: Jan 22, 2008 Time: 11:20:24 AM To change this template use File | Settings
 * | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalArchitect, Application.CapitalEssentialsDesign, Application.ArtisanArchitect,
				Application.SEElectricalDesign})
public class MacroRecordingActionUI extends ActionUI
{

	/**
	 * The icon to be shown when the action is enabled
	 */
	protected Icon enabledIcon;

	/**
	 * The icon to be shown when the action is disabled
	 */
	protected Icon disabledIcon;

	protected String disabledDesc;
	protected String enabledDesc;

	public MacroRecordingActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		String name = ResourceMgr
				.getString(QualitativeSimulationModeActionUI.class, "MacroRecordingActionUI.String.name");
		enabledDesc = ResourceMgr
				.getString(QualitativeSimulationModeActionUI.class, "MacroRecordingActionUI.String.name");
		disabledDesc = ResourceMgr
				.getString(QualitativeSimulationModeActionUI.class, "MacroRecordingActionUI.String.Disable");
		String longDesc = ResourceMgr
				.getString(QualitativeSimulationModeActionUI.class, "MacroRecordingActionUI.String.name");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_S);

		enabledIcon = CHSImageLoader.loadImageIcon("chs/images/app/record_macro.gif");
		disabledIcon = CHSImageLoader.loadImageIcon("chs/images/app/record_macro_disabled.gif");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, enabledDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(SMALL_ICON, enabledIcon);
	}

	public String getActionClass()
	{
		return MacroRecordingAction.class.getName();
	}

	public void adjustUI(boolean recordingOn)
	{
		// if we've enabling the action then we wish to allow the user to disable it
		if (recordingOn) {
			putValue(SMALL_ICON, disabledIcon);
			putValue(SHORT_DESCRIPTION, disabledDesc);
		}
		else {
			putValue(SMALL_ICON, enabledIcon);
			putValue(SHORT_DESCRIPTION, enabledDesc);
		}
	}
}
