/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import org.jetbrains.annotations.NotNull;

/**
 * Description of the Class
 *
 * @author Darin Jackson
 * @created August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_DELETE_ACTION",
		label = "Delete Selected",
		tooltip = "Delete Selected(Delete)",
		icon = "ico_delete-selected_active",
		buttonStyle = "SMALL_IMAGE")
public class DeleteActionUI extends chs.caplets.shared.actions.DeleteActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public DeleteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override @NotNull public String getActionClass()
	{
		return DeleteAction.class.getName();
	}
}
