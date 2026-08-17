/*
 * Copyright 2020 Mentor Graphics Corporation
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
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author chandras on 02-02-2020.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class UpdateLayoutBOMIDsActionUI extends ActionUI
{

	public UpdateLayoutBOMIDsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_table_composite_bom_active.gif");
		Integer iMnemonic = (int) ResourceMgr.getMnemonic(this, "UpdateLayoutBOMIDsActionUI.mnemonic.text");

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(UpdateLayoutBOMIDsActionUI.class,
				"UpdateLayoutBOMIDsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(UpdateLayoutBOMIDsActionUI.class,
				"UpdateLayoutBOMIDsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(UpdateLayoutBOMIDsActionUI.class,
				"UpdateLayoutBOMIDsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_table_composite_bom_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return UpdateLayoutBOMIDsAction.class.getName();
	}
}

