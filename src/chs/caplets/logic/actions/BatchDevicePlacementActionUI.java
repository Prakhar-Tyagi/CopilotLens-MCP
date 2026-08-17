/*
 * Copyright 2019 Mentor Graphics Corporation
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
 * @author chandras on 10-10-2019.
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect, Application.CapitalEssentialsDesign,
		Application.ArtisanFunction, Application.SEElectricalDesign})
public class BatchDevicePlacementActionUI extends ActionUI
{

	/**
	 * Constructor for the BatchDevicePlacementActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public BatchDevicePlacementActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_instantiate_active.gif");
		putValue(NAME, ResourceMgr.getString(BatchDevicePlacementActionUI.class,
				"BatchDevicePlacementActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementActionUI.class,
				"BatchDevicePlacementActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementActionUI.class,
				"BatchDevicePlacementActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(BatchDevicePlacementActionUI.class,
				"BatchDevicePlacementActionUI.mnemonic.decl"));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return BatchDevicePlacementAction.class.getName();
	}
}
