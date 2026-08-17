/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.graphics.CreateRectangleActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalSymbolDesigner}) // formboard capability not in VeSys or Systems
public class CreateFormboardRegionDatumActionUI extends CreateRectangleActionUI
{

	public CreateFormboardRegionDatumActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
	}

	public void setupUI()
	{
		String name = ResourceMgr.getString(CreateFormboardRegionDatumActionUI.class,
				"CreateFormboardRegionDatumActionUI.name.decl");
		String shortDesc = ResourceMgr.getString(CreateFormboardRegionDatumActionUI.class,
				"CreateFormboardRegionDatumActionUI.shortDesc.decl");
		String longDesc = ResourceMgr.getString(CreateFormboardRegionDatumActionUI.class,
				"CreateFormboardRegionDatumActionUI.longDesc.decl");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_drawrectangle_active.gif");
		Integer iMnemonic =
				(int) ResourceMgr.getMnemonic(CreateFormboardRegionDatumActionUI.class,
						"CreateFormboardRegionDatumActionUI.mnemonic");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	public String getActionClass()
	{
		return CreateFormboardRegionDatumAction.class.getName();
	}
}
