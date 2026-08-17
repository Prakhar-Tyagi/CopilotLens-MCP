/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.images.CHSImageLoader;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

/*
 *  Description of the Class
 *
 *@author     Darin Jackson
 *@created    August 1, 2001
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalArchitect, Application.CapitalCapture,
				Application.ArtisanFunction, Application.ArtisanArchitect},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_SHARE_ACTION",
		label = "Share",
		tooltip = "Share an object",
		icon = "ico_share_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ShareActionUI extends BaseShareActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public ShareActionUI(ICaplet caplet)
	{
		super(caplet, ShareActionUI.class);
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		super.setupUI();
		Integer iMnemonic = KeyEvent.VK_R;
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_share_active.gif");
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return ShareAction.class.getName();
	}
}
