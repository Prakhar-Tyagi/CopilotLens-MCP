/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2012-2026 Siemens
 */
package chs.caplets.shared.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caplets.logic.actions.AddToStackPinAction;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 3 Mar, 2011 Time: 1:06:49 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_TO_STACK_PIN_ACTION",
		label = "Add To Stack Pin",
		tooltip = "Add To Stack Pin",
		icon = "ico_stack_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddToStackPinActionUI extends AbstractStackPinActionUI
{

	/**
	 * Constructor for the AddToStackPinActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddToStackPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = KeyEvent.VK_A;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(AddToStackPinActionUI.class, "AddToStackPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddToStackPinActionUI.class, "AddToStackPinActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddToStackPinActionUI.class, "AddToStackPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	@Override public String getActionClass()
	{
		return AddToStackPinAction.class.getName();
	}
}
