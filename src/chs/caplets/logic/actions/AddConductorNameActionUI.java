/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.logical.schem.ISegment;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Description of the Class
 *
 * @author Kevin Witten
 * @created June 23, 2005
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CREATE_NAME_TEXT_ACTION",
		label = "Add Name",
		tooltip = "Add Name Text",
		icon = "add_name",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class AddConductorNameActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AddConductorNameActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_nametext_active.gif");
		putValue(SMALL_ICON, icon);

		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_X);
		putValue(MNEMONIC_KEY, iMnemonic);

		putValue(NAME, ResourceMgr.getString(AddConductorNameActionUI.class, "AddConductorNameActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddConductorNameActionUI.class, "AddConductorNameActionUI.name.decl_1"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddConductorNameActionUI.class, "AddConductorNameActionUI.description.text"));

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_nametext_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddConductorNameAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		//
		// Only if the super says yes, we have an option of making this enabled.
		//
		if (super.isEnabled()) {
			ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
			if (activeSM != null) {
				SelectSet presel = activeSM.getPreSelections();
				if (presel.getSelectCount() >= 1) {
					SelectionIterator iter = presel.getSelected();
					Selection sel = iter.getNext();
					if (ISegment.class.isAssignableFrom(sel.getSelectionClass())) {
						bEnable = true;
					}
				}
			}
		}

		setEnabled(bEnable);
	}
}
