/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 15, 2004 Time: 9:33:10 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_ADD_CHAIN_ACTION",
		label = "Daisy Chain",
		tooltip = "Add Daisy Chain between indicators",
		icon = "ico_chain_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class AddChainActionUI extends ActionUI implements ISelectListener
{

	public AddChainActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return AddChainAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(AddChainActionUI.class, "AddChainActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddChainActionUI.class, "AddChainActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddChainActionUI.class, "AddChainActionUI.putValue.action.text_1"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_chain_active.gif"));
		putValue(MNEMONIC_KEY, (int) ResourceMgr.getMnemonic(AddChainActionUI.class, "AddChainActionUI.mnemonic"));

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
