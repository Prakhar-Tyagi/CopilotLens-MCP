/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.utilities.ResourceMgr;

/**
 * Description of the Class
 *
 * @author Darin Jackson
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_DISCONNECT_ACTION",
		label = "Disconnect Selected",
		tooltip = "Disconnect Selected (D)",
		icon = "ico_disconnect_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class DisconnectActionUI extends AbstractDisconnectActionUI
{

	public DisconnectActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	protected String getLongDescription()
	{
		return ResourceMgr.getString(AbstractDisconnectActionUI.class, "DisconnectActionUI.longDesc.decl");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return DisconnectAction.class.getName();
	}
}
