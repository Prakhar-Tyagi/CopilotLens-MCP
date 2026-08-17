/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2009-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * FEAT00013725 - Automated handling of shared object revisions
 * <p/>
 * UI class for Enhanced Swap Out from CLogic
 * <p/>
 *
 * @author ntewari
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalLogicDesigner, Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class SharedObjectRevisionUsagesActionUI extends ActionUI
{

	/**
	 * Constructor for the SharedObjectRevisionUsagesActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public SharedObjectRevisionUsagesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		Integer iMnemonic =
				(int) ResourceMgr
						.getMnemonic(SharedObjectRevisionUsagesActionUI.class,
								"SharedObjectRevisionUsagesActionUI.mnemonic");
		putValue(MNEMONIC_KEY, iMnemonic);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(SharedObjectRevisionUsagesActionUI.class,
				"SharedObjectRevisionUsagesActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(SharedObjectRevisionUsagesActionUI.class,
				"SharedObjectRevisionUsagesActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(SharedObjectRevisionUsagesActionUI.class,
				"SharedObjectRevisionUsagesActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return SharedObjectRevisionUsagesAction.class.getName();
	}

	/**
	 * Defer to the Action run-time to determine if the Action should be enabled or not, which can depend on state info.
	 */
	public boolean isEnabled()
	{
		IAction action = getAction();
		return action != null && action.isEnabled();
	}
}
