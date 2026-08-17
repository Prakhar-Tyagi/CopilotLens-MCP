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
import chs.images.CHSImageLoader;
import chs.images.CHSImages;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign})
public class CreateMountWithoutPartAndSymbolActionUI extends AbstractCreateOtherComponentActionUI
{

	public CreateMountWithoutPartAndSymbolActionUI(ICaplet caplet)
	{
		super(caplet, CreateMountWithoutPartAndSymbolAction.class.getName());
	}

	protected Icon getIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_RAIL_ACTIVE_ICON);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.LAYOUT_RAIL_INACTIVE_ICON);
	}
}
