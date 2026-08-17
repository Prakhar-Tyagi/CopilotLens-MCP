/*
 * Copyright 2010 Mentor Graphics Corporation
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
import chs.caplets.logic.AutoRouteSettingsAction;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

@ApplicationSpecification(
		allowInQAExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign},
		allowInDevExtensionsFor = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.ArtisanFunction, Application.SEElectricalDesign}
)
public class AutoRouteSettingsActionUI extends ActionUI
{

	public AutoRouteSettingsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		putValue(NAME,
				ResourceMgr.getStringForMenu(AutoRouteSettingsActionUI.class, "AutoRouteSettingsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(AutoRouteSettingsActionUI.class, "AutoRouteSettingsActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AutoRouteSettingsActionUI.class, "AutoRouteSettingsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.SHIFT_MASK));
		putValue(MNEMONIC_KEY, KeyEvent.VK_A);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AutoRouteSettingsAction.class.getName();
	}
}
