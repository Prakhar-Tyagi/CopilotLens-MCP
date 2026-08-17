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
import chs.caplets.shared.actions.AbstractMovePinActionUI;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_MOVE_PIN_ACTION",
		label = "Move Pin(s)",
		tooltip = "Move Pin(s)",
		icon = "ico_move_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class MovePinActionUI extends AbstractMovePinActionUI
{

	public MovePinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return MovePinAction.class.getName();
	}
}
