/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * ActionUI class for PropagateAllHarnessAction
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class PropagateAllHarnessActionUI extends ActionUI
{

	public PropagateAllHarnessActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = IconUtils.getPlaceholderIcon(IconUtils.ACTIVE);
		putValue(NAME,
				ResourceMgr.getString(PropagateAllHarnessActionUI.class, "PropagateAllHarnessActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(PropagateAllHarnessActionUI.class, "PropagateAllHarnessActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(PropagateAllHarnessActionUI.class, "PropagateAllHarnessActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	@NotNull @Override public String getActionClass()
	{
		return PropagateAllHarnessAction.class.getName();
	}
}
