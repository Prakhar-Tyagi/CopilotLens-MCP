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

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_STYLE_APPLY_TO_SEGMENTS_ACTION",
		label = "Apply to Segments",
		tooltip = "Apply Style to Segments",
		icon = "ico_transparent",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ApplyStyleToSegmentsActionUI extends ActionUI
{

	public ApplyStyleToSegmentsActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		putValue(NAME, ResourceMgr.getString(ApplyStyleToSegmentsActionUI.class,
				"ApplyStyleToSegmentsActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(ApplyStyleToSegmentsActionUI.class,
				"ApplyStyleToSegmentsActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(ApplyStyleToSegmentsActionUI.class,
				"ApplyStyleToSegmentsActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_A));
	}

	public String getActionClass()
	{
		return ApplyStyleToSegmentsAction.class.getName();
	}
}
