/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.shared.actions.AbstractStackPinActionUI;
import chs.cof.logical.schem.ISchemStackPin;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA. User: bkadukun Date: Feb 28, 2011 Time: 3:37:45 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_EDIT_STACK_PIN_ACTION",
		label = "Edit Stack Pin",
		tooltip = "Edit Stack Pin",
		icon = "ico_stack_pin_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class EditStackPinActionUI extends AbstractStackPinActionUI	
{
	public EditStackPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = KeyEvent.VK_S;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(EditStackPinActionUI.class, "EditStackPinActionUI.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(EditStackPinActionUI.class, "EditStackPinActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(EditStackPinActionUI.class, "EditStackPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	@Override public String getActionClass()
	{
		return EditStackPinAction.class.getName();
	}

	@Override public void selectionChanged(SelectEvent e)
	{
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		int numberOfHighwaysSelected = 0;
		if (activeSM != null) {
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext();) {

				Selection sel = iter.getNext();

				if (ISchemStackPin.class.isAssignableFrom(sel.getSelectionClass())) {
					numberOfHighwaysSelected++;
				}
			}
		}

		setEnabled(numberOfHighwaysSelected == 1);
	}
}
