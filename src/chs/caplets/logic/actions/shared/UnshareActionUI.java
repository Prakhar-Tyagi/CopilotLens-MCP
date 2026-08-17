/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect, Application.CapitalCapture, Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_UNSHARE_ACTION",
		label = "Unshare",
		tooltip = "Unshare an object",
		icon = "ico_unshare_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class UnshareActionUI extends BaseShareActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public UnshareActionUI(ICaplet caplet)
	{
		super(caplet, UnshareActionUI.class);
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		super.setupUI();
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_unshare_active.gif");
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(UnshareActionUI.class, "UnshareActionUI.mnemonic"));
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return UnshareAction.class.getName();
	}
}
