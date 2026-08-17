/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.border.properties.UserZonePropertiesClient;
import chs.caplets.symbol.Model;
import chs.caplets.symbol.actions.SymbolPropertiesAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

/**
 * @author chandras on 20-09-2018.
 */
public class UserZonePropertiesAction extends SymbolPropertiesAction
{

	public UserZonePropertiesAction(ICapletController controller)
	{
		super(controller, new UserZonePropertiesClient((Model) controller.getCapletModel()));
	}

	@Override public String getActionUIClass()
	{
		return UserZonePropertiesActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, @Nullable SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null) {
			SelectSet userDefinedZones = UserZonePropertiesClient.convertToSelectionOfUserDefinedZones(selections);
			if (userDefinedZones != null) {
				container.add(new ActionEntry(actionUI));
			}
		}
	}
}
