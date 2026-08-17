/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.IFIB;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_RIBBON_MANAGE_CONNECTORS_ACTION",
		label = "Manage Connections",
		tooltip = "Manage Connections",
		icon = "ico_manage_connections")
public class ManageConnectorsActionUI extends ActionUI
{

	public ManageConnectorsActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(ManageConnectorsActionUI.class, "ManageConnectorsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ManageConnectorsActionUI.class, "ManageConnectorsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ManageConnectorsActionUI.class, "ManageConnectorsActionUI.longDesc.decl"));
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.SHIFT_DOWN_MASK);
		putValue(ACCELERATOR_KEY, accel);
	}

	@Override public String getActionClass()
	{
		return ManageConnectorsAction.class.getName();
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			return false;
		}
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ManageConnectorsActionUI.class, "ManageConnectorsActionUI.shortDesc.decl"));
		if (getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}
		return super.isEnabled();
	}
}
