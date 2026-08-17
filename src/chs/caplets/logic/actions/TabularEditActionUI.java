/*
* Copyright 2017 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author pbhawsar on 04-05-2017
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SvcDoc,
		Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_TABULAR_EDIT_ACTION",
		label = "Edit selected objects in table ",
		tooltip = "Select objects in the browser or on the diagram  and edit in a table(Alt+T)",
		icon = "ico_tabular_edit",
		buttonStyle = "SMALL_IMAGE")
public class TabularEditActionUI extends ActionUI
{

	public TabularEditActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, ResourceMgr.getString(AddChainActionUI.class, "TabularEditActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddChainActionUI.class, "TabularEditActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddChainActionUI.class, "TabularEditActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_MASK);
		putValue(ACCELERATOR_KEY, accel);
	}

	@Override public String getActionClass()
	{
		return TabularEditAction.class.getName();
	}
}
