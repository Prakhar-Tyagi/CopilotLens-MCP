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

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * @author Matt Boyd
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class CreatePlugConnectorActionUI extends ActionUI
{

	/**
	 * Constructor for CreatePlugConnectorActionUI.
	 *
	 * @param caplet
	 */
	public CreatePlugConnectorActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.PLUG_CONNECTOR_ICON_ENABLED);
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_W);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME,
				ResourceMgr.getString(CreatePlugConnectorActionUI.class, "CreatePlugConnectorActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreatePlugConnectorActionUI.class, "CreatePlugConnectorActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreatePlugConnectorActionUI.class, "CreatePlugConnectorActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.PLUG_CONNECTOR_ICON_DISABLED);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreatePlugConnectorAction.class.getName();
	}
}
