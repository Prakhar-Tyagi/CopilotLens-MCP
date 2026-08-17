/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Jul 19, 2010 Time: 5:38:40 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalArchitect, Application.CapitalCapture, Application.CapitalHarnessDesigner,
				Application.CapitalSystemsIntegrator, Application.CapitalLogicDesigner, Application.CapitalSymbolDesigner,
				Application.CapitalSymbolForCapture, Application.CapitalEssentialsDesign, Application.CapitalEssentialsHarness,
				Application.CapitalEssentialsSymbolDesigner, Application.SEElectricalDesign, Application.SEElectricalHarness,
				Application.SEElectricalSymbol,
				Application.XSCHarness, Application.XSCSymbol},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_COMMENT_SYMBOL_TO_MC_ACTION",
		label = "Add Comment Symbol",
		tooltip = "Add Comment Symbol",
		icon = "ico_comment_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddCommentSymbolToMCActionUI   extends ActionUI
{

	/**
	 * Constructor for the CreateDeviceActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddCommentSymbolToMCActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_comment_active.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_O);

		putValue(NAME, ResourceMgr.getString(AddCommentSymbolActionUI.class, "AddCommentSymbolToMCActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddCommentSymbolActionUI.class, "AddCommentSymbolToMCActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddCommentSymbolActionUI.class, "AddCommentSymbolToMCActionUI.longDesc.decl"));
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
		return AddCommentSymbolToMCAction.class.getName();
	}
}
