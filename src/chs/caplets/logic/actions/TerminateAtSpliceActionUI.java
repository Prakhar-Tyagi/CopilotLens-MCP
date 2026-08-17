/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign, Application.CapitalLogicDesigner,
		Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_TERMINATE_AT_SPLICE_ACTION",
		label = "Convert to Splice terminated",
		tooltip = "Convert to Splice terminated",
		icon = "ico_splice_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class TerminateAtSpliceActionUI extends ActionUI
{

	public TerminateAtSpliceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_T);

		putValue(NAME, ResourceMgr.getString(TerminateAtSpliceActionUI.class, "TerminateAtSpliceActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(TerminateAtSpliceActionUI.class, "TerminateAtSpliceActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(TerminateAtSpliceActionUI.class, "TerminateAtSpliceActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		return super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return TerminateAtSpliceAction.class.getName();
	}
}
