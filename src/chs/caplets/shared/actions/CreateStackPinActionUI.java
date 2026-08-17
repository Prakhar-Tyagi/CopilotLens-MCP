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
import chs.caplets.logic.actions.CreateStackPinAction;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 8 Feb, 2011 Time: 2:24:06 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_STACK_PIN_ACTION",
		label = "Create Stack Pin",
		tooltip = "Create Stack Pin",
		icon = "ico_stack_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class CreateStackPinActionUI extends AbstractStackPinActionUI
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateStackPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = KeyEvent.VK_C;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateStackPinActionUI.class, "CreateStackPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateStackPinActionUI.class, "CreateStackPinActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(CreateStackPinActionUI.class, "CreateStackPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	@Override public String getActionClass()
	{
		return CreateStackPinAction.class.getName();
	}
}
