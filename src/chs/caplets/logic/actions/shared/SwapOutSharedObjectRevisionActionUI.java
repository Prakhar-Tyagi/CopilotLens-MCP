/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_SWAP_OUT_SHARED_OBJECT_REVISION_ACTION",
		label = "Swap Out Revision...",
		tooltip = "Swap Out Revision...",
		icon = "ico_swap_out_shared_object_revision_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class SwapOutSharedObjectRevisionActionUI extends ActionUI
{

	/**
	 * Constructor for the SwapOutSharedObjectRevisionActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public SwapOutSharedObjectRevisionActionUI(ICaplet caplet)
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
						.getMnemonic(EditSharedPinListActionUI.class, "SwapOutSharedObjectRevisionActionUI.mnemonic");
		putValue(MNEMONIC_KEY, iMnemonic);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(SwapOutSharedObjectRevisionActionUI.class,
				"SwapOutSharedObjectRevisionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(SwapOutSharedObjectRevisionActionUI.class,
				"SwapOutSharedObjectRevisionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(SwapOutSharedObjectRevisionActionUI.class,
				"SwapOutSharedObjectRevisionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		return super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return SwapOutSharedObjectRevisionAction.class.getName();
	}
}
