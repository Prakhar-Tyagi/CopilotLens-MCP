/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class GenerateWiringDiagramActionUI extends ActionUI
{

	public GenerateWiringDiagramActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, ResourceMgr.getStringForMenu(GenerateWiringDiagramActionUI.class,
				"GenerateWiringDiagramActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(GenerateWiringDiagramActionUI.class,
				"GenerateWiringDiagramActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(GenerateWiringDiagramActionUI.class,
				"GenerateWiringDiagramActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(ResourceMgr.getMnemonic(GenerateWiringDiagramActionUI.class,
				"GenerateWiringDiagramActionUI.mnemonic.decl")));
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return GenerateWiringDiagramInteractiveAction.class.getName();
	}
}
