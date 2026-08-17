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
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalLogicDesigner,
				Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_CREATE_SHARED_OBJECT_REVISION_ACTION",
		label = "Create Revision...",
		tooltip = "Create a Revision...",
		icon = "ico_revisions",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateSharedObjectRevisionActionUI extends ActionUI implements ISharedObjectBrowserAction
{

	/**
	 * Constructor for the CreateSharedObjectRevisionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateSharedObjectRevisionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_revisions.gif");

		Integer iMnemonic = (int) ResourceMgr
				.getMnemonic(EditSharedPinListActionUI.class, "CreateSharedObjectRevisionActionUI.mnemonic");
		putValue(MNEMONIC_KEY, iMnemonic);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(CreateSharedObjectRevisionActionUI.class,
				"CreateSharedObjectRevisionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(CreateSharedObjectRevisionActionUI.class,
				"CreateSharedObjectRevisionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateSharedObjectRevisionActionUI.class,
				"CreateSharedObjectRevisionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateSharedObjectRevisionAction.class.getName();
	}

	public boolean isEnabled()
	{
		IAction action = getAction();
		if (action != null) {
			if (ActionRT.isDesignUnderConcurrentEdit()) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
				return false;
			}
			return ISharedObjectBrowserAction.isTreeConstructionComplete() && action.isEnabled();
		}
		return false;
	}
}
